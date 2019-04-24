package com.onek.discount.init;

import com.onek.discount.timer.ActStockCheckTimer;
import com.onek.discount.timer.DiscountRuleTimer;
import com.onek.server.infimp.IIceInitialize;
import elasticsearch.ElasticSearchClientFactory;

import static Ice.Application.communicator;

public class DiscountInitialize implements IIceInitialize {
    @Override
    public void startUp(String serverName) {
        communicator().getLogger().error(serverName+" ####### 开始初始化  ####### ");
        ElasticSearchClientFactory.init();
        try{
            Thread.sleep(5000);
            new DiscountRuleTimer();
            new ActStockCheckTimer();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public int priority() {
        return 1;
    }
}
