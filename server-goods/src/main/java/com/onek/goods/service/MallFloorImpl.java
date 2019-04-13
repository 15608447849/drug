package com.onek.goods.service;

import com.onek.goods.entities.MallFloorVO;
import constant.DSMConst;
import dao.BaseDAO;
import redis.IRedisCache;

import java.util.Arrays;
import java.util.List;

public class MallFloorImpl implements IRedisCache {

    private static BaseDAO baseDao = BaseDAO.getBaseDAO();

    @Override
    public String getPrefix() {
        return "_mallFloor";
    }

    @Override
    public String getKey() {
        return "unqid";
    }

    @Override
    public Class<?> getReturnType() {
        return MallFloorVO.class;
    }

    @Override
    public Object getId(Object id) {
        return null;
    }

    @Override
    public int del(Object id) {
        return 0;
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
        String selectSQL = "select oid,unqid,fname,cstatus from {{?" + DSMConst.TB_MALL_FLOOR + "}} where cstatus&1=0 order by sortno desc";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        MallFloorVO[] levels = new MallFloorVO[queryResult.size()];
        baseDao.convToEntity(queryResult, levels, MallFloorVO.class);
        return Arrays.asList(levels);
    }

    @Override
    public List<?> queryByParams(String[] params) {
        return null;
    }
}
