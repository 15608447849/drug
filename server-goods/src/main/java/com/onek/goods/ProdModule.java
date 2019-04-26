package com.onek.goods;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.internal.LinkedTreeMap;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.context.UserSession;
import com.onek.entitys.Result;
import com.onek.goods.entities.MallFloorVO;
import com.onek.goods.entities.ProdVO;
import com.onek.goods.service.MallFloorImpl;
import com.onek.goods.service.PromTimeService;
import com.onek.goods.util.ProdActPriceUtil;
import com.onek.goods.util.ProdESUtil;
import com.onek.util.dict.DictStore;
import com.onek.util.fs.FileServerUtils;
import com.onek.util.prod.ProdPriceEntity;
import com.onek.util.stock.RedisStockUtil;
import constant.DSMConst;
import dao.BaseDAO;
import com.onek.util.IceRemoteUtil;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import redis.IRedisCache;
import redis.proxy.CacheProxyInstance;
import util.NumUtil;
import util.StringUtils;
import util.TimeUtils;

import java.util.*;

/**
 * 商城商品模块
 *
 * @author JiangWenGuang
 * @version 1.0
 * @since
 */
@SuppressWarnings({"unchecked"})
public class ProdModule {

    private static IRedisCache mallFloorProxy = (IRedisCache) CacheProxyInstance.createInstance(new MallFloorImpl());

    private static final BaseDAO BASE_DAO = BaseDAO.getBaseDAO();

    private static PromTimeService timeService = new PromTimeService();

    private static String RULE_CODE_ACT_PROD_SQL = "select a.unqid,d.gcode,d.actstock,d.limitnum,d.price from " +
            "{{?" + DSMConst.TD_PROM_ACT + "}} a, {{?" + DSMConst.TD_PROM_ASSDRUG + "}} d " +
            "where a.unqid = d.actcode " +
            "and a.brulecode = ? and d.cstatus&1 = 0 " +
            "and fun_prom_cycle(a.unqid, a.acttype, a.actcycle, ?, 1) > 0 " +
            "union all " +
            "select a.unqid,(select sku from {{?" + DSMConst.TD_PROD_SKU + "}} where cstatus&1 =0 and spu like CONCAT('_', d.gcode,'%')) gcode,d.actstock,d.limitnum,d.price from " +
            "{{?" + DSMConst.TD_PROM_ACT + "}} a, {{?" + DSMConst.TD_PROM_ASSDRUG + "}} d " +
            "where a.unqid = d.actcode " +
            "and a.brulecode = ? and d.cstatus&1 = 0 " +
            "and fun_prom_cycle(a.unqid, a.acttype, a.actcycle, ?, 1) > 0 " +
            "and length(d.gcode) < 14 and d.gcode !=0";

    private static String ACT_PROD_BY_ACTCODE_SQL = "select a.unqid,d.gcode,d.actstock,d.limitnum,d.price from " +
            "{{?" + DSMConst.TD_PROM_ACT + "}} a, {{?" + DSMConst.TD_PROM_ASSDRUG + "}} d " +
            "where a.unqid = d.actcode " +
            "and d.actcode = ? " +
            "and a.sdate <= CURRENT_DATE and CURRENT_DATE<= a.edate ";

    private static String NEWMEMBER_ACT_PROD_SQL = "select a.unqid,d.gcode,d.actstock,d.limitnum from " +
            "{{?" + DSMConst.TD_PROM_ACT + "}} a, {{?" + DSMConst.TD_PROM_ASSDRUG + "}} d " +
            " where a.unqid = d.actcode  " +
            "and a.cstatus&1 = 0 " +
            "and a.qualcode = 1 and a.qualvalue = 0 and fun_prom_cycle(a.unqid, a.acttype, a.actcycle, ?, 1) > 0";

    private static String EXEMPOST_ACT_PROD_SQL = "select a.unqid,d.gcode,d.actstock,d.limitnum from " +
            "{{?" + DSMConst.TD_PROM_ACT + "}} a, {{?" + DSMConst.TD_PROM_ASSDRUG + "}} d " +
            " where a.unqid = d.actcode  " +
            "and a.cstatus&1 = 0 " +
            "and brulecode like '112%' " +
            "and fun_prom_cycle(a.unqid, a.acttype, a.actcycle, ?, 1) > 0";

    private static String TEAM_BUY_LADOFF_SQL = "select ladamt,ladnum,offer from " +
            "{{?" + DSMConst.TD_PROM_RELA + "}} r, {{?" + DSMConst.TD_PROM_LADOFF + "}} l where r.ladid = l.unqid and l.offercode like '1133%' and r.actcode = ?";

