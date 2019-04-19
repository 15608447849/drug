package com.onek.util.stock;

import com.onek.util.RedisGlobalKeys;
import redis.util.RedisUtil;
import util.StringUtils;

public class RedisStockUtil {

    public static void setStock(long sku, int initStock){
        RedisUtil.getStringProvide().set(RedisGlobalKeys.STOCK_PREFIX + sku, String.valueOf(initStock));
    }

    public static void setActStock(long sku, long actcode,int initStock){
        RedisUtil.getStringProvide().set(RedisGlobalKeys.ACTSTOCK_PREFIX  + sku + "|" +actcode, String.valueOf(initStock));
        RedisUtil.getStringProvide().set(RedisGlobalKeys.ACTSTOCK_INIT_PREFIX  + sku + "|" +actcode, String.valueOf(initStock));
    }

    public static void clearActStock(long sku, long actcode){
        RedisUtil.getStringProvide().delete(RedisGlobalKeys.ACTSTOCK_PREFIX  + sku + "|" +actcode);
    }

    public static boolean deductionSecKillStock(long sku, int compid, int stock, long actcode){
        String currentStock = RedisUtil.getStringProvide().get(RedisGlobalKeys.ACTSTOCK_PREFIX  + sku + "|" +actcode);
        if(Integer.parseInt(currentStock) <= 0){
            return false;
        }
        if((Integer.parseInt(currentStock) - stock) <= 0){
            return false;
        }
        Long num = RedisUtil.getStringProvide().decrease(RedisGlobalKeys.ACTSTOCK_PREFIX  + sku + "|" +actcode, stock);
        if(num < 0){
            RedisUtil.getStringProvide().increase(RedisGlobalKeys.ACTSTOCK_PREFIX  + + sku + "|" +actcode, stock);
            return false;
        }
        RedisUtil.getStringProvide().decrease(RedisGlobalKeys.STOCK_PREFIX + sku, stock);
        RedisUtil.getListProvide().addEndElement(RedisGlobalKeys.SECKILLPREFIX + sku, compid + "|" + stock);
        return true;
    }

    public static int getStock(long sku){
        String currentStock = RedisUtil.getStringProvide().get(RedisGlobalKeys.STOCK_PREFIX+sku);
        if(StringUtils.isEmpty(currentStock)){
            return 0;
        }
        return Integer.parseInt(currentStock);
    }

    public static int getActStockBySkuAndActno(long sku, long actno){
        String currentStock = RedisUtil.getStringProvide().get(RedisGlobalKeys.ACTSTOCK_PREFIX  + sku + "|" +actno);
        if(StringUtils.isEmpty(currentStock)){
            return 0;
        }
        return Integer.parseInt(currentStock);

    }

    public static boolean deductionStock(long sku, int stock){
        String currentStock = RedisUtil.getStringProvide().get(RedisGlobalKeys.STOCK_PREFIX+sku);
        if(StringUtils.isEmpty(currentStock)){
            return false;
        }
        if(Integer.parseInt(currentStock) <= 0){
            return false;
        }
        if((Integer.parseInt(currentStock) - stock) <= 0){
            return false;
        }
        Long num = RedisUtil.getStringProvide().decrease(RedisGlobalKeys.STOCK_PREFIX+sku, stock);
        if(num < 0){
            RedisUtil.getStringProvide().increase(RedisGlobalKeys.STOCK_PREFIX+sku, stock);
            return false;
        }
        return true;
    }

    public static long addStock(long sku, int stock){
        Long num = RedisUtil.getStringProvide().increase(RedisGlobalKeys.STOCK_PREFIX+sku, stock);
        return num;
    }

}


