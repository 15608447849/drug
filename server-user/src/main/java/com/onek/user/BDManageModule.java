package com.onek.user;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.user.entity.UserInfoVo;
import com.onek.util.GenIdUtil;
import constant.DSMConst;
import dao.BaseDAO;
import org.hyrdpf.util.LogUtil;
import util.EncryptUtils;
import util.GsonUtils;
import util.ModelUtil;
import util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.onek.util.RedisGlobalKeys.getUserCode;
import static util.GaoDeMapUtil.pointJsonToListArrayJson;

/**
 * @author 11842
 * @version 1.1.1
 * @description BD、BDM管理
 * @time 2019/5/30 10:18
 **/
public class BDManageModule {

    private static BaseDAO baseDao = BaseDAO.getBaseDAO();


    /* *
     * @description 查询合伙人下所有BDM
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/5/30 11:01
     * @version 1.1.1
     **/
    @UserPermission(ignore = false)
    public Result getAllBDMByPartner(AppContext appContext) {
        Result result = new Result();
        JsonArray resArr = new JsonArray();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
//        int cid = jsonObject.get("cid").getAsInt();
        int belong = jsonObject.get("belong").getAsInt();
//        int uid = jsonObject.get("uid").getAsInt();
        int cid = appContext.getUserSession().compId;
        int uid = appContext.getUserSession().userId;
        String selectSQL = "select uid,urealname from {{?" + DSMConst.TB_SYSTEM_USER + "}} where cstatus&1=0 "
                + " and cid=? and roleid&4096>0 and belong=? and uid<>" + uid;
        List<Object[]> objects = baseDao.queryNative(selectSQL, cid, belong);
        if (objects == null || objects.isEmpty()) return result.success(resArr);
        for (Object[] obj : objects) {
            JsonObject uObj = new JsonObject();
            uObj.addProperty("uid", String.valueOf(obj[0]));
            uObj.addProperty("urealname", String.valueOf(obj[1]));
            resArr.add(uObj);
        }
        return result.success(resArr);
    }

    /* *
     * @description 操作BDM和BD
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/5/30 10:59
     * @version 1.1.1
     **/
    @UserPermission(ignore = false)
    public Result optBDMByPartner(AppContext appContext) {
        String json = appContext.param.json;
        int cid = appContext.getUserSession().compId;
        int uid = appContext.getUserSession().userId;
        UserInfoVo userInfoVo = GsonUtils.jsonToJavaBean(json, UserInfoVo.class);
        if (userInfoVo != null) {
            if (userInfoVo.getUphone() <= 0 || userInfoVo.getUpw() == null || userInfoVo.getUpw().isEmpty()) {
                return new Result().fail("参数错误！");
            }
            if (checkBDM(userInfoVo)) {
                if ((userInfoVo.getRoleid() & 4096) > 0) {
                    return new Result().fail("该BDM已存在！");
                } else {
                    return new Result().fail("该BD已存在！");
                }
            }
            int code = 0;

            String queryCid = "select cid from {{?"+DSMConst.TB_SYSTEM_USER+"}} where uid = ? and cstatus & 1 = 0";

            List<Object[]> queryCidRet = baseDao.queryNative(queryCid, userInfoVo.getBelong());

            if(queryCidRet == null || queryCidRet.isEmpty()){
                return new Result().fail("用户操作失败！");
            }

            userInfoVo.setCid(Integer.parseInt(queryCidRet.get(0)[0].toString()));


            if (userInfoVo.getUid() <= 0) {
                String insertSQL = "insert into {{?" + DSMConst.TB_SYSTEM_USER + "}} "
                        + "(uid,uphone,uaccount,urealname,upw,roleid,adddate,addtime,cid,belong)"
                        + " values (?,?,?,?,?,?,CURRENT_DATE,CURRENT_TIME,?,?)";

                String pwd = EncryptUtils.encryption(String.valueOf(userInfoVo.getUphone()).substring(5));
                int userCode = getUserCode();
                userInfoVo.setUid(userCode);
                code = baseDao.updateNative(insertSQL, userCode,
                        userInfoVo.getUphone(), userInfoVo.getUaccount(), userInfoVo.getUrealname(),
                        pwd, userInfoVo.getRoleid(), cid, userInfoVo.getBelong());
            } else {
                String updSQL = "update {{?" + DSMConst.TB_SYSTEM_USER + "}} set uphone=?,uaccount=?,"
                        + "urealname=?, roleid=?,cid=?, belong=? where cstatus&1=0 and uid=? ";
                code = baseDao.updateNative(updSQL, userInfoVo.getUphone(), userInfoVo.getUaccount(),
                        userInfoVo.getUrealname(), userInfoVo.getRoleid(), cid,
                        userInfoVo.getBelong(), userInfoVo.getUid());
            }
            if ((userInfoVo.getRoleid() & 4096) > 0) {
                //合伙人为BDM设置辖区

            }
            if (code > 0) {
                return new Result().success(userInfoVo);
            }
        }
        return new Result().fail("用户操作失败！");
    }

