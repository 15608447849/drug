package com.onek.iceabs;

import Ice.Object;
import Ice.*;
import IceBox.Service;
import com.onek.prop.AppProperties;

import java.util.Arrays;

public abstract class IceBoxServerAbs implements Service {

    //服务名
    protected String _serverName;
    protected ObjectAdapter _adapter;
    protected Logger logger;

    @Override
    public void start(String name, Communicator communicator, String[] args) {
        logger = communicator.getLogger();
        _serverName = name;
        _adapter = communicator.createObjectAdapter(_serverName);
        //创建servant并激活
        Ice.Object object = specificServices(args);
        relationID(object,communicator);
        _adapter.activate();
        logger.print("成功启动服务:" + _serverName + " 参数集:"+ Arrays.toString(args) );
    }

    private void relationID(Ice.Object object,Communicator communicator) {
        Identity identity = communicator.stringToIdentity(_serverName);
        _adapter.add(object,identity);
        //查询是否存在组配置信息 (暂时只能 一个服务关联到一个 rpc组 ,理论上 一个服务可关联到多个组, 暂不实现)
        String name = AppProperties.INSTANCE.repSrvMap.get(_serverName);
        if (name == null) return;
        identity = communicator.stringToIdentity(name);
        _adapter.add(object,identity);
        logger.print("关联负载均衡组: " + name);
    }

    protected abstract Object specificServices(String[] args);

    @Override
    public void stop() {
        _adapter.destroy();
        logger.print("销毁服务:" + _serverName);
    }
}
