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
     * 功能:  校验手机号码是否存在
     * 参数类型:    json
     * 参数集:  phone=手机号码
     * 返回值:  code=200(存在) data=结果信息
     * 详情说明:
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
     * 功能: 门店注册
     * 参数类型:    json
     * 参数集:  phone=手机号码,password=明文密码,password2=二次确认密码,smsCode=短信验证码
     * storeName=门店名,addressCode=地区码
     * 返回值:  code=200(成功) data=结果信息 ,map.compid = 企业ID
     * 详情说明: 企业修改接口复用
     */
    @UserPermission (ignore = true)
    public Result register(AppContext appContext){
        String json = appContext.param.json;
        //提交用户信息
        RegisterStoreUserOp userOp = GsonUtils.jsonToJavaBean(json, RegisterStoreUserOp.class);
        userOp.type = 3;
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

     /*
      * 功能: 获取验证码
      * 参数类型: json
      * 参数集: phone=手机号码,type=类型(1,图形验证码,2,短信验证码)
      * 返回值: code=200(成功) , data=图形验证码url/结果信息
      * 详情说明:
      */
    @UserPermission (ignore = true)
    public Result obtainVerificationCode(AppContext appContext){
        String json = appContext.param.json;
        VerificationOp op = GsonUtils.jsonToJavaBean(json, VerificationOp.class);
        assert op!=null;
        return op.execute(appContext);
    }


    /**
     * 功能:门店登陆平台系统
     * 参数类型: json
     * 参数集: phone=手机号码,password=密文密码,key=图形验证码key(uncheck不验证) verification=图形验证
     * 返回值: code=200(成功)
     * 详情说明:
     */
    @UserPermission (ignore = true)
    public Result loginStore(AppContext appContext){
        String json = appContext.param.json;
        LoginStoreOp op = GsonUtils.jsonToJavaBean(json, LoginStoreOp.class);
        assert op!=null;
        return op.execute(appContext);
    }


    /*
     * 功能: 门店登陆锁定,管理系统解锁
     * 参数类型: json
     * 参数集: phone=手机号码
     * 返回值:
     * 详情说明:
     */
    @UserPermission(ignore = true)
    public Result removeUserLoginLock(AppContext appContext){
        String json = appContext.param.json;
        LoginStoreOp op = GsonUtils.jsonToJavaBean(json, LoginStoreOp.class);
        assert op!=null;
        op.removeLockCache();
        return new Result().success("解锁成功");
    }
    /*
     * 功能: 管理端登陆后台系统
     * 参数类型: json
     * 参数集: account=手机号码 password=密文密码
     * 返回值:
     * 详情说明:
     */
    @UserPermission (ignore = true)
    public Result loginBack(AppContext appContext){
        String json = appContext.param.json;
        LoginBackOp op = GsonUtils.jsonToJavaBean(json, LoginBackOp.class);
        assert op!=null;
        return op.execute(appContext);
    }
    
    /*
     * 功能: 修改手机号/密码
     * 参数类型: json
     * 参数集: 修改手机号码: oldPhone=旧手机号码,newPhone=新手机号码,smsCode=短信验证码; 修改密码: oldPassword=MD5旧密码,newPassword=明文密码
     * 返回值:
     * 详情说明:
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
     * APP获取门店用户信息
     */
    public UserSession appStoreInfo(AppContext context){
        if (context.getUserSession().compId > 0){
            StoreBasicInfo info = new StoreBasicInfo(context.getUserSession().compId);
            if (getStoreInfoById(info)){
                return context.getUserSession().cloneStoreUserInfo(info);
            };
        }
        return null;//返回用户信息
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