    @UserPermission(ignore = true)
    public Result getMallFloorProd(AppContext appContext) {
//    public Result getMallFloorProd() {
        List<MallFloorVO> mallFloorVOList = (List<MallFloorVO>) mallFloorProxy.queryAll();
        return new Result().success(mallFloorVOList);
    }

    @UserPermission(ignore = true)
    public Result getNewMallFloor(AppContext appContext) {
        List<Integer> bb = new ArrayList() {{
            add(128);
            add(512);
        }};
        Set<Integer> result = new HashSet<>();
        NumUtil.perComAdd(256, bb, result);
        int pageIndex = appContext.param.pageIndex <= 0 ? 1 : appContext.param.pageIndex;
        int pageSize = appContext.param.pageNumber <= 0 ? 100 : appContext.param.pageNumber;

        List<ProdVO> newProdList = getFilterProdsCommon(result, "",2, pageIndex, pageSize);

        List<ProdVO> filterProdList = loadProd(newProdList, 256);

        return new Result().success(filterProdList);
    }

    @UserPermission(ignore = true)
    public Result getChooseForYouMallFloor(AppContext appContext) {
        List<Integer> bb1 = new ArrayList() {{
            add(218);
            add(256);
        }};
        Set<Integer> result1 = new HashSet<>();
        NumUtil.perComAdd(512, bb1, result1);
        int pageIndex = appContext.param.pageIndex <= 0 ? 1 : appContext.param.pageIndex;
        int pageSize = appContext.param.pageNumber <= 0 ? 100 : appContext.param.pageNumber;


        List<ProdVO> prodList = getFilterProdsCommon(result1,  "",1, pageIndex, pageSize);
        List<ProdVO> filterProdList = loadProd(prodList, 128);

        return new Result().success(filterProdList);
    }

    @UserPermission(ignore = true)
    public Result getHotMallFloor(AppContext appContext) {

        int pageIndex = appContext.param.pageIndex <= 0 ? 1 : appContext.param.pageIndex;
        int pageSize = appContext.param.pageNumber <= 0 ? 100 : appContext.param.pageNumber;

        List<ProdVO> hotProdList = getFilterProdsCommon(null,"", 1, pageIndex, pageSize);

        return new Result().success(hotProdList);
    }

    @UserPermission(ignore = true)
    public Result getBrandMallFloor(AppContext appContext) {

        int pageIndex = appContext.param.pageIndex <= 0 ? 1 : appContext.param.pageIndex;
        int pageSize = appContext.param.pageNumber <= 0 ? 100 : appContext.param.pageNumber;
        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        String keyword = (json.has("keyword") ? json.get("keyword").getAsString() : "").trim();

        SearchResponse response = ProdESUtil.searchProdHasBrand(keyword, pageIndex, pageSize);
        List<ProdVO> prodList = new ArrayList<>();
        assembleData(response, prodList);

        return new Result().success(prodList);
    }

    @UserPermission(ignore = true)
    public Result getNewMemberMallFloor(AppContext appContext) {

        String day = TimeUtils.date_Md_2String(new Date());
        List<Object[]> list = BASE_DAO.queryNative(NEWMEMBER_ACT_PROD_SQL, new Object[]{day});
        List<ProdVO> prodVOList = new ArrayList<>();
        if (list != null && list.size() > 0) {
            List<Long> skuList = new ArrayList<>();
            Map<Long, Integer> dataMap = new HashMap<>();
            for (Object[] objects : list) {
                Long gcode = Long.parseLong(objects[1].toString());
                int limitnum = Integer.parseInt(objects[3].toString());

                skuList.add(gcode);
                dataMap.put(gcode, limitnum);

            }

            SearchResponse response = ProdESUtil.searchProdBySpuList(skuList,"", 1, 100);

            if (response != null && response.getHits().totalHits > 0) {
                assembleData(response, prodVOList);
            }
            if (prodVOList != null && prodVOList.size() > 0) {
                for (ProdVO prodVO : prodVOList) {
                    prodVO.setBuynum(0);
                    prodVO.setStartnum(1);
                    prodVO.setActlimit(dataMap.get(prodVO.getSku()));
                }
            }
        }
        return new Result().success(prodVOList);
    }

