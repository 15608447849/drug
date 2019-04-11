package com.onek.iceabs;

import Ice.Object;
import Ice.*;
import IceBox.Service;
import com.onek.server.infimp.IIceIceInitialize;
import com.onek.server.infimp.IceProperties;
import objectref.ObjectRefUtil;
import threadpool.IOThreadPool;

import java.lang.Exception;

import static Ice.Application.communicator;

public abstract class IceBoxServerAbs implements Service {

    //服务名
    protected String _serverName;
    private ObjectAdapter _adapter;
    protected Logger logger;
    protected Communicator communicator;

    @Override
    public void start(String name, Communicator communicator, String[] args) {
        initIceLogger(name,(CommunicatorI) communicator);

        this.communicator = communicator;
        _serverName = name;
        logger = communicator.getLogger();
        initApplication(name);
        _adapter = communicator.createObjectAdapter(_serverName);
        //创建servant并激活
        Ice.Object object = specificServices();
        relationID(object,communicator);

        _adapter.activate();
        logger.print("\n成功启动服务:" + _serverName+"\n" );
    }

    //初始化 系统应用
    private void initApplication(String serverName) {
        //scan all class
        logger.print(serverName + " 开始初始化系统");
        long time = System.currentTimeMillis();
        IOThreadPool p = new IOThreadPool();
        ObjectRefUtil.scanJarAllClass(classPath -> {
            if (classPath.endsWith("Initialize") && !classPath.equals(IIceIceInitialize.class.getName())){
               p.post(()->{
                   try {
                       java.lang.Object object = ObjectRefUtil.createObject(classPath,null,null);
                       if (object instanceof IIceIceInitialize){
                           ((IIceIceInitialize) object).startUp(serverName);
                       }
                   } catch (Exception ignored) {
                   }
               });
            }
        });
    }

    private void initIceLogger(String name,CommunicatorI ic) {
        Logger logger = ic.getInstance().initializationData().logger;
        if (!(logger instanceof IceLog4jLogger)){
            ic.getInstance().initializationData().logger = new IceLog4jLogger(name);
            initIceLogger(name, (CommunicatorI) communicator());
        }
    }

    private void relationID(Ice.Object object,Communicator communicator) {

        Identity identity = communicator.stringToIdentity(_serverName);
        _adapter.add(IceServiceDispatchInterceptor.getInstance().addIceObject(identity,object),identity);
        //查询是否存在组配置信息 (暂时只能 一个服务关联到一个 rpc组 ,理论上 一个服务可关联到多个组, 暂不实现)
        String name = IceProperties.INSTANCE.repSrvMap.get(_serverName);
        if (name == null) return;
        identity = communicator.stringToIdentity(name);
        _adapter.add(IceServiceDispatchInterceptor.getInstance().addIceObject(identity,object),identity);
        logger.print("服务: "+_serverName +" ,加入负载均衡组 " + name);
    }

    protected abstract Object specificServices();

    @Override
    public void stop() {
        _adapter.destroy();
        logger.print("销毁服务:" + _serverName);
    }

}
