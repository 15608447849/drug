package com.onek.order;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.calculate.entity.Package;
import com.onek.calculate.entity.*;
import com.onek.calculate.util.DiscountUtil;
import com.onek.consts.CSTATUS;
import com.onek.context.AppContext;
import com.onek.entity.DelayedBase;
import com.onek.entity.TranOrder;
import com.onek.entity.TranOrderGoods;
import com.onek.entitys.Result;
import com.onek.prop.AppProperties;
import com.onek.property.LccProperties;
import com.onek.queue.delay.DelayedHandler;
import com.onek.queue.delay.RedisDelayedHandler;
import com.onek.util.*;
import com.onek.util.area.AreaFeeUtil;
import com.onek.util.discount.DiscountRuleStore;
import com.onek.util.order.RedisOrderUtil;
import com.onek.util.prod.ProdInfoStore;
import com.onek.util.stock.RedisStockUtil;
import constant.DSMConst;
import dao.BaseDAO;
import org.hyrdpf.util.LogUtil;
import util.*;
import util.http.HttpRequestUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static com.onek.order.PayModule.*;

/**
 * @服务名 orderServer
 * @author 11842
 * @version 1.1.1
 * @description 订单下单
 * @time 2019/4/16 16:01
 **/
public class TranOrderOptModule {

    private static AppProperties appProperties = AppProperties.INSTANCE;
    private static int cancelOrderMinute;

    static {
        try {
            LccProperties lccProperties = LccProperties.INSTANCE;

            cancelOrderMinute = Integer.parseInt(lccProperties.cancelOrderMinute);
        } catch (Exception e) {
            cancelOrderMinute = 30;
        }
    }

    public static final DelayedHandler<TranOrder> CANCEL_DELAYED =
            new RedisDelayedHandler<>("_CANEL_ORDERS", cancelOrderMinute,
                    (d) -> new TranOrderOptModule().cancelOrder(d.getOrderno(), d.getCusno()),
                    DelayedHandler.TIME_TYPE.MINUTES);

    private static final DelayedHandler<DelayedBase> TAKE_DELAYED =
            new RedisDelayedHandler<>("_TAKE_ORDERS", 120,
                    (d) -> new TranOrderOptModule().takeDelivery(d.getOrderNo(), d.getCompid()),
                    DelayedHandler.TIME_TYPE.HOUR);


    public static final DelayedHandler<DelayedBase> CONFIRM_RECEIPT =
            new RedisDelayedHandler<>("_CONFIRM_RECEIPT", 48,
                    (d) -> new OrderOptModule().comReceipt(d.getOrderNo(), d.getCompid()),
                    DelayedHandler.TIME_TYPE.HOUR);

    private static BaseDAO baseDao = BaseDAO.getBaseDAO();

    private static final String QUERY_TRAN_ORDER_PARAMS =
            " ord.orderno, ord.tradeno, ord.cusno, ord.busno, ord.ostatus, "
                    + " ord.asstatus, ord.pdnum, ord.pdamt, ord.freight, ord.payamt, "
                    + " ord.coupamt, ord.distamt, ord.rvaddno, ord.shipdate, ord.shiptime, "
                    + " ord.settstatus, ord.settdate, ord.setttime, ord.otype, ord.odate, "
                    + " ord.otime, ord.cstatus, ord.consignee, ord.contact, ord.address ";

    private static final String FROM_BK_ORDER = " {{?" + DSMConst.TD_BK_TRAN_ORDER + "}} ord ";

    //订单表新增
    private static final String INSERT_TRAN_ORDER = "insert into {{?" + DSMConst.TD_TRAN_ORDER + "}} "
            + "(orderno,tradeno,cusno,busno,ostatus,"
            + "asstatus,pdnum,pdamt,freight,payamt,"
            + "coupamt,distamt,rvaddno,settstatus,otype,"
            + "odate,otime,cstatus,consignee,contact,"
            + "address,balamt,payway,remarks,invoicetype) "
            + " values(?,?,?,?,?,"
            + "?,?,?,?,?,"
            + "?,?,?,?,?,"
            + "CURRENT_DATE,CURRENT_TIME,0,?,?,"
            + "?,?,-1,?,?)";

    //订单商品表新增
    private static final String INSERT_TRAN_GOODS = "insert into {{?" + DSMConst.TD_TRAN_GOODS + "}} "
            + "(unqid,orderno,compid,pdno,pdprice,"
            + "distprice,payamt,coupamt,promtype,pkgno,"
            + "pnum,asstatus,createdate,createtime,cstatus,"
            + "actcode,balamt) "
            + " values(?,?,?,?,?,"
            + "?,?,?,?,?,"
            + "?,0,CURRENT_DATE,CURRENT_TIME,0,"
            + "?,?)";

    private static final String UPD_TRAN_GOODS = "update {{?" + DSMConst.TD_TRAN_GOODS + "}} set "
            + "orderno=?, pdprice=?, distprice=?,payamt=?,coupamt=?,promtype=?,"
            + "pkgno=?,createdate=CURRENT_DATE,createtime=CURRENT_TIME, actcode=?,balamt=? where cstatus&1=0 and "
            + " pdno=? and compid = ? and orderno=0 and pkgno = ? ";

    private static final String UPD_TRAN_GOODS_NEW = "update {{?" + DSMConst.TD_TRAN_GOODS + "}} set "
            + "orderno=?, pdprice=?, distprice=?,payamt=?,coupamt=?,promtype=?,"
            + "pkgno=?,createdate=CURRENT_DATE,createtime=CURRENT_TIME, actcode=?,balamt=?, pnum=?,cstatus=cstatus&~1 where "
            + " pdno=? and compid = ? and orderno=0";

    //是否要减商品总库存 远程调用
    private static final String UPD_GOODS = "update {{?" + DSMConst.TD_PROD_SKU + "}} set "
            + "freezestore=freezestore+? where cstatus&1=0 and sku=? ";

    //更新订单状态
    private static final String UPD_ORDER_STATUS = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set ostatus=? "
            + " where cstatus&1=0 and orderno=? and ostatus=?";

    //释放商品冻结库存 远程调用
    private static final String UPD_GOODS_FSTORE = "update {{?" + DSMConst.TD_PROD_SKU + "}} set "
            + "freezestore=freezestore-? where cstatus&1=0 and sku=? ";

    //更新优惠券领取表
    private static final String UPD_COUENT_SQL = "update {{?" + DSMConst.TD_PROM_COUENT + "}} set orderno = ?, "
            + "cstatus=cstatus|64 where cstatus&1=0 and unqid=? ";

    // 确认发货
    private static final String UPDATE_DELIVERY =
            " UPDATE {{?" + DSMConst.TD_TRAN_ORDER + "}} "
                    + " SET ostatus = 2, shipdate = CURRENT_DATE, shiptime = CURRENT_TIME "
                    + " WHERE cstatus&1 = 0 AND ostatus = 1 AND orderno = ? ";

    // 确认收货
    private static final String UPDATE_TAKE_DELIVERY =
            " UPDATE {{?" + DSMConst.TD_TRAN_ORDER + "}} "
                    + " SET ostatus = 3 "
                    + " WHERE cstatus&1 = 0 AND ostatus = 2 AND orderno = ? ";

    //远程调用
    private static final String UPDATE_PROM_COURCD = "update {{?" + DSMConst.TD_PROM_COURCD
            + "}} set cstatus=cstatus|1 where cstatus&1=0 and cstatus&64=0 and coupno=? and compid=?";



    private static final String QUERY_ORDER_BASE =
            " SELECT " + QUERY_TRAN_ORDER_PARAMS
                    + " FROM " + FROM_BK_ORDER
                    + " WHERE ord.cstatus&1 = 0 ";


    private static final String INSERT_GIFT =
            " INSERT INTO {{?" + DSMConst.TD_TRAN_REBATE + "}} "
            + " (unqid, orderno, compid, rebate, createdate, createtime) "
            + " VALUES (?, ?, ?, ?, CURRENT_DATE, CURRENT_TIME) ";

    private static final  String QUERY_ORDER_BAL = "SELECT balamt from {{?"+DSMConst.TD_TRAN_ORDER+"}} where balamt > 0 and orderno = ? and ostatus = ? ";

    private static final String UPDATE_COMP_BAL = "update {{?" + DSMConst.TB_COMP + "}} "
            + "set balance = IF((balance + ?) <=0,0,(balance + ?)) where cid = ? ";

