package com.onek.server.infimp;

import Ice.Application;
import Ice.Current;
import Ice.Logger;
import com.onek.server.inf.IParam;
import com.onek.server.inf.IRequest;
import objectref.ObjectPoolManager;
import objectref.ObjectRefUtil;

import java.lang.reflect.Method;


/**
 * @Author: leeping
 * @Date: 2019/3/8 14:32
 */
public abstract class IceContext {

    public String remoteIp;
    public int remotePoint;
    public String serverName;
    public Logger logger;
    public Current current;
    public String refPkg;
    public String refCls;
    public String refMed;
    public IParam param;
    public Class callerCls;
    public Method method;
    public IceDebug debug;

    public IceContext(Current current, IRequest request) throws Exception  {
        if (current!=null){
            this.serverName = current.id.name;
            this.current = current;
            String[] arr = current.con._toString().split("\\n")[1].split("=")[1].trim().split(":");
            this.remoteIp = arr[0];
            this.remotePoint = Integer.parseInt(arr[1]);
        }
        this.logger = Application.communicator().getLogger();
        this.refPkg = request.pkg;
        this.refCls = request.cls;
        this.refMed = request.method;
        this.param = request.param;
        String classPath = this.refPkg + "."+this.refCls;
        this.callerCls = Class.forName(classPath);
        this.method = callerCls.getMethod(refMed,this.getClass());
        debug = method.getAnnotation(IceDebug.class);

    }

    private static Object getCaller(Class cls) throws Exception {
        Object obj = ObjectPoolManager.get().getObject(cls.getName()); //对象池中获取对象
        if (obj == null)  obj = ObjectRefUtil.createObject(cls,null);//创建
        return obj;
    }

    private static void putCaller(Object obj){
        ObjectPoolManager.get().putObject(obj.getClass().getName(),obj);//使用完毕之后再放入池中,缓存对象
    }


////    //    //调用方法
////    private Object getCallClass(String packagePath, String classPath) throws Exception{
////        Object obj = ObjectPoolManager.get().getObject(classPath); //对象池中获取对象
////        if (obj == null)  obj = ObjectRefUtil.createObject(packagePath+"."+classPath);//创建
//        Object methodResultValue =  ObjectRefUtil.callMethod(obj,method,new Class[]{contextCls},iApplicationContext);
////        ObjectPoolManager.get().putObject(classPath,obj);//使用完毕之后再放入池中,缓存对象
////        return methodResultValue;
////    }

    /**初始化*/
    public abstract void initialization()  ;

    /**调用具体方法*/
    public Object call() throws Exception{
        Object caller = getCaller(callerCls);
        Object value;
        try{
            value = ObjectRefUtil.callMethod(caller,method,new Class[]{this.getClass()},this);
        }catch (Exception e){
            throw e;
        }finally {
            putCaller(caller);
        }
        return value;
    }

    /**
     * 返回this
     */
    public <T extends IceContext> T convert(){
        return (T)this;
    }

}
