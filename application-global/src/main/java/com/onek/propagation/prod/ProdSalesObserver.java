package com.onek.propagation.prod;

import com.alibaba.fastjson.JSONObject;
import com.onek.consts.ESConstant;
import constant.DSMConst;
import dao.BaseDAO;
import elasticsearch.ElasticSearchClientFactory;
import elasticsearch.ElasticSearchProvider;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProdSalesObserver implements ProdObserver {

    private static final String UPDATE_SKU_SALES = "update {{?" + DSMConst.TD_PROD_SKU + "}} set sales = sales + ? where sku = ?";

    @Override
    public void update(List<String> list) {
        if (list != null && list.size() > 0) {
            List<JSONObject> filterlist = new ArrayList<>();
            for (String obj : list) {
                JSONObject jsonObject = JSONObject.parseObject(obj);
                if (jsonObject == null || !jsonObject.containsKey("sales")) {
                    continue;
                }
                filterlist.add(jsonObject);
            }

            if (filterlist != null && filterlist.size() > 0) {
                List<Long> skuList = new ArrayList<>();
                Map<Long, Integer> map = new HashMap<>();
                for (int i = 0; i < filterlist.size(); i++) {
                    skuList.add(filterlist.get(i).getLongValue("sku"));
                    map.put(filterlist.get(i).getLongValue("sku"), filterlist.get(i).getInteger("sales"));
                }

                for(Long sku : skuList){

                    GetResponse response = ElasticSearchProvider.getDocumentById(ESConstant.PROD_INDEX, ESConstant.PROD_TYPE, sku +"");
                    boolean updateSuccess = false;
                    if(response != null){
                        Map<String, Object> data = response.getSourceAsMap();
                        int sales = data.get(ESConstant.PROD_COLUMN_SALES) != null ? Integer.parseInt(data.get(ESConstant.PROD_COLUMN_SALES).toString()) : 0;
                        data.put(ESConstant.PROD_COLUMN_SALES, map.get(sku) + sales);
                        HashMap detail = (HashMap) data.get("detail");
                        data.put(ESConstant.PROD_COLUMN_DETAIL, JSONObject.toJSON(detail));
                        data.put(ESConstant.PROD_COLUMN_TIME, data.get(ESConstant.PROD_COLUMN_TIME).toString());
                        UpdateResponse updateResponse = ElasticSearchProvider.updateDocumentById(data, ESConstant.PROD_INDEX, ESConstant.PROD_TYPE, sku+"");
                        if(response != null && RestStatus.OK == updateResponse.status()) {
                            updateSuccess = true;
                        }
                    }
                    if(updateSuccess){
                        BaseDAO.getBaseDAO().updateNative(UPDATE_SKU_SALES, new Object[]{ map.get(sku), sku});
                    }
                }

            }


        }
    }

}
