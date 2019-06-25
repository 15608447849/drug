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
    private static final String SKU_SOLD_MON_SQL = "select classid cid,classname,count(distinct sku) solds,0 days,0 weeks,mons,years from {{?"+ DSMConst.TD_PRODUCE_CLASS +"}} pc join (" +
            " select sku,month(CURRENT_DATE) mons,year(CURRENT_DATE) years from {{?"+DSMConst.TD_PROD_SKU+"}} where store = 0 " +
            " UNION " +
            " select sku,mons,years from {{?"+DSMConst.TP_REPORT_MONSTOCK +"}} where stock = 0) b " +
            " on substring(b.sku, 2) REGEXP CONCAT('^',pc.classid)  group by pc.classid ";

    //SKU售罄天查询
    private static final String SKU_SOLD_DAY_SQL = "select classid cid,classname,count(distinct sku) solds,days,weeks,mons,years from {{?"+ DSMConst.TD_PRODUCE_CLASS +"}} pc join (" +
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
    private static final String SKU_SALE_WEEK_SQL = "select cid,soldsku,maxdxlv,mindxlv,maxcxlv," +
            " avdxlv,avcxlv,maxskunum,minskunum,skunum,maxskuamt,minskuamt,skuamtsum," +
            " IFNULL((select sum(skunum)  from {{?"+DSMConst.V_PD_SALES_WEEKS +"}} where cid = b.cid and days <= b.days),0) hskunum," +
            " IFNULL((select max(skunum)  from {{?"+DSMConst.V_PD_SALES_WEEKS +"}} where cid = b.cid and days <= b.days),0) hmaxskunum," +
            " IFNULL((select min(skunum)  from {{?"+DSMConst.V_PD_SALES_WEEKS +"}} where cid = b.cid and days <= b.days),0) hminskunum," +
            " IFNULL((select sum(skuamtsum)  from {{?"+DSMConst.V_PD_SALES_WEEKS +"}} where cid = b.cid and days <= b.days),0) hskuamt," +
            " IFNULL((select max(skuamtsum)  from {{?"+DSMConst.V_PD_SALES_WEEKS +"}} where cid = b.cid and days <= b.days),0) hmaxskuamt," +
            " IFNULL((select min(skuamtsum)  from {{?"+DSMConst.V_PD_SALES_WEEKS +"}} where cid = b.cid and days <= b.days),0) hminskuamt," +
            " IFNULL((select sum(soldsku)  from {{?"+DSMConst.V_PD_SALES_WEEKS +"}} where cid = b.cid and days <= b.days),0) hsoldsku," +
            " years,mons,days,weeks,mincxlv from {{?"+DSMConst.V_PD_SALES_WEEKS +"}} b ";






    //查询日销量
    private static final String SKU_SALE_DAY_SQL = "select cid,soldsku,maxdxlv,mindxlv,maxcxlv," +
            " avdxlv,avcxlv,maxskunum,minskunum,skunum,maxskuamt,minskuamt,skuamtsum," +
            " IFNULL((select sum(skunum)  from {{?"+DSMConst.V_PD_SALES_DAYS +"}} where cid = b.cid and days <= b.days),0) hskunum," +
            " IFNULL((select max(skunum)  from {{?"+DSMConst.V_PD_SALES_DAYS +"}} where cid = b.cid and days <= b.days),0) hmaxskunum," +
            " IFNULL((select min(skunum)  from {{?"+DSMConst.V_PD_SALES_DAYS +"}} where cid = b.cid and days <= b.days),0) hminskunum," +
            " IFNULL((select sum(skuamtsum)  from {{?"+DSMConst.V_PD_SALES_DAYS +"}} where cid = b.cid and days <= b.days),0) hskuamt," +
            " IFNULL((select max(skuamtsum)  from {{?"+DSMConst.V_PD_SALES_DAYS +"}} where cid = b.cid and days <= b.days),0) hmaxskuamt," +
            " IFNULL((select min(skuamtsum)  from {{?"+DSMConst.V_PD_SALES_DAYS +"}} where cid = b.cid and days <= b.days),0) hminskuamt," +
            " IFNULL((select sum(soldsku)  from {{?"+DSMConst.V_PD_SALES_DAYS +"}} where cid = b.cid and days <= b.days),0) hsoldsku," +
            " years,mons,days,weeks,mincxlv from {{?"+DSMConst.V_PD_SALES_DAYS +"}} b ";

    //查询日销量
