package com.onek.report;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.report.data.SystemConfigData;
import constant.DSMConst;
import dao.BaseDAO;
import util.NumUtil;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 报表模块
 *
 * @author JiangWenGuang
 * @version 1.0
 * @since
 */
public class ReportModule {

    private static final BaseDAO BASE_DAO = BaseDAO.getBaseDAO();

    private static DateFormat DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private static final String COMP_SQL = "select cid,storetype,caddrcode,createdate,year(createdate),month(createdate),sum(regnum),sum(authnum) from ( " +
            "select cid,storetype,substr(caddrcode,1,6) as caddrcode,createdate,1 as regnum,0 as authnum from {{?"+ DSMConst.TB_COMP +"}} where ctype = 0 " +
            "union all " +
            "select cid,storetype,substr(caddrcode,1,6) as caddrcode,createdate,0 as regnum,1 as authnum from {{?"+ DSMConst.TB_COMP +"}} where ctype = 0 and cstatus&256 > 0 " +
            ") tab where caddrcode like ? ";

    private static final String ORDER_SQL = "select cusno,odate,year(odate),month(odate),count(1) as ordernum from {{?"+ DSMConst.TD_BK_TRAN_ORDER+"}} where settstatus = 1 and cstatus &1 = 0 ";

    private static final String AREA_CHILD_SQL = "select areac,arean from {{?"+ DSMConst.TB_AREA_PCA +"}} where areac REGEXP ?  and areac not REGEXP ? and cstatus &1 = 0";

    private static final String PATTERN_PROVINCE = "^[1-9][0-9][0]{10}$";
    private static final String PATTERN_CITY = "^[1-9][0-9][0-9]{2}[0]{8}$";

    private static final String col_areac = "areac";
    private static final String col_detail = "detail";
    private static final String col_arean = "arean";
    private static final String col_first = "first";
    private static final String col_showdate = "showdate";
    private static final String col_year = "year";
    private static final String col_month = "month";
    private static final String col_date = "date";
    private static final String col_begindate = "begindate";
    private static final String col_enddate = "enddate";
    // 市场容量
    private static final String col_mark_etm = "mark_etm";
    private static final String col_mark_chain = "mark_chain";
    private static final String col_mark_other = "mark_other";
    private static final String col_mark_sum = "mark_sum";
    // 注册数量
    private static final String col_reg_etm = "reg_etm";
    private static final String col_reg_chain = "reg_chain";
    private static final String col_reg_other = "reg_other";
    private static final String col_reg_sum = "reg_sum";
    // 认证数量
    private static final String col_auth_etm = "auth_etm";
    private static final String col_auth_chain = "auth_chain";
    private static final String col_auth_other = "auth_other";
    private static final String col_auth_sum = "auth_sum";
    // 活动数量
    private static final String col_act_etm = "act_etm";
    private static final String col_act_chain = "act_chain";
    private static final String col_act_other = "act_other";
    private static final String col_act_sum = "act_sum";
    // 复购数量
    private static final String col_rep_etm = "rep_etm";
    private static final String col_rep_chain = "rep_chain";
    private static final String col_rep_other = "rep_other";
    private static final String col_rep_sum = "rep_sum";
    // 市场占有率
    private static final String col_occ_etm_rate = "occ_etm_rate";
    private static final String col_occ_chain_rate = "occ_chain_rate";
    private static final String col_occ_other_rate = "occ_other_rate";
    private static final String col_occ_sum_rate = "occ_sum_rate";
    // 活动率
    private static final String col_act_etm_rate = "act_etm_rate";
    private static final String col_act_chain_rate = "act_chain_rate";
    private static final String col_act_other_rate = "act_other_rate";
    private static final String col_act_sum_rate = "act_sum_rate";
    // 复购率
    private static final String col_rep_etm_rate = "rep_etm_rate";
    private static final String col_rep_chain_rate = "rep_chain_rate";
    private static final String col_rep_other_rate = "rep_other_rate";
    private static final String col_rep_sum_rate = "rep_sum_rate";

