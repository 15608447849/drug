package dao;

import constant.BUSConst;
import org.hyrdpf.util.LogUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * 多线程全局查询
 */
public class GlobalQuery implements Callable<List<Object[]>>{

    private int dbs;
    private String nativeSQL;
    private Object[] params;
    private int year;
    private BaseDAO baseDAO = null;
    private List<Object[]> resultList = new ArrayList<>();

    public GlobalQuery(int dbs, int year, String nativeSQL, final Object... params){
        this.dbs = dbs;
        this.nativeSQL = nativeSQL;
        this.params = params;
        this.year = year;
        this.baseDAO = BaseDAO.getBaseDAO();
    }

    @Override
    public List<Object[]> call() throws Exception {
        for(int i = 0; i < BUSConst._MODNUM_EIGHT; i++){
            List<Object[]> results = baseDAO.queryNativeGlobal(year,dbs,i,nativeSQL,params);
            LogUtil.getDefaultLogger().debug("服务器编号："+dbs +" , 数据库编号："+i+" , 当前数据库记录条数"+results.size());
            resultList.addAll(results);
        }
        return resultList;
    }
}
