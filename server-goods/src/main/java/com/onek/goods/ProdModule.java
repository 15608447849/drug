package com.onek.goods;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.consts.ESConstant;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.goods.entities.MallFloorVO;
import com.onek.goods.entities.ProdVO;
import com.onek.goods.service.MallFloorImpl;
import com.onek.goods.util.ProdESUtil;
import com.onek.util.dict.DictStore;
import com.onek.util.fs.FileServerUtils;
import com.onek.util.prod.ProdActPriceUtil;
import com.onek.util.prod.ProdPriceEntity;
import constant.DSMConst;
import dao.BaseDAO;
import global.IceRemoteUtil;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import redis.IRedisCache;
import redis.proxy.CacheProxyInstance;
import util.BeanMapUtils;
import util.NumUtil;
import util.StringUtils;
import util.TimeUtils;

import java.util.*;

import static global.IceRemoteUtil.calcMultiProdActPrize;


@SuppressWarnings("unchecked")
public class ProdModule {

    private static IRedisCache mallFloorProxy = (IRedisCache) CacheProxyInstance.createInstance(new MallFloorImpl());

    private static final BaseDAO BASE_DAO = BaseDAO.getBaseDAO();

    private static String RULE_CODE_ACT_PROD_SQL = "select a.unqid,d.gcode,d.actstock,d.limitnum from " +
            "{{?"+ DSMConst.TD_PROM_ACT +"}} a, {{?"+DSMConst.TD_PROM_ASSDRUG+"}} d " +
            "where a.unqid = d.actcode " +
            "and a.brulecode = ? " +
            "and fun_prom_cycle(a.unqid, a.acttype, a.actcycle, ?, 1) > 0";

    private static String ACT_PROD_BY_ACTCODE_SQL = "select a.unqid,d.gcode,d.actstock,d.limitnum from " +
            "{{?"+ DSMConst.TD_PROM_ACT +"}} a, {{?"+DSMConst.TD_PROM_ASSDRUG+"}} d " +
            "where a.unqid = d.actcode " +
            "and d.actcode = ? " +
            "and a.sdate <= CURDATE() and CURDATE()<= a.edate ";

    private static String NEWMEMBER_ACT_PROD_SQL = "select a.unqid,d.gcode,d.actstock,d.limitnum from " +
            "{{?"+ DSMConst.TD_PROM_ACT +"}} a, {{?"+DSMConst.TD_PROM_ASSDRUG+"}} d " +
            " where a.unqid = d.actcode  " +
            "and a.cstatus&1 = 0 " +
            "and a.qualcode = 1 and a.qualvalue = 0 and fun_prom_cycle(a.unqid, a.acttype, a.actcycle, ?, 1) > 0";

    private static String EXEMPOST_ACT_PROD_SQL = "select a.unqid,d.gcode,d.actstock,d.limitnum from " +
            "{{?"+ DSMConst.TD_PROM_ACT +"}} a, {{?"+DSMConst.TD_PROM_ASSDRUG+"}} d " +
            " where a.unqid = d.actcode  " +
            "and a.cstatus&1 = 0 " +
            "and brulecode like '112%' " +
            "and fun_prom_cycle(a.unqid, a.acttype, a.actcycle, ?, 1) > 0";

    private static String TEAM_BUY_LADOFF_SQL = "select ladamt,ladnum,offer from {{?" + DSMConst.TD_PROM_LADOFF + "}} where offercode like '1133%'";

    private static String PROM_TIME_SQL = "select sdate,edate from {{?"+ DSMConst.TD_PROM_TIME +"}} where cstatus&1=0 and actcode = ?";

//
//    public static final Long NEW = 1L; // 新品专区
//    public static final Long HOT = 2L; // 热销专区
//    public static final Long SECKILL = 4L; // 秒杀专区
//    public static final Long TEAMBUY = 8L; // 一块购专区
//    public static final Long EXEMPOST = 16L; // 包邮专区
//    public static final Long NEWMEMBER = 32L; // 新人专享
//    public static final Long CHINAFAMPRE = 64L; // 中华名方
//    public static final Long CHOOSEFORYOU = 128L; // 为你精选
//    public static final Long BRAND = 256L; // 品牌
//    public static final Long DISCOUNT = 512L; // 限时折扣

