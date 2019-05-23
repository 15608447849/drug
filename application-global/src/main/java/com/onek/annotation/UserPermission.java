package com.onek.annotation;



import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UserPermission {
    boolean ignore() default false; //默认不忽略
    long[] allowRoleList() default {}; // 允许访问的角色码 , 默认全部
    boolean needAuthenticated() default false; //默认false 不需要认证, true:企业需要认证


}
