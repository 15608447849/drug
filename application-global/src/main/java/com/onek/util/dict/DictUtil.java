package com.onek.util.dict;

import global.IceRemoteUtil;
import redis.IRedisCache;

import java.util.Arrays;
import java.util.List;

public class DictUtil implements IRedisCache {

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
        return IceRemoteUtil.getId(id);
    }

    @Override
    public List<?> queryAll() {
        DictEntity[] dicts = IceRemoteUtil.queryAll();
        return Arrays.asList(dicts	);
    }

    @Override
    public List<?> queryByParams(String [] params) {
        DictEntity[] dicts = IceRemoteUtil.queryByParams(params);
        return Arrays.asList(dicts	);
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


}
