package com.onek.init;

import com.onek.server.infimp.IIceInitialize;
import org.hyrdpf.ds.AppConfig;
import static Ice.Application.communicator;

/**
 * @Author: leeping
 * @Date: 2019/3/18 15:37
 */
public class SystemInitialize implements IIceInitialize {
    @Override
    public void startUp(String serverName){
        try {
            AppConfig.initLogger();
            AppConfig.initialize();
        } catch (Exception e) {
//            e.printStackTrace();
            communicator().getLogger().error(serverName+" , 初始化错误: "+ e);
        }
    }
}