    @UserPermission(ignore = true)
    public Result getExemPostMallFloor(AppContext appContext) {

        String mmdd = TimeUtils.date_Md_2String(new Date());
        List<Object[]> list = BASE_DAO.queryNative(EXEMPOST_ACT_PROD_SQL, new Object[]{mmdd});
        List<ProdVO> prodVOList = new ArrayList<>();
        if (list != null && list.size() > 0) {
            List<Long> skuList = new ArrayList<>();
            Map<Long, Integer> dataMap = new HashMap<>();
            for (Object[] objects : list) {
                Long gCode = Long.parseLong(objects[1].toString());
                int limitNum = Integer.parseInt(objects[3].toString());

                skuList.add(gCode);
                dataMap.put(gCode, limitNum);

            }

            SearchResponse response = ProdESUtil.searchProdBySpuList(skuList, "",1, 100);

            if (response != null && response.getHits().totalHits > 0) {
                assembleData(response, prodVOList);
            }
            if (prodVOList != null && prodVOList.size() > 0) {
                for (ProdVO prodVO : prodVOList) {
                    prodVO.setBuynum(0);
                    prodVO.setStartnum(1);
                    prodVO.setActlimit(dataMap.get(prodVO.getSku()));
                }
            }
        }
        return new Result().success(prodVOList);
    }

    @UserPermission(ignore = true)
    public Result getTeamBuyMallFloor(AppContext appContext) {

        String mmdd = TimeUtils.date_Md_2String(new Date());
        List<Object[]> list = BASE_DAO.queryNative(RULE_CODE_ACT_PROD_SQL, new Object[]{1133, mmdd, 1133, mmdd});
        List<ProdVO> prodVOList = new ArrayList<>();
        JSONObject result = new JSONObject();
        if (list != null && list.size() > 0) {
            List<Long> actCodeList = new ArrayList<>();
            List<Long> skuList = new ArrayList<>();

            Map<Long, Integer[]> dataMap = new HashMap<>();
            getActData(list, actCodeList, skuList, dataMap);

            long actCode = actCodeList.get(0);

            JSONArray ladOffArray = new JSONArray();
            int minOff = getMinOff(actCode, ladOffArray);

            SearchResponse response = ProdESUtil.searchProdBySpuList(skuList, "",1, 100);

            if (response != null && response.getHits().totalHits > 0) {
                assembleData(response, prodVOList);
            }
            if (prodVOList != null && prodVOList.size() > 0) {
                for (ProdVO prodVO : prodVOList) {
                    convertTeamBuyData(dataMap, actCode, minOff, prodVO);
                }
            }

            List<String[]> times = timeService.getTimesByActcode(actCode);

            GetEffectiveTimeByActCode getEffectiveTimeByActCode = new GetEffectiveTimeByActCode(times).invoke();
            String sdate = getEffectiveTimeByActCode.getSdate();
            String edate = getEffectiveTimeByActCode.getEdate();

            result.put("actcode", actCodeList.get(0));
            result.put("sdate", sdate);
            result.put("edate", edate);
            result.put("list", prodVOList);
            result.put("ladoffArray", ladOffArray);
            result.put("now", TimeUtils.date_yMd_Hms_2String(new Date()));
        }
        return new Result().success(result);
    }

    @UserPermission(ignore = true)
    public Result getSeckillMallFloor(AppContext appContext) {
        Result r = getDiscountMallFloor(appContext);
        return r;

    }

    @UserPermission(ignore = true)
    public Result getDiscountMallFloor(AppContext appContext) {

        String day = TimeUtils.date_Md_2String(new Date());
        List<Object[]> list = BASE_DAO.queryNative(RULE_CODE_ACT_PROD_SQL, new Object[]{1113, day, 1113, day});
        JSONObject result = new JSONObject();
        List<ProdVO> prodVOList = new ArrayList<>();
        if (list != null && list.size() > 0) {
            List<Long> actCodeList = new ArrayList<>();
            List<Long> skuList = new ArrayList<>();
            Map<Long, Integer[]> dataMap = new HashMap<>();

            getActData(list, actCodeList, skuList, dataMap);

            long actCode = actCodeList.get(0);

            SearchResponse response = ProdESUtil.searchProdBySpuList(skuList, "",1, 100);

            if (response != null && response.getHits().totalHits > 0) {
                assembleData(response, prodVOList);
            }
            if (prodVOList != null && prodVOList.size() > 0) {
                for (ProdVO prodVO : prodVOList) {
                    convertDiscountData(dataMap, actCode, prodVO);
                }
            }
            List<String[]> times = timeService.getTimesByActcode(actCodeList.get(0));

            GetEffectiveTimeByActCode getEffectiveTimeByActCode = new GetEffectiveTimeByActCode(times).invoke();
            String startDate = getEffectiveTimeByActCode.getSdate(), endDate = getEffectiveTimeByActCode.getEdate();

            result.put("actcode", actCodeList.get(0));
            result.put("sdate", startDate);
            result.put("edate", endDate);
            result.put("list", prodVOList);
            result.put("now", TimeUtils.date_yMd_Hms_2String(new Date()));
        }
        return new Result().success(result);

    }

