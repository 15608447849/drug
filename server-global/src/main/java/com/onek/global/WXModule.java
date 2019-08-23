package com.onek.global;

import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import redis.util.RedisUtil;
import util.GsonUtils;
import util.http.HttpUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @Author: leeping
 * @Date: 2019/8/23 10:22
 * 微信公众号后台处理
 */
public class WXModule {

//    private static Thread thread = new Thread(){
//        @Override
//        public void run() {
//            while (true){
//
//            }
//        }
//    };
//
//    static {
//        thread.start();
//    }

    public static String APPID = "wxa0777da1f8b79bbf";

    public static String SECRET = "2f92b1e8842e9d808da261cd8b714bb8";

    /**
     * 1.获取 ACCESS_TOKEN
     */
    public static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="+APPID+"&secret="+SECRET;
    private static final String ACCESS_TOKEN_KEY = "_ACCESS_TOKEN_KEY";

    /**
     * 2. JS API
     */
    public final static String JS_API_TICKET_URL = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=%s&type=jsapi";
    public final static String JS_API_TICKET_KEY = "_JS_API_TICKET_KEY";

    /**
     * 获取OPENID
     */
    public static final String GET_OPEN_ID = "https://api.weixin.qq.com/sns/oauth2/access_token?appid="+APPID+"&secret="+SECRET+"&code=%s&grant_type=authorization_code";




    private static String _GET(String url){
       String res =  HttpUtil.formText(url,"GET",null);
        System.out.println("访问: "+ url+"\n\t"+res);
       return res;
    }


    private static final class JsonBean{
        private String access_token;
        private int expires_in;
        private String ticket;
    }

    //{"access_token":"24_4KOTzqhpp1sBk-9QXUcBKqEtMbh-J1_h8gQQN-8K2tphENU8LY1em-vdVZQBDlwrJkSR4ANGA5dqs9cqYW5uNcPil3WU2lwLaNpqxZaeB3x-FLTm1zjxEp0jmyUeMLHWxsPN-NY9cRzi4c7HPVHgAHAGMY","expires_in":7200}
    private static String getAccessToken() {
        String access_token = RedisUtil.getStringProvide().get(ACCESS_TOKEN_KEY);
        if (access_token == null){
            JsonBean bean = GsonUtils.jsonToJavaBean(_GET(ACCESS_TOKEN_URL),JsonBean.class);
            if (bean != null && bean.access_token!=null && bean.access_token.length()>0) {
                RedisUtil.getStringProvide().set(ACCESS_TOKEN_URL,bean.access_token);
                RedisUtil.getStringProvide().expire(ACCESS_TOKEN_URL, bean.expires_in-1);
                access_token = bean.access_token;
            }
        }
        return access_token;
    }


//{"errcode":0,"errmsg":"ok","ticket":"sM4AOVdWfPE4DxkXGEs8VLLlpggcWBKW9KA-xxUfk3Iy4xwiTBik3WhCRcNZ0u6tqKjcaNUdMTfueq2NW2ncOw","expires_in":7200}
    private static String getJsapiTicket(){
        String jsapi_tocket = RedisUtil.getStringProvide().get(JS_API_TICKET_KEY);
        if (jsapi_tocket == null){
            JsonBean bean = GsonUtils.jsonToJavaBean(_GET(String.format(JS_API_TICKET_URL,getAccessToken())),JsonBean.class);
            if (bean != null && bean.ticket!=null && bean.ticket.length()>0) {
                RedisUtil.getStringProvide().set(JS_API_TICKET_KEY,bean.ticket);
                RedisUtil.getStringProvide().expire(JS_API_TICKET_KEY, bean.expires_in-1);
                jsapi_tocket = bean.ticket;
            }
        }
        return jsapi_tocket;
    }

    private static Map<String, String> sign( String url) {
       Map<String, String> ret = new HashMap<>();
       String nonce_str = UUID.randomUUID().toString();
       String timestamp =  Long.toString(System.currentTimeMillis() / 1000);
       String ticket = getJsapiTicket();

        //注意这里参数名必须全部小写，且必须有序
        String str = "jsapi_ticket=" + ticket  +
                "&noncestr=" + nonce_str +
                "&timestamp=" + timestamp +
                "&url=" + url;
        System.out.println(str);

        String signature = null;
        try {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(str.getBytes(StandardCharsets.UTF_8));
            Formatter formatter = new Formatter();
            for (byte b : crypt.digest()) {
                formatter.format("%02x", b);
            }
            signature = formatter.toString();
            formatter.close();

        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        ret.put("url", url);
        ret.put("appId", APPID);
        ret.put("jsapi_ticket", ticket);
        ret.put("nonceStr", nonce_str);
        ret.put("timestamp", timestamp);
        ret.put("signature", signature);

        return ret;
    }



    public static void main(String[] args) {
        String url = "http://app.onek56.com/wx/?code=0019QzD20NwlVJ1q2aE20LYND209QzDB&state=1";
        Map<String,String> map = sign(url);
        System.out.println(map);
    }


    @UserPermission(ignore = true)
    public Result getSingInfo(AppContext context){
        try {
            if (context.param.arrays.length==1){
                String url = context.param.arrays[0];
                return new Result().success(sign(url));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Result().fail("获取微信签名失败");
    }

}
