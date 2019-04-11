package com.onek.user;

import com.alibaba.fastjson.JSONObject;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.user.entity.MemberLevelVO;
import com.onek.user.service.MemberLevelImpl;
import redis.IRedisCache;
import redis.proxy.CacheProxyInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jiangwg
 *  * @version 1.1.1
 *  * @description 会员等级
 *  * @time 2019/4/3 10:40
 */
public class BackgroundMemberModule {

    private static IRedisCache memLevelProxy =(IRedisCache) CacheProxyInstance.createInstance(new MemberLevelImpl());

    @UserPermission(ignore = true)
    public Result queryAllMemberLevel(AppContext appContext) {
        List<MemberLevelVO> levels = (List<MemberLevelVO>)memLevelProxy.queryAll();
        List<JSONObject> results = new ArrayList<>();
        if(levels != null && levels.size() > 0){
            for(MemberLevelVO levelVo : levels){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("value", levelVo.getUnqid());
                jsonObject.put("label", levelVo.getLname());
                results.add(jsonObject);
            }
        }
        return new Result().success(results);
    }

}
