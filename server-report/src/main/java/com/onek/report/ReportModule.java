package com.onek.report;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
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
@SuppressWarnings({"unchecked"})
public class ReportModule {

    private static final BaseDAO BASE_DAO = BaseDAO.getBaseDAO();

    private static DateFormat datafFormat = new SimpleDateFormat("yyyy-MM-dd");

    private static String COMP_SQL = "select cid,storetype,caddrcode,createdate,year(createdate),month(createdate),sum(regnum),sum(authnum) from ( " +
            "select cid,storetype,substr(caddrcode,1,6) as caddrcode,createdate,1 as regnum,0 as authnum from {{?"+ DSMConst.TB_COMP +"}} where ctype = 0 " +
            "union all " +
            "select cid,storetype,substr(caddrcode,1,6) as caddrcode,createdate,0 as regnum,1 as authnum from {{?"+ DSMConst.TB_COMP +"}} where ctype = 0 and cstatus&256 > 0 " +
            ") tab where caddrcode like ? ";

    private static String ORDER_SQL = "select cusno,odate,year(odate),month(odate),count(1) as ordernum from {{?"+ DSMConst.TD_BK_TRAN_ORDER+"}} where settstatus = 1 and cstatus &1 = 0 ";

    private static String AREA_CHILD_SQL = "select areac,arean from {{?"+ DSMConst.TB_AREA_PCA +"}} where areac REGEXP ?  and areac not REGEXP ? and cstatus &1 = 0";

    private static String PATTERN_PROVICE = "^[1-9][0-9][0]{10}$";
    private static String PATTERN_CITY = "^[1-9][0-9][0-9]{2}[0]{8}$";

