package com.onek.user;

import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.user.operations.BDAchievementOP;

/**
 * BD报表接口
 * @author  lzz
 * @date 2019年9月2日
 */
public class BDAchievementModule {


    /**
     * 获取BD报表接口
     * @param appContext
     * @return
     */
    @UserPermission(ignore = true)
    public Result queryBDAchievement(AppContext appContext){
        //直接调用
        return  BDAchievementOP.executeQuery(appContext);
    }
}
