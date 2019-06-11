package com.onek.goods;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.onek.annotation.UserPermission;
import com.onek.consts.ESConstant;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.goods.entities.BgProdVO;
import com.onek.goods.entities.BusScopeVo;
import com.onek.goods.util.CalculateUtil;
import com.onek.goods.util.ProdESUtil;
import com.onek.util.IceRemoteUtil;
import com.onek.util.SmsTempNo;
import com.onek.util.SmsUtil;
import com.onek.util.dict.DictStore;
import com.onek.util.prod.ProduceClassUtil;
import com.onek.util.stock.RedisStockUtil;
import constant.DSMConst;
import dao.BaseDAO;
import elasticsearch.ElasticSearchClientFactory;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.hyrdpf.util.LogUtil;
import redis.util.RedisUtil;
import util.MathUtil;
import util.StringUtils;
import util.TimeUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class BackgroundProdModule {
    private static final BaseDAO BASE_DAO = BaseDAO.getBaseDAO();
    private static final String PZWH = "^国药准字[H|B|S|T|F|J|Z]\\d{8}$";
//    private static final String ZCZBH = "^[\\u4e00-\\u9fa5]食药监械\\([准|进|许]\\)字\\d{4}第\\d{7}号$";

    private static final String GET_MAX_SPU =
            " SELECT SUBSTR(MAX(spu), 8, 3), COUNT(0)"
                    + " FROM {{?" + DSMConst.TD_PROD_SPU + "}} "
                    + " WHERE spu REGEXP ? ";

    private static final String CHECK_SAME_SPU =
            " SELECT spu "
                    + " FROM {{?" + DSMConst.TD_PROD_SPU + "}} "
                    + " WHERE popnameh = CRC32(?) AND popname = ? "
                    + " AND prodnameh = CRC32(?) AND prodname = ? "
                    + " AND standarnoh = CRC32(?) AND standarno = ? "
                    + " AND manuno = ? AND spu REGEXP ? ";

    private static final String INSERT_PROD_SPU =
            " INSERT INTO {{?" + DSMConst.TD_PROD_SPU + "}} "
                    + " (spu, popname, popnameh, prodname, prodnameh, "
                    + " standarno, standarnoh, brandno, manuno, rx, "
                    + " insurance, gspgms, gspsc, detail, busscope) "
                    + " SELECT ?, ?, CRC32(?), ?, CRC32(?), "
                    + " ?, CRC32(?), ?, ?, ?, "
                    + " ?, ?, ?, ?, ? "
                    + " FROM DUAL "
                    + " WHERE NOT EXISTS ( "
                    + " SELECT *"
                    + " FROM {{?" + DSMConst.TD_PROD_SPU + "}} "
                    + " WHERE spu = ? ) ";

    private static final String INSERT_PROD_SKU =
            " INSERT INTO {{?" + DSMConst.TD_PROD_SKU + "}} "
                    + " (spu, sku, vatp, mp, rrp, "
                    + " vaildsdate, vaildedate, "
                    + " prodsdate, prodedate, store, "
                    + " limits, wholenum, medpacknum, unit,"
                    + " ondate, ontime, spec, cstatus, expmonth) "
                    + " VALUES (?, ?, ?, ?, ?, "
                    + " STR_TO_DATE(?, '%Y-%m-%d'), STR_TO_DATE(?, '%Y-%m-%d'),"
                    + " STR_TO_DATE(?, '%Y-%m-%d'), STR_TO_DATE(?, '%Y-%m-%d'), ?, "
                    + " ?, ?, ?, ?, "
                    + " CURRENT_DATE, CURRENT_TIME, ?, ?, ?) ";

    private static final String QUERY_SPU_BASE =
            " SELECT spu.spu, spu.popname, spu.prodname, spu.standarno, "
                    + " spu.brandno, b.brandname, spu.manuno, m.manuname, spu.rx, "
                    + " spu.insurance, spu.gspgms, spu.gspsc, spu.detail, spu.cstatus "
                    + " FROM {{?" + DSMConst.TD_PROD_SPU + "}} spu "
                    + " LEFT JOIN {{?" + DSMConst.TD_PROD_MANU + "}} m ON m.cstatus&1 = 0 AND m.manuno  = spu.manuno "
                    + " LEFT JOIN {{?" + DSMConst.TD_PROD_BRAND + "}} b ON b.cstatus&1 = 0 AND b.brandno = spu.brandno "
                    + " WHERE spu.spu = ? ";

    private static final String QUERY_PROD_BASE =
            " SELECT spu.spu, spu.popname, spu.prodname, spu.standarno, "
                    + " spu.brandno, b.brandname, spu.manuno, m.manuname, spu.rx, "
                    + " spu.insurance, spu.gspgms, spu.gspsc, spu.detail, spu.cstatus, "
                    + " spu.qsc, spu.busscope, s.codename, "
                    + " sku.sku, sku.vatp, sku.mp, sku.rrp, sku.vaildsdate, sku.vaildedate, "
                    + " sku.prodsdate, sku.prodedate, sku.store, "
                    + " sku.limits, sku.sales, sku.wholenum, sku.medpacknum, sku.unit, "
                    + " sku.ondate, sku.ontime, sku.offdate, sku.offtime, sku.spec, sku.prodstatus, "
                    + " sku.imagestatus, sku.cstatus, sku.expmonth, sku.wp "
                    + " FROM ({{?" + DSMConst.TD_PROD_SPU + "}} spu "
                    + " INNER JOIN {{?" + DSMConst.TD_PROD_SKU + "}} sku ON spu.spu = sku.spu ) "
                    + " LEFT  JOIN {{?" + DSMConst.TD_PROD_MANU
                    + "}} m ON m.cstatus&1 = 0 AND m.manuno  = spu.manuno "
                    + " LEFT  JOIN {{?" + DSMConst.TD_PROD_BRAND
                    + "}} b ON b.cstatus&1 = 0 AND b.brandno = spu.brandno "
                    + " LEFT  JOIN {{?" + DSMConst.TB_SYSTEM_BUS_SCOPE
                    + "}} s ON s.cstatus&1 = 0 AND spu.busscope = s.code "
                    + " WHERE 1=1 ";

    private static final String UPDATE_PROD_BASE =
            " UPDATE {{?" + DSMConst.TD_PROD_SKU + "}} sku, "
                    + " {{?" + DSMConst.TD_PROD_SPU + "}} spu "
                    + " SET sku.vatp = ?, sku.mp = ?, sku.rrp = ?, "
                    + " sku.unit = ?, sku.spec = ?, "
                    + " sku.vaildsdate = STR_TO_DATE(?, '%Y-%m-%d'), "
                    + " sku.vaildedate = STR_TO_DATE(?, '%Y-%m-%d'), "
                    + " sku.prodsdate = STR_TO_DATE(?, '%Y-%m-%d'), "
                    + " sku.prodedate = STR_TO_DATE(?, '%Y-%m-%d'), "
                    + " sku.store = ?, sku.limits = ?, sku.wholenum = ?, "
                    + " sku.medpacknum = ?, sku.cstatus = ?,"
                    + " spu.rx = ?, spu.insurance = ?, spu.gspgms = ?, "
                    + " spu.brandno = ?, spu.detail = ?, spu.gspsc = ?, "
                    + " sku.expmonth = ?, spu.busscope = ? "
                    + " WHERE sku.spu = spu.spu AND sku.sku = ? ";

    private static final String QUERY_BUS_SCOPE_BASE =
            " SELECT code, codename "
            + " FROM {{?" + DSMConst.TB_SYSTEM_BUS_SCOPE + "}} "
            + " WHERE cstatus&1 = 0 ";

    public Result onProd(AppContext appContext) {
        String[] params = appContext.param.arrays;

        if (params == null || params.length == 0) {
            return new Result().fail("参数为空");
        }

        String sql = "UPDATE {{?" + DSMConst.TD_PROD_SKU + "}} " +
                " SET prodstatus = 1, ondate = CURRENT_DATE, ontime = CURRENT_TIME " +
                "  WHERE sku = ? AND prodstatus = 0 ";

        List<Object[]> p = new ArrayList<>();
        List<Long> skuList = new ArrayList<>();

        try {
            BgProdVO prodVO;
            for (String ps : params) {
                prodVO = getProd(ps);

                if (prodVO == null) {
                    continue;
                }

                checkOnProd(prodVO);

                p.add(new Object[] { Long.parseLong(ps) });
                skuList.add(Long.parseLong(ps));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Result().fail("上架失败，请保存商品后再上架");
        }

        int status = ProdESUtil.updateProdStatusDocList(skuList, 1);
        if (status > 0) {
            BASE_DAO.updateBatchNative(sql, p, params.length);
        }

        return new Result().success(null);
    }

    public Result offProd(AppContext appContext) {
        String[] params = appContext.param.arrays;

        if (params == null || params.length == 0) {
            return new Result().fail("参数为空");
        }
        String sql = "UPDATE {{?" + DSMConst.TD_PROD_SKU + "}} " +
                " SET prodstatus = 0, offdate = CURRENT_DATE, offtime = CURRENT_TIME " +
                "  WHERE sku = ? AND prodstatus = 1 ";

        List<Object[]> p = new ArrayList<>();
        List<Long> skuList = new ArrayList<>();

        try {
            for (String ps : params) {
                p.add(new Object[] { Long.parseLong(ps) });
                skuList.add(Long.parseLong(ps));
            }
        } catch (Exception e) {
            return new Result().fail("参数存在非法值");
        }

        int status = ProdESUtil.updateProdStatusDocList(skuList, 0);
        if (status > 0) {
            BASE_DAO.updateBatchNative(sql, p, params.length);
        }
        return new Result().success(null);
    }

    private boolean checkStore(long sku, int store) {
        boolean result = sku > 0 && store > 0;

        if (result) {
            String sql = " SELECT s.freezestore "
                    + " FROM {{?" + DSMConst.TD_PROD_SKU + "}} s "
                    + " WHERE s.cstatus&1 = 0 AND s.sku = ? ";

            List<Object[]> queryResult = BASE_DAO.queryNative(sql, sku);

            int actTotal = RedisStockUtil.getSumActStock(sku);
            int freeze = Integer.parseInt(queryResult.get(0)[0].toString());

            result = store >= actTotal + freeze;
        }

        return result;
    }

    public Result updateProd(AppContext appContext) {
        BgProdVO bgProdVO;
        try {
            bgProdVO = JSON.parseObject(appContext.param.json, BgProdVO.class);

            if (bgProdVO == null) {
                throw new IllegalArgumentException("VO is NULL");
            }

            if (bgProdVO.getSku() <= 0) {
                throw new IllegalArgumentException("缺少SKU");
            }

            checkProdBase(bgProdVO);

            BgProdVO sqlProdVO = getProd(bgProdVO.getSku() + "");

            if (sqlProdVO == null) {
                return new Result().fail("此商品不存在");
            }

            if (sqlProdVO.getProdstatus() == 1) {
                checkOnProd(bgProdVO);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Result().fail(e.getMessage());
        }

        if (!checkStore(bgProdVO.getSku(), bgProdVO.getStore())) {
            return new Result().fail("库存不得小于冻结库存和活动总库存之和！");
        }

        bgProdVO.setVatp(MathUtil.exactMul(bgProdVO.getVatp(), 100).intValue());
        bgProdVO.setMp(MathUtil.exactMul(bgProdVO.getMp(), 100).intValue());
        bgProdVO.setRrp(MathUtil.exactMul(bgProdVO.getRrp(), 100).intValue());
        int esResult = ProdESUtil.updateProdDocument(bgProdVO);

        if (esResult != 0) {
            return new Result().fail("操作失败");
        }

        RedisStockUtil.setStock(bgProdVO.getSku(), bgProdVO.getStore());
        BASE_DAO.updateNative(UPDATE_PROD_BASE,
                bgProdVO.getVatp(), bgProdVO.getMp(), bgProdVO.getRrp(),
                bgProdVO.getUnit(), bgProdVO.getSpec(),
                bgProdVO.getVaildsdate(), bgProdVO.getVaildedate(),
                bgProdVO.getProdsdate(), bgProdVO.getProdedate(),
                bgProdVO.getStore(), bgProdVO.getLimits(),
                bgProdVO.getWholenum(), bgProdVO.getMedpacknum(), bgProdVO.getSkuCstatus(),
                bgProdVO.getRx(), bgProdVO.getInsurance(), bgProdVO.getGspGMS(),
                bgProdVO.getBrandNo(), bgProdVO.getDetail(), bgProdVO.getGspSC(),
                bgProdVO.getExpmonth(), bgProdVO.getBusscope(),
                bgProdVO.getSku());

        new ProdReducePriceThread(bgProdVO.getSku(), bgProdVO.getProdname(), bgProdVO.getVatp()).start();
        return new Result().success(null);
    }

    public Result getProds(AppContext appContext) {
        String[] params = appContext.param.arrays;

        if (params == null || params.length == 0) {
            return new Result().fail("参数为空");
        }

        StringBuilder sql = new StringBuilder(QUERY_PROD_BASE);
        sql.append(" AND sku.sku IN (");

        String param;
        for (int i = 0; i < params.length; i++) {
            param = params[i];

            if (!StringUtils.isBiggerZero(param)) {
                return new Result().fail("参数异常");
            }

            sql.append(param);

            if (i < params.length - 1) {
                sql.append(", ");
            }
        }

        sql.append(") ");


        List<Object[]> queryResult = BASE_DAO.queryNative(sql.toString());

        if (queryResult.isEmpty()) {
            return new Result().success(null);
        }

        BgProdVO[] returnResults = new BgProdVO[queryResult.size()];

        BASE_DAO.convToEntity(queryResult, returnResults, BgProdVO.class);

        convProds(returnResults);

        if (appContext.isAnonymous()) {
            for (BgProdVO returnResult : returnResults) {
                returnResult.setRrp(-1);
                returnResult.setMp(-1);
                returnResult.setVatp(-1);
            }
        }

        return new Result().success(returnResults);
    }

    @UserPermission(ignore = true)
    public Result getProd(AppContext appContext) {
        String[] params = appContext.param.arrays;

        if (params == null || params.length == 0) {
            return new Result().fail("参数为空");
        }

        if (!StringUtils.isBiggerZero(params[0])) {
            return new Result().fail("参数错误");
        }


        BgProdVO result = getProd(params[0]);

        if (appContext.isAnonymous()) {
            result.setRrp(-1);
            result.setMp(-1);
            result.setVatp(-1);
        }

        JSONObject jo = JSON.parseObject(JSON.toJSONString(result));

        jo.put("minPrice", CalculateUtil.getProdMinPrice(
                result.getSku(), result.getVatp()));

        return new Result().success(jo);
    }

    private BgProdVO getProd(String sku) {
        if (!StringUtils.isBiggerZero(sku)) {
            return null;
        }

        String sql = QUERY_PROD_BASE + " AND sku.sku = ? ";

        List<Object[]> queryResult = BASE_DAO.queryNative(sql, sku);

        if (queryResult.isEmpty()) {
            return null;
        }

        BgProdVO[] returnResults = new BgProdVO[queryResult.size()];

        BASE_DAO.convToEntity(queryResult, returnResults, BgProdVO.class);

        convProds(returnResults);

        return returnResults[0];
    }


    public Result getSPUInfo(AppContext appContext) {
        String[] params = appContext.param.arrays;

        if (params == null || params.length == 0) {
            return new Result().fail("参数为空");
        }

        if (!StringUtils.isBiggerZero(params[0])) {
            return new Result().fail("参数错误");
        }

        List<Object[]> queryResult = BASE_DAO.queryNative(QUERY_SPU_BASE, params[0]);

        if (queryResult.isEmpty()) {
            return new Result().success(null);
        }

        BgProdVO[] returnResults = new BgProdVO[queryResult.size()];

        BASE_DAO.convToEntity(queryResult, returnResults, BgProdVO.class);

        convProds(returnResults);

        return new Result().success(returnResults[0]);
    }

    public Result queryProds(AppContext appContext) {
        Page page = new Page();
        page.pageIndex = appContext.param.pageIndex;
        page.pageSize = appContext.param.pageNumber;

        PageHolder pageHolder = new PageHolder(page);

        StringBuilder sql = new StringBuilder(QUERY_PROD_BASE);

        List<Object> paramList = new ArrayList<>();
        String[] params = appContext.param.arrays;
        String param = null;

        for (int i = 0; i < params.length; i++) {
            param = params[i];

            if (StringUtils.isEmpty(param)) {
                continue;
            }

            try {
                switch (i) {
                    case 0:
                        sql.append(" AND spu.prodname LIKE ? ");
                        param = "%" + param + "%";
                        break;
                    case 1:
                        sql.append(" AND spu.manuno = ? ");
                        break;
                    case 2:
                        sql.append(" AND sku.spec LIKE ? ");
                        param = "%" + param + "%";
                        break;
                    case 3:
                        sql.append(" AND spu.standarnoh = CRC32(?) AND spu.standarno = ? ");
                        paramList.add(param);
                        break;
                    case 4:
                        sql.append(" AND sku.vaildsdate < STR_TO_DATE(?, '%Y-%m-%d') ");
                        break;
                    case 5:
                        sql.append(" AND sku.prodstatus = ? ");
                        break;
                    case 6:
                        sql.append(" AND spu.spu LIKE ? ");
                        param = "_" + param + "%";
                        break;
                    case 7:
                        sql.append(" AND spu.popname LIKE ? ");
                        param = "%" + param + "%";
                        break;

                }
            } catch (Exception e) {
                continue;
            }

            paramList.add(param);
        }

        List<Object[]> queryResult = BASE_DAO.queryNative(
                pageHolder, page, " sku.oid DESC ", sql.toString(), paramList.toArray());

        BgProdVO[] result = new BgProdVO[queryResult.size()];

        BASE_DAO.convToEntity(queryResult, result, BgProdVO.class);

        convProds(result);

        return new Result().setQuery(result, pageHolder);
    }

    public Result addProd(AppContext appContext) {
        BgProdVO bgProdVO;
        try {
            bgProdVO =
                    JSON.parseObject(appContext.param.json, BgProdVO.class);

            if (bgProdVO == null) {
                throw new IllegalArgumentException("VO is NULL");
            }

            checkProdBase(bgProdVO);

            bgProdVO.setVatp(MathUtil.exactMul(bgProdVO.getVatp(), 100).intValue());
            bgProdVO.setMp(MathUtil.exactMul(bgProdVO.getMp(), 100).intValue());
            bgProdVO.setRrp(MathUtil.exactMul(bgProdVO.getRrp(), 100).intValue());
            bgProdVO.setSkuCstatus(bgProdVO.getSkuCstatus() | 256);
        } catch (Exception e) {
            e.printStackTrace();
            return new Result().fail(e.getMessage());
        }

        String spu = containsSPU(bgProdVO);

        if (spu == null) {
            try {
                spu = getNextSPU(bgProdVO.getClassNo(), bgProdVO.getStandarNo(), bgProdVO.getForm());
            } catch (Exception e) {
                return new Result().fail(e.getMessage());
            }

            if (spu == null) {
                return new Result().fail("SPU生成失败");
            }
        }

        bgProdVO.setSpu(Long.parseLong(spu));

        synchronized (BackgroundProdModule.class) {
            long spu_crease = RedisUtil.getStringProvide().increase(spu);

            if (spu_crease > 99) {
                return new Result().fail("SKU满了");
            }

            String sku = spu + String.format("%02d", spu_crease);

            bgProdVO.setSku(Long.parseLong(sku));

            List<Object[]> params = new ArrayList<>(2);

            //spu, popname, popnameh,
            // prodname, prodnameh, "
            // standarno, standarnoh,
            // brandno, manuno, rx, "
            // insurance, gspgms, gspsc,
            // detail
            params.add(new Object[] {
                    bgProdVO.getSpu(), bgProdVO.getPopname(), bgProdVO.getPopname(),
                    bgProdVO.getProdname(), bgProdVO.getProdname(),
                    bgProdVO.getStandarNo(), bgProdVO.getStandarNo(),
                    bgProdVO.getBrandNo(), bgProdVO.getManuNo(), bgProdVO.getRx(),
                    bgProdVO.getInsurance(), bgProdVO.getGspGMS(), bgProdVO.getGspSC(),
                    bgProdVO.getDetail(), bgProdVO.getBusscope(),
                    bgProdVO.getSpu()
            });

            // spu, sku,
            // vatp,
            // mp,
            // rrp, "
            // vaildsdate, vaildedate,
            // prodsdate, prodedate,
            // store, limits,
            // wholenum, medpacknum, unit,
            // spec, cstatus "
            params.add(new Object[] {
                    bgProdVO.getSpu(), bgProdVO.getSku(),
                    bgProdVO.getVatp(),
                    bgProdVO.getMp(),
                    bgProdVO.getRrp(),
                    bgProdVO.getVaildsdate(), bgProdVO.getVaildedate(),
                    bgProdVO.getProdsdate(), bgProdVO.getProdedate(),
                    bgProdVO.getStore(), bgProdVO.getLimits(),
                    bgProdVO.getWholenum(), bgProdVO.getMedpacknum(), bgProdVO.getUnit(),
                    bgProdVO.getSpec(), bgProdVO.getSkuCstatus(), bgProdVO.getExpmonth(),
            });

            RedisStockUtil.setStock(bgProdVO.getSku(), bgProdVO.getStore());
            int esResult = ProdESUtil.addProdDocument(bgProdVO);

            if (esResult != 0) {
                RedisUtil.getStringProvide().decrease(spu);
                return new Result().fail("操作失败");
            }

            try {
                BASE_DAO.updateTransNative(
                        new String[] { INSERT_PROD_SPU, INSERT_PROD_SKU },
                        params);
            } catch (Exception e) {
                e.printStackTrace();
                ProdESUtil.deleteProdDocument(Long.parseLong(sku));
                RedisUtil.getStringProvide().decrease(spu);
                return new Result().fail("SQL异常");
            }
        }


        bgProdVO.setVatp(MathUtil.exactDiv(bgProdVO.getVatp(), 100).intValue());
        bgProdVO.setMp(MathUtil.exactDiv(bgProdVO.getMp(), 100).intValue());
        bgProdVO.setRrp(MathUtil.exactDiv(bgProdVO.getRrp(), 100).intValue());

        return new Result().success(bgProdVO);
    }

    private void convProds(BgProdVO[] bgProdVOs) {
        String[] spuParser = null;

        for (BgProdVO bgProdVO : bgProdVOs) {
            bgProdVO.setVatp(MathUtil.exactDiv(bgProdVO.getVatp(), 100).doubleValue());
            bgProdVO.setRrp(MathUtil.exactDiv(bgProdVO.getRrp(), 100).doubleValue());
            bgProdVO.setMp(MathUtil.exactDiv(bgProdVO.getMp(), 100).doubleValue());
            bgProdVO.setWp(MathUtil.exactDiv(bgProdVO.getWp(), 100).doubleValue());

            spuParser = parseSPU(bgProdVO.getSpu());

            if (spuParser != null) {
                bgProdVO.setClassNo(Long.parseLong(spuParser[0]));
                bgProdVO.setClassName(ProduceClassUtil.getProdClassName(spuParser[0]));
                bgProdVO.setForm(Integer.parseInt(spuParser[1]));
            }

            bgProdVO.setStore(RedisStockUtil.getStock(bgProdVO.getSku()));
            try {
                DictStore.translate(bgProdVO);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public Result getBusScopes(AppContext appContext) {
        List<Object[]> queryResult = BASE_DAO.queryNative(QUERY_BUS_SCOPE_BASE);

        BusScopeVo[] returnResults = new BusScopeVo[queryResult.size()];

        BASE_DAO.convToEntity(queryResult, returnResults, BusScopeVo.class);

        return new Result().success(returnResults);
    }

    /**
     * 解析SPU码
     *
     * @param spu
     * @return [类型码, 剂型码]
     */

    private String[] parseSPU(long spu) {
        String spuStr = String.valueOf(spu);

        if (spuStr.length() != 12) {
            return null;
        }

        String classNo = spuStr.substring(1, 7);
        String formNo = spuStr.substring(10, 12);

        return new String[] { classNo, formNo };
    }

    private void checkOnProd(BgProdVO prodVO) {
        if (prodVO.getVatp() <= .0) {
            throw new IllegalArgumentException("含税价为0");
        }

        if (prodVO.getStore() <= 0) {
            throw new IllegalArgumentException("库存为0");
        }

        if (prodVO.getRx() <= 0) {
            throw new IllegalArgumentException("未选择是否为处方药");
        }

        if (prodVO.getWholenum() <= 0) {
            prodVO.setWholenum(1);
        }

        if (prodVO.getMedpacknum() <= 0) {
            prodVO.setMedpacknum(1);
        }

    }

    private void checkProdBase(BgProdVO prodVO) {
        // 厂家
        if (prodVO.getManuNo() <= 0) {
            throw new IllegalArgumentException("厂商为空");
        }

        // 通用名
        if (StringUtils.isEmpty(prodVO.getPopname())) {
            throw new IllegalArgumentException("通用名为空");
        }

        // 产品名
        if (StringUtils.isEmpty(prodVO.getProdname())) {
            throw new IllegalArgumentException("产品名为空");
        }

        // 详情格式
        if (!StringUtils.isJsonFormatter(prodVO.getDetail())) {
            throw new IllegalArgumentException("详情格式不正确");
        }

        // 类别码
        if (prodVO.getClassNo() <= 0) {
            throw new IllegalArgumentException("类别码为空");
        }

        // 剂型码
        if (prodVO.getForm() <= 0) {
            throw new IllegalArgumentException("剂型码为空");
        }

        if (prodVO.getBusscope() <= 0) {
            throw new IllegalArgumentException("经营范围为空");
        }

        // 准号
        if (StringUtils.isEmpty(prodVO.getStandarNo())) {
            // 除中药饮片外不可为空
            if (!String.valueOf(prodVO.getClassNo()).startsWith("21")) {
                throw new IllegalArgumentException("批准文号为空");
            }
        }

        if (!StringUtils.isDateFormatter(prodVO.getVaildedate())) {
            prodVO.setVaildedate(null);
        }

        if (!StringUtils.isDateFormatter(prodVO.getVaildsdate())) {
            prodVO.setVaildsdate(null);
        }

        if (!StringUtils.isDateFormatter(prodVO.getProdsdate())) {
            prodVO.setProdsdate(null);
        }

        if (!StringUtils.isDateFormatter(prodVO.getProdedate())) {
            prodVO.setProdedate(null);
        }
    }

    private String containsSPU(BgProdVO prodVO) {
        StringBuilder regexp =
                new StringBuilder("^")
                        .append("[0-9]{1}")
                        .append(prodVO.getClassNo())
                        .append("[0-9]{3}")
                        .append(String.format("%02d", prodVO.getForm()))
                        .append("$");

        List<Object[]> queryResult = BASE_DAO.queryNative(CHECK_SAME_SPU,
                prodVO.getPopname(), prodVO.getPopname(),
                prodVO.getProdname(), prodVO.getProdname(),
                prodVO.getStandarNo(), prodVO.getStandarNo(),
                prodVO.getManuNo(), regexp.toString());

        if (queryResult.isEmpty()) {
            return null;
        }

        return queryResult.get(0)[0].toString();
    }

    private String getNextSPU(long classNo, String standarNo, int form) throws Exception {
        if (!MathUtil.isBetween(1, form, 99)) {
            throw new Exception("剂型不正确");
        }

        if (String.valueOf(classNo).length() != 6) {
            throw new Exception("类别请选择到第三级");
        }

        int mc = StringUtils.isEmpty(standarNo) ? 1 : isMadeInChina(standarNo);

        String formatForm = String.format("%02d", form);

        StringBuilder spuBase = new StringBuilder();
        spuBase.append(mc)
                .append(classNo)
                .append("[0-9]{3}")
                .append(formatForm);

        StringBuilder regexp =
                new StringBuilder("^").append(spuBase).append("$");

        List<Object[]> queryResult;

        synchronized (BackgroundProdModule.class) {
            queryResult = BASE_DAO.queryNative(GET_MAX_SPU, regexp.toString());
        }

        int count = Integer.parseInt(queryResult.get(0)[1].toString());

        if (count == 0) {
            return spuBase.toString().replace("[0-9]{3}", "000");
        }

        if (count == 999) {
            throw new Exception("超过上限");
        }

        count = Integer.parseInt(queryResult.get(0)[0].toString());

        return spuBase.toString().replace("[0-9]{3}", String.format("%03d", count + 1));
    }

    /**
     * 判定是批准文号还是注册证编号
     *
     * @param standarNo
     * @return 0 非法参数
     * 1 批准文号
     * 2 注册证编号
     */
    private int getProdType(String standarNo) {
        if (Pattern.matches(PZWH, standarNo)) {
            return 1;
        }

//        if (Pattern.matches(ZCZBH, standarNo)) {
//            return 2;
//        }

        return 0;
    }

    /**
     * 根据批准文号判定是否为中国产的药品
     *
     * @param standarNo
     * @return 0 非法参数
     * 1 非进口
     * 2 进口
     */
    private int isMadeInChina(String standarNo) {
        int type = getProdType(standarNo);

        switch (type) {
            case 1:
                return standarNo.contains("J") ? 2 : 1;
            default:
                return standarNo.contains("准") ? 1 : 2;
        }
    }

    /**
     * 药品减价通知
     */
    class ProdReducePriceThread extends Thread {

        long sku;
        String prodname;
        double vatp;

        public ProdReducePriceThread(long sku, String prodname, double vatp) {
            this.sku = sku;
            this.prodname = prodname;
            this.vatp = vatp;
        }

        @Override
        public void run() {
            LogUtil.getDefaultLogger().info("++++++ prod reduce price sendMsg start +++++++");
            SearchResponse response = null;
            try {
                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                TermsQueryBuilder builder = QueryBuilders.termsQuery(ESConstant.COLLECT_COLUMN_SKU, sku + "");
                boolQuery.must(builder);

                TransportClient client = ElasticSearchClientFactory.getClientInstance();
                response = client.prepareSearch(ESConstant.COLLECT_INDEX)
                        .setQuery(boolQuery).setSize(10000)
                        .execute().actionGet();
                List<Map<String, Object>> dataList = new ArrayList<>();
                if (response != null && response.getHits() != null) {
                    for (SearchHit hit : response.getHits()) {
                        dataList.add(hit.getSourceAsMap());
                    }
                }

                LogUtil.getDefaultLogger().info(
                        "++++++ prod reduce price sendMsg dataList size: " + dataList.size() + " +++++++");
                if (dataList != null && dataList.size() > 0) {
                    for (Map<String, Object> data : dataList) {
                        double prize = Integer.parseInt(data.get("prize").toString());
                        if (prize > vatp) {
                            double p = MathUtil.exactDiv(prize, 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                            double v = MathUtil.exactDiv(vatp, 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                            try {
                                IceRemoteUtil.sendMessageToClient(Integer.parseInt(data.get("compid").toString()), SmsTempNo.genPushMessageBySystemTemp(SmsTempNo.NOTICE_OF_COMMODITY_REDUCTION, prodname, String.valueOf(p), String.valueOf(v)));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            try {
                                String phone = IceRemoteUtil.getSpecifyStorePhone(Integer.parseInt(data.get("compid").toString()));
                                SmsUtil.sendSmsBySystemTemp(phone, SmsTempNo.NOTICE_OF_COMMODITY_REDUCTION, prodname, String.valueOf(p), String.valueOf(v));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }


            } catch (Exception e) {
                e.printStackTrace();
            }

            LogUtil.getDefaultLogger().info("++++++ prod reduce price sendMsg end +++++++");
        }
    }
}