    @UserPermission(ignore = true)
    public Result marketAnalysis(AppContext appContext) {

        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        int year = json.has("year") ? json.get("year").getAsInt() : 0;
        int month = json.has("month") ? json.get("month").getAsInt() : 0;
        String areac = json.has("areac") ? json.get("areac").getAsString() : "";
        String arean = json.has("arean") ? json.get("arean").getAsString() : "";
        int type = json.has("type") ? json.get("type").getAsInt() : 0;

        String _areac = "";
        boolean isProvice = false;
        boolean isCity = false;
        isProvice = Pattern.matches(PATTERN_PROVICE, areac);
        StringBuilder REGX = new StringBuilder("^");
        StringBuilder REGX2 = new StringBuilder("^");
        if(isProvice){
            _areac = areac.replaceAll("0000000000", "");
            REGX.append(_areac).append("[0-9]{2}0{8}$");
            REGX2.append(_areac).append("[0]{10}$");
        }else{
            isCity = Pattern.matches(PATTERN_CITY, areac);
            if(isCity){
                _areac = areac.replaceAll("00000000", "");
                REGX.append(_areac).append("[0-9]{2}0{6}$");
                REGX2.append(_areac).append("[0]{8}$");
            }else{
                _areac = areac;
            }
        }
        List<String> codeList = new ArrayList<>();
        Map<String, String> areaMap = new HashMap<>();
        if(isProvice || isCity){
            List<Object[]> list = BASE_DAO.queryNative(AREA_CHILD_SQL, REGX.toString(), REGX2.toString());
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

        Calendar cale = Calendar.getInstance();
        List<JSONObject> jsonList = new ArrayList<>();
        if(type == 0 || type == 1){
            cale.set(Calendar.YEAR, year);
            cale.set(Calendar.MONTH, month);
            cale.set(Calendar.DAY_OF_MONTH, 0);
            int maxDate = cale.get(Calendar.DAY_OF_MONTH);
            String m = month < 10 ?  "0" + month : month +"";
            for(String c : codeList){
                JSONObject js = new JSONObject();
                js.put("areac", c);
                js.put("arean", areaMap.get(c));
                List<JSONObject> subList = new ArrayList<>();
                for(int i = 1; i <= maxDate; i++){
                    String d = i < 10 ?  "0" + i : i +"";
                    JSONObject subJS = new JSONObject();
                    subJS.put("date", year + "-" + m + "-" + d);
                    subJS.put("first", i == 1 ? 1 : 0);
                    generateDetailJSON(subJS, "showdate", i + "号", "mark_etmonomer", "mark_chain", "mark_other", "mark_sum", "reg_etmonomer", "reg_chain", "reg_other", "reg_sum", "auth_etmonomer", "auth_chain", "auth_other", "auth_sum", "active_etmonomer", "active_chain", "active_other", "active_sum", "repurchase_etmonomer", "repurchase_chain", "repurchase_other", "repurchase_sum", "occupancy_etmonomer", "occupancy_chain", "occupancy_other", "occupancy_sum", "activerate_etmonomer", "activerate_chain", "activerate_other", "activerate_sum", "repurchaserate_etmonomer", "repurchaserate_chain", "repurchaserate_other");
                    subJS.put("repurchaserate_sum", "0");
                    subList.add(subJS);
                }
                js.put("detail", subList);
                jsonList.add(js);
            }

        }else if(type == 2 || type == 3){
            cale.set(Calendar.YEAR, year);
            cale.set(Calendar.MONTH, month - 1);
            Date first = firstMonthDate(cale.getTime());
            Map<Integer,WeekRange> maps = new HashMap<Integer, WeekRange>();
            getWeekBeginAndEnd(1, first,maps);

            Set<Integer> set = maps.keySet();
            for(String c : codeList){
                JSONObject js = new JSONObject();
                js.put("areac", c);
                js.put("arean", areaMap.get(c));
                List<JSONObject> subList = new ArrayList<>();
                for(int i = 1; i <= set.size(); i++){
                    WeekRange weekRange = maps.get(i);
                    JSONObject subJS = new JSONObject();
                    subJS.put("begindate", datafFormat.format(weekRange.getBegin()));
                    subJS.put("enddate", datafFormat.format(weekRange.getEnd()));
                    subJS.put("first", i == 1 ? 1 : 0);
                    subJS.put("showdate", i + "周");
                    generateDetailJSON(subJS, "mark_etmonomer", "0", "mark_chain", "mark_other", "mark_sum", "reg_etmonomer", "reg_chain", "reg_other", "reg_sum", "auth_etmonomer", "auth_chain", "auth_other", "auth_sum", "active_etmonomer", "active_chain", "active_other", "active_sum", "repurchase_etmonomer", "repurchase_chain", "repurchase_other", "repurchase_sum", "occupancy_etmonomer", "occupancy_chain", "occupancy_other", "occupancy_sum", "activerate_etmonomer", "activerate_chain", "activerate_other", "activerate_sum", "repurchaserate_etmonomer", "repurchaserate_chain", "repurchaserate_other", "repurchaserate_sum");
                    subList.add(subJS);
                }
                js.put("detail", subList);
                jsonList.add(js);
            }

        }else if(type == 4 || type == 5){
            for(String c : codeList){
                JSONObject js = new JSONObject();
                js.put("areac", c);
                js.put("arean", areaMap.get(c));
                List<JSONObject> subList = new ArrayList<>();
                for(int i = 1; i <= 12; i++){
                    JSONObject subJS = new JSONObject();
                    subJS.put("first", i == 1 ? 1 : 0);
                    subJS.put("year", year);
                    subJS.put("month", i);
                    generateDetailJSON(subJS, "showdate", i + "月", "mark_etmonomer", "mark_chain", "mark_other", "mark_sum", "reg_etmonomer", "reg_chain", "reg_other", "reg_sum", "auth_etmonomer", "auth_chain", "auth_other", "auth_sum", "active_etmonomer", "active_chain", "active_other", "active_sum", "repurchase_etmonomer", "repurchase_chain", "repurchase_other", "repurchase_sum", "occupancy_etmonomer", "occupancy_chain", "occupancy_other", "occupancy_sum", "activerate_etmonomer", "activerate_chain", "activerate_other", "activerate_sum", "repurchaserate_etmonomer", "repurchaserate_chain", "repurchaserate_other");
                    subJS.put("repurchaserate_sum", "0");
                    subList.add(subJS);
                }
                js.put("detail", subList);
                jsonList.add(js);
            }

        }else if(type == 6){
            for(String c : codeList){
                JSONObject js = new JSONObject();
                js.put("areac", c);
                js.put("arean", areaMap.get(c));

                List<JSONObject> subList = new ArrayList<>();
                JSONObject subJS = new JSONObject();
                subJS.put("first", 1);
                subJS.put("year", year);
                generateDetailJSON(subJS, "showdate", year + "年", "mark_etmonomer", "mark_chain", "mark_other", "mark_sum", "reg_etmonomer", "reg_chain", "reg_other", "reg_sum", "auth_etmonomer", "auth_chain", "auth_other", "auth_sum", "active_etmonomer", "active_chain", "active_other", "active_sum", "repurchase_etmonomer", "repurchase_chain", "repurchase_other", "repurchase_sum", "occupancy_etmonomer", "occupancy_chain", "occupancy_other", "occupancy_sum", "activerate_etmonomer", "activerate_chain", "activerate_other", "activerate_sum", "repurchaserate_etmonomer", "repurchaserate_chain", "repurchaserate_other");
                subJS.put("repurchaserate_sum", "0");
                subList.add(subJS);

                js.put("detail", subList);
                jsonList.add(js);
            }
        }

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
                 String a = js.getString("areac");
                 JSONArray array = js.getJSONArray("detail");
                 for(int i = 0; i < array.size() ;i++){
                     JSONObject subJs = array.getJSONObject(i);
                     int first = subJs.getInteger("first");
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
                         if(isProvice){
                             if(!a.substring(0,4).equals(storeAreac.substring(0,4))){ // 匹配地区
                                 continue;
                             }
                         }else if(isCity){
                             if(!a.substring(0,6).equals(storeAreac.substring(0,6))){ // 匹配地区
                                 continue;
                             }
                         }
                         if (type == 0) { // 天报(单天)
                             String d = subJs.getString("date");
                             if (!d.equals(_date)) {
                                 continue;
                             }
                         } else if (type == 1) { // 天报(累计)
                             String d = subJs.getString("date");
                             int val = compareDate(_date, d);
                             if (val == -1 && first == 1) {
                             } else if (val == 0) {
                             } else {
                                 continue;
                             }
                         } else if (type == 2) { // 周报(单周)
                             String begindate = subJs.getString("begindate");
                             String enddate = subJs.getString("enddate");
                             try {
                                 if (!dateBetweenRange(_date, begindate, enddate)) {
                                     continue;
                                 }
                             } catch (Exception e) {
                                 e.printStackTrace();
                             }
                         } else if (type == 3) { // 周报(累计)
                             String begindate = subJs.getString("begindate");
                             String enddate = subJs.getString("enddate");
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
                             int m = subJs.getInteger("month");
                             int y = subJs.getInteger("year");
                             if (y != _year || m != _month) {
                                 continue;
                             }
                         } else if (type == 5) { // 月报(累计)
                             int m = subJs.getInteger("month");
                             int y = subJs.getInteger("year");
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
                     }

                     Map<String, Integer> orderMap = new HashMap<>();
                     for(String [] orderArr : orderList){
                         String compid = orderArr[0];
                         if(!filterCompMap.keySet().contains(compid)){
                             continue;
                         }
                         String _date = orderArr[1];
                         int _year = Integer.parseInt(orderArr[2].toString());
                         int _month = Integer.parseInt(orderArr[3].toString());
                         int _num = Integer.parseInt(orderArr[4].toString());
                         if (type == 0 || type == 1) { // 天报
                             String d = subJs.getString("date");
                             if (!d.equals(_date)) {
                                 continue;
                             }
                         } else if (type == 2 || type == 3) { // 周报
                             String begindate = subJs.getString("begindate");
                             String enddate = subJs.getString("enddate");
                             try {
                                 if (!dateBetweenRange(_date, begindate, enddate)) {
                                     continue;
                                 }
                             } catch (Exception e) {
                                 e.printStackTrace();
                             }
                         } else if (type == 4 || type == 5) { // 月报
                             int m = subJs.getInteger("month");
                             int y = subJs.getInteger("year");
                             if (y != _year || m != _month) {
                                 continue;
                             }
                         } else if (type == 6) {
                             //
                         }

                         if(orderMap.containsKey(compid)){
                             int orderNum = orderMap.get(compid);
                             orderMap.put(compid, orderNum + _num);
                         }else{
                             orderMap.put(compid, _num);
                         }
                     }

//                 System.out.println("####### 495 line ["+orderMap.size()+"] ###########");
                     for(String compid : orderMap.keySet()){
                         int orderNum = orderMap.get(compid);
                         String storeType = filterCompMap.get(compid);
                         System.out.println("####### 498 line ["+compid+"]["+orderNum+"] ###########");
                         if(orderNum > 0){
                             if(storeType.equals("0")) ACTIVE_ONE = ACTIVE_ONE + 1;
                             if(storeType.equals("1")) ACTIVE_TWO = ACTIVE_TWO + 1;
                             if(storeType.equals("-1")) ACTIVE_THREE = ACTIVE_THREE + 1;
                             ACTIVE_SUM = ACTIVE_SUM + 1;
                         }
                         if(orderNum > 1){
                             if(storeType.equals("0")) REPURCHASE_ONE = REPURCHASE_ONE + 1;
                             if(storeType.equals("1")) REPURCHASE_TWO = REPURCHASE_TWO + 1;
                             if(storeType.equals("-1")) REPURCHASE_THREE = REPURCHASE_THREE + 1;
                             REPURCHASE_SUM = REPURCHASE_SUM + 1;
                         }

                     }


                     subJs.put("reg_etmonomer", REG_ONE);
                     subJs.put("reg_chain", REG_TWO);
                     subJs.put("reg_other", REG_THREE);
                     subJs.put("reg_sum", REG_SUM);
                     subJs.put("auth_etmonomer", AUTH_ONE);
                     subJs.put("auth_chain", AUTH_TWO);
                     subJs.put("auth_other", AUTH_THREE);
                     subJs.put("auth_sum", AUTH_SUM);

                     subJs.put("active_etmonomer", ACTIVE_ONE);
                     subJs.put("active_chain", ACTIVE_TWO);
                     subJs.put("active_other", ACTIVE_THREE);
                     subJs.put("active_sum", ACTIVE_SUM);
                     subJs.put("repurchase_etmonomer", REPURCHASE_ONE);
                     subJs.put("repurchase_chain", REPURCHASE_TWO);
                     subJs.put("repurchase_other", REPURCHASE_THREE);
                     subJs.put("repurchase_sum", REPURCHASE_SUM);

                     subJs.put("activerate_etmonomer", NumUtil.div(ACTIVE_ONE, AUTH_ONE));
                     subJs.put("activerate_chain", NumUtil.div(ACTIVE_TWO, AUTH_TWO));
                     subJs.put("activerate_other", NumUtil.div(ACTIVE_THREE, AUTH_THREE));
                     subJs.put("activerate_sum", NumUtil.div(ACTIVE_SUM, AUTH_SUM));
                     subJs.put("repurchaserate_etmonomer", NumUtil.div(REPURCHASE_ONE, AUTH_ONE));
                     subJs.put("repurchaserate_chain", NumUtil.div(REPURCHASE_TWO, AUTH_TWO));
                     subJs.put("repurchaserate_other", NumUtil.div(REPURCHASE_THREE, AUTH_THREE));
                     subJs.put("repurchaserate_sum", NumUtil.div(REPURCHASE_SUM, AUTH_SUM));

                 }
             }
        }

