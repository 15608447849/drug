package dao;

import util.GsonUtils;

import java.util.Arrays;
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
        if (currentExecute>15){
            SynDbData.log.warn("数据同步异常: "+ GsonUtils.javaBeanToJson(this));
        }
        SynDbData.syncI.addSyncBean(this);
    }

    public void execute(){
        currentExecute++;
        SynDbData.post(this);
    }

    @Override
    public String toString() {
        return "SQLSyncBean{" +
                "optType=" + optType +
                ", sharding=" + sharding +
                ", tbSharding=" + tbSharding +
                ", nativeSQL=" + Arrays.toString(nativeSQL) +
                ", resultSQL=" + Arrays.toString(resultSQL) +
                ", params=" + params +
                ", param=" + Arrays.toString(param) +
                ", batchSize=" + batchSize +
                '}';
    }
}