    @SuppressWarnings("unused")
    @UserPermission(ignore = true)
    public Result marketAnalysis(AppContext appContext) {

        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        int year = json.has("year") ? json.get("year").getAsInt() : 0;
        int month = json.has("month") ? json.get("month").getAsInt() : 0;
        String areac = json.has("areac") ? json.get("areac").getAsString() : "";
        String arean = json.has("arean") ? json.get("arean").getAsString() : "";
        int type = json.has("type") ? json.get("type").getAsInt() : 0;

        String _areac = "";
        boolean isProvince = false;
        boolean isCity = false;
        int baseVal = 0 , baseVal1 = 0;
        List<String> codeList = new ArrayList<>();
        Map<String, String> areaMap = new HashMap<>();
        Calendar cale = Calendar.getInstance();
        List<JSONObject> jsonList = new ArrayList<>();
        StringBuilder REGULAR_ONE = new StringBuilder("^");
        StringBuilder REGULAR_TWO = new StringBuilder("^");

        int DAY_ACTIVE_NUM = SystemConfigData.getIntegerValByVarName(SystemConfigData.DAY_ACTIVE_NUM);
        int WEEK_ACTIVE_NUM = SystemConfigData.getIntegerValByVarName(SystemConfigData.WEEK_ACTIVE_NUM);
        int MONTH_ACTIVE_NUM = SystemConfigData.getIntegerValByVarName(SystemConfigData.MONTH_ACTIVE_NUM);
        int YEAR_ACTIVE_NUM = SystemConfigData.getIntegerValByVarName(SystemConfigData.YEAR_ACTIVE_NUM);
        int DAY_REPURCHASE_NUM = SystemConfigData.getIntegerValByVarName(SystemConfigData.DAY_REPURCHASE_NUM);
        int WEEK_REPURCHASE_NUM = SystemConfigData.getIntegerValByVarName(SystemConfigData.WEEK_REPURCHASE_NUM);
        int MONTH_REPURCHASE_NUM = SystemConfigData.getIntegerValByVarName(SystemConfigData.MONTH_REPURCHASE_NUM);
        int YEAR_REPURCHASE_NUM = SystemConfigData.getIntegerValByVarName(SystemConfigData.YEAR_REPURCHASE_NUM);

        if (type == 0 || type == 1) { // 天报
            baseVal = DAY_ACTIVE_NUM;
            baseVal1 = DAY_REPURCHASE_NUM;
        } else if (type == 2 || type == 3) { // 周报
            baseVal = WEEK_ACTIVE_NUM;
            baseVal1 = WEEK_REPURCHASE_NUM;
        } else if (type == 4 || type == 5) { // 月报
            baseVal = MONTH_ACTIVE_NUM;
            baseVal1 = MONTH_REPURCHASE_NUM;
        } else if (type == 6) {
            baseVal = YEAR_ACTIVE_NUM;
            baseVal1 = YEAR_REPURCHASE_NUM;
        }

        isProvince = Pattern.matches(PATTERN_PROVINCE, areac);
        if(isProvince){
            _areac = areac.replaceAll("0000000000", "");
            REGULAR_ONE.append(_areac).append("[0-9]{2}0{8}$");
            REGULAR_TWO.append(_areac).append("[0]{10}$");
        }else{
            isCity = Pattern.matches(PATTERN_CITY, areac);
            if(isCity){
                _areac = areac.replaceAll("00000000", "");
                REGULAR_ONE.append(_areac).append("[0-9]{2}0{6}$");
                REGULAR_TWO.append(_areac).append("[0]{8}$");
            }else{
                _areac = areac.replaceAll("000000", "");
            }
        }

        if(isProvince || isCity){
            List<Object[]> list = BASE_DAO.queryNative(AREA_CHILD_SQL, REGULAR_ONE.toString(), REGULAR_TWO.toString());
            if(list != null && list.size() > 0){
                for(Object[] object : list){
                    String code = object[0].toString();
                    String name = object[1].toString();
                    codeList.add(code);
                    areaMap.put(code, name);
                }
            }else{
                return new Result().fail("未找到相应子集地区");
            }

        }else{
            codeList.add(areac);
            areaMap.put(areac, arean);
        }

        // 初始化表格数据
        initTableData(year, month, type, codeList, areaMap, cale, jsonList);

        List<Object> paramCompList = new ArrayList<Object>();
        StringBuilder compSql = new StringBuilder(COMP_SQL);
        paramCompList.add(_areac + "%");

        List<Object> paramOrderList = new ArrayList<>();
        StringBuilder orderSql = new StringBuilder(ORDER_SQL);

        if(type == 0 || type == 2){ // 天报(单天) 周报(单周)
            compSql.append(" and year(tab.createdate) = ? and month(tab.createdate) = ? ");
            paramCompList.add(year);
            paramCompList.add(month);

            orderSql.append(" and year(odate) = ? and month(odate) = ? ");
            paramOrderList.add(year);
            paramOrderList.add(month);

        }else if(type == 1 || type == 3){ // 天报(累计)
            orderSql.append(" and year(odate) = ? and month(odate) = ? ");
            paramOrderList.add(year);
            paramOrderList.add(month);

        }else if(type == 4 || type == 6){ // 月报(单月)
            compSql.append(" and year(tab.createdate) = ? ");
            paramCompList.add(year);

            orderSql.append(" and year(odate) = ? ");
            paramOrderList.add(year);

        }else if(type == 5){ // 月报(累计)
            orderSql.append(" and year(odate) = ? ");
            paramOrderList.add(year);

        }else if(type == 6){ // 年报
            compSql.append(" and year(tab.createdate) = ? ");
            paramCompList.add(year);

            orderSql.append(" and year(odate) = ? ");
            paramOrderList.add(year);

        }
        compSql.append(" group by createdate,caddrcode,cid,storetype order by caddrcode asc,createdate asc");
        List<Object[]> queryList = BASE_DAO.queryNative(compSql.toString(), paramCompList.toArray());
        List<Integer> comList = new ArrayList<>();
        List<String[]> list = new ArrayList<>();
        if(queryList != null && queryList.size() > 0){
            for(Object[] obj : queryList){
                comList.add(Integer.parseInt(obj[0].toString()));
                list.add(new String[]{obj[0].toString(), obj[1].toString(), obj[2].toString(), obj[3].toString(), obj[4].toString(), obj[5].toString(), obj[6].toString(), obj[7].toString()});
            }
        }

        orderSql.append(" group by cusno,odate");
        List<Object[]> orderQueryList = BASE_DAO.queryNativeSharding(0, year, orderSql.toString(), paramOrderList.toArray());
        List<String[]> orderList = new ArrayList<>();
        if(orderQueryList != null && orderQueryList.size() > 0){
            for(Object[] obj : orderQueryList){
                int compid = Integer.parseInt(obj[0].toString());
                if(!comList.contains(compid)){
                    continue;
                }
                orderList.add(new String[]{obj[0].toString(), obj[1].toString(), obj[2].toString(), obj[3].toString(), obj[4].toString()});
            }
        }

        if(list != null && list.size() > 0){
             for(JSONObject js : jsonList){
                 String a = js.getString(col_areac);
                 JSONArray array = js.getJSONArray(col_detail);

                 Map<String,String> cumulativeCompMap = new HashMap<>();
                 List<String> compList = new ArrayList<>();

                 for(int i = 0; i < array.size() ;i++){
                     JSONObject subJs = array.getJSONObject(i);
                     int first = subJs.getInteger(col_first);
                     int REG_ONE = 0, REG_TWO = 0, REG_THREE = 0,REG_SUM  = 0;
                     int AUTH_ONE = 0, AUTH_TWO = 0,AUTH_THREE = 0, AUTH_SUM  = 0;
                     int ACTIVE_ONE = 0, ACTIVE_TWO = 0, ACTIVE_THREE = 0,ACTIVE_SUM = 0;
                     int REPURCHASE_ONE = 0, REPURCHASE_TWO = 0,REPURCHASE_THREE = 0, REPURCHASE_SUM  = 0;

                     Map<String,String> filterCompMap = new HashMap<>();

                     for(String[] arr : list){
                         String compid = arr[0].toString();
                         String storetype = arr[1].toString();
                         String storeAreac = arr[2].toString();
                         String _date = arr[3].toString();
                         int _year = Integer.parseInt(arr[4].toString());
                         int _month = Integer.parseInt(arr[5].toString());
                         int regnum = Integer.parseInt(arr[6].toString());
                         int authnum = Integer.parseInt(arr[7].toString());
                         if(isProvince){
                             if(!a.substring(0,4).equals(storeAreac.substring(0,4))){ // 匹配地区
                                 continue;
                             }
                         }else if(isCity){
                             if(!a.substring(0,6).equals(storeAreac.substring(0,6))){ // 匹配地区
                                 continue;
                             }
                         }
                         if (type == 0) { // 天报(单天)
                             String d = subJs.getString(col_date);
                             if (!d.equals(_date)) {
                                 continue;
                             }
                         } else if (type == 1) { // 天报(累计)
                             String d = subJs.getString(col_date);
                             int val = compareDate(_date, d);
                             if (val == -1 && first == 1) {
                             } else if (val == 0) {
                             } else {
                                 continue;
                             }
                         } else if (type == 2) { // 周报(单周)
                             String begindate = subJs.getString(col_begindate);
                             String enddate = subJs.getString(col_enddate);
                             try {
                                 if (!dateBetweenRange(_date, begindate, enddate)) {
                                     continue;
                                 }
                             } catch (Exception e) {
                                 e.printStackTrace();
                             }
                         } else if (type == 3) { // 周报(累计)
                             String begindate = subJs.getString(col_begindate);
                             String enddate = subJs.getString(col_enddate);
                             try {
                                 if (compareDate(_date, begindate) == -1 && first == 1) {
                                 } else if (dateBetweenRange(_date, begindate, enddate)) {
                                 } else {
                                     continue;
                                 }
                             } catch (Exception e) {
                                 e.printStackTrace();
                             }
                         } else if (type == 4) { // 月报(单月)
                             int m = subJs.getInteger(col_month);
                             int y = subJs.getInteger(col_year);
                             if (y != _year || m != _month) {
                                 continue;
                             }
                         } else if (type == 5) { // 月报(累计)
                             int m = subJs.getInteger(col_month);
                             int y = subJs.getInteger(col_year);
                             if (_year < y && first == 1) {
                             } else if (_year == y && _month == m) {
                             } else {
                                 continue;
                             }
                         } else if (type == 6) {
                             //
                         }

                         if(storetype.equals("0")) REG_ONE = REG_ONE + regnum;
                         if(storetype.equals("1")) REG_TWO = REG_TWO + regnum;
                         if(storetype.equals("-1")) REG_THREE = REG_THREE + regnum;
                         REG_SUM = REG_SUM + regnum;

                         if(storetype.equals("0")) AUTH_ONE = AUTH_ONE + authnum;
                         if(storetype.equals("1")) AUTH_TWO = AUTH_TWO + authnum;
                         if(storetype.equals("-1")) AUTH_THREE = AUTH_THREE + authnum;
                         AUTH_SUM = AUTH_SUM + authnum;

                         filterCompMap.put(compid, storetype);
                         cumulativeCompMap.put(compid, storetype);
                     }

                     Map<String, Integer> orderMap = new HashMap<>();
                     for(String [] orderArr : orderList){
                         String compid = orderArr[0];
                         if(type == 0 || type == 2 || type == 4){
                             if(!filterCompMap.keySet().contains(compid)){
                                 continue;
                             }
                         }else{
                             if(!cumulativeCompMap.keySet().contains(compid)){
                                 continue;
                             }
                         }
                         String _date = orderArr[1];
                         int _year = Integer.parseInt(orderArr[2].toString());
                         int _month = Integer.parseInt(orderArr[3].toString());
                         int _num = Integer.parseInt(orderArr[4].toString());
                         if (type == 0 || type == 1) { // 天报
                             String d = subJs.getString(col_date);
                             System.out.println("###### 355 line  ##### :"+d + ";"+_date);
                             if (!d.equals(_date)) {
                                 continue;
                             }
                         } else if (type == 2 || type == 3) { // 周报
                             String begindate = subJs.getString(col_begindate);
                             String enddate = subJs.getString(col_enddate);
                             try {
                                 if (!dateBetweenRange(_date, begindate, enddate)) {
                                     continue;
                                 }
                             } catch (Exception e) {
                                 e.printStackTrace();
                             }
                         } else if (type == 4 || type == 5) { // 月报
                             int m = subJs.getInteger(col_month);
                             int y = subJs.getInteger(col_year);
                             if (y != _year || m != _month) {
                                 continue;
                             }
                         } else if (type == 6) {
                             //
                         }

                         System.out.println("####### 377 line ["+compid+"]##########");
                         if(orderMap.containsKey(compid)){
                             int orderNum = orderMap.get(compid);
                             orderMap.put(compid, orderNum + _num);
                         }else{
                             orderMap.put(compid, _num);
                         }
                     }

                     for(String compid : orderMap.keySet()){
                         int orderNum = orderMap.get(compid);
                         String storeType = "";
                         if(type == 0 || type == 2 || type == 4){
                             storeType = filterCompMap.get(compid);
                         }else{
                             storeType = cumulativeCompMap.get(compid);
                             if(compList.contains(compid)){
                                 continue;
                             }
                         }
                         System.out.println("####### 498 line ["+compid+"]["+orderNum+"] ###########");

                         if(orderNum >= baseVal){
                             if(storeType.equals("0")) ACTIVE_ONE = ACTIVE_ONE + 1;
                             if(storeType.equals("1")) ACTIVE_TWO = ACTIVE_TWO + 1;
                             if(storeType.equals("-1")) ACTIVE_THREE = ACTIVE_THREE + 1;
                             ACTIVE_SUM = ACTIVE_SUM + 1;
                         }
                         if(orderNum >= baseVal1){
                             if(storeType.equals("0")) REPURCHASE_ONE = REPURCHASE_ONE + 1;
                             if(storeType.equals("1")) REPURCHASE_TWO = REPURCHASE_TWO + 1;
                             if(storeType.equals("-1")) REPURCHASE_THREE = REPURCHASE_THREE + 1;
                             REPURCHASE_SUM = REPURCHASE_SUM + 1;
                         }
                         compList.add(compid);

                     }

                     subJs.put(col_reg_etm, REG_ONE);
                     subJs.put(col_reg_chain, REG_TWO);
                     subJs.put(col_reg_other, REG_THREE);
                     subJs.put(col_reg_sum, REG_SUM);
                     subJs.put(col_auth_etm, AUTH_ONE);
                     subJs.put(col_auth_chain, AUTH_TWO);
                     subJs.put(col_auth_other, AUTH_THREE);
                     subJs.put(col_auth_sum, AUTH_SUM);

                     subJs.put(col_act_etm, ACTIVE_ONE);
                     subJs.put(col_act_chain, ACTIVE_TWO);
                     subJs.put(col_act_other, ACTIVE_THREE);
                     subJs.put(col_act_sum, ACTIVE_SUM);
                     subJs.put(col_rep_etm, REPURCHASE_ONE);
                     subJs.put(col_rep_chain, REPURCHASE_TWO);
                     subJs.put(col_rep_other, REPURCHASE_THREE);
                     subJs.put(col_rep_sum, REPURCHASE_SUM);

                     subJs.put(col_act_etm_rate, NumUtil.div(ACTIVE_ONE, AUTH_ONE));
                     subJs.put(col_act_chain_rate, NumUtil.div(ACTIVE_TWO, AUTH_TWO));
                     subJs.put(col_act_other_rate, NumUtil.div(ACTIVE_THREE, AUTH_THREE));
                     subJs.put(col_act_sum_rate, NumUtil.div(ACTIVE_SUM, AUTH_SUM));
                     subJs.put(col_rep_etm_rate, NumUtil.div(REPURCHASE_ONE, AUTH_ONE));
                     subJs.put(col_rep_chain_rate, NumUtil.div(REPURCHASE_TWO, AUTH_TWO));
                     subJs.put(col_rep_other_rate, NumUtil.div(REPURCHASE_THREE, AUTH_THREE));
                     subJs.put(col_rep_sum_rate, NumUtil.div(REPURCHASE_SUM, AUTH_SUM));

                 }
             }
        }

        // 计算累计量和总计
        calcTotal(type, jsonList);

        return new Result().success(jsonList);
//        return new Result().success();
    }

