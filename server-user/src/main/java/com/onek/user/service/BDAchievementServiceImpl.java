package com.onek.user.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.onek.context.AppContext;
import com.onek.user.entity.BDAchievementVO;
import com.onek.user.entity.BDCompVO;
import com.onek.user.entity.BDToOrderAchieveemntVO;
import com.onek.user.operations.BDAchievementOP;
import constant.DSMConst;
import dao.BaseDAO;
import util.GsonUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * 具体方法实现接口
 */
public class BDAchievementServiceImpl {
    /*按类别查询人员角色*/
    private static String _QUERY_SQL ="select * from ( "+
            "select uid,cid,roleid,urealname,belong,1 flag from {{?"+ DSMConst.TB_SYSTEM_USER+"}} where roleid & 512 > 0 and cstatus =0 union "+
            "select uid,cid,roleid,urealname,belong,2 flag from {{?"+ DSMConst .TB_SYSTEM_USER+"}} where roleid & 1024 > 0 and cstatus =0 union "+
            "select uid,cid,roleid,urealname,belong,3 flag from {{?"+ DSMConst .TB_SYSTEM_USER+"}} where roleid & 2048 > 0 and cstatus =0 union "+
            "select uid,cid,roleid,urealname,belong,4 flag from {{?"+ DSMConst .TB_SYSTEM_USER+"}} where roleid & 4096 > 0 and roleid & 512 =0 and cstatus =0 union "+
            "select uid,cid,roleid,urealname,belong,5 flag from {{?"+ DSMConst .TB_SYSTEM_USER+"}} where roleid & 8192 > 0 and cstatus =0 ) as a where a.uid != 2802";


    private final static int QDJL = 1024;//渠道经理
    private final static int CSHHR = 2048;//城市合伙人
    private final static int BDM = 4096;//BDM
    private final static int BD= 8192;//BD

    private final static int DEFAULT_QUERY_PARAM = 0;

    public String getData(List<String> areaList, List<BDCompVO> compList, List<BDToOrderAchieveemntVO> oList) {
        StringBuilder sqlb = new StringBuilder(_QUERY_SQL);
        List<Object[]> bdLists= BaseDAO.getBaseDAO().queryNative(sqlb.toString());

        BDAchievementVO[] bdAchievementVOS = new BDAchievementVO[bdLists.size()];
        BaseDAO.getBaseDAO().convToEntity(bdLists,bdAchievementVOS,BDAchievementVO.class);

        //循环遍历得到渠道总监-1，渠道经理-2，合伙人-3，BDM-4 , BD-5
        List<BDAchievementVO> boList = Arrays.asList(bdAchievementVOS);
        List<BDAchievementVO> qdzjList = new ArrayList<BDAchievementVO>();
        List<BDAchievementVO> qdjlList = new ArrayList<BDAchievementVO>();
        List<BDAchievementVO> hhrList = new ArrayList<BDAchievementVO>();
        List<BDAchievementVO> bdmList = new ArrayList<BDAchievementVO>();
        List<BDAchievementVO> bdList = new ArrayList<BDAchievementVO>();

        if(areaList.size()<=0) {//查询所有
            long roleFlag = getUserRole(bdAchievementVOS, DEFAULT_QUERY_PARAM);
            System.out.println(roleFlag);
            setData(roleFlag, DEFAULT_QUERY_PARAM, boList, qdzjList, qdjlList, hhrList, bdmList, bdList);
        }else{
            for(String str : areaList){
                long roleFlag = getUserRole(bdAchievementVOS, Long.parseLong(str));
                System.out.println(roleFlag);
                setData(roleFlag, Long.parseLong(str), boList, qdzjList, qdjlList, hhrList, bdmList, bdList);
            }
        }

        /**
         * 去重
         */
        bdList = removeDuplicate(bdList);
        bdmList = removeDuplicate(bdmList);
        hhrList = removeDuplicate(hhrList);
        qdjlList = removeDuplicate(qdjlList);
        qdzjList = removeDuplicate(qdzjList);

        String json = contData(compList,oList,bdList, bdmList, hhrList, qdjlList, qdzjList);
        return json;
    }


