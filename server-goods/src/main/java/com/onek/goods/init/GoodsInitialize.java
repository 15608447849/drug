package com.onek.goods.init;

import com.onek.goods.timer.HigherProfitsTimer;
import com.onek.goods.timer.RemoveNewFlagTimer;
import com.onek.server.infimp.IIceIceInitialize;

import static Ice.Application.communicator;

public class GoodsInitialize implements IIceIceInitialize {
    @Override
    public void startUp(String serverName) {
        communicator().getLogger().error(serverName+" 开始初始化 --- ");
        new RemoveNewFlagTimer();
        new HigherProfitsTimer();
    }
}