        // 累计报表
        if(type == 1 || type == 3 || type == 5){
            for(JSONObject js : jsonList) {
                String a = js.getString("areac");
                JSONArray array = js.getJSONArray("detail");
                int REG_ONE = 0, REG_TWO = 0, REG_THREE = 0,REG_SUM  = 0;
                int AUTH_ONE = 0, AUTH_TWO = 0,AUTH_THREE = 0, AUTH_SUM  = 0;
                int ACTIVE_ONE = 0, ACTIVE_TWO = 0, ACTIVE_THREE = 0,ACTIVE_SUM = 0;
                int REPURCHASE_ONE = 0, REPURCHASE_TWO = 0,REPURCHASE_THREE = 0, REPURCHASE_SUM  = 0;
                for (int i = 0; i < array.size(); i++) {
                    JSONObject subJs = array.getJSONObject(i);

                    REG_ONE += subJs.getInteger("reg_etmonomer");
                    REG_TWO += subJs.getInteger("reg_chain");
                    REG_THREE += subJs.getInteger("reg_other");
                    REG_SUM += subJs.getInteger("reg_sum");

                    subJs.put("reg_etmonomer", REG_ONE);
                    subJs.put("reg_chain", REG_TWO);
                    subJs.put("reg_other", REG_THREE);
                    subJs.put("reg_sum", REG_SUM);

                    AUTH_ONE += subJs.getInteger("auth_etmonomer");
                    AUTH_TWO += subJs.getInteger("auth_chain");
                    AUTH_THREE += subJs.getInteger("auth_other");
                    AUTH_SUM += subJs.getInteger("auth_sum");

                    subJs.put("auth_etmonomer", AUTH_ONE);
                    subJs.put("auth_chain", AUTH_TWO);
                    subJs.put("auth_other", AUTH_THREE);
                    subJs.put("auth_sum", AUTH_SUM);

                    ACTIVE_ONE += subJs.getInteger("active_etmonomer");
                    ACTIVE_TWO += subJs.getInteger("active_chain");
                    ACTIVE_THREE += subJs.getInteger("active_other");
                    ACTIVE_SUM += subJs.getInteger("active_sum");

                    subJs.put("active_etmonomer", ACTIVE_ONE);
                    subJs.put("active_chain", ACTIVE_TWO);
                    subJs.put("active_other", ACTIVE_THREE);
                    subJs.put("active_sum", ACTIVE_SUM);

                    REPURCHASE_ONE += subJs.getInteger("repurchase_etmonomer");
                    REPURCHASE_TWO += subJs.getInteger("repurchase_chain");
                    REPURCHASE_THREE += subJs.getInteger("repurchase_other");
                    REPURCHASE_SUM += subJs.getInteger("repurchase_sum");

                    subJs.put("repurchase_etmonomer", REPURCHASE_ONE);
                    subJs.put("repurchase_chain", REPURCHASE_TWO);
                    subJs.put("repurchase_other", REPURCHASE_THREE);
                    subJs.put("repurchase_sum", REPURCHASE_SUM);
                }
            }
        }

