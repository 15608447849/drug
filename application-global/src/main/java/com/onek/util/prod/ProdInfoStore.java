package com.onek.util.prod;

import com.onek.consts.ESConstant;
import elasticsearch.ElasticSearchProvider;
import org.elasticsearch.action.get.GetResponse;
import util.BeanMapUtils;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class ProdInfoStore {
    public static ProdEntity getProdBySku(long sku){

        ProdEntity entity = null;
        GetResponse response = ElasticSearchProvider.getDocumentById(ESConstant.PROD_INDEX, ESConstant.PROD_TYPE, sku+"");
        if(response != null){
            Map<String, Object> sourceMap = response.getSourceAsMap();
            HashMap detail = (HashMap) sourceMap.get("detail");
            entity = new ProdEntity();
            BeanMapUtils.mapToBean(detail, entity);
        }
        return entity;
    }

}
