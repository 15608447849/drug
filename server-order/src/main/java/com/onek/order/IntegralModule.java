package com.onek.order;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.consts.IntegralConstant;
import com.onek.context.AppContext;
import com.onek.context.UserSession;
import com.onek.entity.IntegralDetailVO;
import com.onek.entitys.Result;
import constant.DSMConst;
import dao.BaseDAO;
import com.onek.util.GenIdUtil;
import com.onek.util.IceRemoteUtil;
import util.GsonUtils;
import util.StringUtils;
import util.TimeUtils;

import java.util.Date;
import java.util.List;

public class IntegralModule {

    public static int[] base = { 5, 5 ,15, 5, 5, 5, 30 };
    private static BaseDAO baseDao = BaseDAO.getBaseDAO();

    private static String GET_SIGNIN_BY_COMP = "select lastdate,sumtimes from {{?"+ DSMConst.TD_SIGNIN + "}} where cstatus&1 =0 and compid = ?";
    private static String INSERT_SIGNIN_SQL = "insert into {{?"+ DSMConst.TD_SIGNIN + "}} (unqid,compid,lastdate,lasttime,sumtimes,cstatus) values(?,?,CURRENT_DATE,CURRENT_TIME,?,?)";
    private static String UPDATE_SIGNIN_SQL = "update {{?"+ DSMConst.TD_SIGNIN + "}} set lastdate = CURRENT_DATE, lasttime = CURRENT_TIME, sumtimes = ? where compid = ?";
    private static String INSERT_INTEGRAL_DETAIL_SQL = "insert into {{?"+ DSMConst.TD_INTEGRAL_DETAIL + "}} (unqid,compid,istatus,integral,busid,createdate,createtime,cstatus) values(?,?,?,?,?,CURRENT_DATE,CURRENT_TIME,?)";

    private static String SELECT_INTEGRAL_DETAIL_BY_COMP = "select istatus,integral,busid,createdate,createtime from {{?"+ DSMConst.TD_INTEGRAL_DETAIL+ "}} where cstatus&1 =0 and compid = ?";

    /**
     * 签到
     * */
    @UserPermission(ignore = true)
    public Result signIn(AppContext appContext){

//        int compid = 536862720;
        UserSession userSession = appContext.getUserSession();
        int compid = userSession.compId;
        List<Object[]> list = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(), GET_SIGNIN_BY_COMP, new Object[]{ compid});
        int times = 0;
        String date = "";
        if(list != null && list.size() > 0){
            date = list.get(0)[0].toString();
            times = Integer.parseInt(list.get(0)[1].toString());

        }
        if(StringUtils.isEmpty(date)){
            long unqid = GenIdUtil.getUnqId();
            baseDao.updateNativeSharding(compid, TimeUtils.getCurrentYear(), INSERT_SIGNIN_SQL, new Object[]{unqid, compid, 1, 0});
        }else{
            if(date.equals(TimeUtils.getCurrentDate())){
                return new Result().fail("您今天已经签过到了!");
            }
            Date d = TimeUtils.str_yMd_2Date(date);
            d = TimeUtils.addDay(d, 1);
            if(TimeUtils.date_yMd_2String(d).equals(TimeUtils.date_yMd_2String(new Date()))){
                baseDao.updateNativeSharding(compid, TimeUtils.getCurrentYear(),UPDATE_SIGNIN_SQL, new Object[]{times + 1,compid });
            }else{
                times = 0;
                baseDao.updateNativeSharding(compid, TimeUtils.getCurrentYear(),UPDATE_SIGNIN_SQL, new Object[]{1, compid });
            }
        }

        int integral = 0;
        if(times > 6){
            integral = 30;
        }else{
            integral = base[times];
        }
        long unqid = GenIdUtil.getUnqId();
        int code = IceRemoteUtil.addPoint(compid, integral);
        if(code > 0) baseDao.updateNativeSharding(compid, TimeUtils.getCurrentYear(),INSERT_INTEGRAL_DETAIL_SQL, new Object[]{unqid, compid, IntegralConstant.SOURCE_SIGNIN, integral, 0,0 });

        return new Result().success(null);
    }

    @UserPermission(ignore = true)
    public Result addIntegral(AppContext appContext){
        Result result = new Result();
        String json = appContext.param.json;
        IntegralDetailVO integralDetailVO = GsonUtils.jsonToJavaBean(json, IntegralDetailVO.class);

        long unqid = GenIdUtil.getUnqId();
        baseDao.updateNativeSharding(integralDetailVO.getCompid(), TimeUtils.getCurrentYear(), INSERT_INTEGRAL_DETAIL_SQL, new Object[]{ unqid, integralDetailVO.getCompid(), integralDetailVO.getIstatus(), integralDetailVO.getIntegral(), integralDetailVO.getBusid(), 0, });

        return new Result().success(null);
    }


    /**
     * 我的积分明细
     * */
    @UserPermission(ignore = true)
//    public Result signIn(AppContext appContext){
    public Result myIntegralDetail(AppContext appContext){
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();

        int pageSize = jsonObject.get("pageSize").getAsInt();
        int pageIndex = jsonObject.get("pageNo").getAsInt();
        int compid = jsonObject.get("compid").getAsInt();
        Page page = new Page();
        page.pageSize = pageSize;
        page.pageIndex = pageIndex;
        PageHolder pageHolder = new PageHolder(page);
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append(SELECT_INTEGRAL_DETAIL_BY_COMP);
        sqlBuilder.append(" order by createdate desc");
        List<Object[]> queryList = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(),pageHolder, page, sqlBuilder.toString(), compid);
        IntegralDetailVO[] integralDetails = new IntegralDetailVO[queryList.size()];
        baseDao.convToEntity(queryList, integralDetails, IntegralDetailVO.class,new String[]{
                "istatus","integral","busid","createdate",
                "createtime"});
        return result.setQuery(integralDetails, pageHolder);
    }

}
