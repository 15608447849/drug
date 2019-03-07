package com.onek.user;

import Ice.Current;
import com.onek.server.inf.IRequest;
import com.onek.server.infimp.IServerInterceptor;

/**
 * @Author: leeping
 * @Date: 2019/3/7 14:28
 */
public class UserServerInterceptor implements IServerInterceptor {
    @Override
    public boolean interceptor(String serverName, IRequest request, Current current) {
        return request.param.json == null;
    }
}