    private void calcTotal(int type, List<JSONObject> jsonList) {
        int MARK_ONE_TOTAL = 0, MARK_TWO_TOTAL = 0, MARK_THREE_TOTAL = 0,MARK_SUM_TOTAL  = 0;
        int REG_ONE_TOTAL = 0, REG_TWO_TOTAL = 0, REG_THREE_TOTAL = 0,REG_SUM_TOTAL  = 0;
        int AUTH_ONE_TOTAL = 0, AUTH_TWO_TOTAL = 0,AUTH_THREE_TOTAL = 0, AUTH_SUM_TOTAL  = 0;
        int ACTIVE_ONE_TOTAL = 0, ACTIVE_TWO_TOTAL = 0, ACTIVE_THREE_TOTAL = 0,ACTIVE_SUM_TOTAL = 0;
        int REPURCHASE_ONE_TOTAL = 0, REPURCHASE_TWO_TOTAL = 0,REPURCHASE_THREE_TOTAL = 0, REPURCHASE_SUM_TOTAL  = 0;
        // 累计报表
        for(JSONObject js : jsonList) {
            String a = js.getString(col_areac);
            JSONArray array = js.getJSONArray(col_detail);
            int REG_ONE = 0, REG_TWO = 0, REG_THREE = 0,REG_SUM  = 0;
            int AUTH_ONE = 0, AUTH_TWO = 0,AUTH_THREE = 0, AUTH_SUM  = 0;
            int ACTIVE_ONE = 0, ACTIVE_TWO = 0, ACTIVE_THREE = 0,ACTIVE_SUM = 0;
            int REPURCHASE_ONE = 0, REPURCHASE_TWO = 0,REPURCHASE_THREE = 0, REPURCHASE_SUM  = 0;
            for (int i = 0; i < array.size(); i++) {
                JSONObject subJs = array.getJSONObject(i);

                int mark_etm = subJs.getInteger(col_mark_etm);
                int mark_chain = subJs.getInteger(col_mark_chain);
                int mark_other = subJs.getInteger(col_mark_other);
                int mark_sum = subJs.getInteger(col_mark_sum);

                int reg_etm = subJs.getInteger(col_reg_etm);
                int reg_chain = subJs.getInteger(col_reg_chain);
                int reg_other = subJs.getInteger(col_reg_other);
                int reg_sum = subJs.getInteger(col_reg_sum);

                int auth_etm = subJs.getInteger(col_auth_etm);
                int auth_chain = subJs.getInteger(col_reg_chain);
                int auth_other = subJs.getInteger(col_reg_other);
                int auth_sum = subJs.getInteger(col_reg_sum);

                int act_etm = subJs.getInteger(col_act_etm);
                int act_chain = subJs.getInteger(col_act_chain);
                int act_other = subJs.getInteger(col_act_other);
                int act_sum = subJs.getInteger(col_act_sum);

                int rep_etm = subJs.getInteger(col_rep_etm);
                int rep_chain = subJs.getInteger(col_rep_chain);
                int rep_other = subJs.getInteger(col_rep_other);
                int rep_sum = subJs.getInteger(col_rep_sum);

                if(type == 1 || type == 3 || type == 5){

                    REG_ONE += reg_etm;  REG_TWO += reg_chain; REG_THREE += reg_other;  REG_SUM += reg_sum;

                    subJs.put(col_reg_etm, REG_ONE); subJs.put(col_reg_chain, REG_TWO);
                    subJs.put(col_reg_other, REG_THREE); subJs.put(col_reg_sum, REG_SUM);

                    AUTH_ONE += auth_etm; AUTH_TWO += auth_chain; AUTH_THREE += auth_other; AUTH_SUM += auth_sum;

                    subJs.put(col_auth_etm, AUTH_ONE); subJs.put(col_auth_chain, AUTH_TWO);
                    subJs.put(col_auth_other, AUTH_THREE); subJs.put(col_auth_sum, AUTH_SUM);

                    ACTIVE_ONE += act_etm; ACTIVE_TWO += act_chain; ACTIVE_THREE += act_other; ACTIVE_SUM += act_sum;

                    subJs.put(col_act_etm, ACTIVE_ONE); subJs.put(col_act_chain, ACTIVE_TWO);
                    subJs.put(col_act_other, ACTIVE_THREE); subJs.put(col_act_sum, ACTIVE_SUM);

                    REPURCHASE_ONE += rep_etm; REPURCHASE_TWO += rep_chain; REPURCHASE_THREE += rep_other; REPURCHASE_SUM += rep_sum;

                    subJs.put(col_rep_etm, REPURCHASE_ONE); subJs.put(col_rep_chain, REPURCHASE_TWO);
                    subJs.put(col_rep_other, REPURCHASE_THREE); subJs.put(col_rep_sum, REPURCHASE_SUM);


                    subJs.put(col_act_etm_rate, NumUtil.div(ACTIVE_ONE, AUTH_ONE));
                    subJs.put(col_act_chain_rate, NumUtil.div(ACTIVE_TWO, AUTH_TWO));
                    subJs.put(col_act_other_rate, NumUtil.div(ACTIVE_THREE, AUTH_THREE));
                    subJs.put(col_act_sum_rate, NumUtil.div(ACTIVE_SUM, AUTH_SUM));
                    subJs.put(col_rep_etm_rate, NumUtil.div(REPURCHASE_ONE, AUTH_ONE));
                    subJs.put(col_rep_chain_rate, NumUtil.div(REPURCHASE_TWO, AUTH_TWO));
                    subJs.put(col_rep_other_rate, NumUtil.div(REPURCHASE_THREE, AUTH_THREE));
                    subJs.put(col_rep_sum_rate, NumUtil.div(REPURCHASE_SUM, AUTH_SUM));
                }

                MARK_ONE_TOTAL += mark_etm;  MARK_TWO_TOTAL += mark_chain; MARK_THREE_TOTAL += mark_other;  MARK_SUM_TOTAL += mark_sum;
                REG_ONE_TOTAL += reg_etm;  REG_TWO_TOTAL += reg_chain; REG_THREE_TOTAL += reg_other;  REG_SUM_TOTAL += reg_sum;
                AUTH_ONE_TOTAL += auth_etm; AUTH_TWO_TOTAL += auth_chain; AUTH_THREE_TOTAL += auth_other; AUTH_SUM_TOTAL += auth_sum;
                ACTIVE_ONE_TOTAL += act_etm; ACTIVE_TWO_TOTAL += act_chain; ACTIVE_THREE_TOTAL += act_other; ACTIVE_SUM_TOTAL += act_sum;
                REPURCHASE_ONE_TOTAL += rep_etm; REPURCHASE_TWO_TOTAL += rep_chain; REPURCHASE_THREE_TOTAL += rep_other; REPURCHASE_SUM_TOTAL += rep_sum;
            }
        }


        JSONObject js = new JSONObject();
        js.put(col_areac, 0);
        js.put(col_arean, "合计");
        List<JSONObject> subList = new ArrayList<>();
        JSONObject subJS = new JSONObject();
        subJS.put(col_mark_etm, MARK_ONE_TOTAL);
        subJS.put(col_mark_chain, MARK_TWO_TOTAL);
        subJS.put(col_mark_other, MARK_THREE_TOTAL);
        subJS.put(col_mark_sum, MARK_SUM_TOTAL);
        subJS.put(col_reg_etm, REG_ONE_TOTAL);
        subJS.put(col_reg_chain, REG_TWO_TOTAL);
        subJS.put(col_reg_other, REG_THREE_TOTAL);
        subJS.put(col_reg_sum, REG_SUM_TOTAL);
        subJS.put(col_auth_etm, AUTH_ONE_TOTAL);
        subJS.put(col_auth_chain, AUTH_TWO_TOTAL);
        subJS.put(col_auth_other, AUTH_THREE_TOTAL);
        subJS.put(col_auth_sum, AUTH_SUM_TOTAL);
        subJS.put(col_act_etm, ACTIVE_ONE_TOTAL);
        subJS.put(col_act_chain, ACTIVE_TWO_TOTAL);
        subJS.put(col_act_other, ACTIVE_THREE_TOTAL);
        subJS.put(col_act_sum, ACTIVE_SUM_TOTAL);
        subJS.put(col_rep_etm, REPURCHASE_ONE_TOTAL);
        subJS.put(col_rep_chain, REPURCHASE_TWO_TOTAL);
        subJS.put(col_rep_other, REPURCHASE_THREE_TOTAL);
        subJS.put(col_rep_sum, REPURCHASE_SUM_TOTAL);
        subJS.put(col_occ_etm_rate, "0");
        subJS.put(col_occ_chain_rate, "0");
        subJS.put(col_occ_other_rate, "0");
        subJS.put(col_occ_sum_rate, "0");
        subJS.put(col_act_etm_rate, NumUtil.div(ACTIVE_ONE_TOTAL, AUTH_ONE_TOTAL));
        subJS.put(col_act_chain_rate, NumUtil.div(ACTIVE_TWO_TOTAL, AUTH_TWO_TOTAL));
        subJS.put(col_act_other_rate, NumUtil.div(ACTIVE_THREE_TOTAL, AUTH_THREE_TOTAL));
        subJS.put(col_act_sum_rate, NumUtil.div(ACTIVE_SUM_TOTAL, AUTH_SUM_TOTAL));
        subJS.put(col_rep_etm_rate, NumUtil.div(REPURCHASE_ONE_TOTAL, AUTH_ONE_TOTAL));
        subJS.put(col_rep_chain_rate, NumUtil.div(REPURCHASE_ONE_TOTAL, AUTH_ONE_TOTAL));
        subJS.put(col_rep_other_rate, NumUtil.div(REPURCHASE_ONE_TOTAL, AUTH_ONE_TOTAL));
        subJS.put(col_rep_sum_rate, NumUtil.div(REPURCHASE_ONE_TOTAL, AUTH_ONE_TOTAL));
        subList.add(subJS);
        js.put(col_detail, subList);
        jsonList.add(js);
    }

