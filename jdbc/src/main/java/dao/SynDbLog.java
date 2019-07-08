package dao;

import constant.BUSConst;
import constant.DSMConst;
import org.hyrdpf.dao.DAOException;
import org.hyrdpf.dao.FacadeProxy;
import org.hyrdpf.dao.jdbc.AbstractJdbcSessionMgr;
import org.hyrdpf.dao.jdbc.JdbcBaseDao;
import org.hyrdpf.dao.jdbc.JdbcTransaction;
import org.hyrdpf.util.LogUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName LogDbUtil
 * @Description TODO
 * @date 2019-03-13 23:15
 */
public class SynDbLog {

    private static BaseDAO baseDao = BaseDAO.getBaseDAO();

    /**原生态SQL查询时，指定的查询表对象固定格式的前缀字符串。*/
    public static final String PREFIX_REGEX = "{{?";
    /**原生态SQL查询时，指定的查询表对象固定格式的后缀字符串。*/
    public static final String SUFFIX_REGEX = "}}";
    /**原生态SQL查询时，指定的查询表对象固定格式的前缀字符串与后缀字符串，对应的正则表达式的字符串*/
    private static final StringBuffer PREFIX_REGEX_SB = new StringBuffer("\\{\\{\\?");
    private static final StringBuffer SUFFIX_REGEX_SB = new StringBuffer("\\}\\}");

    public static  HashMap<Integer,String> map = new HashMap<Integer,String>();

    static{
            map.put(DSMConst.TD_TRAN_ORDER,"orderno");
            map.put(DSMConst.TB_AREA_PCA, "areac");
            map.put(DSMConst.TB_AREA_VILLAGES, "areac");
            map.put(DSMConst.TB_COMP, "cid");
            map.put(DSMConst.TB_COMP_APTITUDE, "cid");
            map.put(DSMConst.TB_COMP_BUS_SCOPE, "compid");
            map.put(DSMConst.TB_COMP_INVOICE, "cid");
            map.put(DSMConst.TB_COMP_SHIP_INFO, "shipid");
            map.put(DSMConst.TB_PROXY_NOTICE, "msgid");
            map.put(DSMConst.TB_SYSTEM_BUS_SCOPE, "code");
            map.put(DSMConst.TB_SYSTEM_USER, "uid");
            map.put(DSMConst.TD_PROD_BRAND, "brandno");
            map.put(DSMConst.TD_PROD_MANU, "manuno");
            map.put(DSMConst.TD_PROD_SKU, "sku");
            map.put(DSMConst.TD_PROD_SPU, "spu");
            map.put(DSMConst.TD_PRODUCE_CLASS, "classid");

    }




    private static final String QUERY_SYNLOG_EXIST = "select oid from {{?"+ DSMConst.TD_SYN_LOG +"}} where tbid = ?" +
            " and sdbid = ? and unqval = ? and sharding = ? and tbsharding = ? ";

    private static final String UPDATE_SYNLOG = "update {{?"+ DSMConst.TD_SYN_LOG +"}} set opttime = now()," +
            "syntime = null,cstatus = 0 where oid = ?";

    private static final String INSERT_SYNLOG = "insert into {{?"+ DSMConst.TD_SYN_LOG +"}} (tbid,sdbid,unqval," +
            "opttime,sharding,tbsharding,cstatus) values (?,?,?,now(),?,?,0) ";

    private static final String QUERY_SYNLOG = "select tbid,sdbid,unqval,opttime,sharding,tbsharding,cstatus from {{?"+ DSMConst.TD_SYN_LOG +"}} " +
            " where cstatus & 1 = 0 ";



