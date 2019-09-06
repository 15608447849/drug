package com.onek.user.operations;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.*;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.user.entity.BDCompVO;
import com.onek.user.entity.BDToOrderAchieveemntVO;
import com.onek.user.service.BDAchievementServiceImpl;
import com.onek.util.GLOBALConst;
import com.onek.util.IceRemoteUtil;
import constant.DSMConst;
import dao.BaseDAO;
import util.GsonUtils;
import util.StringUtils;
import util.TimeUtils;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.*;

public class BDAchievementOP {
    private static BDAchievementServiceImpl bdAchievementService = new BDAchievementServiceImpl();

    private static final BaseDAO baseDao = BaseDAO.getBaseDAO();

    /*查询地区*/
   // private static String _QUERY_AREA_USER = "select uid,areac from {{?"+DSMConst.TB_PROXY_UAREA+"}} where areac=? and cstatus&1 = 0 and cstatus&128>0 GROUP BY uid";
    private static String _QUERY_AREA_USER =  "select uid,urealname,roleid from {{?"+DSMConst.TB_SYSTEM_USER+"}} where uid in (select uid from {{?"+DSMConst.TB_PROXY_UAREA+"}} where areac=? and cstatus&1 = 0 and cstatus&128>0)"+
            "and roleid & 8192 >0 ";//and roleid&4096!=4096";
    private static String _QUERY_USER_BELONG = "SELECT uid from {{?"+DSMConst.TB_SYSTEM_USER+"}} WHERE belong = ?";

    /**
     * 获取BD用户信息
     * @param appContext
     * @return
     */
    public static Result executeQuery(AppContext appContext) {
        String json = appContext.param.json;
        QueryParam param = GsonUtils.jsonToJavaBean(json,QueryParam.class);

        String queryOrdParam = getOrdWhereParam(param);

        List<BDCompVO> compList =getCompInfo(param);
        List<BDToOrderAchieveemntVO> oList = getOrderInfos(queryOrdParam);

        String reString = bdAchievementService.getData(param.uid,getBdWhereParam(param),compList,oList);
        return new Result().success(reString);
    }


    /**
     * 获取查询订单时间条件
     * @param param
     * @return
     */
    private static String getOrdWhereParam(QueryParam param){
        StringBuilder sql = new StringBuilder();

        String sdate = "";
        String edate = "";
        if(param == null){
            return sql.toString();
        }
        System.out.println("=============dateflag = " + param.dateflag);
        switch (param.dateflag) {
            case 0:
                if(StringUtils.isEmpty(param.sdate) || StringUtils.isEmpty(param.edate)){
                    break;
                }
                sdate = param.sdate;
                edate = param.edate;
                break;
            case 1:
                //当前时间-1天为昨天
                sdate = TimeUtils.date_yMd_2String(TimeUtils.subtractDay(new Date(),1));
                edate = TimeUtils.date_yMd_2String(TimeUtils.subtractDay(new Date(),1));
                break;
            case 2:
                //今天
                sdate = TimeUtils.date_yMd_2String(new Date());
                edate = TimeUtils.date_yMd_2String(new Date());
                break;
            case 3:
                //本周
                String[] time = TimeUtils.getFirstAndLastOfWeek();
                sdate = time[0];
                edate = time[1];
                break;
            case 4:
                //本月
                String[] mouth = TimeUtils.getFirstAndLastOfMonth();
                sdate = mouth[0];
                edate = mouth[1];
                break;
        }
        System.out.println("=============sdate = " + sdate);
        System.out.println("=============edate = " + edate);
        if(!sdate.isEmpty() && !edate.isEmpty()) {
            sql.append(" HAVING ord.odate BETWEEN '${var}' and '${var2}' ");

            System.out.println("=============sql = " + sql.toString());
            return sql.toString().replace("${var}", sdate).replace("${var2}", edate);
        }else {
            return "";
        }

    }

