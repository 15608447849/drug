package com.onek.discount.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.onek.discount.entity.ActivityVO;
import com.onek.discount.entity.AssDrugVO;
import com.onek.discount.entity.LadderVO;
import com.onek.discount.entity.TimeVO;
import elasticsearch.ElasticSearchClientFactory;
import elasticsearch.ElasticSearchProvider;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscountESUtil {

    public static int addActivity(ActivityVO activityVO){
        JSONObject jsonObj = (JSONObject) JSON.toJSON(activityVO);
        IndexResponse response = ElasticSearchProvider.addDocument(jsonObj, "prom_act", "prom_act", String.valueOf(activityVO.getUnqid()));
        if(response == null || RestStatus.CREATED != response.status()) {
            return -1;
        }
        return 1;
    }

    public static int addTimeList(List<TimeVO> timeList){
        if(timeList != null && timeList.size() > 0){
            List<JSONObject> jsonList = new ArrayList<>();
            for(TimeVO timeVO : timeList){
                JSONObject jsonObj = (JSONObject) JSON.toJSON(timeVO);
                jsonObj.put("id", timeVO.getUnqid());
                jsonList.add(jsonObj);
            }

            int count = ElasticSearchProvider.addDocumentList(jsonList, "prom_time","prom_time");
            return count;
        }

        return -1;
    }

    public static int addLadoffList(List<LadderVO> ladderList){

        if(ladderList != null && ladderList.size() > 0){
            List<JSONObject> jsonList = new ArrayList<>();
            for(LadderVO ladderVo : ladderList){
                JSONObject jsonObj = (JSONObject) JSON.toJSON(ladderVo);
                jsonObj.put("id", ladderVo.getUnqid());
                jsonList.add(jsonObj);
            }

            int count = ElasticSearchProvider.addDocumentList(jsonList, "prom_ladoff","prom_ladoff");
            return count;
        }
        return -1;

    }

    public static int addAssDrugList(List<AssDrugVO> assDrugList){

        if(assDrugList != null && assDrugList.size() > 0){
            List<JSONObject> jsonList = new ArrayList<>();
            for(AssDrugVO ladderVo : assDrugList){
                JSONObject jsonObj = (JSONObject) JSON.toJSON(ladderVo);
                jsonObj.put("id", ladderVo.getUnqid());
                jsonList.add(jsonObj);
            }

            int count = ElasticSearchProvider.addDocumentList(jsonList, "prom_assdrug","prom_assdrug");
            return count;
        }
        return -1;

    }

    public static int deleteDiscountByActno(long actcode) {

        TransportClient client = ElasticSearchClientFactory.getClientInstance();
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        GetResponse response = client.prepareGet("prom_act", "prom_act", actcode + "").get();
        if (response != null) {
            bulkRequest.add(client.prepareDelete("prom_act", "prom_act", response.getId()).request());
        }

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        MatchQueryBuilder matchQuery = QueryBuilders.matchQuery("actcode", actcode);
        boolQuery.must(matchQuery);
        SearchResponse searchResponse = client.prepareSearch("prom_assdrug", "prom_time", "prom_ladoff")
                .setQuery(boolQuery)
                .execute().actionGet();
        Map<String,String> deleteIdMap = new HashMap<>();
        for (SearchHit searchHit : searchResponse.getHits()) {
            deleteIdMap.put(searchHit.getId(), searchHit.getIndex());
        }

        if(deleteIdMap != null && deleteIdMap.size() > 0){
            for(String key : deleteIdMap.keySet()){
                bulkRequest.add(client.prepareDelete(deleteIdMap.get(key), deleteIdMap.get(key), key).request());
            }
        }
        BulkResponse bulkResponse = bulkRequest.get();
        if (bulkResponse.hasFailures()) {
            for(BulkItemResponse item : bulkResponse.getItems()){
                System.out.println(item.getFailureMessage());
            }
            return -1;
        } else {
            return 1;
        }
    }

//    public static void main(String[] args) {
//        ActivityVO activityVO  = new ActivityVO();
//        activityVO.setUnqid(110100099901L);
//        activityVO.setActname("清明节大满减");
//        activityVO.setIncpriority(1);
//        activityVO.setCpriority(2);
//        activityVO.setQualcode(0);
//        activityVO.setQualvalue(1);
//        activityVO.setExcdiscount(0);
//        activityVO.setSdate("2019-04-02");
//        activityVO.setSdate("2019-04-06");
//        activityVO.setActcycle(1);
//        activityVO.setActdesc("满减大酬宾");
//        activityVO.setRulecode(10);
//        activityVO.setCstatus(0);
//        int status = addActivity(activityVO);
//        System.out.println("添加活动状态:"+status);
//
//        List<TimeVO> timeVOList = new ArrayList<>();
//        TimeVO timeVO = new TimeVO();
//        timeVO.setUnqid(10100100100111L);
//        timeVO.setActcode(activityVO.getUnqid());
//        timeVO.setSdate("09:11:432");
//        timeVO.setEdate("16:00:432");
//        timeVO.setCstatus(0);
//        timeVOList.add(timeVO);
//
//        status = addTimeList(timeVOList);
//        System.out.println("添加场次状态:"+status);
//
//        List<AssDrugVO> drugVOList = new ArrayList<>();
//        AssDrugVO drugVO = new AssDrugVO();
//        drugVO.setUnqid(20200209399399L);
//        drugVO.setActstock(200);
//        drugVO.setMenucode(0);
//        drugVO.setActcode(activityVO.getUnqid());
//        drugVO.setGcode(11010009990102L);
//        drugVO.setCstatus(0);
//        drugVOList.add(drugVO);
//
//        drugVO = new AssDrugVO();
//        drugVO.setUnqid(20200209399400L);
//        drugVO.setActstock(900);
//        drugVO.setMenucode(0);
//        drugVO.setActcode(activityVO.getUnqid());
//        drugVO.setGcode(11010009990103L);
//        drugVO.setCstatus(0);
//        drugVOList.add(drugVO);
//
//        drugVO = new AssDrugVO();
//        drugVO.setUnqid(20200209399401L);
//        drugVO.setActstock(800);
//        drugVO.setMenucode(0);
//        drugVO.setActcode(activityVO.getUnqid());
//        drugVO.setGcode(11010009990104L);
//        drugVO.setCstatus(0);
//        drugVOList.add(drugVO);
//        status = addAssDrugList(drugVOList);
//        System.out.println("添加商品列表:"+status);
//
//        List<LadderVO> ladderVOList = new ArrayList<>();
//        LadderVO ladderVO = new LadderVO();
//        ladderVO.setActcode(activityVO.getUnqid());
//        ladderVO.setUnqid(89929929299929L);
//        ladderVO.setLadamt(200);
//        ladderVO.setOffer(20);
//        ladderVOList.add(ladderVO);
//
//        ladderVO = new LadderVO();
//        ladderVO.setActcode(activityVO.getUnqid());
//        ladderVO.setUnqid(89929929299930L);
//        ladderVO.setLadamt(500);
//        ladderVO.setOffer(60);
//        ladderVOList.add(ladderVO);
//        status = addLadoffList(ladderVOList);
//        System.out.println("添加阶梯:"+status);

//        deleteDiscountByActno(110100099901L);

//    }
}
