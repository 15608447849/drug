package com.onek.util.member;

import global.IceRemoteUtil;
import redis.IRedisPartCache;

public class MemberUtil implements IRedisPartCache {
    @Override
    public String getPrefix() {
        return "member";
    }

    @Override
    public String getKey() {
        return "compid";
    }

    @Override
    public Class<?> getReturnType() {
        return MemberEntity.class;
    }

    @Override
    public Object getId(Object id) {
        if(id == null){
            return null;
        }
        return IceRemoteUtil.getMemberByCompid(Integer.parseInt(id.toString()));
    }

    @Override
    public int del(Object id) {
        return 0;
    }

    @Override
    public int add(Object id, Object obj) {
        return 0;
    }

    @Override
    public int update(Object id, Object obj) {
        return 0;
    }
}
