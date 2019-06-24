package com.onek.report.timer;

import constant.DSMConst;
import dao.BaseDAO;
import org.hyrdpf.util.LogUtil;

import java.util.TimerTask;

/**
 * @author liuhui
 * @version V1.0
 * @ClassName StockDayTask
 * @Description 定时统计每天的库存，每天晚上12点定时统计（数据保存近30天）
 * @date 2019-06-13 10:06
 */
public class StockDayCountTask extends TimerTask {

//    private static final String SELECTSQL = "SELECT sku,store FROM {{?"+ DSMConst.TD_PROD_SKU +"}} " +
//            " WHERE cstatus & 1 = 0 ";
//
//
//    private static final String INSERTSQL = " INSERT INTO {{?" + DSMConst.TP_REPORT_DAYSTOCK + "}} "
//            + " (optdate,opttime,sku,incrnum,store,cstatus) "
//            + " VALUES (CURRENT_DATE, CURRENT_TIME, ?, ?, ?, 0) ";


    private static final String INSERTSELECT = "INSERT INTO  {{?" + DSMConst.TP_REPORT_DAYSTOCK + "}} "
            + " (optdate,opttime,sku,store,cstatus) select CURRENT_DATE,CURRENT_TIME,sku,store,0"
            + " from {{?"+DSMConst.TD_PROD_SKU+"}} WHERE cstatus & 1 = 0";



    @Override
    public void run() {
        LogUtil.getDefaultLogger().debug("===================StockDayCountTask开始执行=======================");
        BaseDAO.getBaseDAO().updateNative(INSERTSELECT);
        LogUtil.getDefaultLogger().debug("===================StockDayCountTask开始执行=======================");
    }
}
