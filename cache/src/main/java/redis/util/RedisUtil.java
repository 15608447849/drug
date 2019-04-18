package redis.util;

import redis.provide.RedisListProvide;
import redis.provide.RedisSetProvide;
import redis.provide.RedisStringProvide;

public class RedisUtil {

    private static RedisStringProvide stringProvide = new RedisStringProvide();
    private static RedisListProvide listProvide = new RedisListProvide();
    private static RedisSetProvide setProvide = new RedisSetProvide();

    public static RedisStringProvide getStringProvide() {
        return stringProvide;
    }

    public static RedisListProvide getListProvide() {
        return listProvide;
    }

    public static RedisSetProvide getSetProvide(){return setProvide;}

}
