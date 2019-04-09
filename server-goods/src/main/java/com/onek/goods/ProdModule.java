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
import objectref.ObjectRefUtil;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import redis.IRedisCache;
import redis.proxy.CacheProxyInstance;
import util.BeanMapUtils;
import util.StringUtils;

import java.util.*;

import static com.onek.goods.util.ProdESUtil.searchProd;

@SuppressWarnings("unchecked")
public class ProdModule {

    private static IRedisCache mallFloorProxy = (IRedisCache) CacheProxyInstance.createInstance(new MallFloorImpl());

    public static final Long NEW = 1L;
    public static final Long HOT = 2L;
    public static final Long SECKILL = 3L;

    @UserPermission(ignore = true)
    public Result getMallFloorProd(AppContext appContext) {
//    public Result getMallFloorProd() {
        List<MallFloorVO> mallFloorVOList = (List<MallFloorVO>) mallFloorProxy.queryAll();
        JSONObject jsonObject = new JSONObject();
        if (mallFloorVOList != null && mallFloorVOList.size() > 0) {
            List<ProdVO> prodVOList = new ArrayList<>();
            SearchResponse response = ProdESUtil.searchProdWithMallFloor("128", 1, 100);
            if (response == null || response.getHits().totalHits <= 10) {
                response = ProdESUtil.searchProdWithMallFloor("", 1, 100);
            }
            List<ProdVO> prodList = new ArrayList<>();
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


            for (MallFloorVO floorVO : mallFloorVOList) {
                if (floorVO.getUnqid() == NEW) {
                    List<ProdVO> filterProdList = loadProd(prodList, 256);
                    jsonObject.put("new", filterProdList);
                } else if (floorVO.getUnqid() == HOT) {
                    List<ProdVO> filterProdList = loadProd(prodList, 128);
                    jsonObject.put("hot", filterProdList);
                }
            }
        }
        return new Result().success(jsonObject);
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
            if ((prodVO.getCstatus() & 256) > 0) {
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
        System.out.println("230 line:"+newProdList.size());
        return newProdList;
    }

}
