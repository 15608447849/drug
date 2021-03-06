package com.onek.goods.timer;

import com.alibaba.fastjson.JSONObject;
import com.onek.consts.ESConstant;
import com.onek.propagation.prod.ActivityManageServer;
import com.onek.propagation.prod.ProdDiscountObserver;
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
 * 功能: 去除商品新品标记
 * 详情说明: 定时器触发
 * 作者: 蒋文广
 */
public class RemoveNewFlagTask extends TimerTask {

    private static final String SELECT_SQL = "select sku from {{?"+ DSMConst.TD_PROD_SKU+"}} where ondate = ?";
    private static final String UPDATE_SQL = "update {{?"+ DSMConst.TD_PROD_SKU+"}} set cstatus = cstatus&~256 where ondate = ?";

    @Override
    public void run() {
        Date date = TimeUtils.addDay(new Date(), -7);
        String y = TimeUtils.date_yMd_2String(date);
        List<Object[]> results = BaseDAO.getBaseDAO().queryNative(SELECT_SQL, y);
        if(results != null && results.size() > 0){
            List<Long> skuList = new ArrayList<>();
            for(Object[] result : results){
                Long sku = (Long) result[0];
                skuList.add(sku);
            }

            int status = batchUpdateEsRemoveNewFlag(skuList);
            if(status > 0){
                BaseDAO.getBaseDAO().updateNative(UPDATE_SQL, y);
            }
        }
    }

    public int batchUpdateEsRemoveNewFlag(List<Long> skuList){
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
                    int skucstatus = Integer.parseInt(sourceMap.get(ESConstant.PROD_COLUMN_SKUCSTATUS).toString());
                    sourceMap.put(ESConstant.PROD_COLUMN_SKUCSTATUS, (skucstatus&~  256));
                    bulkRequest.add(client.prepareUpdate(ESConstant.PROD_INDEX, ESConstant.PROD_TYPE, sku+"").setDoc(sourceMap));

                }
                BulkResponse bulkResponse =  bulkRequest.execute().actionGet();
                if (bulkResponse.hasFailures()) {
                    return -1;
                }

            }


        }catch(Exception e) {
            e.printStackTrace();
        }
        return 1;

    }
}
