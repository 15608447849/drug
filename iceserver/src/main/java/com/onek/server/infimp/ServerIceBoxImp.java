package com.onek.server.infimp;

import Ice.Object;
import com.onek.iceabs.IceBoxServerAbs;

/**
 * 应用入口
 */
public class ServerIceBoxImp extends IceBoxServerAbs {

    public static String rpcGroupName;

    public static ServerImp INSTANCE;

    @Override
    protected Object specificServices() {
        if (INSTANCE == null){
            INSTANCE =  new ServerImp(_communicator,_serverName);
        }
        return INSTANCE;
    }

    @Override
    protected void addRpcGroup(String rpcName) {
        rpcGroupName = rpcName;
    }
}
