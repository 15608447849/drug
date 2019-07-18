package com.onek.user;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.user.entity.StaffImpVO;
import com.onek.util.GenIdUtil;
import com.onek.util.IceRemoteUtil;
import com.onek.util.RedisGlobalKeys;
import com.onek.util.RoleCodeCons;
import constant.DSMConst;
import dao.BaseDAO;
import util.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static com.onek.util.RedisGlobalKeys.getAptID;
import static com.onek.util.RedisGlobalKeys.getUserCode;
import static constant.DSMConst.TB_COMP;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName StaffImportModule
 * @Description TODO
 * @date 2019-07-13 15:00
 */
public class StaffImportModule {


    public static HashMap<String,Integer> roleMap = new HashMap<String,Integer>();

    static{
        roleMap.put("超级管理员",1);
        roleMap.put("门店", 2);
        roleMap.put("药厂", 4);
        roleMap.put("批发商", 8);
        roleMap.put("商城管理员", 16);
        roleMap.put("企业管理员", 32);
        roleMap.put("商品管理员", 64);
        roleMap.put("运营管理员", 128);
        roleMap.put("财务管理员",256);
        roleMap.put("渠道总监", 512);
        roleMap.put("渠道经理", 1024);
        roleMap.put("合伙人", 2048);
        roleMap.put("BDM", 4096);
        roleMap.put("BD", 8192);
    }


    private static final BaseDAO baseDao = BaseDAO.getBaseDAO();

    private final String INSERT_USER = "insert into {{?" + DSMConst.TB_SYSTEM_USER + "}} "
            + "(uid,uphone,urealname,upw,roleid,adddate,addtime,belong,cstatus,cid)"
            + " values (?,?,?,?,?,CURRENT_DATE,CURRENT_TIME,?,?,?)";


    private final String UPDATE_USER = "update {{?" + DSMConst.TB_SYSTEM_USER + "}} "
            + " set urealname = ?,roleid = ?,adddate = CURRENT_DATE,addtime = CURRENT_TIME,belong = ?," +
            "cstatus = ?,cid = ? where uphone = ? ";


    private final String SELECT_USER_BYUPHONE = "select uid from {{?" + DSMConst.TB_SYSTEM_USER + "}} "
            + " where uphone = ? ";


    private final String SELECT_USER_ROLEID = "select uid from {{?" + DSMConst.TB_SYSTEM_ROLE + "}} "
            + " where uphone = ? ";


    private final String SELECT_COMP_BYUPHONE = "select cid from {{?" + DSMConst.TB_COMP + "}} "
            + " where cid in (select cid from {{?"+DSMConst.TB_SYSTEM_USER+"}} where uphone = ?)  ";

    private final String INSERT_COMP = "INSERT INTO {{?"+ TB_COMP +"}} " +
            "(ctype,cid,cname,cnamehash,cstatus,createdate,createtime) " +
            "VALUES (?,?,?,crc32(?),?,CURRENT_DATE,CURRENT_TIME)";


    private final String UPDATE_COMP = "UPDATE {{?"+ TB_COMP +"}} SET cname = ?,cnamehash = crc32(?) " +
            ",createdate = CURRENT_DATE,createtime = CURRENT_TIME " +
            " where cid in (select cid from {{?"+DSMConst.TB_SYSTEM_USER+"}} where uphone = ?) ";

    private final String INSERT_CRT = "INSERT INTO {{?"+DSMConst.TB_COMP_APTITUDE+"}}" +
            " (aptid,compid,atype,certificateno,validitys,validitye)" +
            " VALUES (?,?,?,?,CURRENT_DATE,'2020-07-20')";


    private final String SELECT_AREAC = "select areac from {{?"+DSMConst.TB_AREA_PCA +"}} " +
            "where areac like '43%' and cstatus & 1 = 0 and arean = ?";


    private final String INSERT_AREA = "insert into {{?"+DSMConst.TB_PROXY_UAREA+"}}" +
            " (unqid,uid,areac,cstatus,arearng) values (?,?,?,?,?) ";

    private final String DEL_AREA = "delete from {{?"+DSMConst.TB_PROXY_UAREA+"}}" +
            " where uid = ? ";



    private final String SELECT_AREA_EXT = "select 1 from {{?"+DSMConst.TB_PROXY_UAREA+"}} where  uid = ? and areac = ? and cstatus & 1 = 0 ";




