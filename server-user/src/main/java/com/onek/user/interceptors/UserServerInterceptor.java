package com.onek.user.interceptors;

import com.onek.AppContext;
import com.onek.UserSession;
import com.onek.annotation.UserPermission;
import com.onek.entitys.Result;
import com.onek.permission.PermissionStatus;
import com.onek.server.infimp.IceContext;
import com.onek.server.infimp.IServerInterceptor;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: leeping
 * @Date: 2019/3/7 14:28
 */
public class UserServerInterceptor implements IServerInterceptor {

    //缓存列表
    private final static Map<String, UserPermission> permissionStatusMap = new HashMap<>();

    @Override
    public Result interceptor( IceContext context)  {
        try {
            AppContext appContext =  context.convert();
            String classpath = context.refPkg + "." +context.refCls;
            String method = context.refMed;
            String key = classpath + method;
            UserPermission up;
            if(permissionStatusMap.containsKey(key)){
                up = permissionStatusMap.get(key);
            }else{
                Class<?> clazz = Class.forName(classpath);
                Method m = clazz.getMethod(method, appContext.getClass());
                up = m.getAnnotation(UserPermission.class);
                permissionStatusMap.put(key, up); //存
            }
            if(up != null && !up.ignore() && up.mode() == PermissionStatus.ALREADY_LOGGED){
                UserSession userSession = appContext.getUserSession();
                if(userSession == null){
                    return new Result().intercept("用户未登录");
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
