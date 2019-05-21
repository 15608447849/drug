package com.onek.user.service;

import com.onek.user.entity.ConsigneeVO;
import com.onek.util.RedisGlobalKeys;
import constant.DSMConst;
import dao.BaseDAO;
import redis.IRedisCache;

import java.util.Arrays;
import java.util.List;

/**
 * @author 11842
 * @version 1.1.1
 * @description 我的收货人缓存
 * @time 2019/3/28 11:31
 **/
public class MyConsigneeImpl implements IRedisCache {

    private static BaseDAO baseDao = BaseDAO.getBaseDAO();

    @Override
    public String getPrefix() {
        return RedisGlobalKeys.CONSIGNEE_PREFIX;
    }

    @Override
    public String getKey() {
        return "compid";
    }

    @Override
    public Class<?> getReturnType() {
        return ConsigneeVO.class;
    }

    @Override
    public ConsigneeVO getId(Object id) {
        String selectSQL = "select shipid,compid,contactname,contactphone,cstatus from {{?" + DSMConst.TB_COMP_SHIP_INFO + "}} where cstatus&1=0 "
                + " and copmid=" + id + " and cstatus&2>0";
        List<Object[]> result = baseDao.queryNative(selectSQL);
        ConsigneeVO[] defaultCgs = new ConsigneeVO[result.size()];
        baseDao.convToEntity(result, defaultCgs, ConsigneeVO.class);
        return defaultCgs[0];
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
        String selectSQL = "select shipid,compid,contactname,contactphone,cstatus from {{?"
                + DSMConst.TB_COMP_SHIP_INFO + "}} where cstatus&1=0 and cstatus&2>0";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        ConsigneeVO[] consigneeVOS = new ConsigneeVO[queryResult.size()];
        baseDao.convToEntity(queryResult, consigneeVOS, ConsigneeVO.class);
        return Arrays.asList(consigneeVOS);
    }

    @Override
    public List<?> queryByParams(String[] params) {
        return null;
    }
}
