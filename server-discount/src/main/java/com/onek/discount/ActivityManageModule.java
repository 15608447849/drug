package com.onek.discount;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.context.AppContext;
import com.onek.discount.entity.*;
import com.onek.entitys.Result;
import constant.DSMConst;
import dao.BaseDAO;
import global.GenIdUtil;
import util.BUSUtil;
import util.GsonUtils;
import util.ModelUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author 11842
 * @version 1.1.1
 * @description 活动管理
 * @time 2019/4/1 11:19
 **/
public class ActivityManageModule {

    private static BaseDAO baseDao = BaseDAO.getBaseDAO();

    private static final String SELECT_ACT_SQL = "select unqid,actname,incpriority,cpriority," +
                "qualcode,qualvalue,actdesc,excdiscount,acttype," +
                "actcycle,sdate,edate,rulecode,cstatus from {{?" + DSMConst.TD_PROM_ACT + "}} "
                 + " where cstatus&1=0 ";

    //新增活动
    private static final String INSERT_ACT_SQL = "insert into {{?" + DSMConst.TD_PROM_ACT + "}} "
            + "(unqid,actname,incpriority,cpriority,qualcode,qualvalue,actdesc,"
            + "excdiscount,acttype,actcycle,sdate,edate,rulecode) "
            + "values(?,?,?,?,?,"
            + "?,?,?,?,?,?,?,?)";

    //修改活动
    private static final String UPD_ACT_SQL = "update {{?" + DSMConst.TD_PROM_ACT + "}} set actname=?,"
            + "incpriority=?,cpriority=?,qualcode=?,qualvalue=?,actdesc=?,"
            + "excdiscount=?,acttype=?,actcycle=?,sdate=?,edate=?,rulecode=? where cstatus&1=0 "
            + " and unqid=?";

    //新增场次
    private static final String INSERT_TIME_SQL = "insert into {{?" + DSMConst.TD_PROM_TIME + "}} "
            + "(unqid,actcode,sdate,edate) "
            + " values(?,?,?,?)";

    private static final String DEL_TIME_SQL = "update {{?" + DSMConst.TD_PROM_TIME + "}} set cstatus=cstatus|1 "
            + " where cstatus&1=0 and actcode=?";

    //新增活动商品
    private static final String INSERT_ASS_DRUG_SQL = "insert into {{?" + DSMConst.TD_PROM_ASSDRUG + "}} "
            + "(unqid,actcode,gcode,menucode,actstock) "
            + " values(?,?,?,?,?)";

    private static final String DEL_ASS_DRUG_SQL = "update {{?" + DSMConst.TD_PROM_ASSDRUG + "}} set cstatus=cstatus|1 "
            + " where cstatus&1=0 and actcode=?";

    //优惠阶梯
    private static final String INSERT_LAD_OFF_SQL = "insert into {{?" + DSMConst.TD_PROM_LADOFF + "}} "
            + "(unqid,actcode,ruleno,ladamt,ladnum,offer) "
            + " values(?,?,?,?,?,?)";

    private static final String DEL_LAD_OFF__SQL = "update {{?" + DSMConst.TD_PROM_LADOFF + "}} set cstatus=cstatus|1 "
            + " where cstatus&1=0 and actcode=?";

    //优惠赠换商品
    private static final String INSERT_ASS_GIFT_SQL = "insert into {{?" + DSMConst.TD_PROM_ASSGIFT + "}} "
            + "(unqid,actcode,assgiftno)"
            + " values(?,?,?)";

    private static final String DEL_ASS_GIFT_SQL = "update {{?" + DSMConst.TD_PROM_ASSGIFT + "}} set cstatus=cstatus|1 "
            + " where cstatus&1=0 and actcode=?";

    private static final String UPDATE_ACT_CP = "update {{?" + DSMConst.TD_PROM_ACT + "}} set cpriority=? "
            + " where cstatus&1=0 and incpriority=? and cpriority=?";



