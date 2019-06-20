package com.onek.order;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.consts.IntegralConstant;
import com.onek.context.AppContext;
import com.onek.context.UserSession;
import com.onek.entity.IntegralDetailVO;
import com.onek.entitys.Result;
import com.onek.util.GenIdUtil;
import com.onek.util.member.MemberStore;
import constant.DSMConst;
import dao.BaseDAO;
import util.GsonUtils;
import util.StringUtils;
import util.TimeUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
     *
     * 功能: 签到
     * 参数类型: json
     * 参数集: 无
     * 返回值: code=200 date=结果集
     * 详情说明:
     * 作者: 蒋文广
     */
    @UserPermission(ignore = false, needAuthenticated = true)
    public Result signIn(AppContext appContext){

//        int compid = 536862720;
        UserSession userSession = appContext.getUserSession();
        int compid = userSession != null ? userSession.compId : 0;
        if(compid <= 0){
            return new Result().fail("获取企业码失败!");
        }
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
        int code = MemberStore.addPoint(compid, integral);
        if(code > 0) baseDao.updateNativeSharding(compid, TimeUtils.getCurrentYear(),INSERT_INTEGRAL_DETAIL_SQL, new Object[]{unqid, compid, IntegralConstant.SOURCE_SIGNIN, integral, 0,0 });

        return new Result().success(null);
    }

    /**
     *
     * 功能: 获取历史签到信息
     * 参数类型: json
     * 参数集: 无
     * 返回值: code=200 date=结果集
     * 详情说明:
     * 作者: 蒋文广
     */
    @UserPermission(ignore = false, needAuthenticated = true)
    public Result getHisSignIn(AppContext appContext){

        UserSession userSession = appContext.getUserSession();
        int compid = userSession != null ? userSession.compId : 0;
        if(compid <= 0){
            return new Result().fail("获取企业码失败!");
        }
        List<Object[]> list = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(), GET_SIGNIN_BY_COMP, new Object[]{ compid});
        int times = 0;
        String date = "";
        List<String> dates = new ArrayList<>();
        if(list != null && list.size() > 0){
            date = list.get(0)[0].toString();
            times = Integer.parseInt(list.get(0)[1].toString());
            dates.add(date);
            if(times > 1){
                for(int i = 1; i<times;i++){
                    Date d = TimeUtils.subtractDay(TimeUtils.str_yMd_2Date(date), 1);
                    date = TimeUtils.date_yMd_2String(d);
                    dates.add(date);
                }
            }
        }
        JSONArray dateArray = new JSONArray();
        String d = TimeUtils.getCurrentDate();
        for(int i = 0; i < 7; i++){
            if(i > 0){
                Date dd = TimeUtils.subtractDay(TimeUtils.str_yMd_2Date(d), 1);
                d = TimeUtils.date_yMd_2String(dd);
            }
            JSONObject json = new JSONObject();
            json.put("date", TimeUtils.date_Md_2String(TimeUtils.str_yMd_2Date(d)));
            json.put("status", "0");
            if(dates.contains(d)){
                json.put("status", "1");
            }
            dateArray.add(json);
        }
        JSONObject result = new JSONObject();
        result.put("times", times);
        result.put("dates", dateArray);
        return new Result().success(result);
    }

    /**
     *
     * 功能: 添加积分
     * 参数类型: json
     * 参数集: istatus=积分来源 integral=积分值 compid=企业码 busid=相关业务id
     * 返回值: code=200 date=结果集
     * 详情说明: 企业认证审核复用
     * 作者: 蒋文广
     */
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
     *
     * 功能: 积分明细
     * 参数类型: json
     * 参数集: compid=公司码 pageNo=当前页码 pageSize=条数
     * 返回值: code=200 date=结果集
     * 详情说明:
     * 作者: 蒋文广
     */
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


    /**
     * 根据查询当前企业用户积分明细获取签到明细
     * add by liaoz 2019年6月18日
     * @param appContext 入参[开始时间，结束时间]（当前时间月份开始，当前时间月份结束）
     * @return {code==200，data:[{积分来源（此处为签到来源）,生成时间（签到时间）}]}
     */
    public Result queryIntegralDetailBySign(AppContext appContext){

        //return param
        Result result = new Result();

        //获取时间
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        int maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        int minDay = calendar.getActualMinimum(Calendar.DAY_OF_MONTH);
        Date date = calendar.getTime();
        String dayYM = format.format(date);

        String beginDate = dayYM+"-0"+minDay;//开始时间
        String endDate = dayYM+"-"+maxDay;//结束时间
        System.out.println("BEGIN = " + beginDate + " END = " + endDate);
        int compid = appContext.getUserSession().compId;
        if(compid <= 0){
            return result.fail("企业码不能为空！");
        }
        StringBuilder sqlBuilder = new StringBuilder();
        //查询签到所获积分明细===》获取签到时间
        sqlBuilder.append(SELECT_INTEGRAL_DETAIL_BY_COMP).append(" and istatus = '1'");
        //根据时间，当前月份查询
        sqlBuilder.append(" and createdate between ? and ? ");
        List<Object[]> queryList = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(), sqlBuilder.toString(), compid,beginDate,endDate);
        IntegralDetailVO[] integralDetails = new IntegralDetailVO[queryList.size()];
        baseDao.convToEntity(queryList, integralDetails, IntegralDetailVO.class,new String[]{"istatus","integral","busid","createdate", "createtime"});

        JSONObject reJson = new JSONObject();
        //存储当月签到日期
        List<String> list = new ArrayList<String>();
        for(IntegralDetailVO detailVO : integralDetails){
            list.add(detailVO.getCreatedate());
        }
        //获取当月签到记录
        JSONArray jsonArray = new JSONArray();
        for(int i = minDay;i<=maxDay;i++){
            JSONObject jsonObject = new JSONObject();
            String ymd = "";
            if(i<10){
                ymd = dayYM+"-0"+i;
            }else{
                ymd = dayYM+"-"+i;
            }
            if(list.contains(ymd)){
                jsonObject.put(ymd.split("-")[2],"1");
            }else{
                jsonObject.put(ymd.split("-")[2],"0");
            }
            jsonArray.add(jsonObject);
        }
        reJson.put("signList",jsonArray);
        reJson.put("signSum",integralDetails.length);
        reJson.put("nowDate",new SimpleDateFormat("yyyy年MM月dd日").format(new Date()));
        reJson.put("signMsg",integralDetails);
        return result.success(reJson);
    }
}
