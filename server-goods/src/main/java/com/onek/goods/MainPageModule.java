package com.onek.goods;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.onek.annotation.UserPermission;
import com.onek.calculate.auth.QualJudge;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.goods.entities.ProdVO;
import com.onek.goods.mainpagebean.Attr;
import com.onek.goods.mainpagebean.UiElement;
import com.onek.goods.util.ProdActPriceUtil;
import com.onek.goods.util.ProdESUtil;
import com.onek.server.infimp.IceDebug;
import com.onek.util.dict.DictStore;
import com.onek.util.fs.FileServerUtils;
import com.onek.util.stock.RedisStockUtil;
import constant.DSMConst;
import dao.BaseDAO;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import util.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @Author: leeping
 * @Date: 2019/6/28 9:40
 * @服务名 goodsServer
 */
public class MainPageModule {

    private static final BaseDAO BASE_DAO = BaseDAO.getBaseDAO();

    private static final String SELECT_ACT_SQL = "select unqid,brulecode,qualcode,qualvalue from {{?"
            + DSMConst.TD_PROM_ACT + "}}  where a.cstatus&1=0 and a.cstatus&2048>0 ";

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


    @UserPermission(ignore = true)
    public void getDataSource(AppContext appContext) {
        dataSource(1, true, 1, 5, true);
    }

    private static long getGroupCount(long actCode) {
        List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNative(COUNT_GROUP_NUM, actCode);
        return Long.parseLong(String.valueOf(queryResult.get(0)[0]));
    }

    /**
     * 通过活动码 获取 活动属性 及 商品信息
     **/
    private static Attr dataSource(long bRuleCodes, boolean isQuery, int pageIndex, int pageNumber, boolean isAnonymous){
        Attr attr = new Attr();
        String ruleCodeStr = getCodeStr(bRuleCodes);
        if (ruleCodeStr != null){
            List<Object[]> queryResult = BASE_DAO.queryNative(SELECT_ACT_SQL + " and a.brulecode in(" + ruleCodeStr + ")");
            if (queryResult == null || queryResult.isEmpty()) return attr;
            if (!QualJudge.hasPermission(compid, qualcode, qualvalue)) {
                return attr;
            }
            queryResult.forEach(qResult -> {
                long actCode = Long.parseLong(String.valueOf(qResult[0]));
                int bRuleCode = Integer.parseInt(String.valueOf(qResult[1]));
                boolean isEffect = theActInProgress(actCode);
                JsonObject actObj = new JsonObject();
                if ((bRuleCode == 1113 || bRuleCode == 1133)  && isEffect) {//秒杀团购进行中
                    String[] times = getTimesEff(actCode);
                    actObj.addProperty("actcode", actCode);
                    actObj.addProperty("sdate", times[0]);
                    actObj.addProperty("edate", times[1]);
                    actObj.addProperty("now", TimeUtils.date_yMd_Hms_2String(new Date()));
                    if (bRuleCode == 1133) {//团购
                        actObj.addProperty("ladoffArray", ladOffArray);
                        actObj.addProperty("currNums",getGroupCount(actCode));
                    }
                    attr.actObj = actObj;
                } else {

                }
            });
            if (isQuery) {
                //获取活动下的商品
                getActGoods(attr, pageIndex, pageNumber, isAnonymous);
            }

        }
        return attr;
    }


    private static String getCodeStr(long ruleCode) {
        StringBuilder codeSB = new StringBuilder();
        //拆
        List<Long> codeList = MathUtil.spiltNumToBit(ruleCode);
        codeList.forEach(code -> codeSB.append(code).append(","));
        if (codeSB.toString().contains(",")){
            return codeSB.toString().substring(0, codeSB.toString().length() - 1);
        }
        return null;
    }


