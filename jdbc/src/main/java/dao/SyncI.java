package dao;

/**
 * @Author: leeping
 * @Date: 2019/8/26 12:40
 */
public interface SyncI {
    //添加到消息队列或缓存等持久化存储
    void addSyncBean(SQLSyncBean b);
    //同步失败记录
    void errorSyncBean(SQLSyncBean sqlSyncBean);
    //执行一个同步
    void executeSyncBean();
    //主库已恢复运行
    void notifyMasterActive();
}
