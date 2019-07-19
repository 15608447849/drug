package com.onek.goods.util;

import com.alibaba.fastjson.JSONObject;
import com.onek.consts.ESConstant;
import com.onek.goods.entities.BgProdVO;
import elasticsearch.ElasticSearchClientFactory;
import elasticsearch.ElasticSearchProvider;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import util.StringUtils;
import util.TimeUtils;

import java.util.*;

import static org.elasticsearch.index.query.QueryBuilders.matchQuery;

public class ProdESUtil {

    public final static String DEFAULT_DATE = "1990-01-01";
    public final static String DEFAULT_TIME = "00:00:00";

    /**
     * 添加商品数据到ES中
     * @param paramVo 商品SKU表
     * @return
     */
    public static int addProdDocument(BgProdVO paramVo){

        try {
            BgProdVO prodVO = (BgProdVO) paramVo.clone();
            prodVO.setDetail("");
            if(StringUtils.isEmpty(prodVO.getOffdate())){
                prodVO.setOffdate(DEFAULT_DATE);
                prodVO.setOfftime(DEFAULT_TIME);
            }
            if(StringUtils.isEmpty(prodVO.getVaildsdate())){
                prodVO.setVaildsdate(DEFAULT_DATE);
            }
            if(StringUtils.isEmpty(prodVO.getVaildedate())){
                prodVO.setVaildedate(DEFAULT_DATE);
            }
            if(StringUtils.isEmpty(prodVO.getProdsdate())){
                prodVO.setProdsdate(DEFAULT_DATE);
            }
            if(StringUtils.isEmpty(prodVO.getProdedate())){
                prodVO.setProdedate(DEFAULT_DATE);
            }
            long sku = prodVO.getSku();
            String keyword = StringUtils.checkObjectNull(prodVO.getBrandName(), "").trim()
                    + "|" + StringUtils.checkObjectNull(prodVO.getPopname(), "").trim()
                    + "|" + StringUtils.checkObjectNull(prodVO.getProdname(), "").trim()
                    + "|" + StringUtils.checkObjectNull(prodVO.getManuName(), "").trim();
            String spec = prodVO.getSpec();
            long spu = prodVO.getSpu();
            long manuno = prodVO.getManuNo();
            String manuname = StringUtils.checkObjectNull(prodVO.getManuName(),"").trim();
            long brandno = prodVO.getBrandNo();
            String brandname = StringUtils.checkObjectNull(prodVO.getBrandName(),"").trim();

            Map<String, Object> data = new HashMap<>();
            data.put(ESConstant.PROD_COLUMN_SKU, sku);
            data.put(ESConstant.PROD_COLUMN_CONTENT, keyword);
            data.put(ESConstant.PROD_COLUMN_SPEC, spec);
            data.put(ESConstant.PROD_COLUMN_SPU, spu);
            data.put(ESConstant.PROD_COLUMN_MANUNO, manuno);
            data.put(ESConstant.PROD_COLUMN_MANUNAME, manuname);
            data.put(ESConstant.PROD_COLUMN_BRANDNO, brandno);
            data.put(ESConstant.PROD_COLUMN_BRANDNAME, brandname);
            data.put(ESConstant.PROD_COLUMN_PRODSTATUS, prodVO.getProdstatus());
            data.put(ESConstant.PROD_COLUMN_SKUCSTATUS, prodVO.getSkuCstatus());
            data.put(ESConstant.PROD_COLUMN_VATP, prodVO.getVatp());
            data.put(ESConstant.PROD_COLUMN_SALES, prodVO.getSales());
            data.put(ESConstant.PROD_COLUMN_RULESTATUS, 0);
            data.put(ESConstant.PROD_COLUMN_STORESTATUS, 0);
            data.put(ESConstant.PROD_COLUMN_CONSELL, prodVO.getConsell());
            data.put(ESConstant.PROD_COLUMN_DETAIL, JSONObject.toJSON(prodVO));
            data.put(ESConstant.PROD_COLUMN_TIME, TimeUtils.date_yMd_Hms_2String(new Date()));
            data.put(ESConstant.PROD_COLUMN_UPDATETIME, TimeUtils.date_yMd_Hms_2String(new Date()));
            ElasticSearchProvider.deleteDocumentById(ESConstant.PROD_INDEX, ESConstant.PROD_TYPE, sku+"");
            IndexResponse response = ElasticSearchProvider.addDocument(data, ESConstant.PROD_INDEX, ESConstant.PROD_TYPE, sku+"");
            if(response == null || RestStatus.CREATED != response.status()) {
                return -1;
            }
            return 0;
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("es error:" + e.getMessage());
            return -1;
        }
    }

