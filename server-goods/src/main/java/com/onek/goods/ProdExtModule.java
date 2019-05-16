package com.onek.goods;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import constant.DSMConst;
import dao.BaseDAO;

import java.util.ArrayList;
import java.util.List;

public class ProdExtModule {

    private static final BaseDAO BASE_DAO = BaseDAO.getBaseDAO();
    private static final String QUERY_PROD = "select sku from {{?" + DSMConst.TD_PROD_SKU +"}} where cstatus&1=0 and prodstatus = 1";

    @UserPermission(ignore = true)
    public List<Long> getSkuListByCondition(AppContext appContext){

        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        Long spu = json.has("spu") ? json.get("spu").getAsLong() : 0;
        List<Long> skuList = new ArrayList<>();
        String sql = QUERY_PROD;
        List<Object[]> queryList = null;
        if(spu > 0){
            StringBuilder regexp =
                    new StringBuilder("^")
                            .append(spu);
            sql  += " and spu REGEXP ? ";
            queryList = BASE_DAO.queryNative(sql, spu);
        }else{
            queryList = BASE_DAO.queryNative(sql, new Object[]{});
        }

        if(queryList != null && queryList.size() > 0){
            for(Object[] obj : queryList){
                skuList.add(Long.parseLong(obj[0].toString()));
            }
        }
        return skuList;
    }
}