    private void initTableData(int year, int month, int type, List<String> codeList, Map<String, String> areaMap, Calendar cale, List<JSONObject> jsonList) {
        if(type == 0 || type == 1){
            cale.set(Calendar.YEAR, year);
            cale.set(Calendar.MONTH, month);
            cale.set(Calendar.DAY_OF_MONTH, 0);
            int maxDate = cale.get(Calendar.DAY_OF_MONTH);
            String m = month < 10 ?  "0" + month : month +"";
            for(String c : codeList){
                JSONObject js = new JSONObject();
                js.put(col_areac, c);
                js.put(col_arean, areaMap.get(c));
                List<JSONObject> subList = new ArrayList<>();
                for(int i = 1; i <= maxDate; i++){
                    String d = i < 10 ?  "0" + i : i +"";
                    JSONObject subJS = new JSONObject();
                    subJS.put(col_date, year + "-" + m + "-" + d);
                    subJS.put(col_first, i == 1 ? 1 : 0);
                    subJS.put(col_showdate, i + "号");
                    generateDetailJSON(subJS);
                    subList.add(subJS);
                }
                js.put(col_detail, subList);
                jsonList.add(js);
            }

        }else if(type == 2 || type == 3){
            cale.set(Calendar.YEAR, year);
            cale.set(Calendar.MONTH, month - 1);
            Date first = firstMonthDate(cale.getTime());
            Map<Integer, WeekRange> maps = new HashMap<Integer, WeekRange>();
            getWeekBeginAndEnd(1, first,maps);

            Set<Integer> set = maps.keySet();
            for(String c : codeList){
                JSONObject js = new JSONObject();
                js.put(col_areac, c);
                js.put(col_arean, areaMap.get(c));
                List<JSONObject> subList = new ArrayList<>();
                for(int i = 1; i <= set.size(); i++){
                    WeekRange weekRange = maps.get(i);
                    JSONObject subJS = new JSONObject();
                    subJS.put(col_begindate, DATEFORMAT.format(weekRange.getBegin()));
                    subJS.put(col_enddate, DATEFORMAT.format(weekRange.getEnd()));
                    subJS.put(col_first, i == 1 ? 1 : 0);
                    subJS.put(col_showdate, i + "周");
                    generateDetailJSON(subJS);
                    subList.add(subJS);
                }
                js.put(col_detail, subList);
                jsonList.add(js);
            }

        }else if(type == 4 || type == 5){
            for(String c : codeList){
                JSONObject js = new JSONObject();
                js.put(col_areac, c);
                js.put(col_arean, areaMap.get(c));
                List<JSONObject> subList = new ArrayList<>();
                for(int i = 1; i <= 12; i++){
                    JSONObject subJS = new JSONObject();
                    subJS.put(col_first, i == 1 ? 1 : 0);
                    subJS.put(col_year, year);
                    subJS.put(col_month, i);
                    subJS.put(col_showdate, i + "月");
                    generateDetailJSON(subJS);
                    subList.add(subJS);
                }
                js.put(col_detail, subList);
                jsonList.add(js);
            }

        }else if(type == 6){
            for(String c : codeList){
                JSONObject js = new JSONObject();
                js.put(col_areac, c);
                js.put(col_arean, areaMap.get(c));

                List<JSONObject> subList = new ArrayList<>();
                JSONObject subJS = new JSONObject();
                subJS.put(col_first, 1);
                subJS.put(col_year, year);
                subJS.put(col_showdate,  year + "年");
                generateDetailJSON(subJS);
                subList.add(subJS);

                js.put(col_detail, subList);
                jsonList.add(js);
            }
        }
    }

