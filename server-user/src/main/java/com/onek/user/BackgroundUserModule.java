package com.onek.user;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.AppContext;
import com.onek.entitys.Result;
import com.onek.server.inf.IRequest;
import com.onek.user.entity.UserInfoVo;
import constant.DSMConst;
import dao.BaseDAO;
import redis.util.RedisUtil;
import sun.security.provider.MD5;
import util.EncryptUtils;
import util.GsonUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * @author cyq
 * @version 1.1.1
 * @description 用户操作
 * @time 2019/3/18 15:04
 **/
public class BackgroundUserModule {
    private static final BaseDAO baseDao = BaseDAO.getBaseDAO();

    public Result insertOrUpdUser(AppContext appContext) {
        String json = appContext.param.json;
        UserInfoVo userInfoVo = GsonUtils.jsonToJavaBean(json, UserInfoVo.class);
        if (userInfoVo != null) {
            if (checkUser(userInfoVo)) return new Result().fail("该用户已存在！");
            if (userInfoVo.getUphone() <= 0 || userInfoVo.getUpw() == null || userInfoVo.getUpw().isEmpty()) {
                return new Result().fail("参数错误！");
            }
            int code = 0;
            if (userInfoVo.getUid() <= 0) {
                String insertSQL = "insert into {{?" + DSMConst.D_SYSTEM_USER + "}} "
                        + "(uid,uphone,uaccount,urealname,upw,roleid,adddate,addtime)"
                        + " values (?,?,?,?,?,?,CURRENT_DATE,CURRENT_TIME)";

                String pwd = EncryptUtils.encryption(String.valueOf(userInfoVo.getUphone()).substring(5));
                code = baseDao.updateNative(insertSQL, RedisUtil.getStringProvide().increase("USER_TAB_UID"),
                        userInfoVo.getUphone(), userInfoVo.getUaccount(), userInfoVo.getUrealname(),
                        pwd, userInfoVo.getRoleid());
            } else {
                String updSQL = "update {{?" + DSMConst.D_SYSTEM_USER + "}} set uphone=?,uaccount=?,"
                        + "urealname=?, roleid=? where cstatus&1=0 and uid=? ";
                code = baseDao.updateNative(updSQL, userInfoVo.getUphone(),userInfoVo.getUaccount(),
                        userInfoVo.getUrealname(),userInfoVo.getRoleid(),userInfoVo.getUid());
            }
            if (code > 0) {
                return new Result().success("操作成功");
            }
        }
        return new Result().fail("用户操作失败！");
    }

    private boolean checkUser(UserInfoVo userInfoVo) {
        StringBuilder sqlBuilder = new StringBuilder();
        String sql = "select count(*) from {{?" + DSMConst.D_SYSTEM_USER + "}} where uphone=?" +
                " and cstatus&1=0 ";
        sqlBuilder.append(sql);
        if (userInfoVo.getUid() > 0) {
            sqlBuilder.append(" and uid<>").append(userInfoVo.getUid());
        }
        List<Object[]> queryResult = baseDao.queryNative(sqlBuilder.toString(), userInfoVo.getUphone());
        int count = Integer.parseInt(String.valueOf(queryResult.get(0)[0]));
        return count > 0;
    }

    public Result cancelOrFrozenUser(AppContext appContext){
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int type = jsonObject.get("type").getAsInt();
        int uid = jsonObject.get("uid").getAsInt();
        String updateSql = "update {{?" + DSMConst.D_SYSTEM_USER + "}} set cstatus=cstatus&~32 "
                + " where cstatus&1=0 and cstatus&32>0 and uid=?";//启用
        if (type == 0) {//停用
            updateSql = "update {{?" + DSMConst.D_SYSTEM_USER + "}} set cstatus=cstatus|32 "
                    + " where cstatus&1=0 and cstatus&32=0 and uid=?";
        }
        int code = baseDao.updateNative(updateSql, uid);
        if (code > 0) {
            return result.success("操作成功");
        }
        return result.fail("操作失败");
    }

    public Result queryUsers(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int pageSize = jsonObject.get("pageSize").getAsInt();
        int pageIndex = jsonObject.get("pageIndex").getAsInt();
        Page page = new Page();
        page.pageSize = pageSize;
        page.pageIndex = pageIndex;
        PageHolder pageHolder = new PageHolder(page);
        Result result = new Result();
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder = getgetParamsDYSQL(sqlBuilder, jsonObject);
        String selectSQL = "select uid,uphone,uaccount,urealname,upw,u.roleid,u.adddate,u.addtime"
                + ",u.offdate,u.offtime,ip,logindate,logintime,u.cstatus, rname from {{?"
                + DSMConst.D_SYSTEM_USER + "}} u left join {{?" + DSMConst.D_SYSTEM_ROLE + "}} r "
                + " on u.roleid=r.roleid and u.cstatus&1=0";
        sqlBuilder.append(selectSQL);
        List<Object[]> queryResult = baseDao.queryNative(pageHolder, page, sqlBuilder.toString());
        if (queryResult == null || queryResult.isEmpty()) return result;
        UserInfoVo[] userInfoVos = new UserInfoVo[queryResult.size()];
        baseDao.convToEntity(queryResult, userInfoVos, UserInfoVo.class);
        return result.setQuery(userInfoVos,pageHolder);
    }

    private StringBuilder getgetParamsDYSQL(StringBuilder sqlBuilder, JsonObject jsonObject) {
        String uname = jsonObject.get("uaccount").getAsString();
        String urealname = jsonObject.get("urealname").getAsString();
        int roleid = jsonObject.get("roleid").getAsInt();
        long uphone = jsonObject.get("uphone").getAsLong();
        int state = jsonObject.get("cstatus").getAsInt();
        if (uname != null && !uname.isEmpty()) {
            sqlBuilder.append(" and uaccount like '%").append(uname).append("%'");
        }
        if (urealname != null && !urealname.isEmpty()) {
            sqlBuilder.append(" and urealname like '%").append(urealname).append("%'");
        }
        if (roleid > 0) {
            sqlBuilder.append(" and u.roleid=").append(roleid);
        }
        if (uphone > 0) {
            sqlBuilder.append(" and uphone=").append(uphone);
        }
        if (state == 0) {
            sqlBuilder.append(" and u.cstatus&32=0");
        }
        if (state == 32) {
            sqlBuilder.append(" and u.cstatus&32>0");
        }
        return sqlBuilder;
    }

    public Result getUserDetail(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int uid = jsonObject.get("uid").getAsInt();
        String sql = "select uid,uphone,uaccount,urealname,upw,u.roleid,u.adddate,u.addtime"
                + ",u.offdate,u.offtime,ip,logindate,logintime,u.cstatus, rname from {{?"
                + DSMConst.D_SYSTEM_USER + "}} u left join {{?" + DSMConst.D_SYSTEM_ROLE + "}} r "
                + " on u.roleid=r.roleid and u.cstatus&1=0 and uid=?";
        List<Object[]> queryResult = baseDao.queryNative(sql, uid);
        if (queryResult == null || queryResult.isEmpty()) return result;
        UserInfoVo[] userInfoVos = new UserInfoVo[queryResult.size()];
        baseDao.convToEntity(queryResult, userInfoVos, UserInfoVo.class);
        return result.success(userInfoVos[0]);
    }

    public Result updatePwd(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();

        return result;
    }

}
