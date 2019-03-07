package redis.proxy;

import redis.annation.RedisCache;
import redis.annation.RedisKey;
import redis.provide.RedisListProvide;
import redis.provide.RedisStringProvide;
import util.GsonUtils;
import util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;


public class RedisInvocationHandler<T> implements InvocationHandler {
	
	private static RedisStringProvide stringProvide = new RedisStringProvide();
	private static RedisListProvide listProvide = new RedisListProvide();
	
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
			return loadData(method, args, isEmpty);

		}else if(method.getName().equals("del") || method.getName().equals("add") || method.getName().equals("update")) {
			Object result = flushCache(method, args, isEmpty);
			if (result != null) return result;
		}else {
			Object result = method.invoke(target, args);
			return result;
		}
		
		return null;
		
	}

	private Object loadData(Method method, Object[] args, boolean isEmpty) throws IllegalAccessException, InvocationTargetException {
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
			RedisKey key = clazz.getDeclaredAnnotation(RedisKey.class);
			if(key != null) {
				System.out.println("### key: "+key);
				prefix = key.prefix();
				keycolum = key.key();
			}
		}
		Object cacheObj = null;
		String keyval = "";
		if(!StringUtils.isEmpty(prefix) && !StringUtils.isEmpty(keycolum)) {
			if(method.getName().equals("getId")){
				if(type.equals("string")) {
					Object arg = args[0];
					String val = stringProvide.get(prefix + arg.toString());
					keyval = arg.toString();
					if(!StringUtils.isEmpty(val)) {
						cacheObj = GsonUtils.jsonToJavaBean(val, clazz);
					}

				}
			}else if(method.getName().equals("queryAll")){
				List<String> list = listProvide.getAllElements(prefix + "all");
				List<Object> cacheList = new ArrayList<>();
				for(String val : list){
					if(!StringUtils.isEmpty(val)) {
						Object Obj = GsonUtils.jsonToJavaBean(val, clazz);
						cacheList.add(Obj);
					}
				}
				cacheObj = cacheList;
			}else if(method.getName().equals("queryByParams")){
				long hash = getCrc32(args[0].toString());
				List<String> list = listProvide.getAllElements(prefix + "par_"+hash);
				List<Object> cacheList = new ArrayList<>();
				for(String val : list){
					if(!StringUtils.isEmpty(val)) {
						Object Obj = GsonUtils.jsonToJavaBean(val, clazz);
						cacheList.add(Obj);
					}
				}
				cacheObj = cacheList;
			}

		}
		if(cacheObj == null) {
			Object result = method.invoke(target, args);
			if (result != null) {
				if(method.getName().equals("getId")){
					if (result.getClass().getName().equals(clazz.getName())) {
						String val = GsonUtils.javaBeanToJson(result);
						if(!StringUtils.isEmpty(prefix) && !StringUtils.isEmpty(keycolum)) {
							if(type.equals("string")) {
								stringProvide.set(prefix + keyval, val);
							}
						}
					}else if(result instanceof List){
						List<Object> resultList = (List<Object>)result;
						if(resultList.get(0).getClass().getName().equals(clazz.getName())){
							if(method.getName().equals("queryAll")){
								listProvide.delete( prefix + "all");
								boolean issuccess = true;
								for(Object obj : resultList){
									String val = GsonUtils.javaBeanToJson(obj);
									Long r = listProvide.addEndElement(prefix+"all", val);
									if(r <= 0){
										issuccess = false;
										break;
									}
								}
								if(!issuccess){
									listProvide.delete( prefix + "all");
								}

							}else if(method.getName().equals("queryByParams")){
								long hash = getCrc32(args[0].toString());
								listProvide.delete( prefix + "par_" + hash);
								boolean issuccess = true;
								for(Object obj : resultList){
									String val = GsonUtils.javaBeanToJson(obj);
									Long r = listProvide.addEndElement(prefix +"par_" + hash, val);
									if(r <= 0){
										issuccess = false;
										break;
									}
								}
								if(!issuccess){
									listProvide.delete( prefix + "par_" + hash);
								}
							}
						}
					}
				}

			}
			return result;
		}else {
			System.out.println("cahce hit!!!");
			return cacheObj;

		}
	}

	private Object flushCache(Method method, Object[] args, boolean isEmpty) throws IllegalAccessException, InvocationTargetException {
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
					RedisKey key = clazz.getDeclaredAnnotation(RedisKey.class);
					if(key != null) {
						System.out.println("### key: "+key);
						prefix = key.prefix();
						keycolum = key.key();
					}
				}
				if(!StringUtils.isEmpty(prefix) && !StringUtils.isEmpty(keycolum)) {
					if(type.equals("string")) {
						 stringProvide.deleteRedisKeyStartWith(prefix);
					}
				}
			}
			return result;
		}
		return null;
	}

	/**
	 * getHashCode
	 * @param source
	 * @return
	 */
	public synchronized static long getCrc32(String source) {
		CRC32 crc32 = new CRC32();
		crc32.update(source.getBytes());
		return crc32.getValue();
	}
}
