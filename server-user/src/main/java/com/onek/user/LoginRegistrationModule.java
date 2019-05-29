package com.onek.user;

import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.context.StoreBasicInfo;
import com.onek.context.UserSession;
import com.onek.entitys.Result;
import com.onek.user.operations.*;
import dao.BaseDAO;
import util.GsonUtils;

import static com.onek.user.operations.StoreBasicInfoOp.getStoreInfoById;
import static com.onek.user.operations.StoreBasicInfoOp.infoToCache;
import static constant.DSMConst.TB_SYSTEM_USER;

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
        RegisterStoreUserOp op = GsonUtils.jsonToJavaBean(json, RegisterStoreUserOp.class);
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
        //提交用户信息
        RegisterStoreUserOp userOp = GsonUtils.jsonToJavaBean(json, RegisterStoreUserOp.class);
        assert userOp!=null;
        Result result = userOp.execute(appContext);
        if (!result.isSuccess()){
            return result;
        }
        //提交企业信息
        UpdateStoreOp storeOp = GsonUtils.jsonToJavaBean(json, UpdateStoreOp.class);
        assert storeOp!=null;
        result = storeOp.execute(appContext);
        if (!result.isSuccess()){
            UserSession session = appContext.getUserSession();
            if (session!=null && session.compId == 0){
                //删除用户信息
                int i = BaseDAO.getBaseDAO().updateNative( "DELETE FROM {{?" +TB_SYSTEM_USER+"}} WHERE uid=?",session.userId);
                if (i<=0) result.setHashMap("error","注册失败且无法删除此用户信息");
            }
        }
        return result;

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
        op.type = 0;
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
        op.type = 1;
        return op.execute(appContext);
    }

    /**
     * 登出
     */
    public Result logout(AppContext appContext){
        try {
//            UserSession userSession = appContext.getUserSession();
//            appContext.logger.print("用户:"+ userSession+" 登出中...");
            String phone = appContext.getUserSession().phone;
            String ip = appContext.remoteIp;
            if (appContext.clearTokenByUserSession()) {
                return new Result().success("登出成功");
            }
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
        infoToCache(info); //保存信息到缓存
        return info;
    }
    /**
     * 获取门店用户信息
     */
    @UserPermission(allowRoleList = {2})
    public Result getStoreSession(AppContext appContext){
        if (appContext.getUserSession().compId > 0){
            StoreBasicInfo info = new StoreBasicInfo(appContext.getUserSession().compId);
            if (getStoreInfoById(info)){
                UserSession userSession = appContext.getUserSession().cloneStoreUserInfo(info);
                return new Result().success(userSession);//返回用户信息
            };
        }
        return new Result().success(null);//返回用户信息
    }

    /**
     * 获取用户信息 - 后台运营使用
     */
    public Result getUserSession(AppContext appContext){
        UserSession userSession = appContext.getUserSession().cloneBackUserInfo();
        return new Result().success(userSession);
    }

    /**
     * 判断是否已经登陆
     */
    @UserPermission(ignore = true)
    public Result checkStoreLoginStatus(AppContext appContext){
        return new Result().success(appContext.getUserSession() != null);
    }
}
