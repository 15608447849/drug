package com.onek.goods;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.google.gson.*;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.context.StoreBasicInfo;
import com.onek.context.UserSession;
import com.onek.entitys.Result;
import com.onek.goods.entities.AppriseVO;
import com.onek.goods.entities.ProdVO;
import com.onek.goods.util.ProdActPriceUtil;
import com.onek.goods.util.ProdESUtil;
import com.onek.util.GenIdUtil;
import com.onek.util.IceRemoteUtil;
import com.onek.util.dict.DictStore;
import com.onek.util.FileServerUtils;
import com.onek.util.prod.ProdPriceEntity;
import com.onek.util.stock.RedisStockUtil;
import constant.DSMConst;
import dao.BaseDAO;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import redis.util.RedisUtil;
import util.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 商城商品模块
 * @服务名 goodsServer
 * @author JiangWenGuang
 * @version 1.0
 * @since
 */
@SuppressWarnings({"unchecked"})
public class ProdModule {

    private static final BaseDAO BASE_DAO = BaseDAO.getBaseDAO();

    //商品评价
    private static final String INSERT_APPRISE_SQL = "insert into {{?" + DSMConst.TD_TRAN_APPRAISE + "}} "
            + "(unqid,orderno,level,descmatch,logisticssrv,"
            + "content,createtdate,createtime,cstatus,compid,sku) "
            + " values(?,?,?,?,?,"
            + "?,CURRENT_DATE,CURRENT_TIME,0,?,"
            + "?)";

    private static final String QUERY_SPU = "select spu from {{?" + DSMConst.TD_PROD_SPU + "}} where spu REGEXP ?";

    private static final String QUERY_PROD = "select sku from {{?" + DSMConst.TD_PROD_SKU + "}} where cstatus&1=0 and prodstatus = 1";

    /**
     * @接口摘要 产品详情页推荐 匹配同类别spu码的药品数据,找不到的话抓取药品前10条
     * @业务场景 商品详情右侧推荐
     * @传参类型 JSON
     * @传参列表 actcode:活动码 keyword:关键字
     * @返回列表 code=200 data=结果信息
     * @作者: 蒋文广
     */
    @UserPermission(ignore = true)
    public Result getProdDetailHotArea(AppContext appContext) {
        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        int spu = json.get("spu").getAsInt();

        SearchResponse searchResponse = ProdESUtil.searchProdWithHotAndSpu(null, spu, 1, 10);
        List<ProdVO> prodVOList = new ArrayList<>();
        if (searchResponse == null || searchResponse.getHits().totalHits < 5) {
            if (searchResponse != null && searchResponse.getHits().totalHits > 0) {
                assembleData(appContext, searchResponse, prodVOList);
            }
            searchResponse = ProdESUtil.searchProdWithHotAndSpu(null, 0, 1, 10);
            if (searchResponse != null && searchResponse.getHits().totalHits > 0) {
                assembleData(appContext, searchResponse, prodVOList);
            }
        } else {
            assembleData(appContext, searchResponse, prodVOList);
        }
        return new Result().success(prodVOList);
    }

