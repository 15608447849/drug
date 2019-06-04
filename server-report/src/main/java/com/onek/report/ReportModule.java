package com.onek.report;

import IceInternal.Ex;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import constant.DSMConst;
import dao.BaseDAO;

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

    private static String SQL = "select cid,storetype,caddrcode,createdate,year(createdate),month(createdate),sum(regnum),sum(authnum) from ( " +
            "select cid,storetype,substr(caddrcode,1,6) as caddrcode,createdate,1 as regnum,0 as authnum from {{?"+ DSMConst.TB_COMP +"}} where ctype = 0 " +
            "union all " +
            "select cid,storetype,substr(caddrcode,1,6) as caddrcode,createdate,0 as regnum,1 as authnum from {{?"+ DSMConst.TB_COMP +"}} where ctype = 0 and cstatus&256 > 0 " +
            ") tab where caddrcode like ? ";

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
                for(int i = 1; i <= maxDate; i++){
                    String d = i < 10 ?  "0" + i : i +"";
                    JSONObject js = new JSONObject();
                    js.put("areac", c);
                    js.put("arean", areaMap.get(c));
                    js.put("date", year + "-" + m + "-" + d);
                    js.put("first", i == 1 ? 1 : 0);
                    js.put("showdate", i + "号");
                    js.put("mark_etmonomer", "0");
                    js.put("mark_chain", "0");
                    js.put("mark_other", "0");
                    js.put("mark_sum", "0");
                    js.put("reg_etmonomer", "0");
                    js.put("reg_chain", "0");
                    js.put("reg_other", "0");
                    js.put("reg_sum", "0");
                    js.put("auth_etmonomer", "0");
                    js.put("auth_chain", "0");
                    js.put("auth_other", "0");
                    js.put("auth_sum", "0");
                    js.put("active_etmonomer", "0");
                    js.put("active_chain", "0");
                    js.put("active_other", "0");
                    js.put("active_sum", "0");
                    js.put("repurchase_etmonomer", "0");
                    js.put("repurchase_chain", "0");
                    js.put("repurchase_other", "0");
                    js.put("repurchase_sum", "0");
                    js.put("occupancy_etmonomer", "0");
                    js.put("occupancy_chain", "0");
                    js.put("occupancy_other", "0");
                    js.put("occupancy_sum", "0");
                    js.put("activerate_etmonomer", "0");
                    js.put("activerate_chain", "0");
                    js.put("activerate_other", "0");
                    js.put("activerate_sum", "0");
                    js.put("repurchaserate_etmonomer", "0");
                    js.put("repurchaserate_chain", "0");
                    js.put("repurchaserate_other", "0");
                    js.put("repurchaserate_sum", "0");
                    jsonList.add(js);
                }
            }

        }else if(type == 2 || type == 3){
            cale.set(Calendar.YEAR, year);
            cale.set(Calendar.MONTH, month - 1);
            Date first = firstMonthDate(cale.getTime());
            Map<Integer,WeekRange> maps = new HashMap<Integer, WeekRange>();
            getWeekBeginAndEnd(1, first,maps);

            Set<Integer> set = maps.keySet();
            for(String c : codeList){
                for(int i = 1; i <= set.size(); i++){
                    WeekRange weekRange = maps.get(i);
                    JSONObject js = new JSONObject();
                    js.put("areac", c);
                    js.put("arean", areaMap.get(c));
                    js.put("begindate", datafFormat.format(weekRange.getBegin()));
                    js.put("enddate", datafFormat.format(weekRange.getEnd()));
                    js.put("first", i == 1 ? 1 : 0);
                    js.put("showdate", i + "周");
                    js.put("mark_etmonomer", "0");
                    js.put("mark_chain", "0");
                    js.put("mark_other", "0");
                    js.put("mark_sum", "0");
                    js.put("reg_etmonomer", "0");
                    js.put("reg_chain", "0");
                    js.put("reg_other", "0");
                    js.put("reg_sum", "0");
                    js.put("auth_etmonomer", "0");
                    js.put("auth_chain", "0");
                    js.put("auth_other", "0");
                    js.put("auth_sum", "0");
                    js.put("active_etmonomer", "0");
                    js.put("active_chain", "0");
                    js.put("active_other", "0");
                    js.put("active_sum", "0");
                    js.put("repurchase_etmonomer", "0");
                    js.put("repurchase_chain", "0");
                    js.put("repurchase_other", "0");
                    js.put("repurchase_sum", "0");
                    js.put("occupancy_etmonomer", "0");
                    js.put("occupancy_chain", "0");
                    js.put("occupancy_other", "0");
                    js.put("occupancy_sum", "0");
                    js.put("activerate_etmonomer", "0");
                    js.put("activerate_chain", "0");
                    js.put("activerate_other", "0");
                    js.put("activerate_sum", "0");
                    js.put("repurchaserate_etmonomer", "0");
                    js.put("repurchaserate_chain", "0");
                    js.put("repurchaserate_other", "0");
                    js.put("repurchaserate_sum", "0");
                    jsonList.add(js);
                }
            }

        }else if(type == 4 || type == 5){
            for(String c : codeList){
                for(int i = 1; i <= 12; i++){
                    JSONObject js = new JSONObject();
                    js.put("areac", c);
                    js.put("arean", areaMap.get(c));
                    js.put("first", i == 1 ? 1 : 0);
                    js.put("year", year);
                    js.put("month", i);
                    js.put("showdate", i + "月");
                    js.put("mark_etmonomer", "0");
                    js.put("mark_chain", "0");
                    js.put("mark_other", "0");
                    js.put("mark_sum", "0");
                    js.put("reg_etmonomer", "0");
                    js.put("reg_chain", "0");
                    js.put("reg_other", "0");
                    js.put("reg_sum", "0");
                    js.put("auth_etmonomer", "0");
                    js.put("auth_chain", "0");
                    js.put("auth_other", "0");
                    js.put("auth_sum", "0");
                    js.put("active_etmonomer", "0");
                    js.put("active_chain", "0");
                    js.put("active_other", "0");
                    js.put("active_sum", "0");
                    js.put("repurchase_etmonomer", "0");
                    js.put("repurchase_chain", "0");
                    js.put("repurchase_other", "0");
                    js.put("repurchase_sum", "0");
                    js.put("occupancy_etmonomer", "0");
                    js.put("occupancy_chain", "0");
                    js.put("occupancy_other", "0");
                    js.put("occupancy_sum", "0");
                    js.put("activerate_etmonomer", "0");
                    js.put("activerate_chain", "0");
                    js.put("activerate_other", "0");
                    js.put("activerate_sum", "0");
                    js.put("repurchaserate_etmonomer", "0");
                    js.put("repurchaserate_chain", "0");
                    js.put("repurchaserate_other", "0");
                    js.put("repurchaserate_sum", "0");
                    jsonList.add(js);
                }
            }

        }else if(type == 6){
            for(String c : codeList){
                JSONObject js = new JSONObject();
                js.put("areac", c);
                js.put("arean", areaMap.get(c));
                js.put("first", 1);
                js.put("year", year);
                js.put("showdate", year + "年");
                js.put("mark_etmonomer", "0");
                js.put("mark_chain", "0");
                js.put("mark_other", "0");
                js.put("mark_sum", "0");
                js.put("reg_etmonomer", "0");
                js.put("reg_chain", "0");
                js.put("reg_other", "0");
                js.put("reg_sum", "0");
                js.put("auth_etmonomer", "0");
                js.put("auth_chain", "0");
                js.put("auth_other", "0");
                js.put("auth_sum", "0");
                js.put("active_etmonomer", "0");
                js.put("active_chain", "0");
                js.put("active_other", "0");
                js.put("active_sum", "0");
                js.put("repurchase_etmonomer", "0");
                js.put("repurchase_chain", "0");
                js.put("repurchase_other", "0");
                js.put("repurchase_sum", "0");
                js.put("occupancy_etmonomer", "0");
                js.put("occupancy_chain", "0");
                js.put("occupancy_other", "0");
                js.put("occupancy_sum", "0");
                js.put("activerate_etmonomer", "0");
                js.put("activerate_chain", "0");
                js.put("activerate_other", "0");
                js.put("activerate_sum", "0");
                js.put("repurchaserate_etmonomer", "0");
                js.put("repurchaserate_chain", "0");
                js.put("repurchaserate_other", "0");
                js.put("repurchaserate_sum", "0");
                jsonList.add(js);
            }
        }

        List<Object> paramList = new ArrayList<Object>();
        StringBuilder sql = new StringBuilder(SQL);
        paramList.add(_areac + "%");
        if(type == 0){ // 天报(单天)
            sql.append(" and year(tab.createdate) = ? and month(tab.createdate) = ? ");
            paramList.add(year);
            paramList.add(month);
        }else if(type == 1){ // 天报(累计)

        }else if(type == 2){ // 周报(单周)
            sql.append(" and year(tab.createdate) = ? and month(tab.createdate) = ? ");
            paramList.add(year);
            paramList.add(month);
        }else if(type == 3){ // 周报(累计)

        }else if(type == 4){ // 月报(单月)
            sql.append(" and year(tab.createdate) = ? ");
            paramList.add(year);
        }else if(type == 5){ // 月报(累计)

        }else if(type == 6){ // 年报
            sql.append(" and year(tab.createdate) = ? ");
            paramList.add(year);
        }
        sql.append(" group by createdate,caddrcode,cid,storetype order by caddrcode asc,createdate asc");
        List<Object[]> queryList = BASE_DAO.queryNative(sql.toString(), paramList.toArray());
        List<Integer> comList = new ArrayList<>();
        List<String[]> list = new ArrayList<>();
        if(queryList != null && queryList.size() > 0){
            for(Object[] obj : queryList){
                comList.add(Integer.parseInt(obj[0].toString()));
                list.add(new String[]{obj[1].toString(), obj[2].toString(), obj[3].toString(), obj[4].toString(), obj[5].toString(), obj[6].toString(), obj[7].toString()});
            }
        }

        if(list != null && list.size() > 0){
             for(JSONObject js : jsonList){
                 String a = js.getString("areac");
                 int first = js.getInteger("first");
                 int regnum1 = 0;
                 int regnum2 = 0;
                 int regnum3 = 0;
                 int sumregnum = 0;

                 int authnum1 = 0;
                 int authnum2 = 0;
                 int authnum3 = 0;
                 int sumauthnum = 0;
                 for(String[] arr : list){
                    String storetype = arr[0].toString();
                    String storeAreac = arr[1].toString();
                    String _date = arr[2].toString();
                    int _year = Integer.parseInt(arr[3].toString());
                    int _month = Integer.parseInt(arr[4].toString());
                    int regnum = Integer.parseInt(arr[5].toString());
                    int authnum = Integer.parseInt(arr[6].toString());
                    if(isProvice){
                        if(!a.substring(0,4).equals(storeAreac.substring(0,4))){ // 匹配地区
                            continue;
                        }
                    }else if(isCity){
                        if(!a.substring(0,6).equals(storeAreac.substring(0,6))){ // 匹配地区
                            continue;
                        }
                    }
                     if(type == 0){ // 天报(单天)
                         String d = js.getString("date");
                         if(!d.equals(_date)){
                             continue;
                         }
                     }else if(type == 1){ // 天报(累计)
                         String d = js.getString("date");
                         int val = compareDate(_date, d);
                         if(val == -1 && first == 1){}
                         else if(val == 0){ }
                         else{
                             continue;
                         }
                     }else if(type == 2){ // 周报(单周)
                         String begindate = js.getString("begindate");
                         String enddate = js.getString("enddate");
                         try{
                             if(!dateBetweenRange(_date, begindate, enddate)){
                                 continue;
                             }
                         }catch (Exception e){
                             e.printStackTrace();
                         }
                     }else if(type == 3){ // 周报(累计)
                         String begindate = js.getString("begindate");
                         String enddate = js.getString("enddate");
                         try{
                             if(compareDate(_date, begindate) == -1 && first == 1){ }
                             else if(dateBetweenRange(_date, begindate, enddate)){}
                             else{
                                 continue;
                             }
                         }catch (Exception e){
                             e.printStackTrace();
                         }
                     }else if(type == 4){ // 月报(单月)
                         int m = js.getInteger("month");
                         int y = js.getInteger("year");
                         if(y != _year || m != _month){
                             continue;
                         }
                     }else if(type == 5){ // 月报(累计)
                         int m = js.getInteger("month");
                         int y = js.getInteger("year");
                         if(_year < y && first == 1){ }
                         else if(_year == y && _month == m){}
                         else{
                             continue;
                         }
                     }else if(type == 6){
                         //
                     }
                     System.out.println("######### 368 line ["+storeAreac+"]["+storetype+"]["+_date+"]["+regnum+"] ######");
                     if(storetype.equals("0")) regnum1 = regnum1 + regnum;
                     if(storetype.equals("1")) regnum2 = regnum2 + regnum;
                     if(storetype.equals("-1")) regnum3 = regnum3 + regnum;
                     sumregnum = sumregnum + regnum;

                     if(storetype.equals("0")) authnum1 = authnum1 + authnum;
                     if(storetype.equals("1")) authnum2 = authnum2 + authnum;
                     if(storetype.equals("-1")) authnum3 = authnum3 + authnum;
                     sumauthnum= sumauthnum + authnum;
                     System.out.println("######### 368 line ["+sumregnum+"]["+sumauthnum+"]["+_date+"]["+regnum+"] ######");

                 }

                 js.put("reg_etmonomer", regnum1);
                 js.put("reg_chain", regnum2);
                 js.put("reg_other", regnum3);
                 js.put("reg_sum", sumregnum);
                 js.put("auth_etmonomer", authnum1);
                 js.put("auth_chain", authnum2);
                 js.put("auth_other", authnum3);
                 js.put("auth_sum", sumauthnum);

             }
        }
        return new Result().success(jsonList);
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
