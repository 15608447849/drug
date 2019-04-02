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

public class DictStore{

    private static IRedisCache dictProxy =(IRedisCache) CacheProxyInstance.createInstance(new DictUtil());

    public  static JSONObject getAllDict(){
        JSONObject j = new JSONObject();
        List<DictEntity> dictList = (List<DictEntity>)dictProxy.queryAll();
        if(dictList != null && dictList.size() > 0){
            Map<String, List<DictEntity>> dictMap = new HashMap<>();
            for(DictEntity dictVo : dictList){
                List<DictEntity> subList = dictMap.get(dictVo.getType());
                if(subList == null) subList = new ArrayList<>();
                if((dictVo.getCstatus() & CSTATUS.DELETE) > 0){
                    continue;
                }
                subList.add(dictVo);
                dictMap.put(dictVo.getType(), subList);
            }

            for (String type : dictMap.keySet()){
                JSONArray dictArr = new JSONArray();
                List<DictEntity> subList = dictMap.get(type);
                for(DictEntity dictVo : subList){
                    JSONObject obj = new JSONObject();
                    obj.put("value", dictVo.getDictc());
                    obj.put("text", dictVo.getText());
                    obj.put("label", dictVo.getText());
                    obj.put("remark", dictVo.getText());
                    obj.put("customc", dictVo.getCustomc());
                    dictArr.add(obj);
                }
                j.put(type, dictArr);
            }
        }
        return j;
    }

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
                    if(dictWay == GetDictWay.ID){
                        dictVo = (DictEntity)dictProxy.getId(val);
                    }else{
                        String type = cacheField.type();
                        if(!StringUtils.isEmpty(type)){
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
                        DictEntity dictVo = (DictEntity)dictProxy.getId(id);
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
