package com.onek.user.operations;

import com.google.gson.reflect.TypeToken;
import com.onek.context.AppContext;
import com.onek.util.fs.FileServerUtils;
import com.onek.entitys.IOperation;
import com.onek.entitys.Result;
import com.onek.user.service.USProperties;
import redis.util.RedisUtil;
import util.EncryptUtils;
import util.GsonUtils;
import util.ImageVerificationUtils;
import util.http.HttpRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

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
        if (type == 2) return sendSmsCode();//获取短信验证码
        return new Result().fail("未知的操作类型");
    }

    private Result generateImageCode() {
        try {
            String code = getRandomCode(4);
            InputStream inputStream = ImageVerificationUtils.generateImage(77,33,code);
            HttpRequest result = new HttpRequest();
            String key = EncryptUtils.encryption(code); //k = code 的MD5 ,v = code, 存入redis

            String json = result.addStream(inputStream,EncryptUtils.encryption("image_verification_code"),key+".png")
                    .fileUploadUrl(FileServerUtils.fileUploadAddress())
                    .getRespondContent(); // k作为文件名 文件链接传递到前端
            List<HashMap<String,String>> list = GsonUtils.jsonToJavaBean(json, new TypeToken<List<HashMap<String,String>>>(){}.getType());
            assert list != null;
            HashMap<String,String> map = new HashMap<>();
            map.put("key",key);//前端需要传递到后台系统,从而验证code
            map.put("url",list.get(0).get("httpUrl"));

            String res = RedisUtil.getStringProvide().set(key,code);
            if (res.equals("OK")){
                RedisUtil.getStringProvide().expire(key, USProperties.INSTANCE.vciSurviveTime); // 3分钟内有效
            }
           return new Result().success(GsonUtils.javaBeanToJson(map));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Result().fail("无法生成验证图片");
    }

    //发送短信验证码-等待接入短信接口
    private Result sendSmsCode() {
        String code = getRandomCodeByNum(6);
        //存入缓存
        String res = RedisUtil.getStringProvide().set(phone,code);
        if (res.equals("OK")){
            RedisUtil.getStringProvide().expire(phone, USProperties.INSTANCE.smsSurviveTime); // 5分钟内有效
            //发送短信
            return new Result().success("手机号:"+ phone+",测试验证码:"+code);
        }
        return new Result().fail("获取短信验证码失败");
    }
}
