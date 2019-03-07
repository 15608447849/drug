package com.onek.user.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//
//import com.hsf.framework.jdbc.constant.DSMConst;
//import com.hsf.framework.jdbc.dao.BaseDAO;

import com.onek.user.entity.DictVo;
import constant.DSMConst;
import dao.BaseDAO;
import redis.IRedisCache;
import redis.annation.RedisCache;
@RedisCache(clazz = DictVo.class)
public class DictServiceImpl implements IRedisCache{

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

	@Override
	public int add(Object obj) {
		return 0;
	}

	@Override
	public int update(Object obj) {
		return 0;
	}

	@Override
	public List<?> queryAll() {

		List<Object[]> result = baseDao.queryNative("select * from {{?"+ DSMConst.LC_GLOBAL_DICT +"}} where cstatus&1= 0", new Object[] {});
		DictVo[] dicts = new DictVo[result.size()];
		baseDao.convToEntity(result, dicts, DictVo.class);
		return Arrays.asList(dicts	);

	}

	@Override
	public List<?> queryByParams(String [] params) {
		List<Object[]> result = baseDao.queryNative("select * from {{?"+ DSMConst.LC_GLOBAL_DICT +"}} where cstatus&1= 0 and type= ? and text like CONCAT('%',?,'%')", params);
		DictVo[] dicts = new DictVo[result.size()];
		baseDao.convToEntity(result, dicts, DictVo.class);
		return Arrays.asList(dicts	);
	}


}
