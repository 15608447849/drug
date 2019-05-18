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
import com.onek.discount.entity.*;
import com.onek.entitys.Result;
import com.onek.propagation.prod.ActivityManageServer;
import com.onek.propagation.prod.ProdCurrentActPriceObserver;
import com.onek.propagation.prod.ProdDiscountObserver;
import com.onek.util.IceRemoteUtil;
import com.onek.util.SmsTempNo;
import com.onek.util.SmsUtil;
import com.onek.util.prod.ProdInfoStore;
import com.onek.util.stock.RedisStockUtil;
import constant.DSMConst;
import dao.BaseDAO;
import com.onek.util.GenIdUtil;
import util.BUSUtil;
import util.GsonUtils;
import util.ModelUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.onek.discount.CommonModule.getLaderNo;

/**
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
            + "(unqid,assgiftno,offercode)"
            + " values(?,?,?)";

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
                    + " SET cstatus = cstatus | " + CSTATUS.DELETE
                    + " WHERE unqid = ? ";


    /* *
     * @description 活动查询
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/3 10:03
     * @version 1.1.1
     **/
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
                "actcycle,sdate,edate,a.brulecode,a.cstatus,rulename from {{?" + DSMConst.TD_PROM_ACT + "}} a "
                + " left join {{?" + DSMConst.TD_PROM_RULE +"}} b on a.brulecode=b.brulecode"
                + " where a.cstatus&1=0 ";
        sqlBuilder.append(selectSQL);
        sqlBuilder = getParamsDYSQL(sqlBuilder, jsonObject).append(" group by unqid desc");
        List<Object[]> queryList = baseDao.queryNative(pageHolder, page, sqlBuilder.toString());
        ActivityVO[] activityVOS = new ActivityVO[queryList.size()];
        baseDao.convToEntity(queryList, activityVOS, ActivityVO.class,new String[]{
                "unqid","actname","incpriority","cpriority",
                "qualcode","qualvalue","actdesc","excdiscount",
                "acttype","actcycle","sdate","edate","brulecode",
                "cstatus","ruleName"});
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
     * @description 活动操作
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/2 14:34
     * @version 1.1.1
     **/
    @UserPermission(ignore = true)
    public Result insertActivity(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        ActivityVO activityVO = GsonUtils.jsonToJavaBean(json, ActivityVO.class);
        if (activityVO == null) return result.fail("参数错误");
        int cpt = getCPCount(activityVO.getIncpriority(), activityVO.getCpriority());
        if (cpt == -1) return result.fail("优先级已超过");
        if (Long.parseLong(activityVO.getUnqid()) > 0) {//修改
            if (theActInProgress(Long.parseLong(activityVO.getUnqid()))) {
                return result.fail("活动正在进行中，无法修改！");
            }
            result =  updateActivity(activityVO, cpt);
        } else {//新增
            result = insertActivity(activityVO, cpt);
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            long startTime = dateFormat.parse(activityVO.getSdate()+
                    " " + activityVO.getTimeVOS().get(0).getSdate()).getTime();
            if (startTime > new Date().getTime()) {
                ExecutorService executors = Executors.newSingleThreadExecutor();
                executors.execute(() -> {
                            IceRemoteUtil.sendMessageToAllClient(SmsTempNo.ACTIVITIES_OF_NEW,
                                    "", "【" + activityVO.getActname() + "】将于" + activityVO.getSdate() +
                                            " " +activityVO.getTimeVOS().get(0).getSdate() + "开始进行");
                            SmsUtil.sendMsgToAllBySystemTemp(SmsTempNo.ACTIVITIES_OF_NEW,"", "【" + activityVO.getActname() + "】将于" + activityVO.getSdate() +
                                    " " +activityVO.getTimeVOS().get(0).getSdate() + "开始进行");
                        }

                );
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
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
                    assGiftParams.add(new Object[]{GenIdUtil.getUnqId(), ladderVOS.get(i).getAssgiftno(),offerCode[i]});
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
        String updateSql = "update {{?" + DSMConst.TD_PROM_ASSDRUG + "}} set actstock=?,limitnum=?, vcode=?, price=?, "
                + "cstatus=? where cstatus&1=0 and gcode=? and vcode=? and actcode=?";

        for (GoodsVO insGoodsVO : insertDrugVOS) {
            insertDrugParams.add(new Object[]{GenIdUtil.getUnqId(), actCode, insGoodsVO.getGcode(),
                    insGoodsVO.getMenucode(),insGoodsVO.getActstock(),insGoodsVO.getLimitnum(),
                    insGoodsVO.getPrice(), insGoodsVO.getCstatus()});
        }
        for (GoodsVO updateGoodsVO : updDrugVOS) {
            updateDrugParams.add(new Object[]{updateGoodsVO.getActstock(),updateGoodsVO.getLimitnum(),
                    updateGoodsVO.getVcode()+1, updateGoodsVO.getPrice(),updateGoodsVO.getCstatus(),
                    updateGoodsVO.getGcode(), updateGoodsVO.getVcode(),actCode});
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
     * @description 获取活动详情
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/2 19:24
     * @version 1.1.1
     **/
    @UserPermission(ignore = true)
    public Result getActDetail(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        long actCode = jsonObject.get("unqid").getAsLong();
        String selectSQL = "select a.unqid,actname,incpriority,cpriority," +
                "qualcode,qualvalue,actdesc,excdiscount,acttype," +
                "actcycle,sdate,edate,a.brulecode,a.cstatus,rulename from {{?" + DSMConst.TD_PROM_ACT + "}} a "
                + " left join {{?" + DSMConst.TD_PROM_RULE +"}} b on a.brulecode=b.brulecode"
                + " where a.cstatus&1=0 and a.unqid=" + actCode;
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        ActivityVO[] activityVOS = new ActivityVO[queryResult.size()];
        baseDao.convToEntity(queryResult, activityVOS, ActivityVO.class, new String[]{
                "unqid","actname","incpriority","cpriority",
                "qualcode","qualvalue","actdesc","excdiscount",
                "acttype","actcycle","sdate","edate","brulecode",
                "cstatus","ruleName"
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
        String selectSQL = "select a.unqid,ladamt,ladnum,b.offercode,offer,a.cstatus, assgiftno from {{?" + DSMConst.TD_PROM_RELA
                + "}} a left join {{?" + DSMConst.TD_PROM_LADOFF + "}} b on a.ladid=b.unqid left join {{?"
                + DSMConst.TD_PROM_ASSGIFT + "}} c on b.offercode=c.offercode and c.cstatus&1=0 where a.cstatus&1=0 "
                + " and a.actcode=" + actCode;
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        LadderVO[] ladderVOS = new LadderVO[queryResult.size()];
        baseDao.convToEntity(queryResult, ladderVOS, LadderVO.class);
        for (LadderVO ladderVO:ladderVOS) {
            ladderVO.setLadamt(ladderVO.getLadamt()/100);
            ladderVO.setOffer(ladderVO.getOffer()/100);
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

    private List<AssGiftVO> getAssGift(long actCode) {
        String sql = "select unqid,offerno,assgiftno,cstatus,giftname from {{?" + DSMConst.TD_PROM_ASSGIFT
                + "}} a left join {{?" + DSMConst.TD_PROM_GIFT + "}} g on a.assgiftno=g.unqid "
                + " where cstatus&1=0 and actcode=" + actCode;
        List<Object[]> queryResult = baseDao.queryNative(sql);
        AssGiftVO[] assGiftVOS = new AssGiftVO[queryResult.size()];
        baseDao.convToEntity(queryResult, assGiftVOS, AssGiftVO.class);
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
        if (gcodeList.size() == 0) {

        } else {

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
    private boolean theGoodsStockIsEnough(int actStock) {

/*        SELECT
                gcode,
                store,
                freezestore
        FROM
        td_prom_assdrug a
        LEFT JOIN td_prod_sku s ON a.gcode = s.sku
        WHERE
        a.cstatus & 1 = 0
        and gcode>0
        GROUP BY
        gcode
        HAVING ((store - freezestore ) - ceil (
                sum(IF ( a.cstatus & 256 > 0, actstock, 0 ) ) * 0.01*(store - freezestore ) + sum( IF ( a.cstatus & 256 = 0, actstock, 0 ) )
        )) < 0 LIMIT 0,1*/
        String selectSQLOne = "select sum(IF(cstatus & 256>0, actstock, 0)) * 0.01 as percent," +
                " sum(IF(cstatus & 256 = 0, actstock, 0 )) as total from {{?" + DSMConst.TD_PROM_ASSDRUG + "}} where cstatus&1=0";
        String selectAssSQL = "select cel(sum(if(a.cstatus&256>0),actstock,0))*0.01 + sum(if(a.cstatus&256=0),actstock,0))) from {{?"
                + DSMConst.TD_PROM_ASSDRUG + "}} a left join {{?"
                + DSMConst.TD_PROD_SKU +"}} s on a.gcode=s.sku  where a.cstatus&1=0 and gcode>0 group by gcode";
        String selectSQL = "select sku from {{?" + DSMConst.TD_PROD_SKU+ "}} where (store-freezestore)<" +
                actStock +" LIMIT 0,1";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        return queryResult == null || queryResult.size() == 0;
    }

    /**
     * @description 关联商品
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/6 10:09
     * @version 1.1.1
     **/
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
        if (theActInProgress(actCode)) {
            return result.fail("活动正在进行中，无法修改！");
        }
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
                code = relationClasses(jsonObject, actCode, ruleCode);
                if ((code+ "").length() == 14) {
                    return result.fail("【" + ProdInfoStore.getProdBySku(code).getProdname() + "】库存不够，请重新设置活动库存！");
                }
                if (code == 0){
                    return result.success("关联类别成功");
                }
                return result.fail("关联类别失败");
            default://商品关联
                List<Long> gcodeList = selectGoodsByAct(actCode);
                boolean isAdd = gcodeList.size() == 0;
                String skuStr = getActStockBySkus(jsonObject, actCode, isAdd);
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
        switch (type) {
            case 1:
                updateSQL = updateSQL + " and gcode<>0";
                break;
            case 2:
                updateSQL = updateSQL + " and (gcode=0 or LENGTH(gcode)=14)";
                break;
            default:
                updateSQL = updateSQL + " and (gcode=0 or LENGTH(gcode)<14)";
                break;
        }
        return baseDao.updateNative(updateSQL, actCode);
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
    private long relationClasses(JsonObject jsonObject,long actCode, int rulecode) {
        long result;
        List<Long> gcodeList = selectGoodsByAct(actCode);
        boolean isAdd = gcodeList.size() == 0;
        //关联活动商品
        JsonArray goodsArr = jsonObject.get("goodsArr").getAsJsonArray();
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
                result = classEnoughStock(goodsVO.getGcode(), goodsVO.getCstatus(),goodsVO.getActstock(), actCode, isAdd);
                if (result > 0) {
                    return result;
                }
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

            int re = updateAssByActcode(2, actCode);
            if (re < 0) {
                return -1;
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
        return 1;
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
    private long classEnoughStock(long sku, int cstatus, int actstock, long actCode, boolean isAdd) {
        long code;
        if (isAdd) {

        } else {

        }
        return 0;
    }


    private String getActStockBySkus(JsonObject jsonObject, long actCode, boolean isAdd) {
        String prodNames;
        JsonArray goodsArr = jsonObject.get("goodsArr").getAsJsonArray();
        long result;
        if (goodsArr != null && !goodsArr.toString().isEmpty()) {
            for (int i = 0; i < goodsArr.size(); i++) {
                GoodsVO goodsVO = GsonUtils.jsonToJavaBean(goodsArr.get(i).toString(), GoodsVO.class);
                assert goodsVO != null;
                result = classEnoughStock(goodsVO.getGcode(), goodsVO.getCstatus(),goodsVO.getActstock(), actCode, isAdd);
                if (result > 0) {
                    prodNames = ProdInfoStore.getProdBySku(goodsVO.getGcode()).getProdname();
                    return prodNames;
                }
            }
        }
        return null;
    }


    private String getActStockBySkus1(JsonObject jsonObject) {
        String prodNames=null;
        Map<Long, Integer> actStockMap = new HashMap<>();//活动库存
        Map<Long, Integer> stockMap = new HashMap<>();//可售库存
        StringBuilder skuBuilder = new StringBuilder();
        JsonArray goodsArr = jsonObject.get("goodsArr").getAsJsonArray();
        if (goodsArr != null && !goodsArr.toString().isEmpty()) {
            for (int i = 0; i < goodsArr.size(); i++) {
                GoodsVO goodsVO = GsonUtils.jsonToJavaBean(goodsArr.get(i).toString(), GoodsVO.class);
                assert goodsVO != null;
                skuBuilder.append(goodsVO.getGcode()).append(",");
                actStockMap.put(goodsVO.getGcode(), goodsVO.getActstock());
            }
            String skuStr = skuBuilder.toString().substring(0, skuBuilder.toString().length() - 1);
            getAllGoodsBySku(skuStr, stockMap);
            String selectSQL = "select gcode,actstock from {{?" + DSMConst.TD_PROM_ASSDRUG + "}} where cstatus&1=0 "
                    + " and gcode in(" + skuStr + ")";
            List<Object[]> queryResult = baseDao.queryNative(selectSQL);
            if (queryResult != null && queryResult.size()>0) {
                queryResult.forEach(obj -> {
                    long sku = (long)obj[0];
                    int  actStock = (int)obj[1];
                    if (actStockMap.containsKey(sku)) {
                        actStock = actStockMap.get(sku) + actStock;
                        actStockMap.put(sku, actStock);
                    }
                });
            }
            StringBuilder prodNameSB = new StringBuilder();
            for (Long gcode : actStockMap.keySet()) {
                int actStock = actStockMap.get(gcode);
                if (stockMap.containsKey(gcode)) {
                    int stock = stockMap.get(gcode) - actStock;
                    if (stock < 0) {
                        prodNames = prodNameSB.append("【").append(ProdInfoStore.getProdBySku(gcode).getProdname())
                                .append("】,").toString();
                    }
                } else {

                }
            }
        }
        return prodNames;
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
        String selectSQL = "select count(*) from {{?" + DSMConst.TD_PROM_ACT + "}} a "
                + "left join {{?" + DSMConst.TD_PROM_TIME + "}} t on a.unqid=t.actcode and t.cstatus&1=0 "
                + "where a.cstatus&1=0 and a.unqid=" + actCode + " and a.sdate<=CURRENT_DATE"
                + " and a.edate>=CURRENT_DATE and t.sdate<=CURRENT_TIME and t.edate>=CURRENT_TIME";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        return (long)queryResult.get(0)[0] > 0;
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
                    int stock = RedisStockUtil.getActStockBySkuAndActno(goodsVO.getGcode(), actCode);
                    int orgStock = RedisStockUtil.getActInitStock(goodsVO.getGcode(), actCode);
                    if (stock != orgStock) {
                        return -1;
                    }
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
     * 更新活动状态
     * @param appContext 0 启用  32 停用  1 删除
     * @return
     */
    @UserPermission(ignore = true)
    public Result updateActStatus(AppContext appContext){
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        Result result = new Result();
        long actcode = jsonObject.get("actcode").getAsLong();
        int cstatus = jsonObject.get("cstatus").getAsInt();
//        if (theActInProgress(actcode)) {
//            return result.fail("活动正在进行中，无法删除！");
//        }
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
            return !ModelUtil.updateTransEmpty(baseDao.updateTransNative(new String[]{DELETE_ACT,
                    delAssDrugByActCode}, params));
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
        boolean b = !ModelUtil.updateTransEmpty(baseDao.updateTransNative(new String[]{sqlOne,sqlTwo, sqlThree,
                DELETE_ACT, delAssDrugByActCode}, params));
        if (b) {
            List<GoodsVO> delGoods = new ArrayList<>();
            List<Long> skus = selectGoodsByAct(actCode);
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



    class ActStock {
        private int vocode;
        private int actstock;

        public int getVocode() {
            return vocode;
        }

        public void setVocode(int vocode) {
            this.vocode = vocode;
        }

        public int getActstock() {
            return actstock;
        }

        public void setActstock(int actstock) {
            this.actstock = actstock;
        }
    }

}
