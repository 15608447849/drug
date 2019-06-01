package com.onek.user;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.consts.CSTATUS;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.user.entity.RoleVO;
import com.onek.user.entity.UserInfoVo;
import com.onek.util.GenIdUtil;
import com.onek.util.RoleCodeCons;
import constant.DSMConst;
import dao.BaseDAO;
import util.EncryptUtils;
import util.GsonUtils;
import util.ModelUtil;
import util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.onek.util.RedisGlobalKeys.getUserCode;

/**
 * @author cyq
 * @version 1.1.1
 * @description 用户操作
 * @time 2019/3/18 15:04
 **/
public class BackgroundUserModule {
    private static final BaseDAO baseDao = BaseDAO.getBaseDAO();

    @UserPermission(ignore = true)
    public Result insertOrUpdUser(AppContext appContext) {
        String json = appContext.param.json;
        UserInfoVo userInfoVo = GsonUtils.jsonToJavaBean(json, UserInfoVo.class);
        if (userInfoVo != null) {
            if (checkUser(userInfoVo)) return new Result().fail("该用户已存在！");
            if (userInfoVo.getUphone() <= 0 || userInfoVo.getUpw() == null || userInfoVo.getUpw().isEmpty()) {
                return new Result().fail("参数错误！");
            }
            List<String> sqlList = new ArrayList<>();
            List<Object[]> parmList = new ArrayList<>();

            String queryAreaExtSql = "select 1 from {{?"+DSMConst.TB_PROXY_UAREA+"}} where  uid = ? and areac = ? and cstatus & 1 = 0 ";

            if (userInfoVo.getUid() <= 0) {
                String insertSQL = "insert into {{?" + DSMConst.TB_SYSTEM_USER + "}} "
                        + "(uid,uphone,uaccount,urealname,upw,roleid,adddate,addtime,belong)"
                        + " values (?,?,?,?,?,?,CURRENT_DATE,CURRENT_TIME,?)";

                sqlList.add(insertSQL);
                String pwd = EncryptUtils.encryption(String.valueOf(userInfoVo.getUphone()).substring(5));

                parmList.add(new Object[]{getUserCode(),
                        userInfoVo.getUphone(), userInfoVo.getUaccount(), userInfoVo.getUrealname(),
                        pwd, userInfoVo.getRoleid(),userInfoVo.getBelong()});

                if(userInfoVo.getArean() != null
                        && !StringUtils.isEmpty(userInfoVo.getArean())){
                    String [] areaArry = userInfoVo.getArean().split(",");
                    String insertAreaSql = "insert into {{?"+DSMConst.TB_PROXY_UAREA+"}}" +
                            " (unqid,uid,areac,cstatus) values (?,?,?,?) ";

                    for(int i = 0; i < areaArry.length; i++){
                        if(areaArry[i] != null && !StringUtils.isEmpty(areaArry[i])){
                            List<Object[]> isExt = baseDao.queryNative(queryAreaExtSql, userInfoVo.getUid(), areaArry[i]);
                            if(isExt == null && isExt.isEmpty()){
                                sqlList.add(insertAreaSql);
                                parmList.add(new Object[]{GenIdUtil.getUnqId(),userInfoVo.getUid(),areaArry[i],
                                        0});
                            }
                        }
                    }
                }
            } else {
                String updSQL = "update {{?" + DSMConst.TB_SYSTEM_USER + "}} set uphone=?,uaccount=?,"
                        + "urealname=?, roleid=? where cstatus&1=0 and uid=? ";
                sqlList.add(updSQL);
                parmList.add(new Object[]{userInfoVo.getUphone(),userInfoVo.getUaccount(),
                        userInfoVo.getUrealname(),userInfoVo.getRoleid(),userInfoVo.getUid()});

                if(userInfoVo.getArean() != null
                        && !StringUtils.isEmpty(userInfoVo.getArean())){
                    String [] areaArry = userInfoVo.getArean().split(",");

                    String insertAreaSql = "insert into {{?"+DSMConst.TB_PROXY_UAREA+"}}" +
                            " (unqid,uid,areac,cstatus) values (?,?,?,?) ";

                    String delAreaSql = "update {{?"+DSMConst.TB_PROXY_UAREA+"}} set cstatus = cstatus | 1 " +
                            " where uid = ? and areac = ?";

                    for(int i = 0; i < areaArry.length; i++){
                        if(areaArry[i] != null && !StringUtils.isEmpty(areaArry[i])){
                            if(areaArry[i].startsWith("-")){
                                sqlList.add(delAreaSql);
                                parmList.add(new Object[]{userInfoVo.getUid(),areaArry[i].substring(1)});
                            }else{
                                if(areaArry[i] != null && !StringUtils.isEmpty(areaArry[i])){
                                    List<Object[]> isExt = baseDao.queryNative(queryAreaExtSql, userInfoVo.getUid(), areaArry[i]);
                                    if(isExt == null && isExt.isEmpty()){
                                        sqlList.add(insertAreaSql);
                                        parmList.add(new Object[]{GenIdUtil.getUnqId(),userInfoVo.getUid(),areaArry[i],
                                                0});
                                    }
                                }
                            }
                        }
                    }
                }
            }
            String[] sqlNative = new String[sqlList.size()];
            sqlNative = sqlList.toArray(sqlNative);
            boolean b = !ModelUtil.updateTransEmpty(
                    baseDao.updateTransNative(sqlNative,parmList));
            if (b) {
                return new Result().success("操作成功");
            }
        }
        return new Result().fail("用户操作失败！");
    }

