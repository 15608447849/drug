package com.onek.order;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonParser;
import com.onek.calculate.entity.Gift;
import com.onek.context.AppContext;
import com.onek.entity.*;
import com.onek.entitys.Result;
import com.onek.prop.AppProperties;
import com.onek.util.IceRemoteUtil;
import com.onek.util.dict.DictStore;
import com.onek.util.member.MemberStore;
import com.onek.util.prod.ProdEntity;
import com.onek.util.prod.ProdInfoStore;
import constant.DSMConst;
import dao.BaseDAO;
import org.hyrdpf.util.LogUtil;
import redis.util.RedisUtil;
import util.*;
import util.http.HttpRequestUtil;

import java.io.IOException;
import java.util.*;

public class OrderInfoModule {
    private static final String QUERY_TRAN_ORDER_PARAMS =
            " ord.orderno, ord.tradeno, ord.cusno, ord.busno, ord.ostatus, "
          + " ord.asstatus, ord.pdnum, ord.pdamt, ord.freight, ord.payamt, "
          + " ord.coupamt, ord.distamt, ord.rvaddno, ord.shipdate, ord.shiptime, "
          + " ord.settstatus, ord.settdate, ord.setttime, ord.otype, ord.odate, "
          + " ord.otime, ord.cstatus, ord.consignee, ord.contact, ord.address, ord.balamt, ord.payway, ord.remarks, ord.invoicetype ";

    private static final String QUERY_TRAN_TRANS_PARAMS =
            " trans.payno, trans.payprice, "
          + " trans.payway, trans.paysource, trans.paystatus, trans.payorderno, trans.tppno, "
          + " trans.paydate, trans.paytime, trans.completedate, trans.completetime, trans.cstatus ";

    private static final String QUERY_TRAN_GOODS_PARAMS =
            " goods.unqid, goods.orderno, goods.compid, goods.pdno, goods.pdprice, "
          + " goods.distprice, goods.payamt, goods.coupamt, goods.promtype, goods.pkgno,  "
          + " goods.asstatus, goods.createdate, goods.createtime, goods.cstatus, goods.pnum,"
          + " goods.actcode, goods.balamt ";

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
            + " WHERE ord.orderno = ?"
            + " LIMIT 1 ";

    private static final String QUERY_COMP_ORDER_INFO_BASE =
            "SELECT COUNT(ostatus = 0 OR NULL), COUNT(ostatus = 1 OR NULL),"
                + " COUNT(ostatus = 2 OR NULL), COUNT(ostatus = 3 OR NULL),"
                + " COUNT(ostatus IN (-2, -3) OR NULL) "
                + " FROM {{?" + DSMConst.TD_TRAN_ORDER + "}} "
                + " WHERE cstatus&1 = 0 AND cusno = ? ";


    private static final String QUERY_ASAPP_BASE =
            " SELECT ap.orderno, ap.pdno, ap.asno, ap.compid, ap.astype, "
                    + " ap.gstatus, ap.reason, ap.ckstatus, ap.ckdate, ap.cktime, "
                    + " ap.ckdesc, ap.invoice, ap.cstatus, ap.apdata, ap.aptime, "
                    + " ap.apdesc, ap.refamt, ap.asnum, ap.checkern, ap.compn, ap.invoicetype, "
                    + " goods.pdprice, goods.distprice, goods.payamt, goods.coupamt, "
                    + " goods.asstatus, goods.createdate, goods.createtime, goods.pnum, "
                    + " goods.balamt "
                    + " FROM {{?" + DSMConst.TD_TRAN_ASAPP + "}} ap "
                    + " LEFT JOIN {{?" + DSMConst.TD_BK_TRAN_GOODS + "}} goods "
                    + " ON goods.cstatus&1 = 0 "
                    + " AND goods.orderno = ap.orderno AND ap.pdno = goods.pdno "
                    + " WHERE ap.cstatus&1 = 0 AND ap.compid = ? ";

