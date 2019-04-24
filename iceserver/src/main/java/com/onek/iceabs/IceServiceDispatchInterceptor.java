package com.onek.iceabs;

import Ice.Object;
import Ice.*;

import java.lang.Exception;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static Ice.Application.communicator;

/**
 * @Author: leeping
 * @Date: 2019/3/7 12:01
 */
public class IceServiceDispatchInterceptor extends DispatchInterceptor {

    /**服务质量监控单例模式对象*/
    private static final IceServiceDispatchInterceptor instance = new IceServiceDispatchInterceptor();

    /**用来存放我们需要拦截的Ice服务对象，Key为服务ID，value为对应的Servant*/
    private Map<Identity, Object> map ;

    private IceServiceDispatchInterceptor() {
        map = new ConcurrentHashMap<>();
    }

    public static IceServiceDispatchInterceptor getInstance(){
        return instance;
    }

    /**
     * 添加服务
     */
    public DispatchInterceptor addIceObject(Ice.Identity id, Ice.Object iceObj){
        map.put(id, iceObj);
        communicator().getLogger().print("监听服务:" + id.name);
        return this;
    }

    public void removeIceObject(Identity id){
        map.remove(id);
    }

    private boolean systemRunning = false;

    public void startServer(){
        systemRunning = true;
    }



    /**
     * 移除服务
     */

    @Override
    public DispatchStatus dispatch(Request request) {
        try{
            if (!systemRunning) throw new Exception("服务未初始化完成");
            long time = System.currentTimeMillis();
            Current current = request.getCurrent();
            Identity identity = request.getCurrent().id;
            Object object = map.get(identity);
            DispatchStatus status = object.ice_dispatch(request);
            if (current.operation .equals("accessService")) communicator().getLogger()
                    .print("调用状态: "+ statusString(status) + " , 调用耗时: " + (System.currentTimeMillis() - time) +" ms\n\r");
            return status;
        }catch(Exception ignored){

        }
        return DispatchStatus.DispatchUserException;
    }

    private String statusString(DispatchStatus status){
        if (status == DispatchStatus.DispatchOK) return "调用成功";
        if (status == DispatchStatus.DispatchAsync) return "异步派发";
         if (status == DispatchStatus.DispatchUserException) return "调用错误";
         return "未知状态";
    }
}
