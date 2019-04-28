package com.onek.user.init;

import com.onek.server.infimp.IIceInitialize;
import com.onek.user.timer.ExpireIntegralRemindTimer;
import com.onek.user.timer.IntegralResetTimer;
import com.onek.user.timer.LastYearIntegralTimer;

public class IntegralInitialize implements IIceInitialize {
    @Override
    public void startUp(String serverName) {
        new LastYearIntegralTimer();
        new ExpireIntegralRemindTimer();
        new IntegralResetTimer();
    }

    @Override
    public int priority() {
        return 2;
    }
}
