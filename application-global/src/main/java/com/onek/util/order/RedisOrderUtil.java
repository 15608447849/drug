package com.onek.util.order;

import com.onek.util.RedisGlobalKeys;
import redis.util.RedisUtil;
import util.StringUtils;

public class RedisOrderUtil {

    private static String SEP = "|"; // 分隔符

    public static void addOrderNumByCompid(int compid){
        RedisUtil.getStringProvide().increase(RedisGlobalKeys.ORDER_NUM_PREFIX + SEP + compid);
    }

    public static void reduceOrderNumByCompid(int compid){
        RedisUtil.getStringProvide().decrease(RedisGlobalKeys.ORDER_NUM_PREFIX + SEP + compid);
    }

    /**
     * 根据企业码获取订单数
     *
     * @param compid
     * @return
     */
    public static int getOrderNumByCompid(int compid){
        String orderNum = RedisUtil.getStringProvide().get(RedisGlobalKeys.ORDER_NUM_PREFIX + SEP + compid);
        if (StringUtils.isEmpty(orderNum)) {
            return 0;
        }
        return Integer.parseInt(orderNum);
    }

    /**
     * 添加门店参加活动购买量
     *
     */
    public static long addActBuyNum(int compid, long sku, long actCode,int num){
        if(compid > 0 && sku > 0 && actCode > 0 && num > 0){
            if(RedisUtil.getHashProvide().existsByKey(RedisGlobalKeys.ACT_BUY_NUM_PREFIX + actCode, sku + SEP +compid)) {
                return RedisUtil.getHashProvide().incrByKey(RedisGlobalKeys.ACT_BUY_NUM_PREFIX + actCode, sku + SEP + compid, num);
            }
        }
        return 0;
    }

    /**
     * 减去门店参加活动购买量
     *
     * @param compid 门店ID
     * @param sku sku
     * @param actCode 活动码
     * @param num 购买数量
     * @return
     */
    public static long subtractActBuyNum(int compid, long sku, long actCode,int num){
        if(compid > 0 && sku > 0 && actCode > 0 && num > 0){
            if(RedisUtil.getHashProvide().existsByKey(RedisGlobalKeys.ACT_BUY_NUM_PREFIX + actCode, sku + SEP +compid)) {
                return RedisUtil.getHashProvide().incrByKey(RedisGlobalKeys.ACT_BUY_NUM_PREFIX + actCode, sku + SEP + compid, -num);
            }
        }
        return 0;
    }

    /**
     * 重置活动购买量
     *
     */
    public static void resetActBuyNum(long actCode){
        RedisUtil.getHashProvide().delete(RedisGlobalKeys.ACT_BUY_NUM_PREFIX + actCode);
    }

    /**
     * 得到门店某个活动的购买量
     *
     * @param compid 门店ID
     * @param sku sku
     * @param actCode 活动码
     * @return
     */
    public static int getActBuyNum(int compid, long sku, long actCode){
        String buyNum  = RedisUtil.getHashProvide().getValByKey(RedisGlobalKeys.ACT_BUY_NUM_PREFIX + actCode, sku + SEP +compid);
        if (StringUtils.isEmpty(buyNum)) {
            return 0;
        }
        if (Integer.parseInt(buyNum) <= 0) {
            return 0;
        }
        return Integer.parseInt(buyNum);
    }

    /**
     * 获取活动限购量
     *
     * @param sku
     * @param actCode
     * @return
     */
    public static int getActLimit(long sku, long actCode) {
        String limitNum  = RedisUtil.getHashProvide().getValByKey(RedisGlobalKeys.STOCK_PREFIX + sku, RedisGlobalKeys.ACT_LIMIT_NUM_PREFIX + actCode);
        if (StringUtils.isEmpty(limitNum)) {
            return 0;
        }
        if (Integer.parseInt(limitNum) <= 0) {
            return 0;
        }
        return Integer.parseInt(limitNum);
    }
}