    @UserPermission(ignore = true)
    public Result getMallFloorProd(AppContext appContext) {
//    public Result getMallFloorProd() {
        List<MallFloorVO> mallFloorVOList = (List<MallFloorVO>) mallFloorProxy.queryAll();
        return new Result().success(mallFloorVOList);
    }

    @UserPermission(ignore = true)
    public Result getNewMallFloor(AppContext appContext) {
        List<Integer> bb = new ArrayList(){{
            add(128); add(512);
        }};
        Set<Integer> result = new HashSet<>();
        NumUtil.arrangeAdd(256, bb, result);

        List<ProdVO> newProdList = getFilterProds(result, 1);

        List<ProdVO> filterProdList = loadProd(newProdList, 256);

        return new Result().success(filterProdList);
    }

    @UserPermission(ignore = true)
    public Result getChooseForYouMallFloor(AppContext appContext) {
        List<Integer> bb1 = new ArrayList(){{
            add(218); add(256);
        }};
        Set<Integer> result1 = new HashSet<>();
        NumUtil.arrangeAdd(512, bb1, result1);

        List<ProdVO> prodList = getFilterProds(result1, 1);
        List<ProdVO> filterProdList = loadProd(prodList, 128);

        return new Result().success(filterProdList);
    }

    @UserPermission(ignore = true)
    public Result getHotMallFloor(AppContext appContext) {
        List<Integer> bb1 = new ArrayList(){{
            add(256); add(512);
        }};
        Set<Integer> result = new HashSet<>();
        NumUtil.arrangeAdd(128, bb1, result);

        List<ProdVO> hotProdList = getFilterProds(result, 2);
        List<ProdVO> filterProdList = loadProd(hotProdList, 128);

        return new Result().success(filterProdList);
    }

    @UserPermission(ignore = true)
    public Result getBrandMallFloor(AppContext appContext) {

        SearchResponse response = ProdESUtil.searchProdHasBrand(1, 100);
        List<ProdVO> prodList = new ArrayList<>();
        assembleData(response, prodList);

        return new Result().success(prodList);
    }

    @UserPermission(ignore = true)
    public Result getNewMemberMallFloor(AppContext appContext) {

        String mmdd = TimeUtils.date_Md_2String(new Date());
        List<Object[]> list = BASE_DAO.queryNative(NEWMEMBER_ACT_PROD_SQL, new Object[]{mmdd});
        List<ProdVO> prodVOList = new ArrayList<>();
        if(list != null && list.size() > 0){
            List<Long> actCodeList = new ArrayList<>();
            List<Long> skuList = new ArrayList<>();
            Map<Long,Integer> dataMap = new HashMap<>();
            for(Object[] objects : list){
                Long gcode = Long.parseLong(objects[1].toString());
                int limitnum = Integer.parseInt(objects[3].toString());

                skuList.add(gcode);
                dataMap.put(gcode, limitnum);

            }

            SearchResponse response =  ProdESUtil.searchProdBySpuList(skuList, 1, 100);

            if(response != null && response.getHits().totalHits > 0){
                assembleData(response, prodVOList);
            }
            if(prodVOList != null && prodVOList.size() > 0){
                for(ProdVO prodVO : prodVOList){
                    prodVO.setBuynum(0);
                    prodVO.setStartnum(1);
                    prodVO.setActlimit(dataMap.get(prodVO.getSku()));
                }
            }
        }
        return new Result().success(prodVOList);
    }

    @UserPermission(ignore = true)
    public Result getExemPostMallFloor(AppContext appContext) {

        String mmdd = TimeUtils.date_Md_2String(new Date());
        List<Object[]> list = BASE_DAO.queryNative(EXEMPOST_ACT_PROD_SQL, new Object[]{mmdd});
        List<ProdVO> prodVOList = new ArrayList<>();
        if(list != null && list.size() > 0){
            List<Long> skuList = new ArrayList<>();
            Map<Long,Integer> dataMap = new HashMap<>();
            for(Object[] objects : list){
                Long gcode = Long.parseLong(objects[1].toString());
                int limitnum = Integer.parseInt(objects[3].toString());

                skuList.add(gcode);
                dataMap.put(gcode, limitnum);

            }

            SearchResponse response =  ProdESUtil.searchProdBySpuList(skuList, 1, 100);

            if(response != null && response.getHits().totalHits > 0){
                assembleData(response, prodVOList);
            }
            if(prodVOList != null && prodVOList.size() > 0){
                for(ProdVO prodVO : prodVOList){
                    prodVO.setBuynum(0);
                    prodVO.setStartnum(1);
                    prodVO.setActlimit(dataMap.get(prodVO.getSku()));
                }
            }
        }
        return new Result().success(prodVOList);
    }

