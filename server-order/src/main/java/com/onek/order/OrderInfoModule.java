package com.onek.order;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.onek.context.AppContext;
import com.onek.entity.AsAppVO;
import com.onek.entity.TranOrder;
import com.onek.entity.TranOrderDetail;
import com.onek.entity.TranOrderGoods;
import com.onek.entitys.Result;
import com.onek.util.IceRemoteUtil;
import com.onek.util.dict.DictStore;
import com.onek.util.member.MemberStore;
import com.onek.util.prod.ProdEntity;
import com.onek.util.prod.ProdInfoStore;
import constant.DSMConst;
import dao.BaseDAO;
import redis.util.RedisUtil;
import util.*;

import java.util.*;

public class OrderInfoModule {
    private static final String QUERY_TRAN_ORDER_PARAMS =
            " ord.orderno, ord.tradeno, ord.cusno, ord.busno, ord.ostatus, "
          + " ord.asstatus, ord.pdnum, ord.pdamt, ord.freight, ord.payamt, "
          + " ord.coupamt, ord.distamt, ord.rvaddno, ord.shipdate, ord.shiptime, "
          + " ord.settstatus, ord.settdate, ord.setttime, ord.otype, ord.odate, "
          + " ord.otime, ord.cstatus, ord.consignee, ord.contact, ord.address, ord.balamt, ord.payway ";

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
                    + " ON trans.paystatus = 1 AND trans.orderno = ord.orderno "
                    + " LEFT JOIN " + FROM_APPRAISE
                    + " ON  app.cstatus&1 = 0 "
                    + " AND app.orderno = ord.orderno "
            + " WHERE ord.cstatus&1 = 0 AND ord.orderno = ?"
            + " LIMIT 1 ";

    private static final String QUERY_COMP_ORDER_INFO_BASE =
            "SELECT COUNT(ostatus = 0 OR NULL), COUNT(ostatus = 1 OR NULL),"
                + " COUNT(ostatus = 2 OR NULL), COUNT(ostatus = 3 OR NULL),"
                + " COUNT(ostatus IN (-2, -3) OR NULL) "
                + " FROM {{?" + DSMConst.TD_TRAN_ORDER + "}} "
                + " WHERE cstatus&1 = 0 AND cusno = ? ";


