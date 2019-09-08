package com.onek.user;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.consts.CSTATUS;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.user.entity.BDAchievementVO;
import com.onek.user.entity.ProxyStoreVO;
import com.onek.user.entity.RoleVO;
import com.onek.user.entity.UserInfoVo;
import com.onek.user.operations.BDAchievementOP;
import com.onek.util.GenIdUtil;
import com.onek.util.IceRemoteUtil;
import com.onek.util.RoleCodeCons;
import com.onek.util.area.AreaEntity;
import constant.DSMConst;
import dao.BaseDAO;
import org.hyrdpf.util.LogUtil;
import util.*;

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

  /* *
   * @description 新增或者修改用户
   * @params [uid用户码（新增传0）,uphone（手机号）,uaccount（账户）,urealname（真实姓名）,
   * upw（密码默认手机号后六位）,roleid（角色码）,adddate,addtime,belong（上级（地推））]
   * @return -1失败（参数错误 用户已存在 ） 200成功
   * @exception
   * @author 11842
   * @time  2019/6/5 13:49
   * @version 1.1.1
   **/
    @UserPermission(ignore = true)
    public Result insertOrUpdUser(AppContext appContext) {
        String json = appContext.param.json;
        UserInfoVo userInfoVo = GsonUtils.jsonToJavaBean(json, UserInfoVo.class);
        if (userInfoVo != null) {
            if (userInfoVo.getUphone() <= 0 || userInfoVo.getUpw() == null || userInfoVo.getUpw().isEmpty()) {
                return new Result().fail("参数错误！");
            }
            List<String> sqlList = new ArrayList<>();
            List<Object[]> parmList = new ArrayList<>();

            String queryAreaExtSql = "select 1 from {{?"+DSMConst.TB_PROXY_UAREA+"}} where  uid = ? and areac = ? and cstatus & 1 = 0 ";
            String pwd = EncryptUtils.encryption(String.valueOf(userInfoVo.getUphone()).substring(5));
            if (userInfoVo.getUid() <= 0) {

                if((userInfoVo.getRoleid() & RoleCodeCons._PROXY_DIRECTOR) > 0){
                    String queryDirect = "select 1 from {{?"+DSMConst.TB_SYSTEM_USER+"}} where roleid & 512 > 0 and cstatus & 1 = 0";
                    List<Object[]> objs  = baseDao.queryNative(queryDirect);
                    if(objs != null && !objs.isEmpty()){
                        return new Result().fail("渠道总监不允许新增多个，用户操作失败！");
                    }
                }

                if (checkUser(userInfoVo)) return new Result().fail("该用户已存在！");
                String insertSQL = "insert into {{?" + DSMConst.TB_SYSTEM_USER + "}} "
                        + "(uid,uphone,uaccount,urealname,upw,roleid,adddate,addtime,belong)"
                        + " values (?,?,?,?,?,?,CURRENT_DATE,CURRENT_TIME,?)";

                sqlList.add(insertSQL);

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
                            if(isExt == null || isExt.isEmpty()){
                                sqlList.add(insertAreaSql);
                                parmList.add(new Object[]{GenIdUtil.getUnqId(),userInfoVo.getUid(),areaArry[i],
                                        0});
                            }
                        }
                    }
                }
            } else {
                String updSQL = "update {{?" + DSMConst.TB_SYSTEM_USER + "}} set uphone=?,uaccount=?,"
                        + "urealname=?, roleid=?, upw=? where cstatus&1=0 and uid=? ";
                sqlList.add(updSQL);
                parmList.add(new Object[]{userInfoVo.getUphone(),userInfoVo.getUaccount(),
                        userInfoVo.getUrealname(),userInfoVo.getRoleid(),pwd,userInfoVo.getUid()});

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
                                    if(isExt == null || isExt.isEmpty()){
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


    /* *
     * @description 用户启用停用
     * @params [type（0， 停用  1 启用）]
     * @return -1失败 200成功
     * @exception
     * @author 11842
     * @time  2019/6/5 13:52
     * @version 1.1.1
     **/
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

        String queryRole = "select roleid from {{?"+DSMConst.TB_SYSTEM_USER+"}} where  uid = ? and cstatus & 1 = 0 ";
        List<Object[]> ret = baseDao.queryNative(queryRole, new Object[]{uid});
        if(ret == null || ret.isEmpty()){
            return result.fail("操作失败");
        }

        long roleid = Long.parseLong(ret.get(0)[0].toString());

        if((roleid & RoleCodeCons._PROXY_DIRECTOR) > 0 ){
            return result.fail("渠道总监不允许停用！");
        }

        if((roleid & RoleCodeCons._PROXY_MGR) > 0 ){
            String mgrSql = "select 1 from {{?"+DSMConst.TB_SYSTEM_USER+"}} where belong = ? and cstatus & 1 = 0";
            List<Object[]> mgrRet = baseDao.queryNative(mgrSql, uid);
            if(mgrRet != null && !mgrRet.isEmpty()){
                return result.fail("当前渠道经理不能被停用，存在关联下属！需解除关联下属关系，才可被停用！");
            }
        }

        int code = baseDao.updateNative(updateSql, uid);
        if (code > 0) {
            return result.success("操作成功");
        }
        return result.fail("操作失败");
    }


    /* *
     * @description 用户查询接口
     * @params [pageSize：每页数量 pageNo： 第几页  urealname：真实姓名 roleid：角色码 uphone：电话号码 cstatus：用户状态
     * areac：所在地区码 ]
     * @return 见UserInfoVo.class
     * @exception
     * @author 11842
     * @time  2019/6/5 13:54
     * @version 1.1.1
     **/
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
                + "  where pca.areac = uarea.areac and uarea.cstatus&1 = 0 group by uid) a on a.uid = u.uid "
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

      //  baseDao.convToEntity(queryResult, userInfoVos, UserInfoVo.class);
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
                    areaSb.append(Long.parseLong(areac)).append(",");
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
                    uidSb.append(objs[0].toString()).append(",");
                }
            }
            String uidStr = uidSb.toString();
            if(uidStr.endsWith(",")){
                uidStr = uidStr.substring(0,uidStr.length() - 1);
            }

            if(!StringUtils.isEmpty(uidStr)){
                sqlBuilder.append(" and u.uid in (").append(uidStr).append(")");
            }else{
                sqlBuilder.append(" and 1=2 ");
            }
        }

        if((mroleid & RoleCodeCons._PROXY_DIRECTOR) > 0){
            sqlBuilder.append(" and u.roleid & ");
            sqlBuilder.append(RoleCodeCons._PROXY_MGR);
            sqlBuilder.append(" > 0 ");
            sqlBuilder.append(" and u.belong = ").append(puid);
        }

        if((mroleid & RoleCodeCons._SYS) > 0){
            sqlBuilder.append(" and u.roleid & ");
            sqlBuilder.append(RoleCodeCons._PROXY_PARTNER+RoleCodeCons._DBM
                    +RoleCodeCons._DB+RoleCodeCons._PROXY_MGR);
            sqlBuilder.append(" = 0 ");
        }

        if((mroleid & (RoleCodeCons._SYS+RoleCodeCons._PROXY_DIRECTOR)) == 0){
            sqlBuilder.append(" and u.roleid & ");
            sqlBuilder.append(RoleCodeCons._PROXY_PARTNER+RoleCodeCons._DBM
                    +RoleCodeCons._DB+RoleCodeCons._PROXY_MGR+RoleCodeCons._PROXY_DIRECTOR);
            sqlBuilder.append(" = 0 ");
        }


