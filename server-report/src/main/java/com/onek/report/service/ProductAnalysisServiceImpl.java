package com.onek.report.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.onek.report.data.MarketStoreData;
import com.onek.report.data.SystemConfigData;
import constant.DSMConst;
import dao.BaseDAO;

import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.util.CellRangeAddress;
import org.hyrdpf.util.LogUtil;
import util.MathUtil;
import util.NumUtil;
import util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import static com.onek.util.fs.FileServerUtils.getExcelDownPath;

/**
 * 商品分析报表实现
 */
public class ProductAnalysisServiceImpl {

    private static final BaseDAO BASE_DAO = BaseDAO.getBaseDAO();

    private static DateFormat DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd");

    //SKU新增查询
    private static final String SKU_NEW_SQL = "select pc.classid cid,count(sku.sku) nskucnt," +
            "YEAR(creatdate) years,MONTH(creatdate) mons,creatdate days," +
            "((DAY(creatdate) + WEEKDAY(creatdate - INTERVAL DAY(creatdate) DAY)) DIV 7 + 1 ) weeks " +
            "from {{?"+DSMConst.TD_PRODUCE_CLASS +"}} pc join {{?"+DSMConst.TD_PROD_SKU+"}}  " +
            " sku on substring(sku.sku, 2) REGEXP CONCAT('^',pc.classid) " +
            " group by pc.classid ";

    //SKU售罄月年查询
    private static final String SKU_SOLD_MON_SQL = "select classid cid,classname,count(sku) solds,0 days,0 weeks,mons,years from {{?"+ DSMConst.TD_PRODUCE_CLASS +"}} pc join (" +
            " select sku,month(CURRENT_DATE) mons,year(CURRENT_DATE) years from {{?"+DSMConst.TD_PROD_SKU+"}} where store = 0 " +
            " UNION " +
            " select sku,mons,years from {{?"+DSMConst.TP_REPORT_MONSTOCK +"}} where stock = 0) b " +
            " on substring(b.sku, 2) REGEXP CONCAT('^',pc.classid)  group by pc.classid ";

    //SKU售罄天查询
    private static final String SKU_SOLD_DAY_SQL = "select classid cid,classname,count(sku) solds,days,weeks,mons,years from {{?"+ DSMConst.TD_PRODUCE_CLASS +"}} pc join (" +
            " select sku,CURRENT_DATE days," +
            "((DAY(CURRENT_DATE) + WEEKDAY(CURRENT_DATE - INTERVAL DAY(CURRENT_DATE) DAY)) DIV 7 + 1 ) weeks,MONTH(CURRENT_DATE) mons,YEAR(CURRENT_DATE) years " +
            " from {{?"+DSMConst.TD_PROD_SKU+"}} where store = 0 " +
            " UNION  " +
            " select sku,optdate days, " +
            " ((DAY(optdate) + WEEKDAY(optdate - INTERVAL DAY(optdate) DAY)) DIV 7 + 1 ) weeks,MONTH(optdate) mons,YEAR(optdate) years  " +
            " from {{?"+DSMConst.TP_REPORT_DAYSTOCK +"}} where store = 0) b " +
            " on substring(b.sku, 2) REGEXP CONCAT('^',pc.classid)  group by pc.classid ";


    //查询SKU天库存量
    private static final String SKU_STORE_DAY_SQL = "select classid cid,classname,sum(store) store,max(store) maxstore,min(store) minstore," +
            "days,weeks,mons,years from {{?"+ DSMConst.TD_PRODUCE_CLASS +"}} pc join (" +
            "select sku,store,CURRENT_DATE days," +
            "((DAY(CURRENT_DATE) + WEEKDAY(CURRENT_DATE - INTERVAL DAY(CURRENT_DATE) DAY)) DIV 7 + 1 ) weeks,MONTH(CURRENT_DATE) mons,YEAR(CURRENT_DATE) years from {{?"+DSMConst.TD_PROD_SKU+"}} " +
            " UNION " +
            " select sku,store,optdate days," +
            " ((DAY(optdate) + WEEKDAY(optdate - INTERVAL DAY(optdate) DAY)) DIV 7 + 1 ) weeks,MONTH(optdate) mons,YEAR(optdate) years from {{?"+DSMConst.TP_REPORT_DAYSTOCK +"}}) b " +
            " on substring(b.sku, 2) REGEXP CONCAT('^',pc.classid)  group by pc.classid ";


    //查询SKU月库存量
    private static final String SKU_STORE_MON_SQL = "select classid cid,classname,sum(store) store,max(store) maxstore," +
            "min(store) minstore,0 days,0 weeks,mons,years from {{?"+ DSMConst.TD_PRODUCE_CLASS +"}} pc join (" +
            " select sku,store,month(CURRENT_DATE) mons,year(CURRENT_DATE) years from {{?"+DSMConst.TD_PROD_SKU+"}} " +
            " UNION " +
            " select sku,stock store,mons,years from {{?"+DSMConst.TP_REPORT_MONSTOCK +"}}) b " +
            " on substring(b.sku, 2) REGEXP CONCAT('^',pc.classid)  group by pc.classid ";

    //查询周销量
    private static final String SKU_SALE_WEEK_SQL = "select cid,soldsku," +
            " skunum,maxskunum,minskunum,skuamt,maxskuamt,minskuamt," +
            " (select sum(skunum)  from {{?"+DSMConst.V_PD_SALES_WEEKS +"}} where cid = b.cid and days <= b.days) hskunum," +
            " (select max(skunum)  from {{?"+DSMConst.V_PD_SALES_WEEKS +"}} where cid = b.cid and days <= b.days) hmaxskunum," +
            " (select min(skunum)  from {{?"+DSMConst.V_PD_SALES_WEEKS +"}} where cid = b.cid and days <= b.days) hminskunum," +
            " (select sum(skuamt)  from {{?"+DSMConst.V_PD_SALES_WEEKS +"}} where cid = b.cid and days <= b.days) hskuamt," +
            " (select max(skuamt)  from {{?"+DSMConst.V_PD_SALES_WEEKS +"}} where cid = b.cid and days <= b.days) hmaxskuamt," +
            " (select min(skuamt)  from {{?"+DSMConst.V_PD_SALES_WEEKS +"}} where cid = b.cid and days <= b.days) hminskuamt," +
            " (select sum(soldsku)  from {{?"+DSMConst.V_PD_SALES_WEEKS +"}} where cid = b.cid and days <= b.days) hsoldsku," +
            " years,mons,days,weeks from {{?"+DSMConst.V_PD_SALES_WEEKS +"}} b ";


    //查询日销量
    private static final String SKU_SALE_DAY_SQL = "select cid,soldsku," +
            " skunum,maxskunum,minskunum,skuamt,maxskuamt,minskuamt," +
            " (select sum(skunum)  from {{?"+DSMConst.V_PD_SALES_DAYS +"}} where cid = b.cid and days <= b.days) hskunum," +
            " (select max(skunum)  from {{?"+DSMConst.V_PD_SALES_DAYS +"}} where cid = b.cid and days <= b.days) hmaxskunum," +
            " (select min(skunum)  from {{?"+DSMConst.V_PD_SALES_DAYS +"}} where cid = b.cid and days <= b.days) hminskunum," +
            " (select sum(skuamt)  from {{?"+DSMConst.V_PD_SALES_DAYS +"}} where cid = b.cid and days <= b.days) hskuamt," +
            " (select max(skuamt)  from {{?"+DSMConst.V_PD_SALES_DAYS +"}} where cid = b.cid and days <= b.days) hmaxskuamt," +
            " (select min(skuamt)  from {{?"+DSMConst.V_PD_SALES_DAYS +"}} where cid = b.cid and days <= b.days) hminskuamt," +
            " (select sum(soldsku)  from {{?"+DSMConst.V_PD_SALES_DAYS +"}} where cid = b.cid and days <= b.days) hsoldsku," +
            " years,mons,days,weeks from {{?"+DSMConst.V_PD_SALES_DAYS +"}} b ";


    //查询月销量
    private static final String SKU_SALE_MON_SQL = "select cid,soldsku," +
            " skunum,maxskunum,minskunum,skuamt,maxskuamt,minskuamt," +
            " (select sum(skunum)  from {{?"+DSMConst.V_PD_SALES_MONS +"}} where cid = b.cid and days <= b.days) hskunum," +
            " (select max(skunum)  from {{?"+DSMConst.V_PD_SALES_MONS +"}} where cid = b.cid and days <= b.days) hmaxskunum," +
            " (select min(skunum)  from {{?"+DSMConst.V_PD_SALES_MONS +"}} where cid = b.cid and days <= b.days) hminskunum," +
            " (select sum(skuamt)  from {{?"+DSMConst.V_PD_SALES_MONS +"}} where cid = b.cid and days <= b.days) hskuamt," +
            " (select max(skuamt)  from {{?"+DSMConst.V_PD_SALES_MONS +"}} where cid = b.cid and days <= b.days) hmaxskuamt," +
            " (select min(skuamt)  from {{?"+DSMConst.V_PD_SALES_MONS +"}} where cid = b.cid and days <= b.days) hminskuamt," +
            " (select sum(soldsku)  from {{?"+DSMConst.V_PD_SALES_MONS +"}} where cid = b.cid and days <= b.days) hsoldsku," +
            " years,mons,days,weeks from {{?"+DSMConst.V_PD_SALES_MONS +"}} b ";


