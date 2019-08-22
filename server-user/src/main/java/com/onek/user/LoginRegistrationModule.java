package com.onek.user;

import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.context.StoreBasicInfo;
import com.onek.context.UserSession;
import com.onek.entitys.Result;
import com.onek.server.infimp.IceDebug;
import com.onek.user.operations.*;
import com.onek.util.IceRemoteUtil;
import dao.BaseDAO;
import util.GsonUtils;
import util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.onek.user.operations.StoreBasicInfoOp.*;
import static constant.DSMConst.*;

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
     */
    public Result getUserSession(AppContext appContext){
        return new Result().success(appContext.getUserSession().cloneBackUserInfo());
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


    private static class RelationBean{
        String uid;
        String cid;
        String uphone;
        String upw;
        String cname;
        String caddrcode;
        String caddrcodeStr;
        String caddr;
        boolean isCurrent;
    }
    /***
     * @接口摘要 查询关联用户
     * @业务场景
     * @传参类型 JSON/ARRAY
     * @传参列表
     * @返回列表
     */
    public Result queryRelationUser(AppContext appContext){
        UserSession userSession = appContext.getUserSession();
        String uid =String.valueOf(userSession.userId);
        String sql = "SELECT relacode FROM {{?"+TB_USER_RELA+"}} WHERE uid=?";
        String sql2 = "SELECT uid FROM {{?"+TB_USER_RELA+"}} WHERE relacode = ("+sql+")";
        String sql3 = "SELECT u.uid,u.cid,u.uphone,u.upw,c.cname,c.caddrcode,c.caddr FROM {{?"+TB_SYSTEM_USER+"}} AS u INNER JOIN {{?"+TB_COMP+"}} AS c ON u.cid=c.cid WHERE u.uid IN("+sql2+")";
        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(sql3,uid);
        List<RelationBean> list = new ArrayList<>();
        if (lines.size() > 0){
            for (Object[] rows : lines){
                RelationBean b  = new RelationBean();
                b.uid = StringUtils.obj2Str(rows[0]);
                b.cid = StringUtils.obj2Str(rows[1]);
                b.uphone = StringUtils.obj2Str(rows[2]);
                b.upw = StringUtils.obj2Str(rows[3]);
                b.cname = StringUtils.obj2Str(rows[4]);
                b.caddrcode = StringUtils.obj2Str(rows[5]);
                b.caddr = StringUtils.obj2Str(rows[6]);

                try{
                    b.caddrcodeStr = IceRemoteUtil.getArean(Long.parseLong(b.caddrcode));
                    b.isCurrent = b.uid.equals(uid);
                }catch (Exception ignored){ }

                list.add(b);
            }
        }
        return new Result().success(list);
    }

    public Result tryRelationUser(AppContext appContext){
        try{
            if (appContext.param.arrays.length == 1){
                //删除

            }else{
                //关联
                RelationBean b = GsonUtils.jsonToJavaBean(appContext.param.json,RelationBean.class) ;
                if (b == null || StringUtils.isEmpty(b.uphone,b.upw)) new Result().fail("请输入需要关联账户的用户名/密码");
                //判断手机号码/密码是否有效且角色是否为用户
                String selectSql = "SELECT upw,roleid" +
                        "FROM {{?" + TB_SYSTEM_USER + "}} " +
                        "WHERE cstatus&1=0 AND uphone=?";
                List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql,b.uphone);
                if (lines.size()!=1) return new Result().fail("无法关联此用户,账号或密码不正确");
                String pwd = StringUtils.obj2Str(lines.get(0)[0]);
                if (!pwd.equalsIgnoreCase(b.upw))  return new Result().fail("无法关联此用户,账号或密码不正确");
                int roleid = StringUtils.checkObjectNull(lines.get(0)[1],0);
                if ( (roleid & 2 )== 0)  return new Result().fail("无法关联此用户,不匹配的角色");
                //retrun 陈玉琼的方法
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return new Result().fail("关联失败");
    }


}
