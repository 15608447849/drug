package dao;

import IceInternal.Ex;
import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import constant.BUSConst;
import constant.DSMConst;
import global.GlobalTransCondition;
import global.LoadDbConfig;
import global.Placeholder;
import org.hyrdpf.dao.DAOException;
import org.hyrdpf.dao.FacadeProxy;
import org.hyrdpf.dao.Transaction;
import org.hyrdpf.dao.jdbc.AbstractJdbcSessionMgr;
import org.hyrdpf.dao.jdbc.JdbcBaseDao;
import org.hyrdpf.dao.jdbc.JdbcTransaction;
import org.hyrdpf.ds.AppConfig;
import org.hyrdpf.util.KV;
import org.hyrdpf.util.LogUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Copyright © 2018空间折叠【FOLDING SPACE】. All rights reserved.
 * @ClassName: BaseService
 * @Description: TODO 业务服务DAO基类，主要实现同数据库操作、切分服务器、库、表的封装
 * @version: V1.0
 */
public class BaseDAO {
	/**原生态SQL查询时，指定的查询表对象固定格式的前缀字符串。*/
	public static final String PREFIX_REGEX = "{{?";
	/**原生态SQL查询时，指定的查询表对象固定格式的后缀字符串。*/
	public static final String SUFFIX_REGEX = "}}";
	/**原生态SQL查询时，指定的查询表对象固定格式的前缀字符串与后缀字符串，对应的正则表达式的字符串*/
	private static final StringBuffer PREFIX_REGEX_SB = new StringBuffer("\\{\\{\\?");
	private static final StringBuffer SUFFIX_REGEX_SB = new StringBuffer("\\}\\}");

	private static final ExecutorService ASYN_THREAD = Executors.newCachedThreadPool();


	public static volatile AtomicInteger master = new AtomicInteger(LoadDbConfig.getDbConfigValue("master"));

	private static final int SHARDING_FLAG = LoadDbConfig.getDbConfigValue("sharding");

	private static final BaseDAO BASEDAO = new BaseDAO();
	/**私有化其构造函数，防止随便乱创建此对象。*/
	private BaseDAO(){}

	public static BaseDAO getBaseDAO(){
		return BASEDAO;
	}

	/**多数据源实现,切分服务器与库实现，输入参数：table：是要操作那个基本表，基本表就是没有切分前的表*/
	private AbstractJdbcSessionMgr getSessionMgr(final int table){
		/**公共库只有一个连接,是不需要切分服务器与库及表的。公共库服务器索引是所有数据源的最后一个，库的索引值固定为0*/
		int dbs = master.get();
		int db = 0;

		if(SHARDING_FLAG == 1){
			db = BUSConst._MODNUM_EIGHT;
		}

		LogUtil.getDefaultLogger().debug("dbs="+dbs);
		/**返回对应的数据库连接池供用户使用*/
		return AppConfig.getSessionManager(dbs,db);
	}

	public AbstractJdbcSessionMgr getMySessionByTable(final int table){
		return getSessionMgr(table);
	}

	/**多数据源实现,切分服务器与库实现，输入参数：table：是要操作那个基本表，基本表就是没有切分前的表*/
	private AbstractJdbcSessionMgr getSessionMgr(int sharding, final int table){
		/**公共库只有一个连接,是不需要切分服务器与库及表的。公共库服务器索引是所有数据源的最后一个，库的索引值固定为0*/
		int dbs = master.get();
		int db = 0;
		// 按公司模型切分表
		if ((DSMConst.SEG_TABLE_RULE[table] & 1) > 0) {
			/** 取得数据库服务器的编号 */
			//dbs = sharding / BUSConst._DMNUM  % BUSConst._SMALLINTMAX;
			/** 取得数据库服务器上数据库的编号 */
			db = sharding % BUSConst._DMNUM  % BUSConst._MODNUM_EIGHT;
		}

		if(DSMConst.SEG_TABLE_RULE[table] == 0 && SHARDING_FLAG == 1){
			db = BUSConst._MODNUM_EIGHT;
		}

        if((DSMConst.SEG_TABLE_RULE[table] & 4) > 0){
            dbs = AppConfig.getDBSNum() - BUSConst._ONE;
            db = 0;
        }


		/**返回对应的数据库连接池供用户使用*/
		LogUtil.getDefaultLogger().debug("分片字段："+sharding);
		LogUtil.getDefaultLogger().debug("服务器编号："+dbs);
		LogUtil.getDefaultLogger().debug("数据库编号："+db);
		return AppConfig.getSessionManager(dbs,db);
	}

	/**多数据源实现,切分服务器与库实现，输入参数：table：是要操作那个基本表，基本表就是没有切分前的表*/
	protected AbstractJdbcSessionMgr getBackupSessionMgr(int sharding, final int table,boolean isLog){
		/**公共库只有一个连接,是不需要切分服务器与库及表的。公共库服务器索引是所有数据源的最后一个，库的索引值固定为0*/
		int dbs = master.get() == 0 ? 1 : 0;
        int db = 0;

		// 按公司模型切分表
		if ((DSMConst.SEG_TABLE_RULE[table] & 1) > 0) {
			/** 取得数据库服务器的编号 */
			//dbs = sharding / BUSConst._DMNUM  % BUSConst._SMALLINTMAX;
			/** 取得数据库服务器上数据库的编号 */
			db = sharding % BUSConst._DMNUM  % BUSConst._MODNUM_EIGHT;
			if(isLog){
				db = BUSConst._MODNUM_EIGHT -1;
			}
		}

		if(SHARDING_FLAG == 1 && (DSMConst.SEG_TABLE_RULE[table] & 1) == 0){
			db = BUSConst._MODNUM_EIGHT;
		}

        if((DSMConst.SEG_TABLE_RULE[table] & 4) > 0){
            dbs = AppConfig.getDBSNum() - BUSConst._ONE;
            db = 0;
            if(isLog){
                dbs = master.get();
                db = BUSConst._MODNUM_EIGHT -1;
            }
        }

		/**返回对应的数据库连接池供用户使用*/
		LogUtil.getDefaultLogger().debug("分片字段："+sharding);
		LogUtil.getDefaultLogger().debug("服务器编号："+dbs);
		LogUtil.getDefaultLogger().debug("数据库编号："+db);
		return AppConfig.getSessionManager(dbs,db);
	}


	/**多数据源实现,切分服务器与库实现，输入参数：table：是要操作那个基本表，基本表就是没有切分前的表*/
	public boolean isSameDb(int compid,int tcompid) {
		int dbs = compid / BUSConst._DMNUM % BUSConst._SMALLINTMAX;
		int db = compid % BUSConst._DMNUM % BUSConst._MODNUM_EIGHT;

		int tdbs = tcompid / BUSConst._DMNUM % BUSConst._SMALLINTMAX;
		int tdb = tcompid % BUSConst._DMNUM % BUSConst._MODNUM_EIGHT;

		if(dbs == tdbs && db == tdb){
			return true;
		}
		return false;
	}

	/**切分表实现,table：是要操作那个基本表，基本表就是没有切分前的表*/
	private String getTableName(final int table)
	{
		StringBuilder strSql = new StringBuilder(DSMConst.DB_TABLES[table][BUSConst._ZERO]);
		LogUtil.getDefaultLogger().debug("表名："+strSql.toString());
		return strSql.toString();
	}

	/**切分表实现,table：是要操作那个基本表，基本表就是没有切分前的表*/
	private String getTableName(final int table,int tbSharding)
	{
		StringBuilder strSql = new StringBuilder(DSMConst.DB_TABLES[table][BUSConst._ZERO]);
		//按公司模型切分表
		if((DSMConst.SEG_TABLE_RULE[table] & 1) > 0){
			strSql.append(DSMConst._UNDERLINE);
			if(tbSharding == 0){
				Calendar date = Calendar.getInstance();
				strSql.append(date.get(Calendar.YEAR));
			}else{
				strSql.append(tbSharding);
			}
		}
		LogUtil.getDefaultLogger().debug("表名："+strSql.toString());
		return strSql.toString();
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
        LogUtil.getDefaultLogger().debug("表名："+strSql.toString());
        return strSql.toString();
    }