    //年销量
    private static final String SKU_SALE_YEAR_SQL = "select cid,soldsku," +
            " skunum,maxskunum,minskunum,skuamt,maxskuamt,minskuamt," +
            " (select sum(skunum)  from {{?"+DSMConst.V_PD_SALES_YEARS +"}} where cid = b.cid and days <= b.days) hskunum," +
            " (select max(skunum)  from {{?"+DSMConst.V_PD_SALES_YEARS +"}} where cid = b.cid and days <= b.days) hmaxskunum," +
            " (select min(skunum)  from {{?"+DSMConst.V_PD_SALES_YEARS +"}} where cid = b.cid and days <= b.days) hminskunum," +
            " (select sum(skuamt)  from {{?"+DSMConst.V_PD_SALES_YEARS +"}} where cid = b.cid and days <= b.days) hskuamt," +
            " (select max(skuamt)  from {{?"+DSMConst.V_PD_SALES_YEARS +"}} where cid = b.cid and days <= b.days) hmaxskuamt," +
            " (select min(skuamt)  from {{?"+DSMConst.V_PD_SALES_YEARS +"}} where cid = b.cid and days <= b.days) hminskuamt," +
            " (select sum(soldsku)  from {{?"+DSMConst.V_PD_SALES_YEARS +"}} where cid = b.cid and days <= b.days) hsoldsku," +
            " years,mons,days,weeks from {{?"+DSMConst.V_PD_SALES_YEARS +"}} b ";


    private static final String SKU_NEW_DAY_SQL = "select cid,nskucnt," +
            " (select sum(nskucnt)  from {{?"+DSMConst.V_SKU_NEW_DAY +"}} where cid = b.cid and days <= b.days) hnskucnt," +
            " years,mons,days,weeks from {{?"+DSMConst.V_SKU_NEW_DAY +"}} b ";

    private static final String SKU_NEW_MON_SQL = "select cid,nskucnt," +
            " (select sum(nskucnt)  from {{?"+DSMConst.V_SKU_NEW_MONS +"}} where cid = b.cid and days <= b.days) hnskucnt," +
            " years,mons,days,weeks from {{?"+DSMConst.V_SKU_NEW_MONS +"}} b ";

    private static final String SKU_NEW_WEEK_SQL = "select cid,nskucnt," +
            " (select sum(nskucnt)  from {{?"+DSMConst.V_SKU_NEW_WEEKS +"}} where cid = b.cid and days <= b.days) hnskucnt," +
            " years,mons,days,weeks from {{?"+DSMConst.V_SKU_NEW_WEEKS +"}} b ";

    private static final String SKU_NEW_YEAR_SQL = "select cid,nskucnt," +
            " (select sum(nskucnt)  from {{?"+DSMConst.V_SKU_NEW_YEARS +"}} where cid = b.cid and days <= b.days) hnskucnt," +
            " years,mons,days,weeks from {{?"+DSMConst.V_SKU_NEW_YEARS +"}} b ";




    //查询当前库存
   private static final String QUERY_CURRENT_STOCK = "select pc.classid cid,pc.classname cn,count(sku.sku) skucnt " +
           " from {{?"+DSMConst.TD_PRODUCE_CLASS +"}} pc join {{?"+ +DSMConst.TD_PROD_SKU+"}}  sku" +
           " on substring(sku.sku, 2) REGEXP CONCAT('^',pc.classid) group by pc.classid ";



    private static final String COL_CLASSC = "classc";
    private static final String COL_DETAIL = "detail";
    private static final String COL_TOTAL = "total";
    private static final String COL_CLASSN = "classn";
    private static final String COL_FIRST = "first";
    private static final String COL_SHOWDATE = "showdate";
    private static final String COL_YEAR = "year";
    private static final String COL_MONTH = "month";
    private static final String COL_DATE = "date";
    private static final String COL_WEEK = "week";
    private static final String COL_BEGINDATE = "begindate";
    private static final String COL_ENDDATE = "enddate";
    // sku种类统计
    private static final String COL_PRODUCT_SUM = "product_sum";
    private static final String COL_PRODUCT_ADD = "product_add";
    private static final String COL_PRODUCT_ADDC = "product_addc";
    private static final String COL_PRODUCT_ADDH = "product_addh";
    private static final String COL_PRODUCT_SOLD = "product_sold";
    private static final String COL_PRODUCT_SOLDH = "product_soldh";
    private static final String COL_PRODUCT_SALES = "product_sales";
    private static final String COL_PRODUCT_SALESH = "product_salesh";
    private static final String COL_PRODUCT_SALESC = "product_salesc";
    private static final String COL_PRODUCT_SALESPC = "product_salespc";
    private static final String COL_PRODUCT_SOLDPC = "product_soldpc";
    // 库存统计
    private static final String COL_STOCK_SUM = "stock_sum";
    private static final String COL_STOCK_MAX = "stock_max";
    private static final String COL_STOCK_MIN = "stock_min";

    //销售量
    private static final String COL_SALENUM_SUM = "salenum_sum";
    private static final String COL_SALENUM_MAX = "salenum_max";
    private static final String COL_SALENUM_MIN = "salenum_min";
    private static final String COL_SALENUM_SUMH = "salenum_sumh";
    private static final String COL_SALENUM_MAXH = "salenum_maxh";
    private static final String COL_SALENUM_MINH = "salenum_minh";
    private static final String COL_SALENUM_SUMC = "salenum_sumc";
    private static final String COL_SALENUM_MAXC = "salenum_maxc";
    private static final String COL_SALENUM_MINC = "salenum_minc";

    //销售额
    private static final String COL_SALEAMT_SUM = "saleamt_sum";
    private static final String COL_SALEAMT_MAX = "saleamt_max";
    private static final String COL_SALEAMT_MIN = "saleamt_min";
    private static final String COL_SALEAMT_SUMH = "saleamt_sumh";
    private static final String COL_SALEAMT_MAXH = "saleamt_maxh";
    private static final String COL_SALEAMT_MINH = "saleamt_minh";
    private static final String COL_SALEAMT_SUMC = "saleamt_sumc";
    private static final String COL_SALEAMT_MAXC = "saleamt_maxc";
    private static final String COL_SALEAMT_MINC = "saleamt_minc";


    //动销率
    private static final String COL_SALESPC_SUM = "salespc_sum";
    private static final String COL_SALESPC_MAX = "salespc_max";
    private static final String COL_SALESPC_MIN = "salespc_min";

    //存销比
    private static final String COL_STOCK_SALESPC_SUM = "stock_salespc_sum";
    private static final String COL_STOCK_SALESPC_MAX = "stock_salespc_max";
    private static final String COL_STOCK_SALESPC_MIN = "stock_salespc_min";
    // 总合计
    private static final String COL_SUM_TOTAL  = "sumtotal";
    private static final String COL_LIST  = "list";

    public List<JSONObject> productAnalysis(int year,int month,
                                            int classno,String classname,int type) {

        Calendar cale = Calendar.getInstance();
        List<JSONObject> jsonList = new ArrayList<>();
        StringBuilder initSqlSb = new StringBuilder(QUERY_CURRENT_STOCK);
        List<Object[]> initList
                = BaseDAO.getBaseDAO().queryNative(this.getCodSql(classno,
                QUERY_CURRENT_STOCK+" having "));

        // 初始化表格数据
        initTableData(year, month, type, cale, classno,classname, jsonList,initList);

        GetDbData getDbData = new GetDbData(year, month, type, classno).invoke();

        convInitList(jsonList,getDbData);

        calcPercent(type,jsonList);


        return jsonList;

    }

    /**
     * 是否匹配数据
     *
     */
    private boolean isMatch(int type, int cid, JSONObject subJs,
                            String _date, int _year, int _month,int _week) {
        LogUtil.getDefaultLogger().debug(subJs.toJSONString());
        int scid = subJs.getIntValue(COL_CLASSC);
        LogUtil.getDefaultLogger().debug("_year:"+_year+"  _month:"+_month);
        LogUtil.getDefaultLogger().debug("_date:"+_date+"  _week:"+_week);
        if(cid == scid) {
            LogUtil.getDefaultLogger().debug("类别码相等："+cid);
            if (type == 0 || type == 1) { // 天报(单天)
                String d = subJs.getString(COL_DATE);
                if (d.equals(_date)) {
                    LogUtil.getDefaultLogger().debug("月报测试："+cid+" "+d);
                    return true;
                }
            } else if (type == 2 || type == 3) { // 周(累计)
                int y = subJs.getInteger(COL_YEAR);
                int m = subJs.getInteger(COL_MONTH);
                int w = subJs.getInteger(COL_WEEK);
                if (_week == w && _month == m && _year == y) {
                    return true;
                }
            } else if (type == 4 || type == 5) { // 周报(单周)
                int y = subJs.getInteger(COL_YEAR);
                int m = subJs.getInteger(COL_MONTH);
                LogUtil.getDefaultLogger().debug("月报测试："+cid+" "+y+" "+m);
                if (_year == y && _month == m) {
                    LogUtil.getDefaultLogger().debug("月报测试匹配成功："+cid);
                    return true;
                }
            } else if (type == 6) { // 周报(累计)
                int y = subJs.getInteger(COL_YEAR);
                if (_year == y) {
                    return true;
                }
            }
        }
        return false;
    }

