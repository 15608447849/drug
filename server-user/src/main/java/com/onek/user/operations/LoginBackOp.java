package com.onek.user.operations;

import com.onek.context.AppContext;
import com.onek.entitys.IOperation;
import com.onek.entitys.Result;

/**
 * @Author: leeping
 * @Date: 2019/3/12 15:05
 */
public class LoginBackOp extends LoginAbsOp implements IOperation<AppContext> {

    String account;
    String password;

    protected LoginBackOp() {
        super(256);
    }

    @Override
    public Result execute(AppContext context) {
        try {
            if (!checkAccountPassword(account,password)) return new Result().fail(error);
            //检测用户名/密码是否正确
            if (!checkSqlAndUserExist(context.remoteIp,account,password)) return new Result().fail(error);
            //关联token-用户信息
            if (relationTokenUserSession(context,false)) return new Result().success("登陆成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Result().fail("登陆失败");
    }
}