    @UserPermission(ignore = true)
    public Result getTeamBuyMallFloor(AppContext appContext) {

        String mmdd = TimeUtils.date_Md_2String(new Date());
        List<Object[]> list = BASE_DAO.queryNative(RULE_CODE_ACT_PROD_SQL, new Object[]{1133, mmdd});
        List<ProdVO> prodVOList = new ArrayList<>();
        JSONObject result = new JSONObject();
        if(list != null && list.size() > 0){
            List<Long> actCodeList = new ArrayList<>();
            List<Long> skuList = new ArrayList<>();
            Map<Long,List<String[]>> timeMap = new HashMap<>();
            Map<Long,Integer[]> dataMap = new HashMap<>();
            for(Object[] objects : list){
                Long actcode = Long.parseLong(objects[0].toString());
                Long gcode = Long.parseLong(objects[1].toString());
                int actstock = Integer.parseInt(objects[2].toString());
                int limitnum = Integer.parseInt(objects[3].toString());

                skuList.add(gcode);
                if(!actCodeList.contains(actcode)){
                    List<Object[]> list2= BASE_DAO.queryNative(PROM_TIME_SQL, new Object[]{ actcode });
                    List<String[]> times = new ArrayList<>();
                    for(Object[] objects1 : list2){
                        String sdate =  objects1[0].toString();
                        String edate =  objects1[1].toString();
                        times.add(new String[]{ sdate, edate});
                    }
                    timeMap.put(actcode, times);
                    actCodeList.add(actcode);
                }
                if(actCodeList.size() >1){
                    break;
                }
                dataMap.put(gcode, new Integer[]{limitnum ,actstock});

            }

            List<Object[]> ladoffList =BASE_DAO.queryNative(TEAM_BUY_LADOFF_SQL, new Object[]{});
            int minoff = 100;
            JSONArray ladoffArray = new JSONArray();
            if(ladoffList != null && ladoffList.size() > 0){
                int i = 0;
                for(Object[] objects : ladoffList){
                    int ladamt = Integer.parseInt(objects[0].toString());
                    int ladnum = Integer.parseInt(objects[1].toString());
                    int offer = Integer.parseInt(objects[2].toString()) / 100;
                    if(i == 0){
                        minoff = offer;
                    }
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("ladamt", ladamt);
                    jsonObject.put("ladnum", ladnum);
                    jsonObject.put("offer", offer);
                    ladoffArray.add(jsonObject);
                    i++;
                }
            }

            SearchResponse response =  ProdESUtil.searchProdBySpuList(skuList, 1, 100);

            if(response != null && response.getHits().totalHits > 0){
                assembleData(response, prodVOList);
            }
            if(prodVOList != null && prodVOList.size() > 0){
                for(ProdVO prodVO : prodVOList){
                    prodVO.setBuynum(0);
                    prodVO.setStartnum(0);
                    prodVO.setActlimit(dataMap.get(prodVO.getSku())[0]);
                    prodVO.setSurplusstock(dataMap.get(prodVO.getSku())[1]);
                    prodVO.setActprize(prodVO.getVatp() * minoff / 100);
                }
            }
            GetEffectiveTimeByActCode getEffectiveTimeByActCode = new GetEffectiveTimeByActCode(actCodeList, timeMap).invoke();
            String sdate = getEffectiveTimeByActCode.getSdate();
            String edate = getEffectiveTimeByActCode.getEdate();

            result.put("actcode",actCodeList.get(0));
            result.put("sdate", sdate);
            result.put("edate", edate);
            result.put("list", prodVOList);
            result.put("ladoffArray", ladoffArray);
            result.put("now", TimeUtils.date_yMd_Hms_2String(new Date()));
        }
        return new Result().success(result);
    }

    @UserPermission(ignore = true)
    public Result getSeckillMallFloor(AppContext appContext) {
        Result r = getDiscountMallFloor(appContext);
        return r;

    }

