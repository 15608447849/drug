package com.onek.user;

import com.onek.AppContext;
import com.onek.UserSession;
import com.onek.annotation.UserPermission;
import com.onek.entitys.Result;
import com.onek.permission.PermissionStatus;
import com.onek.server.inf.IRequest;
import com.onek.server.infimp.IApplicationContext;
import com.onek.server.infimp.IServerInterceptor;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: leeping
 * @Date: 2019/3/7 14:28
 */
public class UserServerInterceptor implements IServerInterceptor {

    private static Map<String, UserPermission> permissionStatusMap = new HashMap<>();

    @Override
    public Result interceptor(String packagePath,String serverName, IRequest request, IApplicationContext context)  {

        try {
            String classpath = packagePath + "." +request.cls;
            String method = request.method;
            //System.out.println("30 line : "+classpath + "--"+method);
            UserPermission up = null;
            if(permissionStatusMap.containsKey(classpath + method)){
                up = permissionStatusMap.get(classpath + method);
            }else{
                Class<?> clazz = Class.forName(classpath);
                Method m = clazz.getMethod(method, new Class[]{ AppContext.class});
                up = m.getAnnotation(UserPermission.class);
                permissionStatusMap.put(classpath + method, up);
            }
            //System.out.println("40 line : "+up);
            if(up != null && up.ignore() == false && up.mode() == PermissionStatus.ALREADY_LOGGED){
                AppContext appContext =  (AppContext) context;
                UserSession userSession = appContext.getUserSession();
                if(userSession == null){
                    return new Result().intercept("用户未登录!");
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
