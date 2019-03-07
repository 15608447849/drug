package redis;

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
    Object getId(Object id);

    int del(Object id);

    int add(Object obj);

    int update(Object obj);

    List<Object> queryAll();

    List<Object> queryByParams(Object... params);

}