    @UserPermission(ignore = true)
    public Result getDiscountMallFloor(AppContext appContext) {

        String mmdd = TimeUtils.date_Md_2String(new Date());
        List<Object[]> list = BASE_DAO.queryNative(RULE_CODE_ACT_PROD_SQL, new Object[]{1113, mmdd});
        JSONObject result = new JSONObject();
        List<ProdVO> prodVOList = new ArrayList<>();
        if(list != null && list.size() > 0){
            List<Long> actCodeList = new ArrayList<>();
            List<Long> skuList = new ArrayList<>();
            Map<Long,List<String[]>> timeMap = new HashMap<>();
            Map<Long,Integer[]> dataMap = new HashMap<>();

            for(Object[] objects : list){
                Long actcode = Long.parseLong(objects[0].toString());
                Long gcode = Long.parseLong(objects[1].toString());
                int actstock = Integer.parseInt(objects[2].toString());
                int limitnum = Integer.parseInt(objects[3].toString());

                if(!actCodeList.contains(actcode)){
                    actCodeList.add(actcode);
                    List<Object[]> list2 = BASE_DAO.queryNative(PROM_TIME_SQL, new Object[]{actcode});
                    List<String[]> times = new ArrayList<>();
                    for(Object[] objects1 : list2){
                        String sdate =  objects1[0].toString();
                        String edate =  objects1[1].toString();
                        times.add(new String[]{ sdate, edate});
                    }
                    timeMap.put(actcode, times);
                }
                if(actCodeList.size() >1){
                    break;
                }

                skuList.add(gcode);
                dataMap.put(gcode, new Integer[]{limitnum ,actstock});

            }

            SearchResponse response =  ProdESUtil.searchProdBySpuList(skuList, 1, 100);

            if(response != null && response.getHits().totalHits > 0){
                assembleData(response, prodVOList);
            }
            List<ProdPriceEntity> priceEntities = new ArrayList<>();
            if(prodVOList != null && prodVOList.size() > 0){
                for(ProdVO prodVO : prodVOList){
                    prodVO.setBuynum(0);
                    prodVO.setStartnum(0);
                    prodVO.setActlimit(dataMap.get(prodVO.getSku())[0]);
                    prodVO.setSurplusstock(dataMap.get(prodVO.getSku())[1]);
                    ProdPriceEntity priceEntity = new ProdPriceEntity();
                    priceEntity.setSku(prodVO.getSku());
                    priceEntity.setVatp(prodVO.getVatp());
                    priceEntities.add(priceEntity);
                }
            }

            if(priceEntities != null && priceEntities.size() > 0){
                ProdPriceEntity[] entities = IceRemoteUtil.calcMultiProdActPrize(actCodeList.get(0), priceEntities);
                for(ProdVO prodVO : prodVOList){
                    for(ProdPriceEntity calcEntity: entities){
                        if(prodVO.getSku() == calcEntity.getSku()){
                            prodVO.setActprize(calcEntity.getActprice());
                        }
                    }
                }
            }

            GetEffectiveTimeByActCode getEffectiveTimeByActCode = new GetEffectiveTimeByActCode(actCodeList, timeMap).invoke();
            String sdate = getEffectiveTimeByActCode.getSdate();
            String edate = getEffectiveTimeByActCode.getEdate();

            result.put("actcode", actCodeList.get(0));
            result.put("sdate", sdate);
            result.put("edate", edate);
            result.put("list", prodVOList);
            result.put("now", TimeUtils.date_yMd_Hms_2String(new Date()));
        }
        return new Result().success(result);

    }

