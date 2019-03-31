package com.onek.util.dict;

import constant.DSMConst;
import dao.BaseDAO;
import redis.IRedisCache;

import java.util.Arrays;
import java.util.List;

public class DictUtil implements IRedisCache {

    private static BaseDAO baseDao = BaseDAO.getBaseDAO();

    @Override
    public String getPrefix() {
        return "dict_";
    }

    @Override
    public String getKey() {
        return "dictc";
    }

    @Override
    public Class<?> getReturnType() {
        return DictEntity.class;
    }

    @Override
    public DictEntity getId(Object id) {
        List<Object[]> result = baseDao.queryNative("select * from {{?"+ DSMConst.D_GLOBAL_DICT +"}} where cstatus&1= 0 and dictc = ?", new Object[] {id});
        DictEntity[] dicts = new DictEntity[result.size()];
        baseDao.convToEntity(result, dicts, DictEntity.class);
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

        List<Object[]> result = baseDao.queryNative("select * from {{?"+ DSMConst.D_GLOBAL_DICT +"}} where cstatus&1= 0", new Object[] {});
        DictEntity[] dicts = new DictEntity[result.size()];
        baseDao.convToEntity(result, dicts, DictEntity.class);
        return Arrays.asList(dicts	);

    }

    @Override
    public List<?> queryByParams(String [] params) {
        return null;
    }

}
