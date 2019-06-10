package com.onek.user;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import cn.hy.otms.rpcproxy.sysmanage.User;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.*;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.context.UserSession;
import com.onek.entitys.Result;
import com.onek.user.entity.*;
import com.onek.util.GenIdUtil;
import com.onek.util.IceRemoteUtil;
import com.onek.util.RedisGlobalKeys;
import com.onek.util.RoleCodeCons;
import com.onek.util.area.AreaEntity;
import constant.DSMConst;
import dao.BaseDAO;
import org.hyrdpf.ds.AppConfig;
import org.hyrdpf.util.LogUtil;
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
            }
            for (Object[] objs : queryRet) {
                ProxyAreaTreeVO pproxyAreaVO = new ProxyAreaTreeVO();
                pproxyAreaVO.setArean(objs[1].toString());
                pproxyAreaVO.setAreac((objs[0].toString()));
                pproxyAreaVO.setLayer(1);
                areaList.add(pproxyAreaVO);
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
                + " where tu.cstatus&1=0 and ctype in (2,3) ";

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
                new String[]{"uid","uphone","urealname","roleid","adddate","addtime","offdate",
                        "offtime","ip","logindate","logintime","cstatus","arean","buid","buname"});

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
            return new Result().fail("当前门店不在该BD管辖范围内！");
        }

        GaoDeMapUtil.Point compPoint
                = new GaoDeMapUtil.Point(Double.parseDouble(cppoint.get(0)[0].toString()),
                Double.parseDouble(cppoint.get(0)[1].toString()));

        String selectAreaSQL = " select arearng from {{?" + DSMConst.TB_PROXY_UAREA + "}} where uid = ? and cstatus&1 = 0";

        List<Object[]> arpoint = null;
        if(bdid > 0){
            arpoint = baseDao.queryNative(selectAreaSQL,bdid);
            for(Object[] objs : arpoint){
                if(objs == null || StringUtils.isEmpty(objs[0].toString())){
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
        return new Result().fail("当前门店不在该BD管辖范围内！");
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
                " stu.uphone cursorPhone,sstu.uid bdmid,sstu.urealname bdmn from {{?"
                + DSMConst.TB_COMP + "}} cp  join {{?" + DSMConst.TB_SYSTEM_USER + "}} tu  on cp.cid = tu.cid "
                + "left join {{?" + DSMConst.TB_SYSTEM_USER + "}} stu on stu.uid = cp.inviter "
                + "left join {{?" + DSMConst.TB_SYSTEM_USER + "}} sstu on sstu.uid = stu.belong "
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
                new String[]{"companyId","phone","uid","company","addressCode","address","createdate",
                        "createtime","status","cursorId","cursorName","cursorPhone","bdmid","bdmn"});

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

        String  optSql = "update {{?"+DSMConst.TB_SYSTEM_USER+"}} set cstatus = cstatus | 32  where uid = ? and cstatus&1=0 and cstatus&32=0 ";
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


    public static void main(String[] args) {

    }
}
