package com.onek.util;

import redis.util.RedisUtil;

/**
 * @Author: leeping
 * @Date: 2019/3/20 15:05
 */
public class RedisGlobalKeys {

    //用户表 uid 自增键
    public static final String USER_TAB_UID = "USER_TAB_UID";

    //企业码初始值 分库分表相关
    public static final int  COMP_INIT_VAR =  536862720;

    //企业表 企业码compid  自增键
    public static final String COMP_TAB_COMPID = "COMP_TAB_COMPID";

    //企业资质表 资质id 自增键
    public static final String COMP_APT_TAB_APTID = "COMP_APT_TAB_APTID";

    //优惠码
    public static final String OFFER_CODE = "OFFER_CODE";

    // 秒杀
    public static final String SECKILL_TOKEN_PREFIX = "SECKILL_TOKEN";

    // 库存前缀
    public static final String STOCK_PREFIX = "STOCK";

    // 库存活动前缀
    public static final String ACTSTOCK_PREFIX = "ACTSTOCK";

    // 库存活动前缀
    public static final String ACTSTOCK_INIT_PREFIX = "ACTINITSTOCK";

    // 秒杀前缀
    public static final String SECKILLPREFIX = "SECKILL";

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


    /**
     * 获取用户ID
     */
    public static long getUserCode(){
        return RedisUtil.getStringProvide().increase(USER_TAB_UID);
    }
    /**
     * 获取企业ID
     */
    public static long getCompanyCode(){
        return  RedisUtil.getStringProvide().increase(COMP_TAB_COMPID,1) + COMP_INIT_VAR;
    }
    /**
     * 获取企业资质ID
     */
    public static long getCompanyAptCode(){
        return  RedisUtil.getStringProvide().increase(COMP_APT_TAB_APTID,1);
    }

    /**
     * 获取优惠码ID
     */
    public static int getOfferCode(){
        return Math.toIntExact(RedisUtil.getStringProvide().increase(OFFER_CODE));
    }
}
