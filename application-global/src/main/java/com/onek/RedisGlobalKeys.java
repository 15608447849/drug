package com.onek;

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



    public static long getUserCode(){
        return RedisUtil.getStringProvide().increase(USER_TAB_UID);
    }

    public static long getCompanyCode(){
        return  RedisUtil.getStringProvide().increase(COMP_TAB_COMPID,1) + COMP_INIT_VAR;
    }

}
