package com.onek.user.service;

import com.onek.user.entity.ConsigneeVO;
import com.onek.user.entity.MemberLevelVO;
import com.onek.util.RedisGlobalKeys;
import constant.DSMConst;
import dao.BaseDAO;
import redis.IRedisCache;

import java.util.Arrays;
import java.util.List;

public class MemberLevelImpl implements IRedisCache {

    private static BaseDAO baseDao = BaseDAO.getBaseDAO();

    @Override
    public String getPrefix() {
        return RedisGlobalKeys.MEMBER_LEVEL_PREFIX;
    }

    @Override
    public String getKey() {
        return "unqid";
    }

    @Override
    public Class<?> getReturnType() {
        return MemberLevelVO.class;
    }

    @Override
    public MemberLevelVO getId(Object id) {
        String selectSQL = "select unqid,lname,groval,cstatus from {{?" + DSMConst.TD_MEMBER_LEVEL + "}} where cstatus&1=0 "
                + " and unqid=" + id + " and cstatus&1=0";
        List<Object[]> result = baseDao.queryNative(selectSQL);
        MemberLevelVO[] levels = new MemberLevelVO[result.size()];
        baseDao.convToEntity(result, levels, MemberLevelVO.class);
        return levels[0];
    }

    @Override
    public int del(Object id) {
        return 1;
    }

    @Override
    public int add(Object obj) {
        return 1;
    }

    @Override
    public int update(Object obj) {
        return 1;
    }

    @Override
    public List<?> queryAll() {
        String selectSQL = "select unqid,lname,groval,cstatus from {{?" + DSMConst.TD_MEMBER_LEVEL + "}} where cstatus&1=0";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        MemberLevelVO[] levels = new MemberLevelVO[queryResult.size()];
        baseDao.convToEntity(queryResult, levels, MemberLevelVO.class);
        return Arrays.asList(levels);
    }

    @Override
    public List<?> queryByParams(String[] params) {
        return null;
    }
}
