package com.onek.user.operations;

import com.google.gson.reflect.TypeToken;
import com.onek.AppContext;
import com.onek.entitys.IOperation;
import com.onek.entitys.Result;
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

/**
 * @Author: leeping
 * @Date: 2019/3/14 13:42
 * 验证码操作
 */
public class VerificationOp implements IOperation<AppContext> {
    public int type = 0;
    @Override
    public Result execute(AppContext context) {
        if (type == 1) return generateImageCode();//获取图形验证码
        return new Result().fail("未知的操作类型");
    }

    private Result generateImageCode() {
        try {
            String code = getRandomCode(4);
            InputStream inputStream = ImageVerificationUtils.generateImage(77,33,code);
            HttpRequest result = new HttpRequest();
            String key = EncryptUtils.encryption(code); //k = code 的MD5 ,v = code, 存入redis
            RedisUtil.getStringProvide().set(key,code);
            RedisUtil.getStringProvide().expire(key, 60); //60秒有效
            String json = result.addStream(inputStream,EncryptUtils.encryption("image_verification_code"),key)
                    .fileUploadUrl("http://192.168.1.241:8888/upload").getRespondContent(); // k作为文件名 文件链接传递到前端
            List<HashMap<String,String>> list = GsonUtils.jsonToJavaBean(json, new TypeToken<List<HashMap<String,String>>>(){}.getType());
            assert list != null;
            HashMap<String,String> map = new HashMap<>();
            map.put("key",key);//前端需要传递到后台系统,从而验证code
            map.put("url",list.get(0).get("httpUrl"));
           return new Result().success(GsonUtils.javaBeanToJson(map));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Result().fail("无法生成验证图片");
    }
}
