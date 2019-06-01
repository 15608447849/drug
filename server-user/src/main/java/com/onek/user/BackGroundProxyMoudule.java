package com.onek.user;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.user.entity.*;
import com.onek.util.GenIdUtil;
import com.onek.util.IceRemoteUtil;
import com.onek.util.RedisGlobalKeys;
import com.onek.util.RoleCodeCons;
import com.onek.util.area.AreaEntity;
import constant.DSMConst;
import dao.BaseDAO;
import util.*;

import java.util.*;

import static com.onek.util.RedisGlobalKeys.getUserCode;
import static constant.DSMConst.TB_COMP;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName BackGroundProxyMoudule
 * @Description TODO
 * @date 2019-05-28 18:21
 */
public class BackGroundProxyMoudule {

    private static final BaseDAO baseDao = BaseDAO.getBaseDAO();

    private final String  QUERY_MY_AREAC = "select ura.areac,arean from {{?"+DSMConst.TB_PROXY_UAREA +"}} ura," +
            "{{?"+ DSMConst.TB_AREA_PCA+"}} pca where ura.areac = pca.areac" +
            " and uid = ? and ura.cstatus & 1 = 0 ";


    private final String QUERY_DIST_AREAC = "select ura.areac,arean,su.uid,su.uphone,su.urealname from {{?"+DSMConst.TB_PROXY_UAREA+"}} ura join " +
            "{{?"+ DSMConst.TB_SYSTEM_USER+"}} su  on ura.uid = su.uid " +
            " join {{?"+ DSMConst.TB_AREA_PCA+"}} pca on ura.areac = pca.areac " +
            " where roleid & ? > 0 and ura.cstatus & 1 = 0 " +
            " and su.cstatus & 1 = 0 ";

    @UserPermission(ignore = true)
    public Result getAreaParms(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        Result result = new Result();
        int roleid = jsonObject.get("role").getAsInt();
        int uid = jsonObject.get("uid").getAsInt();

        if(roleid <= 0 || uid <= 0){
            return result.success(null);
        }

        if((roleid & RoleCodeCons._SYS) > 0){
            AreaEntity[] children = IceRemoteUtil.getChildren(430000000000L);
         //   AreaEntity[] children = AreaStore.getChildren(430000000000L);
            List<ProxyAreaTreeVO>  areaList = new ArrayList<>();
            ProxyAreaTreeVO parent = new ProxyAreaTreeVO();
            parent.setAreac("430000000000");
            parent.setArean("湖南省");
            areaList.add(parent);
            for (AreaEntity areaEntity : children){
                ProxyAreaTreeVO proxyAreaVO = new ProxyAreaTreeVO();
                proxyAreaVO.setAreac(areaEntity.getAreac()+"");
                proxyAreaVO.setArean(areaEntity.getArean());
                areaList.add(proxyAreaVO);
             //   AreaEntity[] subChildren = AreaStore.getChildren(areaEntity.getAreac());
                AreaEntity[] subChildren = IceRemoteUtil.getChildren(areaEntity.getAreac());
                for(AreaEntity subArea : subChildren){
                    ProxyAreaTreeVO subProxyAreaVO = new ProxyAreaTreeVO();
                    subProxyAreaVO.setAreac(subArea.getAreac()+"");
                    subProxyAreaVO.setArean(subArea.getArean());
                    subProxyAreaVO.setLayer(1);
                    areaList.add(subProxyAreaVO);
                }
            }
            List<ProxyAreaTreeVO> iTreeNodes = TreeUtil.list2Tree(areaList);
            return result.success(iTreeNodes);
        }

        //渠道经理
        if((roleid & RoleCodeCons._PROXY_DIRECTOR) > 0){
            AreaEntity[] children = IceRemoteUtil.getChildren(430000000000L);
           // AreaEntity[] children = AreaStore.getChildren(430000000000L);
            List<ProxyAreaTreeVO>  areaList = new ArrayList<>();
            ProxyAreaTreeVO parent = new ProxyAreaTreeVO();
            parent.setAreac("430000000000");
            parent.setArean("湖南省");
            areaList.add(parent);
            for (AreaEntity areaEntity : children){
                ProxyAreaTreeVO proxyAreaVO = new ProxyAreaTreeVO();
                proxyAreaVO.setAreac(areaEntity.getAreac()+"");
                proxyAreaVO.setArean(areaEntity.getArean());
                areaList.add(proxyAreaVO);
            }
            List<ProxyAreaTreeVO> iTreeNodes = TreeUtil.list2Tree(areaList);
            return result.success(iTreeNodes);
        }

        //合伙人
        if((roleid & RoleCodeCons._PROXY_MGR) > 0){
            List<Object[]> queryRet = baseDao.queryNative(QUERY_MY_AREAC, uid);
            if(queryRet == null || queryRet.isEmpty()){
                return result.success(null);
            }

            List<ProxyAreaTreeVO>  areaList = new ArrayList<>();
            for (Object[] objects : queryRet){
                ProxyAreaTreeVO proxyAreaVO = new ProxyAreaTreeVO();
                proxyAreaVO.setLayer(1);
                proxyAreaVO.setArean(objects[1].toString());
                proxyAreaVO.setAreac((objects[0].toString()));
                areaList.add(proxyAreaVO);
                AreaEntity[] children = IceRemoteUtil.getChildren(Long.parseLong(objects[0].toString()));
              //  AreaEntity[] children = AreaStore.getChildren(Long.parseLong(objects[0].toString()));
                for (AreaEntity areaEntity : children){
                    ProxyAreaTreeVO cproxyAreaVO = new ProxyAreaTreeVO();
                    proxyAreaVO.setLayer(1);
                    cproxyAreaVO.setAreac(areaEntity.getAreac()+"");
                    cproxyAreaVO.setArean(areaEntity.getArean());
                    areaList.add(cproxyAreaVO);
                }
            }
            List<ProxyAreaTreeVO> iTreeNodes = TreeUtil.list2Tree(areaList);
            return result.success(iTreeNodes);
        }
        return result.success(null);
    }


