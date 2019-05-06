package com.onek.util;

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

public class CollectESUtil {

    /**
     * 添加收藏数据到ES中
     * @return
     */
    public static int addCollectDocument(long unqid,int compid,int promtype,int prize,long sku){

        try {
            Map<String, Object> data = new HashMap<>();
            data.put(ESConstant.COLLECT_COLUMN_UNQID, unqid);
            data.put(ESConstant.COLLECT_COLUMN_SKU, sku);
            data.put(ESConstant.COLLECT_COLUMN_COMPID, compid);
            data.put(ESConstant.COLLECT_COLUMN_PRIZE, prize);
            data.put(ESConstant.COLLECT_COLUMN_PROMTYPE, promtype);
            data.put(ESConstant.COLLECT_COLUMN_CREATEDATE, TimeUtils.date_yMd_Hms_2String(new Date()));
            IndexResponse response = ElasticSearchProvider.addDocument(data, ESConstant.COLLECT_INDEX, ESConstant.COLLECT_TYPE, unqid+"");
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
     * 删除收藏数据ES
     * @return
     */
    public static int deleteCollectDocument(long unqid){

        try {
            DeleteResponse response = ElasticSearchProvider.deleteDocumentById(ESConstant.COLLECT_INDEX, ESConstant.COLLECT_TYPE, unqid+"");
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
     * 修改收藏数据ES
     * @return
     */
    public static int updateCollectDocument(long unqid,int compid,int promtype,int prize,long sku){

        try {
            Map<String, Object> data = new HashMap<>();
            data.put(ESConstant.COLLECT_COLUMN_UNQID, unqid);
            data.put(ESConstant.COLLECT_COLUMN_SKU, sku);
            data.put(ESConstant.COLLECT_COLUMN_COMPID, compid);
            data.put(ESConstant.COLLECT_COLUMN_PRIZE, prize);
            data.put(ESConstant.COLLECT_COLUMN_PROMTYPE, promtype);
            data.put(ESConstant.COLLECT_COLUMN_CREATEDATE, TimeUtils.date_yMd_Hms_2String(new Date()));
            UpdateResponse response = ElasticSearchProvider.updateDocumentById(data, ESConstant.COLLECT_INDEX, ESConstant.COLLECT_TYPE, unqid+"");
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