    private boolean checkBDM(UserInfoVo userInfoVo) {
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

    /* *
     * @description 查询合伙人下的BDM和BD
     * @params
     * @return
     * @exception
     * @author 11842
     * @time  2019/5/30 17:30
     * @version 1.1.1
     **/
    @UserPermission(ignore = false)
    public Result queryBDAndBDM(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int belong = jsonObject.get("belong").getAsInt();
        Page page = new Page();
        page.pageSize = appContext.param.pageNumber;
        page.pageIndex = appContext.param.pageIndex;
        PageHolder pageHolder = new PageHolder(page);
        Result result = new Result();
        StringBuilder sqlBuilder = new StringBuilder();
        String selectSQL = "select uid,uphone,uaccount,urealname,upw,u.roleid,u.adddate,u.addtime"
                + ",u.offdate,u.offtime,u.cstatus,logindate,logintime, GROUP_CONCAT(rname) as rname from {{?"
                + DSMConst.TB_SYSTEM_USER + "}} u left join {{?" + DSMConst.TB_SYSTEM_ROLE + "}} r "
                + " on u.roleid&r.roleid>0 and r.cstatus&1=0 where u.cstatus&1=0 and (u.roleid&8192>0 or u.roleid&4096>0) "
                + " and (belong=? or belong in ("
                + " select uid from {{?" + DSMConst.TB_SYSTEM_USER + "}} where cstatus&1=0 and belong=?)) ";
        sqlBuilder.append(selectSQL);
        sqlBuilder = getParamsDYSQL(sqlBuilder, jsonObject, 1).append(" group by uid order by oid desc");
        List<Object[]> queryResult = baseDao.queryNativeC(pageHolder, page, sqlBuilder.toString(), belong, belong);
        if (queryResult == null || queryResult.isEmpty()) return result.success(new Object[]{});
        UserInfoVo[] userInfoVos = new UserInfoVo[queryResult.size()];
        baseDao.convToEntity(queryResult, userInfoVos, UserInfoVo.class, new String[]{
                "uid","uphone","uaccount","urealname","upw","roleid","adddate","addtime",
                "offdate","offtime","cstatus","logindate","logintime","rname"});
        return result.setQuery(userInfoVos,  pageHolder);
    }


    /* *
     * @description 查询BDM下的BD
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/5/30 17:30
     * @version 1.1.1
     **/
    @UserPermission(ignore = false)
    public Result queryBD(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int belong = jsonObject.get("belong").getAsInt();
        Page page = new Page();
        page.pageSize = appContext.param.pageNumber;
        page.pageIndex = appContext.param.pageIndex;
        PageHolder pageHolder = new PageHolder(page);
        Result result = new Result();
        StringBuilder sqlBuilder = new StringBuilder();
        String selectSQL = "select uid,uphone,uaccount,urealname,upw,u.roleid,u.adddate,u.addtime"
                + ",u.offdate,u.offtime,u.cstatus,logindate,logintime, GROUP_CONCAT(rname) as rname from {{?"
                + DSMConst.TB_SYSTEM_USER + "}} u left join {{?" + DSMConst.TB_SYSTEM_ROLE + "}} r "
                + " on u.roleid&r.roleid>0 and r.cstatus&1=0 where u.cstatus&1=0 and u.roleid&8192>0 "
                + " and belong=? ";
//        String selectSQL = "select uid,uphone,uaccount,urealname,upw,u.roleid,u.adddate,u.addtime"
//                + ",u.offdate,u.offtime,ip,logindate,logintime,u.cstatus, GROUP_CONCAT(rname) as rname,"
//                + " CONCAT('[',GROUP_CONCAT(arearng,','),']') as arearng from {{?"
//                + DSMConst.TB_SYSTEM_USER + "}} u left join {{?" + DSMConst.TB_SYSTEM_ROLE + "}} r "
//                + " on u.roleid&r.roleid>0 and r.cstatus&1=0 left join {{?"
//                + DSMConst.TB_PROXY_UAREA + "}} ua on ua.uid=u.uid where u.cstatus&1=0 and ua.cstatus&1=0 "
//                + " group by ua.uid";
        sqlBuilder.append(selectSQL);
        sqlBuilder = getParamsDYSQL(sqlBuilder, jsonObject, 0).append(" group by uid desc");
        List<Object[]> queryResult = baseDao.queryNative(pageHolder, page, sqlBuilder.toString(), belong);
        if (queryResult == null || queryResult.isEmpty()) return result.success(new Object[]{});
        UserInfoVo[] userInfoVos = new UserInfoVo[queryResult.size()];
        baseDao.convToEntity(queryResult, userInfoVos, UserInfoVo.class, new String[]{
                "uid","uphone","uaccount","urealname","upw","roleid","adddate","addtime",
                "offdate","offtime","cstatus","logindate","logintime","rname"});
        return result.setQuery(userInfoVos, pageHolder);
    }

    private StringBuilder getParamsDYSQL(StringBuilder sqlBuilder, JsonObject jsonObject, int type) {
        String urealname = jsonObject.get("urealname").getAsString();
        String uphone = jsonObject.get("uphone").getAsString();
        int state = jsonObject.get("cstatus").getAsInt();
        if (urealname != null && !urealname.isEmpty()) {
            sqlBuilder.append(" and urealname like '%").append(urealname).append("%'");
        }
        if (type == 1) {
            long roleid = jsonObject.get("roleid").getAsLong();
            if (roleid > 0) {
                sqlBuilder.append(" and u.roleid&").append(roleid).append(">0");
            }
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
        return sqlBuilder;
    }

    /* *
     * @description 详情
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/5/30 14:30
     * @version 1.1.1
     **/
    @UserPermission(ignore = false)
    public Result getBDOrBDMDetail(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int uid = jsonObject.get("uid").getAsInt();
//        String selectSQL = "select uid,uphone,uaccount,urealname,upw,u.roleid,u.adddate,u.addtime"
//                + ",u.offdate,u.offtime,ip,logindate,logintime,u.cstatus, GROUP_CONCAT(rname) as rname,"
//                + " CONCAT('[',GROUP_CONCAT(arearng,','),']') as arearng from {{?"
//                + DSMConst.TB_SYSTEM_USER + "}} u left join {{?" + DSMConst.TB_SYSTEM_ROLE + "}} r "
//                + " on u.roleid&r.roleid>0 and r.cstatus&1=0 left join {{?"
//                + DSMConst.TB_PROXY_UAREA + "}} ua on ua.uid=u.uid where u.cstatus&1=0 and ua.cstatus&1=0 "
//                + "  and uid=?";
        String selectSQL = "select uid,uphone,uaccount,urealname,upw,u.roleid,u.adddate,u.addtime"
                + ",u.offdate,u.offtime,u.cstatus, GROUP_CONCAT(rname) as rname, belong from {{?"
                + DSMConst.TB_SYSTEM_USER + "}} u left join {{?" + DSMConst.TB_SYSTEM_ROLE + "}} r "
                + " on u.roleid&r.roleid>0 and r.cstatus&1=0 where u.cstatus&1=0 and uid=?";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL, uid);
        if (queryResult == null || queryResult.isEmpty()) return result.success(null);
        UserInfoVo[] userInfoVos = new UserInfoVo[queryResult.size()];
        baseDao.convToEntity(queryResult, userInfoVos, UserInfoVo.class, new String[]{
                "uid","uphone","uaccount","urealname","upw","roleid","adddate","addtime",
                "offdate","offtime","cstatus","rname","belong"});
        return result.success(userInfoVos[0]);
    }


    /* *
     * @description 查看辖区
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/5/30 14:17
     * @version 1.1.1
     **/
    @UserPermission(ignore = false)
    public Result seeAreas(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        Map<String, JsonArray> oMap = new HashMap<>();
        JsonArray setAreaArr = new JsonArray();
        JsonArray superiorArr = new JsonArray();
        int uid = jsonObject.get("uid").getAsInt();//设置的用户码
        int puid = jsonObject.get("puid").getAsInt();//上级用户码
        //上级
        String selectPSQL = "select a.areac, arean, arearng from {{?" + DSMConst.TB_PROXY_UAREA + "}} a, {{?"
                +DSMConst.TB_AREA_PCA+"}} b where a.cstatus&1=0 and a.areac=b.areac"
                + " and uid=" + puid;
        List<Object[]> qPResult = baseDao.queryNative(selectPSQL);
        if (qPResult == null || qPResult.isEmpty()) return result.success(superiorArr);
        for (Object[] o : qPResult) {
            JsonObject areaObj = new JsonObject();
            areaObj.addProperty("areac", String.valueOf(o[0]));
            areaObj.addProperty("areaName", String.valueOf(o[1]));
            areaObj.addProperty("arearng",pointJsonToListArrayJson(String.valueOf(o[2])));
            superiorArr.add(areaObj);
        }
        //设置的
        String selectSQL = "select a.areac, arean,arearng from {{?" + DSMConst.TB_PROXY_UAREA + "}} a, {{?"
                + DSMConst.TB_AREA_PCA + "}} b where a.cstatus&1=0 and a.areac=b.areac"
                + " and uid=" + uid;
        List<Object[]> qResult = baseDao.queryNative(selectSQL);
        if (qResult != null && !qResult.isEmpty()) {
            for (Object[] o : qResult) {
                JsonObject areaObj = new JsonObject();
                areaObj.addProperty("areac", String.valueOf(o[0]));
                areaObj.addProperty("areaName", String.valueOf(o[1]));
                areaObj.addProperty("arearng",pointJsonToListArrayJson(String.valueOf(o[2])));
                setAreaArr.add(areaObj);
            }
        }
        oMap.put("pArea", superiorArr);
        oMap.put("srea", setAreaArr);
        return result.success(oMap);
    }


    /* *
     * @description 设置辖区
     * @params []
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/5/30 14:31
     * @version 1.1.1
     **/
    @UserPermission(ignore = false)
    public Result setAreas(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        long areac = jsonObject.get("areac").getAsLong();
        int uid = jsonObject.get("uid").getAsInt();
        String arearng = jsonObject.get("arearng").getAsString();
        String optSQL = "insert into {{?" + DSMConst.TB_PROXY_UAREA + "}} (unqid,uid,areac,cstatus,arearng) "
                + " values(?,?,?,?,?)";
        int code = baseDao.updateNative(optSQL, GenIdUtil.getUnqId(), uid, areac,
                0, arearng);
        return code > 0 ? result.success("设置成功") : result.fail("设置失败");
    }

    /* *
     * @description 批量设置管辖区域
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/6/4 11:25
     * @version 1.1.1
     **/
    @UserPermission(ignore = false)
    public Result setAreaArr(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int uid = jsonObject.get("uid").getAsInt();
        //删除之前的
        String updSQL = "update {{?" + DSMConst.TB_PROXY_UAREA + "}} set cstatus=cstatus|1 where cstatus&1=0 "
                + " and uid=" + uid;
        int code = baseDao.updateNative(updSQL);
        LogUtil.getDefaultLogger().info("code---- " + code);
        if(code >= 0) {
            List<Object[]> params  = new ArrayList<>();
            JsonArray areaArr = jsonObject.get("areaArr").getAsJsonArray();
            for (int i = 0; i < areaArr.size(); i++) {
                JsonElement areaObj = areaArr.get(i);
                long areac =areaObj.getAsJsonObject().get("areac").getAsLong();
                String arearng = areaObj.getAsJsonObject().get("arearng").getAsString();
                params.add(new Object[]{GenIdUtil.getUnqId(), uid, areac, 0, arearng});
            }
            String optSQL = "insert into {{?" + DSMConst.TB_PROXY_UAREA + "}} (unqid,uid,areac,cstatus,arearng) "
                    + " values(?,?,?,?,?)";
            boolean b = ModelUtil.updateTransEmpty(baseDao.updateBatchNative(optSQL, params, params.size()));
            return b ? result.success("设置成功") : result.fail("设置失败");
        }
        return result.fail("设置失败");
    }

}
