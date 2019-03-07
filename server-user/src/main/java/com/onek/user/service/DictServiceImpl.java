package com.onek.user.service;

import java.util.List;

//
//import com.hsf.framework.jdbc.constant.DSMConst;
//import com.hsf.framework.jdbc.dao.BaseDAO;

import com.onek.user.entity.DictVo;
import redis.IRedisCache;
import redis.annation.RedisCache;
@RedisCache(clazz = DictVo.class)
public class DictServiceImpl implements IRedisCache{

//	private static BaseDAO baseDao = BaseDAO.getBaseDAO();
	
	@Override
	public Object getId(Object id) {
//		List<Object[]> result = baseDao.queryNative("select * from {{?"+ DSMConst.LC_GLOBAL_DICT +"}} where cstatus&1= 0 and dictc = ?", new Object[] {id});
//		DictVo[] dicts = new DictVo[result.size()];
//		baseDao.convToEntity(result, dicts, DictVo.class);
//		return dicts[0];
		return null;
	}

	@Override
	public int del(Object id) {
		return 1;
	}

	@Override
	public int add(Object obj) {
		return 0;
	}

	@Override
	public int update(Object obj) {
		return 0;
	}

	@Override
	public List<Object> queryAll() {
		return null;
	}

	@Override
	public List<Object> queryByParams(Object... params) {
		return null;
	}


}
