package com.onek.user.service;

import com.onek.user.entity.MemberVO;
import constant.DSMConst;
import dao.BaseDAO;
import redis.IRedisPartCache;

import java.util.List;

public class MemberImpl implements IRedisPartCache {

    private static final BaseDAO baseDao = BaseDAO.getBaseDAO();

    private static final String GET_SQL = "select unqid,compid,accupoints,balpoints,createdate,createtime,cstatus from {{?" + DSMConst.TD_MEMBER + "}} where compid = ? and cstatus&1=0";
    private static final String UPDATE_SQL = "update {{?" + DSMConst.TD_MEMBER + "}} set accupoints = ?, balpoints = ? where compid = ?";

    @Override
    public String getPrefix() {
        return "member";
    }

    @Override
    public String getKey() {
        return "compid";
    }

    @Override
    public Class<?> getReturnType() {
        return MemberVO.class;
    }

    @Override
    public Object getId(Object id) {

        List<Object[]> result = baseDao.queryNative(GET_SQL);
        MemberVO[] levels = new MemberVO[result.size()];
        baseDao.convToEntity(result, levels, MemberVO.class);
        return levels[0];
    }

    @Override
    public int del(Object id) {
        return 0;
    }

    @Override
    public int add(Object id, Object obj) {
        return 0;
    }

    @Override
    public int update(Object id, Object obj) {

        MemberVO memberVO = (MemberVO) obj;
        return baseDao.updateNative(UPDATE_SQL, new Object[]{ memberVO.getAccupoints(), memberVO.getBalpoints(), id});
    }
}
