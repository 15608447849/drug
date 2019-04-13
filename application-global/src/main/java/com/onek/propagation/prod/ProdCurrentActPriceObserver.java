package com.onek.propagation.prod;

import redis.provide.RedisStringProvide;
import redis.util.RedisUtil;

import java.util.List;

public class ProdCurrentActPriceObserver implements ProdObserver {

    @Override
    public void update(List<String> list) {
        if (list != null && list.size() > 0) {
            RedisStringProvide rs = RedisUtil.getStringProvide();
            String key = "_currprize_version";
            long version = rs.get(key) != null ? Long.parseLong(rs.get(key)) : 0;
            rs.set("_currprize_version", version + 1);
        }
    }
}
