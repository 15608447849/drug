package com.onek.util.order;

import com.onek.util.RedisGlobalKeys;
import redis.util.RedisUtil;
import util.StringUtils;

public class RedisOrderUtil {

    private static String SUCCESS = "OK";
    private static String SEP = "|"; // 分隔符

    public static void addOrderNumByCompid(int compid){
        RedisUtil.getStringProvide().increase(RedisGlobalKeys.ORDER_NUM_PREFIX + SEP + compid);
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
        return RedisUtil.getStringProvide().increase(RedisGlobalKeys.ACT_BUY_NUM_PREFIX + SEP + actCode + SEP + sku + SEP + compid , num);
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
        return RedisUtil.getStringProvide().decrease(RedisGlobalKeys.ACT_BUY_NUM_PREFIX + SEP + actCode + SEP + sku + SEP + compid, num);
    }

    /**
     * 重置活动购买量
     *
     */
    public static void resetActBuyNum(long sku, long actCode){
        RedisUtil.getStringProvide().deleteRedisKeyStartWith(RedisGlobalKeys.ACT_BUY_NUM_PREFIX + SEP + actCode + SEP + sku);
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
        String buyNum  = RedisUtil.getStringProvide().get(RedisGlobalKeys.ACT_BUY_NUM_PREFIX + SEP + actCode + SEP + sku  + SEP + compid );
        if (StringUtils.isEmpty(buyNum)) {
            return 0;
        }
        if (Integer.parseInt(buyNum) <= 0) {
            return 0;
        }
        return Integer.parseInt(buyNum);
    }

    /**
     * 设活动限购量 1:设置成功 0:设置失败
     *
     * @param sku
     * @param actCode
     * @param limitnum
     * @return
     */
    public static int setActLimit(long sku, long actCode, int limitnum) {
        String result = RedisUtil.getStringProvide().set(RedisGlobalKeys.ACT_LIMIT_NUM_PREFIX + SEP + sku + SEP + actCode, String.valueOf(limitnum));
        return SUCCESS.equals(result) ? 1 : 0;
    }

    /**
     * 获取活动限购量
     *
     * @param sku
     * @param actCode
     * @return
     */
    public static int getActLimit(long sku, long actCode) {
        String limitNum  = RedisUtil.getStringProvide().get(RedisGlobalKeys.ACT_LIMIT_NUM_PREFIX + SEP + sku + SEP + actCode);
        if (StringUtils.isEmpty(limitNum)) {
            return 0;
        }
        if (Integer.parseInt(limitNum) <= 0) {
            return 0;
        }
        return Integer.parseInt(limitNum);
    }
}
