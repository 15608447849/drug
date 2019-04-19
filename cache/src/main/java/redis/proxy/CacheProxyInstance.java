package redis.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import redis.IRedisCache;
import redis.IRedisPartCache;

public class CacheProxyInstance {
	
	public static IRedisCache createInstance(IRedisCache target) {
		InvocationHandler inv = new RedisInvocationHandler<>(target);
		IRedisCache proxy = (IRedisCache)Proxy.newProxyInstance(IRedisCache.class.getClassLoader(), new Class<?>[] {IRedisCache.class}, inv);
		return proxy;
	}

	public static IRedisPartCache createPartInstance(IRedisPartCache target) {
		InvocationHandler inv = new RedisInvocationHandler<>(target);
		IRedisPartCache proxy = (IRedisPartCache)Proxy.newProxyInstance(IRedisCache.class.getClassLoader(), new Class<?>[] {IRedisPartCache.class}, inv);
		return proxy;
	}
}
