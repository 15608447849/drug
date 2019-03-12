package com.onek.permission;

public enum PermissionStatus {

    ALREADY_LOGGED(10000, "是否已经登录");

    int code;
    String desc;

    PermissionStatus(int code,String desc){
        this.code = code;
        this.desc = desc;
    }

    public static PermissionStatus getPermissionStatus(int status) {
        for (PermissionStatus ps : values()) {
            if (status == ps.code) {
                return ps;
            }
        }
        return null;
    }
}