        return new Result().success(jsonList);
//        return new Result().success();
    }

    private void generateDetailJSON(JSONObject subJS, String mark_etmonomer, String s, String mark_chain, String mark_other, String mark_sum, String reg_etmonomer, String reg_chain, String reg_other, String reg_sum, String auth_etmonomer, String auth_chain, String auth_other, String auth_sum, String active_etmonomer, String active_chain, String active_other, String active_sum, String repurchase_etmonomer, String repurchase_chain, String repurchase_other, String repurchase_sum, String occupancy_etmonomer, String occupancy_chain, String occupancy_other, String occupancy_sum, String activerate_etmonomer, String activerate_chain, String activerate_other, String activerate_sum, String repurchaserate_etmonomer, String repurchaserate_chain, String repurchaserate_other, String repurchaserate_sum) {
        subJS.put(mark_etmonomer, s);
        subJS.put(mark_chain, "0");
        subJS.put(mark_other, "0");
        subJS.put(mark_sum, "0");
        subJS.put(reg_etmonomer, "0");
        subJS.put(reg_chain, "0");
        subJS.put(reg_other, "0");
        subJS.put(reg_sum, "0");
        subJS.put(auth_etmonomer, "0");
        subJS.put(auth_chain, "0");
        subJS.put(auth_other, "0");
        subJS.put(auth_sum, "0");
        subJS.put(active_etmonomer, "0");
        subJS.put(active_chain, "0");
        subJS.put(active_other, "0");
        subJS.put(active_sum, "0");
        subJS.put(repurchase_etmonomer, "0");
        subJS.put(repurchase_chain, "0");
        subJS.put(repurchase_other, "0");
        subJS.put(repurchase_sum, "0");
        subJS.put(occupancy_etmonomer, "0");
        subJS.put(occupancy_chain, "0");
        subJS.put(occupancy_other, "0");
        subJS.put(occupancy_sum, "0");
        subJS.put(activerate_etmonomer, "0");
        subJS.put(activerate_chain, "0");
        subJS.put(activerate_other, "0");
        subJS.put(activerate_sum, "0");
        subJS.put(repurchaserate_etmonomer, "0");
        subJS.put(repurchaserate_chain, "0");
        subJS.put(repurchaserate_other, "0");
        subJS.put(repurchaserate_sum, "0");
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
        return datafFormat.format(date);
    }

    public static int compareDate(String date1, String date2) {
        try {
            Date date3 = datafFormat.parse(date1);
            Date date4 = datafFormat.parse(date2);
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
            Date nowDate = datafFormat.parse(nowDateStr);
            Date startDate = datafFormat.parse(startDateStr);
            Date endDate = datafFormat.parse(endDateStr);

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
