package com.onek.goods;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.goods.entities.MallFloorVO;
import com.onek.goods.entities.ProdVO;
import com.onek.goods.service.MallFloorImpl;
import com.onek.goods.util.ProdESUtil;
import com.onek.util.dict.DictStore;
import com.onek.util.dict.DictUtil;
import com.onek.util.fs.FileServerUtils;
import com.onek.util.prod.ProdEntity;
import constant.DSMConst;
import dao.BaseDAO;
import objectref.ObjectRefUtil;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import redis.IRedisCache;
import redis.proxy.CacheProxyInstance;
import util.BeanMapUtils;
import util.NumUtil;
import util.StringUtils;

import java.util.*;

import static com.onek.goods.util.ProdESUtil.searchProd;

@SuppressWarnings("unchecked")
public class ProdModule {

    private static IRedisCache mallFloorProxy = (IRedisCache) CacheProxyInstance.createInstance(new MallFloorImpl());

    private static final BaseDAO BASE_DAO = BaseDAO.getBaseDAO();

    private static String SQL = "select a.unqid,d.gcode,d.actstock,d.limitnum from " +
            "{{?"+ DSMConst.TD_PROM_ACT +"}} a, {{?"+DSMConst.TD_PROM_ASSDRUG+"}} d " +
            "where a.unqid = d.actcode " +
            "and a.brulecode = ? " +
            "and a.sdate <= CURDATE() and CURDATE()<= a.edate";

    private static String SQL2 = "select sdate,edate from {{?"+ DSMConst.TD_PROM_TIME +"}} where cstatus&1=0 and actcode = ?";

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
        List<Integer> bb = new ArrayList<>();
        bb.add(128);
        bb.add(512);
        Set<Integer> result = new HashSet<>();
        NumUtil.arrangeAdd(256, bb, result);

        List<ProdVO> newprodList = getFilterProds(result);

        List<ProdVO> filterProdList = loadProd(newprodList, 256);

