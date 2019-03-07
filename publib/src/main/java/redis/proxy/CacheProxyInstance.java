package redis.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import redis.IRedisCache;

public class CacheProxyInstance {
	
	public static IRedisCache createInstance(Object target) {
		InvocationHandler inv = new RedisInvocationHandler<>(target);
		IRedisCache proxy = (IRedisCache)Proxy.newProxyInstance(IRedisCache.class.getClassLoader(), new Class<?>[] {IRedisCache.class}, inv);
		return proxy;
	}
}
