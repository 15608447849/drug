package com.onek.goods;

import com.google.gson.Gson;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.goods.entities.BgProdVO;
import constant.DSMConst;
import dao.BaseDAO;

import java.util.List;
import java.util.regex.Pattern;

public class BackgroundProdModule {
    private static final BaseDAO BASE_DAO = BaseDAO.getBaseDAO();

    private static final String PZWH = "^国药准字[J|Z]\\d{8}$";
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

    private static final String INSERT_PROD_BASE = "";

    public Result addProd(AppContext appContext) {
        BgProdVO bgProdVO;
        try {
            bgProdVO =
                    new Gson().fromJson(appContext.param.json, BgProdVO.class);

            if (bgProdVO == null) {
                throw new IllegalArgumentException("VO is NULL");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Result().fail("参数错误");
        }

        String spu = checkSPU(bgProdVO);

        if (spu == null) {
            try {
                spu = genSPU(bgProdVO.getStandarNo(), bgProdVO.getClassNo(), bgProdVO.getForm());
            } catch (Exception e) {
                return new Result().fail(e.getMessage());
            }

            if (spu == null) {
                return new Result().fail("SPU生成失败");
            }
        }

        bgProdVO.setSpu(Long.parseLong(spu));

        return null;
    }

    private String checkSPU(BgProdVO prodVO) {
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

    private String genSPU(String standarNo, long classNo, int form) throws Exception {
        int mc = isMadeInChina(standarNo);

        if (mc < 0 || classNo <= 0 || form <= 0 || form > 99) {
            return null;
        }

        if (String.valueOf(classNo).length() != 6) {
            return null;
        }

        String formatForm;
        try {
            formatForm = String.format(String.valueOf(form), "%02d");
        } catch (Exception e) {
            return null;
        }

        StringBuilder spuBase = new StringBuilder();
        spuBase.append(mc)
                .append(classNo)
                .append("[0-9]{3}")
                .append(formatForm);

        StringBuilder regexp =
                new StringBuilder("^").append(spuBase).append("$");
        List<Object[]> queryResult;

        synchronized(this) {
            queryResult = BASE_DAO.queryNative(GET_MAX_SPU, regexp);
        }

        int count = Integer.parseInt(queryResult.get(0)[1].toString());

        if (count == 0) {
            return spuBase.toString().replace("[0-9]{3}", "000");
        }

        if (count == 999) {
            throw new Exception("超过上限");
        }

        count = Integer.parseInt(queryResult.get(0)[0].toString());

        return spuBase.toString().replace("[0-9]{3}", String.format(String.valueOf(count + 1), "%03d"));
    }

    /**
     * 判定为批准文号还是注册证编号
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
        int result = 0;
        switch (type) {
            case 1  :
                result = standarNo.contains("Z") ? 1 : 2;
                break;
            case 2  :
                result = standarNo.contains("准") ? 1 : 2;
                break;
            default :
                break;
        }

        return result;
    }


}