    /**
     * 获取查询当前查询最大角色
     * @param boList
     * @param uidparam
     * @return
     */
    private long getUserRole(BDAchievementVO[] boList,long uidparam) {
        if(uidparam == 0) {
            return 1;
        }
        long rolesoure = Long.MAX_VALUE;
        for (int i = 0; i < boList.length; i++) {
            long uid = boList[i].getUid();
            long role = boList[i].getFlag();
            if(uid == uidparam) {
                rolesoure = Math.min(rolesoure, role);
            }
        }
        return rolesoure;
    }

    /**
     * 组装数据
     * @param flag
     * @param uid
     * @param boList
     */
    private void setData(long flag,long uid,List<BDAchievementVO> ...boList) {
        for (int i = 0; i < boList[0].size(); i++) {
            BDAchievementVO BDAchievementVO = boList[0].get(i);
            if(BDAchievementVO.getFlag() == flag && String.valueOf(uid).equals(String.valueOf(BDAchievementVO.getUid())) && uid >0) {
                boList[Integer.parseInt(String.valueOf(flag))].add(BDAchievementVO);
            }
            if(BDAchievementVO.getFlag() == 1 && BDAchievementVO.getFlag() >=flag) {
                if(BDAchievementVO.getFlag() == flag && uid>0) {
                    continue;
                }
                boList[1].add(BDAchievementVO);
            }else if(BDAchievementVO.getFlag() == 2 && BDAchievementVO.getFlag() >=flag) {
                if(BDAchievementVO.getFlag() == flag && uid>0) {
                    continue;
                }
                boList[2].add(BDAchievementVO);
            }else if(BDAchievementVO.getFlag() == 3 && BDAchievementVO.getFlag() >=flag) {
                if(BDAchievementVO.getFlag() == flag && uid>0) {
                    continue;
                }
                boList[3].add(BDAchievementVO);
            }else if(BDAchievementVO.getFlag() == 4 && BDAchievementVO.getFlag() >=flag) {
                if(BDAchievementVO.getFlag() == flag && uid>0) {
                    continue;
                }
                boList[4].add(BDAchievementVO);
            }else if(BDAchievementVO.getFlag() == 5 && BDAchievementVO.getFlag() >=flag) {
                if(BDAchievementVO.getFlag() == flag && uid>0) {
                    continue;
                }
                boList[5].add(BDAchievementVO);
            }
        }

        if(flag != 1 && flag >0) {
            BDAchievementVO bdbdm = boList[Integer.valueOf(String.valueOf(flag))].get(0);
            String pbelong = String.valueOf(bdbdm.getBelong());
            for(int i =Integer.valueOf(String.valueOf(flag))-1; i>0;i--) {

                for(int j= 0; j < boList[0].size(); j++) {
                    BDAchievementVO BDAchievementVO = boList[0].get(j);
                    String puid = String.valueOf(BDAchievementVO.getUid());
                    String pbelong2 = String.valueOf(BDAchievementVO.getBelong());

                    if(puid.equals(pbelong) && String.valueOf(BDAchievementVO.getFlag()).equals(String.valueOf(i))) {

                        boList[i].add(BDAchievementVO);
                        pbelong =pbelong2;
                        break;
                    }
                    if(boList[i].size()<=0) {
                        if(pbelong2.equals(pbelong)  && String.valueOf(BDAchievementVO.getFlag()).equals(String.valueOf(i))) {
                            boList[i].add(BDAchievementVO);

                        }
                    }
                }
            }
        }
    }