//        sqlBuilder.append(RoleCodeCons._PROXY_DIRECTOR);
//        sqlBuilder.append(" > 0 ");

        return sqlBuilder;
    }

    /* *
     * @description 获取用户详情
     * @params [uid：用户码 ]
     * @return 见UserInfoVo.class
     * @exception
     * @author 11842
     * @time  2019/6/5 13:58
     * @version 1.1.1
     **/
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

    /* *
     * @description 重置密码
     * @params [uid：用户码]
     * @return -1 失败  200 成功
     * @exception
     * @author 11842
     * @time  2019/6/5 13:59
     * @version 1.1.1
     **/
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
        sqlSb.append("}} WHERE cstatus&33 = 0 ");

//        if((roleid & RoleCodeCons._PROXY_DIRECTOR) > 0){
//            int addRole = RoleCodeCons._PROXY_MGR
//                    + RoleCodeCons._PROXY_PARTNER
//                    + RoleCodeCons._DBM
//                    + RoleCodeCons._DB;
//            sqlSb.append(" AND roleid &  ").append(addRole).append(" > 0 ");
//        }
        if ((roleid & RoleCodeCons._SYS) > 0 ) {
            sqlSb.append(" AND roleid &  ")
                    .append(RoleCodeCons._PROXY_MGR|RoleCodeCons._PROXY_PARTNER
                            |RoleCodeCons._DBM|RoleCodeCons._DB).append(" = 0 ");
        } else if ((roleid & RoleCodeCons._OPER) > 0) {
            sqlSb.append(" AND roleid & 1 = 0  AND roleid &  ")
                    .append(RoleCodeCons._PROXY_MGR|RoleCodeCons._PROXY_PARTNER
                            |RoleCodeCons._DBM|RoleCodeCons._DB).append(" = 0 ");
        } else if ((roleid & RoleCodeCons._PROXY_DIRECTOR) > 0) {
            sqlSb.append(" AND roleid & 1 = 0  AND roleid &  ").append(RoleCodeCons._PROXY_MGR).append(" > 0 ");
        } else {
            sqlSb.append(" AND roleid & 1 = 0 ");
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

    /**
     * 获取余额抵扣百分比
     * @param appContext
     * @return
     */
    @UserPermission(ignore = true)
    public String getUseBal(AppContext appContext) {
        String key = appContext.param.arrays[0];

        if(key.isEmpty()){
            key = "BALANCE_DEDUCTION";
        }
        String sql = "select conf.value from {{?"+DSMConst.TB_SYSTEM_CONFIG+"}} conf where conf.varname = ? and conf.cstatus & 1 =0";
        List<Object[]> vlist = baseDao.queryNative(sql,key);
        System.out.println("余额抵扣百分比："+vlist.get(0)[0].toString());
        return vlist.get(0)[0].toString();
    }



    private static final String _QUERY_BDUSER_INFO = " select distinct cp.cid companyId, tu.uphone phone,tu.uid,cname company,caddrcode addressCode, " +
                                                    " caddr address,cp.auditdate,cp.audittime,cp.cstatus,stu.uid cursorId,stu.urealname cursorName, "+
                                                    " stu.uphone cursorPhone,"+
                                                    " control,cp.storetype storetype  FROM {{?"+DSMConst.TB_COMP+"}} cp, {{?"+DSMConst.TB_SYSTEM_USER+"}} tu, "+
                                                    " {{?"+DSMConst.TB_SYSTEM_USER+"}} stu WHERE ctype = 0 and tu.cid = cp.cid and stu.uid = cp.inviter ";


    /**
     * 内部查询门店BEAN
     */
    private class Param{
        private int selectFlag; //查询门店类型 1-审核成功  2-审核失败  0-所有
        private long uid; //当前用户id
        private long roleid; //当前用户权限id
        private String sdate; //开始时间
        private String edate; //结束时间
        private String addrcode;//地区
    }
    /**
     * 获取BD用户信息
     * @param appContext
     * @return
     */
    public Result getBdUserInfo(AppContext appContext){
        long loginroleid = appContext.getUserSession().roleCode;

        Page page = new Page();
        page.pageIndex = appContext.param.pageIndex;
        page.pageSize = appContext.param.pageNumber;

        PageHolder pageHolder = new PageHolder(page);

        String json = appContext.param.json;
        Param param = GsonUtils.jsonToJavaBean(json,Param.class);
        StringBuilder sb = new StringBuilder(_QUERY_BDUSER_INFO);
        if(param == null){
            return new Result().fail("查询条件不足！");
        }
        //判断是否具有权限
        if((loginroleid&1)>0){ //过滤超级管理员
        }else {
            boolean flag = getRole(loginroleid, param.roleid);
            if (!flag) {
                return new Result().fail("当前用户无权限查询上级数据");
            }
        }

        String selectParams = BDAchievementOP.getBDUser(param.uid,param.roleid);
        if(StringUtils.isEmpty(selectParams)){
            return new Result().fail("当前人员暂无管理企业信息！");
        }
        sb.append(" and cp.inviter in ( ").append(selectParams).append(" ) ");
        if(param.selectFlag == 1){//查询审核通过门店
            sb.append(" and cp.cstatus&256=256 ");
        }else if(param.selectFlag == 2){//查询审核不通过门店
            sb.append(" and cp.cstatus&256!=256");
        }else{//查询所有

        }

        //时间查询
        sb.append(" and cp.auditdate BETWEEN ? and ? ");
        if(!StringUtils.isEmpty(param.addrcode) && !"430000000000".equals(param.addrcode)){
            sb.append(" AND cp.caddrcode = "+ param.addrcode );
        }
        //分组以及排序
        sb.append(" GROUP BY cp.cid desc  ");

        List<Object[]> queryResult = baseDao.queryNative(pageHolder, page, "cp.submitdate DESC, cp.submittime DESC", sb.toString(),param.sdate,param.edate);
        ProxyStoreVO[] proxyStoreVOS = new ProxyStoreVO[queryResult.size()];
        if (queryResult == null || queryResult.isEmpty()) return new Result().setQuery(proxyStoreVOS,pageHolder);

        baseDao.convToEntity(queryResult, proxyStoreVOS, ProxyStoreVO.class,
                "companyId","phone","uid","company","addressCode","address","createdate",
                "createtime","status","cursorId","cursorName","cursorPhone","control","storetype");

        for (ProxyStoreVO proxyStoreVO : proxyStoreVOS){
            AreaEntity[] ancestors = IceRemoteUtil.getAncestors(Long.parseLong(proxyStoreVO.getAddressCode()));
            if(ancestors != null && ancestors.length > 0){
                LogUtil.getDefaultLogger().debug(ancestors[0].getArean());
                proxyStoreVO.setProvince(ancestors[0].getArean());
                if (ancestors.length > 1) {
                    proxyStoreVO.setCity(ancestors[1].getArean());
                    if (ancestors.length > 2) {
                        proxyStoreVO.setRegion(ancestors[2].getArean());
                    }
                }
            }
        }
        return new Result().setQuery(proxyStoreVOS,pageHolder);
    }


    /**
     * 获取当前地区的BDM用户
     * @return
     */
    @UserPermission(ignore = true)
    public Result getBDMByAreaCode(AppContext appContext){
        String[] arr = appContext.param.arrays;
        List<String> list = BDAchievementOP.getArea(arr[0]);
        String json = "";
        for (String str : list){
            json += str+",";
        }
        return new Result().success(json.substring(0,json.length()-1));
    }


    /**
     * 判断当前用户是否有权限查询上级
     * @param loginrole 登陆用户
     * @param nowrole //查询的用户
     * @return
     */
    public static boolean getRole(long loginrole,long nowrole) {
        int[] a = new int[]{512,1024,2048,4096,8192};

        long login_maxrole = Long.MAX_VALUE;

        long now_maxrole = Long.MAX_VALUE;
        long login_roleid = loginrole;

        long now_roleid = nowrole;
        for (int i: a){
            if((login_roleid&i)>0){
                login_maxrole = Math.min(i,login_maxrole);

            }
            if((now_roleid&i)>0){
                now_maxrole = Math.min(i,now_maxrole);
            }
        }
        if(login_maxrole>now_maxrole){
           return false;
        }else{
            return true;
        }
    }
}
