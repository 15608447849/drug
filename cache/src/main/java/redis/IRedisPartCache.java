package redis;

import redis.annation.CacheInvoke;

import java.util.List;

/**
 * Redis部分缓存接口
 * 只会清除某个key对应的缓存
 *
 * @author JiangWenGuang
 * @since 2019-03-07
 * @version 1.0
 *
 */
public interface IRedisPartCache {

    /**
     * Redis存在缓存键的前缀
     *
     * @return
     */
    String getPrefix();

    /**
     * 此字段用来通过反射来获取对象字段值,所以需要与getReturnType()对象字段名一致
     * @return
     */
    String getKey();

    Class<?> getReturnType();

    @CacheInvoke(method = "loadCacheObject")
    Object getId(Object id);

    @CacheInvoke(method = "flushPartCache")
    int del(Object id);

    @CacheInvoke(method = "flushPartCache")
    int add(Object id,Object obj);

    @CacheInvoke(method = "flushPartCache")
    int update(Object id,Object obj);

}