    /* *
     * @description 活动查询
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/3 10:03
     * @version 1.1.1
     **/
    public Result queryActivities(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int ruleCode = jsonObject.get("rulecode").getAsInt();
        String actName = jsonObject.get("actname").getAsString();
        
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
    public Result insertActivity(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        ActivityVO activityVO = GsonUtils.jsonToJavaBean(json, ActivityVO.class);
        if (activityVO == null) return result.fail("参数错误");
        int cpt = getCPCount(activityVO.getIncpriority(), activityVO.getCpriority());
        if (cpt == -1) return result.fail("优先级已超过");
        if (activityVO.getUnqid() > 0) {//修改
            return insertActivity(activityVO, cpt);
        } else {//新增

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
        Result result = new Result();
        long actCode = GenIdUtil.getUnqId();//唯一码(活动码)
        //新增活动
        if (cpt == activityVO.getCpriority()) {
           int re = baseDao.updateNative(INSERT_ACT_SQL,actCode, activityVO.getActname(),
                   activityVO.getIncpriority(), activityVO.getCpriority(), activityVO.getQualcode(), activityVO.getQualvalue(), activityVO.getActdesc(),
                   activityVO.getExcdiscount(), activityVO.getActtype(), activityVO.getActcycle(), activityVO.getSdate(),
                   activityVO.getEdate(), activityVO.getRulecode());
           b = re > 0;
        } else {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[]{cpt, activityVO.getIncpriority(), activityVO.getCpriority()});
            params.add(new Object[]{actCode, activityVO.getActname(),
                    activityVO.getIncpriority(), activityVO.getCpriority(), activityVO.getQualcode(), activityVO.getQualvalue(), activityVO.getActdesc(),
                    activityVO.getExcdiscount(), activityVO.getActtype(), activityVO.getActcycle(), activityVO.getSdate(),
                    activityVO.getEdate(), activityVO.getRulecode()});
            int[] actResult = baseDao.updateTransNative(new String[]{UPDATE_ACT_CP, INSERT_ACT_SQL}, params);
            b = !ModelUtil.updateTransEmpty(actResult);
        }
        if (b) {
            //新增活动场次
            if (activityVO.getTimeVOS() != null && !activityVO.getTimeVOS().isEmpty()) {
                insertTimes(activityVO.getTimeVOS(), actCode);
            }
            //新增阶梯
            if (activityVO.getLadderVOS() != null && !activityVO.getLadderVOS().isEmpty()) {
                insertLadOff(activityVO.getLadderVOS(), actCode);
            }
            //新增优惠赠换商品
            if (activityVO.getAssGiftVOS() != null && !activityVO.getAssGiftVOS().isEmpty()) {
                insertAssGift(activityVO.getAssGiftVOS(), actCode);
            }
            //新增活动商品
            if (activityVO.getAssDrugVOS() != null && !activityVO.getAssDrugVOS().isEmpty()) {
                insertAssDrug(activityVO.getAssDrugVOS());
            }
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
        long actCode = activityVO.getUnqid();
        //新增活动
        if (cpt == activityVO.getCpriority()) {
            int re = baseDao.updateNative(UPD_ACT_SQL, activityVO.getActname(),
                    activityVO.getIncpriority(), activityVO.getCpriority(), activityVO.getQualcode(), activityVO.getQualvalue(), activityVO.getActdesc(),
                    activityVO.getExcdiscount(), activityVO.getActtype(), activityVO.getActcycle(), activityVO.getSdate(),
                    activityVO.getEdate(), activityVO.getRulecode(),actCode);
            b = re > 0;
        } else {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[]{cpt, activityVO.getIncpriority(), activityVO.getCpriority()});
            params.add(new Object[]{activityVO.getActname(),
                    activityVO.getIncpriority(), activityVO.getCpriority(), activityVO.getQualcode(), activityVO.getQualvalue(), activityVO.getActdesc(),
                    activityVO.getExcdiscount(), activityVO.getActtype(), activityVO.getActcycle(), activityVO.getSdate(),
                    activityVO.getEdate(), activityVO.getRulecode(),actCode});
            int[] actResult = baseDao.updateTransNative(new String[]{UPD_ACT_SQL, INSERT_ACT_SQL}, params);
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
            if (activityVO.getLadderVOS() != null && !activityVO.getLadderVOS().isEmpty()) {
                if (baseDao.updateNative(DEL_LAD_OFF__SQL, actCode) > 0) {
                    insertLadOff(activityVO.getLadderVOS(), actCode);
                }
            }
            //新增优惠赠换商品
            if (activityVO.getAssGiftVOS() != null && !activityVO.getAssGiftVOS().isEmpty()) {
                if (baseDao.updateNative(DEL_ASS_GIFT_SQL,actCode) > 0) {
                    insertAssGift(activityVO.getAssGiftVOS(), actCode);
                }
            }
            //新增活动商品
            if (activityVO.getAssDrugVOS() != null && !activityVO.getAssDrugVOS().isEmpty()) {
                if (baseDao.updateNative(DEL_ASS_DRUG_SQL,actCode) > 0) {
                    insertAssDrug(activityVO.getAssDrugVOS());
                }
            }
        } else {
            result.fail("修改失败");
        }

        return result.success("修改成功");
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
    private void insertLadOff(List<LadderVO> ladderVOS, long actCode) {
        List<Object[]> ladOffParams = new ArrayList<>();
        for (LadderVO ladderVO : ladderVOS) {
            ladOffParams.add(new Object[]{GenIdUtil.getUnqId(), actCode,ladderVO.getRuleno(),
                    ladderVO.getLadamt(),ladderVO.getLadnum(),ladderVO.getOffer()});
        }
        int[] result = baseDao.updateBatchNative(INSERT_LAD_OFF_SQL, ladOffParams, ladderVOS.size());
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
     * @description 新增活动商品
     * @params [goodsVOS]
     * @return void
     * @exception
     * @author 11842
     * @time  2019/4/2 16:23
     * @version 1.1.1
     **/
    private void insertAssDrug(List<GoodsVO> assDrugVOS) {
        List<Object[]> assDrugParams = new ArrayList<>();
        for (GoodsVO assDrugVO : assDrugVOS) {
            assDrugParams.add(new Object[]{GenIdUtil.getUnqId(), assDrugVO.getActcode(),assDrugVO.getGcode(),
                    assDrugVO.getMenucode(),assDrugVO.getActstock()});
        }
        int[] result = baseDao.updateBatchNative(INSERT_ASS_DRUG_SQL, assDrugParams, assDrugVOS.size());
        boolean b = !ModelUtil.updateTransEmpty(result);
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
        if (queryResult.size() > 9) {
            return -1;//优先级超出
        }
        int[] points = new int[queryResult.size()];
        for (int i = 0; i < queryResult.size(); i++) {
            points[i] = (int) queryResult.get(i)[0];
        }
        if (valueExit(points, cpriority)) {
            return BUSUtil.getBreak(points, 0, 9);
        }
        return cpriority;
    }

    private boolean valueExit(int[] arr, int value) {
        for (int anArr : arr) {
            return anArr == value;
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
    public Result getActDetail(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int actCode = jsonObject.get("unqid").getAsInt();
        String selectSQL = "select unqid,actname,incpriority,cpriority," +
                "qualcode,qualvalue,actdesc,excdiscount,acttype," +
                "actcycle,sdate,edate,rulecode,cstatus,rulename from {{?" + DSMConst.TD_PROM_ACT + "}} a "
                + " left join {{?" + DSMConst.TD_PROM_RULE +"}} b on a.rulecode=b.rulecode"
                + " where cstatus&1=0 and unqid=" + actCode;
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        ActivityVO[] activityVOS = new ActivityVO[queryResult.size()];
        baseDao.convToEntity(queryResult, activityVOS, ActivityVO.class, new String[]{
                "unqid","actname","incpriority","cpriority",
                "qualcode","qualvalue","actdesc","excdiscount",
                "acttype","actcycle","sdate","edate","rulecode",
                "cstatus","ruleName"
        });
        activityVOS[0].setTimeVOS(getTimes(actCode));
        activityVOS[0].setLadderVOS(getLadder(actCode));
        activityVOS[0].setAssGiftVOS(getAssGift(actCode));
        activityVOS[0].setAssDrugVOS(new CouponManageModule().getCoupGoods(actCode));
        return result;
    }

    /* *
     * @description 根据活动码获取商品
     * @params []
     * @return java.util.List<com.onek.discount.entity.AssDrugVO>
     * @exception
     * @author 11842
     * @time  2019/4/2 19:40
     * @version 1.1.1
     **/
//    private List<AssDrugVO> getAssDrugByCode(int actCode) {
//        String sql = "select unqid,actcode,gcode,menucode,actstock,cstatus,limitnum from {{?"
//                + DSMConst.TD_PROM_ASSDRUG + "}} where cstatus&1=0 and actcode=" + actCode;
//        List<Object[]> queryResult = baseDao.queryNative(sql);
//        AssDrugVO[] assDrugVOS = new AssDrugVO[queryResult.size()];
//        baseDao.convToEntity(queryResult, assDrugVOS, AssDrugVO.class);
//        for (int i = 0; i < assDrugVOS.length; i++) {
//            assDrugVOS[i].setProdname();
//        }
//        return Arrays.asList(assDrugVOS);
//    }


    /* *
     * @description 根据活动码获取活动场次
     * @params [actCode]
     * @return java.util.List<com.onek.discount.entity.TimeVO>
     * @exception
     * @author 11842
     * @time  2019/4/2 22:33
     * @version 1.1.1
     **/
    private List<TimeVO> getTimes(int actCode) {
        String sql = "select unqid,actcode,sdate,edate,cstatus from {{?" + DSMConst.TD_PROM_TIME
                + "}} where cstatus&1=0 and actcode=" + actCode;
        List<Object[]> queryResult = baseDao.queryNative(sql);
        TimeVO[] timeVOS = new TimeVO[queryResult.size()];
        baseDao.convToEntity(queryResult, timeVOS, TimeVO.class);
        return Arrays.asList(timeVOS);
    }


    private List<LadderVO> getLadder(int actCode) {
        String sql = "select unqid,ladno,ruleno,ladamt,ladnum,offer,cstatus from {{?" + DSMConst.TD_PROM_LADOFF
                + "}} where cstatus&1=0 and actcode=" + actCode;
        List<Object[]> queryResult = baseDao.queryNative(sql);
        LadderVO[] ladderVOS = new LadderVO[queryResult.size()];
        baseDao.convToEntity(queryResult, ladderVOS, LadderVO.class);
        return Arrays.asList(ladderVOS);
    }

    private List<AssGiftVO> getAssGift(int actCode) {
        String sql = "select unqid,offerno,assgiftno,cstatus,giftname from {{?" + DSMConst.TD_PROM_ASSGIFT
                + "}} a left join {{?" + DSMConst.TD_PROM_GIFT + "}} g on a.assgiftno=g.unqid "
                + " where cstatus&1=0 and actcode=" + actCode;
        List<Object[]> queryResult = baseDao.queryNative(sql);
        AssGiftVO[] assGiftVOS = new AssGiftVO[queryResult.size()];
        baseDao.convToEntity(queryResult, assGiftVOS, AssGiftVO.class);
        return Arrays.asList(assGiftVOS);
    }

}
