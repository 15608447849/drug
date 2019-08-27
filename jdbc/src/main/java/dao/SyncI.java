package dao;

/**
 * @Author: leeping
 * @Date: 2019/8/26 12:40
 */
public interface SyncI {
    //添加到消息队列或缓存等持久化存储
    void addSyncBean(SQLSyncBean b);
}