    @UserPermission(ignore = true)
    public Result queryProxyArea(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        Result result = new Result();
        int roleid = jsonObject.get("role").getAsInt();
        int uid = jsonObject.get("uid").getAsInt();
        int suid = jsonObject.get("suid").getAsInt();
        int ctype = jsonObject.get("ctype").getAsInt();

        if(roleid <= 0 || uid <= 0){
            return result.success(null);
        }

        switch (ctype){
            case 1:
                return  result.success(getMyProxyAreac(roleid,suid,uid));
            case 2:
                return  result.success(getAddProxyAreac(roleid,uid));
            case 3:
                return  result.success(getOtherProxyAreac(roleid,suid,uid));
        }

        return result.success(null);
    }

    public List<ProxyAreaTreeVO> getMyProxyAreac(int roleid,int suid,int uid){

        if(suid <= 0){
            return null;
        }
        List<Object[]> queryRet = baseDao.queryNative(QUERY_MY_AREAC, suid);
        //渠道总监
        if((roleid & RoleCodeCons._PROXY_DIRECTOR) > 0){
            List<ProxyAreaTreeVO>  mList = new ArrayList<>();
            ProxyAreaTreeVO parent = new ProxyAreaTreeVO();
            parent.setAreac("430000000000");
            parent.setArean("湖南省");
            mList.add(parent);
            for (Object[] objects : queryRet) {
                ProxyAreaTreeVO proxyAreaVO = new ProxyAreaTreeVO();
                proxyAreaVO.setArean(objects[1].toString());
                proxyAreaVO.setAreac((objects[0].toString()));
                mList.add(proxyAreaVO);
            }
            return TreeUtil.list2Tree(mList);
        }

        //渠道经理
        if((roleid & RoleCodeCons._PROXY_MGR) > 0){
            List<Object[]> queryParentRet = baseDao.queryNative(QUERY_MY_AREAC, uid);
            if(queryParentRet == null || queryParentRet.isEmpty()){
                return null;
            }

            List<ProxyAreaTreeVO>  areaList = new ArrayList<>();
            for (Object[] objects : queryParentRet){
                ProxyAreaTreeVO proxyAreaVO = new ProxyAreaTreeVO();
                proxyAreaVO.setArean(objects[1].toString());
                proxyAreaVO.setAreac((objects[0].toString()));
                proxyAreaVO.setLayer(1);
                areaList.add(proxyAreaVO);

                for (Object[] objs : queryRet) {
                    ProxyAreaTreeVO pproxyAreaVO = new ProxyAreaTreeVO();
                    pproxyAreaVO.setArean(objs[1].toString());
                    pproxyAreaVO.setAreac((objs[0].toString()));
                    proxyAreaVO.setLayer(1);
                    areaList.add(pproxyAreaVO);
                }
            }
            return TreeUtil.list2Tree(areaList);
        }
        return null;
    }


