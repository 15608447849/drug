package com.onek.order;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.onek.context.AppContext;
import com.onek.entity.TranOrder;
import com.onek.entity.TranOrderDetail;
import com.onek.entity.TranOrderGoods;
import com.onek.entitys.Result;
import com.onek.util.prod.ProdEntity;
import com.onek.util.prod.ProdInfoStore;
import constant.DSMConst;
import dao.BaseDAO;
import util.ArrayUtil;
import util.MathUtil;
import util.StringUtils;
import util.TimeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OrderInfoModule {
    private static final String QUERY_TRAN_ORDER_PARAMS =
            " ord.orderno, ord.tradeno, ord.cusno, ord.busno, ord.ostatus, "
          + " ord.asstatus, ord.pdnum, ord.pdamt, ord.freight, ord.payamt, "
          + " ord.coupamt, ord.distamt, ord.rvaddno, ord.shipdate, ord.shiptime, "
          + " ord.settstatus, ord.settdate, ord.setttime, ord.otype, ord.odate, "
          + " ord.otime, ord.cstatus ";

    private static final String QUERY_TRAN_TRANS_PARAMS =
            " trans.payno, trans.payprice, "
          + " trans.payway, trans.paysource, trans.paystatus, trans.payorderno, trans.tppno, "
          + " trans.paydate, trans.paytime, trans.completedate, trans.completetime, trans.cstatus ";

    private static final String QUERY_TRAN_GOODS_PARAMS =
            " goods.unqid, goods.orderno, goods.compid, goods.pdno, goods.pdprice, "
          + " goods.distprice, goods.payamt, goods.coupamt, goods.promtype, goods.pkgno,  "
          + " goods.asstatus, goods.createdate, goods.createtime, goods.cstatus, goods.pnum ";

    private static final String QUERY_TRAN_APPRAISE_PARAMS =
            " app.level, app.descmatch, app.logisticssrv, "
          + " app.content, app.createtdate, app.createtime, app.cstatus ";


    private static final String FROM_ORDER = " {{?" + DSMConst.TD_TRAN_ORDER + "}} ord ";
    private static final String FROM_GOODS = " {{?" + DSMConst.TD_TRAN_GOODS + "}} goods ";
    private static final String FROM_TRANS = " {{?" + DSMConst.TD_TRAN_TRANS + "}} trans ";
    private static final String FROM_APPRAISE = " {{?" + DSMConst.TD_TRAN_APPRAISE + "}} app ";

    private static final String QUERY_ORDER_BASE =
            " SELECT " + QUERY_TRAN_ORDER_PARAMS
            + " FROM " + FROM_ORDER
            + " WHERE ord.cstatus&1 = 0 ";

    private static final String QUERY_ORDER_GOODS =
            " SELECT " + QUERY_TRAN_GOODS_PARAMS
            + " FROM " + FROM_GOODS
            + " WHERE goods.cstatus&1 = 0 AND goods.orderno = ? ";

    private static final String QUERY_ORDER_DETAIL =
            " SELECT " + QUERY_TRAN_ORDER_PARAMS
                    + ", " + QUERY_TRAN_TRANS_PARAMS
                    + ", " + QUERY_TRAN_APPRAISE_PARAMS
            + " FROM " + FROM_ORDER
                    + " LEFT JOIN " + FROM_TRANS
                    + " ON trans.cstatus&1 = 0 "
                    + " AND trans.paystatus = 1 AND trans.orderno = ord.orderno "
                    + " LEFT JOIN " + FROM_APPRAISE
                    + " ON app.cstatus&1 = 0 "
                    + " AND app.orderno = ord.orderno "
            + " WHERE ord.cstatus&1 = 0 AND ord.orderno = ? ";

    public Result getOrderDetail(AppContext appContext) {
        String[] params = appContext.param.arrays;

        if (params == null || params.length == 0) {
            return new Result().fail("参数为空");
        }

        if (!StringUtils.isBiggerZero(params[0])) {
            return new Result().fail("非法参数");
        }

        int compid = Integer.parseInt(params[0]);

        List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNativeSharding(
                compid, TimeUtils.getCurrentYear(), QUERY_ORDER_DETAIL, params[1]);

        TranOrderDetail[] result = new TranOrderDetail[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, result, TranOrderDetail.class);

        for (TranOrderDetail tranOrder : result) {
            tranOrder.setPayprice(
                    MathUtil.exactDiv(tranOrder.getPayprice(), 100).doubleValue());
            tranOrder.setGoods(getOrderGoods(tranOrder.getOrderno(), compid));
        }

        return new Result().success(result);
    }

    public Result queryOrders(AppContext appContext) {
        String[] params = appContext.param.arrays;

        if (params == null || params.length == 0) {
            return new Result().fail("参数为空");
        }

        int compid = appContext.getUserSession().compId;

        Page page = new Page();
        page.pageIndex = appContext.param.pageIndex;
        page.pageSize = appContext.param.pageNumber;

        PageHolder pageHolder = new PageHolder(page);

        params = ArrayUtil.unshift(params, String.valueOf(compid));

        TranOrder[] result = queryOrders(params, compid, pageHolder, page);

        return new Result().setQuery(result, pageHolder);
    }

    private TranOrder[] queryOrders(String[] params, int compid,
                                    PageHolder pageHolder, Page page) {
        StringBuilder sql = new StringBuilder(QUERY_ORDER_BASE);

        List<Object> paramList = new ArrayList<>();

        String param = null;
        for (int i = 0; i < params.length; i++) {
            param = params[i];

            if (StringUtils.isEmpty(param)) {
                continue;
            }

            try {
                switch (i) {
                    case 0:
                        sql.append(" AND ord.cusno = ? ");
                        break;
                    case 1:
                        sql.append(" AND ord.ostatus = ? ");
                        break;
                    case 2:
                        sql.append(" AND ord.orderno = ? ");
                        break;
                }
            } catch (Exception e) {
                continue;
            }

            paramList.add(param);
        }

        List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNativeSharding(
                compid, TimeUtils.getCurrentYear(), pageHolder, page,
                " ord.oid DESC ", sql.toString(), paramList.toArray());

        TranOrder[] result = new TranOrder[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, result, TranOrder.class);

        for (TranOrder tranOrder : result) {
            tranOrder.setGoods(getOrderGoods(tranOrder.getOrderno(), compid));
        }

        return result;
    }

    private List<TranOrderGoods> getOrderGoods(String orderno, int compid) {
        if (!StringUtils.isBiggerZero(orderno)) {
            return Collections.EMPTY_LIST;
        }

        List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNativeSharding(
                compid, TimeUtils.getCurrentYear(), QUERY_ORDER_GOODS, orderno);

        TranOrderGoods[] result = new TranOrderGoods[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, result, TranOrderGoods.class);

        ProdEntity prod;
        for (TranOrderGoods tranOrderGoods : result) {
            prod = ProdInfoStore.getProdBySku(tranOrderGoods.getPdno());
            tranOrderGoods.setPayamt(MathUtil.exactDiv(tranOrderGoods.getPayamt(), 100).doubleValue());
            tranOrderGoods.setPdprice(MathUtil.exactDiv(tranOrderGoods.getPdprice(), 100).doubleValue());
            tranOrderGoods.setDistprice(MathUtil.exactDiv(tranOrderGoods.getDistprice(), 100).doubleValue());
            tranOrderGoods.setCoupamt(MathUtil.exactDiv(tranOrderGoods.getCoupamt(), 100).doubleValue());

            if (prod != null) {
                tranOrderGoods.setPname(prod.getProdname());
                tranOrderGoods.setPspec(prod.getSpec());
                tranOrderGoods.setManun(prod.getManuName());
            }
        }

        return new ArrayList<>(Arrays.asList(result));
    }

}