    /**
     * @接口摘要 猜你喜欢推荐
     * @业务场景
     * @传参类型 JSON
     * @传参列表 spu:spu码
     * @返回列表 code=200 data=结果信息
     * @作者: 蒋文广
     */
    @UserPermission(ignore = false)
    public Result guessYouLikeArea(AppContext appContext) {

        UserSession userSession = appContext.getUserSession();
        List<ProdVO> prodList = new ArrayList<>();
        if (userSession != null && userSession.compId > 0) {
            List<String> footPrintMap = IceRemoteUtil.queryFootprint(userSession.compId);
            List<Long> skuList = new ArrayList<>();
            if (footPrintMap != null && footPrintMap.size() > 0) {
                for (String sku : footPrintMap) {
                    try {
                        skuList.add(Long.parseLong(sku));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            SearchResponse response = ProdESUtil.searchProdBySpuList(skuList, "", 1, 10);
            if (response == null || response.getHits().totalHits <= 5) {
                long totalHits = response != null ? response.getHits().totalHits : 0;
                if (totalHits > 0) {
                    assembleData(appContext, response, prodList);
                }
                List<Integer> bb1 = new ArrayList() {{
                    add(218);
                    add(256);
                    add(1024);
                }};
                Set<Integer> result1 = new HashSet<>();
                NumUtil.perComAdd(512, bb1, result1);
                result1.add(512);

                response = ProdESUtil.searchProdWithStatusList(result1, "", 1, 1, 100);
            }

            assembleData(appContext, response, prodList);

        }
        return new Result().success(prodList);
    }

    /**
     * @接口摘要 商城全文搜索商品的搜索条件区域
     * @业务场景
     * @传参类型 JSON
     * @传参列表 keyword=关键字 spu=药品分类码
     * @返回列表 code=200 data=结果信息
     * @作者: 蒋文广
     */
    @UserPermission(ignore = true)
    public Result getConditionByFullTextSearch(AppContext appContext) {
        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        String keyword = (json.has("keyword") ? json.get("keyword").getAsString() : "").trim();
        int spu = json.has("spu") ? json.get("spu").getAsInt() : 0;
        String brandName = (json.has("brandname") ? json.get("brandname").getAsString() : "").trim();//品牌名
        String maNuName = (json.has("manuname") ? json.get("manuname").getAsString() : "").trim();//厂家名
/*        String spec = (json.has("spec") ? json.get("spec").getAsString() : "").trim();//规格名
        if (StringUtils.isEmpty(keyword) && spu <= 0) {
            return new Result().success(null);
        }*/
        Map<String, List<String>> map = new HashMap<>();
        if (StringUtils.isEmpty(brandName)) {
            List<String> brandList = ProdESUtil.getConditions(keyword, spu, "brandname", brandName, maNuName);
            map.put("brandnameList", brandList);
        }
        if (StringUtils.isEmpty(maNuName)) {
            List<String> maNuList = ProdESUtil.getConditions(keyword, spu, "manuname", brandName,maNuName);
            map.put("manunameList", maNuList);
        }
        List<String> specList = ProdESUtil.getConditions(keyword, spu, "spec", brandName, maNuName);
        map.put("specList", specList);
        return new Result().success(map);
    }



    /**
     * @接口摘要 商城全文搜索商品
     * @业务场景
     * @传参类型 JSON
     * @传参列表 keyword=关键字 spu=药品分类码 specArray[]=规格数组 manuArray[]=厂商数组  brandArray[]=品牌数组
     * @返回列表 code=200 data=结果信息
     * @作者: 蒋文广
     */
    @UserPermission(ignore = true)
    public Result fullTextsearchProdMall(AppContext appContext) {
        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        String keyword = (json.has("keyword") ? json.get("keyword").getAsString() : "").trim();
        long spu = json.has("spu") ? json.get("spu").getAsLong() : 0;
        JsonArray specArray = json.has("specArray") ? json.get("specArray").getAsJsonArray() : null;
        JsonArray manuArray = json.has("manuArray") ? json.get("manuArray").getAsJsonArray() : null;
        JsonArray brandArray = json.has("brandArray") ? json.get("brandArray").getAsJsonArray() : null;
        int sort = json.has("sort") ? json.get("sort").getAsInt() : 0;
        List<String> specList = new ArrayList<>();
        if (specArray != null && specArray.size() > 0) {
            Iterator<JsonElement> it = specArray.iterator();
            while (it.hasNext()) {
                JsonElement elem = it.next();
                specList.add(elem.getAsJsonObject().get("val").getAsString());
            }
        }

        List<String> manunameList = new ArrayList<>();
        if (manuArray != null && manuArray.size() > 0) {
            Iterator<JsonElement> it = manuArray.iterator();
            while (it.hasNext()) {
                JsonElement elem = it.next();
                manunameList.add(elem.getAsJsonObject().get("val").getAsString());
            }
        }

        List<String> brandnameList = new ArrayList<>();
        if (brandArray != null && brandArray.size() > 0) {
            Iterator<JsonElement> it = brandArray.iterator();
            while (it.hasNext()) {
                JsonElement elem = it.next();
                brandnameList.add(elem.getAsJsonObject().get("val").getAsString());
            }
        }

        Result r = new Result();
        SearchResponse response = ProdESUtil.searchProdMall(keyword, spu, specList, manunameList, brandnameList, sort, appContext.param.pageIndex, appContext.param.pageNumber);

        List<ProdVO> prodList = new ArrayList<>();
        if (response != null) {
            for (SearchHit searchHit : response.getHits()) {
                convertSearchData(appContext, prodList, searchHit);
            }

        }

        Page page = new Page();
        page.pageSize = appContext.param.pageNumber;
        page.pageIndex = appContext.param.pageIndex;
        page.totalItems = response != null && response.getHits() != null ? (int) response.getHits().totalHits : 0;
        PageHolder pageHolder = new PageHolder(page);
        pageHolder.value = page;
        return r.setQuery(prodList, pageHolder);
    }

    /**
     * @接口摘要 商城智能推荐
     * @业务场景
     * @传参类型 JSON
     * @传参列表 keyword=关键字
     * @返回列表 code=200 data=结果信息
     * @作者: 蒋文广
     */
    @UserPermission(ignore = true)
    public Result intelligentFullTextsearch(AppContext appContext) {
        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        String keyword = (json.has("keyword") ? json.get("keyword").getAsString() : "").trim();

        List<String> contentList = new ArrayList<>();
        SearchResponse response = ProdESUtil.searchProdMall(keyword, 0, null, null, null, 1, 1, 10);
        if (response != null) {
            for (SearchHit searchHit : response.getHits()) {
                String content = searchHit.getSourceAsMap().get("content").toString();
                String[] arr = content.split("[|]");
                contentList.add(arr[1] + " " + arr[2] + " " + arr[3]);
            }

        }
        return new Result().success(contentList);
    }

    /**
     * @接口摘要 商城智能推荐关键字
     * @业务场景
     * @传参类型 JSON
     * @传参列表 keyword=关键字
     * @返回列表 code=200 data=结果信息
     * @作者: 蒋文广
     */
    @UserPermission(ignore = true)
    public Result usualKeyword(AppContext appContext) {

        List<String> contentList = new ArrayList<>();
        SearchResponse response = ProdESUtil.searchProdMall("", 0, null, null, null, 1, 1, 100);
        List<String> keywords = new ArrayList<>();
        List<String> filterKeywords = new ArrayList<>();
        if (response != null) {
            for (SearchHit searchHit : response.getHits()) {
                Map<String, Object> sourceMap = searchHit.getSourceAsMap();
                HashMap detail = (HashMap) sourceMap.get("detail");
                String popname = detail.get("popname") != null ? detail.get("popname").toString() : "";
                if (!keywords.contains(popname)) {
                    keywords.add(popname);
                }
            }
        }
        if (keywords != null && keywords.size() > 0 && keywords.size() >= 6) {
            filterKeywords = keywords.subList(0, 6);
        }
        return new Result().success(filterKeywords);
    }

    /**
     * @接口摘要 运营后台全文搜索商品提供方法
     * @业务场景
     * @传参类型 JSON
     * @传参列表 keyword=关键字 spu=药品分类码 specArray[]=规格数组 manuArray[]=厂商数组  spuArray[]=商品分类码数组
     * @返回列表 code=200 data=结果信息
     * @作者: 蒋文广
     */
    @UserPermission(ignore = true)
    public Result fullTextsearchProd(AppContext appContext) {
        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        String keyword = (json.has("keyword") ? json.get("keyword").getAsString() : "").trim();
        JsonArray specArray = json.get("specArray").getAsJsonArray();
        JsonArray manuArray = json.get("manuArray").getAsJsonArray();
        JsonArray spuArray = json.has("spuArray") ? json.get("spuArray").getAsJsonArray() : null;
        List<String> specList = new ArrayList<>();
        if (specArray != null && specArray.size() > 0) {
            Iterator<JsonElement> it = specArray.iterator();
            while (it.hasNext()) {
                JsonElement elem = it.next();
                specList.add(elem.getAsJsonObject().get("val").getAsString());
            }
        }

        List<Long> manunoList = new ArrayList<>();
        if (manuArray != null && manuArray.size() > 0) {
            Iterator<JsonElement> it = manuArray.iterator();
            while (it.hasNext()) {
                JsonElement elem = it.next();
                manunoList.add(elem.getAsJsonObject().get("val").getAsLong());
            }
        }

        List<Long> spuList = new ArrayList<>();
        if (spuArray != null && spuArray.size() > 0) {
            Iterator<JsonElement> it = spuArray.iterator();
            while (it.hasNext()) {
                JsonElement elem = it.next();
                String val = elem.getAsJsonObject().get("val").getAsString();
                StringBuilder regexp =
                        new StringBuilder("^")
                                .append("[0-9]{1}");

                if (val.length() == 2) {
                    regexp.append(val)
                            .append("[0-9]{9}")
                            .append("$");
                } else if (val.length() == 4) {
                    regexp.append(val)
                            .append("[0-9]{7}")
                            .append("$");
                } else if (val.length() == 6) {
                    regexp.append(val)
                            .append("[0-9]{5}")
                            .append("$");
                }


                List<Object[]> queryList = BASE_DAO.queryNative(QUERY_SPU, regexp.toString());
                if (queryList != null && queryList.size() > 0) {
                    for (Object[] obj : queryList) {
                        spuList.add(Long.parseLong(obj[0].toString()));
                    }
                } else {
                    spuList.add(Long.parseLong(val));
                }
            }
        }

        Result r = new Result();
        SearchResponse response = ProdESUtil.searchProd(keyword, specList, manunoList, spuList, appContext.param.pageIndex, appContext.param.pageNumber);
        List<Map<String, Object>> resultList = new ArrayList<>();
        if (response != null) {
            for (SearchHit searchHit : response.getHits()) {
                Map<String, Object> sourceMap = searchHit.getSourceAsMap();
                resultList.add(sourceMap);
            }

        }
        // r.success(resultList);
        Page page = new Page();
        page.pageSize = appContext.param.pageNumber;
        page.pageIndex = appContext.param.pageIndex;
        page.totalItems = response != null && response.getHits() != null ? (int) response.getHits().totalHits : 0;
        PageHolder pageHolder = new PageHolder(page);
        pageHolder.value = page;
        return r.setQuery(resultList, pageHolder);
    }

    private static void assembleData(AppContext context, SearchResponse response, List<ProdVO> prodList) {
        if (response == null || response.getHits().totalHits <= 0) {
            return;
        }
        for (SearchHit searchHit : response.getHits()) {
            ProdVO prodVO = new ProdVO();
            Map<String, Object> sourceMap = searchHit.getSourceAsMap();

            HashMap detail = (HashMap) sourceMap.get("detail");
            assembleObjectFromEs(prodVO, sourceMap, detail);

            prodList.add(prodVO);
            prodVO.setImageUrl(FileServerUtils.goodsFilePath(prodVO.getSpu(), prodVO.getSku()));
            int ruleStatus = ProdActPriceUtil.getRuleBySku(prodVO.getSku());
            prodVO.setRulestatus(ruleStatus);

            //商品参加活动描述
            prodVO.setLadOffDesc(ProdActPriceUtil.getLoadOffBySku(prodVO.getSku()));

            // 列表显示购进价
            double purchaseprice = NumUtil.div(prodVO.getVatp(), 100);

            // 表示存在活动
            if (ruleStatus > 0) {
                prodVO.setActprod(true);
                List<Integer> bits = NumUtil.getNonZeroBits(ruleStatus);
                if (bits.size() > 1) {
                    prodVO.setMutiact(true);
                }

                if ((ruleStatus & 2048) > 0) { // 秒杀
                    ProdPriceEntity prizeEntity = ProdActPriceUtil.getActIntervalPrizeBySku(prodVO.getSku(), purchaseprice);
                    if (prizeEntity != null) {
                        prodVO.setMinprize(prizeEntity.getMinactprize());
                        prodVO.setMaxprize(prizeEntity.getMaxactprize());
                        prodVO.setActcode(prizeEntity.getActcode() + "");
                        purchaseprice = prizeEntity.getMinactprize();
                        // 代表值存在一个活动
                        if (prizeEntity.getActcode() > 0 && bits.size() == 1) {
                            List<String[]> times = ProdActPriceUtil.getTimesByActcode(prizeEntity.getActcode());
                            GetEffectiveTimeByActCode getEffectiveTimeByActCode = new GetEffectiveTimeByActCode(times).invoke();
                            String sdate = getEffectiveTimeByActCode.getSdate();
                            String edate = getEffectiveTimeByActCode.getEdate();

                            if (StringUtils.isEmpty(sdate) || StringUtils.isEmpty(edate)) { // // 表示搜索的商品不在活动时间内
                                prodVO.setRulestatus(0);
                                prodVO.setActprod(false);
                                prodVO.setMutiact(false);
                                prodVO.setMinprize(NumUtil.div(prodVO.getVatp(), 100));
                                prodVO.setMaxprize(NumUtil.div(prodVO.getVatp(), 100));
                                prodVO.setActprize(NumUtil.div(prodVO.getVatp(), 100));
                                purchaseprice = NumUtil.div(prodVO.getVatp(), 100);;
                                prodVO.setActcode(prizeEntity.getActcode() + "");
                            } else {
                                prodVO.setSdate(sdate);
                                prodVO.setEdate(edate);
                                int initStock = RedisStockUtil.getActInitStock(prodVO.getSku(), prizeEntity.getActcode());
                                int surplusStock = RedisStockUtil.getActStockBySkuAndActno(prodVO.getSku(), prizeEntity.getActcode());
                                prodVO.setActinitstock(initStock);
                                prodVO.setSurplusstock(surplusStock);
                                prodVO.setBuynum(initStock - surplusStock);
                                prodVO.setActprize(prizeEntity.getMinactprize());
                            }
                        }
                    }
                }

                //设置优惠价格毛利润
                prodVO.setGrossProfit(NumUtil.div(prodVO.getRrp(), 100),purchaseprice);
//            else {
//                ProdPriceEntity prizeEntity = ProdActPriceUtil.getActIntervalPrizeBySku(prodVO.getSku(), prodVO.getVatp());
//                if (prizeEntity != null) {
//                    prodVO.setMinprize(prizeEntity.getMinactprize());
//                    prodVO.setMaxprize(prizeEntity.getMaxactprize());
//                    prodVO.setActcode(prizeEntity.getActcode() + "");
//                    int surplusStock = RedisStockUtil.getActStockBySkuAndActno(prodVO.getSku(), prizeEntity.getActcode());
//                    // 代表值存在一个活动
//                    if (prizeEntity.getActcode() > 0 && bits.size() == 1) {
//                        List<String[]> times = ProdActPriceUtil.getTimesByActcode(prizeEntity.getActcode());
//                        GetEffectiveTimeByActCode getEffectiveTimeByActCode = new GetEffectiveTimeByActCode(times).invoke();
//                        String sdate = getEffectiveTimeByActCode.getSdate();
//                        String edate = getEffectiveTimeByActCode.getEdate();
//
//                        if (StringUtils.isEmpty(sdate) || StringUtils.isEmpty(edate) || surplusStock <= 0) { // // 表示搜索的商品不在活动时间内
//                            prodVO.setRulestatus(0);
//                            prodVO.setActprod(false);
//                            prodVO.setMutiact(false);
//                        }
//                    } else if (prizeEntity.getActcode() == 0) { // 没有活动
//                        prodVO.setRulestatus(0);
//                        prodVO.setActprod(false);
//                        prodVO.setMutiact(false);
//                    }
//
//                }
//            }
            }

            if (context.isAnonymous()) {//无权限价格不可见
                prodVO.setVatp(-1);
                prodVO.setMp(-1);
                prodVO.setRrp(-1);
                prodVO.setActprize(-1);
                prodVO.setPurchaseprice(-1);
            } else {
                int controlCode = context.getUserSession() != null ? context.getUserSession().comp.controlCode : 0;
                if ((prodVO.getConsell() & controlCode) == prodVO.getConsell()) {
                    prodVO.setVatp(NumUtil.div(prodVO.getVatp(), 100));
                    prodVO.setMp(NumUtil.div(prodVO.getMp(), 100));
                    prodVO.setRrp(NumUtil.div(prodVO.getRrp(), 100));
                    prodVO.setPurchaseprice(purchaseprice);
                } else {//控销商品未签约价格不可见
                    prodVO.setVatp(-2);
                    prodVO.setMp(-2);
                    prodVO.setRrp(-2);
                    prodVO.setActprize(-2);
                    prodVO.setPurchaseprice(-2);
                }
            }


            prodVO.setStore(RedisStockUtil.getStock(prodVO.getSku()));
            try {
                DictStore.translate(prodVO);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void assembleObjectFromEs(ProdVO prodVO, Map<String, Object> sourceMap, HashMap detail) {
        try {
            if (detail != null) {
                prodVO.setSpu(detail.get("spu") != null ? Long.parseLong(detail.get("spu").toString()) : 0);
                prodVO.setPopname(detail.get("popname") != null ? detail.get("popname").toString() : "");
                prodVO.setProdname(detail.get("prodname") != null ? detail.get("prodname").toString() : "");
                prodVO.setStandarNo(detail.get("standarNo") != null ? detail.get("standarNo").toString() : "");
                prodVO.setBrandNo(detail.get("brandNo") != null ? detail.get("brandNo").toString() : "");
                prodVO.setBrandName(detail.get("brandName") != null ? detail.get("brandName").toString() : "");
                prodVO.setManuNo(detail.get("manuNo") != null ? detail.get("manuNo").toString() : "");
                prodVO.setManuName(detail.get("manuName") != null ? detail.get("manuName").toString() : "");

                prodVO.setSku(detail.get("sku") != null ? Long.parseLong(detail.get("sku").toString()) : 0);
                prodVO.setVatp(detail.get("vatp") != null ? Double.parseDouble(detail.get("vatp").toString()) : 0);
                prodVO.setMp(detail.get("mp") != null ? Double.parseDouble(detail.get("mp").toString()) : 0);
                prodVO.setRrp(detail.get("rrp") != null ? Double.parseDouble(detail.get("rrp").toString()) : 0);

                prodVO.setVaildsdate(detail.get("vaildsdate") != null ? detail.get("vaildsdate").toString() : "");
                prodVO.setVaildedate(detail.get("vaildedate") != null ? detail.get("vaildedate").toString() : "");
                prodVO.setProdsdate(detail.get("prodsdate") != null ? detail.get("prodsdate").toString() : "");
                prodVO.setProdedate(detail.get("prodedate") != null ? detail.get("prodedate").toString() : "");
                prodVO.setSales(sourceMap.get("sales") != null ? Integer.parseInt(sourceMap.get("sales").toString()) : 0);
                prodVO.setWholenum(detail.get("wholenum") != null ? Integer.parseInt(detail.get("wholenum").toString()) : 0);
                prodVO.setMedpacknum(detail.get("medpacknum") != null ? Integer.parseInt(detail.get("medpacknum").toString()) : 0);
                prodVO.setUnit(detail.get("unit") != null ? Integer.parseInt(detail.get("unit").toString()) : 0);

                prodVO.setLimits(detail.get("limits") != null ? Integer.parseInt(detail.get("limits").toString()) : 0);
                prodVO.setStore(detail.get("store") != null ? Integer.parseInt(detail.get("store").toString()) : 0);
                prodVO.setSpec(detail.get("spec") != null ? detail.get("spec").toString() : "");

                prodVO.setSkuCstatus(sourceMap.get("skuCstatus") != null ? Integer.parseInt(sourceMap.get("skuCstatus").toString()) : 0);
                prodVO.setConsell(sourceMap.get("consell") != null ?  Integer.parseInt(detail.get("consell").toString()) : 0);

            }
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }



    /**
     * 组装搜索的数据
     *fullTextsearchProdMall
     * @param prodList
     * @param searchHit
     */
    private static void convertSearchData(AppContext context, List<ProdVO> prodList, SearchHit searchHit) {
        Map<String, Object> sourceMap = searchHit.getSourceAsMap();
        ProdVO prodVO = new ProdVO();
        HashMap detail = (HashMap) sourceMap.get("detail");
        assembleObjectFromEs(prodVO, sourceMap, detail);

        prodVO.setStore(RedisStockUtil.getStock(prodVO.getSku()));
        prodVO.setMutiact(false); // 初始化标记没有活动
        prodVO.setActprod(false);
        prodList.add(prodVO);

        int ruleStatus = ProdActPriceUtil.getRuleBySku(prodVO.getSku());
        prodVO.setRulestatus(ruleStatus);

        //商品参加活动描述
        prodVO.setLadOffDesc(ProdActPriceUtil.getLoadOffBySku(prodVO.getSku()));

        // 列表显示购进价
        double purchaseprice = NumUtil.div(prodVO.getVatp(), 100);

        // 表示存在活动
        if (ruleStatus > 0) {
            prodVO.setActprod(true);
            List<Integer> bits = NumUtil.getNonZeroBits(ruleStatus);
            if (bits.size() > 1) {
                prodVO.setMutiact(true);
            }

            if ((ruleStatus & 2048) > 0) { // 秒杀
                ProdPriceEntity prizeEntity = ProdActPriceUtil.getActIntervalPrizeBySku(prodVO.getSku(), purchaseprice);
                if (prizeEntity != null) {
                    prodVO.setMinprize(prizeEntity.getMinactprize());
                    prodVO.setMaxprize(prizeEntity.getMaxactprize());
                    prodVO.setActcode(prizeEntity.getActcode() + "");
                    purchaseprice = prizeEntity.getMinactprize();
                    // 代表值存在一个活动
                    if (prizeEntity.getActcode() > 0 && bits.size() == 1) {
                        List<String[]> times = ProdActPriceUtil.getTimesByActcode(prizeEntity.getActcode());
                        GetEffectiveTimeByActCode getEffectiveTimeByActCode = new GetEffectiveTimeByActCode(times).invoke();
                        String sdate = getEffectiveTimeByActCode.getSdate();
                        String edate = getEffectiveTimeByActCode.getEdate();

                        if (StringUtils.isEmpty(sdate) || StringUtils.isEmpty(edate)) { // // 表示搜索的商品不在活动时间内
                            prodVO.setRulestatus(0);
                            prodVO.setActprod(false);
                            prodVO.setMutiact(false);
                            prodVO.setMinprize(NumUtil.div(prodVO.getVatp(), 100));
                            prodVO.setMaxprize(NumUtil.div(prodVO.getVatp(), 100));
                            prodVO.setActprize(NumUtil.div(prodVO.getVatp(), 100));
                            purchaseprice = NumUtil.div(prodVO.getVatp(), 100);;
                            prodVO.setActcode(prizeEntity.getActcode() + "");
                        } else {
                            prodVO.setSdate(sdate);
                            prodVO.setEdate(edate);
                            int initStock = RedisStockUtil.getActInitStock(prodVO.getSku(), prizeEntity.getActcode());
                            int surplusStock = RedisStockUtil.getActStockBySkuAndActno(prodVO.getSku(), prizeEntity.getActcode());
                            prodVO.setActinitstock(initStock);
                            prodVO.setSurplusstock(surplusStock);
                            prodVO.setBuynum(initStock - surplusStock);
                            prodVO.setActprize(prizeEntity.getMinactprize());
                        }
                    }
                }
            }

//            else {
//                ProdPriceEntity prizeEntity = ProdActPriceUtil.getActIntervalPrizeBySku(prodVO.getSku(), prodVO.getVatp());
//                if (prizeEntity != null) {
//                    prodVO.setMinprize(prizeEntity.getMinactprize());
//                    prodVO.setMaxprize(prizeEntity.getMaxactprize());
//                    prodVO.setActcode(prizeEntity.getActcode() + "");
//                    int surplusStock = RedisStockUtil.getActStockBySkuAndActno(prodVO.getSku(), prizeEntity.getActcode());
//                    // 代表值存在一个活动
//                    if (prizeEntity.getActcode() > 0 && bits.size() == 1) {
//                        List<String[]> times = ProdActPriceUtil.getTimesByActcode(prizeEntity.getActcode());
//                        GetEffectiveTimeByActCode getEffectiveTimeByActCode = new GetEffectiveTimeByActCode(times).invoke();
//                        String sdate = getEffectiveTimeByActCode.getSdate();
//                        String edate = getEffectiveTimeByActCode.getEdate();
//
//                        if (StringUtils.isEmpty(sdate) || StringUtils.isEmpty(edate) || surplusStock <= 0) { // // 表示搜索的商品不在活动时间内
//                            prodVO.setRulestatus(0);
//                            prodVO.setActprod(false);
//                            prodVO.setMutiact(false);
//                        }
//                    } else if (prizeEntity.getActcode() == 0) { // 没有活动
//                        prodVO.setRulestatus(0);
//                        prodVO.setActprod(false);
//                        prodVO.setMutiact(false);
//                    }
//
//                }
//            }
        }

        //设置优惠价格毛利润
        prodVO.setGrossProfit(NumUtil.div(prodVO.getRrp(), 100),purchaseprice);
        if (context.isAnonymous()) {//无权限价格不可见
            prodVO.setVatp(-1);
            prodVO.setMp(-1);
            prodVO.setRrp(-1);
            prodVO.setActprize(-1);
            prodVO.setPurchaseprice(-1);
        } else {
            int controlCode = context.getUserSession() != null ? context.getUserSession().comp.controlCode : 0;
            if ((prodVO.getConsell() & controlCode) == prodVO.getConsell()) {
                prodVO.setVatp(NumUtil.div(prodVO.getVatp(), 100));
                prodVO.setMp(NumUtil.div(prodVO.getMp(), 100));
                prodVO.setRrp(NumUtil.div(prodVO.getRrp(), 100));
                prodVO.setPurchaseprice(purchaseprice);
            } else {//控销商品未签约价格不可见
                prodVO.setVatp(-2);
                prodVO.setMp(-2);
                prodVO.setRrp(-2);
                prodVO.setActprize(-2);
                prodVO.setPurchaseprice(-2);
            }
        }

        try {
            DictStore.translate(prodVO);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean assemblySpecActProd(List<Object[]> list) {
        boolean isAll = false;
        if(list != null && list.size() > 0){
            List<Object[]> newList = new ArrayList<>();
            Long gCode = Long.parseLong(list.get(0)[1].toString());

            if (gCode == 0) {
                List<Object[]> prodList = BASE_DAO.queryNative(QUERY_PROD);
                for (Object[] objects : prodList) {
                    Long sku = Long.parseLong(objects[0].toString());

                    newList.add(new Object[]{list.get(0)[0], sku, list.get(0)[2], list.get(0)[3], list.get(0)[4], list.get(0)[5]});
                }
                isAll = true;
            } else {
                for (Object[] aa : list) {
                    String gc = aa[1].toString();
                    if (gc.length() < 14) {
                        List<Object[]> prodList = BASE_DAO.queryNative(QUERY_PROD + " and spu like CONCAT('_', ?,'%')", new Object[]{gc});
                        for (Object[] obj : prodList) {
                            Long sku = Long.parseLong(obj[0].toString());

                            newList.add(new Object[]{aa[0], sku, aa[2], aa[3], aa[4], aa[5]});
                        }
                    }
                }
            }

            list.addAll(newList);
        }

        return isAll;
    }

    public static class GetEffectiveTimeByActCode {
        private List<String[]> times;
        private String sdate;
        private String edate;

        public GetEffectiveTimeByActCode(List<String[]> times) {
            this.times = times;
        }

        public String getSdate() {
            return sdate;
        }

        public String getEdate() {
            return edate;
        }

        public GetEffectiveTimeByActCode invoke() {
            if (times == null || times.size() <= 0) {
                sdate = "00:00:00";
                edate = "23:59:59";
            }
            if (times != null && times.size() > 0) {
                for (String[] time : times) {
                    if (TimeUtils.isEffectiveTime(time[0], time[1])) {
                        sdate = time[0];
                        edate = time[1];
                    }
                }
            }
            return this;
        }
    }

    /**
     * @接口摘要 查询商品评价
     * @业务场景 商品详情页评价查询
     * @传参类型 json
     * @传参列表 {sku: 商品sku}
     * @返回列表 AppriseVO对象数组
     */
    @UserPermission(ignore = true)
    public Result getGoodsApprise(AppContext appContext) {
        Result result = new Result();
        LocalDateTime localDateTime = LocalDateTime.now();
        String json = appContext.param.json;
        Page page = new Page();
        page.pageIndex = appContext.param.pageIndex;
        page.pageSize = appContext.param.pageNumber;
        PageHolder pageHolder = new PageHolder(page);
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        long sku = jsonObject.get("sku").getAsLong();
        String selectSQL = "select unqid,orderno,level,descmatch,logisticssrv,"
                + "content,createtdate,createtime,cstatus,compid from {{?"
                + DSMConst.TD_TRAN_APPRAISE + "}} where cstatus&1=0 and sku=" + sku + " order by oid desc";
        List<Object[]> queryResult = BASE_DAO.queryNativeSharding(0, localDateTime.getYear(),
                pageHolder, page, selectSQL);
        AppriseVO[] appriseVOS = new AppriseVO[queryResult.size()];
        BASE_DAO.convToEntity(queryResult, appriseVOS, AppriseVO.class);
        for (AppriseVO appriseVO : appriseVOS) {
            String compStr = RedisUtil.getStringProvide().get(appriseVO.getCompid() + "");
            //storeName
            StoreBasicInfo storeBasicInfo = GsonUtils.jsonToJavaBean(compStr, StoreBasicInfo.class);
            if (storeBasicInfo != null) {
                appriseVO.setCompName(storeBasicInfo.storeName);//暂无接口。。。。
            }
            double compEval = BigDecimal.valueOf((appriseVO.getLevel()
                    + appriseVO.getDescmatch() + appriseVO.getLogisticssrv()) / 3.0).setScale(1, BigDecimal.ROUND_CEILING).doubleValue();
            appriseVO.setCompEval(compEval);
        }
        return result.setQuery(appriseVOS, pageHolder);
    }

    /**
     * @接口摘要 订单评价商品接口
     * @业务场景
     * @传参类型 json
     * @传参列表 {orderno: 订单号 compid: 企业码 appriseArr: 评价数组[见AppriseVO.class]}
     * @返回列表 200成功 -1 失败
     */
    @UserPermission(ignore = true)
    public int insertApprise(AppContext appContext) {
        Gson gson = new Gson();
        JsonParser jsonParser = new JsonParser();
        String json = appContext.param.arrays[0];
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String orderNo = jsonObject.get("orderno").getAsString();
        int compid = jsonObject.get("compid").getAsInt();
        JsonArray appriseArr = jsonObject.get("appriseArr").getAsJsonArray();
        LocalDateTime localDateTime = LocalDateTime.now();
        List<Object[]> params = new ArrayList<>();
        for (int i = 0; i < appriseArr.size(); i++) {
            AppriseVO appriseVO = gson.fromJson(appriseArr.get(i).toString(), AppriseVO.class);
            params.add(new Object[]{GenIdUtil.getUnqId(), orderNo, appriseVO.getLevel(), appriseVO.getDescmatch(),
                    appriseVO.getLogisticssrv(), appriseVO.getContent(), compid, appriseVO.getSku()});
        }
        return !ModelUtil.updateTransEmpty(BASE_DAO.updateBatchNativeSharding(0, localDateTime.getYear(),
                INSERT_APPRISE_SQL, params, params.size())) ? 1 : 0;
    }

}
