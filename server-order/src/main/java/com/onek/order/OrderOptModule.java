package com.onek.order;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.context.UserSession;
import com.onek.entity.AsAppDtListVO;
import com.onek.entity.AsAppDtVO;
import com.onek.entity.AsAppVO;
import com.onek.entity.TranOrderGoods;
import com.onek.entitys.Result;
import com.onek.op.RefundOp;
import com.onek.server.infimp.IceDebug;
import com.onek.util.*;
import com.onek.util.prod.ProdEntity;
import com.onek.util.prod.ProdInfoStore;
import constant.DSMConst;
import dao.BaseDAO;
import org.dom4j.Element;
import org.hyrdpf.util.LogUtil;
import org.joda.time.LocalDate;
import redis.util.RedisUtil;
import util.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.onek.order.TranOrderOptModule.CONFIRM_RECEIPT;
import static constant.DSMConst.TD_BK_TRAN_GOODS;
import static constant.DSMConst.TD_BK_TRAN_ORDER;

/**
 * @author 11842
 * @version 1.1.1
 * @服务名 orderServer
 * @description 订单其他操作模块
 * @time 2019/4/20 14:27
 **/
public class OrderOptModule {
    private static BaseDAO baseDao = BaseDAO.getBaseDAO();


    //售后申请
    private static final String INSERT_ASAPP_SQL = "insert into {{?" + DSMConst.TD_TRAN_ASAPP + "}} "
            + "(orderno,pdno,asno,compid,astype,gstatus,reason,ckstatus,"
            + "ckdesc,invoice,cstatus,apdata,aptime,apdesc,refamt,asnum,invoicetype, pkgno) "
            + " values(?,?,?,?,?,"
            + "?,?,?,?,?,"
            + "?,CURRENT_DATE,CURRENT_TIME,?,?,?,?,?)";

    //更新订单售后状态
    private static final String UPD_ORDER_SQL = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set ostatus=? "
            + " where cstatus&1=0 and orderno=? and ostatus in(3,4)";


    //更新订单相关商品售后状态
    private static final String UPD_ORDER_GOODS_SQL = "update {{?" + DSMConst.TD_TRAN_GOODS + "}} set asstatus=? "
            + " where cstatus&1=0 and pdno=? and asstatus=? and orderno=? and compid = ? and pkgno = ? ";

    //更新售后表售后状态
    private static final String UPD_ASAPP_SQL = "update {{?" + DSMConst.TD_TRAN_ASAPP + "}} set ckstatus=?,"
            + " where cstatus&1=0 and pdno=? and orderno = ? and compid = ?";


    //更新售后表售后审核状态(-1－拒绝； 0－未审核 ；1－审核通过)
    private static final String UPD_ASAPP_CK_SQL = "update {{?" + DSMConst.TD_TRAN_ASAPP + "}} set ckstatus=?, "
            + "ckdate = CURRENT_DATE,cktime = CURRENT_TIME,checker = ?,checkern = ?,astype = ?,ckdesc = ?,refamt = ?,realrefamt = ?" +
            " where cstatus&1=0 and  ckstatus = 0 and asno=?  ";


    //查询售后详情
    private static final String QUERY_ASAPP_INFO_SQL = " select asapp.orderno,asapp.compid,asapp.asno,asapp.pdno," +
            "asapp.asnum,goods.pdprice/100 spdprice," +
            "goods.payamt/100 spayamt,distprice/100 sdistprice,goods.pnum,astype,reason,apdesc,refamt/100 refamt,realrefamt/100 realrefamt," +
            "ckstatus,ckdesc,gstatus,ckdate,cktime,apdata,aptime,asapp.cstatus,goods.balamt/100 balamt, asapp.pkgno " +
            " from {{?" + DSMConst.TD_TRAN_ASAPP + "}} asapp inner join {{?" +
            TD_BK_TRAN_GOODS + "}} goods on asapp.orderno = goods.orderno and " +
            " asapp.pdno = goods.pdno and asapp.pkgno = goods.pkgno where asapp.asno = ? and asno != 0 ";


    //查询售后列表
    private static final String QUERY_ASAPP_LIST_SQL =
            "  select distinct asapp.orderno,asapp.compid,asapp.asno,astype," +
                    "ckstatus,gstatus,apdata,aptime,checkern,contact,address,refamt/100 refamt,compn,asapp.pkgno from " +
                    " {{?" + DSMConst.TD_TRAN_ASAPP + "}} asapp inner join {{?" +
                    TD_BK_TRAN_GOODS + "}} goods on asapp.orderno = goods.orderno and " +
                    " asapp.pdno = goods.pdno and asapp.compid = goods.compid and asapp.pkgno = goods.pkgno " +
                    " inner join {{?" + DSMConst.TD_BK_TRAN_ORDER + "}} orders" +
                    " on orders.orderno = goods.orderno where asapp.cstatus & 1 = 0 and asno != 0 ";

    //查询发票售后详情
    private static final String QUERY_ASAPP_INVOICE_INFO_SQL =
            "  select asapp.orderno,asapp.compid,asapp.asno,asapp.pdno,asapp.asnum," +
                    "astype,reason,apdesc,refamt,ckstatus,ckdesc,gstatus,ckdate,cktime,apdata,aptime,asapp.cstatus,invoice,asapp.invoicetype "
                    +
                    " from {{?" + DSMConst.TD_TRAN_ASAPP + "}} asapp  inner join {{?" + DSMConst.TD_BK_TRAN_ORDER
                    + "}} orders " +
                    " on asapp.orderno = orders.orderno where asapp.cstatus & 1 = 0 and asapp.asno = ? and asno != 0";

    //查询发票售后详情
    private static final String QUERY_GOODS_SQL =
            " select goods.pdno,goods.pnum,goods.pdprice,goods.distprice,goods.payamt from {{?" +
                    TD_BK_TRAN_GOODS + "}} goods where goods.orderno = ? and goods.compid = ?";

    //查询售后发票列表
    private static final String QUERY_ASAPP_BILL_LIST_SQL =
            "select distinct asapp.orderno,asapp.compid,asapp.asno,astype," +
                    "ckstatus,gstatus,apdata,aptime,checkern,contact,address,compn,asapp.invoicetype from " +
                    " {{?" + DSMConst.TD_TRAN_ASAPP + "}} asapp " +
                    " inner join {{?" + DSMConst.TD_BK_TRAN_ORDER + "}} orders" +
                    " on asapp.orderno = orders.orderno where asno != 0 and asapp.cstatus & 1 = 0 and asapp.astype in (3,4) ";


    //查询团购订单记录
    private static final String QUERY_TEAM_BUY_ORDER_SQL = "select g.orderno,g.pdprice as payamt,g.compid,g.pnum from " +
            " {{?" + TD_BK_TRAN_GOODS + "}} g , {{? "+ TD_BK_TRAN_ORDER+"}} o " +
            " where g.orderno = o.orderno and o.settstatus = 1 and g.createdate >= ? and g.createdate <= ? and g.promtype& 4096 > 0  and g.actcode like concat('%',?,'%')";

    //更新订单售后状态
    private static final String UPD_ORDER_CK_SQL = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set ostatus=? "
            + " where cstatus&1=0 and orderno=? and ostatus = ? ";

    //更新订单相关商品售后状态
    private static final String UPD_ORDER_GOODS_CK_SQL = "update {{?" + DSMConst.TD_TRAN_GOODS + "}} set asstatus=? "
            + " where cstatus&1=0 and pdno=? and asstatus= ? and orderno=? and compid  = ? and pkgno = ? ";


    //查询分摊余额
    private static final String QUERY_ORDER_GOODS_BAL_SQL = "select balamt from {{?" + DSMConst.TD_TRAN_GOODS + "}}   "
            + " where cstatus&1=0 and orderno = ? and pdno = ? and compid = ? and pkgno = ? ";