    public List<ProxyAreaTreeVO> getAddProxyAreac(int roleid,int uid){

        if(uid <= 0){
            return null;
        }

        if((roleid & RoleCodeCons._PROXY_DIRECTOR) > 0){
            List<Object[]> queryRet = baseDao.queryNative(QUERY_DIST_AREAC,
                    RoleCodeCons._PROXY_MGR);
            AreaEntity[] children = IceRemoteUtil.getChildren(430000000000L);
           // AreaEntity[] children = AreaStore.getChildren(430000000000L);
            List<AreaEntity> chList = Arrays.asList(children);
            Iterator<AreaEntity> chListIterator = chList.iterator();

            while(chListIterator.hasNext()){
                AreaEntity areaEntity = chListIterator.next();
                for (Object[] objs : queryRet){
                    if(objs[0].toString().equals(areaEntity.getAreac())){
                        chListIterator.remove();
                    }
                }
            }

            List<ProxyAreaTreeVO>  mList = new ArrayList<>();
            ProxyAreaTreeVO parent = new ProxyAreaTreeVO();
            parent.setAreac("430000000000");
            parent.setArean("湖南省");
            mList.add(parent);
            for (AreaEntity areaEntity : chList){
                ProxyAreaTreeVO proxyAreaVO = new ProxyAreaTreeVO();
                proxyAreaVO.setAreac(areaEntity.getAreac()+"");
                proxyAreaVO.setArean(areaEntity.getArean());
                mList.add(proxyAreaVO);
            }

            return TreeUtil.list2Tree(mList);
        }

        //合伙人
        if((roleid & RoleCodeCons._PROXY_MGR) > 0){
            List<Object[]> queryRet = baseDao.queryNative(QUERY_DIST_AREAC,
                    RoleCodeCons._PROXY_PARTNER);
            List<Object[]> queryParentRet = baseDao.queryNative(QUERY_MY_AREAC, uid);
            if(queryParentRet == null || queryParentRet.isEmpty()){
                return null;
            }

            List<ProxyAreaTreeVO>  areaList = new ArrayList<>();
            for (Object[] objects : queryParentRet){
                ProxyAreaTreeVO proxyAreaVO = new ProxyAreaTreeVO();
                proxyAreaVO.setArean(objects[1].toString());
                proxyAreaVO.setAreac((objects[0].toString()));
                proxyAreaVO.setLayer(1);
                areaList.add(proxyAreaVO);

                AreaEntity[] children = IceRemoteUtil.getChildren(Long.parseLong(objects[0].toString()));
                      //  AreaEntity[] children = AreaStore.getChildren(Long.parseLong(objects[0].toString()));
                for (AreaEntity areaEntity : children){
                    ProxyAreaTreeVO cproxyAreaVO = new ProxyAreaTreeVO();
                    cproxyAreaVO.setAreac(areaEntity.getAreac()+"");
                    cproxyAreaVO.setArean(areaEntity.getArean());
                    proxyAreaVO.setLayer(1);
                    areaList.add(cproxyAreaVO);
                }
            }

            Iterator<ProxyAreaTreeVO> proxyAreaVOIterator = areaList.iterator();
            while(proxyAreaVOIterator.hasNext()){
                ProxyAreaTreeVO proxyAreaVO = proxyAreaVOIterator.next();
                for (Object[] objs : queryRet){
                    if(objs[0].toString().equals(proxyAreaVO.getAreac())){
                        proxyAreaVOIterator.remove();
                    }
                }
            }

            return TreeUtil.list2Tree(areaList);
        }
        return null;
    }


    public ProxyUareaVO[] getOtherProxyAreac(int roleid,int suid,int uid){

        if(suid <= 0){
            return null;
        }

        StringBuilder sqlbuild = new StringBuilder(QUERY_DIST_AREAC);
        sqlbuild.append(" and su.uid != ").append(suid);


        int roleParm = 0;
        if((roleid & RoleCodeCons._PROXY_DIRECTOR) > 0){
            roleParm = RoleCodeCons._PROXY_MGR;
        }

        //合伙人
        if((roleid & RoleCodeCons._PROXY_MGR) > 0){
            roleParm = RoleCodeCons._PROXY_PARTNER;

       }

        List<Object[]> queryRet = baseDao.queryNative(sqlbuild.toString(),
                roleParm);

            if(queryRet == null || queryRet.isEmpty()){
                return null;
            }

            ProxyUareaVO[] proxyUareaVO = new ProxyUareaVO[queryRet.size()];

            baseDao.convToEntity(queryRet, proxyUareaVO, ProxyUareaVO.class,
                    new String[]{"areac","arean","uid","uphone","urealname"});

            return proxyUareaVO;
    }