    public static boolean isBaseSqlError(Throwable e){
        if(e instanceof SQLException){
            SQLException sqlException = (SQLException) e;
            LogUtil.getDefaultLogger().debug("SQL异常编号："+sqlException.getSQLState());
            LogUtil.getDefaultLogger().error("SQL异常信息：",e);
            if(sqlException.getSQLState() != null
                    && ((sqlException.getSQLState()).startsWith("S0")
                    || (sqlException.getSQLState()).startsWith("S1")
                    ||(sqlException.getSQLState()).startsWith("22")
                    || (sqlException.getSQLState()).startsWith("23")
                    ||(sqlException.getSQLState()).startsWith("42"))){
                LogUtil.getDefaultLogger().debug("SQL异常！");
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
        AbstractJdbcSessionMgr sessionMgr = baseDao.getBackupSessionMgr(sharding,Integer.parseInt(resultSQL[0]),true);
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

            List<Object[]> unqidList =
                    getUnqId(resultSQL[1],params.get(i),Integer.parseInt(resultSQL[0]),sharding,tbSharding);

            String unqId = params.get(i)[0].toString();
            if(unqidList != null && !unqidList.isEmpty()){
                unqId = unqidList.get(0)[0].toString();
            }

             List<Object[]> queryResult = queryNative(QUERY_SYNLOG_EXIST,
                    new Object[]{resultSQL[0],dbs,unqId,sharding,tbSharding});

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
        jdbcBaseDao.setManager(baseDao.getBackupSessionMgr(0,Integer.parseInt(result[0]),true));
        resultList = jdbcBaseDao.query(result[1], params);
        return resultList;
    }


    public static List<Object[]> queryUnqId(String nativeSQL,int sharding,int tbSharding, final Object... params){
        List<Object[]> resultList = null;
        String[] result = baseDao.getNativeSQL(nativeSQL);
        if(sharding > 0){
            result = baseDao.getNativeSQL(nativeSQL,tbSharding);
            //判断是否为同步运营平台的表
            if((DSMConst.SEG_TABLE_RULE[Integer.parseInt(result[0])] & 4) > 0){
                result = getLogNativeReplaceSQL(nativeSQL,tbSharding);
            }
        }
        LogUtil.getDefaultLogger().debug("【Debug】LOG SQL：" + result[1]);
        JdbcBaseDao jdbcBaseDao = FacadeProxy.create(JdbcBaseDao.class);
        jdbcBaseDao.setManager(baseDao.getBackupSessionMgr(sharding,Integer.parseInt(result[0]),false));
        resultList = jdbcBaseDao.query(result[1], params);
        return resultList;
    }


    /**
     * 根据SQL获取对应记录的唯一ID
     * @param nativeSQL
     * @param params
     * @param tbidx
     * @param sharding
     * @param tbSharding
     * @return
     */
    public static List<Object[]> getUnqId(String nativeSQL,Object[] params,int tbidx,
                                  int sharding, int tbSharding){
        List<Object[]> result = null;
        String sql = nativeSQL;
        sql = sql.trim().toLowerCase();
        String wSql = "";
        int wParmSize = 0;
        if(!sql.startsWith("update")){
            return null;
        }else{
            if(sql.contains("where")){
                wSql = sql.substring(sql.indexOf("where"));
                System.out.println(wSql);
            }
            wParmSize = wSql.split("\\?").length;
        }
        List parmList = new ArrayList();
        for(int i = params.length - wParmSize; i < params.length; i++){
            parmList.add(params[i]);
        }
        String keyWord = "unqid";
        if(map.containsKey(tbidx)){
            keyWord = map.get(tbidx);
        }
        StringBuilder keySqlSb = new StringBuilder();
        keySqlSb.append("select ");
        keySqlSb.append(keyWord);
        keySqlSb.append(" from {{?").append(tbidx);
        keySqlSb.append("}} ");
        keySqlSb.append(wSql);
        Object[] parms = new Object[parmList.size()];
        parms = parmList.toArray(parms);
        result = queryUnqId(keySqlSb.toString(),
                sharding, tbSharding, parms);
        return result;
    }


    public static boolean isSynBackDB(int tbidx){
        if((DSMConst.SEG_TABLE_RULE[tbidx]  & (2+4)) > 0){
            return false;
        }
        return true;
    }



    public static String[] getLogNativeReplaceSQL(String nativeSQL,int tbSharding){
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
            String regex = (PREFIX_REGEX_SB).toString() + tableIndex + SUFFIX_REGEX_SB;
            //取和数据库里真正的要查询的表名
            nativeSQL = nativeSQL.replaceAll(regex,getLogReplaceTableName(tableIndex,tbSharding));
        }
        result[1] = nativeSQL;
        return result;
    }


    private static String getLogReplaceTableName(int table,int tbSharding)
    {
        //替换表 TD_BK_TRAN_ORDER
        if(table == DSMConst.TD_BK_TRAN_ORDER){
            table = DSMConst.TD_TRAN_ORDER;
        }

        if(table == DSMConst.TD_BK_TRAN_GOODS){
            table = DSMConst.TD_TRAN_GOODS;
        }

        if(table == DSMConst.TD_BK_TRAN_REBATE){
            table = DSMConst.TD_TRAN_REBATE;
        }

        StringBuilder strSql = new StringBuilder(DSMConst.DB_TABLES[table][BUSConst._ZERO]);
        //按公司模型切分表
        if(tbSharding != 0){
            strSql.append(DSMConst._UNDERLINE);
            if(tbSharding == 0){
                Calendar date = Calendar.getInstance();
                strSql.append(date.get(Calendar.YEAR));
            }else{
                strSql.append(tbSharding);
            }
        }
        return strSql.toString();
    }


    /**
     * 查询同步失败的日志记录
     * @param isSharding 是否存在表库切分
     * @return
     */
    public List<Object[]> getLogRecords(boolean isSharding){
        List<Object[]> resultList = null;
        int shardingFlag = 0;
        if(isSharding){
            //表分片标志 1 标识分库分表 4 标识同步运营平台
            shardingFlag = 1+4;
        }
        String[] result = baseDao.getNativeSQL(QUERY_SYNLOG,0);
        LogUtil.getDefaultLogger().debug("【Debug】LOG SQL：" + result[1]);
        JdbcBaseDao jdbcBaseDao = FacadeProxy.create(JdbcBaseDao.class);
        jdbcBaseDao.setManager(baseDao.getBackupSessionMgr(0,shardingFlag,true));
        resultList = jdbcBaseDao.query(result[1]);
        return resultList;
    }





    public static void main(String[] args) {

    }
}
