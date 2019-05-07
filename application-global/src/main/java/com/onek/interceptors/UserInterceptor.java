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
            
            appContext.initialization();//初始化上下文用户信息

            /*判断接口是否对用户权限进行拦截 条件:
            * 1.调用方法没有注解一定拦截权限
            * 2.注解显示不忽略拦截,拦截权限(默认不忽略)
            */
            if(up == null || !up.ignore()){

                UserSession userSession = appContext.getUserSession();

                if(userSession == null) {
                    return new Result().intercept(-2,"用户未登录");
                }

                if (up != null){
                    //判断是否需要关联企业
                    if (!up.allowedUnrelated()){
                        //必须关联企业
                        if (userSession.compId == 0) return new Result().intercept(-3,"用户未关联企业信息");
                    }
                    //判断是否需要认证企业
                    if(up.needAuthenticated()){
                        if(userSession.comp == null ||  (userSession.comp.authenticationStatus & 256) <= 0){
                            return new Result().intercept(-3,"企业没有认证");
                        }
                    }

                    //允许访问的角色码判断
                    long[] roleArr = up.allowRoleList();
                    //角色判断
                    if (roleArr.length > 0){
                        boolean isAccess = false;//默认不允许访问
                        for (long role : roleArr) {
                            if ((userSession.roleCode & role) > 0) {
                                //当前进入的用户 有这个角色
                                isAccess = true;
                                break;
                            }
                        }
                        if (!isAccess){
                            return new Result().intercept(-4,"用户角色权限拒绝");
                        }
                    }

                }
            }

        return null;
    }
}