    private final static String QUERY_INVOICE_BASE =
            "SELECT i.oid, i.cid, a.certificateno, i.bankers, i.account, i.tel, i.cstatus, i.email "
                    + " FROM {{?" + DSMConst.TB_COMP_INVOICE + "}} i "
                    + " LEFT JOIN {{?" + DSMConst.TB_COMP_APTITUDE + "}} a "
                    + " ON a.cstatus&1 = 0 AND a.atype = 10 "
                    + " AND i.cid = a.compid "
//                + " AND a.validitys <= CURRENT_DATE "
//                + " AND CURRENT_DATE <= a.validitye"
                    + " WHERE i.cstatus&1 = 0 AND i.cid = ? ";

    /**
     * @接口摘要 获取订单详情
     * @业务场景 获取订单详情
     * @传参类型 arrays
     * @传参列表 [compid, orderno]
     * @返回列表 JSONObject:
     *              goods 订单详情
     *              gifts 赠品列表
     */

    public Result getOrderDetail(AppContext appContext) {
        String[] params = appContext.param.arrays;

        if (params == null || params.length == 0) {
            return new Result().fail("参数为空");
        }

        if (!StringUtils.isBiggerZero(params[0])) {
            return new Result().fail("非法参数");
        }

        int compid = Integer.parseInt(params[0]);
        String orderno = params[1];
        List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNativeSharding(
                compid, TimeUtils.getYearByOrderno(params[1]), QUERY_ORDER_DETAIL, orderno);

        TranOrderDetail[] result = new TranOrderDetail[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, result, TranOrderDetail.class);

        List<Object[]> invoiceResult = IceRemoteUtil.queryNative(QUERY_INVOICE_BASE, compid);

        InvoiceVO[] results = new InvoiceVO[invoiceResult.size()];

        BaseDAO.getBaseDAO().convToEntity(invoiceResult, results, InvoiceVO.class);

        JSONObject compJson;
        List<TranOrderGoods> goods;
        for (TranOrderDetail tranOrder : result) {
            String compStr = RedisUtil.getStringProvide().get(String.valueOf(tranOrder.getCusno()));

            compJson = JSON.parseObject(compStr);

            if (compJson != null) {
                tranOrder.setCusname(compJson.getString("storeName"));
                tranOrder.setCusaddress(compJson.getString("address"));
                tranOrder.setAreaAllName(
                        IceRemoteUtil.getCompleteName(compJson.getString("addressCode")));
            }

            if (results.length > 0) {
                tranOrder.setInvoice(results[0]);
            }

            tranOrder.setPayprice(
                    MathUtil.exactDiv(tranOrder.getPayprice(), 100).doubleValue());
            tranOrder.setGoods(goods = getOrderGoods(tranOrder.getOrderno(), compid));
            tranOrder.setPayamt(MathUtil.exactDiv(tranOrder.getPayamt(), 100).doubleValue());
            tranOrder.setPdamt(MathUtil.exactDiv(tranOrder.getPdamt(), 100).doubleValue());
            tranOrder.setPayprice(MathUtil.exactDiv(tranOrder.getPayprice(), 100).doubleValue());
            tranOrder.setDistamt(MathUtil.exactDiv(tranOrder.getDistamt(), 100).doubleValue());
            tranOrder.setCoupamt(MathUtil.exactDiv(tranOrder.getCoupamt(), 100).doubleValue());
            tranOrder.setFreight(MathUtil.exactDiv(tranOrder.getFreight(), 100).doubleValue());
            tranOrder.setBalamt(MathUtil.exactDiv(tranOrder.getBalamt(), 100).doubleValue());
            tranOrder.setClassNum(goods.size());
            int totalNum = 0;
            for (TranOrderGoods good : goods) {
                totalNum += good.getPnum();
            }
            tranOrder.setTotalNum(totalNum);
        }


        List<Gift> gifts =
                CouponRevModule.getGifts(Long.parseLong(orderno), compid);

        List<TranOrderGoods> giftVOS = new ArrayList<>();

        if (!gifts.isEmpty()) {
            TranOrderGoods good;
            for (Gift gift : gifts) {
                if (gift.getType() != 3) {
                    continue;
                }

                good = new TranOrderGoods();
                good.setPdno(gift.getId());
                good.setActcode(String.valueOf(gift.getActivityCode()));
                good.setPnum(gift.getTotalNums());
                good.setPname(gift.getGiftName());
                giftVOS.add(good);
            }
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("goods", result);
        jsonObject.put("gifts", giftVOS);

        return new Result().success(jsonObject);
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
                    case 4:
                        sql.append(" AND ? <= ord.odate ");
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
//
//    static {
//        /**初始化LOG4J2日志环境*/
//        AppConfig.initLogger("log4j2.xml");
//        /**初始化应用程序环境，如数据源等*/
//        AppConfig.initialize();
//    }
//
//    public static void main(String[] args) {
//        new OrderInfoModule().getOrderGoods(536862725, 2019);
//    }

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
            s = returnResult.get(Long.parseLong(tranOrderGoods.getOrderno()));

            if (s == null) {
                s = new ArrayList<>();
                returnResult.put(Long.parseLong(tranOrderGoods.getOrderno()), s);
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

        List<TranOrderGoods> resultList = new ArrayList<>(Arrays.asList(result));

        return resultList;
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
            tranOrderGoods.setBalamt(MathUtil.exactDiv(tranOrderGoods.getBalamt(), 100).doubleValue());
            tranOrderGoods.setSpu(
                    String.valueOf(tranOrderGoods.getPdno())
                            .substring(0, 12));

            if (prod != null) {
                tranOrderGoods.setVatp(MathUtil.exactDiv(prod.getVatp(), 100).doubleValue());
                tranOrderGoods.setRrp(MathUtil.exactDiv(prod.getRrp(), 100).doubleValue());
                tranOrderGoods.setMp(MathUtil.exactDiv(prod.getMp(), 100).doubleValue());
                tranOrderGoods.setPname(prod.getProdname());
                tranOrderGoods.setPspec(prod.getSpec());
                tranOrderGoods.setManun(prod.getManuName());
                tranOrderGoods.setStandarNo(prod.getStandarNo());
                tranOrderGoods.setBrandn(prod.getBrandName());
            }
        }
    }

    /**
     * @接口摘要 统计企业信息
     * @业务场景 企业展示信息
     * @传参类型 -
     * @传参列表 -
     * @返回列表 code=200 data=[优惠券数，积分数，未付款数，未发货数，未签收数，未评价数，退货中数]
     */

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

//    public Result getAsapp(AppContext appContext) {
//        String[] arrays = appContext.param.arrays;
//
//        if (ArrayUtil.isEmpty(arrays)) {
//            return new Result().fail("参数为空");
//        }
//
//        int compid = Integer.parseInt(arrays[0]);
//        String asno  = arrays[1];
//
//        String sql = " SELECT "
//                + " FROM {{?" + DSMConst.TD_TRAN_ASAPP + "}} ap "
//                + " LEFT JOIN {{?" + DSMConst.TD_TRAN_GOODS + "}} goods "
//                + " ON ap.cstatus&1 = 0 AND goods.cstatus&1 = 0"
//                + " AND ap.asno = ? AND ap.compid = ? "
//                + " AND goods.orderno = ap.orderno AND ap.pdno = goods.pdno "
//                + " WHERE 1 = 1 ";
//
//        List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNativeSharding(compid, TimeUtils.getYearByOrderno(asno), sql, asno, compid);
//
//
//
//    }
    /**
     * @接口摘要 查询售后列表
     * @业务场景 查询售后列表
     * @传参类型 arrays
     * @传参列表 [订单号, sku, 售后单号, 企业码, 售后类型, 货物状态, 原因, 审核状态, 审核时间起, 审核时间止, 申请时间起, 申请时间止]
     * @返回列表 code=200 data=结果信息
     */
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
                        year = TimeUtils.getYearByOrderno(param);
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
            asAppVO.setPdprice(MathUtil.exactDiv(asAppVO.getPdprice(), 100.0).doubleValue());
            asAppVO.setDistprice(MathUtil.exactDiv(asAppVO.getDistprice(), 100.0).doubleValue());
            asAppVO.setPayamt(MathUtil.exactDiv(asAppVO.getPayamt(), 100.0).doubleValue());
            asAppVO.setCoupamt(MathUtil.exactDiv(asAppVO.getCoupamt(), 100.0).doubleValue());
            asAppVO.setBalamt(MathUtil.exactDiv(asAppVO.getBalamt(), 100.0).doubleValue());

            try {
                asAppVO.setReasonName(DictStore.getDictById(asAppVO.getReason()).getText());
            } catch (Exception e) {}

            if (asAppVO.getPdno() == 0) {
                prod = null;
                //TODO 查询发票
            } else {
                try {
                    prod = ProdInfoStore.getProdBySku(asAppVO.getPdno());
                } catch (Exception e) { prod=null; }
            }

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


    /**
     * app端调用，查询企业门店信息，门店积分，门店优惠券，门店余额信息
     * add by liaoz 2019年6月11日
     * @param appContext 参入参数， 获取session.getCompId ,当前企业码查询
     * @return （200==成功，对象object[]对象size=7 [优惠券数量,积分余额，待付数量，待发货数量，待收货数量，待评价数量，退货数量]）
     * 内部复用countCompInfo方法。
     */
    public Result appCountCompInfo(AppContext appContext){
        return countCompInfo(appContext);
    }

    /**
     * @接口摘要 获取药监报告地址
     * @业务场景 订单内查看药监报告
     * @传参类型 json
     * @传参列表 {sku:sku, compid:购买方企业码, orderno:订单号}
     * @返回列表 code=200 data=结果信息
     */
    public Result getSOAPath(AppContext appContext) {
        String url = AppProperties.INSTANCE.erpUrlPrev + "/getSOAPath";
        String result;
        JSONObject params = JSONObject.parseObject(appContext.param.json);
        try {
            if (params.getIntValue("compid") <= 0) {
                return new Result().fail("参数错误");
            }

            String erpSKU = IceRemoteUtil.getErpSKU(params.getString("sku"));

            if (erpSKU == null || "null".equalsIgnoreCase(erpSKU)) {
                return new Result().fail("获取失败");
            }

            params.put("erpsku", erpSKU);

            result = HttpRequestUtil.postJson(url, params.toJSONString());
        } catch (Exception e) {
            e.printStackTrace();
            return new Result().fail("接口调用失败");
        }

        if (result != null && !result.isEmpty()) {
            JSONObject jo = JSON.parseObject(result);

            if (jo.getIntValue("code") == 200) {//同步失败处理
                return new Result().success(jo.getString("path"));
            }
        }

        return new Result().fail("获取失败");
    }

    /**
     * @接口摘要 获取发票下载链接
     * @业务场景 订单内查看发票
     * @传参类型 json
     * @传参列表 {compid:购买方企业码, orderno:订单号}
     * @返回列表 code=200 data=结果信息
     */
    public Result getINVPath(AppContext appContext) {
        String url = AppProperties.INSTANCE.erpUrlPrev + "/getINVPath";
        String result;
        try {
            result = HttpRequestUtil.postJson(url, appContext.param.json);
        } catch (Exception e) {
            e.printStackTrace();
            return new Result().fail("获取失败");
        }

        if (result != null && !result.isEmpty()) {
            JSONObject jo = JSON.parseObject(result);

            if (jo.getIntValue("code") == 200) {//同步失败处理
                return new Result().success(jo.getString("path"));
            }
        }

        return new Result().fail("获取失败");
    }

}
