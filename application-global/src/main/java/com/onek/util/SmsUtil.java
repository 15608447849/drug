package com.onek.util;

import org.apache.http.client.fluent.Request;
import properties.abs.ApplicationPropertiesBase;
import properties.annotations.PropertiesFilePath;
import properties.annotations.PropertiesName;

import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * @description: 发送短信工具
 * @author: lzp
 */

@PropertiesFilePath("/sms.properties")
public class SmsUtil  extends ApplicationPropertiesBase {

    private static SmsUtil INSTANCE = new SmsUtil();

    @PropertiesName("sms.ip")
    public String ip;

    @PropertiesName("sms.port")
    public String port;

    @PropertiesName("sms.username")
    public String username;

    @PropertiesName("sms.password")
    public String password;

    public static String sendMsg(String phone, String context) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = sdf.format(new Date());
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(INSTANCE.username.getBytes("utf8"));
            md5.update(INSTANCE.password.getBytes("utf8"));
            md5.update(timestamp.getBytes("utf8"));
            md5.update(context.getBytes("utf8"));

            Base64.Encoder encoder = Base64.getEncoder();
            String passwordMd5 = encoder.encodeToString(md5.digest());
            passwordMd5 = URLEncoder.encode(passwordMd5);
            /*String passwordMd5 = Base64.encodeBase64String(md5.digest());*/
            String url = "http://" + INSTANCE.ip + ":" + INSTANCE.port + "/mt";
            // 短信相关的必须参数
            String mobile = phone;
            /*        String extendCode = "4443";*/
            String message = context;

            // 装配GET所需的参数
            StringBuilder sb = new StringBuilder();
            sb.append(url);
            sb.append("?dc=8"); // unicode编码
            sb.append("&sm=").append(URLEncoder.encode(message, "utf8"));
            sb.append("&da=").append(mobile);
            /*        sb.append("&sa=").append( extendCode );*/
            sb.append("&un=").append(INSTANCE.username);
            sb.append("&pw=").append(passwordMd5);
            sb.append("&tf=3"); // 表示短信内容为 urlencode+utf8
            sb.append("&rf=2");//json返回
            sb.append("&ts=").append(timestamp);//加密时间戳
            String request = sb.toString();
//            System.out.println("request:" + request);
            // 以GET方式发起请求
            String result = Request.Get(request).execute().returnContent().asString();
//            System.out.println("result" + result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static void sendSmsBySystemTemp(String phone,int tempNo,String... params){
        IOThreadUtils.runTask(()->{
            //获取短信
            String message = IceRemoteUtil.getMessageByNo(tempNo,params);
            SmsUtil.sendMsg(phone,message);
        });
    }

    //发送短信到所有人
    public static void sendMsgToAllBySystemTemp(int tempNo,String... params){
        IOThreadUtils.runTask(()->{
            //获取消息
            String message = IceRemoteUtil.getMessageByNo(tempNo,params);

            //获取所有用户手机号码
           List<String> list =  IceRemoteUtil.getAllStorePhone();

            //循环发送
            for (String phone : list){
                SmsUtil.sendMsg(phone,message);
            }

        });
    }



}