    public JSONObject pruductAnalysisByTime(int year,int month,int classno,String classname,int type) {
        List<JSONObject> jsonList = productAnalysis(year, month,classno,classname, type);
        if(jsonList == null || jsonList.size() <= 0){
            return null;
        }
        return calTotal(classno,classname,jsonList);
    }


//    public String exportMarketAnalysisByTime(int year,int month,String areaC,String areaN,int type) {
//        JSONObject r = marketAnalysisByTime(year, month, areaC, areaN, type);
//        if(r == null){
//            return "";
//        }
//
//        String str = month > 0 ? (year + "_" + month) : year + "";
//        StringBuilder fileName = new StringBuilder(str).append("_").append(areaN);
//        if(type == 0) { fileName.append("日报"); }
//        else if(type == 1) { fileName.append("日报(累计)"); }
//        else if(type == 2) { fileName.append("周报"); }
//        else if(type == 3) { fileName.append("周报(累计)"); }
//        else if(type == 4) { fileName.append("月报"); }
//        else if(type == 5) { fileName.append("月报(累计)"); }
//        else  { fileName.append("年报"); }
//
//        try (HSSFWorkbook hwb = new HSSFWorkbook()){
//            HSSFSheet sheet = hwb.createSheet();
//            HSSFCellStyle style = hwb.createCellStyle();
//            style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
//            style.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
//            style.setBorderBottom(HSSFCellStyle.BORDER_THIN);
//            style.setBorderLeft(HSSFCellStyle.BORDER_THIN);
//            style.setBorderRight(HSSFCellStyle.BORDER_THIN);
//            style.setBorderTop(HSSFCellStyle.BORDER_THIN);
//            style.setTopBorderColor(HSSFColor.BLACK.index);
//            style.setBottomBorderColor(HSSFColor.BLACK.index);
//            style.setLeftBorderColor(HSSFColor.BLACK.index);
//            style.setRightBorderColor(HSSFColor.BLACK.index);
//            HSSFRow row;
//            HSSFCell cell;
//
//            row = sheet.createRow(0);
//            String [] columns = new String [] {"时间", "地区", "市场容量(累计)", "市场容量(累计)", "市场容量(累计)", "市场容量(累计)",
//                    "注册数量", "注册数量", "注册数量", "注册数量", "认证数量", "认证数量", "认证数量", "认证数量",
//                    "活跃数量", "活跃数量", "活跃数量", "活跃数量", "复购数量", "复购数量", "复购数量", "复购数量",
//                    "市场占有率", "市场占有率", "市场占有率", "市场占有率", "活跃率", "活跃率", "活跃率", "活跃率",
//                    "复购率", "复购率", "复购率", "复购率"
//            };
//            for(int i = 0; i < columns.length; i++){
//                cell = row.createCell(i);
//                cell.setCellStyle(style);
//                cell.setCellValue(columns[i]);
//            }
//
//            row = sheet.createRow(1);
//            String [] columns1 = new String [] {"时间", "地区", "单体", "连锁", "其他", "小计",
//                    "单体", "连锁", "其他", "小计","单体", "连锁", "其他", "小计",
//                    "单体", "连锁", "其他", "小计","单体", "连锁", "其他", "小计",
//                    "单体", "连锁", "其他", "小计","单体", "连锁", "其他", "小计",
//                    "单体", "连锁", "其他", "小计"
//            };
//            for(int i = 0; i < columns1.length; i++){
//                cell = row.createCell(i);
//                cell.setCellStyle(style);
//                cell.setCellValue(columns1[i]);
//            }
//
//            int [][] mergedCol = {
//                    {0, 0, 2, 5},
//                    {0, 0, 6, 9},
//                    {0, 0, 10, 13},
//                    {0, 0, 14, 17},
//                    {0, 0, 18, 21},
//                    {0, 0, 22, 25},
//                    {0, 0, 26, 29},
//                    {0, 0, 30, 33},
//                    {0, 1, 0, 0},
//                    {0, 1, 1, 1}
//
//            };
//            if(mergedCol != null && mergedCol.length > 0){
//                for(int i = 0; i < mergedCol.length; i++){
//                    CellRangeAddress region = new CellRangeAddress(mergedCol[i][0], mergedCol[i][1], mergedCol[i][2], mergedCol[i][3]);
//                    sheet.addMergedRegion(region);
//                }
//            }
//
//            CellRangeAddress region = null;
//            int k = 2;
//            JSONArray array = r.getJSONArray(COL_LIST);
//            for(int i = 0; i < array.size(); i++){
//                JSONObject jss = array.getJSONObject(i);
//                String showdate = jss.getString(COL_SHOWDATE);
//                JSONArray subJsonArray = jss.getJSONArray(COL_DETAIL);
//                int start = k;
//                for(int j = 0; j < subJsonArray.size(); j++){
//                    JSONObject js = subJsonArray.getJSONObject(j);
//                    row = sheet.createRow(k);
//                    k++;
//                    createExcelDataRow(style, row, js, showdate);
//                }
//                int end = k - 1;
//                if(start != end){ // 不合并同一行
//                    region = new CellRangeAddress(start, end, 0, 0);
//                    sheet.addMergedRegion(region);
//                }
//                if(jss.containsKey(COL_TOTAL) && jss.getJSONObject(COL_TOTAL).containsKey(COL_AREAN)){
//                    JSONObject js = jss.getJSONObject(COL_TOTAL);
//                    row = sheet.createRow(k);
//                    k++;
//                    createExcelDataRow(style, row, js, js.getString(COL_SHOWDATE));
//                }
//            }
//
//            JSONObject js = r.getJSONObject(COL_SUM_TOTAL);
//            if(js != null && js.containsKey(COL_AREAN)){
//                row = sheet.createRow(k);
//                k++;
//                createExcelDataRow(style, row, js, js.getString(COL_SHOWDATE));
//            }
//
//            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()){
//                hwb.write(bos);
//
//                String title = getExcelDownPath(fileName.toString(), new ByteArrayInputStream(bos.toByteArray()));
//                return title;
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return "";
//    }

    /**
     * 创建excel数据行
     *
     * @param style excel样式
     * @param row excel行
     * @param js 结果集的JSON数据
     * @param data 数据
     */
//    private void createExcelDataRow(HSSFCellStyle style, HSSFRow row, JSONObject js, String data) {
//        createCell(style, row, 0, data);
//        createCell(style, row, 1, js.getString(COL_AREAN));
//        createCell(style, row, 2, js.getString(COL_MARK_ETM));
//        createCell(style, row, 3, js.getString(COL_MARK_CHAIN));
//        createCell(style, row, 4, js.getString(COL_MARK_OTHER));
//        createCell(style, row, 5, js.getString(COL_MARK_SUM));
//        createCell(style, row, 6, js.getString(COL_REG_ETM));
//        createCell(style, row, 7, js.getString(COL_REG_CHAIN));
//        createCell(style, row, 8, js.getString(COL_REG_OTHER));
//        createCell(style, row, 9, js.getString(COL_REG_SUM));
//        createCell(style, row, 10, js.getString(COL_AUTH_ETM));
//        createCell(style, row, 11, js.getString(COL_AUTH_CHAIN));
//        createCell(style, row, 12, js.getString(COL_AUTH_OTHER));
//        createCell(style, row, 13, js.getString(COL_AUTH_SUM));
//        createCell(style, row, 14, js.getString(COL_ACT_ETM));
//        createCell(style, row, 15, js.getString(COL_ACT_CHAIN));
//        createCell(style, row, 16, js.getString(COL_ACT_OTHER));
//        createCell(style, row, 17, js.getString(COL_ACT_SUM));
//        createCell(style, row, 18, js.getString(COL_REP_ETM));
//        createCell(style, row, 19, js.getString(COL_REP_CHAIN));
//        createCell(style, row, 20, js.getString(COL_REP_OTHER));
//        createCell(style, row, 21, js.getString(COL_REP_SUM));
//        createCell(style, row, 22, js.getString(COL_OCC_ETM_RATE));
//        createCell(style, row, 23, js.getString(COL_OCC_CHAIN_RATE));
//        createCell(style, row, 24, js.getString(COL_OCC_OTHER_RATE));
//        createCell(style, row, 25, js.getString(COL_OCC_SUM_RATE));
//        createCell(style, row, 26, js.getString(COL_ACT_ETM_RATE));
//        createCell(style, row, 27, js.getString(COL_ACT_CHAIN_RATE));
//        createCell(style, row, 28, js.getString(COL_ACT_OTHER_RATE));
//        createCell(style, row, 29, js.getString(COL_ACT_SUM_RATE));
//        createCell(style, row, 30, js.getString(COL_REP_ETM_RATE));
//        createCell(style, row, 31, js.getString(COL_REP_CHAIN_RATE));
//        createCell(style, row, 32, js.getString(COL_REP_OTHER_RATE));
//        createCell(style, row, 33, js.getString(COL_REP_SUM_RATE));
//    }

