package com.onek.discount.timer;

import com.alibaba.fastjson.JSONObject;
import com.onek.propagation.prod.ActivityManageServer;
import com.onek.propagation.prod.ProdDiscountObserver;
import com.onek.util.stock.RedisStockUtil;
import constant.DSMConst;
import dao.BaseDAO;
import util.TimeUtils;

import java.util.*;

public class ActStockCheckTask extends TimerTask {

    private static final String SQL = "select d.gcode,d.actcode,a.brulecode,d.cstatus from {{?"+ DSMConst.TD_PROM_ASSDRUG +"}} d " +
            "left join {{?"+ DSMConst.TD_PROM_ACT+"}} a " +
            "on d.actcode = a.unqid " +
            "where edate = ? ";

    @Override
    public void run() {
        System.out.println("####### act stock check ["+ TimeUtils.date_yMd_Hms_2String(new Date())+"] start #########");
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
            }

        }
        System.out.println("####### act stock check end #########");
    }
}
