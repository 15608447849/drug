package com.onek.interceptors;

import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.context.UserSession;
import com.onek.entitys.Result;
import com.onek.server.infimp.IServerInterceptor;
import com.onek.server.infimp.IceContext;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: leeping
 * @Date: 2019/3/7 14:28
 * 用户是否登陆拦截器 - 单点登陆实现
 */
public class UserInterceptor implements IServerInterceptor {

    //缓存列表
    private final static Map<String, UserPermission> permissionStatusMap = new HashMap<>();

    @Override
    public Result interceptor( IceContext context) throws Exception {

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
            //判断接口是否对用户权限进行拦截
            if(up == null || !up.ignore()){
                appContext.initialization();//初始化上下文用户信息
                UserSession userSession = appContext.getUserSession();

                if(userSession == null) {
                    return new Result().intercept("用户未登录");
                }

                if(up != null && up.compAuth()){
                    if(userSession.comp == null ||  (userSession.comp.authenticationStatus & 256) <= 0){
                        return new Result().intercept("企业没有认证");
                    }
                }

                if (up != null){
                    long[] roleArr = up.role();
                    //角色判断
                    if (roleArr.length > 0){
                        //允许访问的角色码
                        boolean isAccess = false;
                        for (long role : roleArr) {
                            if ((userSession.roleCode & role) > 0) {
                                //有这个角色
                                isAccess = true;
                                break;
                            }
                        }
                        if (!isAccess){
                            return new Result().intercept("用户角色拒绝");
                        }
                    }
                }
            }

        return null;
    }
}
