package com.onek.goods;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.google.gson.Gson;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.goods.entities.ProdManuVO;
import constant.DSMConst;
import dao.BaseDAO;
import global.GenIdUtil;
import util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class BackgroundProdManuModule {
    private static final BaseDAO BASE_DAO = BaseDAO.getBaseDAO();

    private static final String INSERT_PRODMANU_BASE =
            "INSERT INTO {{?" + DSMConst.TD_PROD_MANU + "}} "
            + " (manuno, manuname, manunameh, areac, address, createdate, createtime) "
            + " SELECT ?, ?, CRC32(?), ?, ?, CURRENT_DATE, CURRENT_TIME "
            + " FROM DUAL "
            + " WHERE NOT EXISTS ("
                 + " SELECT *"
                 + " FROM {{?" + DSMConst.TD_PROD_MANU + "}} "
                 + " WHERE manunameh = CRC32(?) AND manuname = ? ) ";

    private static final String QUERY_PRODMANU_BASE =
            " SELECT oid, manuno, manuname, areac, address,"
            + " createdate, createtime, cstatus "
            + " FROM {{?" + DSMConst.TD_PROD_MANU + "}} "
            + " WHERE cstatus&1 = 0 ";

    public Result addProdManu(AppContext appContext) {
        ProdManuVO prodManuVO;
        try {
            prodManuVO =
                    new Gson().fromJson(appContext.param.json, ProdManuVO.class);

            if (prodManuVO == null) {
                throw new IllegalArgumentException("VO is NULL");
            }

            if (StringUtils.isEmpty(prodManuVO.getManuname())) {
                throw new IllegalArgumentException("manuname is empty");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Result().fail("参数错误");
        }

        long manuId = GenIdUtil.getUnqId();

        int result = BASE_DAO.updateNative(INSERT_PRODMANU_BASE,
                        manuId, prodManuVO.getManuname(), prodManuVO.getManuname(),
                        prodManuVO.getAreac(), prodManuVO.getAddress(),
                        prodManuVO.getManuname(), prodManuVO.getManuname());

        return new Result().success(result > 0 ? manuId : 0);
    }

    public Result queryProdManu(AppContext appContext) {
        Page page = new Page();
        page.pageIndex = appContext.param.pageIndex;
        page.pageSize  = appContext.param.pageNumber;

        PageHolder pageHolder = new PageHolder(page);

        StringBuilder sql = new StringBuilder(QUERY_PRODMANU_BASE);

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
                        sql.append(" AND manuname LIKE ? ");
                        param = "%" + param + "%";
                        break;
                    case 1:
                        sql.append(" AND areac = ? ");
                        break;
                }
            } catch (Exception e) {
                continue;
            }

            paramList.add(param);
        }

        List<Object[]> queryResult = BASE_DAO.queryNative(
                pageHolder, page, sql.toString(), paramList.toArray());
        ProdManuVO[] result = new ProdManuVO[queryResult.size()];

        if (result.length > 0) {
            BASE_DAO.convToEntity(queryResult, result, ProdManuVO.class);
        }

        return new Result().setQuery(result, pageHolder);
    }

}
