package com.onek.report.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
 * 市场分析报表实现
 */
public class MarketAnalysisServiceImpl {

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

    private static final String COL_AREAC = "areac";
    private static final String COL_DETAIL = "detail";
    private static final String COL_TOTAL = "total";
    private static final String COL_AREAN = "arean";
    private static final String COL_FIRST = "first";
    private static final String COL_SHOWDATE = "showdate";
    private static final String COL_YEAR = "year";
    private static final String COL_MONTH = "month";
    private static final String COL_DATE = "date";
    private static final String COL_BEGINDATE = "begindate";
    private static final String COL_ENDDATE = "enddate";
    // 市场容量
    private static final String COL_MARK_ETM = "mark_etm";
    private static final String COL_MARK_CHAIN = "mark_chain";
    private static final String COL_MARK_OTHER = "mark_other";
    private static final String COL_MARK_SUM = "mark_sum";
    // 注册数量
    private static final String COL_REG_ETM = "reg_etm";
    private static final String COL_REG_CHAIN = "reg_chain";
    private static final String COL_REG_OTHER = "reg_other";
    private static final String COL_REG_SUM = "reg_sum";
    // 认证数量
    private static final String COL_AUTH_ETM = "auth_etm";
    private static final String COL_AUTH_CHAIN = "auth_chain";
    private static final String COL_AUTH_OTHER = "auth_other";
    private static final String COL_AUTH_SUM = "auth_sum";
    // 活动数量
    private static final String COL_ACT_ETM = "act_etm";
    private static final String COL_ACT_CHAIN = "act_chain";
    private static final String COL_ACT_OTHER = "act_other";
    private static final String COL_ACT_SUM = "act_sum";
    // 复购数量
    private static final String COL_REP_ETM = "rep_etm";
    private static final String COL_REP_CHAIN = "rep_chain";
    private static final String COL_REP_OTHER = "rep_other";
    private static final String COL_REP_SUM = "rep_sum";
    // 市场占有率
    private static final String COL_OCC_ETM_RATE = "occ_etm_rate";
    private static final String COL_OCC_CHAIN_RATE = "occ_chain_rate";
    private static final String COL_OCC_OTHER_RATE = "occ_other_rate";
    private static final String COL_OCC_SUM_RATE = "occ_sum_rate";
    // 活动率
    private static final String COL_ACT_ETM_RATE = "act_etm_rate";
    private static final String COL_ACT_CHAIN_RATE = "act_chain_rate";
    private static final String COL_ACT_OTHER_RATE = "act_other_rate";
    private static final String COL_ACT_SUM_RATE = "act_sum_rate";
    // 复购率
    private static final String COL_REP_ETM_RATE = "rep_etm_rate";
    private static final String COL_REP_CHAIN_RATE = "rep_chain_rate";
    private static final String COL_REP_OTHER_RATE = "rep_other_rate";
    private static final String COL_REP_SUM_RATE = "rep_sum_rate";
    // 总合计
    private static final String COL_SUM_TOTAL  = "sumtotal";
    private static final String COL_LIST  = "list";

