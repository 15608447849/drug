package com.onek.goods;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.calculate.auth.QualJudge;
import com.onek.consts.ESConstant;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.goods.entities.ProdVO;
import com.onek.goods.util.ProdESUtil;
import com.onek.util.IceRemoteUtil;
import com.onek.util.stock.RedisStockUtil;
import constant.DSMConst;
import dao.BaseDAO;
import elasticsearch.ElasticSearchClientFactory;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHits;
import org.hyrdpf.util.LogUtil;
import util.GsonUtils;
import util.MathUtil;
import util.TimeUtils;

import java.util.*;

import static com.onek.goods.MainPageModule.assembleData;

/**
 * @author 11842
 * @version 1.1.1
 * @description 套餐///
 * @time 2019/7/30 15:27
 **/
public class PackageModule {
    private static final BaseDAO BASE_DAO = BaseDAO.getBaseDAO();

    private static final String SELECT_BASE_SQL = "select sku,brandno, manuno,actcode from {{?"
            + DSMConst.TD_PROM_ASSDRUG + "}} a left join {{?" + DSMConst.TD_PROD_SKU +"}} s on "
            + " gcode=sku left join {{?" + DSMConst.TD_PROD_SPU + "}} p on s.spu=p.spu "
            + " where a.cstatus&1=0 and s.cstatus&1=0 and p.cstatus&1=0  and gcode > 0  "
            + " and prodstatus=1 and brandno>0 ";

    private static final String SELECT_ACT_SQL = "select unqid from td_prom_act  where unqid=actcode " +
            " and cstatus&1=0 and cstatus&2048>0 and ckstatus&32=0 and fun_prom_cycle(unqid, acttype,"
            + " actcycle, ?, 1) >0 ";

    private static final String SELECT_PACK_SQL = "select unqid,qualcode,qualvalue,cpriority from {{?"
            + DSMConst.TD_PROM_ACT + "}}  where cstatus&1=0 and brulecode=1114 and cstatus&2048>0 and ckstatus&32=0 "
            + " and fun_prom_cycle(unqid, acttype, actcycle, ?, 1) > 0 ";

    private static final String SALL_MENU_SQL = "select menucode, gcode, pkgprodnum, price,actstock,limitnum," +
            "actcode,cstatus from {{?" + DSMConst.TD_PROM_ASSDRUG + "}} where cstatus&1=0 and menucode>0 ";




    @UserPermission(ignore = true)
    public Result getBrand(AppContext appContext) {
        Map<Long, String> brandMap = new HashMap<>();
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int type = jsonObject.get("type").getAsInt();
        if (type == 0) {//品牌专区
            getAllBrand(brandMap);
        } else {//banner
            getBrandWithAct(brandMap);
        }
        return result.success(brandMap);
    }

    private void getBrandWithAct(Map<Long, String> brandMap) {
        List<Long> brandNoList = new ArrayList<>();
        String mmdd = TimeUtils.date_Md_2String(new Date());
        String selectSQL = SELECT_BASE_SQL + " and  exists (" + SELECT_ACT_SQL + ") group by brandno ";
        List<Object[]> queryResult = BASE_DAO.queryNative(selectSQL, mmdd);
        if (queryResult == null || queryResult.isEmpty()) return ;
        queryResult.forEach(qr -> brandNoList.add(Long.valueOf(String.valueOf(qr[1]))));
        getEsBrand(brandNoList, brandMap);
    }