    @UserPermission(ignore = true)
    public Result queryPartners(AppContext appContext) {
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

        String selectSQL = "select cp.cid,stu.uid,cname,cp.cstatus ckstatus,tu.cstatus opstatus,tu.urealname "
                + ",createdate,createtime,arean,ctype,tu.roleid,examine from {{?"
                + DSMConst.TB_COMP + "}} cp  join {{?" + DSMConst.TB_SYSTEM_USER + "}} tu  on cp.cid = tu.cid "
                + "left join {{?" + DSMConst.TB_SYSTEM_USER + "}} stu on stu.uid = tu.belong "
                + " left join (select uid,GROUP_CONCAT(pca.arean) as arean "
                + " from {{?"+DSMConst.TB_PROXY_UAREA+"}} uarea,{{?"+DSMConst.TB_AREA_PCA +"}} pca "
                + "  where pca.areac = uarea.areac group by uid) a on a.uid = tu.uid "
                + " where tu.cstatus&1=0 and ctype in (2,3) ";

        sqlBuilder.append(selectSQL);
        sqlBuilder = getgetParamsDYSQL(sqlBuilder, jsonObject).append(" group by tu.uid desc ");
        List<Object[]> queryResult = baseDao.queryNative(pageHolder, page, sqlBuilder.toString());
        if (queryResult == null || queryResult.isEmpty()) return result.success(null);
        ProxyPartnerVO[] proxyPartnerVOS = new ProxyPartnerVO[queryResult.size()];
        baseDao.convToEntity(queryResult, proxyPartnerVOS, ProxyPartnerVO.class,
                new String[]{"cid","uid","cname","ckstatus","opstatus","urealname","createdate",
                "createtime","arean","ctype","roleid","examine"});
        return result.setQuery(proxyPartnerVOS,pageHolder);
    }

    private StringBuilder getgetParamsDYSQL(StringBuilder sqlBuilder, JsonObject jsonObject) {

        String cname = jsonObject.get("cname").getAsString();
        String urealname = jsonObject.get("urealname").getAsString();
        int ckstatus = jsonObject.get("ckstatus").getAsInt()  ;
        String uphone = jsonObject.get("uphone").getAsString();
        int opstatus = jsonObject.get("opstatus").getAsInt();
        String areaStr = jsonObject.get("areac").getAsString();
        int mroleid = jsonObject.get("mroleid").getAsInt();
        int puid = jsonObject.get("puid").getAsInt();

        if (cname != null && !cname.isEmpty()) {
            sqlBuilder.append(" and cp.cname like '%").append(cname).append("%'");
        }

        if (urealname != null && !urealname.isEmpty()) {
            sqlBuilder.append(" and tu.urealname like '%").append(urealname).append("%'");
        }
        //  状态 （1:删除, 128:审核中; 256:认证成功 512:认证失败; 1024:停用）
        if (ckstatus >= 0) {
            sqlBuilder.append(" and cp.cstatus &").append(ckstatus).append(">0");
        }

        if (opstatus == 0 || opstatus == 32) {
            if(opstatus == 0 ){
                sqlBuilder.append(" and u.cstatus&32=0");
            }

            if (opstatus == 32) {
                sqlBuilder.append(" and u.cstatus&32>0");
            }
        }


        if (!StringUtils.isEmpty(uphone) && Long.parseLong(uphone) > 0) {
            sqlBuilder.append(" and uphone=").append(uphone);
        }

        if(!StringUtils.isEmpty(areaStr)){
            String [] areaArry = areaStr.split(",");
            String areaSql = "";
            StringBuilder areaSb = new StringBuilder();

            if((mroleid & RoleCodeCons._PROXY_DIRECTOR) > 0){
                for(String areac : areaArry){
                    if(areac != null && !StringUtils.isEmpty(areac)){
                        areac.substring(0,4);
                        areaSb.append("^");
                        areaSb.append(areac.substring(0,4));
                        areaSb.append("[0-9]{1}[1-9]{1}[0]{6}$");
                        areaSb.append("|");
                    }
                }

                String areacStr = areaSb.toString();
                if(areacStr.endsWith("|")){
                    areacStr = areacStr.substring(0,areacStr.length() - 1);
                }
                areaSql = "select uid from {{?"+DSMConst.TB_PROXY_UAREA+"}} where  cstatus & 1 = 0 and REGEXP'"+areacStr+"'";
            }else if((mroleid & RoleCodeCons._PROXY_MGR) > 0){

                for(String areac : areaArry){
                    if(areac != null && !StringUtils.isEmpty(areac)){
                        areaSb.append(Integer.parseInt(areac)).append(",");
                    }
                }
                String areacStr = areaSb.toString();
                if(areacStr.endsWith(",")){
                    areacStr = areacStr.substring(0,areacStr.length() - 1);
                }
                areaSql = "select uid from {{?"+DSMConst.TB_PROXY_UAREA+"}} where areac in ("+areacStr+") and cstatus & 1 = 0";
            }

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
            sqlBuilder.append(" and tu.uid in (").append(uidStr).append(")");
        }

        //渠道总监
        if((mroleid & RoleCodeCons._PROXY_DIRECTOR) > 0){
            String selectSQL = " select uid from {{?" + DSMConst.TB_SYSTEM_USER + "}} where roleid & 1024 > 0 and belong = ? ";

            List<Object[]> queryResult = baseDao.queryNative(selectSQL,puid);
            if (queryResult != null && !queryResult.isEmpty()) {
                StringBuilder uidSb = new StringBuilder();
                for (Object[] obs: queryResult){
                    uidSb.append(Integer.parseInt(obs[0].toString())).append(",");
                }
                String uidStr = uidSb.toString();
                if(uidStr.endsWith(",")){
                    uidStr = uidStr.substring(0,uidStr.length() - 1);
                }
                sqlBuilder.append(" and u.belong in (").append(uidStr).append(")");
                return sqlBuilder;
            }

        }

        //渠道经理
        if((mroleid & RoleCodeCons._PROXY_MGR) > 0){
            sqlBuilder.append(" and u.belong = ").append(puid);
        }

        return sqlBuilder;
    }


