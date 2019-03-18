package com.onek.user.operations;

import com.onek.AppContext;
import com.onek.UserSession;
import com.onek.entitys.IOperation;
import com.onek.entitys.Result;
import constant.DSMConst;
import dao.BaseDAO;
import org.hyrdpf.ds.AppConfig;
import redis.util.RedisUtil;
import util.EncryptUtils;
import util.GsonUtils;
import util.StringUtils;

import java.util.List;

/**
 * @Author: leeping
 * @Date: 2019/3/12 15:05
 */
public class LoginOp  implements IOperation<AppContext> {

    String phone;
    String password;
    String key;
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
        if (StringUtils.isEmpty(phone)) return false;
        String selectSql = "SELECT uid,upw,times FROM {{?" + DSMConst.D_SYSTEM_USER + "}} WHERE cstatus&1 = 0 AND uphone = ?";
        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql,phone);
        if (lines.size()>0){
            Object[] objects = lines.get(0);
            if (objects[1].equals(password)) {
                //密码正确

//                String updateSql = "UPDATE uid,upw,times FROM {{?" + DSMConst.D_SYSTEM_USER + "}} WHERE cstatus&1 = 0 AND uphone = ?";
            }
            System.out.println(objects[0]+" "+ objects[1]+" "+objects[2] );
        }
        return false;
    }

    //检测图形验证码
    private boolean checkVerification() {
        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(verification)) return false;
        String code = RedisUtil.getStringProvide().get(key);
        return verification.equals(code);
    }



}
