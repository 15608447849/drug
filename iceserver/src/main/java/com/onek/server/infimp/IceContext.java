package com.onek.server.infimp;

import Ice.Current;
import Ice.Logger;
import com.onek.entitys.Result;
import com.onek.server.inf.IParam;
import com.onek.server.inf.IRequest;
import com.onek.server.inf.PushMessageClientPrx;

import java.util.Map;


/**
 * @Author: leeping
 * @Date: 2019/3/8 14:32
 */
public class IceContext {

    public String remoteIp;
    public int remotePoint;
    public String serverName;
    public Logger logger;
    public Current current;
    public String refPkg;
    public String refCls;
    public String refMed;
    public IParam param;

    public IceContext(Current current, IRequest request)  {
            this.serverName = current.id.name;
            this.current = current;
            String[] arr = current.con._toString().split("\\n")[1].split("=")[1].trim().split(":");
            this.remoteIp = arr[0];
            this.remotePoint = Integer.parseInt(arr[1]);
            this.logger = current.adapter.getCommunicator().getLogger();
            this.refPkg = request.pkg;
            this.refCls = request.cls;
            this.refMed = request.method;
            this.param = request.param;
    }

    /**初始化*/
    public void initialization()  {

    }
    protected void longConnectionSetting(Map<String, PushMessageClientPrx> map, Result result){

    }
    /**
     * 返回this
     */
    public <T extends IceContext> T convert(){
        return (T)this;
    }
}