    @UserPermission(ignore = true)
    public Result importStaffData(AppContext appContext) {
        String json = appContext.param.json;
        Result result = new Result();
        JsonParser jsonParser = new JsonParser();
        JsonArray jsonArray = jsonParser.parse(json).getAsJsonArray();

        if (jsonArray.size() == 0) {
            return result.success(null);
        }
        List<StaffImpVO> staffImpVOS = new ArrayList<>();
        Gson gson = new Gson();
        for (JsonElement shopVO : jsonArray) {
            StaffImpVO staffImpVO = gson.fromJson(shopVO, StaffImpVO.class);
            staffImpVOS.add(staffImpVO);
        }
        this.importStaffData(staffImpVOS);
        return result.success("导入完成！");
    }

    public void importStaffData(List<StaffImpVO> staffImpVOList){
        if(staffImpVOList == null || staffImpVOList.isEmpty()){
            return;
        }

        //合伙人List
        List<StaffImpVO> partnerList = new ArrayList<>();

        //BDM List
        List<StaffImpVO> bdmList = new ArrayList<>();

        //BD List
        List<StaffImpVO> bdList = new ArrayList<>();

        //其他人员 List
        List<StaffImpVO> otherList = new ArrayList<>();


        for (StaffImpVO staffImpVO: staffImpVOList){

            if(staffImpVO.getRoleName().startsWith("合伙人")){
                partnerList.add(staffImpVO);
            }else if(staffImpVO.getRoleName().equals("BDM")){
                bdmList.add(staffImpVO);
            }else if(staffImpVO.getRoleName().equals("BD")){
                bdList.add(staffImpVO);
            }else{
                otherList.add(staffImpVO);
            }
        }
        insertOtherStaff(otherList);
        insertParnerStaff(partnerList);
        insertBdmStaff(bdmList);
        insertBdStaff(bdList);
    }

    /**
     * 新增其他人员
     * @param otherList
     * @return
     */
    private int insertOtherStaff(List<StaffImpVO> otherList){
        List<String> sqlList = new ArrayList<>();
        List<Object[]> parmList = new ArrayList<>();
        for (StaffImpVO staffImpVO: otherList){
            List<Object[]> userExt = baseDao.queryNative(SELECT_USER_BYUPHONE, staffImpVO.getUphone());
            int uid = 0;
            if(userExt != null && !userExt.isEmpty()){
                parmList.add(new Object[]{staffImpVO.getUname(),
                        roleMap.get(staffImpVO.getRoleName()),
                        0,0,0,staffImpVO.getUphone()
                 });
                sqlList.add(UPDATE_USER);
                uid = Integer.parseInt(userExt.get(0)[0].toString());
            }else{
                String pwd = EncryptUtils.encryption(String.valueOf(staffImpVO.getUphone()).substring(5));
                uid = getUserCode();

                parmList.add(new Object[]{uid,staffImpVO.getUphone(),staffImpVO.getUname(),pwd,
                        roleMap.get(staffImpVO.getRoleName()),0,0,0});
                sqlList.add(INSERT_USER);
            }
            if(uid > 0 && (roleMap.get(staffImpVO.getRoleName()) & 1024)>0 ){
                baseDao.updateNative(DEL_AREA,uid);
                insertArea(staffImpVO.getArean(),uid);
            }

        }

        String[] sqlStr = new String[sqlList.size()];
        sqlStr = sqlList.toArray(sqlStr);
        boolean b = !ModelUtil.updateTransEmpty(
                baseDao.updateTransNative(sqlStr,parmList));
        if(b){
            String selectBelong =  "select uid from {{?" + DSMConst.TB_SYSTEM_USER + "}}  where roleid & 512 > 0 and cstatus & 1 = 0";
            String updateBelong = "update {{?"+DSMConst.TB_SYSTEM_USER +"}} set belong = ? where roleid & 1024 > 0 and cstatus & 1 = 0";

            List<Object[]> belongExt = baseDao.queryNative(selectBelong);
            int belong = 0;
            if(belongExt == null|| belongExt.isEmpty()){
                return 0;
            }
            belong = Integer.parseInt(belongExt.get(0)[0].toString());
            baseDao.updateNative(updateBelong,belong);
            return 1;
        }
        return 0;
    }