    /**
     * 查询BD人员条件
     * @param param
     * @return
     */
    private static List<String> getBdWhereParam(QueryParam param){
        List<String> reList = new ArrayList<String>();
        if(param == null){
            return new ArrayList();
        }

        if((param.roleid & 1) > 0){
            if(!StringUtils.isEmpty(param.areac) && "430000000000".equals(param.areac)) {
                reList.add("0");
            }else{
                List<Object[]> list = baseDao.queryNative(_QUERY_AREA_USER,param.areac);
                if(list.size()>0){
                    for (Object[] obj : list) {
                        reList.add(obj[0].toString());
                    }
                }
            }
            return reList;
        }

        if(!StringUtils.isEmpty(param.areac) && !"430000000000".equals(param.areac)){
            List<Object[]> list = baseDao.queryNative(_QUERY_AREA_USER,param.areac);
            if(list.size()>0){
                for (Object[] obj : list) {
                    reList.add(obj[0].toString());
                }
            }
        }else{
            reList.add(String.valueOf(param.uid));
        }
        //当地区查询条件为空则只查询当前登陆用户
        /*
        if(reList.size()<=0) {
           // reList.add(String.valueOf(param.uid));
        }else{
            List<String> userAreaList = new ArrayList<String>();
            List<Object[]> list  = baseDao.queryNative(_QUERY_USER_BELONG,param.uid);
            //如果当前地区下人员包含当前登陆所属人员
            for (Object[] obj:list){
                if(reList.contains(obj[0].toString())){
                    userAreaList.add(obj[0].toString());

                }
            }
            return userAreaList;
        }
        */
        System.out.println("==================="+reList.size());
        return reList;
    }
    /**
     * 查询条件
     */
    private class QueryParam {
        String sdate;
        String edate;
        String areac;
        long uid;
        long roleid;
        int dateflag;//0-自定义时间，1-昨天，2-今天，3-本周，4-本月
    }

    //查询出所有的企业
    private static final String _QUERY_COMP = "select cid compid,IFNULL(inviter,0) inviter,cstatus from {{?"+DSMConst.TB_COMP+"}} where ctype = 0 and inviter !=0";

    /**
     * 获取当前所有企业码
     * @return
     */
    private static List<BDCompVO> getCompInfo(QueryParam param){
        StringBuilder sb = new StringBuilder(_QUERY_COMP);
        if(param != null){
            if(StringUtils.isEmpty(param.sdate) || StringUtils.isEmpty(param.edate)){
            }else{
                sb.append(" and createdate BETWEEN '"+param.sdate+"' and '"+param.edate+"' ");
            }
            if(!StringUtils.isEmpty(param.areac) && !"430000000000".equals(param.areac)){
                sb.append(" and caddrcode = "+param.areac);
            }
        }
        List<String> fList = new ArrayList<String>();
        List<Object[]> list = baseDao.queryNative(sb.toString());
        BDCompVO[] comps = new BDCompVO[list.size()];
        baseDao.convToEntity(list,comps,BDCompVO.class);
        return Arrays.asList(comps);
    }

    /**
     * 获取所有订单详情
     * @return
     */
    public static List<BDToOrderAchieveemntVO> getOrderInfos(String ordeWhereParam){
        String json = IceRemoteUtil.getBDToOrderInfo(ordeWhereParam);
        JSONObject jsons = JSONObject.parseObject(json);
        List<BDToOrderAchieveemntVO> list = GsonUtils.json2List(jsons.getString("data"),BDToOrderAchieveemntVO.class);
        return list;
    }



    public static Map getCumulative(QueryParam param) {
        String time = "";
        if (param == null || StringUtils.isEmpty(param.edate)) {
            time = String.valueOf(TimeUtils.date_yMd_2String(new Date()));
        } else {
            time = param.edate;
        }
        String json = IceRemoteUtil.getBDToOrderInfo(time);
        JSONObject jsons = JSONObject.parseObject(json);
        Map map = GsonUtils.string2Map(jsons.getString("data"));

        return map;
    }
}
