package com.onek.discount;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.consts.CSTATUS;
import com.onek.context.AppContext;
import com.onek.context.UserSession;
import com.onek.discount.entity.*;
import com.onek.discount.util.RelationGoodsSQL;
import com.onek.entitys.Result;
import com.onek.propagation.prod.ActivityManageServer;
import com.onek.propagation.prod.ProdCurrentActPriceObserver;
import com.onek.propagation.prod.ProdDiscountObserver;
import com.onek.queue.delay.DelayedHandler;
import com.onek.queue.delay.RedisDelayedHandler;
import com.onek.util.IceRemoteUtil;
import com.onek.util.SmsTempNo;
import com.onek.util.SmsUtil;
import com.onek.util.prod.ProdInfoStore;
import constant.DSMConst;
import dao.BaseDAO;
import com.onek.util.GenIdUtil;
import org.hyrdpf.util.LogUtil;
import util.BUSUtil;
import util.GsonUtils;
import util.ModelUtil;
import util.TimeUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.onek.discount.CommonModule.getLaderNo;

/**
 * @服务名 discountServer
 * @author 11842
 * @version 1.1.1
 * @description 活动管理
 * @time 2019/4/1 11:19
 **/
public class ActivityManageModule {

    private static BaseDAO baseDao = BaseDAO.getBaseDAO();

    //新增活动
    private static final String INSERT_ACT_SQL = "insert into {{?" + DSMConst.TD_PROM_ACT + "}} "
            + "(unqid,actname,incpriority,cpriority,qualcode,qualvalue,actdesc,"
            + "excdiscount,acttype,actcycle,sdate,edate,brulecode) "
            + "values(?,?,?,?,?,"
            + "?,?,?,?,?,?,?,?)";

    //修改活动
    private static final String UPD_ACT_SQL = "update {{?" + DSMConst.TD_PROM_ACT + "}} set actname=?,"
            + "incpriority=?,cpriority=?,qualcode=?,qualvalue=?,actdesc=?,"
            + "excdiscount=?,acttype=?,actcycle=?,sdate=?,edate=?,brulecode=? where cstatus&1=0 "
            + " and unqid=?";

    //新增场次
    private static final String INSERT_TIME_SQL = "insert into {{?" + DSMConst.TD_PROM_TIME + "}} "
            + "(unqid,actcode,sdate,edate) "
            + " values(?,?,?,?)";

    private static final String DEL_TIME_SQL = "update {{?" + DSMConst.TD_PROM_TIME + "}} set cstatus=cstatus|1 "
            + " where cstatus&1=0 and actcode=?";

    //新增活动商品
    private static final String INSERT_ASS_DRUG_SQL = "insert into {{?" + DSMConst.TD_PROM_ASSDRUG + "}} "
            + "(unqid,actcode,gcode,menucode,actstock,limitnum,price,cstatus) "
            + " values(?,?,?,?,?,?,?,?)";

    private static final String DEL_ASS_DRUG_SQL = "update {{?" + DSMConst.TD_PROM_ASSDRUG + "}} set cstatus=cstatus|1 "
            + " where cstatus&1=0 and actcode=?";

    //优惠阶梯
    private static final String INSERT_LAD_OFF_SQL = "insert into {{?" + DSMConst.TD_PROM_LADOFF + "}} "
            + "(unqid,ladamt,ladnum,offercode,offer) "
            + " values(?,?,?,?,?)";

    private static final String DEL_LAD_OFF_SQL = "update {{?" + DSMConst.TD_PROM_LADOFF + "}} set cstatus=cstatus|1 "
            + " where cstatus&1=0 ";

    private static final String INSERT_RELA_SQL = "insert into {{?" + DSMConst.TD_PROM_RELA + "}} "
            + "(unqid,actcode,ladid) values(?,?,?)";

    //优惠赠换商品
    private static final String INSERT_ASS_GIFT_SQL = "insert into {{?" + DSMConst.TD_PROM_ASSGIFT + "}} "
            + "(unqid,assgiftno,offercode,giftnum)"
            + " values(?,?,?,?)";

    private static final String DEL_ASS_GIFT_SQL = "update {{?" + DSMConst.TD_PROM_ASSGIFT + "}} set cstatus=cstatus|1 "
            + " where cstatus&1=0 ";

    private static final String UPDATE_ACT_CP = "update {{?" + DSMConst.TD_PROM_ACT + "}} set cpriority=? "
            + " where cstatus&1=0 and incpriority=? and cpriority=?";

    private static final String UPDATE_ACT_CPONE = "update {{?" + DSMConst.TD_PROM_ACT + "}} set cpriority=? "
            + " where cstatus&1=0 ";



    //启用
    private static final String OPEN_ACT =
            " UPDATE {{?" + DSMConst.TD_PROM_ACT + "}}"
                    + " SET cstatus = cstatus & " + ~CSTATUS.CLOSE
                    + " WHERE unqid = ? ";

    //停用
    private static final String CLOSE_ACT =
            " UPDATE {{?" + DSMConst.TD_PROM_ACT + "}}"
                    + " SET cstatus = cstatus | " + CSTATUS.CLOSE
                    + " WHERE unqid = ? ";

    //删除
    private static final String DELETE_ACT =
            " UPDATE {{?" + DSMConst.TD_PROM_ACT + "}}"
                    + " SET cstatus = cstatus | 1  WHERE unqid = ? ";


    /**
     * @接口摘要 活动查询
     * @业务场景 后台管理系统查询活动
     * @传参类型 json
     * @传参列表 {pageSize: 每页数量 pageNo：页码 actname: 活动名称 brulecode 活动规则码}
     * @返回列表 ActivityVO对象数组
     */
    @UserPermission(ignore = true)
    public Result queryActivities(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();

        int pageSize = jsonObject.get("pageSize").getAsInt();
        int pageIndex = jsonObject.get("pageNo").getAsInt();
        Page page = new Page();
        page.pageSize = pageSize;
        page.pageIndex = pageIndex;
        PageHolder pageHolder = new PageHolder(page);
        StringBuilder sqlBuilder = new StringBuilder();

        String selectSQL = "select a.unqid,actname,incpriority,cpriority," +
                "qualcode,qualvalue,actdesc,excdiscount,acttype," +
                "actcycle,sdate,edate,a.brulecode,a.cstatus,rulename,(ckstatus & ~32) ckstatus," +
                "case when fun_prom_cycle(unqid,acttype,actcycle,?,1) = 1 and " +
                " a.cstatus & 2048 > 0 and a.cstatus & 1 = 0 and ckstatus & 32 = 0  then 1 " +
                " when ckstatus & 32 > 0 then -1 else 0 end actstatus "+
                " from {{?" + DSMConst.TD_PROM_ACT + "}} a "
                + " left join {{?" + DSMConst.TD_PROM_RULE +"}} b on a.brulecode=b.brulecode"
                + " where a.cstatus&1=0 ";
        sqlBuilder.append(selectSQL);
        sqlBuilder = getParamsDYSQL(sqlBuilder, jsonObject).append(" group by unqid desc");
        String day = TimeUtils.date_Md_2String(new Date());
        List<Object[]> queryList = baseDao.queryNative(pageHolder, page, sqlBuilder.toString(),new Object[]{day});
        ActivityVO[] activityVOS = new ActivityVO[queryList.size()];
        baseDao.convToEntity(queryList, activityVOS, ActivityVO.class,new String[]{
                "unqid","actname","incpriority","cpriority",
                "qualcode","qualvalue","actdesc","excdiscount",
                "acttype","actcycle","sdate","edate","brulecode",
                "cstatus","ruleName","ckstatus","actstatus"});
        return result.setQuery(activityVOS, pageHolder);
    }


    private StringBuilder getParamsDYSQL(StringBuilder sqlBuilder, JsonObject jsonObject) {
        String actName = jsonObject.get("actname").getAsString();
        if (actName != null && !actName.isEmpty()) {
            sqlBuilder.append(" and actname like '%").append(actName).append("%'");
        }

        if (jsonObject.get("brulecode") != null && !jsonObject.get("brulecode").getAsString().isEmpty()) {
            int ruleCode = jsonObject.get("brulecode").getAsInt();
            if (ruleCode > 0) {
                sqlBuilder.append(" and a.brulecode=").append(ruleCode);
            }
        }
        return sqlBuilder;
    }



