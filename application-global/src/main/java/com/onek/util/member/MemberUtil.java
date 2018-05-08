package com.onek.util.member;

import com.onek.util.IceRemoteUtil;
import com.onek.util.RedisGlobalKeys;
import redis.IRedisPartCache;

public class MemberUtil implements IRedisPartCache {
    @Override
    public String getPrefix() {
        return RedisGlobalKeys.MEMBER_PREFIX;
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
        return IceRemoteUtil.addPoint((int)id, (int)obj);
    }

    @Override
    public int update(Object id, Object obj) {
        return IceRemoteUtil.reducePoint((int)id, (int)obj);
    }

}
