package com.onek.user;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.user.service.MemberImpl;
import com.onek.util.member.MemberEntity;
import redis.IRedisPartCache;
import redis.proxy.CacheProxyInstance;

import java.util.List;

public class MemberModule {

    private static IRedisPartCache memProxy =(IRedisPartCache) CacheProxyInstance.createPartInstance(new MemberImpl());

    @UserPermission(ignore = true)
    public Result addPoint(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int compid = jsonObject.get("compid").getAsInt();
        int point = jsonObject.get("point").getAsInt();
        MemberEntity memberVO = (MemberEntity) memProxy.getId(compid);
        if(memberVO != null){
            int accupoints = memberVO.getAccupoints();
            int balpoints = memberVO.getBalpoints();

            MemberEntity updateMemberVO = new MemberEntity();
            updateMemberVO.setCompid(compid);
            updateMemberVO.setAccupoints(accupoints+ point);
            updateMemberVO.setBalpoints(balpoints + point);
            memProxy.update(compid, updateMemberVO);
        }

        return new Result().success(null);
    }

    @UserPermission(ignore = true)
    public Result reducePoint(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int compid = jsonObject.get("compid").getAsInt();
        int point = jsonObject.get("point").getAsInt();
        MemberEntity memberVO = (MemberEntity) memProxy.getId(compid);
        if(memberVO != null){
            int accupoints = memberVO.getAccupoints();
            int balpoints = memberVO.getBalpoints();

            MemberEntity updateMemberVO = new MemberEntity();
            updateMemberVO.setCompid(compid);
            updateMemberVO.setAccupoints(accupoints);
            updateMemberVO.setBalpoints(balpoints - point);
            memProxy.update(compid, updateMemberVO);
        }

        return new Result().success(null);
    }

    @UserPermission(ignore = true)
    public Result getMember(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int compid = jsonObject.get("compid").getAsInt();
        MemberEntity memberVO = (MemberEntity) memProxy.getId(compid);
        return new Result().success(memberVO);
    }
}