    private void calcTotal(int type, List<JSONObject> jsonList) {
//        int MARK_ONE_TOTAL = 0, MARK_TWO_TOTAL = 0, MARK_THREE_TOTAL = 0,MARK_SUM_TOTAL  = 0;
//        int REG_ONE_TOTAL = 0, REG_TWO_TOTAL = 0, REG_THREE_TOTAL = 0,REG_SUM_TOTAL  = 0;
//        int AUTH_ONE_TOTAL = 0, AUTH_TWO_TOTAL = 0,AUTH_THREE_TOTAL = 0, AUTH_SUM_TOTAL  = 0;
//        int ACTIVE_ONE_TOTAL = 0, ACTIVE_TWO_TOTAL = 0, ACTIVE_THREE_TOTAL = 0,ACTIVE_SUM_TOTAL = 0;
//        int REPURCHASE_ONE_TOTAL = 0, REPURCHASE_TWO_TOTAL = 0,REPURCHASE_THREE_TOTAL = 0, REPURCHASE_SUM_TOTAL  = 0;
        // 累计报表
//        for(JSONObject js : jsonList) {
////            String a = js.getString(COL_AREAC);
//            JSONArray array = js.getJSONArray(COL_DETAIL);
//
//            int mark_etm = js.getInteger(COL_MARK_ETM);
//            int mark_chain = js.getInteger(COL_MARK_CHAIN);
//            int mark_other = js.getInteger(COL_MARK_OTHER);
//            int mark_sum = js.getInteger(COL_MARK_SUM);
//
//            int REG_ONE = 0, REG_TWO = 0, REG_THREE = 0,REG_SUM  = 0;
//            int AUTH_ONE = 0, AUTH_TWO = 0,AUTH_THREE = 0, AUTH_SUM  = 0;
//            int ACTIVE_ONE = 0, ACTIVE_TWO = 0, ACTIVE_THREE = 0,ACTIVE_SUM = 0;
//            int REPURCHASE_ONE = 0, REPURCHASE_TWO = 0,REPURCHASE_THREE = 0, REPURCHASE_SUM  = 0;
//            for (int i = 0; i < array.size(); i++) {
//                JSONObject subJs = array.getJSONObject(i);
//
//                int reg_etm = subJs.getInteger(COL_REG_ETM);
//                int reg_chain = subJs.getInteger(COL_REG_CHAIN);
//                int reg_other = subJs.getInteger(COL_REG_OTHER);
//                int reg_sum = subJs.getInteger(COL_REG_SUM);
//
//                int auth_etm = subJs.getInteger(COL_AUTH_ETM);
//                int auth_chain = subJs.getInteger(COL_REG_CHAIN);
//                int auth_other = subJs.getInteger(COL_REG_OTHER);
//                int auth_sum = subJs.getInteger(COL_REG_SUM);
//
//                int act_etm = subJs.getInteger(COL_ACT_ETM);
//                int act_chain = subJs.getInteger(COL_ACT_CHAIN);
//                int act_other = subJs.getInteger(COL_ACT_OTHER);
//                int act_sum = subJs.getInteger(COL_ACT_SUM);
//
//                int rep_etm = subJs.getInteger(COL_REP_ETM);
//                int rep_chain = subJs.getInteger(COL_REP_CHAIN);
//                int rep_other = subJs.getInteger(COL_REP_OTHER);
//                int rep_sum = subJs.getInteger(COL_REP_SUM);
//
//                if(type == 1 || type == 3 || type == 5){
//
//                    REG_ONE += reg_etm;  REG_TWO += reg_chain; REG_THREE += reg_other;  REG_SUM += reg_sum;
//
//                    subJs.put(COL_REG_ETM, REG_ONE); subJs.put(COL_REG_CHAIN, REG_TWO);
//                    subJs.put(COL_REG_OTHER, REG_THREE); subJs.put(COL_REG_SUM, REG_SUM);
//
//                    AUTH_ONE += auth_etm; AUTH_TWO += auth_chain; AUTH_THREE += auth_other; AUTH_SUM += auth_sum;
//
//                    subJs.put(COL_AUTH_ETM, AUTH_ONE); subJs.put(COL_AUTH_CHAIN, AUTH_TWO);
//                    subJs.put(COL_AUTH_OTHER, AUTH_THREE); subJs.put(COL_AUTH_SUM, AUTH_SUM);
//
//                    ACTIVE_ONE += act_etm; ACTIVE_TWO += act_chain; ACTIVE_THREE += act_other; ACTIVE_SUM += act_sum;
//
//                    subJs.put(COL_ACT_ETM, ACTIVE_ONE); subJs.put(COL_ACT_CHAIN, ACTIVE_TWO);
//                    subJs.put(COL_ACT_OTHER, ACTIVE_THREE); subJs.put(COL_ACT_SUM, ACTIVE_SUM);
//
//                    REPURCHASE_ONE += rep_etm; REPURCHASE_TWO += rep_chain; REPURCHASE_THREE += rep_other; REPURCHASE_SUM += rep_sum;
//
//                    subJs.put(COL_REP_ETM, REPURCHASE_ONE); subJs.put(COL_REP_CHAIN, REPURCHASE_TWO);
//                    subJs.put(COL_REP_OTHER, REPURCHASE_THREE); subJs.put(COL_REP_SUM, REPURCHASE_SUM);
//
//
//                    subJs.put(COL_ACT_ETM_RATE, calcPercentage(ACTIVE_ONE, AUTH_ONE));
//                    subJs.put(COL_ACT_CHAIN_RATE, calcPercentage(ACTIVE_TWO, AUTH_TWO));
//                    subJs.put(COL_ACT_OTHER_RATE, calcPercentage(ACTIVE_THREE, AUTH_THREE));
//                    subJs.put(COL_ACT_SUM_RATE, calcPercentage(ACTIVE_SUM, AUTH_SUM));
//                    subJs.put(COL_REP_ETM_RATE, calcPercentage(REPURCHASE_ONE, AUTH_ONE));
//                    subJs.put(COL_REP_CHAIN_RATE, calcPercentage(REPURCHASE_TWO, AUTH_TWO));
//                    subJs.put(COL_REP_OTHER_RATE, calcPercentage(REPURCHASE_THREE, AUTH_THREE));
//                    subJs.put(COL_REP_SUM_RATE, calcPercentage(REPURCHASE_SUM, AUTH_SUM));
//
//                    subJs.put(COL_OCC_ETM_RATE,  calcPercentage(REG_ONE, mark_etm));
//                    subJs.put(COL_OCC_CHAIN_RATE,  calcPercentage(REG_TWO, mark_chain));
//                    subJs.put(COL_OCC_OTHER_RATE,  calcPercentage(REG_THREE, mark_other));
//                    subJs.put(COL_OCC_SUM_RATE,  calcPercentage(REG_SUM, mark_sum));
//                }
//
//                REG_ONE_TOTAL += reg_etm;  REG_TWO_TOTAL += reg_chain; REG_THREE_TOTAL += reg_other;  REG_SUM_TOTAL += reg_sum;
//                AUTH_ONE_TOTAL += auth_etm; AUTH_TWO_TOTAL += auth_chain; AUTH_THREE_TOTAL += auth_other; AUTH_SUM_TOTAL += auth_sum;
//                ACTIVE_ONE_TOTAL += act_etm; ACTIVE_TWO_TOTAL += act_chain; ACTIVE_THREE_TOTAL += act_other; ACTIVE_SUM_TOTAL += act_sum;
//                REPURCHASE_ONE_TOTAL += rep_etm; REPURCHASE_TWO_TOTAL += rep_chain; REPURCHASE_THREE_TOTAL += rep_other; REPURCHASE_SUM_TOTAL += rep_sum;
//            }
//
//            MARK_ONE_TOTAL += mark_etm;  MARK_TWO_TOTAL += mark_chain; MARK_THREE_TOTAL += mark_other;  MARK_SUM_TOTAL += mark_sum;
//        }


    }

