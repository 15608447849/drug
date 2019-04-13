package com.onek.discount.init;

import com.onek.discount.timer.DiscountRuleTimer;
import com.onek.server.infimp.IIceInitialize;

import static Ice.Application.communicator;

public class DiscountInitialize implements IIceInitialize {
    @Override
    public void startUp(String serverName) {
        communicator().getLogger().error(serverName+" ####### 开始初始化  ####### ");
        new DiscountRuleTimer();
    }
}
