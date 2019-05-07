package com.onek.annotation;



import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UserPermission {
    boolean ignore() default false; //默认不忽略
    boolean ignoreButInitialize() default false;//忽略但是初始化 - 默认 false - 不初始化
    long[] allowRoleList() default {}; // 允许访问的角色码 , 默认全部
    boolean allowedUnrelated() default false;//默认false 必须关联企业 , true:允许不关联企业
    boolean needAuthenticated() default false; //默认false 不需要认证, true:企业需要认证


}
