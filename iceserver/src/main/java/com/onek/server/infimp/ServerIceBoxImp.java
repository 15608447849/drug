package com.onek.server.infimp;

import Ice.Object;
import com.onek.iceabs.IceBoxServerAbs;

/**
 * 应用入口
 */
public class ServerIceBoxImp extends IceBoxServerAbs {
    @Override
    protected Object specificServices() {
        return new ServerImp(communicator,_serverName);
    }
}
