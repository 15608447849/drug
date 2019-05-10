package com.onek.util;

/**
 * @Author: leeping
 * @Date: 2019/4/26 11:56
 */
public class SmsTempNo {
    /**
     * 短信验证码
     * 注册成功
     * 认证成功
     * 认证失败
     * 修改手机
     * 修改密码
     * 活动新增
     * 优惠券新增
     * 团购结束
     * 订单支付成功
     * 商品降价通知
     */
    public static final int MESSAGE_AUTHENTICATION_CODE = 1;
    public static final int REGISTERED_SUCCESSFULLY = 2;
    public static final int AUTHENTICATION_SUCCESS = 3;
    public static final int AUTHENTICATION_FAILURE = 4;
    public static final int MODIFY_THE_PHONE = 5;
    public static final int CHANGE_PASSWORD  = 6;
    public static final int ACTIVITIES_OF_NEW = 7;
    public static final int NEW_COUPONS = 8;
    public static final int GROUP_BUYING_END = 9;
    public static final int ORDER_PAYMENT_SUCCESSFUL = 10;
    public static final int NOTICE_OF_COMMODITY_REDUCTION = 11;

    //发送推送消息的权限值
    public static final int PUSH_MESSAGE_POWER =  2;
    //发送短信消息的权限值
    public static final int POWER_SMS =  4;

    //生成系统消息
    public static String genPushMessageBySystemTemp(int tempNo,String... params){
        String message = "push:"+tempNo;
        if (params!=null && params.length>0){
            message+="#"+ String.join("#",params);
        }
        return  message;
    }

    //判断短信是否可发送
    public static boolean isSmsAllow(int tempNo){
        return (IceRemoteUtil.getMessagePower(tempNo) & SmsTempNo.POWER_SMS) != 0;
    }
    //判断短信是否可发送
    public static boolean isPmAllow(int tempNo){
        return (IceRemoteUtil.getMessagePower(tempNo) & SmsTempNo.PUSH_MESSAGE_POWER) != 0;
    }
}
