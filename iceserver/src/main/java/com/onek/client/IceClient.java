package com.onek.client;

import com.onek.server.inf.IRequest;
import com.onek.server.inf.InterfacesPrx;
import com.onek.server.inf.InterfacesPrxHelper;
import threadpool.IOThreadPool;
import threadpool.MThreadPool;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

/**
 * @Author: leeping
 * @Date: 2019/4/9 14:15
 * ice客户端远程调用
 */
public class IceClient {
    private MThreadPool pool;

    private  Ice.Communicator ic = null;

    private final HashMap<String,InterfacesPrx> prxMaps;

    private final String[] args ;

    public IceClient(String tag, String host, int port) {
        String str = "--Ice.Default.Locator=%s/Locator:tcp -h %s -p %d";
        args = new String[]{String.format(Locale.CHINA,str,tag,host,port)};
        prxMaps = new HashMap<>();
    }

    synchronized
    public IceClient startCommunication() {
        if (ic == null) {
            ic = Ice.Util.initialize(args);
            pool = new MThreadPool();
        }
        return this;
    }

    synchronized
    public IceClient stopCommunication() {
        if (ic != null) {
            try {
                pool.close();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                ic.destroy();
            }
        }
        return this;
    }
    private InterfacesPrx curPrx;

    public IceClient settingProxy(String serverName){
        InterfacesPrx prx = prxMaps.get(serverName);
        if (prx == null){
            Ice.ObjectPrx base = ic.stringToProxy(serverName);
            prx =  InterfacesPrxHelper.checkedCast(base);
            if (prx!=null) prxMaps.put(serverName,prx);
        }
        curPrx = prx;
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

    public String executeSync(){
        if (curPrx!=null && request!=null){
            return curPrx.accessService(request);
        }
        return "";
    }

    interface Callback{
        void callback(String result);
    }

    public void executeAsync(Callback callback){
        pool.post(() -> {
            String result =executeSync();
            if (callback!=null) callback.callback(result);
        });

    }
}
