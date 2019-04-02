package com.onek.goods;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.goods.util.ProdESUtil;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.onek.goods.util.ProdESUtil.searchProd;

public class ProdModule {

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
