package redis.proxy;

import redis.annation.RedisCache;
import redis.annation.RedisKey;
import redis.provide.RedisStringProvide;
import util.GsonUtils;
import util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;


public class RedisInvocationHandler<T> implements InvocationHandler {
	
	private static RedisStringProvide stringProvide = new RedisStringProvide();
	
	T target;

	public RedisInvocationHandler(T target) {
		this.target = target;
	}

	/**
	 * proxy:代表动态代理对象 method：代表正在执行的方法 args：代表调用目标方法时传入的实参
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

		System.out.println("############ redis inocation invoke ###############");
		boolean isEmpty = target.getClass().isAnnotationPresent(RedisCache.class);
		if(!isEmpty) {
			return method.invoke(target, args);
		}
		if(method.getName().equals("getId") || method.getName().equals("queryAll") || method.getName().equals("queryByParams")) {
			System.out.println(isEmpty);
			Annotation[] annotation = null;
			Class<?> clazz = null;
			String type = null;
			if (isEmpty) {
				annotation = target.getClass().getAnnotations();// 获取注解接口中的
				for (Annotation a : annotation) {
					RedisCache my = (RedisCache) a;// 强制转换成RedisCache类型
					clazz = my.clazz();
					type = my.type();
				}
			}
			String keycolum = "";
			String prefix = "";
			if(clazz != null) {
				Annotation[] anns = clazz.getAnnotations();
				System.out.println("###size:"+anns.length);
				RedisKey key = clazz.getDeclaredAnnotation(RedisKey.class);
				System.out.println("### key: "+key);
				prefix = key.prefix();
				keycolum = key.key();
			}
			Object cacheObj = null;
			String keyval = "";
			if(!StringUtils.isEmpty(prefix) && !StringUtils.isEmpty(keycolum)) {
				if(type.equals("string")) {
					 Object arg = args[0];
					 if(arg instanceof Integer) {
						 String val = stringProvide.get(prefix + arg.toString());
						 keyval = arg.toString();
						 if(!StringUtils.isEmpty(val)) {
							 cacheObj = GsonUtils.jsonToJavaBean(val, clazz);
						 }
						 
					 }
				}
			}
			if(cacheObj == null) {
				Object result = method.invoke(target, args);
				if (result != null) {
					if (result.getClass().getName().equals(clazz.getName())) {
						String val = GsonUtils.javaBeanToJson(result);
						if(!StringUtils.isEmpty(prefix) && !StringUtils.isEmpty(keycolum)) {
							if(type.equals("string")) {
								stringProvide.set(prefix + keyval, val);
							}
						}
					}
				}
				return result;
			}else {
				System.out.println("cahce hit!!!");
				return cacheObj;

			}
			
		}else if(method.getName().equals("del") || method.getName().equals("add") || method.getName().equals("update")) {
			Object result = method.invoke(target, args);
			if (result != null) {
				Integer num = (Integer)result;
				if(num > 0) {
					Annotation[] annotation = null;
					Class<?> clazz = null;
					String type = null;
					if (isEmpty) {
						annotation = target.getClass().getAnnotations();// 获取注解接口中的
						for (Annotation a : annotation) {
							RedisCache my = (RedisCache) a;// RedisCache
							clazz = my.clazz();
							type = my.type();
						}
					}
					String keycolum = "";
					String prefix = "";
					if(clazz != null) {
						Annotation[] anns = clazz.getAnnotations();
						System.out.println("###size:"+anns.length);
						RedisKey key = clazz.getDeclaredAnnotation(RedisKey.class);
						System.out.println("### key: "+key);
						prefix = key.prefix();
						keycolum = key.key();
					}
					if(!StringUtils.isEmpty(prefix) && !StringUtils.isEmpty(keycolum)) {
						if(type.equals("string")) {
							 stringProvide.deleteRedisKeyStartWith(prefix);
						}
					}
				}
				return result;
			}
		}else {
			Object result = method.invoke(target, args);
			return result;
		}
		
		return null;
		
	}
}