    @UserPermission(ignore = true)
    public Result brandMallSearch(AppContext appContext) {

        int pageIndex = appContext.param.pageIndex <= 0 ? 1 : appContext.param.pageIndex;
        int pageSize = appContext.param.pageNumber <= 0 ? 100 : appContext.param.pageNumber;
        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        String keyword = (json.has("keyword") ? json.get("keyword").getAsString() : "").trim();

        SearchResponse response = ProdESUtil.searchProdHasBrand(keyword, pageIndex, pageSize);
        List<ProdVO> prodList = new ArrayList<>();
        assembleData(response, prodList);

        Result r = new Result();
        Page page = new Page();
        page.pageSize = appContext.param.pageNumber;
        page.pageIndex = appContext.param.pageIndex;
        page.totalItems = response != null && response.getHits() != null ? (int) response.getHits().totalHits : 0;
        PageHolder pageHolder = new PageHolder(page);
        pageHolder.value = page;
        return r.setQuery(prodList, pageHolder);

    }

    @UserPermission(ignore = true)
    public Result newProdSearch(AppContext appContext) {
        List<Integer> bb = new ArrayList() {{
            add(128);
            add(512);
        }};
        Set<Integer> result = new HashSet<>();
        NumUtil.perComAdd(256, bb, result);
        int pageIndex = appContext.param.pageIndex <= 0 ? 1 : appContext.param.pageIndex;
        int pageSize = appContext.param.pageNumber <= 0 ? 100 : appContext.param.pageNumber;
        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        String keyword = (json.has("keyword") ? json.get("keyword").getAsString() : "").trim();

        List<ProdVO> newProdList = getFilterProdsCommon(result, keyword, 2, pageIndex, pageSize);

        Result r = new Result();
        Page page = new Page();
        page.pageSize = appContext.param.pageNumber;
        page.pageIndex = appContext.param.pageIndex;
        page.totalItems = newProdList != null  ? (int) newProdList.size() : 0;
        PageHolder pageHolder = new PageHolder(page);
        pageHolder.value = page;
        return r.setQuery(newProdList, pageHolder);
    }

    @UserPermission(ignore = true)
    public Result hotProdSearch(AppContext appContext) {

        int pageIndex = appContext.param.pageIndex <= 0 ? 1 : appContext.param.pageIndex;
        int pageSize = appContext.param.pageNumber <= 0 ? 100 : appContext.param.pageNumber;

        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        String keyword = (json.has("keyword") ? json.get("keyword").getAsString() : "").trim();

        List<ProdVO> hotProdList = getFilterProdsCommon(null,keyword, 1, pageIndex, pageSize);

        Result r = new Result();
        Page page = new Page();
        page.pageSize = appContext.param.pageNumber;
        page.pageIndex = appContext.param.pageIndex;
        page.totalItems = hotProdList != null  ? (int) hotProdList.size() : 0;
        PageHolder pageHolder = new PageHolder(page);
        pageHolder.value = page;
        return r.setQuery(hotProdList, pageHolder);
    }

    @UserPermission(ignore = true)
    public Result newMemberSearch(AppContext appContext) {

        int pageIndex = appContext.param.pageIndex <= 0 ? 1 : appContext.param.pageIndex;
        int pageSize = appContext.param.pageNumber <= 0 ? 100 : appContext.param.pageNumber;

        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        String keyword = (json.has("keyword") ? json.get("keyword").getAsString() : "").trim();

        String day = TimeUtils.date_Md_2String(new Date());
        List<Object[]> list = BASE_DAO.queryNative(NEWMEMBER_ACT_PROD_SQL, new Object[]{day});
        List<ProdVO> prodVOList = new ArrayList<>();
        if (list != null && list.size() > 0) {
            List<Long> skuList = new ArrayList<>();
            Map<Long, Integer> dataMap = new HashMap<>();
            for (Object[] objects : list) {
                Long gcode = Long.parseLong(objects[1].toString());
                int limitnum = Integer.parseInt(objects[3].toString());

                skuList.add(gcode);
                dataMap.put(gcode, limitnum);

            }

            SearchResponse response = ProdESUtil.searchProdBySpuList(skuList, keyword, pageIndex, pageSize);

            if (response != null && response.getHits().totalHits > 0) {
                assembleData(response, prodVOList);
            }
            if (prodVOList != null && prodVOList.size() > 0) {
                for (ProdVO prodVO : prodVOList) {
                    prodVO.setBuynum(0);
                    prodVO.setStartnum(1);
                    prodVO.setActlimit(dataMap.get(prodVO.getSku()));
                }
            }
        }
        Result r = new Result();
        Page page = new Page();
        page.pageSize = appContext.param.pageNumber;
        page.pageIndex = appContext.param.pageIndex;
        page.totalItems = prodVOList != null  ? (int) prodVOList.size() : 0;
        PageHolder pageHolder = new PageHolder(page);
        pageHolder.value = page;
        return r.setQuery(prodVOList, pageHolder);
    }