    private void generateDetailJSON(JSONObject subJS) {
        subJS.put(col_mark_etm, "0");
        subJS.put(col_mark_chain, "0");
        subJS.put(col_mark_other, "0");
        subJS.put(col_mark_sum, "0");
        subJS.put(col_reg_etm, "0");
        subJS.put(col_reg_chain, "0");
        subJS.put(col_reg_other, "0");
        subJS.put(col_reg_sum, "0");
        subJS.put(col_auth_etm, "0");
        subJS.put(col_auth_chain, "0");
        subJS.put(col_auth_other, "0");
        subJS.put(col_auth_sum, "0");
        subJS.put(col_act_etm, "0");
        subJS.put(col_act_chain, "0");
        subJS.put(col_act_other, "0");
        subJS.put(col_act_sum, "0");
        subJS.put(col_rep_etm, "0");
        subJS.put(col_rep_chain, "0");
        subJS.put(col_rep_other, "0");
        subJS.put(col_rep_sum, "0");
        subJS.put(col_occ_etm_rate, "0");
        subJS.put(col_occ_chain_rate, "0");
        subJS.put(col_occ_other_rate, "0");
        subJS.put(col_occ_sum_rate, "0");
        subJS.put(col_act_etm_rate, "0");
        subJS.put(col_act_chain_rate, "0");
        subJS.put(col_act_other_rate, "0");
        subJS.put(col_act_sum_rate, "0");
        subJS.put(col_rep_etm_rate, "0");
        subJS.put(col_rep_chain_rate, "0");
        subJS.put(col_rep_other_rate, "0");
        subJS.put(col_rep_sum_rate, "0");
    }

