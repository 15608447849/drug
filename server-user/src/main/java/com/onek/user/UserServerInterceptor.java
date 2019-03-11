package com.onek.user;

import Ice.Current;
import com.onek.entitys.Result;
import com.onek.server.inf.IRequest;
import com.onek.server.infimp.IApplicationContext;
import com.onek.server.infimp.IServerInterceptor;

import java.util.HashMap;

/**
 * @Author: leeping
 * @Date: 2019/3/7 14:28
 */
public class UserServerInterceptor implements IServerInterceptor {


    @Override
    public Result interceptor(String serverName, IRequest request, IApplicationContext context)  {
        context.logger.print(" - " + context.param.json);
        HashMap<String,String> map = new HashMap<>();
        map.put("page","500");
        map.put("user","156s1a6156da12sd1qw16dqwasd23");

        if (context.param.json != null) return new Result().intercept(map);
        return null;
    }
}