    @UserPermission(ignore = true)
    public Result chooseForYouSearch(AppContext appContext) {
        List<Integer> bb1 = new ArrayList() {{
            add(218);
            add(256);
        }};
        Set<Integer> result1 = new HashSet<>();
        NumUtil.perComAdd(512, bb1, result1);
        int pageIndex = appContext.param.pageIndex <= 0 ? 1 : appContext.param.pageIndex;
        int pageSize = appContext.param.pageNumber <= 0 ? 100 : appContext.param.pageNumber;

        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        String keyword = (json.has("keyword") ? json.get("keyword").getAsString() : "").trim();

        List<ProdVO> prodList = getFilterProdsCommon(result1,  keyword,1, pageIndex, pageSize);

        Result r = new Result();
        Page page = new Page();
        page.pageSize = appContext.param.pageNumber;
        page.pageIndex = appContext.param.pageIndex;
        page.totalItems = prodList != null  ? (int) prodList.size() : 0;
        PageHolder pageHolder = new PageHolder(page);
        pageHolder.value = page;
        return r.setQuery(prodList, pageHolder);
    }

    @UserPermission(ignore = true)
    public Result getAllDiscount(AppContext appContext) {
        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        Long actCode = json.has("actcode") ? json.get("actcode").getAsLong() : 0;
        String keyword = json.has("keyword") ? json.get("keyword").getAsString() : "";
        if (actCode == 0) {
            return new Result().fail("参数错误!");
        }
        JSONObject result = new JSONObject();
        List<ProdVO> prodVOList = new ArrayList<>();
        List<Long> skuList = new ArrayList<>();
        List<Long> actCodeList = new ArrayList<>();

        actCodeList.add(actCode);

        Map<Long, Integer[]> dataMap = new HashMap<>();

        List<Object[]> list = BASE_DAO.queryNative(ACT_PROD_BY_ACTCODE_SQL, new Object[]{actCode});

        getActData(list, actCodeList, skuList, dataMap);

        List<String[]> times = timeService.getTimesByActcode(actCode);

        GetEffectiveTimeByActCode getEffectiveTimeByActCode = new GetEffectiveTimeByActCode(times).invoke();
        String startDate = getEffectiveTimeByActCode.getSdate(), endDate = getEffectiveTimeByActCode.getEdate();

        JSONArray array = new JSONArray();
        if(times != null && times.size() > 0){
            for (String[] objects1 : times) {
                String s = objects1[0], d = objects1[1];
                JSONObject time = new JSONObject();
                time.put("sdate", s);
                time.put("edate", d);
                array.add(time);
            }
        }

        if (skuList.size() > 0) {
            SearchResponse response = ProdESUtil.searchProdBySpuListAndKeyword(skuList, keyword);

            if (response != null && response.getHits().totalHits > 0) {
                assembleData(response, prodVOList);
            }
            if (prodVOList != null && prodVOList.size() > 0) {
                for (ProdVO prodVO : prodVOList) {
                    convertDiscountData(dataMap, actCode, prodVO);
                }
            }
        }

        result.put("timeArray", array);
        result.put("sdate", startDate);
        result.put("edate", endDate);
        result.put("list", prodVOList);
        result.put("now", TimeUtils.date_yMd_Hms_2String(new Date()));
        result.put("actcode", actCode);

        return new Result().success(result);
    }