    private static final String UPD_ASAPP_CK_FINISH_SQL =
            "update {{?" + DSMConst.TD_TRAN_ASAPP + "}} set ckstatus = ? " +
                    " where cstatus&1=0 and asno = ?  ";


    /**
     * @接口摘要 订单评价商品接口
     * @业务场景 订单评价
     * @传参类型 json
     * @传参列表 {orderno: 订单号 compid: 企业码 appriseArr: 评价数组[见AppriseVO.class]}
     * @返回列表 200 成功 -1 失败
     */
    @UserPermission(ignore = false)
    public Result insertApprise(AppContext appContext) {
        boolean b = false;
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String orderNo = jsonObject.get("orderno").getAsString();
        int compid = jsonObject.get("compid").getAsInt();
        int year = Integer.parseInt("20" + orderNo.substring(0, 2));
        //更新订单状态为已评价
        String updSQL = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set cstatus=cstatus|128 "
                + " where cstatus&1=0 and orderno=" + orderNo + " and ostatus in(3,4)";
        if (baseDao.updateNativeSharding(compid, year, updSQL) > 0) {
            b = IceRemoteUtil.insertApprise(json) > 0;
        }

        return b ? result.success("感谢您的评价!","评价成功!") : result.fail("评价失败!");
    }


    /**
     * @接口摘要 订单售后申请接口
     * @业务场景 申请售后
     * @传参类型 json
     * @传参列表 {asAppArr:评价数组[AsAppVO.class] orderno: 订单号 asType：售后类型}
     * @返回列表 200 成功 -1 失败
     */
    @UserPermission(ignore = false)
    public Result afterSaleApp(AppContext appContext) {
        boolean b;
        Result result = new Result();
        LocalDateTime localDateTime = LocalDateTime.now();
        List<String> sqlList = new ArrayList<>();
        List<Object[]> params = new ArrayList<>();
        List<Object[]> paramsInsert = new ArrayList<>();
        List<AsAppVO> asAppVOS = new ArrayList<>();//sku集合
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        JsonArray asAppArr = jsonObject.get("asAppArr").getAsJsonArray();
        String orderNo = jsonObject.get("orderno").getAsString();
        int compid = appContext.getUserSession().compId;
        int asType = jsonObject.get("asType").getAsInt();//astype 售后类型(0 换货 1退款退货 2 仅退款 3 开发票)
        if (StringUtils.isEmpty(orderNo) || compid <= 0) {
            return result.fail("申请参数有误");
        }
        for (int i = 0; i < asAppArr.size(); i++) {
            AsAppVO asAppVO = GsonUtils.jsonToJavaBean(asAppArr.get(i).toString(), AsAppVO.class);
            if (asAppVO != null) {
                asAppVO.setAstype(asType);
                asAppVOS.add(asAppVO);
            }
        }
        if (doApply(asAppVOS, orderNo, asType)) {
            return result.fail("该订单售后申请正在处理中！");
        }

        int year = Integer.parseInt("20" + orderNo.substring(0, 2));
        if (asType == 0 || asType == 1 || asType == 2) {
            //更新订单售后状态
            sqlList.add(UPD_ORDER_SQL);
            params.add(new Object[]{-1, orderNo});
            getUpdGoodsSql(asAppVOS, sqlList, params, orderNo, compid);
            String[] sqlNative = new String[sqlList.size()];
            sqlNative = sqlList.toArray(sqlNative);
            b = !ModelUtil.updateTransEmpty(baseDao.updateTransNativeSharding(compid, year, sqlNative, params));
            if (b) {
                //向售后表插入申请数据
                getInsertSql(asAppVOS, paramsInsert);
                b = !ModelUtil.updateTransEmpty(baseDao.updateBatchNativeSharding(0, localDateTime.getYear(),
                        INSERT_ASAPP_SQL, paramsInsert, asAppVOS.size()));
            }
        } else {
//            if (baseDao.updateNativeSharding(compid, year, updSQL, orderNo) > 0) {
            //向售后表插入申请数据
            int invoType = asAppVOS.get(0).getInvoicetype();

            if (!MathUtil.isBetween(1, invoType, 3)) {
                return new Result().fail("未选择发票类型！");
            }

            String asOrderId = GenIdUtil.getAsOrderId();
            Object[] pramsObj = new Object[]{asAppVOS.get(0).getOrderno(), asAppVOS.get(0).getPdno(), asOrderId,
                    asAppVOS.get(0).getCompid(), asAppVOS.get(0).getAstype(), asAppVOS.get(0).getGstatus(), asAppVOS.get(0).getReason(),
                    asAppVOS.get(0).getCkstatus(), asAppVOS.get(0).getCkdesc(), asAppVOS.get(0).getInvoice(),
                    invoType == 1 ? 0 : 1, asAppVOS.get(0).getApdesc(),
                    asAppVOS.get(0).getRefamt() * 100, asAppVOS.get(0).getAsnum(), invoType, asAppVOS.get(0).getPkgno()};

            int res = baseDao.updateNativeSharding(0, localDateTime.getYear(), INSERT_ASAPP_SQL, pramsObj);

            return res > 0 ? result.success(asOrderId) : result.fail("申请失败");
//            }
        }
        return b ? result.success("您的售后已提交，客服会为您尽快处理！","申请成功") : result.fail("申请失败");
    }

    private boolean doApply(List<AsAppVO> asAppVOS, String orderNo, int asType) {
        String sql;
        LocalDateTime localDateTime = LocalDateTime.now();
        if (asType == 3 || asType == 4) {
            sql = "select count(*) from {{?" + DSMConst.TD_TRAN_ASAPP + "}} where cstatus&1=0 "
                    + " and orderno=" + orderNo + " and astype in(3,4)";
        } else {
            StringBuilder skuBuilder = new StringBuilder();
            for (AsAppVO asAppVO : asAppVOS) {
                skuBuilder.append(asAppVO.getPdno()).append(",");
            }
            String pdnoStr = skuBuilder.toString().substring(0, skuBuilder.toString().length() - 1);
            sql = "select pdno, pkgno from {{?" + DSMConst.TD_TRAN_ASAPP + "}} where cstatus&1=0 "
                    + " and orderno=" + orderNo
//                    + " and ckstatus in(0,1) "
                    + " and astype not in(3,4) "
                    + " and pdno in(" + pdnoStr + ")";

            List<Object[]> queryResult = baseDao.queryNativeSharding(0, localDateTime.getYear(), sql);

            int pkgno;
            long pdno;
            for (Object[] objects : queryResult) {
                pkgno = Integer.parseInt(objects[1].toString());
                pdno  = Long.valueOf(objects[0].toString());
                for (AsAppVO asAppVO : asAppVOS) {
                    if (asAppVO.getPkgno() == pkgno
                            && asAppVO.getPdno() == pdno) {
                        return true;
                    }
                }
            }

            return false;
        }

        List<Object[]> queryResult = baseDao.queryNativeSharding(0, localDateTime.getYear(), sql);
        return (long) queryResult.get(0)[0] > 0;

    }

