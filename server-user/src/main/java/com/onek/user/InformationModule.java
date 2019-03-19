package com.onek.user;

import com.onek.AppContext;
import com.onek.annotation.UserPermission;
import com.onek.entitys.Result;
import com.onek.user.interactive.StoreUserInfo;
import redis.util.RedisUtil;

/**
 * @Author: leeping
 * @Date: 2019/3/19 14:26
 * 门店使用
 * 信息获取
 */
public class InformationModule {
    /**
     * 获取门店用户基础信息
     */
    @UserPermission
    public Result basicInfo(AppContext appContext){
        StoreUserInfo storeUserInfo = new StoreUserInfo();
        storeUserInfo.phone = appContext.getUserSession().phone;
       //获取用户信息
        return new Result().success(storeUserInfo);
    }


}
