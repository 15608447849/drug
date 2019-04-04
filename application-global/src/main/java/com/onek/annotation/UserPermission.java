package com.onek.annotation;



import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UserPermission {
    boolean ignore() default false; //默认不忽略
    long[] role() default {}; // 允许访问的角色码 , 默认全部
}
