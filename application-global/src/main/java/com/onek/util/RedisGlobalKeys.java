package com.onek.util;

import redis.util.RedisUtil;

import static util.TimeUtils.getCurrentDate_Md;
import static util.TimeUtils.getCurrentYear;

/**
 * @Author: leeping
 * @Date: 2019/3/20 15:05
 */
public class RedisGlobalKeys {

    //用户表 uid 自增键
    public static final String USER_TAB_UID = "USER_TAB_UID";

    //资质id 自增键
    public static final String APT_TAB_UID = "APT_TAB_UID";

    //企业码初始值 分库分表相关
    public static final int  COMP_INIT_VAR =  536862720;

    //渠道企业码初始值
    public static final int  COMP_INIT_PROXY_VAR =100000000;

    //企业表 企业码compid  自增键
    public static final String COMP_TAB_COMPID = "COMP_TAB_COMPID";

    //渠道企业表 企业码compid  自增键
    public static final String COMP_TAB_PROXY_COMPID = "COMP_TAB_PROXY_COMPID";

    //优惠码
    public static final String OFFER_CODE = "OFFER_CODE";

    // 秒杀
    public static final String SECKILL_TOKEN_PREFIX = "SECKILL_TOKEN";

    // 库存前缀
    public static final String STOCK_PREFIX = "STOCK";

    // 可用库存前缀(代表库存-∑活动库存)
    public static final String AVAILSTOCK_PREFIX = "AVAILSTOCK";

    // 库存活动前缀
    public static final String ACTSTOCK_PREFIX = "ACTSTOCK";

    // 库存活动(初始化)前缀
    public static final String ACTSTOCK_INIT_PREFIX = "ACTINITSTOCK";

    // 活动前缀
    public static final String ACT_PREFIX = "ACT";
//
//    // 秒杀前缀
//    public static final String SECKILLPREFIX = "SECKILL";

    // 活动版本
    public static final String ACTVERSION = "ACTVERSION";

    // 会员前缀
    public static final String MEMBER_PREFIX = "MEMBER";

    // 会员等级前缀
    public static final String MEMBER_LEVEL_PREFIX = "MEMBER_LEVEL";

    // 收货人前缀
    public static final String CONSIGNEE_PREFIX = "CONSIGNEE";

    // 字典前缀
    public static final String DICT_PREFIX = "DRUG_DICT";

    // 订单数前缀
    public static final String ORDER_NUM_PREFIX = "ORDER_NUM_DICT";

    // 活动购买量前缀
    public static final String ACT_BUY_NUM_PREFIX = "ACT_BUY_NUM";

    // 活动限购量前缀
    public static final String ACT_LIMIT_NUM_PREFIX = "ACT_LIMIT_NUM";

    // 收货人id
    public static final String SHIP_ID = "SHIP_ID";

    /**
     * 获取用户ID
     */
    public static int getUserCode(){
        return   Math.toIntExact(getCurrentYear()+getCurrentDate_Md()+RedisUtil.getStringProvide().increase(USER_TAB_UID));
    }
    /**
     * 获取企业ID
     */
    public static long getCompanyCode(){
        return  RedisUtil.getStringProvide().increase(COMP_TAB_COMPID,1) + COMP_INIT_VAR;
    }

    /**
     * 获取渠道企业ID
     */
    public static long getProxyCompanyCode(){
        return  RedisUtil.getStringProvide().increase(COMP_TAB_PROXY_COMPID,1) + COMP_INIT_PROXY_VAR;
    }


    public static void main(String[] args) {
        System.out.println(getUserCode());
        System.out.println(getCompanyCode());;
    }

    /**
     * 获取优惠码ID
     */
    public static int getOfferCode(){
        return Math.toIntExact(RedisUtil.getStringProvide().increase(OFFER_CODE));
    }

    /**
     * 获取优惠码ID
     */
    public static int getShipId(){
        return Math.toIntExact(RedisUtil.getStringProvide().increase(SHIP_ID));
    }

    /**
     * 获取资质ID
     */
    public static int getAptID(){
        return   Math.toIntExact(getCurrentYear()+getCurrentDate_Md()+RedisUtil.getStringProvide().increase(APT_TAB_UID));
    }
}
