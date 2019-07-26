package com.onek.goods;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.calculate.auth.QualJudge;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.goods.entities.ProdBrandVO;
import com.onek.goods.entities.ProdVO;
import com.onek.goods.mainpagebean.Attr;
import com.onek.goods.mainpagebean.UiElement;
import com.onek.goods.util.ProdActPriceUtil;
import com.onek.goods.util.ProdESUtil;
import com.onek.server.infimp.IceDebug;
import com.onek.util.FileServerUtils;
import com.onek.util.IceRemoteUtil;
import com.onek.util.dict.DictStore;
import com.onek.util.stock.RedisStockUtil;
import constant.DSMConst;
import dao.BaseDAO;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHits;
import org.hyrdpf.util.LogUtil;
import redis.util.RedisUtil;
import util.*;

import java.math.BigDecimal;
import java.util.*;

import static constant.DSMConst.TB_UI_PAGE;

/**
 * @Author: leeping
 * @Date: 2019/6/28 9:40
 * @服务名 goodsServer
 */
@SuppressWarnings({"unchecked"})
public class MainPageModule {

    private static final BaseDAO BASE_DAO = BaseDAO.getBaseDAO();

    private static final String SELECT_ACT_SQL = "select unqid,brulecode,qualcode,qualvalue,cpriority from {{?"
            + DSMConst.TD_PROM_ACT + "}}  where cstatus&1=0 and cstatus&2048>0 "
            + " and fun_prom_cycle(unqid, acttype, actcycle, ?, 1) > 0 ";

    private final static String COUNT_GROUP_NUM =
            " SELECT COUNT(DISTINCT compid) "
                    + " FROM {{?" + DSMConst.TD_PROM_GROUP + "}} g, "
                    + " {{?" + DSMConst.TD_PROM_ACT + "}} a, "
                    + " {{?" + DSMConst.TD_PROM_TIME + "}} t "
                    + " WHERE g.cstatus&1 = 0 AND a.cstatus&1 = 0 AND t.cstatus&1 = 0 "
                    + " AND g.actcode = a.unqid AND a.unqid = t.actcode "
                    + " AND g.joindate BETWEEN a.sdate AND a.edate "
                    + " AND g.jointime BETWEEN t.sdate AND t.edate "
                    + " AND g.actcode = ?";

    private static final String QUERY_PROD_BRAND = "  SELECT b.brandno, b.brandname  FROM {{?" + DSMConst.TD_PROD_BRAND +"}} b, {{?" + DSMConst.TD_PROD_SPU +"}} spu, {{?" + DSMConst.TD_PROD_SKU +"}} sku WHERE b.cstatus&1 = 0 " +
            "and spu.cstatus&1 = 0 and sku.cstatus&1 = 0 and sku.prodstatus = 1 and b.brandno = spu.brandno and spu.spu = sku.spu group by b.brandno, b.brandname";


    @UserPermission(ignore = true)
    public void getDataSource(AppContext appContext) {
        long sss = System.currentTimeMillis();
        long br = Long.parseLong(appContext.param.arrays[0]);
        Attr attr = dataSource(br, false, false,appContext.param.pageIndex, 6, appContext, "");
    }

    private static long getGroupCount(long actCode) {
        List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNative(COUNT_GROUP_NUM, actCode);
        return Long.parseLong(String.valueOf(queryResult.get(0)[0]));
    }

