package com.onek.util;

import Ice.Application;
import org.apache.http.client.fluent.Request;
import properties.abs.ApplicationPropertiesBase;
import properties.annotations.PropertiesFilePath;
import properties.annotations.PropertiesName;
import util.StringUtils;

import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static com.onek.util.SmsTempNo.isSmsAllow;
import static com.onek.util.SmsTempNo.isSmsMarketAllow;

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

    @PropertiesName("sms.username.market")
    public String usernameMarket;
    @PropertiesName("sms.password.market")
    public String passwordMarket;


    private static Base64.Encoder encoder = Base64.getEncoder();

    private static String sendMsg(String phone, String content,boolean isMarket) {
        try {
            if (StringUtils.isEmpty(phone,content)) return null;
            String username = INSTANCE.username;
            String password = INSTANCE.password;
            if (isMarket){
                username = INSTANCE.usernameMarket;
                password = INSTANCE.passwordMarket;
            }
            System.out.println(username+" "+password);
            // 短信相关的必须参数
            String mobile = phone;
            String message = content;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = sdf.format(new Date());
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(username.getBytes("utf8"));
            md5.update(password.getBytes("utf8"));
            md5.update(timestamp.getBytes("utf8"));
            md5.update(content.getBytes("utf8"));

            String passwordMd5 = encoder.encodeToString(md5.digest());
            passwordMd5 = URLEncoder.encode(passwordMd5,"utf-8");

            String url = "http://" + INSTANCE.ip + ":" + INSTANCE.port + "/mt";
            // 装配GET所需的参数
            StringBuilder sb = new StringBuilder(url);
            sb.append("?dc=8"); // unicode编码
            sb.append("&sm=").append(URLEncoder.encode(message, "utf8"));
            sb.append("&da=").append(mobile);
            sb.append("&un=").append(username);
            sb.append("&pw=").append(passwordMd5);
            sb.append("&tf=3"); // 表示短信内容为 urlencode+utf8
            sb.append("&rf=2");//json返回
            sb.append("&ts=").append(timestamp);//加密时间戳
            String request = sb.toString();
            // 以GET方式发起请求
            System.out.println(username+" "+request);
            return Request.Get(request).execute().returnContent().asString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //发送系统消息到指定号码
    public static void sendSmsBySystemTemp(String phone,int tempNo,String... params){
        IOThreadUtils.runTask(()->{
            boolean isMarket = isSmsMarketAllow(tempNo);
            if (isSmsAllow(tempNo) || isMarket) {
                //获取短信
                String message = IceRemoteUtil.getMessageByNo(tempNo,params);
                String res = SmsUtil.sendMsg(phone,message,isMarket);
                Application.communicator().getLogger().print(phone +" -> "+ message+" ,res = "+ res);
            }
        });
    }

    //发送短信到所有人
    public static void sendMsgToAllBySystemTemp(int tempNo,String... params){
        IOThreadUtils.runTask(()->{
            boolean isMarket = isSmsMarketAllow(tempNo);
            if (isSmsAllow(tempNo) || isMarket) {
                //获取消息
                String message = IceRemoteUtil.getMessageByNo(tempNo,params);
                //获取所有用户手机号码
                List<String> list =  IceRemoteUtil.getAllStorePhone();
                //循环发送
                for (String phone : list){
                    SmsUtil.sendMsg(phone,message,isMarket);
                }
            }
        });
    }


    public static void main(String[] args) {
        String res = sendMsg("15608447849","【一块医药】尊敬的用户：您收藏的商品：SOD眼霜 降价啦，原价：1800，现价：1200，赶快前去采购吧。",true);
        System.out.println(res);
    }



}