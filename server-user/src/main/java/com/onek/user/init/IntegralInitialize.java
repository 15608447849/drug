package com.onek.user.init;

import com.onek.server.infimp.IIceInitialize;

public class IntegralInitialize implements IIceInitialize {
    @Override
    public void startUp(String serverName) {

    }

    @Override
    public int priority() {
        return 2;
    }
}
