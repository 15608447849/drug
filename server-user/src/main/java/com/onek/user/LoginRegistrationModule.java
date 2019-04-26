package com.onek.user;

import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.context.StoreBasicInfo;
import com.onek.context.UserSession;
import com.onek.entitys.Result;
import com.onek.user.operations.*;
import util.GsonUtils;

import static com.onek.user.operations.StoreBasicInfoOp.getStoreInfoById;

/**
 * 登陆 / 注册 模块
 * lzp
 */
public class LoginRegistrationModule {

    /**
     * 校验手机号码是否存在
     */
    @UserPermission (ignore = true)
    public Result checkPhoneExist(AppContext appContext){
        String json = appContext.param.json;
        StoreRegisterOp op = GsonUtils.jsonToJavaBean(json, StoreRegisterOp.class);
        assert op!=null;
        op.type = 1;
        return op.execute(appContext);
    }

    /**
     * 门店用户注册
     */
    @UserPermission (ignore = true)
    public Result register(AppContext appContext){
        String json = appContext.param.json;
        StoreRegisterOp op = GsonUtils.jsonToJavaBean(json, StoreRegisterOp.class);
        assert op!=null;
        return op.execute(appContext);
    }

    /**
     * 获取验证码
     * 1 图形
     * 2 短信
     */
    @UserPermission (ignore = true)
    public Result obtainVerificationCode(AppContext appContext){
        String json = appContext.param.json;
        VerificationOp op = GsonUtils.jsonToJavaBean(json, VerificationOp.class);
        assert op!=null;
        return op.execute(appContext);
    }

    /**
     * 门店登陆平台系统
     */
    @UserPermission (ignore = true)
    public Result loginStore(AppContext appContext){
        String json = appContext.param.json;
        LoginStoreOp op = GsonUtils.jsonToJavaBean(json, LoginStoreOp.class);
        assert op!=null;
        return op.execute(appContext);
    }
    /**
     * 解锁门店用户锁定
     */
    @UserPermission(ignore = true)
    public Result removeUserLoginLock(AppContext appContext){
        String json = appContext.param.json;
        LoginStoreOp op = GsonUtils.jsonToJavaBean(json, LoginStoreOp.class);
        assert op!=null;
        op.removeLockCache();
        return new Result().success("解锁成功");
    }

    /**
     * 管理员登陆后台系统
     */
    @UserPermission (ignore = true)
    public Result loginBack(AppContext appContext){
        String json = appContext.param.json;
        LoginBackOp op = GsonUtils.jsonToJavaBean(json, LoginBackOp.class);
        assert op!=null;
        return op.execute(appContext);
    }

    /**
     * 修改手机号
     *  修改密码
     *
     */
    public Result changeUserInfo(AppContext appContext){
        String json = appContext.param.json;
        UpdateUserOp op = GsonUtils.jsonToJavaBean(json, UpdateUserOp.class);
        assert op!=null;
        return op.execute(appContext);
    }

    /**
     *  忘记密码
     */
    @UserPermission(ignore = true)
    public Result forgetPassword(AppContext appContext){
        String json = appContext.param.json;
        UpdateUserOp op = GsonUtils.jsonToJavaBean(json, UpdateUserOp.class);
        assert op!=null;
        return op.execute(appContext);
    }

    /**
     * 登出
     */
    public Result logout(AppContext appContext){
        try {
//            UserSession userSession = appContext.getUserSession();
//            appContext.logger.print("用户:"+ userSession+" 登出中...");
            if (appContext.clearTokenByUserSession())  return new Result().success("登出成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Result().fail("登出失败");
    }

    /**
     * 内部调用 获取企业信息
     */
    @UserPermission(ignore = true)
    public StoreBasicInfo getStoreInfo(AppContext appContext){
        int compid = Integer.parseInt(appContext.param.arrays[0]);
        StoreBasicInfo info = new StoreBasicInfo(compid);
        getStoreInfoById(info);
        return info;
    }
    /**
     * 获取门店用户信息
     */
    @UserPermission(allowRoleList = {2},allowedUnrelated = true)
    public Result getStoreSession(AppContext appContext){
        if (appContext.getUserSession().compId > 0){
            StoreBasicInfo info = new StoreBasicInfo(appContext.getUserSession().compId);
            if (getStoreInfoById(info)){
                UserSession userSession = appContext.getUserSession().cloneStoreUserInfo(info);
                return new Result().success(userSession);//返回用户信息
            };
        }
        return new Result().fail("没有企业信息,请关联企业");//返回用户信息
    }

    /**
     * 获取用户信息 - 后台运营使用
     */
    public Result getUserSession(AppContext appContext){
        UserSession userSession = appContext.getUserSession().cloneBackUserInfo();
        return new Result().success(userSession);
    }
}
