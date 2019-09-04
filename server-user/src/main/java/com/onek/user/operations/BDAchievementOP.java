package com.onek.user.operations;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.*;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.user.entity.BDCompVO;
import com.onek.user.entity.BDToOrderAchieveemntVO;
import com.onek.user.service.BDAchievementServiceImpl;
import com.onek.util.IceRemoteUtil;
import constant.DSMConst;
import dao.BaseDAO;
import util.GsonUtils;
import util.StringUtils;
import util.TimeUtils;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class BDAchievementOP {
    private static BDAchievementServiceImpl bdAchievementService = new BDAchievementServiceImpl();

    private static final BaseDAO baseDao = BaseDAO.getBaseDAO();

    /*查询地区*/
    private static String _QUERY_AREA_USER = "select uid,areac from {{?"+DSMConst.TB_PROXY_UAREA+"}} where areac=? GROUP BY uid";

    /**
     * 获取BD用户信息
     * @param appContext
     * @return
     */
    public static Result executeQuery(AppContext appContext) {
        String json = appContext.param.json;
        QueryParam param = GsonUtils.jsonToJavaBean(json,QueryParam.class);

        String queryOrdParam = getOrdWhereParam(param);

        List<BDCompVO> compList =getCompInfo();
        List<BDToOrderAchieveemntVO> oList = getOrderInfos(queryOrdParam);

        String reString = bdAchievementService.getData(getBdWhereParam(param),compList,oList);
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
            sql.append(" HAVING re.odate BETWEEN '${var}' and '${var2}' ");

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
        if(!param.areac.isEmpty()){
            List<Object[]> list = baseDao.queryNative(_QUERY_AREA_USER,param.areac);
            if(list.size()>0){
                for (Object[] obj : list) {
                    reList.add(obj[0].toString());
                }

            }
        }

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
        int dateflag;//0-自定义时间，1-昨天，2-今天，3-本周，4-本月
    }

    //查询出所有的企业
    private static final String _QUERY_COMP = "select cid compid,IFNULL(inviter,0) inviter,cstatus from {{?"+DSMConst.TB_COMP+"}} where ctype = 0 and inviter !=0";

    /**
     * 获取当前所有企业码
     * @return
     */
    private static List<BDCompVO> getCompInfo(){
        List<String> fList = new ArrayList<String>();
        List<Object[]> list = baseDao.queryNative(_QUERY_COMP);
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
}