    @UserPermission(ignore = true)
    public Result queryChMgr(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        Result result = new Result();
        int uid = jsonObject.get("uid").getAsInt();
        StringBuilder sqlBuilder = new StringBuilder();
        String selectSQL = " select uid,urealname from {{?" + DSMConst.TB_SYSTEM_USER + "}} where roleid & 1024 > 0 and belong = ? ";
        sqlBuilder.append(selectSQL);
        List<Object[]> queryResult = baseDao.queryNative(selectSQL,uid);
        if (queryResult == null || queryResult.isEmpty()) return result.success(null);
        HashMap<String,String> map = new HashMap<>();

        for (Object[] objects : queryResult){
            map.put(objects[0].toString(),objects[1].toString());
        }
        return  result.success(map);
    }


    @UserPermission(ignore = true)
    public Result insertPartners(AppContext appContext) {
        String json = appContext.param.json;
        ProxyPartnerVO proxyPartnerVO = GsonUtils.jsonToJavaBean(json, ProxyPartnerVO.class);
        if (proxyPartnerVO != null) {
            if (checkUser(proxyPartnerVO)) return new Result().fail("该用户已存在！");
            if (StringUtils.isEmpty(proxyPartnerVO.getUphone())) {
                return new Result().fail("参数错误！");
            }
            List<String> sqlList = new ArrayList<>();
            List<Object[]> parmList = new ArrayList<>();
            if (proxyPartnerVO.getUid() <= 0) {
                String insertUserSQL = "insert into {{?" + DSMConst.TB_SYSTEM_USER + "}} "
                        + "(uid,uphone,urealname,upw,roleid,adddate,addtime,belong,cstatus)"
                        + " values (?,?,?,?,?,?,CURRENT_DATE,CURRENT_TIME,?,32)";

                sqlList.add(insertUserSQL);
                String pwd = EncryptUtils.encryption(String.valueOf(proxyPartnerVO.getUphone()).substring(5));

                int uid = getUserCode();
                parmList.add(new Object[]{uid,
                        proxyPartnerVO.getUphone(), proxyPartnerVO.getUrealname(),
                        pwd, proxyPartnerVO.getRoleid(),proxyPartnerVO.getBelong()});

                String insertCompSql = "INSERT INTO {{?"+ TB_COMP +"}} " +
                        "(ctype,cid,cname,cnamehash,cstatus,createdate,createtime) " +
                        "VALUES(?,?,?,crc32(?),?,CURRENT_DATE,CURRENT_TIME)";

                sqlList.add(insertCompSql);
                long compid = RedisGlobalKeys.getProxyCompanyCode();
                parmList.add(new Object[]{proxyPartnerVO.getCtype(),
                        compid, proxyPartnerVO.getCname(),
                        proxyPartnerVO.getCname(),0});


                if(proxyPartnerVO.getArean() != null
                        && StringUtils.isEmpty(proxyPartnerVO.getArean())){
                    String [] areaArry = proxyPartnerVO.getArean().split(",");

                    String insertAreaSql = "insert into {{?"+DSMConst.TB_PROXY_UAREA+"}}" +
                            " (unqid,uid,areac,cstatus,arearng) values (?,?,?,?,?) ";


                    for(int i = 0; i < areaArry.length; i++){
                        if(areaArry[i] != null && !StringUtils.isEmpty(areaArry[i])){
                            IceRemoteUtil.getCompleteName(areaArry[i]);
//                            String[] allArea =  AreaStore.getCompleteName(Long.parseLong(areaArry[i]));
//                            StringBuilder asb = new StringBuilder();
//                            for (String area : allArea){
//                                asb.append(area);
//                            }
                            List<GaoDeMapUtil.Point> points = GaoDeMapUtil.areaPolyline(IceRemoteUtil.getCompleteName(areaArry[i]));
                            String jwp = GsonUtils.javaBeanToJson(points);
                            sqlList.add(insertAreaSql);


                            parmList.add(new Object[]{GenIdUtil.getUnqId(),uid,areaArry[i],
                                    0,jwp});
                        }
                    }
                }

            String[] sqlNative = new String[sqlList.size()];
            sqlNative = sqlList.toArray(sqlNative);
            boolean b = !ModelUtil.updateTransEmpty(
                    baseDao.updateTransNative(sqlNative,parmList));
            if (b) {
                Map<String,String> map = new HashMap<>();
                        map.put("uid",uid+"");
                map.put("cid",compid+"");
                return new Result().success("操作成功！",map);
            }
          }
        }
        return new Result().fail("用户操作失败！");
    }