    /**
     * 新增合伙人
     * @param partnerList
     * @return
     */
    private int insertParnerStaff(List<StaffImpVO> partnerList){
        List<String> sqlList = new ArrayList<>();
        List<Object[]> parmList = new ArrayList<>();



        String updateUserSql = "update {{?" + DSMConst.TB_SYSTEM_USER + "}} "
                + " set urealname = ?,roleid = ?,adddate = CURRENT_DATE,addtime = CURRENT_TIME,belong = ?," +
                "cstatus = ? where uphone = ? ";
        for (StaffImpVO staffImpVO: partnerList){
            int roleid = 2048;
            if(staffImpVO.getRoleName().contains("BDM")){
                roleid = 2048 | 4096;
            }
            List<Object[]> userExt = baseDao.queryNative(SELECT_USER_BYUPHONE, staffImpVO.getUphone());
            List<Object[]> belongExt = baseDao.queryNative(SELECT_USER_BYUPHONE, staffImpVO.getBmanager());
            int belong = 0;
            int uid = 0;
            if(belongExt != null && !belongExt.isEmpty()){
                belong = Integer.parseInt(belongExt.get(0)[0].toString());
            }
            if(userExt != null && !userExt.isEmpty()){
                uid = Integer.parseInt(userExt.get(0)[0].toString());
                parmList.add(new Object[]{staffImpVO.getUname(),
                        roleid,
                        belong,0,staffImpVO.getUphone()
                });

                parmList.add(new Object[]{staffImpVO.getPartnerName(),staffImpVO.getPartnerName(),
                      staffImpVO.getUphone()
                });
                sqlList.add(updateUserSql);
                sqlList.add(UPDATE_COMP);
            }else{
                String pwd = EncryptUtils.encryption(String.valueOf(staffImpVO.getUphone()).substring(5));
                uid = getUserCode();
                long compid = RedisGlobalKeys.getProxyCompanyCode();

                parmList.add(new Object[]{uid,staffImpVO.getUphone(),staffImpVO.getUname(),pwd,
                        roleid,belong,0,compid});

                parmList.add(new Object[]{2,compid,staffImpVO.getPartnerName(),
                        staffImpVO.getPartnerName(),0});

                parmList.add(new Object[]{getAptID(),compid,10,
                        "111"});

                parmList.add(new Object[]{getAptID(),compid,13,
                        "222"});

                sqlList.add(INSERT_USER);
                sqlList.add(INSERT_COMP);
                sqlList.add(INSERT_CRT);
                sqlList.add(INSERT_CRT);
            }
            baseDao.updateNative(DEL_AREA,uid);
            insertArea(staffImpVO.getArean(),uid);
        }

        String[] sqlStr = new String[sqlList.size()];
        sqlStr = sqlList.toArray(sqlStr);
        boolean b = !ModelUtil.updateTransEmpty(
                baseDao.updateTransNative(sqlStr,parmList));
        if(b){
            return 1;
        }
        return 0;
    }


    private int insertArea(String areanStr,int uid){
        if(StringUtils.isEmpty(areanStr)){
            return 0;
        }
        List<String> sqlList = new ArrayList<>();
        List<Object[]> parmList = new ArrayList<>();
        String [] areans = areanStr.split("、");
        for (String arean : areans){
            List<Object[]> acRet = baseDao.queryNative(SELECT_AREAC, arean);
            if(acRet == null || acRet.isEmpty()){
                continue;
            }
            List<List<GaoDeMapUtil.Point>> lists = GaoDeMapUtil.areaPolyline(IceRemoteUtil.getCompleteName(acRet.get(0)[0].toString()));
            for (List<GaoDeMapUtil.Point> plist : lists){
                String jwp = GsonUtils.javaBeanToJson(plist);
                sqlList.add(INSERT_AREA);
                parmList.add(new Object[]{GenIdUtil.getUnqId(),uid,acRet.get(0)[0],
                        0,jwp});
            }
        }
        String[] sqlStr = new String[sqlList.size()];
        sqlStr = sqlList.toArray(sqlStr);
        boolean b = !ModelUtil.updateTransEmpty(
                baseDao.updateTransNative(sqlStr,parmList));
        if(b){
            return 1;
        }
        return 0;
    }