        return new Result().success(filterProdList);
    }

    @UserPermission(ignore = true)
    public Result getChooseForYouMallFloor(AppContext appContext) {
        List<Integer> bb1 = new ArrayList<>();
        bb1.add(218);
        bb1.add(256);
        Set<Integer> result1 = new HashSet<>();
        NumUtil.arrangeAdd(512, bb1, result1);

        List<ProdVO> hotprodList = getFilterProds(result1);
        List<ProdVO> filterProdList = loadProd(hotprodList, 128);

        return new Result().success(filterProdList);
    }

    @UserPermission(ignore = true)
    public Result getHotMallFloor(AppContext appContext) {
        List<Integer> bb1 = new ArrayList<>();
        bb1.add(256);
        bb1.add(512);
        Set<Integer> result1 = new HashSet<>();
        NumUtil.arrangeAdd(128, bb1, result1);

        List<ProdVO> hotprodList = getFilterProds(result1);
        List<ProdVO> filterProdList = loadProd(hotprodList, 128);

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
    public Result getTeamBuyMallFloor(AppContext appContext) {

        List<Object[]> list = BASE_DAO.queryNative(SQL, new Object[]{8});
        List<ProdVO> prodVOList = new ArrayList<>();
        if(list != null && list.size() > 0){
            List<Long> actCodeList = new ArrayList<>();
            List<Long> skuList = new ArrayList<>();
            Map<Long,String[]> timeMap = new HashMap<>();
            Map<Long,Integer> dataMap = new HashMap<>();
            for(Object[] objects : list){
                Long actcode = Long.parseLong(objects[0].toString());
                Long gcode = Long.parseLong(objects[1].toString());
                int limitnum = Integer.parseInt(objects[3].toString());

                skuList.add(gcode);
                if(!actCodeList.contains(actcode)){
                    List<Object[]> list2= BASE_DAO.queryNative(SQL2, new Object[]{ actcode });
                    for(Object[] objects1 : list2){
                        String sdate =  objects1[0].toString();
                        String edate =  objects1[1].toString();
                        timeMap.put(actcode, new String[]{sdate, edate});
                    }
                    actCodeList.add(actcode);
                }
                dataMap.put(gcode, limitnum);

            }

            SearchResponse response =  ProdESUtil.searchProdBySpuList(skuList, 1, 100);

            if(response != null && response.getHits().totalHits > 0){
                assembleData(response, prodVOList);
            }
            if(prodVOList != null && prodVOList.size() > 0){
                for(ProdVO prodVO : prodVOList){
                    prodVO.setBuynum(0);
                    prodVO.setStartnum(0);
                    prodVO.setActlimit(dataMap.get(prodVO.getSku()));
                    if(timeMap.get(prodVO.getSku())==null || timeMap.get(prodVO.getSku()).length<=0){
                        prodVO.setSdate("00:00:00");
                        prodVO.setEdate("23:59:59");
                    }else{
                        prodVO.setSdate(timeMap.get(prodVO.getSku())[0]);
                        prodVO.setEdate(timeMap.get(prodVO.getSku())[1]);
                    }

                }
            }
        }
        return new Result().success(prodVOList);
    }

    @UserPermission(ignore = true)
    public Result getSeckillMallFloor(AppContext appContext) {

        List<Object[]> list = BASE_DAO.queryNative(SQL, new Object[]{8});
        JSONObject result = new JSONObject();
        List<ProdVO> prodVOList = new ArrayList<>();
        if(list != null && list.size() > 0){
            List<Long> actCodeList = new ArrayList<>();
            List<Long> skuList = new ArrayList<>();
            Map<Long,String[]> timeMap = new HashMap<>();
            Map<Long,Integer> dataMap = new HashMap<>();

            for(Object[] objects : list){
                Long actcode = Long.parseLong(objects[0].toString());
                Long gcode = Long.parseLong(objects[1].toString());
                int limitnum = Integer.parseInt(objects[3].toString());

                if(!actCodeList.contains(actcode)){
                    actCodeList.add(actcode);
                    List<Object[]> list2 = BASE_DAO.queryNative(SQL2, new Object[]{actcode});
                    for (Object[] objects1 : list2) {
                        String sdate = objects1[0].toString();
                        String edate = objects1[1].toString();
                        timeMap.put(actcode, new String[]{sdate, edate});
                    }
                }
                if(actCodeList.size() >1){
                    break;
                }

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
                    prodVO.setStartnum(0);
                    prodVO.setActlimit(dataMap.get(prodVO.getSku()));
                }
            }
            result.put("sdate", timeMap.get(actCodeList.get(0))[0]);
            result.put("edate", timeMap.get(actCodeList.get(0))[1]);
            result.put("list", prodVOList);
        }
        return new Result().success(result);
    }

    @UserPermission(ignore = true)
    public Result getDiscountMallFloor(AppContext appContext) {

        List<Object[]> list = BASE_DAO.queryNative(SQL, new Object[]{8});
        JSONObject result = new JSONObject();
        List<ProdVO> prodVOList = new ArrayList<>();
        if(list != null && list.size() > 0){
            List<Long> actCodeList = new ArrayList<>();
            List<Long> skuList = new ArrayList<>();
            Map<Long,String[]> timeMap = new HashMap<>();
            Map<Long,Integer> dataMap = new HashMap<>();

            for(Object[] objects : list){
                Long actcode = Long.parseLong(objects[0].toString());
                Long gcode = Long.parseLong(objects[1].toString());
                int limitnum = Integer.parseInt(objects[3].toString());

                if(!actCodeList.contains(actcode)){
                    actCodeList.add(actcode);
                    List<Object[]> list2 = BASE_DAO.queryNative(SQL2, new Object[]{actcode});
                    for (Object[] objects1 : list2) {
                        String sdate = objects1[0].toString();
                        String edate = objects1[1].toString();
                        timeMap.put(actcode, new String[]{sdate, edate});
                    }
                }
                if(actCodeList.size() >1){
                    break;
                }

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
                    prodVO.setStartnum(0);
                    prodVO.setActlimit(dataMap.get(prodVO.getSku()));
                }
            }
            result.put("sdate", timeMap.get(actCodeList.get(0))[0]);
            result.put("edate", timeMap.get(actCodeList.get(0))[1]);
            result.put("list", prodVOList);
        }
        return new Result().success(result);
    }

    @UserPermission(ignore = true)
    public Result getProdDetailHotArea(AppContext appContext) {
        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        int spu = json.get("spu").getAsInt();
        List<Integer> bb1 = new ArrayList<>();
        bb1.add(256);
        bb1.add(512);
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

                prodList.add(prodVO);
                try{
                    DictStore.translate(prodVO);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }

        }
        // r.success(resultList);
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

//       for(String spec : specList){
//           System.out.println("spec:"+spec);
//       }

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

    private List<ProdVO> getFilterProds(Set<Integer> result) {
        SearchResponse response = ProdESUtil.searchProdWithMallFloor(result, 1, 100);
        List<ProdVO> prodList = new ArrayList<>();
        if (response == null || response.getHits().totalHits <= 10) {
            SearchHits hits = response.getHits();
            if(hits.totalHits > 0){
                assembleData(response, prodList);
            }
            response = ProdESUtil.searchProdWithMallFloor(null, 1, 100);
        }

        assembleData(response, prodList);
        return prodList;
    }

    private void assembleData(SearchResponse response, List<ProdVO> prodList) {
        if(response == null || response.getHits().totalHits<=0){
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

}
