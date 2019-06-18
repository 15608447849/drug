package com.onek.report.timer;

import constant.DSMConst;
import dao.BaseDAO;
import org.hyrdpf.util.LogUtil;
import util.TimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 * @author liuhui
 * @version V1.0
 * @ClassName StockMonCountTask
 * @Description 按月统计库存
 * @date 2019-06-13 10:43
 */
public class StockMonCountTask  extends TimerTask {

    private static final String SELECTSQL = "SELECT sku,store FROM {{?"+ DSMConst.TD_PROD_SKU +"}} " +
            " WHERE cstatus & 1 = 0 ";

    private static final String INSERTSQL = " INSERT INTO {{?" + DSMConst.TP_REPORT_DAYSTOCK + "}} "
            + " (years,mons,sku,stock,salenum,saleamt,cstatus) "
            + " VALUES (YEAR(CURDATE()),MONTH(CURDATE()),?, ?, ?, ?, 0) ";


    private static final String UPDATESQL = " UPDATE {{?" + DSMConst.TP_REPORT_DAYSTOCK + "}} "
            + " SET salenum = ?,saleamt = ?  "
            + " WHERE sku = ? and years = YEAR(CURDATE()) and mons =  MONTH(CURDATE()) ";


    private static final String INSERTSELECT = "INSERT INTO  {{?" + DSMConst.TP_REPORT_MONSTOCK + "}} "
            + " (years,mons,sku,stock,salenum,saleamt,cstatus) select YEAR(CURDATE()),MONTH(CURDATE()),sku,store,0,0,0"
            + " from {{?"+DSMConst.TD_PROD_SKU+"}} WHERE cstatus & 1 = 0";

    private static final String SELECT_ORDER_SALE = "SELECT bkgoods.pdno sku,sum(bkgoods.payamt) skuamt," +
            "sum(bkgoods.pnum) skunum FROM {{?"+DSMConst.TD_BK_TRAN_ORDER+"}} bkorder join {{?"
            +DSMConst.TD_BK_TRAN_GOODS+"}} bkgoods  on bkorder.orderno = bkgoods.orderno" +
            " where bkorder.ostatus > 0 group by pdno ";



    @Override
    public void run() {
        LogUtil.getDefaultLogger().debug("===================StockMonCountTask开始执行=======================");
        BaseDAO.getBaseDAO().updateNative(INSERTSELECT);
        List<Object[]> queryRet = BaseDAO.getBaseDAO().queryNativeSharding(0,
                TimeUtils.getCurrentYear(), SELECT_ORDER_SALE, new Object[]{});

        if(queryRet != null && !queryRet.isEmpty()){
            List<Object[]> parm = new ArrayList<>();
            for (Object[] objects : queryRet){
                parm.add(new Object[]{objects[2],objects[1],objects[0]});
            }
            BaseDAO.getBaseDAO().updateBatchNative(UPDATESQL,parm,parm.size());
        }
        LogUtil.getDefaultLogger().debug("===================StockMonCountTask开始完成=======================");
    }

}
