package com.onek.user;

import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.context.StoreBasicInfo;
import com.onek.context.UserSession;
import com.onek.entitys.Result;
import com.onek.server.infimp.IceDebug;
import com.onek.user.operations.*;
import dao.BaseDAO;
import util.GsonUtils;

import static com.onek.user.operations.StoreBasicInfoOp.*;
import static constant.DSMConst.TB_SYSTEM_USER;

/**
 * 登陆 / 注册 模块
 * lzp
 * @服务名 userServer
 */
public class LoginRegistrationModule {

    /**
     * @接口摘要 校验手机号码是否存在
     * @业务场景 注册 忘记密码
     * @传参类型 JSON
     * @传参列表 phone - 手机号码
     * @返回列表 code - 200 - 成功 , message - 成功/失败信息
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
     * @接口摘要 门店注册
     * @业务场景
     * @传参类型 JSON
     * @传参列表 phone=手机号码,password=明文密码,password2=二次确认密码,smsCode=短信验证码,code=200(成功) data=结果信息 ,map.compid = 企业ID
     * @返回列表
     *  code-200-成功
     */
    @IceDebug
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

    /**
     * @接口摘要 获取验证码
     * @业务场景
     * @传参类型 JSON
     * @传参列表 phone=手机号码,type=类型(1,图形验证码,2,短信验证码)
     * @返回列表
     *   code=200(成功) , data=图形验证码url/结果信息
     */
    @UserPermission (ignore = true)
    public Result obtainVerificationCode(AppContext appContext){
        String json = appContext.param.json;
        VerificationOp op = GsonUtils.jsonToJavaBean(json, VerificationOp.class);
        assert op!=null;
        return op.execute(appContext);
    }

    /***
     * @接口摘要 门店登陆平台系统
     * @业务场景
     * @传参类型 JSON
     * @传参列表 phone=手机号码,password=密文密码,key=图形验证码key(uncheck不验证) verification=图形验证
     * @返回列表
     *code=200(成功)
     */
    @UserPermission (ignore = true)
    public Result loginStore(AppContext appContext){
        String json = appContext.param.json;
        LoginStoreOp op = GsonUtils.jsonToJavaBean(json, LoginStoreOp.class);
        assert op!=null;
        return op.execute(appContext);
    }
    /**
     * @接口摘要 门店登陆锁定,管理系统解锁
     * @业务场景
     * @传参类型 JSON
     * @传参列表 phone=手机号码
     * @返回列表 code-200-成功
     */
    @UserPermission(ignore = true)
    public Result removeUserLoginLock(AppContext appContext){
        String json = appContext.param.json;
        LoginStoreOp op = GsonUtils.jsonToJavaBean(json, LoginStoreOp.class);
        assert op!=null;
        op.removeLockCache();
        return new Result().success("解锁成功");
    }

    /***
     * @接口摘要 管理端登陆后台系统
     * @业务场景
     * @传参类型 JSON/ARRAY
     * @传参列表 account=手机号码 password=密文密码
     * @返回列表
     *  code-200-成功 , message 成功/失败结果提示
     */
    @UserPermission (ignore = true)
    public Result loginBack(AppContext appContext){
        String json = appContext.param.json;
        LoginBackOp op = GsonUtils.jsonToJavaBean(json, LoginBackOp.class);
        assert op!=null;
        return op.execute(appContext);
    }

    /**
     * @接口摘要 修改手机号/密码
     * @业务场景
     * @传参类型 JSON
     * @传参列表 修改手机号码: oldPhone=旧手机号码,newPhone=新手机号码,smsCode=短信验证码; 修改密码: oldPassword=MD5旧密码,newPassword=明文密码
     * @返回列表
     *  code200 修改成功
     */
    public Result changeUserInfo(AppContext appContext){
        String json = appContext.param.json;
        UpdateUserOp op = GsonUtils.jsonToJavaBean(json, UpdateUserOp.class);
        assert op!=null;
        op.type = 0;
        return op.execute(appContext);
    }

    /**
     * @接口摘要 忘记密码
     * @业务场景
     * @传参类型 JSON
     * @传参列表
     *  oldPhone 旧手机号码 , newPassword 新密码 ,smsCode短信验证码
     * @返回列表 code200 成功 / message 成功/失败结果
     *
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
     * @接口摘要 登出
     * @业务场景
     * @传参类型 JSON/ARRAY
     * @传参列表
     *
     * @返回列表
     *
     */
    public Result logout(AppContext appContext){
        try {
            if (appContext.clearTokenByUserSession()) {
                return new Result().success("登出成功");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Result().fail("登出失败");
    }

    /**
     * @接口摘要 内部调用,获取企业信息
     */
    @UserPermission(ignore = true)
    public StoreBasicInfo getStoreInfo(AppContext appContext){
        int compid = Integer.parseInt(appContext.param.arrays[0]);
        return updateCompInfoToCacheById(compid,false);
    }

    /**
     * @接口摘要 用户获取信息
     * @业务场景
     * @传参类型
     * @传参列表
     *
     * @返回列表
     *      int userId; //用户ID
     *      long roleCode; //角色复合码
     *      String phone; //手机号码
     *      String userName;//姓名
     *      int compId;//企业ID
     *      StoreBasicInfo comp; //公司信息
     */
    public Result getStoreSession(AppContext appContext){
        return new Result().success( appStoreInfo(appContext));//返回用户信息
    }

    /**
     * @接口摘要 APP获取门店用户信息
     * @业务场景
     * @传参类型 JSON/ARRAY
     * @传参列表
     *
     * @返回列表
     *
     */
    public UserSession appStoreInfo(AppContext context){

        if (context.getUserSession().compId > 0){
            //返回用户信息
            StoreBasicInfo info = updateCompInfoToCacheById(context.getUserSession().compId,false);
            return context.getUserSession().cloneStoreUserInfo(info);
        }
        return null;
    }

    /***
     * @接口摘要 获取用户信息 - 后台运营使用
     * @业务场景
     * @传参类型
     * @传参列表
     *
     * @返回列表
     *      int userId; //用户ID
     *      long roleCode; //角色复合码
     *      String phone; //手机号码
     *      String userName;//姓名
     *      int compId;//企业ID
     *      int belong;//所属ID
     *      StoreBasicInfo comp; //公司信息
     *
     */
    public Result getUserSession(AppContext appContext){
        UserSession userSession = appContext.getUserSession().cloneBackUserInfo();
        return new Result().success(userSession);
    }

    /**
     * @接口摘要 判断是否已经登陆
     * @业务场景
     * @传参类型
     * @传参列表
     *
     * @返回列表 data-登陆成功-true
     *
     */
    @UserPermission(ignore = true)
    public Result checkStoreLoginStatus(AppContext appContext){
        return new Result().success(appContext.getUserSession() != null);
    }
}
