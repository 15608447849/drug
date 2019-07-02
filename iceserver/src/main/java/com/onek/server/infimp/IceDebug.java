package com.onek.server.infimp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author: leeping
 * @Date: 2019/6/26 18:58
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IceDebug {
    boolean inPrint() default true; // 是否输入调用时的传参信息
    boolean outPrint() default false;// 是否输出调用后的结果
    boolean timePrint() default false;//是否打印调用时间
}