    @UserPermission(ignore = true)
    public Result getAllTeamBuy(AppContext appContext) {
        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        Long actCode = json.has("actcode") ? json.get("actcode").getAsLong() : 0;
        String keyword = json.has("keyword") ? json.get("keyword").getAsString() : "";

        if (actCode <= 0) {
            return new Result().fail("参数错误!");
        }

        JSONObject result = new JSONObject();
        List<ProdVO> prodVOList = new ArrayList<>();
        List<Long> skuList = new ArrayList<>();
        List<Long> actCodeList = new ArrayList<>();

        List<Object[]> list = BASE_DAO.queryNative(ACT_PROD_BY_ACTCODE_SQL, new Object[]{actCode});

        Map<Long, Integer[]> dataMap = new HashMap<>();

        getActData(list, actCodeList, skuList, dataMap);

        JSONArray ladoffArray = new JSONArray();
        int minOff = getMinOff(actCode, ladoffArray);
        List<String[]> times = timeService.getTimesByActcode(actCode);

        GetEffectiveTimeByActCode getEffectiveTimeByActCode = new GetEffectiveTimeByActCode(times).invoke();
        String startDate = getEffectiveTimeByActCode.getSdate();
        String endDate = getEffectiveTimeByActCode.getEdate();

        JSONArray array = new JSONArray();

        if(times != null && times.size() > 0){
            for (String[] objects1 : times) {
                String s = objects1[0], d = objects1[1];
                JSONObject time = new JSONObject();
                time.put("sdate", s);
                time.put("edate", d);
                array.add(time);
            }
        }

        if (skuList.size() > 0) {
            SearchResponse response = ProdESUtil.searchProdBySpuListAndKeyword(skuList, keyword);

            if (response != null && response.getHits().totalHits > 0) {
                assembleData(response, prodVOList);
            }
            if (prodVOList != null && prodVOList.size() > 0) {
                for (ProdVO prodVO : prodVOList) {
                    convertTeamBuyData(dataMap, actCode, minOff, prodVO);
                }
            }
        }

        result.put("timeArray", array);
        result.put("sdate", startDate);
        result.put("edate", endDate);
        result.put("list", prodVOList);
        result.put("now", TimeUtils.date_yMd_Hms_2String(new Date()));
        result.put("actcode", actCode);

        return new Result().success(result);
    }

    @UserPermission(ignore = true)
    public Result getProdDetailHotArea(AppContext appContext) {
        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        int spu = json.get("spu").getAsInt();
        List<Integer> bb1 = new ArrayList() {{
            add(256);
            add(512);
        }};
        Set<Integer> result1 = new HashSet<>();
        NumUtil.perComAdd(128, bb1, result1);

        SearchResponse searchResponse = ProdESUtil.searchProdWithHotAndSpu(result1, spu, 1, 10);
        List<ProdVO> prodVOList = new ArrayList<>();
        if (searchResponse == null || searchResponse.getHits().totalHits < 5) {
            if (searchResponse != null && searchResponse.getHits().totalHits > 0) {
                assembleData(searchResponse, prodVOList);
            }
            searchResponse = ProdESUtil.searchProdWithHotAndSpu(result1, spu, 1, 10);
            if (searchResponse != null && searchResponse.getHits().totalHits > 0) {
                assembleData(searchResponse, prodVOList);
            }
            if (prodVOList.size() < 5) {
                searchResponse = ProdESUtil.searchProdWithHotAndSpu(null, spu, 1, 10);
                assembleData(searchResponse, prodVOList);
            }
        } else {
            assembleData(searchResponse, prodVOList);
        }
        return new Result().success(prodVOList);
    }

