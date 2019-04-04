package com.onek.goods.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import com.onek.goods.entities.BgProdVO;
import elasticsearch.ElasticSearchClientFactory;
import elasticsearch.ElasticSearchProvider;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.sort.SortOrder;
import util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProdESUtil {

    /**
     * 添加商品数据到ES中
     * @param prodVO 商品SKU表
     * @return
     */
    public static int addProdDocument(BgProdVO prodVO){

        long sku = prodVO.getSku();
        String keyword = prodVO.getBrandName() + "|" + prodVO.getPopname() + "|" + prodVO.getProdname()+"|"+prodVO.getManuName();
        String spec = prodVO.getSpec();
        long spu = prodVO.getSpu();
        long manuno = prodVO.getManuNo();

        Map<String, Object> data = new HashMap<>();
        data.put("sku", sku);
        data.put("content", keyword);
        data.put("spec", spec);
        data.put("spu", spu);
        data.put("manuno", manuno);
        data.put("prodstatus", prodVO.getProdstatus());
        data.put("detail", JSONObject.toJSON(prodVO));
        IndexResponse response = ElasticSearchProvider.addDocument(data, "prod", "prod_type", sku+"");
        if(response == null || RestStatus.CREATED != response.status()) {
            return -1;
        }
        return 0;
    }

    /**
     * 修改商品数据到ES中
     *
     * @param prodVO 商品SKU对象
     * @return
     */
    public static int updateProdDocument(BgProdVO prodVO){

        long sku = prodVO.getSku();
        String keyword = prodVO.getBrandName() + "|" + prodVO.getPopname() + "|" + prodVO.getProdname()+"|"+prodVO.getManuName();
        String spec = prodVO.getSpec();
        long spu = prodVO.getSpu();
        long manuno = prodVO.getManuNo();

        Map<String, Object> data = new HashMap<>();
        data.put("sku", sku);
        data.put("content", keyword);
        data.put("spec", spec);
        data.put("spu", spu);
        data.put("manuno", manuno);
        data.put("prodstatus", prodVO.getProdstatus());
        data.put("detail", JSONObject.toJSON(prodVO));
        UpdateResponse response = ElasticSearchProvider.updateDocumentById(data, "prod", "prod_type", sku+"");
        if(response == null || RestStatus.OK != response.status()) {
            return -1;
        }
        return 0;
    }

    /**
     * 根据sku码删除商品文档
     *
     * @param sku sku码
     * @return
     */
    public static void deleteProdDocument(long sku){
        GetResponse response = ElasticSearchProvider.getDocumentById("prod", "prod_type", sku+"");
        if (response != null && response.isExists()) {
            ElasticSearchProvider.deleteDocumentById("prod", "prod_type", sku+"");
        }
    }

    /**
     * 根据条件全文检索商品
     *
     * @param keyword
     * @param specList
     * @param manuNoList
     * @param pagenum
     * @param pagesize
     * @return
     */
    public static SearchResponse searchProd(String keyword,List<String> specList,List<Long> manuNoList,int pagenum,int pagesize){
        SearchResponse response = null;
        try {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            if(specList != null && specList.size() > 0){
                String [] specArray = new String[specList.size()];
                specArray = specList.toArray(specArray);
                TermsQueryBuilder builder = QueryBuilders.termsQuery("spec", specArray);
			    boolQuery.must(builder);
            }
            if(manuNoList != null && manuNoList.size() > 0){
                String [] manuNoArray = new String[manuNoList.size()];
                manuNoArray = manuNoList.toArray(manuNoArray);
                TermsQueryBuilder builder = QueryBuilders.termsQuery("manuno", manuNoArray);
                boolQuery.must(builder);
            }
            if(!StringUtils.isEmpty(keyword)){
                MatchQueryBuilder matchQuery = QueryBuilders.matchQuery("content", keyword).analyzer("ik_max_word");
                boolQuery.must(matchQuery);
            }
            TransportClient client = ElasticSearchClientFactory.getClientInstance();
            int from = pagenum * pagesize - pagesize;
            response = client.prepareSearch("prod")
                    .setQuery(boolQuery)
                    .setFrom(from)
                    .setSize(pagesize)
                    .execute().actionGet();

        }catch(Exception e) {
            e.printStackTrace();
        }

        return response;
    }


    /**
     * 根据条件全文检索商品
     *
     * @param status
     * @param pagenum
     * @param pagesize
     * @return
     */
    public static SearchResponse searchProdWithMallFloor(String status,int pagenum,int pagesize){
        SearchResponse response = null;
        try {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            if(!StringUtils.isEmpty(status)){
                RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("cstatus");
                rangeQuery.gt(status);
                boolQuery.must(rangeQuery);
            }
            TransportClient client = ElasticSearchClientFactory.getClientInstance();
            int from = pagenum * pagesize - pagesize;
            response = client.prepareSearch("prod")
                    .setQuery(boolQuery)
                    .setFrom(from)
                    .setSize(pagesize)
                    .execute().actionGet();

        }catch(Exception e) {
            e.printStackTrace();
        }

        return response;
    }
}
