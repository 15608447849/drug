package com.onek.user;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.alibaba.fastjson.JSON;
import com.google.gson.*;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.context.UserSession;
import com.onek.entitys.Result;
import com.onek.user.entity.*;
import com.onek.user.operations.StoreBasicInfoOp;
import com.onek.util.GenIdUtil;
import com.onek.util.IceRemoteUtil;
import com.onek.util.RedisGlobalKeys;
import com.onek.util.RoleCodeCons;
import com.onek.util.area.AreaEntity;
import com.onek.util.FileServerUtils;
import constant.DSMConst;
import dao.BaseDAO;
import org.hyrdpf.util.LogUtil;
import util.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.onek.util.RedisGlobalKeys.getAptID;
import static com.onek.util.RedisGlobalKeys.getBusID;
import static com.onek.util.RedisGlobalKeys.getUserCode;
import static constant.DSMConst.TB_COMP;

/**
 * @服务名 userServer
 */

public class BackGroundProxyMoudule {

    private static final BaseDAO baseDao = BaseDAO.getBaseDAO();

    private final String  QUERY_MY_AREAC = "select distinct ura.areac,arean from {{?"+DSMConst.TB_PROXY_UAREA +"}} ura," +
            "{{?"+ DSMConst.TB_AREA_PCA+"}} pca where ura.areac = pca.areac" +
            " and uid = ? and ura.cstatus & 1 = 0 ";


    private final String QUERY_DIST_AREAC = "select distinct ura.areac,arean,su.uid,su.uphone,su.urealname from {{?"+DSMConst.TB_PROXY_UAREA+"}} ura join " +
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
        UserSession userSession = appContext.getUserSession();
        long curRoleid = userSession.roleCode;
        int roleid = jsonObject.get("role").getAsInt();
        int uid = jsonObject.get("uid").getAsInt();
        int suid = jsonObject.get("suid").getAsInt();
        int ctype = jsonObject.get("ctype").getAsInt();

        if(roleid <= 0 || uid <= 0){
            return result.success(null);
        }

        switch (ctype){
            case 1:
                return  result.success(getMyProxyAreac(roleid,suid,uid,curRoleid));
            case 2:
                return  result.success(getAddProxyAreac(roleid,uid));
            case 3:
                return  result.success(getOtherProxyAreac(roleid,suid,uid));
        }

