package com.onek.server.infimp;

import Ice.Current;
import com.onek.entitys.Result;
import com.onek.server.inf.IRequest;

/**
 * @Author: leeping
 * @Date: 2019/3/7 13:37
 * 拦截器
 */

public interface IServerInterceptor {
    /**
     * packagePath 包名
     * serverName 对服务名进行拦截
     * current 中可以获取客户端IP信息 进行拦截
     * request 中可以获取参数信息进行拦截
     * 如果拦截返回结果,否则NULL
     */
    Result interceptor(String packagePath,String serverName, IRequest request, IApplicationContext context) throws Exception;
}