    private void initTableData(int year, int month, int type, Calendar cale,
                               int classno,String classname, List<JSONObject>
                                       jsonList,List<Object[]> initList) {
        if(type == 0 || type == 1){
            cale.set(Calendar.YEAR, year);
            cale.set(Calendar.MONTH, month);
            cale.set(Calendar.DAY_OF_MONTH, 0);
            int maxDate = cale.get(Calendar.DAY_OF_MONTH);
            String m = month < 10 ?  "0" + month : month +"";
            JSONObject js = new JSONObject();
                List<JSONObject> subList = new ArrayList<>();

                for(int i = 1; i <= maxDate; i++){
                    String d = i < 10 ?  "0" + i : i +"";
                    for(int j = 0; j <initList.size(); j++ ){
                        if(j == 0){
                            continue;
                        }
                        JSONObject subJS = new JSONObject();
                        subJS.put(COL_CLASSC, initList.get(j)[0]);
                        subJS.put(COL_CLASSN, initList.get(j)[1]);
                        if(year <= cale.get(Calendar.YEAR) && year > 2018){
                            subJS.put(COL_PRODUCT_SUM, initList.get(j)[2]);
                        }else{
                            subJS.put(COL_PRODUCT_SUM, 0);
                        }
                        subJS.put(COL_DATE, year + "-" + m + "-" + d);
                        subJS.put(COL_FIRST, i == 1 ? 1 : 0);
                        subJS.put(COL_SHOWDATE,  year + "-" + m + "-" + d);
                        subJS.put("col_size",initList.size() -1);
                        generateDetailJSON(subJS);
                        subList.add(subJS);
                    }
                    JSONObject subJS = new JSONObject();
                    subJS.put(COL_CLASSC, initList.get(0)[0]);
                    subJS.put(COL_CLASSN, initList.get(0)[1]);
                    subJS.put(COL_PRODUCT_SUM, initList.get(0)[2]);
                    subJS.put(COL_DATE, year + "-" + m + "-" + d);
                    subJS.put(COL_FIRST, i == 1 ? 1 : 0);
                    subJS.put(COL_SHOWDATE,  "合计");
                    subJS.put("col_size",initList.size() -1);
                    generateDetailJSON(subJS);
                    subList.add(subJS);
                }
                js.put(COL_DETAIL, subList);
                jsonList.add(js);
        }else if(type == 2 || type == 3){
            cale.set(Calendar.YEAR, year);
            cale.set(Calendar.MONTH, month - 1);
            Date first = firstMonthDate(cale.getTime());
            Map<Integer, ProductAnalysisServiceImpl.WeekRange> maps = new HashMap<Integer, ProductAnalysisServiceImpl.WeekRange>();
            getWeekBeginAndEnd(1, first, maps);
            JSONObject js = new JSONObject();
            Set<Integer> set = maps.keySet();
                List<JSONObject> subList = new ArrayList<>();
                for(int i = 1; i <= set.size(); i++){
                    WeekRange weekRange = maps.get(i);
                    for(int j = 0; j <initList.size(); j++ ){
                        if(j == 0){
                            continue;
                        }
                        JSONObject subJS = new JSONObject();
                        subJS.put(COL_CLASSC, initList.get(j)[0]);
                        subJS.put(COL_CLASSN, initList.get(j)[1]);
                        subJS.put(COL_PRODUCT_SUM, initList.get(j)[2]);
                        subJS.put(COL_BEGINDATE, DATEFORMAT.format(weekRange.getBegin()));
                        subJS.put(COL_ENDDATE, DATEFORMAT.format(weekRange.getEnd()));
                        subJS.put(COL_WEEK, i);
                        subJS.put(COL_FIRST, i == 1 ? 1 : 0);
                        subJS.put(COL_YEAR, year);
                        subJS.put(COL_MONTH, month);
                        subJS.put(COL_SHOWDATE, i + "周");
                        subJS.put("col_size",initList.size() -1);
                        generateDetailJSON(subJS);
                        subList.add(subJS);
                    }
                    JSONObject subJS = new JSONObject();
                    subJS.put(COL_CLASSC, initList.get(0)[0]);
                    subJS.put(COL_CLASSN, initList.get(0)[1]);
                    subJS.put(COL_PRODUCT_SUM, initList.get(0)[2]);
                    subJS.put(COL_BEGINDATE, DATEFORMAT.format(weekRange.getBegin()));
                    subJS.put(COL_ENDDATE, DATEFORMAT.format(weekRange.getEnd()));
                    subJS.put(COL_FIRST, i == 1 ? 1 : 0);
                    subJS.put(COL_YEAR, year);
                    subJS.put(COL_WEEK, i);
                    subJS.put(COL_MONTH, month);
                    subJS.put("col_size",initList.size() -1);
                    subJS.put(COL_SHOWDATE,  "合计");
                    generateDetailJSON(subJS);
                    subList.add(subJS);

                }
                js.put(COL_DETAIL, subList);
                jsonList.add(js);

        }else if(type == 4 || type == 5){
                JSONObject js = new JSONObject();
                List<JSONObject> subList = new ArrayList<>();
                int monSize = 12;
                Calendar mcale = Calendar.getInstance();
                if(year == mcale.get(Calendar.YEAR)){
                    monSize = mcale.get(Calendar.MONTH) + 1;
                }
                for(int i = 1; i <= monSize; i++){
                    for(int j = 0; j <initList.size(); j++ ){
                        if(j == 0){
                            continue;
                        }

                        JSONObject subJS = new JSONObject();
                        subJS.put(COL_CLASSC, initList.get(j)[0]);
                        subJS.put(COL_CLASSN, initList.get(j)[1]);
                        subJS.put(COL_PRODUCT_SUM, initList.get(j)[2]);
                        subJS.put(COL_FIRST, i == 1 ? 1 : 0);
                        subJS.put(COL_YEAR, year);
                        subJS.put(COL_MONTH, i);
                        subJS.put(COL_SHOWDATE, i + "月");
                        subJS.put("col_size",initList.size() -1);
                        generateDetailJSON(subJS);
                        subList.add(subJS);
                    }

                    JSONObject subJS = new JSONObject();
                    subJS.put(COL_CLASSC, initList.get(0)[0]);
                    subJS.put(COL_CLASSN, initList.get(0)[1]);
                    subJS.put(COL_PRODUCT_SUM, initList.get(0)[2]);
                    subJS.put(COL_FIRST, i == 1 ? 1 : 0);
                    subJS.put(COL_YEAR, year);
                    subJS.put(COL_MONTH, i);
                    subJS.put("col_size",initList.size() -1);
                    subJS.put(COL_SHOWDATE,  "合计");
                    generateDetailJSON(subJS);
                    subList.add(subJS);
                }
                js.put(COL_DETAIL, subList);
                jsonList.add(js);
        }else if(type == 6){
            JSONObject js = new JSONObject();
            List<JSONObject> subList = new ArrayList<>();
            for(int j = 0; j <initList.size(); j++ ){
                    if(j == 0){
                        continue;
                    }
                    JSONObject subJS = new JSONObject();
                    subJS.put(COL_CLASSC, initList.get(j)[0]);
                    subJS.put(COL_CLASSN, initList.get(j)[1]);
                    subJS.put(COL_PRODUCT_SUM, initList.get(j)[2]);
                    subJS.put(COL_FIRST, 1);
                    subJS.put(COL_YEAR, year);
                    subJS.put(COL_SHOWDATE, year + "年");
                    subJS.put("col_size",initList.size() -1);
                    generateDetailJSON(subJS);
                    subList.add(subJS);
                }
                JSONObject subJS = new JSONObject();
                subJS.put(COL_CLASSC, initList.get(0)[0]);
                subJS.put(COL_CLASSN, initList.get(0)[1]);
                subJS.put(COL_PRODUCT_SUM, initList.get(0)[2]);
                subJS.put(COL_FIRST, 1);
                subJS.put(COL_YEAR, year);
                subJS.put("colsize",initList.size() -1);
                subJS.put(COL_SHOWDATE,  "合计");
                generateDetailJSON(subJS);
                subList.add(subJS);
                js.put(COL_DETAIL, subList);
                jsonList.add(js);
            }
    }

