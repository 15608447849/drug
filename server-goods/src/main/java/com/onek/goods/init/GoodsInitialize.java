package com.onek.goods.init;

import com.onek.goods.timer.HigherProfitsTimer;
import com.onek.goods.timer.RemoveNewFlagTimer;
import com.onek.server.infimp.IIceInitialize;
import elasticsearch.ElasticSearchClientFactory;

public class GoodsInitialize implements IIceInitialize {
    @Override
    public void startUp(String serverName) {
        ElasticSearchClientFactory.init();
        try{
            Thread.sleep(2000);
            new RemoveNewFlagTimer();
            new HigherProfitsTimer();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public int priority() {
        return 1;
    }
}
