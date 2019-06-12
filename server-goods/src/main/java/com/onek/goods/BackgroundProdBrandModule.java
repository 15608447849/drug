package com.onek.goods;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.google.gson.Gson;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.goods.entities.ProdBrandVO;
import constant.DSMConst;
import dao.BaseDAO;
import com.onek.util.GenIdUtil;
import util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class BackgroundProdBrandModule {
    private static final BaseDAO BASE_DAO = BaseDAO.getBaseDAO();

    private static final String INSERT_PRODBRAND_BASE =
            "INSERT INTO {{?" + DSMConst.TD_PROD_BRAND + "}} "
                    + " (brandno, brandname, brandnameh) "
                    + " SELECT ?, ?, CRC32(?) "
                    + " FROM DUAL "
                    + " WHERE NOT EXISTS ("
                    + " SELECT *"
                    + " FROM {{?" + DSMConst.TD_PROD_BRAND + "}} "
                    + " WHERE brandnameh = CRC32(?) AND brandname = ? ) ";

    private static final String QUERY_PRODBRAND_BASE =
            " SELECT oid, brandno, brandname, cstatus "
                    + " FROM {{?" + DSMConst.TD_PROD_BRAND + "}} "
                    + " WHERE cstatus&1 = 0 ";

    /**
     *
     * 功能: 增加商品品牌
     * 参数类型: json
     * 参数集: ProdBrandVO的json
     * 返回值: Result
     * 详情说明:
     * 日期: 2019/6/11 14:12
     * 作者: Helena Rubinstein
     */
    public Result addProdBrand(AppContext appContext) {
        ProdBrandVO brandVO;
        try {
            brandVO =
                    new Gson().fromJson(appContext.param.json, ProdBrandVO.class);

            if (brandVO == null) {
                throw new IllegalArgumentException("VO is NULL");
            }

            if (StringUtils.isEmpty(brandVO.getBrandname())) {
                throw new IllegalArgumentException("brandname is empty");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new Result().fail("参数错误");
        }

        long brandId = GenIdUtil.getUnqId();

        int result = BASE_DAO.updateNative(INSERT_PRODBRAND_BASE,
                brandId, brandVO.getBrandname(), brandVO.getBrandname(),
                brandVO.getBrandname(), brandVO.getBrandname());

        return new Result().success(result > 0 ? brandId : 0);
    }

    /**
     *
     * 功能: 查询商品品牌
     * 参数类型: arrays
     * 参数集: [商品名]
     * 返回值: Result
     * 详情说明:
     * 日期: 2019/6/11 14:12
     * 作者: Helena Rubinstein
     */
    public Result queryProdBrand(AppContext appContext) {
        Page page = new Page();
        page.pageIndex = appContext.param.pageIndex;
        page.pageSize  = appContext.param.pageNumber;

        PageHolder pageHolder = new PageHolder(page);

        StringBuilder sql = new StringBuilder(QUERY_PRODBRAND_BASE);

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
                        sql.append(" AND brandname LIKE ? ");
                        param = "%" + param + "%";
                        break;
                }
            } catch (Exception e) {
                continue;
            }

            paramList.add(param);
        }

        List<Object[]> queryResult = BASE_DAO.queryNative(
                pageHolder, page, sql.toString(), paramList.toArray());
        ProdBrandVO[] result = new ProdBrandVO[queryResult.size()];

        if (result.length > 0) {
            BASE_DAO.convToEntity(queryResult, result, ProdBrandVO.class);
        }

        return new Result().setQuery(result, pageHolder);
    }

}
