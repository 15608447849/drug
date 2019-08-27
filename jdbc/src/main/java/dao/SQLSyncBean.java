package dao;

import util.GsonUtils;

import java.util.List;

/**
 * @Author: leeping
 * @Date: 2019/8/26 11:19
 */
public class SQLSyncBean {

    int optType;//执行方法标识
    int sharding; //分库
    int tbSharding;//分表
    String[] nativeSQL;//需要执行的多条sql (事务) (需要转换)
    String[] resultSQL;//可执行的sql (已转换)
    List<Object[]> params;//sql参数
    Object[] param; //单sql
    int batchSize;//批量执行时的参数
    int currentExecute = 0;

    SQLSyncBean(int optType) {
        this.optType = optType;
    }

    void submit(){

    }

    public void execute(){
        SynDbData.post(this);
    }

    @Override
    public String toString() {
        return GsonUtils.javaBeanToJson(this);
    }

    void errorSubmit() {
        SynDbData.syncI.errorSyncBean(this);
    }
}
