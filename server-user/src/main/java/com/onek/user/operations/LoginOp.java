package com.onek.user.operations;

import com.onek.AppContext;
import com.onek.UserSession;
import com.onek.entitys.IOperation;
import com.onek.entitys.Result;
import redis.util.RedisUtil;
import util.EncryptUtils;
import util.GsonUtils;

/**
 * @Author: leeping
 * @Date: 2019/3/12 15:05
 */
public class LoginOp  implements IOperation<AppContext> {

    String username;
    String password;
    String verification;


    @Override
    public Result execute(AppContext context) {
        try {
            //检测验证码是否正确
            if (!checkVerification()) return new Result().fail("验证码不正确");

            //检测用户名/密码是否正确
            if (!checkSqlAndUserExist()) return new Result().fail("用户名或密码不正确");

            //创建token标识
            String token = createToken(context);
            UserSession session = readUserInfo();

            //关联token-用户信息
            if (relationTokenUserSession(context,token,session)) return new Result().success("登陆成功");
            else return new Result().success("无法关联用户信息");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Result().fail("登陆失败");
    }

    private UserSession readUserInfo() {
        UserSession userSession = new UserSession();
        userSession.oid = 0;
        userSession.cstatus = 0;
        userSession.roleid = 1000;
        userSession.uname = "超级管理员-测试用户";
        return userSession;
    }

    private boolean relationTokenUserSession(AppContext context, String token, UserSession session) {
        String res = RedisUtil.getStringProvide().set(token, GsonUtils.javaBeanToJson(session));
        context.logger.print("存入: k = " + token +" ; v = " + res );
        if (res.equals("OK")){
            res =RedisUtil.getStringProvide().set(context.param.token ,token);
            context.logger.print("存入: k = " + context.param.token +" ; v = " + res );
            if (res.equals("OK")){
                return true;
            }
        }
        //RedisUtil.getStringProvide().expire(token, appContext.SESSION_EFFECTIVE_SESSIONS); 暂时没设置时效性
       return false;
    }

    private String createToken(AppContext context) {
        context.param.token = context.param.token + "@" + context.remoteIp;
        return EncryptUtils.encryption(context.param.token);
    }

    private boolean checkSqlAndUserExist() {
        if (username.equals("admin") || password.equals("admin")){
            return true;
        }
        return false;
    }

    private boolean checkVerification() {
        return true;
    }



}
