package com.onek.user;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.user.service.MemberImpl;
import com.onek.util.member.MemberEntity;
import constant.DSMConst;
import dao.BaseDAO;
import redis.IRedisPartCache;
import redis.proxy.CacheProxyInstance;

import java.util.List;

public class MemberModule {

    private static final BaseDAO baseDao = BaseDAO.getBaseDAO();

    private static final String GET_SQL = "select unqid,compid,accupoints,balpoints,expirepoint,cstatus from {{?" + DSMConst.TD_MEMBER + "}} where compid = ? and cstatus&1=0";
    private static final String UPDATE_SQL = "update {{?" + DSMConst.TD_MEMBER + "}} set accupoints = ?, balpoints = ? where compid = ?";

//    private static IRedisPartCache memProxy =(IRedisPartCache) CacheProxyInstance.createPartInstance(new MemberImpl());

    @UserPermission(ignore = true)
    public Result addPoint(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int compid = jsonObject.get("compid").getAsInt();
        int point = jsonObject.get("point").getAsInt();
        MemberEntity memberVO = (MemberEntity) getId(compid);
        if(memberVO != null){
            int accupoints = memberVO.getAccupoints();
            int balpoints = memberVO.getBalpoints();

            MemberEntity updateMemberVO = new MemberEntity();
            updateMemberVO.setCompid(compid);
            updateMemberVO.setAccupoints(accupoints+ point);
            updateMemberVO.setBalpoints(balpoints + point);
            update(compid, updateMemberVO);
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
        MemberEntity memberVO = (MemberEntity) getId(compid);
        if(memberVO != null){
            int accupoints = memberVO.getAccupoints();
            int balpoints = memberVO.getBalpoints();

            MemberEntity updateMemberVO = new MemberEntity();
            updateMemberVO.setCompid(compid);
            updateMemberVO.setAccupoints(accupoints);
            updateMemberVO.setBalpoints(balpoints - point);
            update(compid, updateMemberVO);
        }

        return new Result().success(null);
    }

    @UserPermission(ignore = true)
    public Result getMember(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int compid = jsonObject.get("compid").getAsInt();
        MemberEntity memberVO = (MemberEntity) getId(compid);
        return new Result().success(memberVO);
    }

    public Object getId(Object id) {

        List<Object[]> result = baseDao.queryNative(GET_SQL, new Object[]{ id});
        MemberEntity[] levels = new MemberEntity[result.size()];
        if(levels != null && levels.length > 0){
            baseDao.convToEntity(result, levels, MemberEntity.class);
            return levels[0];
        }
        return null;
    }

    public int update(Object id, Object obj) {

        MemberEntity memberVO = (MemberEntity) obj;
        return baseDao.updateNative(UPDATE_SQL, new Object[]{ memberVO.getAccupoints(), memberVO.getBalpoints(), id});
    }
}
