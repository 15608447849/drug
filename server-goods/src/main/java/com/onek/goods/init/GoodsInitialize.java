package com.onek.goods.init;

import com.onek.goods.timer.HigherProfitsTimer;
import com.onek.goods.timer.RemoveNewFlagTimer;
import com.onek.server.infimp.IIceInitialize;
import elasticsearch.ElasticSearchClientFactory;

public class GoodsInitialize implements IIceInitialize {
    @Override
    public void startUp(String serverName) {
        new RemoveNewFlagTimer();
        new HigherProfitsTimer();
        ElasticSearchClientFactory.init();
    }

    @Override
    public int priority() {
        return 1;
    }
}
