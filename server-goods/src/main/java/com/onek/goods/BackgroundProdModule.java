package com.onek.goods;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.google.gson.Gson;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.goods.entities.BgProdVO;
import com.onek.goods.util.ProdESUtil;
import com.onek.server.inf.IRequest;
import com.onek.util.dict.DictStore;
import com.onek.util.prod.ProduceStore;
import constant.DSMConst;
import dao.BaseDAO;
import redis.util.RedisUtil;
import util.MathUtil;
import util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class BackgroundProdModule {
    private static final BaseDAO BASE_DAO = BaseDAO.getBaseDAO();
    private static final String PZWH = "^国药准字[H|B|S|T|F|J|Z]\\d{8}$";
    private static final String ZCZBH = "^[\\u4e00-\\u9fa5]食药监械\\([准|进|许]\\)字\\d{4}第\\d{7}号$";

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
            + " AND manuno = ? ";

    private static final String INSERT_PROD_SPU =
            " INSERT INTO {{?" + DSMConst.TD_PROD_SPU + "}} "
            + " (spu, popname, popnameh, prodname, prodnameh, "
            + " standarno, standarnoh, brandno, manuno, rx, "
            + " insurance, gspgms, gspsc, detail) "
                + " SELECT ?, ?, CRC32(?), ?, CRC32(?), "
                        + " ?, CRC32(?), ?, ?, ?, "
                        + " ?, ?, ?, ? "
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
            + " activitystore, limits, wholenum, medpacknum, unit,"
            + " ondate, ontime, spec) "
            + " VALUES (?, ?, ?, ?, ?, "
                    + " STR_TO_DATE(?, '%Y-%m-%d'), STR_TO_DATE(?, '%Y-%m-%d'),"
                    + " STR_TO_DATE(?, '%Y-%m-%d'), STR_TO_DATE(?, '%Y-%m-%d'), ?, "
                    + " ?, ?, ?, ?, ?, "
                    + " CURRENT_DATE, CURRENT_TIME, ?) ";

    private static final String QUERY_SPU_BASE =
            " SELECT spu.spu, spu.popname, spu.prodname, spu.standarno, "
            + " spu.brandno, b.brandname, spu.manuno, m.manuname, spu.rx, "
            + " spu.insurance, spu.gspgms, spu.gspsc, spu.detail, spu.cstatus "
            + " FROM {{?" + DSMConst.TD_PROD_SPU + "}} spu "
            + " LEFT JOIN {{?" + DSMConst.TD_PROD_MANU  + "}} m ON m.cstatus&1 = 0 AND m.manuno  = spu.manuno "
            + " LEFT JOIN {{?" + DSMConst.TD_PROD_BRAND + "}} b ON b.cstatus&1 = 0 AND b.brandno = spu.brandno "
            + " WHERE spu.spu = ? ";

    private static final String QUERY_PROD_BASE =
            " SELECT spu.spu, spu.popname, spu.prodname, spu.standarno, "
            + " spu.brandno, b.brandname, spu.manuno, m.manuname, spu.rx, "
            + " spu.insurance, spu.gspgms, spu.gspsc, spu.detail, spu.cstatus,"
            + " sku.sku, sku.vatp, sku.mp, sku.rrp, sku.vaildsdate, sku.vaildedate,"
            + " sku.prodsdate, sku.prodedate, sku.store, sku.activitystore, "
            + " sku.limits, sku.sales, sku.wholenum, sku.medpacknum, sku.unit, "
            + " sku.ondate, sku.ontime, sku.offdate, sku.offtime, sku.spec, sku.prodstatus, "
            + " sku.imagestatus, sku.cstatus "
            + " FROM ({{?" + DSMConst.TD_PROD_SPU + "}} spu "
            + " INNER JOIN {{?" + DSMConst.TD_PROD_SKU   + "}} sku ON spu.spu = sku.spu ) "
            + " LEFT  JOIN {{?" + DSMConst.TD_PROD_MANU  + "}} m   ON m.cstatus&1 = 0 AND m.manuno  = spu.manuno "
            + " LEFT  JOIN {{?" + DSMConst.TD_PROD_BRAND + "}} b   ON b.cstatus&1 = 0 AND b.brandno = spu.brandno "
            + " WHERE 1=1 ";

    private static final String UPDATE_PROD_BASE =
            " UPDATE {{?" + DSMConst.TD_PROD_SKU + "}} "
            + " SET vatp = ?, mp = ?, rrp = ?, unit = ?, spec = ?, "
            + " vaildsdate = STR_TO_DATE(?, '%Y-%m-%d'), vaildedate = STR_TO_DATE(?, '%Y-%m-%d'), "
            + " prodsdate = STR_TO_DATE(?, '%Y-%m-%d'), prodedate = STR_TO_DATE(?, '%Y-%m-%d'), "
            + " store = ?, activitystore = ?, limits = ?, wholenum = ?, medpacknum = ? "
            + " WHERE sku = ? ";

    public Result updateProd(AppContext appContext) {
        BgProdVO bgProdVO;
        try {
            bgProdVO = new Gson().fromJson(appContext.param.json, BgProdVO.class);

            if (bgProdVO == null) {
                throw new IllegalArgumentException("VO is NULL");
            }

            if (bgProdVO.getSku() <= 0) {
                throw new IllegalArgumentException("缺少SKU");
            }

            if (!checkProd(bgProdVO)) {
                throw new IllegalArgumentException();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new Result().fail("参数错误");
        }

        int esResult = ProdESUtil.updateProdDocument(bgProdVO);

        if (esResult != 0) {
            return new Result().fail("操作失败");
        }

        BASE_DAO.updateNative(UPDATE_PROD_BASE,
                MathUtil.exactMul(bgProdVO.getVatp(), 100).intValue(),
                MathUtil.exactMul(bgProdVO.getMp  (), 100).intValue(),
                MathUtil.exactMul(bgProdVO.getRrp (), 100).intValue(),
                bgProdVO.getUnit(), bgProdVO.getSpec(),
                bgProdVO.getVaildsdate(), bgProdVO.getVaildedate(),
                bgProdVO.getProdsdate(), bgProdVO.getProdedate(),
                bgProdVO.getStore(), bgProdVO.getActivitystore(), bgProdVO.getLimits(),
                bgProdVO.getWholenum(), bgProdVO.getMedpacknum(),
                bgProdVO.getSku());

        return new Result().success(null);
    }

    public Result getProd(AppContext appContext) {
        String[] params = appContext.param.arrays;

        if (params == null || params.length == 0) {
            return new Result().fail("参数为空");
        }

        if (!StringUtils.isBiggerZero(params[0])) {
            return new Result().fail("参数错误");
        }

        String sql = QUERY_PROD_BASE + " AND sku.sku = ? ";

        List<Object[]> queryResult = BASE_DAO.queryNative(sql, params[0]);

        if (queryResult.isEmpty()) {
            return new Result().success(null);
        }

        BgProdVO[] returnResults = new BgProdVO[queryResult.size()];

        BASE_DAO.convToEntity(queryResult, returnResults, BgProdVO.class);

        convProds(returnResults);

        return new Result().success(returnResults[0]);
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
        page.pageSize  = appContext.param.pageNumber;

        PageHolder pageHolder = new PageHolder(page);

        StringBuilder sql = new StringBuilder(QUERY_PROD_BASE);

        List<Object> paramList = new ArrayList<>();
        String[] params =  appContext.param.arrays;
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
                    new Gson().fromJson(appContext.param.json, BgProdVO.class);

            if (bgProdVO == null) {
                throw new IllegalArgumentException("VO is NULL");
            }
            
            if (!checkProd(bgProdVO)) {
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Result().fail("参数错误");
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
                    bgProdVO.getDetail(),
                    bgProdVO.getSpu()
            });

            // spu, sku,
            // vatp,
            // mp,
            // rrp, "
            // vaildsdate, vaildedate,
            // prodsdate, prodedate,
            // store, activitystore, limits,
            // wholenum, medpacknum, unit,
            // spec, "
            params.add(new Object[] {
                    bgProdVO.getSpu(), bgProdVO.getSku(),
                    MathUtil.exactMul(bgProdVO.getVatp(), 100).intValue(),
                    MathUtil.exactMul(bgProdVO.getMp  (), 100).intValue(),
                    MathUtil.exactMul(bgProdVO.getRrp (), 100).intValue(),
                    bgProdVO.getVaildsdate(), bgProdVO.getVaildedate(),
                    bgProdVO.getProdsdate(), bgProdVO.getProdedate(),
                    bgProdVO.getStore(), bgProdVO.getActivitystore(), bgProdVO.getLimits(),
                    bgProdVO.getWholenum(), bgProdVO.getMedpacknum(), bgProdVO.getUnit(),
                    bgProdVO.getSpec()
            });


            int esResult = ProdESUtil.addProdDocument(bgProdVO);

            if (esResult != 0) {
                return new Result().fail("操作失败");
            }

            try {
                BASE_DAO.updateTransNative(
                        new String[] {INSERT_PROD_SPU, INSERT_PROD_SKU},
                        params);
            } catch (Exception e) {
                e.printStackTrace();
                ProdESUtil.deleteProdDocument(Long.parseLong(sku));
                RedisUtil.getStringProvide().decrease(spu);
                return new Result().fail("SQL异常");
            }
        }

        return new Result().success(bgProdVO);
    }

    private void convProds(BgProdVO[] bgProdVOs) {
        String[] spuParser;
        for (BgProdVO bgProdVO : bgProdVOs) {
            bgProdVO.setVatp(MathUtil.exactDiv(bgProdVO.getVatp(), 100).doubleValue());
            bgProdVO.setRrp (MathUtil.exactDiv(bgProdVO.getRrp (), 100).doubleValue());
            bgProdVO.setMp  (MathUtil.exactDiv(bgProdVO.getMp  (), 100).doubleValue());

            spuParser = parseSPU(bgProdVO.getSpu());

            if (spuParser != null) {
                bgProdVO.setClassNo(Long.parseLong(spuParser[0]));
                bgProdVO.setForm(Integer.parseInt(spuParser[1]));
                bgProdVO.setClassName(ProduceStore.getProduceName(spuParser[0]));
            }

            try {
                DictStore.translate(bgProdVO);
            } catch (Exception e) {e.printStackTrace();}
        }
    }

    /**
     * 解析SPU码
     * @param spu
     * @return [类型码, 剂型码]
     */

    private String[] parseSPU(long spu) {
        String spuStr = String.valueOf(spu);

        if (spuStr.length() != 12) {
            return null;
        }

        String classNo = spuStr.substring(1, 7);
        String formNo  = spuStr.substring(10, 12);

        return new String[] {classNo, formNo};
    }

    private boolean checkProd(BgProdVO prodVO) {
        return prodVO != null
                && prodVO.getManuNo() > 0
                && !StringUtils.isEmpty(prodVO.getPopname())
                && !StringUtils.isEmpty(prodVO.getProdname())
                && !StringUtils.isEmpty(prodVO.getStandarNo())
                && StringUtils.isDateFormatter(prodVO.getVaildedate())
                && StringUtils.isDateFormatter(prodVO.getVaildsdate())
                && StringUtils.isDateFormatter(prodVO.getProdsdate())
                && StringUtils.isDateFormatter(prodVO.getProdedate())
                && StringUtils.isJsonFormatter(prodVO.getDetail());
    }

    private String containsSPU(BgProdVO prodVO) {
        List<Object[]> queryResult = BASE_DAO.queryNative(CHECK_SAME_SPU,
                prodVO.getPopname(), prodVO.getPopname(),
                prodVO.getProdname(), prodVO.getProdname(),
                prodVO.getStandarNo(), prodVO.getStandarNo(),
                prodVO.getManuNo());

        if (queryResult.isEmpty()) {
            return null;
        }

        return queryResult.get(0)[0].toString();
    }

    private String getNextSPU(long classNo, String standarNo, int form) throws Exception {
        if (!MathUtil.isBetween(1, form, 99)
                || String.valueOf(classNo).length() != 6) {
            return null;
        }

        int mc = isMadeInChina(standarNo);

        if (mc == 0) {
            throw new IllegalArgumentException("标准号不正确");
        }

        String formatForm = String.format("%02d", form);

        StringBuilder spuBase = new StringBuilder();
        spuBase.append(mc)
                .append(classNo)
                .append("[0-9]{3}")
                .append(formatForm);

        StringBuilder regexp =
                new StringBuilder("^").append(spuBase).append("$");

        List<Object[]> queryResult;

        synchronized(BackgroundProdModule.class) {
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
     * @param standarNo
     * @return
     *  0 非法参数
     *  1 批准文号
     *  2 注册证编号
     */
    private int getProdType(String standarNo) {
        if (Pattern.matches(PZWH, standarNo)) {
            return 1;
        }

        if (Pattern.matches(ZCZBH, standarNo)) {
            return 2;
        }

        return 0;
    }

    /**
     * 根据批准文号判定是否为中国产的药品
     * @param standarNo
     * @return
     *  0 非法参数
     *  1 非进口
     *  2 进口
     */
    private int isMadeInChina(String standarNo) {
        int type = getProdType(standarNo);

        switch (type) {
            case 1  :
                return standarNo.contains("J")  ? 2 : 1;
            case 2  :
                return standarNo.contains("准") ? 1 : 2;
            default :
                return 0;
        }
    }


}
