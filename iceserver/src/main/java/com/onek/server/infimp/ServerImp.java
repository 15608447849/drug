package com.onek.server.infimp;

import Ice.Current;
import Ice.Logger;
import Ice.Request;
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

    //拦截器
    private ArrayList<IServerInterceptor> interceptorList;

    //服务名
    private String serverName;

    //日志
    private Logger logger;

    //服务对象的反射包路径
    private String pkgPath;

    //上下文实例 - 默认
    private Class contextCls = IceContext.class;

    ServerImp(String serverName, Logger logger,String[] args) {
        this.serverName = serverName;
        this.logger = logger;
        this.pkgPath = IceProperties.INSTANCE.pkgSrvMap.get(serverName);
        initInterceptorList();
        initContextClass();
        initApplication(args);
    }

    //初始化 系统应用
    private void initApplication(String[] args) {
        if (StringUtils.isEmpty(IceProperties.INSTANCE.appInitializationImp)) return;
        Object initObj = null;
        try {
            initObj = ObjectRefUtil.createObject(IceProperties.INSTANCE.appInitializationImp,null,null);
        } catch (Exception ignored) {
            logger.print("创建初始化器失败,path = " + IceProperties.INSTANCE.appInitializationImp);
        }
        if (initObj!=null){
            try {
                ObjectRefUtil.callMethod(initObj,"startUp",new Class[]{String.class,String[].class},serverName,args);
            } catch (Exception e) {
                logger.print("无法调用初始化器方法startUp,原因: " + e);
            }
        }

    }

    //初始化拦截器
    private void initInterceptorList() {
        interceptorList = new ArrayList<>();
        for (String path : IceProperties.INSTANCE.getInterceptList()){
            //反射构建类
            IServerInterceptor iServerInterceptor = createInterceptor(path);
            if (iServerInterceptor!=null) {
                interceptorList.add(iServerInterceptor);
            }
        }
    }

    //初始化上下文对象
    private void initContextClass() {
        if (StringUtils.isEmpty(IceProperties.INSTANCE.contextImp)) return;
        try {
            contextCls = Class.forName(IceProperties.INSTANCE.contextImp);
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

    //打印参数
    private void printParam(IRequest request, Current __current) {
        try {
            logger.print(
                    "->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->-\n" +
                    "call:\t" + request.pkg +"_" + request.cls +"_"+request.method+"\n"+
                    "token:\t"+ request.param.token+"\n"     +
                    "json:\t" + request.param.json +"\n" +
                    "array:\t" + Arrays.toString(request.param.arrays)+"\n" +
                    "Paging:\t"+ request.param.pageIndex +" , " +request.param.pageNumber
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //检测,查询配置的包路径 - 优先 客户端指定的全路径
    private void check(IRequest request) throws Exception {

        if (StringUtils.isEmpty(request.method)) throw new Exception("没有指定相关服务方法");

        if (StringUtils.isEmpty(request.cls)) throw new Exception("没有指定相关服务类路径");

        if (StringUtils.isEmpty(request.pkg)){
            if (StringUtils.isEmpty(pkgPath)) throw new Exception("没有指定相关服务包路径");
            request.pkg = pkgPath;
        }
    }

    private Object callObjectMethod(String packagePath, String classPath, String method, IceContext iApplicationContext) throws Exception{
        Object obj = ObjectPoolManager.get().getObject(classPath);
        if (obj == null){
            //创建
            obj = ObjectRefUtil.createObject(packagePath+"."+classPath,null,null);
            //使用完毕之后再放入池中
        }
        return  ObjectRefUtil.callMethod(obj,method,new Class[]{contextCls},iApplicationContext);
    }

    //产生平台上下文对象
    private IceContext generateContext(Current current, IRequest request)  {
        try {
            Object obj = ObjectRefUtil.createObject(
                    contextCls,
                    new Class[]{Current.class, IRequest.class},
                    new Object[]{current,request}
                    );
            if (obj instanceof IceContext) return (IceContext) obj;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new IceContext(current,request);
    }

    private Result interceptor(IceContext context) throws Exception {
        Result result = null;
        Iterator<IServerInterceptor> it = interceptorList.iterator();
        while (it.hasNext()){
            result = it.next().interceptor(context);
            if (result != null) break;
        }
        return result;
    }

    private String printResult(Object result) {
        String resultString = GsonUtils.javaBeanToJson(result);
        logger.print(
                resultString+"\n"+
                "-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-\n");
        return resultString;
    }

    //客户端 - 接入服务
    @Override
    public String accessService(IRequest request, Current __current) {
        Object result;
        try {
            check(request);
            printParam(request,__current);
            //产生Application上下文
            IceContext context = generateContext(__current,request);
            result = interceptor(context);
            if (result == null) result = callObjectMethod(context.refPkg,context.refCls,context.refMed,context);
        } catch (Exception e) {
            e.printStackTrace();
            logger.print(__current.con._toString().split("\\n")[1]+"\t"+e);
            result = new Result().message(e.toString());
        }
        return printResult(result);
    }



}
