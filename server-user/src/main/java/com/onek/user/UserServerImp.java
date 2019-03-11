package com.onek.user;

import Ice.Current;
import Ice.Logger;
import com.onek.AppContext;
import com.onek.UserSession;
import com.onek.annotation.UserPermission;
import com.onek.permission.PermissionStatus;
import com.onek.server.inf.IParam;
import com.onek.entitys.Result;
import com.onek.server.infimp.IApplicationContext;
import redis.util.RedisUtil;
import util.GsonUtils;
import util.TokenUtil;

public class UserServerImp {

    public Result login(AppContext appContext){

       int userid = 29;
       int storeid = 593939;
       String token = TokenUtil.getInstance().makeToken(new String[]{userid+"", storeid+""});
        UserSession u = new UserSession();
        u.setUname("张小佳");
        u.setRoleid(1);
       if(token != null){
           u.setToken(token);
           String json = GsonUtils.javaBeanToJson(u);
           RedisUtil.getStringProvide().set(token, json);
           RedisUtil.getStringProvide().expire(token, appContext.SESSION_EFFECTIVE_SESSIONS);
       }
       return new Result().success("登陆成功");
    }

    @UserPermission(mode = PermissionStatus.ALREADY_LOGGED)
    public Result getUser(AppContext appContext){
        return new Result().success("获取成功"+appContext.getUserSession().getUname());
    }

}