    public List<JSONObject> marketAnalysis(int year,int month,String areaC,String areaN,int type) {

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

        MarketAnalysisServiceImpl.GetBaseVal getBaseVal = new MarketAnalysisServiceImpl.GetBaseVal(type, baseVal, baseVal1).invoke();
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
                return jsonList;
            }

        }else{
            codeList.add(areaC);
            areaMap.put(areaC, areaN);
        }

        // 初始化表格数据
        initTableData(year, month, type, codeList, areaMap, cale, jsonList);

        GetDbData getDbData = new GetDbData(year, month, type, _areac).invoke();
        List<String[]> list = getDbData.getList();
        List<String[]> orderList = getDbData.getOrderList();

        if(list != null && list.size() > 0){
            for(JSONObject js : jsonList){
                String a = js.getString(COL_AREAC);
                JSONArray array = js.getJSONArray(COL_DETAIL);

                Map<String,String> cumulativeCompMap = new HashMap<>();
                List<String> compList = new ArrayList<>();

                for(int i = 0; i < array.size() ;i++){
                    JSONObject subJs = array.getJSONObject(i);
                    int first = subJs.getInteger(COL_FIRST);
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

                        if (isMatch(type, isProvince, isCity, a, subJs, first, storeAreac, _date, _year, _month))
                            continue;

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
                            String d = subJs.getString(COL_DATE);
                            if (!d.equals(_date)) {
                                continue;
                            }
                        } else if (type == 2 || type == 3) { // 周报
                            String beginDate = subJs.getString(COL_BEGINDATE);
                            String endDate = subJs.getString(COL_ENDDATE);
                            try {
                                if (!dateBetweenRange(_date, beginDate, endDate)) {
                                    continue;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (type == 4 || type == 5) { // 月报
                            int m = subJs.getInteger(COL_MONTH);
                            int y = subJs.getInteger(COL_YEAR);
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

                    subJs.put(COL_REG_ETM, REG_ONE);
                    subJs.put(COL_REG_CHAIN, REG_TWO);
                    subJs.put(COL_REG_OTHER, REG_THREE);
                    subJs.put(COL_REG_SUM, REG_SUM);
                    subJs.put(COL_AUTH_ETM, AUTH_ONE);
                    subJs.put(COL_AUTH_CHAIN, AUTH_TWO);
                    subJs.put(COL_AUTH_OTHER, AUTH_THREE);
                    subJs.put(COL_AUTH_SUM, AUTH_SUM);

                    subJs.put(COL_ACT_ETM, ACTIVE_ONE);
                    subJs.put(COL_ACT_CHAIN, ACTIVE_TWO);
                    subJs.put(COL_ACT_OTHER, ACTIVE_THREE);
                    subJs.put(COL_ACT_SUM, ACTIVE_SUM);
                    subJs.put(COL_REP_ETM, REPURCHASE_ONE);
                    subJs.put(COL_REP_CHAIN, REPURCHASE_TWO);
                    subJs.put(COL_REP_OTHER, REPURCHASE_THREE);
                    subJs.put(COL_REP_SUM, REPURCHASE_SUM);

                    subJs.put(COL_ACT_ETM_RATE, calcPercentage(ACTIVE_ONE, AUTH_ONE));
                    subJs.put(COL_ACT_CHAIN_RATE, calcPercentage(ACTIVE_TWO, AUTH_TWO));
                    subJs.put(COL_ACT_OTHER_RATE, calcPercentage(ACTIVE_THREE, AUTH_THREE));
                    subJs.put(COL_ACT_SUM_RATE, calcPercentage(ACTIVE_SUM, AUTH_SUM));
                    subJs.put(COL_REP_ETM_RATE, calcPercentage(REPURCHASE_ONE, AUTH_ONE));
                    subJs.put(COL_REP_CHAIN_RATE, calcPercentage(REPURCHASE_TWO, AUTH_TWO));
                    subJs.put(COL_REP_OTHER_RATE, calcPercentage(REPURCHASE_THREE, AUTH_THREE));
                    subJs.put(COL_REP_SUM_RATE, calcPercentage(REPURCHASE_SUM, AUTH_SUM));

                }
            }
        }

        // 计算累计量和总计
        calcTotal(type, jsonList);

        return jsonList;

    }

    /**
     * 是否匹配数据
     *
     */
    private boolean isMatch(int type, boolean isProvince, boolean isCity, String a, JSONObject subJs, int first, String storeAreac, String _date, int _year, int _month) {
        if(isProvince){
            if(!a.substring(0,4).equals(storeAreac.substring(0,4))){ // 匹配地区
                return true;
            }
        }else if(isCity){
            if(!a.substring(0,6).equals(storeAreac.substring(0,6))){ // 匹配地区
                return true;
            }
        }
        if (type == 0) { // 天报(单天)
            String d = subJs.getString(COL_DATE);
            if (!d.equals(_date)) {
                return true;
            }
        } else if (type == 1) { // 天报(累计)
            String d = subJs.getString(COL_DATE);
            int val = compareDate(_date, d);
            if (val == -1 && first == 1) {
            } else if (val == 0) {
            } else {
                return true;
            }
        } else if (type == 2) { // 周报(单周)
            String begindate = subJs.getString(COL_BEGINDATE);
            String enddate = subJs.getString(COL_ENDDATE);
            try {
                if (!dateBetweenRange(_date, begindate, enddate)) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (type == 3) { // 周报(累计)
            String begindate = subJs.getString(COL_BEGINDATE);
            String enddate = subJs.getString(COL_ENDDATE);
            try {
                if (compareDate(_date, begindate) == -1 && first == 1) {
                } else if (dateBetweenRange(_date, begindate, enddate)) {
                } else {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (type == 4) { // 月报(单月)
            int m = subJs.getInteger(COL_MONTH);
            int y = subJs.getInteger(COL_YEAR);
            if (y != _year || m != _month) {
                return true;
            }
        } else if (type == 5) { // 月报(累计)
            int m = subJs.getInteger(COL_MONTH);
            int y = subJs.getInteger(COL_YEAR);
            if (_year < y && first == 1) {
            } else if (_year == y && _month == m) {
            } else {
                return true;
            }
        } else if (type == 6) {
            //
        }
        return false;
    }

    public JSONObject marketAnalysisByTime(int year,int month,String areaC,String areaN,int type) {
        List<JSONObject> jsonList = marketAnalysis(year, month, areaC, areaN, type);
        if(jsonList == null || jsonList.size() <= 0){
            return null;
        }

        JSONObject resultJson = new JSONObject();
        resultJson.put(COL_SUM_TOTAL, new JSONObject());
        List<JSONObject> list = new ArrayList<>();
        if(jsonList != null && jsonList.size() > 0){
            List<JSONObject> newJsonList = new ArrayList<>();
            JSONArray jsArr = jsonList.get(0).getJSONArray(COL_DETAIL);
            if(jsArr != null && jsArr.size() > 0){
                for (int j = 0; j < jsArr.size(); j++) {
                    JSONObject subJs = jsArr.getJSONObject(j);
                    JSONObject newJson = new JSONObject();
                    if(subJs.containsKey(COL_MONTH)){
                        newJson.put(COL_MONTH, subJs.get(COL_MONTH));
                    }else{
                        newJson.put(COL_MONTH, "");
                    }
                    newJson.put(COL_SHOWDATE, subJs.getString(COL_SHOWDATE));
                    newJsonList.add(newJson);
                }
            }
            for(int i = 0; i < jsonList.size() -1; i++){
                JSONObject jsonObject = jsonList.get(i);
                String areac = jsonList.get(i).getString(COL_AREAC);
                String arean = jsonList.get(i).getString(COL_AREAN);
                int mark_etm = jsonObject.getInteger(COL_MARK_ETM);
                int mark_chain = jsonObject.getInteger(COL_MARK_CHAIN);
                int mark_other = jsonObject.getInteger(COL_MARK_OTHER);
                int mark_sum = jsonObject.getInteger(COL_MARK_SUM);
                JSONArray jsonArray = jsonObject.getJSONArray(COL_DETAIL);
                for (int j = 0; j < jsonArray.size(); j++) {
                    JSONObject subJs = jsonArray.getJSONObject(j);
                    JSONObject newJson = newJsonList.get(j);
                    if(newJson.getString(COL_SHOWDATE).equals(subJs.getString(COL_SHOWDATE))){
                        JSONObject newSubJson = new JSONObject();
                        newSubJson.put(COL_AREAC, areac);
                        newSubJson.put(COL_AREAN, arean);
                        newSubJson.put(COL_MARK_ETM, mark_etm);
                        newSubJson.put(COL_MARK_CHAIN, mark_chain);
                        newSubJson.put(COL_MARK_OTHER, mark_other);
                        newSubJson.put(COL_MARK_SUM, mark_sum);
                        newSubJson.put(COL_REG_ETM, subJs.getIntValue(COL_REG_ETM));
                        newSubJson.put(COL_REG_CHAIN, subJs.getIntValue(COL_REG_CHAIN));
                        newSubJson.put(COL_REG_OTHER, subJs.getIntValue(COL_REG_OTHER));
                        newSubJson.put(COL_REG_SUM, subJs.getIntValue(COL_REG_SUM));
                        newSubJson.put(COL_AUTH_ETM, subJs.getIntValue(COL_AUTH_ETM));
                        newSubJson.put(COL_AUTH_CHAIN, subJs.getIntValue(COL_AUTH_CHAIN));
                        newSubJson.put(COL_AUTH_OTHER, subJs.getIntValue(COL_AUTH_OTHER));
                        newSubJson.put(COL_AUTH_SUM, subJs.getIntValue(COL_AUTH_SUM));
                        newSubJson.put(COL_ACT_ETM, subJs.getIntValue(COL_ACT_ETM));
                        newSubJson.put(COL_ACT_CHAIN, subJs.getIntValue(COL_ACT_CHAIN));
                        newSubJson.put(COL_ACT_OTHER, subJs.getIntValue(COL_ACT_OTHER));
                        newSubJson.put(COL_ACT_SUM, subJs.getIntValue(COL_ACT_SUM));
                        newSubJson.put(COL_REP_ETM, subJs.getIntValue(COL_REP_ETM));
                        newSubJson.put(COL_REP_CHAIN, subJs.getIntValue(COL_REP_CHAIN));
                        newSubJson.put(COL_REP_OTHER, subJs.getIntValue(COL_REP_OTHER));
                        newSubJson.put(COL_REP_SUM, subJs.getIntValue(COL_REP_SUM));
                        newSubJson.put(COL_OCC_ETM_RATE, subJs.getString(COL_OCC_ETM_RATE));
                        newSubJson.put(COL_OCC_CHAIN_RATE, subJs.getString(COL_OCC_CHAIN_RATE));
                        newSubJson.put(COL_OCC_OTHER_RATE, subJs.getString(COL_OCC_OTHER_RATE));
                        newSubJson.put(COL_OCC_SUM_RATE, subJs.getString(COL_OCC_SUM_RATE));
                        newSubJson.put(COL_ACT_ETM_RATE, subJs.getString(COL_ACT_ETM_RATE));
                        newSubJson.put(COL_ACT_CHAIN_RATE, subJs.getString(COL_ACT_CHAIN_RATE));
                        newSubJson.put(COL_ACT_OTHER_RATE, subJs.getString(COL_ACT_OTHER_RATE));
                        newSubJson.put(COL_ACT_SUM_RATE, subJs.getString(COL_ACT_SUM_RATE));
                        newSubJson.put(COL_REP_ETM_RATE, subJs.getString(COL_REP_ETM_RATE));
                        newSubJson.put(COL_REP_CHAIN_RATE, subJs.getString(COL_REP_CHAIN_RATE));
                        newSubJson.put(COL_REP_OTHER_RATE, subJs.getString(COL_REP_OTHER_RATE));
                        newSubJson.put(COL_REP_SUM_RATE, subJs.getString(COL_REP_SUM_RATE));

                        String m = newJson.containsKey(COL_MONTH) ? newJson.getString(COL_MONTH) : "";
                        newSubJson.put(COL_MONTH, m);
                        JSONArray newJsonArray = newJson.getJSONArray(COL_DETAIL);
                        if(newJsonArray == null || newJsonArray.size()  <= 0){
                            newJsonArray = new JSONArray();
                        }
                        newJsonArray.add(newSubJson);
                        newJson.put(COL_DETAIL, newJsonArray);
                    }
                }
            }

            int MARK_ONE_TOTAL = 0, MARK_TWO_TOTAL = 0, MARK_THREE_TOTAL = 0,MARK_SUM_TOTAL  = 0;
            int REG_ONE_TOTAL = 0, REG_TWO_TOTAL = 0, REG_THREE_TOTAL = 0,REG_SUM_TOTAL  = 0;
            int AUTH_ONE_TOTAL = 0, AUTH_TWO_TOTAL = 0,AUTH_THREE_TOTAL = 0, AUTH_SUM_TOTAL  = 0;
            int ACTIVE_ONE_TOTAL = 0, ACTIVE_TWO_TOTAL = 0, ACTIVE_THREE_TOTAL = 0,ACTIVE_SUM_TOTAL = 0;
            int REPURCHASE_ONE_TOTAL = 0, REPURCHASE_TWO_TOTAL = 0,REPURCHASE_THREE_TOTAL = 0, REPURCHASE_SUM_TOTAL  = 0;
            for(JSONObject obj : newJsonList){
                JSONArray jsonArray = obj.getJSONArray(COL_DETAIL);
                int MARK_ONE = 0, MARK_TWO = 0, MARK_THREE = 0,MARK_SUM  = 0;
                int REG_ONE = 0, REG_TWO = 0, REG_THREE = 0,REG_SUM  = 0;
                int AUTH_ONE = 0, AUTH_TWO = 0,AUTH_THREE = 0, AUTH_SUM  = 0;
                int ACTIVE_ONE = 0, ACTIVE_TWO = 0, ACTIVE_THREE = 0,ACTIVE_SUM = 0;
                int REPURCHASE_ONE = 0, REPURCHASE_TWO = 0,REPURCHASE_THREE = 0, REPURCHASE_SUM  = 0;

                String m = obj.containsKey(COL_MONTH) ? obj.getString(COL_MONTH) : "";

                for (int j = 0; j < jsonArray.size(); j++) {
                    JSONObject subJs = jsonArray.getJSONObject(j);

                    int mark_etm = subJs.getInteger(COL_MARK_ETM);
                    int mark_chain = subJs.getInteger(COL_MARK_CHAIN);
                    int mark_other = subJs.getInteger(COL_MARK_OTHER);
                    int mark_sum = subJs.getInteger(COL_MARK_SUM);

                    int reg_etm = subJs.getInteger(COL_REG_ETM);
                    int reg_chain = subJs.getInteger(COL_REG_CHAIN);
                    int reg_other = subJs.getInteger(COL_REG_OTHER);
                    int reg_sum = subJs.getInteger(COL_REG_SUM);

                    int auth_etm = subJs.getInteger(COL_AUTH_ETM);
                    int auth_chain = subJs.getInteger(COL_REG_CHAIN);
                    int auth_other = subJs.getInteger(COL_REG_OTHER);
                    int auth_sum = subJs.getInteger(COL_REG_SUM);

                    int act_etm = subJs.getInteger(COL_ACT_ETM);
                    int act_chain = subJs.getInteger(COL_ACT_CHAIN);
                    int act_other = subJs.getInteger(COL_ACT_OTHER);
                    int act_sum = subJs.getInteger(COL_ACT_SUM);

                    int rep_etm = subJs.getInteger(COL_REP_ETM);
                    int rep_chain = subJs.getInteger(COL_REP_CHAIN);
                    int rep_other = subJs.getInteger(COL_REP_OTHER);
                    int rep_sum = subJs.getInteger(COL_REP_SUM);

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
                obj.put(COL_DETAIL, jsonArray);
                list.add(obj);

                if(type != 6){ // 不是年报
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(COL_MONTH, m);
                    jsonObject.put(COL_SHOWDATE, "合计");
                    jsonObject.put(COL_AREAC, areaC);
                    jsonObject.put(COL_AREAN, areaN);
                    jsonObject.put(COL_MARK_ETM, MARK_ONE);
                    jsonObject.put(COL_MARK_CHAIN, MARK_TWO);
                    jsonObject.put(COL_MARK_OTHER, MARK_THREE);
                    jsonObject.put(COL_MARK_SUM, MARK_SUM);
                    jsonObject.put(COL_REG_ETM, REG_ONE);
                    jsonObject.put(COL_REG_CHAIN, REG_TWO);
                    jsonObject.put(COL_REG_OTHER, REG_THREE);
                    jsonObject.put(COL_REG_SUM, REG_SUM);
                    jsonObject.put(COL_AUTH_ETM, AUTH_ONE);
                    jsonObject.put(COL_AUTH_CHAIN, AUTH_TWO);
                    jsonObject.put(COL_AUTH_OTHER, AUTH_THREE);
                    jsonObject.put(COL_AUTH_SUM, AUTH_SUM);
                    jsonObject.put(COL_ACT_ETM, ACTIVE_ONE);
                    jsonObject.put(COL_ACT_CHAIN, ACTIVE_TWO);
                    jsonObject.put(COL_ACT_OTHER, ACTIVE_THREE);
                    jsonObject.put(COL_ACT_SUM, ACTIVE_SUM);
                    jsonObject.put(COL_REP_ETM, REPURCHASE_ONE);
                    jsonObject.put(COL_REP_CHAIN, REPURCHASE_TWO);
                    jsonObject.put(COL_REP_OTHER, REPURCHASE_THREE);
                    jsonObject.put(COL_REP_SUM, REPURCHASE_SUM);
                    jsonObject.put(COL_ACT_ETM_RATE, calcPercentage(ACTIVE_ONE, AUTH_ONE));
                    jsonObject.put(COL_ACT_CHAIN_RATE, calcPercentage(ACTIVE_TWO, AUTH_TWO));
                    jsonObject.put(COL_ACT_OTHER_RATE, calcPercentage(ACTIVE_THREE, AUTH_THREE));
                    jsonObject.put(COL_ACT_SUM_RATE, calcPercentage(ACTIVE_SUM, AUTH_SUM));
                    jsonObject.put(COL_REP_ETM_RATE, calcPercentage(REPURCHASE_ONE, AUTH_ONE));
                    jsonObject.put(COL_REP_CHAIN_RATE, calcPercentage(REPURCHASE_TWO, AUTH_TWO));
                    jsonObject.put(COL_REP_OTHER_RATE, calcPercentage(REPURCHASE_THREE, AUTH_THREE));
                    jsonObject.put(COL_REP_SUM_RATE, calcPercentage(REPURCHASE_SUM, AUTH_SUM));

                    jsonObject.put(COL_OCC_ETM_RATE,  calcPercentage(REG_ONE, MARK_ONE));
                    jsonObject.put(COL_OCC_CHAIN_RATE,  calcPercentage(REG_TWO, MARK_TWO));
                    jsonObject.put(COL_OCC_OTHER_RATE,  calcPercentage(REG_THREE, MARK_THREE));
                    jsonObject.put(COL_OCC_SUM_RATE,  calcPercentage(REG_SUM, MARK_SUM));
                    obj.put(COL_TOTAL, jsonObject);

                }

                MARK_ONE_TOTAL = MARK_ONE;  MARK_TWO_TOTAL = MARK_TWO; MARK_THREE_TOTAL = MARK_THREE;  MARK_SUM_TOTAL = MARK_SUM;
            }

            List<JSONObject> filterList = new ArrayList<>();
            if(month > 0 && (type == 4 || type == 5)){
                if(list != null && list.size() > 0){
                    for(JSONObject jsonObject : list){
                        boolean isMatch = true;

                        if(jsonObject.containsKey(COL_MONTH)){
                            if(!jsonObject.getString(COL_MONTH).equals(month+"")){
                                isMatch = false;
                            }
                        }
                        if(isMatch){
                            filterList.add(jsonObject);
                        }
                    }
                }
            }else{
                filterList = list;
            }


            resultJson.put(COL_LIST, filterList);
            // 不为累计报表
            if(((type == 0 || type ==2 || type == 4 ) && filterList.size() > 1)  || type == 6){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(COL_SHOWDATE, "合计");
                jsonObject.put(COL_AREAC, areaC);
                jsonObject.put(COL_AREAN, areaN);
                jsonObject.put(COL_MARK_ETM, MARK_ONE_TOTAL);
                jsonObject.put(COL_MARK_CHAIN, MARK_TWO_TOTAL);
                jsonObject.put(COL_MARK_OTHER, MARK_THREE_TOTAL);
                jsonObject.put(COL_MARK_SUM, MARK_SUM_TOTAL);
                jsonObject.put(COL_REG_ETM, REG_ONE_TOTAL);
                jsonObject.put(COL_REG_CHAIN, REG_TWO_TOTAL);
                jsonObject.put(COL_REG_OTHER, REG_THREE_TOTAL);
                jsonObject.put(COL_REG_SUM, REG_SUM_TOTAL);
                jsonObject.put(COL_AUTH_ETM, AUTH_ONE_TOTAL);
                jsonObject.put(COL_AUTH_CHAIN, AUTH_TWO_TOTAL);
                jsonObject.put(COL_AUTH_OTHER, AUTH_THREE_TOTAL);
                jsonObject.put(COL_AUTH_SUM, AUTH_SUM_TOTAL);
                jsonObject.put(COL_ACT_ETM, ACTIVE_ONE_TOTAL);
                jsonObject.put(COL_ACT_CHAIN, ACTIVE_TWO_TOTAL);
                jsonObject.put(COL_ACT_OTHER, ACTIVE_THREE_TOTAL);
                jsonObject.put(COL_ACT_SUM, ACTIVE_SUM_TOTAL);
                jsonObject.put(COL_REP_ETM, REPURCHASE_ONE_TOTAL);
                jsonObject.put(COL_REP_CHAIN, REPURCHASE_TWO_TOTAL);
                jsonObject.put(COL_REP_OTHER, REPURCHASE_THREE_TOTAL);
                jsonObject.put(COL_REP_SUM, REPURCHASE_SUM_TOTAL);
                jsonObject.put(COL_ACT_ETM_RATE, calcPercentage(ACTIVE_ONE_TOTAL, AUTH_ONE_TOTAL));
                jsonObject.put(COL_ACT_CHAIN_RATE, calcPercentage(ACTIVE_TWO_TOTAL, AUTH_TWO_TOTAL));
                jsonObject.put(COL_ACT_OTHER_RATE, calcPercentage(ACTIVE_THREE_TOTAL, AUTH_THREE_TOTAL));
                jsonObject.put(COL_ACT_SUM_RATE, calcPercentage(ACTIVE_SUM_TOTAL, AUTH_SUM_TOTAL));
                jsonObject.put(COL_REP_ETM_RATE, calcPercentage(REPURCHASE_ONE_TOTAL, AUTH_ONE_TOTAL));
                jsonObject.put(COL_REP_CHAIN_RATE, calcPercentage(REPURCHASE_TWO_TOTAL, AUTH_TWO_TOTAL));
                jsonObject.put(COL_REP_OTHER_RATE, calcPercentage(REPURCHASE_THREE_TOTAL, AUTH_THREE_TOTAL));
                jsonObject.put(COL_REP_SUM_RATE, calcPercentage(REPURCHASE_SUM_TOTAL, AUTH_SUM_TOTAL));

                jsonObject.put(COL_OCC_ETM_RATE,  calcPercentage(REG_ONE_TOTAL, MARK_ONE_TOTAL));
                jsonObject.put(COL_OCC_CHAIN_RATE,  calcPercentage(REG_TWO_TOTAL, MARK_TWO_TOTAL));
                jsonObject.put(COL_OCC_OTHER_RATE,  calcPercentage(REG_THREE_TOTAL, MARK_THREE_TOTAL));
                jsonObject.put(COL_OCC_SUM_RATE,  calcPercentage(REG_SUM_TOTAL, MARK_SUM_TOTAL));
                resultJson.put(COL_SUM_TOTAL, jsonObject);
            }

        }
        return resultJson;
    }


    public String exportMarketAnalysisByTime(int year,int month,String areaC,String areaN,int type) {
        JSONObject r = marketAnalysisByTime(year, month, areaC, areaN, type);
        if(r == null){
            return "";
        }

        String str = month > 0 ? (year + "_" + month) : year + "";
        StringBuilder fileName = new StringBuilder(str).append("_").append(areaN);
        if(type == 0) { fileName.append("日报"); }
        else if(type == 1) { fileName.append("日报(累计)"); }
        else if(type == 2) { fileName.append("周报"); }
        else if(type == 3) { fileName.append("周报(累计)"); }
        else if(type == 4) { fileName.append("月报"); }
        else if(type == 5) { fileName.append("月报(累计)"); }
        else  { fileName.append("年报"); }

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
            JSONArray array = r.getJSONArray(COL_LIST);
            for(int i = 0; i < array.size(); i++){
                JSONObject jss = array.getJSONObject(i);
                String showdate = jss.getString(COL_SHOWDATE);
                JSONArray subJsonArray = jss.getJSONArray(COL_DETAIL);
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
                if(jss.containsKey(COL_TOTAL) && jss.getJSONObject(COL_TOTAL).containsKey(COL_AREAN)){
                    JSONObject js = jss.getJSONObject(COL_TOTAL);
                    row = sheet.createRow(k);
                    k++;
                    createExcelDataRow(style, row, js, js.getString(COL_SHOWDATE));
                }
            }

            JSONObject js = r.getJSONObject(COL_SUM_TOTAL);
            if(js != null && js.containsKey(COL_AREAN)){
                row = sheet.createRow(k);
                k++;
                createExcelDataRow(style, row, js, js.getString(COL_SHOWDATE));
            }

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()){
                hwb.write(bos);

                String title = getExcelDownPath(fileName.toString(), new ByteArrayInputStream(bos.toByteArray()));
                return title;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
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
        createCell(style, row, 1, js.getString(COL_AREAN));
        createCell(style, row, 2, js.getString(COL_MARK_ETM));
        createCell(style, row, 3, js.getString(COL_MARK_CHAIN));
        createCell(style, row, 4, js.getString(COL_MARK_OTHER));
        createCell(style, row, 5, js.getString(COL_MARK_SUM));
        createCell(style, row, 6, js.getString(COL_REG_ETM));
        createCell(style, row, 7, js.getString(COL_REG_CHAIN));
        createCell(style, row, 8, js.getString(COL_REG_OTHER));
        createCell(style, row, 9, js.getString(COL_REG_SUM));
        createCell(style, row, 10, js.getString(COL_AUTH_ETM));
        createCell(style, row, 11, js.getString(COL_AUTH_CHAIN));
        createCell(style, row, 12, js.getString(COL_AUTH_OTHER));
        createCell(style, row, 13, js.getString(COL_AUTH_SUM));
        createCell(style, row, 14, js.getString(COL_ACT_ETM));
        createCell(style, row, 15, js.getString(COL_ACT_CHAIN));
        createCell(style, row, 16, js.getString(COL_ACT_OTHER));
        createCell(style, row, 17, js.getString(COL_ACT_SUM));
        createCell(style, row, 18, js.getString(COL_REP_ETM));
        createCell(style, row, 19, js.getString(COL_REP_CHAIN));
        createCell(style, row, 20, js.getString(COL_REP_OTHER));
        createCell(style, row, 21, js.getString(COL_REP_SUM));
        createCell(style, row, 22, js.getString(COL_OCC_ETM_RATE));
        createCell(style, row, 23, js.getString(COL_OCC_CHAIN_RATE));
        createCell(style, row, 24, js.getString(COL_OCC_OTHER_RATE));
        createCell(style, row, 25, js.getString(COL_OCC_SUM_RATE));
        createCell(style, row, 26, js.getString(COL_ACT_ETM_RATE));
        createCell(style, row, 27, js.getString(COL_ACT_CHAIN_RATE));
        createCell(style, row, 28, js.getString(COL_ACT_OTHER_RATE));
        createCell(style, row, 29, js.getString(COL_ACT_SUM_RATE));
        createCell(style, row, 30, js.getString(COL_REP_ETM_RATE));
        createCell(style, row, 31, js.getString(COL_REP_CHAIN_RATE));
        createCell(style, row, 32, js.getString(COL_REP_OTHER_RATE));
        createCell(style, row, 33, js.getString(COL_REP_SUM_RATE));
    }

    private void calcTotal(int type, List<JSONObject> jsonList) {
        int MARK_ONE_TOTAL = 0, MARK_TWO_TOTAL = 0, MARK_THREE_TOTAL = 0,MARK_SUM_TOTAL  = 0;
        int REG_ONE_TOTAL = 0, REG_TWO_TOTAL = 0, REG_THREE_TOTAL = 0,REG_SUM_TOTAL  = 0;
        int AUTH_ONE_TOTAL = 0, AUTH_TWO_TOTAL = 0,AUTH_THREE_TOTAL = 0, AUTH_SUM_TOTAL  = 0;
        int ACTIVE_ONE_TOTAL = 0, ACTIVE_TWO_TOTAL = 0, ACTIVE_THREE_TOTAL = 0,ACTIVE_SUM_TOTAL = 0;
        int REPURCHASE_ONE_TOTAL = 0, REPURCHASE_TWO_TOTAL = 0,REPURCHASE_THREE_TOTAL = 0, REPURCHASE_SUM_TOTAL  = 0;
        // 累计报表
        for(JSONObject js : jsonList) {
//            String a = js.getString(COL_AREAC);
            JSONArray array = js.getJSONArray(COL_DETAIL);

            int mark_etm = js.getInteger(COL_MARK_ETM);
            int mark_chain = js.getInteger(COL_MARK_CHAIN);
            int mark_other = js.getInteger(COL_MARK_OTHER);
            int mark_sum = js.getInteger(COL_MARK_SUM);

            int REG_ONE = 0, REG_TWO = 0, REG_THREE = 0,REG_SUM  = 0;
            int AUTH_ONE = 0, AUTH_TWO = 0,AUTH_THREE = 0, AUTH_SUM  = 0;
            int ACTIVE_ONE = 0, ACTIVE_TWO = 0, ACTIVE_THREE = 0,ACTIVE_SUM = 0;
            int REPURCHASE_ONE = 0, REPURCHASE_TWO = 0,REPURCHASE_THREE = 0, REPURCHASE_SUM  = 0;
            for (int i = 0; i < array.size(); i++) {
                JSONObject subJs = array.getJSONObject(i);

                int reg_etm = subJs.getInteger(COL_REG_ETM);
                int reg_chain = subJs.getInteger(COL_REG_CHAIN);
                int reg_other = subJs.getInteger(COL_REG_OTHER);
                int reg_sum = subJs.getInteger(COL_REG_SUM);

                int auth_etm = subJs.getInteger(COL_AUTH_ETM);
                int auth_chain = subJs.getInteger(COL_REG_CHAIN);
                int auth_other = subJs.getInteger(COL_REG_OTHER);
                int auth_sum = subJs.getInteger(COL_REG_SUM);

                int act_etm = subJs.getInteger(COL_ACT_ETM);
                int act_chain = subJs.getInteger(COL_ACT_CHAIN);
                int act_other = subJs.getInteger(COL_ACT_OTHER);
                int act_sum = subJs.getInteger(COL_ACT_SUM);

                int rep_etm = subJs.getInteger(COL_REP_ETM);
                int rep_chain = subJs.getInteger(COL_REP_CHAIN);
                int rep_other = subJs.getInteger(COL_REP_OTHER);
                int rep_sum = subJs.getInteger(COL_REP_SUM);


                subJs.put(COL_OCC_ETM_RATE,  calcPercentage(reg_etm, mark_etm));
                subJs.put(COL_OCC_CHAIN_RATE,  calcPercentage(reg_chain, mark_chain));
                subJs.put(COL_OCC_OTHER_RATE,  calcPercentage(reg_other, mark_other));
                subJs.put(COL_OCC_SUM_RATE,  calcPercentage(reg_sum, mark_sum));

                if(type == 1 || type == 3 || type == 5){

                    REG_ONE += reg_etm;  REG_TWO += reg_chain; REG_THREE += reg_other;  REG_SUM += reg_sum;

                    subJs.put(COL_REG_ETM, REG_ONE); subJs.put(COL_REG_CHAIN, REG_TWO);
                    subJs.put(COL_REG_OTHER, REG_THREE); subJs.put(COL_REG_SUM, REG_SUM);

                    AUTH_ONE += auth_etm; AUTH_TWO += auth_chain; AUTH_THREE += auth_other; AUTH_SUM += auth_sum;

                    subJs.put(COL_AUTH_ETM, AUTH_ONE); subJs.put(COL_AUTH_CHAIN, AUTH_TWO);
                    subJs.put(COL_AUTH_OTHER, AUTH_THREE); subJs.put(COL_AUTH_SUM, AUTH_SUM);

                    ACTIVE_ONE += act_etm; ACTIVE_TWO += act_chain; ACTIVE_THREE += act_other; ACTIVE_SUM += act_sum;

                    subJs.put(COL_ACT_ETM, ACTIVE_ONE); subJs.put(COL_ACT_CHAIN, ACTIVE_TWO);
                    subJs.put(COL_ACT_OTHER, ACTIVE_THREE); subJs.put(COL_ACT_SUM, ACTIVE_SUM);

                    REPURCHASE_ONE += rep_etm; REPURCHASE_TWO += rep_chain; REPURCHASE_THREE += rep_other; REPURCHASE_SUM += rep_sum;

                    subJs.put(COL_REP_ETM, REPURCHASE_ONE); subJs.put(COL_REP_CHAIN, REPURCHASE_TWO);
                    subJs.put(COL_REP_OTHER, REPURCHASE_THREE); subJs.put(COL_REP_SUM, REPURCHASE_SUM);


                    subJs.put(COL_ACT_ETM_RATE, calcPercentage(ACTIVE_ONE, AUTH_ONE));
                    subJs.put(COL_ACT_CHAIN_RATE, calcPercentage(ACTIVE_TWO, AUTH_TWO));
                    subJs.put(COL_ACT_OTHER_RATE, calcPercentage(ACTIVE_THREE, AUTH_THREE));
                    subJs.put(COL_ACT_SUM_RATE, calcPercentage(ACTIVE_SUM, AUTH_SUM));
                    subJs.put(COL_REP_ETM_RATE, calcPercentage(REPURCHASE_ONE, AUTH_ONE));
                    subJs.put(COL_REP_CHAIN_RATE, calcPercentage(REPURCHASE_TWO, AUTH_TWO));
                    subJs.put(COL_REP_OTHER_RATE, calcPercentage(REPURCHASE_THREE, AUTH_THREE));
                    subJs.put(COL_REP_SUM_RATE, calcPercentage(REPURCHASE_SUM, AUTH_SUM));

                    subJs.put(COL_OCC_ETM_RATE,  calcPercentage(REG_ONE, mark_etm));
                    subJs.put(COL_OCC_CHAIN_RATE,  calcPercentage(REG_TWO, mark_chain));
                    subJs.put(COL_OCC_OTHER_RATE,  calcPercentage(REG_THREE, mark_other));
                    subJs.put(COL_OCC_SUM_RATE,  calcPercentage(REG_SUM, mark_sum));
                }

                REG_ONE_TOTAL += reg_etm;  REG_TWO_TOTAL += reg_chain; REG_THREE_TOTAL += reg_other;  REG_SUM_TOTAL += reg_sum;
                AUTH_ONE_TOTAL += auth_etm; AUTH_TWO_TOTAL += auth_chain; AUTH_THREE_TOTAL += auth_other; AUTH_SUM_TOTAL += auth_sum;
                ACTIVE_ONE_TOTAL += act_etm; ACTIVE_TWO_TOTAL += act_chain; ACTIVE_THREE_TOTAL += act_other; ACTIVE_SUM_TOTAL += act_sum;
                REPURCHASE_ONE_TOTAL += rep_etm; REPURCHASE_TWO_TOTAL += rep_chain; REPURCHASE_THREE_TOTAL += rep_other; REPURCHASE_SUM_TOTAL += rep_sum;
            }

            MARK_ONE_TOTAL += mark_etm;  MARK_TWO_TOTAL += mark_chain; MARK_THREE_TOTAL += mark_other;  MARK_SUM_TOTAL += mark_sum;
        }


        JSONObject js = new JSONObject();
        js.put(COL_AREAC, 0);
        js.put(COL_AREAN, "合计");
        List<JSONObject> subList = new ArrayList<>();
        JSONObject subJS = new JSONObject();
        subJS.put(COL_MARK_ETM, MARK_ONE_TOTAL);
        subJS.put(COL_MARK_CHAIN, MARK_TWO_TOTAL);
        subJS.put(COL_MARK_OTHER, MARK_THREE_TOTAL);
        subJS.put(COL_MARK_SUM, MARK_SUM_TOTAL);
        subJS.put(COL_REG_ETM, REG_ONE_TOTAL);
        subJS.put(COL_REG_CHAIN, REG_TWO_TOTAL);
        subJS.put(COL_REG_OTHER, REG_THREE_TOTAL);
        subJS.put(COL_REG_SUM, REG_SUM_TOTAL);
        subJS.put(COL_AUTH_ETM, AUTH_ONE_TOTAL);
        subJS.put(COL_AUTH_CHAIN, AUTH_TWO_TOTAL);
        subJS.put(COL_AUTH_OTHER, AUTH_THREE_TOTAL);
        subJS.put(COL_AUTH_SUM, AUTH_SUM_TOTAL);
        subJS.put(COL_ACT_ETM, ACTIVE_ONE_TOTAL);
        subJS.put(COL_ACT_CHAIN, ACTIVE_TWO_TOTAL);
        subJS.put(COL_ACT_OTHER, ACTIVE_THREE_TOTAL);
        subJS.put(COL_ACT_SUM, ACTIVE_SUM_TOTAL);
        subJS.put(COL_REP_ETM, REPURCHASE_ONE_TOTAL);
        subJS.put(COL_REP_CHAIN, REPURCHASE_TWO_TOTAL);
        subJS.put(COL_REP_OTHER, REPURCHASE_THREE_TOTAL);
        subJS.put(COL_REP_SUM, REPURCHASE_SUM_TOTAL);
        subJS.put(COL_OCC_ETM_RATE,  calcPercentage(REG_ONE_TOTAL, MARK_ONE_TOTAL));
        subJS.put(COL_OCC_CHAIN_RATE,  calcPercentage(REG_TWO_TOTAL, MARK_TWO_TOTAL));
        subJS.put(COL_OCC_OTHER_RATE,  calcPercentage(REG_THREE_TOTAL, MARK_THREE_TOTAL));
        subJS.put(COL_OCC_SUM_RATE,  calcPercentage(REG_SUM_TOTAL, MARK_SUM_TOTAL));
        subJS.put(COL_ACT_ETM_RATE, calcPercentage(ACTIVE_ONE_TOTAL, AUTH_ONE_TOTAL));
        subJS.put(COL_ACT_CHAIN_RATE, calcPercentage(ACTIVE_TWO_TOTAL, AUTH_TWO_TOTAL));
        subJS.put(COL_ACT_OTHER_RATE, calcPercentage(ACTIVE_THREE_TOTAL, AUTH_THREE_TOTAL));
        subJS.put(COL_ACT_SUM_RATE, calcPercentage(ACTIVE_SUM_TOTAL, AUTH_SUM_TOTAL));
        subJS.put(COL_REP_ETM_RATE, calcPercentage(REPURCHASE_ONE_TOTAL, AUTH_ONE_TOTAL));
        subJS.put(COL_REP_CHAIN_RATE, calcPercentage(REPURCHASE_TWO_TOTAL, AUTH_TWO_TOTAL));
        subJS.put(COL_REP_OTHER_RATE, calcPercentage(REPURCHASE_THREE_TOTAL, AUTH_THREE_TOTAL));
        subJS.put(COL_REP_SUM_RATE, calcPercentage(REPURCHASE_SUM_TOTAL, AUTH_SUM_TOTAL));
        subList.add(subJS);
        js.put(COL_DETAIL, subList);
        jsonList.add(js);
    }

    private void initTableData(int year, int month, int type, List<String> codeList, Map<String, String> areaMap, Calendar cale, List<JSONObject> jsonList) {
        if(type == 0 || type == 1){
            cale.set(Calendar.YEAR, year);
            int maxMonth = cale.get(Calendar.MONTH) + 1;
            int maxDate = 0;
            if(month < maxMonth){
                cale.set(Calendar.MONTH, month - 1);
                maxDate = cale.getActualMaximum(Calendar.DAY_OF_MONTH);
            }else{
                cale.set(Calendar.MONTH, month - 1);
                maxDate = cale.get(Calendar.DATE);
            }

            //int maxDate = cale.get(Calendar.DAY_OF_MONTH);
            String m = month < 10 ?  "0" + month : month +"";
            for(String c : codeList){
                JSONObject js = new JSONObject();
                js.put(COL_AREAC, c);
                js.put(COL_AREAN, areaMap.get(c));
                int m1 = MarketStoreData.getEtmByAreac(c);
                int m2 = MarketStoreData.getChainByAreac(c);
                int m3 = MarketStoreData.getOtherByAreac(c);
                js.put(COL_MARK_ETM, m1);
                js.put(COL_MARK_CHAIN, m2);
                js.put(COL_MARK_OTHER, m3);
                js.put(COL_MARK_SUM, m1 + m2 + m3);
                List<JSONObject> subList = new ArrayList<>();
                for(int i = 1; i <= maxDate; i++){
                    String d = i < 10 ?  "0" + i : i +"";
                    JSONObject subJS = new JSONObject();
                    subJS.put(COL_MONTH, month);
                    subJS.put(COL_DATE, year + "-" + m + "-" + d);
                    subJS.put(COL_FIRST, i == 1 ? 1 : 0);
                    subJS.put(COL_SHOWDATE, i + "号");
                    generateDetailJSON(subJS);
                    subList.add(subJS);
                }
                js.put(COL_DETAIL, subList);
                jsonList.add(js);
            }

        }else if(type == 2 || type == 3){
            cale.set(Calendar.YEAR, year);
            cale.set(Calendar.MONTH, month - 1);
            Date first = firstMonthDate(cale.getTime());
            Map<Integer, MarketAnalysisServiceImpl.WeekRange> maps = new HashMap<Integer, MarketAnalysisServiceImpl.WeekRange>();
            getWeekBeginAndEnd(1, first, maps);

            Set<Integer> set = maps.keySet();
            for(String c : codeList){
                JSONObject js = new JSONObject();
                js.put(COL_AREAC, c);
                js.put(COL_AREAN, areaMap.get(c));
                int m1 = MarketStoreData.getEtmByAreac(c);
                int m2 = MarketStoreData.getChainByAreac(c);
                int m3 = MarketStoreData.getOtherByAreac(c);
                js.put(COL_MARK_ETM, m1);
                js.put(COL_MARK_CHAIN, m2);
                js.put(COL_MARK_OTHER, m3);
                js.put(COL_MARK_SUM, m1 + m2 + m3);
                List<JSONObject> subList = new ArrayList<>();
                for(int i = 1; i <= set.size(); i++){
                    WeekRange weekRange = maps.get(i);
                    JSONObject subJS = new JSONObject();
                    subJS.put(COL_MONTH, month);
                    subJS.put(COL_BEGINDATE, DATEFORMAT.format(weekRange.getBegin()));
                    subJS.put(COL_ENDDATE, DATEFORMAT.format(weekRange.getEnd()));
                    subJS.put(COL_FIRST, i == 1 ? 1 : 0);
                    subJS.put(COL_SHOWDATE, i + "周");
                    generateDetailJSON(subJS);
                    subList.add(subJS);
                }
                js.put(COL_DETAIL, subList);
                jsonList.add(js);
            }

        }else if(type == 4 || type == 5){
            cale.set(Calendar.YEAR, year);
            int maxMonth = cale.get(Calendar.MONTH) + 1;
            for(String c : codeList){
                JSONObject js = new JSONObject();
                js.put(COL_AREAC, c);
                js.put(COL_AREAN, areaMap.get(c));
                int m1 = MarketStoreData.getEtmByAreac(c);
                int m2 = MarketStoreData.getChainByAreac(c);
                int m3 = MarketStoreData.getOtherByAreac(c);
                js.put(COL_MARK_ETM, m1);
                js.put(COL_MARK_CHAIN, m2);
                js.put(COL_MARK_OTHER, m3);
                js.put(COL_MARK_SUM, m1 + m2 + m3);
                List<JSONObject> subList = new ArrayList<>();
                for(int i = 1; i <= maxMonth; i++){
                    JSONObject subJS = new JSONObject();
                    subJS.put(COL_FIRST, i == 1 ? 1 : 0);
                    subJS.put(COL_YEAR, year);
                    subJS.put(COL_MONTH, i);
                    subJS.put(COL_SHOWDATE, i + "月");
                    generateDetailJSON(subJS);
                    subList.add(subJS);
                }
                js.put(COL_DETAIL, subList);
                jsonList.add(js);
            }

        }else if(type == 6){
            for(String c : codeList){
                JSONObject js = new JSONObject();
                js.put(COL_AREAC, c);
                js.put(COL_AREAN, areaMap.get(c));
                int m1 = MarketStoreData.getEtmByAreac(c);
                int m2 = MarketStoreData.getChainByAreac(c);
                int m3 = MarketStoreData.getOtherByAreac(c);
                js.put(COL_MARK_ETM, m1);
                js.put(COL_MARK_CHAIN, m2);
                js.put(COL_MARK_OTHER, m3);
                js.put(COL_MARK_SUM, m1 + m2 + m3);

                List<JSONObject> subList = new ArrayList<>();
                JSONObject subJS = new JSONObject();
                subJS.put(COL_FIRST, 1);
                subJS.put(COL_YEAR, year);
                subJS.put(COL_SHOWDATE,  year + "年");
                generateDetailJSON(subJS);
                subList.add(subJS);

                js.put(COL_DETAIL, subList);
                jsonList.add(js);
            }
        }
    }

    /**
     * 生成初始化JSON数据
     */
    private void generateDetailJSON(JSONObject subJS) {
        subJS.put(COL_REG_ETM, "0");
        subJS.put(COL_REG_CHAIN, "0");
        subJS.put(COL_REG_OTHER, "0");
        subJS.put(COL_REG_SUM, "0");
        subJS.put(COL_AUTH_ETM, "0");
        subJS.put(COL_AUTH_CHAIN, "0");
        subJS.put(COL_AUTH_OTHER, "0");
        subJS.put(COL_AUTH_SUM, "0");
        subJS.put(COL_ACT_ETM, "0");
        subJS.put(COL_ACT_CHAIN, "0");
        subJS.put(COL_ACT_OTHER, "0");
        subJS.put(COL_ACT_SUM, "0");
        subJS.put(COL_REP_ETM, "0");
        subJS.put(COL_REP_CHAIN, "0");
        subJS.put(COL_REP_OTHER, "0");
        subJS.put(COL_REP_SUM, "0");
        subJS.put(COL_OCC_ETM_RATE, "0");
        subJS.put(COL_OCC_CHAIN_RATE, "0");
        subJS.put(COL_OCC_OTHER_RATE, "0");
        subJS.put(COL_OCC_SUM_RATE, "0");
        subJS.put(COL_ACT_ETM_RATE, "0");
        subJS.put(COL_ACT_CHAIN_RATE, "0");
        subJS.put(COL_ACT_OTHER_RATE, "0");
        subJS.put(COL_ACT_SUM_RATE, "0");
        subJS.put(COL_REP_ETM_RATE, "0");
        subJS.put(COL_REP_CHAIN_RATE, "0");
        subJS.put(COL_REP_OTHER_RATE, "0");
        subJS.put(COL_REP_SUM_RATE, "0");
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
    public static void getWeekBeginAndEnd(int index,Date currentDate,Map<Integer, WeekRange> maps){
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

    private class GetDbData {
        private int year;
        private int month;
        private int type;
        private String areac;
        private List<String[]> list;
        private List<String[]> orderList;

        public GetDbData(int year, int month, int type, String _areac) {
            this.year = year;
            this.month = month;
            this.type = type;
            areac = _areac;
        }

        public List<String[]> getList() {
            return list;
        }

        public List<String[]> getOrderList() {
            return orderList;
        }

        public GetDbData invoke() {
            List<Object> paramCompList = new ArrayList<Object>();
            StringBuilder compSql = new StringBuilder(COMP_SQL);
            paramCompList.add(areac + "%");

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
            list = new ArrayList<>();
            if(queryList != null && queryList.size() > 0){
                for(Object[] obj : queryList){
                    comList.add(Integer.parseInt(obj[0].toString()));
                    list.add(new String[]{obj[0].toString(), obj[1].toString(), obj[2].toString(), obj[3].toString(), obj[4].toString(), obj[5].toString(), obj[6].toString(), obj[7].toString()});
                }
            }

            orderSql.append(" group by cusno,odate");
            List<Object[]> orderQueryList = BASE_DAO.queryNativeSharding(0, year, orderSql.toString(), paramOrderList.toArray());
            orderList = new ArrayList<>();
            if(orderQueryList != null && orderQueryList.size() > 0){
                for(Object[] obj : orderQueryList){
                    int compid = Integer.parseInt(obj[0].toString());
                    if(!comList.contains(compid)){
                        continue;
                    }
                    orderList.add(new String[]{obj[0].toString(), obj[1].toString(), obj[2].toString(), obj[3].toString(), obj[4].toString()});
                }
            }
            return this;
        }
    }
}
