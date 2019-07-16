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
import com.onek.user.entity.ProxyAreaTreeVO;
import com.onek.user.entity.ProxyPartnerVO;
import com.onek.user.entity.UserInfoVo;
import com.onek.user.service.USProperties;
import com.onek.util.*;
import com.onek.util.area.AreaEntity;
import com.onek.util.area.AreaUtil;
import constant.DSMConst;
import dao.BaseDAO;
import org.hyrdpf.util.LogUtil;
import redis.util.RedisUtil;
import util.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.onek.util.RedisGlobalKeys.getUserCode;
import static util.GaoDeMapUtil.pointJsonToListArrayJson;
import static util.ImageVerificationUtils.getRandomCodeByNum;

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
     * @params json {belong 所属合伙人}
     * @return json数组 [{uid 用户码 urealname 用户真实姓名}]
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
        int ruid = 0;
        int belong = jsonObject.get("belong").getAsInt();
        if (jsonObject.get("ruid") != null && !jsonObject.get("ruid").getAsString().isEmpty()) {
            ruid = jsonObject.get("ruid").getAsInt();
        }
        int cid = appContext.getUserSession().compId;
        int uid = appContext.getUserSession().userId;
        String selectSQL = "select uid,urealname from {{?" + DSMConst.TB_SYSTEM_USER + "}} where cstatus&1=0 "
                + " and (cid=? and roleid&4096>0 and belong=? and uid<>" + uid + " and uid<>" + ruid+") or uid = ?";
        List<Object[]> objects = baseDao.queryNative(selectSQL, cid, belong,uid);
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
     * @description 操作BDM和BD（新增修改）
     * @params json { uid用户码（新增传0）,uphone（手机号）,uaccount（账户）,urealname（真实姓名）,
     * upw（密码默认手机号后六位）,roleid（角色码）,adddate,addtime,belong（上级（地推））}
     * @return -1失败 200成功
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

            String deleteArea =  "update {{?"+DSMConst.TB_PROXY_UAREA+"}} set cstatus = cstatus | 1 where cstatus & 1 = 0  ";

            String queryIsHasDb = "select 1 from {{?"+DSMConst.TB_SYSTEM_USER+"}} where belong = ? and roleid & 8192 > 0 ";

            String updateRole = "update {{?"+DSMConst.TB_SYSTEM_USER+"}}  set roleid = roleid | 4096 where roleid & 4096 = 0 and uid = ? ";

            boolean code;
            if (userInfoVo.getUid() <= 0 && checkBDM(userInfoVo)) {
                String updateCompBd = "update {{?" + DSMConst.TB_SYSTEM_USER + "}} "
                        + "set cid = ?,upw = ?,urealname = ?,belong = ?,roleid = ?,cstatus = ? where uphone = ? ";
                String pwd = EncryptUtils.encryption(String.valueOf(userInfoVo.getUphone()).substring(5));
                if(isNeedSmsVerify(userInfoVo.getUphone()+"")){
                    if ((userInfoVo.getRoleid() & 4096) > 0) {
                        return new Result().fail("该BDM已存在！");
                    } else {
                        return new Result().fail("该BD已存在！");
                    }
                }
//                    String queryRole = "select roleid,uid from {{?"+DSMConst.TB_SYSTEM_USER+"}} where uphone = ? ";
//                    List<Object[]> roleRet = baseDao.queryNative(queryRole,userInfoVo.getUphone());
//                    List<Object[]> hasDb = baseDao.queryNative(queryIsHasDb,roleRet.get(0)[1]);
//                    if((Integer.parseInt(roleRet.get(0)[0].toString())
//                            & RoleCodeCons._DBM) > 0 &&
//                            (hasDb != null && !hasDb.isEmpty())){
//                        return new Result().fail("当前DBM有关联DB存在，需先解除DB与DBM关系才能添加！");
//                    }
//                    String res = RedisUtil.getStringProvide().get("SMS"+userInfoVo.getUphone());
//                    if(StringUtils.isEmpty(userInfoVo.getVcode()) || !res.equals(userInfoVo.getVcode())){
//                       // return new Result().fail("验证码验证错误！");
//                    }
//                }


                code = baseDao.updateNative(updateCompBd,new Object[]{cid,pwd,userInfoVo.getUrealname(),
                        userInfoVo.getBelong(),userInfoVo.getRoleid(),0,userInfoVo.getUphone()}) > 0;

                if(code){
                    StringBuilder sqlSb = new StringBuilder(deleteArea);
                    sqlSb.append("and uid in (select uid from {{?");
                    sqlSb.append(DSMConst.TB_SYSTEM_USER);
                    sqlSb.append("}} where uphone = ? and cstatus & 1 = 0)");
                    baseDao.updateNative(sqlSb.toString(),userInfoVo.getUphone());
                    if(uid == userInfoVo.getBelong()){
                        baseDao.updateNative(updateRole,uid);
                    }
                    return new Result().success("操作成功！");
                }
//                if ((userInfoVo.getRoleid() & 4096) > 0) {
//                    return new Result().fail("该BDM已存在！");
//                } else {
//                    return new Result().fail("该BD已存在！");
//                }
            }

            if(checkBDM(userInfoVo)){
                if ((userInfoVo.getRoleid() & 4096) > 0) {
                    return new Result().fail("该BDM已存在！");
                } else {
                    return new Result().fail("该BD已存在！");
                }
            }


            boolean isDelArea = false;
            if (userInfoVo.getUid() <= 0) {
                String insertSQL = "insert into {{?" + DSMConst.TB_SYSTEM_USER + "}} "
                        + "(uid,uphone,uaccount,urealname,upw,roleid,adddate,addtime,cid,belong)"
                        + " values (?,?,?,?,?,?,CURRENT_DATE,CURRENT_TIME,?,?)";

                String pwd = EncryptUtils.encryption(String.valueOf(userInfoVo.getUphone()).substring(5));
                int userCode = getUserCode();
                userInfoVo.setUid(userCode);
                code = baseDao.updateNative(insertSQL, userCode,
                        userInfoVo.getUphone(), userInfoVo.getUaccount(), userInfoVo.getUrealname(),
                        pwd, userInfoVo.getRoleid(), cid, userInfoVo.getBelong()) > 0;
            } else {
                List<Object[]> params = new ArrayList<>();
                String updSQL = "update {{?" + DSMConst.TB_SYSTEM_USER + "}} set uphone=?,uaccount=?,"
                        + "urealname=?, roleid=?,cid=?, belong=? where cstatus&1=0 and uid=? ";
                String delAreaSQL = "update {{?" + DSMConst.TB_PROXY_UAREA + "}} set cstatus=cstatus|1 "
                        + " where cstatus&1=0 and uid=" + userInfoVo.getUid();
                long roleid = getRoleIdByUid(userInfoVo.getUid());
                if ((roleid & userInfoVo.getRoleid()) > 0) {
                    code = baseDao.updateNative(updSQL, userInfoVo.getUphone(), userInfoVo.getUaccount(),
                            userInfoVo.getUrealname(), userInfoVo.getRoleid(), cid,
                            userInfoVo.getBelong(), userInfoVo.getUid()) > 0;
                } else {
                    if ((roleid & 4096) > 0) {
                        if (BDMHasBD(userInfoVo.getUid())) {
                            return new Result().fail("该BDM下已存在BD,无法变更为BD!");
                        }
                    }
                    //如果BDM下不存在BD 删除该BDM辖区
                    params.add(new Object[]{userInfoVo.getUphone(), userInfoVo.getUaccount(),
                            userInfoVo.getUrealname(), userInfoVo.getRoleid(), cid,
                            userInfoVo.getBelong(), userInfoVo.getUid()});
                    params.add(new Object[]{});
                    code = !ModelUtil.updateTransEmpty(baseDao.updateTransNative(new String[]{updSQL, delAreaSQL}, params));
                }
            }
            if (code) {
                if(uid == userInfoVo.getBelong()){
                    baseDao.updateNative(updateRole,uid);
                }
                return new Result().success(userInfoVo);
            }
        }
        return new Result().fail("用户操作失败！");
    }

    private void deleteAreas() {

    }

    private boolean BDMHasBD(int ruid) {
        String selectSQL = "select count(*) from {{?" + DSMConst.TB_SYSTEM_USER + "}} where cstatus&1=0 "
                + " and belong=" + ruid;
        List<Object[]> qResult = baseDao.queryNative(selectSQL);
        long count = Long.parseLong(String.valueOf(qResult.get(0)[0]));
        return count > 0;
    }

    private long getRoleIdByUid(int ruid) {
        String selectSQL = "select roleid from {{?" + DSMConst.TB_SYSTEM_USER + "}} where cstatus&1=0 "
                + " and uid=" + ruid;
        List<Object[]> qResult = baseDao.queryNative(selectSQL);
        return Long.parseLong(String.valueOf(qResult.get(0)[0]));
    }

    @UserPermission(ignore = true)
    public Result addBdVerifySms(AppContext appContext){
        String json = appContext.param.json;
        int cid = appContext.getUserSession().compId;
        UserInfoVo userInfoVo = GsonUtils.jsonToJavaBean(json, UserInfoVo.class);
        if (userInfoVo != null) {
            if (userInfoVo.getUphone() <= 0 || userInfoVo.getUpw() == null || userInfoVo.getUpw().isEmpty()) {
                return new Result().fail("参数错误！");
            }
            if (checkBDM(userInfoVo) && isNeedSmsVerify(userInfoVo.getUphone()+"")) {
                String getcnameSql = "select cname from {{?"+DSMConst.TB_COMP+"}} where cid = ? ";
                List<Object[]> ret = baseDao.queryNative(getcnameSql, new Object[]{cid});
                if(ret != null && !ret.isEmpty()){
                    String code = genSmsCodeStoreCache(userInfoVo.getUphone()+"");
                    if (!StringUtils.isEmpty(code)){
                        SmsUtil.sendSmsBySystemTemp(userInfoVo.getUphone()+"",
                                SmsTempNo.PROXY_PARNER_ADD_USER_VERIFY,ret.get(0)[0].toString(),code);
                        return new Result().success("该手机号已存在，需短信验证，已发送短信验证信息！");
                    }
                }
            }
        }
        return new Result().fail("");
    }


    public boolean isNeedSmsVerify(String phone) {
        String queryCompStatus = "select cp.cstatus from {{?" + DSMConst.TB_COMP + "}} " +
                " cp join {{?" + DSMConst.TB_SYSTEM_USER + "}} su on cp.cid = su.cid and su.uphone = ? ";
        List<Object[]> queryStatus = baseDao.queryNative(queryCompStatus, phone);
        if (queryStatus != null && !queryStatus.isEmpty()) {
            int cstatus = Integer.parseInt(queryStatus.get(0)[0].toString());
            if ((cstatus & 33) == 0) {
                return true;
            }
        }
        return false;
    }

    private static String genSmsCodeStoreCache(String phone){
        if(StringUtils.isEmpty(phone)) return null; //手机号码不能为空
        String code = getRandomCodeByNum(6);
        //存入缓存
        String res = RedisUtil.getStringProvide().set("SMS"+phone,code);
        if (res.equals("OK")){
            RedisUtil.getStringProvide().expire("SMS"+phone, USProperties.INSTANCE.smsSurviveTime); // 5分钟内有效
            return code;
        }
        return null;
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
     * @params json {belong: 用户码 }
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
        long roleId = appContext.getUserSession().roleCode;
        int cid = appContext.getUserSession().compId;
        Page page = new Page();
        page.pageSize = appContext.param.pageNumber;
        page.pageIndex = appContext.param.pageIndex;
        PageHolder pageHolder = new PageHolder(page);
        Result result = new Result();
        StringBuilder sqlBuilder = new StringBuilder();
        Object[] params;
        String selectSQL = "select uid,uphone,uaccount,urealname,upw,u.roleid,u.adddate,u.addtime"
                + ",u.offdate,u.offtime,u.cstatus,logindate,logintime, GROUP_CONCAT(rname) as rname from {{?"
                + DSMConst.TB_SYSTEM_USER + "}} u left join {{?" + DSMConst.TB_SYSTEM_ROLE + "}} r "
                + " on u.roleid&r.roleid>0 and r.cstatus&1=0 where u.cstatus&1=0 and (u.roleid&8192>0 or u.roleid&4096>0) "
                + " and cid=? and (belong=? or belong in ("
                + " select uid from {{?" + DSMConst.TB_SYSTEM_USER + "}} where cstatus&1=0 and belong=?)) ";

        if ((roleId & 1) > 0) {
            selectSQL = "select uid,uphone,uaccount,urealname,upw,u.roleid,u.adddate,u.addtime"
                    + ",u.offdate,u.offtime,u.cstatus,logindate,logintime, GROUP_CONCAT(rname) as rname from {{?"
                    + DSMConst.TB_SYSTEM_USER + "}} u left join {{?" + DSMConst.TB_SYSTEM_ROLE + "}} r "
                    + " on u.roleid&r.roleid>0 and r.cstatus&1=0 where u.cstatus&1=0 and (u.roleid&8192>0 or u.roleid&4096>0) ";
            params = new Object[]{};
        } else {
            params = new Object[]{cid, belong, belong};
        }
        sqlBuilder.append(selectSQL);
        sqlBuilder = getParamsDYSQL(sqlBuilder, jsonObject, 1).append(" group by uid order by oid desc");
        List<Object[]> queryResult = baseDao.queryNativeC(pageHolder, page, sqlBuilder.toString(), params);
        if (queryResult == null || queryResult.isEmpty()) return result.success(new Object[]{});
        UserInfoVo[] userInfoVos = new UserInfoVo[queryResult.size()];
        baseDao.convToEntity(queryResult, userInfoVos, UserInfoVo.class, new String[]{
                "uid","uphone","uaccount","urealname","upw","roleid","adddate","addtime",
                "offdate","offtime","cstatus","logindate","logintime","rname"});
        return result.setQuery(userInfoVos,  pageHolder);
    }


    /* *
     * @description 查询BDM下的BD
     * @params json {urealname：真实姓名 roleid：角色码 uphone：电话号码 cstatus：用户状态 }
     *   分页参数 pageSize：每页数量 pageNo： 第几页
     * @return UserInfoVo对象数组（见UserInfoVo.class）
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
        long roleId = appContext.getUserSession().roleCode;
        int cid = appContext.getUserSession().compId;
        Page page = new Page();
        page.pageSize = appContext.param.pageNumber;
        page.pageIndex = appContext.param.pageIndex;
        PageHolder pageHolder = new PageHolder(page);
        Result result = new Result();
        StringBuilder sqlBuilder = new StringBuilder();
        Object[] params;
        String selectSQL = "select uid,uphone,uaccount,urealname,upw,u.roleid,u.adddate,u.addtime"
                + ",u.offdate,u.offtime,u.cstatus,logindate,logintime, GROUP_CONCAT(rname) as rname from {{?"
                + DSMConst.TB_SYSTEM_USER + "}} u left join {{?" + DSMConst.TB_SYSTEM_ROLE + "}} r "
                + " on u.roleid&r.roleid>0 and r.cstatus&1=0 where u.cstatus&1=0 and u.roleid&8192>0 "
                + " and belong=? and cid=?";
        if ((roleId & 1) > 0) {
            selectSQL = "select uid,uphone,uaccount,urealname,upw,u.roleid,u.adddate,u.addtime"
                    + ",u.offdate,u.offtime,u.cstatus,logindate,logintime, GROUP_CONCAT(rname) as rname from {{?"
                    + DSMConst.TB_SYSTEM_USER + "}} u left join {{?" + DSMConst.TB_SYSTEM_ROLE + "}} r "
                    + " on u.roleid&r.roleid>0 and r.cstatus&1=0 where u.cstatus&1=0 and u.roleid&8192>0 ";
            params = new Object[]{};
        } else {
            params = new Object[]{belong,cid};
        }
//        String selectSQL = "select uid,uphone,uaccount,urealname,upw,u.roleid,u.adddate,u.addtime"
//                + ",u.offdate,u.offtime,ip,logindate,logintime,u.cstatus, GROUP_CONCAT(rname) as rname,"
//                + " CONCAT('[',GROUP_CONCAT(arearng,','),']') as arearng from {{?"
//                + DSMConst.TB_SYSTEM_USER + "}} u left join {{?" + DSMConst.TB_SYSTEM_ROLE + "}} r "
//                + " on u.roleid&r.roleid>0 and r.cstatus&1=0 left join {{?"
//                + DSMConst.TB_PROXY_UAREA + "}} ua on ua.uid=u.uid where u.cstatus&1=0 and ua.cstatus&1=0 "
//                + " group by ua.uid";
        sqlBuilder.append(selectSQL);
        sqlBuilder = getParamsDYSQL(sqlBuilder, jsonObject, 0).append(" group by uid desc");
        List<Object[]> queryResult = baseDao.queryNative(pageHolder, page, sqlBuilder.toString(), params);
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
     * @description 查询BD或者BDM详情
     * @params json {uid 用户码}
     * @return 见 UserInfoVo.class
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
     * @description 查看BD或者BDM辖区
     * @params json {uid: 用户码 puid：上级用户码}
     * @return json数组 [{areac 地区码 areaName 地区名 arearng 区域经纬度}]
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
     * @description 设置BD或者BDM默认辖区
     * @params json {areac 地区码 uid 用户码}
     * @return -1失败 200成功
     * @exception
     * @author 11842
     * @time  2019/5/30 14:31
     * @version 1.1.1
     **/
    @UserPermission(ignore = false)
    public Result setDefaultAreas(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int uid = jsonObject.get("uid").getAsInt();
        int suid = appContext.getUserSession().userId;
        JsonArray superiorArr = getSuperiorArea(suid);
        List<Object[]> params;
        String optSQL = "insert into {{?" + DSMConst.TB_PROXY_UAREA + "}} (unqid,uid,areac,cstatus,arearng) "
                + " values(?,?,?,?,?)";
        if (superiorArr.size() > 0) {
            //修改用户综合状态码 256（设置全部辖区）
            if (updOrin(uid)) {
                params = getDefaultAreaParams(superiorArr, uid);
                boolean b = !ModelUtil.updateTransEmpty(baseDao.updateBatchNative(optSQL, params, params.size()));
                return b ? result.success("设置成功") : result.fail("设置失败");
            } else {
                return result.fail("设置失败");
            }
        } else {
            return  result.fail(appContext.getUserSession().userName + "下无辖区！");
        }
    }

    private boolean updOrin(int uid) {
        List<Object[]> paramsOne = new ArrayList<>();
        String updUserSQL = "update {{?" + DSMConst.TB_SYSTEM_USER + "}} set cstatus=cstatus|256 where "
                + " cstatus&1=0 and uid=" + uid;
        paramsOne.add(new Object[]{});
        String updSQL = "update {{?" + DSMConst.TB_PROXY_UAREA + "}} set cstatus=cstatus|1 where cstatus&1=0 "
                + " and uid=" + uid;
        paramsOne.add(new Object[]{});
        return !ModelUtil.updateTransEmpty(baseDao.updateTransNative(new String[]{updSQL, updUserSQL},paramsOne));
    }

    //根据用户id获取用户所有的管辖区域
    private JsonArray getSuperiorArea(int suid) {
        JsonArray superiorArr = new JsonArray();
        String selectPSQL = "select areac, arearng from {{?" + DSMConst.TB_PROXY_UAREA + "}}  where cstatus&1=0 and uid=" + suid;
        List<Object[]> qPResult = baseDao.queryNative(selectPSQL);
        if (qPResult == null || qPResult.isEmpty()) return superiorArr;
        for (Object[] o : qPResult) {
            JsonObject areaObj = new JsonObject();
            areaObj.addProperty("areac", String.valueOf(o[0]));
            areaObj.addProperty("arearng", String.valueOf(o[1]));
            superiorArr.add(areaObj);
        }
        return superiorArr;
    }

    private List<Object[]> getDefaultAreaParams(JsonArray superiorArr, int uid) {
        List<Object[]> params = new ArrayList<>();
        for (int i = 0; i < superiorArr.size(); i++) {
            JsonObject elementObj = superiorArr.get(i).getAsJsonObject();
            params.add(new Object[]{GenIdUtil.getUnqId(), uid, elementObj.get("areac").getAsString(),
                    0, elementObj.get("arearng").getAsString()});
        }
        return params;
    }

    /* *
     * @description 批量设置管辖区域
     * @params json {uid 用户码 areaArr[{areac 地区码 arearng 经纬度数组字符串}]}
     * @return -1失败 200成功
     * @exception
     * @author 11842
     * @time  2019/6/4 11:25
     * @version 1.1.1
     **/
    @UserPermission(ignore = false)
    public Result setAreaArr(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        List<Object[]> paramsOne = new ArrayList<>();
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int uid = jsonObject.get("uid").getAsInt();
        int type = jsonObject.get("type").getAsInt();
        //删除之前的
        String updSQL = "update {{?" + DSMConst.TB_PROXY_UAREA + "}} set cstatus=cstatus|1 where cstatus&1=0 "
                + " and uid=" + uid;
        paramsOne.add(new Object[]{});
        String updUserSQL = "update {{?" + DSMConst.TB_SYSTEM_USER + "}} set cstatus=cstatus&~256 where "
                + " cstatus&1=0 and uid=" + uid;
        paramsOne.add(new Object[]{});
        boolean code = !ModelUtil.updateTransEmpty(baseDao.updateTransNative(new String[]{updSQL, updUserSQL},paramsOne));
        if(code) {
            List<Object[]> params  = new ArrayList<>();
            JsonArray areaArr = jsonObject.get("areaArr").getAsJsonArray();
            for (int i = 0; i < areaArr.size(); i++) {
                String arearng;
                JsonElement areaObj = areaArr.get(i);
                long areac =areaObj.getAsJsonObject().get("areac").getAsLong();
                if (type == 1) {
                    String arean = IceRemoteUtil.getCompleteName(areac+"");
//                    System.out.println("arean00000000000000000000---------- " +  arean);
                    List<List<GaoDeMapUtil.Point>> lists =  GaoDeMapUtil.areaPolyline(arean);
                    for (List<GaoDeMapUtil.Point> plist : lists){
                        String jwp = GsonUtils.javaBeanToJson(plist);
                        params.add(new Object[]{GenIdUtil.getUnqId(), uid, areac, 128, jwp});
                    }
                } else {
                    arearng = areaObj.getAsJsonObject().get("arearng").getAsString();
                    params.add(new Object[]{GenIdUtil.getUnqId(), uid, areac, 0, arearng});
                }
            }
            String optSQL = "insert into {{?" + DSMConst.TB_PROXY_UAREA + "}} (unqid,uid,areac,cstatus,arearng) "
                    + " values(?,?,?,?,?)";
            boolean b = !ModelUtil.updateTransEmpty(baseDao.updateBatchNative(optSQL, params, params.size()));
            return b ? result.success("设置成功") : result.fail("设置失败");
        }
        return result.fail("设置失败");
    }

    /**
     * 功能: BDM下是否存在BD
     * 参数类型: json {ruid: 所选BDM uid}
     * 返回值: false 不存在
     * 详情说明:
     */
    @UserPermission(ignore = false)
    public Result BDMHasBD(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int ruid = jsonObject.get("ruid").getAsInt();
        String selectSQL = "select count(*) from {{?" + DSMConst.TB_SYSTEM_USER + "}} where cstatus&1=0 "
                + " and belong=" + ruid;
        List<Object[]> qResult = baseDao.queryNative(selectSQL);
        long count = Long.parseLong(String.valueOf(qResult.get(0)[0]));
        return count > 0 ? result.success(true) : result.success(false);
    }

    @UserPermission(ignore = true)
    public Result getAreaByUid(AppContext context) {
        String json = context.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        Result result = new Result();
        int puid = jsonObject.get("puid").getAsInt();
        long roleId = context.getUserSession().roleCode;
        String selectSQL;
        List<Object[]> queryRet;
        if ((roleId & 1024) > 0) {//渠道经理
            String sSQL = "select distinct areac from {{?" + DSMConst.TB_PROXY_UAREA +"}} where uid=? "
                    + " and cstatus&1=0 and cstatus&128=0 ";
            List<Object[]> queryResult = baseDao.queryNative(sSQL, puid);
            if (queryResult == null || queryResult.isEmpty()) return result.success(null);
            StringBuilder areaStrSB = new StringBuilder("^(");
            StringBuilder areaCSB = new StringBuilder();
            queryResult.forEach(areaC -> {
                areaStrSB.append(String.valueOf(areaC[0]), 0, 4).append("|");
                areaCSB.append(String.valueOf(areaC[0])).append(",");
            });
            String areaStr = areaStrSB.toString().substring(0, areaStrSB.toString().length() - 1) + ")[0-9]+";
            String areaCStr = areaCSB.toString().substring(0, areaCSB.toString().length() - 1);
            selectSQL = "select areac,arean from {{?"+DSMConst.TB_AREA_PCA +"}} where cstatus&1=0 "
                    + " and areac REGEXP ? and areac not in(" + areaCStr +")";
            queryRet = baseDao.queryNative(selectSQL, areaStr);
        } else {//合伙人
            selectSQL = "select distinct ura.areac,arean from {{?"+DSMConst.TB_PROXY_UAREA +"}} ura," +
                    "{{?"+ DSMConst.TB_AREA_PCA+"}} pca where ura.areac = pca.areac" +
                    " and uid = ? and ura.cstatus & 1 = 0 and ura.cstatus&128=0";
            queryRet = baseDao.queryNative(selectSQL, puid);
        }
        if(queryRet == null || queryRet.isEmpty()){
            return result.success(null);
        }
        JsonArray array = new JsonArray();
        queryRet.forEach(qr -> {
            JsonObject object = new JsonObject();
            object.addProperty("areac", String.valueOf(qr[0]));
            object.addProperty("arean", String.valueOf(qr[1]));
            array.add(object);
        });
        return result.success(array);
    }


    @UserPermission(ignore = true)
    public Result getAreas(AppContext context) {
        String json = context.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        Result result = new Result();
        int uid = jsonObject.get("uid").getAsInt();
        String selectSQL = "select distinct areac from {{?"+DSMConst.TB_PROXY_UAREA +"}} " +
                " where uid = ? and cstatus&1 = 0 ";
        List<Object[]> queryRet = baseDao.queryNative(selectSQL, uid);
        if(queryRet == null || queryRet.isEmpty()){
            return result.success(null);
        }
        List<String> areaCL = new ArrayList<>();
        queryRet.forEach(qr -> areaCL.add(String.valueOf(qr[0])));
        return result.success(areaCL);
    }

}
