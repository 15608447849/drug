package com.onek.propagation.prod;

import com.onek.util.RedisGlobalKeys;
import redis.provide.RedisStringProvide;
import redis.util.RedisUtil;

import java.util.List;

public class ProdCurrentActPriceObserver implements ProdObserver {

    @Override
    public void update(List<String> list) {
        if (list != null && list.size() > 0) {
            RedisStringProvide rs = RedisUtil.getStringProvide();
            long version = rs.get(RedisGlobalKeys.ACTVERSION) != null ? Long.parseLong(rs.get(RedisGlobalKeys.ACTVERSION)) : 0;
            rs.set(RedisGlobalKeys.ACTVERSION, version + 1);
        }
    }
}