    /* *
     * @description 更新订单下商品售后状态
     * @params [asType, sqlList, params]
     * @return void
     * @exception
     * @author 11842
     * @time  2019/4/23 14:54
     * @version 1.1.1
     **/
    private void getUpdGoodsSql(List<AsAppVO> asAppVOS, List<String> sqlList, List<Object[]> params,
                                String orderNo, int compid) {
        List<Long> skuList = new ArrayList<>();
        TranOrderGoods[] tranOrderGoods = TranOrderOptModule.getGoodsArr(orderNo, compid);
        for (AsAppVO asAppVO : asAppVOS) {
            for (TranOrderGoods tranOrderGoods1 : tranOrderGoods) {
                if (asAppVO.getPdno() == tranOrderGoods1.getPdno()
                && asAppVO.getPkgno() == tranOrderGoods1.getPkgno()) {
                    int year = Integer.parseInt("20" + orderNo.substring(0, 2));
                    List<Object[]> ret = baseDao.queryNativeSharding(compid, year,
                            QUERY_ORDER_GOODS_BAL_SQL, new Object[]{orderNo, asAppVO.getPdno(), compid, asAppVO.getPkgno()});
                    double bal = 0;
                    if (ret != null && !ret.isEmpty()) {
                        bal = Double.parseDouble(ret.get(0)[0].toString());
                    }


                    bal = MathUtil.exactDiv(bal, 100).setScale(2, BigDecimal.ROUND_HALF_UP).
                            doubleValue();

                    double payamt = MathUtil.exactDiv(tranOrderGoods1.getPayamt(), 100).
                            setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();

                    double refamt = 0;
                    if (asAppVO.getAsnum() == tranOrderGoods1.getPnum()) {
                        refamt = MathUtil.exactAdd(bal, payamt).
                                setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    } else {
                        double acprice = MathUtil.exactDiv(payamt, tranOrderGoods1.getPnum()).
                                setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();

                        refamt = MathUtil.exactMul(acprice, asAppVO.getAsnum()).
                                setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();

                        if (bal > 0) {
                            bal = MathUtil.exactDiv(bal,
                                    tranOrderGoods1.getPnum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();

                            double acbalamt = MathUtil.exactMul(bal,
                                    asAppVO.getAsnum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();

                            refamt = MathUtil.exactAdd(refamt, acbalamt).
                                    setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                        }
                    }


                    asAppVO.setRefamt(refamt);
                }
                if (asAppVO.getPdno() == tranOrderGoods1.getPdno() && asAppVO.getAsnum() == tranOrderGoods1.getPnum()) {
                    skuList.add(asAppVO.getPdno());
                }
            }
        }
        if (skuList.size() == tranOrderGoods.length) {
            for (TranOrderGoods tog : tranOrderGoods) {
                sqlList.add(UPD_ORDER_GOODS_SQL);
                params.add(new Object[]{1, tog.getPdno(), 0, orderNo, compid, tog.getPkgno()});
            }
        } else {
            for (AsAppVO asAppVO : asAppVOS) {
                long sku = asAppVO.getPdno();
                sqlList.add(UPD_ORDER_GOODS_SQL);
                params.add(new Object[]{1, sku, 0, orderNo, compid, asAppVO.getPkgno()});
            }
        }
    }

    /* *
     * @description 获取新增sql
     * @params [asAppVOS, paramsInsert]
     * @return void
     * @exception
     * @author 11842
     * @time  2019/4/26 11:10
     * @version 1.1.1
     **/
    private void getInsertSql(List<AsAppVO> asAppVOS, List<Object[]> paramsInsert) {
        for (AsAppVO asAppVO : asAppVOS) {
            paramsInsert.add(new Object[]{asAppVO.getOrderno(), asAppVO.getPdno(), GenIdUtil.getAsOrderId(),
                    asAppVO.getCompid(), asAppVO.getAstype(), asAppVO.getGstatus(), asAppVO.getReason(),
                    asAppVO.getCkstatus(), asAppVO.getCkdesc(), asAppVO.getInvoice(), 0,
                    asAppVO.getApdesc(), asAppVO.getRefamt() * 100, asAppVO.getAsnum(), asAppVO.getInvoicetype(), asAppVO.getPkgno()});
        }
    }

    /**
     * @接口摘要 订单取消售后接口
     * @业务场景 取消售后
     * @传参类型 json
     * @传参列表 {sku:商品sku orderno: 订单号 compid：买家企业码}
     * @返回列表 200 成功 -1 失败
     */
    @UserPermission(ignore = false)
    public Result cancelAfterSale(AppContext appContext) {
        Result result = new Result();
        LocalDateTime localDateTime = LocalDateTime.now();
//        List<String> sqlList = new ArrayList<>();
//        List<Object[]> params = new ArrayList<>();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String orderNo = jsonObject.get("orderno").getAsString();
        int year = Integer.parseInt("20" + orderNo.substring(0, 2));
        int compId = jsonObject.get("compid").getAsInt();
        long sku = jsonObject.get("sku").getAsLong();

//        String updOrderSql = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set "
//                + "asstatus=? where cstatus&1=0 and orderno=? and asstatus in(1,2,3)";
        //更新订单商品售后状态
        String updOrderGoodsSql = "update {{?" + DSMConst.TD_TRAN_GOODS + "}} set asstatus=? "
                + " where cstatus&1=0 and pdno=? and orderno = ? and compid = ? and asstatus in(1,2,3)";
        int resultCode = baseDao.updateNativeSharding(compId, year, updOrderGoodsSql,
                -1, sku, orderNo, compId);

        if (resultCode > 0) {
            //更新售后表状态
            resultCode = baseDao.updateNativeSharding(0, localDateTime.getYear(),
                    UPD_ASAPP_SQL, -2, sku, orderNo, compId);

        }
        return resultCode > 0 ? result.success("取消成功") : result.fail("取消失败");
//        sqlList.add(updOrderSql);
//        params.add(new Object[]{-1, orderNo});
//        TranOrderGoods[] tranOrderGoods = TranOrderOptModule.getGoodsArr(orderNo, compId);
//        for (TranOrderGoods tog :tranOrderGoods) {
//            sqlList.add(updOrderGoodsSql);
//            params.add(new Object[]{-1, tog.getPdno(), 0});
//        }
//        String[] sqlNative = new String[sqlList.size()];
//        sqlNative = sqlList.toArray(sqlNative);
//        boolean b = !ModelUtil.updateTransEmpty(baseDao.updateTransNativeSharding(compId, year, sqlNative, params));
//        return b ? result.success("取消成功") : result.fail("取消失败");

    }

    @UserPermission(ignore = true)
    public Result getLogisticsInfo(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String orderNo = jsonObject.has("orderno") ? jsonObject.get("orderno").getAsString() : "";
        JSONObject result;
        if (IceRemoteUtil.systemConfigOpen("SYNC_YKWL")){//不同步到ykwl
            result = LccOrderUtil.queryTraceByOrderno(orderNo);
        } else {
            return queryNodes(appContext);
        }

        return new Result().success(result);
    }


    /* *
     * @description 售后申请审核
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @version 1.1.1
     **/
    @IceDebug(outPrint = true)
    public Result afterSaleReview(AppContext appContext) {
        String json = appContext.param.json;
        Result result = new Result();
        JsonParser jsonParser = new JsonParser();
        UserSession userSession = appContext.getUserSession();
//        if (userSession == null || (userSession.roleCode & (128 + 1)) == 0) {
//            return result.fail("当前用户没有该权限");
//        }

        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        long asno = jsonObject.get("asno").getAsLong();
        String ckdesc = jsonObject.get("ckdesc").getAsString();
        int ckstatus = jsonObject.get("ckstatus").getAsInt();
        int astype = jsonObject.get("astype").getAsInt();
        double refamt = jsonObject.get("refamt").getAsDouble();
        //double realrefamt = jsonObject.get("realrefamt").getAsDouble();

        String queryOrderno = "select orderno,compid,pdno,pkgno from {{?" + DSMConst.TD_TRAN_ASAPP + "}} where asno = ? ";


        List<Object[]> queryRet = baseDao.queryNativeSharding(0, TimeUtils.getCurrentYear(), queryOrderno, asno);

        if (queryRet == null || queryRet.isEmpty()) {
            return result.fail("操作失败");
        }

        int ret = baseDao.updateNativeSharding(0,
                TimeUtils.getCurrentYear(), UPD_ASAPP_CK_SQL,
                ckstatus, userSession.userId, userSession.userName, astype, ckdesc, refamt * 100, 0, asno);
        //此处仅进行审核申请
        if (ret > 0) {
            //退货失败
            int ostatus;
            int gstatus = 4;
            int asstatus = 0;

            int compid = Integer.parseInt(queryRet.get(0)[1].toString());
            LinkedList<String> sqls = new LinkedList<>();
            LinkedList<Object[]> params = new LinkedList<>();
            double subbal = .0;
            double subpay = .0;
            long balUnq = 0, payUnq = 0;
            String type = null, refundno = null, p0 = null, p1 = null, p4 = null, p2 = null;
            if (ckstatus == 1) {
                //1退款退货 2 仅退款
                if (astype == 1 || astype == 2) {
                        List<Object[]> queryResult = baseDao.queryNativeSharding(0,
                                TimeUtils.getCurrentYear(), QUERY_ASAPP_INFO_SQL, asno);
                    // asapp.orderno, asapp.compid, asapp.asno, asapp.pdno, asapp.asnum,
                    // goods.pdprice/100 spdprice, goods.payamt/100 spayamt,
                    // distprice/100 sdistprice, goods.pnum, asstype,
                    // reason, apdesc, refamt/100 refamt, ckstatus, ckdesc,
                    // gstatus, ckdate, cktime, apdata, aptim,
                    // asapp.cstatus, goods.balamt/100 balamt

                    // 订单号，企业码，售后编号，退货商品码，退货数量
                    // 商品单价，实付金额，折扣金额，商品总数量，售后类型
                    // 原因，描述，要求退款金额，？，？
                    // ?,?,?,?,?
                    // 状态，余额抵扣, 支付总金额

                    // 退货数量
                    int asnum = Integer.parseInt(queryResult.get(0)[4].toString());
                    // 余额抵扣
                    double balamt = Double.parseDouble(queryResult.get(0)[21].toString());
                    // 总数量
                    int pnum = Integer.parseInt(queryResult.get(0)[8].toString());
                    double payamt = Double.parseDouble(queryResult.get(0)[6].toString());

                    LogUtil.getDefaultLogger().debug("审核通过获取余额：" + balamt);

                    if (balamt > 0) {
                        List<Object[]> qresult = baseDao.queryNativeSharding(compid,
                                TimeUtils.getYearByOrderno(queryRet.get(0)[0].toString()),
                                " SELECT paysource, payway, payprice, payno, tppno "
                                        + " FROM {{?" + DSMConst.TD_TRAN_TRANS + "}} "
                                        + " WHERE cstatus&1 = 0 AND orderno = ? AND payway = 0 ",
                                queryRet.get(0)[0].toString());

                        if (!qresult.isEmpty()) {
                            if (pnum == asnum) {
                                subbal = balamt;
                            } else {
                                double apamt = MathUtil.exactDiv(balamt, pnum).
                                        setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                                LogUtil.getDefaultLogger().debug("审核通过单个商品分摊余额：" + apamt + " 商品数量：" + pnum);

                                subbal = MathUtil.exactMul(asnum, apamt).
                                        setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                                LogUtil.getDefaultLogger().debug("实际退回余额分摊余额：" + subbal + " 退货数量：" + asnum);
                            }

                            sqls.add(" INSERT INTO {{?" + DSMConst.TD_TRAN_TRANS + "}} "
                                    + " (unqid, compid, orderno, payno, payprice, "
                                    + " payway, paysource, paystatus, payorderno, tppno, "
                                    + " paydate, paytime, completedate, completetime, cstatus) VALUES "
                                    + " (?,?,?,?,?, ?,?,?,?,?, CURRENT_DATE,CURRENT_TIME,NULL,NULL,?) ");
                            balUnq = GenIdUtil.getUnqId();
                            params.add(new Object[]{
                                    balUnq, compid, asno, 0, subbal * 100,
                                    0, qresult.get(0)[0].toString(), 0, 0, qresult.get(0)[4].toString(),
                                    1024
                            });
                        }

                    }
                    LogUtil.getDefaultLogger().debug("审核通过获取shifu：" + payamt);
                    if (payamt > 0) {
                        List<Object[]> qresult = baseDao.queryNativeSharding(compid,
                                TimeUtils.getYearByOrderno(queryRet.get(0)[0].toString()),
                                " SELECT paysource, payway, payprice, payno, tppno "
                                        + " FROM {{?" + DSMConst.TD_TRAN_TRANS + "}} "
                                        + " WHERE cstatus&1 = 0 AND orderno = ? AND payway IN (1, 2) ", queryRet.get(0)[0].toString());

                        if (!qresult.isEmpty()) {
                            p2 = String.valueOf(MathUtil.exactDiv(Long.parseLong(qresult.get(0)[2].toString()), 100.0).doubleValue());

                            if (pnum == asnum) {
                                subpay = payamt;
                            } else {
                                double apamt = MathUtil.exactDiv(payamt, pnum).
                                        setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                                LogUtil.getDefaultLogger().debug("审核通过单个商品分摊实付：" + apamt + " 商品数量：" + pnum);

                                subpay = MathUtil.exactMul(asnum, apamt).
                                        setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                                LogUtil.getDefaultLogger().debug("实际退回余额分摊实付：" + subpay + " 退货数量：" + asnum);
                            }

                            if (Integer.parseInt(qresult.get(0)[1].toString()) == 2) {
                                type = "alipay";
                            } else {
                                type = "wxpay";
                            }

//                            refundno = qresult.get(0)[3].toString()
//                                    + String.format("%02d", RedisUtil.getStringProvide().increase(qresult.get(0)[3].toString()));
                            refundno = String.valueOf(GenIdUtil.getUnqId());
                            sqls.add(" INSERT INTO {{?" + DSMConst.TD_TRAN_TRANS + "}} "
                                    + " (unqid, compid, orderno, payno, payprice, "
                                    + " payway, paysource, paystatus, payorderno, tppno, "
                                    + " paydate, paytime, completedate, completetime, cstatus) VALUES "
                                    + " (?,?,?,?,?, ?,?,?,?,?, CURRENT_DATE,CURRENT_TIME,NULL,NULL,?) ");

                            payUnq = GenIdUtil.getUnqId();
                            params.add(new Object[]{
                                    payUnq, compid, asno, refundno, subpay * 100,
                                    p1 = qresult.get(0)[1].toString(), p0 = qresult.get(0)[0].toString(),
                                    0, 0, p4 = qresult.get(0)[4].toString(),
                                    1024
                            });
                        }
                    }

                    sendMessageToSpecify(SmsTempNo.AFTER_SALE_AUDIT_PASSED, compid, asno, "");
                }
                ostatus = -2;
                gstatus = 3;

                if (astype == 3 || astype == 4) {
                    sendMessageToSpecify(SmsTempNo.AFTER_SALE_BILL_AUDIT_PASSED, compid, asno, "");
                }


            } else {
                ostatus = 4;
                asstatus = 200;
                if (astype == 1 || astype == 2) {
                    sendMessageToSpecify(SmsTempNo.AFTER_SALE_AUDIT_FAILED_TO_PASSED, compid, asno, ckdesc);
                }

                if (astype == 3 || astype == 4) {
                    sendMessageToSpecify(SmsTempNo.AFTER_SALE_BILL_AUDIT_FAILED_TO_PASSED, compid, asno, ckdesc);
                }

            }

            String updateOrderNew = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set ostatus=?, asstatus = ? "
                    + " where cstatus&1=0 and orderno=? and ostatus = ? ";

            int year = Integer.parseInt("20" + queryRet.get(0)[0].toString().substring(0, 2));

            sqls.addFirst(updateOrderNew);
            params.addFirst(new Object[]{ostatus, asstatus, queryRet.get(0)[0], -1});
            sqls.add(UPD_ORDER_GOODS_CK_SQL);
            params.add(new Object[]{gstatus, queryRet.get(0)[2], 1, queryRet.get(0)[0], queryRet.get(0)[1], queryRet.get(0)[3]});

            baseDao.updateTransNativeSharding(Integer.parseInt(queryRet.get(0)[1].toString()), year,
                    sqls.toArray(new String[]{}), params);

            String update = " UPDATE {{?" + DSMConst.TD_TRAN_TRANS + "}} "
                    + " SET paystatus = ?, completedate = CURRENT_DATE, completetime = CURRENT_TIME "
                    + " WHERE unqid = ? ";
            List<Object[]> pss = new LinkedList<>();

            if (subbal > 0) {
                int r = IceRemoteUtil.updateCompBal(Integer.parseInt(queryRet.get(0)[1].toString()),
                        MathUtil.exactMul(subbal, 100).intValue());

                pss.add(new Object[]{r > 0 ? 1 : -2, balUnq});

            }

            if (subpay > 0 && type != null) {
                HashMap<String, Object> refundResult =
                        FileServerUtils.refund(type, refundno, p4, subpay,
                                Double.parseDouble(p2),
                                "1".equals(p0));
                boolean r = refundResult.containsKey("code")
                        && 2.0 == Double.parseDouble(refundResult.get("code").toString());

                pss.add(new Object[]{r ? 1 : -2, payUnq});
            }

            if (!pss.isEmpty()) {
                baseDao.updateBatchNativeSharding(compid, year, update, pss, pss.size());
            }

        }  // 退货成功
        return ret > 0 ? result.success("操作成功") : result.fail("操作失败");
    }

    private void sendMessageToSpecify(int tempId, int compid, long asno, String ckdesc) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            String specifyStorePhone = IceRemoteUtil.getSpecifyStorePhone(compid);
            SmsTempNo.sendMessageToSpecify(compid, specifyStorePhone,
                    tempId, asno + "", ckdesc);
        });
    }

    /**
     * @接口摘要 删除订单接口
     * @业务场景 已取消或已完成的订单删除
     * @传参类型 json
     * @传参列表 {orderno: 订单号 }
     * @返回列表 200 成功 -1 失败
     */
    @UserPermission(ignore = false)
    public Result deleteOrder(AppContext appContext) {
        Result result = new Result();
        List<String> sqlList = new ArrayList<>();
        List<Object[]> params = new ArrayList<>();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        UserSession userSession = appContext.getUserSession();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String orderNo = jsonObject.get("orderno").getAsString();
        int compId = userSession.compId;
        int year = Integer.parseInt("20" + orderNo.substring(0, 2));
        String updateOrderSql = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set cstatus=cstatus|1 where "
                + " cstatus&1=0 and ostatus in(-4,4) and orderno=" + orderNo;
        int re = baseDao.updateNativeSharding(compId, year, updateOrderSql);
//        sqlList.add(updateOrderSql);
//        params.add(new Object[]{});
//        String updateGoodsSql = "update {{?" + DSMConst.TD_TRAN_GOODS + "}} set cstatus=cstatus|1 where "
//                + " cstatus&1=0 and orderno=" + orderNo;
//        sqlList.add(updateGoodsSql);
//        params.add(new Object[]{});
//        String[] sqlNative = new String[sqlList.size()];
//        sqlNative = sqlList.toArray(sqlNative);
//        boolean b = !ModelUtil.updateTransEmpty(baseDao.updateTransNativeSharding(compId, year, sqlNative, params));
        return re > 0 ? result.success("订单删除成功","删除成功") : result.fail("订单删除失败");
    }

    /**
     * @接口摘要 查询售后订单详情
     * @业务场景 售后订单详情
     * @传参类型 json
     * @传参列表 {asno: 售后单号 }
     * @返回列表 200 成功 -1 失败
     */
    @UserPermission(ignore = true)
    public Result queryAsOrderInfo(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String asno = jsonObject.get("asno").getAsString();

        List<Object[]> queryResult = baseDao.queryNativeSharding(0,
                TimeUtils.getCurrentYear(), QUERY_ASAPP_INFO_SQL, asno);


        if (queryResult.isEmpty()) {
            return null;
        }

        AsAppDtVO[] asAppDtVOs = new AsAppDtVO[queryResult.size()];

        baseDao.convToEntity(queryResult, asAppDtVOs, AsAppDtVO.class,
                new String[]{"orderno", "compid", "asno", "pdno", "asnum",
                        "spdprice", "spayamt", "sdistprice", "pnum",
                        "astype", "reason", "apdesc", "refamt","realrefamt", "ckstatus", "ckdesc", "gstatus",
                        "ckdate", "cktime", "apdata", "aptime", "cstatus", "balamt", "pkgno"});

        AsAppDtVO asAppDtVO = asAppDtVOs[0];
        if (asAppDtVO != null) {
            double acprice = MathUtil.exactDiv(asAppDtVO.getSpayamt(), asAppDtVO.getPnum()).
                    setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();

            double disprice = MathUtil.exactDiv(asAppDtVO.getDistprice(), asAppDtVO.getPnum()).
                    setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();

            asAppDtVO.setDistprice(disprice);
            asAppDtVO.setAcprice(acprice);

//            MathUtil.exactMul(asAppDtVO.getAsnum(),asAppDtVO.getBalamt()).
//                    setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()
            //商品合计金额
            asAppDtVO.setPayamt(new BigDecimal(String.valueOf(asAppDtVO.getSpayamt())).divide(new BigDecimal(String.valueOf(asAppDtVO.getPnum())),3,BigDecimal.ROUND_HALF_UP).
                    multiply(new BigDecimal(String.valueOf(asAppDtVO.getAsnum()))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
            appContext.logger.print("---------------------->商品合计金额 = " + asAppDtVO.getPayamt());
//            asAppDtVO.setPayamt(MathUtil.exactMul(asAppDtVO.getAsnum(), acprice)
//                    .setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());


            double acbalamt = MathUtil.exactDiv(asAppDtVO.getBalamt(),
                    asAppDtVO.getPnum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            //退款余额合计
            asAppDtVO.setRefbal(new BigDecimal(String.valueOf(asAppDtVO.getBalamt())).divide(new BigDecimal(String.valueOf(asAppDtVO.getPnum())),3,BigDecimal.ROUND_HALF_UP).
                    multiply(new BigDecimal(String.valueOf(asAppDtVO.getAsnum()))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
            appContext.logger.print("---------------------->退款余额合计 = " + asAppDtVO.getRefbal());
//            asAppDtVO.setRefbal(MathUtil.exactMul(asAppDtVO.getAsnum(), acbalamt).
//                    setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
            asAppDtVO.setSumRefAmt(asAppDtVO.getRefamt());
            asAppDtVO.setBalamt(acbalamt);
            ProdEntity prodBySku = null;
            try {
                prodBySku = ProdInfoStore.getProdBySku(asAppDtVO.getPdno());
                if (prodBySku != null) {
                    asAppDtVO.setSpec(prodBySku.getSpec());
                    asAppDtVO.setPname(prodBySku.getProdname());
                    asAppDtVO.setManuName(prodBySku.getManuName());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result.success(asAppDtVO);
    }

    /**
     * @接口摘要 查询售后订单详情[补开发票]
     * @业务场景 补开发票
     * @传参类型 json
     * @传参列表 {asno: 售后单号 }
     * @返回列表 200 成功 -1 失败
     */
    @UserPermission(ignore = true)
    public Result queryAsOrderInvoiceInfo(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String asno = jsonObject.get("asno").getAsString();

        List<Object[]> queryResult = baseDao.queryNativeSharding(0,
                TimeUtils.getCurrentYear(), QUERY_ASAPP_INVOICE_INFO_SQL, asno);


        if (queryResult.isEmpty()) {
            return null;
        }

        AsAppDtVO[] asAppDtVOs = new AsAppDtVO[queryResult.size()];
        baseDao.convToEntity(queryResult, asAppDtVOs, AsAppDtVO.class,
                new String[]{"orderno", "compid", "asno", "pdno", "asnum",
                        "astype", "reason", "apdesc", "refamt", "ckstatus", "ckdesc", "gstatus",
                        "ckdate", "cktime", "apdata", "aptime", "cstatus", "invoice", "invoicetype"});

        JSONObject resultObject = new JSONObject();
        AsAppDtVO asAppDtVO = asAppDtVOs[0];
        if (asAppDtVO != null) {
            asAppDtVO.setPaytype(2);
            String compStr = RedisUtil.getStringProvide()
                    .get(String.valueOf(asAppDtVO.getCompid()));

            JSONObject compJson = JSON.parseObject(compStr);

            if (compJson != null) {
                asAppDtVO.setAreaAllName(
                        IceRemoteUtil.getCompleteName(compJson.getString("addressCode")));
            }
            resultObject.put("asapp", asAppDtVO);
            queryResult = baseDao.queryNativeSharding(0,
                    TimeUtils.getCurrentYear(), QUERY_GOODS_SQL, asAppDtVO.getOrderno(), asAppDtVO.getCompid());
            List<TranOrderGoods> list = new ArrayList<>();
            if (queryResult != null && queryResult.size() > 0) {
                for (Object[] arr : queryResult) {
                    TranOrderGoods goods = new TranOrderGoods();
                    goods.setPdno(Long.parseLong(arr[0].toString()));
                    ProdEntity prodBySku = null;
                    try {
                        prodBySku = ProdInfoStore.getProdBySku(goods.getPdno());

                        if (prodBySku != null) {
                            goods.setPname(prodBySku.getProdname());
                            goods.setPspec(prodBySku.getSpec());
                            goods.setManun(prodBySku.getManuName());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    goods.setPnum(Integer.parseInt(arr[1].toString()));
                    goods.setPdprice(MathUtil.exactDiv(Double.parseDouble(arr[2].toString()), 100).doubleValue());
                    goods.setDistprice(MathUtil.exactDiv(Double.parseDouble(arr[3].toString()), 100).doubleValue());
                    goods.setPayamt(MathUtil.exactDiv(Double.parseDouble(arr[4].toString()), 100).doubleValue());
                    list.add(goods);
                }
            }
            resultObject.put("goodslist", list);

        }
        return result.success(resultObject);
    }


    /**
     * @接口摘要 查询售后订单列表
     * @业务场景 查询
     * @传参类型 json
     * @传参列表 {pageSize: 每页条数  pageNo 页码 astype 售后类型 sdate 开始时间 edate 结束时间 ckstatus 审核状态}
     * @返回列表 AsAppDtListVO数组
     */
    @UserPermission(ignore = true)
    public Result queryAsOrderList(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int pageSize = jsonObject.get("pageSize").getAsInt();
        int pageIndex = jsonObject.get("pageNo").getAsInt();
        Page page = new Page();
        page.pageSize = pageSize;
        page.pageIndex = pageIndex;
        PageHolder pageHolder = new PageHolder(page);
        Result result = new Result();


        StringBuilder sqlBuilder = new StringBuilder(QUERY_ASAPP_LIST_SQL);

        int astype = jsonObject.get("astype").getAsInt();

        String sdate = jsonObject.get("sdate").getAsString();

        String edate = jsonObject.get("edate").getAsString();

        int ckstatus = jsonObject.get("ckstatus").getAsInt();

        if (astype > -1) {
            sqlBuilder.append(" and asapp.astype = ");
            sqlBuilder.append(astype);
        }

        if (ckstatus != -2) {
            sqlBuilder.append(" and asapp.ckstatus = ");
            sqlBuilder.append(ckstatus);
        }


        if (!StringUtils.isEmpty(sdate) && StringUtils.isEmpty(edate)) {
            sqlBuilder.append(" and apdata >= '").append(sdate).append("' ");
        }

        if (StringUtils.isEmpty(sdate) && !StringUtils.isEmpty(edate)) {
            sqlBuilder.append(" and apdata <= '").append(edate).append("' ");
        }

        if (!StringUtils.isEmpty(sdate) && !StringUtils.isEmpty(edate)) {
            sqlBuilder.append(" and apdata between '").append(sdate).append("' and '").append(edate).append("' ");
        }
        sqlBuilder.append(" order by asapp.apdata desc,asapp.aptime desc ");

        List<Object[]> queryResult = baseDao.queryNativeSharding(0,
                TimeUtils.getCurrentYear(), pageHolder, page, sqlBuilder.toString());


        AsAppDtListVO[] asAppDtListVOS = new AsAppDtListVO[queryResult.size()];
        if (queryResult == null || queryResult.isEmpty()) {
            return result.setQuery(asAppDtListVOS, pageHolder);
        }

        baseDao.convToEntity(queryResult, asAppDtListVOS, AsAppDtListVO.class,
                new String[]{"orderno", "compid", "asno", "astype", "ckstatus",
                        "gstatus", "apdata", "aptime", "checkern",
                        "contact", "address", "refamt", "compn", "pkgno"});

        for (AsAppDtListVO asAppDtListVO : asAppDtListVOS) {
            String compStr = RedisUtil.getStringProvide()
                    .get(String.valueOf(asAppDtListVO.getCompid()));
            if (!StringUtils.isEmpty(compStr)) {
                JSONObject compJson = JSON.parseObject(compStr);
                asAppDtListVO.setCompn(compJson.getString("storeName"));
            }
        }

        return result.setQuery(asAppDtListVOS, pageHolder);
    }


    /**
     * @接口摘要 查询售后订单发票列表
     * @业务场景 查询
     * @传参类型 json
     * @传参列表 {pageSize: 每页条数  pageNo 页码 astype 售后类型 sdate 开始时间 edate 结束时间 ckstatus 审核状态}
     * @返回列表 AsAppDtListVO数组
     */
    @UserPermission(ignore = true)
    public Result queryAsOrderBillList(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int pageSize = jsonObject.get("pageSize").getAsInt();
        int pageIndex = jsonObject.get("pageNo").getAsInt();
        Page page = new Page();
        page.pageSize = pageSize;
        page.pageIndex = pageIndex;
        PageHolder pageHolder = new PageHolder(page);
        Result result = new Result();


        StringBuilder sqlBuilder = new StringBuilder(QUERY_ASAPP_BILL_LIST_SQL);

        int astype = jsonObject.get("astype").getAsInt();

        String sdate = jsonObject.get("sdate").getAsString();

        String edate = jsonObject.get("edate").getAsString();

        int ckstatus = jsonObject.get("ckstatus").getAsInt();

        if (astype > -1) {
            sqlBuilder.append(" and asapp.astype = ");
            sqlBuilder.append(astype);
        }

        if (ckstatus != -2) {
            sqlBuilder.append(" and asapp.ckstatus = ");
            sqlBuilder.append(ckstatus);
        }


        if (!StringUtils.isEmpty(sdate) && StringUtils.isEmpty(edate)) {
            sqlBuilder.append(" and apdata >= '").append(sdate).append("' ");
        }

        if (StringUtils.isEmpty(sdate) && !StringUtils.isEmpty(edate)) {
            sqlBuilder.append(" and apdata <= '").append(edate).append("' ");
        }

        if (!StringUtils.isEmpty(sdate) && !StringUtils.isEmpty(edate)) {
            sqlBuilder.append(" and apdata between '").append(sdate).append("' and '").append(edate).append("' ");
        }

        sqlBuilder.append(" order by asapp.apdata,asapp.aptime desc ");

        List<Object[]> queryResult = baseDao.queryNativeSharding(0,
                TimeUtils.getCurrentYear(), pageHolder, page, sqlBuilder.toString());


        AsAppDtListVO[] asAppDtListVOS = new AsAppDtListVO[queryResult.size()];
        if (queryResult == null || queryResult.isEmpty()) {
            return result.setQuery(asAppDtListVOS, pageHolder);
        }

        baseDao.convToEntity(queryResult, asAppDtListVOS, AsAppDtListVO.class,
                new String[]{"orderno", "compid", "asno", "astype", "ckstatus",
                        "gstatus", "apdata", "aptime", "checkern",
                        "contact", "address", "compn", "invoicetype"});

        for (AsAppDtListVO asAppDtListVO : asAppDtListVOS) {
            String compStr = RedisUtil.getStringProvide()
                    .get(String.valueOf(asAppDtListVO.getCompid()));
            if (!StringUtils.isEmpty(compStr)) {
                JSONObject compJson = JSON.parseObject(compStr);
                asAppDtListVO.setCompn(compJson.getString("storeName"));
            }
        }


        return result.setQuery(asAppDtListVOS, pageHolder);
    }


    /**
     * @接口摘要 根据活动起始时间和结束时间和活动码查询团购订单记录
     * @业务场景 查询
     * @传参类型 json
     * @传参列表 {sdate 开始时间 edate 结束时间 actno 活动码}
     * @返回列表 JSONArray [{orderno 订单号 payamt 支付金额  compid 企业码 pnum 数量}]
     */
    @UserPermission(ignore = true)
    public Result queryTeamBuyOrder(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String sdate = jsonObject.get("sdate").getAsString();
        String edate = jsonObject.get("edate").getAsString();
        String actno = jsonObject.get("actno").getAsString();

        JSONArray jsonArray = new JSONArray();
        List<Object[]> queryList = baseDao.queryNativeSharding(0, TimeUtils.getCurrentYear(), QUERY_TEAM_BUY_ORDER_SQL, new Object[]{sdate, edate, actno});
        if (queryList != null && queryList.size() > 0) {
            for (Object[] obj : queryList) {
                JSONObject j = new JSONObject();
                j.put("orderno", obj[0].toString());
                j.put("payamt", obj[1].toString());
                j.put("compid", obj[2].toString());
                j.put("pnum", obj[3].toString());
                jsonArray.add(j);
            }
        }
        return new Result().success(jsonArray.toJSONString());
    }


    /**
     * @接口摘要 确认签收
     * @业务场景 订单签收后确认签收
     * @传参类型 json
     * @传参列表 {orderno 订单号 cusno 企业码 }
     * @返回列表 200 成功 -1 失败
     */
    @UserPermission(ignore = false)
    public Result confirmReceipt(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String orderNo = jsonObject.get("orderno").getAsString();//订单号
        int cusno = jsonObject.get("cusno").getAsInt(); //企业码
        if (comReceipt(orderNo, cusno)) {
            //移出队列
            CONFIRM_RECEIPT.removeByKey(orderNo);
            return result.success("操作成功");
        }
        return result.fail("操作失败");
    }

    public boolean comReceipt(String orderNo, int cusno) {
        int year = Integer.parseInt("20" + orderNo.substring(0, 2));
        //更新订单状态为交易完成
        String updSQL = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set ostatus=4 "
                + " where cstatus&1=0 and orderno=" + orderNo + " and ostatus in(2,3)";
        String updateRBSQL = "update {{?" + DSMConst.TD_TRAN_REBATE + "}} set cstatus=cstatus|64"
                + " where cstatus&1=0 and orderno=?";

        int result = baseDao.updateNativeSharding(cusno, year, updSQL);

        if (result <= 0) {
            return false;
        }
        try{
            //满赠赠优惠券
            if (CouponRevModule.revGiftCoupon(Long.parseLong(orderNo), cusno)) {//若返利成功则标记为已返利
                baseDao.updateNativeSharding(cusno, year, updateRBSQL, orderNo);
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return true;
    }


    /**
     * @接口摘要 售后完成更新状态
     * @业务场景 订单签收后确认签收
     * @传参类型 json
     * @传参列表 {asno 售后单号}
     * @返回列表 200 成功 -1 失败
     */
    public Result afterSaleFinish(AppContext appContext) {
        String json = appContext.param.json;
        Result result = new Result();
        JsonParser jsonParser = new JsonParser();
        UserSession userSession = appContext.getUserSession();
        if (userSession == null) {
            return result.fail("当前用户没有该权限");
        }

        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        long asno = jsonObject.get("asno").getAsLong();
        String queryOrderno = "select orderno,compid,pdno,pkgno from {{?" + DSMConst.TD_TRAN_ASAPP + "}} where asno = ? ";
        List<Object[]> queryRet = baseDao.queryNativeSharding(0, TimeUtils.getCurrentYear(), queryOrderno, asno);
        if (queryRet == null || queryRet.isEmpty()) {
            return result.fail("操作失败");
        }

        int year = Integer.parseInt("20" + queryRet.get(0)[0].toString().substring(0, 2));
        List<Object[]> params = new ArrayList<>();
        params.add(new Object[]{-3, queryRet.get(0)[0], -2});
        params.add(new Object[]{200, queryRet.get(0)[2], 3, queryRet.get(0)[0], queryRet.get(0)[1], queryRet.get(0)[3]});


        baseDao.updateNativeSharding(0, TimeUtils.getCurrentYear(), UPD_ASAPP_CK_FINISH_SQL, new Object[]{200, asno});
        boolean b = !ModelUtil.updateTransEmpty(baseDao.updateTransNativeSharding(Integer.parseInt(queryRet.get(0)[1].toString()), year,
                new String[]{UPD_ORDER_CK_SQL, UPD_ORDER_GOODS_CK_SQL}, params));

        return b ? result.success("操作成功") : result.fail("操作失败");
    }


    /* *
     * @description 物流节点信息生成
     * @params [orderNo, compId, jsonNode]
     * @return void
     * @exception
     * @author 11842
     * @time  2019/7/15 11:31
     * @version 1.1.1
     **/
    public static void generateNode(String orderNo, int compId, JSONObject jsonObject) {
        String optSQL, node;
        Object[] params;
        String selectSQL = "select unqid,node from {{?" + DSMConst.TD_TRAN_AFTERSALES + "}} "
                + " where cstatus&1=0 and orderno=? and afsatype=3 and afsano=0 ";
        List<Object[]> queryResult = baseDao.queryNativeSharding(compId, TimeUtils.getYearByOrderno(orderNo), selectSQL, orderNo);
        if (queryResult == null || queryResult.isEmpty()) {//新增
            node = combatNodeInfo(jsonObject, null);
            optSQL = "insert into {{?" + DSMConst.TD_TRAN_AFTERSALES + "}} "
                    + "(unqid,compid,orderno,afsano,logisno,afsatype,lstatus,logistype,node)"
                    + " values(?,?,?,?,?,?,?,?,?)";
            params = new Object[]{GenIdUtil.getUnqId(), compId, orderNo, 0, 0, 3, 0, 1, node};
        } else {//修改
            node = String.valueOf(queryResult.get(0)[1]);
            long unqid = Long.parseLong(String.valueOf(queryResult.get(0)[0]));
            node = combatNodeInfo(jsonObject, node);
            optSQL = "update {{?" + DSMConst.TD_TRAN_AFTERSALES + "}} set node=? "
                    + " where cstatus&1=0 and unqid=?";
            params = new Object[]{node, unqid};
        }
        baseDao.updateNativeSharding(compId, TimeUtils.getYearByOrderno(orderNo), optSQL, params);
    }

    private static String combatNodeInfo(JSONObject jsonObject, String node) {
        JSONArray nodeArr = new JSONArray();
        if (node != null) {
            nodeArr = JSON.parseArray(node);
        }
        nodeArr.add(jsonObject);
        return nodeArr.toString();
    }


    public static JSONObject generateNodeObj(int type) {
        JSONObject jsonObject = new JSONObject();
        switch (type) {
            case 1://准备出库
                jsonObject.put("des", "您的订单已提交，进入仓库准备出库");
                jsonObject.put("status", "已下单");
                break;
            case 2://出库
                jsonObject.put("des", "您的订单在仓库发货完成，准备运输");
                jsonObject.put("status", "已发货");
                break;
            case 3://在途
                jsonObject.put("des", "");
//                jsonObject.put("des", "您的订单正在运输中，请耐心等待收货");
                jsonObject.put("status", "运输中");
                break;
            default://签收
                jsonObject.put("des", "");
//                jsonObject.put("des", "您的订单已签收，感谢您本次采购，欢迎再次购买");
                jsonObject.put("status", "已签收");
                break;
        }
        jsonObject.put("date", LocalDate.now().toString());
        jsonObject.put("time", LocalTime.now().toString());
        return jsonObject;
    }

    @UserPermission(ignore = false)
    public Result queryNodes(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String orderNo = jsonObject.get("orderno").getAsString();//订单号
        int compId = appContext.getUserSession().compId;
        if (compId == 0) {
            compId = jsonObject.get("compid").getAsInt();
        }
        String selectSQL = "select node from {{?" + DSMConst.TD_TRAN_AFTERSALES + "}} "
                + " where cstatus&1=0 and orderno=? and afsatype=3 and afsano=0 ";

        List<Object[]> queryResult = baseDao.queryNativeSharding(compId, TimeUtils.getYearByOrderno(orderNo), selectSQL, orderNo);

        if (queryResult == null || queryResult.isEmpty()) return result.success(null);

        JSONArray jsonResult = JSONArray.parseArray(queryResult.get(0)[0].toString());

        JSONArray returnResult = new JSONArray();
        JSONObject jo;

        for (int i = 0; i < jsonResult.size(); i++) {
            jo = new JSONObject();
            jo.put("status", jsonResult.getJSONObject(i).remove("status"));
            JSONArray tempJa = new JSONArray();
            tempJa.add(jsonResult.getJSONObject(i));
            jo.put("info", tempJa);
            returnResult.add(jo);
        }

        if (jsonResult.size() >= 2) {
            if (returnResult.size() > 2) {
                returnResult.remove(2);
            }

//            String routeNo = "SF1011099703744,SF2000932309557,SF2000932309566,SF2000932309575";
            String routeNo = FQExpressUtils.getTravingCode(orderNo);

            String routeInfo = FQExpressUtils.getRouteInfo(routeNo);

            if (StringUtils.isEmpty(routeInfo)) {
                return result.success(returnResult.toJSONString());
            }

            Element root = XMLUtils.loadXML(routeInfo).getRootElement();

            if (!root.element("Head").getTextTrim().equalsIgnoreCase("OK")) {
                return result.success(returnResult.toJSONString());
            }

            List<Element> routeResponseList = root.element("Body").elements("RouteResponse");

            JSONArray tempJa = new JSONArray();
            jo = new JSONObject();
            returnResult.add(2, jo);
            String status = "运输中";
            jo.put("infos", tempJa);
            String mainNo = routeNo.split(",")[0];

            for (Element routeResponse : routeResponseList) {
                List<Element> it = routeResponse.elements("Route");
                Element e;
                JSONObject j = new JSONObject();
                JSONArray innerTempJa = new JSONArray();
                j.put("mailno", routeResponse.attributeValue("mailno"));
                j.put("info", innerTempJa);
                j.put("isMain", mainNo.equals(routeResponse.attributeValue("mailno")));
                tempJa.add(j);

                for (int i = it.size() - 1; i >= 0 ; i--) {
                    e = it.get(i);
                    JSONObject tempJo = new JSONObject();
                    innerTempJa.add(tempJo);
                    String[] date_time = e.attributeValue("accept_time").split(" ");
                    tempJo.put("date", date_time[0]);
                    tempJo.put("time", date_time[1]);
                    tempJo.put("des",
                            "【" + e.attributeValue("accept_address") + "】"
                                    + e.attributeValue("remark"));
                }

            }

            jo.put("status", status);
        }

        return result.success(returnResult.toJSONString());
    }


    /**
     * 退货退款审核通过之后进行退款
     *
     * @param appContext
     * @param :asno->售后编号，realrefamt->实际退款金额 ,astype->售后类型
     * @return
     */
    public Result saleAfterRefund(AppContext appContext) {
        String json = appContext.param.json;
        RefundOp op = GsonUtils.jsonToJavaBean(json, RefundOp.class);
        assert op!= null;
        return op.execute(appContext);
    }

    /**
     * 重写提交退货退款申请
     * @time 2019年7月16日13点36分
     * @param appContext
     * @return
     */
    public Result afterSale(AppContext appContext) {
        String json = appContext.param.json;
        Result result = new Result();
        JsonParser jsonParser = new JsonParser();
        UserSession userSession = appContext.getUserSession();
//        if (userSession == null || (userSession.roleCode & (128 + 1)) == 0) {
//            return result.fail("当前用户没有该权限");
//        }

        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        long asno = jsonObject.get("asno").getAsLong();
        String ckdesc = jsonObject.get("ckdesc").getAsString();
        int ckstatus = jsonObject.get("ckstatus").getAsInt();
        int astype = jsonObject.get("astype").getAsInt();
        double refamt = jsonObject.get("refamt").getAsDouble();
        double realrefamt = jsonObject.get("realrefamt").getAsDouble();

        String queryOrderno = "select orderno,compid,pdno,pkgno from {{?" + DSMConst.TD_TRAN_ASAPP + "}} where asno = ? ";

        List<Object[]> queryRet = baseDao.queryNativeSharding(0, TimeUtils.getCurrentYear(), queryOrderno, asno);

        if (queryRet == null || queryRet.isEmpty()) {
            return result.fail("操作失败");
        }

        int ret = baseDao.updateNativeSharding(0,
                TimeUtils.getCurrentYear(), UPD_ASAPP_CK_SQL,
                ckstatus, userSession.userId, userSession.userName, astype, ckdesc, refamt * 100, realrefamt * 100, asno);

        if(ret > 0){
            int ostatus = 3;
            int gstatus = 4;
            int asstatus = 0;

            int compid = Integer.parseInt(queryRet.get(0)[1].toString());
            String specifyStorePhone = IceRemoteUtil.getSpecifyStorePhone(compid);
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            if(ckstatus == 1){
                //1退款退货 2 仅退款
                if(astype == 1 || astype == 2){
                    executorService.execute(() ->SmsTempNo.sendMessageToSpecify(compid,specifyStorePhone,
                            SmsTempNo.AFTER_SALE_AUDIT_PASSED,asno+""));
                }
                ostatus = -2;
                gstatus = 3;

                if(astype == 3 || astype == 4){
                    SmsTempNo.sendMessageToSpecify(compid,specifyStorePhone,
                            SmsTempNo.AFTER_SALE_BILL_AUDIT_PASSED,asno+"");
                }
            }else{
                ostatus = 4;
                asstatus = 200;
                if(astype == 1 || astype == 2){
                    executorService.execute(() -> SmsTempNo.sendMessageToSpecify(compid,specifyStorePhone,
                            SmsTempNo.AFTER_SALE_AUDIT_FAILED_TO_PASSED,asno+"",ckdesc));
                }

                if(astype == 3 || astype == 4){
                    executorService.execute(() ->   SmsTempNo.sendMessageToSpecify(compid,specifyStorePhone,
                            SmsTempNo.AFTER_SALE_BILL_AUDIT_FAILED_TO_PASSED,asno+"",ckdesc));
                }

            }

            String updateOrderNew = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set ostatus=?, asstatus = ? "
                    + " where cstatus&1=0 and orderno=? and ostatus = ? ";

            int year = Integer.parseInt("20" + queryRet.get(0)[0].toString().substring(0, 2));
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[]{ostatus, asstatus, queryRet.get(0)[0],-1});
            params.add(new Object[]{gstatus, queryRet.get(0)[2],1,queryRet.get(0)[0],queryRet.get(0)[1], queryRet.get(0)[3]});
            baseDao.updateTransNativeSharding(Integer.parseInt(queryRet.get(0)[1].toString()),year,
                    new String[]{updateOrderNew,UPD_ORDER_GOODS_CK_SQL},params);
        }
        return ret > 0 ? result.success("操作成功") : result.fail("操作失败");
    }
}
