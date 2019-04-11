package com.onek.propagation.prod;

import com.alibaba.fastjson.JSONObject;
import com.onek.consts.ESConstant;
import constant.DSMConst;
import dao.BaseDAO;
import elasticsearch.ElasticSearchClientFactory;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProdDiscountObserver implements ProdObserver {

    private static final String UPDATE_SKU_DEL_DISCOUNT_RULE = "update {{?" + DSMConst.TD_PROD_SKU + "}} set rulestatus = rulestatus&~?";
    private static final String UPDATE_SKU_ADD_DISCOUNT_RULE = "update {{?" + DSMConst.TD_PROD_SKU + "}} set rulestatus = rulestatus|?";

    @Override
    public void update(List<String> list) {
        if (list != null && list.size() > 0) {
            List<JSONObject> delList = new ArrayList<>();
            List<JSONObject> addList = new ArrayList<>();
            for (String obj : list) {
                JSONObject jsonObject = JSONObject.parseObject(obj);
                if (jsonObject == null || !jsonObject.containsKey("discount")) {
                    continue;
                }
                int cstatus = jsonObject.getInteger("cstatus");
                if ((cstatus & 1) > 0) {
                    delList.add(jsonObject);
                } else if ((cstatus & 1) == 0) {
                    addList.add(jsonObject);
                }
            }

            if (delList != null && delList.size() > 0) {
                int rulecode = delList.get(0).getInteger("rulecode");
                StringBuffer sb = new StringBuffer();
                List<Long> spuList = new ArrayList<>();
                for (int i = 0; i < delList.size(); i++) {
                    sb.append(delList.get(i).getLong("gcode"));
                    spuList.add(delList.get(i).getLong("gcode"));
                    if (i != delList.size() - 1) {
                        sb.append(",");
                    }
                }

                int delstatus = batchUpdateEsDelRule(spuList, rulecode);

                if(delstatus > 0){
                    String sql = UPDATE_SKU_DEL_DISCOUNT_RULE + " where sku in (" + sb.toString() + ")";
                    BaseDAO.getBaseDAO().updateNative(sql, new Object[]{ rulecode });

                }
            }

            if (addList != null && addList.size() > 0) {
                int rulecode = addList.get(0).getInteger("rulecode");
                StringBuffer sb = new StringBuffer();
                List<Long> spuList = new ArrayList<>();
                for (int i = 0; i < addList.size(); i++) {
                    sb.append(addList.get(i).getLong("gcode"));
                    spuList.add(addList.get(i).getLong("gcode"));
                    if (i != addList.size() - 1) {
                        sb.append(",");
                    }
                }

                int delstatus = batchUpdateEsAddRule(spuList, rulecode);

                if(delstatus > 0){
                    String sql = UPDATE_SKU_ADD_DISCOUNT_RULE + " where sku in (" + sb.toString() + ")";
                    BaseDAO.getBaseDAO().updateNative(sql, new Object[]{ rulecode });

                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public int batchUpdateEsDelRule(List<Long> delList,int rulecode){
        SearchResponse response = null;
        try {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            if(delList != null && delList.size() > 0){
                Object [] skuArray = new Long[delList.size()];
                skuArray = delList.toArray(skuArray);
                TermsQueryBuilder builder = QueryBuilders.termsQuery(ESConstant.PROD_COLUMN_SKU, skuArray);
                boolQuery.must(builder);
            }

            TransportClient client = ElasticSearchClientFactory.getClientInstance();

            SearchRequestBuilder requestBuilder = client.prepareSearch(ESConstant.PROD_INDEX)
                    .setQuery(boolQuery);


            response = requestBuilder
                    .execute().actionGet();

            BulkRequestBuilder bulkRequest = client.prepareBulk();
            if(response != null && response.getHits().totalHits > 0){
                for (SearchHit searchHit : response.getHits()) {
                    Map<String, Object> sourceMap = searchHit.getSourceAsMap();
                    long sku = Long.parseLong(sourceMap.get(ESConstant.PROD_COLUMN_SKU).toString());
                    int rulestatus = Integer.parseInt(sourceMap.get(ESConstant.PROD_COLUMN_RULESTATUS).toString());
                    sourceMap.put(ESConstant.PROD_COLUMN_RULESTATUS, rulestatus&~  rulecode);
                    bulkRequest.add(client.prepareUpdate(ESConstant.PROD_INDEX, ESConstant.PROD_TYPE, sku+"").setDoc(sourceMap));

                }
            }

            BulkResponse bulkResponse = bulkRequest.get();
            if (bulkResponse.hasFailures()) {
                for(BulkItemResponse item : bulkResponse.getItems()){
                    System.out.println(item.getFailureMessage());
                }
                return -1;
            }

        }catch(Exception e) {
            e.printStackTrace();
        }
        return 1;

    }

    @SuppressWarnings("unchecked")
    public int batchUpdateEsAddRule(List<Long> delList,int rulecode){
        SearchResponse response = null;
        try {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            if(delList != null && delList.size() > 0){
                Object [] skuArray = new Long[delList.size()];
                skuArray = delList.toArray(skuArray);
                TermsQueryBuilder builder = QueryBuilders.termsQuery(ESConstant.PROD_COLUMN_SKU, skuArray);
                boolQuery.must(builder);
            }

            TransportClient client = ElasticSearchClientFactory.getClientInstance();

            SearchRequestBuilder requestBuilder = client.prepareSearch(ESConstant.PROD_INDEX)
                    .setQuery(boolQuery);


            response = requestBuilder
                    .execute().actionGet();

            BulkRequestBuilder bulkRequest = client.prepareBulk();
            if(response != null && response.getHits().totalHits > 0){
                for (SearchHit searchHit : response.getHits()) {
                    Map<String, Object> sourceMap = searchHit.getSourceAsMap();
                    long sku = Long.parseLong(sourceMap.get(ESConstant.PROD_COLUMN_SKU).toString());
                    int rulestatus = Integer.parseInt(sourceMap.get(ESConstant.PROD_COLUMN_RULESTATUS).toString());
                    sourceMap.put(ESConstant.PROD_COLUMN_RULESTATUS, rulestatus|rulecode);
                    bulkRequest.add(client.prepareUpdate(ESConstant.PROD_INDEX, ESConstant.PROD_TYPE, sku+"").setDoc(sourceMap));

                }
            }

            BulkResponse bulkResponse = bulkRequest.get();
            if (bulkResponse.hasFailures()) {
                for(BulkItemResponse item : bulkResponse.getItems()){
                    System.out.println(item.getFailureMessage());
                }
                return -1;
            }

        }catch(Exception e) {
            e.printStackTrace();
        }
        return 1;

    }
}