        return result.success(null);
    }

    public List<ProxyAreaTreeVO> getMyProxyAreac(int roleid,int suid,int uid,long curRoleid){

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
            List<ProxyAreaTreeVO>  areaList = new ArrayList<>();
            if((curRoleid & RoleCodeCons._PROXY_DIRECTOR) > 0){
                String queryPuidSql = "select belong from {{?"+ DSMConst.TB_SYSTEM_USER+"}} where uid = ?";
                List<Object[]> puidRet = baseDao.queryNative(queryPuidSql, suid);
                if(puidRet == null || puidRet.isEmpty()){
                    return null;
                }
                uid = Integer.parseInt(puidRet.get(0)[0].toString());
            }
            List<Object[]> queryParentRet = baseDao.queryNative(QUERY_MY_AREAC, uid);
            if(queryParentRet == null || queryParentRet.isEmpty()){
                return null;
            }
            for (Object[] objects : queryParentRet){
                ProxyAreaTreeVO proxyAreaVO = new ProxyAreaTreeVO();
                proxyAreaVO.setArean(objects[1].toString());
                proxyAreaVO.setAreac((objects[0].toString()));
                proxyAreaVO.setLayer(1);
                areaList.add(proxyAreaVO);

                LogUtil.getDefaultLogger().debug(proxyAreaVO.getArean()+"  "+proxyAreaVO.getAreac());

            }
            for (Object[] objs : queryRet) {
                ProxyAreaTreeVO pproxyAreaVO = new ProxyAreaTreeVO();
                pproxyAreaVO.setArean(objs[1].toString());
                pproxyAreaVO.setAreac((objs[0].toString()));
                pproxyAreaVO.setLayer(0);
                areaList.add(pproxyAreaVO);
                LogUtil.getDefaultLogger().debug(pproxyAreaVO.getArean()+"  "+pproxyAreaVO.getAreac());
            }
            LogUtil.getDefaultLogger().debug(areaList.size());
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
            List<AreaEntity> chList = new ArrayList<>(Arrays.asList(children));

            Iterator<AreaEntity> chListIterator = chList.iterator();

            while(chListIterator.hasNext()){
                AreaEntity areaEntity = chListIterator.next();
                for (Object[] objs : queryRet){
                    if(objs[0].toString().equals(areaEntity.getAreac()+"")){
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
                    if(objs[0].toString().equals(proxyAreaVO.getAreac()+"")){
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

//        StringBuilder sqlbuild = new StringBuilder(QUERY_DIST_AREAC);
//        sqlbuild.append(" and su.uid != ").append(suid);


        StringBuilder sb = new StringBuilder(QUERY_DIST_AREAC);
        int roleParm = 0;
        if((roleid & RoleCodeCons._PROXY_DIRECTOR) > 0){
            roleParm = RoleCodeCons._PROXY_MGR;
        }

        //合伙人
        if((roleid & RoleCodeCons._PROXY_MGR) > 0){
//            String queryMgr = " select uid from {{?"+DSMConst.TB_SYSTEM_USER+"}} where uid = ? ";
//            List<Object[]> mgrs = baseDao.queryNative(queryMgr);
//            if(mgrs == null || mgrs.isEmpty()){
//
//            }
            roleParm = RoleCodeCons._PROXY_PARTNER;
            sb.append(" and su.belong = ").append(uid);
       }
        List<Object[]> queryRet = baseDao.queryNative(sb.toString(),
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

        String selectSQL = "select cp.cid,tu.uid,cname,cp.cstatus ckstatus,tu.cstatus opstatus,tu.urealname "
                + ",createdate,createtime,arean,ctype,tu.roleid,examine,tu.uphone,stu.urealname purealname,stu.uid puid from {{?"
                + DSMConst.TB_COMP + "}} cp  join {{?" + DSMConst.TB_SYSTEM_USER + "}} tu  on cp.cid = tu.cid "
                + "left join {{?" + DSMConst.TB_SYSTEM_USER + "}} stu on stu.uid = tu.belong "
                + " left join (select uid,GROUP_CONCAT(distinct pca.arean) as arean "
                + " from {{?"+DSMConst.TB_PROXY_UAREA+"}} uarea,{{?"+DSMConst.TB_AREA_PCA +"}} pca "
                + "  where pca.areac = uarea.areac and uarea.cstatus&1 = 0 group by uid) a on a.uid = tu.uid "
                + " where tu.cstatus&1=0 and ctype in (2,3) and  tu.roleid & 2048 > 0 ";

        sqlBuilder.append(selectSQL);
        sqlBuilder = getgetParamsDYSQL(sqlBuilder, jsonObject).append(" group by tu.uid desc ");
        LogUtil.getDefaultLogger().debug("queryPartners: "+sqlBuilder.toString());
        List<Object[]> queryResult = baseDao.queryNative(pageHolder, page, sqlBuilder.toString());
        if (queryResult == null || queryResult.isEmpty()) return result.setQuery(null,pageHolder);
        ProxyPartnerVO[] proxyPartnerVOS = new ProxyPartnerVO[queryResult.size()];
        baseDao.convToEntity(queryResult, proxyPartnerVOS, ProxyPartnerVO.class,
                new String[]{"cid","uid","cname","ckstatus","opstatus","urealname","createdate",
                "createtime","arean","ctype","roleid","examine","uphone","purealname","puid"});
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
        int pmuid =jsonObject.get("pmuid").getAsInt();

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
                sqlBuilder.append(" and tu.cstatus&32=0");
            }

            if (opstatus == 32) {
                sqlBuilder.append(" and tu.cstatus&32>0");
            }
        }


        if (!StringUtils.isEmpty(uphone) && Long.parseLong(uphone) > 0) {
            sqlBuilder.append(" and tu.uphone=").append(uphone);
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
                areaSql = "select uid from {{?"+DSMConst.TB_PROXY_UAREA+"}} where  cstatus & 1 = 0 and areac REGEXP '"+areacStr+"'";

                LogUtil.getDefaultLogger().debug(areaSql);
            }else if((mroleid & RoleCodeCons._PROXY_MGR) > 0){



                for(String areac : areaArry){
                    if(areac != null && !StringUtils.isEmpty(areac)){
                        areaSb.append(Long.parseLong(areac)).append(",");
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
                    uidSb.append(objs[0].toString()).append(",");
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

            if(pmuid > 0){
                sqlBuilder.append(" and tu.belong = ").append(pmuid);
                return sqlBuilder;
            }

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
                sqlBuilder.append(" and tu.belong in (").append(uidStr).append(")");
                return sqlBuilder;
            }

        }

        //渠道经理
        if((mroleid & RoleCodeCons._PROXY_MGR) > 0){
            sqlBuilder.append(" and tu.belong = ").append(puid);
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
        String selectSQL = " select uid,urealname from {{?" + DSMConst.TB_SYSTEM_USER + "}} where roleid & 1024 > 0 and belong = ? and cstatus & 33 = 0 ";
        sqlBuilder.append(selectSQL);
        List<Object[]> queryResult = baseDao.queryNative(selectSQL,uid);
        if (queryResult == null || queryResult.isEmpty()) return result.success(null);
        List<ChMgrVO> chMgrVOList = new ArrayList<>();
        for (Object[] objects : queryResult){
            if(objects[0] != null && objects[1] != null){
                ChMgrVO chMgrVO = new ChMgrVO();
                chMgrVO.setMuid(objects[0].toString());
                chMgrVO.setMname(objects[1].toString());
                chMgrVOList.add(chMgrVO);
            }
        }
        return  result.success(chMgrVOList);
    }


    @UserPermission(ignore = true)
    public Result insertPartners(AppContext appContext) {
        String json = appContext.param.json;
        ProxyPartnerVO proxyPartnerVO = GsonUtils.jsonToJavaBean(json, ProxyPartnerVO.class);
        if (proxyPartnerVO != null) {
            if (StringUtils.isEmpty(proxyPartnerVO.getUphone())) {
                return new Result().fail("参数错误！");
            }
            if (checkUser(proxyPartnerVO)){
                return new Result().fail("当前用户已存在！");
            }
            List<String> sqlList = new ArrayList<>();
            List<Object[]> parmList = new ArrayList<>();
            if (proxyPartnerVO.getUid() <= 0) {
                String insertUserSQL = "insert into {{?" + DSMConst.TB_SYSTEM_USER + "}} "
                        + "(uid,uphone,urealname,upw,roleid,adddate,addtime,belong,cstatus,cid)"
                        + " values (?,?,?,?,?,CURRENT_DATE,CURRENT_TIME,?,32,?)";

                sqlList.add(insertUserSQL);
                String pwd = EncryptUtils.encryption(String.valueOf(proxyPartnerVO.getUphone()).substring(5));

                int uid = getUserCode();
                long compid = RedisGlobalKeys.getProxyCompanyCode();
                parmList.add(new Object[]{uid,
                        proxyPartnerVO.getUphone(), proxyPartnerVO.getUrealname(),
                        pwd, RoleCodeCons._PROXY_PARTNER,proxyPartnerVO.getBelong(),compid});

                String insertCompSql = "INSERT INTO {{?"+ TB_COMP +"}} " +
                        "(ctype,cid,cname,cnamehash,cstatus,createdate,createtime) " +
                        "VALUES(?,?,?,crc32(?),?,CURRENT_DATE,CURRENT_TIME)";

                sqlList.add(insertCompSql);

                parmList.add(new Object[]{proxyPartnerVO.getCtype(),
                        compid, proxyPartnerVO.getCname(),
                        proxyPartnerVO.getCname(),128});


                if(proxyPartnerVO.getArean() != null
                        && !StringUtils.isEmpty(proxyPartnerVO.getArean())){
                    String [] areaArry = proxyPartnerVO.getArean().split(",");

                    String queryAreaExtSql = "select 1 from {{?"+DSMConst.TB_PROXY_UAREA+"}} where  uid = ? and areac = ? and cstatus & 1 = 0 ";

                    String insertAreaSql = "insert into {{?"+DSMConst.TB_PROXY_UAREA+"}}" +
                            " (unqid,uid,areac,cstatus,arearng) values (?,?,?,?,?) ";



                    for(int i = 0; i < areaArry.length; i++){
                        if(areaArry[i] != null && !StringUtils.isEmpty(areaArry[i])){
                            List<Object[]> isExt = baseDao.queryNative(queryAreaExtSql, proxyPartnerVO.getUid(), areaArry[i]);

                            if((isExt == null || isExt.isEmpty())){

                            IceRemoteUtil.getCompleteName(areaArry[i]);
//                            String[] allArea =  AreaStore.getCompleteName(Long.parseLong(areaArry[i]));
//                            StringBuilder asb = new StringBuilder();
//                            for (String area : allArea){
//                                asb.append(area);
//                            }
                                List<List<GaoDeMapUtil.Point>> lists = GaoDeMapUtil.areaPolyline(IceRemoteUtil.getCompleteName(areaArry[i]));
                                for (List<GaoDeMapUtil.Point> plist : lists){
                                    String jwp = GsonUtils.javaBeanToJson(plist);
                                    sqlList.add(insertAreaSql);
                                    parmList.add(new Object[]{GenIdUtil.getUnqId(),uid,areaArry[i],
                                            0,jwp});
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
        UserSession userSession = appContext.getUserSession();
        long roleid = userSession.roleCode;
        ProxyPartnerVO proxyPartnerVO = GsonUtils.jsonToJavaBean(json, ProxyPartnerVO.class);
        if (proxyPartnerVO != null) {
            if (StringUtils.isEmpty(proxyPartnerVO.getUphone())) {
                return new Result().fail("参数错误！");
            }
            List<String> sqlList = new ArrayList<>();
            List<Object[]> parmList = new ArrayList<>();
            if (proxyPartnerVO.getUid() > 0) {
                String updateUserSql = "update {{?" +DSMConst.TB_SYSTEM_USER +"}} "
                        + " set uphone = ?,urealname = ?,belong = ? where uid = ?";
                sqlList.add(updateUserSql);

                parmList.add(new Object[]{proxyPartnerVO.getUphone(),proxyPartnerVO.getUrealname()
                        ,proxyPartnerVO.getBelong(),proxyPartnerVO.getUid()});

                String updateCompSql = "update {{?"+DSMConst.TB_COMP +"}} set cname = ?," +
                        " cnamehash = crc32(?) where cid = ?";
                sqlList.add(updateCompSql);
                parmList.add(new Object[]{proxyPartnerVO.getCname(),
                        proxyPartnerVO.getCname(),proxyPartnerVO.getCid()});


                if(proxyPartnerVO.getArean() != null
                        && !StringUtils.isEmpty(proxyPartnerVO.getArean())){
                    String [] areaArry = proxyPartnerVO.getArean().split(",");

                    String insertAreaSql = "insert into {{?"+DSMConst.TB_PROXY_UAREA+"}}" +
                            " (unqid,uid,areac,cstatus,arearng) values (?,?,?,?,?) ";



                    String delAreaSql = "update {{?"+DSMConst.TB_PROXY_UAREA+"}} set cstatus = cstatus | 1 " +
                            " where uid = ? and areac = ?";

                    String queryAreaExtSql = "select 1 from {{?"+DSMConst.TB_PROXY_UAREA+"}} where  uid = ? and areac = ? and cstatus & 1 = 0 ";

                    for(int i = 0; i < areaArry.length; i++){
                        if(areaArry[i] != null && !StringUtils.isEmpty(areaArry[i])){
                            if(areaArry[i].startsWith("-")){
                                sqlList.add(delAreaSql);
                                parmList.add(new Object[]{proxyPartnerVO.getUid(),areaArry[i].substring(1)});
                            }else{
                                List<Object[]> isExt = baseDao.queryNative(queryAreaExtSql, proxyPartnerVO.getUid(), areaArry[i]);
                                if(isExt == null || isExt.isEmpty()){

                                String completeName = IceRemoteUtil.getCompleteName(areaArry[i]);
                                    String jwp = null;
                                    try{
                                       LogUtil.getDefaultLogger().debug(completeName);
                                        List<List<GaoDeMapUtil.Point>> lists = GaoDeMapUtil.areaPolyline(IceRemoteUtil.getCompleteName(areaArry[i]));
                                        for (List<GaoDeMapUtil.Point> plist : lists){
                                            jwp = GsonUtils.javaBeanToJson(plist);
                                            sqlList.add(insertAreaSql);
                                            parmList.add(new Object[]{GenIdUtil.getUnqId(),proxyPartnerVO.getUid(),areaArry[i],
                                                    0,jwp});
                                        }


                                    }catch (Exception e){
                                        LogUtil.getDefaultLogger().debug("获取区域经纬度失败！");
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                }

                if((roleid & RoleCodeCons._PROXY_DIRECTOR) > 0){
                    String queryBlong = "select belong,cid from {{?"+DSMConst.TB_SYSTEM_USER+"}} where uid = ? and cstatus & 1 = 0 ";
                    List<Object[]> belongRet = baseDao.queryNative(queryBlong, proxyPartnerVO.getUid());
                    int sbelong = Integer.parseInt(belongRet.get(0)[0].toString());
                    if(sbelong != proxyPartnerVO.getBelong() && proxyPartnerVO.getBelong() != 0){
                        String deleteAreaSql = "update {{?"+DSMConst.TB_PROXY_UAREA +"}} set cstatus = cstatus | 1  where  cstatus & 1 = 0 ";
                        String compSql = "select uid from {{?"+DSMConst.TB_SYSTEM_USER +"}} where cid = ? and cstatus & 1 = 0";
                            List<Object[]> uidList = baseDao.queryNative(compSql, belongRet.get(0)[1]);
                            StringBuilder sb = new StringBuilder();
                            for(Object[] objects: uidList){
                                sb.append(Integer.parseInt(objects[0].toString())).append(",");
                            }

                            String uidStr = sb.toString();
                            if(uidStr.endsWith(",")){
                                uidStr = uidStr.substring(0,uidStr.length()-1);
                            }

                            if(!StringUtils.isEmpty(uidStr)){
                                deleteAreaSql = deleteAreaSql + " and uid in ("+uidStr+")";
                                sqlList.add(deleteAreaSql);
                                parmList.add(new Object[]{});
                            }

                    //    String updateAreaSql = "update {{?"+DSMConst.TB_PROXY_UAREA +"}} set uid = ? where uid = ? and cstatus & 1 = 0 ";

                      //  sqlList.add(updateAreaSql);

                        //parmList.add(new Object[]{proxyPartnerVO.getBelong(),sbelong});
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
        String ckreson = jsonObject.get("ckreson").getAsString();
        int suid = jsonObject.get("suid").getAsInt();

        if((roleid & RoleCodeCons._PROXY_DIRECTOR) == 0){
            return new Result().fail("当前用户没权限操作！");
        }



        //审核资质
        if(ctype == 1){
           // int cstatus = ckstatus == 1 ? 256 : 512;
            String ckSql = " update {{?"+DSMConst.TB_COMP+"}}" +
                    " set cstatus = ?,auditdate = CURRENT_DATE,audittime = CURRENT_TIME,"
                    + "examine = ?,auditer = ?  where cid = ? ";

            if(baseDao.updateNative(ckSql,ckstatus,ckreson,uid,cid) > 0){
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
                    " set cstatus = cstatus & ~32 where uid = ? ";
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
        String selectSQL = "select u.uid,u.uphone,u.urealname,u.roleid,u.adddate,u.addtime"
                + ",u.offdate,u.offtime,u.ip,u.logindate,u.logintime,u.cstatus,arean as arean," +
                "stu.uid buid,stu.urealname buname from {{?"
                + DSMConst.TB_SYSTEM_USER + "}} u "
                + " left join (select uid,GROUP_CONCAT(distinct pca.arean) as arean "
                + " from {{?"+DSMConst.TB_PROXY_UAREA+"}} uarea,{{?"+DSMConst.TB_AREA_PCA +"}} pca "
                + " where pca.areac = uarea.areac and uarea.cstatus & 1 = 0 group by uid) a on a.uid = u.uid "
                + " left join {{?" + DSMConst.TB_SYSTEM_USER + "}} stu on stu.uid = u.belong "
                + " where u.cstatus&1=0 and u.cid = ? ";

        sqlBuilder.append(selectSQL);
        sqlBuilder = getgetParamsUDYSQL(sqlBuilder, jsonObject).append(" group by u.uid desc ");
        List<Object[]> queryResult = baseDao.queryNative(pageHolder, page,
                sqlBuilder.toString(),new Object[]{cid});
        UserInfoVo[] userInfoVos = new UserInfoVo[queryResult.size()];

        if (queryResult == null || queryResult.isEmpty()){
           return result.setQuery(userInfoVos,pageHolder);
        }

        baseDao.convToEntity(queryResult, userInfoVos, UserInfoVo.class,
                "uid","uphone","urealname","roleid","adddate","addtime","offdate",
                "offtime","ip","logindate","logintime","cstatus","arean","buid","buname");

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
            sqlBuilder.append(" and u.urealname like '%").append(urealname).append("%'");
        }
        if (roleid > 0) {
            sqlBuilder.append(" and u.roleid&").append(roleid).append(">0");
        }

        if (!StringUtils.isEmpty(uphone) && Long.parseLong(uphone) > 0) {
            sqlBuilder.append(" and u.uphone=").append(uphone);
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
                    areaSb.append(Long.parseLong(areac)).append(",");
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
        int cid = appContext.getUserSession().compId;
        long roleCode = appContext.getUserSession().roleCode;

        List<Object[]> queryResult = null;

        if((roleCode & RoleCodeCons._PROXY_DIRECTOR) > 0){
            String querySql = "select uid,urealname from {{?"+DSMConst.TB_SYSTEM_USER+"}} u join " +
                    "(select cid from {{?"+DSMConst.TB_SYSTEM_USER+"}} su where belong in (select uid from {{?"+DSMConst.TB_SYSTEM_USER+"}} u " +
                    "  where belong = ? and u.cstatus & 1 = 0) and su.cstatus & 1 = 0) a " +
                    "on u.cid = a.cid where u.roleid & ? > 0 and u.cstatus & 1 = 0 ";
            queryResult = baseDao.queryNative(querySql
                    ,uid,RoleCodeCons._DBM+RoleCodeCons._DB);

            if (queryResult == null || queryResult.isEmpty()) return result.success(null);

            return  result.success(convBdList(queryResult));
        }

        if((roleCode & RoleCodeCons._PROXY_MGR) > 0){
           String querySql = "select uid,urealname from {{?"+DSMConst.TB_SYSTEM_USER+"}} u join "+
           " (select cid from {{?"+DSMConst.TB_SYSTEM_USER+"}} su where belong = ?) a "+
           " on u.cid = a.cid where u.roleid & ? > 0 and u.cstatus & 1 = 0 ";

            queryResult = baseDao.queryNative(querySql
                    ,uid,RoleCodeCons._DBM+RoleCodeCons._DB);
            if (queryResult == null || queryResult.isEmpty()) return result.success(null);
            return  result.success(convBdList(queryResult));
        }

        if((roleCode & RoleCodeCons._PROXY_PARTNER) > 0){
            String selectSQL = " select uid,urealname from {{?" + DSMConst.TB_SYSTEM_USER + "}} where roleid & ? > 0 and cstatus & 1 = 0 and cid = ? ";
            queryResult = baseDao.queryNative(selectSQL,roleid,cid);
            if (queryResult == null || queryResult.isEmpty()) return result.success(null);
            return  result.success(convBdList(queryResult));
        }

        if((roleCode & RoleCodeCons._DBM) > 0){
            String selectSQL =  " select uid,urealname from {{?" + DSMConst.TB_SYSTEM_USER + "}} where roleid & ? > 0 and cstatus & 1 = 0 and belong = ? or uid = ? ";
            queryResult = baseDao.queryNative(selectSQL,roleid,uid,uid);
            if (queryResult == null || queryResult.isEmpty()) return result.success(null);
            return  result.success(convBdList(queryResult));
        }
        return  result.success(null);
    }



    @UserPermission(ignore = true)
    public Result queryBdByArea(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        Result result = new Result();
        int uid = jsonObject.get("uid").getAsInt();
        int roleid = jsonObject.get("roleid").getAsInt();
        long areac = jsonObject.get("areac").getAsLong();
        int cid = appContext.getUserSession().compId;
        long roleCode = appContext.getUserSession().roleCode;

        List<Object[]> queryResult = null;

        if((roleCode & RoleCodeCons._PROXY_PARTNER) > 0){
            String selectSQL = " select uid,urealname from {{?" + DSMConst.TB_SYSTEM_USER + "}} where roleid & ? > 0 and cstatus & 1 = 0 and cid = ? " +
                    "and uid in (select distinct uid from {{?" +DSMConst.TB_PROXY_UAREA+"}} where areac = ? and cstatus & 1 = 0)";
            queryResult = baseDao.queryNative(selectSQL,roleid,cid,areac);
            if (queryResult == null || queryResult.isEmpty()) return result.success(null);
            return  result.success(convBdList(queryResult));
        }

        if((roleCode & RoleCodeCons._DBM) > 0){
            String selectSQL =  " select uid,urealname from {{?" + DSMConst.TB_SYSTEM_USER + "}} where roleid & ? > 0" +
                    " and cstatus & 1 = 0 and belong = ? or uid = ? "+
            "and uid in (select distinct uid from {{?" +DSMConst.TB_PROXY_UAREA+"}} where areac = ? and cstatus & 1 = 0)";
            queryResult = baseDao.queryNative(selectSQL,roleid,uid,uid,areac);
            if (queryResult == null || queryResult.isEmpty()) return result.success(null);
            return  result.success(convBdList(queryResult));
        }

        return  result.success(null);
    }

    public List<BindBdVO> convBdList(List<Object[]> queryResult){
        List<BindBdVO> bdList = new ArrayList<>();
        for (Object[] objects : queryResult){
            if(objects[0] != null || objects[1] != null){
                BindBdVO bindBdVO = new BindBdVO();
                bindBdVO.setBuid(objects[0].toString());
                bindBdVO.setBname(objects[1].toString());
                bdList.add(bindBdVO);
            }
        }
        return bdList;
    }


    @UserPermission(ignore = true)
    public Result queryAllBds(AppContext appContext) {
        Result result = new Result();
        StringBuilder sqlBuilder = new StringBuilder();
        String selectSQL = " select uid,urealname from {{?" + DSMConst.TB_SYSTEM_USER + "}} where roleid & (4096+8192) > 0 and cstatus & 33 = 0 ";
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
    public Result bindBdm(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int bdmid = jsonObject.get("bdm").getAsInt();
        int uid = jsonObject.get("uid").getAsInt();
        if(bdmid <= 0){
            return new Result().fail("指派BDM失败！");
        }

        String bingSql = "update {{?" + DSMConst.TB_SYSTEM_USER +"}} set belong = ? where uid = ?";
        if(baseDao.updateNative(bingSql,bdmid,uid)> 0){
            return new Result().success("指派BDM成功！");
        }
        return  new Result().fail("指派BDM失败！");
    }

    @UserPermission(ignore = true)
    public Result bindCheck(AppContext appContext){
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();

        int ctype = jsonObject.get("ctype").getAsInt();
        int bdid = 0;
        int cid = 0;
        if(ctype == 1){
            String phone = jsonObject.get("phone").getAsString();
            String querySql = "select uid,cid {{?"+DSMConst.TB_SYSTEM_USER+"}} where uphone = ?  and cstatus & 33 = 0 ";
            List<Object[]> ret = baseDao.queryNative(querySql, new Object[]{phone});

            if(ret == null || ret.isEmpty()){
                return new Result().fail("当前门店不在该BD管辖范围内！");
            }

            bdid = Integer.parseInt(ret.get(0)[0].toString());
            cid = Integer.parseInt(ret.get(0)[1].toString());
        }else{
            bdid = jsonObject.get("bd").getAsInt();
            cid = jsonObject.get("cid").getAsInt();
        }

        if(bdid <= 0){
            return new Result().fail("参数错误！");
        }
        String selectCompSQL = " select lng,lat from {{?" + DSMConst.TB_COMP + "}} where cid = ? and cstatus&1 = 0";
        List<Object[]> cppoint = baseDao.queryNative(selectCompSQL,cid);

        if(cppoint == null || cppoint.isEmpty()){
            return new Result().fail("当前门店不在该BD管辖范围内,确定是否继续操作？");
        }

        GaoDeMapUtil.Point compPoint
                = new GaoDeMapUtil.Point(Double.parseDouble(cppoint.get(0)[0].toString()),
                Double.parseDouble(cppoint.get(0)[1].toString()));

        String selectAreaSQL = " select arearng from {{?" + DSMConst.TB_PROXY_UAREA + "}} where uid = ? and cstatus&1 = 0";

        List<Object[]> arpoint = null;
        if(bdid > 0){
            arpoint = baseDao.queryNative(selectAreaSQL,bdid);
            for(Object[] objs : arpoint){
                if(objs == null || objs[0] == null || StringUtils.isEmpty(objs[0].toString())){
                    continue;
                }
                try{
                    List<GaoDeMapUtil.Point>
                            points = GsonUtils.json2List(objs[0].toString(), GaoDeMapUtil.Point.class);
                    if(GaoDeMapUtil.checkPointOnRange(compPoint,points)){
                        return new Result().success("校验通过!");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    continue;
                }
            }
        }
        return new Result().fail("当前门店不在该BD管辖范围内,确定是否继续操作？");
    }


    @UserPermission(ignore = true)
    public Result bindBd(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int bdid = jsonObject.get("bd").getAsInt();
        int cid = jsonObject.get("cid").getAsInt();
        UserSession userSession = appContext.getUserSession();
        int scid = userSession.compId;

        if(bdid <= 0){
            return new Result().fail("参数错误，绑定失败！");
        }

        String bingSql = "update {{?" + DSMConst.TB_COMP + "}} set inviter = ?,invitercid = ? where cid = ?";

        if(baseDao.updateNative(bingSql,bdid,scid,cid) > 0){
            return new Result().success("绑定成功!");
        }
        return new Result().fail("绑定失败！");
    }


    @UserPermission(ignore = true)
    public Result queryStores(AppContext appContext) {
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
        int puid = jsonObject.get("puid").getAsInt();
        int mroleid = jsonObject.get("mroleid").getAsInt();


        String selectSQL = "select distinct cp.cid companyId,tu.uphone phone,tu.uid,cname company,caddrcode addressCode, "
                + "caddr address,createdate,createtime,cp.cstatus,stu.uid cursorId,stu.urealname cursorName," +
                " stu.uphone cursorPhone,IFNULL(sstu.uid,bdu.uid) bdmid,IFNULL(sstu.urealname,bdu.urealname) bdmn,control from {{?"
                + DSMConst.TB_COMP + "}} cp  join {{?" + DSMConst.TB_SYSTEM_USER + "}} tu  on cp.cid = tu.cid "
                + "left join {{?" + DSMConst.TB_SYSTEM_USER + "}} stu on stu.uid = cp.inviter "
                + "left join {{?" + DSMConst.TB_SYSTEM_USER + "}} sstu on sstu.uid = stu.belong "
                + "left join {{?" + DSMConst.TB_SYSTEM_USER + "}} bdu on bdu.uid = tu.belong "
                + " where tu.cstatus&1=0 and ctype = 0  ";
        sqlBuilder.append(selectSQL);


        StringBuilder dySql  = new StringBuilder();
        if((mroleid & (RoleCodeCons._PROXY_DIRECTOR+RoleCodeCons._PROXY_MGR
                +RoleCodeCons._PROXY_PARTNER+RoleCodeCons._DBM+RoleCodeCons._DB)) > 0){
            dySql.append(" and (");

            boolean drctFlag = false;
            if((mroleid & RoleCodeCons._PROXY_DIRECTOR)> 0){
                dySql.append(" 1=1 ");
                drctFlag = true;
            }

            if(!drctFlag){
                dySql.append(" 1=2 ");
                if((mroleid & RoleCodeCons._PROXY_MGR)> 0){
                    String queryAreaSql = " select distinct areac from {{?"+DSMConst.TB_PROXY_UAREA+"}} where uid = ? and cstatus & 1 = 0";

                    List<Object[]> areaArry = baseDao.queryNative(queryAreaSql, puid);

                    if(areaArry != null && !areaArry.isEmpty()){

                        StringBuilder areaSb = new StringBuilder();
                        for(Object[] objs : areaArry){
                            if(objs[0] != null && !StringUtils.isEmpty(objs[0].toString())){
                                String areac = objs[0].toString().substring(0,4);
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

                        if(!StringUtils.isEmpty(areacStr)){
                            dySql.append(" or caddrcode REGEXP '"+areacStr+"'");
                        }

                    }

                }

                if((mroleid & RoleCodeCons._PROXY_PARTNER)> 0){


                    String queryArea =  " select areac from {{?"+DSMConst.TB_PROXY_UAREA+"}} where uid = ? and cstatus & 1 = 0";

                    List<Object[]> areaRet = baseDao.queryNative(queryArea, puid);

                    if(areaRet != null && !areaRet.isEmpty()){
                        StringBuilder areaSb = new StringBuilder();
                        for (Object[] objs : areaRet){
                            areaSb.append(objs[0].toString()).append(",");
                        }
                        String areaStr = areaSb.toString();
                        if(areaStr.endsWith(",")){
                            areaStr = areaStr.substring(0,areaStr.length() - 1);
                        }

                        dySql.append(" or caddrcode in ("+areaStr+")");
                    }
                }

                if((mroleid & RoleCodeCons._DBM) > 0){
                    dySql.append(" or stu.belong = ").append(puid);
                    dySql.append(" or stu.uid = ").append(puid);
                    dySql.append(" or tu.belong = ").append(puid);
                }

                if((mroleid & RoleCodeCons._DB) > 0){
                    dySql.append(" or stu.uid = ").append(puid);
                }
            }

            dySql.append(")");
        }

//        if(dySql.toString().trim().equals("and ()")){
//            return result.setQuery(null,pageHolder);
//        }
        sqlBuilder.append(dySql.toString());
        sqlBuilder = getgetParamsSTDYSQL(sqlBuilder, jsonObject).append(" group by tu.uid desc ");
        LogUtil.getDefaultLogger().debug(sqlBuilder.toString());
        List<Object[]> queryResult = baseDao.queryNative(pageHolder, page, sqlBuilder.toString());
        ProxyStoreVO[] proxyStoreVOS = new ProxyStoreVO[queryResult.size()];
        if (queryResult == null || queryResult.isEmpty()) return result.setQuery(proxyStoreVOS,pageHolder);

        baseDao.convToEntity(queryResult, proxyStoreVOS, ProxyStoreVO.class,
                "companyId","phone","uid","company","addressCode","address","createdate",
                "createtime","status","cursorId","cursorName","cursorPhone","bdmid","bdmn","control");

        for (ProxyStoreVO proxyStoreVO : proxyStoreVOS){
            AreaEntity[] ancestors = IceRemoteUtil.getAncestors(Long.parseLong(proxyStoreVO.getAddressCode()));
            if(ancestors != null && ancestors.length > 0){
                LogUtil.getDefaultLogger().debug(ancestors[0].getArean());
                proxyStoreVO.setProvince(ancestors[0].getArean());
                proxyStoreVO.setCity(ancestors[1].getArean());
                proxyStoreVO.setRegion(ancestors[2].getArean());
            }
        }
        return result.setQuery(proxyStoreVOS,pageHolder);
    }

    private StringBuilder getgetParamsSTDYSQL(StringBuilder sqlBuilder, JsonObject jsonObject) {

        String cname = jsonObject.get("cname").getAsString();
       // String urealname = jsonObject.get("urealname").getAsString();
        int ckstatus = jsonObject.get("ckstatus").getAsInt()  ;
        String uphone = jsonObject.get("uphone").getAsString();
       // int opstatus = jsonObject.get("cstatus").getAsInt();
        String areaStr = jsonObject.get("address").getAsString();
        int bd = jsonObject.get("bd").getAsInt();



        if (cname != null && !cname.isEmpty()) {
            sqlBuilder.append(" and cp.cname like '%").append(cname).append("%'");
        }

        if (areaStr != null && !areaStr.isEmpty()) {
            sqlBuilder.append(" and cp.caddr like '%").append(areaStr).append("%'");
        }

        //  状态 （1:删除, 128:审核中; 256:认证成功 512:认证失败; 1024:停用）
        if (ckstatus > 0) {
            sqlBuilder.append(" and cp.cstatus &").append(ckstatus).append(">0");
        }


        if (!StringUtils.isEmpty(uphone) && Long.parseLong(uphone) > 0) {
            sqlBuilder.append(" and tu.uphone=").append(uphone);
        }

        if (bd > 0) {
            sqlBuilder.append(" and cp.inviter=").append(bd);
        }
        return sqlBuilder;
    }


    @UserPermission(ignore = true)
    public Result switchStatus(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        UserSession userSession = appContext.getUserSession();
        int ctype = jsonObject.get("ctype").getAsInt();
        int suid = jsonObject.get("uid").getAsInt();
        int sroleid = jsonObject.get("roleid").getAsInt();
        long roleid = userSession.roleCode;
        int cid = userSession.compId;
        int userId = userSession.userId;
        //CURRENT_DATE,CURRENT_TIME
        String  optSql = "update {{?"+DSMConst.TB_SYSTEM_USER+"}} set cstatus = cstatus | 32,offdate = CURRENT_DATE,offtime = CURRENT_TIME  where uid = ? and cstatus&1=0 and cstatus&32=0 ";
       // String coptSql = "update {{?"+DSMConst.TB_SYSTEM_USER+"}} set cstatus = cstatus | 32  where  cid = ? and cstatus&1=0 and cstatus&32=0 ";
        String pcSql = "update {{?"+DSMConst.TB_COMP+"}} set cstatus = cstatus | 32 where cid = ? and cstatus&1=0 and cstatus&32=0 ";
        String aerSql =  "update {{?"+DSMConst.TB_PROXY_UAREA+"}} set cstatus = cstatus | 1 where cstatus & 1 = 0 ";
        if(ctype != 1){
            optSql = "update {{?"+DSMConst.TB_SYSTEM_USER+"}} set cstatus = cstatus & ~32  where uid = ? and cstatus&1=0 and cstatus&32>0 ";
         //   coptSql = "update {{?"+DSMConst.TB_SYSTEM_USER+"}} set cstatus = cstatus & ~32  where  cid = ? and cstatus&1=0 and cstatus&32>0 ";
            pcSql = "update {{?"+DSMConst.TB_COMP+"}} set cstatus = cstatus  & ~32 where cid = ?  and cstatus&1=0 and cstatus&32>0 ";
        }

        if((sroleid & RoleCodeCons._PROXY_DIRECTOR) > 0){
            return new Result().fail("渠道总监不允许停用！");
        }

        if((sroleid & RoleCodeCons._PROXY_PARTNER) > 0)  {
            String queryIsDrect = "select 1 from {{?"+DSMConst.TB_COMP+"}} where cid = ? and ctype = 2 ";
            String compSql = "select uid from {{?"+DSMConst.TB_SYSTEM_USER +"}} where cid = ? and cstatus & 1 = 0";
            cid = jsonObject.get("cid").getAsInt();
            List<Object[]> drectRet = baseDao.queryNative(queryIsDrect, cid);
            if(drectRet != null && !drectRet.isEmpty()){
                List<Object[]> uidList = baseDao.queryNative(compSql, cid);
                StringBuilder sb = new StringBuilder();
                for(Object[] objects: uidList){
                    sb.append(Integer.parseInt(objects[0].toString())).append(",");
                }

                String uidStr = sb.toString();
                if(uidStr.endsWith(",")){
                    uidStr = uidStr.substring(0,uidStr.length()-1);
                }

                List<Object[]> parm = new ArrayList<>();
                List<String> sqlList = new ArrayList<>();
                if(!StringUtils.isEmpty(uidStr)){
                    aerSql = aerSql + " and uid in ("+uidStr+")";
                    parm.add(new Object[]{});
                    sqlList.add(aerSql);
                }

                parm.add(new Object[]{cid});
                sqlList.add(pcSql);
                String[] sqlArray = new String[sqlList.size()];
                sqlList.toArray(sqlArray);
                boolean b = !ModelUtil.updateTransEmpty(
                        baseDao.updateTransNative(sqlArray,parm));
                if(b){
                    return new Result().success("操作成功！");
                }
            }
        }

        if((sroleid & RoleCodeCons._DBM) > 0){
            String selectSQL = "select count(*) from {{?" + DSMConst.TB_SYSTEM_USER + "}} where cstatus&33=0 "
                    + " and roleid & 8192 > 0 and belong=" + suid;
            List<Object[]> qResult = baseDao.queryNative(selectSQL);
            long count = Long.parseLong(String.valueOf(qResult.get(0)[0]));
            if(count > 0){
                return new Result().fail("当前BDM下存在BD，需解除关系后，才能停用！");
            }
         //   return count > 0 ? result.success(true) : result.success(false);
        }



//        if((sroleid & RoleCodeCons._DBM) > 0){
//
//        }

        if(baseDao.updateNative(optSql,suid) > 0){
            return new Result().success("操作成功！");
        }
        return new Result().fail("操作失败！");
    }


    @UserPermission(ignore = true)
    public Result optQualificationCert(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonArray jsonArray = jsonParser.parse(json).getAsJsonArray();
        List<ProxyQualificationCert> proxyQualificationCerts = new ArrayList<>();
        Gson gson = new Gson();
        for (JsonElement proxyQualif : jsonArray) {
            ProxyQualificationCert proxyQualificationCert = gson.fromJson(proxyQualif, ProxyQualificationCert.class);
            proxyQualificationCerts.add(proxyQualificationCert);
        }

        if(proxyQualificationCerts.isEmpty()){
            return new Result().fail("操作失败！");
        }
        String selectAptSql = "SELECT aptid FROM {{?"+DSMConst.TB_COMP_APTITUDE+"}} WHERE compid = ? AND atype = ?";

        String insertSql = "INSERT INTO {{?"+DSMConst.TB_COMP_APTITUDE+"}} (aptid,compid,atype,certificateno,validitys,validitye) VALUES (?,?,?,?,?,?)";

        String updateSql = "UPDATE {{?"+DSMConst.TB_COMP_APTITUDE+"}} SET certificateno=?,validitys=?,validitye=? WHERE aptid = ?";

        List<String> sqlList = new ArrayList<>();
        List<Object[]> parmList = new ArrayList<>();
        for (ProxyQualificationCert proxyQualificationCert:proxyQualificationCerts){
            List<Object[]> isExtRet = baseDao.queryNative(selectAptSql,
                    new Object[]{proxyQualificationCert.getCompid(),
                    proxyQualificationCert.getAtype()});

            if(isExtRet != null && !isExtRet.isEmpty()){
                sqlList.add(updateSql);
                parmList.add(new Object[]{proxyQualificationCert.getCertificateno(),
                        proxyQualificationCert.getValiditys(),
                        proxyQualificationCert.getValiditye(),
                        proxyQualificationCert.getAptid()});
            }else{
                sqlList.add(insertSql);

                parmList.add(new Object[]{getAptID(),
                        proxyQualificationCert.getCompid(),
                        proxyQualificationCert.getAtype(),
                        proxyQualificationCert.getCertificateno(),
                        proxyQualificationCert.getValiditys(),
                        proxyQualificationCert.getValiditye() });
            }
        }

        String[] sqlNative = new String[sqlList.size()];
        sqlNative = sqlList.toArray(sqlNative);
        boolean b = !ModelUtil.updateTransEmpty(
                baseDao.updateTransNative(sqlNative,parmList));

        if(b){
            String stopSql = " update {{?"+DSMConst.TB_COMP+"}}" +
                    " set cstatus = ? where cid = ? ";

            baseDao.updateNative(stopSql,new Object[]{128,proxyQualificationCerts.get(0).getCompid()});

            return new Result().success("操作成功！");
        }
        return new Result().success("操作失败！");
    }


    @UserPermission(ignore = true)
    public Result queryQualificationCert(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        Result result = new Result();
        int cid = jsonObject.get("cid").getAsInt();
        if(cid <= 0){
            return  result.success(null);
        }

        String selectSQL = " select aptid,compid,atype,certificateno,validitys,validitye from {{?"+DSMConst.TB_COMP_APTITUDE+"}} where compid = ? and cstatus & 1 = 0 ";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL,cid);
        if (queryResult == null || queryResult.isEmpty()) return result.success(null);
        ProxyQualificationCert[] proxyQualificationCerts = new ProxyQualificationCert[queryResult.size()];

        baseDao.convToEntity(queryResult, proxyQualificationCerts, ProxyQualificationCert.class,
                new String[]{"aptid","compid","atype","certificateno","validitys","validitye"});

        return  result.success(proxyQualificationCerts);
    }


    /**
     * liuhui
     * 查询门店下绑定的的BD
     * @return json {uid 用户码 urealname 用户真实姓名 uphone 手机号}
     * @param appContext
     * @return
     */
    @UserPermission(ignore = true)
    public Result queryStoreBd(AppContext appContext) {
        Result result = new Result();
        int cid = appContext.getUserSession().compId;
        String querySql = "select su.uid,su.urealname,su.uphone from {{?"+DSMConst.TB_COMP+"}} cp join " +
                    " {{?"+DSMConst.TB_SYSTEM_USER+"}} su on su.uid = cp.inviter where cp.cid = ? ";

        List<Object[]> queryResult = baseDao.queryNative(querySql
                    ,cid);

        if (queryResult == null || queryResult.isEmpty()) return result.success("该门店没有绑定BD",null);

        Map<String,String> retMap = null;
        try{
            retMap = new HashMap<>();
            retMap.put("uid",queryResult.get(0)[0].toString());
            retMap.put("urealname",queryResult.get(0)[1].toString());
            retMap.put("uphone",queryResult.get(0)[2].toString());
        }catch (Exception e){
            e.printStackTrace();
        }
        return  result.success(retMap);
    }

    
    /**
     * @接口摘要 门店管理资质完善
     * @业务场景 BD完善资质信息
     * @传参类型 json
     * @传参列表 见CompInfoVO.class
     * @返回列表 200成功
     */
    public Result storePerfectingInfo(AppContext appContext) {
        String taxpayer = "";//营业执照证件编号（纳税人识别号）
        Result result = new Result();
        String json = appContext.param.json;
        CompInfoVO compInfoVO = GsonUtils.jsonToJavaBean(json, CompInfoVO.class);
        //药店类型  0 医疗单位, 1 批发企业, 2零售连锁门店, 3零售单体门店
        assert compInfoVO != null;
        int storetype = compInfoVO.getStoretype();//门店类型
        int compId = compInfoVO.getCid();//企业码
        InvoiceVO invoice = compInfoVO.getInvoiceVO();//发票信息
        List<AptitudeVO> frontAptList = compInfoVO.getAptitudeVOS();
        if (frontAptList.size() > 0) {
            Set<Integer> aTypeList = new HashSet<>();
            String companyFilePath = FileServerUtils.companyFilePath(compId);
            List<String> list = FileServerUtils.showDirFilesPath(companyFilePath, false);
            for (AptitudeVO aptitudeVO : frontAptList) {
                assert aptitudeVO != null;
                aTypeList.add(aptitudeVO.getAtype());
                if (aptitudeVO.getAtype() == 11 && storetype != 0) {
                    storetype = settingCompStoretype(aptitudeVO.getCertificateno());
                }
                if (aptitudeVO.getAtype() == 10) {
                    taxpayer = aptitudeVO.getCertificateno();
                }
                if(list != null && list.size() >0){ // ERP对接需要文件后缀名
                    for(String filename : list){
                        if(filename.contains(aptitudeVO.getAtype()+"")){
                            aptitudeVO.setFilepath(companyFilePath + filename);
                        }
                    }
                }
            }
            //判断必填资质是否都有
//            String checkStr = checkAptInfo(storetype, aTypeList);
//            if (checkStr != null) {
//                return result.fail(checkStr);
//            }
            if (!updCompType(storetype, compInfoVO.getControl(), compId,invoice,taxpayer)) {
                return result.fail("修改失败！");
            }
            StoreBasicInfoOp.updateCompInfoToCacheById(compId);
            boolean b = optAptInfo(compId,frontAptList,aTypeList);
            if (b) {//同步信息到ERP
                compInfoVO.getInvoiceVO().setTaxpayer(taxpayer);
                compInfoVO.setStoretype(storetype);
                SyncCustomerInfoModule.addOrUpdateCus(compInfoVO);
            }


            return b ? result.success("保存成功") : result.fail("保存失败");
        } else {
            return result.fail("资质信息未完善！");
        }
    }

    /* *
     * @description 更新门店类型
     * @params [storetype, compId]
     * @return boolean
     * @exception
     * @author 11842
     * @time  2019/6/20 15:51
     * @version 1.1.1
     **/
    private boolean updCompType(int storetype, int control, int compId, InvoiceVO invoiceVO, String taxpayer) {
        List<Object[]> params = new ArrayList<>();
        String optInvSQL;
        if (getInvoice(compId) == null) {
            optInvSQL = "insert into {{?" + DSMConst.TB_COMP_INVOICE + "}} "
                    + "(cid, taxpayer, bankers, account, tel, cstatus, email) "
                    + " values(?,?,?,?,?,?,?)";
            params.add(new Object[]{compId, taxpayer, invoiceVO.getBankers(),invoiceVO.getAccount(),
                    invoiceVO.getTel(), 0, invoiceVO.getEmail()});
        } else {
            optInvSQL = "update {{?" + DSMConst.TB_COMP_INVOICE + "}} set taxpayer=?,bankers=?,account=?,"
                     + "tel=?, email=? where cstatus&1=0 and cid=?";
            params.add(new Object[]{taxpayer, invoiceVO.getBankers(),invoiceVO.getAccount(),
                    invoiceVO.getTel(),invoiceVO.getEmail(), compId});
        }
        //修改门店类型
        String updCompSQL = "update {{?" + DSMConst.TB_COMP + "}} set storetype=?,control=? "
                + " where cstatus&1=0 and cid=?";
        params.add(new Object[]{storetype, control, compId});
        return !ModelUtil.updateTransEmpty(baseDao.updateTransNative(new String[]{optInvSQL, updCompSQL}, params));
    }

    private InvoiceVO getInvoice(int compId) {
        String selectSQL = "select taxpayer,bankers,account,tel,email from {{?" + DSMConst.TB_COMP_INVOICE
                + "}} where cstatus&1=0 and cid=" + compId;
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        if (queryResult == null || queryResult.isEmpty()) return null;
        InvoiceVO[] invoiceVOS = new InvoiceVO[queryResult.size()];
        baseDao.convToEntity(queryResult, invoiceVOS, InvoiceVO.class, "taxpayer","bankers","account", "tel","email");
        return invoiceVOS[0];
    }


    private int settingCompStoretype(String certificateno) {
        Pattern pattern = Pattern.compile("^[\\u4e00-\\u9fa5]([A-D])[A-B][0-9].*$");
        Matcher matcher = pattern.matcher(certificateno);
        while (matcher.find()) {
            String str = matcher.group(1);
            return str.equals("A") ? 1 :
                    (str.equals("B") ? 2 :
                            (str.equals("C") ? 3 :
                                    (str.equals("D") ? 4 : -1) ));
        }
        return -1;
    }


    //10-营业执照  11-药店经营许可证  12-gsp认证  13-采购/提货委托书  14-采购/提货人员复印件  15-医疗机构执业许可证(医疗单位)
    //药店类型  0 医疗单位(10,13,14,15 ), 1 批发企业(10,11, 12,13, 14), 2零售连锁企业(10,11, 12,13, 14),
    // 3 零售连锁门店 4 零售单体企业(10,11, 12,13, 14) 4
    private String checkAptInfo(int storetype, Set<Integer> aTypeList) {
        Integer[] aTArr = new Integer[aTypeList.size()];
        Integer[] ylArr = {10,13,14,15};
        Integer[] otherArr = {10,11,12,13,14};
        Integer[] newArr;
        aTArr = aTypeList.toArray(aTArr);
        Arrays.sort(aTArr);
        switch (storetype) {
            case 0://医疗机构
                if (aTArr.length >= ylArr.length) {
                    newArr = Arrays.copyOfRange(aTArr,0,4);
                    if (!Arrays.equals(newArr,ylArr)) {
                        return "资质信息不完整！";
                    }
                } else {
                    return "资质信息不完整！";
                }
                break;
            default://其他
                if (aTArr.length >= otherArr.length) {
                    newArr = Arrays.copyOfRange(aTArr,0,5);
                    if (!Arrays.equals(newArr,otherArr)) {
                        return "资质信息不完整！";
                    }
                } else {
                    return "资质信息不完整！";
                }
                break;
        }
        return null;
    }


    private boolean optAptInfo(int compId, List<AptitudeVO> frontAptList, Set<Integer> faTypeList) {
        String insertAptSql = "insert into {{?" + DSMConst.TB_COMP_APTITUDE + "}} "
                + "(aptid,compid,atype,certificateno,validitys,validitye,cstatus,pname) "
                + " values(?,?,?,?,?,?,?,?) ";
        String updAptSql = "update {{?" + DSMConst.TB_COMP_APTITUDE + "}} set certificateno=?,"
                + " validitys=?,validitye=?,pname=? where cstatus&1=0 and compid=? and atype=?";
        String delSql = "update {{?" + DSMConst.TB_COMP_APTITUDE + "}} set cstatus=cstatus|1 where "
                + " cstatus&1=0 and compid=? and atype=?";
        List<AptitudeVO> aptitudeVOList = selectApt(compId);//该企业下已上传的资质
        List<String> sqlList = new ArrayList<>();
        List<Object[]> params = new ArrayList<>();
        if (aptitudeVOList.size() == 0) {//第一次完善资质信息
            for (AptitudeVO aptitudeVO :frontAptList) {
                params.add(new Object[]{getAptID(), compId, aptitudeVO.getAtype(), aptitudeVO.getCertificateno(),
                        aptitudeVO.getValiditys(),aptitudeVO.getValiditye(), 0, aptitudeVO.getPname()});
            }
            return !ModelUtil.updateTransEmpty(baseDao.updateBatchNative(insertAptSql, params, params.size()));
        } else {//之前完善过资质
            Set<Integer> oTypeList = new HashSet<>();//后台已保存的资质类型
            for (AptitudeVO aptitudeVO :aptitudeVOList) {
                oTypeList.add(aptitudeVO.getAtype());
                if (!faTypeList.contains(aptitudeVO.getAtype())) {
                    sqlList.add(delSql);
                    params.add(new Object[]{compId, aptitudeVO.getAtype()});
                }
            }
            for (AptitudeVO aptitudeFrontVO :frontAptList) {//遍历前台传过来的资质
                if (oTypeList.contains(aptitudeFrontVO.getAtype())) {//若存在更新资质
                    sqlList.add(updAptSql);
                    params.add(new Object[]{aptitudeFrontVO.getCertificateno(), aptitudeFrontVO.getValiditys(),
                            aptitudeFrontVO.getValiditye(), aptitudeFrontVO.getPname(), compId, aptitudeFrontVO.getAtype()});
                } else {//否则新增资质
                    sqlList.add(insertAptSql);
                    params.add(new Object[]{getAptID(), compId, aptitudeFrontVO.getAtype(), aptitudeFrontVO.getCertificateno(),
                            aptitudeFrontVO.getValiditys(),aptitudeFrontVO.getValiditye(), 0, aptitudeFrontVO.getPname()});
                }
            }
            String[] sqlNative = new String[sqlList.size()];
            sqlNative = sqlList.toArray(sqlNative);
            return !ModelUtil.updateTransEmpty(baseDao.updateTransNative(sqlNative, params));

        }

    }

    /* *
     * @description 查询企业下的资质
     * @params [compId]
     * @return void
     * @exception
     * @author 11842
     * @time  2019/6/20 16:01
     * @version 1.1.1
     **/
    private List<AptitudeVO> selectApt(int compId) {
        String selectSQL = "select aptid,compid,atype,certificateno,validitys,validitye,cstatus,pname "
                + " from {{?" + DSMConst.TB_COMP_APTITUDE + "}} where cstatus&1=0 and compid=? ";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL, compId);
        if (queryResult == null || queryResult.isEmpty()) return new ArrayList<>();
        AptitudeVO[] aptitudeVOS = new AptitudeVO[queryResult.size()];
        baseDao.convToEntity(queryResult, aptitudeVOS, AptitudeVO.class);
        return Arrays.asList(aptitudeVOS);
    }

    private List<BusScopeVO> getBusScopes(int compId) {
        String selectSQL = "select bscid,compid,busscope,codename,bs.cstatus "
                + " from {{?" + DSMConst.TB_COMP_BUS_SCOPE + "}} bs left join {{?"
                + DSMConst.TB_SYSTEM_BUS_SCOPE + "}} ss on bs.busscope=ss.code "
                + " where bs.cstatus&1=0 and compid=? ";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL, compId);
        if (queryResult == null || queryResult.isEmpty()) return new ArrayList<>();
        BusScopeVO[] busScopeVOS = new BusScopeVO[queryResult.size()];
        baseDao.convToEntity(queryResult, busScopeVOS, BusScopeVO.class);
        return Arrays.asList(busScopeVOS);
    }


    /**
     * @接口摘要 门店管理BD资质完善详情
     * @业务场景
     * @传参类型 json
     * @传参列表 {compid  企业id }
     * @返回列表 code=200(成功) data=结果信息
     */
    public Result getCompInfo(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int compid = jsonObject.get("compid").getAsInt();
        CompInfoVO compInfoVO = getCompInfo(compid);
        if (compInfoVO == null) return result.fail("未找到企业信息!");
        return result.success(compInfoVO);
    }

    public CompInfoVO getCompInfo(int compid){
        String sSQL = "select c.cid,cname,caddrcode,caddr,inviter,storetype,u.uphone,c.cstatus,control from {{?"
                + DSMConst.TB_COMP + "}} c left join {{?" + DSMConst.TB_SYSTEM_USER + "}} u "
                + " on c.cid = u.cid where c.cstatus&1=0 and u.cstatus&1=0 and c.cid=" + compid;
        List<Object[]> queryResult = baseDao.queryNative(sSQL);
        if (queryResult == null || queryResult.isEmpty()) return null;
        CompInfoVO[] compInfoVOS = new CompInfoVO[queryResult.size()];
        baseDao.convToEntity(queryResult,compInfoVOS, CompInfoVO.class);
        InvoiceVO invoiceVO = getInvoice(compid);
        compInfoVOS[0].setInvoiceVO(invoiceVO == null ? new InvoiceVO() : invoiceVO);
        compInfoVOS[0].setAptitudeVOS(selectApt(compid));
        List<BusScopeVO> busScopeVOS = getBusScopes(compid);
        compInfoVOS[0].setBusScopeVOS(busScopeVOS);
        StringBuilder bsSb = new StringBuilder();
        for (BusScopeVO busScopeVO : busScopeVOS) {
            bsSb.append(busScopeVO.getCodename()).append(",");
        }
        if (!bsSb.toString().isEmpty()){
            compInfoVOS[0].setBusScopeStr(bsSb.toString().substring(0, bsSb.toString().length() - 1));
        }
        compInfoVOS[0].setCaddrname(IceRemoteUtil.getCompleteName(compInfoVOS[0].getCaddrcode() + ""));
        return compInfoVOS[0];
    }

    /**
     * @接口摘要 门店经营范围维护 一块医药erp对接使用
     * @业务场景 ERP更新经营范围到一块医药
     * @传参类型 json
     * @传参列表 {compid  企业id   busArr [经营范围码数组]}
     * @返回列表 code=200(成功) data=结果信息
     */
    @UserPermission(ignore = true)
    public Result optBusScope(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject busObj = jsonParser.parse(json).getAsJsonObject();
        int compId = busObj.get("compid").getAsInt();
        String busArr = busObj.get("busArr").getAsString();
        if (compId > 0){
            List<Integer> newBusIdList = JSON.parseArray(busArr).toJavaList(Integer.class);
            operateData(newBusIdList, compId);
        } else {
            return result.fail("企业码为空！");
        }
        return result.success("操作成功！");
    }

    private List<Integer> getBusIdByCompId(int compId) {
        List<Integer> busList = new ArrayList<>();
        String selectSQL = "select busscope from {{?" + DSMConst.TB_COMP_BUS_SCOPE + "}} where "
                + " cstatus&1=0 and compid=?";
        List<Object[]> qResult = baseDao.queryNative(selectSQL, compId);
        if (qResult == null || qResult.isEmpty()) return busList;
        qResult.forEach(q -> {
            busList.add(Integer.parseInt(String.valueOf(q[0])));
        });
        return busList;
    }

    private void operateData(List<Integer> newBusIdList, int compId) {
        List<Object[]> insertParams = new ArrayList<>();
        List<Object[]> delParams = new ArrayList<>();
        List<Integer> oldBusIdList = getBusIdByCompId(compId);
        String insertSQL = "insert into {{?" + DSMConst.TB_COMP_BUS_SCOPE + "}} "
                + " (compid,busscope,bscid,cstatus) "
                + " values(?,?,?,?)";
        String delSql = "update {{?" + DSMConst.TB_COMP_BUS_SCOPE + "}} set cstatus=cstatus|1 where "
                + " cstatus&1=0 and compid=? and busscope=?";
        for (int newBusId: newBusIdList) {
            if (!oldBusIdList.contains(newBusId)) {
                insertParams.add(new Object[]{compId, newBusId,getBusID(),0});
            }
        }
        for (int oldBudId:oldBusIdList) {
            if (!newBusIdList.contains(oldBudId)) {
                delParams.add(new Object[]{compId, oldBudId});
            }
        }
        baseDao.updateBatchNative(insertSQL, insertParams, insertParams.size());
        baseDao.updateBatchNative(delSql, delParams, delParams.size());
    }

}
