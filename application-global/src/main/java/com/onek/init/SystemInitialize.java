package com.onek.init;

import org.hyrdpf.ds.AppConfig;

/**
 * @Author: leeping
 * @Date: 2019/3/18 15:37
 */
public class SystemInitialize {

    public void startUp(String serverName){
        AppConfig.initLogger();
        AppConfig.initialize();
    }
}
