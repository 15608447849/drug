package com.onek.iceabs;

import Ice.*;
import Ice.Object;
import com.onek.server.inf.IRequest;

import java.lang.Exception;
import java.util.Arrays;
import java.util.List;
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

    private Logger logger;

    private IceServiceDispatchInterceptor() {
        map = new ConcurrentHashMap<>();
        logger = communicator().getLogger();
    }

    public static IceServiceDispatchInterceptor getInstance(){
        return instance;
    }

    /**
     * 添加服务
     */
    public DispatchInterceptor addIceObject(Ice.Identity id, Ice.Object iceObj){
        map.put(id, iceObj);
        logger.print("监听服务:" + id.name);
        return this;
    }

    public void removeIceObject(Identity id){
        map.remove(id);
    }

    /**
     * 移除服务
     */

    @Override
    public DispatchStatus dispatch(Request request) {
        try{

            long time = System.currentTimeMillis();

            Current current = request.getCurrent();
            Identity identity = request.getCurrent().id;

            logger.print(
                    current.con._toString().split("\\n")[1] +
                    " ,服务名:"+identity.name +
                    " ,方法名:" + current.operation
            );

            DispatchStatus status = map.get(identity).ice_dispatch(request);
//            logger.print("DispatchStatus = " + status);
//            if (status == DispatchStatus.DispatchOK){
//                logger.print("调用成功" );
//            }else if (status == DispatchStatus.DispatchAsync){
//                logger.print("异步派发" );
//            }else if (status == DispatchStatus.DispatchUserException){
//                logger.print("调用错误" );
//            }
            logger.print("调用状态"+ status + " , 调用耗时: " + (System.currentTimeMillis() - time) +" ms" );
            return status;
        }catch(Exception e){
            logger.print(e.toString());
            e.printStackTrace();
        }

        return null;
    }
}