    // 月初
    public static Date firstMonthDate(Date date){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
        return calendar.getTime();
    }

    // 月末
    public static Date lastMonthDate(Date date){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        return calendar.getTime();
    }

    // 星期几
    public static int week(Date date){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.DAY_OF_WEEK) - 1;
    }

    // 下一天
    public static Date nextDate(Date date){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        return calendar.getTime();
    }

    // 每周开始结束时间
    public static void getWeekBeginAndEnd(int index,Date currentDate,Map<Integer,WeekRange> maps){
        //月末
        Date lastMonthDate = lastMonthDate(currentDate);
        int week = week(currentDate);
        if(null == maps){
            WeekRange range = new WeekRange(currentDate, currentDate);
            maps = new HashMap<Integer, WeekRange>();
            maps.put(index,range);
        }else{
            WeekRange range = maps.get(index);
            if(null == range){
                range = new WeekRange(currentDate);
            }
            range.setEnd(currentDate);
            maps.put(index,range);
        }

        if(currentDate.equals(lastMonthDate)){
            return;
        }

        if(week == 0){
            index++;
        }

        getWeekBeginAndEnd(index,nextDate(currentDate),maps);
    }

    public static String format(Date date){
        return DATEFORMAT.format(date);
    }

    public static int compareDate(String date1, String date2) {
        try {
            Date date3 = DATEFORMAT.parse(date1);
            Date date4 = DATEFORMAT.parse(date2);
            return compareDate(date3,date4);//方式一
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * @param date1
     * @param date2
     */
    public static int compareDate(Date date1, Date date2) {
        if (date1.before(date2)) {
            return -1;
        } else if (date1.after(date2)) {
            return 1;
        } else {
           return 0;
        }
    }

    public static boolean dateBetweenRange(String nowDateStr, String startDateStr, String endDateStr){

        try{
            Date nowDate = DATEFORMAT.parse(nowDateStr);
            Date startDate = DATEFORMAT.parse(startDateStr);
            Date endDate = DATEFORMAT.parse(endDateStr);

            long nowTime = nowDate.getTime();
            long startTime = startDate.getTime();
            long endTime = endDate.getTime();

            return nowTime >= startTime && nowTime <= endTime;
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;

    }

    static class WeekRange{
        Date begin;//周开始日
        Date end;//周结束日
        public WeekRange(Date begin) {
            super();
            this.begin = begin;
        }
        public WeekRange(Date begin, Date end) {
            super();
            this.begin = begin;
            this.end = end;
        }
        public Date getBegin() {
            return begin;
        }
        public void setBegin(Date begin) {
            this.begin = begin;
        }
        public Date getEnd() {
            return end;
        }
        public void setEnd(Date end) {
            this.end = end;
        }
    }

//    public static void main(String[] args) {
//        Calendar cale = Calendar.getInstance();
//        cale.set(Calendar.YEAR, 2019);
//        cale.set(Calendar.MONTH, 4);
//        Date first = firstMonthDate(cale.getTime());
//        Map<Integer,WeekRange> maps = new HashMap<Integer, WeekRange>();
//        getWeekBeginAndEnd(1, first,maps);
//        Set<Integer> a = maps.keySet();
//        for(Integer k : a){
//            System.out.println(k);
//            System.out.println(format(maps.get(k).getBegin()) );
//        }
//    }

}
