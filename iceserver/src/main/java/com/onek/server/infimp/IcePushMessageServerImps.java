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
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: leeping
 * @Date: 2019/4/9 17:57
 * 消息推送 服务端实现
 */
public class IcePushMessageServerImps extends _InterfacesDisp implements IPushMessageStore,Runnable {

    private final ReentrantLock lock = new ReentrantLock();
    /**
     *  当前在线的所有客户端
     */
    private HashMap<String,HashMap<String, ArrayList<PushMessageClientPrx>>> onlineClientMaps;

    protected IOThreadPool pool ;

    protected final Communicator communicator;

    private IPushMessageStore iPushMessageStore;

    private volatile boolean lockQueue = false;

    IcePushMessageServerImps(Communicator communicator,String serverName) {
        this.communicator = communicator;
        startPushMessageServer(serverName);
    }

    private void startPushMessageServer(String serverName){
        if (StringUtils.isEmpty(IceProperties.INSTANCE.allowPushMessageServer)) return;
        if (!IceProperties.INSTANCE.allowPushMessageServer.contains(serverName)) return;
        pool = new IOThreadPool();
        onlineClientMaps = new HashMap<>();
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
    public void online(Identity identity, Current __current)  {
        try {
            //后期根据类型分发客户端 -
            Ice.ObjectPrx base = __current.con.createProxy(identity);
            final String identityName = identity.name;
            final String clientType =  identity.category;
            communicator.getLogger().print(Thread.currentThread()+" 上线提示 ---------------- "+"客户端( "+ clientType + "),id = "+ identity.name  );
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


    @Override
    public void offline(String clientType,String identityName, Current __current) {
        //移除客户端
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
                    list.add(entry.getValue());
                }
            }
        }
       return list;
    }

    //发送消息到客户端
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
                List<ArrayList<PushMessageClientPrx>> clientPrxList = getClientPrxList(message.identityName);
                boolean isSend = false;
                for (ArrayList<PushMessageClientPrx> list : clientPrxList){

                   Iterator<PushMessageClientPrx> iterator = list.iterator();
                    while (iterator.hasNext()){
                        PushMessageClientPrx clientPrx = iterator.next();
                        try {
                            clientPrx.receive(convertMessage(message));
                            communicator.getLogger().print(Thread.currentThread()+" , "+"send ok , '"+message.identityName+"' msg:"+message.content);
                            isSend = true;

                        } catch (Exception e) {
                            iterator.remove();//连接失效
                            communicator.getLogger().error(Thread.currentThread()+" , "+"发送失败," +
                                    "id =  '"+message.identityName+"' msg:"+message.content+" ( " + communicator.identityToString(clientPrx.ice_getIdentity()) + " )"+" ,错误原因:"+ e);
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
                checkConnect(); //监测Pc端
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void checkConnect() {

        Iterator<Map.Entry<String,HashMap<String,ArrayList<PushMessageClientPrx>>>> it = onlineClientMaps.entrySet().iterator();
        while (it.hasNext()){
            HashMap<String,ArrayList<PushMessageClientPrx>> map = it.next().getValue();
            Iterator<Map.Entry<String,ArrayList<PushMessageClientPrx>>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()){
                ArrayList<PushMessageClientPrx> list = iterator.next().getValue();
                Iterator<PushMessageClientPrx> iteratorList = list.iterator();
                while (iteratorList.hasNext()){
                    PushMessageClientPrx clientPrx = iteratorList.next();
                    try {
                        clientPrx.ice_ping();
                    } catch (Exception e) {
                        iterator.remove();
                        communicator.getLogger().print(Thread.currentThread()+" , "+"在线监测客户端移除:" +
                                " "+ communicator.identityToString(clientPrx.ice_getIdentity())+",当前在线客户端数:"+ list.size());
                    }
                }
            }
        }

    }


}
