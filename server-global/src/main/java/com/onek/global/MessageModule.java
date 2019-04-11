package com.onek.global;

import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.global.message.MessageTemplateUtil;

/**
 * @Author: leeping
 * @Date: 2019/4/11 13:37
 */
public class MessageModule {
    /**
     * 根据编号/参数 ->获取信息模板
     * @param appContext
     * @return
     */
    @UserPermission(ignore = true)
    public Result convertMessage(AppContext appContext) {
        int no = Integer.parseInt(appContext.param.arrays[0]);
        Object [] params = new Object[appContext.param.arrays.length-1];
        if (appContext.param.arrays.length - 1 >= 0)
            System.arraycopy(appContext.param.arrays, 1, params, 0, appContext.param.arrays.length - 1);
        String message = MessageTemplateUtil.templateConvertMessage(no,params);
        return new Result().success(message);
    }




}