    @UserPermission(ignore = true)
    public Result updatePartners(AppContext appContext) {
        String json = appContext.param.json;
        ProxyPartnerVO proxyPartnerVO = GsonUtils.jsonToJavaBean(json, ProxyPartnerVO.class);
        if (proxyPartnerVO != null) {
            if (checkUser(proxyPartnerVO)) return new Result().fail("该用户已存在！");
            if (StringUtils.isEmpty(proxyPartnerVO.getUphone())) {
                return new Result().fail("参数错误！");
            }
            List<String> sqlList = new ArrayList<>();
            List<Object[]> parmList = new ArrayList<>();
            if (proxyPartnerVO.getUid() > 0) {
                String updateUserSql = "update {{?" +DSMConst.TB_SYSTEM_USER +"}} "
                        + " set urealname = ?,uphone = ? where uid = ?";
                sqlList.add(updateUserSql);

                parmList.add(new Object[]{proxyPartnerVO.getUrealname(),
                        proxyPartnerVO.getUphone(),proxyPartnerVO.getUid()});

                String updateCompSql = "update {{?"+DSMConst.TB_COMP +"}} set cname = ?," +
                        " cnamehash = crc32(?) where cid = ?";
                sqlList.add(updateCompSql);
                parmList.add(new Object[]{proxyPartnerVO.getCname(),
                        proxyPartnerVO.getCname(),proxyPartnerVO.getCid()});


                if(proxyPartnerVO.getArean() != null
                        && StringUtils.isEmpty(proxyPartnerVO.getArean())){
                    String [] areaArry = proxyPartnerVO.getArean().split(",");

                    String insertAreaSql = "insert into {{?"+DSMConst.TB_PROXY_UAREA+"}}" +
                            " (unqid,uid,areac,cstatus,arearng) values (?,?,?,?,?) ";

                    String delAreaSql = "update {{?"+DSMConst.TB_PROXY_UAREA+"}} set cstatus = cstatus & 1 " +
                            " where uid = ? and areac = ?";

                    for(int i = 0; i < areaArry.length; i++){
                        if(areaArry[i] != null && !StringUtils.isEmpty(areaArry[i])){
                            if(areaArry[i].startsWith("-")){
                                sqlList.add(delAreaSql);
                                parmList.add(new Object[]{proxyPartnerVO.getUid(),areaArry[i].substring(1)});
                            }else{

                               // String[] allArea =  AreaStore.getCompleteName(Long.parseLong(areaArry[i]));
                                String completeName = IceRemoteUtil.getCompleteName(areaArry[i]);
//                                StringBuilder asb = new StringBuilder();
//                                for (String area : allArea){
//                                    asb.append(area);
//                                }
                                List<GaoDeMapUtil.Point> points = GaoDeMapUtil.areaPolyline(completeName);
                                String jwp = GsonUtils.javaBeanToJson(points);

                                sqlList.add(insertAreaSql);
                                parmList.add(new Object[]{GenIdUtil.getUnqId(),proxyPartnerVO.getUid(),areaArry[i],
                                        0,jwp});
                            }
                        }
                    }
                }
                String[] sqlNative = new String[sqlList.size()];
                sqlNative = sqlList.toArray(sqlNative);
                boolean b = !ModelUtil.updateTransEmpty(
                        baseDao.updateTransNative(sqlNative,parmList));
                if (b) {
                    return new Result().success("操作成功！");
                }
            }
        }
        return new Result().fail("用户操作失败！");
    }



