package com.onek.goods.timer;

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
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.SearchHit;
import util.TimeUtils;

import java.util.*;

public class HigherProfitsTask extends TimerTask {

    private static final String SELECT_OLD_HIGHPROFIT_SQL = "select sku from {{?"+ DSMConst.TD_PROD_SKU+"}} sku " +
            "where cstatus&512 > 0";

    private static final String SELECT_HIGHPROFIT_SQL = "select sku,rate from ( " +
            "select sku,(sku.mp-sku.vatp)/sku.vatp as rate from {{?"+ DSMConst.TD_PROD_SKU+"}} sku " +
            ") t where t.rate > 0 order by t.rate desc limit 100";

    private static final String UPDATE_CLEAR_HIGHPROFIT_SQL = "update {{?"+ DSMConst.TD_PROD_SKU+"}} set cstatus = cstatus&~512 where cstatus&512 > 0";
    private static final String UPDATE_ADD_HIGHPROFIT_SQL = "update {{?"+ DSMConst.TD_PROD_SKU+"}} set cstatus = cstatus|512 where 1=1";

    @Override
    public void run() {
        List<Object[]> results = BaseDAO.getBaseDAO().queryNative(SELECT_OLD_HIGHPROFIT_SQL);
        if(results != null && results.size() > 0) {
            List<Long> skuList = new ArrayList<>();
            for (Object[] result : results) {
                Long sku = (Long) result[0];
                skuList.add(sku);
            }

            if(skuList.size() > 0){
                int result = batchDelHighProfitNewFlag(skuList);
                if(result > 0) BaseDAO.getBaseDAO().updateNative(UPDATE_CLEAR_HIGHPROFIT_SQL,new Object[]{});
            }
        }

        results = BaseDAO.getBaseDAO().queryNative(SELECT_HIGHPROFIT_SQL);
        if(results != null && results.size() > 0) {
            List<Long> skuList = new ArrayList<>();
            for (Object[] result : results) {
                Long sku = (Long) result[0];
                skuList.add(sku);
            }

            if(skuList.size() > 0){
                int result = batchDelHighProfitNewFlag(skuList);
                if(result > 0) BaseDAO.getBaseDAO().updateNative(UPDATE_ADD_HIGHPROFIT_SQL, new Object[]{});
            }
        }
    }

    public int batchDelHighProfitNewFlag(List<Long> skuList){
        SearchResponse response = null;
        try {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            if(skuList != null && skuList.size() > 0){
                Object [] skuArray = new Long[skuList.size()];
                skuArray = skuList.toArray(skuArray);
                TermsQueryBuilder builder = QueryBuilders.termsQuery("sku", skuArray);
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
                    long sku = Long.parseLong(sourceMap.get("sku").toString());
                    int skucstatus = Integer.parseInt(sourceMap.get("skucstatus").toString());
                    sourceMap.put("skucstatus", skucstatus&~512);
                    bulkRequest.add(client.prepareUpdate(ESConstant.PROD_INDEX, ESConstant.PROD_TYPE, sku+"").setDoc(sourceMap));

                }
                BulkResponse bulkResponse = bulkRequest.get();
                if (bulkResponse.hasFailures()) {
                    for(BulkItemResponse item : bulkResponse.getItems()){
                        System.out.println(item.getFailureMessage());
                    }
                    return -1;
                }

            }


        }catch(Exception e) {
            e.printStackTrace();
        }
        return 1;

    }

    public int batchAddHighProfitNewFlag(List<Long> skuList){
        SearchResponse response = null;
        try {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            if(skuList != null && skuList.size() > 0){
                Object [] skuArray = new Long[skuList.size()];
                skuArray = skuList.toArray(skuArray);
                TermsQueryBuilder builder = QueryBuilders.termsQuery("sku", skuArray);
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
                    long sku = Long.parseLong(sourceMap.get("sku").toString());
                    int skucstatus = Integer.parseInt(sourceMap.get("skucstatus").toString());
                    sourceMap.put("skucstatus", skucstatus|512);
                    bulkRequest.add(client.prepareUpdate(ESConstant.PROD_INDEX, ESConstant.PROD_TYPE, sku+"").setDoc(sourceMap));

                }
                BulkResponse bulkResponse = bulkRequest.get();
                if (bulkResponse.hasFailures()) {
                    for(BulkItemResponse item : bulkResponse.getItems()){
                        System.out.println(item.getFailureMessage());
                    }
                    return -1;
                }

            }


        }catch(Exception e) {
            e.printStackTrace();
        }
        return 1;

    }
}