    /**
     * 修改商品数据到ES中
     *
     * @param paramVo 商品SKU对象
     * @return
     */
    public static int updateProdDocument(BgProdVO paramVo){

        try{
            BgProdVO prodVO = (BgProdVO)paramVo.clone();
            prodVO.setDetail("");
            if(StringUtils.isEmpty(prodVO.getOffdate())){
                prodVO.setOffdate(DEFAULT_DATE);
                prodVO.setOfftime(DEFAULT_TIME);
            }
            if(StringUtils.isEmpty(prodVO.getVaildsdate())){
                prodVO.setVaildsdate(DEFAULT_DATE);
            }
            if(StringUtils.isEmpty(prodVO.getVaildedate())){
                prodVO.setVaildedate(DEFAULT_DATE);
            }
            if(StringUtils.isEmpty(prodVO.getProdsdate())){
                prodVO.setProdsdate(DEFAULT_DATE);
            }
            if(StringUtils.isEmpty(prodVO.getProdedate())){
                prodVO.setProdedate(DEFAULT_DATE);
            }
            long sku = prodVO.getSku();
            String keyword = StringUtils.checkObjectNull(prodVO.getBrandName(), "").trim()
                    + "|" + StringUtils.checkObjectNull(prodVO.getPopname(), "").trim()
                    + "|" + StringUtils.checkObjectNull(prodVO.getProdname(), "").trim()
                    + "|" + StringUtils.checkObjectNull(prodVO.getManuName(), "").trim();
            String spec = prodVO.getSpec();
            long spu = prodVO.getSpu();
            long manuno = prodVO.getManuNo();
            String manuname = StringUtils.checkObjectNull(prodVO.getManuName(),"").trim();
            long brandno = prodVO.getBrandNo();
            String brandname = StringUtils.checkObjectNull(prodVO.getBrandName(),"").trim();

            prodVO.clone();
            GetResponse getResponse = ElasticSearchProvider.getDocumentById(ESConstant.PROD_INDEX, ESConstant.PROD_TYPE, sku+"");
            Map<String, Object> data = new HashMap<>();
            data.put(ESConstant.PROD_COLUMN_SKU, sku);
            data.put(ESConstant.PROD_COLUMN_CONTENT, keyword);
            data.put(ESConstant.PROD_COLUMN_SPEC, spec);
            data.put(ESConstant.PROD_COLUMN_SPU, spu);
            data.put(ESConstant.PROD_COLUMN_MANUNO, manuno);
            data.put(ESConstant.PROD_COLUMN_MANUNAME, manuname);
            data.put(ESConstant.PROD_COLUMN_BRANDNO, brandno);
            data.put(ESConstant.PROD_COLUMN_BRANDNAME, brandname);
            data.put(ESConstant.PROD_COLUMN_PRODSTATUS, getResponse.getSourceAsMap().get(ESConstant.PROD_COLUMN_PRODSTATUS));
            data.put(ESConstant.PROD_COLUMN_SKUCSTATUS,  paramVo.getSkuCstatus());
            data.put(ESConstant.PROD_COLUMN_VATP, prodVO.getVatp());
            data.put(ESConstant.PROD_COLUMN_SALES, getResponse.getSourceAsMap().get(ESConstant.PROD_COLUMN_SALES));
            data.put(ESConstant.PROD_COLUMN_RULESTATUS, 0);
            data.put(ESConstant.PROD_COLUMN_STORESTATUS, 0);
            data.put(ESConstant.PROD_COLUMN_CONSELL, prodVO.getConsell());
            data.put(ESConstant.PROD_COLUMN_DETAIL, JSONObject.toJSON(prodVO));
            data.put(ESConstant.PROD_COLUMN_TIME, getResponse.getSourceAsMap().get(ESConstant.PROD_COLUMN_TIME).toString());
            data.put(ESConstant.PROD_COLUMN_UPDATETIME, TimeUtils.date_yMd_Hms_2String(new Date()));
            UpdateResponse response = ElasticSearchProvider.updateDocumentById(data, ESConstant.PROD_INDEX, ESConstant.PROD_TYPE, sku+"");
            if(response == null || RestStatus.OK != response.status()) {
                return -1;
            }
            return 0;
        }catch (Exception e){
            e.printStackTrace();
            return -1;
        }

    }