    public static void getEsBrand(List<Long> brandNoList, Map<Long, String> brandMap) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        if(brandNoList != null && brandNoList.size() > 0){
            TermsQueryBuilder builder = QueryBuilders.termsQuery(ESConstant.PROD_COLUMN_BRANDNO, brandNoList);
            boolQuery.must(builder);
        }
        MatchQueryBuilder pBuilder = QueryBuilders.matchQuery(ESConstant.PROD_COLUMN_PRODSTATUS, "1");
        boolQuery.must(pBuilder);
        TransportClient client = ElasticSearchClientFactory.getClientInstance();
        SearchRequestBuilder requestBuilder = client.prepareSearch(ESConstant.PROD_INDEX).setQuery(boolQuery);
        AggregationBuilder aggregationBuilder = AggregationBuilders.terms("agg")
                .field(ESConstant.PROD_COLUMN_BRANDNO).subAggregation(AggregationBuilders.topHits("top")
                        .explain(true).size(1)).size(Integer.MAX_VALUE);
        SearchResponse searchResponse = requestBuilder.addAggregation(aggregationBuilder)
                .setExplain(true).execute().actionGet();
        Terms agg = searchResponse.getAggregations().get("agg");
        for (Terms.Bucket bucket : agg.getBuckets()) {
            TopHits topHits = bucket.getAggregations().get("top");
            SearchHit[] maNuHits = topHits.getHits().getHits();
            long brandNo = Long.valueOf(maNuHits[0].getSourceAsMap().get("brandno").toString());
            String brandName = maNuHits[0].getSourceAsMap().get("brandname").toString();
            brandMap.put(brandNo, brandName);
        }
        System.out.println("品牌列表---->>>>>>>>.. " + GsonUtils.javaBeanToJson(brandMap));
    }

    public static void getAllBrand(Map<Long, String> brandMap) {
        SearchResponse response = ProdESUtil.searchProdGroupByBrand(null,"", "", "");
        if (response != null && response.getHits().totalHits > 0) {
            Terms agg = response.getAggregations().get("agg");
            for (Terms.Bucket bucket : agg.getBuckets()) {
                long brandNo = Long.valueOf(bucket.getKey().toString());
                TopHits topHits = bucket.getAggregations().get("top");
                SearchHit[] maNuHits = topHits.getHits().getHits();
                String brandName = maNuHits[0].getSourceAsMap().get("brandname").toString();
                brandMap.put(brandNo, brandName);
            }
        }
        System.out.println("品牌列表---->>>>>>>>.. " + GsonUtils.javaBeanToJson(brandMap));
    }

    @UserPermission(ignore = true)
    public Result getFactoryAndGoods(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        long brandNo = jsonObject.get("brandno").getAsLong();
        boolean isAct = jsonObject.get("onlyActivity").getAsBoolean();
        if (isAct) {
            List<Long> skuList = new ArrayList<>();
            String mmdd = TimeUtils.date_Md_2String(new Date());
            String selectSQL = SELECT_BASE_SQL + " and brandno=? and  exists (" + SELECT_ACT_SQL + ") group by sku";
            List<Object[]> queryResult = BASE_DAO.queryNative(selectSQL,brandNo, mmdd);
            if (queryResult == null || queryResult.isEmpty()) return result.success(null);
            queryResult.forEach(qr -> skuList.add(Long.valueOf(String.valueOf(qr[0]))));
            //查询品牌下所有商品（根据厂家分组）

        } else {

        }
        return result;
    }

    public static void getEsGoods(List<Long> skuList, Map<Object, List<ProdVO>> brandMap) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        if(skuList != null && skuList.size() > 0){
            TermsQueryBuilder builder = QueryBuilders.termsQuery(ESConstant.PROD_COLUMN_SKU, skuList);
            boolQuery.must(builder);
        }
        MatchQueryBuilder pBuilder = QueryBuilders.matchQuery(ESConstant.PROD_COLUMN_PRODSTATUS, "1");
        boolQuery.must(pBuilder);
        TransportClient client = ElasticSearchClientFactory.getClientInstance();
        SearchRequestBuilder requestBuilder = client.prepareSearch(ESConstant.PROD_INDEX).setQuery(boolQuery);

        AggregationBuilder aggregationBuilder = AggregationBuilders.terms("agg")
                .field(ESConstant.PROD_COLUMN_MANUNO).subAggregation(AggregationBuilders.topHits("top")
                        .explain(true).size(1000)).size(Integer.MAX_VALUE);
        SearchResponse searchResponse = requestBuilder.addAggregation(aggregationBuilder)
                .setExplain(true).execute().actionGet();
        Terms agg = searchResponse.getAggregations().get("agg");
        for (Terms.Bucket bucket : agg.getBuckets()) {
            TopHits topHits = bucket.getAggregations().get("top");
            SearchHit[] maNuHits = topHits.getHits().getHits();
            for (SearchHit hit : maNuHits) {
                Map<String, Object> sMap = hit.getSourceAsMap();
                long maNuNo = Long.valueOf(sMap.get("manuno").toString());
                String maNuName = sMap.get("manuname").toString();

            }
        }
        System.out.println("品牌列表---->>>>>>>>.. " + GsonUtils.javaBeanToJson(brandMap));
    }



    /**
     * @接口摘要 查询所有套餐
     * @业务场景
     * @传参类型 json
     * @传参列表 ActivityVO对象
     * @返回列表 200 成功 -1 失败
     */
    @UserPermission(ignore = true)
    public Result queryAllPackage(AppContext appContext) {
        Result result = new Result();
        Map<Integer, List<ProdVO>> menuProdMap = new HashMap<>();
        try {
            int compId = appContext.getUserSession() != null ? appContext.getUserSession().compId : 0;
            StringBuilder actCodeSB = new StringBuilder();
            String mmdd = TimeUtils.date_Md_2String(new Date());
            List<Object[]> queryResult = BASE_DAO.queryNative(SELECT_PACK_SQL,mmdd);
            for (Object[] qResult : queryResult) {
                int qualcode = Integer.parseInt(String.valueOf(qResult[1]));
                long qualvalue = Integer.parseInt(String.valueOf(qResult[2]));
                if (QualJudge.hasPermission(compId, qualcode, qualvalue)) {
                    long actCode = Long.parseLong(String.valueOf(qResult[0]));
                    actCodeSB.append(actCode).append(",");
                }
            }
            if (actCodeSB.toString().contains(",")) {
                String actCodeStr = actCodeSB.toString().substring(0, actCodeSB.toString().length() - 1);
                List<Object[]> menuInfos = BASE_DAO.queryNative(SALL_MENU_SQL + " and actcode in(" + actCodeStr + ")");
                if (menuInfos == null || menuInfos.isEmpty()) {
                    return result.success("暂无套餐！", "");
                }
                Map<Long, String[]> dataMap = new HashMap<>();
                Map<Integer, Set<Object[]>> menuMap = new HashMap<>();
                Map<Integer, Object[]> menuActMap = new HashMap<>();
                List<Long> skuList = new ArrayList<>();
                String[] otherArr = {"1114", "0", compId + ""};
                Set<String> menuCodeList = new HashSet<>();
                //menucode, gcode, pkgprodnum, price,actstock,limitnum,actcode,cstatus
                menuInfos.forEach(qResult -> {
                    int menucode = Integer.valueOf(String.valueOf(qResult[0]));//套餐码
                    long sku = Long.valueOf(String.valueOf(qResult[1]));//sku
                    String actCode = String.valueOf(qResult[6]);
                    int pkgprodnum =  Integer.valueOf(String.valueOf(qResult[2]));//套餐数量
                    String price = String.valueOf(qResult[3]);
                    skuList.add(sku);//sku
                    dataMap.put(sku, new String[]{String.valueOf(qResult[4]),String.valueOf(qResult[5]), price,
                                    String.valueOf(qResult[7]), actCode, pkgprodnum + ""});
                    Set<Object[]> skuSet = new HashSet<>();
                    menuCodeList.add(menucode+"");
                    if (menuMap.containsKey(menucode)) {
                        skuSet = menuMap.get(menucode);
                    }
                    skuSet.add(new Object[]{sku, pkgprodnum, price});
                    menuMap.put(menucode,skuSet);
                    menuActMap.put(menucode, new Object[]{Long.valueOf(actCode),price});
                });
                List<ProdVO> prodVOList = new ArrayList<>();
                SearchResponse response = ProdESUtil.searchProdBySpuList(new ArrayList<>(skuList), "", 1, skuList.size());
                assembleData(appContext, response, prodVOList, dataMap, otherArr);
                menuProdMap = assMenuData(menuMap, prodVOList, menuActMap);
                remoteQueryPkgShopCartNum(compId,menuCodeList, menuProdMap, appContext.isAnonymous());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.success(menuProdMap);
    }


    private static Map<Integer, List<ProdVO>> assMenuData( Map<Integer, Set<Object[]>> menuMap, List<ProdVO> prodVOList,
                                                           Map<Integer, Object[]> menuActMap) {
        Map<Integer, List<ProdVO>> menuProdMap = new HashMap<>();
        for(Map.Entry<Integer, Set<Object[]>> entry : menuMap.entrySet()){
            long actCode = Long.valueOf(menuActMap.get(entry.getKey())[0].toString());
            List<ProdVO> menuProdList = new ArrayList<>();
            if (menuProdMap.containsKey(entry.getKey())) {
                menuProdList = menuProdMap.get(entry.getKey());
            }
            for (Object[] menuObj : entry.getValue()) {
                for (ProdVO prodDTO : prodVOList) {
                    if (prodDTO.getSku() == Long.valueOf(menuObj[0].toString())) {
                        int pkgprodnum = Integer.valueOf(menuObj[1].toString());
                        String price = menuObj[2].toString();
                        ProdVO prodVO = JSON.parseObject(JSON.toJSONString(prodDTO), ProdVO.class);
                        prodVO.setActinitstock(RedisStockUtil.getActInitStock(Long.valueOf(menuObj[0].toString()), actCode));
                        prodVO.setSurplusstock(RedisStockUtil.getActStockBySkuAndActno(Long.valueOf(menuObj[0].toString()), actCode));
                        prodVO.setBuynum(prodVO.getActinitstock() - prodVO.getSurplusstock());
                        prodVO.setPkgActUPrice(MathUtil.exactDiv(Double.parseDouble(price), 100).doubleValue());
                        prodVO.setPkgprodnum(pkgprodnum);
                        menuProdList.add(prodVO);
                        menuProdMap.put(entry.getKey(), menuProdList);
                    }
                }
            }
        }
        for(Map.Entry<Integer, List<ProdVO>> menuProd : menuProdMap.entrySet()){
            double pkgOrgPrice = 0, actPrice = 0;
            double vatp = pkgPrice(menuProd.getValue());
//            double actPrice = setActPrice(menuProd.getValue());
            for (ProdVO prod : menuProd.getValue()) {
                prod.setPkgUnEnough(pkgUnEnough(menuProd.getValue()));
                prod.setActprize(actPrice);
                if (vatp < 0) {
                    prod.setActprize(vatp);
                    prod.setPkgOrgPrice(vatp);//控销药价格不可见 未登录价格不可见
                } else {
                    pkgOrgPrice = MathUtil.exactAdd(pkgOrgPrice,
                            MathUtil.exactMul(prod.getVatp(), prod.getPkgprodnum()).doubleValue()).doubleValue();
                    actPrice = MathUtil.exactAdd(actPrice,
                            MathUtil.exactMul(prod.getPkgActUPrice(), prod.getPkgprodnum()).doubleValue()).doubleValue();
                }
            }
            if (vatp == 1) {
                menuProd.getValue().get(0).setPkgOrgPrice(pkgOrgPrice);
                menuProd.getValue().get(0).setActprize(actPrice);
            }
        }
//        LogUtil.getDefaultLogger().info("newProdList----111111111111---------->>> " + GsonUtils.javaBeanToJson(menuProdMap));
        return menuProdMap;
    }

    private static boolean pkgUnEnough(List<ProdVO> prodVOList) {
        for (ProdVO prod : prodVOList) {
           if (prod.getSurplusstock() < prod.getPkgprodnum()) {
               return true;
           }
        }
        return false;
    }

    private static double setActPrice(List<ProdVO> prodVOList) {
        double actPrice = 0;
        for (ProdVO prod : prodVOList) {
            actPrice = MathUtil.exactAdd(actPrice,
                    MathUtil.exactMul(prod.getPkgActUPrice(), prod.getPkgprodnum()).doubleValue()).doubleValue();
        }
        return actPrice;
    }

    private static double pkgPrice(List<ProdVO> prodVOList) {
        for (ProdVO prod : prodVOList) {
           if (prod.getVatp() == -2 || prod.getVatp() == -1) {
               prod.setActprize(prod.getVatp());
               return prod.getVatp();
           }
        }
        return 1;
    }

    private static void remoteQueryPkgShopCartNum(int compId,Set<String> menuCodeList, Map<Integer,
            List<ProdVO>> menuProdMap, boolean isAnonymous) {
        if (compId <= 0 || isAnonymous) return;
        if (menuCodeList.size() > 0) {
            String numArr = IceRemoteUtil.queryPkgShopCartNum(compId, String.join(",",  menuCodeList.toArray(new String[0])));
            if (numArr != null && !numArr.isEmpty()) {
                JSONArray goodsArr = JSON.parseArray(numArr);
                for (int i = 0; i < goodsArr.size(); i++) {
                    int pkgNo = goodsArr.getJSONObject(i).getInteger("pkgno");
                    int num = goodsArr.getJSONObject(i).getInteger("pnum");
                    long sku = goodsArr.getJSONObject(i).getLong("sku");
                    if (menuProdMap.containsKey(pkgNo)) {
                        for (ProdVO prodVO : menuProdMap.get(pkgNo)) {
                            if (prodVO.getSku() == sku) {
                                menuProdMap.get(pkgNo).get(0).cart = num/prodVO.getPkgprodnum();
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    static {
//        ElasticSearchClientFactory.init();
//        AppConfig.initLogger("log4j2.xml");
//        AppConfig.initialize();
    }

    public static void main(String[] args) {
//        queryAllPackage();
    }

}
