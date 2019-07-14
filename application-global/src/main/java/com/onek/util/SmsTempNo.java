package com.onek.util;

/**
 * @Author: leeping
 * @Date: 2019/4/26 11:56
 */
public class SmsTempNo {

    public static final int MESSAGE_AUTHENTICATION_CODE = 1;//短信验证码
    public static final int REGISTERED_SUCCESSFULLY = 2; // 注册成功
    public static final int AUTHENTICATION_SUCCESS = 3;//认证成功
    public static final int AUTHENTICATION_FAILURE = 4;//认证失败
    public static final int MODIFY_THE_PHONE = 5;//修改手机
    public static final int CHANGE_PASSWORD  = 6;//修改密码
    public static final int ACTIVITIES_OF_NEW = 7;//活动新增
    public static final int NEW_COUPONS = 8;//优惠券新增
    public static final int GROUP_BUYING_END = 9;//团购结束
    public static final int ORDER_PAYMENT_SUCCESSFUL = 10;//订单支付成功
    public static final int NOTICE_OF_COMMODITY_REDUCTION = 11;//商品降价通知
    public static final int QUALIFICATION_EXPIRED = 12;//资质过期
    public static final int AFTER_SALE_AUDIT_PASSED = 13;//售后审核通过
    public static final int AFTER_SALE_AUDIT_FAILED_TO_PASSED = 14;//售后审核不通过
    public static final int AFTER_SALE_BILL_AUDIT_PASSED = 15; // 补开发票审核通过
    public static final int AFTER_SALE_BILL_AUDIT_FAILED_TO_PASSED = 16;//补开发票审核不通过
    public static final int PROXY_PARNER_ADD_USER_VERIFY= 17;//新增地推人员验证
    public static final int QUALIFICATION_PRE_EXPIRED= 18;//资质过期预警
    public static final int ACTIVE_INVENTORY= 19; //活动库存自动更新提醒
    public static final int ERP_WARN = 20; //活动库存自动更新提醒

    //发送推送消息的权限值
    public static final int PUSH_MESSAGE_POWER =  2;

    //发送短信消息的权限值
    public static final int POWER_SMS =  4;

    //发送营销短信的权限值
    public static final int POWER_SMS_MARKET = 8;

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

    //判断短信营销
    public static boolean isSmsMarketAllow(int tempNo){
        return (IceRemoteUtil.getMessagePower(tempNo) & SmsTempNo.POWER_SMS_MARKET) != 0;
    }

    //判断栈内消息是否可发送
    public static boolean isPmAllow(int tempNo){
        return (IceRemoteUtil.getMessagePower(tempNo) & SmsTempNo.PUSH_MESSAGE_POWER) != 0;
    }
    //发送短信+栈内到指定
    public static void sendMessageToSpecify(int compid,String phone,int tempNo,String... args){
        //发送短信
        SmsUtil.sendSmsBySystemTemp(phone, tempNo,args);
        //发送信息
        IceRemoteUtil.sendTempMessageToClient(compid,tempNo,args);
    }
    //发送短信+栈内到所有
    public static void sendMessageToAll(int tempNo,String args){
        SmsUtil.sendMsgToAllBySystemTemp(tempNo,args);
        IceRemoteUtil.sendMessageToAllClient(tempNo,args);
    }
}
