package com.onek.user;

import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.user.entity.MemberVO;
import constant.DSMConst;
import dao.BaseDAO;
import util.GsonUtils;

import java.util.List;

import static dao.SynDbLog.queryNative;

public class MemberModule {

    private static final BaseDAO baseDao = BaseDAO.getBaseDAO();

    private static final String GET_SQL = "select accupoints,balpoints from {{?" + DSMConst.TD_MEMBER + "}} where compid = ?";
    private static final String UPDATE_SQL = "update {{?" + DSMConst.TD_MEMBER + "}} set accupoints = ?, balpoints = ? where compid = ?";

    @UserPermission(ignore = true)
    public Result addPoint(AppContext appContext) {
        String json = appContext.param.json;
        MemberVO memberVO = GsonUtils.jsonToJavaBean(json, MemberVO.class);
        List<Object[]> list = baseDao.queryNative(GET_SQL, new Object[]{ memberVO.getCompid()});
        if(list != null && list.size() > 0){
            int accupoints = Integer.parseInt(list.get(0)[0].toString());
            int balpoints = Integer.parseInt(list.get(0)[1].toString());
            baseDao.updateNative(UPDATE_SQL, new Object[]{ accupoints+ memberVO.getAccupoints(), balpoints + memberVO.getBalpoints(), memberVO.getCompid()});
        }

        return new Result().success(null);
    }

    @UserPermission(ignore = true)
    public Result reducePoint(AppContext appContext) {
        String json = appContext.param.json;
        MemberVO memberVO = GsonUtils.jsonToJavaBean(json, MemberVO.class);
        List<Object[]> list = baseDao.queryNative(GET_SQL, new Object[]{ memberVO.getCompid()});
        if(list != null && list.size() > 0){
            int accupoints = Integer.parseInt(list.get(0)[0].toString());
            int balpoints = Integer.parseInt(list.get(0)[1].toString());
            baseDao.updateNative(UPDATE_SQL, new Object[]{ accupoints+ memberVO.getAccupoints(), balpoints - memberVO.getBalpoints(), memberVO.getCompid()});
        }

        return new Result().success(null);
    }
}
