package com.onek.propagation.prod;

import com.alibaba.fastjson.JSONObject;
import constant.DSMConst;
import dao.BaseDAO;
import elasticsearch.ElasticSearchClientFactory;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.SearchHit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProdSalesObserver implements ProdObserver {

    private static final String UPDATE_SKU_SALES = "update {{?" + DSMConst.TD_PROD_SKU + "}} set sales = ? where sku = ?";

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
                List<Long> spuList = new ArrayList<>();
                Map<Long, Integer> map = new HashMap<>();
                for (int i = 0; i < filterlist.size(); i++) {
                    spuList.add(filterlist.get(i).getLongValue("sku"));
                    map.put(filterlist.get(i).getLongValue("sku"), filterlist.get(i).getInteger("sales"));
                }

                int status = batchUpdateEsSales(spuList, map);

                if (status > 0) {
                    String[] sqls = new String[map.size()];
                    List<Object[]> paramsList = new ArrayList<>();
                    int i = 0;
                    for (Long sku : map.keySet()) {
                        map.get(sku);
                        sqls[i] = UPDATE_SKU_SALES;
                        paramsList.add(new Object[]{sku, map.get(sku)});
                        i++;
                    }
                    BaseDAO.getBaseDAO().updateTransNative(sqls, paramsList);

                }
            }


        }
    }

    @SuppressWarnings("unchecked")
    public int batchUpdateEsSales(List<Long> skuList, Map<Long, Integer> salesMap) {
        SearchResponse response = null;
        try {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            if (skuList != null && skuList.size() > 0) {
                Long[] skuArray = new Long[skuList.size()];
                skuArray = skuList.toArray(skuArray);
                TermsQueryBuilder builder = QueryBuilders.termsQuery("sku", skuArray);
                boolQuery.must(builder);
            }

            TransportClient client = ElasticSearchClientFactory.getClientInstance();

            SearchRequestBuilder requestBuilder = client.prepareSearch("prod")
                    .setQuery(boolQuery);


            response = requestBuilder
                    .execute().actionGet();

            BulkRequestBuilder bulkRequest = client.prepareBulk();
            if (response != null && response.getHits().totalHits > 0) {
                for (SearchHit searchHit : response.getHits()) {
                    Map<String, Object> sourceMap = searchHit.getSourceAsMap();
                    long sku = Long.parseLong(sourceMap.get("sku").toString());
                    int sales = Integer.parseInt(sourceMap.get("sales").toString());
                    sourceMap.put("sales", salesMap.get(sku));
                    bulkRequest.add(client.prepareUpdate("prod", "prod_type", sku + "").setDoc(sourceMap));

                }
            }

            BulkResponse bulkResponse = bulkRequest.get();
            if (bulkResponse.hasFailures()) {
                for (BulkItemResponse item : bulkResponse.getItems()) {
                    System.out.println(item.getFailureMessage());
                }
                return -1;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;

    }
}
