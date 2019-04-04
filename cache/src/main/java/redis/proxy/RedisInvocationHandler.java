package redis.proxy;

import redis.IRedisCache;
import redis.annation.CacheInvoke;
import redis.provide.RedisListProvide;
import redis.provide.RedisStringProvide;
import util.GsonUtils;
import util.StringUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
public class RedisInvocationHandler<T> implements InvocationHandler {

    public static String PREFIX_ALL  = "@a";
    public static String PREFIX_PARAMS  = "@p";

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
        CacheInvoke cacheInvoke =method.getAnnotation(CacheInvoke.class);
        if(cacheInvoke == null) {
            return method.invoke(target, args);
        }

        String m = cacheInvoke.method();
        Method invokeMethod = getClass().getDeclaredMethod(m, Method.class, Object.class, Object[].class);
        return invokeMethod.invoke(this, method, target, args);

	}

	private Object loadCacheObject(Method method, Object target,Object[] args) throws IllegalAccessException, InvocationTargetException {
		Class<?> clazz = null;
		String type = null;

        String keycolum = "";
        IRedisCache t = (IRedisCache)(target);
        keycolum = t.getKey();
        String prefix = "";
        prefix = t.getPrefix();
        clazz = t.getReturnType();

		Object cacheObj = null;
		String keyval = "";
		if(!StringUtils.isEmpty(prefix) && !StringUtils.isEmpty(keycolum)) {

            Object arg = args[0];
            String val = stringProvide.get(prefix + arg.toString());
            keyval = arg.toString();
            if(!StringUtils.isEmpty(val)) {
                cacheObj = GsonUtils.jsonToJavaBean(val, clazz);
            }

		}
		if(cacheObj == null) {
			Object result = method.invoke(target, args);
			if (result != null) {
                if (result.getClass().getName().equals(clazz.getName())) {
                    String val = GsonUtils.javaBeanToJson(result);
                    if(!StringUtils.isEmpty(prefix) && !StringUtils.isEmpty(keycolum)) {
                        stringProvide.set(prefix + keyval, val);

                    }
                }

			}
			return result;
		}else {
			System.out.println("#### loadCacheObject cahce hit!!!");
			return cacheObj;

		}
	}

    private Object loadAllCache(Method method, Object target,Object[] args) throws IllegalAccessException, InvocationTargetException {

        Class<?> clazz = null;
        String type = null;

        String keycolum = "";
        IRedisCache t = (IRedisCache)(target);
        keycolum = t.getKey();
        String prefix = "";
        prefix = t.getPrefix();
        clazz = t.getReturnType();

        Object cacheObj = null;
        if(!StringUtils.isEmpty(prefix) && !StringUtils.isEmpty(keycolum)) {
            List<String> list = listProvide.getAllElements(prefix + PREFIX_ALL);
            List<Object> cacheList = new ArrayList<>();
            for(String val : list){
                if(!StringUtils.isEmpty(val)) {
                    Object Obj = GsonUtils.jsonToJavaBean(val, clazz);
                    cacheList.add(Obj);
                }
            }
            cacheObj = cacheList.size() <=0  ? null  :cacheList;

        }
        if(cacheObj == null) {
            Object result = method.invoke(target, args);
            if (result != null) {
                if(result instanceof List){
                    List<Object> resultList = (List<Object>)result;
                    if(resultList.get(0).getClass().getName().equals(clazz.getName())){
                        listProvide.delete( prefix + PREFIX_ALL);
                        boolean issuccess = true;
                        for(Object obj : resultList){
                            String val = GsonUtils.javaBeanToJson(obj);
                            Long r = listProvide.addEndElement(prefix+ PREFIX_ALL, val);
                            if(r <= 0){
                                issuccess = false;
                                break;
                            }
                        }
                        if(!issuccess){
                            listProvide.delete( prefix + PREFIX_ALL);
                        }

                    }
                }

            }
            return result;
        }else {
            System.out.println("#### loadAllCache cahce hit!!!");
            return cacheObj;

        }
    }

    private Object loadCacheList(Method method, Object target,Object[] args) throws IllegalAccessException, InvocationTargetException,NoSuchMethodException {

        Class<?> clazz = null;
        Object cacheObj = null;
        String [] strings = (String []) args[0];

        String keycolum = "";
        IRedisCache t = (IRedisCache)(target);
        keycolum = t.getKey();
        String prefix = "";
        prefix = t.getPrefix();
        clazz = t.getReturnType();

        String key = String.join(",", strings);
        String hash = md5(key);
        System.out.println("##### hash: "+hash + "#######");

        if(!StringUtils.isEmpty(prefix) && !StringUtils.isEmpty(keycolum)) {

            List<String> list = listProvide.getAllElements(prefix + PREFIX_PARAMS +hash);
            List<Object> cacheList = new ArrayList<>();
            for(String val : list){
                if(!StringUtils.isEmpty(val)) {
                    Object Obj = GsonUtils.jsonToJavaBean(val, clazz);
                    cacheList.add(Obj);
                }
            }
            cacheObj = cacheList.size() <=0  ? null  :cacheList;

        }
        if(cacheObj == null) {
            Object result = method.invoke(target, args);
            if (result != null) {
                if(result instanceof List){
                    if(result instanceof List){
                        List<Object> resultList = (List<Object>)result;

                        listProvide.delete( prefix + PREFIX_PARAMS + hash);
                        boolean issuccess = true;
                        for(Object obj : resultList){
                            String val = GsonUtils.javaBeanToJson(obj);
                            Long r = listProvide.addEndElement(prefix + PREFIX_PARAMS + hash, val);
                            if(r <= 0){
                                issuccess = false;
                                break;
                            }
                        }
                        if(!issuccess){
                            listProvide.delete( prefix + PREFIX_PARAMS + hash);
                        }
                    }
                }

            }
            return result;
        }else {
            System.out.println("#### loadCacheList cahce hit!!!");
            return cacheObj;

        }
    }

	private Object flushCache(Method method, Object target,Object[] args) throws IllegalAccessException, InvocationTargetException {
		Object result = method.invoke(target, args);
		if (result != null) {
			Integer num = (Integer)result;
			if(num > 0) {

                String keycolum = "";
                IRedisCache t = (IRedisCache)(target);
                keycolum = t.getKey();
                String prefix = "";
                prefix = t.getPrefix();

				if(!StringUtils.isEmpty(prefix) && !StringUtils.isEmpty(keycolum)) {
				    stringProvide.deleteRedisKeyStartWith(prefix);

				}
			}
			return result;
		}
		return null;
	}

    /**
     * 使用md5的算法进行加密(具体根据需求)
     *
     */
    public static String md5(String plainText) {
        byte[] secretBytes = null;
        try {
            secretBytes = MessageDigest.getInstance("md5").digest(
                    plainText.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("没有md5这个算法！");
        }
        String md5code = new BigInteger(1, secretBytes).toString(16);// 16进制数字
        // 如果生成数字未满32位，需要前面补0
        for (int i = 0; i < 32 - md5code.length(); i++) {
            md5code = "0" + md5code;
        }
        return md5code;
    }

}
