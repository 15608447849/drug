package com.onek.server.infimp;

import com.onek.entitys.Result;

/**
 * @Author: leeping
 * @Date: 2019/3/7 13:37
 * 拦截器
 */

public interface IServerInterceptor {
    /**
     * 如果拦截返回结果,否则NULL
     */
    Result interceptor(IceContext context) throws Exception;
}