    /**
     * 将查询出的用户组装成为json
     * @param bd
     * @param bdms
     * @param csjls
     * @param qdjls
     * @param qdzjs
     * @return
     */
    private static String contData(List<BDCompVO> compList, List<BDToOrderAchieveemntVO> ordList,List<BDAchievementVO> bd, List<BDAchievementVO>bdms, List<BDAchievementVO>csjls, List<BDAchievementVO>qdjls, List<BDAchievementVO>qdzjs) {
//        List<OrderVO> ordList = getOrderInfos();
//        List<Comp> compList = getCompInfo();
        List gl = new ArrayList();
        List bduids = new ArrayList();
        JSONArray reJson = new JSONArray();
        for (int i = 0; i < qdzjs.size(); i++) {
            JSONObject json = new JSONObject();
            JSONObject qdzjSubtotal = new JSONObject();//渠道总监小计
            Map map = (Map) bean2Map(qdzjs.get(i));
            Long uid = (Long) map.get("uid");
            String name = (String) map.get("name");
            Long role = (Long) map.get("role");
            Long belong = (Long) map.get("belong");

            if(uid == 0) {
                continue;
            }
            json.put("uid", uid);
            json.put("name",name);
            json.put("roleid", role);
            JSONArray qdjl = new JSONArray();
            for (int j = 0; j < qdjls.size(); j++) {
                JSONObject qdjlSubtotal = new JSONObject();//渠道经理小计
                Map qdjlmap = (Map) bean2Map(qdjls.get(j));
                Long qdjluid = (Long) qdjlmap.get("uid");
                String qdjlname= (String) qdjlmap.get("name");
                Long qdjlrole = (Long) qdjlmap.get("role");
                Long qdjlbelong = (Long) qdjlmap.get("belong");
                if(qdjluid == 0) {
                    continue;
                }
                if(String.valueOf(uid).equals(String.valueOf(qdjlbelong))) {
                    JSONObject qdjljson = new JSONObject();
                    qdjljson.put("uid", qdjluid);
                    qdjljson.put("name",qdjlname);
                    qdjljson.put("roleid", qdjlrole);
                    qdjl.add(qdjljson);

                    JSONArray csjl = new JSONArray();

                    for (int k = 0; k < csjls.size(); k++) {
                        JSONObject csjlSubtotal = new JSONObject();//城市经理小计
                        Map csjlmap = (Map) bean2Map(csjls.get(k));
                        Long csjluid = (Long) csjlmap.get("uid");
                        String csjlname= (String) csjlmap.get("name");
                        Long csjlrole = (Long) csjlmap.get("role");
                        Long csjlbelong = (Long) csjlmap.get("belong");

                        if(csjluid == 0) {
                            continue;
                        }
                        if(String.valueOf(qdjluid).equals(String.valueOf(csjlbelong)) || (csjlrole & QDJL)>0) {
                            JSONObject csjljson = new JSONObject();
                            csjljson.put("uid", csjluid);
                            csjljson.put("name",csjlname);
                            csjljson.put("roleid", csjlrole);
                            csjl.add(csjljson);

//

                            JSONArray bdm = new JSONArray();
                            //csjl是否为BDM
                            JSONObject bdjson2 = new JSONObject();
                            if((csjlrole & BDM)>0) {
//								bdjson2.put("uid", csjluid);
//								bdjson2.put("name",csjlname);
//								bdjson2.put("roleid", csjlrole);
//								bdm.put(bdjson2);

//								BDAchievementVO bd2 = new BDAchievementVO();
//								bd2.setCid(0);
//								bd2.setUid(csjluid);
//								bd2.setRoleid(csjlrole);
//								bd2.setUrealname(csjlname);
//								bd2.setBelong(csjluid);
//								bd2.setFlag(3);
//								bdms.add(bd2);
                                //csjluid =
                            }
                            for (int l = 0; l < bdms.size(); l++) {
                                Map bdmmap = (Map) bean2Map(bdms.get(l));
                                Long bdmuid = (Long) bdmmap.get("uid");
                                String bdmname= (String) bdmmap.get("name");
                                Long bdmrole = (Long) bdmmap.get("role");
                                Long bdmbelong = (Long) bdmmap.get("belong");

                                if(bdmuid == 0) {
                                    continue;
                                }

                                if(String.valueOf(csjluid).equals(String.valueOf(bdmbelong))) {
                                    JSONObject bdmjson = new JSONObject();
                                    bdmjson.put("uid", bdmuid);
                                    bdmjson.put("name",bdmname);
                                    bdmjson.put("roleid", bdmrole);
                                    bdm.add(bdmjson);

                                    JSONArray bds = new JSONArray();
//									if(!bdjson2.isEmpty()) {
//										bds.put(bdjson2);
//									}

                                    JSONObject bdSubtotal = new JSONObject();

                                    //bdm是否为BD
                                    if((bdmrole & BD)>0) {
                                        gl.add(bdmuid);
                                        //bduids.add(bdmuid);
                                        if(!bduids.contains(bdmuid)) {
                                            JSONObject bdjson1 = new JSONObject();
                                            bdjson1.put("uid", bdmuid);
                                            bdjson1.put("name",bdmname);
                                            bdjson1.put("roleid", bdmrole);
                                            String info = BDOrderAchievementServiceImpl.excall(bdmuid,compList,ordList);
                                            getSubtotal(bdSubtotal, info);
                                            bdjson1.put("info", info);
                                            bds.add(bdjson1);
                                        }
                                    }
                                    for (int m = 0; m < bd.size(); m++) {
                                        Map bdmap = (Map) bean2Map(bd.get(m));
                                        Long bduid = (Long) bdmap.get("uid");
                                        String bdname= (String) bdmap.get("name");
                                        Long bdrole = (Long) bdmap.get("role");
                                        Long bdbelong = (Long) bdmap.get("belong");

                                        if(bduid == 0) {
                                            continue;
                                        }

                                        JSONObject bdjson = new JSONObject();
                                        if(String.valueOf(bdmuid).equals(String.valueOf(bdbelong))) {
                                            bdjson.put("uid", bduid);
                                            bdjson.put("name",bdname);
                                            bdjson.put("roleid", bdrole);
                                            //if((bdrole&BDM) == 0 && (bdrole&BD) > 0)
                                            if(gl.contains(bduid)) {
                                                continue;
                                            }
                                            bduids.add(bduid);
                                            String info = BDOrderAchievementServiceImpl.excall(bduid,compList,ordList);
                                            getSubtotal(bdSubtotal, info);
                                            bdjson.put("info", info);
                                            bds.add(bdjson);

                                        }

                                    }

                                    bdmjson.put("subtotal",bdSubtotal.toString());
                                    getSubtotal(csjlSubtotal, bdSubtotal.toString());
                                    getSubtotal(qdjlSubtotal, bdSubtotal.toString());
                                    getSubtotal(qdzjSubtotal, bdSubtotal.toString());
                                    bdmjson.put("children",bds);
                                }
                            }
                            csjljson.put("subtotal",csjlSubtotal.toString());
                            csjljson.put("children",bdm);
                        }
                    }
                    qdjljson.put("subtotal",qdjlSubtotal.toString());
                    qdjljson.put("children", csjl);
                }
            }
            json.put("subtotal",qdzjSubtotal.toString());
            json.put("children", qdjl);
            reJson.add(json);
        }

        return reJson.toString();
    }


