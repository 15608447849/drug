package com.onek.server.infimp;

import Ice.Current;
import Ice.Logger;
import IceInternal.Ex;
import com.onek.prop.AppProperties;
import com.onek.server.inf.IParam;
import com.onek.server.inf.IRequest;
import com.onek.server.inf._InterfacesDisp;
import com.onek.entitys.Result;
import objectref.ObjectPoolManager;
import objectref.ObjectRefUtil;
import util.GsonUtils;
import util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * ice bind type = ::inf::Interfaces
 * 接口实现
 */
public class ServerImp extends _InterfacesDisp {

    private ArrayList<IServerInterceptor> interceptorList;

    private String serverName;

    private Logger logger;

    private String pkgPath;

    //上下文实例
    private Class contextCls = IApplicationContext.class;

    public ServerImp(String serverName, Logger logger) {
        this.serverName = serverName;
        this.logger = logger;
        pkgPath = AppProperties.INSTANCE.pkgSrvMap.get(serverName);
        initInterceptorList();
        initContextClass();
    }


    private void initInterceptorList() {
        interceptorList = new ArrayList<>();
        for (String path : AppProperties.INSTANCE.getInterceptList()){
            //反射构建类
            IServerInterceptor iServerInterceptor = createInterceptor(path);
            if (iServerInterceptor!=null) {
                interceptorList.add(iServerInterceptor);
            }
        }
    }

    private void initContextClass() {
        if (StringUtils.isEmpty(AppProperties.INSTANCE.contextImp)) return;
        try {
            contextCls = Class.forName(AppProperties.INSTANCE.contextImp);
        } catch (ClassNotFoundException ignored) {
        }
    }

    //创建拦截器
    private IServerInterceptor createInterceptor(String path) {
        try {
            Object obj = ObjectRefUtil.createObject(path,null,null);
            if (obj instanceof  IServerInterceptor){
                return (IServerInterceptor) obj;
            }
        } catch (Exception e) {
            logger.print("创建拦截器失败,path = " + path);
        }
        return  null;
    }

    private void printInfo(IRequest request, Current __current) {
        try {
            logger.print(
                    "定位: " +pkgPath +" - " + request.cls +" - "+request.method+"\n"+
                    "json数据: " + request.param.json +"\n" +
                    "array数据: " + Arrays.toString(request.param.arrays)+"\n" +
                    "分页信息: "+ request.param.pageIndex +" , " +request.param.pageNumber
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void check(IRequest request) throws Exception {
        //查询配置的包路径
        if (StringUtils.isEmpty(pkgPath)) {
            if (StringUtils.isEmpty(request.pkg)) throw new Exception("没有指定相关服务包路径");
            pkgPath = request.pkg;
        }
        if (StringUtils.isEmpty(request.cls)) throw new Exception("没有指定相关服务类路径");
        if (StringUtils.isEmpty(request.method)) throw new Exception("没有指定相关服务方法");
    }

    private Object callObjectMethod(String packagePath, String classPath, String method,IApplicationContext iApplicationContext) throws Exception{
        Object obj = ObjectPoolManager.get().getObject(classPath);
        if (obj == null){
            //创建
            obj = ObjectRefUtil.createObject(packagePath+"."+classPath,null,null);
            //使用完毕之后再放入池中
        }
        return  ObjectRefUtil.callMethod(obj,method,new Class[]{contextCls},iApplicationContext);
    }

    @Override
    public String accessService(IRequest request, Current __current) {

        Object result;
        try {
            check(request);
            printInfo(request,__current);
            //产生Application上下文
            IApplicationContext context = genApplicationContext(__current,request.param);
            result = interceptor(pkgPath,serverName,request,context);
            logger.print("拦截结果: "+ result);
            if (result == null) result = callObjectMethod(pkgPath,request.cls,request.method,context);
        } catch (Exception e) {
            e.printStackTrace();
            logger.print(__current.con._toString().split("\\n")[1]+"->"+e);
            result = new Result().message(e.toString());
        }
        return GsonUtils.javaBeanToJson(result);
    }

    //产生平台上下文对象
    private IApplicationContext genApplicationContext(Current current, IParam param) {
        try{
            Object obj = ObjectRefUtil.createObject(contextCls,
                    new Class[]{Current.class,Logger.class,IParam.class,},
                    new Object[]{current,logger,param});
            if (obj instanceof IApplicationContext) return (IApplicationContext) obj;
        }catch (Exception ignored){
        }
        return new IApplicationContext(current,logger,param);
    }

    private Result interceptor(String packagePath,String serverName, IRequest request, IApplicationContext context) throws Exception {
        Result result = null;
        Iterator<IServerInterceptor> it = interceptorList.iterator();
        while (it.hasNext()){
            result = it.next().interceptor(packagePath, serverName,request,context);
            if (result != null) break;
        }
        return result;
    }


}
