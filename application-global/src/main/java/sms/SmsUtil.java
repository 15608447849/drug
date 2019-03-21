package sms;

import org.apache.http.client.fluent.Request;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

import static Ice.Application.communicator;

/**
 * @program: framework
 * @description: 发送短信工具
 * @author: liu yan
 * @create: 2018-07-13 09:24
 */
public class SmsUtil {

    private final static SmsProperties PROPERTIES_UTIL = SmsProperties.getInstance();
    private String username = PROPERTIES_UTIL.getSmsUserName();
    private String password = PROPERTIES_UTIL.getSmsPassword();
    private String ip = PROPERTIES_UTIL.getSmsIp();
    private String port = PROPERTIES_UTIL.getSmsPort();

    private static SmsUtil smsUtil = new SmsUtil();

    public static SmsUtil getInstance() {
        return smsUtil;
    }

    private SmsUtil() {
    }


    public String sendMsg(String phone, String context) throws IOException, NoSuchAlgorithmException {


        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = sdf.format(new Date());
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(username.getBytes("utf8"));
        md5.update(password.getBytes("utf8"));
        md5.update(timestamp.getBytes("utf8"));
        md5.update(context.getBytes("utf8"));

        ;
        Base64.Encoder encoder = Base64.getEncoder();
        String passwordMd5 = encoder.encodeToString(md5.digest());
        passwordMd5 = URLEncoder.encode(passwordMd5);
        /*String passwordMd5 = Base64.encodeBase64String(md5.digest());*/
        String url = "http://" + ip + ":" + port + "/mt";
        // 短信相关的必须参数
        String mobile = phone;
/*        String extendCode = "4443";*/
        String message = context;

        // 装配GET所需的参数
        StringBuilder sb = new StringBuilder(2000);
        sb.append(url);
        sb.append("?dc=8"); // unicode编码
        sb.append("&sm=").append(URLEncoder.encode(message, "utf8"));
        sb.append("&da=").append(mobile);
/*        sb.append("&sa=").append( extendCode );*/
        sb.append("&un=").append(username);
        sb.append("&pw=").append(passwordMd5);
        sb.append("&tf=3"); // 表示短信内容为 urlencode+utf8
        sb.append("&rf=2");//json返回
        sb.append("&ts=").append(timestamp);//加密时间戳
        String request = sb.toString();

//        communicator().getLogger().print("request:" + request);
        System.out.println("request:" + request);

        // 以GET方式发起请求
        String result = Request.Get(request).execute().returnContent().asString();

//        communicator().getLogger().print("result" + result);
        System.out.println("result" + result);

        return result;
    }

    public String fetch() throws NoSuchAlgorithmException, IOException {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = sdf.format(new Date());
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(username.getBytes("utf8"));
        md5.update(password.getBytes("utf8"));
        md5.update(timestamp.getBytes("utf8"));

        Base64.Encoder encoder = Base64.getEncoder();
        String passwordMd5 = encoder.encodeToString(md5.digest());
        passwordMd5 = URLEncoder.encode(passwordMd5);

        String url = "http://" + ip + ":" + port + "/mo";
        // 装配GET所需的参数
        StringBuilder sb = new StringBuilder(2000);
        sb.append(url);
        sb.append("?");
        sb.append("un=").append(username);
        sb.append("&pw=").append(passwordMd5);
        sb.append("&rf=2");//json返回
        sb.append("&ts=").append(timestamp);//加密时间戳
        String request = sb.toString();

        communicator().getLogger().print("request:" + request);

        // 以GET方式发起请求
        String result = Request.Get(request).execute().returnContent().asString();

        communicator().getLogger().print("result" + result);


        return result;
    }

    public static class smsRetrun {
        private boolean success;
        private Long id;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }


}