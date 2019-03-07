package org.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import sql.Cache;
public class ProxyInstance {
	
	public static Cache createInstance(Object target) {
		InvocationHandler inv = new RedisInvocationHandler<>(target);
		Cache proxy = (Cache)Proxy.newProxyInstance(Cache.class.getClassLoader(), new Class<?>[] {Cache.class}, inv);
		return proxy;
	}
}
