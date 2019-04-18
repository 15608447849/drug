package com.onek.user;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.user.entity.MemberVO;
import constant.DSMConst;
import dao.BaseDAO;
import util.GsonUtils;

import java.util.List;

public class MemberModule {

    private static final BaseDAO baseDao = BaseDAO.getBaseDAO();

    private static final String GET_SQL = "select accupoints,balpoints from {{?" + DSMConst.TD_MEMBER + "}} where compid = ?";
    private static final String UPDATE_SQL = "update {{?" + DSMConst.TD_MEMBER + "}} set accupoints = ?, balpoints = ? where compid = ?";

    @UserPermission(ignore = true)
    public Result addPoint(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int compid = jsonObject.get("compid").getAsInt();
        int point = jsonObject.get("point").getAsInt();
        List<Object[]> list = baseDao.queryNative(GET_SQL, new Object[]{ compid});
        if(list != null && list.size() > 0){
            int accupoints = Integer.parseInt(list.get(0)[0].toString());
            int balpoints = Integer.parseInt(list.get(0)[1].toString());
            baseDao.updateNative(UPDATE_SQL, new Object[]{ accupoints+ point, balpoints + point, compid});
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
        List<Object[]> list = baseDao.queryNative(GET_SQL, new Object[]{ compid});
        if(list != null && list.size() > 0){
            int accupoints = Integer.parseInt(list.get(0)[0].toString());
            int balpoints = Integer.parseInt(list.get(0)[1].toString());
            baseDao.updateNative(UPDATE_SQL, new Object[]{ accupoints, balpoints - point, compid});
        }

        return new Result().success(null);
    }
}
