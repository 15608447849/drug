package com.onek.annotation;



import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UserPermission {
    boolean ignore() default false; //检查匿名访问 , 默认不允许-false
    boolean needAuthenticated() default false; //检查门店企业认证 默认不需要认证-false
}
