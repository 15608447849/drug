package com.onek.init;

import Ice.Application;
import org.hyrdpf.ds.AppConfig;

/**
 * @Author: leeping
 * @Date: 2019/3/18 15:37
 */
public class SystemInitialize {
    public void startUp(String serverName){
        try {
            AppConfig.initLogger();
            AppConfig.initialize();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
