package com.onek.util.stock;

import com.onek.util.RedisGlobalKeys;
import redis.util.RedisUtil;
import util.StringUtils;

public class RedisStockUtil {

    private static String SUCCESS = "OK";
    private static String SEP = "|"; // 分隔符

    /**
     * 设置库存 1:设置成功 0:设置失败
     *
     * @param sku
     * @param initStock
     * @return
     */
    public static int setStock(long sku, int initStock) {
        String result = RedisUtil.getStringProvide().set(RedisGlobalKeys.STOCK_PREFIX + sku, String.valueOf(initStock));
        return SUCCESS.equals(result) ? 1 : 0;
    }

    /**
     * 设活动置库存 1:设置成功 0:设置失败
     *
     * @param sku
     * @param actCode
     * @param initStock
     * @return
     */
    public static int setActStock(long sku, long actCode, int initStock) {
        RedisUtil.getStringProvide().set(RedisGlobalKeys.ACTSTOCK_PREFIX + SEP + sku + SEP + actCode, String.valueOf(initStock));
        String result = RedisUtil.getStringProvide().set(RedisGlobalKeys.ACTSTOCK_INIT_PREFIX + SEP + sku + SEP + actCode, String.valueOf(initStock));
        return SUCCESS.equals(result) ? 1 : 0;
    }


    /**
     * 清除活动库存 大于0代表成功; 0:代表失败
     *
     * @param sku
     * @param actCode
     * @return
     */
    public static int clearActStock(long sku, long actCode) {
        Long r = RedisUtil.getStringProvide().delete(RedisGlobalKeys.ACTSTOCK_PREFIX + SEP + sku + SEP + actCode);
        return r.intValue();
    }

    public static boolean deductionSecKillStock(long sku, int compid, int stock, long actCode) {
        String currentStock = RedisUtil.getStringProvide().get(RedisGlobalKeys.ACTSTOCK_PREFIX + SEP + sku + SEP + actCode);
        if (Integer.parseInt(currentStock) <= 0) {
            return false;
        }
        if ((Integer.parseInt(currentStock) - stock) <= 0) {
            return false;
        }
        Long num = RedisUtil.getStringProvide().decrease(RedisGlobalKeys.ACTSTOCK_PREFIX + SEP + sku + SEP + actCode, stock);
        if (num < 0) {
            RedisUtil.getStringProvide().increase(RedisGlobalKeys.ACTSTOCK_PREFIX + SEP + sku + SEP + actCode, stock);
            return false;
        }
        RedisUtil.getStringProvide().decrease(RedisGlobalKeys.STOCK_PREFIX + sku, stock);
        RedisUtil.getListProvide().addEndElement(RedisGlobalKeys.SECKILLPREFIX + sku, compid + "|" + stock);
        return true;
    }

    public static int getStock(long sku) {
        String currentStock = RedisUtil.getStringProvide().get(RedisGlobalKeys.STOCK_PREFIX + sku);
        if (StringUtils.isEmpty(currentStock)) {
            return 0;
        }
        return Integer.parseInt(currentStock);
    }

    public static int getActStockBySkuAndActno(long sku, long actCode) {
        String currentStock = RedisUtil.getStringProvide().get(RedisGlobalKeys.ACTSTOCK_PREFIX + SEP + sku + SEP + actCode);
        if (StringUtils.isEmpty(currentStock)) {
            return 0;
        }
        return Integer.parseInt(currentStock);

    }

    public static boolean deductionStock(long sku, int stock) {
        String currentStock = RedisUtil.getStringProvide().get(RedisGlobalKeys.STOCK_PREFIX + sku);
        if (StringUtils.isEmpty(currentStock)) {
            return false;
        }
        if (Integer.parseInt(currentStock) <= 0) {
            return false;
        }
        if ((Integer.parseInt(currentStock) - stock) <= 0) {
            return false;
        }
        Long num = RedisUtil.getStringProvide().decrease(RedisGlobalKeys.STOCK_PREFIX + sku, stock);
        if (num < 0) {
            RedisUtil.getStringProvide().increase(RedisGlobalKeys.STOCK_PREFIX + sku, stock);
            return false;
        }
        return true;
    }

    /**
     * 添加库存
     *
     * @param sku
     * @param stock
     * @return
     */
    public static long addStock(long sku, int stock) {
        Long num = RedisUtil.getStringProvide().increase(RedisGlobalKeys.STOCK_PREFIX + sku, stock);
        return num;
    }

    /**
     * 添加库存
     *
     * @param sku
     * @param stock
     * @return
     */
    public static long addActStock(long sku, int actCode, int stock) {
        Long num = RedisUtil.getStringProvide().increase(RedisGlobalKeys.ACTSTOCK_PREFIX + SEP + sku + SEP + actCode, stock);
        return num;
    }

    /**
     * 减少库存
     *
     * @param sku
     * @param stock
     * @return
     */
    public static long deductionActStock(long sku, int actCode, int stock) {
        String currentStock = RedisUtil.getStringProvide().get(RedisGlobalKeys.ACTSTOCK_PREFIX + SEP + sku + SEP + actCode);
        if (Integer.parseInt(currentStock) <= 0) {
            return 0;
        }
        if ((Integer.parseInt(currentStock) - stock) <= 0) {
            return 0;
        }
        Long num = RedisUtil.getStringProvide().increase(RedisGlobalKeys.ACTSTOCK_PREFIX + SEP + sku + SEP + actCode, stock);
        return num;
    }

}