    @UserPermission(ignore = true)
    public Result getAllDiscount(AppContext appContext) {
        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        Long actcode = json.has("actcode") ? json.get("actcode").getAsLong() : 0;
        String keyword = json.has("keyword") ? json.get("keyword").getAsString(): "";
        if(actcode == 0){
            return new Result().fail("参数错误! 没有活动id");
        }
        JSONObject result = new JSONObject();
        List<ProdVO> prodVOList = new ArrayList<>();
        List<Long> skuList = new ArrayList<>();

        Map<Long,Integer[]> dataMap = new HashMap<>();

        List<Object[]> list = BASE_DAO.queryNative(ACT_PROD_BY_ACTCODE_SQL, new Object[]{actcode});

        if(list != null && list.size() > 0){
            for(Object[] objects : list) {
                Long gcode = Long.parseLong(objects[1].toString());
                int actstock = Integer.parseInt(objects[2].toString());
                int limitnum = Integer.parseInt(objects[3].toString());
                dataMap.put(gcode, new Integer[]{limitnum, actstock});
                skuList.add(gcode);
            }
        }
        List<Object[]> list2 = BASE_DAO.queryNative(PROM_TIME_SQL, new Object[]{actcode});
        Map<Long,List<String[]>> timeMap = new HashMap<>();
        List<String[]> times = new ArrayList<>();
        JSONArray array = new JSONArray();
        for(Object[] objects1 : list2){
            String sdate =  objects1[0].toString();
            String edate =  objects1[1].toString();
            times.add(new String[]{ sdate, edate});
            JSONObject time = new JSONObject();
            time.put("sdate", sdate);
            time.put("edate", edate);
            array.add(time);
        }
        if(times.size() <= 0){
            JSONObject time = new JSONObject();
            time.put("sdate", "00:00:00");
            time.put("edate", "23:59:59");
            array.add(time);
        }
        timeMap.put(actcode, times);
        List<Long> actCodeList = new ArrayList<>();
        actCodeList.add(actcode);
        GetEffectiveTimeByActCode getEffectiveTimeByActCode = new GetEffectiveTimeByActCode(actCodeList, timeMap).invoke();
        String sdate = getEffectiveTimeByActCode.getSdate();
        String edate = getEffectiveTimeByActCode.getEdate();

        if(skuList.size() > 0){
            SearchResponse response =  ProdESUtil.searchProdBySpuListAndKeyword(skuList, keyword);

            if(response != null && response.getHits().totalHits > 0){
                assembleData(response, prodVOList);
            }
            List<ProdPriceEntity> priceEntities = new ArrayList<>();
            if(prodVOList != null && prodVOList.size() > 0){
                for(ProdVO prodVO : prodVOList){
                    prodVO.setBuynum(0);
                    prodVO.setStartnum(0);
                    prodVO.setActlimit(dataMap.get(prodVO.getSku())[0]);
                    prodVO.setSurplusstock(dataMap.get(prodVO.getSku())[1]);
                    ProdPriceEntity priceEntity = new ProdPriceEntity();
                    priceEntity.setSku(prodVO.getSku());
                    priceEntity.setVatp(prodVO.getVatp());
                    priceEntities.add(priceEntity);
                }
            }

            if(priceEntities != null && priceEntities.size() > 0){
                ProdPriceEntity[] entities = IceRemoteUtil.calcMultiProdActPrize(actCodeList.get(0), priceEntities);
                for(ProdVO prodVO : prodVOList){
                    for(ProdPriceEntity calcEntity: entities){
                        if(prodVO.getSku() == calcEntity.getSku()){
                            prodVO.setActprize(calcEntity.getActprice());
                        }
                    }
                }
            }
        }

        result.put("timeArray", array);
        result.put("sdate", sdate);
        result.put("edate", edate);
        result.put("list", prodVOList);
        result.put("now", TimeUtils.date_yMd_Hms_2String(new Date()));

        return new Result().success(result);
    }

