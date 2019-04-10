package com.onek.server.infimp;

import Ice.Communicator;
import Ice.Current;
import Ice.Identity;
import Ice.Logger;
import com.onek.server.inf.*;
import objectref.ObjectRefUtil;
import threadpool.IOThreadPool;
import util.StringUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: leeping
 * @Date: 2019/4/9 17:57
 * 消息推送 服务端实现
 */
public class IcePushMessageServerImps extends _InterfacesDisp implements IPushMessageStore,Runnable {
    /**
     * 在线客户端
     */
    private Map<String, PushMessageClientPrx> _clientsMaps = new ConcurrentHashMap<>();
//    private Map<String, PushMessageClientPrx> _clientsMaps = new HashMap<>();

    protected IOThreadPool pool = new IOThreadPool();

    protected Communicator communicator;

    private IPushMessageStore iPushMessageStore;

    public IcePushMessageServerImps(Communicator communicator) {
        this.communicator = communicator;
        //注入消息存储实现
        createMessageStoreImps();
        new Thread(this).start();
    }

    private void createMessageStoreImps() {
        if (StringUtils.isEmpty(IceProperties.INSTANCE.pmStoreImp)) return;
        try {
            java.lang.Object object = ObjectRefUtil.createObject(IceProperties.INSTANCE.pmStoreImp,null,null);
            if (object instanceof IPushMessageStore) {
                iPushMessageStore = (IPushMessageStore) object;
                communicator.getLogger().print("注入数据存储实现:"+ object.getClass());
            }
        } catch (Exception ignored) {

        }
    }

    @Override
    public String accessService(IRequest request, Current __current) {
        return null;
    }

    @Override
    public void online(Identity identity, Current __current) {
        try {

            Ice.ObjectPrx base = __current.con.createProxy(identity);
            String identityName = communicator.identityToString(identity);
            PushMessageClientPrx client = PushMessageClientPrxHelper.uncheckedCast(base);
            _clientsMaps.put(identityName,client);
            communicator.getLogger().print("添加客户端,id = "+ identityName );
            //异步检测是存在可推送的消息
            pool.post(()->{
               checkOfflineMessageFromDbByIdentityName(identityName);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void offline(String identityName, Current __current) {
       removeClient(identityName);
    }

    private void removeClient(String identityName) {
        _clientsMaps.remove(identityName);
        communicator.getLogger().print("移除客户端,id = "+ identityName );
    }


    @Override
    public void sendMessageToClient(String identityName, String message, Current __current) {
       sendMessage(identityName,message);
    }

    private void sendMessage(String identityName, String message) {
        pool.post(()->{
            //存入数据库
            long id = storeMessageToDb(identityName,message);
            //获取客户端
            PushMessageClientPrx clientPrx = _clientsMaps.get(identityName);
            if (clientPrx!=null){
                try {
                    clientPrx.receive(message);
                    communicator.getLogger().print("send ok , '"+identityName+"' msg:"+message);
                    changeMessageStateToDb(identityName,id);
                } catch (Exception e) {
                    removeClient(identityName);
                }
            }
        });

    }

    @Override
    public long storeMessageToDb(String identityName, String message) {
        if (iPushMessageStore!=null){
            return iPushMessageStore.storeMessageToDb(identityName,message);
        }
        return 0;
    }

    @Override
    public void changeMessageStateToDb(String identityName,long id) {
        if (iPushMessageStore!=null){
            iPushMessageStore.changeMessageStateToDb(identityName,id);
        }
    }

    @Override
    public List<String> checkOfflineMessageFromDbByIdentityName(String identityName) {
        if (iPushMessageStore!=null){
            List<String> messageList = iPushMessageStore.checkOfflineMessageFromDbByIdentityName(identityName);
            for (String message : messageList){
                sendMessage(identityName,message);
            }
        }
        return null;
    }

    @Override
    public void run() {
        //循环检测
        while (true){
            try {
                Thread.sleep( 3* 60 * 1000);

                if (_clientsMaps.size() == 0) continue;

                Iterator<Map.Entry<String,PushMessageClientPrx>> iterator = _clientsMaps.entrySet().iterator();
                PushMessageClientPrx prx;

                while (iterator.hasNext()){
                    prx = iterator.next().getValue();
                    try {
                        prx.ice_ping();
                    } catch (Exception e) {
                        iterator.remove();
                        communicator.getLogger().print("@offline client :"+ communicator.identityToString(prx.ice_getIdentity()));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
