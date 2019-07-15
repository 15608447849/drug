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

/**
 *
 * 功能: 高毛利定时任务
 * 详情说明: 抽取30条高毛利商品
 * 作者: 蒋文广
 */
public class HigherProfitsTask extends TimerTask {

    private static final String SELECT_OLD_HIGHPROFIT_SQL = "select sku from {{?"+ DSMConst.TD_PROD_SKU+"}} sku " +
            "where cstatus&512 > 0";


    private static final String SELECT_HIGHPROFIT_SQL = "select sku,rate from ( " +
            "select sku,convert((sku.rrp-sku.vatp)/sku.rrp,decimal(10,2)) as rate from {{?"+ DSMConst.TD_PROD_SKU+"}} sku " +
            ") t where t.rate >= ? order by t.rate desc ";

    private static final String UPDATE_CLEAR_HIGHPROFIT_SQL = "update {{?"+ DSMConst.TD_PROD_SKU+"}} set cstatus = cstatus&~512 where cstatus&512 > 0";
    private static final String UPDATE_ADD_HIGHPROFIT_SQL = "update {{?"+ DSMConst.TD_PROD_SKU+"}} set cstatus = cstatus|512 where 1=1";
    private static final String SELECT_CONFIG_VALUE = "select `value`/100 from {{?" + DSMConst.TB_SYSTEM_CONFIG + "}} where "
            + " cstatus&1=0 and varname=?";

    @Override
    public void run() {
        double value = 0.3D;
        List<Object[]> results = BaseDAO.getBaseDAO().queryNative(SELECT_OLD_HIGHPROFIT_SQL);
        if(results != null && results.size() > 0) {
            List<Long> skuList = new ArrayList<>();
            for (Object[] result : results) {
                Long sku = (Long) result[0];
                skuList.add(sku);
            }

            if(skuList.size() > 0){
                int result = batchDelHighProfitNewFlag(skuList);
                if(result > 0) BaseDAO.getBaseDAO().updateNative(UPDATE_CLEAR_HIGHPROFIT_SQL);
            }
        }
        List<Object[]> values = BaseDAO.getBaseDAO().queryNative(SELECT_CONFIG_VALUE, "HIGH_PROFIT");
        if (values != null && values.size() > 0) {
            value = Double.parseDouble(String.valueOf(values.get(0)[0]));
        }
        results = BaseDAO.getBaseDAO().queryNative(SELECT_HIGHPROFIT_SQL, value);
        if(results != null && results.size() > 0) {
            List<Long> skuList = new ArrayList<>();
            for (Object[] result : results) {
                Long sku = (Long) result[0];
                skuList.add(sku);
            }

            if(skuList.size() > 0){
                int result = batchAddHighProfitNewFlag(skuList);
                if(result > 0) {
                    StringBuilder sql = new StringBuilder(UPDATE_ADD_HIGHPROFIT_SQL +" and sku in(");

                    int index = 0;
                    for (Long sku : skuList) {
                        index++;
                        sql.append(sku);
                        if (index < skuList.size()) {
                            sql.append(",");
                        }
                    }

                    sql.append(") ");

                    BaseDAO.getBaseDAO().updateNative(sql.toString());
                }
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
                TermsQueryBuilder builder = QueryBuilders.termsQuery(ESConstant.PROD_COLUMN_SKU, skuArray);
                boolQuery.must(builder);
            }

            TransportClient client = ElasticSearchClientFactory.getClientInstance();

            SearchRequestBuilder requestBuilder = client.prepareSearch(ESConstant.PROD_INDEX)
                    .setQuery(boolQuery).setSize(skuList.size());


            response = requestBuilder
                    .execute().actionGet();

            BulkRequestBuilder bulkRequest = client.prepareBulk();
            if(response != null && response.getHits().totalHits > 0){
                for (SearchHit searchHit : response.getHits()) {
                    Map<String, Object> sourceMap = searchHit.getSourceAsMap();
                    long sku = Long.parseLong(sourceMap.get(ESConstant.PROD_COLUMN_SKU).toString());
                    int skucstatus = sourceMap.get(ESConstant.PROD_COLUMN_SKUCSTATUS) != null ? Integer.parseInt(sourceMap.get(ESConstant.PROD_COLUMN_SKUCSTATUS).toString()): 0;
                    sourceMap.put(ESConstant.PROD_COLUMN_SKUCSTATUS, (skucstatus&~512));
                    bulkRequest.add(client.prepareUpdate(ESConstant.PROD_INDEX, ESConstant.PROD_TYPE, sku+"").setDoc(sourceMap));

                }
                BulkResponse bulkResponse =  bulkRequest.execute().actionGet();
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
                TermsQueryBuilder builder = QueryBuilders.termsQuery(ESConstant.PROD_COLUMN_SKU, skuArray);
                boolQuery.must(builder);
            }

            TransportClient client = ElasticSearchClientFactory.getClientInstance();

            SearchRequestBuilder requestBuilder = client.prepareSearch(ESConstant.PROD_INDEX)
                    .setQuery(boolQuery).setSize(skuList.size());


            response = requestBuilder
                    .execute().actionGet();

            BulkRequestBuilder bulkRequest = client.prepareBulk();
//            System.out.println("@@@@"+ response.getHits().totalHits);
            if(response != null && response.getHits().totalHits > 0){
                for (SearchHit searchHit : response.getHits()) {
                    Map<String, Object> sourceMap = searchHit.getSourceAsMap();
                    long sku = Long.parseLong(sourceMap.get(ESConstant.PROD_COLUMN_SKU).toString());
                    int skucstatus = sourceMap.get(ESConstant.PROD_COLUMN_SKUCSTATUS) != null ? Integer.parseInt(sourceMap.get(ESConstant.PROD_COLUMN_SKUCSTATUS).toString()): 0;
                    sourceMap.put(ESConstant.PROD_COLUMN_SKUCSTATUS, (skucstatus|512));
                    bulkRequest.add(client.prepareUpdate(ESConstant.PROD_INDEX, ESConstant.PROD_TYPE, sku+"").setDoc(sourceMap));
//                    System.out.println("++++ sku:" + sku + ";" + skucstatus);

                }

                BulkResponse bulkResponse =  bulkRequest.execute().actionGet();
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
