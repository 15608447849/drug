package com.onek.init;

import com.onek.server.infimp.IIceInitialize;
import dao.SQLSyncBean;
import dao.SynDbData;
import dao.SyncI;
import org.hyrdpf.ds.AppConfig;
import org.hyrdpf.util.LogUtil;
import redis.util.RedisUtil;
import util.GsonUtils;

import static Ice.Application.communicator;

/**
 * @Author: leeping
 * @Date: 2019/3/18 15:37
 */
public class SystemInitialize implements IIceInitialize {
    @Override
    public void startUp(String serverName){
        try {
            AppConfig.initLogger();
            AppConfig.initialize();
            //异步日志
            System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");

            setSynI(serverName);

        } catch (Exception e) {
//            e.printStackTrace();
            communicator().getLogger().error(serverName+" , 初始化错误: "+ e);
        }
    }

    private void setSynI(String listKey) {
        final String SQL_SYNC_LIST = listKey;

        SynDbData.syncI = new SyncI() {
            @Override
            public void addSyncBean(SQLSyncBean b) {
                //添加一个同步数据任务到redis
                try {
                    String json =  GsonUtils.javaBeanToJson(b);
                    Long i = RedisUtil.getListProvide().addEndElement(SQL_SYNC_LIST, GsonUtils.javaBeanToJson(b));
//                    LogUtil.getDefaultLogger().info("向缓存存入一个任务:\n"+json+" \t结果:" + i);
                    synchronized (SQL_SYNC_LIST){
                        SQL_SYNC_LIST.notify();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        new Thread(){
            @Override
            public void run() {
                LogUtil.getDefaultLogger().info("启动数据库同步轮询线程");
                //从redis 获取一个任务执行
                while (true){
                    String json = RedisUtil.getListProvide().removeHeadElement(SQL_SYNC_LIST);
                    if (json!=null){
                        SQLSyncBean b = GsonUtils.jsonToJavaBean(json,SQLSyncBean.class);
                        LogUtil.getDefaultLogger().info("从缓存获取一个任务:\n"+b);
                        if (b != null) b.execute();
                    } else{
                        synchronized (SQL_SYNC_LIST){
                            try {
                                SQL_SYNC_LIST.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }.start();
    }

    @Override
    public int priority() {
        return 0;
    }

}
