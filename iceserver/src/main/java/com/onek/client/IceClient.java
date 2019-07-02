package com.onek.client;

import com.onek.server.inf.IRequest;
import com.onek.server.inf.InterfacesPrx;
import com.onek.server.inf.InterfacesPrxHelper;
import com.onek.server.infimp.ServerIceBoxImp;
import util.GsonUtils;

import java.util.Locale;

/**
 * @Author: leeping
 * @Date: 2019/4/9 14:15
 * ice客户端远程调用
 */
public class IceClient {

    private  Ice.Communicator ic = null;

    private final String[] args ;

    private int timeout = 30000;

    public IceClient(String tag,String serverAdds) {
        StringBuffer sb = new StringBuffer("--Ice.Default.Locator="+tag+"/Locator");
        String str = ":tcp -h %s -p %s";
        String[] infos = serverAdds.split(";");
        for (String info : infos){
            String[] host_port = info.split(":");
            sb.append(String.format(Locale.CHINA,str, host_port[0],host_port[1]));
        }
        args = new String[]{sb.toString(),"idleTimeOutSeconds=300","--Ice.MessageSizeMax=4096"};
    }

    public IceClient setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    synchronized
    public IceClient startCommunication() {
        if (ic == null) {
            ic = Ice.Util.initialize(args);
        }
        return this;
    }

    synchronized
    public IceClient stopCommunication() {
        if (ic != null) {
            ic.destroy();
        }
        return this;
    }
    public InterfacesPrx curPrx;

    public IceClient settingProxy(String serverName){
        if (isLocalCallback(serverName)){
            curPrx = null;
            isLocal = true;
        }else{
            Ice.ObjectPrx base = ic.stringToProxy(serverName).ice_invocationTimeout(timeout);
            curPrx =  InterfacesPrxHelper.checkedCast(base);
            isLocal = false;
        }
        return this;
    }
    private IRequest request;

    public IceClient settingReq(String token,String cls,String med){
        request = new IRequest();
        request.cls = cls;
        request.method = med;
        request.param.token = token;
        return this;
    }

    public IceClient settingReq(String token,String serverName,String clazz,String method){
        return settingProxy(serverName).settingReq(token,clazz,method);
    }

    public IceClient setServerAndRequest(String serverName,String clazz,String method){
        return settingProxy(serverName).settingReq("",clazz,method);
    }

    public IceClient setArrayParams(Object... objects){
        String[] arr = new String[objects.length];
        for (int i = 0; i< objects.length; i++) {
            arr[i] = String.valueOf(objects[i]);
        }
        return settingParam(arr);
    }

    public IceClient setJsonParams(Object obj){
        return settingParam(GsonUtils.javaBeanToJson(obj));
    }

    public IceClient settingParam(String json, int index, int number){
        request.param.json = json;
        request.param.pageIndex = index;
        request.param.pageNumber = number;
        return this;
    }
    public IceClient settingParam(String json){
        request.param.json = json;
        return this;
    }
    public IceClient settingParam(String[] array, int index, int number){
        request.param.arrays = array;
        request.param.pageIndex = index;
        request.param.pageNumber = number;
        return this;
    }
    public IceClient settingParam(String[] array){
        request.param.arrays = array;
        return this;
    }

    private boolean isLocalCallback(String remoteServerName){
        if (ServerIceBoxImp.INSTANCE == null || ServerIceBoxImp.rpcGroupName == null){
            return false;
        }
        if (ServerIceBoxImp.rpcGroupName.equals(remoteServerName)){
            return true;
        }
        return false;
    }
    private boolean isLocal = false;

    public String execute() {
        if (isLocal && request!=null){
            return ServerIceBoxImp.INSTANCE.accessService(request);
        }else if (curPrx!=null && request!=null){
            return curPrx.accessService(request);
        }
        curPrx = null;
        throw new RuntimeException("ICE 未开始连接或找不到远程代理或请求参数异常");
    }

    public void sendMessageToClient(String identity,String message){
        if (isLocal){
            ServerIceBoxImp.INSTANCE.sendMessageToClient(identity,message);
        } else if (curPrx!=null){
             curPrx.sendMessageToClient(identity,message);
        }
    }
}
