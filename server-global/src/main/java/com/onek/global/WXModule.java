package com.onek.global;

import com.google.gson.reflect.TypeToken;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.util.FileServerUtils;
import redis.util.RedisUtil;
import util.GsonUtils;
import util.http.HttpRequest;
import util.http.HttpUtil;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
    private static String APPID = "wxa0777da1f8b79bbf";

    private static String SECRET = "2f92b1e8842e9d808da261cd8b714bb8";

    /**
     * 1.获取 ACCESS_TOKEN
     */
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="+APPID+"&secret="+SECRET;
    private static final String ACCESS_TOKEN_KEY = "_ACCESS_TOKEN_KEY";

    /**
     * 2. JS API
     */
    private final static String JS_API_TICKET_URL = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=%s&type=jsapi";
    private final static String JS_API_TICKET_KEY = "_JS_API_TICKET_KEY";

    /**
     * 获取OPENID
     */
    private static final String OPEN_ID_URL = "https://api.weixin.qq.com/sns/oauth2/access_token?appid="+APPID+"&secret="+SECRET+"&code=%s&grant_type=authorization_code";

    /**
     * 获取图片等媒体文件
     */
    public final static String GET_MEDIA_URL = "https://api.weixin.qq.com/cgi-bin/media/get?access_token=%s&media_id=%s";



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

    private static class Param{
        String url;
        String code;

        String mediaId;
        String savePath;
        String fileName;
    }

    @UserPermission(ignore = true)
    public Result getInfo(AppContext context){
        try {
            Param p = GsonUtils.jsonToJavaBean(context.param.json,Param.class);
            if (p!=null && p.url!=null && p.url.length() > 0){
                return new Result().success(sign(p.url));
            }
            if (p!=null && p.code !=null && p.code.length() > 0){
                return new Result().success(_GET(String.format(OPEN_ID_URL,p.code)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Result().fail("获取微信信息失败");
    }

    @UserPermission(ignore = true)
    public Result downloadImage(AppContext context){
        try {
            Param p = GsonUtils.jsonToJavaBean(context.param.json,Param.class);
            assert p!=null;
            String token = getAccessToken();
            String url = String.format(GET_MEDIA_URL,token,p.mediaId);

            HttpsURLConnection httpUrlConn = null;
            InputStream in = null;
            try{
                httpUrlConn = (HttpsURLConnection) new URL(url).openConnection();
                httpUrlConn.setRequestMethod("GET");
                httpUrlConn.connect();
                in = httpUrlConn.getInputStream();
                //上传图片
                String json = new HttpRequest().addStream(in, p.savePath,  p.fileName)
                        .fileUploadUrl(FileServerUtils.fileUploadAddress())//文件上传URL
                        .getRespondContent();
                HashMap<String,Object> maps = GsonUtils.jsonToJavaBean(json,new TypeToken<HashMap<String,Object>>(){}.getType());
                return new Result().success(maps);
            }catch (Exception e){
                throw e;
            }finally {
                if (in!=null) try { in.close(); } catch (IOException ignored) { }
                if (httpUrlConn!=null )try { httpUrlConn.disconnect(); } catch (Exception ignored) { }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Result().fail("图片无法获取");
    }



}