//    private static final String SKU_SALE_DAY_SQL = "select cid,soldsku," +
//            " skunum,maxskunum,minskunum,skuamt,maxskuamt,minskuamt," +
//            " (select sum(skunum)  from {{?"+DSMConst.V_PD_SALES_DAYS +"}} where cid = b.cid and days <= b.days) hskunum," +
//            " (select max(skunum)  from {{?"+DSMConst.V_PD_SALES_DAYS +"}} where cid = b.cid and days <= b.days) hmaxskunum," +
//            " (select min(skunum)  from {{?"+DSMConst.V_PD_SALES_DAYS +"}} where cid = b.cid and days <= b.days) hminskunum," +
//            " (select sum(skuamt)  from {{?"+DSMConst.V_PD_SALES_DAYS +"}} where cid = b.cid and days <= b.days) hskuamt," +
//            " (select max(skuamt)  from {{?"+DSMConst.V_PD_SALES_DAYS +"}} where cid = b.cid and days <= b.days) hmaxskuamt," +
//            " (select min(skuamt)  from {{?"+DSMConst.V_PD_SALES_DAYS +"}} where cid = b.cid and days <= b.days) hminskuamt," +
//            " (select sum(soldsku)  from {{?"+DSMConst.V_PD_SALES_DAYS +"}} where cid = b.cid and days <= b.days) hsoldsku," +
//            " years,mons,days,weeks from {{?"+DSMConst.V_PD_SALES_DAYS +"}} b ";




    //查询月销量
//    private static final String SKU_SALE_MON_SQL = "select cid,soldsku," +
//            " skunum,maxskunum,minskunum,skuamt,maxskuamt,minskuamt," +
//            " (select sum(skunum)  from {{?"+DSMConst.V_PD_SALES_MONS +"}} where cid = b.cid and days <= b.days) hskunum," +
//            " (select max(skunum)  from {{?"+DSMConst.V_PD_SALES_MONS +"}} where cid = b.cid and days <= b.days) hmaxskunum," +
//            " (select min(skunum)  from {{?"+DSMConst.V_PD_SALES_MONS +"}} where cid = b.cid and days <= b.days) hminskunum," +
//            " (select sum(skuamt)  from {{?"+DSMConst.V_PD_SALES_MONS +"}} where cid = b.cid and days <= b.days) hskuamt," +
//            " (select max(skuamt)  from {{?"+DSMConst.V_PD_SALES_MONS +"}} where cid = b.cid and days <= b.days) hmaxskuamt," +
//            " (select min(skuamt)  from {{?"+DSMConst.V_PD_SALES_MONS +"}} where cid = b.cid and days <= b.days) hminskuamt," +
//            " (select sum(soldsku)  from {{?"+DSMConst.V_PD_SALES_MONS +"}} where cid = b.cid and days <= b.days) hsoldsku," +
//            " years,mons,days,weeks from {{?"+DSMConst.V_PD_SALES_MONS +"}} b ";

    //查询月销量
    private static final String SKU_SALE_MON_SQL = "select cid,soldsku,maxdxlv,mindxlv,maxcxlv," +
            " avdxlv,avcxlv,maxskunum,minskunum,skunum,maxskuamt,minskuamt,skuamtsum," +
            " IFNULL((select sum(skunum)  from {{?"+DSMConst.V_PD_SALES_MONS +"}} where cid = b.cid and days <= b.days),0) hskunum," +
            " IFNULL((select max(skunum)  from {{?"+DSMConst.V_PD_SALES_MONS +"}} where cid = b.cid and days <= b.days),0) hmaxskunum," +
            " IFNULL((select min(skunum)  from {{?"+DSMConst.V_PD_SALES_MONS +"}} where cid = b.cid and days <= b.days),0) hminskunum," +
            " IFNULL((select sum(skuamtsum)  from {{?"+DSMConst.V_PD_SALES_MONS +"}} where cid = b.cid and days <= b.days),0) hskuamt," +
            " IFNULL((select max(skuamtsum)  from {{?"+DSMConst.V_PD_SALES_MONS +"}} where cid = b.cid and days <= b.days),0) hmaxskuamt," +
            " IFNULL((select min(skuamtsum)  from {{?"+DSMConst.V_PD_SALES_MONS +"}} where cid = b.cid and days <= b.days),0) hminskuamt," +
            " IFNULL((select sum(soldsku)  from {{?"+DSMConst.V_PD_SALES_MONS +"}} where cid = b.cid and days <= b.days),0) hsoldsku," +
            " years,mons,days,weeks,mincxlv from {{?"+DSMConst.V_PD_SALES_MONS +"}} b ";


    //年销量
    private static final String SKU_SALE_YEAR_SQL = "select cid,soldsku,maxdxlv,mindxlv,maxcxlv," +
            " avdxlv,avcxlv,maxskunum,minskunum,skunum,maxskuamt,minskuamt,skuamtsum," +
            " IFNULL((select sum(skunum)  from {{?"+DSMConst.V_PD_SALES_YEARS +"}} where cid = b.cid and days <= b.days),0) hskunum," +
            " IFNULL((select max(skunum)  from {{?"+DSMConst.V_PD_SALES_YEARS +"}} where cid = b.cid and days <= b.days),0) hmaxskunum," +
            " IFNULL((select min(skunum)  from {{?"+DSMConst.V_PD_SALES_YEARS +"}} where cid = b.cid and days <= b.days),0) hminskunum," +
            " IFNULL((select sum(skuamtsum)  from {{?"+DSMConst.V_PD_SALES_YEARS +"}} where cid = b.cid and days <= b.days),0) hskuamt," +
            " IFNULL((select max(skuamtsum)  from {{?"+DSMConst.V_PD_SALES_YEARS +"}} where cid = b.cid and days <= b.days),0) hmaxskuamt," +
            " IFNULL((select min(skuamtsum)  from {{?"+DSMConst.V_PD_SALES_YEARS +"}} where cid = b.cid and days <= b.days),0) hminskuamt," +
            " IFNULL((select sum(soldsku)  from {{?"+DSMConst.V_PD_SALES_YEARS +"}} where cid = b.cid and days <= b.days),0) hsoldsku," +
            " years,mons,days,weeks,mincxlv from {{?"+DSMConst.V_PD_SALES_YEARS +"}} b ";
    //年销量
