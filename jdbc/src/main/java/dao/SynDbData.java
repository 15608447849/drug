package dao;

import constant.DSMConst;
import org.hyrdpf.dao.DAOException;
import org.hyrdpf.dao.FacadeProxy;
import org.hyrdpf.dao.jdbc.AbstractJdbcSessionMgr;
import org.hyrdpf.dao.jdbc.JdbcBaseDao;
import org.hyrdpf.dao.jdbc.JdbcTransaction;
import org.hyrdpf.util.KV;
import org.hyrdpf.util.LogUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName SynDbData
 * @Description TODO
 * @date 2019-03-11 1:44
 */
public class SynDbData implements Callable<Object> {

    private BaseDAO baseDao = BaseDAO.getBaseDAO();

    private int sharding;
    private int tbSharding;
    private String[] nativeSQL;
    private List<Object[]> params;
    private JdbcBaseDao jdbcBaseDao = null;
    private int master;
    private int optType;
    private String[] GPKNativeSQL;
    private Object[] param;
    private String[] resultSQL;
    private int batchSize;



    public SynDbData(String[] resultSQL, Object[] param, int master, int optType){
        this.resultSQL = resultSQL;
        this.param = param;
        this.master = master;
        this.optType = optType;
    }

    public SynDbData(){
    }

    @Override
    public Object call() throws Exception {
        switch (optType){
            case 0:
                return  updateNative();
            case 1:
                return  updateAndGPKNative();
            case 2:
                return  updateTransNative();
            case 3:
                return updateAndGPKTransNative();
            case 4:
                return updateBatchNative();
            case 5:
                return updateNativeBk();
            case 6:
                return updateTransNativeBk();
            case 7:
                return updateBatchNativeBK();
        }
        return null;
    }

    public String[] getResultSQL() {
        return resultSQL;
    }

    public void setResultSQL(String[] resultSQL) {
        this.resultSQL = resultSQL;
    }

    public int getSharding() {
        return sharding;
    }

    public void setSharding(int sharding) {
        this.sharding = sharding;
    }

    public int getTbSharding() {
        return tbSharding;
    }

    public void setTbSharding(int tbSharding) {
        this.tbSharding = tbSharding;
    }

    public String[] getNativeSQL() {
        return nativeSQL;
    }

    public void setNativeSQL(String[] nativeSQL) {
        this.nativeSQL = nativeSQL;
    }

    public List<Object[]> getParams() {
        return params;
    }

    public void setParams(List<Object[]> params) {
        this.params = params;
    }

    public JdbcBaseDao getJdbcBaseDao() {
        return jdbcBaseDao;
    }

    public void setJdbcBaseDao(JdbcBaseDao jdbcBaseDao) {
        this.jdbcBaseDao = jdbcBaseDao;
    }

    public int getMaster() {
        return master;
    }

    public void setMaster(int master) {
        this.master = master;
    }

    public int getOptType() {
        return optType;
    }

    public void setOptType(int optType) {
        this.optType = optType;
    }

    public Object[] getParam() {
        return param;
    }

    public void setParam(Object[] param) {
        this.param = param;
    }

    public String[] getGPKNativeSQL() {
        return GPKNativeSQL;
    }

