package com.onek.user.operations;

import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import com.onek.context.AppContext;
import com.onek.entitys.IOperation;
import com.onek.entitys.Result;
import com.onek.user.service.USProperties;
import com.onek.util.SmsTempNo;
import com.onek.util.SmsUtil;
import com.onek.util.fs.FileServerUtils;
import redis.util.RedisUtil;
import util.EncryptUtils;
import util.GsonUtils;
import util.ImageVerificationUtils;
import util.StringUtils;
import util.http.HttpRequest;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import static util.ImageVerificationUtils.getRandomCode;
import static util.ImageVerificationUtils.getRandomCodeByNum;

/**
 * @Author: leeping
 * @Date: 2019/3/14 13:42
 * 验证码操作
 */
public class VerificationOp implements IOperation<AppContext> {





    public int type = 0;
    public String phone; //手机号

    public VerificationOp setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public VerificationOp setType(int type) {
        this.type = type;
        return this;
    }

    @Override
    public Result execute(AppContext context) {
        if (type == 1) return generateImageCode();//获取图形验证码
        if (type == 2) return sendSmsCode(SmsTempNo.MESSAGE_AUTHENTICATION_CODE);//获取注册短信验证码
        if (type == 3) return sendSmsCode(SmsTempNo.MODIFY_THE_PHONE);//获取修改手机 短信验证码
        if (type == 4) return sendSmsCode(SmsTempNo.CHANGE_PASSWORD);//获取修改密码 短信验证码
        return new Result().fail("未知的操作类型");
    }

    //生成图形验证码
    private Result generateImageCode() {
        try {
            String code = getRandomCode(4);
            InputStream inputStream = ImageVerificationUtils.generateImage(77,33,code);

            String key = EncryptUtils.encryption(code);

            RedisUtil.getStringProvide().set(key,code); //k = code 的MD5 ,v = code, 存入redis
            RedisUtil.getStringProvide().expire(key, USProperties.INSTANCE.vciSurviveTime); // 3分钟内有效

            String json = new HttpRequest().addStream(
                    inputStream,
                    FileServerUtils.defaultVerificationDir(),  //远程路径
                    key+".png"  //k作为文件名
                    )
                    .fileUploadUrl(FileServerUtils.fileUploadAddress())//文件上传URL
                    .getRespondContent();

            HashMap<String,Object> maps = GsonUtils.jsonToJavaBean(json,new TypeToken<HashMap<String,Object>>(){}.getType());
            ArrayList<LinkedTreeMap<String,Object>> list = (ArrayList<LinkedTreeMap<String, Object>>) maps.get("data");
            assert list != null;
            HashMap<String,String> map = new HashMap<>();
            map.put("key",key);//前端需要传递到后台系统,从而验证code
            map.put("url",list.get(0).get("httpUrl").toString());

            final String filePath = list.get(0).get("relativePath").toString();
            //开启定时器
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    new HttpRequest().deleteFile(FileServerUtils.fileDeleteAddress(),filePath);
                }
            },USProperties.INSTANCE.vciSurviveTime * 1000);
            // 文件链接传递到前端
           return new Result().success(GsonUtils.javaBeanToJson(map));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Result().fail("无法生成验证图片");
    }

    //发送短信验证码-等待接入短信接口
    private Result sendSmsCode(int tempNo) {
        String code = genSmsCodeStoreCache(phone);
        if (!StringUtils.isEmpty(code)){
            SmsUtil.sendSmsBySystemTemp(phone,tempNo,code);
            return new Result().success("已发送手机短信验证码,请查收");
        }
        return new Result().fail("获取短信验证码失败");
    }

    private static String genSmsCodeStoreCache(String phone){
        String code = getRandomCodeByNum(6);
        //存入缓存
        String res = RedisUtil.getStringProvide().set("SMS"+phone,code);
        if (res.equals("OK")){
            RedisUtil.getStringProvide().expire("SMS"+phone, USProperties.INSTANCE.smsSurviveTime); // 5分钟内有效
            return code;
        }
        return null;
    }

}
