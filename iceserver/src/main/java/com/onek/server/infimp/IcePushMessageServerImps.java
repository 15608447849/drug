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
     *  k - 客户端类型 ,v - Map(公司码-客户端)
     *  暂不实现
     */
    private HashMap<String,ConcurrentHashMap<String, PushMessageClientPrx>> allMap;

    /**
     * 在线客户端-PC
     */
    private Map<String, PushMessageClientPrx> _clientsMaps_pc;

    protected IOThreadPool pool ;

    protected final Communicator communicator;

    private IPushMessageStore iPushMessageStore;

    private volatile boolean lockQueue = false;

    IcePushMessageServerImps(Communicator communicator,String serverName) {
        this.communicator = communicator;
        startPushMessageServer(serverName);
    }

    public boolean checkClientOnlineStatus(String clientType,String identity){
        if (_clientsMaps_pc != null){
            try {
                boolean flag = false;
                if (_clientsMaps_pc.containsKey(identity)){
                    flag = true;
                }
                //检查客户端是否在线
                communicator.getLogger().print("检查客户端是否在线 : "+ clientType+" , "+ identity +" status: "+ flag);
                return !flag;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    private void startPushMessageServer(String serverName){
        if (StringUtils.isEmpty(IceProperties.INSTANCE.allowPushMessageServer)) return;
        if (!IceProperties.INSTANCE.allowPushMessageServer.contains(serverName)) return;
        pool = new IOThreadPool();
        _clientsMaps_pc = new ConcurrentHashMap<>();
        //注入消息存储实现
        createMessageStoreImps();
        new Thread(this).start();//心跳线程
        new Thread(pushRunnable()).start();//消息发送
    }


    private void createMessageStoreImps() {//创建消息存储实例
        if (StringUtils.isEmpty(IceProperties.INSTANCE.pmStoreImp)) return;
        try {
            java.lang.Object object = ObjectRefUtil.createObject(IceProperties.INSTANCE.pmStoreImp);
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
            //后期根据类型分发客户端 -
            clientOnline(identity,__current);
        });
    }

    private void clientOnline(Identity identity, Current __current) {
        pool.post(()->{
            try {
                Ice.ObjectPrx base = __current.con.createProxy(identity);
                String identityName = communicator.identityToString(identity);
                PushMessageClientPrx client = PushMessageClientPrxHelper.uncheckedCast(base);
                addClient(identityName,client);
                communicator.getLogger().print(Thread.currentThread()+" , "+"添加客户端,id = "+ identityName );
                //检测是否存在可推送的消息
                checkOfflineMessageFromDbByIdentityName(identityName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void offline(String identityName, Current __current) {
        pool.post(()->{
            removeClient(identityName);
        });
    }

    private synchronized void addClient(String identityName, PushMessageClientPrx clientPrx){
        final PushMessageClientPrx _clientPrx = _clientsMaps_pc.get(identityName); //已存在的旧的客户端
        _clientsMaps_pc.put(identityName,clientPrx);
        communicator.getLogger().print(" --@@@@@@@@@@@@---> "+"添加客户端,id = "+ identityName +" ,当前客户端数量:"+ _clientsMaps_pc.size());
    }

    private synchronized void removeClient(String identityName) {
        _clientsMaps_pc.remove(identityName);
        communicator.getLogger().print("--XXXXXXXXXXXX---> "+"移除客户端,id = "+ identityName+" ,当前客户端数量:"+ _clientsMaps_pc.size() );
    }

    @Override
    public void sendMessageToClient(String identityName, String message, Current __current) {
        pool.post(()->{
            addMessage(identityName,message);
        });
    }

    //添加消息到队列
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

    //发送消息到客户端
    private boolean sendMessage(IPMessage message) {
        try {

                if (message.id == 0) {
                    //存入数据库
                    message.id = storeMessageToDb(message);
                }

                //获取客户端
                PushMessageClientPrx clientPrx = _clientsMaps_pc.get(message.identityName);
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

            communicator.getLogger().error(Thread.currentThread()+" , "+"发送失败,id =  '"+message.identityName+"' msg:"+message.content+" ,错误原因:"+ e);
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


    //消息发送线程
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
                checkPc(); //监测Pc端
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void checkPc() {
        if (_clientsMaps_pc.size() == 0) return;

        Iterator<Map.Entry<String,PushMessageClientPrx>> iterator = _clientsMaps_pc.entrySet().iterator();
        PushMessageClientPrx prx;

        while (iterator.hasNext()){
            prx = iterator.next().getValue();
            try {
                prx.ice_ping();
            } catch (Exception e) {
                iterator.remove();
                communicator.getLogger().print(Thread.currentThread()+" , "+"在线监测,PC-客户端移除: "+ communicator.identityToString(prx.ice_getIdentity())+",当前在线客户端数:"+ _clientsMaps_pc.size());
            }
        }
    }


}
