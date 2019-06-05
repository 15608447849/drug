package com.onek.report.init;

import com.onek.report.data.MarketStoreData;
import com.onek.report.data.SystemConfigData;
import com.onek.server.infimp.IIceInitialize;

import static Ice.Application.communicator;

public class ReportInitialize implements IIceInitialize {

    @Override
    public void startUp(String serverName) {
        communicator().getLogger().error(serverName+" ####### 开始初始化  ####### ");
        try{
            Thread.sleep(2000);
            SystemConfigData.init();
            MarketStoreData.init();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public int priority() {
        return 1;
    }
}
