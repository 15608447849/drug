package dao;

import constant.BUSConst;
import constant.DSMConst;
import org.apache.logging.log4j.Logger;
import org.hyrdpf.dao.DAOException;
import org.hyrdpf.dao.FacadeProxy;
import org.hyrdpf.dao.jdbc.AbstractJdbcSessionMgr;
import org.hyrdpf.dao.jdbc.JdbcBaseDao;
import org.hyrdpf.dao.jdbc.JdbcTransaction;
import org.hyrdpf.ds.AppConfig;
import org.hyrdpf.util.LogUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dao.BaseDAO.*;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName SynDbData
 * @Description TODO
 * @date 2019-03-11 1:44
 */
public class SynDbData {

    public static SyncI syncI = new SyncI() {
        @Override
        public void addSyncBean(SQLSyncBean b) {

        }

        @Override
        public void errorSyncBean(SQLSyncBean sqlSyncBean) {

        }

        @Override
        public void executeSyncBean() {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void notifyMasterActive() {

        }
    };

    static {
        new Thread(){
            @Override
            public void run() {
                log.info("启动数据库同步轮询");
                while (true){
                  syncI.executeSyncBean();
                }
            }
        }.start();
    }

    final static Logger log = LogUtil.getDefaultLogger();

    private final BaseDAO baseDao = BaseDAO.getBaseDAO();

    private final SQLSyncBean b;

    SynDbData(SQLSyncBean b){
        this.b = b;
    }

    public static boolean post(SQLSyncBean sqlSyncBean) {
//        pool.post();
       return new SynDbData(sqlSyncBean).execute();
    }

    private boolean execute(){
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
                case 100:
                    baseDao.updateNative(false,b.nativeSQL[0],b.param);
                    break;
                case 101:
                    baseDao.updateNativeSharding(false,b.sharding,b.tbSharding,b.nativeSQL[0],b.param);
                    break;
                case 102:
                    baseDao.updateTransNative(false,b.nativeSQL,b.params);
                    break;
                case 103:
                    baseDao.updateTransNativeSharding(false,b.sharding,b.tbSharding,b.nativeSQL,b.params);
                    break;
                case 104:
                    baseDao.updateBatchNative(false,b.nativeSQL[0],b.params,b.batchSize);
                    break;
                case 105:
                    baseDao.updateBatchNativeSharding(false,b.sharding,b.tbSharding,b.nativeSQL[0],b.params,b.batchSize);
                    break;
            }
            return true;
        } catch (Exception e) {
          log.error(e);
          b.currentExecute++;
          if (b.currentExecute > 3){
              b.errorSubmit();
              return true;
          }else{
              execute();
          }
        }
        return false;
    }

    private final String ERROR_FORMAT = "同步失败,SQL: %s, 参数: %s, 结果: %d";


    /***************************************************************同步到主库***************************************************************/
    //获取备份服务器
    private AbstractJdbcSessionMgr getBackupSessionMgr(int sharding, final int table){
        int dbs = 1;  //只负责向从库连接
        int db = 0;
        if ((DSMConst.SEG_TABLE_RULE[table] & 1) > 0) { //
            db = sharding % BUSConst._DMNUM  % BUSConst._MODNUM_EIGHT;
        }
        return AppConfig.getSessionManager(dbs,db);
    }

    //执行一条sql
    public void updateNative() throws DAOException{
        log.debug("【同步从库】updateNative：" + b.resultSQL[1] +","+ Arrays.toString( b.param));
        JdbcBaseDao jdbcBaseDao = FacadeProxy.create(JdbcBaseDao.class);
        jdbcBaseDao.setManager(getBackupSessionMgr(b.sharding,Integer.parseInt(b.resultSQL[0])));
        int result = jdbcBaseDao.update(b.resultSQL[1], b.param);
        if (result<=0) throw new DAOException(
                String.format(ERROR_FORMAT,b.resultSQL[1],Arrays.toString(b.param),result)
              );
    }

    //批量执行
    public void updateBatchNative() throws DAOException{
        log.debug("【同步从库】updateBatchNative：" + b.resultSQL[1] +","+ BaseDAO.paramListString(b.params)+","+b.batchSize);
        JdbcBaseDao jdbcBaseDao = FacadeProxy.create(JdbcBaseDao.class);
        jdbcBaseDao.setManager(getBackupSessionMgr(b.sharding,Integer.parseInt(b.resultSQL[0])));
        int[] resultArr = jdbcBaseDao.updateBatch(b.resultSQL[1], b.params,b.batchSize);
        for (int i = 0; i <resultArr.length; i++){
            int result = resultArr[i];
            if (result<=0) throw new DAOException(
                    String.format(ERROR_FORMAT,b.nativeSQL[0],Arrays.toString( b.params.get(i)),result)
            );
        }
    }

    //执行多条sql - 事务
    public void updateTransNative() throws DAOException{
        AbstractJdbcSessionMgr sessionMgr = getBackupSessionMgr(b.sharding,Integer.parseInt(b.resultSQL[0]));
        JdbcBaseDao dao = FacadeProxy.create(JdbcBaseDao.class);
        dao.setManager(sessionMgr);
        FacadeProxy.executeCustomTransaction(sessionMgr,new JdbcTransaction() {
            @Override
            public void execute(AbstractJdbcSessionMgr sessionMgr) throws DAOException {
                for (int i = 0; i < b.nativeSQL.length; i++) {
                    String[] _resultSQL = baseDao.getNativeSQL(b.nativeSQL[i],b.tbSharding);
                    Object[] _params = b.params.get(i);
                    log.debug("【同步从库】updateTransNative：" +  Arrays.toString(b.resultSQL)+","+Arrays.toString(b.param));
                    int result =  dao.update(_resultSQL[1], _params);
                    if (result<=0) throw new DAOException(
                            String.format(ERROR_FORMAT, Arrays.toString(b.resultSQL),Arrays.toString(b.param),result)
                    );
                }
            }
        });

    }


    /**************************************************同步到运营************************************************/




    protected String[] getNativeReplaceSQL(String nativeSQL,int tbSharding){
        String[] result = new String[2];
        //默认从第一个字符串开始查询指定的子字符串
        int startIndex = 0;
        //查询指定的子字符串终止的位置值
        int endIndex = nativeSQL.lastIndexOf(SUFFIX_REGEX);

        while (startIndex < endIndex) {
            //查找原生态查询语句中第一个表对象固定格式的前缀所在的起始位置值。
            startIndex = nativeSQL.indexOf(PREFIX_REGEX, startIndex);
            //查找原生态查询语句中第一个表对象固定格式的后缀所在的起始位置值。
            endIndex = nativeSQL.indexOf(SUFFIX_REGEX, startIndex + BUSConst._FOUR);
            //指定第二次查找子字符串时的起始查找点的位置值
            if(startIndex == endIndex) break;
            //取得原生态查询语句中表对象在DSMConst.DB_TABLES常量里索引值，需要在表对象固定格式的前缀所在的起始位置值加上固定格式“{{?”三位长度。
            result[0] = nativeSQL.substring(startIndex + BUSConst._THREE,endIndex);
            int tableIndex = Integer.parseInt(result[0]);
            //正则表达式
            String regex = PREFIX_REGEX_SB.toString() + tableIndex + SUFFIX_REGEX_SB;
            //取和数据库里真正的要查询的表名
            nativeSQL = nativeSQL.replaceAll(regex,getReplaceTableName(tableIndex,tbSharding));
        }
        result[1] = nativeSQL;
        return result;
    }

    /**切分表实现,table：是要操作那个基本表，基本表就是没有切分前的表*/
    private String getReplaceTableName(int table,int tbSharding)
    {
        //替换表
        if(table == DSMConst.TD_TRAN_ORDER){
            table = DSMConst.TD_BK_TRAN_ORDER;
        }

        if(table == DSMConst.TD_TRAN_GOODS){
            table = DSMConst.TD_BK_TRAN_GOODS;
        }

        if(table == DSMConst.TD_TRAN_REBATE){
            table = DSMConst.TD_BK_TRAN_REBATE;
        }

        StringBuilder strSql = new StringBuilder(DSMConst.DB_TABLES[table][BUSConst._ZERO]);
        //按公司模型切分表
        if(tbSharding > 0){
            strSql.append(DSMConst._UNDERLINE);
            strSql.append(tbSharding);
        }
        return strSql.toString();
    }

    //分库分表
    private AbstractJdbcSessionMgr getSessionMgrBk(final int table){
        int dbs = 0;
        int db = 0;
        //与主从无关 如果是运营库,则只会获取运营库的连接
        if((DSMConst.SEG_TABLE_RULE[table] & 4) > 0){
            dbs = AppConfig.getDBSNum() - BUSConst._ONE;
        }
        return AppConfig.getSessionManager(dbs,db);
    }

    //同步到运营平台
    private void updateTransNativeBk() throws DAOException{

        final List<Object[]> bkParm = new ArrayList<>();
        final List<String[]> bkResultSql = new ArrayList<>();
        for (int i = 0; i < b.nativeSQL.length; i++){
            String[] resultSQL = getNativeReplaceSQL(b.nativeSQL[i],b.tbSharding);
            int tabInx = Integer.parseInt(resultSQL[0]);
            if(tabInx == DSMConst.TD_TRAN_ORDER
                    || tabInx == DSMConst.TD_TRAN_GOODS
                    || tabInx == DSMConst.TD_TRAN_REBATE){
                bkResultSql.add(resultSQL);
                bkParm.add(b.params.get(i));
            }
        }

        AbstractJdbcSessionMgr sessionMgr = getSessionMgrBk(DSMConst.TD_BK_TRAN_ORDER);
        JdbcBaseDao dao = FacadeProxy.create(JdbcBaseDao.class);
        dao.setManager(sessionMgr);
        FacadeProxy.executeCustomTransaction(sessionMgr,new JdbcTransaction() {
            @Override
            public void execute(AbstractJdbcSessionMgr sessionMgr) throws DAOException {
                for (int i = 0; i < bkResultSql.size(); i++) {
                    log.debug("【同步运营】updateTransNativeBk：" +  bkResultSql.get(i)[1]+","+Arrays.toString(bkParm.get(i)));
                    int result =  dao.update(bkResultSql.get(i)[1],bkParm.get(i));
                    if (result<=0) throw new DAOException(
                            String.format(ERROR_FORMAT, bkResultSql.get(i)[1],Arrays.toString(bkParm.get(i)),result)
                    );

                }
            }
        });
    }

    //同步运营后台
    private void updateNativeBk() throws DAOException{
        String[] resultSQL = getNativeReplaceSQL(b.nativeSQL[0],b.tbSharding);
        JdbcBaseDao jdbcBaseDao = FacadeProxy.create(JdbcBaseDao.class);
        jdbcBaseDao.setManager(getSessionMgrBk(DSMConst.TD_BK_TRAN_ORDER));
        log.debug("【同步运营】updateNativeBk：" +  resultSQL[1]+","+Arrays.toString(b.param));
        int result  = jdbcBaseDao.update(resultSQL[1], b.param);
        if (result<=0) throw new DAOException(
                String.format(ERROR_FORMAT,b.resultSQL[1],Arrays.toString(b.param),result)
        );
    }

    //运营后台
    private void updateBatchNativeBK() throws DAOException{
        String[] resultSQL = getNativeReplaceSQL(b.nativeSQL[0],b.tbSharding);
        JdbcBaseDao jdbcBaseDao = FacadeProxy.create(JdbcBaseDao.class);
        jdbcBaseDao.setManager(getSessionMgrBk(DSMConst.TD_BK_TRAN_ORDER));
        log.debug("【同步运营】updateBatchNativeBK：" +  resultSQL[1]+","+BaseDAO.paramListString(b.params)+","+b.batchSize);
        int[] resultArr = jdbcBaseDao.updateBatch(resultSQL[1], b.params,b.batchSize);
        for (int i = 0; i <resultArr.length; i++){
            int result = resultArr[i];
            if (result<=0) throw new DAOException(
                    String.format(ERROR_FORMAT,b.nativeSQL[0],Arrays.toString( b.params.get(i)),result)
            );
        }
    }

}