    public void setGPKNativeSQL(String[] GPKNativeSQL) {
        this.GPKNativeSQL = GPKNativeSQL;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int updateNative(){
        int result = 0;
        LogUtil.getDefaultLogger().debug("【Debug】SYN Native SQL：" + resultSQL[1]);
        JdbcBaseDao jdbcBaseDao = null;
        try {
            jdbcBaseDao = FacadeProxy.create(JdbcBaseDao.class);
            jdbcBaseDao.setManager(baseDao.getBackupSessionMgr(sharding,Integer.parseInt(resultSQL[0]),false));
            result = jdbcBaseDao.update(resultSQL[1], param);
        } catch (DAOException e) {
            e.printStackTrace();
            LogUtil.getDefaultLogger().error("异步线程同步异常",e);
            if(SynDbLog.isBaseSqlError(e.getCause().getCause())){
                return 0;
            }
            int dbs = (master == 0 ? 1 : 0);
            List<Object[]> paramList = new ArrayList<>();
            paramList.add(getParam());
            SynDbLog.updateTransNative(getNativeSQL(),paramList,0,0,dbs,false);
        }
        return result;
    }





    public KV<Integer, List<Object>> updateAndGPKNative(){
        KV<Integer,List<Object>> keys = null;
        LogUtil.getDefaultLogger().debug("【Debug】SYN Native SQL：" + resultSQL[1]);
        JdbcBaseDao jdbcBaseDao = null;
        try{
            jdbcBaseDao = FacadeProxy.create(JdbcBaseDao.class);
            jdbcBaseDao.setManager(baseDao.getBackupSessionMgr(sharding,Integer.parseInt(resultSQL[0]),false));
            keys = jdbcBaseDao.updateAndGenerateKeys(resultSQL[1], param);
        }catch (DAOException e) {
            LogUtil.getDefaultLogger().debug("异步线程写入数据异常！");
            LogUtil.getDefaultLogger().error("异步线程同步异常",e);

            e.printStackTrace();
            if(SynDbLog.isBaseSqlError(e.getCause().getCause())){
                return null;
            }
            int dbs = (master == 0 ? 1 : 0);
            List<Object[]> paramList = new ArrayList<>();
            paramList.add(getParam());
            SynDbLog.updateTransNative(getNativeSQL(),paramList,getSharding(),getTbSharding(),dbs,false);
        }
        return keys;
    }




    public int[] updateTransNative(){

        int[] result = new int[nativeSQL.length];
        AbstractJdbcSessionMgr sessionMgr = baseDao.getBackupSessionMgr(sharding,Integer.parseInt(resultSQL[0]),false);
        try {
            FacadeProxy.executeCustomTransaction(sessionMgr,new JdbcTransaction() {
                @Override
                public void execute(AbstractJdbcSessionMgr sessionMgr) throws DAOException {
                    for (int i = 0; i < nativeSQL.length; i++) {
                        if(sharding != 0){
                            result[i] = baseDao.updateNativeInCallSharding(sharding,tbSharding,nativeSQL[i],1,params.get(i));
                        }else{
                            result[i] = baseDao.updateNativeInCall(nativeSQL[i],1,params.get(i));
                        }
                    }
                }
            });
        } catch (DAOException e) {
            LogUtil.getDefaultLogger().debug("异步线程写入数据异常！");
            LogUtil.getDefaultLogger().error("异步线程同步异常",e);
            e.printStackTrace();
            if(SynDbLog.isBaseSqlError(e.getCause().getCause())){
                return null;
            }
            int dbs = (master == 0 ? 1 : 0);
            SynDbLog.updateTransNative(getNativeSQL(),getParams(),getSharding(),getTbSharding(),dbs,false);
        }
        return result;
    }



    public int[] updateTransNativeBk(){
        int[] result = new int[nativeSQL.length];
        AbstractJdbcSessionMgr sessionMgr = baseDao.getSessionMgr(0,
                DSMConst.TD_BK_TRAN_ORDER);
        List<Object[]> bkParm = new ArrayList<>();
        List<String[]> bkResultSql = new ArrayList<>();
        for (int i = 0; i < nativeSQL.length; i++){
            String[] resultSQL = baseDao.getNativeReplaceSQL(nativeSQL[i],tbSharding);
            int tabInx = Integer.parseInt(resultSQL[0]);
            if(tabInx == DSMConst.TD_TRAN_ORDER || tabInx == DSMConst.TD_TRAN_GOODS){
                bkResultSql.add(resultSQL);
                bkParm.add(params.get(i));
            }
        }
        try {
            FacadeProxy.executeCustomTransaction(sessionMgr,new JdbcTransaction() {
                @Override
                public void execute(AbstractJdbcSessionMgr sessionMgr) throws DAOException {
                    for (int i = 0; i < bkResultSql.size(); i++) {
                        result[i] = baseDao.updateNativeInCallBKSharding(sharding,tbSharding,bkResultSql.get(i),bkParm.get(i));
                    }
                }
            });
        } catch (DAOException e) {
            LogUtil.getDefaultLogger().debug("异步线程写入数据异常！");
            LogUtil.getDefaultLogger().error("异步线程同步异常",e);
            e.printStackTrace();
            int dbs = master;
            SynDbLog.updateTransNative(getNativeSQL(),getParams(),
                    getSharding(),getTbSharding(),dbs,false);
        }
        return result;
    }


    public int updateNativeBk(){
        int result = 0;
        String[] resultSQL = baseDao.getNativeReplaceSQL(nativeSQL[0],tbSharding);
        LogUtil.getDefaultLogger().debug("【Debug】SYN Native SQL：" + resultSQL[1]);
        JdbcBaseDao jdbcBaseDao = null;
        try {
            jdbcBaseDao = FacadeProxy.create(JdbcBaseDao.class);
            jdbcBaseDao.setManager(baseDao.getSessionMgr(0,DSMConst.TD_BK_TRAN_ORDER));
            result = jdbcBaseDao.update(resultSQL[1], param);
        } catch (DAOException e) {
            LogUtil.getDefaultLogger().error("异步线程同步运营平台异常",e);
            e.printStackTrace();
            int dbs = master;
            List<Object[]> paramList = new ArrayList<>();
            paramList.add(getParam());
            SynDbLog.updateTransNative(getNativeSQL(),paramList,0,0,dbs,false);
        }
        return result;
    }


    public int[] updateBatchNativeBK(){
        String[] resultSQL = baseDao.getNativeReplaceSQL(nativeSQL[0],tbSharding);
        LogUtil.getDefaultLogger().debug("【Debug】SYN Native SQL：" + resultSQL[1]);
        int[] result = null;
        JdbcBaseDao jdbcBaseDao = null;
        try {
            jdbcBaseDao = FacadeProxy.create(JdbcBaseDao.class);
            jdbcBaseDao.setManager(baseDao.getSessionMgr(0,DSMConst.TD_BK_TRAN_ORDER));
            result = jdbcBaseDao.updateBatch(resultSQL[1], params,batchSize);
        } catch (DAOException e) {
            LogUtil.getDefaultLogger().error("异步线程同步运营平台异常",e);
            e.printStackTrace();
            int dbs = master;
            List<Object[]> paramList = new ArrayList<>();
            paramList.add(getParam());
            SynDbLog.updateTransNative(getNativeSQL(),paramList,0,0,dbs,true);
        }
        return result;
    }



    public  int[] updateAndGPKTransNative(){
        int[] result = new int[GPKNativeSQL.length + nativeSQL.length];
        AbstractJdbcSessionMgr sessionMgr = baseDao.getBackupSessionMgr(sharding,Integer.parseInt(resultSQL[0]),false);
        try {
            FacadeProxy.executeCustomTransaction(sessionMgr,new JdbcTransaction() {
                @Override
                public void execute(AbstractJdbcSessionMgr sessionMgr) throws DAOException {
                    List<Object[]> autoGPK = new ArrayList<Object[]>();
                    for (int i = 0; i < GPKNativeSQL.length; i++) {
                        KV<Integer,List<Object>> keys = null;
                        if(sharding != 0){
                            keys = baseDao.updateAndGPKNativeInCallSharding(sharding,tbSharding,GPKNativeSQL[i],1,params.get(i));
                        }else{
                            keys = baseDao.updateAndGPKNativeInCall(GPKNativeSQL[i],1,params.get(i));
                        }
                        //返回类型为[4,5],代表插入了两行，产生了两个自增长值，分别为4和5
                        autoGPK.add(keys.getValue().toArray());
                        //自增值个数，也就是行数（同时插入多行记录）
                        result[i] = keys.getKey();
                    }
                    Object[] paramsTrue;
                    for (int i = 0 ; i < nativeSQL.length; i++) {
                        paramsTrue = baseDao.getTrueParams(autoGPK,params.get(i + GPKNativeSQL.length));
                        if(sharding != 0){
                            result[i + GPKNativeSQL.length] = baseDao.updateNativeInCallSharding(sharding,tbSharding,nativeSQL[i],1,paramsTrue);
                        }else{
                            result[i + GPKNativeSQL.length] = baseDao.updateNativeInCall(nativeSQL[i],1,paramsTrue);
                        }

                    }
                }
            });
        } catch (DAOException e) {
            LogUtil.getDefaultLogger().debug("异步线程写入数据异常！");
            LogUtil.getDefaultLogger().error("异步线程同步异常",e);
            e.printStackTrace();
            if(SynDbLog.isBaseSqlError(e.getCause().getCause())){
                return null;
            }
            int dbs = (master == 0 ? 1 : 0);
            SynDbLog.updateTransNative(getNativeSQL(),getParams(),getSharding(),getTbSharding(),dbs,false);
        }
        return result;
    }


    public int[] updateBatchNative(){
        LogUtil.getDefaultLogger().debug("【Debug】SYN Native SQL：" + resultSQL[1]);
        int[] result = null;
        JdbcBaseDao jdbcBaseDao = null;
        try {
            jdbcBaseDao = FacadeProxy.create(JdbcBaseDao.class);
            jdbcBaseDao.setManager(baseDao.getBackupSessionMgr(sharding,Integer.parseInt(resultSQL[0]),false));
            result = jdbcBaseDao.updateBatch(resultSQL[1], params,batchSize);
        } catch (DAOException e) {
            LogUtil.getDefaultLogger().debug("异步线程写入数据异常！");
            LogUtil.getDefaultLogger().error("异步线程同步异常",e);
            e.printStackTrace();
            if(SynDbLog.isBaseSqlError(e.getCause().getCause())){
                return null;
            }
            int dbs = (master == 0 ? 1 : 0);
            SynDbLog.updateTransNative(getNativeSQL(),getParams(),getSharding(),getTbSharding(),dbs,true);
        }
        return result;
    }


}