    /**
     * 对象转换程为Map
     * @param bdbdm
     * @return
     */
    private static Map bean2Map(BDAchievementVO bdbdm) {
        HashMap map = new HashMap();
        map.put("uid", bdbdm.getUid());
        map.put("role", bdbdm.getRoleid());
        map.put("name", bdbdm.getUrealname());
        map.put("belong",bdbdm.getBelong());
        return map;
    }


    /**
     * list去重
     * @param list
     * @return
     */
    public static List removeDuplicate(List list) {
        HashSet h = new HashSet(list);
        list.clear();
        list.addAll(h);
        return list;
    }

    /**
     * 组装数据
     * @param jsonObject
     * @param json
     */
    private static void getSubtotal(JSONObject jsonObject,String json) {
        JSONObject js = JSONObject.parseObject(json);
        Set set = js.keySet();
        Iterator iterator = set.iterator();
        while (iterator.hasNext()) {
            String str = (String) iterator.next();
            String re =js.getString(str);
            if(jsonObject.containsKey(str)) {
                String oldre = jsonObject.getString(str);
                jsonObject.put(str, getResult(oldre,re));
            }else {
                jsonObject.put(str,re);
            }
        }
    }

    private static double getResult(String num,String num2) {
        BigDecimal bigDecimal = new BigDecimal(num);
        BigDecimal bigDecimal2 = new BigDecimal(num2);
        return bigDecimal.add(bigDecimal2).doubleValue();
    }
}