    private boolean checkUser(UserInfoVo userInfoVo) {
        StringBuilder sqlBuilder = new StringBuilder();
        String sql = "select count(*) from {{?" + DSMConst.TB_SYSTEM_USER + "}} where uphone=?" +
                " and cstatus&1=0 ";
        sqlBuilder.append(sql);
        if (userInfoVo.getUid() > 0) {
            sqlBuilder.append(" and uid<>").append(userInfoVo.getUid());
        }
        List<Object[]> queryResult = baseDao.queryNative(sqlBuilder.toString(), userInfoVo.getUphone());
        int count = Integer.parseInt(String.valueOf(queryResult.get(0)[0]));
        return count > 0;
    }

    @UserPermission(ignore = true)
    public Result cancelOrFrozenUser(AppContext appContext){
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int type = jsonObject.get("type").getAsInt();
        int uid = jsonObject.get("uid").getAsInt();
        String updateSql = "update {{?" + DSMConst.TB_SYSTEM_USER + "}} set cstatus=cstatus&~32 "
                + " where cstatus&1=0 and cstatus&32>0 and uid=?";//启用
        if (type == 0) {//停用
            updateSql = "update {{?" + DSMConst.TB_SYSTEM_USER + "}} set cstatus=cstatus|32 "
                    + " where cstatus&1=0 and cstatus&32=0 and uid=?";
        }
        int code = baseDao.updateNative(updateSql, uid);
        if (code > 0) {
            return result.success("操作成功");
        }
        return result.fail("操作失败");
    }