    @UserPermission(ignore = true)
    public Result getAllTeamBuy(AppContext appContext) {
        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        Long actcode = json.has("actcode") ? json.get("actcode").getAsLong() : 0;
        String keyword = json.has("keyword") ? json.get("keyword").getAsString(): "";
        if(actcode == 0){
            return new Result().fail("参数错误! 没有活动id");
        }
        JSONObject result = new JSONObject();
        List<ProdVO> prodVOList = new ArrayList<>();
        List<Long> skuList = new ArrayList<>();

        Map<Long,Integer[]> dataMap = new HashMap<>();

        List<Object[]> list = BASE_DAO.queryNative(ACT_PROD_BY_ACTCODE_SQL, new Object[]{actcode});

        if(list != null && list.size() > 0){
            for(Object[] objects : list) {
                Long gcode = Long.parseLong(objects[1].toString());
                int limitnum = Integer.parseInt(objects[3].toString());
                int actstock = Integer.parseInt(objects[2].toString());
                dataMap.put(gcode, new Integer[]{limitnum, actstock});
                skuList.add(gcode);
            }
        }
        List<Object[]> list2 = BASE_DAO.queryNative(PROM_TIME_SQL, new Object[]{actcode});
        Map<Long,List<String[]>> timeMap = new HashMap<>();
        List<String[]> times = new ArrayList<>();
        JSONArray array = new JSONArray();
        for(Object[] objects1 : list2){
            String sdate =  objects1[0].toString();
            String edate =  objects1[1].toString();
            times.add(new String[]{ sdate, edate});
            JSONObject time = new JSONObject();
            time.put("sdate", sdate);
            time.put("edate", edate);
            array.add(time);
        }
        if(times.size() <= 0){
            JSONObject time = new JSONObject();
            time.put("sdate", "00:00:00");
            time.put("edate", "23:59:59");
            array.add(time);
        }
        timeMap.put(actcode, times);

        List<Object[]> ladoffList =BASE_DAO.queryNative(TEAM_BUY_LADOFF_SQL, new Object[]{});
        int minoff = 100;
        JSONArray ladoffArray = new JSONArray();
        if(ladoffList != null && ladoffList.size() > 0){
            int i = 0;
            for(Object[] objects : ladoffList){
                int ladamt = Integer.parseInt(objects[0].toString());
                int ladnum = Integer.parseInt(objects[1].toString());
                int offer = Integer.parseInt(objects[2].toString()) / 100;
                if(i == 0){
                    minoff = offer;
                }
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("ladamt", ladamt);
                jsonObject.put("ladnum", ladnum);
                jsonObject.put("offer", offer);
                ladoffArray.add(jsonObject);
                i++;
            }
        }

        List<Long> actCodeList = new ArrayList<>();
        actCodeList.add(actcode);
        GetEffectiveTimeByActCode getEffectiveTimeByActCode = new GetEffectiveTimeByActCode(actCodeList, timeMap).invoke();
        String sdate = getEffectiveTimeByActCode.getSdate();
        String edate = getEffectiveTimeByActCode.getEdate();

        if(skuList.size() > 0){
            SearchResponse response =  ProdESUtil.searchProdBySpuListAndKeyword(skuList, keyword);

            if(response != null && response.getHits().totalHits > 0){
                assembleData(response, prodVOList);
            }
            if(prodVOList != null && prodVOList.size() > 0){
                for(ProdVO prodVO : prodVOList){
                    prodVO.setBuynum(0);
                    prodVO.setStartnum(1);
                    prodVO.setActlimit(dataMap.get(prodVO.getSku())[0]);
                    prodVO.setSurplusstock(dataMap.get(prodVO.getSku())[1]);
                    prodVO.setActprize(prodVO.getVatp() * minoff / 100);
                }
            }
        }

        result.put("timeArray", array);
        result.put("sdate", sdate);
        result.put("edate", edate);
        result.put("list", prodVOList);
        result.put("now", TimeUtils.date_yMd_Hms_2String(new Date()));

//        result.put("")
        return new Result().success(result);
    }

    @UserPermission(ignore = true)
    public Result getProdDetailHotArea(AppContext appContext) {
        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        int spu = json.get("spu").getAsInt();
        List<Integer> bb1 = new ArrayList(){{
            add(256); add(512);
        }};
        Set<Integer> result1 = new HashSet<>();
        NumUtil.arrangeAdd(128, bb1, result1);

        SearchResponse searchResponse = ProdESUtil.searchProdWithHotAndSpu(result1, spu,1, 10);
        List<ProdVO> prodVOList = new ArrayList<>();
        if(searchResponse == null || searchResponse.getHits().totalHits <5){
            if(searchResponse != null && searchResponse.getHits().totalHits > 0){
                assembleData(searchResponse, prodVOList);
            }
            searchResponse = ProdESUtil.searchProdWithHotAndSpu(result1, spu,1, 10);
            if(searchResponse != null && searchResponse.getHits().totalHits > 0){
                assembleData(searchResponse, prodVOList);
            }
            if(prodVOList.size() < 5){
                searchResponse = ProdESUtil.searchProdWithHotAndSpu(null, spu, 1, 10);
                assembleData(searchResponse, prodVOList);
            }
        }else{
            assembleData(searchResponse, prodVOList);
        }
        return new Result().success(prodVOList);
    }

