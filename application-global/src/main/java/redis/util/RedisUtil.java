package redis.util;

import redis.provide.RedisListProvide;
import redis.provide.RedisStringProvide;

public class RedisUtil {

    private static RedisStringProvide stringProvide = new RedisStringProvide();
    private static RedisListProvide listProvide = new RedisListProvide();

    public static RedisStringProvide getStringProvide() {
        return stringProvide;
    }

    public static RedisListProvide getListProvide() {
        return listProvide;
    }

}
