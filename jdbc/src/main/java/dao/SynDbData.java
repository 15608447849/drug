package dao;

import constant.DSMConst;
import org.apache.logging.log4j.Logger;
import org.hyrdpf.dao.DAOException;
import org.hyrdpf.dao.FacadeProxy;
import org.hyrdpf.dao.jdbc.AbstractJdbcSessionMgr;
import org.hyrdpf.dao.jdbc.JdbcBaseDao;
import org.hyrdpf.dao.jdbc.JdbcTransaction;
import org.hyrdpf.util.LogUtil;
import threadpool.IOThreadPool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName SynDbData
 * @Description TODO
 * @date 2019-03-11 1:44
 */
public class SynDbData implements Runnable {

    private static final  IOThreadPool pool = new IOThreadPool();

    public static SyncI syncI = new SyncI() {
        @Override
        public void addSyncBean(SQLSyncBean b) {
            log.info("添加同步任务: "+ b);
        }
    };

    public static boolean isSynBackDB(int tbidx){
        if (BaseDAO.isMasterIndex .get() == 1) return false; //不同步从库
        if((DSMConst.SEG_TABLE_RULE[tbidx]  & (2+4)) > 0){
            return false;
        }
        return true;
    }

    final static Logger log = LogUtil.getDefaultLogger();

    private final BaseDAO baseDao = BaseDAO.getBaseDAO();

    private final SQLSyncBean b;
    SynDbData(SQLSyncBean b){
        this.b = b;
    }

    public static void post(SQLSyncBean sqlSyncBean) {
        pool.post(new SynDbData(sqlSyncBean));
    }




    @Override
    public void run(){
        try {
            //尝试执行sql
            switch (b.optType){
                case 0:
                      updateNative();
                      break;
                case 2:
                      updateTransNative();
                        break;
                case 4:
                     updateBatchNative();
                    break;
                case 5:
                     updateNativeBk();
                    break;
                case 6:
                     updateTransNativeBk();
                    break;
                case 7:
                     updateBatchNativeBK();
                    break;
            }
        } catch (Exception e) {
          log.error("执行同步sql失败",  e);
          b.submit();
        }
    }

    private final String ERROR_FORMAT = "同步失败,SQL: %s, 参数: %s, 结果: %d";


    //执行一条sql
    public void updateNative() throws DAOException{
        JdbcBaseDao jdbcBaseDao = FacadeProxy.create(JdbcBaseDao.class);
        jdbcBaseDao.setManager(baseDao.getBackupSessionMgr(b.sharding,Integer.parseInt(b.resultSQL[0])));
        int result = jdbcBaseDao.update(b.resultSQL[1], b.param);
        if (result<=0) throw new DAOException(
                String.format(ERROR_FORMAT,b.resultSQL[1],Arrays.toString(b.param),result)
              );
    }

    //运行后台
    public void updateNativeBk() throws DAOException{
        String[] resultSQL = baseDao.getNativeReplaceSQL(b.nativeSQL[0],b.tbSharding);
        JdbcBaseDao jdbcBaseDao = FacadeProxy.create(JdbcBaseDao.class);
        jdbcBaseDao.setManager(baseDao.getSessionMgr(0,DSMConst.TD_BK_TRAN_ORDER));
        int result  = jdbcBaseDao.update(resultSQL[1], b.param);
        if (result<=0) throw new DAOException(
                String.format(ERROR_FORMAT,b.resultSQL[1],Arrays.toString(b.param),result)
        );
    }

    //执行多条sql - 事务
    public void updateTransNative() throws DAOException{
        AbstractJdbcSessionMgr sessionMgr = baseDao.getBackupSessionMgr(b.sharding,Integer.parseInt(b.resultSQL[0]));
        FacadeProxy.executeCustomTransaction(sessionMgr,new JdbcTransaction() {
            @Override
            public void execute(AbstractJdbcSessionMgr sessionMgr) throws DAOException {
                for (int i = 0; i < b.nativeSQL.length; i++) {
                    int result;
                    if(b.sharding != 0){
                        result = baseDao.updateNativeInCallSharding(b.sharding,b.tbSharding,b.nativeSQL[i],1,b.params.get(i));
                    }else{
                        result = baseDao.updateNativeInCall(b.nativeSQL[i],1,b.params.get(i));
                    }
                    if (result<=0) throw new DAOException(
                            String.format(ERROR_FORMAT,b.resultSQL[1],Arrays.toString(b.param),result)
                    );
                }
            }
        });

    }

    //同步到运营平台
    public void updateTransNativeBk() throws DAOException{
        AbstractJdbcSessionMgr sessionMgr = baseDao.getSessionMgr(0,DSMConst.TD_BK_TRAN_ORDER);
        List<Object[]> bkParm = new ArrayList<>();
        List<String[]> bkResultSql = new ArrayList<>();
        for (int i = 0; i < b.nativeSQL.length; i++){
            String[] resultSQL = baseDao.getNativeReplaceSQL(b.nativeSQL[i],b.tbSharding);
            int tabInx = Integer.parseInt(resultSQL[0]);
            if(tabInx == DSMConst.TD_TRAN_ORDER
                    || tabInx == DSMConst.TD_TRAN_GOODS
                    || tabInx == DSMConst.TD_TRAN_REBATE){
                bkResultSql.add(resultSQL);
                bkParm.add(b.params.get(i));
            }
        }
        FacadeProxy.executeCustomTransaction(sessionMgr,new JdbcTransaction() {
            @Override
            public void execute(AbstractJdbcSessionMgr sessionMgr) throws DAOException {
                for (int i = 0; i < bkResultSql.size(); i++) {
                    int result = baseDao.updateNativeInCallBKSharding(b.sharding,b.tbSharding,bkResultSql.get(i),bkParm.get(i));
                    if (result<=0) throw new DAOException(
                            String.format(ERROR_FORMAT,bkResultSql.get(i),Arrays.toString(bkParm.get(i)),result)
                    );

                }
            }
        });
    }
    //批量执行
    public void updateBatchNative() throws DAOException{
        JdbcBaseDao jdbcBaseDao = FacadeProxy.create(JdbcBaseDao.class);
        jdbcBaseDao.setManager(baseDao.getBackupSessionMgr(b.sharding,Integer.parseInt(b.resultSQL[0])));
        int[] resultArr = jdbcBaseDao.updateBatch(b.resultSQL[1], b.params,b.batchSize);
        for (int i = 0; i <resultArr.length; i++){
            int result = resultArr[i];
            if (result<=0) throw new DAOException(
                    String.format(ERROR_FORMAT,b.nativeSQL[0],Arrays.toString( b.params.get(i)),result)
            );
        }
    }

    //运营后台
    public void updateBatchNativeBK() throws DAOException{
        String[] resultSQL = baseDao.getNativeReplaceSQL(b.nativeSQL[0],b.tbSharding);
        JdbcBaseDao jdbcBaseDao = FacadeProxy.create(JdbcBaseDao.class);
        jdbcBaseDao.setManager(baseDao.getSessionMgr(0,DSMConst.TD_BK_TRAN_ORDER));
        int[] resultArr = jdbcBaseDao.updateBatch(resultSQL[1], b.params,b.batchSize);
        for (int i = 0; i <resultArr.length; i++){
            int result = resultArr[i];
            if (result<=0) throw new DAOException(
                    String.format(ERROR_FORMAT,b.nativeSQL[0],Arrays.toString( b.params.get(i)),result)
            );
        }
    }

}
