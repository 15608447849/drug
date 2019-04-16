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
        }
    }

}
