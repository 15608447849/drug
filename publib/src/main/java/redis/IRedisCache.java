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

    @CacheInvoke(method = "loadCacheObject")
    Object getId(Object id);

    @CacheInvoke(method = "clearCache")
    int del(Object id);

    @CacheInvoke(method = "clearCache")
    int add(Object obj);

    @CacheInvoke(method = "clearCache")
    int update(Object obj);

    @CacheInvoke(method = "loadAllCache")
    List<?> queryAll();

    @CacheInvoke(method = "loadCacheList")
    List<?> queryByParams(String [] params);

}