    //更新优惠券领取表
    private static final String UPD_COUENT_BACK_SQL = "update {{?" + DSMConst.TD_PROM_COUENT + "}} set orderno = 0, "
            + "cstatus=cstatus & ~64 where cstatus & 64 > 0 and cstatus & 1 = 0 and orderno = ? and ctype = ? ";


    /**
     * @接口摘要 下单生成订单接口
     * @业务场景 前端下单调用
     * @传参类型 json
     * @传参列表  {placeType 下单类型(购物车还是直接下单) balway 是否使用余额支付 coupon 优惠券码 unqid 优惠券使用表唯一码
     *              orderObj: {remarks 备注 cusno 买家企业码 busno 卖家企业码 consignee 收货人姓名
     *              contact 收货人联系方式 rvaddno 收货地区码 address 详细收货地址 invoicetype 发票类型
     *              goodsArr:[{pdno 商品sku pnum 商品数量 pdprice 商品原价 actcode [参加的活动数组]}]
     *              orderType 订单类型 actcode[参加的活动数组]}}
     * @返回列表 200成功
     */
    @UserPermission(ignore = false)
    public Result placeOrder(AppContext appContext) {
        long unqid = 0, coupon = 0;//优惠券码
        Result result = new Result();
        Gson gson = new Gson();
        LinkedList<String> sqlList = new LinkedList<>();
        LinkedList<Object[]> params = new LinkedList<>();
        List<TranOrderGoods> tranOrderGoods = new ArrayList<>();
        String json = appContext.param.json;
        JSONObject jsonObject = JSON.parseObject(json);
        int orderType = 0;//下单类型 0普通
        if (jsonObject.containsKey("orderType") && !jsonObject.getString("orderType").isEmpty()) {
            orderType = jsonObject.getInteger("orderType");
        }
        int placeType = jsonObject.getInteger("placeType");//1、直接下单 2、购物车下单
        JSONObject orderObj = jsonObject.getJSONObject("orderObj");
        JSONArray goodsArr = jsonObject.getJSONArray("goodsArr");
        if (goodsArr == null || goodsArr.size() == 0) {
            LogUtil.getDefaultLogger().info("print by cyq placeOrder ----------- 下单操作未选择商品");
            return result.fail("请选择商品！");
        }
        TranOrder tranOrder = JSON.parseObject(orderObj.toJSONString(), TranOrder.class);
        if (tranOrder == null) return result.fail("订单信息有误");
        if(StringUtils.isEmpty(tranOrder.getConsignee()) || StringUtils.isEmpty(tranOrder.getContact())
                || Long.valueOf(tranOrder.getContact()) <= 0) {
            return result.fail("请填写收货人信息");
        }
        if (!jsonObject.getString("unqid").isEmpty()) {
            unqid = jsonObject.getLong("unqid");
        }
        if (!jsonObject.getString("coupon").isEmpty()) {
            coupon = jsonObject.getLong("coupon");
        }
        int pdnum = 0;
        for (int i = 0; i < goodsArr.size(); i++) {
            TranOrderGoods goodsVO = gson.fromJson(goodsArr.get(i).toString(), TranOrderGoods.class);
            if (goodsVO.isGift() || goodsVO.getPdprice() <= .0) {
                continue;
            }
            pdnum += goodsVO.getPnum();
            goodsVO.setCompid(tranOrder.getCusno());
            goodsVO.setActcode(String.valueOf(goodsVO.getActcode()));
            goodsVO.setPayamt(goodsVO.getPnum()*goodsVO.getPdprice());
            tranOrderGoods.add(goodsVO);
        }
        tranOrder.setPdnum(pdnum);
        List<GoodsStock> goodsStockList = new ArrayList<>();
        String orderNo = GenIdUtil.getOrderId(tranOrder.getCusno());//订单号生成
        tranOrder.setOrderno(orderNo);
        //订单费用计算（费用分摊以及总费用计算）
        try {
            LogUtil.getDefaultLogger().info("print by cyq placeOrder -----------下单操作计算价格开始");
            calculatePrice(tranOrderGoods, tranOrder, unqid, sqlList, params);
            LogUtil.getDefaultLogger().info("print by cyq placeOrder -----------下单操作计算价格结束");
        } catch (IllegalArgumentException e) {
            return result.fail(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.getDefaultLogger().info("print by cyq placeOrder -----------下单操作计算价格失败");
            return result.fail("下单失败");
        }

        String cStr = null;
        try {
            cStr = theGoodsHasChange(tranOrderGoods, tranOrder.getCusno(), placeType);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (cStr != null) {
            return result.fail(cStr);
        }
        if (orderType == 0) {
            //库存判断
            try {
                long sku = stockIsEnough(goodsStockList,tranOrderGoods);
                if (sku > 0) {
                    //库存不足处理
                    stockRecovery(goodsStockList);
                    return result.fail("【" + ProdInfoStore.getProdBySku(sku).getProdname()+ "】库存不足！");
                }
            } catch (Exception e) {
              /*  LogUtil.getDefaultLogger().info(Arrays.toString(e.getStackTrace()));
                e.printStackTrace();*/
                LogUtil.getDefaultLogger().info("print by placeOrder--------->>>>redis库存扣减库存失败！");
                return  result.fail("下单减库存失败！");
            }
        } else if (orderType == 1) { // 秒杀
            //库存判断
            long actcode = 0;
            if (jsonObject.containsKey("actcode") && !jsonObject.getString("actcode").isEmpty()) {
                actcode = jsonObject.getLong("actcode");
            }
            List<TranOrderGoods> goodsList = secKillStockIsEnough(actcode, tranOrderGoods);
            if (goodsList.size() != tranOrderGoods.size()) {
                //库存不足处理
                secKillStockRecovery(actcode, goodsList);
                return result.fail("商品库存不足！");
            }
            int num = RedisOrderUtil.getActBuyNum(tranOrder.getCusno(), tranOrderGoods.get(0).getPdno() ,actcode);
            int limitNum = RedisOrderUtil.getActLimit(tranOrderGoods.get(0).getPdno(), actcode);
            if(num > 0 && limitNum > 0 && (limitNum - (num + tranOrderGoods.get(0).getPnum())) < 0){
                return new Result().fail("秒杀商品下单数量过多或秒杀次数过于频繁!");
            }
        }
        double payamt = tranOrder.getPayamt();
        double bal = 0;
        int balway = 0;
        //数据库相关操作
        try{
            if (jsonObject.containsKey("balway") && !jsonObject.getString("balway").isEmpty()) {
                balway = jsonObject.getInteger("balway");
            }
            if(balway > 0){
                bal = IceRemoteUtil.queryCompBal(tranOrder.getCusno());

                //可抵扣余额
                int useBal = MathUtil.exactSub(CouponRevModule.getUseBal(payamt,new HashMap()), 0).intValue();
//                appContext.logger.print("线上支付金额："+ payamt);
//                appContext.logger.print("余额支付金额："+ bal);
//                appContext.logger.print("最高可抵扣余额：" + useBal) ;
                if(useBal>0) {
                    if (bal >= useBal) { //余额大于可抵扣余额
                        payamt = MathUtil.exactSub(payamt, useBal).
                                setScale(2, RoundingMode.DOWN).intValue(); //支付金额-可抵扣余额=支付金额
                        bal = useBal;//余额抵扣为可抵扣余额
                    } else {//余额小于可抵扣余额
                        payamt = MathUtil.exactSub(payamt, bal).
                                setScale(2, RoundingMode.DOWN).intValue(); //支付金额-当前用户拥有余额=支付金额
                    }
                }
                bal = Math.max(bal, 0);
//                appContext.logger.print("end线上支付金额："+ payamt);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        sqlList.addFirst(INSERT_TRAN_ORDER);
        params.addFirst(new Object[]{orderNo, 0, tranOrder.getCusno(), tranOrder.getBusno(), 0, 0, tranOrder.getPdnum(),
                tranOrder.getPdamt(), tranOrder.getFreight(), payamt, tranOrder.getCoupamt(), tranOrder.getDistamt(),
                tranOrder.getRvaddno(), 0, 0, tranOrder.getConsignee(), tranOrder.getContact(), tranOrder.getAddress(),
                bal, tranOrder.getRemarks(), tranOrder.getInvoicetype()});
        if (unqid > 0) {
            //使用优惠券
            sqlList.add(UPD_COUENT_SQL);
            params.add(new Object[]{orderNo,unqid});
        }
        //分摊余额
        if(bal > 0){
            apportionBal(tranOrderGoods,bal,payamt,tranOrder.getFreight());
        }
        if (placeType == 1) {
            getInsertSqlList(sqlList, params, tranOrderGoods, orderNo);
        } else {
            getUpdSqlList(sqlList, params, tranOrderGoods, orderNo);
        }
        String[] sqlNative = new String[sqlList.size()];
        sqlNative = sqlList.toArray(sqlNative);
        int year = Integer.parseInt("20" + orderNo.substring(0, 2));
        boolean b = !ModelUtil.updateTransEmpty(baseDao.updateTransNativeSharding(tranOrder.getCusno(), year, sqlNative, params));
        if (b) {
            //下单成功后续操作
            otherPlaceOrderOpt(tranOrder, coupon, orderNo, tranOrderGoods, bal);
            CANCEL_DELAYED.add(tranOrder);
            JsonObject object = new JsonObject();
            object.addProperty("orderno", orderNo);
            object.addProperty("message", "下单成功");
            return result.success(object);
        } else {//下单失败
            //库存处理
            stockRecovery(goodsStockList);
            return result.fail("下单失败");
        }
    }

    private String theGoodsHasChange(List<TranOrderGoods> tranOrderGoods, int compId, int placeType) {
        if (placeType == 1) return null;
        Set<String> pkgNoSet = new HashSet<>();
        Set<String> zeroNoSet = new HashSet<>();
        for (TranOrderGoods transGoods: tranOrderGoods) {
            if (transGoods.getPkgno() > 0) {
                pkgNoSet.add(transGoods.getPkgno() + "");
            } else {
                zeroNoSet.add(transGoods.getPdno() + "");
            }
        }
        String selectGoodsSQL = "select pdno, pnum, pkgno from {{?" + DSMConst.TD_TRAN_GOODS + "}} where cstatus&1=0 "
                 + " and orderno=0 and compid=" + compId ;
        String pdNoStr = " (pdno in(" + String.join(",",zeroNoSet.toArray(new String[0])) + ") and pkgno=0) ";
        String pkgNoStr = " pkgno in(" + String.join(",",pkgNoSet.toArray(new String[0])) + ") ";
        if (zeroNoSet.size() > 0 && pkgNoSet.size() == 0) {
            selectGoodsSQL = selectGoodsSQL + " and " + pdNoStr;
        }
        if (pkgNoSet.size() > 0 && zeroNoSet.size() == 0) {
            selectGoodsSQL = selectGoodsSQL + " and " + pkgNoStr;
        }
        if (pkgNoSet.size() > 0 && zeroNoSet.size() > 0) {
            selectGoodsSQL = selectGoodsSQL + "  and  (" + pdNoStr + " or " + pkgNoStr + ")";
        }
        List<Object[]> queryResult = baseDao.queryNativeSharding(compId, TimeUtils.getCurrentYear(), selectGoodsSQL);
        if (queryResult == null || queryResult.isEmpty()) {
            return "购物车商品发生改变";
        }
        if (tranOrderGoods.size() != queryResult.size()) {
            return "购物车商品发生改变！";
        }
        for (Object[] qr : queryResult) {
            for (TranOrderGoods goods: tranOrderGoods) {
                int pkgNo = qr[2] != null && !qr[2].toString().isEmpty() ? Integer.valueOf(qr[2].toString()) : 0;
                if (Long.valueOf(qr[0].toString()) == goods.getPdno() && goods.getPkgno() == pkgNo
                        && Integer.parseInt(qr[1].toString()) != goods.getPnum()) {
                    return "购物车商品数量发生改变！";
                }
            }
        }
        return null;
    }

    private void otherPlaceOrderOpt(TranOrder tranOrder, long coupon, String orderNo, List<TranOrderGoods> tranOrderGoods, double bal) {
        updateSku(tranOrderGoods);//若失败则需要处理（保证一致性）
        List<String> sqlList = new ArrayList<>();
        List<Object[]> params = new ArrayList<>();
        sqlList.add(UPDATE_COMP_BAL);
        params.add(new Object[]{-new Double(bal).intValue(),-new Double(bal).intValue(),tranOrder.getCusno()});
        if (coupon > 0) {
            sqlList.add(UPDATE_PROM_COURCD);
            params.add(new Object[]{coupon, tranOrder.getCusno()});
        }
        String[] sqlNative = new String[sqlList.size()];
        sqlNative = sqlList.toArray(sqlNative);
        //远程调用
        LogUtil.getDefaultLogger().info("订单号：【" + orderNo + "】-->>>print by cyq ---- 下单优惠券使用操作开始");
        boolean b = !ModelUtil.updateTransEmpty(IceRemoteUtil.updateTransNative(sqlNative,params));
        LogUtil.getDefaultLogger().info("订单号：【" + orderNo + "】-->>>print by cyq ---- 下单优惠券使用操作结果code>>>> " + b);
        addActBuyNum(tranOrder.getCusno(), tranOrderGoods);
        RedisOrderUtil.addOrderNumByCompid(tranOrder.getCusno());
//        ExecutorService executorService = Executors.newSingleThreadExecutor();
//        executorService.execute(()->{
//            try{
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//        });
    }



    private boolean updateSku(List<TranOrderGoods> tranOrderGoodsList) {
        List<Object[]> paramOne = new ArrayList<>();
        for (TranOrderGoods tranOrderGoods : tranOrderGoodsList) {
            paramOne.add(new Object[]{tranOrderGoods.getPnum(), tranOrderGoods.getPdno()});
        }
        //远程调用
        LogUtil.getDefaultLogger().info("订单号：【" + tranOrderGoodsList.get(0).getOrderno() + "】-->>>print by cyq ---- 下单锁定库存操作开始");
        boolean b = !ModelUtil.updateTransEmpty(IceRemoteUtil.updateBatchNative(UPD_GOODS, paramOne, tranOrderGoodsList.size()));
        LogUtil.getDefaultLogger().info("订单号：【" + tranOrderGoodsList.get(0).getOrderno() + "】-->>>print by cyq -------下单锁定库存结果b>>>>>>>>>>>> " + b );
        return b;
    }

    /* *
     * @description 价格计算
     * @params [tranOrderGoodsList, tranOrder]
     * @return void
     * @exception
     * @author 11842
     * @time  2019/4/17 15:50
     * @version 1.1.1
     **/
    private void calculatePrice(List<TranOrderGoods> tranOrderGoodsList, TranOrder tranOrder,
                                long coupon, List<String> sqlList, List<Object[]> params) {
        List<TranOrderGoods> finalTranOrderGoods = new ArrayList<>();//最终的商品详情
        List<IProduct> tempProds = new ArrayList<>();
        tranOrderGoodsList.forEach(tranOrderGoods -> {
            if (tranOrderGoods.getPkgno() > 0) {
                Package product = new Package();
                product.setPackageId(tranOrderGoods.getPkgno());
                product.setNums(tranOrderGoods.getPnum());
                tempProds.add(product);
            } else {
                Product product = new Product();
                product.setSku(tranOrderGoods.getPdno());
                product.autoSetCurrentPrice(tranOrderGoods.getPdprice(), tranOrderGoods.getPnum());
                tempProds.add(product);
            }
        });

        DiscountResult discountResult = CalculateUtil.calculate(tranOrder.getCusno(), tempProds, coupon);

        if (discountResult.isPkgExpired()) {
            throw new IllegalArgumentException("含有无效的套餐，请重新选择！");
        }

        int total = 0;
        for (IProduct tempProd : tempProds) {
            if (tempProd instanceof Package) {
                Package pkg = ((Package) tempProd);
                TranOrderGoods currGoods = null;
                for (TranOrderGoods tranOrderGoods : tranOrderGoodsList) {
                    if (tranOrderGoods.getPkgno() > 0
                        && tranOrderGoods.getPkgno() == pkg.getPackageId()) {
                        currGoods = tranOrderGoods;
                    }
                }

                if (currGoods == null) {
                    throw new IllegalArgumentException("不存在的套餐");
                }

                for (Product product : pkg.getPacageProdList()) {
                    TranOrderGoods temp = new TranOrderGoods();
                    temp.setActcode(currGoods.getActcode());
                    temp.setPnum(product.getNums());
                    temp.setPkgno((int) pkg.getPackageId());
                    temp.setPdno(product.getSku());
                    temp.setOrderno(tranOrder.getOrderno());
                    tranOrderGoodsList.add(temp);
                }

                total += pkg.getTotalNums();
            } else {
                total += tempProd.getNums();
            }
        }


        Iterator<TranOrderGoods> it = tranOrderGoodsList.iterator();
        while (it.hasNext()) {
            TranOrderGoods good = it.next();

            if (good.getPdno() == 0) {
                it.remove();
            }
        }

        tranOrder.setPdnum(total);

        // 保存返利信息
        if (!discountResult.getGiftList().isEmpty()) {
            sqlList.add(INSERT_GIFT);
            // unqid, orderno, compid, rebate
            params.add(new Object[] {
                    GenIdUtil.getUnqId(),
                    tranOrder.getOrderno(),
                    tranOrder.getCusno(),
                    JSON.toJSONString(discountResult.getGiftList())});
        }

//        if (discountResult == null) return finalTranOrderGoods;
        tranOrder.setPdamt((discountResult.getTotalCurrentPrice() + discountResult.getTotalDiscount()) * 100);
        tranOrder.setCoupamt((discountResult.getCouponValue() * 100));//订单使用优惠券(李康亮记得填)
        if (discountResult.isFreeShipping()) {//免邮
            tranOrder.setFreight(0);
        } else {
            tranOrder.setFreight(AreaFeeUtil.getFee(tranOrder.getRvaddno()) * 100);//运费(暂无)
        }
        tranOrder.setPayamt((discountResult.getTotalCurrentPrice() * 100) + tranOrder.getFreight());
        tranOrder.setDistamt((discountResult.getTotalDiscount() * 100));

        List<IDiscount> iDiscountList = discountResult.getActivityList();
        for (IDiscount iDiscount : iDiscountList) {
            for (int i = 0; i < iDiscount.getProductList().size(); i++) {
//                if (iDiscount.getLimits(iDiscount.getProductList().get(i).getSKU())
//                        - RedisOrderUtil.getActBuyNum(tranOrder.getCusno(), iDiscount.getProductList().get(i).getSKU(), iDiscount.getDiscountNo())
//                        < iDiscount.getProductList().get(i).getNums()) {
//                    throw new IllegalArgumentException("超过活动限购数！");
//                }

                TranOrderGoods tranOrderGoods = new TranOrderGoods();
                tranOrderGoods.setCompid(tranOrder.getCusno());
                if (iDiscount.getProductList().get(i) instanceof Package) {
                    Package pkg = (Package) iDiscount.getProductList().get(i);

                    for (Product product : pkg.getPacageProdList()) {
                        tranOrderGoods = new TranOrderGoods();
                        tranOrderGoods.setActcode(iDiscount.getDiscountNo() + "");
                        tranOrderGoods.setPkgno((int) iDiscount.getProductList().get(i).getSKU());
                        tranOrderGoods.setPdno(product.getSKU());
                        tranOrderGoods.setPdprice(product.getOriginalPrice());
                        tranOrderGoods.setDistprice(product.getDiscounted());
                        tranOrderGoods.setPayamt(product.getCurrentPrice());
                        finalTranOrderGoods.add(tranOrderGoods);
                    }

                } else {
                    tranOrderGoods.setPdno(iDiscount.getProductList().get(i).getSKU());
                    tranOrderGoods.setPdprice(iDiscount.getProductList().get(i).getOriginalPrice());
                    tranOrderGoods.setDistprice(iDiscount.getProductList().get(i).getDiscounted());
                    tranOrderGoods.setPayamt(iDiscount.getProductList().get(i).getCurrentPrice());
                    finalTranOrderGoods.add(tranOrderGoods);
                }
                for (TranOrderGoods finalTranOrderGood : finalTranOrderGoods) {
                    if (iDiscount.getProductList().get(i) instanceof Package) {
                        if (finalTranOrderGood.getPkgno() == iDiscount.getProductList().get(i).getSKU()) {
                            int ruleCode = DiscountRuleStore.getRuleByBRule((int) iDiscount.getBRule());
                            tranOrderGoods.setPromtype(ruleCode | finalTranOrderGood.getPromtype());
                        }
                    } else {
                        if (finalTranOrderGood.getPdno() == iDiscount.getProductList().get(i).getSKU()) {
                            int ruleCode = DiscountRuleStore.getRuleByBRule((int) iDiscount.getBRule());
                            tranOrderGoods.setPromtype(ruleCode | finalTranOrderGood.getPromtype());
                        }
                    }
                }
            }
        }

        for (TranOrderGoods goodsPrice : tranOrderGoodsList) {//传进来的
            for (TranOrderGoods finalGoods : finalTranOrderGoods) {
                if (goodsPrice.getPdno() == finalGoods.getPdno()
                        && goodsPrice.getPkgno() == finalGoods.getPkgno()) {
                    goodsPrice.setCompid(tranOrder.getCusno());
                    goodsPrice.setPdprice(finalGoods.getPdprice());
                    goodsPrice.setDistprice(finalGoods.getDistprice() * 100);
                    goodsPrice.setPayamt(finalGoods.getPayamt());
                    goodsPrice.setPromtype(finalGoods.getPromtype());
                }
            }
            goodsPrice.setPdprice(goodsPrice.getPdprice() * 100);
            goodsPrice.setPayamt(goodsPrice.getPayamt() * 100);
//            if (goodsPrice.getPayamt() == 0) {
//                goodsPrice.setPayamt(goodsPrice.getPdprice() * goodsPrice.getPnum());
//            }
        }
    }


    /**
     * @return boolean
     * @throws
     * @description 判断秒杀库存是否足够
     * @params []
     * @author jiangwenguang
     * @time 2019/4/20 11:49
     * @version 1.1.1
     **/
    private List<TranOrderGoods> secKillStockIsEnough(long actCode, List<TranOrderGoods> tranOrderGoodsList) {
        List<TranOrderGoods> goodsList = new ArrayList<>();
        List<Long> actList = new ArrayList<>();
        actList.add(actCode);
        for (TranOrderGoods tranOrderGoods : tranOrderGoodsList) {
            if (!RedisStockUtil.deductionActStock(tranOrderGoods.getPdno(), tranOrderGoods.getPnum(), actList)) {
                return goodsList;
            }
            tranOrderGoods.setActcode("[" + actCode + "]");
            goodsList.add(tranOrderGoods);
        }
        return goodsList;
    }

    /**
     * @return boolean
     * @throws
     * @description 秒杀库存恢复
     * @params [tranOrderGoodsList]
     * @author jiangwenguang
     * @time 2019/4/20 11:49
     * @version 1.1.1
     **/
    private void secKillStockRecovery(long actCode, List<TranOrderGoods> goodsList) {
        for (TranOrderGoods aGoodsList : goodsList) {
            RedisStockUtil.addActStock(aGoodsList.getPdno(), actCode, aGoodsList.getPnum());
        }
    }

    /**
     * @return boolean
     * @throws
     * @description 判断库存是否足够
     * @params []
     * @author 11842
     * @time 2019/4/17 11:49
     * @version 1.1.1
     **/
    private long stockIsEnough(List<GoodsStock> goodsStockList, List<TranOrderGoods> tranOrderGoodsList) {
        for (TranOrderGoods tranOrderGoods : tranOrderGoodsList) {
            List<Long> list = new ArrayList<>();
            String actCodeStr = tranOrderGoods.getActcode();
//            LogUtil.getDefaultLogger().info("actCodeStr11111111111111122222222222222222 " + actCodeStr);
            if (!StringUtils.isEmpty(actCodeStr)) {
                list = JSON.parseArray(actCodeStr).toJavaList(Long.class);
            }
            if (list.size() == 0) {//无活动码
                if (RedisStockUtil.deductionStock(tranOrderGoods.getPdno(),
                        tranOrderGoods.getPnum()) != 2) {
                    return tranOrderGoods.getPdno();
                } else {
                    setGoodsStock(tranOrderGoods, 0L, goodsStockList);
                }
            } else {
                if (!RedisStockUtil.deductionActStock(tranOrderGoods.getPdno(),
                        tranOrderGoods.getPnum(), list)) {
                    return tranOrderGoods.getPdno();
                } else {
                    for (Long aList : list) {
                        setGoodsStock(tranOrderGoods, aList, goodsStockList);
                    }
                }
            }
        }
        return 0;
    }

    private List<GoodsStock> setGoodsStock(TranOrderGoods tranOrderGoods, Long aList, List<GoodsStock> goodsStockList) {
        GoodsStock goodsStock = new GoodsStock();
        goodsStock.setActCode(aList);
        goodsStock.setSku(tranOrderGoods.getPdno());
        goodsStock.setStock(tranOrderGoods.getPnum());
        goodsStockList.add(goodsStock);
        return goodsStockList;
    }

    /**
     * @return boolean
     * @throws
     * @description 库存恢复
     * @params [tranOrderGoodsList]
     * @author 11842
     * @time 2019/4/17 14:38
     * @version 1.1.1
     **/
    private void stockRecovery(List<GoodsStock> goodsStockList) {

        for (GoodsStock goodsStock : goodsStockList) {
            if (goodsStock.getActCode() > 0) {
                RedisStockUtil.addActStock(goodsStock.getSku(), goodsStock.getActCode(), goodsStock.getStock());
            }
        }

        List<Long> gcodeList = new ArrayList<>();
        for (GoodsStock goodsStock : goodsStockList) {
            if(!gcodeList.contains(goodsStock.getSku())){
                RedisStockUtil.addStock(goodsStock.getSku(), goodsStock.getStock());
            }
            gcodeList.add(goodsStock.getSku());
        }
    }


    /* *
     * @description
     * @params [sqlList, params, tranOrderGoods, orderNo]
     * @return void
     * @exception
     * @author 11842
     * @time  2019/4/16 17:41
     * @version 1.1.1
     **/
    private void getInsertSqlList(List<String> sqlList, List<Object[]> params, List<TranOrderGoods> tranOrderGoodsList,
                                  String orderNo) {
        for (TranOrderGoods tranOrderGoods : tranOrderGoodsList) {
            sqlList.add(INSERT_TRAN_GOODS);
//            unqid,orderno,compid,pdno,pdprice,distprice,payamt,
//                    coupamt,promtype,pkgno,asstatus,createdate,createtime,cstatus
            params.add(new Object[]{GenIdUtil.getUnqId(), orderNo, tranOrderGoods.getCompid(), tranOrderGoods.getPdno(),
                    tranOrderGoods.getPdprice(), tranOrderGoods.getDistprice(), tranOrderGoods.getPayamt(), tranOrderGoods.getCoupamt(),
                    tranOrderGoods.getPromtype(), tranOrderGoods.getPkgno(), tranOrderGoods.getPnum(), tranOrderGoods.getActcode(),tranOrderGoods.getBalamt()});
        }

    }

    /* *
     * @description
     * @params [sqlList, params, tranOrderGoodsList, orderNo]
     * @return void
     * @exception
     * @author 11842
     * @time  2019/4/16 17:55
     * @version 1.1.1
     **/
    private void getUpdSqlList(List<String> sqlList, List<Object[]> params, List<TranOrderGoods> tranOrderGoodsList,
                               String orderNo) {
        for (TranOrderGoods tranOrderGoods : tranOrderGoodsList) {
            sqlList.add(UPD_TRAN_GOODS);
//            + "orderno=?, pdprice=?, distprice=?,payamt=?,coupamt=?,promtype=?,"
//                    + "pkgno=?,createdate=CURRENT_DATE,createtime=CURRENT_TIME, actcode=? where cstatus&1=0 and "
//                    + " pdno=? and orderno=0";
            params.add(new Object[]{orderNo, tranOrderGoods.getPdprice(), tranOrderGoods.getDistprice(), tranOrderGoods.getPayamt(),
                    tranOrderGoods.getCoupamt(), tranOrderGoods.getPromtype(), tranOrderGoods.getPkgno(), tranOrderGoods.getActcode(),
                    tranOrderGoods.getBalamt(),tranOrderGoods.getPdno(),tranOrderGoods.getCompid(), tranOrderGoods.getPkgno()});
        }
    }

    private void getUpdSqlListNew(List<String> sqlList, List<Object[]> params, List<TranOrderGoods> tranOrderGoodsList,
                               String orderNo) {
        for (TranOrderGoods tranOrderGoods : tranOrderGoodsList) {
            sqlList.add(UPD_TRAN_GOODS_NEW);
            params.add(new Object[]{orderNo, tranOrderGoods.getPdprice(), tranOrderGoods.getDistprice(), tranOrderGoods.getPayamt(),
                    tranOrderGoods.getCoupamt(), tranOrderGoods.getPromtype(), tranOrderGoods.getPkgno(), tranOrderGoods.getActcode(),
                    tranOrderGoods.getBalamt(),tranOrderGoods.getPnum(),tranOrderGoods.getPdno(),tranOrderGoods.getCompid()});
        }
    }

    /**
     * 添加活动购买量
     *
     * @param compid
     * @param tranOrderGoodsList
     */
    private void addActBuyNum(int compid,List<TranOrderGoods> tranOrderGoodsList) {

        for (TranOrderGoods tranOrderGoods : tranOrderGoodsList) {
            String actCodeStr = tranOrderGoods.getActcode();
            List<Long> list = JSON.parseArray(actCodeStr).toJavaList(Long.class);
            if (list.size() > 0) {
                for (Long aList : list) {
                    if (aList > 0) {
                        if (aList > 0) {
                            RedisOrderUtil.addActBuyNum(compid, tranOrderGoods.getPdno(), aList, tranOrderGoods.getPnum());
                        }
                    }

                }

            }
        }
    }


    /**
     * @接口摘要 取消订单
     * @业务场景 自动取消或线上支付取消
     * @传参类型 json
     * @传参列表 {orderno: 订单号 cusno：门店id}
     * @返回列表 200 成功 -1 失败
     */
    @UserPermission(ignore = false)
    public Result cancelOrder(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String orderNo = jsonObject.get("orderno").getAsString();//订单号
        int cusno = jsonObject.get("cusno").getAsInt(); //企业码
        boolean b = cancelOrder(orderNo, cusno);
        if (b) {
            CANCEL_DELAYED.removeByKey(orderNo);
        }
        return b ? result.success("取消成功") : result.fail("取消失败");
    }

    public boolean cancelOrder(String orderNo, int cusno) {
        int year = Integer.parseInt("20" + orderNo.substring(0, 2));
        int res = baseDao.updateNativeSharding(cusno, year, UPD_ORDER_STATUS, -4, orderNo, 0);
        if (res > 0) {
            recoverGoodsStock(orderNo, cusno,0);
            List<Object[]> queryResult = baseDao.queryNativeSharding(cusno, year, QUERY_ORDER_BAL, orderNo, -4);
            if(queryResult != null && !queryResult.isEmpty()){
                int bal = Integer.parseInt(queryResult.get(0)[0].toString());
                LogUtil.getDefaultLogger().debug("订单取消退回余额开始："+bal);
                int rrrr = IceRemoteUtil.updateCompBal(cusno,bal);
                LogUtil.getDefaultLogger().debug("订单号【" + orderNo +"】---->>>订单取消退回余额结果：" + (rrrr>0));
            }
            baseDao.updateNativeSharding(cusno,year,UPD_COUENT_BACK_SQL,
                    new Object[]{orderNo,4});
        }
        return res > 0;
    }

    private void recoverGoodsStock(String orderNo, int cusno, int type) {
        List<Object[]> params = new ArrayList<>();
        TranOrderGoods[] tranOrderGoods = getGoodsArr(orderNo, cusno);
        for (TranOrderGoods tranOrderGood : tranOrderGoods) {
            String actCodeStr = tranOrderGood.getActcode();
            List<Long> list = JSON.parseArray(actCodeStr).toJavaList(Long.class);
            for (Long actcode: list) {
                if (actcode > 0) {
                    RedisStockUtil.addActStock(tranOrderGood.getPdno(), actcode, tranOrderGood.getPnum());
                    RedisOrderUtil.subtractActBuyNum(cusno, tranOrderGood.getPdno(), actcode, tranOrderGood.getPnum());
                }
            }
            RedisStockUtil.addStock(tranOrderGood.getPdno(), tranOrderGood.getPnum());//恢复redis库存
            params.add(new Object[]{tranOrderGood.getPnum(), tranOrderGood.getPdno()});
        }
        try{
            RedisOrderUtil.reduceOrderNumByCompid(cusno);
        }catch (Exception e){

        }
        if (type == 0) {//线上支付释放锁定库存 远程调用
            LogUtil.getDefaultLogger().info("订单号：【" + orderNo + "】-->>>print by cyq ------- 线上支付释放锁定库存操作开始");
            boolean res = !ModelUtil.updateTransEmpty(IceRemoteUtil.updateBatchNative(UPD_GOODS_FSTORE, params, tranOrderGoods.length));
            LogUtil.getDefaultLogger().info("订单号：【" + orderNo + "】-->>>print by cyq ------- 线上支付释放锁定库存res(false表示失败)>>>>>>>>>>>> " + res);
        }
    }


    /**
     * @接口摘要 取消订单
     * @业务场景 线下即付、线下到付门店30分钟内取消
     * @传参类型 json
     * @传参列表 {orderno: 订单号 cusno：门店id}
     * @返回列表 200 成功 -1 失败
     */
    @UserPermission(ignore = false)
    public Result cancelOffLineOrder(AppContext appContext) {
        //此时数据库库存已更新
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String orderNo = jsonObject.get("orderno").getAsString();//订单号
        int cusno = jsonObject.get("cusno").getAsInt(); //企业码
        int year = Integer.parseInt("20" + orderNo.substring(0, 2));
        String selectSQL = "select payway,balamt from {{?" + DSMConst.TD_TRAN_ORDER + "}} where orderno=? "
                + "and ((payway=4 and ostatus=0) or (payway=5 and ostatus=1)) and" +
                " ( unix_timestamp(CURRENT_TIMESTAMP) - unix_timestamp(CONCAT(odate,' ', otime)) ) < 30 * 60";
        String updateSQL =  "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set ostatus=? "
                + " where cstatus&1=0 and orderno=? ";
        List<Object[]> list = baseDao.queryNativeSharding(cusno, TimeUtils.getCurrentYear(), selectSQL, orderNo);
        if(list != null && list.size() > 0) {
            int code;
            int payway = (int) list.get(0)[0];//支付方式
            int balamt = (int)list.get(0)[1];//订单使用的余额
            if (payway == 4) {//线下即付（待付款状态）30分钟内可取消
                updateSQL = updateSQL + " and payway=4 and ostatus=0";
            } else {//线下到付（待发货状态）30分钟内可取消
                updateSQL = updateSQL + " and payway=5 and ostatus=1";
            }
            code = baseDao.updateNativeSharding(cusno, year, updateSQL, -4, orderNo);
            if (code > 0) {
                if (payway == 4) {//取消线下即付一小时轮询
//                    CANCEL_XXJF.removeByKey(orderNo);
                }
                if (payway == 5) {
                    //取消24小时轮询
                    DELIVERY_DELAYED.removeByKey(orderNo);
                }
                //库存操作(redis库存返还)
                recoverGoodsStock(orderNo, cusno, 1);
                //门店自己取消返还数据库相关库存
                LogUtil.getDefaultLogger().info("print by cyq cancelOffLineOrder offline-----------线下支付门店取消订单返还库存操作开始");
                addGoodsDbStock(orderNo, cusno);
//                LogUtil.getDefaultLogger().info("print by cyq cancelOffLineOrder offline-----------线下支付门店取消订单返还库存操作结束");
                //余额返回
                IceRemoteUtil.updateCompBal(cusno,balamt);
            }
        } else {
            return result.fail("订单取消失败");
        }
        return result.success("取消成功");
    }

    /**
     * @接口摘要 取消订单
     * @业务场景 客服取消订单
     * @传参类型 json
     * @传参列表 {orderno: 订单号 cusno：门店id}
     * @返回列表 200 成功 -1 失败
     */
    @UserPermission(ignore = true)
    public Result cancelBackOrder(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String orderNo = jsonObject.get("orderno").getAsString();//订单号
        int cusno = jsonObject.get("cusno").getAsInt(); //企业码
        int year = Integer.parseInt("20" + orderNo.substring(0, 2));
        String selectSQL = "select payway, balamt,payamt,settstatus,ostatus from {{?" + DSMConst.TD_TRAN_ORDER + "}} where orderno=?";
        List<Object[]> list = baseDao.queryNativeSharding(cusno, year, selectSQL, orderNo);
        if(list != null && list.size() > 0) {
            int payway = (int)list.get(0)[0];//支付方式
            int balamt = (int)list.get(0)[1];//订单使用的余额
            int payamt = (int)list.get(0)[2];//订单支付金额
            int settstatus = (int)list.get(0)[3];//订单支付金额
            int ostatus = (int)list.get(0)[4];//订单状态

            if (ostatus > 0 && settstatus == 1) {
                // 已结算待发货状态时取消 需要通知ERP。
                if (IceRemoteUtil.systemConfigOpen("ORDER_SYNC")) {
                    try {
                        int erpResult = cancelERPOrder(orderNo, cusno);

                        if (erpResult < 0) {
                            switch (erpResult) {
                                case -4:
                                    return new Result().fail("订单已出库，此单不允许取消！ Code:" + erpResult);
                                default:
                                    return new Result().fail("ERP处理异常，此单不允许取消！ Code:" + erpResult);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        return new Result().fail("调用ERP接口异常，此单不允许取消！");
                    }
                }
            }

            String updSQL = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set ostatus=?, cstatus=cstatus|? "
                    + " where cstatus&1=0 and orderno=? and ostatus=?";
            if (ostatus == 0 && payway == 4) {//线下转账未付款未结算客服取消订单
                if (baseDao.updateNativeSharding(cusno, year, updSQL,
                        -4, CSTATUS.ORDER_BACK_CANCEL, orderNo, 0) > 0) {
                   return offLineCancelOrderOpt(orderNo, cusno, balamt, settstatus, result);
                }
            } else {
                int res = baseDao.updateNativeSharding(cusno, year, updSQL, -4, CSTATUS.ORDER_BACK_CANCEL, orderNo, 1);
                if (res > 0) {//退款
                    if (payway == 4 || payway == 5) {//线下即付退款???
                        //取消24小时轮询
                        DELIVERY_DELAYED.removeByKey(orderNo);
                        LogUtil.getDefaultLogger().info("print by cyq cancelBackOrder offline-----------线下支付客服取消订单返还库存操作开始");
                        return offLineCancelOrderOpt(orderNo, cusno, balamt, settstatus, result);
                    } else {//线上支付退款
                        // 取消24小时轮询
                        DELIVERY_DELAYED.removeByKey(orderNo);
                        LogUtil.getDefaultLogger().info("print by cyq cancelBackOrder online-----------线上支付客服取消订单返还库存操作开始");

                        //客服取消返还数据库相关库存
                        addGoodsDbStock(orderNo, cusno);
                        //库存操作(redis库存返还)
                        recoverGoodsStock(orderNo, cusno, 0);
                        return result.success("订单取消成功，请及时处理退款并确认退款");
                    }
                }
            }
        }
        return result.fail("订单取消失败");
    }

    private int cancelERPOrder(String orderNo, int compid) throws IOException {
        String url = appProperties.erpUrlPrev + "/orderCSDCancel";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("orderno", orderNo);
        jsonObject.put("compid", compid);

        String result = HttpRequestUtil.postJson(url, jsonObject.toString());

        LogUtil.getDefaultLogger().info("生成订单调用ERP接口结果返回： " + result);

        if (!StringUtils.isEmpty(result)) {
            return new JsonParser().parse(result).getAsJsonObject().get("code").getAsInt();
        }

        return -2;
    }

    /* *
     * @description 线下订单相关操作
     * @params [orderNo, cusno, balamt, settstatus, result]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/6/12 14:55
     * @version 1.1.1
     **/
    private Result offLineCancelOrderOpt(String orderNo, int cusno, int balamt, int settstatus, Result result) {
        //客服取消返还数据库相关库存
        addGoodsDbStock(orderNo, cusno);
        //库存操作(redis库存返还)
        recoverGoodsStock(orderNo, cusno, 1);
        //未结算退回余额
        if (balamt > 0 && settstatus == 0) {
            IceRemoteUtil.updateCompBal(cusno, balamt);
            return result.success("订单取消成功！");
        }
        return result.success("订单取消成功，已付款的订单请及时进行退款处理！");
    }

    /**
     * @接口摘要 确认退款
     * @业务场景 客服确认退款
     * @传参类型 json
     * @传参列表 {orderno: 订单号 cusno：门店id}
     * @返回列表 200 成功 -1 失败
     */
    @UserPermission(ignore = true)
    public Result confirmRefund(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String orderNo = jsonObject.get("orderno").getAsString();//订单号
        int cusno = jsonObject.get("cusno").getAsInt(); //企业码
        int year = Integer.parseInt("20" + orderNo.substring(0, 2));
        String updSQL = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set settstatus=? "
                + " where cstatus&1=0 and orderno=? and ostatus=? and settstatus=?";
        String selectSQL = "select payway, balamt,payamt from {{?" + DSMConst.TD_TRAN_ORDER + "}} where orderno=?";
        List<Object[]> list = baseDao.queryNativeSharding(cusno, year, selectSQL, orderNo);
        if(list != null && list.size() > 0) {
            int payway = (int) list.get(0)[0];//支付方式
            int balamt = (int) list.get(0)[1];//订单使用的余额
            int payamt = (int) list.get(0)[2];//订单支付金额
            int res = baseDao.updateNativeSharding(cusno, year, updSQL, -1, orderNo, -4, 1);
            if (res > 0) {
                if (balamt > 0) {//退回余额
                    boolean b = refundBal(orderNo,cusno,balamt,"客服取消订单确认退款") > 0;
                    if (!b) {
                        //退款失败订单处理
                        baseDao.updateNativeSharding(cusno, year, updSQL, 1, orderNo, -4, -1);
                        return result.fail("确认退款失败");
                    }
                }

                if (payamt > 0 && payway <= 3) {
                    List<Object[]> transResult =
                            baseDao.queryNativeSharding(cusno, year,
                                " SELECT payprice, payway, paysource, tppno "
                                + " FROM {{?" + DSMConst.TD_TRAN_TRANS + "}} "
                                + " WHERE cstatus&(1+1024) = 0 AND orderno = ? AND payPrice > 0 "
                                + " AND paystatus = 1 AND payway IN (1, 2) ", orderNo);

                    if (transResult.isEmpty()) {
                        return result.success("请确认线下支付退款");
                    }

                    double payPrice = Double.parseDouble(transResult.get(0)[0].toString());
                    String payWay = transResult.get(0)[1].toString();
                    String paySource = transResult.get(0)[2].toString();
                    String tppno = transResult.get(0)[3].toString();
                    String refundno = String.valueOf(GenIdUtil.getUnqId());
                    long payUnq = GenIdUtil.getUnqId();

                    try {
                        baseDao.updateNativeSharding(cusno, year, " INSERT INTO {{?" + DSMConst.TD_TRAN_TRANS + "}} "
                                + " (unqid, compid, orderno, payno, payprice, "
                                + " payway, paysource, paystatus, payorderno, tppno, "
                                + " paydate, paytime, completedate, completetime, cstatus) VALUES "
                                + " (?,?,?,?,?, ?,?,?,?,?, CURRENT_DATE,CURRENT_TIME,NULL,NULL,?) ",
                                payUnq, cusno, orderNo, refundno, payPrice,
                                payWay, paySource, 0, 0, tppno, 1024);

                        HashMap<String, Object> refundResult = FileServerUtils.refund(
                                "1".equals(payWay) ? "wxpay" : "alipay", refundno,
                                tppno, MathUtil.exactDiv(payPrice, 100.0).doubleValue(), MathUtil.exactDiv(payPrice, 100.0).doubleValue(), "1".equals(paySource));
                        boolean r = false;
                        try {
                            r = refundResult.containsKey("code")
                                    && 2.0 == Double.parseDouble(refundResult.get("code").toString());

                            baseDao.updateNativeSharding(cusno, year, " UPDATE {{?" + DSMConst.TD_TRAN_TRANS + "}} "
                                    + " SET paystatus = ?, completedate = CURRENT_DATE, completetime = CURRENT_TIME "
                                    + " WHERE unqid = ? ", r ? 1 : -2, payUnq);

                        } catch (Exception e) {
                            e.printStackTrace();
                            if (!r) {
                                throw new Exception(refundResult.getOrDefault("message", "").toString());
                            }
                            return result.success("确认退款成功, 但退款状态未改变！");
                        }

                        if (!r) {
                            throw new Exception(refundResult.getOrDefault("message", "").toString());
                        }

                    } catch (Exception e) {
                        baseDao.updateNativeSharding(cusno, year, updSQL, 1, orderNo, -4, -1);

                        if (balamt > 0) {
                            int r = IceRemoteUtil.updateCompBal(cusno, -balamt);

                            if (r <= 0) {
                                return result.fail("确认退款失败, 余额扣减失败");
                            }
                        }
                        return result.fail("确认退款失败," + e.getMessage());
                    }
                }

                return result.success("确认退款成功");
            }
        }
        return result.fail("操作失败");

    }

    static TranOrderGoods[] getGoodsArr(String orderNo, int cusno) {
        String selectGoodsSql = "select pdno, pnum, payamt,actcode, pkgno from {{?" + DSMConst.TD_TRAN_GOODS + "}} where cstatus&1=0 "
                + " and orderno=" + orderNo;
        int year = Integer.parseInt("20" + orderNo.substring(0, 2));
        List<Object[]> queryResult = baseDao.queryNativeSharding(cusno, year, selectGoodsSql);
        TranOrderGoods[] tranOrderGoods = new TranOrderGoods[queryResult.size()];
        baseDao.convToEntity(queryResult, tranOrderGoods, TranOrderGoods.class, new String[]{
                "pdno", "pnum", "payamt", "actcode", "pkgno"
        });
        return tranOrderGoods;
    }

    private int getCompid(String orderno) {
        if (!StringUtils.isBiggerZero(orderno)) {
            return 0;
        }

        List<Object[]> queryResult = baseDao.queryNativeSharding(
                        0, TimeUtils.getYearByOrderno(orderno),
                        " SELECT cusno "
                        + " FROM {{?" + DSMConst.TD_BK_TRAN_ORDER + "}} "
                        + " WHERE cstatus&1 = 0 AND orderno = ? ", orderno);

        if (queryResult.isEmpty()) {
            return 0;
        }

        return Integer.parseInt(queryResult.get(0)[0].toString());
    }

    public boolean delivery(String orderno, int compid) {
        boolean result = BaseDAO.getBaseDAO().updateNativeSharding(compid, TimeUtils.getYearByOrderno(orderno),
                UPDATE_DELIVERY, orderno) > 0;

        if (result) {
            PayModule.DELIVERY_DELAYED.removeByKey(orderno);
            TAKE_DELAYED.add(new DelayedBase(compid, orderno));
/*            String sql = " SELECT payway "
                        + " FROM {{?" + DSMConst.TD_TRAN_TRANS + "}} "
                        + " WHERE cstatus&1 = 0 AND compid = ? AND orderno = ? ";

            List<Object[]> queryResult =
                    baseDao.queryNativeSharding(compid, TimeUtils.getYearByOrderno(orderno), sql, compid, orderno);

            if (!queryResult.isEmpty()) {
                int t = Integer.parseInt(queryResult.get(0)[0].toString());
                if (t != 4 && t != 5) {
                    TAKE_DELAYED.add(new DelayedBase(compid, orderno));
                }
            }*/

        }

        return result;
    }


    /**
     * @接口摘要 收货
     * @业务场景 一块物流司机APP签收时调用
     * @传参类型 arrays
     * @传参列表 [cusno：门店id, orderno: 订单号 ]
     * @返回列表 200 成功 -1 失败
     */
    @UserPermission(ignore = true)
    public Result takeDelivery(AppContext appContext) {
        String[] params = appContext.param.arrays;

        if (ArrayUtil.isEmpty(params)) {
            return new Result().fail("参数为空");
        }

        String orderNo = params[1];
        int compid = getCompid(orderNo);

        if (compid <= 0 || !StringUtils.isBiggerZero(orderNo)) {
            return new Result().fail("非法参数");
        }

        boolean result = takeDelivery(orderNo, compid);
        //生成节点信息四同步一块物流状态
        OrderUtil.changeYKWLOrderState(orderNo,4, compid);
        return result ? new Result().success("已签收") : new Result().fail("操作失败");
    }


    public boolean takeDelivery(String orderno, int compid) {
        boolean result = BaseDAO.getBaseDAO().updateNativeSharding(compid, TimeUtils.getYearByOrderno(orderno),
                UPDATE_TAKE_DELIVERY, orderno) > 0;

        if (result) {
            TAKE_DELAYED.removeByKey(orderno);
            CONFIRM_RECEIPT.add(new DelayedBase(compid, orderno));
        }

        return result;
    }

    /**
     * @接口摘要 发货
     * @业务场景 一块物流司机APP取货时调用
     * @传参类型 arrays
     * @传参列表 [ cusno：门店id, orderno: 订单号]
     * @返回列表 200 成功 -1 失败
     */
    @UserPermission(ignore = true)
    public Result delivery(AppContext appContext) {
        String[] params = appContext.param.arrays;

        if (ArrayUtil.isEmpty(params)) {
            return new Result().fail("参数为空");
        }

        String orderNo = params[1];
        int compid = getCompid(orderNo);

        if (compid <= 0 || !StringUtils.isBiggerZero(orderNo)) {
            return new Result().fail("非法参数");
        }

        boolean result = delivery(orderNo, compid);
        //生成节点信息二三 修改一块物流状态
        OrderUtil.changeYKWLOrderState(orderNo,3, compid);
        return result ? new Result().success("已发货") : new Result().fail("操作失败");
    }

    /**
     * @接口摘要 客服确认收款
     * @业务场景 线下转账确认收款
     * @传参类型 arrays
     * @传参列表 [ cusno：门店id, orderno: 订单号]
     * @返回列表 200 成功 -1 失败
     */
    @UserPermission(ignore = true)
    public Result confirmCash(AppContext appContext) {
        boolean b = false;
        String[] params = appContext.param.arrays;
        if (ArrayUtil.isEmpty(params)) {
            return new Result().fail("参数为空");
        }
        String orderNo = params[1];
        int compid = getCompid(orderNo);
        int year = Integer.parseInt("20" + orderNo.substring(0,2));

        if (compid <= 0 && !StringUtils.isBiggerZero(orderNo)) {
            return new Result().fail("非法参数");
        }
        List<String> sqlList = new ArrayList<>();
        List<Object[]> paramsObj = new ArrayList<>();
        String selectSQL = "select payamt,payway,balamt from {{?" + DSMConst.TD_TRAN_ORDER + "}} where orderno=? "
                + " and settstatus=0 and (payway=4 || payway=5) and ostatus<>-4";
        //线下到付确认收款  将订单结算状态改为已结算
        String updateXxdfSQL = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set settstatus=?, settdate=CURRENT_DATE,"
                + "setttime=CURRENT_TIME where cstatus&1=0 and orderno=? and payway=5 and settstatus=0";
        //线下即付确认收款
        String updateXxjfSQL = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set ostatus=(case ostatus when 0 then 1 "
                + " else ostatus end), settstatus=?,"
                + " settdate=CURRENT_DATE,setttime=CURRENT_TIME where cstatus&1=0 and orderno=? and "
                + " payway=4 and settstatus=0";
        //插入支付记录
        String insertPayerSQL = "insert into {{?" + DSMConst.TD_TRAN_PAYREC + "}} "
                + "(unqid,compid,payno,eventdesc,resultdesc,"
                + "completedate,completetime,cstatus)"
                + " values(?,?,?,?,?,"
                + "CURRENT_DATE,CURRENT_TIME,0)";

        //插入交易记录
        String insertTransSQL = "insert into {{?" + DSMConst.TD_TRAN_TRANS + "}} "
                + "(unqid,compid,orderno,payno,payprice,payway,paysource,paystatus,"
                + "payorderno,tppno,paydate,paytime,completedate,completetime,cstatus)"
                + " values(?,?,?,?,?,"
                + "?,?,?,?,?,"
                + "CURRENT_DATE,CURRENT_TIME,CURRENT_DATE,CURRENT_TIME,?)";
        List<Object[]> list = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(), selectSQL, orderNo);
        if(list != null && list.size() > 0) {
            TranOrder[] tranOrders = new TranOrder[list.size()];
            baseDao.convToEntity(list, tranOrders, TranOrder.class, new String[]{"payamt", "payway","balamt"});
            double payamt = tranOrders[0].getPayamt();
            double bal = tranOrders[0].getBalamt();
            int paytype = Integer.parseInt(tranOrders[0].getPayway().trim());
            if (paytype == 4) {
                sqlList.add(updateXxjfSQL);
            } else {
                sqlList.add(updateXxdfSQL);
            }
            paramsObj.add(new Object[]{1, orderNo});
            if (bal > 0) {
                sqlList.add(insertPayerSQL);
                paramsObj.add(new Object[]{GenIdUtil.getUnqId(),compid, 0, "{}", "{}"});
                sqlList.add(insertTransSQL);
                paramsObj.add(new Object[]{GenIdUtil.getUnqId(), compid, orderNo, 0,  tranOrders[0].getBalamt(), 0,
                        0, 1, GenIdUtil.getUnqId(), 0, 0});
            }
            if (payamt > 0) {
                sqlList.add(insertTransSQL);
                paramsObj.add(new Object[]{GenIdUtil.getUnqId(), compid, orderNo, 0, tranOrders[0].getPayamt(), paytype,
                        0, 1, GenIdUtil.getUnqId(), 0, 0});
            }
            String[] sqlNative = new String[sqlList.size()];
            sqlNative = sqlList.toArray(sqlNative);
            b = !ModelUtil.updateTransEmpty(baseDao.updateTransNativeSharding(compid,year, sqlNative, paramsObj));
            if (b) {
                if (paytype == 4) {
                    //线下转账确认收款后24小时变为已发货
                    DELIVERY_DELAYED.add(new DelayedBase(compid, orderNo));
                    //取消线下即付一小时轮询
//                    CANCEL_XXJF.removeByKey(orderNo);
                    //生成订单到一块物流
                    OrderUtil.generateLccOrder(compid, orderNo);
                }

//                try{
//                    //满赠赠优惠券
//                    CouponRevModule.revGiftCoupon(Long.parseLong(orderNo),compid);
//                }catch (Exception e){
//                    e.printStackTrace();
//                }

                //更新销量
                OrderUtil.updateSales(compid, orderNo);

            }
        }
        if (b) {
            //订单生成到ERP(异步执行)
            OrderDockedWithERPModule.generationOrder2ERP(orderNo, compid);
        }

        return b ? new Result().success("操作成功") : new Result().fail("操作失败");
    }

    /**
     * 扣减余额
     * @param compid
     * @param bal
     */
    public static void deductionBal(int compid,double bal){
        IOThreadUtils.runTask(()->{
            IceRemoteUtil.updateCompBal(compid,-new Double(bal).intValue());
        });
    }


    public static void apportionBal(List<TranOrderGoods> tranOrderGoodsList,double bal,double payment,double freight){
        double[] dprice = new double[tranOrderGoodsList.size()];
        double afterDiscountPrice = .0;
        bal = MathUtil.exactDiv(bal, 100.0).doubleValue();

        for (int i = 0; i < tranOrderGoodsList.size(); i++){
            dprice[i] = MathUtil.exactDiv(tranOrderGoodsList.get(i).getPayamt(), 100.0).doubleValue();

            afterDiscountPrice =
                    MathUtil.exactAdd(afterDiscountPrice, dprice[i])
                            .setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        }


        bal = Math.min(bal, afterDiscountPrice);

        double[] cdprice = DiscountUtil.shareDiscount(dprice, bal);

        for (int i = 0; i < tranOrderGoodsList.size(); i++){
            tranOrderGoodsList.get(i).setBalamt(MathUtil.exactSub(dprice[i],cdprice[i])
                    .setScale(2,BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue());

            tranOrderGoodsList.get(i).setPayamt(MathUtil.exactSub(tranOrderGoodsList.get(i).getPayamt(),
                    tranOrderGoodsList.get(i).getBalamt()).
                    setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue());
        }

//        LogUtil.getDefaultLogger().info("---- bal " + bal);
//        LogUtil.getDefaultLogger().info("---- dprice " + Arrays.toString(dprice));
//        LogUtil.getDefaultLogger().info("---- cdprice " + Arrays.toString(cdprice));
//        LogUtil.getDefaultLogger().info("---- afterDiscountPrice " + afterDiscountPrice);

    }




    class GoodsStock {
        private long sku;
        private int stock;
        private long actCode;

        public long getSku() {
            return sku;
        }

        public void setSku(long sku) {
            this.sku = sku;
        }

        public int getStock() {
            return stock;
        }

        public void setStock(int stock) {
            this.stock = stock;
        }

        public long getActCode() {
            return actCode;
        }

        public void setActCode(long actCode) {
            this.actCode = actCode;
        }
    }
}