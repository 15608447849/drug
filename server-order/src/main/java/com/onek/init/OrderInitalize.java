package com.onek.init;

import com.onek.server.infimp.IIceInitialize;
import elasticsearch.ElasticSearchClientFactory;

import static Ice.Application.communicator;

public class OrderInitalize implements IIceInitialize {
    @Override
    public void startUp(String serverName) {
        communicator().getLogger().error(serverName+" ####### 开始初始化  ####### ");
        ElasticSearchClientFactory.init();
    }

    @Override
    public int priority() {
        return 1;
    }
}
