package com.onek.discount.timer;

import com.alibaba.fastjson.JSONObject;
import com.onek.propagation.prod.ActivityManageServer;
import com.onek.propagation.prod.ProdDiscountObserver;
import com.onek.util.order.RedisOrderUtil;
import com.onek.util.stock.RedisStockUtil;
import constant.DSMConst;
import dao.BaseDAO;
import org.hyrdpf.util.LogUtil;
import util.TimeUtils;

import java.util.*;

public class ActStockCheckTask extends TimerTask {

    private static final String SQL = "select d.gcode,d.actcode,a.brulecode,d.cstatus from {{?"+ DSMConst.TD_PROM_ASSDRUG +"}} d " +
            "left join {{?"+ DSMConst.TD_PROM_ACT+"}} a " +
            "on d.actcode = a.unqid " +
            "where edate = ? ";

    private static final String UPDATE_SQL = "update {{?"+ DSMConst.TD_PROM_ASSDRUG +"}} d " +
            "left join {{?"+ DSMConst.TD_PROM_ACT+"}} a " +
            "on d.actcode = a.unqid set actstock = 0 " +
            "where edate = ? ";

    @Override
    public void run() {
        LogUtil.getDefaultLogger().info("####### act stock check ["+ TimeUtils.date_yMd_Hms_2String(new Date())+"] start #########");
        Date date = TimeUtils.addDay(new Date(), -1);
        String y = TimeUtils.date_yMd_2String(date);
        List<Object[]> results = BaseDAO.getBaseDAO().queryNative(SQL, y);
        if(results != null && results.size() > 0){
            ActivityManageServer server = new ActivityManageServer();
            server.registerObserver(new ProdDiscountObserver());

            for(Object[] result : results){
                Long goodsCode = (Long) result[0];
                Long actCode = (Long) result[1];
                if(goodsCode > 0 && String.valueOf(goodsCode).length() >= 14){
                    RedisStockUtil.clearActStock(goodsCode, actCode);
                    RedisStockUtil.clearActInitStock(goodsCode, actCode);
                }
                if(actCode > 0 &&  String.valueOf(result[0]).length() >= 14){
                    try {
                        LogUtil.getDefaultLogger().info("###### DiscountRuleTask reset act buy num actcode:["+actCode+"] gcode:["+ goodsCode+"]###########");
                        RedisOrderUtil.resetActBuyNum(goodsCode, actCode);
                    }catch (Exception e){ e.printStackTrace();}
                }
            }

        }
        BaseDAO.getBaseDAO().updateNative(UPDATE_SQL, y);
        LogUtil.getDefaultLogger().info("####### act stock check end #########");
    }
}
