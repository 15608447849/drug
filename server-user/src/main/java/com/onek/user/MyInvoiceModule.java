package com.onek.user;

import com.google.gson.Gson;
import com.onek.AppContext;
import com.onek.entitys.Result;
import com.onek.user.entity.InvoiceVO;
import constant.DSMConst;
import dao.BaseDAO;

import java.util.List;

/**
 * 我的发票模块
 */

public class MyInvoiceModule {
    private final static String QUERY_INVOICE_BASE =
            "SELECT * "
            + " FROM {{?" + DSMConst.D_COMP_INVOICE + "}} "
            + " WHERE cstatus&1 = 0 AND cid = ? ";

    private final static String INSERT_INVOICE =
            "INSERT INTO {{?" + DSMConst.D_COMP_INVOICE + "}}"
            + " (cid, taxpayer, bankers, account, tel) "
            + " SELECT ?, ?, ?, ?, ? "
            + " FROM DUAL "
            + " WHERE NOT EXISTS ("
                    + " SELECT * "
                    + " FROM {{?" + DSMConst.D_COMP_INVOICE + "}}"
                    + " WHERE cid = ? ) ";

    private final static String UPDATE_INVOICE =
            "UPDATE {{?" + DSMConst.D_COMP_INVOICE + "}}"
            + " SET taxpayer = ?, bankers = ?, account = ?, tel = ? "
            + " WHERE cstatus&1 = 0 AND cid = ? ";

    public Result getInvoice(AppContext appContext) {
        int compId = appContext.getUserSession().compId;

        List<Object[]> queryResult =
                BaseDAO.getBaseDAO().queryNative(QUERY_INVOICE_BASE, compId);

        InvoiceVO[] results = new InvoiceVO[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, results, InvoiceVO.class);

        return new Result().setQuery(results, null);
    }

    public Result saveInvoice(AppContext appContext) {
        int compId = appContext.getUserSession().compId;
        InvoiceVO frontVO = null;

        try {
            frontVO = new Gson()
                      .fromJson(appContext.param.json, InvoiceVO.class);

            if (frontVO == null) {
                throw new NullPointerException("VO is null!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Result().fail("参数错误！");
        }

        InvoiceVO[] query = (InvoiceVO[]) getInvoice(appContext).data;

        if (query.length == 0) {
            // insert
            BaseDAO.getBaseDAO().updateNative(INSERT_INVOICE, compId,
                    frontVO.getTaxpayer(), frontVO.getBankers(),
                    frontVO.getAccount (), frontVO.getTel(), compId);
        } else {
            // update
            BaseDAO.getBaseDAO().updateNative(UPDATE_INVOICE,
                    frontVO.getTaxpayer(), frontVO.getBankers(),
                    frontVO.getAccount (), frontVO.getTel(), compId);
        }

        return new Result().success("操作成功");
    }
}
