package com.onek.order;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.onek.context.AppContext;
import com.onek.entity.TranOrder;
import com.onek.entity.TranOrderGoods;
import com.onek.entitys.Result;
import com.onek.util.prod.ProdEntity;
import com.onek.util.prod.ProdInfoStore;
import constant.DSMConst;
import dao.BaseDAO;
import redis.util.RedisUtil;
import util.GsonUtils;
import util.MathUtil;
import util.StringUtils;
import util.TimeUtils;

import java.util.*;

public class BackOrderInfoModule {
    private static final String QUERY_TRAN_ORDER_PARAMS =
            " ord.orderno, ord.tradeno, ord.cusno, ord.busno, ord.ostatus, "
                    + " ord.asstatus, ord.pdnum, ord.pdamt, ord.freight, ord.payamt, "
                    + " ord.coupamt, ord.distamt, ord.rvaddno, ord.shipdate, ord.shiptime, "
                    + " ord.settstatus, ord.settdate, ord.setttime, ord.otype, ord.odate, "
                    + " ord.otime, ord.cstatus, ord.consignee, ord.contact, ord.address ";

    private static final String QUERY_TRAN_GOODS_PARAMS =
            " goods.unqid, goods.orderno, goods.compid, goods.pdno, goods.pdprice, "
                    + " goods.distprice, goods.payamt, goods.coupamt, goods.promtype, goods.pkgno,  "
                    + " goods.asstatus, goods.createdate, goods.createtime, goods.cstatus, goods.pnum ";

    private static final String FROM_BK_ORDER = " {{?" + DSMConst.TD_BK_TRAN_ORDER + "}} ord ";
    private static final String FROM_BK_GOODS = " {{?" + DSMConst.TD_BK_TRAN_GOODS + "}} goods ";

    private static final String QUERY_ORDER_BASE =
            " SELECT " + QUERY_TRAN_ORDER_PARAMS
                    + " FROM " + FROM_BK_ORDER
                    + " WHERE ord.cstatus&1 = 0 ";

    private static final String QUERY_ORDER_GOODS =
            " SELECT " + QUERY_TRAN_GOODS_PARAMS
                    + " FROM " + FROM_BK_GOODS
                    + " WHERE goods.cstatus&1 = 0 AND goods.orderno = ? ";

    public Result queryOrders(AppContext appContext) {
        String[] params = appContext.param.arrays;

        if (params == null || params.length == 0) {
            return new Result().fail("参数为空");
        }

        int compid = Integer.parseInt(params[0]);

        Page page = new Page();
        page.pageIndex = appContext.param.pageIndex;
        page.pageSize = appContext.param.pageNumber;

        PageHolder pageHolder = new PageHolder(page);

        TranOrder[] result = queryOrders(params, compid, pageHolder, page);

        return new Result().setQuery(result, pageHolder);
    }

    private TranOrder[] queryOrders(String[] params, int compid,
                                    PageHolder pageHolder, Page page) {
        StringBuilder sql = new StringBuilder(QUERY_ORDER_BASE);

        List<Object> paramList = new ArrayList<>();

        String param;
        for (int i = 0; i < params.length; i++) {
            param = params[i];

            if (StringUtils.isEmpty(param)) {
                continue;
            }

            try {
                switch (i) {
                    case 0:
                        sql.append(" AND ord.busno = ? ");
                        break;
                    case 1:
                        sql.append(" AND ord.cusno = ? ");
                        break;
                    case 2:
                        sql.append(" AND ord.ostatus = ? ");
                        break;
                    case 3:
                        sql.append(" AND ord.orderno = ? ");
                        break;
                    case 4:
                        sql.append(" AND ord.odate >= ? ");
                        break;
                    case 5:
                        sql.append(" AND ord.odate <= ? ");
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

        Map<String, String> compMap;
        for (TranOrder tranOrder : result) {
            String compStr = RedisUtil.getStringProvide().get(String.valueOf(tranOrder.getCusno()));

            compMap = GsonUtils.string2Map(compStr);

            if (compMap != null) {
                tranOrder.setCusname(compMap.get("storeName"));
            }

            tranOrder.setGoods(getOrderGoods(tranOrder.getOrderno(), compid));
            tranOrder.setPayamt(MathUtil.exactDiv(tranOrder.getPayamt(), 100).doubleValue());
            tranOrder.setFreight(MathUtil.exactDiv(tranOrder.getFreight(), 100).doubleValue());
            tranOrder.setPdamt(MathUtil.exactDiv(tranOrder.getPdamt(), 100).doubleValue());
            tranOrder.setDistamt(MathUtil.exactDiv(tranOrder.getDistamt(), 100).doubleValue());
            tranOrder.setCoupamt(MathUtil.exactDiv(tranOrder.getCoupamt(), 100).doubleValue());
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
            tranOrderGoods.setPayamt(MathUtil.exactDiv(tranOrderGoods.getPayamt(), 100).doubleValue());
            tranOrderGoods.setPdprice(MathUtil.exactDiv(tranOrderGoods.getPdprice(), 100).doubleValue());
            tranOrderGoods.setDistprice(MathUtil.exactDiv(tranOrderGoods.getDistprice(), 100).doubleValue());
            tranOrderGoods.setCoupamt(MathUtil.exactDiv(tranOrderGoods.getCoupamt(), 100).doubleValue());

            prod = ProdInfoStore.getProdBySku(tranOrderGoods.getPdno());

            if (prod != null) {
                tranOrderGoods.setPname(prod.getProdname());
                tranOrderGoods.setPspec(prod.getSpec());
                tranOrderGoods.setManun(prod.getManuName());
            }
        }

        return new ArrayList<>(Arrays.asList(result));
    }

}
