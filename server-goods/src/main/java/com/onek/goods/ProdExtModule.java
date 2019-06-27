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
    private static final String QUERY_PRODBRAND = "  SELECT b.brandno, b.brandname  FROM {{?" + DSMConst.TD_PROD_BRAND +"}} b, {{?" + DSMConst.TD_PROD_SPU +"}} spu, {{?" + DSMConst.TD_PROD_SKU +"}} sku WHERE b.cstatus&1 = 0 " +
            "and spu.cstatus&1 = 0 and sku.cstatus&1 = 0 and sku.prodstatus = 1 and b.brandno = spu.brandno and spu.spu = sku.spu group by b.brandno, b.brandname";

    /**
     * @接口摘要 根据商品分类码获取相关商品列表
     * @业务场景 运营后台活动管理维护商品触发库存变化时调用
     * @传参类型 JSON
     * @传参列表 spu=商品分类码
     * @返回列表 code=200 data=结果信息
     * @作者: 蒋文广
     */
    @UserPermission(ignore = true)
    public List<Long> getSkuListByCondition(AppContext appContext){

        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        Long spu = json.has("spu") ? json.get("spu").getAsLong() : 0;
        List<Long> skuList = new ArrayList<>();
        String sql = QUERY_PROD;
        List<Object[]> queryList = null;
        if(spu > 0){
            StringBuilder regexp =
                    new StringBuilder("^[0-9]{1}")
                            .append(spu);
            sql  += " and spu REGEXP ? ";
            queryList = BASE_DAO.queryNative(sql, regexp.toString());
        }else{
            queryList = BASE_DAO.queryNative(sql);
        }

        if(queryList != null && queryList.size() > 0){
            for(Object[] obj : queryList){
                skuList.add(Long.parseLong(obj[0].toString()));
            }
        }
        return skuList;
    }

    /**
     * @接口摘要 查询品牌信息 销售量高在前
     * @业务场景 运营后台活动管理维护商品触发库存变化时调用
     * @传参类型 JSON
     * @传参列表 无
     * @返回列表 code=200 data=结果信息
     * @作者: 蒋文广
     */
    @UserPermission(ignore = true)
    public Result queryBrandInfo(AppContext appContext){
        Page page = new Page();
        page.pageIndex = appContext.param.pageIndex;
        page.pageSize  = appContext.param.pageNumber;

        List<Object> paramList = new ArrayList<>();
        PageHolder pageHolder = new PageHolder(page);
        List<Object[]> queryResult = BASE_DAO.queryNative(
                    pageHolder, page, "sku.sales desc", QUERY_PRODBRAND, paramList.toArray());
        ProdBrandVO[] result = new ProdBrandVO[queryResult.size()];

        if (result.length > 0) {
            BASE_DAO.convToEntity(queryResult, result, ProdBrandVO.class, new String[]{"brandno", "brandname"});
        }

        return new Result().setQuery(result, pageHolder);
    }
}
