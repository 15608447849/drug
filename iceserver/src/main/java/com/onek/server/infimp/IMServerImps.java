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

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: leeping
 * @Date: 2019/4/9 17:57
 * 消息推送 服务端实现
 */
public class IMServerImps extends _InterfacesDisp implements IPushMessageStore {


    //超时时间毫秒数
    private final int PING_TIMEOUT_MAX = 30 * 1000;
    private final ReentrantLock lock = new ReentrantLock();
    /**
     *  当前在线的所有客户端
     */
    private HashMap<String,HashMap<String, ArrayList<PushMessageClientPrx>>> onlineClientMaps;

    protected IOThreadPool pool ;

    protected final Communicator communicator;

    private IPushMessageStore iPushMessageStore;


    //待发送消息存储的队列
    private ConcurrentLinkedQueue<IPMessage> messageQueue;

    IMServerImps(Communicator communicator, String serverName) {
        this.communicator = communicator;
        startPushMessageServer(serverName);
    }

    private void startPushMessageServer(String serverName){
        if (StringUtils.isEmpty(IceProperties.INSTANCE.allowPushMessageServer)) return;
        if (!IceProperties.INSTANCE.allowPushMessageServer.contains(serverName)) return;
        pool = new IOThreadPool();
        onlineClientMaps = new HashMap<>();
        messageQueue = new ConcurrentLinkedQueue<>();
        //注入消息存储实现
        createMessageStoreImps();
        pool.post(heartRunnable());//心跳线程
        pool.post(pushRunnable());//消息发送
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
    public void online(Identity identity, Current __current)  {
        try {
            final String identityName = identity.name;
            final String clientType =  identity.category;
            if ( StringUtils.isEmpty(identityName,clientType) ) throw new Exception("客户端信息不完整,不允许接入");
            Ice.ObjectPrx base = __current.con.createProxy(identity);
            final PushMessageClientPrx client = PushMessageClientPrxHelper.uncheckedCast(base);
            pool.post(()->{
                //添加到队列
                addClient(clientType,identityName,client);
                //检测是否存在可推送的消息
                checkOfflineMessageFromDbByIdentityName(identityName);
            });
        } catch (java.lang.Exception e) {
            throw new Ice.Exception(e.getCause()){
                @Override
                public String ice_name() {
                    return "通讯服务连接拒绝";
                }
            };
        }
    }

    //添加客户端到队列
    private void addClient(String clientType,String identityName, PushMessageClientPrx clientPrx){
        try{
            lock.lock();
            //1.根据种类判断是否存在,不存在创建并存入
            HashMap<String, ArrayList<PushMessageClientPrx>> map = onlineClientMaps.computeIfAbsent(clientType, k -> new HashMap<>());
            //2.根据标识查询客户端列表,不存在列表,创建并存入
            ArrayList<PushMessageClientPrx> list = map.computeIfAbsent(identityName,k -> new ArrayList<>());
            //3.加入列表
            list.add(clientPrx);
            communicator.getLogger().print(" --@@@@@@@@@@@@---> "+clientType+" 添加客户端,id = "+ identityName +" ,相同连接数量:"+ list.size());
        }finally {
            lock.unlock();
        }
    }

    //获取此标识的全部客户端列表
    private List<ArrayList<PushMessageClientPrx>> getClientPrxList(String identityName) {

        List<ArrayList<PushMessageClientPrx>> list = new ArrayList<>();
        Iterator<Map.Entry<String,HashMap<String,ArrayList<PushMessageClientPrx>>>> it = onlineClientMaps.entrySet().iterator();
        while (it.hasNext()){
            HashMap<String,ArrayList<PushMessageClientPrx>> map = it.next().getValue();
            Iterator<Map.Entry<String,ArrayList<PushMessageClientPrx>>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry<String,ArrayList<PushMessageClientPrx>> entry = iterator.next();
                String key = entry.getKey();
                if (identityName.equals(key)){
                    list.add(new ArrayList<>(entry.getValue())); //复制客户端出来
                }
            }
        }
       return list;
    }

    //发送消息到客户端
    @Override
    public void sendMessageToClient(String identityName, String message, Current __current) {
        //放入消息队列
        messageQueue.offer(new IPMessage(identityName, message));
        synchronized (messageQueue){
            messageQueue.notify();
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
                List<ArrayList<PushMessageClientPrx>> clientPrxList = getClientPrxList(message.identityName);
                boolean isSend = false;
                for (ArrayList<PushMessageClientPrx> list : clientPrxList){

                   Iterator<PushMessageClientPrx> iterator = list.iterator();
                    while (iterator.hasNext()){
                        PushMessageClientPrx clientPrx = iterator.next();
                        try {
                            clientPrx.receive(convertMessage(message));
                            isSend = true;
                        } catch (Exception e) {
                            communicator.getLogger().error(Thread.currentThread()+" , "+"发送失败, 客户端-"+communicator.identityToString(clientPrx.ice_getIdentity())+" ,msg:"+message.content+" ,错误原因:"+ e);
                        }
                    }
                }
                if (isSend)  changeMessageStateToDb(message); //数据发送成功

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
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

    //检测是否存在离线消息
    @Override
    public List<IPMessage> checkOfflineMessageFromDbByIdentityName(String identityName) {
        if (iPushMessageStore!=null){
            List<IPMessage> messageList = iPushMessageStore.checkOfflineMessageFromDbByIdentityName(identityName);
            for (IPMessage message : messageList){
                if (!sendMessage(message)) break;
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
                    IPMessage message = messageQueue.poll();
                    if (message == null){
                        synchronized (messageQueue){
                            try {
                                messageQueue.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        continue;
                    }
                    sendMessage(message);
                } catch (Exception ignored) {
                }
            }
        };
    }

    private Runnable heartRunnable(){
        return () ->{
            //循环检测 -保活
            while (!communicator.isShutdown()){
                try {
                    Thread.sleep( 30 * 1000);
                    checkConnect(); //监测
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }


    //检测连接
    private void checkConnect() {

        Iterator<Map.Entry<String,HashMap<String, ArrayList<PushMessageClientPrx>>>> it = onlineClientMaps.entrySet().iterator();
        while (it.hasNext()){
            HashMap<String,ArrayList<PushMessageClientPrx>> map = it.next().getValue();
            Iterator<Map.Entry<String,ArrayList<PushMessageClientPrx>>> it2 = map.entrySet().iterator();
            while (it2.hasNext()){
                ArrayList<PushMessageClientPrx> list = it2.next().getValue();
                Iterator<PushMessageClientPrx> it3 = list.iterator();
                while (it3.hasNext()){
                    PushMessageClientPrx clientPrx = it3.next();
                    try {
                        long t = System.currentTimeMillis();
                        clientPrx.ice_invocationTimeout(PING_TIMEOUT_MAX).ice_ping();
                        communicator.getLogger().print("检测存活: " + clientPrx.ice_getIdentity() +" , 耗时: " + (System.currentTimeMillis() - t)+" 毫秒");
                    } catch (Exception e) {
                        it3.remove();
                        e.printStackTrace();
                        communicator.getLogger().print(
                                Thread.currentThread()+" , "+"在线监测失败, 移除客户端:" +
                                " "+ communicator.identityToString(clientPrx.ice_getIdentity())+" 原因:"+  e);
                    }
                }
                if (list.size() == 0) it2.remove();
            }
            if (map.size() == 0) it.remove();
        }
    }
}