    /**
     * 根据sku码删除商品文档
     *
     * @param sku sku码
     * @return
     */
    public static void deleteProdDocument(long sku){
        GetResponse response = ElasticSearchProvider.getDocumentById(ESConstant.PROD_INDEX, ESConstant.PROD_TYPE, sku+"");
        if (response != null && response.isExists()) {
            ElasticSearchProvider.deleteDocumentById(ESConstant.PROD_INDEX, ESConstant.PROD_TYPE, sku+"");
        }
    }

    /**
     * 批量修改商品状态
     *
     * @param skuList sku列表
     * @param prodstatus 1:代表上架 0:代表下架
     * @return
     */
    public static int updateProdStatusDocList(List<Long> skuList,int prodstatus){
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
                    .setQuery(boolQuery);


            response = requestBuilder
                    .execute().actionGet();

            BulkRequestBuilder bulkRequest = client.prepareBulk();
            if(response != null && response.getHits().totalHits > 0){
                for (SearchHit searchHit : response.getHits()) {
                    Map<String, Object> sourceMap = searchHit.getSourceAsMap();
                    long sku = Long.parseLong(sourceMap.get(ESConstant.PROD_COLUMN_SKU).toString());
                    sourceMap.put(ESConstant.PROD_COLUMN_PRODSTATUS, prodstatus);
                    sourceMap.put(ESConstant.PROD_COLUMN_UPDATETIME, TimeUtils.date_yMd_Hms_2String(new Date()));
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

    /**
     * 根据条件全文检索商品
     *
     * @param keyword
     * @param specList
     * @param manuNoList
     * @param spuList
     * @param pagenum
     * @param pagesize
     * @return
     */
    public static SearchResponse searchProd(String keyword,List<String> specList,List<Long> manuNoList, List<Long> spuList,int pagenum,int pagesize){
        SearchResponse response = null;
        try {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            if(specList != null && specList.size() > 0){
                Object [] specArray = new String[specList.size()];
                specArray = specList.toArray(specArray);
                TermsQueryBuilder builder = QueryBuilders.termsQuery(ESConstant.PROD_COLUMN_SPEC, specArray);
			    boolQuery.must(builder);
            }
            if(manuNoList != null && manuNoList.size() > 0){
                Object [] manuNoArray = new String[manuNoList.size()];
                manuNoArray = manuNoList.toArray(manuNoArray);
                TermsQueryBuilder builder = QueryBuilders.termsQuery(ESConstant.PROD_COLUMN_MANUNO, manuNoArray);
                boolQuery.must(builder);
            }
            if(spuList != null && spuList.size() > 0){
                Object [] spuArray = new Long[spuList.size()];
                spuArray = spuList.toArray(spuArray);
                TermsQueryBuilder builder = QueryBuilders.termsQuery(ESConstant.PROD_COLUMN_SPU, spuArray);
                boolQuery.must(builder);
            }
            if(!StringUtils.isEmpty(keyword)){
                MatchQueryBuilder matchQuery = matchQuery(ESConstant.PROD_COLUMN_CONTENT, keyword).analyzer("ik_max_word");
                boolQuery.must(matchQuery);
            }
            MatchQueryBuilder builder = QueryBuilders.matchQuery(ESConstant.PROD_COLUMN_PRODSTATUS, "1");
            boolQuery.must(builder);
            TransportClient client = ElasticSearchClientFactory.getClientInstance();
            int from = pagenum * pagesize - pagesize;
            response = client.prepareSearch(ESConstant.PROD_INDEX)
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
     * @param keyword
     * @param specList
     * @param manunameList
     * @param brandnameList
     * @param sort
     * @param pagenum
     * @param pagesize
     * @return
     */
    public static SearchResponse searchProdMall(String keyword,long spu,List<String> specList,List<String> manunameList,List<String> brandnameList,int sort,int pagenum,int pagesize){
        SearchResponse response = null;
        try {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            if(specList != null && specList.size() > 0){
                Object [] specArray = new String[specList.size()];
                specArray = specList.toArray(specArray);
                TermsQueryBuilder builder = QueryBuilders.termsQuery(ESConstant.PROD_COLUMN_SPEC, specArray);
                boolQuery.must(builder);
            }
            if(manunameList != null && manunameList.size() > 0){
                Object [] manuNoArray = new String[manunameList.size()];
                manuNoArray = manunameList.toArray(manuNoArray);
                TermsQueryBuilder builder = QueryBuilders.termsQuery(ESConstant.PROD_COLUMN_MANUNAME, manuNoArray);
                boolQuery.must(builder);
            }
            if(brandnameList != null && brandnameList.size() > 0){
                Object [] brandNameArray = new String[brandnameList.size()];
                brandNameArray = brandnameList.toArray(brandNameArray);
                TermsQueryBuilder builder = QueryBuilders.termsQuery(ESConstant.PROD_COLUMN_BRANDNAME, brandNameArray);
                boolQuery.must(builder);
            }
            if(!StringUtils.isEmpty(keyword)){
                MatchQueryBuilder matchQuery = QueryBuilders.matchQuery(ESConstant.PROD_COLUMN_CONTENT, keyword).analyzer("ik_max_word");
                boolQuery.must(matchQuery);
            }
            MatchQueryBuilder builder = QueryBuilders.matchQuery(ESConstant.PROD_COLUMN_PRODSTATUS, "1");
            boolQuery.must(builder);
            if(spu > 0){
                String spuStr = String.valueOf(spu);

                String start = "1"+addZeroForNum(spuStr, 6 - spuStr.length(), '0') +"00000";
                String end   = "1"+addZeroForNum(spuStr, 6 - spuStr.length(), '9') +"99999";
                RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("spu");
                rangeQuery.gt(start);
                rangeQuery.lt(end);

                String start1 = "2"+addZeroForNum(spuStr, 6 - spuStr.length(), '0') +"00000";
                String end1   = "2"+addZeroForNum(spuStr, 6 - spuStr.length(), '9') +"99999";
                RangeQueryBuilder rangeQuery1 = QueryBuilders.rangeQuery("spu");
                rangeQuery1.gt(start1);
                rangeQuery1.lt(end1);
                org.elasticsearch.index.query.QueryBuilder postFilterBool =QueryBuilders.boolQuery()
                        .should(rangeQuery)
                        .should(rangeQuery1);
                boolQuery.must(postFilterBool);
            }
            FieldSortBuilder sortBuilder = null;
            if(sort > 0){
                if(sort == 1){ // 销量
                    sortBuilder = SortBuilders.fieldSort(ESConstant.PROD_COLUMN_SALES).order(SortOrder.DESC);
                }else if(sort == 2){ // 价格从搞到低
                    sortBuilder = SortBuilders.fieldSort(ESConstant.PROD_COLUMN_VATP).order(SortOrder.DESC);
                }else if(sort ==3){ // 价格从低到高
                    sortBuilder = SortBuilders.fieldSort(ESConstant.PROD_COLUMN_VATP).order(SortOrder.ASC);
                }

            }
            TransportClient client = ElasticSearchClientFactory.getClientInstance();
            int from = pagenum * pagesize - pagesize;
            SearchRequestBuilder requestBuilder = client.prepareSearch(ESConstant.PROD_INDEX)
                    .setQuery(boolQuery)
                    .setFrom(from)
                    .setSize(pagesize);
            if(sortBuilder != null){
                requestBuilder.addSort(sortBuilder);
            }
            response = requestBuilder
                    .execute().actionGet();

        }catch(Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    /**
     * 根据条件全文检索商品
     *
     * @param statusSet
     * @param  sort
     * @param pagenum
     * @param pagesize
     * @return
     */
    public static SearchResponse searchProdWithStatusList(Set<Integer> statusSet,String keyword, int sort,int pagenum, int pagesize){
        SearchResponse response = null;
        try {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            if(!StringUtils.isEmpty(keyword)){
                MatchQueryBuilder matchQuery = matchQuery(ESConstant.PROD_COLUMN_CONTENT, keyword).analyzer("ik_max_word");
                boolQuery.must(matchQuery);
            }
            if(statusSet!=null && statusSet.size() >0){
                Object [] resultArray = new Integer[statusSet.size()];
                resultArray = statusSet.toArray(resultArray);
                TermsQueryBuilder builder = QueryBuilders.termsQuery(ESConstant.PROD_COLUMN_SKUCSTATUS, resultArray);
                boolQuery.must(builder);
            }
            MatchQueryBuilder builder = QueryBuilders.matchQuery(ESConstant.PROD_COLUMN_PRODSTATUS, "1");
            boolQuery.must(builder);
            TransportClient client = ElasticSearchClientFactory.getClientInstance();
            int from = pagenum * pagesize - pagesize;
            FieldSortBuilder sortBuilder = null;
            if(sort > 0){
                if(sort == 1){ // 销量
                    sortBuilder = SortBuilders.fieldSort(ESConstant.PROD_COLUMN_SALES).order(SortOrder.DESC);
                }else if(sort == 2){ // 时间
                    sortBuilder = SortBuilders.fieldSort(ESConstant.PROD_COLUMN_TIME).order(SortOrder.DESC);
                }

            }
            SearchRequestBuilder requestBuilder = client.prepareSearch(ESConstant.PROD_INDEX)
                    .setQuery(boolQuery)
                    .setFrom(from)
                    .setSize(pagesize);
            if(sortBuilder != null){
                requestBuilder.addSort(sortBuilder);
            }
            response = requestBuilder
                    .execute().actionGet();

        }catch(Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    /**
     * 根据条件全文检索热销商品
     *
     * @param keyword type 0 热销  1 控销
     * @param pagenum
     * @param pagesize
     * @return
     */
    public static SearchResponse searchHotProd(int type, String keyword,int pagenum, int pagesize){
        SearchResponse response = null;
        try {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            if (type == 0) {//热销
                RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(ESConstant.PROD_COLUMN_SALES);
                rangeQuery.gt(0);
                boolQuery.must(rangeQuery);
            } else {//控销
                MatchQueryBuilder builder = QueryBuilders.matchQuery(ESConstant.PROD_COLUMN_CONSELL, "0");
                boolQuery.mustNot(builder);
            }
            if(!StringUtils.isEmpty(keyword)){
                MatchQueryBuilder matchQuery = matchQuery(ESConstant.PROD_COLUMN_CONTENT, keyword).analyzer("ik_max_word");
                boolQuery.must(matchQuery);
            }

            MatchQueryBuilder builder = QueryBuilders.matchQuery(ESConstant.PROD_COLUMN_PRODSTATUS, "1");
            boolQuery.must(builder);

            TransportClient client = ElasticSearchClientFactory.getClientInstance();
            int from = pagenum * pagesize - pagesize;
            response = client.prepareSearch(ESConstant.PROD_INDEX)
                    .setQuery(boolQuery)
                    .setFrom(from)
                    .setSize(pagesize)
                    .addSort(ESConstant.PROD_COLUMN_SALES, SortOrder.DESC)
                    .execute().actionGet();

        }catch(Exception e) {
            e.printStackTrace();
        }

        return response;
    }


    /**
     * 根据条件全文检索品牌商品
     *
     * @param keyword
     * @param pagenum
     * @param pagesize
     * @return
     */
    public static SearchResponse searchProdHasBrand(String keyword,String brandno, String manuName, int pagenum, int pagesize){
        SearchResponse response = null;
        try {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(ESConstant.PROD_COLUMN_BRANDNO);
            rangeQuery.gt(0);
            boolQuery.must(rangeQuery);
            if(!StringUtils.isEmpty(keyword)){
                MatchQueryBuilder matchQuery = matchQuery(ESConstant.PROD_COLUMN_CONTENT, keyword).analyzer("ik_max_word");
                boolQuery.must(matchQuery);
            }
            if(!StringUtils.isEmpty(brandno)){
                MatchQueryBuilder matchQuery = matchQuery(ESConstant.PROD_COLUMN_BRANDNO, brandno);
                boolQuery.must(matchQuery);
            }
            if(!StringUtils.isEmpty(manuName)){
                MatchQueryBuilder matchQuery = matchQuery(ESConstant.PROD_COLUMN_MANUNAME, manuName);
                boolQuery.must(matchQuery);
            }
            MatchQueryBuilder builder = QueryBuilders.matchQuery(ESConstant.PROD_COLUMN_PRODSTATUS, "1");
            boolQuery.must(builder);

            TransportClient client = ElasticSearchClientFactory.getClientInstance();
            int from = pagenum * pagesize - pagesize;
            response = client.prepareSearch(ESConstant.PROD_INDEX)
                    .setQuery(boolQuery)
                    .setFrom(from)
                    .setSize(pagesize)
                    .addSort(ESConstant.PROD_COLUMN_SALES, SortOrder.DESC)
                    .addSort(ESConstant.PROD_COLUMN_BRANDNO, SortOrder.DESC)
                    .execute().actionGet();

        }catch(Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    /**
     * 根据状态码列表和sku全文筛选商品
     *
     * @param statusSet
     * @param spu
     * @param pagenum
     * @param pagesize
     * @return
     */
    public static SearchResponse searchProdWithHotAndSpu(Set<Integer> statusSet, long spu,int pagenum, int pagesize){
        SearchResponse response = null;
        try {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            if(statusSet!=null && statusSet.size() >0){
                Object[] resultArray = new Integer[statusSet.size()];
                resultArray = statusSet.toArray(resultArray);
                TermsQueryBuilder builder = QueryBuilders.termsQuery(ESConstant.PROD_COLUMN_SKUCSTATUS, resultArray);
                boolQuery.must(builder);
            }
            if(spu > 0){
                String start = "1"+addZeroForNum(spu+"", 6) +"00000";
                String end = "1"+addZeroForNum(spu+"", 6) +"99999";
                RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("spu");
                rangeQuery.gt(start);
                rangeQuery.lt(end);

                String start1 = "2"+addZeroForNum(spu+"", 6) +"00000";
                String end1 = "2"+addZeroForNum(spu+"", 6) +"99999";
                RangeQueryBuilder rangeQuery1 = QueryBuilders.rangeQuery(ESConstant.PROD_COLUMN_SPU);
                rangeQuery1.gt(start1);
                rangeQuery1.lt(end1);
                org.elasticsearch.index.query.QueryBuilder postFilterBool =QueryBuilders.boolQuery()
                        .should(rangeQuery)
                        .should(rangeQuery1);
                boolQuery.must(postFilterBool);
            }
            MatchQueryBuilder builder = QueryBuilders.matchQuery(ESConstant.PROD_COLUMN_PRODSTATUS, "1");
            boolQuery.must(builder);
            TransportClient client = ElasticSearchClientFactory.getClientInstance();
            int from = pagenum * pagesize - pagesize;
            response = client.prepareSearch(ESConstant.PROD_INDEX)
                    .setQuery(boolQuery)
                    .setFrom(from)
                    .setSize(pagesize)
                     .addSort(ESConstant.PROD_COLUMN_SALES, SortOrder.DESC)
                    .addSort(ESConstant.PROD_COLUMN_TIME, SortOrder.DESC)
                    .execute().actionGet();

        }catch(Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    public static List<String> getConditions(String keyword,int spu,String column){

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        if(!StringUtils.isEmpty(keyword)){
            MatchQueryBuilder matchQuery = matchQuery(ESConstant.PROD_COLUMN_CONTENT, keyword).analyzer("ik_max_word");
            boolQuery.must(matchQuery);
        }

        if(spu > 0){
            //110000000101
            String start = "1"+addZeroForNum(spu+"", 6) +"00000";
            String end = "1"+addZeroForNum(spu+"", 6) +"99999";
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("spu");
            rangeQuery.gt(start);
            rangeQuery.lt(end);

            String start1 = "2"+addZeroForNum(spu+"", 6) +"00000";
            String end1 = "2"+addZeroForNum(spu+"", 6) +"99999";
            RangeQueryBuilder rangeQuery1 = QueryBuilders.rangeQuery(ESConstant.PROD_COLUMN_SPU);
            rangeQuery1.gt(start1);
            rangeQuery1.lt(end1);
            org.elasticsearch.index.query.QueryBuilder postFilterBool =QueryBuilders.boolQuery()
                    .should(rangeQuery)
                    .should(rangeQuery1);
            boolQuery.must(postFilterBool);
        }
        MatchQueryBuilder builder = QueryBuilders.matchQuery(ESConstant.PROD_COLUMN_PRODSTATUS, "1");
        boolQuery.must(builder);

        TransportClient client = ElasticSearchClientFactory.getClientInstance();
        SearchRequestBuilder requestBuilder = client.prepareSearch(ESConstant.PROD_INDEX).setQuery(boolQuery);
        AggregationBuilder aggregationBuilder = AggregationBuilders.terms("agg").field(column);
        SearchResponse response = requestBuilder
                .addAggregation(aggregationBuilder)
                .setExplain(true).execute().actionGet();

        Terms agg = response.getAggregations().get("agg");

        List<String> keys = new ArrayList<>();
        for (Terms.Bucket bucket : agg.getBuckets()) {
            System.out.println(bucket.getKey() + ":" + bucket.getDocCount());
            keys.add(bucket.getKey().toString());
        }
        return keys;
    }

    /**
     * 根据sku列表检索商品
     *
     * @param skuList
     * @param keyword
     * @return
     */
    public static SearchResponse searchProdBySpuList(List<Long> skuList,String keyword,int pagenum, int pagesize){
        SearchResponse response = null;
        try {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            if(!StringUtils.isEmpty(keyword)){
                MatchQueryBuilder matchQuery = matchQuery(ESConstant.PROD_COLUMN_CONTENT, keyword).analyzer("ik_max_word");
                boolQuery.must(matchQuery);
            }
            if(skuList != null && skuList.size() > 0){
                Object [] spuArray = new Long[skuList.size()];
                spuArray = skuList.toArray(spuArray);
                TermsQueryBuilder builder = QueryBuilders.termsQuery(ESConstant.PROD_COLUMN_SKU, spuArray);
                boolQuery.must(builder);
            }
            MatchQueryBuilder builder = QueryBuilders.matchQuery(ESConstant.PROD_COLUMN_PRODSTATUS, "1");
            boolQuery.must(builder);

            int from = pagenum * pagesize - pagesize;
            TransportClient client = ElasticSearchClientFactory.getClientInstance();
            response = client.prepareSearch(ESConstant.PROD_INDEX)
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
     * 根据sku列表检索商品
     *
     * @param skuList
     * @param  keyword
     * @return
     */
    public static SearchResponse searchProdBySpuListAndKeyword(List<Long> skuList,String keyword){
        SearchResponse response = null;
        try {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            if(skuList != null && skuList.size() > 0){
                Object [] spuArray = new Long[skuList.size()];
                spuArray = skuList.toArray(spuArray);
                TermsQueryBuilder builder = QueryBuilders.termsQuery(ESConstant.PROD_COLUMN_SKU, spuArray);
                boolQuery.must(builder);
            }

            if(!StringUtils.isEmpty(keyword)){
                MatchQueryBuilder matchQuery = matchQuery(ESConstant.PROD_COLUMN_CONTENT, keyword).analyzer("ik_max_word");
                boolQuery.must(matchQuery);
            }
            MatchQueryBuilder builder = QueryBuilders.matchQuery(ESConstant.PROD_COLUMN_PRODSTATUS, "1");
            boolQuery.must(builder);

            TransportClient client = ElasticSearchClientFactory.getClientInstance();
            response = client.prepareSearch(ESConstant.PROD_INDEX)
                    .setQuery(boolQuery)
                    .execute().actionGet();

        }catch(Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    public static String addZeroForNum(String str, int strLength, char c) {
        int strLen = str.length();
        StringBuffer sb = null;

        if (strLen <= 0) {
            return str;
        }

        while (strLen < strLength) {
            sb = new StringBuffer();
            sb.append(str).append(c);// 右补0
            str = sb.toString();
            strLen = str.length();
        }
        return str;
    }

    public static void main(String[] args) {
//        getConditions("", 10, "manuname");
        Set<Integer> statusList = new HashSet<>();
        statusList.add(0);
//        statusList.add(256);
//        SearchResponse response = searchProdWithStatusList(statusList,1, 1,100);
//        System.out.println(response.getHits().totalHits);
//        SearchResponse response = searchProdMall("",1000,null,null,null,0,1, 20);
//        System.out.println(response.getHits().totalHits);
//        List<Long> spuList = new ArrayList<>();
//        spuList.add(110000000010L);
//        spuList.add(110000000110L);
//        spuList.add(110000000210L);
//        spuList.add(110000000310L);
//        spuList.add(110070100010L);
//        spuList.add(110010100010L);
//        spuList.add(110010400010L);
//        spuList.add(110100100010L);
//        spuList.add(110090100011L);
//        spuList.add(110010200010L);
//        spuList.add(110100100110L);
//        spuList.add(110050400036L);
//        spuList.add(210050400024L);
//        spuList.add(110070300010L);
//        spuList.add(110020100019L);
//        spuList.add(110010400110L);
//        spuList.add(210000000010L);
//        spuList.add(110180300011L);
//        spuList.add(110000000410L);
//        spuList.add(110000000510L);
//        spuList.add(110100100012L);
//        spuList.add(110100100013L);
//        spuList.add(110000000016L);
//        spuList.add(110000000015L);
//        spuList.add(110120100023L);
//        spuList.add(110000000115L);
//        spuList.add(110000000215L);
//        spuList.add(110000000610L);
//        spuList.add(210000000110L);
//        spuList.add(110000000315L);
//        spuList.add(110000000710L);
//        spuList.add(210000000210L);
//        spuList.add(110180400010L);
//        SearchResponse response = searchProd("",null, null, null,1,20);
//        System.out.println(response.getHits().totalHits);
//        SearchResponse response = searchProdHasBrand("", "3853312138937344", 1,20);
//        System.out.println(response.getHits().totalHits);
    }

}
