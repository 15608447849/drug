package redis;

import properties.annotations.PropertiesName;
import redis.annation.CacheInvoke;

import java.util.List;

/**
 * Redis缓存接口
 *
 * @author JiangWenGuang
 * @since 2019-03-07
 * @version 1.0
 *
 */
public interface IRedisCache {

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

    @CacheInvoke(method = "flushCache")
    int del(Object id);

    @CacheInvoke(method = "flushCache")
    int add(Object obj);

    @CacheInvoke(method = "flushCache")
    int update(Object obj);

    @CacheInvoke(method = "loadAllCache")
    List<?> queryAll();

    @CacheInvoke(method = "loadCacheList")
    List<?> queryByParams(String [] params);
}