    private static void getActGoods(Attr attr, int pageIndex, int pageNumber, boolean isAnonymous) {
        Page page = new Page();
        page.pageIndex = pageIndex;
        page.pageSize = pageNumber;
        PageHolder pageHolder = new PageHolder(page);
        List<ProdVO> prodVOList = new ArrayList<>();
        List<Long> skuList = new ArrayList<>();
        String selectClassSQL = "select gcode from {{?" + DSMConst.TD_PROM_ASSDRUG + "}} a left join {{?"
                + DSMConst.TD_PROD_SKU + "}} s on s.sku LIKE CONCAT( '_', a.gcode, '%' ) where a.cstatus&1=0 "
                + " and length( gcode ) < 14 AND gcode > 0 and actcode=? ";
        String selectGoodsSQL = "select gcode from {{?" + DSMConst.TD_PROM_ASSDRUG + "}} a left join {{?"
                + DSMConst.TD_PROD_SKU + "}} s on s.sku = gcode where a.cstatus&1=0 "
                + " and length(gcode) = 14 and actcode=? ";
        String selectAllSQL = "select gcode from {{?" + DSMConst.TD_PROM_ASSDRUG + "}} a ,{{?"
                + DSMConst.TD_PROD_SKU + "}} s where cstatus&1=0 "
                + " and gcode=0 and actcode=? ";
        String sqlBuilder = "SELECT sku FROM (" + selectClassSQL + " UNION ALL " + selectGoodsSQL +
                " UNION ALL " + selectAllSQL + ") ua";
        List<Object[]> queryResult = BASE_DAO.queryNative(pageHolder, page, sqlBuilder,
                attr.actCode, attr.actCode, attr.actCode);
        if (queryResult == null || queryResult.isEmpty()) return ;
        queryResult.forEach(qResult -> skuList.add(Long.valueOf(String.valueOf(qResult[0]))));
        //ES数据组装
        SearchResponse response = ProdESUtil.searchProdBySpuList(skuList, "", 1, 100);
        if (response != null && response.getHits().totalHits > 0) {
            assembleData(isAnonymous, response, prodVOList);
        }
        attr.list = prodVOList;
        attr.page = pageHolder;
    }

    private static void assembleData(boolean isAnonymous, SearchResponse response, List<ProdVO> prodList) {
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
            if (isAnonymous) { // 有权限
                prodVO.setVatp(NumUtil.div(prodVO.getVatp(), 100));
                prodVO.setMp(NumUtil.div(prodVO.getMp(), 100));
                prodVO.setRrp(NumUtil.div(prodVO.getRrp(), 100));
            } else {
                prodVO.setVatp(-1);
                prodVO.setMp(-1);
                prodVO.setRrp(-1);
            }
            prodVO.setStore(RedisStockUtil.getStock(prodVO.getSku()));
            try {
                DictStore.translate(prodVO);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void assembleObjectFromEs(ProdVO prodVO, Map<String, Object> sourceMap, HashMap detail) {
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

            }
        } catch (Exception e) {
//             e.printStackTrace();
        }
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
    public Result pageInfo(){

        return null;
    }


    private static class Param{
        long identity; //活动叠加码
        int limit= -1;//楼层商品限制数
    }
    /**
     * @接口摘要 客户端首页展示
     * @业务场景
     * @传参类型 json
     * @传参列表 {不带参数-获取首页楼层元素,
     * identity=活动叠加码 + limit=楼层商品限制数 -> 根据指定活动码返回指定数量商品,
     * identity + 分页信息 -根据指定活动码/分页信息 查询商品/活动属性 ;}
     * @返回列表 MainPage/UiElement 对象
     */
    @IceDebug
    @UserPermission(ignore = true)
    public Result pageInfo(AppContext context){
        String json = context.param.json;
        Param param = GsonUtils.jsonToJavaBean(json,Param.class);
        if (param == null){
            Map<String, List<UiElement>> map = allElement();
            //获取全部UI元素数据
            return map.size() == 0 ? new Result().fail("没有主页元素信息,请配置界面"): new Result().success(map);
        }
        if (param.identity > 0 && param.limit > 0 ){
            //获取指定活动的商品信息
            return new Result().success(dataSource(param.identity,true,1,param.limit,!context.isAnonymous()));
        }
        if (param.identity > 0 && param.limit == -1){
            return new Result().success(dataSource(param.identity,true,context.param.pageIndex,context.param.pageNumber,!context.isAnonymous()));
        }
        return new Result().fail("参数异常");
    }
    //获取页面全部元素信息
    private Map<String, List<UiElement>> allElement() {
        Map<String,List<UiElement>> map = new HashMap<>();
        String sql = "SELECT uiname,uimodel,tempId,brulecode,imgPath,seq FROM tb_ui_page WHERE cstatus&1 = 0";
        List<Object[]> lines =  BaseDAO.getBaseDAO().queryNative(sql);
        if (lines.size() >0){
            for (Object[] rows : lines){
                UiElement el = new UiElement();
                el.name = StringUtils.obj2Str(rows[0]);
                el.module = StringUtils.obj2Str(rows[1]);
                el.template = StringUtils.checkObjectNull(rows[2],0);
                el.brulecode = StringUtils.checkObjectNull(rows[3],0L);
                el.img = StringUtils.obj2Str(rows[4]);
                el.index = StringUtils.checkObjectNull(rows[5],0);
                el.attr = dataSource(el.brulecode,false,0,0,false);
                if (map.containsKey(el.module)){
                    map.get(el.module).add(el);
                }else{
                    List<UiElement> list = new LinkedList<>();
                    list.add(el);
                    map.put(el.module,list);
                }
            }
        }
        return map;
    }


}
