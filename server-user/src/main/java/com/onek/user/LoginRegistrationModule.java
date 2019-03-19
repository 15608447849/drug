package com.onek.user;

import com.onek.AppContext;
import com.onek.annotation.UserPermission;
import com.onek.entitys.Result;
import com.onek.user.operations.LoginBackOp;
import com.onek.user.operations.LoginStoreOp;
import com.onek.user.operations.RegisterOp;
import com.onek.user.operations.VerificationOp;
import redis.util.RedisUtil;
import util.GsonUtils;

/**
 * 登陆 / 注册 模块
 * lzp
 */
public class LoginRegistrationModule {

    /**
     * 校验手机号码是否存在
     */
    public Result checkPhoneExist(AppContext appContext){
        String json = appContext.param.json;
        RegisterOp op = GsonUtils.jsonToJavaBean(json, RegisterOp.class);
        assert op!=null;
        op.type = 1;
        return op.execute(appContext);
    }

    /**
     * 门店用户注册
     */
    public Result register(AppContext appContext){
        String json = appContext.param.json;
        RegisterOp op = GsonUtils.jsonToJavaBean(json, RegisterOp.class);
        assert op!=null;
        return op.execute(appContext);
    }

    /**
     * 获取验证码
     */
    public Result obtainVerificationCode(AppContext appContext){
        String json = appContext.param.json;
        VerificationOp op = GsonUtils.jsonToJavaBean(json, VerificationOp.class);
        assert op!=null;
        return op.execute(appContext);
    }

    /**
     * 门店登陆平台系统
     */
    public Result loginStore(AppContext appContext){
        String json = appContext.param.json;
        LoginStoreOp op = GsonUtils.jsonToJavaBean(json, LoginStoreOp.class);
        assert op!=null;
        return op.execute(appContext);
    }

    /**
     * 管理员登陆后台系统
     */
    public Result loginBack(AppContext appContext){
        String json = appContext.param.json;
        LoginBackOp op = GsonUtils.jsonToJavaBean(json, LoginBackOp.class);
        assert op!=null;
        return op.execute(appContext);
    }

    /**
     * 登出
     */
    @UserPermission
    public Result logout(AppContext appContext){
        String key = appContext.param.token + "@" + appContext.remoteIp;
        String value = RedisUtil.getStringProvide().get(key);
        RedisUtil.getStringProvide().delete(value);
        RedisUtil.getStringProvide().delete(key);
        return new Result().success("登出成功");
    }
}
