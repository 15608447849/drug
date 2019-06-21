package com.onek.report;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.report.col.ColTotal;
import com.onek.report.service.MarketAnalysisServiceImpl;
import com.onek.report.service.OrderAnalysisServiceImpl;
import com.onek.report.service.ProductAnalysisServiceImpl;
import util.MathUtil;
import util.StringUtils;


/**
 * 报表模块
 *
 * @author JiangWenGuang
 * @version 1.0
 * @since 20190605
 */
public class ReportModule {

    private static MarketAnalysisServiceImpl marketAnalysisService = new MarketAnalysisServiceImpl();

    private static ProductAnalysisServiceImpl productAnalysisService = new ProductAnalysisServiceImpl();

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
        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        int year = json.has("year") ? json.get("year").getAsInt() : 0;
        int month = json.has("month") ? json.get("month").getAsInt() : 0;
        String areaC = json.has("areac") ? json.get("areac").getAsString() : "";
        String areaN = json.has("arean") ? json.get("arean").getAsString() : "";
        int type = json.has("type") ? json.get("type").getAsInt() : 0;

        JSONObject resultJson = marketAnalysisService.marketAnalysisByTime(year,month, areaC, areaN, type);

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

        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        int year = json.has("year") ? json.get("year").getAsInt() : 0;
        int month = json.has("month") ? json.get("month").getAsInt() : 0;
        String areaC = json.has("areac") ? json.get("areac").getAsString() : "";
        String areaN = json.has("arean") ? json.get("arean").getAsString() : "";
        int type = json.has("type") ? json.get("type").getAsInt() : 0;

        String title = marketAnalysisService.exportMarketAnalysisByTime(year, month, areaC, areaN, type);
        if(StringUtils.isEmpty(title)){
            return new Result().fail("导出失败");
        }
        return new Result().success(title);
    }

    /**
     *
     * 功能: 站在时间维度订单分析报表
     * 参数类型: json
     * 参数集: year:年 month:月  areac:地区码  type:统计类型
     * 返回值: Result
     * 详情说明:
     * 日期: 2019/6/5 22:08
     * 作者: Helena Rubinstein
     */
    public Result orderAnalysisByTime(AppContext appContext) {
        JSONObject json = JSON.parseObject(appContext.param.json);

        try {
            int year = json.getIntValue("year");

            if (year < 2019) {
                return new Result().success(null);
            }

            int month = json.getIntValue("month");
            int type = json.getIntValue("type");
            String date;

            try {
                 date = genDateStr(year, month, type);
            } catch (Exception e) {
                return new Result().fail(e.getMessage());
            }

            ColTotal result =
                    new OrderAnalysisServiceImpl(type, json.getLongValue("areac"), date).getResult();

            return new Result().success(JSON.toJSON(result));
        } catch (Exception e) {
            e.printStackTrace();
            return new Result().fail("查询失败");
        }

    }

    // 1-是否必须有月;2-是否必须无月
    // 0天，1天累，2周，3周累，4月，5月累，6年
    private static final int[] TYPE_LIMIT = { 1, 1, 1, 1, 0, 2, 2 };

    private String genDateStr(int year, int month, int type) {
        int limit = TYPE_LIMIT[type];
        String date = String.valueOf(year);

        if ((limit & 1) > 0) {
            if (!MathUtil.isBetween(1, month, 12)) {
                throw new IllegalArgumentException("月份缺失");
            }

            date = date + "-" + String.format("%02d", month);
        } else {
            if ((limit & 2) == 0 && MathUtil.isBetween(1, month, 12)) {
                date = date + "-" + String.format("%02d", month);
            }
        }

        return date;
    }

    /**
     *
     * 功能: 导出订单分析报表
     * 参数类型: json
     * 参数集: year:年 month:月  areac:地区码  type:统计类型
     * 返回值: Result 下载地址
     * 详情说明:
     * 日期: 2019/6/5 22:08
     * 作者: Helena Rubinstein
     */
    public Result exportOrderAnalysis(AppContext appContext) {
        JSONObject json = JSON.parseObject(appContext.param.json);

        try {
            int year = json.getIntValue("year");

            if (year < 2019) {
                return new Result().success(null);
            }

            int month = json.getIntValue("month");
            int type = json.getIntValue("type");
            String date;

            try {
                date = genDateStr(year, month, type);
            } catch (Exception e) {
                return new Result().fail(e.getMessage());
            }

            String result =
                    new OrderAnalysisServiceImpl(type, json.getLongValue("areac"), date).getExportPath();

            return new Result().success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return new Result().fail("查询失败");
        }
    }

    /*static {
        *//**初始化LOG4J2日志环境*//*
        AppConfig.initLogger("log4j2.xml");
        *//**初始化应用程序环境，如数据源等*//*
        AppConfig.initialize();
    }


    /*public static void main(String[] args) {
        String r = new OrderAnalysisServiceImpl(0, 430100000000L, "2019-06").getExportPath();
        System.out.println(r);
	}*/
	
	
	
    /*
     * 功能: 商品报表
     * 参数类型: json
     * 参数集: year=年份 month=月份 areac=地区码 arean=地区名 type=报表类型
     *         type详细说明: 0:日报; 1:日报(累计); 2:周报; 3:周报(累计); 4:月报; 5:月报(累计); 6:年报
     * 返回值: code=200 data=结果信息 data.list=统计结果信息 data.sumtotal=合计信息
     * 详情说明: 导出报表可复用
     * 作者: liuhui
     */
    @UserPermission(ignore = true)
    public Result pruductAnalysisByTime(AppContext appContext) {
        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        int year = json.has("year") ? json.get("year").getAsInt() : 0;
        int month = json.has("month") ? json.get("month").getAsInt() : 0;
        int classno = json.has("classno") ? json.get("classno").getAsInt() : 0;
        if(StringUtils.isEmpty(json.get("classno").getAsString()) || classno == 0){
            classno = 10;
        }
        String classname = json.has("classname") ? json.get("classname").getAsString() : "";
        int type = json.has("type") ? json.get("type").getAsInt() : 0;

        JSONObject resultJson = productAnalysisService.pruductAnalysisByTime(year,month, classno,classname, type);

        return new Result().success(resultJson);
    }

    @UserPermission(ignore = true)
    public Result exportProuductAnalysisByTime(AppContext appContext) {
        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        int year = json.has("year") ? json.get("year").getAsInt() : 0;
        int month = json.has("month") ? json.get("month").getAsInt() : 0;
        int classno = json.has("classno") ? json.get("classno").getAsInt() : 0;
        if(StringUtils.isEmpty(json.get("classno").getAsString()) || classno == 0){
            classno = 10;
        }
        String classname = json.has("classname") ? json.get("classname").getAsString() : "";
        int type = json.has("type") ? json.get("type").getAsInt() : 0;

        String title = productAnalysisService.exportProductAnalysisByTime(year, month, classno, classname, type);
        if(StringUtils.isEmpty(title)){
            return new Result().fail("导出失败");
        }
        return new Result().success(title);
    }

}
