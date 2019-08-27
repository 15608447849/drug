package com.onek.init;

import com.onek.server.infimp.IIceInitialize;
import dao.SQLSyncBean;
import dao.SynDbData;
import dao.SyncI;
import org.hyrdpf.ds.AppConfig;
import redis.util.RedisUtil;
import util.GsonUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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
        final String errorLog = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath()).getParent()+"/sql_sync_err_"+SQL_SYNC_LIST+".log";
        SynDbData.syncI = new SyncI() {
            @Override
            public void addSyncBean(SQLSyncBean b) {
                //添加一个同步数据任务到redis
                try {
                    Long i = RedisUtil.getListProvide().addEndElement(SQL_SYNC_LIST, GsonUtils.javaBeanToJson(b));
//                    LogUtil.getDefaultLogger().info("向缓存存入一个任务:\n"+json+" \t结果:" + i);
                    synchronized (SQL_SYNC_LIST){
                        SQL_SYNC_LIST.notify();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void errorSyncBean(SQLSyncBean sqlSyncBean) {

                try {
                    File file =new File(errorLog);
                    if(!file.exists()){
                        file.createNewFile();
                    }
                    FileWriter fileWriter =new FileWriter(file, true);
                    String info = sqlSyncBean +System.getProperty("line.separator");
                    fileWriter.write(info);
                    fileWriter.flush();
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void executeSyncBean() {
                //从redis 获取一个任务执行
                String json = RedisUtil.getListProvide().removeHeadElement(SQL_SYNC_LIST);
                if (json!=null){
                    SQLSyncBean b = GsonUtils.jsonToJavaBean(json,SQLSyncBean.class);
                    //LogUtil.getDefaultLogger().info("从缓存获取一个任务:\n"+b);
                    if (b != null) b.execute();
                } else{
                    synchronized (SQL_SYNC_LIST){
                        try {
                            SQL_SYNC_LIST.wait(60000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
    }

    @Override
    public int priority() {
        return 0;
    }

}
