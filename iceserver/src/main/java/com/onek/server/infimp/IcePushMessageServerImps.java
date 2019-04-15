package com.onek.server.infimp;

import Ice.Communicator;
import Ice.Current;
import Ice.Identity;
import com.onek.server.inf.IRequest;
import com.onek.server.inf.PushMessageClientPrx;
import com.onek.server.inf.PushMessageClientPrxHelper;
import com.onek.server.inf._InterfacesDisp;
import objectref.ObjectRefUtil;
import threadpool.IOThreadPool;
import util.StringUtils;

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
    private Map<String, PushMessageClientPrx> _clientsMaps ;

    protected IOThreadPool pool ;

    protected final Communicator communicator;

    private IPushMessageStore iPushMessageStore;

    protected volatile boolean isLongConnection = false;

    private volatile boolean lockQueue = false;

    public IcePushMessageServerImps(Communicator communicator,String serverName) {
        this.communicator = communicator;
        startPushMessageServer(serverName);
    }

    private void startPushMessageServer(String serverName){
        if (StringUtils.isEmpty(IceProperties.INSTANCE.allowPushMessageServer)) return;
        if (!IceProperties.INSTANCE.allowPushMessageServer.contains(serverName)) return;
        pool = new IOThreadPool();
        _clientsMaps = new ConcurrentHashMap<>();
        //注入消息存储实现
        createMessageStoreImps();
        isLongConnection = true;
        pool.post(this);//心跳线程
        pool.post(pushRunnable());
//        new Thread(this).start();


    }


    private void createMessageStoreImps() {
        if (StringUtils.isEmpty(IceProperties.INSTANCE.pmStoreImp)) return;
        try {
            java.lang.Object object = ObjectRefUtil.createObject(IceProperties.INSTANCE.pmStoreImp,null,null);
            if (object instanceof IPushMessageStore) {
                iPushMessageStore = (IPushMessageStore) object;
                communicator.getLogger().print(Thread.currentThread()+"注入数据存储实现:"+ iPushMessageStore.getClass());
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
        pool.post(()->{
            clientOnile(identity,__current);
        });
    }

    private void clientOnile(Identity identity,Current __current) {
        try {
            Ice.ObjectPrx base = __current.con.createProxy(identity);
            String identityName = communicator.identityToString(identity);
            PushMessageClientPrx client = PushMessageClientPrxHelper.uncheckedCast(base);
            _clientsMaps.put(identityName,client);
            communicator.getLogger().print(Thread.currentThread()+" , "+"添加客户端,id = "+ identityName );
            //异步检测是否存在可推送的消息
            checkOfflineMessageFromDbByIdentityName(identityName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void offline(String identityName, Current __current) {
        pool.post(()->{
            removeClient(identityName);
        });
    }

    private void removeClient(String identityName) {
        _clientsMaps.remove(identityName);
        communicator.getLogger().print(Thread.currentThread()+" , "+"移除客户端,id = "+ identityName );
    }

    @Override
    public void sendMessageToClient(String identityName, String message, Current __current) {
        pool.post(()->{
            addMessage(identityName,message);
        });
    }

    private void addMessage(String identityName, String message) {
        //放入队列
        storeMessageToQueue(new IPMessage(identityName, message));
        if (lockQueue){
            //解锁
            synchronized (communicator){
                communicator.notify();
            }
            lockQueue = false;
        }
    }

    private boolean sendMessage(IPMessage message) {
        try {

                if (message.id == 0) {
                    //存入数据库
                    message.id = storeMessageToDb(message);
                }

                //获取客户端
                PushMessageClientPrx clientPrx = _clientsMaps.get(message.identityName);
                if (clientPrx!=null){
                    try {

                        clientPrx.receive(convertMessage(message));
                        communicator.getLogger().print(Thread.currentThread()+" , "+"send ok , '"+message.identityName+"' msg:"+message.content);
                        changeMessageStateToDb(message);
                        return true;
                    } catch (Exception e) {
                        removeClient(message.identityName);
                    }
                }
        } catch (Exception e) {
            e.printStackTrace();
            communicator.getLogger().print(Thread.currentThread()+" , "+"发送失败,id =  '"+message.identityName+"' msg:"+message.content);
        }
        return false;
    }

    @Override
    public boolean storeMessageToQueue(IPMessage message) {
        if (iPushMessageStore!=null){
            return iPushMessageStore.storeMessageToQueue(message);
        }
        return false;
    }

    @Override
    public IPMessage pullMessageFromQueue() {
        if (iPushMessageStore!=null){
            return iPushMessageStore.pullMessageFromQueue();
        }
        return null;
    }

    @Override
    public long storeMessageToDb(IPMessage message) {
        if (iPushMessageStore!=null){
            return iPushMessageStore.storeMessageToDb(message);
        }
        return 0;
    }

    @Override
    public void changeMessageStateToDb(IPMessage message) {
        if (iPushMessageStore!=null){
            iPushMessageStore.changeMessageStateToDb(message);
        }
    }

    @Override
    public List<IPMessage> checkOfflineMessageFromDbByIdentityName(String identityName) {
        if (iPushMessageStore!=null){
            List<IPMessage> messageList = iPushMessageStore.checkOfflineMessageFromDbByIdentityName(identityName);
            int i = 0;
            for (IPMessage message : messageList){
                if (!sendMessage(message)) break;
                i++;
                communicator.getLogger().print(Thread.currentThread()+" , 已发送进度: " + i + "/" + messageList.size() );
            }
        }
        return null;
    }

    //消息准换
    @Override
    public String convertMessage(IPMessage message) {
        if (iPushMessageStore!=null) {
            return iPushMessageStore.convertMessage(message);
        }
        return message.content;
    }


    private Runnable pushRunnable() {
        return () -> {
            while (!communicator.isShutdown()){
                try {
                    IPMessage message = pullMessageFromQueue();
                    if (message == null){
                        lockQueue = true;
                        synchronized (communicator){
                            communicator.wait();
                        }
                        continue;
                    }
                    sendMessage(message);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    @Override
    public void run() {
        //循环检测 -保活

        while (!communicator.isShutdown()){
            try {
                Thread.sleep( 5 * 1000);

                if (_clientsMaps.size() == 0) continue;

                Iterator<Map.Entry<String,PushMessageClientPrx>> iterator = _clientsMaps.entrySet().iterator();
                PushMessageClientPrx prx;

                while (iterator.hasNext()){
                    prx = iterator.next().getValue();
                    try {
                        prx.ice_ping();
                    } catch (Exception e) {
                        iterator.remove();
                        communicator.getLogger().print(Thread.currentThread()+" , "+"@offline client :"+ communicator.identityToString(prx.ice_getIdentity()));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}
