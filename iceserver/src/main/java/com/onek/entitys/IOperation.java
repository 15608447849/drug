package com.onek.entitys;

import com.onek.server.infimp.IceContext;

/**
 * @Author: leeping
 * @Date: 2019/3/12 14:37
 */
public interface IOperation<T extends IceContext> {
    Result execute(T context);
}
