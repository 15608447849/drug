package com.onek.util;

import com.alibaba.fastjson.JSONObject;
import com.onek.consts.ESConstant;
import elasticsearch.ElasticSearchProvider;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.rest.RestStatus;
import util.TimeUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FootPrintESUtil {

    /**
     * 添加足迹数据到ES中
     * @return
     */
    public static int addFootPrintDocument(long unqid,int compid,int sku){

        try {
            Map<String, Object> data = new HashMap<>();
            data.put(ESConstant.FOOTPRINT_COLUMN_UNQID, unqid);
            data.put(ESConstant.FOOTPRINT_COLUMN_SKU, sku);
            data.put(ESConstant.FOOTPRINT_COLUMN_COMPID, compid);
            data.put(ESConstant.FOOTPRINT_COLUMN_BROWSEDATE, TimeUtils.date_yMd_Hms_2String(new Date()));
            IndexResponse response = ElasticSearchProvider.addDocument(data, ESConstant.FOOTPRINT_INDEX, ESConstant.FOOTPRINT_TYPE, unqid+"");
            if(response == null || RestStatus.CREATED != response.status()) {
                return -1;
            }
            return 0;
        }catch (Exception e){
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * 修改足迹数据ES
     * @return
     */
    public static int deleteFootPrintDocument(long unqid){

        try {
            DeleteResponse response = ElasticSearchProvider.deleteDocumentById(ESConstant.FOOTPRINT_INDEX, ESConstant.FOOTPRINT_TYPE, unqid+"");
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
     * 修改足迹数据ES
     * @return
     */
    public static int updateFootPrintDocument(long unqid,int compid,int sku){

        try {
            Map<String, Object> data = new HashMap<>();
            data.put(ESConstant.FOOTPRINT_COLUMN_UNQID, unqid);
            data.put(ESConstant.FOOTPRINT_COLUMN_SKU, sku);
            data.put(ESConstant.FOOTPRINT_COLUMN_COMPID, compid);
            data.put(ESConstant.FOOTPRINT_COLUMN_BROWSEDATE, TimeUtils.date_yMd_Hms_2String(new Date()));
            UpdateResponse response = ElasticSearchProvider.updateDocumentById(data, ESConstant.FOOTPRINT_INDEX, ESConstant.FOOTPRINT_TYPE, unqid+"");
            if(response == null || RestStatus.OK != response.status()) {
                return -1;
            }
            return 0;
        }catch (Exception e){
            e.printStackTrace();
            return -1;
        }
    }
}
