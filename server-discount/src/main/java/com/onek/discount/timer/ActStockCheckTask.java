package com.onek.discount.timer;

import com.alibaba.fastjson.JSONObject;
import com.onek.propagation.prod.ActivityManageServer;
import com.onek.propagation.prod.ProdDiscountObserver;
import com.onek.util.IceRemoteUtil;
import com.onek.util.RedisGlobalKeys;
import com.onek.util.order.RedisOrderUtil;
import com.onek.util.stock.RedisStockUtil;
import constant.DSMConst;
import dao.BaseDAO;
import org.hyrdpf.util.LogUtil;
import redis.util.RedisUtil;
import util.TimeUtils;

import java.util.*;

/**
 *
 * 功能: 活动到期时清空活动库存和活动购买量
 * 详情说明:定时任务触发
 * 作者: 蒋文广
 */
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
//            ActivityManageServer server = new ActivityManageServer();
//            server.registerObserver(new ProdDiscountObserver());
            Set<Long> actCodeList = new HashSet<>();
            for(Object[] result : results){
                Long goodsCode = (Long) result[0];
                Long actCode = (Long) result[1];
                int rulecode = (Integer) result[2];
                actCodeList.add(actCode);
                if(goodsCode > 0 && String.valueOf(goodsCode).length() >= 14){
                    RedisStockUtil.clearActStock(goodsCode, actCode);
                }
                if ("0".equals(result[0]) ||  result[0].toString().length() <= 12) {
                    List<Long> skuList = IceRemoteUtil.querySkuListByCondition(goodsCode);
                    for (Long sku : skuList) {
                        RedisStockUtil.clearActStock(sku, actCode);
                    }
                }

            }
            for(Long actCode : actCodeList){
                if(actCode > 0){
                    try {
                        LogUtil.getDefaultLogger().info("###### DiscountRuleTask reset act buy num actcode:["+actCode+"] ###########");
                        RedisOrderUtil.resetActBuyNum(actCode);
                    }catch (Exception e){ e.printStackTrace();}

                    try {
                        LogUtil.getDefaultLogger().info("###### DiscountRuleTask delete act relation prod actcode:["+actCode+"] ###########");
                        RedisUtil.getHashProvide().delete(RedisGlobalKeys.ACT_PREFIX + actCode);
                    }catch (Exception e){ e.printStackTrace();}
                }
            }

        }
        BaseDAO.getBaseDAO().updateNative(UPDATE_SQL, y);
        LogUtil.getDefaultLogger().info("####### act stock check end #########");
    }
}
