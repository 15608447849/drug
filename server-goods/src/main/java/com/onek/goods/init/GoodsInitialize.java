package com.onek.goods.init;

import com.onek.goods.timer.HigherProfitsTimer;
import com.onek.goods.timer.RemoveNewFlagTimer;
import com.onek.server.infimp.IIceInitialize;
import elasticsearch.ElasticSearchClientFactory;
import elasticsearch.ElasticSearchProvider;

import static Ice.Application.communicator;

public class GoodsInitialize implements IIceInitialize {
    @Override
    public void startUp(String serverName) {
        communicator().getLogger().error(serverName+" 开始初始化 --- ");
        new RemoveNewFlagTimer();
        new HigherProfitsTimer();
        ElasticSearchClientFactory.init();
    }
}
