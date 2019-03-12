package com.onek.annotation;

import com.onek.permission.PermissionStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UserPermission {
    boolean ignore() default false; // 默认不忽略
    PermissionStatus mode() default PermissionStatus.ALREADY_LOGGED;
}
