package com.onek.global;

import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
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
    public String convertMessage(AppContext appContext) {
        int no = Integer.parseInt(appContext.param.arrays[0]);
        Object [] params = new Object[appContext.param.arrays.length-1];
        if (appContext.param.arrays.length - 1 >= 0)
            System.arraycopy(appContext.param.arrays, 1, params, 0, appContext.param.arrays.length - 1);
        return MessageTemplateUtil.templateConvertMessage(no,params);
    }

    /**
     * 消息编号获取消息发送权限
     */
    @UserPermission(ignore = true)
    public String getMessageTempPower(AppContext appContext){
        int no = Integer.parseInt(appContext.param.arrays[0]);
        return MessageTemplateUtil.messageTempStatus(no);
    }
}
