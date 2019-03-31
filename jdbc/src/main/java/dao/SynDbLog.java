package dao;

import constant.DSMConst;
import dao.BaseDAO;
import dao.SynDbData;
import org.hyrdpf.dao.DAOException;
import org.hyrdpf.dao.FacadeProxy;
import org.hyrdpf.dao.jdbc.AbstractJdbcSessionMgr;
import org.hyrdpf.dao.jdbc.JdbcBaseDao;
import org.hyrdpf.dao.jdbc.JdbcTransaction;
import org.hyrdpf.util.LogUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName LogDbUtil
 * @Description TODO
 * @date 2019-03-13 23:15
 */
public class SynDbLog {

    private static BaseDAO baseDao = BaseDAO.getBaseDAO();


    private static final String QUERY_SYNLOG_EXIST = "select oid from {{?"+ DSMConst.D_SYN_LOG +"}} where tbid = ?" +
            " and sdbid = ? and unqval = ? and sharding = ? and tbsharding = ? ";

    private static final String UPDATE_SYNLOG = "update {{?"+ DSMConst.D_SYN_LOG +"}} set opttime = now()," +
            "syntime = null,cstatus = 0 where oid = ?";

    private static final String INSERT_SYNLOG = "insert into {{?"+ DSMConst.D_SYN_LOG +"}} tbid,sdbid,unqval," +
            "opttime,sharding,tbsharding,cstatus) values (?,?,?,now(),?,?,0) ";



    public static boolean isBaseSqlError(Throwable e){
        if(e instanceof SQLException){
            LogUtil.getDefaultLogger().debug("SQL异常！");
            SQLException sqlException = (SQLException) e;
            if(sqlException.getSQLState() != null
                    && ((sqlException.getSQLState()).startsWith("22")
                    || (sqlException.getSQLState()).startsWith("23")
                    ||(sqlException.getSQLState()).startsWith("42"))){
                return true;
            }
        }
        return false;
    }



    public static int[] updateTransNative(String[] nativeSQL,List<Object[]> params,
                                          int sharding,int tbSharding,int master,boolean isbatch){
        Object[] parmsResult = convParms(nativeSQL,params,sharding,tbSharding,master,isbatch);
        if(parmsResult == null) return null;
        int[] result = new int[nativeSQL.length];
        String[] logSql = (String[])parmsResult[0];
        List<Object[]> logParams = (List<Object[]>)parmsResult[1];
        String[] resultSQL = baseDao.getNativeSQL(logSql[0],tbSharding);
        AbstractJdbcSessionMgr sessionMgr = baseDao.getBackupSessionMgr(Integer.parseInt(resultSQL[0]),sharding,true);
        try {
            FacadeProxy.executeCustomTransaction(sessionMgr,new JdbcTransaction() {
                @Override
                public void execute(AbstractJdbcSessionMgr sessionMgr) throws DAOException {
                    for (int i = 0; i < logSql.length; i++) {
                        result[i] = baseDao.updateNativeInCallSharding(sharding,tbSharding,logSql[i],2,logParams.get(i));
                    }
                }
            });
        } catch (DAOException e) {
            LogUtil.getDefaultLogger().error(e.getStackTrace());
            return null;
        }
        return result;
    }






    public static Object[] convParms(String[] nativeSQL, List<Object[]> params,
                                                  int sharding, int tbSharding,int master,
                                     boolean isbatch){
        if(!isbatch && nativeSQL.length != params.size()) return null;
        String[] logNativeSql = new String[params.size()];
        List<Object[]> logParams = new ArrayList<>();
        int dbs = (master == 0 ? 1 : 0);
        Object[] objArray = new Object[2];
        for (int i = 0; i < params.size(); i++){
            String[] resultSQL = baseDao.getNativeSQL(nativeSQL[i],tbSharding);
            List<Object[]> queryResult = queryNative(QUERY_SYNLOG_EXIST,
                    new Object[]{resultSQL[0],dbs,params.get(i)[0],sharding,tbSharding});

            if(queryResult == null || queryResult.isEmpty()){
                logNativeSql[i] = INSERT_SYNLOG;
                logParams.add(new Object[]{resultSQL[0],dbs,params.get(i)[0],sharding,tbSharding});
            }else{
                logNativeSql[i] = UPDATE_SYNLOG;
                logParams.add(new Object[]{queryResult.get(0)[0]});
            }
        }
        objArray[0] = logNativeSql;
        objArray[1] = logParams;
        return objArray;
    }


    public static List<Object[]> queryNative(String nativeSQL,final Object... params){
        List<Object[]> resultList = null;
        String[] result = baseDao.getNativeSQL(nativeSQL,0);
        LogUtil.getDefaultLogger().debug("【Debug】LOG SQL：" + result[1]);
        JdbcBaseDao jdbcBaseDao = FacadeProxy.create(JdbcBaseDao.class);
        jdbcBaseDao.setManager(baseDao.getBackupSessionMgr(Integer.parseInt(result[0]),0,true));
        resultList = jdbcBaseDao.query(result[1], params);
        return resultList;
    }


    public static boolean isSynBackDB(int tbidx){
        System.out.println(tbidx);
        if((DSMConst.SEG_TABLE_RULE[tbidx]  & 2) > 0){
            return true;
        }
        return false;
    }
}
