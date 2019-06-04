package com.onek.discount.recovery;

import com.alibaba.fastjson.JSONObject;
import com.onek.propagation.prod.ActivityManageServer;
import com.onek.propagation.prod.ProdCurrentActPriceObserver;
import com.onek.propagation.prod.ProdDiscountObserver;
import com.onek.util.stock.RedisStockUtil;
import constant.DSMConst;
import dao.BaseDAO;
import org.hyrdpf.util.LogUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 库存还原 从数据库还原到REDIS中
 * @Author JiangWenGuang
 * @since 1.0
 */
public class StockDataRecovery {

    private static final String SKU_SQL = "select sku,(store-freezestore) as store from {{?"+ DSMConst.TD_PROD_SKU+"}} where cstatus &1 = 0";

    private static final String ACT_SQL  = "select d.actcode,d.gcode,d.actstock,d.limitnum,d.cstatus,a.brulecode " +
            "from {{?"+ DSMConst.TD_PROM_ASSDRUG +"}} d left join {{?"+ DSMConst.TD_PROM_ACT+"}} a " +
            "on d.actcode = a.unqid " +
            "where 1=1 and a.sdate <= CURRENT_DATE and CURRENT_DATE <= a.edate " +
            "and a.cstatus&1=0 " +
            "and d.cstatus&1=0 ";

    public static void recovery(){

        LogUtil.getDefaultLogger().info("++++++ StockDataRecovery execute start +++++++");

        List<Object[]> results = BaseDAO.getBaseDAO().queryNative(SKU_SQL);
        if(results != null && results.size() > 0){
            for(Object[] result : results){
                Long sku = Long.parseLong(result[0].toString());
                int store = Integer.parseInt(result[1].toString());
                if(sku > 0 && store > 0){
                    RedisStockUtil.setStock(sku, store);
                }
            }
        }

        LogUtil.getDefaultLogger().info("++++++ prod store recovery success end +++++++");

        List<String> proList = new ArrayList<>();
        List<Object[]> actResults = BaseDAO.getBaseDAO().queryNative(ACT_SQL);
        if(actResults != null && actResults.size() > 0){
            for(Object[] result : actResults) {
                String actCode = result[0].toString();
                Long gcode = Long.parseLong(result[1].toString());
                int actstock = Integer.parseInt(result[2].toString());
                int limitnum = Integer.parseInt(result[3].toString());
                int cstatus = Integer.parseInt(result[4].toString());
                int rulecode = Integer.parseInt(result[5].toString());

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("discount", 1);
                jsonObject.put("gcode", gcode);
                jsonObject.put("cstatus", cstatus);
                jsonObject.put("limitnum", limitnum);
                jsonObject.put("rulecode", rulecode);
                jsonObject.put("stock", actstock);
                jsonObject.put("actcode", actCode);
                proList.add(jsonObject.toJSONString());

            }
        }

        ActivityManageServer activityManageServer = new ActivityManageServer();
        activityManageServer.registerObserver(new ProdDiscountObserver());
        activityManageServer.registerObserver(new ProdCurrentActPriceObserver());
        activityManageServer.setProd(proList);

        LogUtil.getDefaultLogger().info("++++++ activity store recovery success end +++++++");

        LogUtil.getDefaultLogger().info("++++++ StockDataRecovery execute end +++++++");
    }
}