    @UserPermission(ignore = true)
    public Result reviewPartners(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int roleid = jsonObject.get("roleid").getAsInt();
        int uid = jsonObject.get("uid").getAsInt();
        int cid = jsonObject.get("cid").getAsInt();
        int ctype = jsonObject.get("ctype").getAsInt();
        int ckstatus = jsonObject.get("ckstatus").getAsInt();
        int ckreson = jsonObject.get("ckreson").getAsInt();
        int suid = jsonObject.get("suid").getAsInt();

        if((roleid & RoleCodeCons._PROXY_DIRECTOR) == 0){
            return new Result().fail("当前用户没权限操作！");
        }


        //审核资质
        if(ctype == 1){
            int cstatus = ckstatus == 1 ? 256 : 512;
            String ckSql = " update {{?"+DSMConst.TB_COMP+"}}" +
                    " set cstatus = cstatus|?,auditdate = CURRENT_DATE,audittime = CURRENT_TIME,"
                    + "examine = ?,auditer = ?  where cid = ? ";
            if(baseDao.updateNative(ckSql,cstatus,ckreson,uid,cid) > 0){
                return new Result().success("操作成功！");
            }
        }

        //开通账号
        if(ctype == 2){
            String qrSql = "select 1 from {{?"+DSMConst.TB_COMP+"}} where cid = ? and cstatus & 256 > 0 ";
            List<Object[]> queryRet = baseDao.queryNative(qrSql, cid);
            if(queryRet == null || queryRet.isEmpty()){
                return new Result().fail("当前合伙人审核没通过！");
            }
            String ckSql = " update {{?"+DSMConst.TB_SYSTEM_USER+"}}" +
                    " set cstatus = cstatus~32 where uid = ? ";
            if(baseDao.updateNative(ckSql,suid) > 0){
                return new Result().success("操作成功！");
            }
        }
        return new Result().success("操作失败！");
    }



    private boolean checkUser(ProxyPartnerVO proxyPartnerVO) {
        StringBuilder sqlBuilder = new StringBuilder();
        String sql = "select count(*) from {{?" + DSMConst.TB_SYSTEM_USER + "}} where uphone=?" +
                " and cstatus&1=0 ";
        sqlBuilder.append(sql);
        if (proxyPartnerVO.getUid() > 0) {
            sqlBuilder.append(" and uid<>").append(proxyPartnerVO.getUid());
        }
        List<Object[]> queryResult = baseDao.queryNative(sqlBuilder.toString(), proxyPartnerVO.getUphone());
        int count = Integer.parseInt(String.valueOf(queryResult.get(0)[0]));
        return count > 0;
    }


    @UserPermission(ignore = true)
    public Result queryStaffs(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int pageSize = jsonObject.get("pageSize").getAsInt();
        int pageIndex = jsonObject.get("pageNo").getAsInt();
        int belong = jsonObject.get("belong").getAsInt();
        int cid = jsonObject.get("cid").getAsInt()  ;
        Page page = new Page();
        page.pageSize = pageSize;
        page.pageIndex = pageIndex;
        PageHolder pageHolder = new PageHolder(page);
        Result result = new Result();
        StringBuilder sqlBuilder = new StringBuilder();
        String selectSQL = "select uid,uphone,urealname,u.roleid,u.adddate,u.addtime"
                + ",u.offdate,u.offtime,ip,logindate,logintime,u.cstatus,arean as arean from {{?"
                + DSMConst.TB_SYSTEM_USER + "}} u "
                + " left join (select uid,GROUP_CONCAT(pca.arean) as arean "
                + " from {{?"+DSMConst.TB_PROXY_UAREA+"}} uarea,{{?"+DSMConst.TB_AREA_PCA +"}} pca "
                + "  where pca.areac = uarea.areac and uarea.cstatus & 1 = 0 group by uid) a on a.uid = u.uid "
                + " where u.cstatus&1=0 and belong = ? and u.cid = ? ";

        sqlBuilder.append(selectSQL);
        sqlBuilder = getgetParamsUDYSQL(sqlBuilder, jsonObject).append(" group by u.uid desc ");
        List<Object[]> queryResult = baseDao.queryNative(pageHolder, page,
                sqlBuilder.toString(),new Object[]{belong,cid});

        if (queryResult == null || queryResult.isEmpty()) return result.success(null);
        UserInfoVo[] userInfoVos = new UserInfoVo[queryResult.size()];

        baseDao.convToEntity(queryResult, userInfoVos, UserInfoVo.class,
                new String[]{"uid","uphone","urealname","roleid","adddate","addtime","offdate",
                        "offtime","ip","logindate","logintime","cstatus","arean"});

        return result.setQuery(userInfoVos,pageHolder);
    }

