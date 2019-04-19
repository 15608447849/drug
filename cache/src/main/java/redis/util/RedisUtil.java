package redis.util;

import redis.provide.RedisHashProvide;
import redis.provide.RedisListProvide;
import redis.provide.RedisSetProvide;
import redis.provide.RedisStringProvide;

public class RedisUtil {

    private static RedisStringProvide stringProvide = new RedisStringProvide();
    private static RedisListProvide listProvide = new RedisListProvide();
    private static RedisSetProvide setProvide = new RedisSetProvide();
    private static RedisHashProvide redisHashProvide = new RedisHashProvide();

    public static RedisStringProvide getStringProvide() {
        return stringProvide;
    }

    public static RedisListProvide getListProvide() {
        return listProvide;
    }

    public static RedisSetProvide getSetProvide(){return setProvide;}

    public static RedisHashProvide getHashProvide(){return redisHashProvide;}

}
