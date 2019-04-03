package com.onek.iceabs;

import Ice.InitializationData;
import Ice.Util;
import java.util.Arrays;

/**
 * @Author: leeping
 * @Date: 2019/4/3 14:13
 */
public class Server {
    public static void main(String[] args) {
        IceLog4jLogger log4jLogger = new IceLog4jLogger("system");
        log4jLogger.print("启动服务 ,args = " + Arrays.toString(args));
        InitializationData initData = new InitializationData();
        initData.properties = Util.createProperties();
        initData.properties.setProperty("Ice.Admin.DelayCreation", "1");
        initData.logger = log4jLogger;
        IceBox.Server server = new IceBox.Server();
        System.exit(server.main("IceBox.Server", args, initData));
    }
}
