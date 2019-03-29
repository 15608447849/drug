package com.onek.user;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.onek.consts.CSTATUS;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.user.entity.DictVo;
import com.onek.user.service.DictServiceImpl;
import com.onek.util.area.AreaStore;
import redis.IRedisCache;
import redis.proxy.CacheProxyInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommonModule {

    private static IRedisCache dictProxy = CacheProxyInstance.createInstance(new DictServiceImpl());

    public Result getAreas(AppContext appContext) {
        return new Result().success(AreaStore.getTreeJson());
    }

    public Result getDicts(AppContext appContext){
        JSONObject j = new JSONObject();
        List<DictVo> dictList = (List<DictVo>)dictProxy.queryAll();
        if(dictList != null && dictList.size() > 0){
            Map<String, List<DictVo>> dictMap = new HashMap<>();
            for(DictVo dictVo : dictList){
                List<DictVo> subList = dictMap.get(dictVo.getType());
                if(subList == null) subList = new ArrayList<>();
                if((dictVo.getCstatus() & CSTATUS.DELETE) > 0){
                    continue;
                }
                subList.add(dictVo);
            }

            for (String type : dictMap.keySet()){
                JSONArray dictArr = new JSONArray();
                List<DictVo> subList = dictMap.get(type);
                for(DictVo dictVo : subList){
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
        return new Result().success(j.toJSONString());
    }
}
