package com.onek.iceabs;

import Ice.Object;
import Ice.*;
import IceBox.Service;
import com.onek.server.infimp.IIceInitialize;
import com.onek.server.infimp.IceProperties;
import objectref.ObjectRefUtil;

import java.lang.Exception;

import static Ice.Application.communicator;

public abstract class IceBoxServerAbs implements Service {

    //服务名
    protected String _serverName;
    private ObjectAdapter _adapter;

    protected Communicator _communicator;

    @Override
    public void start(String name, Communicator communicator, String[] args) {
        initIceLogger(name,(CommunicatorI) communicator);
        _communicator = communicator;
        _serverName = name;
        _adapter = _communicator.createObjectAdapter(_serverName);
        //创建servant
        Ice.Object object = specificServices();
        //关联servant
        relationID(object,communicator);
        //激活适配器
        _adapter.activate();
        _communicator.getLogger().print("启动服务:" + _serverName );
        initApplication(name);
    }

    //初始化 系统应用
    private void initApplication(String serverName) {
        //scan all class
        _communicator.getLogger().print(serverName + " 开始初始化系统");
        long time = System.currentTimeMillis();
//        IOThreadPool p = new IOThreadPool();
        ObjectRefUtil.scanJarAllClass(classPath -> {
            if (classPath.endsWith("Initialize") && !classPath.equals(IIceInitialize.class.getName())){
//               p.post(()->{
                   try {
                       java.lang.Object object = ObjectRefUtil.createObject(classPath,null,null);
                       if (object instanceof IIceInitialize){
                           ((IIceInitialize) object).startUp(serverName);
                       }
                   } catch (Exception ignored) {
                   }
//               });
            }
        });
        _communicator.getLogger().print("初始化完成,耗时:"+ (System.currentTimeMillis() - time)+"ms");
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
        _communicator.getLogger().print("服务: "+_serverName +" ,加入负载均衡组 " + name);
    }

    protected abstract Object specificServices();

    @Override
    public void stop() {
        _adapter.destroy();
        _communicator.getLogger().print("销毁服务:" + _serverName);
    }

}
