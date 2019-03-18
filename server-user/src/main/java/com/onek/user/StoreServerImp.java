package com.onek.user;

import com.onek.AppContext;
import com.onek.annotation.UserPermission;
import com.onek.entitys.Result;
import com.onek.user.operations.LoginOp;
import com.onek.user.operations.RegisterOp;
import com.onek.user.operations.VerificationOp;
import org.hyrdpf.ds.AppConfig;
import util.GsonUtils;

public class StoreServerImp {

    static {
        AppConfig.initLogger();
        AppConfig.initialize();
    }

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
     *用户注册
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
     * 登陆系统
     */
    public Result login(AppContext appContext){
        String json = appContext.param.json;
        LoginOp op = GsonUtils.jsonToJavaBean(json, LoginOp.class);
        assert op!=null;
        return op.execute(appContext);
    }


}
