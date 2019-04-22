package com.onek.util.dict;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.onek.consts.CSTATUS;
import redis.IRedisCache;
import redis.annation.CacheField;
import redis.annation.DictCacheField;
import redis.annation.GetDictWay;
import redis.proxy.CacheProxyInstance;
import util.StringUtils;

import java.lang.reflect.Field;
import java.util.*;

@SuppressWarnings("unchecked")
public class DictStore{

    private static IRedisCache dictProxy =(IRedisCache) CacheProxyInstance.createInstance(new DictUtil());

    public static DictEntity getDictById(int dictc){
        return (DictEntity) dictProxy.getId(dictc);
    }

    public static DictEntity getDictyByCustomc(int customc,String type){
        List<DictEntity> dicts = (List<DictEntity>)dictProxy.queryByParams(new String[]{customc+"", type});
        if(dicts != null && dicts.size() > 0){
            return dicts.get(0);
        }
        return null;
    }

    public static String getDictNameByCustomc(int customc,String type) {
        DictEntity dict = getDictyByCustomc(customc, type);

        return dict == null ? "" : dict.getText();
    }

    public static Object translate(Object obj) throws Exception{
        if(obj != null) {
            Field[] fields = obj.getClass().getDeclaredFields();
            Map<Field, DictCacheField> cacheFieldMap = new HashMap<>();
            for(Field f : fields) {
                DictCacheField cache = f.getDeclaredAnnotation(DictCacheField.class);
                if(cache != null) {
                    cacheFieldMap.put(f, cache);
                }
            }
            Set<Field> fieldKeys = cacheFieldMap.keySet();
            for(Field field : fieldKeys) {
                DictCacheField cacheField = cacheFieldMap.get(field);
                String col = "text";
                String refcol = cacheField.reflectcolumn();

                if(cacheField != null && !StringUtils.isEmpty(refcol)) {
                    field.setAccessible(true);
                    Object val = field.get(obj);
                    Field f = obj.getClass().getDeclaredField(refcol);
                    f.setAccessible(true);
                    GetDictWay dictWay = cacheField.dictWay();
                    DictEntity dictVo = null;
                    if(dictWay == GetDictWay.ID && Integer.parseInt(val.toString())>0){
                        dictVo = (DictEntity)dictProxy.getId(val);
                    }else if(dictWay == GetDictWay.CUSTOMC_AND_TYPE){
                        String type = cacheField.type();
                        if(!StringUtils.isEmpty(type) && Integer.parseInt(val.toString())>0){
                            List<DictEntity> dicts = (List<DictEntity>)dictProxy.queryByParams(new String[]{val.toString(), type});
                            dictVo = dicts != null && dicts.size() > 0 ? dicts.get(0) : null;
                        }

                    }

                    if(dictVo != null){
                        Field ff = dictVo.getClass().getDeclaredField(col);
                        ff.setAccessible(true);
                        Object value = ff.get(dictVo);
                        f.set(obj, value);
                    }

                }


            }

        }

        return obj;
    }

    public static List<?> translate(List<?> list) throws Exception{
        if(list != null && list.size() > 0) {
            Field[] fields = list.get(0).getClass().getDeclaredFields();
            Map<Field, DictCacheField> cacheFieldMap = new HashMap<>();
            for(Field f : fields) {
                DictCacheField cache = f.getDeclaredAnnotation(DictCacheField.class);
                if(cache != null) {
                    cacheFieldMap.put(f, cache);
                }
            }
            Set<Field> fieldKeys = cacheFieldMap.keySet();
            for(Field field : fieldKeys) {
                DictCacheField cacheField = cacheFieldMap.get(field);
                String col = "text";
                String refcol = cacheField.reflectcolumn();

                if(cacheField != null && !StringUtils.isEmpty(refcol)) {
                    for (Object object : list) {
                        field.setAccessible(true);
                        Object val = field.get(object);
                        Field f = object.getClass().getDeclaredField(refcol);
                        f.setAccessible(true);
                        GetDictWay dictWay = cacheField.dictWay();
                        DictEntity dictVo = null;
                        if(dictWay == GetDictWay.ID && Integer.parseInt(val.toString())>0){
                            dictVo = (DictEntity)dictProxy.getId(val);
                        }else if(dictWay == GetDictWay.CUSTOMC_AND_TYPE){
                            String type = cacheField.type();
                            if(!StringUtils.isEmpty(type) && Integer.parseInt(val.toString())>0){
                                List<DictEntity> dicts = (List<DictEntity>)dictProxy.queryByParams(new String[]{val.toString(), type});
                                dictVo = dicts != null && dicts.size() > 0 ? dicts.get(0) : null;
                            }

                        }

                        if(dictVo != null){
                            Field ff = dictVo.getClass().getDeclaredField(col);
                            ff.setAccessible(true);
                            Object value = ff.get(dictVo);
                            f.set(object, value);
                        }
                    }
                }

            }

        }

        return list;
    }

}