    @UserPermission(ignore = false)
    public Result guessYouLikeArea(AppContext appContext) {

        UserSession userSession = appContext.getUserSession();
        List<ProdVO> prodList = new ArrayList<>();
        if (userSession != null && userSession.compId > 0) {
            List<String> footPrintMap = IceRemoteUtil.queryFootprint(userSession.compId);
            List<Long> skuList = new ArrayList<>();
            if (footPrintMap != null && footPrintMap.size() > 0) {
                for (String sku  : footPrintMap) {
                    try {
                        skuList.add(Long.parseLong(sku));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            SearchResponse response = ProdESUtil.searchProdBySpuList(skuList, "",1, 10);
            if (response == null || response.getHits().totalHits <= 5) {
                long totalHits = response != null ? response.getHits().totalHits : 0;
                if (totalHits > 0) {
                    assembleData(response, prodList);
                }
                List<Integer> bb1 = new ArrayList() {{
                    add(218);
                    add(256);
                }};
                Set<Integer> result1 = new HashSet<>();
                NumUtil.perComAdd(512, bb1, result1);

                response = ProdESUtil.searchProdWithStatusList(result1, "", 1, 1, 100);
            }

            assembleData(response, prodList);

        }
        return new Result().success(prodList);
    }

    @UserPermission(ignore = true)
    public Result getConditionByFullTextSearch(AppContext appContext) {
        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        String keyword = (json.has("keyword") ? json.get("keyword").getAsString() : "").trim();
        int spu = json.has("spu") ? json.get("spu").getAsInt() : 0;
        if (StringUtils.isEmpty(keyword) && spu <= 0) {
            return new Result().success(null);
        }
        List<String> specList = ProdESUtil.getConditions(keyword, spu, "spec");
        List<String> manunameList = ProdESUtil.getConditions(keyword, spu, "manuname");
        List<String> brandnameList = ProdESUtil.getConditions(keyword, spu, "brandname");
        Map<String, List<String>> map = new HashMap<>();
        map.put("specList", specList);
        map.put("manunameList", manunameList);
        map.put("brandnameList", brandnameList);
        return new Result().success(map);
    }

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
                convertSearchData(prodList, searchHit);
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

    @UserPermission(ignore = true)
    public Result fullTextsearchProd(AppContext appContext) {
        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        String keyword = (json.has("keyword") ? json.get("keyword").getAsString() : "").trim();
        JsonArray specArray = json.get("specArray").getAsJsonArray();
        JsonArray manuArray = json.get("manuArray").getAsJsonArray();
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

        Result r = new Result();
        SearchResponse response = ProdESUtil.searchProd(keyword, specList, manunoList, appContext.param.pageIndex, appContext.param.pageNumber);
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

    private static List<ProdVO> loadProd(List<ProdVO> prodList, int type) {
        List<ProdVO> newProdList = new ArrayList<>();
        for (ProdVO prodVO : prodList) {
            if ((prodVO.getCstatus() & type) > 0) {
                newProdList.add(prodVO);
            }
        }
        if (newProdList.size() <= 20) {
            int i = newProdList.size();
            for (ProdVO prodVO : prodList) {
                if (i++ <= 20) {
                    newProdList.add(prodVO);
                }
            }
        }
        return newProdList;
    }

    private static List<ProdVO> getFilterProdsCommon(Set<Integer> result, String keyword, int sort,int pageNum, int pageSize) {
        SearchResponse response = ProdESUtil.searchProdWithStatusList(result, keyword, sort, pageNum, pageSize);
        List<ProdVO> prodList = new ArrayList<>();
        if (response != null && response.getHits() != null && response.getHits().totalHits > 0) {
            SearchHits hits = response.getHits();
            if (hits.totalHits > 0) {
                assembleData(response, prodList);
            }
        }

        return prodList;
    }


    private static List<ProdVO> getFilterProds(Set<Integer> result, int sort) {
        SearchResponse response = ProdESUtil.searchProdWithStatusList(result, "", sort, 1, 100);
        List<ProdVO> prodList = new ArrayList<>();
        if (response == null || response.getHits().totalHits <= 10) {
            SearchHits hits = response.getHits();
            if (hits.totalHits > 0) {
                assembleData(response, prodList);
            }
            response = ProdESUtil.searchProdWithStatusList(null,"", sort, 1, 100);
        }

        assembleData(response, prodList);
        return prodList;
    }

    private static void assembleData(SearchResponse response, List<ProdVO> prodList) {
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
            prodVO.setVatp(NumUtil.div(prodVO.getVatp(), 100));
            prodVO.setMp(NumUtil.div(prodVO.getMp(), 100));
            prodVO.setRrp(NumUtil.div(prodVO.getRrp(), 100));
            prodVO.setStore(RedisStockUtil.getStock(prodVO.getSku()));
            try{
                DictStore.translate(prodVO);
            }catch(Exception e){
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
                prodVO.setBrandNo(detail.get("brandNo") != null ? Long.parseLong(detail.get("brandNo").toString()) : 0);
                prodVO.setBrandName(detail.get("brandName") != null ? detail.get("brandName").toString() : "");
                prodVO.setManuNo(detail.get("manuNo") != null ? Long.parseLong(detail.get("manuNo").toString()) : 0);
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
            // e.printStackTrace();
        }
    }

    /**
     * 得到活动数据
     *
     * @param list        活动数据列表
     * @param actCodeList 活动码列表
     * @param skuList     存放的sku列表
     * @param dataMap     存放其他信息列表
     */
    private void getActData(List<Object[]> list, List<Long> actCodeList, List<Long> skuList, Map<Long, Integer[]> dataMap) {
        for (Object[] objects : list) {
            Long actCode = Long.parseLong(objects[0].toString());
            Long gCode = Long.parseLong(objects[1].toString());
            int actStock = Integer.parseInt(objects[2].toString());
            int limitNum = Integer.parseInt(objects[3].toString());
            int prize = Integer.parseInt(objects[4].toString());

            skuList.add(gCode);
            if (!actCodeList.contains(actCode)) {
                actCodeList.add(actCode);
            }
            if (actCodeList.size() > 1) {
                break;
            }
            dataMap.put(gCode, new Integer[]{limitNum, actStock, prize});

        }
    }

    /**
     * 获取团购最高优惠
     *
     * @param actCode     活动码
     * @param ladOffArray 存放优惠阶梯
     * @return
     */
    private static int getMinOff(long actCode, JSONArray ladOffArray) {

        List<Object[]> ladOffList = BASE_DAO.queryNative(TEAM_BUY_LADOFF_SQL, new Object[]{actCode});
        int minOff = 100;
        if (ladOffList != null && ladOffList.size() > 0) {
            int i = 0;
            for (Object[] objects : ladOffList) {
                int amt = Integer.parseInt(objects[0].toString());
                int num = Integer.parseInt(objects[1].toString());
                int offer = Integer.parseInt(objects[2].toString()) / 100;
                if (i == 0) {
                    minOff = offer;
                }
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("ladamt", amt);
                jsonObject.put("ladnum", num);
                jsonObject.put("offer", offer);
                ladOffArray.add(jsonObject);
                i++;
            }
        }
        return minOff;
    }

    /**
     * 组装团购数据
     *
     * @param dataMap
     * @param actCode
     * @param minOff
     * @param prodVO
     */
    private static void convertTeamBuyData(Map<Long, Integer[]> dataMap, long actCode, int minOff, ProdVO prodVO) {
        int initStock = RedisStockUtil.getActInitStock(prodVO.getSku(), actCode);
        int surplusStock = RedisStockUtil.getActStockBySkuAndActno(prodVO.getSku(), actCode);
        prodVO.setBuynum(initStock - surplusStock);
        prodVO.setStartnum(prodVO.getMedpacknum());
        prodVO.setActlimit(dataMap.containsKey(prodVO.getSku()) ? dataMap.get(prodVO.getSku())[0]: 0);
        prodVO.setStore(RedisStockUtil.getStock(prodVO.getSku()));
        prodVO.setActinitstock(initStock);
        prodVO.setSurplusstock(surplusStock);
        prodVO.setActprize(NumUtil.roundup(NumUtil.div(prodVO.getVatp() * minOff, 100)));
        if (prodVO.getRulestatus() > 0) prodVO.setActprod(true);
    }

    /**
     * 组装秒杀数据
     *
     * @param dataMap
     * @param actCode
     * @param prodVO
     */
    private static void convertDiscountData(Map<Long, Integer[]> dataMap, long actCode, ProdVO prodVO) {
        int initStock = RedisStockUtil.getActInitStock(prodVO.getSku(), actCode);
        int surplusStock = RedisStockUtil.getActStockBySkuAndActno(prodVO.getSku(), actCode);
        prodVO.setBuynum(initStock - surplusStock);
        prodVO.setStartnum(prodVO.getMedpacknum());
        prodVO.setActlimit(dataMap.containsKey(prodVO.getSku()) ? dataMap.get(prodVO.getSku())[0]: 0);
        prodVO.setStore(RedisStockUtil.getStock(prodVO.getSku()));
        prodVO.setActinitstock(initStock);
        prodVO.setSurplusstock(surplusStock);

        prodVO.setActprize(NumUtil.div(dataMap.get(prodVO.getSku())[2], 100));
        if (prodVO.getRulestatus() > 0) prodVO.setActprod(true);
    }

    /**
     * 组装搜索的数据
     *
     * @param prodList
     * @param searchHit
     */
    private static void convertSearchData(List<ProdVO> prodList, SearchHit searchHit) {
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
        prodVO.setVatp(NumUtil.div(prodVO.getVatp(), 100));
        prodVO.setMp(NumUtil.div(prodVO.getMp(), 100));
        prodVO.setRrp(NumUtil.div(prodVO.getRrp(), 100));

        // 表示存在活动
        if (ruleStatus > 0) {
            prodVO.setActprod(true);
            List<Integer> bits = NumUtil.getNonZeroBits(ruleStatus);
            if (bits.size() > 1) {
                prodVO.setMutiact(true);
            }

            ProdPriceEntity prizeEntity = ProdActPriceUtil.getActIntervalPrizeBySku(prodVO.getSku(), prodVO.getVatp());
            if (prizeEntity != null) {
                prodVO.setMinprize(prizeEntity.getMinactprize());
                prodVO.setMaxprize(prizeEntity.getMaxactprize());
                prodVO.setActcode(prizeEntity.getActcode());
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
                        prodVO.setMinprize(prodVO.getVatp());
                        prodVO.setMaxprize(prodVO.getVatp());
                        prodVO.setActcode(prizeEntity.getActcode());
                    } else {
                        prodVO.setSdate(sdate);
                        prodVO.setEdate(edate);
                        int initStock = RedisStockUtil.getActInitStock(prodVO.getSku(), prizeEntity.getActcode());
                        int surplusStock = RedisStockUtil.getActStockBySkuAndActno(prodVO.getSku(), prizeEntity.getActcode());
                        prodVO.setActinitstock(initStock);
                        prodVO.setSurplusstock(surplusStock);
                        prodVO.setBuynum(initStock - surplusStock);
                    }
                }
            }
        }
        try {
            DictStore.translate(prodVO);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static class GetEffectiveTimeByActCode {
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

}
