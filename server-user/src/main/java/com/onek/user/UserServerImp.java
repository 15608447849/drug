package com.onek.user;

import com.onek.AppContext;
import com.onek.annotation.UserPermission;
import com.onek.entitys.Result;
import com.onek.user.operations.LoginOp;
import util.GsonUtils;

public class UserServerImp {


    /**
     * 登陆系统
     */
    public Result login(AppContext appContext){
        String json = appContext.param.json;
        LoginOp op = GsonUtils.jsonToJavaBean(json, LoginOp.class);
        assert op!=null;
        return op.execute(appContext);
    }

    @UserPermission
    public Result loginSuccessTest(AppContext appContext){
        return new Result().success("哈哈哈哈哈哈哈");
    }


//    @UserPermission(mode = PermissionStatus.ALREADY_LOGGED)
//    public Result getUser(AppContext appContext){
//
//        return new Result().success("获取成功"+appContext.getUserSession().getUname());
//    }

}