    /**
     * 生成初始化JSON数据
     */
    private void generateDetailJSON(JSONObject subJS) {
        subJS.put(COL_PRODUCT_ADD, "0");
        subJS.put(COL_PRODUCT_ADDH, "0");
        subJS.put(COL_PRODUCT_SOLD, "0");
        subJS.put(COL_PRODUCT_SOLDH, "0");
        subJS.put(COL_PRODUCT_SALES, "0");
        subJS.put(COL_PRODUCT_SALESH, "0");
        subJS.put(COL_PRODUCT_SALESPC, "0");
        subJS.put(COL_PRODUCT_SOLDPC, "0");
        subJS.put(COL_STOCK_SUM, "0");
        subJS.put(COL_STOCK_MAX, "0");
        subJS.put(COL_STOCK_MIN, "0");
        subJS.put(COL_SALENUM_SUM, "0");
        subJS.put(COL_SALENUM_SUMH, "0");
        subJS.put(COL_SALENUM_MAX, "0");
        subJS.put(COL_SALENUM_MAXH, "0");
        subJS.put(COL_SALENUM_MIN, "0");
        subJS.put(COL_SALENUM_MINH, "0");
        subJS.put(COL_SALEAMT_SUM, "0");
        subJS.put(COL_SALEAMT_SUMH, "0");
        subJS.put(COL_SALEAMT_MAX, "0");
        subJS.put(COL_SALEAMT_MAXH, "0");
        subJS.put(COL_SALEAMT_MIN, "0");
        subJS.put(COL_SALEAMT_MINH, "0");
        subJS.put(COL_SALESPC_SUM, "0");
        subJS.put(COL_SALESPC_MAX, "0");
        subJS.put(COL_SALESPC_MIN, "0");
        subJS.put(COL_STOCK_SALESPC_SUM, "0");
        subJS.put(COL_STOCK_SALESPC_MAX, "0");
        subJS.put(COL_STOCK_SALESPC_MIN, "0");
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


    private class GetDbData {
        private int year;
        private int month;
        private int type;
        private int classno;
        private List<Object[]> skuNewList;
        private List<Object[]> skuSoldList;
        private List<Object[]> skuStockList;
        private List<Object[]> skuSaleList;

        public GetDbData(int year, int month, int type, int classno) {
            this.year = year;
            this.month = month;
            this.type = type;
            this.classno = classno;
        }

        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }

        public int getMonth() {
            return month;
        }

        public void setMonth(int month) {
            this.month = month;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public int getClassno() {
            return classno;
        }

        public void setClassno(int classno) {
            this.classno = classno;
        }

        public List<Object[]> getSkuNewList() {
            return skuNewList;
        }

        public void setSkuNewList(List<Object[]> skuNewList) {
            this.skuNewList = skuNewList;
        }

        public List<Object[]> getSkuSoldList() {
            return skuSoldList;
        }

        public void setSkuSoldList(List<Object[]> skuSoldList) {
            this.skuSoldList = skuSoldList;
        }

        public List<Object[]> getSkuStockList() {
            return skuStockList;
        }

        public void setSkuStockList(List<Object[]> skuStockList) {
            this.skuStockList = skuStockList;
        }

        public List<Object[]> getSkuSaleList() {
            return skuSaleList;
        }

        public void setSkuSaleList(List<Object[]> skuSaleList) {
            this.skuSaleList = skuSaleList;
        }

        public GetDbData invoke() {
            StringBuilder skuNewSql = new StringBuilder();
            StringBuilder skuSoldSql = new StringBuilder();
            StringBuilder skuStockSql = new StringBuilder();
            StringBuilder skuSaleSql = new StringBuilder();

            //天报
            if (type == 0 || type == 1) {
//                skuNewSql.append(SKU_NEW_SQL).
//                        append(",days having mons = ").
//                        append(month).
//                        append(" and years = ").
//                        append(year).
//                        append(" and ");

                skuNewSql.append(SKU_NEW_DAY_SQL).
                        append(" where mons = ").
                        append(month).
                        append(" and years = ").
                        append(year).append(" and ");


                skuSoldSql.append(SKU_SOLD_DAY_SQL).
                        append(",days having mons = ").
                        append(month).
                        append(" and years = ").
                        append(year).append(" and ");

                skuStockSql.append(SKU_STORE_DAY_SQL).
                        append(",days having mons = ").
                        append(month).
                        append(" and years = ").
                        append(year).append(" and ");

                skuSaleSql.append(SKU_SALE_DAY_SQL).
                        append(" where mons = ").
                        append(month).
                        append(" and years = ").
                        append(year).append(" and ");
            //周报
            } else if (type == 2 || type == 3) { // 天报(累计)
//                skuNewSql.append(SKU_NEW_SQL).
//                        append(",weeks having mons = ").
//                        append(month).
//                        append(" and years = ").
//                        append(year).
//                        append(" and ");

                skuNewSql.append(SKU_NEW_WEEK_SQL).
                        append(" where mons = ").
                        append(month).
                        append(" and years = ").
                        append(year).append(" and ");

                skuSoldSql.append(SKU_SOLD_DAY_SQL).
                        append(",weeks having mons = ").
                        append(month).
                        append(" and years = ").
                        append(year).append(" and ");

                skuStockSql.append(SKU_STORE_DAY_SQL).
                        append(",weeks having mons = ").
                        append(month).
                        append(" and years = ").
                        append(year).append(" and ");

                skuSaleSql.append(SKU_SALE_WEEK_SQL).
                        append(" where mons = ").
                        append(month).
                        append(" and years = ").
                        append(year).append(" and ");
            //月报
            } else if (type == 4 || type == 5) { // 月报(单月)

                skuNewSql.append(SKU_NEW_MON_SQL).
                        append(" where years = ").
                        append(year).
                        append(" and ");


                skuSoldSql.append(SKU_SOLD_MON_SQL).
                        append(",mons having years = ").
                        append(year).
                        append(" and ");

                skuStockSql.append(SKU_STORE_MON_SQL).
                        append(",mons having years = ").
                        append(year).
                        append(" and ");

                skuSaleSql.append(SKU_SALE_MON_SQL).
                        append(" where years = ").
                        append(year).
                        append(" and ");
            //年报
            } else if (type == 6) {
                skuNewSql.append(SKU_NEW_YEAR_SQL).
                        append(" where ");

                skuSoldSql.append(SKU_SOLD_MON_SQL).
                        append(",years having  ");

                skuStockSql.append(SKU_STORE_MON_SQL).
                        append(",years having ");

                skuSaleSql.append(SKU_SALE_YEAR_SQL).
                        append(" where ");
            }

            skuNewList = BASE_DAO.queryNative(getCodSql(classno, skuNewSql.toString()), new Object[]{});

            skuSoldList = BASE_DAO.queryNative(getCodSql(classno, skuSoldSql.toString()), new Object[]{});

            skuStockList = BASE_DAO.queryNative(getCodSql(classno, skuStockSql.toString()), new Object[]{});

            skuSaleList = BASE_DAO.queryNativeSharding(0,0,getCodSql(classno, skuSaleSql.toString()), new Object[]{});

            return this;
        }
    }

    public String getCodSql(int classno,String sql){
        StringBuilder sqlSb = new StringBuilder(sql);
        if(classno > 0) {
            int ctype = (classno + "").length();
            switch (ctype) {
                case 2:
                    sqlSb.append(" cid like '").append(classno).append("%'");
                    sqlSb.append(" and LENGTH(cid) <= 4 ");
                    break;
                case 4:
                    sqlSb.append(" cid like '").append(classno).append("%'");
                    sqlSb.append(" and LENGTH(cid) >= 4 ");
                    break;
                case 6:
                    sqlSb.append(" cid = ").append(classno);
                    break;
            }
        }
        return sqlSb.toString();

    }


    public List<JSONObject> convInitList(List<JSONObject> initList,GetDbData dbData) {

        List<Object[]> skuNewList = dbData.getSkuNewList();

        List<Object[]> skuSoldList = dbData.getSkuSoldList();

        List<Object[]> skuStockList = dbData.getSkuStockList();

        List<Object[]> skuSaleList = dbData.getSkuSaleList();

        int type = dbData.getType();
        int year = dbData.getYear();
        int month = dbData.getMonth();


        for (JSONObject js : initList) {
            JSONArray array = js.getJSONArray(COL_DETAIL);
            for (int i = 0; i < array.size(); i++) {
                JSONObject subJs = array.getJSONObject(i);
                if (skuNewList != null && !skuNewList.isEmpty()) {
                    for (Object[] objs : skuNewList) {
                        LogUtil.getDefaultLogger().debug("新增SKU:" + skuNewList.size());
                        LogUtil.getDefaultLogger().debug("新增SKU参数:" + objs[0]);
                        LogUtil.getDefaultLogger().debug("新增SKU参数:" + objs[4]);
                        LogUtil.getDefaultLogger().debug("新增SKU月:" + objs[3]);
                        if (isMatch(type, Integer.parseInt(objs[0].toString()),
                                subJs, objs[5].toString(),Integer.parseInt(objs[3].toString()),
                                Integer.parseInt(objs[4].toString()),
                                Integer.parseInt(objs[6].toString()))) {

                            int skunum = subJs.getIntValue(COL_PRODUCT_SUM);
                            int skuAdd = Integer.parseInt(objs[1].toString());
                            int hskuAdd = Integer.parseInt(objs[2].toString());
                            subJs.put(COL_PRODUCT_SUM, skunum - hskuAdd);
                            subJs.put(COL_PRODUCT_ADD, skuAdd);
                            subJs.put(COL_PRODUCT_ADDC, skuAdd);
                            subJs.put(COL_PRODUCT_ADDH, hskuAdd);
                            if (type == 1 || type == 3 || type == 5){
                                subJs.put(COL_PRODUCT_ADD, hskuAdd);
                            }

                        }
                    }

                }

                if (skuSoldList != null && !skuSoldList.isEmpty()) {
                    for (Object[] objs : skuSoldList) {
                        if (isMatch(type, Integer.parseInt(objs[0].toString()),
                                subJs, objs[3].toString(), Integer.parseInt(objs[4].toString()),
                                Integer.parseInt(objs[6].toString()),
                                Integer.parseInt(objs[5].toString()))) {
                            int soldnum = Integer.parseInt(objs[2].toString());
                            subJs.put(COL_PRODUCT_SOLD, soldnum);
                        }
                    }
                }

                if (skuStockList != null && !skuStockList.isEmpty()) {
                    for (Object[] objs : skuStockList) {
                        if (isMatch(type, Integer.parseInt(objs[0].toString()),
                                subJs, objs[5].toString(), Integer.parseInt(objs[8].toString()),
                                Integer.parseInt(objs[7].toString()),
                                Integer.parseInt(objs[6].toString()))) {
                            int store = Integer.parseInt(objs[2].toString());
                            int maxStore = Integer.parseInt(objs[3].toString());
                            int minStore = Integer.parseInt(objs[4].toString());
                            subJs.put(COL_STOCK_SUM, store);
                            subJs.put(COL_STOCK_MAX, maxStore);
                            subJs.put(COL_STOCK_MIN, minStore);
                        }
                    }
                }


                //0:日报; 1:日报(累计); 2:周报; 3:周报(累计); 4:月报; 5:月报(累计); 6:年报
                if (skuSaleList != null && !skuSaleList.isEmpty()) {
                    for (Object[] objs : skuSaleList) {
                        if (isMatch(type, Integer.parseInt(objs[0].toString()),
                                subJs, objs[17].toString(), Integer.parseInt(objs[15].toString()),
                                Integer.parseInt(objs[16].toString()),
                                Integer.parseInt(objs[18].toString()))) {
                            int salesku = Integer.parseInt(objs[1].toString());
                            int skunum = Integer.parseInt(objs[2].toString());
                            int maxskunum = Integer.parseInt(objs[3].toString());
                            int minskunum = Integer.parseInt(objs[4].toString());
                            double skuamt = Double.parseDouble(objs[5].toString());
                            double maxskuamt = Double.parseDouble(objs[6].toString());
                            double minskuamt = Double.parseDouble(objs[7].toString());
                            int hskunum = Integer.parseInt(objs[8].toString());
                            int hmaxskunum = Integer.parseInt(objs[9].toString());
                            int hminskunum = Integer.parseInt(objs[10].toString());
                            double hskuamt = Double.parseDouble(objs[11].toString());
                            double hmaxskuamt = Double.parseDouble(objs[12].toString());
                            double hminskuamt = Double.parseDouble(objs[13].toString());
                            int hsalesku = Integer.parseInt(objs[14].toString());

                            if (type == 1 || type == 3 || type == 5) {
                                subJs.put(COL_SALEAMT_MAXH, hmaxskuamt);
                                subJs.put(COL_SALEAMT_MAX, hmaxskuamt);
                                subJs.put(COL_SALEAMT_MAXC, maxskuamt);
                                subJs.put(COL_SALEAMT_MIN, hminskuamt);
                                subJs.put(COL_SALEAMT_MINH, hminskuamt);
                                subJs.put(COL_SALEAMT_MINC, minskuamt);
                                subJs.put(COL_SALEAMT_SUM, hskuamt);
                                subJs.put(COL_SALEAMT_SUMH, hskuamt);
                                subJs.put(COL_SALEAMT_SUMC, skuamt);
                                subJs.put(COL_SALENUM_MINH, hminskunum);
                                subJs.put(COL_SALENUM_MIN, hminskunum);
                                subJs.put(COL_SALENUM_MINC, minskunum);
                                subJs.put(COL_SALENUM_MAXH, hmaxskunum);
                                subJs.put(COL_SALENUM_MAX, hmaxskunum);
                                subJs.put(COL_SALENUM_MAXC, maxskunum);
                                subJs.put(COL_SALENUM_SUMH, hskunum);
                                subJs.put(COL_SALENUM_SUM, hskunum);
                                subJs.put(COL_SALENUM_SUMC, skunum);
                                subJs.put(COL_PRODUCT_SALES, hsalesku);
                                subJs.put(COL_PRODUCT_SALESH, hsalesku);
                                subJs.put(COL_PRODUCT_SALESC, salesku);
                            } else {
                                subJs.put(COL_SALEAMT_MAXH, hmaxskuamt);
                                subJs.put(COL_SALEAMT_MAX, maxskuamt);
                                subJs.put(COL_SALEAMT_MAXC, maxskuamt);
                                subJs.put(COL_SALEAMT_MIN, minskuamt);
                                subJs.put(COL_SALEAMT_MINH, hminskuamt);
                                subJs.put(COL_SALEAMT_MINC, minskuamt);
                                subJs.put(COL_SALEAMT_SUM, skuamt);
                                subJs.put(COL_SALEAMT_SUMH, hskuamt);
                                subJs.put(COL_SALEAMT_SUMC, skuamt);
                                subJs.put(COL_SALENUM_MINH, hminskunum);
                                subJs.put(COL_SALENUM_MIN, minskunum);
                                subJs.put(COL_SALENUM_MINC, minskunum);
                                subJs.put(COL_SALENUM_MAXH, hmaxskunum);
                                subJs.put(COL_SALENUM_MAX, maxskunum);
                                subJs.put(COL_SALENUM_MAXC, maxskunum);
                                subJs.put(COL_SALENUM_SUMH, hskunum);
                                subJs.put(COL_SALENUM_SUM, skunum);
                                subJs.put(COL_SALENUM_SUMC, skunum);
                                subJs.put(COL_PRODUCT_SALES, salesku);
                                subJs.put(COL_PRODUCT_SALESH, hsalesku);
                                subJs.put(COL_PRODUCT_SALESC, salesku);
                            }
                        }
                    }
                }
            }
        }
        return initList;
    }

    private void convSkuStoreAdd(int type, List<JSONObject> jsonList){
        for (JSONObject js : jsonList) {
            JSONArray array = js.getJSONArray(COL_DETAIL);
            for (int i = 0; i < array.size(); i++) {
                if(i == 0){
                    continue;
                }
                JSONObject subJs = array.getJSONObject(i);
                //SKU计算
                int pdSkuSum = subJs.getIntValue(COL_PRODUCT_SUM);
                int pdSkuSale = subJs.getIntValue(COL_PRODUCT_SALES);
                int pdSkuSold = subJs.getIntValue(COL_PRODUCT_SOLD);
                subJs.put(COL_PRODUCT_SALESPC, calcPercentage(pdSkuSale, pdSkuSum));
                subJs.put(COL_PRODUCT_SOLDPC, calcPercentage(pdSkuSold, pdSkuSum));

                //动销率计算
                int saleSkuNum = subJs.getIntValue(COL_SALENUM_SUMH);
                int saleSkuMinNum = subJs.getIntValue(COL_SALENUM_MINH);
                int saleSkuMaxNum = subJs.getIntValue(COL_SALENUM_MAXH);
                int skuStoreNum = subJs.getIntValue(COL_STOCK_SUM);
                LogUtil.getDefaultLogger().debug("skuStoreNum:"+skuStoreNum);

                subJs.put(COL_SALESPC_SUM, calcPercentage(saleSkuNum, skuStoreNum));
                subJs.put(COL_SALESPC_MAX, calcPercentage(saleSkuMaxNum, skuStoreNum));
                subJs.put(COL_SALESPC_MIN, calcPercentage(saleSkuMinNum, skuStoreNum));

                //计算存销比
                int saleSkuNumc = subJs.getIntValue(COL_SALENUM_SUMC);
                int saleSkuMinNumc = subJs.getIntValue(COL_SALENUM_MINC);
                int saleSkuMaxNumc = subJs.getIntValue(COL_SALENUM_MAXC);

                LogUtil.getDefaultLogger().debug("saleSkuNumc:"+saleSkuNumc);
                LogUtil.getDefaultLogger().debug("saleSkuMinNumc:"+saleSkuMinNumc);
                LogUtil.getDefaultLogger().debug("saleSkuMaxNumc:"+saleSkuMaxNumc);
                if (skuStoreNum > 0) {
                    LogUtil.getDefaultLogger().debug("COL_STOCK_SALESPC_MAX:"+MathUtil.exactDiv(saleSkuMaxNumc, skuStoreNum).doubleValue());
                    LogUtil.getDefaultLogger().debug("saleSkuNumc:"+saleSkuNumc);
                    DecimalFormat df = new DecimalFormat("0.000000");
                    subJs.put(COL_STOCK_SALESPC_MAX, df.format((float)saleSkuMaxNumc/skuStoreNum));
                    subJs.put(COL_STOCK_SALESPC_MIN, df.format((float)saleSkuMinNumc/skuStoreNum));
                    subJs.put(COL_STOCK_SALESPC_SUM, df.format((float)saleSkuNumc/skuStoreNum));
                }
            }
        }
    }


    private void calcPercent(int type, List<JSONObject> jsonList){
        for (JSONObject js : jsonList) {
            JSONArray array = js.getJSONArray(COL_DETAIL);
            for (int i = 0; i < array.size(); i++) {
                JSONObject subJs = array.getJSONObject(i);
                //SKU计算
                int pdSkuSum = subJs.getIntValue(COL_PRODUCT_SUM);
                int pdSkuSale = subJs.getIntValue(COL_PRODUCT_SALES);
                int pdSkuSold = subJs.getIntValue(COL_PRODUCT_SOLD);
                subJs.put(COL_PRODUCT_SALESPC, calcPercentage(pdSkuSale, pdSkuSum));
                subJs.put(COL_PRODUCT_SOLDPC, calcPercentage(pdSkuSold, pdSkuSum));

                //动销率计算
                int saleSkuNum = subJs.getIntValue(COL_SALENUM_SUMH);
                int saleSkuMinNum = subJs.getIntValue(COL_SALENUM_MINH);
                int saleSkuMaxNum = subJs.getIntValue(COL_SALENUM_MAXH);
                int skuStoreNum = subJs.getIntValue(COL_STOCK_SUM);
                LogUtil.getDefaultLogger().debug("skuStoreNum:"+skuStoreNum);

                subJs.put(COL_SALESPC_SUM, calcPercentage(saleSkuNum, skuStoreNum));
                subJs.put(COL_SALESPC_MAX, calcPercentage(saleSkuMaxNum, skuStoreNum));
                subJs.put(COL_SALESPC_MIN, calcPercentage(saleSkuMinNum, skuStoreNum));

                //计算存销比
                int saleSkuNumc = subJs.getIntValue(COL_SALENUM_SUMC);
                int saleSkuMinNumc = subJs.getIntValue(COL_SALENUM_MINC);
                int saleSkuMaxNumc = subJs.getIntValue(COL_SALENUM_MAXC);

                LogUtil.getDefaultLogger().debug("saleSkuNumc:"+saleSkuNumc);
                LogUtil.getDefaultLogger().debug("saleSkuMinNumc:"+saleSkuMinNumc);
                LogUtil.getDefaultLogger().debug("saleSkuMaxNumc:"+saleSkuMaxNumc);
                if (skuStoreNum > 0) {
                    LogUtil.getDefaultLogger().debug("saleSkuNumc:"+saleSkuNumc);
                    DecimalFormat df = new DecimalFormat("0.000000");
                    subJs.put(COL_STOCK_SALESPC_MAX, df.format((float)saleSkuMaxNumc/skuStoreNum));
                    subJs.put(COL_STOCK_SALESPC_MIN, df.format((float)saleSkuMinNumc/skuStoreNum));
                    subJs.put(COL_STOCK_SALESPC_SUM, df.format((float)saleSkuNumc/skuStoreNum));
                }
            }
        }
    }

    public JSONObject calTotal(int classno,String classname,List<JSONObject> jsonList){
        JSONObject resultJson = new JSONObject();
        JSONObject nsJson = new JSONObject();
        nsJson.put(COL_SHOWDATE, "总计");
        nsJson.put(COL_CLASSC, classno);
        nsJson.put(COL_CLASSN, classname);

        int pdsums = 0;
        int addnums = 0;
        int pdsolds = 0;
        int pdsales = 0;
        double pdsalepcs = 0;
        double pdsoldpcs = 0;
        int stocksums = 0;
        int stockmaxs = 0;
        int stockmins = 0;
        int salenums = 0;
        int salenummaxs = 0;
        int salenummins = 0;
        double saleamts = 0;
        double saleamtmaxs = 0;
        double saleamtmins = 0;
        double salespcsums = 0;
        double salespcmaxs = 0;
        double salespcmins = 0;
        double stsalespcsums = 0;
        double stsalespcmaxs = 0;
        double stsalespcmins = 0;


        for (JSONObject js : jsonList) {
            JSONArray array = js.getJSONArray(COL_DETAIL);
            for (int i = 0; i < array.size(); i++) {
                JSONObject subJs = array.getJSONObject(i);
                int classNo = subJs.getIntValue(COL_CLASSC);
                if (classNo == classno) {
                    int pdsum = subJs.getIntValue(COL_PRODUCT_SUM);
                    int addnum = subJs.getIntValue(COL_PRODUCT_ADD);
                    int pdsold = subJs.getIntValue(COL_PRODUCT_SOLD);
                    int pdsale = subJs.getIntValue(COL_PRODUCT_SALES);
                    String pdsalepc = subJs.getString(COL_PRODUCT_SALESPC);
                    String pdsoldpc = subJs.getString(COL_PRODUCT_SOLDPC);
                    int stocksum = subJs.getIntValue(COL_STOCK_SUM);
                    int stockmax = subJs.getIntValue(COL_STOCK_MAX);
                    int stockmin = subJs.getIntValue(COL_STOCK_MIN);
                    int salenum = subJs.getIntValue(COL_SALENUM_SUM);
                    int salenummax = subJs.getIntValue(COL_SALENUM_MAX);
                    int salenummin = subJs.getIntValue(COL_SALENUM_MIN);

                    double saleamt = subJs.getDoubleValue(COL_SALEAMT_SUM);
                    double saleamtmax = subJs.getDoubleValue(COL_SALEAMT_MAX);
                    double saleamtmin = subJs.getDoubleValue(COL_SALEAMT_MIN);

                    String salespcsum = subJs.getString(COL_SALESPC_SUM);
                    String salespcmax = subJs.getString(COL_SALESPC_MAX);
                    String salespcmin = subJs.getString(COL_SALESPC_MIN);

                    double stsalespcsum = subJs.getDoubleValue(COL_STOCK_SALESPC_SUM);
                    double stsalespcmax = subJs.getDoubleValue(COL_STOCK_SALESPC_MAX);
                    double stsalespcmin = subJs.getDoubleValue(COL_STOCK_SALESPC_MIN);


                    if (stsalespcmax > stsalespcmaxs) {
                        stsalespcmaxs = stsalespcmax;
                    }

                    if (stsalespcmin < stsalespcmins) {
                        stsalespcmins = stsalespcmin;
                    }


                    if (saleamtmax > saleamtmaxs) {
                        saleamtmaxs = saleamtmax;
                    }

                    if (saleamtmin < saleamtmins) {
                        saleamtmins = saleamtmin;
                    }


                    if (salenummax > salenummaxs) {
                        salenummaxs = salenummax;
                    }

                    if (salenummin < salenummins) {
                        salenummins = salenummin;
                    }

                    if (stockmax > stockmaxs) {
                        stockmaxs = stockmax;
                    }

                    if (stockmin < stockmins) {
                        stockmins = stockmin;
                    }

                    stsalespcsums = stsalespcsums + stsalespcsum;
                    saleamts = saleamts + saleamt;
                    salenums = salenums + salenum;
                  //  pdsums = pdsums + pdsum;
                    addnums = addnums + addnum;
                    pdsolds = pdsolds + pdsold;
                    pdsales = pdsales + pdsale;
                   // stocksums = stocksums + stocksum;

                    if(stocksums < stocksum){
                        stocksums = stocksum;
                    }

                    if(pdsums < pdsum){
                        pdsums = pdsum;
                    }

                    if (pdsalepc.contains("%")) {
                        pdsalepc = pdsalepc.replaceAll("%", "");
                        if (!StringUtils.isEmpty(pdsalepc)) {
                            pdsalepcs = pdsalepcs + Double.parseDouble(pdsalepc);
                        }
                    }

                    if (pdsoldpc.contains("%")) {
                        pdsoldpc = pdsoldpc.replaceAll("%", "");
                        if (!StringUtils.isEmpty(pdsoldpc)) {
                            pdsoldpcs = pdsoldpcs + Double.parseDouble(pdsoldpc);
                        }
                    }

                    if (salespcsum.contains("%")) {
                        salespcsum = salespcsum.replaceAll("%", "");
                        if (!StringUtils.isEmpty(salespcsum)) {
                            salespcsums = salespcsums + Double.parseDouble(salespcsum);
                        }
                    }

                    if (salespcmax.contains("%")) {
                        salespcmax = salespcmax.replaceAll("%", "");
                        if (!StringUtils.isEmpty(salespcmax)) {
                            salespcmaxs = salespcmaxs + Double.parseDouble(salespcmax);
                        }
                    }

                    if (salespcmin.contains("%")) {
                        salespcmin = salespcmin.replaceAll("%", "");
                        if (!StringUtils.isEmpty(salespcmin)) {
                            salespcmins = salespcmins + Double.parseDouble(salespcmin);
                        }
                    }
                }
            }
        }

        nsJson.put(COL_PRODUCT_SUM,pdsums);
        nsJson.put(COL_PRODUCT_ADD,addnums);
        nsJson.put(COL_PRODUCT_SOLD, pdsolds);
        nsJson.put(COL_PRODUCT_SALES, pdsales);
        nsJson.put(COL_PRODUCT_SALESPC, pdsalepcs+"%");
        nsJson.put(COL_PRODUCT_SOLDPC,pdsoldpcs+"%");
        nsJson.put(COL_STOCK_SUM, stocksums);
        nsJson.put(COL_STOCK_MAX, stockmaxs);
        nsJson.put(COL_STOCK_MIN, stockmins);
        nsJson.put(COL_SALENUM_SUM,salenums);
        nsJson.put(COL_SALENUM_MAX, salenummaxs);
        nsJson.put(COL_SALENUM_MIN,  salenummins);

        nsJson.put(COL_SALEAMT_SUM, saleamts);
        nsJson.put(COL_SALEAMT_MAX, saleamtmaxs);
        nsJson.put(COL_SALEAMT_MIN, saleamtmins);

        nsJson.put(COL_SALESPC_SUM, salespcsums+"%");
        nsJson.put(COL_SALESPC_MAX, salespcmaxs+"%");
        nsJson.put(COL_SALESPC_MIN,salespcmins+"%");
        nsJson.put(COL_STOCK_SALESPC_SUM, stsalespcsums);
        nsJson.put(COL_STOCK_SALESPC_MAX, stsalespcmaxs);
        nsJson.put(COL_STOCK_SALESPC_MIN, stsalespcmins);
        resultJson.put("list",jsonList);
        resultJson.put(COL_SUM_TOTAL,nsJson);
        return resultJson;
    }


    public static void main(String[] args){

    }

}