//    private static final String SKU_SALE_YEAR_SQL = "select cid,soldsku," +
//            " skunum,maxskunum,minskunum,skuamt,maxskuamt,minskuamt," +
//            " (select sum(skunum)  from {{?"+DSMConst.V_PD_SALES_YEARS +"}} where cid = b.cid and days <= b.days) hskunum," +
//            " (select max(skunum)  from {{?"+DSMConst.V_PD_SALES_YEARS +"}} where cid = b.cid and days <= b.days) hmaxskunum," +
//            " (select min(skunum)  from {{?"+DSMConst.V_PD_SALES_YEARS +"}} where cid = b.cid and days <= b.days) hminskunum," +
//            " (select sum(skuamt)  from {{?"+DSMConst.V_PD_SALES_YEARS +"}} where cid = b.cid and days <= b.days) hskuamt," +
//            " (select max(skuamt)  from {{?"+DSMConst.V_PD_SALES_YEARS +"}} where cid = b.cid and days <= b.days) hmaxskuamt," +
//            " (select min(skuamt)  from {{?"+DSMConst.V_PD_SALES_YEARS +"}} where cid = b.cid and days <= b.days) hminskuamt," +
//            " (select sum(soldsku)  from {{?"+DSMConst.V_PD_SALES_YEARS +"}} where cid = b.cid and days <= b.days) hsoldsku," +
//            " years,mons,days,weeks from {{?"+DSMConst.V_PD_SALES_YEARS +"}} b ";


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

    private static final int START_YEAR = 2019;

    private static final int START_MON = 5;

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

        convSkuNewAdd(jsonList,getDbData);

        calcPercent(type,jsonList);


        return jsonList;

    }

    /**
     * 是否匹配数据
     *
     */
    private boolean isMatch(int type, int cid, JSONObject subJs,
                            String _date, int _year, int _month,int _week) {
        //LogUtil.getDefaultLogger().debug(subJs.toJSONString());
        int scid = subJs.getIntValue(COL_CLASSC);
       // LogUtil.getDefaultLogger().debug("_year:"+_year+"  _month:"+_month);
       // LogUtil.getDefaultLogger().debug("_date:"+_date+"  _week:"+_week);
        if(cid == scid) {
         //   LogUtil.getDefaultLogger().debug("类别码相等："+cid);
            if (type == 0 || type == 1) { // 天报(单天)
                String d = subJs.getString(COL_DATE);
                if (d.equals(_date)) {
                 //   LogUtil.getDefaultLogger().debug("月报测试："+cid+" "+d);
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
               // LogUtil.getDefaultLogger().debug("月报测试："+cid+" "+y+" "+m);
                if (_year == y && _month == m) {
                  //  LogUtil.getDefaultLogger().debug("月报测试匹配成功："+cid);
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

        Calendar mcale = Calendar.getInstance();
        if(year < START_YEAR || year > mcale.get(Calendar.YEAR)){
            return null;
        }

        if(type < 4 && year == 2019 && month < START_MON){
            return null;
        }

        if(type < 4 && year == mcale.get(Calendar.YEAR) && month > (mcale.get(Calendar.MONTH)+1)){
            return null;
        }


        List<JSONObject> jsonList = productAnalysis(year, month,classno,classname, type);
        if(jsonList == null || jsonList.size() <= 0){
            return null;
        }
        return calTotal(classno,classname,jsonList);
    }




    private void initTableData(int year, int month, int type, Calendar cale,
                               int classno,String classname, List<JSONObject>
                                       jsonList,List<Object[]> initList) {
        Calendar curCale = Calendar.getInstance();
        if(type == 0 || type == 1){
            cale.set(Calendar.YEAR, year);
            cale.set(Calendar.MONTH, month);
            cale.set(Calendar.DAY_OF_MONTH, 0);
            int maxDate = cale.get(Calendar.DAY_OF_MONTH);
            String m = month < 10 ?  "0" + month : month +"";
            JSONObject js = new JSONObject();
            List<JSONObject> subList = new ArrayList<>();

            if(curCale.get(Calendar.YEAR) == year
                    && (curCale.get(Calendar.MONTH)+1) == month){
                maxDate = curCale.get(Calendar.DATE);
            }


                for(int i = 1; i <= maxDate; i++){
                    String d = i < 10 ?  "0" + i : i +"";
                    for(int j = 0; j <initList.size(); j++ ){
                        if(initList.size() > 1 && j == 0){
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
                        if(initList.size() > 1){
                            subJS.put("col_size",initList.size() -1);
                        }else{
                            subJS.put("col_size",initList.size());
                        }

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
                    if(initList.size() > 1){
                        subJS.put("col_size",initList.size() -1);
                    }else{
                        subJS.put("col_size",initList.size());
                    }
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
                        if(initList.size() > 1 && j == 0){
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
                        if(initList.size() > 1){
                            subJS.put("col_size",initList.size() -1);
                        }else{
                            subJS.put("col_size",initList.size());
                        }
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
                    if(initList.size() > 1){
                        subJS.put("col_size",initList.size() -1);
                    }else{
                        subJS.put("col_size",initList.size());
                    }
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
                int startMon = 1;
                Calendar mcale = Calendar.getInstance();
                if(year == mcale.get(Calendar.YEAR)){
                    monSize = mcale.get(Calendar.MONTH) + 1;
                }

                if(year == START_YEAR){
                    startMon = START_MON;
                }

                if(month > 0){
                    monSize = month;
                    startMon = month;
                }


                for(int i = startMon; i <= monSize; i++){
                    for(int j = 0; j <initList.size(); j++ ){
                        if(initList.size() > 1 && j == 0){
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
                        if(initList.size() > 1){
                            subJS.put("col_size",initList.size() -1);
                        }else{
                            subJS.put("col_size",initList.size());
                        }
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
                    if(initList.size() > 1){
                        subJS.put("col_size",initList.size() -1);
                    }else{
                        subJS.put("col_size",initList.size());
                    }
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
                    if(initList.size() > 1 && j == 0){
                        continue;
                    }
                    JSONObject subJS = new JSONObject();
                    subJS.put(COL_CLASSC, initList.get(j)[0]);
                    subJS.put(COL_CLASSN, initList.get(j)[1]);
                    subJS.put(COL_PRODUCT_SUM, initList.get(j)[2]);
                    subJS.put(COL_FIRST, 1);
                    subJS.put(COL_YEAR, year);
                    subJS.put(COL_SHOWDATE, year + "年");
                    if(initList.size() > 1){
                        subJS.put("col_size",initList.size() -1);
                    }else{
                        subJS.put("col_size",initList.size());
                    }
                    generateDetailJSON(subJS);
                    subList.add(subJS);
                }
                JSONObject subJS = new JSONObject();
                subJS.put(COL_CLASSC, initList.get(0)[0]);
                subJS.put(COL_CLASSN, initList.get(0)[1]);
                subJS.put(COL_PRODUCT_SUM, initList.get(0)[2]);
                subJS.put(COL_FIRST, 1);
                subJS.put(COL_YEAR, year);
                if(initList.size() > 1){
                    subJS.put("col_size",initList.size() -1);
                }else{
                    subJS.put("col_size",initList.size());
                }
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
//                        LogUtil.getDefaultLogger().debug("新增SKU:" + skuNewList.size());
//                        LogUtil.getDefaultLogger().debug("新增SKU参数:" + objs[0]);
//                        LogUtil.getDefaultLogger().debug("新增SKU参数:" + objs[4]);
//                        LogUtil.getDefaultLogger().debug("新增SKU月:" + objs[3]);
                        if (isMatch(type, Integer.parseInt(objs[0].toString()),
                                subJs, objs[5].toString(),Integer.parseInt(objs[3].toString()),
                                Integer.parseInt(objs[4].toString()),
                                Integer.parseInt(objs[6].toString()))) {

                            int skunum = subJs.getIntValue(COL_PRODUCT_SUM);
                            int skuAdd = Integer.parseInt(objs[1].toString());
                            int hskuAdd = Integer.parseInt(objs[2].toString());
                           // subJs.put(COL_PRODUCT_SUM, skunum - hskuAdd);
                            subJs.put(COL_PRODUCT_SUM,hskuAdd);
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
                                subJs, objs[3].toString(), Integer.parseInt(objs[6].toString()),
                                Integer.parseInt(objs[5].toString()),
                                Integer.parseInt(objs[4].toString()))) {
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
                                subJs, objs[22].toString(), Integer.parseInt(objs[20].toString()),
                                Integer.parseInt(objs[21].toString()),
                                Integer.parseInt(objs[23].toString()))) {
                            String maxdxlv = "";
                            if(objs[2] != null){
                                maxdxlv = objs[2].toString();
                                subJs.put(COL_SALESPC_MAX, maxdxlv+"%");
                            }
                            String mindxlv = "";
                            if(objs[3] != null){
                                mindxlv = objs[3].toString();
                                subJs.put(COL_SALESPC_MIN, mindxlv+"%");
                            }

                            String maxcxlv = "";
                            if(objs[4] != null){
                                maxcxlv = objs[4].toString();
                                subJs.put(COL_STOCK_SALESPC_MAX, maxcxlv);
                            }

                            String avdxlv = "";
                            if(objs[5] != null){
                                avdxlv = objs[5].toString();
                                subJs.put(COL_SALESPC_SUM, avdxlv+"%");
                            }

                            String avcxlv = "";
                            if(objs[6] != null){
                                avcxlv = objs[6].toString();
                                subJs.put(COL_STOCK_SALESPC_SUM, avcxlv);
                            }

                            String mincxlv = "";
                            if(objs[24] != null){
                                mincxlv = objs[24].toString();
                                subJs.put(COL_STOCK_SALESPC_MIN, mincxlv);
                            }

                            int salesku = Integer.parseInt(objs[1].toString());
                            int skunum = Integer.parseInt(objs[9].toString());
                            int maxskunum = Integer.parseInt(objs[7].toString());
                            int minskunum = Integer.parseInt(objs[8].toString());
                            double skuamt = Double.parseDouble(objs[12].toString());
                            double maxskuamt = Double.parseDouble(objs[10].toString());
                            double minskuamt = Double.parseDouble(objs[11].toString());
                            int hskunum = Integer.parseInt(objs[13].toString());
                            int hmaxskunum = Integer.parseInt(objs[14].toString());
                            int hminskunum = Integer.parseInt(objs[15].toString());
                            double hskuamt = Double.parseDouble(objs[16].toString());
                            double hmaxskuamt = Double.parseDouble(objs[17].toString());
                            double hminskuamt = Double.parseDouble(objs[18].toString());
                            int hsalesku = Integer.parseInt(objs[19].toString());


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
                              //  subJs.put(COL_PRODUCT_SALES, hsalesku);
                                subJs.put(COL_PRODUCT_SALES, salesku);
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

    private void convSkuNewAdd(List<JSONObject> jsonList,GetDbData dbData){
        List<Object[]> skuNewList = dbData.getSkuNewList();
        Map<Integer,List<HashMap<String,String>>> skuNewMap = new HashMap<>();
        int type = dbData.getType();
        for(Object[] ojbs : skuNewList){
            HashMap<String,String> map = new HashMap();
            if(type == 0 || type == 1){
                map.put("date",ojbs[5].toString());
            }
            if(type == 2 || type == 3){
                map.put("date",ojbs[6].toString());
            }

            if(type == 4 || type == 5){
                map.put("date",ojbs[4].toString());
            }

            if(type == 6){
                map.put("date",ojbs[3].toString());
            }

            map.put("type",type+"");
            map.put("skucnt",ojbs[1].toString());
            map.put("hskucnt",ojbs[2].toString());

            if(!skuNewMap.containsKey(Integer.parseInt(ojbs[0].toString()))){
                List<HashMap<String,String>> list = new ArrayList<>();
                list.add(map);
                skuNewMap.put(Integer.parseInt(ojbs[0].toString()),list);
            }else{
                List<HashMap<String,String>> extlist = skuNewMap.get(Integer.parseInt(ojbs[0].toString()));
                extlist.add(map);
            }
        }


        for (JSONObject js : jsonList) {
            JSONArray array = js.getJSONArray(COL_DETAIL);

            for (int i = 0; i < array.size(); i++) {
                JSONObject subJs = array.getJSONObject(i);
                int cid = subJs.getIntValue(COL_CLASSC);
                if(skuNewMap.containsKey(cid)){
                    List<HashMap<String,String>> list = skuNewMap.get(cid);
                    for(int j = 0; j < list.size(); j++){
                        HashMap<String,String> map = list.get(j);
                        String date = "";
                        if(type == 0 || type == 1){
                            date = subJs.getString(COL_DATE);
                        }

                        if(type == 2 || type == 3){
                            date = subJs.getString(COL_WEEK);
                        }

                        if(type == 4 || type == 5){
                            date = subJs.getString(COL_MONTH);
                        }

                        if(type == 6){
                            date = subJs.getString(COL_YEAR);
                        }

                        int addh =  subJs.getIntValue(COL_PRODUCT_ADDH);
                        if(addh > 0){
                            break;
                        }

                        if(compareDate(type,date,map.get("date")) == -1){
                           int cskucnt =  Integer.parseInt(map.get("hskucnt"))
                                   - Integer.parseInt(map.get("skucnt"));
                            subJs.put(COL_PRODUCT_ADDH,cskucnt);
                            subJs.put(COL_PRODUCT_SUM,cskucnt);
                            break;
                        }
                    }
                    int addh =  subJs.getIntValue(COL_PRODUCT_ADDH);
                    if(addh == 0){
                        HashMap<String, String> map = skuNewMap.get(cid).get(skuNewMap.get(cid).size() - 1);
                        int cskucnt = Integer.parseInt(map.get("hskucnt"));
                        subJs.put(COL_PRODUCT_ADDH,cskucnt);
                        subJs.put(COL_PRODUCT_SUM,cskucnt);
                    }

                }

                if(type == 1 || type == 3 || type == 5){
                    subJs.put(COL_PRODUCT_ADD,subJs.getIntValue(COL_PRODUCT_SUM));
                }

            }
        }







    }

    private int compareDate(int type,String sdate,String tdate){
        if(type == 0 || type == 1){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            try{
               return sdf.parse(sdate.toString()).compareTo(sdf.parse(tdate.toString()));
            }catch (Exception e){
                e.printStackTrace();
            }
            return -2;
        }else{
            int svalue = Integer.parseInt(sdate);
            int tvalue = Integer.parseInt(tdate);

            if(svalue < tvalue){
                return -1;
            }

            if(svalue == tvalue){
                return 0;
            }

            if(svalue > tvalue){
                return 1;
            }



        }
        return -2;

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
//                int saleSkuNum = subJs.getIntValue(COL_SALENUM_SUMH);
//                int saleSkuMinNum = subJs.getIntValue(COL_SALENUM_MINH);
//                int saleSkuMaxNum = subJs.getIntValue(COL_SALENUM_MAXH);
//                int skuStoreNum = subJs.getIntValue(COL_STOCK_SUM);
//                LogUtil.getDefaultLogger().debug("skuStoreNum:"+skuStoreNum);
//
//                subJs.put(COL_SALESPC_SUM, calcPercentage(saleSkuNum, skuStoreNum));
//                subJs.put(COL_SALESPC_MAX, calcPercentage(saleSkuMaxNum, skuStoreNum));
//                subJs.put(COL_SALESPC_MIN, calcPercentage(saleSkuMinNum, skuStoreNum));

                //计算存销比
//                int saleSkuNumc = subJs.getIntValue(COL_SALENUM_SUMC);
//                int saleSkuMinNumc = subJs.getIntValue(COL_SALENUM_MINC);
//                int saleSkuMaxNumc = subJs.getIntValue(COL_SALENUM_MAXC);
//
//                LogUtil.getDefaultLogger().debug("saleSkuNumc:"+saleSkuNumc);
//                LogUtil.getDefaultLogger().debug("saleSkuMinNumc:"+saleSkuMinNumc);
//                LogUtil.getDefaultLogger().debug("saleSkuMaxNumc:"+saleSkuMaxNumc);
//                if (skuStoreNum > 0) {
//                    LogUtil.getDefaultLogger().debug("saleSkuNumc:"+saleSkuNumc);
//                    DecimalFormat df = new DecimalFormat("0.000000");
//                    subJs.put(COL_STOCK_SALESPC_MAX, df.format((float)saleSkuMaxNumc/skuStoreNum));
//                    subJs.put(COL_STOCK_SALESPC_MIN, df.format((float)saleSkuMinNumc/skuStoreNum));
//                    subJs.put(COL_STOCK_SALESPC_SUM, df.format((float)saleSkuNumc/skuStoreNum));
//                }
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
        int stockmins = Integer.MAX_VALUE;
        int salenums = 0;
        int salenummaxs = 0;
        int salenummins = Integer.MAX_VALUE;
        double saleamts = 0;
        double saleamtmaxs = 0;
        double saleamtmins = Double.MAX_VALUE;
        double salespcsums = 0;
        double salespcmaxs = 0;
        double salespcmins = Double.MAX_VALUE;
        double stsalespcsums = 0;
        double stsalespcmaxs = 0;
        double stsalespcmins = Double.MAX_VALUE;
        int countdx = 0;
        int countcx = 0;

        for (JSONObject js : jsonList) {
            JSONArray array = js.getJSONArray(COL_DETAIL);

            for (int i = 0; i < array.size(); i++) {
                JSONObject subJs = array.getJSONObject(i);
                int classNo = subJs.getIntValue(COL_CLASSC);
                String showDate = subJs.getString(COL_SHOWDATE);
                if (classNo == classno && showDate.equals("合计")) {
                    countdx++;
                    countcx++;
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

                    if(pdsold > pdsolds){
                        pdsolds = pdsold;
                    }

                    stsalespcsums = stsalespcsums + stsalespcsum;
                    saleamts = saleamts + saleamt;
                    salenums = salenums + salenum;
                  //  pdsums = pdsums + pdsum;
                    //addnums = addnums + addnum;
                   // pdsolds = pdsolds + pdsold;
                   // pdsales = pdsales + pdsale;
                   // stocksums = stocksums + stocksum;

                    if(pdsale > pdsales){
                        pdsales = pdsale;
                    }

                    if(addnum > addnums){
                        addnums = addnum;
                    }

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
                            //countdx++;
                        }
                    }

                    if (salespcmax.contains("%")) {
                        salespcmax = salespcmax.replaceAll("%", "");
                        if (!StringUtils.isEmpty(salespcmax)) {
                            if(salespcmaxs < Double.parseDouble(salespcmax)){
                                salespcmaxs = Double.parseDouble(salespcmax);
                            }
                          //  salespcmaxs = salespcmaxs + Double.parseDouble(salespcmax);
                        }
                    }

                    if (salespcmin.contains("%")) {
                        salespcmin = salespcmin.replaceAll("%", "");
                        if (!StringUtils.isEmpty(salespcmin)) {
                            if(salespcmins > Double.parseDouble(salespcmin)){
                                salespcmins = Double.parseDouble(salespcmin);
                            }
                           // salespcmins = salespcmins + Double.parseDouble(salespcmin);
                        }
                    }else{
                        salespcmins = 0;
                    }
                }
            }
        }

        //DecimalFormat fdf=new DecimalFormat("0.0000");
        DecimalFormat tdf=new DecimalFormat("0.00");

        nsJson.put(COL_PRODUCT_SUM,pdsums);
        nsJson.put(COL_PRODUCT_ADD,addnums);
        nsJson.put(COL_PRODUCT_SOLD, pdsolds);
        nsJson.put(COL_PRODUCT_SALES, pdsales);
        nsJson.put(COL_PRODUCT_SALESPC, tdf.format(pdsalepcs)+"%");
        nsJson.put(COL_PRODUCT_SOLDPC,tdf.format(pdsoldpcs)+"%");
        nsJson.put(COL_STOCK_SUM, stocksums);
        nsJson.put(COL_STOCK_MAX, stockmaxs);
        nsJson.put(COL_STOCK_MIN, stockmins);
        nsJson.put(COL_SALENUM_SUM,salenums);
        nsJson.put(COL_SALENUM_MAX, salenummaxs);
        nsJson.put(COL_SALENUM_MIN,  salenummins);

        nsJson.put(COL_SALEAMT_SUM, tdf.format(saleamts));
        nsJson.put(COL_SALEAMT_MAX, saleamtmaxs);
        nsJson.put(COL_SALEAMT_MIN, saleamtmins);

        nsJson.put(COL_SALESPC_SUM, tdf.format(salespcsums/countdx)+"%");
        nsJson.put(COL_SALESPC_MAX, salespcmaxs+"%");
        nsJson.put(COL_SALESPC_MIN,salespcmins+"%");
        nsJson.put(COL_STOCK_SALESPC_SUM, tdf.format(stsalespcsums/countcx));
        nsJson.put(COL_STOCK_SALESPC_MAX, stsalespcmaxs);
        nsJson.put(COL_STOCK_SALESPC_MIN, tdf.format(stsalespcmins));
        resultJson.put("list",jsonList);
        resultJson.put(COL_SUM_TOTAL,nsJson);
        return resultJson;
    }


    public String exportProductAnalysisByTime(int year,int month,int classno,String classname,int type) {

        JSONObject r = pruductAnalysisByTime(year, month, classno, classname, type);
        if(r == null){
            return "";
        }

        String str = month > 0 ? (year + "_" + month) : year + "";
        StringBuilder fileName = new StringBuilder(str).append("_").append(classname);
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
            String [] columns = new String [] {"时间", "类目", "SKU", "SKU", "SKU", "SKU",
                    "SKU", "SKU", "库存", "库存", "库存", "销售量", "销售量", "销售量",
                    "销售额", "销售额", "销售额", "动销率", "动销率", "动销率", "存销比", "存销比",
                    "存销比"
            };
            for(int i = 0; i < columns.length; i++){
                cell = row.createCell(i);
                cell.setCellStyle(style);
                cell.setCellValue(columns[i]);
            }

            row = sheet.createRow(1);
            String [] columns1 = new String [] {"时间", "类目", "总量", "新增", "售罄", "动销",
                    "SKU动销率", "售罄率", "总量", "最高","最低", "总量", "最高", "最低",
                    "总额", "最高", "最低", "最高","最低", "平均", "最高", "最低",
                    "平均"
            };
            for(int i = 0; i < columns1.length; i++){
                cell = row.createCell(i);
                cell.setCellStyle(style);
                cell.setCellValue(columns1[i]);
            }

            int [][] mergedCol = {
                    {0, 0, 2, 7},
                    {0, 0, 8, 10},
                    {0, 0, 11, 13},
                    {0, 0, 14, 16},
                    {0, 0, 17, 19},
                    {0, 0, 20, 22},
                    {0, 1, 0, 0},
                    {0, 1, 1, 1}

            };
            //起始行号，终止行号， 起始列号，终止列号



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
                    String subShowdate = js.getString(COL_SHOWDATE);
                    row = sheet.createRow(k);
                    k++;
                    createExcelDataRow(style, row, js, subShowdate);
                    if(subShowdate.equals("合计")){
                        createExcelDataRow(style, row, js, subShowdate);
                        int colsize = js.getIntValue("colSize");
                        if(subJsonArray.size() > 2){
                            region = new CellRangeAddress(start, k-2, 0, 0);
                            sheet.addMergedRegion(region);
                        }
                        start = k;
                    }
;
                }
//                int end = k - 1;
//                if(start != end){ // 不合并同一行
//                    region = new CellRangeAddress(start, end, 0, 0);
//                    sheet.addMergedRegion(region);
//                }
//                if(jss.containsKey(COL_TOTAL) && jss.getJSONObject(COL_TOTAL).containsKey(COL_CLASSN)){
//                    JSONObject js = jss.getJSONObject(COL_TOTAL);
//                    row = sheet.createRow(k);
//                    k++;
//                    createExcelDataRow(style, row, js, js.getString(COL_SHOWDATE));
//                }
            }

            JSONObject js = r.getJSONObject(COL_SUM_TOTAL);
            if(js != null && js.containsKey(COL_CLASSN)){
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
        createCell(style, row, 1, js.getString(COL_CLASSN));
        createCell(style, row, 2, js.getString(COL_PRODUCT_SUM));
        createCell(style, row, 3, js.getString(COL_PRODUCT_ADD));
        createCell(style, row, 4, js.getString(COL_PRODUCT_SOLD));
        createCell(style, row, 5, js.getString(COL_PRODUCT_SALES));
        createCell(style, row, 6, js.getString(COL_PRODUCT_SALESPC));
        createCell(style, row, 7, js.getString(COL_PRODUCT_SOLDPC));
        createCell(style, row, 8, js.getString(COL_STOCK_SUM));
        createCell(style, row, 9, js.getString(COL_STOCK_MAX));
        createCell(style, row, 10, js.getString(COL_STOCK_MIN));
        createCell(style, row, 11, js.getString(COL_SALENUM_SUM));
        createCell(style, row, 12, js.getString(COL_SALENUM_MAX));
        createCell(style, row, 13, js.getString(COL_SALENUM_MIN));
        createCell(style, row, 14, js.getString(COL_SALEAMT_SUM));
        createCell(style, row, 15, js.getString(COL_SALEAMT_MAX));
        createCell(style, row, 16, js.getString(COL_SALEAMT_MIN));
        createCell(style, row, 17, js.getString(COL_SALESPC_MAX));
        createCell(style, row, 18, js.getString(COL_SALESPC_MIN));
        createCell(style, row, 19, js.getString(COL_SALESPC_SUM));
        createCell(style, row, 20, js.getString(COL_STOCK_SALESPC_MAX));
        createCell(style, row, 21, js.getString(COL_STOCK_SALESPC_MIN));
        createCell(style, row, 22, js.getString(COL_STOCK_SALESPC_SUM));
    }


    public static void main(String[] args){

    }

}