    @UserPermission(ignore = true)
    public Result getConditionByFullTextSearch(AppContext appContext) {
        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        String keyword = json.has("keyword") ? json.get("keyword").getAsString() : "";
        int spu = json.has("spu") ? json.get("spu").getAsInt() : 0;
        if(StringUtils.isEmpty(keyword) && spu <=0){
            return new Result().success(null);
        }
        List<String> specList = ProdESUtil.getConditions(keyword, spu, "spec");
        List<String> manunameList = ProdESUtil.getConditions(keyword, spu, "manuname");
        List<String> brandnameList = ProdESUtil.getConditions(keyword, spu, "brandname");
        Map<String, List<String>> map = new HashMap<>();
        map.put("specList", specList);
        map.put("manunameList", manunameList);
        map.put("brandnameList", brandnameList);
        return new Result().success(map);
    }

    @UserPermission(ignore = true)
    public Result fullTextsearchProdMall(AppContext appContext) {
        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        String keyword = json.has("keyword") ? json.get("keyword").getAsString() : "";
        long spu = json.has("spu") ? json.get("spu").getAsLong() : 0;
        JsonArray specArray = json.has("specArray") ? json.get("specArray").getAsJsonArray() : null;
        JsonArray manuArray = json.has("manuArray") ? json.get("manuArray").getAsJsonArray() : null;
        JsonArray brandArray = json.has("brandArray") ? json.get("brandArray").getAsJsonArray() : null;
        int sort = json.has("sort") ? json.get("sort").getAsInt() : 0;
        List<String> specList = new ArrayList<>();
        if (specArray != null && specArray.size() > 0) {
            Iterator<JsonElement> it = specArray.iterator();
            while (it.hasNext()) {
                JsonElement elem = it.next();
                specList.add(elem.getAsJsonObject().get("val").getAsString());
            }
        }

        List<String> manunameList = new ArrayList<>();
        if (manuArray != null && manuArray.size() > 0) {
            Iterator<JsonElement> it = manuArray.iterator();
            while (it.hasNext()) {
                JsonElement elem = it.next();
                manunameList.add(elem.getAsJsonObject().get("val").getAsString());
            }
        }

        List<String> brandnameList = new ArrayList<>();
        if (brandArray != null && brandArray.size() > 0) {
            Iterator<JsonElement> it = brandArray.iterator();
            while (it.hasNext()) {
                JsonElement elem = it.next();
                brandnameList.add(elem.getAsJsonObject().get("val").getAsString());
            }
        }

        Result r = new Result();
        SearchResponse response = ProdESUtil.searchProdMall(keyword, spu, specList, manunameList,brandnameList, sort,appContext.param.pageIndex, appContext.param.pageNumber);
        //List<Map<String, Object>> resultList = new ArrayList<>();
        List<ProdVO> prodList = new ArrayList<>();
        if (response != null) {
            for (SearchHit searchHit : response.getHits()) {
                Map<String, Object> sourceMap = searchHit.getSourceAsMap();
                ProdVO prodVO = new ProdVO();
                HashMap detail = (HashMap) sourceMap.get("detail");
                BeanMapUtils.mapToBean(detail, prodVO);
                prodVO.setMutiact(false);
                prodVO.setActprod(false);
                prodList.add(prodVO);
                int rulestatus = Integer.parseInt(sourceMap.get(ESConstant.PROD_COLUMN_RULESTATUS).toString());
                if(rulestatus > 0){
                    prodVO.setActprod(true);
                    List<Integer> bits = NumUtil.getNonZeroBits(rulestatus);
                    if(bits.size() > 1){
                        prodVO.setMutiact(true);
                    }
                    ProdPriceEntity prizeEntity = ProdActPriceUtil.getActIntervalPrizeBySku(prodVO.getSku(), prodVO.getVatp());
                    if(prizeEntity != null){
                        prodVO.setMinprize(prizeEntity.getMinactprize());
                        prodVO.setMaxprize(prizeEntity.getMaxactprize());
                        prodVO.setActcode(prizeEntity.getActcode());
                    }
                }
                try{
                    DictStore.translate(prodVO);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }

        }

        Page page = new Page();
        page.pageSize = appContext.param.pageNumber;
        page.pageIndex = appContext.param.pageIndex;
        page.totalItems = response != null && response.getHits() != null ? (int) response.getHits().totalHits : 0;
        PageHolder pageHolder = new PageHolder(page);
        pageHolder.value = page;
        return r.setQuery(prodList, pageHolder);
    }

    @UserPermission(ignore = true)
    public Result fullTextsearchProd(AppContext appContext) {
        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        String keyword = json.has("keyword") ? json.get("keyword").getAsString() : "";
        JsonArray specArray = json.get("specArray").getAsJsonArray();
        JsonArray manuArray = json.get("manuArray").getAsJsonArray();
        List<String> specList = new ArrayList<>();
        if (specArray != null && specArray.size() > 0) {
            Iterator<JsonElement> it = specArray.iterator();
            while (it.hasNext()) {
                JsonElement elem = it.next();
                specList.add(elem.getAsJsonObject().get("val").getAsString());
            }
        }

        List<Long> manunoList = new ArrayList<>();
        if (manuArray != null && manuArray.size() > 0) {
            Iterator<JsonElement> it = manuArray.iterator();
            while (it.hasNext()) {
                JsonElement elem = it.next();
                manunoList.add(elem.getAsJsonObject().get("val").getAsLong());
            }
        }

        Result r = new Result();
        SearchResponse response = ProdESUtil.searchProd(keyword, specList, manunoList, appContext.param.pageIndex, appContext.param.pageNumber);
        List<Map<String, Object>> resultList = new ArrayList<>();
        if (response != null) {
            for (SearchHit searchHit : response.getHits()) {
                Map<String, Object> sourceMap = searchHit.getSourceAsMap();
                resultList.add(sourceMap);
            }

        }
        // r.success(resultList);
        Page page = new Page();
        page.pageSize = appContext.param.pageNumber;
        page.pageIndex = appContext.param.pageIndex;
        page.totalItems = response != null && response.getHits() != null ? (int) response.getHits().totalHits : 0;
        PageHolder pageHolder = new PageHolder(page);
        pageHolder.value = page;
        return r.setQuery(resultList, pageHolder);
    }

    private List<ProdVO> loadProd(List<ProdVO> prodList, int type) {
        List<ProdVO> newProdList = new ArrayList<>();
        for (ProdVO prodVO : prodList) {
            if ((prodVO.getCstatus() & type) > 0) {
                newProdList.add(prodVO);
            }
        }
        if (newProdList.size() <= 20) {
            int i = newProdList.size();
            for (ProdVO prodVO : prodList) {
                if(i++<=20){
                    newProdList.add(prodVO);
                }
            }
        }
        return newProdList;
    }

    private List<ProdVO> getFilterProds(Set<Integer> result, int sort) {
        SearchResponse response = ProdESUtil.searchProdWithStatusList(result, sort,1, 100);
        List<ProdVO> prodList = new ArrayList<>();
        if (response == null || response.getHits().totalHits <= 10) {
            SearchHits hits = response.getHits();
            if(hits.totalHits > 0){
                assembleData(response, prodList);
            }
            response = ProdESUtil.searchProdWithStatusList(null, sort,1, 100);
        }

        assembleData(response, prodList);
        return prodList;
    }

    private void assembleData(SearchResponse response, List<ProdVO> prodList) {
        if(response == null || response.getHits().totalHits <= 0){
            return;
        }
        for (SearchHit searchHit : response.getHits()) {
            ProdVO prodVO = new ProdVO();
            Map<String, Object> sourceMap = searchHit.getSourceAsMap();

            HashMap detail = (HashMap) sourceMap.get("detail");
            BeanMapUtils.mapToBean(detail, prodVO);

            prodList.add(prodVO);
            prodVO.setImageUrl(FileServerUtils.goodsFilePath(prodVO.getSpu(), prodVO.getSku()));
            try{
                DictStore.translate(prodVO);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    private class GetEffectiveTimeByActCode {
        private List<Long> actCodeList;
        private Map<Long, List<String[]>> timeMap;
        private String sdate;
        private String edate;

        public GetEffectiveTimeByActCode(List<Long> actCodeList, Map<Long, List<String[]>> timeMap) {
            this.actCodeList = actCodeList;
            this.timeMap = timeMap;
        }

        public String getSdate() {
            return sdate;
        }

        public String getEdate() {
            return edate;
        }

        public GetEffectiveTimeByActCode invoke() {
            List<String[]> times = timeMap.get(actCodeList.get(0));
            sdate = "00:00:00";
            edate = "23:59:59";
            if(times != null && times.size() > 0){
               for(String[] time: times){
                   if(TimeUtils.isEffectiveTime(time[0], time[1])){
                       sdate = time[0];
                       edate = time[1];
                   }
               }
            }
            return this;
        }
    }
}