    private static final String QUERY_ASAPP_BASE =
            " SELECT orderno, pdno, asno, compid, astype, "
                    + " gstatus, reason, ckstatus, ckdate, cktime, "
                    + " ckdesc, invoice, cstatus, apdata, aptime, "
                    + " apdesc, refamt, asnum "
                    + " FROM {{?" + DSMConst.TD_TRAN_ASAPP + "}} "
                    + " WHERE cstatus&1 = 0 AND compid = ? ";

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
                compid, TimeUtils.getYearByOrderno(params[1]), QUERY_ORDER_DETAIL, params[1]);

        TranOrderDetail[] result = new TranOrderDetail[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, result, TranOrderDetail.class);

        JSONObject compJson;
        for (TranOrderDetail tranOrder : result) {
            String compStr = RedisUtil.getStringProvide().get(String.valueOf(tranOrder.getCusno()));

            compJson = JSON.parseObject(compStr);

            if (compJson != null) {
                tranOrder.setCusname(compJson.getString("storeName"));
                tranOrder.setAreaAllName(
                        IceRemoteUtil.getCompleteName(compJson.getString("addressCode")));
            }

            tranOrder.setPayprice(
                    MathUtil.exactDiv(tranOrder.getPayprice(), 100).doubleValue());
            tranOrder.setGoods(getOrderGoods(tranOrder.getOrderno(), compid));
            tranOrder.setPayamt(MathUtil.exactDiv(tranOrder.getPayamt(), 100).doubleValue());
            tranOrder.setPdamt(MathUtil.exactDiv(tranOrder.getPdamt(), 100).doubleValue());
            tranOrder.setPayprice(MathUtil.exactDiv(tranOrder.getPayprice(), 100).doubleValue());
            tranOrder.setDistamt(MathUtil.exactDiv(tranOrder.getDistamt(), 100).doubleValue());
            tranOrder.setCoupamt(MathUtil.exactDiv(tranOrder.getCoupamt(), 100).doubleValue());
            tranOrder.setFreight(MathUtil.exactDiv(tranOrder.getFreight(), 100).doubleValue());
            tranOrder.setBalamt(MathUtil.exactDiv(tranOrder.getBalamt(), 100).doubleValue());

        }

        return new Result().success(result);
    }

    public Result queryOrders(AppContext appContext) {
        String[] params = appContext.param.arrays;

        if (ArrayUtil.isEmpty(params)) {
            return new Result().fail("参数为空");
        }

        int compid = appContext.getUserSession().compId;
        Page page = new Page();
        page.pageIndex = appContext.param.pageIndex;
        page.pageSize = appContext.param.pageNumber;

        PageHolder pageHolder = new PageHolder(page);

        String year = params[0];

        if (!StringUtils.isBiggerZero(year)) {
            year = "0";
        }

        params = ArrayUtil.unshift(ArrayUtil.shift(params), String.valueOf(compid));

        TranOrder[] result = queryOrders(params, compid, Integer.parseInt(year), pageHolder, page);

        return new Result().setQuery(result, pageHolder);
    }

    public TranOrder[] queryOrders(String[] params, int compid, int year,
                                    PageHolder pageHolder, Page page) {
        if (year == 0) {
            year = TimeUtils.getCurrentYear();
        }

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
                        sql.append(" AND ord.cusno = ? ");
                        break;
                    case 1:
                        sql.append(" AND ord.ostatus = ? ");
                        break;
                    case 2:
                        sql.append(" AND ord.orderno = ? ");
                        break;
                    case 3:
                        sql.append(" AND ord.asstatus = ? ");
                        break;
                }
            } catch (Exception e) {
                continue;
            }

            paramList.add(param);
        }

        List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNativeSharding(
                compid, year, pageHolder, page,
                " ord.oid DESC ", sql.toString(), paramList.toArray());

        TranOrder[] result = new TranOrder[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, result, TranOrder.class);

        Map<String, String> compMap;

        Map<Long, List<TranOrderGoods>> og = getOrderGoods(compid, year);

        for (TranOrder tranOrder : result) {
            String compStr = RedisUtil.getStringProvide().get(String.valueOf(tranOrder.getCusno()));

            compMap = GsonUtils.string2Map(compStr);

            if (compMap != null) {
                tranOrder.setCusname(compMap.get("storeName"));
            }

            tranOrder.setGoods(og.get(Long.parseLong(tranOrder.getOrderno())));
            tranOrder.setPayamt(MathUtil.exactDiv(tranOrder.getPayamt(), 100).doubleValue());
            tranOrder.setFreight(MathUtil.exactDiv(tranOrder.getFreight(), 100).doubleValue());
            tranOrder.setPdamt(MathUtil.exactDiv(tranOrder.getPdamt(), 100).doubleValue());
            tranOrder.setDistamt(MathUtil.exactDiv(tranOrder.getDistamt(), 100).doubleValue());
            tranOrder.setCoupamt(MathUtil.exactDiv(tranOrder.getCoupamt(), 100).doubleValue());
            tranOrder.setBalamt(MathUtil.exactDiv(tranOrder.getBalamt(), 100).doubleValue());
        }

        return result;
    }

    private Map<Long, List<TranOrderGoods>> getOrderGoods(int compid, int year) {
        String sql = " SELECT " + QUERY_TRAN_GOODS_PARAMS
                + " FROM " + FROM_GOODS
                + " WHERE goods.cstatus&1 = 0 AND goods.compid = ? "
                + " AND goods.orderno > 0 ";

        List<Object[]> queryResult =
                BaseDAO.getBaseDAO().queryNativeSharding(compid, year, sql, compid);

        TranOrderGoods[] result = new TranOrderGoods[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, result, TranOrderGoods.class);

        convTranGoods(result);

        Map<Long, List<TranOrderGoods>> returnResult = new HashMap<>();
        List<TranOrderGoods> s;
        for (TranOrderGoods tranOrderGoods : result) {
            s = returnResult.get(tranOrderGoods.getOrderno());

            if (s == null) {
                s = new ArrayList<>();
                returnResult.put(tranOrderGoods.getOrderno(), s);
            }

            s.add(tranOrderGoods);
        }

        return returnResult;
    }

    private List<TranOrderGoods> getOrderGoods(String orderno, int compid) {
        if (!StringUtils.isBiggerZero(orderno)) {
            return Collections.EMPTY_LIST;
        }

        List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNativeSharding(
                compid, TimeUtils.getYearByOrderno(orderno), QUERY_ORDER_GOODS, orderno);

        TranOrderGoods[] result = new TranOrderGoods[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, result, TranOrderGoods.class);

        convTranGoods(result);

        return new ArrayList<>(Arrays.asList(result));
    }

    private void convTranGoods(TranOrderGoods[] result) {
        ProdEntity prod;
        for (TranOrderGoods tranOrderGoods : result) {
            try {
                prod = ProdInfoStore.getProdBySku(tranOrderGoods.getPdno());
            } catch (Exception e) { prod=null; }
            tranOrderGoods.setPayamt(MathUtil.exactDiv(tranOrderGoods.getPayamt(), 100).doubleValue());
            tranOrderGoods.setPdprice(MathUtil.exactDiv(tranOrderGoods.getPdprice(), 100).doubleValue());
            tranOrderGoods.setDistprice(MathUtil.exactDiv(tranOrderGoods.getDistprice(), 100).doubleValue());
            tranOrderGoods.setCoupamt(MathUtil.exactDiv(tranOrderGoods.getCoupamt(), 100).doubleValue());
            tranOrderGoods.setSpu(
                    String.valueOf(tranOrderGoods.getPdno())
                            .substring(0, 12));

            if (prod != null) {
                tranOrderGoods.setPname(prod.getProdname());
                tranOrderGoods.setPspec(prod.getSpec());
                tranOrderGoods.setManun(prod.getManuName());
            }
        }
    }

    public Result countCompInfo(AppContext appContext) {
        int compid = appContext.getUserSession().compId;

        if (compid <= 0) {
            return new Result().success(
                    new int[] { 0, 0, 0, 0, 0, 0, 0 });
        }

        List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNativeSharding(
                                        compid, TimeUtils.getCurrentYear(),
                                        QUERY_COMP_ORDER_INFO_BASE, compid);

        int couponCount = new CouponRevModule().couponRevCount(compid);
        int pointsCount = MemberStore.getIntegralByCompid(compid);

        Object[] result = ArrayUtil.unshift(queryResult.get(0), couponCount, pointsCount);

        return new Result().success(result);
    }

    public Result queryAsapp(AppContext appContext) {
        int compid = appContext.getUserSession().compId;

        if (compid <= 0) {
            return new Result().fail("企业为空");
        }

        String[] params = appContext.param.arrays;

        if (params == null) {
            return new Result().fail("参数为空");
        }

        int year = TimeUtils.getCurrentYear();

        Page page = new Page();
        page.pageIndex = appContext.param.pageIndex;
        page.pageSize = appContext.param.pageNumber;

        PageHolder pageHolder = new PageHolder(page);

        StringBuilder sql = new StringBuilder(QUERY_ASAPP_BASE);

        List<Object> paramList = new ArrayList<>();

        paramList.add(compid);

        String param;
        for (int i = 0; i < params.length; i++) {
            param = params[i];

            if (StringUtils.isEmpty(param)) {
                continue;
            }

            try {
                switch (i) {
                    case 0:
                        sql.append(" AND orderno = ? ");
                        year = TimeUtils.getYearByOrderno(param);
                        break;
                    case 1:
                        sql.append(" AND pdno = ? ");
                        break;
                    case 2:
                        sql.append(" AND asno = ? ");
                        break;
                    case 3:
                        sql.append(" AND compid = ? ");
                        break;
                    case 4:
                        sql.append(" AND astype = ? ");
                        break;
                    case 5:
                        sql.append(" AND gstatus = ? ");
                        break;
                    case 6:
                        sql.append(" AND reason = ? ");
                        break;
                    case 7:
                        sql.append(" AND ckstatus = ? ");
                        break;
                    case 8:
                        sql.append(" AND ckdate >= ? ");
                        break;
                    case 9:
                        sql.append(" AND ckdate <= ? ");
                        break;
                    case 10:
                        sql.append(" AND apdata >= ? ");
                        break;
                    case 11:
                        sql.append(" AND apdata <= ? ");
                        break;
                }
            } catch (Exception e) {
                continue;
            }

            paramList.add(param);
        }

        List<Object[]> queryResult;

        try {
            queryResult = BaseDAO.getBaseDAO().queryNativeSharding(
                    0, year, pageHolder, page,
                    " apdata DESC, aptime DESC ",
                    sql.toString(), paramList.toArray());
        } catch (Exception e) {
            return new Result().setQuery(new AsAppVO[0], pageHolder);
        }


        AsAppVO[] result = new AsAppVO[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, result, AsAppVO.class);

        ProdEntity prod;
        for (AsAppVO asAppVO : result) {
            asAppVO.setRefamt(MathUtil.exactDiv(asAppVO.getRefamt(), 100.0).doubleValue());
            try {
                asAppVO.setReasonName(DictStore.getDictById(asAppVO.getReason()).getText());
            } catch (Exception e) {}

            try {
                prod = ProdInfoStore.getProdBySku(asAppVO.getPdno());
            } catch (Exception e) { prod=null; }

            if (prod != null) {
                asAppVO.setProdname(prod.getProdname());
                asAppVO.setSpec(prod.getSpec());
                asAppVO.setBrandname(prod.getBrandName());
                asAppVO.setManuname(prod.getManuName());
                asAppVO.setSpu(String.valueOf(asAppVO.getPdno())
                        .substring(0, 12));
            }
        }


        return new Result().setQuery(result, pageHolder);
    }




}
