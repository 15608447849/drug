package com.onek.iceabs;

import Ice.Object;
import Ice.*;
import IceBox.Server;
import IceBox.Service;
import com.onek.server.infimp.IceProperties;

import java.lang.Exception;
import java.util.Arrays;

import static Ice.Application.communicator;

public abstract class IceBoxServerAbs implements Service {

    //服务名
    protected String _serverName;
    private ObjectAdapter _adapter;
    protected Logger logger;

    @Override
    public void start(String name, Communicator communicator, String[] args) {
        initIceLogger(name,(CommunicatorI) communicator);

        _serverName = name;
        logger = communicator.getLogger();
        _adapter = communicator.createObjectAdapter(_serverName);
        //创建servant并激活
        Ice.Object object = specificServices();
        relationID(object,communicator);
        _adapter.activate();
        logger.print("成功启动服务:" + _serverName + " 参数集:"+ Arrays.toString(args) );
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
        logger.print(_serverName +" 加入负载均衡组 " + name);
    }

    protected abstract Object specificServices();

    @Override
    public void stop() {
        _adapter.destroy();
        logger.print("销毁服务:" + _serverName);
    }

}
