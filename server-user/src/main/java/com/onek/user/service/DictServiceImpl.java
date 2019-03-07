package sql.impl;

import java.util.List;

import org.annation.RedisCache;
import org.entity.DictVo;

import com.hsf.framework.jdbc.constant.DSMConst;
import com.hsf.framework.jdbc.dao.BaseDAO;

import sql.Cache;

@RedisCache(clazz = DictVo.class)
public class DictServiceImpl implements Cache{

	private static BaseDAO baseDao = BaseDAO.getBaseDAO();
	
	@Override
	public Object getId(Object id) {
		List<Object[]> result = baseDao.queryNative("select * from {{?"+ DSMConst.LC_GLOBAL_DICT +"}} where cstatus&1= 0 and dictc = ?", new Object[] {id});
		DictVo[] dicts = new DictVo[result.size()];
		baseDao.convToEntity(result, dicts, DictVo.class);
		return dicts[0];
	}

	@Override
	public int del(Object id) {
		return 1;
	}

	
}
