package com.onek.util.order;

import com.onek.util.RedisGlobalKeys;
import redis.util.RedisUtil;
import util.StringUtils;

public class RedisOrderUtil {

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
}