    private StringBuilder getgetParamsUDYSQL(StringBuilder sqlBuilder, JsonObject jsonObject) {
        //  String uname = jsonObject.get("uaccount").getAsString();
        String urealname = jsonObject.get("urealname").getAsString();
        long roleid = jsonObject.get("roleid").getAsLong()  ;
        String uphone = jsonObject.get("uphone").getAsString();
        int state = jsonObject.get("cstatus").getAsInt();
        String areaStr = jsonObject.get("areac").getAsString();

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

            String areaSql = "select uid from {{?"+DSMConst.TB_PROXY_UAREA+"}} where areac in ("+areacStr+") and cstatus & 1 = 0 ";
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

        return sqlBuilder;
    }


    @UserPermission(ignore = true)
    public Result queryBds(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        Result result = new Result();
        int uid = jsonObject.get("uid").getAsInt();
        int roleid = jsonObject.get("roleid").getAsInt();
        StringBuilder sqlBuilder = new StringBuilder();
        String selectSQL = " select uid,urealname from {{?" + DSMConst.TB_SYSTEM_USER + "}} where roleid & ? > 0 and cstatus & 1 = 0 and belong = ? ";
        sqlBuilder.append(selectSQL);
        List<Object[]> queryResult = baseDao.queryNative(selectSQL,roleid,uid);
        if (queryResult == null || queryResult.isEmpty()) return result.success(null);
        HashMap<String,String> map = new HashMap<>();

        for (Object[] objects : queryResult){
            map.put(objects[0].toString(),objects[1].toString());
        }
        return  result.success(map);
    }


    @UserPermission(ignore = true)
    public Result queryAllBds(AppContext appContext) {
        Result result = new Result();
        StringBuilder sqlBuilder = new StringBuilder();
        String selectSQL = " select uid,urealname from {{?" + DSMConst.TB_SYSTEM_USER + "}} where roleid & (4096+8192) > 0 and cstatus & 1 = 0 ";
        sqlBuilder.append(selectSQL);
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        if (queryResult == null || queryResult.isEmpty()) return result.success(null);
        HashMap<String,String> map = new HashMap<>();

        for (Object[] objects : queryResult){
            map.put(objects[0].toString(),objects[1].toString());
        }
        return  result.success(map);
    }


    @UserPermission(ignore = true)
    public Result bindBd(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        Result result = new Result();
        int uid = jsonObject.get("bd").getAsInt();
        int cid = jsonObject.get("cid").getAsInt();
        StringBuilder sqlBuilder = new StringBuilder();

        String selectCompSQL = " select lng,lat from {{?" + DSMConst.TB_COMP + "}} where cid = ? and cstatus&1 = 0";
        List<Object[]> cppoint = baseDao.queryNative(selectCompSQL,cid);

        if(cppoint == null || cppoint.isEmpty()){
            return new Result().fail("绑定失败！");
        }

        String selectAreaSQL = " select arearng from {{?" + DSMConst.TB_PROXY_UAREA + "}} where uid = ? and cstatus&1 = 0";

        List<Object[]> arpoint = baseDao.queryNative(selectAreaSQL,uid);

        if(arpoint == null || arpoint.isEmpty()){
            return new Result().fail("绑定失败！");
        }

        GaoDeMapUtil.Point compPoint
                = new GaoDeMapUtil.Point(Double.parseDouble(cppoint.get(0)[0].toString()),
                Double.parseDouble(cppoint.get(0)[1].toString()));

        for(Object[] objs : arpoint){
            if(objs == null || StringUtils.isEmpty(objs.toString())){
                continue;
            }
            try{
                List<GaoDeMapUtil.Point>
                        points = GsonUtils.json2List(objs.toString(), GaoDeMapUtil.Point.class);
                if(GaoDeMapUtil.checkPointOnRange(compPoint,points)){
                    String bingSql = "update {{?" + DSMConst.TB_COMP + "}} set inviter = ? where cid = ?";
                    if(baseDao.updateNative(bingSql,uid,cid) > 0){
                        return new Result().success("绑定成功!");
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                continue;
            }
        }
        return  new Result().fail("绑定失败！");
    }








    public static void main(String[] args) {

    }

}
