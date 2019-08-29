package com.onek.init;

import com.onek.server.infimp.IIceInitialize;
import dao.SQLSyncBean;
import dao.SynDbData;
import dao.SyncI;
import org.hyrdpf.ds.AppConfig;
import org.hyrdpf.util.LogUtil;
import redis.util.RedisUtil;
import util.GsonUtils;
import util.TimeUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

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
        final String SQL_MASTER_RESUME = SQL_SYNC_LIST+"_m";
        final String error_sync_Log = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath()).getParent()+"/"+listKey+"_sql_sync_err.log";
        final String error_master_Log = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath()).getParent()+"/"+listKey+"_sql_resume_master_err.log";
        SynDbData.syncI = new SyncI() {
            @Override
            public void addSyncBean(SQLSyncBean b) {
                //添加一个同步数据任务到redis
                try {
                    long i = 0;
                    if (b.isToMaster()){
                        //从库更新了,主库等待恢复的sql
                        i = RedisUtil.getListProvide().addEndElement(SQL_MASTER_RESUME, GsonUtils.javaBeanToJson(b));
                    }else{
                        i = RedisUtil.getListProvide().addEndElement(SQL_SYNC_LIST, GsonUtils.javaBeanToJson(b));

                    }

                    if (i == 1){
                       LogUtil.getDefaultLogger().info("持久化sql同步对象: "+b);
                    }else{
                        b.errorSubmit();
                    }

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
                    File file =new File(sqlSyncBean.isToMaster() ? error_master_Log : error_sync_Log);
                    if(!file.exists()){
                        file.createNewFile();
                    }
                    FileWriter fileWriter =new FileWriter(file, true);
                    String info = TimeUtils.date_yMd_Hms_2String(new Date())+"\t"+sqlSyncBean +System.getProperty("line.separator");
                    fileWriter.write(info);
                    fileWriter.flush();
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void executeSyncBean() {
               if (RedisUtil.getListProvide().size(SQL_MASTER_RESUME) > 0){
                   //从redis 获取一个任务执行
                   String json = RedisUtil.getListProvide().getElementByIndex(SQL_SYNC_LIST, 0);
                   if (json!=null){
                       SQLSyncBean b = SQLSyncBean.deserialization(json);
                       //LogUtil.getDefaultLogger().info("从缓存获取一个任务:\n"+b);
                       if (b != null && b.execute()){
                           long s1 = RedisUtil.getListProvide().size(SQL_SYNC_LIST);
                           json = RedisUtil.getListProvide().removeHeadElement(SQL_SYNC_LIST);
                           long s2 = RedisUtil.getListProvide().size(SQL_SYNC_LIST);
                           LogUtil.getDefaultLogger().info("同步前后列表大小:"+s1+">>>"+s2+", 同步数据: "+ json);
                       }else{
                           RedisUtil.getListProvide().removeHeadElement(SQL_SYNC_LIST);
                       }
                   }
               } else{
                    synchronized (SQL_SYNC_LIST){
                        try {
                            SQL_SYNC_LIST.wait(5 * 60 * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            boolean isSyncMaster = false;
            @Override
            public void notifyMasterActive() {
                if (isSyncMaster) return;
                new Thread(){
                    @Override
                    public void run() {
                        isSyncMaster = true;
                        while (RedisUtil.getListProvide().size(SQL_MASTER_RESUME) > 0) {
                                //从redis 列表,获取一个sql任务执行
                                String json = RedisUtil.getListProvide().getElementByIndex(SQL_MASTER_RESUME, 0);
                                if (json == null) {
                                    SQLSyncBean b = SQLSyncBean.deserialization(json);
                                    if (b != null && b.execute()){
                                        long s1 = RedisUtil.getListProvide().size(SQL_MASTER_RESUME);
                                        json = RedisUtil.getListProvide().removeHeadElement(SQL_MASTER_RESUME);
                                        long s2 = RedisUtil.getListProvide().size(SQL_MASTER_RESUME);
                                        LogUtil.getDefaultLogger().info("还原前后列表大小:"+s1+">>>"+s2+", 还原数据: "+ json);
                                    }
                                }
                        }
                        isSyncMaster = false;
                    }
                }.start();

            }
        };
    }

    @Override
    public int priority() {
        return 0;
    }

}
