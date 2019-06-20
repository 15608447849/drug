package com.onek.server.infimp;

import Ice.Communicator;
import Ice.Current;
import Ice.Logger;
import com.onek.entitys.Result;
import com.onek.server.inf.IRequest;
import objectref.ObjectPoolManager;
import objectref.ObjectRefUtil;
import org.hyrdpf.util.LogUtil;
import util.GsonUtils;
import util.StringUtils;

import java.lang.reflect.InvocationTargetException;
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
            Object obj = ObjectRefUtil.createObject(path);
            if (obj instanceof  IServerInterceptor){
                return (IServerInterceptor) obj;
            }
        } catch (Exception e) {
            logger.error("创建拦截器失败,path = " + path);
        }
        return  null;
    }

    //打印参数
    private String printParam(IRequest request, Current __current) {

            try {
                StringBuilder sb = new StringBuilder();
                if (__current != null) {
                    sb.append(__current.con.toString().split("\n")[1]);
                }else{
                    sb.append("本地调用");
                }
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
                return sb.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        return "";
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

    //调用方法
    private Object callObjectMethod(String packagePath, String classPath, String method, IceContext iApplicationContext) throws Exception{
        Object obj = ObjectPoolManager.get().getObject(classPath); //对象池中获取对象
        if (obj == null)  obj = ObjectRefUtil.createObject(packagePath+"."+classPath);//创建
        Object methodResultValue =  ObjectRefUtil.callMethod(obj,method,new Class[]{contextCls},iApplicationContext);
        ObjectPoolManager.get().putObject(classPath,obj);//使用完毕之后再放入池中,缓存对象
        return methodResultValue;
    }

    //产生平台上下文对象
    private IceContext generateContext(Current current, IRequest request) throws Exception {
            Object obj = ObjectRefUtil.createObject(
                    contextCls,
                    new Class[]{Current.class, IRequest.class},
                    current,request
                    );
            if (obj instanceof IceContext) return (IceContext) obj;
        return new IceContext(current,request);
    }
    //拦截
    private Result interceptor(IceContext context) throws Exception {
        Result result = null;
        for (IServerInterceptor iServerInterceptor : interceptorList) {
            result = iServerInterceptor.interceptor(context);
            if (result != null) break;
        }
        return result;
    }

    //打印结果
    private String printResult(Object result) {
        String resultString;
        if (result instanceof String){
            resultString = String.valueOf(result);
        }else{
            resultString = GsonUtils.javaBeanToJson(result);
        }
        logger.print("↓↓↓↓ 返 ↓↓ 回 ↓↓ 值↓↓↓↓\n\t"
                +resultString );
        result = null;
        //+"\n-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-<-"
        return resultString;
    }

    //客户端 - 接入服务
    @Override
    public String accessService(IRequest request, Current __current) {
        Object result;
        String callInfo = "没有调用信息";
        try {
            check(request);
            callInfo = printParam(request,__current);
            //产生context
            IceContext context = generateContext(__current,request);
            //拦截器
            result = interceptor(context);
            //具体业务实现调用 返回值不限制
            if (result == null) result = callObjectMethod(context.refPkg,context.refCls,context.refMed,context);
            if (result instanceof Result) context.isAllowOnline((Result) result);
        } catch (Exception e) {
            Throwable targetEx = e;
            if (e instanceof InvocationTargetException) {
                targetEx =((InvocationTargetException)e).getTargetException();
            }
            LogUtil.getDefaultLogger().error(callInfo,targetEx);
            result = new Result().error("错误调用",targetEx);
        }
        return printResult(result);
    }



}