	/**
	* @version 版本：1.0
	* @parameter  输入参数：带固定格式"{{?d}}"的原生态查询SQL语句。
	* @return  返回值：索引为0的返回值代表要使用的数据源id，索引为1的返回值代表是原生态查询SQL语句字符串，
	 */
	private String[] getNativeSQL(String nativeSQL){
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
			nativeSQL = nativeSQL.replaceAll(regex,getTableName(tableIndex));
		}
		result[1] = nativeSQL;
		return result;
	}


	/**
	 * @version 版本：1.0
	 * @parameter  输入参数：带固定格式"{{?d}}"的原生态查询SQL语句。
	 * @return  返回值：索引为0的返回值代表要使用的数据源id，索引为1的返回值代表是原生态查询SQL语句字符串，
	 */
	protected String[] getNativeSQL(String nativeSQL,int tbSharding){
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
			nativeSQL = nativeSQL.replaceAll(regex.toString(),getTableName(tableIndex,tbSharding));
		}
		result[1] = nativeSQL;
		return result;
	}


    /**
     * @version 版本：1.0
     * @parameter  输入参数：带固定格式"{{?d}}"的原生态查询SQL语句。
     * @return  返回值：索引为0的返回值代表要使用的数据源id，索引为1的返回值代表是原生态查询SQL语句字符串，
     */
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

	/**
	* @version 版本：1.0
	* @param autoGPK 同一事务中刚新增记录的自增长字段的值集合。
	* @param params　同一事务中需要新增记录，同时该记录中需要插入上一个刚插入的自增长字段的值的表需要插入的记录值参数对象数组。
	* @return　用同一事务中刚插入的记录的自增字段值取代了占位符类的值的参数对象数组。
	* {@code} 用自增长字段的值替代占位符，形成最终需要保存的参数对象数组。
	 */
	public Object[] getTrueParams(List<Object[]> autoGPK,Object[] params){
		for(int i = 0 ; i < params.length ; i++){
			if(params[i] instanceof Placeholder){
				Placeholder placeholder = (Placeholder)params[i];
				params[i] = autoGPK.get(placeholder.getListIndex())[placeholder.getObjectIndex()];
			}
		}
		return params;
	}
	/**
	* @version 版本：1.0
	* @param nativeSQL：原生态SQL查询语句；但是所有表名用“{{?^\\d+$}}”,如：{{?0}}代替，"?"后面的数字，请同DSMConst中DB_TABLES的索引一一对应。
	* @param params：原生态SQL查询语句中“?”的参数值；
	* @return：查询结果列表；
	* 查询方法。特别注意：多表查询时，请确保多表都是需要切分或都不要切分。也就是说，基本数据库里表，请从内存里拿，不要通过SQL关联来查询，
	* 因为基本数据表同业务切分表是在不同的服务器上。
	 */
	@Transaction(false)
	public List<Object[]> queryNative(String nativeSQL,final Object... params){
		List<Object[]> resultList;
		String[] result = getNativeSQL(nativeSQL);
		LogUtil.getDefaultLogger().debug("【Debug】Native SQL：" + result[1]+"\n"+Arrays.toString(params));
		JdbcBaseDao baseDao = FacadeProxy.create(JdbcBaseDao.class);
		baseDao.setManager(getSessionMgr(Integer.parseInt(result[0])));
		resultList = baseDao.query(result[1], params);
		return resultList;
	}


	/**
	 * @version 版本：1.0
	 * @param sharding ：分片字段
	 * @param tbSharding : 表分片字段
	 * @param nativeSQL：原生态SQL查询语句；但是所有表名用“{{?^\\d+$}}”,如：{{?0}}代替，"?"后面的数字，请同DSMConst中DB_TABLES的索引一一对应。
	 * @param params：原生态SQL查询语句中“?”的参数值；
	 * @return：查询结果列表；
	 * 查询方法。特别注意：多表查询时，请确保多表都是需要切分或都不要切分。也就是说，基本数据库里表，请从内存里拿，不要通过SQL关联来查询，
	 * 因为基本数据表同业务切分表是在不同的服务器上。
	 */
	@Transaction(false)
	public List<Object[]> queryNativeSharding(int sharding,int tbSharding,String nativeSQL,final Object... params){
		List<Object[]> resultList = null;
		String[] result = getNativeSQL(nativeSQL,tbSharding);
		LogUtil.getDefaultLogger().debug("【Debug】Native SQL：" + result[1]);
		JdbcBaseDao baseDao = FacadeProxy.create(JdbcBaseDao.class);
		baseDao.setManager(getSessionMgr(sharding,Integer.parseInt(result[0])));
		resultList = baseDao.query(result[1], params);
		return resultList;
	}

	/**
	 * @version 版本：1.0
	 * 全局查询
	 * @param tbSharding : 表分片字段
	 * @param dbs 服务器坐标
	 * @param db 数据库坐标
	 * @param nativeSQL：原生态SQL查询语句；但是所有表名用“{{?^\\d+$}}”,如：{{?0}}代替，"?"后面的数字，请同DSMConst中DB_TABLES的索引一一对应。
	 * @param params：原生态SQL查询语句中“?”的参数值；
	 * @return：查询结果列表；
	 * 查询方法。特别注意：多表查询时，请确保多表都是需要切分或都不要切分。也就是说，基本数据库里表，请从内存里拿，不要通过SQL关联来查询，
	 * 因为基本数据表同业务切分表是在不同的服务器上。
	 */
	@Transaction(false)
	public List<Object[]> queryNativeGlobal(int tbSharding,int dbs,int db,String nativeSQL,final Object... params){
		List<Object[]> resultList = null;
		String[] result = getNativeSQL(nativeSQL,tbSharding);
		LogUtil.getDefaultLogger().debug("【Debug】Native SQL：" + result[1]+"\n"+Arrays.toString(params));
		JdbcBaseDao baseDao = FacadeProxy.create(JdbcBaseDao.class);
		baseDao.setManager(AppConfig.getSessionManager(dbs,db));
		resultList = baseDao.query(result[1], params);
		return resultList;
	}

	/**
	 * 全局查询汇总
	 * @param tbSharding
	 * @param nativeSQL
	 * @param params
	 * @return
	 */
	public List<Object[]> queryGlobalCollect(int tbSharding,String nativeSQL,final Object... params){
		int dbs = AppConfig.getDBSNum() - BUSConst._ONE;
		ExecutorService executor= Executors.newFixedThreadPool(dbs);
		List<Object[]> queryResultList = null;
		List<GlobalQuery> queryThreads = new ArrayList<>();
		for (int i = 0; i < dbs; i++){
			queryThreads.add(new GlobalQuery(i,tbSharding,nativeSQL,params));
		}
        //接受处理返回的结果集
		List<Future<List<Object[]>>> results = null;
		//执行线程处理
		try {
			results = executor.invokeAll(queryThreads);
			queryResultList = new ArrayList<Object[]>();
			for(Future<List<Object[]>> future : results){
				queryResultList.addAll((List<Object[]>)future.get());
			}
		} catch (Exception e) {
			LogUtil.getDefaultLogger().error(getClass().getSimpleName(),e);
		}
		return queryResultList;
	}


	/**
	* @version 版本：1.0
	* @param nativeSQL　原生态SQL查询语句；但是所有表名用“{{?^\\d+$}}”,如：{{?0}}代替，"?"后面的数字，请同DSMConst中DB_TABLES的索引一一对应。
	* @param params　原生态SQL查询语句中“?”的参数值；
	* @return　成功更新的记录数
	* 单表新增、修改、删除方法。
	 */
	public int updateNative(String nativeSQL,final Object... params){
		int result = 0;
		String[] resultSQL = getNativeSQL(nativeSQL);
		LogUtil.getDefaultLogger().debug("【Debug】Native SQL：" + resultSQL[1]+"\n"+Arrays.toString(params));
		//异步同步到备份库中
		Future<Object> future = null;
		if(SynDbLog.isSynBackDB(Integer.parseInt(resultSQL[0]))){
			SynDbData synDbData = new SynDbData(resultSQL,params,master.get(),0);
			synDbData.setSharding(0);
			synDbData.setNativeSQL(new String[]{nativeSQL});
			future = ASYN_THREAD.submit(synDbData);
		}
		JdbcBaseDao baseDao = null;
		try{
			baseDao = FacadeProxy.create(JdbcBaseDao.class);
			baseDao.setManager(getSessionMgr(Integer.parseInt(resultSQL[0])));
			result = baseDao.update(resultSQL[1], params);
		}catch (DAOException e) {
			LogUtil.getDefaultLogger().error(getClass().getSimpleName(),e);
			e.printStackTrace();
			if(SynDbLog.isBaseSqlError(e.getCause().getCause())
					|| !SynDbLog.isSynBackDB(Integer.parseInt(resultSQL[0]))){
				return 0;
			}
			//写入日志
			List<Object[]> paramList = new ArrayList<>();
			paramList.add(params);
			Object swRet = switchDB(future,new String[]{nativeSQL},
					paramList,0,0,master.get(),false);
			if(swRet != null){
				return (int)swRet;
			}
		}
		return result;
	}


	/**
	 * @version 版本：1.0
	 * @param nativeSQL　原生态SQL查询语句；但是所有表名用“{{?^\\d+$}}”,如：{{?0}}代替，"?"后面的数字，请同DSMConst中DB_TABLES的索引一一对应。
	 * @param params　原生态SQL查询语句中“?”的参数值；
	 * @return　成功更新的记录数
	 * 单表新增、修改、删除方法。
	 */
	protected int updateNativeInCall(String nativeSQL,int synFlag,final Object... params){
		String[] resultSQL = getNativeSQL(nativeSQL);
		LogUtil.getDefaultLogger().debug("【Debug】Native SQL：" + resultSQL[1]+"\n"+Arrays.toString(params));
		JdbcBaseDao baseDao = FacadeProxy.create(JdbcBaseDao.class);
		if(synFlag == 0){
			baseDao.setManager(getSessionMgr(Integer.parseInt(resultSQL[0])));
			return baseDao.update(resultSQL[1], params);
		}
		if(synFlag == 1 && SynDbLog.isSynBackDB(Integer.parseInt(resultSQL[0]))){
			baseDao.setManager(getBackupSessionMgr(0,Integer.parseInt(resultSQL[0]),false));
			return baseDao.update(resultSQL[1], params);
		}
        if(synFlag == 2){
            LogUtil.getDefaultLogger().debug("【Debug】Native LOG SQL：" + resultSQL[1]);
            baseDao.setManager(getBackupSessionMgr(0,Integer.parseInt(resultSQL[0]),true));
            return baseDao.update(resultSQL[1], params);
        }
		return 0;
	}


	/**
	 * @version 版本：1.0
	 * @param sharding 分片字段
	 * @param tbSharding 查询年份
	 * @param nativeSQL　原生态SQL查询语句；但是所有表名用“{{?^\\d+$}}”,如：{{?0}}代替，"?"后面的数字，请同DSMConst中DB_TABLES的索引一一对应。
	 * @param params　原生态SQL查询语句中“?”的参数值；
	 * @return　成功更新的记录数
	 * 单表新增、修改、删除方法。
	 */
	public int updateNativeSharding(int sharding,int tbSharding,String nativeSQL,final Object... params){
		int result = 0;
		String[] resultSQL = getNativeSQL(nativeSQL,tbSharding);
		LogUtil.getDefaultLogger().debug("【Debug】Native SQL：" + resultSQL[1]);

		//异步同步到备份库
		Future<Object> future = null;
        SynDbData synDbData = null;
		if(SynDbLog.isSynBackDB(Integer.parseInt(resultSQL[0]))) {
            synDbData = new SynDbData(resultSQL, params, master.get(), 0);
			synDbData.setSharding(sharding);
			synDbData.setTbSharding(tbSharding);
			synDbData.setNativeSQL(new String[]{nativeSQL});
			future = ASYN_THREAD.submit(synDbData);
		}

        //异步同步到订单运营后台
        if(Integer.parseInt(resultSQL[0]) == DSMConst.TD_TRAN_ORDER
                || Integer.parseInt(resultSQL[0]) == DSMConst.TD_TRAN_GOODS){
            synDbData = new SynDbData(resultSQL, params, master.get(), 6);
            synDbData.setSharding(sharding);
            synDbData.setTbSharding(tbSharding);
            synDbData.setNativeSQL(new String[]{nativeSQL});
            ASYN_THREAD.submit(synDbData);
        }

		JdbcBaseDao baseDao = null;
		try {
			baseDao = FacadeProxy.create(JdbcBaseDao.class);
			baseDao.setManager(getSessionMgr(sharding,Integer.parseInt(resultSQL[0])));
			result = baseDao.update(resultSQL[1], params);
		} catch (DAOException e) {
			LogUtil.getDefaultLogger().error(getClass().getSimpleName(),e);
			e.printStackTrace();
			if(SynDbLog.isBaseSqlError(e.getCause().getCause()) ||
			!SynDbLog.isSynBackDB(Integer.parseInt(resultSQL[0]))){
				return 0;
			}
			//写入日志
			List<Object[]> paramList = new ArrayList<>();
			paramList.add(params);
			Object swRet = switchDB(future,new String[]{nativeSQL},
					paramList,sharding,tbSharding,master.get(),false);
			if(swRet != null){
				return (int)swRet;
			}
		}
		return result;
	}


	/**
	 * @version 版本：1.0
	 * @param sharding 分片字段
	 * @param tbSharding 查询年份
	 * @param nativeSQL　原生态SQL查询语句；但是所有表名用“{{?^\\d+$}}”,如：{{?0}}代替，"?"后面的数字，请同DSMConst中DB_TABLES的索引一一对应。
	 * @param params　原生态SQL查询语句中“?”的参数值；
	 * @return　成功更新的记录数
	 * 单表新增、修改、删除方法。
	 */
	protected int updateNativeInCallSharding(int sharding,int tbSharding,String nativeSQL,int synFlag,final Object... params){
		String[] resultSQL = getNativeSQL(nativeSQL,tbSharding);

		JdbcBaseDao baseDao = FacadeProxy.create(JdbcBaseDao.class);
		if(synFlag == 0){
			LogUtil.getDefaultLogger().debug("【Debug】Native SQL：" + resultSQL[1]);
			baseDao.setManager(getSessionMgr(sharding,Integer.parseInt(resultSQL[0])));
			return baseDao.update(resultSQL[1], params);
		}
		if(synFlag == 1 && SynDbLog.isSynBackDB(Integer.parseInt(resultSQL[0]))){
			LogUtil.getDefaultLogger().debug("【Debug】Native SYN SQL：" + resultSQL[1]);
			baseDao.setManager(getBackupSessionMgr(sharding,Integer.parseInt(resultSQL[0]),false));
			return baseDao.update(resultSQL[1], params);
		}
		if(synFlag == 2){
			LogUtil.getDefaultLogger().debug("【Debug】Native LOG SQL：" + resultSQL[1]);
			baseDao.setManager(getBackupSessionMgr(sharding,Integer.parseInt(resultSQL[0]),true));
			return baseDao.update(resultSQL[1], params);
		}
		return 0;
	}

    protected int updateNativeInCallBKSharding(int sharding,int tbSharding,String[] resultSQL,
                                               final Object... params){
        JdbcBaseDao baseDao = FacadeProxy.create(JdbcBaseDao.class);
        LogUtil.getDefaultLogger().debug("【Debug】Native BKSYN SQL：" + resultSQL[1]);
        baseDao.setManager(getSessionMgr(sharding,DSMConst.TD_BK_TRAN_ORDER));
        return baseDao.update(resultSQL[1], params);
    }



	/**
	 * @version 版本：1.0
	 * @param nativeSQL 原生态SQL查询语句；但是所有表名用“{{?^\\d+$}}”,如：{{?0}}代替，"?"后面的数字，请同DSMConst中DB_TABLES的索引一一对应。
	 * @param params 原生态SQL查询语句中“?”的参数值；
	 * @return Key成功更新的记录数；Value自增的字段值集合。
	 * 单表新增并返回自增主键键。
	 */
	public KV<Integer, List<Object>> updateAndGPKNative(String nativeSQL, final Object... params){
		KV<Integer,List<Object>> keys = null;
		String[] resultSQL = getNativeSQL(nativeSQL);
		LogUtil.getDefaultLogger().debug("【Debug】Native SQL：" + resultSQL[1]+"\n"+Arrays.toString(params));
		//异步同步到备份库
		Future<Object> future = null;
		if(SynDbLog.isSynBackDB(Integer.parseInt(resultSQL[0]))){
			SynDbData synDbData = new SynDbData(resultSQL,params,master.get(),2);
			synDbData.setNativeSQL(new String[]{nativeSQL});
			synDbData.setSharding(0);
			future = ASYN_THREAD.submit(synDbData);
		}

		JdbcBaseDao baseDao;
		try{
			baseDao = FacadeProxy.create(JdbcBaseDao.class);
			baseDao.setManager(getSessionMgr(Integer.parseInt(resultSQL[0])));
			keys = baseDao.updateAndGenerateKeys(resultSQL[1], params);
		}catch (DAOException e) {
			LogUtil.getDefaultLogger().error(getClass().getSimpleName(),e);
			e.printStackTrace();

			if(SynDbLog.isBaseSqlError(e.getCause().getCause()) ||
					!SynDbLog.isSynBackDB(Integer.parseInt(resultSQL[0]))){
				return null;
			}
			//写入日志
			List<Object[]> paramList = new ArrayList<>();
			paramList.add(params);
			Object swRet = switchDB(future,new String[]{nativeSQL},
					paramList,0,0,master.get(),false);
			if(swRet != null){
				return (KV<Integer, List<Object>>)swRet;
			}
		}
		return keys;
	}



	/**
	* @version 版本：1.0
	 * @param sharding 分片字段
	* @param nativeSQL 原生态SQL查询语句；但是所有表名用“{{?^\\d+$}}”,如：{{?0}}代替，"?"后面的数字，请同DSMConst中DB_TABLES的索引一一对应。
	* @param params 原生态SQL查询语句中“?”的参数值；
	* @return Key成功更新的记录数；Value自增的字段值集合。
	* 单表新增并返回自增主键键。
	 */
	public KV<Integer, List<Object>> updateAndGPKNativeSharding(int sharding, int tbSharding, String nativeSQL, final Object... params){
		KV<Integer,List<Object>> keys = null;
		String[] resultSQL = getNativeSQL(nativeSQL,tbSharding);
		LogUtil.getDefaultLogger().debug("【Debug】Native SQL：" + resultSQL[1]);

		//异步同步到备份库
		Future<Object> future = null;
		if(SynDbLog.isSynBackDB(Integer.parseInt(resultSQL[0]))){
			SynDbData synDbData = new SynDbData(resultSQL,params,master.get(),2);
			synDbData.setTbSharding(tbSharding);
			synDbData.setSharding(sharding);
			synDbData.setNativeSQL(new String[]{nativeSQL});
			future = ASYN_THREAD.submit(synDbData);
		}


		JdbcBaseDao baseDao = null;
		try{
			baseDao = FacadeProxy.create(JdbcBaseDao.class);
			baseDao.setManager(getSessionMgr(sharding,Integer.parseInt(resultSQL[0])));
			keys = baseDao.updateAndGenerateKeys(resultSQL[1], params);
		}catch (DAOException e) {
			LogUtil.getDefaultLogger().error(getClass().getSimpleName(),e);
			e.printStackTrace();
			if(SynDbLog.isBaseSqlError(e.getCause().getCause()) ||
					!SynDbLog.isSynBackDB(Integer.parseInt(resultSQL[0]))){
				return null;
			}
			//写入日志
			List<Object[]> paramList = new ArrayList<>();
			paramList.add(params);
			Object swRet = switchDB(future,new String[]{nativeSQL},
					paramList,sharding,tbSharding,master.get(),false);
			if(swRet != null){
				return (KV<Integer, List<Object>>)swRet;
			}
		}
		return keys;
	}


	/**
	 * @version 版本：1.0
	 * @param nativeSQL 原生态SQL查询语句；但是所有表名用“{{?^\\d+$}}”,如：{{?0}}代替，"?"后面的数字，请同DSMConst中DB_TABLES的索引一一对应。
	 * @param params 原生态SQL查询语句中“?”的参数值；
	 * @return Key成功更新的记录数；Value自增的字段值集合。
	 * 单表新增并返回自增主键键。
	 */
	protected KV<Integer, List<Object>> updateAndGPKNativeInCall(String nativeSQL,int synFlag,final Object... params){
		String[] resultSQL = getNativeSQL(nativeSQL);
		LogUtil.getDefaultLogger().debug("【Debug】Native SQL：" + resultSQL[1]+"\n"+Arrays.toString(params));
		JdbcBaseDao baseDao = FacadeProxy.create(JdbcBaseDao.class);
		if(synFlag == 0){
			LogUtil.getDefaultLogger().debug("【Debug】Native SQL：" + resultSQL[1]);
			baseDao.setManager(getSessionMgr(Integer.parseInt(resultSQL[0])));
			return baseDao.updateAndGenerateKeys(resultSQL[1], params);
		}
		if(synFlag == 1 && SynDbLog.isSynBackDB(Integer.parseInt(resultSQL[0]))){
			LogUtil.getDefaultLogger().debug("【Debug】Native SYN SQL：" + resultSQL[1]);
			baseDao.setManager(getBackupSessionMgr(0,Integer.parseInt(resultSQL[0]),false));
			return baseDao.updateAndGenerateKeys(resultSQL[1], params);
		}
		return null;
	}


	/**
	 * @version 版本：1.0
	 * @param sharding 分片字段
	 * @param nativeSQL 原生态SQL查询语句；但是所有表名用“{{?^\\d+$}}”,如：{{?0}}代替，"?"后面的数字，请同DSMConst中DB_TABLES的索引一一对应。
	 * @param params 原生态SQL查询语句中“?”的参数值；
	 * @return Key成功更新的记录数；Value自增的字段值集合。
	 * 单表新增并返回自增主键键。
	 */
	protected KV<Integer, List<Object>> updateAndGPKNativeInCallSharding(int sharding,int tbSharding,String nativeSQL,int synFlag,final Object... params){
		KV<Integer,List<Object>> keys;
		String[] resultSQL = getNativeSQL(nativeSQL,tbSharding);
		LogUtil.getDefaultLogger().debug("【Debug】Native SQL：" + resultSQL[1]);
		JdbcBaseDao baseDao = FacadeProxy.create(JdbcBaseDao.class);
		if(synFlag == 0){
			LogUtil.getDefaultLogger().debug("【Debug】Native SQL：" + resultSQL[1]);
			baseDao.setManager(getSessionMgr(sharding,Integer.parseInt(resultSQL[0])));
			return baseDao.updateAndGenerateKeys(resultSQL[1], params);
		}
		if(synFlag == 1 && SynDbLog.isSynBackDB(Integer.parseInt(resultSQL[0]))){
			LogUtil.getDefaultLogger().debug("【Debug】Native SYN SQL：" + resultSQL[1]);
			baseDao.setManager(getBackupSessionMgr(sharding,Integer.parseInt(resultSQL[0]),false));
			return baseDao.updateAndGenerateKeys(resultSQL[1], params);
		}
		return null;
	}



	/**
	* @version 版本：1.0
	* @param nativeSQL 原生态SQL查询语句；但是所有表名用“{{?^\\d+$}}”,如：{{?0}}代替，"?"后面的数字，请同DSMConst中DB_TABLES的索引一一对应。
	* @param params　原生态SQL查询语句中“?”的参数值数组；
	* @return 每一个表影响的记录数
	* 多表事务更新
	 */
	public int[] updateTransNative(String[] nativeSQL,final List<Object[]> params){
		int[] result = new int[nativeSQL.length];
		String[] resultSQL = getNativeSQL(nativeSQL[0]);
		AbstractJdbcSessionMgr sessionMgr = getSessionMgr(Integer.parseInt(resultSQL[0]));

		//异步同步到备份库
		SynDbData synDbData = new SynDbData(resultSQL,null,master.get(),2);
		synDbData.setNativeSQL(nativeSQL);
		synDbData.setParams(params);
		synDbData.setSharding(0);
		Future<Object> future = ASYN_THREAD.submit(synDbData);

		try {
			FacadeProxy.executeCustomTransaction(sessionMgr,new JdbcTransaction() {
			    @Override
				public void execute(AbstractJdbcSessionMgr sessionMgr) throws DAOException {
			        for (int i = 0; i < nativeSQL.length; i++) {
						result[i] = updateNativeInCall(nativeSQL[i],0,params.get(i));
					}
				}
			});
		} catch (DAOException e) {
			LogUtil.getDefaultLogger().error(getClass().getSimpleName(),e);
			e.printStackTrace();
			if(SynDbLog.isBaseSqlError(e.getCause().getCause())){
				return null;
			}
			//写入日志
			Object swRet = switchDB(future,nativeSQL,
					params,0,0,master.get(),false);
			if(swRet != null){
				return (int[])swRet;
			}
		}
		return result;
	}




	/**
	 * @version 版本：1.0
	 * @param sharding 分片字段
	 * @param nativeSQL 原生态SQL查询语句；但是所有表名用“{{?^\\d+$}}”,如：{{?0}}代替，"?"后面的数字，请同DSMConst中DB_TABLES的索引一一对应。
	 * @param params　原生态SQL查询语句中“?”的参数值数组；
	 * @return 每一个表影响的记录数
	 * 多表事务更新
	 */
	public int[] updateTransNativeSharding(int sharding,int tbSharding,String[] nativeSQL,final List<Object[]> params){
		int[] result = new int[nativeSQL.length];
		String[] resultSQL = getNativeSQL(nativeSQL[0]);
		AbstractJdbcSessionMgr sessionMgr = getSessionMgr(sharding,Integer.parseInt(resultSQL[0]));

		//异步同步到备份库
        SynDbData synDbData = new SynDbData(resultSQL,null,master.get(),2);
        synDbData.setNativeSQL(nativeSQL);
        synDbData.setParams(params);
        synDbData.setSharding(sharding);
        synDbData.setTbSharding(tbSharding);
        Future<Object> future = ASYN_THREAD.submit(synDbData);

        //异步同步到订单运营后台
        if(Integer.parseInt(resultSQL[0]) == DSMConst.TD_TRAN_ORDER
                || Integer.parseInt(resultSQL[0]) == DSMConst.TD_TRAN_GOODS){

            synDbData = new SynDbData(resultSQL,null,master.get(),5);
            synDbData.setNativeSQL(nativeSQL);
            synDbData.setParams(params);
            synDbData.setSharding(sharding);
            synDbData.setTbSharding(tbSharding);
            ASYN_THREAD.submit(synDbData);
        }

		try {
			FacadeProxy.executeCustomTransaction(sessionMgr,new JdbcTransaction() {
				@Override
				public void execute(AbstractJdbcSessionMgr sessionMgr) throws DAOException {
					for (int i = 0; i < nativeSQL.length; i++) {
						result[i] = updateNativeInCallSharding(sharding,tbSharding,nativeSQL[i],0,params.get(i));
					}
				}
			});
		} catch (DAOException e) {
			LogUtil.getDefaultLogger().error(getClass().getSimpleName(),e);
			e.printStackTrace();
			if(SynDbLog.isBaseSqlError(e.getCause().getCause())){
				return null;
			}
			//写入日志
			Object swRet = switchDB(future,nativeSQL,
					params,sharding,tbSharding,master.get(),false);
			if(swRet != null){
				return (int[])swRet;
			}
		}
		return result;
	}

	/**
	 * @version 版本：1.0
	 * @param conditionList 条件集合（sharding 分片字段，nativeSQL  执行SQL，params SQL参数）
	 * @param tbSharding 年份，为0 则默认当年
	 * 多表事务更新
	 */
	public boolean updateMaualTransNativeGlobal(List<GlobalTransCondition> conditionList, int tbSharding){
		Set<AbstractJdbcSessionMgr> mgrSet = new HashSet();
		boolean isSuccess = true;
		boolean isCommit = true;
		try {
			for(GlobalTransCondition ct: conditionList){
                AbstractJdbcSessionMgr sessionMgr = getSessionMgr(ct.getSharding(),
                        Integer.parseInt(getNativeSQL(ct.getNativeSQL()[0])[0]));
                sessionMgr.setInvoking(true);
				sessionMgr.beginTransaction();
				mgrSet.add(sessionMgr);
                if(!updateMaualTransNative(sessionMgr,tbSharding,ct.getNativeSQL(),ct.getParams())){
                    isCommit = false;
                    break;
                }
            }

            if(mgrSet.isEmpty()){
			    return false;
            }

			if(isCommit){
                for (AbstractJdbcSessionMgr mgr : mgrSet){
                    mgr.commit();
                }
            }else{
                for (AbstractJdbcSessionMgr mgr : mgrSet){
                    mgr.rollback();
                }
				isSuccess = false;
            }
		} catch (Exception e) {
			isSuccess = false;
			for (AbstractJdbcSessionMgr mgr : mgrSet){
				mgr.rollback();
			}
			
		} finally {
			for (AbstractJdbcSessionMgr mgr : mgrSet){
				mgr.setInvoking(false);
				mgr.closeSession();
			}
		}
		return isSuccess;
	}

	private boolean updateMaualTransNative(AbstractJdbcSessionMgr sessionMgr, int tbSharding, String[] nativeSQL, final List<Object[]> params){
		boolean isUpdate = true;
		String[] resultSQL = getNativeSQL(nativeSQL[0]);
		try {
			JdbcBaseDao baseDao = FacadeProxy.create(JdbcBaseDao.class);
			baseDao.setManager(sessionMgr);
			for (int i = 0; i < nativeSQL.length; i++) {
				baseDao.update(getNativeSQL(nativeSQL[i],tbSharding)[1], params.get(i));
			}
		} catch (Exception e) {
			LogUtil.getDefaultLogger().error(getClass().getSimpleName(),e);
		}
		return isUpdate;
	}

	public boolean updateManalTransNativeEx(AbstractJdbcSessionMgr sessionMgr, String[] nativeSQL, final List<Object[]> params){
		return updateMaualTransNative(sessionMgr,0,nativeSQL,params);
	}

	/**
	* @version 版本：1.0
	* @param GPKNativeSQL 需要返回自增长字段值的更新SQL集合对象。
	* @param nativeSQL 需要插入刚产生的自增长字段值的更新SQL集合对象，
	* @param params GPKNativeSQL和nativeSQL中带"?"参数的参数值集合。
	* @return Key成功更新的记录数；Value自增的字段值集合。
	* 多表事务更新，并需要返回部分表的自增主键作为另一更新语句的参数值。
	 */
	public  int[] updateAndGPKTransNative(String[] GPKNativeSQL,String[] nativeSQL,final List<Object[]> params){
		int[] result = new int[GPKNativeSQL.length + nativeSQL.length];
        String[] resultSQL = getNativeSQL(nativeSQL[0]);
		AbstractJdbcSessionMgr sessionMgr = getSessionMgr(Integer.parseInt(resultSQL[0]));

		SynDbData synDbData = new SynDbData(resultSQL,null,master.get(),3);
		synDbData.setGPKNativeSQL(GPKNativeSQL);
		synDbData.setParams(params);
		synDbData.setNativeSQL(nativeSQL);
		synDbData.setSharding(0);

		Future<Object> future = ASYN_THREAD.submit(synDbData);

		try {
			FacadeProxy.executeCustomTransaction(sessionMgr,new JdbcTransaction() {
			    @Override
				public void execute(AbstractJdbcSessionMgr sessionMgr) throws DAOException {
			    	List<Object[]> autoGPK = new ArrayList<Object[]>();
			        for (int i = 0; i < GPKNativeSQL.length; i++) {
			        	KV<Integer,List<Object>> keys = updateAndGPKNativeInCall(GPKNativeSQL[i],0,params.get(i));
						//返回类型为[4,5],代表插入了两行，产生了两个自增长值，分别为4和5
			        	autoGPK.add(keys.getValue().toArray());
						//自增值个数，也就是行数（同时插入多行记录）
			        	result[i] = keys.getKey();
				    }
			        Object[] paramsTrue;
			        for (int i = 0 ; i < nativeSQL.length; i++) {
			        	paramsTrue = getTrueParams(autoGPK,params.get(i + GPKNativeSQL.length));
				        result[i + GPKNativeSQL.length] = updateNativeInCall(nativeSQL[i],0,paramsTrue);
				    }
				}
			});
		} catch (DAOException e) {
			LogUtil.getDefaultLogger().error(getClass().getSimpleName(),e);
			if(SynDbLog.isBaseSqlError(e.getCause().getCause())){
				return null;
			}
			//写入日志
			Object swRet = switchDB(future,nativeSQL,
					params,0,0,master.get(),false);
			if(swRet != null){
				return (int[])swRet;
			}
		}
		return result;
	}




	/**
	 * @version 版本：1.0
	 * @param sharding 分片字段
	 * @param tbSharding 表分片字段
	 * @param GPKNativeSQL 需要返回自增长字段值的更新SQL集合对象。
	 * @param nativeSQL 需要插入刚产生的自增长字段值的更新SQL集合对象，
	 * @param params GPKNativeSQL和nativeSQL中带"?"参数的参数值集合。
	 * @return Key成功更新的记录数；Value自增的字段值集合。
	 * 多表事务更新，并需要返回部分表的自增主键作为另一更新语句的参数值。
	 */
	public  int[] updateAndGPKTransNativeSharding(int sharding,int tbSharding,String[] GPKNativeSQL,String[] nativeSQL,final List<Object[]> params){
		int[] result = new int[GPKNativeSQL.length + nativeSQL.length];
		String[] resultSQL = getNativeSQL(nativeSQL[0]);
		AbstractJdbcSessionMgr sessionMgr = getSessionMgr(sharding,Integer.parseInt(resultSQL[0]));

		SynDbData synDbData = new SynDbData(resultSQL,null,master.get(),3);
		synDbData.setGPKNativeSQL(GPKNativeSQL);
		synDbData.setParams(params);
		synDbData.setNativeSQL(nativeSQL);
		synDbData.setTbSharding(tbSharding);
		synDbData.setSharding(sharding);
		Future<Object> future = ASYN_THREAD.submit(synDbData);

		try {
			FacadeProxy.executeCustomTransaction(sessionMgr,new JdbcTransaction() {
				@Override
				public void execute(AbstractJdbcSessionMgr sessionMgr) throws DAOException {
					List<Object[]> autoGPK = new ArrayList<Object[]>();
					for (int i = 0; i < GPKNativeSQL.length; i++) {
						KV<Integer,List<Object>> keys = updateAndGPKNativeInCallSharding(sharding,tbSharding,GPKNativeSQL[i],0,params.get(i));
						//返回类型为[4,5],代表插入了两行，产生了两个自增长值，分别为4和5
						autoGPK.add(keys.getValue().toArray());
						//自增值个数，也就是行数（同时插入多行记录）
						result[i] = keys.getKey();
					}
					Object[] paramsTrue;
					for (int i = 0 ; i < nativeSQL.length; i++) {
						paramsTrue = getTrueParams(autoGPK,params.get(i + GPKNativeSQL.length));
						result[i + GPKNativeSQL.length] = updateNativeInCallSharding(sharding,tbSharding,nativeSQL[i],0,paramsTrue);
					}
				}
			});
		} catch (DAOException e) {
			LogUtil.getDefaultLogger().error(getClass().getSimpleName(),e);
			if(SynDbLog.isBaseSqlError(e.getCause().getCause())){
				return null;
			}
			//写入日志
			Object swRet = switchDB(future,nativeSQL,
					params,sharding,tbSharding,master.get(),false);
			if(swRet != null){
				return (int[])swRet;
			}
		}
		return result;
	}


	/**
	 * @Title: updateBatchNative
	 * @Description: TODO 单表批量更新
	 * @param nativeSQL 原生态SQL查询语句；但是所有表名用“{{?^\\d+$}}”,如：{{?0}}代替，"?"后面的数字，请同DSMConst中DB_TABLES的索引一一对应。
	 * @param params 原生态SQL查询语句中“?”的参数值数组集合，每个数组为一条语句赋值；
	 * @param batchSize 多少条语句组成一个批量执行。
	 * @return: int更新的记录数
	 */
	public int[] updateBatchNative(String nativeSQL, List<Object[]> params, int batchSize){
		String[] resultSQL = getNativeSQL(nativeSQL);
		LogUtil.getDefaultLogger().debug("【Debug】Native SQL：" + resultSQL[1]);
		int[] result = null;
		SynDbData synDbData = new SynDbData(resultSQL,null,master.get(),4);
		synDbData.setBatchSize(batchSize);
		synDbData.setParams(params);
		synDbData.setNativeSQL(new String[]{nativeSQL});
		synDbData.setSharding(0);
		Future<Object> future = ASYN_THREAD.submit(synDbData);
		try{
			JdbcBaseDao baseDao = FacadeProxy.create(JdbcBaseDao.class);
			baseDao.setManager(getSessionMgr(Integer.parseInt(resultSQL[0])));
			result = baseDao.updateBatch(resultSQL[1], params, batchSize);
		}catch (DAOException e) {
			LogUtil.getDefaultLogger().error(getClass().getSimpleName(),e);
			e.printStackTrace();
			if(SynDbLog.isBaseSqlError(e.getCause().getCause())){
				return null;
			}
			//写入日志
			Object swRet = switchDB(future,new String[]{nativeSQL},
					params,0,0,master.get(),true);
			if(swRet != null){
				return (int[])swRet;
			}
		}
		return result;
	}


	/**
	 * @Title: updateBatchNativeSharding
	 * @Description: TODO 单表批量更新
	 * @param  sharding 分片字段
	 * @param nativeSQL 原生态SQL查询语句；但是所有表名用“{{?^\\d+$}}”,如：{{?0}}代替，"?"后面的数字，请同DSMConst中DB_TABLES的索引一一对应。
	 * @param params 原生态SQL查询语句中“?”的参数值数组集合，每个数组为一条语句赋值；
	 * @param batchSize 多少条语句组成一个批量执行。
	 * @return: int更新的记录数
	 */
	public int[] updateBatchNativeSharding(int sharding,int tbSharding,String nativeSQL, List<Object[]> params, int batchSize){
		String[] resultSQL = getNativeSQL(nativeSQL,tbSharding);
		LogUtil.getDefaultLogger().debug("【Debug】Native SQL：" + resultSQL[1]);
		int[] result = null;

		SynDbData synDbData = new SynDbData(resultSQL,null,master.get(),4);
		synDbData.setBatchSize(batchSize);
		synDbData.setParams(params);
		synDbData.setSharding(sharding);
		synDbData.setTbSharding(tbSharding);
		synDbData.setNativeSQL(new String[]{nativeSQL});
		Future<Object> future = ASYN_THREAD.submit(synDbData);
		try{
			JdbcBaseDao baseDao = FacadeProxy.create(JdbcBaseDao.class);
			baseDao.setManager(getSessionMgr(sharding,Integer.parseInt(resultSQL[0])));
			result = baseDao.updateBatch(resultSQL[1], params, batchSize);
		}catch (DAOException e){
			LogUtil.getDefaultLogger().error(getClass().getSimpleName(),e);
			e.printStackTrace();
			if(SynDbLog.isBaseSqlError(e.getCause().getCause())){
				return null;
			}
			//写入日志
			Object swRet = switchDB(future,new String[]{nativeSQL},
					params,sharding,tbSharding,master.get(),true);
			if(swRet != null){
				return (int[])swRet;
			}
		}
		return result;
	}

	/*=====================分页相关方法开始===================================*/
	public List<Object[]> queryNative(PageHolder pageOut, Page paged,String nativeSQL, final Object... params) {
        return queryNative(pageOut, paged, "", nativeSQL, params);
    }


    public List<Object[]> queryNativeSharding(int sharding,int tbSharding,PageHolder pageOut, Page paged,String nativeSQL, final Object... params) {
        return queryNativeSharding(sharding,tbSharding,pageOut, paged, "", nativeSQL, params);
    }

    public List<Object[]> queryNativeC(PageHolder pageOut, Page paged,String nativeSQL, final Object... params) {
        return queryNativeC(pageOut, paged, null, nativeSQL, params);
    }

	public List<Object[]> queryNativecSharding(int sharding,int tbSharding,PageHolder pageOut, Page paged,String nativeSQL, final Object... params) {
		return queryNativecSharding(sharding,tbSharding,pageOut, paged, null, nativeSQL, params);
	}

    public List<Object[]> queryNative(PageHolder pageOut, Page paged, String sortBy, String nativeSQL, final Object... params) {
        return queryNative(pageOut, paged, sortBy, null, nativeSQL, params);
    }

    public List<Object[]> queryNativeSharding(int sharding,int tbSharding,PageHolder pageOut, Page paged,String sortBy, String nativeSQL, final Object... params) {
        return queryNativeSharding(sharding,tbSharding,pageOut, paged, sortBy, null, nativeSQL, params);
    }

	public List<Object[]> queryNativeC(PageHolder pageOut, Page paged, String sortBy, String nativeSQL, final Object... params) {
		return queryNativeC(pageOut, paged, sortBy, null, nativeSQL, params);
	}

	public List<Object[]> queryNativecSharding(int sharding, int tbSharding, PageHolder pageOut, Page paged, String sortBy, String nativeSQL, final Object... params) {
		return queryNativecSharding(sharding,tbSharding,pageOut, paged, sortBy, null, nativeSQL, params);
	}


    public List<Object[]> queryNative(PageHolder pageOut, Page paged,String sortBy, Object[] countParams, String nativeSQL,final Object... params) {
        String sql = nativeSQL;
        PagedList list = new PagedListI(false, paged, sortBy, sql, countParams,params);
        List<Object[]> result = list.getList();
        if (pageOut != null)
            pageOut.value = queryPage(list);
        return result;
    }

    public List<Object[]> queryNativeSharding(int sharding,int tbSharding,PageHolder pageOut, Page paged,String sortBy, Object[] countParams, String nativeSQL,final Object... params) {
        String sql = nativeSQL;
        PagedList list = new PagedListI(false, paged, sortBy, sql, countParams,params);
        List<Object[]> result = list.getList(sharding,tbSharding);
        if (pageOut != null)
            pageOut.value = queryPageSharding(sharding,tbSharding,list);
        return result;
    }

    public List<Object[]> queryNativeC(PageHolder pageOut, Page paged,String sortBy, Object[] countParams, String nativeSQL,final Object... params) {
        String sql = nativeSQL;
        PagedList list = new PagedListI(true, paged, sortBy, sql, countParams,params);
        List<Object[]> result = list.getList();
        if (pageOut != null)
            pageOut.value = queryPage(list);
        return result;
    }

	public List<Object[]> queryNativecSharding(int sharding,int tbSharding,PageHolder pageOut, Page paged,String sortBy, Object[] countParams, String nativeSQL,final Object... params) {
		String sql = nativeSQL;
		PagedList list = new PagedListI(true, paged, sortBy, sql, countParams,params);
		List<Object[]> result = list.getList(sharding,tbSharding);
		if (pageOut != null)
			pageOut.value = queryPageSharding(sharding,tbSharding,list);
		return result;
	}
    
    public Page queryPage(PagedList pagedList) {
        Page page = new Page();
        page.pageIndex = pagedList.getPageIndex();
        page.pageSize = pagedList.getPageSize();
        page.totalItems = pagedList.getTotalRowCount();
        page.totalPageCount = pagedList.getTotalPageCount();
        return page;
    }

	public Page queryPageSharding(int sharding,int tbSharding,PagedList pagedList) {
		Page page = new Page();
		page.pageIndex = pagedList.getPageIndex();
		page.pageSize = pagedList.getPageSize();
		page.totalItems = pagedList.getTotalRowCount(sharding,tbSharding);
		page.totalPageCount = pagedList.getTotalPageCount();
		return page;
	}
    /*=====================分页相关方法结束===================================*/
	/**
     * @Title: convToEntity shanben-CN
     * @Description: TODO 利用反射技术将数据查询出来的List<Object[]>转换成RPC所需要的实体数组对象。
     * @param srcLst
     *            数据库查询的结果集合
     * @param tArray
     *            输出参数，RPC所需要的实体数组对象
     * @param clszz
     *            通过泛型传进来的对象
     * @param proName
     *            通过泛型传进来的对象需要赋值的属性名数组。当传进null时，代表通过泛型传进来的对象的属性与查询出来的数据的列是一一对应的。
     */
    public <T> void convToEntity(List<Object[]> srcLst, T[] tArray,
            Class<T> clszz, String[] proName) {
        if (proName == null) {
            convToEntityMatch(srcLst, tArray, clszz);
        } else {
            convToEntityNoMatch(srcLst, tArray, clszz, proName);
        }
    }
	
    /**
     * @Title: convToEntityMatch shanben-CN
     * @Description: TODO 通过泛型传进来的对象的属性与查询出来的数据的列是一一对应的情况下调用此方法
     * @param srcLst
     *            　数据库查询的结果集合
     * @param tArray
     *            　输出参数，RPC所需要的实体数组对象
     * @param clszz
     *            　 通过泛型传进来的对象
     */
    @SuppressWarnings("unchecked")
    private <T> void convToEntityMatch(List<Object[]> srcLst, T[] tArray,
            Class<T> clszz) {
        try {
            // 通过泛型传进来的对象
            Object obj;
            // 传进来的数据集合个数(数据库查询的结果行数)计数器
            int arraryIndex = 0;
            // 通过泛型传进来的对象实体中定义的全部属性。
            Field[] fedAtt = clszz.getDeclaredFields();
            for (Object[] srcArr : srcLst) {
                // 反射动态创建实例。
                obj = clszz.newInstance();
                // 循环给实体的属性赋值
                for (int i = 0; i < srcArr.length; i++) {
                    fedAtt[i].setAccessible(true); 
                	fedAtt[i].set(obj, getFieldTypeValue(fedAtt[i],srcArr[i]));
                }
                // 将赋值好的对象放到输出参数数组中去；
                tArray[arraryIndex] = (T) obj;
                arraryIndex++;
            }
        } catch (Exception e) {
			LogUtil.getDefaultLogger().error(getClass().getSimpleName(),e);
        }
    }

    /**
     * @Title: convToEntityNoMatch shanben-CN
     * @Description: TODO 通过泛型传进来的对象的属性与查询出来的数据的列不是一一对应的情况下调用此方法
     * @param srcLst
     *            　数据库查询的结果集合
     * @param tArray
     *            　输出参数，RPC所需要的实体数组对象
     * @param clszz
     *            　 通过泛型传进来的对象
     */
    @SuppressWarnings("unchecked")
    private <T> void convToEntityNoMatch(List<Object[]> srcLst, T[] tArray,
            Class<T> clszz, String[] proName) {
        try {
            // 通过泛型传进来的对象
            Object obj;
            // 传进来的数据集合个数(数据库查询的结果行数)计数器
            int arraryIndex = 0;
            // 循环取数据集合
            for (Object[] srcArr : srcLst) {
                // 反射动态创建实例。
                obj = clszz.newInstance();
                // 传进来的实体对象需要赋值的属性计数器
                int colsNum = 0;
                // 循环给实体的属性赋值
                for (String fieldName : proName) {
                    Field f = clszz.getDeclaredField(fieldName);
                    f.setAccessible(true); 
                	f.set(obj, getFieldTypeValue(f,srcArr[colsNum]));
                    colsNum++;
                }
                // 将赋值好的对象放到输出参数数组中去；
                tArray[arraryIndex] = (T) obj;
                arraryIndex++;
            }
        } catch (Exception e) {
			LogUtil.getDefaultLogger().error(getClass().getSimpleName(),e);
        }
    }
    
	/**
	 * @Title: convToEntity shanben-cN
	 * @Description: TODO 利用反射技术将数据查询出来的List<Object[]>转换成RPC所需要的实体数组对象。
	 * @param srcLst 数据库查询的结果集合
	 * @param tArray 输出参数，RPC所需要的实体数组对象
	 * @param clszz 通过泛型传进来的对象
	 */
	@SuppressWarnings({"unchecked" })
	public <T> void convToEntity(List<Object[]> srcLst, T[] tArray, Class<T> clszz){   
        try {  
        	//通过泛型传进来的对象
        	Object obj;
            //通过泛型传进来的对象实体中定义的全部属性。
            Field[] fedAtt = clszz.getDeclaredFields();
            //传进来的数据集合个数(数据库查询的结果行数)计数器
            int arraryIndex = 0;
            //循环取数据集合
            for (Object[] srcArr : srcLst) {  
                //反射动态创建实例。
                obj = newInstance(clszz);
                //循环给实体的属性赋值
                for (int i = 0; i < srcArr.length; i++) {
                	fedAtt[i].setAccessible(true); 
                	fedAtt[i].set(obj, getFieldTypeValue(fedAtt[i],srcArr[i]));
                }
                //将赋值好的对象放到输出参数数组中去；
                tArray[arraryIndex] = (T)obj;
              	arraryIndex++;
            }     
        } catch (Exception e) {
			LogUtil.getDefaultLogger().error(getClass().getSimpleName(),e);
        } 
    }

    private Object newInstance(Class clszz) throws Exception {
		Object result;
		try {
			result = clszz.newInstance();
		} catch (Exception e) {
			Constructor constructor = clszz.getDeclaredConstructor();
			constructor.setAccessible(true);
			result = constructor.newInstance();
		}

		return result;
	}

	/**
	 * @Title: getFieldTypeValue shanben-CN
	 * @Description: TODO 根据反射出的字段的数据类型，再其需要赋值的变量改变成对应的数据类型的值。
	 * @param field 反射出来的对象的字段对象
	 * @param value 从数据库中查询出来的该字段的值
	 * @return: Object 同字段类型匹配的值。
	 */
	private Object getFieldTypeValue(Field field,Object value){
		  Object resultValue = null;
		  switch(field.getType().getName()){
		  case "int":
		  case "java.lang.Integer":
			  if(value==null) return 0;
			  resultValue = Integer.parseInt(value.toString());
			  break;
		  case "long":
		  case "java.lang.Long":
			  if(value==null) return 0;
			  resultValue = Long.parseLong(value.toString());
			  break;
		  case "double":
		  case "java.lang.Double":
			  if(value==null) return 0;
			  resultValue = Double.parseDouble(value.toString());
			  break;
		  case "boolean":
		  case "java.lang.Boolean":
			  if(value==null) return false;
			  resultValue = Boolean.parseBoolean(value.toString());
			  break;
		  case "byte":
		  case "java.lang.Byte":
			  if(value==null) return 0;
			  resultValue = Byte.parseByte(value.toString());
			  break;
		  case "short":
		  case "java.lang.Short":
			  if(value==null) return 0;
			  resultValue = Short.parseShort(value.toString());
			  break;
		  case "float":
		  case "java.lang.Float":
			  if(value==null) return 0;
			  resultValue = Float.parseFloat(value.toString());
			  break;
		  default:
			  if(value==null) return "";
			  resultValue = value.toString();
		  }
		  return resultValue;
	}
	
    /**
	 * @Title: getDelRange
	 * @Description: TODO 批量删除时，根据用户传进来的多个ID值进行连续的ID值只产生一条删除SQL语句。
	 * @param ids 用户需要删除的多条记录的ID值数组。
	 * @return: List<int[]>要产生的删除SQL的条数及每条删除语句id的最小值及最大值。List的size对应删除语句的条数，int[]里，索引为0的代表最小值，索引为1的代表最大值。
	 */
	public List<Object[]> getDelRange(int[] ids){
		Arrays.sort(ids);
		int preValue = ids[0];
        int[] minRange = new int[ids.length];
        int[] maxRange = new int[ids.length];
        minRange[0] = preValue;
        maxRange[0] = preValue;
        int index = 0;
		for(int i = 1; i < ids.length ; i++){
		    if(preValue == ids[i] - 1) {
		    	maxRange[index] = ids[i];
			    preValue++;
		    }else{
		    	preValue = ids[i];
		    	index++;
		    	minRange[index] = ids[i];
		    	maxRange[index] = ids[i];
		    } 
		}
		List<Object[]> result = new ArrayList<Object[]>();
		for(int i = 0; i < maxRange.length; i++){
			if(0 == maxRange[i]) break;
		    result.add(new Object[]{minRange[i],maxRange[i]});
		}
		return result;
	}
    
    /**
     * @Title: getDelRange
     * @Description: TODO 批量删除时，根据用户传进来的多个ID值进行连续的ID值只产生一条删除SQL语句。
     * @param ids 用户需要删除的多条记录的ID值数组
     *            。
     * @return: 
     *          List<int[]>要产生的删除SQL的条数及每条删除语句id的最小值及最大值。List的size对应删除语句的条数，int[]
     *          里，索引为0的代表最小值，索引为1的代表最大值。
     */
    public List<Object[]> getDelRange(long[] ids) {
        Arrays.sort(ids);
        long preValue = ids[0];
        long[] minRange = new long[ids.length];
        long[] maxRange = new long[ids.length];
        minRange[0] = preValue;
        maxRange[0] = preValue;
        int index = 0;
        for (int i = 1; i < ids.length; i++) {
            if (preValue == ids[i] - 1) {
                maxRange[index] = ids[i];
                preValue++;
            } else {
                preValue = ids[i];
                index++;
                minRange[index] = ids[i];
                maxRange[index] = ids[i];
            }
        }
        List<Object[]> result = new ArrayList<Object[]>();
        for (int i = 0; i < maxRange.length; i++) {
            if (0 == maxRange[i])
                break;
            result.add(new Object[] { minRange[i], maxRange[i] });
        }
        return result;
    }



	private Object switchDB(Future<Object> future,
							String[] nativeSQL,List<Object[]> params,
							int sharding,int tbSharding,int masterval,boolean isbatch){
    	Object result = null;
		try {
			LogUtil.getDefaultLogger().debug("准备切换数据库");
			result = future.get();
			if(result != null){
				int[] results = SynDbLog.updateTransNative
						(nativeSQL,params,sharding,tbSharding,masterval,isbatch);
				//切换数据库
				if(results != null && results.length > 0 && results[0] > 0){
					master.set(master.get() == 0 ? 1: 0);
					LoadDbConfig.setDbMasterNum(master.get());
					LogUtil.getDefaultLogger().debug("准备切换数据库成功，"+master.get());
				}
			}
		} catch (Exception e) {
			LogUtil.getDefaultLogger().debug("切换数据库失败！");
			e.printStackTrace();
		}
		return result;
	}

}
