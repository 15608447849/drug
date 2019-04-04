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
import com.onek.util.dict.DictUtil;
import com.onek.util.prod.ProdEntity;
import objectref.ObjectRefUtil;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import redis.IRedisCache;
import redis.proxy.CacheProxyInstance;
import util.BeanMapUtils;

import java.util.*;

import static com.onek.goods.util.ProdESUtil.searchProd;

public class ProdModule {

    private static IRedisCache mallFloorProxy =(IRedisCache) CacheProxyInstance.createInstance(new MallFloorImpl());

    public static final Long NEW = 1L;
    public static final Long HOT = 2L;
    public static final Long SECKILL = 3L;

    @SuppressWarnings("unchecked")
    @UserPermission(ignore = true)
    public Result getMallFloorProd(AppContext appContext){
        List<MallFloorVO> mallFloorVOList = (List<MallFloorVO>) mallFloorProxy.queryAll();
        Result r = new Result();
        JSONObject jsonObject = new JSONObject();
        if(mallFloorVOList != null && mallFloorVOList.size() > 0){
            List<ProdVO> prodVOList = new ArrayList<>();
            SearchResponse response = ProdESUtil.searchProdWithMallFloor("128", 1, 100);
            if(response == null || response.getHits().totalHits <= 0){
                response = ProdESUtil.searchProdWithMallFloor("", 1, 100);
            }
            List<ProdVO> prodList = new ArrayList<>();
            for(SearchHit searchHit : response.getHits()){
                ProdVO prodVO = new ProdVO();
                Map<String, Object> sourceMap = searchHit.getSourceAsMap();

                HashMap detail = (HashMap)sourceMap.get("detail");
                BeanMapUtils.mapToBean(detail, prodVO);

                prodList.add(prodVO);
            }
             for(MallFloorVO floorVO : mallFloorVOList){
                 if(floorVO.getUnqid() == NEW){
                     List<ProdVO> filterProdList = loadProd(prodList, 256);
                     jsonObject.put("new", filterProdList);
                 }else if(floorVO.getUnqid() == HOT){
                     List<ProdVO> filterProdList = loadProd(prodList, 128);
                     jsonObject.put("hot", filterProdList);
                 }
             }
        }
        return new Result().success(jsonObject);
    }

    private List<ProdVO> loadProd(List<ProdVO> prodList,int type) {
        List<ProdVO> newProdList = new ArrayList<>();
        for(ProdVO prodVO : prodList){
            if((prodVO.getCstatus() & 256) > 0){
                newProdList.add(prodVO);
            }
        }
        if(newProdList.size()<=0){
            int i = 10;
            for(ProdVO prodVO : prodList){
                if(i++>10){
                    break;
                }
                newProdList.add(prodVO);
            }
        }
        return newProdList;
    }

    @UserPermission(ignore = true)
    public Result fullTextsearchProd(AppContext appContext) {
        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        String keyword = json.get("keyword").getAsString();
        JsonArray specArray = json.get("specArray").getAsJsonArray();
        JsonArray manuArray = json.get("manuArray").getAsJsonArray();
        List<String> specList =new ArrayList<>();
        if(specArray != null && specArray.size() > 0){
            Iterator<JsonElement> it = specArray.iterator();
            while(it.hasNext()){
                JsonElement elem = it.next();
                specList.add(elem.getAsJsonObject().get("val").getAsString());
            }
        }

//       for(String spec : specList){
//           System.out.println("spec:"+spec);
//       }

        List<Long> manunoList =new ArrayList<>();
        if(manuArray != null && manuArray.size() > 0){
            Iterator<JsonElement> it = manuArray.iterator();
            while(it.hasNext()){
                JsonElement elem = it.next();
                manunoList.add(elem.getAsJsonObject().get("val").getAsLong());
            }
        }

        Result r = new Result();
        SearchResponse response = ProdESUtil.searchProd(keyword, specList, manunoList, appContext.param.pageIndex, appContext.param.pageNumber);
        List<Map<String,Object>> resultList = new ArrayList<>();
        if(response != null){
            for (SearchHit searchHit : response.getHits()) {
                Map<String,Object> sourceMap = searchHit.getSourceAsMap();
                resultList.add(sourceMap);
            }

        }
        // r.success(resultList);
        Page page = new Page();
        page.pageSize = appContext.param.pageNumber;
        page.pageIndex = appContext.param.pageIndex;
        page.totalItems = response!=null && response.getHits() != null ? (int)response.getHits().totalHits : 0;
        PageHolder pageHolder = new PageHolder(page);
        pageHolder.value = page;
        return r.setQuery(resultList, pageHolder);
    }

}