    @UserPermission(ignore = true)
    public Result queryUsers(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int pageSize = jsonObject.get("pageSize").getAsInt();
        int pageIndex = jsonObject.get("pageNo").getAsInt();
        Page page = new Page();
        page.pageSize = pageSize;
        page.pageIndex = pageIndex;
        PageHolder pageHolder = new PageHolder(page);
        Result result = new Result();
        StringBuilder sqlBuilder = new StringBuilder();
        String selectSQL = "select u.uid,uphone,uaccount,urealname,upw,u.roleid,u.adddate,u.addtime"
                + ",u.offdate,u.offtime,ip,logindate,logintime,u.cstatus, GROUP_CONCAT(rname) as rname,arean as arean from {{?"
                + DSMConst.TB_SYSTEM_USER + "}} u left join {{?" + DSMConst.TB_SYSTEM_ROLE + "}} r "
                + " on u.roleid&r.roleid>0 and r.cstatus&1=0"
                + " left join (select uid,GROUP_CONCAT(pca.arean) as arean "
                + " from {{?"+DSMConst.TB_PROXY_UAREA+"}} uarea,{{?"+DSMConst.TB_AREA_PCA +"}} pca "
                + "  where pca.areac = uarea.areac group by uid) a on a.uid = u.uid "
                + " where u.cstatus&1=0 ";

        sqlBuilder.append(selectSQL);
        sqlBuilder = getgetParamsDYSQL(sqlBuilder, jsonObject).append(" group by u.uid desc ");
        List<Object[]> queryResult = baseDao.queryNative(pageHolder, page, sqlBuilder.toString());
        if (queryResult == null || queryResult.isEmpty()) return result.success(null);
        UserInfoVo[] userInfoVos = new UserInfoVo[queryResult.size()];

        baseDao.convToEntity(queryResult, userInfoVos, UserInfoVo.class,
                new String[]{"uid","uphone","uaccount","urealname","upw",
                        "roleid","adddate","addtime","offdate",
                        "offtime","ip","logindate","logintime",
                        "cstatus","rname","arean"});

        baseDao.convToEntity(queryResult, userInfoVos, UserInfoVo.class);
        return result.setQuery(userInfoVos,pageHolder);
    }

    private StringBuilder getgetParamsDYSQL(StringBuilder sqlBuilder, JsonObject jsonObject) {
      //  String uname = jsonObject.get("uaccount").getAsString();
        String urealname = jsonObject.get("urealname").getAsString();
        long roleid = jsonObject.get("roleid").getAsLong()  ;
        String uphone = jsonObject.get("uphone").getAsString();
        int state = jsonObject.get("cstatus").getAsInt();
        String areaStr = jsonObject.get("areac").getAsString();
        int rstate = jsonObject.get("rstate").getAsInt();
        int mroleid = jsonObject.get("mroleid").getAsInt();
        int puid = jsonObject.get("puid").getAsInt();

        if (urealname != null && !urealname.isEmpty()) {
            sqlBuilder.append(" and urealname like '%").append(urealname).append("%'");
        }
        if (roleid > 0) {
            sqlBuilder.append(" and u.roleid&").append(roleid).append(">0");
        }

        if (!StringUtils.isEmpty(uphone) && Long.parseLong(uphone) > 0) {
            sqlBuilder.append(" and uphone=").append(uphone);
        }
        if (state == 0) {
            sqlBuilder.append(" and u.cstatus&32=0");
        }
        if (state == 32) {
            sqlBuilder.append(" and u.cstatus&32>0");
        }

        if (rstate == 0) {
            sqlBuilder.append(" and r.cstatus&32=0");
        }
        if (rstate == 32) {
            sqlBuilder.append(" and r.cstatus&32>0");
        }

        StringBuilder areaSb = new StringBuilder();
        if(!StringUtils.isEmpty(areaStr)){
            String [] areaArry = areaStr.split(",");
            for(String areac : areaArry){
                if(areac != null && !StringUtils.isEmpty(areac)){
                    areaSb.append(Integer.parseInt(areac)).append(",");
                }
            }
            String areacStr = areaSb.toString();
            if(areacStr.endsWith(",")){
                areacStr = areacStr.substring(0,areacStr.length() - 1);
            }

            String areaSql = "select uid from {{?"+DSMConst.TB_PROXY_UAREA+"}} where areac in ("+areacStr+") and cstatus & 1 = 0";
            List<Object[]> queryResult = baseDao.queryNative(areaSql, new Object[]{});
            StringBuilder uidSb = new StringBuilder();
            if(queryResult != null && !queryResult.isEmpty()){
                for (Object[] objs : queryResult){
                    uidSb.append(objs.toString()).append(",");
                }
            }
            String uidStr = uidSb.toString();
            if(uidStr.endsWith(",")){
                uidStr = uidStr.substring(0,uidStr.length() - 1);
            }
            sqlBuilder.append(" and u.uid in (").append(uidStr).append(")");
        }

        if((mroleid & RoleCodeCons._PROXY_DIRECTOR) > 0){
            sqlBuilder.append(" and u.belong = ").append(puid);
        }

        return sqlBuilder;
    }