    /**
     * @接口摘要 新增修改活动
     * @业务场景 后台管理活动新增修改
     * @传参类型 json
     * @传参列表 ActivityVO对象
     * @返回列表 200 成功 -1 失败
     */
    @UserPermission(ignore = true)
    public Result insertActivity(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        ActivityVO activityVO = GsonUtils.jsonToJavaBean(json, ActivityVO.class);
        if (activityVO == null) return result.fail("参数错误");
        int cpt = getCPCount(activityVO.getIncpriority(), activityVO.getCpriority());
        if (cpt == -1) return result.fail("优先级已超过");
        if (Long.parseLong(activityVO.getUnqid()) > 0) {//修改
//            if (theActInProgress(Long.parseLong(activityVO.getUnqid()))) {
//                return result.fail("活动正在进行中，无法修改！");
//            }
            result =  updateActivity(activityVO, cpt);
        } else {//新增
            result = insertActivity(activityVO, cpt);
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            //活动开始前五分钟
            long start5Time = dateFormat.parse(activityVO.getSdate()+
                    " " + activityVO.getTimeVOS().get(0).getSdate()).getTime() - 5*60*1000;
            long now = new Date().getTime();
            if (start5Time > now) {
                long times = (start5Time - now)/1000/60;
                DelayedHandler<ActivityVO> notice_all_comp = new RedisDelayedHandler<>("_NOTICE_ALL_COMP_ACT",
                        times, ActivityManageModule::noticeAllComp,
                        DelayedHandler.TIME_TYPE.MINUTES);
                notice_all_comp.add(activityVO);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static boolean noticeAllComp(ActivityVO activityVO) {
        IceRemoteUtil.sendMessageToAllClient(SmsTempNo.ACTIVITIES_OF_NEW,
                "", "【" + activityVO.getActname() + "】将于" + activityVO.getSdate() +
                        " " +activityVO.getTimeVOS().get(0).getSdate() + "开始进行");
        SmsUtil.sendMsgToAllBySystemTemp(SmsTempNo.ACTIVITIES_OF_NEW,"", "【" + activityVO.getActname() + "】将于" + activityVO.getSdate() +
                " " +activityVO.getTimeVOS().get(0).getSdate() + "开始进行");
        return true;
    }

    /**
     * @description 活动新增
     * @params [activityVO]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/2 15:43
     * @version 1.1.1
     **/
    private Result insertActivity(ActivityVO activityVO, int cpt) {
        boolean b;
        int bruleCode = activityVO.getBrulecode();//规则码(前四位)
        int rCode = Integer.parseInt(bruleCode + "" + activityVO.getRulecomp());//前五位码
        Result result = new Result();
        long actCode = GenIdUtil.getUnqId();//唯一码(活动码)
        //新增活动
        if (cpt == activityVO.getCpriority()) {
           int re = baseDao.updateNative(INSERT_ACT_SQL,actCode, activityVO.getActname(),
                   activityVO.getIncpriority(), activityVO.getCpriority(), activityVO.getQualcode(), activityVO.getQualvalue(), activityVO.getActdesc(),
                   activityVO.getExcdiscount(), activityVO.getActtype(), activityVO.getActcycle(), activityVO.getSdate(),
                   activityVO.getEdate(), bruleCode);
           b = re > 0;
        } else {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[]{cpt, activityVO.getIncpriority(), activityVO.getCpriority()});
            params.add(new Object[]{actCode, activityVO.getActname(),
                    activityVO.getIncpriority(), activityVO.getCpriority(), activityVO.getQualcode(), activityVO.getQualvalue(), activityVO.getActdesc(),
                    activityVO.getExcdiscount(), activityVO.getActtype(), activityVO.getActcycle(), activityVO.getSdate(),
                    activityVO.getEdate(), bruleCode});
            int[] actResult = baseDao.updateTransNative(new String[]{UPDATE_ACT_CP, INSERT_ACT_SQL}, params);
            b = !ModelUtil.updateTransEmpty(actResult);
        }
        if (b) {
            //新增活动场次
            if (activityVO.getTimeVOS() != null && !activityVO.getTimeVOS().isEmpty()) {
                insertTimes(activityVO.getTimeVOS(), actCode);
            }
            //新增阶梯
            if (activityVO.getLadderVOS() != null && !activityVO.getLadderVOS().isEmpty() && bruleCode != 1113) {
                insertLadOff(activityVO.getLadderVOS(),bruleCode, rCode, actCode);
            }
            //新增活动商品
//            if (activityVO.getAssDrugVOS() != null && !activityVO.getAssDrugVOS().isEmpty()) {
//                insertAssDrug(activityVO.getAssDrugVOS());
//            }
        } else {
            return result.fail("新增失败");
        }
        return result.success("新增成功");
    }





    /**
     * @description 活动修改
     * @params [activityVO]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/2 15:43
     * @version 1.1.1
     **/
    private Result updateActivity(ActivityVO activityVO, int cpt) {
        boolean b;
        Result result = new Result();
        long actCode = Long.parseLong(activityVO.getUnqid());
        int oldRuleCode = selectBRuleCode(actCode);
        int bRuleCode = activityVO.getBrulecode();//前四位 规则业务码
        int rCode = Integer.parseInt(bRuleCode + "" + activityVO.getRulecomp());//前五位码
        //新增活动
        if (cpt == activityVO.getCpriority()) {
            int re = baseDao.updateNative(UPD_ACT_SQL, activityVO.getActname(),
                    activityVO.getIncpriority(), activityVO.getCpriority(), activityVO.getQualcode(), activityVO.getQualvalue(), activityVO.getActdesc(),
                    activityVO.getExcdiscount(), activityVO.getActtype(), activityVO.getActcycle(), activityVO.getSdate(),
                    activityVO.getEdate(), bRuleCode, actCode);
            b = re > 0;
        } else {
            long otherCode = selectActCode(activityVO.getIncpriority(), activityVO.getCpriority());
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[]{cpt});
            params.add(new Object[]{activityVO.getActname(),
                    activityVO.getIncpriority(), activityVO.getCpriority() , activityVO.getQualcode(), activityVO.getQualvalue(), activityVO.getActdesc(),
                    activityVO.getExcdiscount(), activityVO.getActtype(), activityVO.getActcycle(), activityVO.getSdate(),
                    activityVO.getEdate(), activityVO.getBrulecode(),actCode});
            int[] actResult = baseDao.updateTransNative(new String[]{UPDATE_ACT_CPONE + " and unqid=" + otherCode,
                    UPD_ACT_SQL}, params);
            b = !ModelUtil.updateTransEmpty(actResult);
        }
        if (b) {
            //新增活动场次
            if (activityVO.getTimeVOS() != null && !activityVO.getTimeVOS().isEmpty()) {
                if (baseDao.updateNative(DEL_TIME_SQL,actCode) > 0) {
                    insertTimes(activityVO.getTimeVOS(), actCode);
                }
            }
            //新增阶梯
            if (activityVO.getLadderVOS() != null && !activityVO.getLadderVOS().isEmpty() && bRuleCode != 1113) {
                if (delRelaAndLadder(actCode)) {
                    insertLadOff(activityVO.getLadderVOS(), bRuleCode, rCode,actCode);
                }
            }

            // 通知活动有修改
            noticeActUpdate(actCode);

        } else {
            result.fail("修改失败");
        }

        return result.success("修改成功");
    }

    private boolean delRelaAndLadder(long actCode) {
        List<Object[]> params = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder offerBuilder = new StringBuilder();
        String selectSQL = "select ladid,offercode from {{?" + DSMConst.TD_PROM_RELA + "}} a left join {{?"
                + DSMConst.TD_PROM_LADOFF +"}} b on a.ladid=b.unqid  where a.cstatus&1=0" +
                " and actcode=" + actCode;
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        if (queryResult == null || queryResult.isEmpty()) return false;
        for (Object[] aQueryResult : queryResult) {
            stringBuilder.append((long) aQueryResult[0]).append(",");
            offerBuilder.append((int) aQueryResult[1]).append(",");
        }
        String ladIdStr = stringBuilder.toString().substring(0, stringBuilder.toString().length() - 1);
        String offerStr = offerBuilder.toString().substring(0, offerBuilder.toString().length() - 1);
        String sqlOne = DEL_LAD_OFF_SQL + " and unqid in(" + ladIdStr + ")";
        params.add(new Object[]{});
        String sqlTwo = "update {{?" + DSMConst.TD_PROM_RELA + "}} set cstatus=cstatus|1 where cstatus&1=0 "
                + " and actcode=" + actCode;
        params.add(new Object[]{});
        String sqlThree = "update {{?" + DSMConst.TD_PROM_ASSGIFT + "}} set cstatus=cstatus|1 where cstatus&1=0 "
                + " and offercode in(" + offerStr +")";
        params.add(new Object[]{});
        return !ModelUtil.updateTransEmpty(baseDao.updateTransNative(new String[]{sqlOne,sqlTwo, sqlThree}, params));
    }

    private int selectBRuleCode(long actCode) {
        String sql = "select brulecode from {{?" + DSMConst.TD_PROM_ACT + "}} where cstatus&1=0 "
                + " and unqid=" + actCode;
        List<Object[]> queryResult = baseDao.queryNative(sql);
        return Integer.parseInt(String.valueOf(queryResult.get(0)[0]));
    }

    private long selectActCode(int incpriority, int cpriority) {
        String sql = "select unqid from {{?" + DSMConst.TD_PROM_ACT + "}} where cstatus&1=0 "
                + " and incpriority=" + incpriority + " and cpriority=" + cpriority;
        List<Object[]> queryResult = baseDao.queryNative(sql);
        return Long.parseLong(String.valueOf(queryResult.get(0)[0]));
    }


    /* *
     * @description 新增活动场次
     * @params [timeVOS]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/2 15:59
     * @version 1.1.1
     **/
    private void insertTimes(List<TimeVO> timeVOS, long actCode) {
        List<Object[]> timeParams = new ArrayList<>();
        for (TimeVO timeVO : timeVOS) {
            timeParams.add(new Object[]{GenIdUtil.getUnqId(), actCode,
                    timeVO.getSdate(), timeVO.getEdate()});
        }
        int[] result = baseDao.updateBatchNative(INSERT_TIME_SQL, timeParams, timeVOS.size());
        boolean b = !ModelUtil.updateTransEmpty(result);
    }

    /* *
     * @description 新增活动优惠阶梯
     * @params []
     * @return int
     * @exception
     * @author 11842
     * @time  2019/4/2 16:09
     * @version 1.1.1
     **/
    private void insertLadOff(List<LadderVO> ladderVOS, int bRuleCode, int rCode, long actCode) {
        List<Object[]> ladOffParams = new ArrayList<>();
        List<Object[]> relaParams = new ArrayList<>();
        List<Object[]> assGiftParams = new ArrayList<>();
        String stype = bRuleCode + "";
        int offerCode[] = getLaderNo(rCode + "", ladderVOS.size());
        for (int i = 0; i < ladderVOS.size(); i++) {
            if (offerCode != null) {
                long ladderId = GenIdUtil.getUnqId();
                ladOffParams.add(new Object[]{ladderId, ladderVOS.get(i).getLadamt()*100,
                        ladderVOS.get(i).getLadnum(),offerCode[i],ladderVOS.get(i).getOffer()*100});
                relaParams.add(new Object[]{GenIdUtil.getUnqId(),actCode,ladderId});
                //新增优惠赠换商品
                if (stype.startsWith("124")) {
                    List<AssGiftVO> assGiftVOList = ladderVOS.get(i).getAssGiftVOS();
                    for (AssGiftVO assGiftVO : assGiftVOList) {
                        assGiftParams.add(new Object[]{GenIdUtil.getUnqId(), assGiftVO.getAssgiftno(),offerCode[i], assGiftVO.getGiftnum()});
                    }
                }
            }
        }
        int[] result = baseDao.updateBatchNative(INSERT_LAD_OFF_SQL, ladOffParams, ladderVOS.size());
        baseDao.updateBatchNative(INSERT_RELA_SQL, relaParams, ladderVOS.size());
        if (stype.startsWith("124")) {
            baseDao.updateBatchNative(INSERT_ASS_GIFT_SQL, assGiftParams, ladderVOS.size());
        }
        boolean b = !ModelUtil.updateTransEmpty(result);
//        return b ? 1 : 0;
    }

    /* *
     * @description 新增优惠赠换商品
     * @params [assGiftVOS]
     * @return void
     * @exception
     * @author 11842
     * @time  2019/4/2 16:19
     * @version 1.1.1
     **/
    private void insertAssGift(List<AssGiftVO> assGiftVOS, long actCode) {
        List<Object[]> assGiftParams = new ArrayList<>();
        for (AssGiftVO assGiftVO : assGiftVOS) {
            assGiftParams.add(new Object[]{GenIdUtil.getUnqId(), actCode, assGiftVO.getAssgiftno()});
        }
        int[] result = baseDao.updateBatchNative(INSERT_ASS_GIFT_SQL, assGiftParams, assGiftVOS.size());
        boolean b = !ModelUtil.updateTransEmpty(result);
//        return b ? 1 : 0;
    }
    
    
    /* *
     * @description 关联活动商品
     * @params [goodsVOS]
     * @return void
     * @exception
     * @author 11842
     * @time  2019/4/2 16:23
     * @version 1.1.1
     **/
    private void relationAssDrug(List<GoodsVO> insertDrugVOS, List<GoodsVO> updDrugVOS, List<Long> delGoodsGCode,long actCode) {
        List<Object[]> insertDrugParams = new ArrayList<>();
        List<Object[]> updateDrugParams = new ArrayList<>();
        StringBuilder gCodeSb = new StringBuilder();
        if (delGoodsGCode.size() > 0) {
            for (long gCode : delGoodsGCode) {
                gCodeSb.append(gCode).append(",");
            }
            String gCodeStr = gCodeSb.toString().substring(0, gCodeSb.toString().length() - 1);
            String delSql = "update {{?" + DSMConst.TD_PROM_ASSDRUG + "}} set cstatus=cstatus|1 "
                    + " where cstatus&1=0 and actcode=" + actCode + " and gcode in(" + gCodeStr +")";
            baseDao.updateNative(delSql);
        }
        String updateSql = "update {{?" + DSMConst.TD_PROM_ASSDRUG + "}} set actstock=?,limitnum=?, price=?, "
                + "cstatus=? where cstatus&1=0 and gcode=? and actcode=?";

        for (GoodsVO insGoodsVO : insertDrugVOS) {
            insertDrugParams.add(new Object[]{GenIdUtil.getUnqId(), actCode, insGoodsVO.getGcode(),
                    insGoodsVO.getMenucode(),insGoodsVO.getActstock(),insGoodsVO.getLimitnum(),
                    insGoodsVO.getPrice(), insGoodsVO.getCstatus()});
        }
        for (GoodsVO updateGoodsVO : updDrugVOS) {
            updateDrugParams.add(new Object[]{updateGoodsVO.getActstock(),updateGoodsVO.getLimitnum(),
                    updateGoodsVO.getPrice(),updateGoodsVO.getCstatus(),
                    updateGoodsVO.getGcode(),actCode});
        }
        baseDao.updateBatchNative(INSERT_ASS_DRUG_SQL, insertDrugParams, insertDrugVOS.size());
        baseDao.updateBatchNative(updateSql, updateDrugParams, updDrugVOS.size());
    }


    /* *
     * @description 根据活动组获取组内可用优先级
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/1 17:44
     * @version 1.1.1
     **/
    /**
     * @接口摘要 根据活动组获取组内可用优先级
     * @业务场景 活动优先级下拉列表调用
     * @传参类型 json
     * @传参列表 {incpriority 当前活动组内优先级}
     * @返回列表 优先级int数组
     */
    @UserPermission(ignore = true)
    public Result getCPriority(AppContext appContext) {
        Result result = new Result();
        List<Integer> cps = new ArrayList<>();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int incpriority = jsonObject.get("incpriority").getAsInt();
        String selectSQL = "select DISTINCT cpriority from {{?" + DSMConst.TD_PROM_ACT + "}} where cstatus&1=0 "
                + " and incpriority=" + incpriority;
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        if (queryResult == null || queryResult.isEmpty()) {
            return result.success(new Integer[]{0,1,2,3,4,5,6,7,8,9});
        }
        for (int i = 0; i < 10 ; i++) {
            for (Object[] aQueryResult : queryResult) {
                int re = (int) aQueryResult[0];
                if (re != i) {
                    cps.add(i);
                }
            }
        }
        return result.success(cps.toArray(new Integer[0]));
    }

    /**
     * @description
     * @params [incpriority]
     * @return int
     * @exception
     * @author 11842
     * @time  2019/4/2 9:11
     * @version 1.1.1
     **/
    private int getCPCount(int incpriority, int cpriority) {
        String selectSQL = "select cpriority from {{?" + DSMConst.TD_PROM_ACT + "}} where cstatus&1=0 "
                + " and incpriority=" + incpriority + " order by cpriority";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        if (queryResult == null || queryResult.isEmpty()) {
            return cpriority;//优先级全部可用
        }
        if (queryResult.size() > 8) {
            return -1;//优先级超出
        }
        int[] points = new int[queryResult.size()];
        for (int i = 0; i < queryResult.size(); i++) {
            points[i] = (int) queryResult.get(i)[0];
        }
        if (valueExit(points, cpriority)) {
            return BUSUtil.getBreak(points, 1, 9);
        }
        return cpriority;
    }

    private boolean valueExit(int[] arr, int value) {
        for (int anArr : arr) {
            if (anArr == value){
                return true;
            }
        }
        return false;
    }

    /**
     * @接口摘要 获取活动详情
     * @业务场景 详情
     * @传参类型 json
     * @传参列表 {unqid 活动码}
     * @返回列表 ActivityVO对象
     */
    @UserPermission(ignore = true)
    public Result getActDetail(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        long actCode = jsonObject.get("unqid").getAsLong();
        String selectSQL = "select a.unqid,actname,incpriority,cpriority," +
                "qualcode,qualvalue,actdesc,excdiscount,acttype," +
                "actcycle,sdate,edate,a.brulecode,a.cstatus,rulename,(ckstatus & ~32) ckstatus from {{?" + DSMConst.TD_PROM_ACT + "}} a "
                + " left join {{?" + DSMConst.TD_PROM_RULE +"}} b on a.brulecode=b.brulecode"
                + " where a.cstatus&1=0 and a.unqid=" + actCode;
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        ActivityVO[] activityVOS = new ActivityVO[queryResult.size()];
        baseDao.convToEntity(queryResult, activityVOS, ActivityVO.class, new String[]{
                "unqid","actname","incpriority","cpriority",
                "qualcode","qualvalue","actdesc","excdiscount",
                "acttype","actcycle","sdate","edate","brulecode",
                "cstatus","ruleName","ckstatus"
        });
        int rRuleCode = activityVOS[0].getBrulecode();
        String rType = rRuleCode + "";
        activityVOS[0].setRuletype(Integer.parseInt(rType.substring(1,2)));
        activityVOS[0].setPreWay(Integer.parseInt(rType.substring(2,3)));
        activityVOS[0].setTimeVOS(getTimes(actCode));
        if (activityVOS[0].getBrulecode() != 1113) {
            activityVOS[0].setLadderVOS(getLadder(activityVOS[0], actCode));
        }
        activityVOS[0].setActiveRule(getRules(rRuleCode));
        return result.success(activityVOS[0]);
    }

    /* *
     * @description 根据活动码获取活动场次
     * @params [actCode]
     * @return java.util.List<com.onek.discount.entity.TimeVO>
     * @exception
     * @author 11842
     * @time  2019/4/2 22:33
     * @version 1.1.1
     **/
    private List<TimeVO> getTimes(long actCode) {
        String sql = "select unqid,actcode,sdate,edate,cstatus from {{?" + DSMConst.TD_PROM_TIME
                + "}} where cstatus&1=0 and actcode=" + actCode;
        List<Object[]> queryResult = baseDao.queryNative(sql);
        TimeVO[] timeVOS = new TimeVO[queryResult.size()];
        baseDao.convToEntity(queryResult, timeVOS, TimeVO.class);
        return Arrays.asList(timeVOS);
    }


    private List<LadderVO> getLadder(ActivityVO activityVO, long actCode) {
        String selectSQL = "select a.unqid,ladamt,ladnum,offercode,offer,a.cstatus from {{?" + DSMConst.TD_PROM_RELA
                + "}} a left join {{?" + DSMConst.TD_PROM_LADOFF + "}} b on a.ladid=b.unqid "
                + " where a.cstatus&1=0 and a.actcode=" + actCode;
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        LadderVO[] ladderVOS = new LadderVO[queryResult.size()];
        baseDao.convToEntity(queryResult, ladderVOS, LadderVO.class);
        for (LadderVO ladderVO:ladderVOS) {
            ladderVO.setLadamt(ladderVO.getLadamt()/100);
            ladderVO.setOffer(ladderVO.getOffer()/100);
            ladderVO.setAssGiftVOS(getAssGift(ladderVO.getOffercode()));
        }
        String offerCode = ladderVOS[0].getOffercode() + "";
        activityVO.setRulecomp(Integer.parseInt(offerCode.substring(4,5)));
        return Arrays.asList(ladderVOS);
    }

    private List<RulesVO> getRules(int bRuleCode) {
        int code = Integer.parseInt((bRuleCode + "").substring(0,3));
        String selectSQL = "select brulecode,rulename from {{?" + DSMConst.TD_PROM_RULE + "}} a where cstatus&1=0 "
                + " and brulecode like '" + code + "%'";
//        String selectSQL = "select brulecode,rulename from {{?" + DSMConst.TD_PROM_RULE + "}} a where cstatus&1=0 "
//                + " and brulecode like '" + code + "%' and  NOT EXISTS(select brulecode from {{?"
//                + DSMConst.TD_PROM_ACT +"}} b where cstatus&1=0 and a.brulecode = b.brulecode and brulecode like '"
//                + code +"%' and edate>CURRENT_DATE and a.brulecode<>"+bRuleCode+")";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        RulesVO[] rulesVOS = new RulesVO[queryResult.size()];
        baseDao.convToEntity(queryResult, rulesVOS, RulesVO.class, new String[]{"brulecode", "rulename"});
        return Arrays.asList(rulesVOS);
    }

    private List<AssGiftVO> getAssGift(int offercode) {
        String sql = "select assgiftno,a.giftnum,giftname from {{?" + DSMConst.TD_PROM_ASSGIFT
                + "}} a left join {{?" + DSMConst.TD_PROM_GIFT + "}} g on a.assgiftno=g.unqid "
                + " where a.cstatus&1=0 and offercode=" + offercode;
        List<Object[]> queryResult = baseDao.queryNative(sql);
        AssGiftVO[] assGiftVOS = new AssGiftVO[queryResult.size()];
        baseDao.convToEntity(queryResult, assGiftVOS, AssGiftVO.class, new String[]{
                "assgiftno", "giftnum", "giftname"
        });
        return Arrays.asList(assGiftVOS);
    }

    
    /* *
     * @description 全部商品
     * @params [jsonObject, actCode, rulecode]
     * @return int
     * @exception
     * @author 11842
     * @time  2019/5/15 23:51
     * @version 1.1.1
     **/
    private long relationAllGoods(JsonObject jsonObject,long actCode, int rulecode) {
        long result = 0;
        List<Object[]> params = new ArrayList<>();
        List<Long> skus = new ArrayList<>();
        int limitnum = jsonObject.get("limitnum").getAsInt();
        int actstock = jsonObject.get("actstock").getAsInt();
        double price = jsonObject.get("price").getAsDouble();
        int cstatus = jsonObject.get("cstatus").getAsInt();
        List<Long> gcodeList = selectGoodsByAct(actCode);
        //全部商品判断库存
        result = theGoodsStockIsEnough(actstock, cstatus, actCode);
        if (result > 0) {
            return result;
        }
        if ((cstatus & 512) == 0) {
            price = price * 100;
        }
        if (gcodeList.size() == 1 && gcodeList.get(0) == 0 ) {
            String updateSQL = "update {{?" + DSMConst.TD_PROM_ASSDRUG + "}} set actstock=?, limitnum=?,"
                    + "price=?, cstatus=? where cstatus&1=0 and actcode=? ";
            result = baseDao.updateNative(updateSQL, actstock, limitnum, price,
                    cstatus,actCode);
        } else if (gcodeList.size() == 0){
            result = baseDao.updateNative(INSERT_ASS_DRUG_SQL, GenIdUtil.getUnqId(), actCode, 0, 0,
                    actstock, limitnum, price,cstatus);
        } else {
            skus = gcodeList;
            String updateAssDrug = "update {{?" + DSMConst.TD_PROM_ASSDRUG + "}} set cstatus=cstatus|1 "
                     + " where cstatus&1=0 and  actcode=? ";
            params.add(new Object[]{actCode});
            params.add(new Object[]{GenIdUtil.getUnqId(), actCode, 0, 0, actstock, limitnum, price,cstatus});
            result = !ModelUtil.updateTransEmpty(baseDao.updateTransNative(new String[]{updateAssDrug,
                    INSERT_ASS_DRUG_SQL}, params)) ? 1 : 0;

        }
        List<GoodsVO> goodsVOS = new ArrayList<>();
        GoodsVO goodsVO = new GoodsVO();
        goodsVO.setActstock(actstock);
        goodsVO.setLimitnum(limitnum);
        goodsVO.setCstatus(cstatus);
        goodsVO.setActcode(actCode + "");
        goodsVOS.add(goodsVO);
        List<GoodsVO> delGoods = new ArrayList<>();
        for (long sku :skus) {
            GoodsVO goodsVO1 = new GoodsVO();
            goodsVO1.setActcode(actCode + "");
            goodsVO1.setGcode(sku);
            delGoods.add(goodsVO1);
        }
        noticeGoodsUpd(1, goodsVOS, delGoods,rulecode, actCode);
        return result;
    }


    /* *
     * @description 判断商品是否足够
     * @params []
     * @return boolean
     * @exception
     * @author 11842
     * @time  2019/5/15 23:24
     * @version 1.1.1
     **/
    private long theGoodsStockIsEnough(int actStock, int cstatus, long actCode) {
        String selectSQL = "select sku, systore,notused as minunused from (select sku,systore, sum(used) as uused, " +
                " (systore-sum(used)) as notused from ( select sku,systore, used  from ( " +
                "SELECT sku,gcode,(store-freezestore) as systore, " +
                "  ceil(IF( ua.cstatus & 256 > 0, sum( actstock ), 0 ) * 0.01 * ( store - freezestore ) + IF " +
                "  ( ua.cstatus & 256 = 0, sum( actstock ), 0 ) ) AS used FROM   " +
                "  (SELECT " +
                "  spu,sku,gcode,store,freezestore,actstock,a.cstatus  " +
                "FROM  {{?" + DSMConst.TD_PROM_ASSDRUG+"}} a " +
                "  LEFT JOIN td_prod_sku s ON s.sku LIKE CONCAT( '_', a.gcode, '%' )  " +
                "WHERE  a.cstatus & 1 = 0 AND length( gcode ) < 14 AND gcode > 0  " +
                "  AND actcode IN ( SELECT unqid FROM td_prom_act WHERE cstatus & 1 = 0 ) and actcode<>"
                + actCode+" UNION ALL " +
                "SELECT  spu,  sku,gcode,  store,  freezestore,actstock,  a.cstatus  " +
                "FROM td_prom_assdrug a LEFT JOIN td_prod_sku s ON s.sku = gcode  " +
                "WHERE a.cstatus & 1 = 0  AND length( gcode ) = 14  " +
                "  AND actcode IN ( SELECT unqid FROM td_prom_act WHERE cstatus & 1 = 0 ) and actcode<>"
                + actCode+" UNION ALL " +
                "SELECT  spu,  sku,  gcode,  store,  freezestore,  actstock,  a.cstatus  " +
                "FROM  td_prom_assdrug a,  td_prod_sku s  " +
                "WHERE  a.cstatus & 1 = 0   AND gcode = 0  " + " and actcode<>" + actCode +
                "  AND actcode IN ( SELECT unqid FROM td_prom_act WHERE cstatus & 1 = 0 ) " +
                "  ) ua " +
                "WHERE  ua.cstatus & 1 = 0  " +
                "GROUP BY  sku , ua.cstatus ) ub UNION ALL  " +
                "select sku,store, 0 from td_prod_sku where cstatus&1=0) uc GROUP BY sku) ud  ";
        if ((cstatus & 256) > 0) {//库存百分比
            double percent = actStock * 0.01;
            selectSQL = selectSQL +  " HAVING minunused < ceil(store*"+percent+")  LIMIT 0,1";
        } else {//库存量
            selectSQL = selectSQL +  " HAVING minunused < "+ actStock + " LIMIT 0,1";
        }
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        if (queryResult == null || queryResult.size() == 0) return 0;
        return  (long)queryResult.get(0)[0];
    }


    /**
     * @接口摘要 活动关联商品
     * @业务场景 活动配置商品
     * @传参类型 json
     * @传参列表 {type 1:全部商品  3部分商品  2 部分类别  actCode 活动码 rulecode 活动规则码}
     * @返回列表 200 成功 -1 失败
     */
    @UserPermission(ignore = true)
    public Result relationGoods(AppContext appContext) {
        int re = 0;
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int type = jsonObject.get("type").getAsInt();//1:全部商品  3部分商品  2 部分类别
        long actCode = jsonObject.get("actCode").getAsLong();
        int ruleCode = jsonObject.get("rulecode").getAsInt();
        //判断活动是否在进行中。。。
//        if (theActInProgress(actCode)) {
//            return result.fail("活动正在进行中，无法修改！");
//        }
        long code;
        switch (type) {
            case 1://全部商品
                //删除该活动下分类设置和商品设置
                code = relationAllGoods(jsonObject, actCode, ruleCode);
                if ((code+ "").length() == 14) {
                    return result.fail("【" + ProdInfoStore.getProdBySku(code).getProdname() + "】库存不够，请重新设置活动库存！");
                }
                if (code > 0) {
                    return result.success("关联全部商品成功");
                }
                return result.fail("关联全部商品失败");
            case 2://类别关联
                JsonObject codeStr = relationClasses(jsonObject, actCode, ruleCode);
                assert codeStr != null;
                if (codeStr.get("result").getAsInt() > 0) {
                    return result.success("关联类别成功");
                }
                return result.fail(codeStr.get("message").getAsString());
            default://商品关联
                List<Long> gcodeList = selectGoodsByAct(actCode);
                String skuStr = getActStockBySkus1(jsonObject, actCode);
                if (skuStr != null) {
                    return result.fail("【" + skuStr + "】商品库存不够，保存失败！");
                }
                re = updateAssByActcode(3, actCode);
                if (re < 0) {
                    return  result.fail("关联商品失败");
                }
                int a = relationGoods(jsonObject, actCode, ruleCode, gcodeList);
                if (a == -1) {
                    return result.fail("活动正在进行中，无法修改！");
                }
                return result.success("关联商品成功");
        }
    }

    private int updateAssByActcode(int type, long actCode) {
        String updateSQL = "update {{?" + DSMConst.TD_PROM_ASSDRUG + "}} set cstatus=cstatus|1 "
                + " where cstatus&1=0 and actcode=? ";
        List<Long> skus;
        switch (type) {
            case 1:
                updateSQL = updateSQL + " and gcode<>0";
                skus = selectGoodsByType(actCode, " and gcode<>0");
                break;
            case 2:
                updateSQL = updateSQL + " and (gcode=0 or LENGTH(gcode)=14)";
                skus = selectGoodsByType(actCode, " and (gcode=0 or LENGTH(gcode)=14)");
                break;
            default:
                updateSQL = updateSQL + " and (gcode=0 or LENGTH(gcode)<14)";
                skus = selectGoodsByType(actCode, " and (gcode=0 or LENGTH(gcode)<14)");
                break;
        }
        int result = baseDao.updateNative(updateSQL, actCode);
        if (result > 0) {
            List<GoodsVO> delGoods = new ArrayList<>();
            for (long sku :skus) {
                GoodsVO goodsVO = new GoodsVO();
                goodsVO.setActcode(actCode + "");
                goodsVO.setGcode(sku);
                delGoods.add(goodsVO);
            }
            noticeGoodsUpd(-1, null, delGoods, 0, actCode);

        }
        return result;
    }

    private List<Long> selectGoodsByType(long actCode, String conDSql) {
        List<Long> gcodeList = new ArrayList<>();
        String sql = "select gcode from {{?" + DSMConst.TD_PROM_ASSDRUG + "}} where cstatus&1=0 and "
                + " actcode=" + actCode + conDSql;
        List<Object[]> queryResult = baseDao.queryNative(sql);
        if (queryResult==null || queryResult.isEmpty()) return gcodeList;
        for (Object[] aQueryResult : queryResult) {
            gcodeList.add((long) aQueryResult[0]);
        }
        return gcodeList;
    }
    
    /* *
     * @description 指定类别
     * @params [jsonObject, actCode, rulecode]
     * @return int
     * @exception
     * @author 11842
     * @time  2019/5/15 23:52
     * @version 1.1.1
     **/
    private JsonObject relationClasses(JsonObject jsonObject,long actCode, int rulecode) {
        JsonObject resultObj = new JsonObject();
        List<Long> gcodeList = selectGoodsByAct(actCode);
        //关联活动商品
        JsonArray goodsArr = jsonObject.get("goodsArr").getAsJsonArray();
        if (goodsArr != null && goodsArr.size() > 10) {
            resultObj.addProperty("result", "-10");
            resultObj.addProperty("message", "单次最多选择十个类别");
            return resultObj;
        }

        List<GoodsVO> goodsVOS = new ArrayList<>();
        List<GoodsVO> insertGoodsVOS = new ArrayList<>();
        List<GoodsVO> updateGoodsVOS = new ArrayList<>();

        StringBuilder skuBuilder = new StringBuilder();
        int saveType = jsonObject.get("saveType").getAsInt();//保存方式（0 分组保存  1 保存全部）
        //查询该活动下所有类别
        List<Long> delGoodsGCode = new ArrayList<>();
        if (saveType == 1) {
            delGoodsGCode = gcodeList;
        }
        if (goodsArr != null && !goodsArr.toString().isEmpty()) {
            for (int i = 0; i < goodsArr.size(); i++) {
                GoodsVO goodsVO = GsonUtils.jsonToJavaBean(goodsArr.get(i).toString(), GoodsVO.class);
                assert goodsVO != null;
                skuBuilder.append(goodsVO.getGcode()).append(",");
                if (saveType == 1) {
                    if (delGoodsGCode.contains(goodsVO.getGcode())) {
                        delGoodsGCode.remove(goodsVO.getGcode());
                    }
                }
                if ((goodsVO.getCstatus() & 512) == 0) {
                    goodsVO.setPrice(goodsVO.getPrice() * 100);
                }
                goodsVOS.add(goodsVO);
            }

            String prodName = classEnoughStock(goodsVOS, actCode);
            if (prodName != null) {
                resultObj.addProperty("result", "-3");
                resultObj.addProperty("message", prodName);
                return resultObj;
            }

            int re = updateAssByActcode(2, actCode);
            if (re < 0) {
                resultObj.addProperty("result", "-1");
                resultObj.addProperty("message", "关联类别失败！");
                return resultObj;
            }
            String skuStr = skuBuilder.toString().substring(0, skuBuilder.toString().length() - 1);
            Map<Long,ActStock> goodsMap = getAllGoods(skuStr, actCode);
            for (GoodsVO goodsVO : goodsVOS) {
                if (goodsMap != null) {
                    if (goodsMap.containsKey(goodsVO.getGcode())) {//数据库不存在该商品 新增 否则修改
                        goodsVO.setVcode(goodsMap.get(goodsVO.getGcode()).getVocode());
                        updateGoodsVOS.add(goodsVO);
                    } else {
                        insertGoodsVOS.add(goodsVO);
                    }
                } else {
                    insertGoodsVOS.add(goodsVO);
                }
            }
            relationAssDrug(insertGoodsVOS, updateGoodsVOS, delGoodsGCode, actCode);
            //通知notice
            List<GoodsVO> delGoods = new ArrayList<>();
            for (long sku :delGoodsGCode) {
                GoodsVO goodsVO1 = new GoodsVO();
                goodsVO1.setActcode(actCode + "");
                goodsVO1.setGcode(sku);
                delGoods.add(goodsVO1);
            }
            noticeGoodsUpd(2, goodsVOS,delGoods, rulecode, actCode);
        }
        resultObj.addProperty("result", "1");
        resultObj.addProperty("message", "关联类别成功");
        return resultObj;
    }




    /* *
     * @description 判断类别库存是否足够
     * @params
     * @return
     * @exception
     * @author 11842
     * @time  2019/5/17 13:53
     * @version 1.1.1
     **/
    private String classEnoughStock( List<GoodsVO> goodsVOS, long actCode) {

        Map<Long, ClassStock> class2Map = new HashMap<>();
        Map<Long, ClassStock> class4Map = new HashMap<>();
        Map<Long, ClassStock> class6Map = new HashMap<>();
        String classStr;
        StringBuilder class2No = new StringBuilder();
        StringBuilder class4No = new StringBuilder();
        StringBuilder class6No = new StringBuilder();

        for (GoodsVO goodsVO : goodsVOS) {
            long classno = goodsVO.getGcode();
            ClassStock classStock = new ClassStock(goodsVO.getActstock(), goodsVO.getCstatus());
            if ((classno + "").length() == 2) {
                class2Map.put(classno, classStock);
                class2No.append(classno).append(",");
            }
            if ((classno + "").length() == 4) {
                class4Map.put(classno, classStock);
                class4No.append(classno).append(",");
            }

            if ((classno + "").length() == 6) {
                class6Map.put(classno, classStock);
                class6No.append(classno).append(",");
            }
        }
        //判断最大类
        if (class2Map.size() > 0) {
            Map<Long, AllAvailableStock> stock2Map = new HashMap<>();//可售库存
            StringBuilder sql2Builder = new StringBuilder();
            sql2Builder.append(RelationGoodsSQL.SELECT_ClASS_GOODS);
            classStr = class2No.toString().substring(0, class2No.toString().length() - 1);
            sql2Builder.append(" GROUP BY classno2 HAVING classno2 in(").append(classStr).append(")");
            List<Object[]> query2Result = baseDao.queryNative(sql2Builder.toString(), actCode, actCode, actCode);
       /*     classno6,classno4,classno2,sku, store, " +
            " min(notused) as minnotused*/
            if (query2Result != null && query2Result.size()>0) {
                query2Result.forEach(obj -> {
                    int minunused = BigDecimal.valueOf(Double.parseDouble(String.valueOf(obj[5]))).intValue();
                    AllAvailableStock allAvailableStock = new AllAvailableStock((long)obj[3],(int)obj[4], minunused);
                    stock2Map.put(Long.parseLong(String.valueOf(obj[2])), allAvailableStock);
                });

                for (long classno : class2Map.keySet()) {
                    int actStock = class2Map.get(classno).getActStock();
                    if ((class2Map.get(classno).getCstatus() & 256) > 0) {
                        actStock = (int) Math.ceil(stock2Map.get(classno).getStore() * 0.01 * actStock);
                    }
                    int stock = stock2Map.get(classno).getAvailable() - actStock;

                    if (stock < 0) {
                        return  "【" + ProdInfoStore.getProdBySku(stock2Map.get(classno).getSku()).getProdname()+ "】库存不够，请重新设置活动库存！";
                    }
                }
            } else {
                return "该类别下无没有商品！";
            }
        }

        if (class4Map.size() > 0) {
            Map<Long, AllAvailableStock> stock4Map = new HashMap<>();//可售库存
            StringBuilder sql4Builder = new StringBuilder();
            sql4Builder.append(RelationGoodsSQL.SELECT_ClASS_GOODS);
            classStr = class4No.toString().substring(0, class4No.toString().length() - 1);
            sql4Builder.append(" GROUP BY classno4 HAVING classno4 in(").append(classStr).append(")");
            List<Object[]> query4Result = baseDao.queryNative(sql4Builder.toString(), actCode, actCode, actCode);
            if (query4Result != null && query4Result.size()>0) {
                query4Result.forEach(obj -> {
                    int minunused = BigDecimal.valueOf(Double.parseDouble(String.valueOf(obj[5]))).intValue();
                    AllAvailableStock allAvailableStock = new AllAvailableStock((long)obj[3],(int)obj[4], minunused);
                    stock4Map.put(Long.parseLong(String.valueOf(obj[1])), allAvailableStock);
                });

                for (long classno : class2Map.keySet()) {
                    int actStock = class2Map.get(classno).getActStock();
                    if ((class2Map.get(classno).getCstatus() & 256) > 0) {
                        actStock = (int) Math.ceil(stock4Map.get(classno).getStore() * 0.01 * actStock);
                    }
                    int stock = stock4Map.get(classno).getAvailable() - actStock;

                    if (stock < 0) {
                        return "【" + ProdInfoStore.getProdBySku(stock4Map.get(classno).getSku()).getProdname()+ "】库存不够，请重新设置活动库存！";
                    }
                }
            } else {
                return "该类别下无没有商品！";
            }
        }

        if (class6Map.size() > 0) {
            Map<Long, AllAvailableStock> stock6Map = new HashMap<>();//可售库存
            StringBuilder sql6Builder = new StringBuilder();
            sql6Builder.append(RelationGoodsSQL.SELECT_ClASS_GOODS);
            classStr = class6No.toString().substring(0, class6No.toString().length() - 1);
            sql6Builder.append(" GROUP BY classno6 HAVING classno6 in(").append(classStr).append(")");
            List<Object[]> query6Result = baseDao.queryNative(sql6Builder.toString(), actCode, actCode, actCode);
            if (query6Result != null && query6Result.size()>0) {
                query6Result.forEach(obj -> {
                    int minunused = BigDecimal.valueOf(Double.parseDouble(String.valueOf(obj[5]))).intValue();
                    AllAvailableStock allAvailableStock = new AllAvailableStock((long)obj[3],(int)obj[4], minunused);
                    stock6Map.put(Long.parseLong(String.valueOf(obj[0])), allAvailableStock);
                });

                for (long classno : class2Map.keySet()) {
                    int actStock = class2Map.get(classno).getActStock();
                    if ((class2Map.get(classno).getCstatus() & 256) > 0) {
                        actStock = (int) Math.ceil(stock6Map.get(classno).getStore() * 0.01 * actStock);
                    }
                    int stock = stock6Map.get(classno).getAvailable() - actStock;

                    if (stock < 0) {
                        return "【" + ProdInfoStore.getProdBySku(stock6Map.get(classno).getSku()).getProdname()+ "】库存不够，请重新设置活动库存！";
                    }
                }
            } else {
                return "该类别下无没有商品！";
            }
        }

        return null;
    }


    private String getActStockBySkus1(JsonObject jsonObject, long actCode) {
        List<GoodsVO> goodsVOList = new ArrayList<>();//活动库存(前端传过来的)
        Map<Long, AllAvailableStock> stockMap = new HashMap<>();//可售库存
        StringBuilder skuBuilder = new StringBuilder();
        JsonArray goodsArr = jsonObject.get("goodsArr").getAsJsonArray();
        if (goodsArr != null && goodsArr.size() > 100) {
            return "单次最多设置一百个商品！";
        }
        if (goodsArr != null && !goodsArr.toString().isEmpty()) {
            for (int i = 0; i < goodsArr.size(); i++) {
                GoodsVO goodsVO = GsonUtils.jsonToJavaBean(goodsArr.get(i).toString(), GoodsVO.class);
                assert goodsVO != null;
                skuBuilder.append(goodsVO.getGcode()).append(",");
                goodsVOList.add(goodsVO);
            }
            String skuStr = skuBuilder.toString().substring(0, skuBuilder.toString().length() - 1);
            String selectSQL = "select sku, systore,notused as minunused from (select sku,systore, sum(used) as uused, (systore-sum(used)) as notused from ( " +
                    "select sku,systore, used  from ( " +
                    "SELECT sku,gcode,(store-freezestore) as systore, " +
                    "  ceil(IF( ua.cstatus & 256 > 0, sum( actstock ), 0 ) * 0.01 * ( store - freezestore ) + IF " +
                    "  ( ua.cstatus & 256 = 0, sum( actstock ), 0 ) ) AS used FROM   " +
                    "  (SELECT " +
                    "  spu,sku,gcode,store,freezestore,actstock,a.cstatus  " +
                    "FROM  {{?" + DSMConst.TD_PROM_ASSDRUG+"}} a " +
                    "  LEFT JOIN td_prod_sku s ON s.sku LIKE CONCAT( '_', a.gcode, '%' )  " +
                    "WHERE  a.cstatus & 1 = 0 AND length( gcode ) < 14 AND gcode > 0  " +
                    "  AND actcode IN ( SELECT unqid FROM td_prom_act WHERE cstatus & 1 = 0 ) and actcode<>"
                    + actCode+" UNION ALL " +
                    "SELECT  spu,  sku,gcode,  store,  freezestore,actstock,  a.cstatus  " +
                    "FROM td_prom_assdrug a LEFT JOIN td_prod_sku s ON s.sku = gcode  " +
                    "WHERE a.cstatus & 1 = 0  AND length( gcode ) = 14  " +
                    "  AND actcode IN ( SELECT unqid FROM td_prom_act WHERE cstatus & 1 = 0 ) and actcode<>"
                    + actCode+" UNION ALL " +
                    "SELECT  spu,  sku,  gcode,  store,  freezestore,  actstock,  a.cstatus  " +
                    "FROM  td_prom_assdrug a,  td_prod_sku s  " +
                    "WHERE  a.cstatus & 1 = 0   AND gcode = 0  " + " and actcode<>" + actCode +
                    "  AND actcode IN ( SELECT unqid FROM td_prom_act WHERE cstatus & 1 = 0 ) " +
                    "  ) ua " +
                    "WHERE  ua.cstatus & 1 = 0  " +
                    "GROUP BY  sku , ua.cstatus ) ub UNION ALL  " +
                    "select sku,store, 0 from td_prod_sku where cstatus&1=0) uc GROUP BY sku) ud where "
                    + " sku in (" + skuStr + ")";
            List<Object[]> queryResult = baseDao.queryNative(selectSQL);
            if (queryResult != null && queryResult.size()>0) {
                queryResult.forEach(obj -> {
                    int minunused = BigDecimal.valueOf(Double.parseDouble(String.valueOf(obj[2]))).intValue();
                    AllAvailableStock allAvailableStock = new AllAvailableStock((long)obj[0], (int)obj[1], minunused);
                    stockMap.put((long)obj[0], allAvailableStock);
                });
            }
            for (GoodsVO goodsVO : goodsVOList) {
                int actStock = goodsVO.getActstock();
                if ((goodsVO.getCstatus() & 256) > 0) {
                    actStock = (int) Math.ceil(stockMap.get(goodsVO.getGcode()).getStore() * 0.01 * actStock);
                }
                int stock = stockMap.get(goodsVO.getGcode()).getAvailable() - actStock;

                if (stock < 0) {
                    return ProdInfoStore.getProdBySku(goodsVO.getGcode()).getProdname();
                }
            }
        }
        return null;
    }

    /* *
     * @description 获取商品可售库存
     * @params [skuStr, stockMap]
     * @return void
     * @exception
     * @author 11842
     * @time  2019/5/14 11:40
     * @version 1.1.1
     **/
    private void getAllGoodsBySku(String skuStr, Map<Long, Integer> stockMap) {
        String selectSQL = "select sku,store from {{?" + DSMConst.TD_PROD_SKU + "}} where cstatus&1=0 "
                + " and sku in(" + skuStr + ")";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        if (queryResult == null || queryResult.isEmpty()) return ;
        queryResult.forEach(obj -> {
            long sku = (long)obj[0];
            int  store = (int)obj[1];
            stockMap.put(sku, store);
        });
    }


    /* *
     * @description 判断活动是否正在进行
     * @params [actCode]
     * @return boolean
     * @exception
     * @author 11842
     * @time  2019/5/6 10:31
     * @version 1.1.1
     **/
    private boolean theActInProgress(long actCode) {
        String selectTypeSQL = "select acttype,actcycle,brulecode from {{?" + DSMConst.TD_PROM_ACT + "}} "
                + " where cstatus&1=0 and unqid=" + actCode;
        List<Object[]> qTResult = baseDao.queryNative(selectTypeSQL);
        int acttype = (int)qTResult.get(0)[0];
        long actcycle = (long)qTResult.get(0)[1];
        int brulecode = (int) qTResult.get(0)[2];
        String selectSQL = "";
        if(brulecode == 1133){
            selectSQL = "select count(*) from {{?" + DSMConst.TD_PROM_ACT + "}} a "
                    + "where a.cstatus&1=0 and a.unqid=" + actCode + " and a.sdate<=CURRENT_DATE"
                    + " and a.edate>=CURRENT_DATE";
        }else{
            selectSQL = "select count(*) from {{?" + DSMConst.TD_PROM_ACT + "}} a "
                    + "left join {{?" + DSMConst.TD_PROM_TIME + "}} t on a.unqid=t.actcode and t.cstatus&1=0 "
                    + "where a.cstatus&1=0 and a.unqid=" + actCode + " and a.sdate<=CURRENT_DATE"
                    + " and a.edate>=CURRENT_DATE and t.sdate<=CURRENT_TIME and t.edate>=CURRENT_TIME";
        }
        LocalDateTime localDateTime = LocalDateTime.now();
        if (acttype == 1) {//每周
           int dayOfWeek = localDateTime.getDayOfWeek().getValue();
           int valW = BigDecimal.valueOf(Math.pow(2,dayOfWeek-1)).intValue();
           if (((int)actcycle & valW) == 0) {
               return false;
           }
//            selectSQL = "select count(*) from {{?" + DSMConst.TD_PROM_ACT + "}} a "
//                    + "left join {{?" + DSMConst.TD_PROM_TIME + "}} t on a.unqid=t.actcode and t.cstatus&1=0 "
//                    + "where a.cstatus&1=0 and a.unqid=" + actCode
//                    + " and t.sdate<=CURRENT_TIME and t.edate>=CURRENT_TIME";
        } else if (acttype == 2) {//每月
            int dayOfMouth = localDateTime.getDayOfMonth();
            int valM = BigDecimal.valueOf(Math.pow(2,dayOfMouth-1)).intValue();
            if (((int)actcycle & valM) == 0) {
                return false;
            }
//            selectSQL = "select count(*) from {{?" + DSMConst.TD_PROM_ACT + "}} a "
//                    + "left join {{?" + DSMConst.TD_PROM_TIME + "}} t on a.unqid=t.actcode and t.cstatus&1=0 "
//                    + "where a.cstatus&1=0 and a.unqid=" + actCode
//                    + " and t.sdate<=CURRENT_TIME and t.edate>=CURRENT_TIME";
        } else if (acttype == 3) {//每年
            String actcycleStr = String.valueOf(actcycle);
            String actDate =TimeUtils.getCurrentYear() + "-" + actcycleStr.substring(2,4)
                    + "-" +  actcycleStr.substring(4,6);
            if(brulecode == 1133){
                selectSQL = "select count(*) from {{?" + DSMConst.TD_PROM_ACT + "}} a "
                        + "where a.cstatus&1=0 and a.unqid=" + actCode +  " and CURRENT_DATE='" + actDate + "'";
            }else{
                selectSQL = "select count(*) from {{?" + DSMConst.TD_PROM_ACT + "}} a "
                        + "left join {{?" + DSMConst.TD_PROM_TIME + "}} t on a.unqid=t.actcode and t.cstatus&1=0 "
                        + "where a.cstatus&1=0 and a.unqid=" + actCode + " and CURRENT_DATE='" + actDate
                        + "' and t.sdate<=CURRENT_TIME and t.edate>=CURRENT_TIME";
            }
        }
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        return (long)queryResult.get(0)[0] > 0;
    }

    public static void main(String[] args) {
        LocalDateTime localDateTime = LocalDateTime.now();
        System.out.println(localDateTime.getDayOfMonth());
    }


    /* *
     * @description 通知前台修改商品价格
     * @params []
     * @return void
     * @exception
     * @author 11842
     * @time  2019/4/15 15:20
     * @version 1.1.1
     **/
    private void noticeGoodsUpd(int type, List<GoodsVO> goodsVOS, List<GoodsVO> delGoods, int rulecode, long actCode) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            ActivityManageServer activityManageServer = new ActivityManageServer();
            activityManageServer.registerObserver(new ProdDiscountObserver());
            activityManageServer.registerObserver(new ProdCurrentActPriceObserver());
            List<String> proList = new ArrayList<>();
            if (type == 1) {//全部商品
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("discount", 1);
                jsonObject.put("gcode", 0);
                jsonObject.put("cstatus", goodsVOS.get(0).getCstatus());
                jsonObject.put("limitnum", goodsVOS.get(0).getLimitnum());
                jsonObject.put("rulecode", rulecode);
                jsonObject.put("stock", goodsVOS.get(0).getActstock());
                jsonObject.put("actcode", actCode);
                proList.add(jsonObject.toJSONString());
            } else {
                if (goodsVOS != null && goodsVOS.size() > 0) {
                    for (GoodsVO goodsVO : goodsVOS) {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("discount", 1);
                        jsonObject.put("gcode", goodsVO.getGcode());
                        jsonObject.put("cstatus", goodsVO.getCstatus());
                        jsonObject.put("rulecode", rulecode);
                        jsonObject.put("actcode", actCode);
                        jsonObject.put("stock", goodsVO.getActstock());
                        jsonObject.put("limitnum", goodsVO.getLimitnum());
                        proList.add(jsonObject.toJSONString());
                    }
                }
            }
            if (delGoods!=null && delGoods.size() > 0) {
                //LogUtil.getDefaultLogger().info("####### 1259 line: delGoods-->"+delGoods);
                for (GoodsVO goodsVO : delGoods) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("discount", 1);
                    jsonObject.put("gcode", goodsVO.getGcode());
                    jsonObject.put("cstatus", 1);
                    jsonObject.put("rulecode", rulecode);
                    jsonObject.put("actcode", actCode);
                    jsonObject.put("stock", 0);
                    jsonObject.put("limitnum", 0);
                    proList.add(jsonObject.toJSONString());
                }
            }
            activityManageServer.setProd(proList);
        });
    }

    /* *
     * @description 通知活动修改
     * @params []
     * @return void
     * @exception
     * @author jiangwenguang
     * @time  2019/4/19 1:50
     * @version 1.1.1
     **/
    private void noticeActUpdate(long actcode) {
        ActivityManageServer activityManageServer = new ActivityManageServer();
        activityManageServer.registerObserver(new ProdCurrentActPriceObserver());
        activityManageServer.actUpdate(actcode);
    }

    private List<Long> selectGoodsByAct(long actCode) {
        List<Long> gcodeList = new ArrayList<>();
        String sql = "select gcode from {{?" + DSMConst.TD_PROM_ASSDRUG + "}} where cstatus&1=0 and "
                + " actcode=" + actCode;
        List<Object[]> queryResult = baseDao.queryNative(sql);
        if (queryResult==null || queryResult.isEmpty()) return gcodeList;
        for (int i = 0; i < queryResult.size(); i++) {
            gcodeList.add((long)queryResult.get(i)[0]);
        } 
        return gcodeList;
    }




    private int relationGoods(JsonObject jsonObject, long actCode, int rulecode, List<Long> gcodeList) {
        //关联活动商品
        JsonArray goodsArr = jsonObject.get("goodsArr").getAsJsonArray();
        int saveType = jsonObject.get("saveType").getAsInt();//保存方式（0 分组保存  1 保存全部）
        List<GoodsVO> goodsVOS = new ArrayList<>();
        List<GoodsVO> insertGoodsVOS = new ArrayList<>();
        List<GoodsVO> updateGoodsVOS = new ArrayList<>();
        StringBuilder skuBuilder = new StringBuilder();
        //查询该活动下所有商品
        List<Long> delGoodsGCode = new ArrayList<>();
        if (saveType == 1) {
            delGoodsGCode = gcodeList;
        }
        if (goodsArr != null && !goodsArr.toString().isEmpty()) {
            for (int i = 0; i < goodsArr.size(); i++) {
                GoodsVO goodsVO = GsonUtils.jsonToJavaBean(goodsArr.get(i).toString(), GoodsVO.class);
                if (goodsVO != null) {
                    skuBuilder.append(goodsVO.getGcode()).append(",");
                    if (saveType == 1) {
                        if (delGoodsGCode.contains(goodsVO.getGcode())) {
                            delGoodsGCode.remove(goodsVO.getGcode());
                        }
                    }
                    if ((goodsVO.getCstatus() & 512) == 0) {
                        goodsVO.setPrice(goodsVO.getPrice() * 100);
                    }
                }
                goodsVOS.add(goodsVO);
            }

            String skuStr = skuBuilder.toString().substring(0, skuBuilder.toString().length() - 1);
            Map<Long,ActStock> goodsMap = getAllGoods(skuStr, actCode);
            for (GoodsVO goodsVO : goodsVOS) {
                if (goodsMap != null) {
                    if (goodsMap.containsKey(goodsVO.getGcode())) {//数据库不存在该商品 新增 否则修改
                        updateGoodsVOS.add(goodsVO);
                    } else {
                        insertGoodsVOS.add(goodsVO);
                    }
                } else {
                    insertGoodsVOS.add(goodsVO);
                }
            }
            relationAssDrug(insertGoodsVOS, updateGoodsVOS,delGoodsGCode,actCode);
            //通知notice
            List<GoodsVO> delGoods = new ArrayList<>();
            for (long sku :delGoodsGCode) {
                GoodsVO goodsVO1 = new GoodsVO();
                goodsVO1.setActcode(actCode + "");
                goodsVO1.setGcode(sku);
                delGoods.add(goodsVO1);
            }
            noticeGoodsUpd(2, goodsVOS, delGoods, rulecode,actCode);
        }
        return 1;
    }
    
    /* *
     * @description 获取活动码下所有商品
     * @params [actCode]
     * @return void
     * @exception
     * @author 11842
     * @time  2019/4/30 17:48
     * @version 1.1.1
     **/
    private Set<Long> getAllGoodsByActCode(long actCode) {
        Set<Long> gcodeList = new HashSet<>();
        String sql = "select gcode from {{?" + DSMConst.TD_PROM_ASSDRUG + "}} where cstatus&1=0 and "
                + " actcode=" + actCode;
        List<Object[]> queryResult = baseDao.queryNative(sql);
        if (queryResult == null || queryResult.isEmpty()) return gcodeList;
        for (int i = 0; i < queryResult.size(); i++) {
            gcodeList.add((long)queryResult.get(i)[0]);
        }
        return gcodeList;
    }

    private Map<Long,ActStock> getAllGoods(String skuStr,long actcode) {
        Map<Long,ActStock> vcodeMap = new HashMap<>();
        String selectSQL = "select gcode,vcode,actstock from {{?" + DSMConst.TD_PROM_ASSDRUG + "}} where cstatus&1=0 "
                + " and gcode in(" + skuStr + ") and actcode=?";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL, actcode);
        if (queryResult == null || queryResult.isEmpty()) return null;
        queryResult.forEach(obj -> {
            ActStock actStock = new ActStock();
            actStock.setVocode((int)obj[1]);
            actStock.setActstock((int)obj[2]);
            vcodeMap.put((long)obj[0], actStock);
        });
        return vcodeMap;
    }

    /**
     * @接口摘要 更新活动状态
     * @业务场景 启用停用 删除活动
     * @传参类型 json
     * @传参列表 {actCode 活动码 cstatus 0 启用  32 停用  1 删除}
     * @返回列表 200 成功 -1 失败
     */
    @UserPermission(ignore = true)
    public Result updateActStatus(AppContext appContext){
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        Result result = new Result();
        long actcode = jsonObject.get("actcode").getAsLong();
        int cstatus = jsonObject.get("cstatus").getAsInt();
        if (theActInProgress(actcode)) {
            return result.fail("活动正在进行中，无法删除！");
        }
        int ret = 0;
        switch (cstatus){
            case 0:
                ret = baseDao.updateNative(OPEN_ACT,actcode);
                break;
            case 1:
                boolean b = delActCode(actcode);
                ret = b ? 1 : 0;
                break;
            case 32:
                ret = baseDao.updateNative(CLOSE_ACT,actcode);
                break;
        }
        if(ret > 0){
            ActivityManageServer activityManageServer = new ActivityManageServer();
            activityManageServer.registerObserver(new ProdCurrentActPriceObserver());

            activityManageServer.actUpdate(actcode);
        }
        return ret > 0 ? result.success("操作成功") : result.fail("操作失败");
    }


    private boolean delActCode(long actCode) {
        boolean b;
        List<Long> skus = selectGoodsByAct(actCode);
        List<Object[]> params = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder offerBuilder = new StringBuilder();
        String selectSQL = "select ladid,offercode from {{?" + DSMConst.TD_PROM_RELA + "}} a left join {{?"
                + DSMConst.TD_PROM_LADOFF +"}} b on a.ladid=b.unqid  where a.cstatus&1=0" +
                " and actcode=" + actCode;
        String delAssDrugByActCode = "update {{?" + DSMConst.TD_PROM_ASSDRUG + "}} set cstatus=cstatus|1 "
                + " where cstatus&1=0 and actcode=?";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        if (queryResult == null || queryResult.isEmpty()) {
            params.add(new Object[]{actCode});
            params.add(new Object[]{actCode});
            b = !ModelUtil.updateTransEmpty(baseDao.updateTransNative(new String[]{DELETE_ACT,
                    delAssDrugByActCode}, params));
            if (b) {
                List<GoodsVO> delGoods = new ArrayList<>();
                for (long sku :skus) {
                    GoodsVO goodsVO1 = new GoodsVO();
                    goodsVO1.setActcode(actCode + "");
                    goodsVO1.setGcode(sku);
                    delGoods.add(goodsVO1);
                }
                noticeGoodsUpd(-1, null, delGoods, 0, actCode);
            }
            return b;
        }
        for (Object[] aQueryResult : queryResult) {
            stringBuilder.append((long) aQueryResult[0]).append(",");
            offerBuilder.append((int) aQueryResult[1]).append(",");
        }
        String ladIdStr = stringBuilder.toString().substring(0, stringBuilder.toString().length() - 1);
        String offerStr = offerBuilder.toString().substring(0, offerBuilder.toString().length() - 1);
        String sqlOne = DEL_LAD_OFF_SQL + " and unqid in(" + ladIdStr + ")";
        params.add(new Object[]{});
        String sqlTwo = "update {{?" + DSMConst.TD_PROM_RELA + "}} set cstatus=cstatus|1 where cstatus&1=0 "
                + " and actcode=" + actCode;
        params.add(new Object[]{});
        String sqlThree = "update {{?" + DSMConst.TD_PROM_ASSGIFT + "}} set cstatus=cstatus|1 where cstatus&1=0 "
                + " and offercode in(" + offerStr +")";
        params.add(new Object[]{});
        params.add(new Object[]{actCode});
        params.add(new Object[]{actCode});
        b = !ModelUtil.updateTransEmpty(baseDao.updateTransNative(new String[]{sqlOne,sqlTwo, sqlThree,
                DELETE_ACT, delAssDrugByActCode}, params));
        if (b) {
            List<GoodsVO> delGoods = new ArrayList<>();
            for (long sku :skus) {
                GoodsVO goodsVO1 = new GoodsVO();
                goodsVO1.setActcode(actCode + "");
                goodsVO1.setGcode(sku);
                delGoods.add(goodsVO1);
            }
            noticeGoodsUpd(-1, null, delGoods, 0, actCode);
        }
        return b;
    }

    class AllAvailableStock {
        private long sku;
        private int store;
        private int available;

        public AllAvailableStock(long sku, int store, int available) {
            this.sku = sku;
            this.store = store;
            this.available = available;
        }

        public int getStore() {
            return store;
        }

        public void setStore(int store) {
            this.store = store;
        }

        public int getAvailable() {
            return available;
        }

        public void setAvailable(int available) {
            this.available = available;
        }

        public long getSku() {
            return sku;
        }

        public void setSku(long sku) {
            this.sku = sku;
        }
    }


    class ClassStock {
        private int actStock;
        private int cstatus;

        public ClassStock(int actStock, int cstatus) {
            this.actStock = actStock;
            this.cstatus = cstatus;
        }

        public int getActStock() {
            return actStock;
        }

        public void setActStock(int actStock) {
            this.actStock = actStock;
        }

        public int getCstatus() {
            return cstatus;
        }

        public void setCstatus(int cstatus) {
            this.cstatus = cstatus;
        }
    }


    /**
     * @接口摘要 审批活动、优惠券
     * @业务场景 审批活动
     * @传参类型 json
     * @传参列表  {actcod：活动码/优惠券码,ctype: 1 活动 2 优惠券，cstatus 1 审核通过 3 审核不通过}
     * @返回列表 200 成功 -1 失败
     */
    @UserPermission(ignore = true)
    public Result approvalAct(AppContext appContext){
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        Result result = new Result();
        UserSession userSession = appContext.getUserSession();
        long roleid = userSession.roleCode;
        long actcode = jsonObject.get("actcode").getAsLong();
        int type = jsonObject.get("ctype").getAsInt();
        int cstatus = jsonObject.get("cstatus").getAsInt();


        String cussActSql = " UPDATE {{?" + DSMConst.TD_PROM_ACT + "}}" +
                " SET cstatus = cstatus | 2048,ckstatus = 2 WHERE unqid = ? ";


        String cussCoupSql = " UPDATE {{?" + DSMConst.TD_PROM_COUPON + "}}" +
                " SET cstatus = cstatus | 2048,ckstatus = 2 WHERE unqid = ? ";


        String failActSql = " UPDATE {{?" + DSMConst.TD_PROM_ACT + "}}" +
                " set ckstatus = 3 WHERE  unqid = ? ";


        String failCoupSql = " UPDATE {{?" + DSMConst.TD_PROM_COUPON + "}}" +
                " set ckstatus = 3 WHERE unqid = ? ";


        String  assDugSql = " select 1 " +
                " from {{?" + DSMConst.TD_PROM_ASSDRUG + "}}  where actcode=? and cstatus & 1 = 0";



        String exSqlAct = "";
        String exSqlCoup = "";

        if(cstatus == 1){
            if((roleid & 32) > 0){
                exSqlAct = cussActSql;
                exSqlCoup = cussCoupSql;
            }
            if((roleid & 128) > 0){
                exSqlAct = " UPDATE {{?" + DSMConst.TD_PROM_ACT + "}}" +
                        " SET ckstatus = 1 WHERE unqid = ? ";

                exSqlCoup = " UPDATE {{?" + DSMConst.TD_PROM_COUPON + "}}" +
                        " SET ckstatus = 1 WHERE unqid = ? ";
            }
        }else{
            exSqlAct = failActSql;
            exSqlCoup = failCoupSql;
        }

        List<Object[]> assRet = baseDao.queryNative(assDugSql, actcode);

        if(type == 1){
            if(assRet == null || assRet.isEmpty()){
                return new Result().fail("该活动没有关联商品，需关联商品后再提交审批！");
            }
        }
        int ret = 0;
        switch (type){
            case 1:
                ret = baseDao.updateNative(exSqlAct,actcode);
                break;
            case 2:
                ret = baseDao.updateNative(exSqlCoup,actcode);
                break;
        }

        return ret > 0 ? result.success("操作成功") : result.fail("操作失败");
    }




    /**
     * @接口摘要 停用活动（清空活动库存）
     * @业务场景 停用活动
     * @传参类型 json
     * @传参列表  {actcode :活动码}
     * @返回列表 200 成功 -1 失败
     */
    @UserPermission(ignore = true)
    public Result disableAct(AppContext appContext){
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String actcode = jsonObject.get("actcode").getAsString();
        //int ctype = jsonObject.get("ctype").getAsInt();

        String assDugSql = " select actcode,gcode,a.cstatus,actstock,limitnum,brulecode" +
                " from {{?" + DSMConst.TD_PROM_ASSDRUG + "}} a,{{?"+DSMConst.TD_PROM_ACT+"}} b "
                + "where a.actcode = b.unqid and actcode=? ";

        String updateActSql = " UPDATE {{?" + DSMConst.TD_PROM_ASSDRUG + "}}" +
                " SET actstock = 0  where actcode = ? ";

        String updateActStatusSql = " UPDATE {{?" + DSMConst.TD_PROM_ACT + "}}" +
                " SET ckstatus = ckstatus | 32  where unqid = ? ";


        //更新审批状态
//        String updateCkSql =
//                " UPDATE {{?" + DSMConst.TD_PROM_ACT + "}}"
//                        + " SET cstatus = cstatus & " + ~2080
//                        + " ckstatus = 0 WHERE unqid = ? ";

      //  if(ctype == 0){
            List<Object[]> assRet = baseDao.queryNative(assDugSql, actcode);
            if(assRet == null || assRet.isEmpty()){
                return new Result().fail("该活动没有关联商品，无需停用！");
            }

            List<GoodsVO> goodsVOList = new ArrayList<>();
            int brulecode = Integer.parseInt(assRet.get(0)[5].toString());
            for (Object[] objs : assRet){
                GoodsVO goodsVO = new GoodsVO();
                goodsVO.setActcode(objs[0].toString());
                goodsVO.setGcode(Long.parseLong(objs[1].toString()));
                goodsVO.setCstatus(Integer.parseInt(objs[2].toString()));
                goodsVO.setActstock(Integer.parseInt(objs[3].toString()));
                goodsVO.setLimitnum(Integer.parseInt(objs[4].toString()));
                goodsVOList.add(goodsVO);
            }

            long gcode = Long.parseLong(assRet.get(0)[0].toString());
            int type = 0;
            if(gcode == 0){
                type = 1;
            }
            try{
                noticeGoodsUpd(type, goodsVOList, goodsVOList,brulecode, Long.parseLong(actcode));
                List<Object[]> parm = new ArrayList<>();
                parm.add(new Object[]{actcode});
                parm.add(new Object[]{actcode});
                baseDao.updateTransNative(new String[]{updateActSql,updateActStatusSql},parm);
               // baseDao.updateNative(updateActSql,actcode);
            }catch (Exception e){
                LogUtil.getDefaultLogger().debug("停用更新商品活动库存缓存失败");
                return new Result().fail("操作失败！");
            }
            return new Result().success("操作成功");
//        }else{
//            int ret = baseDao.updateNative(updateCkSql, actcode);
//            if(ret > 0){
//                return new Result().success("操作成功");
//            }
//            return new Result().fail("操作失败！");
//        }
    }
}
