package com.onek.report;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.report.data.MarketStoreData;
import com.onek.report.data.SystemConfigData;
import constant.DSMConst;
import dao.BaseDAO;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.util.CellRangeAddress;
import util.NumUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import static com.onek.util.fs.FileServerUtils.getExcelDownPath;

/**
 * 报表模块
 *
 * @author JiangWenGuang
 * @version 1.0
 * @since 20190605
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

    // 匹配省份正则表达式
    private static final String PATTERN_PROVINCE = "^[1-9][0-9][0]{10}$";
    // 匹配城市正则表达式
    private static final String PATTERN_CITY = "^[1-9][0-9][0-9]{2}[0]{8}$";

    private static final String col_areac = "areac";
    private static final String col_detail = "detail";
    private static final String col_total = "total";
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
    // 总合计
    private static final String col_sum_total  = "sumtotal";
    private static final String col_list  = "list";

    @SuppressWarnings("unused")
    @UserPermission(ignore = true)
    public Result marketAnalysis(AppContext appContext) {

        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        int year = json.has("year") ? json.get("year").getAsInt() : 0;
        int month = json.has("month") ? json.get("month").getAsInt() : 0;
        String areaC = json.has("areac") ? json.get("areac").getAsString() : "";
        String areaN = json.has("arean") ? json.get("arean").getAsString() : "";
        int type = json.has("type") ? json.get("type").getAsInt() : 0;

        String _areac;
        boolean isProvince = false;
        boolean isCity = false;
        int baseVal = 0 , baseVal1 = 0;
        List<String> codeList = new ArrayList<>();
        Map<String, String> areaMap = new HashMap<>();
        Calendar cale = Calendar.getInstance();
        List<JSONObject> jsonList = new ArrayList<>();
        StringBuilder REGULAR_ONE = new StringBuilder("^");
        StringBuilder REGULAR_TWO = new StringBuilder("^");

        GetBaseVal getBaseVal = new GetBaseVal(type, baseVal, baseVal1).invoke();
        baseVal = getBaseVal.getBaseVal();
        baseVal1 = getBaseVal.getBaseVal1();

        isProvince = Pattern.matches(PATTERN_PROVINCE, areaC);
        if(isProvince){
            _areac = areaC.replaceAll("0000000000", "");
            REGULAR_ONE.append(_areac).append("[0-9]{2}0{8}$");
            REGULAR_TWO.append(_areac).append("[0]{10}$");
        }else{
            isCity = Pattern.matches(PATTERN_CITY, areaC);
            if(isCity){
                _areac = areaC.replaceAll("00000000", "");
                REGULAR_ONE.append(_areac).append("[0-9]{2}0{6}$");
                REGULAR_TWO.append(_areac).append("[0]{8}$");
            }else{
                _areac = areaC.replaceAll("000000", "");
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
            codeList.add(areaC);
            areaMap.put(areaC, areaN);
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
                         String compId = arr[0].toString();
                         String storeType = arr[1].toString();
                         String storeAreac = arr[2].toString();
                         String _date = arr[3].toString();
                         int _year = Integer.parseInt(arr[4].toString());
                         int _month = Integer.parseInt(arr[5].toString());
                         int regNum = Integer.parseInt(arr[6].toString());
                         int authNum = Integer.parseInt(arr[7].toString());
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

                         if(storeType.equals("0")) REG_ONE = REG_ONE + regNum;
                         if(storeType.equals("1")) REG_TWO = REG_TWO + regNum;
                         if(storeType.equals("-1")) REG_THREE = REG_THREE + regNum;
                         REG_SUM = REG_SUM + regNum;

                         if(storeType.equals("0")) AUTH_ONE = AUTH_ONE + authNum;
                         if(storeType.equals("1")) AUTH_TWO = AUTH_TWO + authNum;
                         if(storeType.equals("-1")) AUTH_THREE = AUTH_THREE + authNum;
                         AUTH_SUM = AUTH_SUM + authNum;

                         filterCompMap.put(compId, storeType);
                         cumulativeCompMap.put(compId, storeType);
                     }

                     Map<String, Integer> orderMap = new HashMap<>();
                     for(String [] orderArr : orderList){
                         String compId = orderArr[0];
                         if(type == 0 || type == 2 || type == 4){ // 不是累计报表
                             if(!filterCompMap.keySet().contains(compId)){
                                 continue;
                             }
                         }else{
                             if(!cumulativeCompMap.keySet().contains(compId)){
                                 continue;
                             }
                         }
                         String _date = orderArr[1];
                         int _year = Integer.parseInt(orderArr[2].toString());
                         int _month = Integer.parseInt(orderArr[3].toString());
                         int _num = Integer.parseInt(orderArr[4].toString());
                         if (type == 0 || type == 1) { // 天报
                             String d = subJs.getString(col_date);
                             if (!d.equals(_date)) {
                                 continue;
                             }
                         } else if (type == 2 || type == 3) { // 周报
                             String beginDate = subJs.getString(col_begindate);
                             String endDate = subJs.getString(col_enddate);
                             try {
                                 if (!dateBetweenRange(_date, beginDate, endDate)) {
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

                         if(orderMap.containsKey(compId)){
                             int orderNum = orderMap.get(compId);
                             orderMap.put(compId, orderNum + _num);
                         }else{
                             orderMap.put(compId, _num);
                         }
                     }

                     for(String compId : orderMap.keySet()){
                         int orderNum = orderMap.get(compId);
                         String storeType = "";
                         if(type == 0 || type == 2 || type == 4){
                             storeType = filterCompMap.get(compId);
                         }else{
                             storeType = cumulativeCompMap.get(compId);
                             if(compList.contains(compId)){
                                 continue;
                             }
                         }
                         System.out.println("####### 498 line ["+compId+"]["+orderNum+"] ###########");

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
                         compList.add(compId);

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

                     subJs.put(col_act_etm_rate, calcPercentage(ACTIVE_ONE, AUTH_ONE));
                     subJs.put(col_act_chain_rate, calcPercentage(ACTIVE_TWO, AUTH_TWO));
                     subJs.put(col_act_other_rate, calcPercentage(ACTIVE_THREE, AUTH_THREE));
                     subJs.put(col_act_sum_rate, calcPercentage(ACTIVE_SUM, AUTH_SUM));
                     subJs.put(col_rep_etm_rate, calcPercentage(REPURCHASE_ONE, AUTH_ONE));
                     subJs.put(col_rep_chain_rate, calcPercentage(REPURCHASE_TWO, AUTH_TWO));
                     subJs.put(col_rep_other_rate, calcPercentage(REPURCHASE_THREE, AUTH_THREE));
                     subJs.put(col_rep_sum_rate, calcPercentage(REPURCHASE_SUM, AUTH_SUM));

                 }
             }
        }

        // 计算累计量和总计
        calcTotal(type, jsonList);

        return new Result().success(jsonList);
//        return new Result().success();
    }

    /**
     *
     * 功能: 站在时间维度市场分析报表
     * 参数类型: json
     * 参数集: year=年份 month=月份 areac=地区码 arean=地区名 type=报表类型
     *         type详细说明: 0:日报; 1:日报(累计); 2:周报; 3:周报(累计); 4:月报; 5:月报(累计); 6:年报
     * 返回值: code=200 data=结果信息 data.list=统计结果信息 data.sumtotal=合计信息
     * 详情说明: 导出报表可复用
     * 作者: 蒋文广
     */
    @UserPermission(ignore = true)
    public Result marketAnalysisByTime(AppContext appContext) {
         Result r = marketAnalysis(appContext);
         if(r.code != 200){
             return r;
         }
        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        String _areac_p = json.has("areac") ? json.get("areac").getAsString() : "";
        String _arean_p = json.has("arean") ? json.get("arean").getAsString() : "";
        int _type_p = json.has("type") ? json.get("type").getAsInt() : 0;
        List<JSONObject> jsonList = (List<JSONObject>) r.data;

        JSONObject resultJson = new JSONObject();
        resultJson.put(col_sum_total, new JSONObject());
        List<JSONObject> list = new ArrayList<>();
         if(jsonList != null && jsonList.size() > 0){
             List<JSONObject> newJsonList = new ArrayList<>();
             JSONArray jsArr = jsonList.get(0).getJSONArray(col_detail);
             if(jsArr != null && jsArr.size() > 0){
                 for (int j = 0; j < jsArr.size(); j++) {
                     JSONObject subJs = jsArr.getJSONObject(j);
                     JSONObject newJson = new JSONObject();
                     newJson.put(col_showdate, subJs.getString(col_showdate));
                     newJsonList.add(newJson);
                 }
             }
             for(int i = 0; i < jsonList.size() -1; i++){
                 JSONObject jsonObject = jsonList.get(i);
                 String areac = jsonList.get(i).getString(col_areac);
                 String arean = jsonList.get(i).getString(col_arean);
                 int mark_etm = jsonObject.getInteger(col_mark_etm);
                 int mark_chain = jsonObject.getInteger(col_mark_chain);
                 int mark_other = jsonObject.getInteger(col_mark_other);
                 int mark_sum = jsonObject.getInteger(col_mark_sum);
                 JSONArray jsonArray = jsonObject.getJSONArray(col_detail);
                 for (int j = 0; j < jsonArray.size(); j++) {
                     JSONObject subJs = jsonArray.getJSONObject(j);
                     JSONObject newJson = newJsonList.get(j);
                     if(newJson.getString(col_showdate).equals(subJs.getString(col_showdate))){
                         JSONObject newSubJson = new JSONObject();
                         newSubJson.put(col_areac, areac);
                         newSubJson.put(col_arean, arean);
                         newSubJson.put(col_mark_etm, mark_etm);
                         newSubJson.put(col_mark_chain, mark_chain);
                         newSubJson.put(col_mark_other, mark_other);
                         newSubJson.put(col_mark_sum, mark_sum);
                         newSubJson.put(col_reg_etm, subJs.getIntValue(col_reg_etm));
                         newSubJson.put(col_reg_chain, subJs.getIntValue(col_reg_chain));
                         newSubJson.put(col_reg_other, subJs.getIntValue(col_reg_other));
                         newSubJson.put(col_reg_sum, subJs.getIntValue(col_reg_sum));
                         newSubJson.put(col_auth_etm, subJs.getIntValue(col_auth_etm));
                         newSubJson.put(col_auth_chain, subJs.getIntValue(col_auth_chain));
                         newSubJson.put(col_auth_other, subJs.getIntValue(col_auth_other));
                         newSubJson.put(col_auth_sum, subJs.getIntValue(col_auth_sum));
                         newSubJson.put(col_act_etm, subJs.getIntValue(col_act_etm));
                         newSubJson.put(col_act_chain, subJs.getIntValue(col_act_chain));
                         newSubJson.put(col_act_other, subJs.getIntValue(col_act_other));
                         newSubJson.put(col_act_sum, subJs.getIntValue(col_act_sum));
                         newSubJson.put(col_rep_etm, subJs.getIntValue(col_rep_etm));
                         newSubJson.put(col_rep_chain, subJs.getIntValue(col_rep_chain));
                         newSubJson.put(col_rep_other, subJs.getIntValue(col_rep_other));
                         newSubJson.put(col_rep_sum, subJs.getIntValue(col_rep_sum));
                         newSubJson.put(col_occ_etm_rate, subJs.getIntValue(col_occ_etm_rate));
                         newSubJson.put(col_occ_chain_rate, subJs.getIntValue(col_occ_chain_rate));
                         newSubJson.put(col_occ_other_rate, subJs.getIntValue(col_occ_other_rate));
                         newSubJson.put(col_occ_sum_rate, subJs.getIntValue(col_occ_sum_rate));
                         newSubJson.put(col_act_etm_rate, subJs.getIntValue(col_act_etm_rate));
                         newSubJson.put(col_act_chain_rate, subJs.getIntValue(col_act_chain_rate));
                         newSubJson.put(col_act_other_rate, subJs.getIntValue(col_act_other_rate));
                         newSubJson.put(col_act_sum_rate, subJs.getIntValue(col_act_sum_rate));
                         newSubJson.put(col_rep_etm_rate, subJs.getIntValue(col_rep_etm_rate));
                         newSubJson.put(col_rep_chain_rate, subJs.getIntValue(col_rep_chain_rate));
                         newSubJson.put(col_rep_other_rate, subJs.getIntValue(col_rep_other_rate));
                         newSubJson.put(col_rep_sum_rate, subJs.getIntValue(col_rep_sum_rate));
                         JSONArray newJsonArray = newJson.getJSONArray(col_detail);
                         if(newJsonArray == null || newJsonArray.size()  <= 0){
                             newJsonArray = new JSONArray();
                         }
                         newJsonArray.add(newSubJson);
                         newJson.put(col_detail, newJsonArray);
                     }
                 }
             }

             int MARK_ONE_TOTAL = 0, MARK_TWO_TOTAL = 0, MARK_THREE_TOTAL = 0,MARK_SUM_TOTAL  = 0;
             int REG_ONE_TOTAL = 0, REG_TWO_TOTAL = 0, REG_THREE_TOTAL = 0,REG_SUM_TOTAL  = 0;
             int AUTH_ONE_TOTAL = 0, AUTH_TWO_TOTAL = 0,AUTH_THREE_TOTAL = 0, AUTH_SUM_TOTAL  = 0;
             int ACTIVE_ONE_TOTAL = 0, ACTIVE_TWO_TOTAL = 0, ACTIVE_THREE_TOTAL = 0,ACTIVE_SUM_TOTAL = 0;
             int REPURCHASE_ONE_TOTAL = 0, REPURCHASE_TWO_TOTAL = 0,REPURCHASE_THREE_TOTAL = 0, REPURCHASE_SUM_TOTAL  = 0;
             for(JSONObject obj : newJsonList){
                 JSONArray jsonArray = obj.getJSONArray(col_detail);
                 int MARK_ONE = 0, MARK_TWO = 0, MARK_THREE = 0,MARK_SUM  = 0;
                 int REG_ONE = 0, REG_TWO = 0, REG_THREE = 0,REG_SUM  = 0;
                 int AUTH_ONE = 0, AUTH_TWO = 0,AUTH_THREE = 0, AUTH_SUM  = 0;
                 int ACTIVE_ONE = 0, ACTIVE_TWO = 0, ACTIVE_THREE = 0,ACTIVE_SUM = 0;
                 int REPURCHASE_ONE = 0, REPURCHASE_TWO = 0,REPURCHASE_THREE = 0, REPURCHASE_SUM  = 0;

                 for (int j = 0; j < jsonArray.size(); j++) {
                     JSONObject subJs = jsonArray.getJSONObject(j);

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

                     MARK_ONE += mark_etm;  MARK_TWO += mark_chain; MARK_THREE += mark_other;  MARK_SUM += mark_sum;
                     REG_ONE += reg_etm;  REG_TWO += reg_chain; REG_THREE += reg_other;  REG_SUM += reg_sum;
                     AUTH_ONE += auth_etm; AUTH_TWO += auth_chain; AUTH_THREE += auth_other; AUTH_SUM += auth_sum;
                     ACTIVE_ONE += act_etm; ACTIVE_TWO += act_chain; ACTIVE_THREE += act_other; ACTIVE_SUM += act_sum;
                     REPURCHASE_ONE += rep_etm; REPURCHASE_TWO += rep_chain; REPURCHASE_THREE += rep_other; REPURCHASE_SUM += rep_sum;

                     REG_ONE_TOTAL += reg_etm;  REG_TWO_TOTAL += reg_chain; REG_THREE_TOTAL += reg_other;  REG_SUM_TOTAL += reg_sum;
                     AUTH_ONE_TOTAL += auth_etm; AUTH_TWO_TOTAL += auth_chain; AUTH_THREE_TOTAL += auth_other; AUTH_SUM_TOTAL += auth_sum;
                     ACTIVE_ONE_TOTAL += act_etm; ACTIVE_TWO_TOTAL += act_chain; ACTIVE_THREE_TOTAL += act_other; ACTIVE_SUM_TOTAL += act_sum;
                     REPURCHASE_ONE_TOTAL += rep_etm; REPURCHASE_TWO_TOTAL += rep_chain; REPURCHASE_THREE_TOTAL += rep_other; REPURCHASE_SUM_TOTAL += rep_sum;

                 }
                 obj.put(col_detail, jsonArray);
                 list.add(obj);

                 if(_type_p != 6){ // 不是年报
                     JSONObject jsonObject = new JSONObject();
                     jsonObject.put(col_showdate, "合计");
                     jsonObject.put(col_areac, _areac_p);
                     jsonObject.put(col_arean, _arean_p);
                     jsonObject.put(col_mark_etm, MARK_ONE);
                     jsonObject.put(col_mark_chain, MARK_TWO);
                     jsonObject.put(col_mark_other, MARK_THREE);
                     jsonObject.put(col_mark_sum, MARK_SUM);
                     jsonObject.put(col_reg_etm, REG_ONE);
                     jsonObject.put(col_reg_chain, REG_TWO);
                     jsonObject.put(col_reg_other, REG_THREE);
                     jsonObject.put(col_reg_sum, REG_SUM);
                     jsonObject.put(col_auth_etm, AUTH_ONE);
                     jsonObject.put(col_auth_chain, AUTH_TWO);
                     jsonObject.put(col_auth_other, AUTH_THREE);
                     jsonObject.put(col_auth_sum, AUTH_SUM);
                     jsonObject.put(col_act_etm, ACTIVE_ONE);
                     jsonObject.put(col_act_chain, ACTIVE_TWO);
                     jsonObject.put(col_act_other, ACTIVE_THREE);
                     jsonObject.put(col_act_sum, ACTIVE_SUM);
                     jsonObject.put(col_rep_etm, REPURCHASE_ONE);
                     jsonObject.put(col_rep_chain, REPURCHASE_TWO);
                     jsonObject.put(col_rep_other, REPURCHASE_THREE);
                     jsonObject.put(col_rep_sum, REPURCHASE_SUM);
                     jsonObject.put(col_act_etm_rate, calcPercentage(ACTIVE_ONE, AUTH_ONE));
                     jsonObject.put(col_act_chain_rate, calcPercentage(ACTIVE_TWO, AUTH_TWO));
                     jsonObject.put(col_act_other_rate, calcPercentage(ACTIVE_THREE, AUTH_THREE));
                     jsonObject.put(col_act_sum_rate, calcPercentage(ACTIVE_SUM, AUTH_SUM));
                     jsonObject.put(col_rep_etm_rate, calcPercentage(REPURCHASE_ONE, AUTH_ONE));
                     jsonObject.put(col_rep_chain_rate, calcPercentage(REPURCHASE_TWO, AUTH_TWO));
                     jsonObject.put(col_rep_other_rate, calcPercentage(REPURCHASE_THREE, AUTH_THREE));
                     jsonObject.put(col_rep_sum_rate, calcPercentage(REPURCHASE_SUM, AUTH_SUM));

                     jsonObject.put(col_occ_etm_rate,  calcPercentage(REG_ONE, MARK_ONE));
                     jsonObject.put(col_occ_chain_rate,  calcPercentage(REG_TWO, MARK_TWO));
                     jsonObject.put(col_occ_other_rate,  calcPercentage(REG_THREE, MARK_THREE));
                     jsonObject.put(col_occ_sum_rate,  calcPercentage(REG_SUM, MARK_SUM));
                     obj.put(col_total, jsonObject);

                 }

                 MARK_ONE_TOTAL = MARK_ONE;  MARK_TWO_TOTAL = MARK_TWO; MARK_THREE_TOTAL = MARK_THREE;  MARK_SUM_TOTAL = MARK_SUM;
             }
             resultJson.put(col_list, list);
             // 不为累计报表
             if(_type_p == 0 || _type_p ==2 || _type_p == 4 || _type_p == 6){
                 JSONObject jsonObject = new JSONObject();
                 jsonObject.put(col_showdate, "合计");
                 jsonObject.put(col_areac, _areac_p);
                 jsonObject.put(col_arean, _arean_p);
                 jsonObject.put(col_mark_etm, MARK_ONE_TOTAL);
                 jsonObject.put(col_mark_chain, MARK_TWO_TOTAL);
                 jsonObject.put(col_mark_other, MARK_THREE_TOTAL);
                 jsonObject.put(col_mark_sum, MARK_SUM_TOTAL);
                 jsonObject.put(col_reg_etm, REG_ONE_TOTAL);
                 jsonObject.put(col_reg_chain, REG_TWO_TOTAL);
                 jsonObject.put(col_reg_other, REG_THREE_TOTAL);
                 jsonObject.put(col_reg_sum, REG_SUM_TOTAL);
                 jsonObject.put(col_auth_etm, AUTH_ONE_TOTAL);
                 jsonObject.put(col_auth_chain, AUTH_TWO_TOTAL);
                 jsonObject.put(col_auth_other, AUTH_THREE_TOTAL);
                 jsonObject.put(col_auth_sum, AUTH_SUM_TOTAL);
                 jsonObject.put(col_act_etm, ACTIVE_ONE_TOTAL);
                 jsonObject.put(col_act_chain, ACTIVE_TWO_TOTAL);
                 jsonObject.put(col_act_other, ACTIVE_THREE_TOTAL);
                 jsonObject.put(col_act_sum, ACTIVE_SUM_TOTAL);
                 jsonObject.put(col_rep_etm, REPURCHASE_ONE_TOTAL);
                 jsonObject.put(col_rep_chain, REPURCHASE_TWO_TOTAL);
                 jsonObject.put(col_rep_other, REPURCHASE_THREE_TOTAL);
                 jsonObject.put(col_rep_sum, REPURCHASE_SUM_TOTAL);
                 jsonObject.put(col_act_etm_rate, calcPercentage(ACTIVE_ONE_TOTAL, AUTH_ONE_TOTAL));
                 jsonObject.put(col_act_chain_rate, calcPercentage(ACTIVE_TWO_TOTAL, AUTH_TWO_TOTAL));
                 jsonObject.put(col_act_other_rate, calcPercentage(ACTIVE_THREE_TOTAL, AUTH_THREE_TOTAL));
                 jsonObject.put(col_act_sum_rate, calcPercentage(ACTIVE_SUM_TOTAL, AUTH_SUM_TOTAL));
                 jsonObject.put(col_rep_etm_rate, calcPercentage(REPURCHASE_ONE_TOTAL, AUTH_ONE_TOTAL));
                 jsonObject.put(col_rep_chain_rate, calcPercentage(REPURCHASE_TWO_TOTAL, AUTH_TWO_TOTAL));
                 jsonObject.put(col_rep_other_rate, calcPercentage(REPURCHASE_THREE_TOTAL, AUTH_THREE_TOTAL));
                 jsonObject.put(col_rep_sum_rate, calcPercentage(REPURCHASE_SUM_TOTAL, AUTH_SUM_TOTAL));

                 jsonObject.put(col_occ_etm_rate,  calcPercentage(REG_ONE_TOTAL, MARK_ONE_TOTAL));
                 jsonObject.put(col_occ_chain_rate,  calcPercentage(REG_TWO_TOTAL, MARK_TWO_TOTAL));
                 jsonObject.put(col_occ_other_rate,  calcPercentage(REG_THREE_TOTAL, MARK_THREE_TOTAL));
                 jsonObject.put(col_occ_sum_rate,  calcPercentage(REG_SUM_TOTAL, MARK_SUM_TOTAL));
                 resultJson.put(col_sum_total, jsonObject);
             }

         }
         return new Result().success(resultJson);
    }

    /**
     *
     * 功能: 站在时间维度导出市场分析报表
     * 参数类型: json
     * 参数集: year=年份 month=月份 areac=地区码 arean=地区名 type=报表类型
     *         type详细说明: 0:日报; 1:日报(累计); 2:周报; 3:周报(累计); 4:月报; 5:月报(累计); 6:年报
     * 返回值: code=200 data=文件路径
     * 详情说明:
     * 作者: 蒋文广
     */
    @UserPermission(ignore = true)
    public Result exportMarketAnalysisByTime(AppContext appContext) {
        Result r = marketAnalysisByTime(appContext);
        if(r.code != 200){
            return r;
        }
        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        int year = json.has("year") ? json.get("year").getAsInt() : 0;
        int month = json.has("month") ? json.get("month").getAsInt() : 0;
        String arean = json.has("arean") ? json.get("arean").getAsString() : "";
        int type = json.has("type") ? json.get("type").getAsInt() : 0;
        String str = month > 0 ? (year + "_" + month) : year + "";
        StringBuilder fileName = new StringBuilder(str).append("_").append(arean);
        if(type == 0) { fileName.append("日报"); }
        else if(type == 1) { fileName.append("日报(累计)"); }
        else if(type == 2) { fileName.append("周报"); }
        else if(type == 3) { fileName.append("周报(累计)"); }
        else if(type == 4) { fileName.append("月报"); }
        else if(type == 5) { fileName.append("月报(累计)"); }
        else  { fileName.append("年报"); }

        JSONObject data = (JSONObject) r.data;

        try (HSSFWorkbook hwb = new HSSFWorkbook()){
            HSSFSheet sheet = hwb.createSheet();
            HSSFCellStyle style = hwb.createCellStyle();
            style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
            style.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
            style.setBorderBottom(HSSFCellStyle.BORDER_THIN);
            style.setBorderLeft(HSSFCellStyle.BORDER_THIN);
            style.setBorderRight(HSSFCellStyle.BORDER_THIN);
            style.setBorderTop(HSSFCellStyle.BORDER_THIN);
            style.setTopBorderColor(HSSFColor.BLACK.index);
            style.setBottomBorderColor(HSSFColor.BLACK.index);
            style.setLeftBorderColor(HSSFColor.BLACK.index);
            style.setRightBorderColor(HSSFColor.BLACK.index);
            HSSFRow row;
            HSSFCell cell;

            row = sheet.createRow(0);
            String [] columns = new String [] {"时间", "地区", "市场容量(累计)", "市场容量(累计)", "市场容量(累计)", "市场容量(累计)",
                    "注册数量", "注册数量", "注册数量", "注册数量", "认证数量", "认证数量", "认证数量", "认证数量",
                    "活跃数量", "活跃数量", "活跃数量", "活跃数量", "复购数量", "复购数量", "复购数量", "复购数量",
                    "市场占有率", "市场占有率", "市场占有率", "市场占有率", "活跃率", "活跃率", "活跃率", "活跃率",
                    "复购率", "复购率", "复购率", "复购率"
            };
            for(int i = 0; i < columns.length; i++){
                cell = row.createCell(i);
                cell.setCellStyle(style);
                cell.setCellValue(columns[i]);
            }

            row = sheet.createRow(1);
            String [] columns1 = new String [] {"时间", "地区", "单体", "连锁", "其他", "小计",
                    "单体", "连锁", "其他", "小计","单体", "连锁", "其他", "小计",
                    "单体", "连锁", "其他", "小计","单体", "连锁", "其他", "小计",
                    "单体", "连锁", "其他", "小计","单体", "连锁", "其他", "小计",
                    "单体", "连锁", "其他", "小计"
            };
            for(int i = 0; i < columns1.length; i++){
                cell = row.createCell(i);
                cell.setCellStyle(style);
                cell.setCellValue(columns1[i]);
            }

            int [][] mergedCol = {
                    {0, 0, 2, 5},
                    {0, 0, 6, 9},
                    {0, 0, 10, 13},
                    {0, 0, 14, 17},
                    {0, 0, 18, 21},
                    {0, 0, 22, 25},
                    {0, 0, 26, 29},
                    {0, 0, 30, 33},
                    {0, 1, 0, 0},
                    {0, 1, 1, 1}

            };
            if(mergedCol != null && mergedCol.length > 0){
                for(int i = 0; i < mergedCol.length; i++){
                    CellRangeAddress region = new CellRangeAddress(mergedCol[i][0], mergedCol[i][1], mergedCol[i][2], mergedCol[i][3]);
                    sheet.addMergedRegion(region);
                }
            }

            CellRangeAddress region = null;
            int k = 2;
            JSONArray array = data.getJSONArray(col_list);
            for(int i = 0; i < array.size(); i++){
                JSONObject jss = array.getJSONObject(i);
                String showdate = jss.getString(col_showdate);
                JSONArray subJsonArray = jss.getJSONArray(col_detail);
                int start = k;
                for(int j = 0; j < subJsonArray.size(); j++){
                    JSONObject js = subJsonArray.getJSONObject(j);
                    row = sheet.createRow(k);
                    k++;
                    createExcelDataRow(style, row, js, showdate);
                }
                int end = k - 1;
                if(start != end){ // 不合并同一行
                    region = new CellRangeAddress(start, end, 0, 0);
                    sheet.addMergedRegion(region);
                }
                if(jss.containsKey(col_total) && jss.getJSONObject(col_total).containsKey(col_arean)){
                    JSONObject js = jss.getJSONObject(col_total);
                    row = sheet.createRow(k);
                    k++;
                    createExcelDataRow(style, row, js, js.getString(col_showdate));
                }
            }

            JSONObject js = data.getJSONObject(col_sum_total);
            if(js != null && js.containsKey(col_arean)){
                row = sheet.createRow(k);
                k++;
                createExcelDataRow(style, row, js, js.getString(col_showdate));
            }


//            File file = new File("E:\\demo.xls");
//            FileOutputStream fout = new FileOutputStream(file);
//            hwb.write(fout);
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()){
                hwb.write(bos);

                String title = getExcelDownPath(fileName.toString(), new ByteArrayInputStream(bos.toByteArray()));
                return new Result().success(title);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new Result().fail("导出失败");
    }

    /**
     * 创建excel数据行
     *
     * @param style excel样式
     * @param row excel行
     * @param js 结果集的JSON数据
     * @param data 数据
     */
    private void createExcelDataRow(HSSFCellStyle style, HSSFRow row, JSONObject js, String data) {
        createCell(style, row, 0, data);
        createCell(style, row, 1, js.getString(col_arean));
        createCell(style, row, 2, js.getString(col_mark_etm));
        createCell(style, row, 3, js.getString(col_mark_chain));
        createCell(style, row, 4, js.getString(col_mark_other));
        createCell(style, row, 5, js.getString(col_mark_sum));
        createCell(style, row, 6, js.getString(col_reg_etm));
        createCell(style, row, 7, js.getString(col_reg_chain));
        createCell(style, row, 8, js.getString(col_reg_other));
        createCell(style, row, 9, js.getString(col_reg_sum));
        createCell(style, row, 10, js.getString(col_auth_etm));
        createCell(style, row, 11, js.getString(col_auth_chain));
        createCell(style, row, 12, js.getString(col_auth_other));
        createCell(style, row, 13, js.getString(col_auth_sum));
        createCell(style, row, 14, js.getString(col_act_etm));
        createCell(style, row, 15, js.getString(col_act_chain));
        createCell(style, row, 16, js.getString(col_act_other));
        createCell(style, row, 17, js.getString(col_act_sum));
        createCell(style, row, 18, js.getString(col_rep_etm));
        createCell(style, row, 19, js.getString(col_rep_chain));
        createCell(style, row, 20, js.getString(col_rep_other));
        createCell(style, row, 21, js.getString(col_rep_sum));
        createCell(style, row, 22, js.getString(col_occ_etm_rate));
        createCell(style, row, 23, js.getString(col_occ_chain_rate));
        createCell(style, row, 24, js.getString(col_occ_other_rate));
        createCell(style, row, 25, js.getString(col_occ_sum_rate));
        createCell(style, row, 26, js.getString(col_act_etm_rate));
        createCell(style, row, 27, js.getString(col_act_chain_rate));
        createCell(style, row, 28, js.getString(col_act_other_rate));
        createCell(style, row, 29, js.getString(col_act_sum_rate));
        createCell(style, row, 30, js.getString(col_rep_etm_rate));
        createCell(style, row, 31, js.getString(col_rep_chain_rate));
        createCell(style, row, 32, js.getString(col_rep_other_rate));
        createCell(style, row, 33, js.getString(col_rep_sum_rate));
    }

    private void calcTotal(int type, List<JSONObject> jsonList) {
        int MARK_ONE_TOTAL = 0, MARK_TWO_TOTAL = 0, MARK_THREE_TOTAL = 0,MARK_SUM_TOTAL  = 0;
        int REG_ONE_TOTAL = 0, REG_TWO_TOTAL = 0, REG_THREE_TOTAL = 0,REG_SUM_TOTAL  = 0;
        int AUTH_ONE_TOTAL = 0, AUTH_TWO_TOTAL = 0,AUTH_THREE_TOTAL = 0, AUTH_SUM_TOTAL  = 0;
        int ACTIVE_ONE_TOTAL = 0, ACTIVE_TWO_TOTAL = 0, ACTIVE_THREE_TOTAL = 0,ACTIVE_SUM_TOTAL = 0;
        int REPURCHASE_ONE_TOTAL = 0, REPURCHASE_TWO_TOTAL = 0,REPURCHASE_THREE_TOTAL = 0, REPURCHASE_SUM_TOTAL  = 0;
        // 累计报表
        for(JSONObject js : jsonList) {
//            String a = js.getString(col_areac);
            JSONArray array = js.getJSONArray(col_detail);

            int mark_etm = js.getInteger(col_mark_etm);
            int mark_chain = js.getInteger(col_mark_chain);
            int mark_other = js.getInteger(col_mark_other);
            int mark_sum = js.getInteger(col_mark_sum);

            int REG_ONE = 0, REG_TWO = 0, REG_THREE = 0,REG_SUM  = 0;
            int AUTH_ONE = 0, AUTH_TWO = 0,AUTH_THREE = 0, AUTH_SUM  = 0;
            int ACTIVE_ONE = 0, ACTIVE_TWO = 0, ACTIVE_THREE = 0,ACTIVE_SUM = 0;
            int REPURCHASE_ONE = 0, REPURCHASE_TWO = 0,REPURCHASE_THREE = 0, REPURCHASE_SUM  = 0;
            for (int i = 0; i < array.size(); i++) {
                JSONObject subJs = array.getJSONObject(i);

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


                    subJs.put(col_act_etm_rate, calcPercentage(ACTIVE_ONE, AUTH_ONE));
                    subJs.put(col_act_chain_rate, calcPercentage(ACTIVE_TWO, AUTH_TWO));
                    subJs.put(col_act_other_rate, calcPercentage(ACTIVE_THREE, AUTH_THREE));
                    subJs.put(col_act_sum_rate, calcPercentage(ACTIVE_SUM, AUTH_SUM));
                    subJs.put(col_rep_etm_rate, calcPercentage(REPURCHASE_ONE, AUTH_ONE));
                    subJs.put(col_rep_chain_rate, calcPercentage(REPURCHASE_TWO, AUTH_TWO));
                    subJs.put(col_rep_other_rate, calcPercentage(REPURCHASE_THREE, AUTH_THREE));
                    subJs.put(col_rep_sum_rate, calcPercentage(REPURCHASE_SUM, AUTH_SUM));

                    subJs.put(col_occ_etm_rate,  calcPercentage(REG_ONE, mark_etm));
                    subJs.put(col_occ_chain_rate,  calcPercentage(REG_TWO, mark_chain));
                    subJs.put(col_occ_other_rate,  calcPercentage(REG_THREE, mark_other));
                    subJs.put(col_occ_sum_rate,  calcPercentage(REG_SUM, mark_sum));
                }

                REG_ONE_TOTAL += reg_etm;  REG_TWO_TOTAL += reg_chain; REG_THREE_TOTAL += reg_other;  REG_SUM_TOTAL += reg_sum;
                AUTH_ONE_TOTAL += auth_etm; AUTH_TWO_TOTAL += auth_chain; AUTH_THREE_TOTAL += auth_other; AUTH_SUM_TOTAL += auth_sum;
                ACTIVE_ONE_TOTAL += act_etm; ACTIVE_TWO_TOTAL += act_chain; ACTIVE_THREE_TOTAL += act_other; ACTIVE_SUM_TOTAL += act_sum;
                REPURCHASE_ONE_TOTAL += rep_etm; REPURCHASE_TWO_TOTAL += rep_chain; REPURCHASE_THREE_TOTAL += rep_other; REPURCHASE_SUM_TOTAL += rep_sum;
            }

            MARK_ONE_TOTAL += mark_etm;  MARK_TWO_TOTAL += mark_chain; MARK_THREE_TOTAL += mark_other;  MARK_SUM_TOTAL += mark_sum;
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
        subJS.put(col_occ_etm_rate,  calcPercentage(REG_ONE_TOTAL, MARK_ONE_TOTAL));
        subJS.put(col_occ_chain_rate,  calcPercentage(REG_TWO_TOTAL, MARK_TWO_TOTAL));
        subJS.put(col_occ_other_rate,  calcPercentage(REG_THREE_TOTAL, MARK_THREE_TOTAL));
        subJS.put(col_occ_sum_rate,  calcPercentage(REG_SUM_TOTAL, MARK_SUM_TOTAL));
        subJS.put(col_act_etm_rate, calcPercentage(ACTIVE_ONE_TOTAL, AUTH_ONE_TOTAL));
        subJS.put(col_act_chain_rate, calcPercentage(ACTIVE_TWO_TOTAL, AUTH_TWO_TOTAL));
        subJS.put(col_act_other_rate, calcPercentage(ACTIVE_THREE_TOTAL, AUTH_THREE_TOTAL));
        subJS.put(col_act_sum_rate, calcPercentage(ACTIVE_SUM_TOTAL, AUTH_SUM_TOTAL));
        subJS.put(col_rep_etm_rate, calcPercentage(REPURCHASE_ONE_TOTAL, AUTH_ONE_TOTAL));
        subJS.put(col_rep_chain_rate, calcPercentage(REPURCHASE_TWO_TOTAL, AUTH_TWO_TOTAL));
        subJS.put(col_rep_other_rate, calcPercentage(REPURCHASE_THREE_TOTAL, AUTH_THREE_TOTAL));
        subJS.put(col_rep_sum_rate, calcPercentage(REPURCHASE_SUM_TOTAL, AUTH_SUM_TOTAL));
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
                int m1 = MarketStoreData.getEtmByAreac(c);
                int m2 = MarketStoreData.getChainByAreac(c);
                int m3 = MarketStoreData.getOtherByAreac(c);
                js.put(col_mark_etm, m1);
                js.put(col_mark_chain, m2);
                js.put(col_mark_other, m3);
                js.put(col_mark_sum, m1 + m2 + m3);
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
                int m1 = MarketStoreData.getEtmByAreac(c);
                int m2 = MarketStoreData.getChainByAreac(c);
                int m3 = MarketStoreData.getOtherByAreac(c);
                js.put(col_mark_etm, m1);
                js.put(col_mark_chain, m2);
                js.put(col_mark_other, m3);
                js.put(col_mark_sum, m1 + m2 + m3);
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
                int m1 = MarketStoreData.getEtmByAreac(c);
                int m2 = MarketStoreData.getChainByAreac(c);
                int m3 = MarketStoreData.getOtherByAreac(c);
                js.put(col_mark_etm, m1);
                js.put(col_mark_chain, m2);
                js.put(col_mark_other, m3);
                js.put(col_mark_sum, m1 + m2 + m3);
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
                int m1 = MarketStoreData.getEtmByAreac(c);
                int m2 = MarketStoreData.getChainByAreac(c);
                int m3 = MarketStoreData.getOtherByAreac(c);
                js.put(col_mark_etm, m1);
                js.put(col_mark_chain, m2);
                js.put(col_mark_other, m3);
                js.put(col_mark_sum, m1 + m2 + m3);

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

    /**
     * 生成初始化JSON数据
     */
    private void generateDetailJSON(JSONObject subJS) {
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

    private static void createCell(HSSFCellStyle style, HSSFRow row, int i, String value) {
        HSSFCell cell;
        cell = row.createCell(i);
        cell.setCellStyle(style);
        cell.setCellValue(value);
    }

    /**
     * 计算百分比
     *
     * @param numerator 分子
     * @param denominator 分母
     * @return 百分比 举例50%
     */
    private static String calcPercentage(int numerator, int denominator){
        return NumUtil.div(numerator * 100, denominator) + "%";
    }

    /**
     * 获取系统设置报表的基准值
     */
    private class GetBaseVal {
        private int type;
        private int baseVal;
        private int baseVal1;

        public GetBaseVal(int type, int baseVal, int baseVal1) {
            this.type = type;
            this.baseVal = baseVal;
            this.baseVal1 = baseVal1;
        }

        public int getBaseVal() {
            return baseVal;
        }

        public int getBaseVal1() {
            return baseVal1;
        }

        public GetBaseVal invoke() {

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
            return this;
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