    @UserPermission(ignore = true)
    public Result getUserDetail(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int uid = jsonObject.get("uid").getAsInt();
        String sql = "select u.uid,uphone,uaccount,urealname,upw,u.roleid,u.adddate,u.addtime"
                + ",u.offdate,u.offtime,ip,logindate,logintime,u.cstatus,GROUP_CONCAT(rname) rname from {{?"
                + DSMConst.TB_SYSTEM_USER + "}} u left join {{?" + DSMConst.TB_SYSTEM_ROLE + "}} r "
                + " on u.roleid=r.roleid and u.cstatus&1=0 and uid=?";
        List<Object[]> queryResult = baseDao.queryNative(sql, uid);
        if (queryResult == null || queryResult.isEmpty()) return result.success(null);
        UserInfoVo[] userInfoVos = new UserInfoVo[queryResult.size()];

        baseDao.convToEntity(queryResult, userInfoVos, UserInfoVo.class,
                new String[]{"uid","uphone","uaccount","urealname","upw",
                        "roleid","adddate","addtime","offdate",
                        "offtime","ip","logindate","logintime",
                        "cstatus","rname"});

        return result.success(userInfoVos[0]);
    }

    @UserPermission(ignore = true)
    public Result updatePwd(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();

        return result;
    }

    @UserPermission(ignore = true)
    public Result resetPwd(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int uid = jsonObject.get("uid").getAsInt();
        String updateSQL = "update {{?" + DSMConst.TB_SYSTEM_USER + "}} set upw=md5(SUBSTR(uphone,6)) "
                + " where cstatus&1=0 and uid=" + uid;
        int code = baseDao.updateNative(updateSQL);
        return code > 0 ? result.success("操作成功") : result.fail("操作失败") ;
    }


    /**
     * 人员管理选择角色
     * @param appContext
     * @return
     */
    @UserPermission(ignore = true)
    public Result queryRole(AppContext appContext) {

        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int roleid = jsonObject.get("roleid").getAsInt();
        int sroleid = jsonObject.get("sroleid").getAsInt();
        StringBuilder sqlSb = new StringBuilder("SELECT roleid,rname,adddate,addtime,offdate,offtime,0 cstatus  FROM {{?");
        sqlSb.append(DSMConst.TB_SYSTEM_ROLE);
        sqlSb.append("}} WHERE cstatus&33 = 0 AND roleid & 1 = 0 ");

        if((roleid & RoleCodeCons._PROXY_DIRECTOR) > 0){
            int addRole = RoleCodeCons._PROXY_MGR
                    + RoleCodeCons._PROXY_PARTNER
                    + RoleCodeCons._DBM
                    + RoleCodeCons._DB;
            sqlSb.append(" AND roleid &  ").append(addRole).append(" > 0 ");
        }

        List<Object[]> queryResult = baseDao.queryNative(
                sqlSb.toString());

        RoleVO[] result = new RoleVO[queryResult.size()];

        baseDao.convToEntity(queryResult, result, RoleVO.class);

        if (result.length > 0) {
            baseDao.convToEntity(queryResult, result, RoleVO.class);
            if(sroleid > 0){
                for (RoleVO roleVO : result){
                    if((roleVO.getRoleId() & sroleid) > 0){
                        roleVO.setCstatus(1);
                    }
                }
            }
        }
        return new Result().success(result);
    }


}