    /**
     * 通过活动码 获取 活动属性 及 商品信息
     **/
    private static Attr dataSource(long bRuleCodes, boolean isQuery, boolean onlySpecActivity,int pageIndex, int pageNumber,
                                   AppContext context, String jsonStr) {

//        LogUtil.getDefaultLogger().info("品牌参数111111111111---------0000000000000000000 " + jsonStr);
        Attr attr = new Attr();
        Page page = new Page();
        page.pageIndex = pageIndex <= 0 ? 1 : pageIndex;
        page.pageSize = pageNumber <= 0 ? 100 : pageNumber;
        int compId = context.getUserSession() != null ? context.getUserSession().compId : 0;
        String keyword = "", brandno = "", manuName = "";
        if (jsonStr != null && !jsonStr.isEmpty()) {
            JsonObject json = new JsonParser().parse(jsonStr).getAsJsonObject();
            keyword = (json.has("keyword") ? json.get("keyword").getAsString() : "").trim();
            brandno = (json.has("brandno") ? json.get("brandno").getAsString() : "").trim();
            manuName = (json.has("manuname") ? json.get("manuname").getAsString() : "").trim();
        }
        try {
            if (bRuleCodes < 0 && bRuleCodes >= -10 && !onlySpecActivity) {
                //非活动专区
                if (isQuery) {
                    List<ProdVO> prodVOS = getFloorByState(bRuleCodes, context.isAnonymous(), context.isSignControlAgree(),
                            page, attr, keyword, brandno, manuName);
                    if (prodVOS.size() > 0) {
                        remoteQueryShopCartNumBySku(compId, prodVOS, context.isAnonymous());
                        attr.list = prodVOS;
                        page.totalPageCount = page.totalItems%page.pageSize > 0 ?
                                page.totalItems/page.pageSize+1 :page.totalItems/page.pageSize;
                        attr.page = new PageHolder(page);
                    }
                }
            } else {
                getActProds(bRuleCodes, isQuery, attr, context, page, compId,
                        onlySpecActivity, new String[]{keyword, brandno, manuName});
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.getDefaultLogger().error(e);
            attr = null;
        }
//        if (onlyActivity && attr!=null && attr.list!=null && attr.list.size() > 0){
//            attr.list.removeIf(prodVO -> prodVO.getRulestatus() <= 0);
//        }
        return attr;
    }

    private static void getActProds(long bRuleCodes, boolean isQuery, Attr attr, AppContext context,
                                    Page page, int compId, boolean onlySpecActivity, String[] params) {
        //活动专区
        String ruleCodeStr;
        String SQL = SELECT_ACT_SQL + " and brulecode in(select brulecode from {{?" + DSMConst.TD_PROM_RULE + "}} where cstatus&1=0 ";
        if (bRuleCodes == -11) {//新人专享
            SQL = SQL + " ) and qualcode = 1 and qualvalue = 0";
        } else {
            if (bRuleCodes != -4) {
                ruleCodeStr = getCodeStr(bRuleCodes);
                if (ruleCodeStr == null) return;
                SQL = SQL + " and rulecode in(" + ruleCodeStr + ")) ";
            } else {
                SQL = SQL + ")";
            }
        }
        String mmdd = TimeUtils.date_Md_2String(new Date());
        List<Object[]> queryResult = BASE_DAO.queryNative(SQL, mmdd);
        if (queryResult == null || queryResult.isEmpty()) return ;
        if (isQuery) {
            combatActData(attr,queryResult,context, page, compId, onlySpecActivity, params);
        }
    }

    /* *
     * @description 获取购物车数量
     * @params [prodVOS]
     * @return void
     * @exception
     * @author 11842
     * @time  2019/7/2 17:33
     * @version 1.1.1
     **/
    private static void remoteQueryShopCartNumBySku(int compId, List<ProdVO> prodVOS, boolean isAnonymous) {
        if (compId <= 0 || isAnonymous) return;
        StringBuilder skuSB = new StringBuilder();
        for (ProdVO prodVO : prodVOS) {
            skuSB.append(prodVO.getSku()).append(",");
        }
        if (skuSB.toString().contains(",")) {
            String skuStr = skuSB.toString().substring(0, skuSB.toString().length() - 1);
            String numArr = IceRemoteUtil.queryShopCartNumBySkus(compId, skuStr);
            if (numArr != null && !numArr.isEmpty() && !"null".equalsIgnoreCase(numArr)) {
                Map<Long, Integer> shopCartNum = new HashMap<>();
                JsonArray goodsArr = new JsonParser().parse(numArr).getAsJsonArray();
                goodsArr.forEach(goods -> {
                    shopCartNum.put(goods.getAsJsonObject().get("sku").getAsLong()
                            ,goods.getAsJsonObject().get("pnum").getAsInt());
                });
               prodVOS.forEach(prod -> {
                   if (shopCartNum.containsKey(prod.getSku())) {
                       prod.cart = shopCartNum.get(prod.getSku());
                   }
               });
            }
        }
    }

    /* *
     * @description 活动数据组装
     * @params [attr, queryResult, isQuery, context, page]
     * @return void
     * @exception
     * @author 11842
     * @time  2019/6/29 14:40
     * @version 1.1.1
     **/
    private static void combatActData(Attr attr, List<Object[]> queryResult,
                                      AppContext context,Page page, int compId, boolean onlySpecActivity,String[] params){
        String actCodeStr;
        StringBuilder actCodeSB = new StringBuilder();
        String[] otherArr = {String.valueOf(queryResult.get(0)[1]), "0", compId + ""};
        for (Object[] qResult : queryResult) {
            int qualcode = Integer.parseInt(String.valueOf(qResult[2]));
            long qualvalue = Integer.parseInt(String.valueOf(qResult[3]));
            if (QualJudge.hasPermission(compId, qualcode, qualvalue)) {
                long actCode = Long.parseLong(String.valueOf(qResult[0]));
                int bRuleCode = Integer.parseInt(String.valueOf(qResult[1]));
                actCodeSB.append(actCode).append(",");
                JsonObject actObj = new JsonObject();
                if (bRuleCode == 1113 || bRuleCode == 1133) {//秒杀团购进行中
                    attr.actCode = actCode;
                    String[] times = getTimesEff(actCode);
                    actObj.addProperty("actcode", actCode);
                    actObj.addProperty("sdate", times[0]);
                    actObj.addProperty("edate", times[1]);
                    actObj.addProperty("now", TimeUtils.date_yMd_Hms_2String(new Date()));
                    if (bRuleCode == 1133) {//团购
                        JsonArray ladOffArray = new JsonArray();
                        double minOff = getMinOff(actCode, ladOffArray);
                        otherArr = new String[]{String.valueOf(queryResult.get(0)[1]), minOff + "",  compId + ""};
                        actObj.add("ladoffArray", ladOffArray);
                        actObj.addProperty("currNums", getGroupCount(actCode));
                    }
                    attr.actObj = actObj;
                }
            }

        }
        if (actCodeSB.toString().contains(",")) {
            actCodeStr = actCodeSB.toString().substring(0, actCodeSB.toString().length() - 1);
            //获取活动下的商品
            getActGoods(attr, page, context.isAnonymous(), context.isSignControlAgree(),actCodeStr,
                    otherArr, onlySpecActivity, params);
        }
    }


    private static String[] getTimesEff(long actCode) {
        String selectSQL = "select sdate,edate from {{?" + DSMConst.TD_PROM_TIME + "}} " +
                "where cstatus&1=0 and actcode = ? and sdate<=CURRENT_TIME and edate>=CURRENT_TIME";
        List<Object[]> queryResult = BASE_DAO.queryNative(selectSQL, actCode);
        if (queryResult == null || queryResult.isEmpty()) {
            return new String[]{"00:00:00", "23:59:59"};
        }
        return new String[]{String.valueOf(queryResult.get(0)[0]), String.valueOf(queryResult.get(0)[1])};

    }

    /**
     * 获取团购最高优惠
     *
     * @param actCode     活动码
     * @param ladOffArray 存放优惠阶梯
     * @return
     */
    private static double getMinOff(long actCode, JsonArray ladOffArray) {
        String selectSQL = "select ladamt,ladnum,offer from " +
                "{{?" + DSMConst.TD_PROM_RELA + "}} r, {{?" + DSMConst.TD_PROM_LADOFF + "}} l where r.ladid = l.unqid " +
                "and l.offercode like '1133%' and r.actcode = ? and r.cstatus&1=0 ";
        List<Object[]> ladOffList = BASE_DAO.queryNative(selectSQL, actCode);
        double minOff = 100;
        if (ladOffList != null && ladOffList.size() > 0) {
            int i = 0;
            for (Object[] objects : ladOffList) {
                int amt = Integer.parseInt(objects[0].toString());
                int num = Integer.parseInt(objects[1].toString());
                double offer = MathUtil.exactDiv(Integer.parseInt(objects[2].toString()), 100.0).doubleValue();
                if (i == 0) {
                    minOff = offer;
                }
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("ladamt", amt);
                jsonObject.addProperty("ladnum", num);
                jsonObject.addProperty("offer", offer);
                ladOffArray.add(jsonObject);
                i++;
            }
        }
        return minOff;
    }

    private static String getCodeStr(long ruleCode) {
        StringBuilder codeSB = new StringBuilder();
        //拆
        List<Long> codeList = MathUtil.spiltNumToBit(ruleCode);
        codeList.forEach(code -> {
            codeSB.append(code).append(",");
        });
        if (codeSB.toString().contains(",")) {
            return codeSB.toString().substring(0, codeSB.toString().length() - 1);
        }
        return null;
    }


    private static void getActGoods(Attr attr, Page page, boolean isAnonymous,boolean isSign,
                                    String actCodeStr, String[] otherArr, boolean onlySpecActivity,String[] params) {
//        LogUtil.getDefaultLogger().info("参数111111111111---------0000000000000000000 " + Arrays.toString(params));
        PageHolder pageHolder = new PageHolder(page);
        List<ProdVO> prodVOList = new ArrayList<>();
        List<Long> skuList = new ArrayList<>();
        Map<Long, String[]> dataMap = new HashMap<>();
        SearchResponse response;
        List<Object[]> queryResult;
        String selectClassSQL ,selectGoodsSQL ,selectAllSQL, sqlBuilder ;
        if (onlySpecActivity) {//只差特殊活动（剔除全局活动）
            selectGoodsSQL = "select sku,actstock,limitnum,price,a.cstatus,actcode,gcode,brandno from {{?"
                    + DSMConst.TD_PROM_ASSDRUG + "}} a left join {{?"
                    + DSMConst.TD_PROD_SKU + "}} s on (s.sku LIKE CONCAT( '_', a.gcode, '%' ) or gcode=sku) left join {{?"
                    + DSMConst.TD_PROD_SPU + "}} p on s.spu=p.spu where a.cstatus&1=0 and s.cstatus&1=0 and p.cstatus&1=0 "
                    + " and gcode > 0 and prodstatus=1 and actcode in (" + actCodeStr + ") ";
            if (!StringUtils.isEmpty(params[2])) {
                sqlBuilder = "SELECT sku,max(actstock),limitnum,price,cstatus,actcode,gcode,brandno FROM ("
                         + selectGoodsSQL + ") ua WHERE brandno=? group by sku  " ;
                queryResult = BASE_DAO.queryNativeC(pageHolder, page, null, sqlBuilder, params[1]);
            } else {
                sqlBuilder = "SELECT sku,max(actstock),limitnum,price,cstatus,actcode,gcode,brandno FROM ("
                        + selectGoodsSQL + ") ua group by sku ";
                queryResult = BASE_DAO.queryNativeC(pageHolder, page, sqlBuilder);
            }
        } else {
            selectClassSQL = "select sku,actstock,limitnum,price,a.cstatus,actcode,gcode from {{?"
                    + DSMConst.TD_PROM_ASSDRUG + "}} a left join {{?"
                    + DSMConst.TD_PROD_SKU + "}} s on s.sku LIKE CONCAT( '_', a.gcode, '%' ) where a.cstatus&1=0 "
                    + " and length( gcode ) < 14 AND gcode > 0 and prodstatus=1 and actcode in (" + actCodeStr + ") ";
            selectGoodsSQL = "select sku,actstock,limitnum,price,a.cstatus,actcode,gcode from {{?"
                    + DSMConst.TD_PROM_ASSDRUG + "}} a left join {{?"
                    + DSMConst.TD_PROD_SKU + "}} s on s.sku = gcode where a.cstatus&1=0 "
                    + " and length(gcode) = 14 and prodstatus=1 and actcode in (" + actCodeStr + ") ";
           selectAllSQL = "select sku,actstock,limitnum,price,a.cstatus,actcode,gcode from {{?"
                    + DSMConst.TD_PROM_ASSDRUG + "}} a ,{{?"
                    + DSMConst.TD_PROD_SKU + "}} s where a.cstatus&1=0 "
                    + " and gcode=0 and prodstatus=1 and actcode in (" + actCodeStr + ") ";
            sqlBuilder = "SELECT sku,max(actstock),limitnum,price,cstatus,actcode,gcode FROM ("
                    + selectClassSQL + " UNION ALL " + selectGoodsSQL +
                    " UNION ALL " + selectAllSQL + ") ua group by sku";
            queryResult = BASE_DAO.queryNativeC(pageHolder, page, sqlBuilder);
        }
        if (queryResult == null || queryResult.isEmpty()) {
            return;
        }
        queryResult.forEach(qResult -> {
            skuList.add(Long.valueOf(String.valueOf(qResult[0])));
            dataMap.put(Long.valueOf(String.valueOf(qResult[0])),
                    new String[]{String.valueOf(qResult[1]), String.valueOf(qResult[2]),
                            String.valueOf(qResult[3]), String.valueOf(qResult[4]), String.valueOf(qResult[5])});
        });
        //ES数据组装
        if (onlySpecActivity) {
            if (!StringUtils.isEmpty(params[2])) {
                response = ProdESUtil.searchProdHasBrand(skuList, params[0], params[1], params[2], 1, 100);
            } else {
                attr.actObj = selectAllBarnd(skuList);
                return;
            }
        } else {
            response = ProdESUtil.searchProdBySpuList(skuList, params[0], 1, 100);
        }
        if (response != null && response.getHits().totalHits > 0) {
            assembleData(isAnonymous, isSign, response, prodVOList, dataMap, otherArr);
            page.totalItems =  response.getHits() != null ? (int) response.getHits().totalHits : 0;
        }
        if (prodVOList.size() > 0) {
            remoteQueryShopCartNumBySku(Integer.parseInt(otherArr[2]), prodVOList, isAnonymous);
            attr.list = prodVOList;
            attr.page = pageHolder;
        }
    }

    private static void assembleData(boolean isAnonymous, boolean isSign, SearchResponse response, List<ProdVO> prodList,
                                     Map<Long, String[]> dataMap, String[] otherArr) {
        if (response == null || response.getHits().totalHits <= 0) {
            return;
        }
        for (SearchHit searchHit : response.getHits()) {
            ProdVO prodVO = new ProdVO();
            Map<String, Object> sourceMap = searchHit.getSourceAsMap();

            HashMap detail = (HashMap) sourceMap.get("detail");
            assembleObjectFromEs(prodVO, sourceMap, detail, dataMap, otherArr);

            prodList.add(prodVO);
            prodVO.setImageUrl(
                    FileServerUtils.fileDownloadPrev()
                    + FileServerUtils.goodsFilePath(prodVO.getSpu(), prodVO.getSku())
                    +"/"+ prodVO.getSku()+"-200x200.jpg");
            int ruleStatus = ProdActPriceUtil.getRuleBySku(prodVO.getSku());
            prodVO.setRulestatus(ruleStatus);
            //商品参加活动描述
            prodVO.setLadOffDesc(ProdActPriceUtil.getLoadOffBySku(prodVO.getSku()));

            if (isAnonymous) {//无权限价格不可见
                prodVO.setVatp(-1);
                prodVO.setMp(-1);
                prodVO.setRrp(-1);
            } else {
                if (prodVO.getConsell() > 0 && !isSign) {//控销商品未签约价格不可见
                    prodVO.setVatp(-2);
                    prodVO.setMp(-2);
                    prodVO.setRrp(-2);
                } else {
                    prodVO.setVatp(NumUtil.div(prodVO.getVatp(), 100));
                    prodVO.setMp(NumUtil.div(prodVO.getMp(), 100));
                    prodVO.setRrp(NumUtil.div(prodVO.getRrp(), 100));
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

    private static void assembleObjectFromEs(ProdVO prodVO, Map<String, Object> sourceMap, HashMap detail,
                                             Map<Long, String[]> dataMap, String[] otherArr) {
        try {
            if (detail != null) {
                long sku = detail.get("sku") != null ? Long.parseLong(detail.get("sku").toString()) : 0;
                prodVO.setSpu(detail.get("spu") != null ? Long.parseLong(detail.get("spu").toString()) : 0);
                prodVO.setPopname(detail.get("popname") != null ? detail.get("popname").toString() : "");
                prodVO.setProdname(detail.get("prodname") != null ? detail.get("prodname").toString() : "");
                prodVO.setStandarNo(detail.get("standarNo") != null ? detail.get("standarNo").toString() : "");
                prodVO.setBrandNo(detail.get("brandNo") != null ? detail.get("brandNo").toString() : "");
                prodVO.setBrandName(detail.get("brandName") != null ? detail.get("brandName").toString() : "");
                prodVO.setManuNo(detail.get("manuNo") != null ? detail.get("manuNo").toString() : "");
                prodVO.setManuName(detail.get("manuName") != null ? detail.get("manuName").toString() : "");

                prodVO.setSku(sku);
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

                //设置毛利润
                prodVO.setGrossProfit(prodVO.getRrp(),prodVO.getVatp());

                if (dataMap != null && otherArr != null) {
                    int ruleCode = Integer.parseInt(otherArr[0]);
                    double minOff = Double.parseDouble(otherArr[1]);
                    //                int actstock =  dataMap.get(sku) != null ? Integer.parseInt(dataMap.get(sku)[0]): 0;
                    int limitNum = dataMap.get(sku) != null ? Integer.parseInt(dataMap.get(sku)[1]) : 0;
                    double price = dataMap.get(sku) != null ? Double.parseDouble(dataMap.get(sku)[2]) : 0;
                    int cstatus = dataMap.get(sku) != null ? Integer.parseInt(dataMap.get(sku)[3]) : 0;
                    long actCode = dataMap.get(sku) != null ? Long.parseLong(dataMap.get(sku)[4]) : 0;
                    int initStock = RedisStockUtil.getActInitStock(prodVO.getSku(), actCode);
                    int surplusStock = RedisStockUtil.getActStockBySkuAndActno(prodVO.getSku(), actCode);
                    prodVO.setBuynum(initStock - surplusStock);
                    prodVO.setStartnum(prodVO.getMedpacknum());
                    prodVO.setActlimit(dataMap.containsKey(prodVO.getSku()) ? limitNum : 0);
                    prodVO.setStore(RedisStockUtil.getStock(prodVO.getSku()));
                    prodVO.setActinitstock(initStock);
                    prodVO.setSurplusstock(surplusStock);
                    if (ruleCode == 1113) {
                        if ((cstatus & 512) > 0) {
                            double rate = MathUtil.exactDiv(price, 100F).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                            double actprice = MathUtil.exactMul(prodVO.getVatp(), rate).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                            prodVO.setActprize(actprice);
                        } else {
                            prodVO.setActprize(NumUtil.div(price, 100));
                        }
                    } else if (ruleCode == 1133) {
                        prodVO.setActprize(NumUtil.roundup(NumUtil.div(prodVO.getVatp() * minOff, 100)));
                    } else {
                        prodVO.setActprize(NumUtil.div(prodVO.getVatp(), 100));
                    }
                }

                if (prodVO.getRulestatus() > 0) prodVO.setActprod(true);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /* *
     * @description 非活动专区数据组装
     * @params [state, isAnonymous, page]
     * @return java.util.List<com.onek.goods.entities.ProdVO>
     * @exception
     * @author 11842
     * @time  2019/6/29 14:41
     * @version 1.1.1
     **/
    private static List<ProdVO> getFloorByState(long state, boolean isAnonymous, boolean isSign, Page page,
                                                 Attr attr,String keyword, String brandno,  String manuName) {

        Set<Integer> result = new HashSet<>();
        if (state == -1) { // 新品
            List<Integer> bb = new ArrayList() {{
                add(128);
                add(512);
                add(1024);
            }};
            NumUtil.perComAdd(256, bb, result);
            result.add(256);
        } else if (state == -2) { // 为你精选
            List<Integer> bb1 = new ArrayList() {{
                add(128);
                add(256);
                add(1024);
            }};
            NumUtil.perComAdd(512, bb1, result);
            result.add(512);
        } else if (state == -3) { // 名方
            List<Integer> bb1 = new ArrayList() {{
                add(128);
                add(256);
                add(512);
            }};
            NumUtil.perComAdd(1024, bb1, result);
            result.add(1024);
        } else {//品牌专区-4 //热销专区 -5 //控销专区 -6
            return getOtherMallFloor(page, isAnonymous, isSign, state, attr, keyword, brandno, manuName);
        }
        Map<String, Object> resultMap = getFilterProdsCommon(isAnonymous, isSign, result, keyword, 1, page);
        return (List<ProdVO>) resultMap.get("prodList");
    }




    //品牌-4 热销 -5
    private static List<ProdVO> getOtherMallFloor(Page page, boolean isAnonymous, boolean isSign,
                                                  long state, Attr attr, String keyword, String brandno, String manuName) {

        SearchResponse response = null;
        if (state == -4) {//品牌 - 多厂商
//           List<ProdBrandVO> prodBarnds = getBrandInfo();
            if (!StringUtils.isEmpty(manuName)){
//                LogUtil.getDefaultLogger().info("品牌-厂家-查询关键字: "+manuName );
                //厂家码
                response = ProdESUtil.searchProdHasBrand(null, keyword, brandno, manuName, page.pageIndex, page.pageSize);

            }else{
                attr.actObj = selectAllBarnd(null);
            }

        } else if (state == -5) { //热销
            response = ProdESUtil.searchHotProd(0, keyword, page.pageIndex, page.pageSize);
        } else {//控销专区
            response = ProdESUtil.searchHotProd(1, keyword, page.pageIndex, page.pageSize);
        }
        page.totalItems = response != null && response.getHits() != null ? (int) response.getHits().totalHits : 0;
        List<ProdVO> prodList = new ArrayList<>();
        assembleData(isAnonymous, isSign, response, prodList, null, null);
        return prodList;
    }

    private static class BarndManu{

        private static class Manu{
            long manuNo; // 厂商码
            String manuName;//厂商名

            private Manu(long manuNo, String manuName) {
                this.manuNo = manuNo;
                this.manuName = manuName;
            }
        }

        private long brandno; //品牌码
        private String brandname;//品牌名
        private List<Manu> manuList; //关联的厂商列表

        private BarndManu(long brandno, String brandname) {
            this.brandno = brandno;
            this.brandname = brandname;
            this.manuList = new ArrayList<>();
        }
        void add(long manuNo, String manuName){
            for (Manu m : manuList){
                if (m.manuNo == manuNo) return;
            }
           manuList.add(new Manu(manuNo,manuName));
        }
    }

    public static List<BarndManu> selectAllBarnd(List<Long> skuList) {

        List<BarndManu> brandMaNuList = new ArrayList<>();
        SearchResponse response = ProdESUtil.searchProdGroupByBrand(skuList,"", "", "");
        if (response != null && response.getHits().totalHits > 0) {
            Terms agg = response.getAggregations().get("agg");
            for (Terms.Bucket bucket : agg.getBuckets()) {
                long brandNo = Long.valueOf(bucket.getKey().toString());
                TopHits topHits = bucket.getAggregations().get("top");
                SearchHit[] maNuHits = topHits.getHits().getHits();
                String brandName = maNuHits[0].getSourceAsMap().get("brandname").toString();
                BarndManu barndManu = new BarndManu(brandNo, brandName);
                for (SearchHit hit : maNuHits) {
                    Map<String, Object> sMap = hit.getSourceAsMap();
                    long maNuNo = Long.valueOf(sMap.get("manuno").toString());
                    String maNuName = sMap.get("manuname").toString();
                    barndManu.add(maNuNo, maNuName);
                }
                brandMaNuList.add(barndManu);
            }
        }
        return brandMaNuList;
    }

    //获取所有品牌
    private static List<ProdBrandVO> getBrandInfo() {
        List<Object[]> queryResult = BASE_DAO.queryNative(QUERY_PROD_BRAND + " order by sku.sales desc");
        if (queryResult == null || queryResult.isEmpty()) return null;
        ProdBrandVO[] result = new ProdBrandVO[queryResult.size()];
        BASE_DAO.convToEntity(queryResult, result, ProdBrandVO.class, "brandno", "brandname");
        return Arrays.asList(result);
    }



    private static Map<String, Object> getFilterProdsCommon(boolean isAnonymous, boolean isSign, Set<Integer> result,
                                                            String keyword, int sort, Page page) {
        Map<String, Object> resultMap = new HashMap<>();
        SearchResponse response = ProdESUtil.searchProdWithStatusList(result, keyword, sort, page.pageIndex, page.pageSize);
        List<ProdVO> prodList = new ArrayList<>();
        if (response != null && response.getHits() != null && response.getHits().totalHits > 0) {
            SearchHits hits = response.getHits();
            if (hits.totalHits > 0) {
                assembleData(isAnonymous, isSign,response, prodList, null, null);
            }
            page.totalItems =  response.getHits() != null ? (int) response.getHits().totalHits : 0;
        }
        resultMap.put("response", response);
        resultMap.put("prodList", prodList);
        return resultMap;
    }


    /**
     * @接口摘要 客户端首页展示
     * @业务场景
     * @传参类型 json
     * @传参列表 {setup = 1-获取首页楼层  }
     * @返回列表
     */
    @IceDebug
    @UserPermission(ignore = true)
    public Result pageInfo() {

        return null;
    }

    private static class Param {
        long identity; //活动叠加码
        int limit = -1;//楼层商品限制数
        boolean onlyActivity;
        String jsonStr;//其他附加参数
    }

    private static final String MAIN_PAGE_JSON = "MAIN_PAGE_JSON";

    //获取页面全部元素信息
    private Map<String, List<UiElement>> allElement() {
        Map<String, List<UiElement>> map;
        String json = RedisUtil.getStringProvide().get(MAIN_PAGE_JSON);
        if (StringUtils.isEmpty(json)){
            map = new HashMap<>();
            try {
                String sql = "SELECT uiname,uimodel,uioption,codes,seq,attr,temp,extlink FROM {{?" + TB_UI_PAGE + "}} WHERE cstatus&1 = 0";
                List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(sql);
                if (lines.size() > 0) {
                    for (Object[] rows : lines) {

                        UiElement el = new UiElement();
                        el.name = StringUtils.obj2Str(rows[0]);
                        el.module = StringUtils.obj2Str(rows[1]);
                        el.option = StringUtils.checkObjectNull(rows[2], 0);
                        el.code = StringUtils.checkObjectNull(rows[3], 0L);

                        el.index = StringUtils.checkObjectNull(rows[4], 0);
                        el.route = StringUtils.obj2Str(rows[5]);
                        el.template = StringUtils.checkObjectNull(rows[6],0);
                        el.extLink = StringUtils.obj2Str(rows[7]);
                        genElementAttr(el);
                        if (map.containsKey(el.module)) {
                            map.get(el.module).add(el);
                        } else {
                            List<UiElement> list = new LinkedList<>();
                            list.add(el);
                            map.put(el.module, list);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (map.size() > 0) {
                RedisUtil.getStringProvide().set(MAIN_PAGE_JSON,GsonUtils.javaBeanToJson(map));
                RedisUtil.getStringProvide().expire(MAIN_PAGE_JSON, 5 * 60);
            }
        }else{
            map = GsonUtils.string2Map(json);
        }
        return map;
    }

    private void genElementAttr(UiElement el) {
        String imgPrev = FileServerUtils.fileDownloadPrev();
        //图片路径规则     首页对应图片 : 主目录 /image / 模块名_序号.png
        el.img = imgPrev + FileServerUtils.defaultHome()+"/image/"+el.module+"_"+el.index+".png";
        //详情页图片路径:       主目录/image/活动名.png
        el.dtlImg = imgPrev + FileServerUtils.defaultHome()+"/image/"+el.code+".png";
    }

    /**
     * @接口摘要 客户端首页展示
     * @业务场景
     * @传参类型 json
     * @传参列表 {不带参数-获取首页楼层元素,
     * identity=活动叠加码 + limit=楼层商品限制数 -> 根据指定活动码返回指定数量商品,
     * identity + 分页信息 -根据指定活动码/分页信息 查询商品/活动属性}
     * onlyActivity 只看活动商品
     * @返回列表 MainPage/UiElement 对象
     */
    @UserPermission(ignore = true)
    public Result pageInfo(AppContext context) {
        String json = context.param.json;
        Param param = GsonUtils.jsonToJavaBean(json, Param.class);
        if (param == null || param.identity==0 && param.limit == -1) {
            Map<String, List<UiElement>> map = allElement();
            //获取全部UI元素数据
            return map.size() == 0 ? new Result().fail("没有主页元素信息,请配置界面") : new Result().success(map);
        }else{
            if (param.limit > 0) {
                //获取指定活动的商品信息
                return new Result().success(dataSource(param.identity, true, param.onlyActivity,1, param.limit, context, param.jsonStr));
            }
            if (param.limit == -1) {
                return new Result().success(dataSource(param.identity, true, param.onlyActivity,context.param.pageIndex, context.param.pageNumber, context, param.jsonStr));
            }
        }
        return new Result().fail("参数异常");
    }

    @UserPermission(ignore = true)
    public int insert(AppContext appContext) {
        String sql = "insert into {{?" + DSMConst.TD_PROM_RULE +"}}1 "
                + " (`rulecode`, `brulecode`, `rulename`, `desc`, `cstatus`)"
                + " values(?,?,?,?,?)";
        return BaseDAO.getBaseDAO().updateNative(sql,1,1,"哈哈呵呵","dasdad",0);
    }

}
