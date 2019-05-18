package com.onek.goods;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.goods.entities.ProdBrandVO;
import constant.DSMConst;
import dao.BaseDAO;

import java.util.ArrayList;
import java.util.List;

public class ProdExtModule {

    private static final BaseDAO BASE_DAO = BaseDAO.getBaseDAO();
    private static final String QUERY_PROD = "select sku from {{?" + DSMConst.TD_PROD_SKU +"}} where cstatus&1=0 and prodstatus = 1";
    private static final String QUERY_PRODBRAND = " SELECT oid, brandno, brandname, cstatus  FROM {{?" + DSMConst.TD_PROD_BRAND + "}} WHERE cstatus&1 = 0 ";

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

    @UserPermission(ignore = true)
    public Result queryBrandInfo(AppContext appContext){
        Page page = new Page();
        page.pageIndex = appContext.param.pageIndex;
        page.pageSize  = appContext.param.pageNumber;

        List<Object> paramList = new ArrayList<>();
        PageHolder pageHolder = new PageHolder(page);
        List<Object[]> queryResult = BASE_DAO.queryNative(
                pageHolder, page, QUERY_PRODBRAND, paramList.toArray());
        ProdBrandVO[] result = new ProdBrandVO[queryResult.size()];

        if (result.length > 0) {
            BASE_DAO.convToEntity(queryResult, result, ProdBrandVO.class);
        }

        return new Result().setQuery(result, pageHolder);
    }
}
