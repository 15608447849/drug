package com.onek.server.infimp;

import Ice.Communicator;
import Ice.Current;
import Ice.Logger;
import com.onek.entitys.Result;
import com.onek.server.inf.IRequest;
import objectref.ObjectPoolManager;
import objectref.ObjectRefUtil;
import util.GsonUtils;
import util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * ice bind type = ::inf::Interfaces
 * 接口实现
 */
public class ServerImp extends IcePushMessageServerImps {

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

    ServerImp(Communicator communicator,String serverName) {
        super(communicator,serverName);
        this.serverName = serverName;
        this.logger = communicator.getLogger();
        this.pkgPath = IceProperties.INSTANCE.pkgSrvMap.get(serverName);
        initInterceptorList();
        initContextClass();
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
            StringBuilder sb = new StringBuilder();
            sb.append(__current.con.toString().split("\n")[1]);
            sb.append("\t" +serverName+ " >>> " + request.pkg +" >>> " + request.cls +" >>> "+request.method);
            if(!StringUtils.isEmpty(request.param.token)){
                sb.append( "\ntoken:\t"+ request.param.token);
            }
            if(!StringUtils.isEmpty(request.param.json)){
                sb.append("\njson:\t" + request.param.json );
            }
            if(request.param.arrays!=null &&request.param.arrays.length>0){
                sb.append("\narray:\t" + Arrays.toString(request.param.arrays));
            }
            if(request.param.pageIndex > 0 && request.param.pageNumber > 0){
                sb.append("\npaging:\t"+ request.param.pageIndex +" , " +request.param.pageNumber);
            }
            logger.print("->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->->-\n"+sb.toString());

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
        Object methodResultValue =  ObjectRefUtil.callMethod(obj,method,new Class[]{contextCls},iApplicationContext);
        ObjectPoolManager.get().putObject(classPath,obj); //缓存对象
        return methodResultValue;
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
        for (IServerInterceptor iServerInterceptor : interceptorList) {
            result = iServerInterceptor.interceptor(context);
            if (result != null) break;
        }
        return result;
    }

    private String printResult(Object result) {
        String resultString = GsonUtils.javaBeanToJson(result);
        logger.print(resultString +"\n\n"); //+"\n-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-"
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
            if (isLongConnection && result instanceof Result) {
                context.longConnectionSetting(_clientsMaps,(Result)result);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("accessService 捕获错误:"+e.toString());
            result = new Result().error("accessService()",e);
        }
        return printResult(result);
    }



}
