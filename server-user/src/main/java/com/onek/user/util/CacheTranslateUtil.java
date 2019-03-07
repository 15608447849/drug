package com.onek.user.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.onek.user.entity.DictVo;
import com.onek.user.service.DictServiceImpl;

import redis.IRedisCache;
import redis.annation.CacheField;
import redis.proxy.CacheProxyInstance;

public class CacheTranslateUtil {
	
	private static IRedisCache dictProxy =(IRedisCache) CacheProxyInstance.createInstance(new DictServiceImpl());
	
	public static List<?> translate(List<?> list) throws Exception{
		if(list != null && list.size() > 0) {
			Field[] fields = list.get(0).getClass().getDeclaredFields();
			Map<Field, CacheField> cacheFieldMap = new HashMap<>();
			for(Field f : fields) {
				CacheField cache = f.getDeclaredAnnotation(CacheField.class);
				if(cache != null) {
					cacheFieldMap.put(f, cache);
				}
			}
			Set<Field> fieldKeys = cacheFieldMap.keySet();
			for(Field field : fieldKeys) {
				CacheField cacheField = cacheFieldMap.get(field);
				String col = cacheField.cachecolumn();
				String refcol = cacheField.reflectcolumn();
				String key = cacheField.key();
				String prefix = cacheField.prefix();
				

				for (Object object : list) {
					field.setAccessible(true);
					Object id = field.get(object);
					Field f = object.getClass().getDeclaredField(refcol);
					f.setAccessible(true);
					if(prefix.equals("dict_")) {
						DictVo dictVo = (DictVo)dictProxy.getId(id);
						Field ff = dictVo.getClass().getDeclaredField(col);
						ff.setAccessible(true);
						Object value = ff.get(dictVo);
						f.set(object, value);
					}
				}
			}
			
		}
		
		return list;
	}
}