    /**
     * 新增BDM
     * @param bdmList
     * @return
     */
    private int insertBdmStaff(List<StaffImpVO> bdmList){
        List<String> sqlList = new ArrayList<>();
        List<Object[]> parmList = new ArrayList<>();
        for (StaffImpVO staffImpVO: bdmList){
            List<Object[]> userExt = baseDao.queryNative(SELECT_USER_BYUPHONE, staffImpVO.getUphone());
            List<Object[]> belongExt = baseDao.queryNative(SELECT_USER_BYUPHONE, staffImpVO.getBpartner());

            int belong = 0;
            int belongCid = 0;
            int uid = 0;
            if(belongExt != null && !belongExt.isEmpty()){
                belong = Integer.parseInt(belongExt.get(0)[0].toString());
            }

            if(staffImpVO.getPartnerName().equals("直营")){
                belongCid = 100000001;
            }else{
                List<Object[]> belongCidExt = baseDao.queryNative(SELECT_COMP_BYUPHONE, staffImpVO.getBpartner());
                if(belongCidExt != null && !belongCidExt.isEmpty()){
                    belongCid = Integer.parseInt(belongCidExt.get(0)[0].toString());
                }
            }

            if(userExt != null && !userExt.isEmpty()){
                uid = Integer.parseInt(userExt.get(0)[0].toString());
                parmList.add(new Object[]{staffImpVO.getUname(),
                        roleMap.get(staffImpVO.getRoleName()),
                        belong,0,belongCid,staffImpVO.getUphone()
                });
                sqlList.add(UPDATE_USER);
            }else{
                String pwd = EncryptUtils.encryption(String.valueOf(staffImpVO.getUphone()).substring(5));
                uid = getUserCode();
                parmList.add(new Object[]{uid,staffImpVO.getUphone(),staffImpVO.getUname(),pwd,
                        roleMap.get(staffImpVO.getRoleName()),belong,0,belongCid});

                sqlList.add(INSERT_USER);
            }
            baseDao.updateNative(DEL_AREA,uid);
            insertArea(staffImpVO.getArean(),uid);
        }

        String[] sqlStr = new String[sqlList.size()];
        sqlStr = sqlList.toArray(sqlStr);
        boolean b = !ModelUtil.updateTransEmpty(
                baseDao.updateTransNative(sqlStr,parmList));
        if(b){
            return 1;
        }
        return 0;
    }


    /**
     * 新增BD
     * @param bdmList
     * @return
     */
    private int insertBdStaff(List<StaffImpVO> bdmList){
        List<String> sqlList = new ArrayList<>();
        List<Object[]> parmList = new ArrayList<>();
        for (StaffImpVO staffImpVO: bdmList){
            List<Object[]> userExt = baseDao.queryNative(SELECT_USER_BYUPHONE, staffImpVO.getUphone());
            List<Object[]> belongExt = baseDao.queryNative(SELECT_USER_BYUPHONE, staffImpVO.getBbdm());
           // List<Object[]> belongCidExt = baseDao.queryNative(SELECT_COMP_BYUPHONE, staffImpVO.getBpartner());
            int belong = 0;
            int belongCid = 0;
            int uid = 0;
            if(belongExt != null && !belongExt.isEmpty()){
                belong = Integer.parseInt(belongExt.get(0)[0].toString());
            }

            if(staffImpVO.getPartnerName().equals("直营")){
                belongCid = 100000001;
            }else{
                List<Object[]> belongCidExt = baseDao.queryNative(SELECT_COMP_BYUPHONE, staffImpVO.getBpartner());
                if(belongCidExt != null && !belongCidExt.isEmpty()){
                    belongCid = Integer.parseInt(belongCidExt.get(0)[0].toString());
                }
            }
            if(userExt != null && !userExt.isEmpty()){
                uid = Integer.parseInt(userExt.get(0)[0].toString());
                parmList.add(new Object[]{staffImpVO.getUname(),
                        roleMap.get(staffImpVO.getRoleName()),
                        belong,0,belongCid,staffImpVO.getUphone()
                });
                sqlList.add(UPDATE_USER);
            }else{
                String pwd = EncryptUtils.encryption(String.valueOf(staffImpVO.getUphone()).substring(5));
                uid = getUserCode();
                parmList.add(new Object[]{uid,staffImpVO.getUphone(),staffImpVO.getUname(),pwd,
                        roleMap.get(staffImpVO.getRoleName()),belong,0,belongCid});
                sqlList.add(INSERT_USER);
            }
            baseDao.updateNative(DEL_AREA,uid);
            insertArea(staffImpVO.getArean(),uid);
        }

        String[] sqlStr = new String[sqlList.size()];
        sqlStr = sqlList.toArray(sqlStr);
        boolean b = !ModelUtil.updateTransEmpty(
                baseDao.updateTransNative(sqlStr,parmList));
        if(b){
            return 1;
        }
        return 0;
    }
}
