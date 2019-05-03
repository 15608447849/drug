package com.onek.order;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.*;
import com.onek.annotation.UserPermission;
import com.onek.calculate.entity.DiscountResult;
import com.onek.calculate.entity.IDiscount;
import com.onek.calculate.entity.Product;
import com.onek.context.AppContext;
import com.onek.entity.DelayedBase;
import com.onek.entity.TranOrder;
import com.onek.entity.TranOrderGoods;
import com.onek.entitys.Result;
import com.onek.queue.delay.DelayedHandler;
import com.onek.queue.delay.RedisDelayedHandler;
import com.onek.util.CalculateUtil;
import com.onek.util.GenIdUtil;
import com.onek.util.area.AreaFeeUtil;
import com.onek.util.stock.RedisStockUtil;
import constant.DSMConst;
import dao.BaseDAO;
import org.hyrdpf.util.LogUtil;
import util.ArrayUtil;
import util.ModelUtil;
import util.StringUtils;
import util.TimeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 11842
 * @version 1.1.1
 * @description 订单下单
 * @time 2019/4/16 16:01
 **/
public class TranOrderOptModule {
    private static final DelayedHandler<TranOrder> CANCEL_DELAYED =
            new RedisDelayedHandler<>("_CANEL_ORDERS", 15,
                    (d) -> new TranOrderOptModule().cancelOrder(d.getOrderno(), d.getCusno()),
                    DelayedHandler.TIME_TYPE.MINUTES);

    private static final DelayedHandler<DelayedBase> TAKE_DELAYED =
            new RedisDelayedHandler<>("_TAKE_ORDERS", 48,
                    (d) -> new TranOrderOptModule().takeDelivery(d.getOrderNo(), d.getCompid()),
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
            + "(orderno,tradeno,cusno,busno,ostatus,asstatus,pdnum," +
            "pdamt,freight,payamt,coupamt,distamt,rvaddno," +
            "settstatus,otype,odate,otime,cstatus,consignee,contact,address) "
            + " values(?,?,?,?,?,"
            + "?,?,?,?,?,"
            + "?,?,?,?,?,"
            + "CURRENT_DATE,CURRENT_TIME,0,?,?,?)";

    //订单商品表新增
    private static final String INSERT_TRAN_GOODS = "insert into {{?" + DSMConst.TD_TRAN_GOODS + "}} "
            + "(unqid,orderno,compid,pdno,pdprice,distprice,payamt,"
            + "coupamt,promtype,pkgno,pnum,asstatus,createdate,createtime,cstatus,actcode) "
            + " values(?,?,?,?,?,"
            + "?,?,?,?,?,"
            + "?,0,CURRENT_DATE,CURRENT_TIME,0,?)";

    private static final String UPD_TRAN_GOODS = "update {{?" + DSMConst.TD_TRAN_GOODS + "}} set "
            + "orderno=?, pdprice=?, distprice=?,payamt=?,coupamt=?,promtype=?,"
            + "pkgno=?,createdate=CURRENT_DATE,createtime=CURRENT_TIME, actcode=? where cstatus&1=0 and "
            + " pdno=? and orderno=0";

    //是否要减商品总库存
    private static final String UPD_GOODS = "update {{?" + DSMConst.TD_PROD_SKU + "}} set "
            + "freezestore=freezestore+? where cstatus&1=0 and sku=? ";

    //更新订单状态
    private static final String UPD_ORDER_STATUS = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set ostatus=? "
            + " where cstatus&1=0 and orderno=? and ostatus=?";

    //释放商品冻结库存
    private static final String UPD_GOODS_FSTORE = "update {{?" + DSMConst.TD_PROD_SKU + "}} set "
            + "freezestore=freezestore-? where cstatus&1=0 and sku=? ";

    //更新优惠券领取表
    private static final String UPD_COUENT_SQL = "update {{?" + DSMConst.TD_PROM_COUENT + "}} set "
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

    private static final String UPDATE_PROM_COURCD = "update {{?" + DSMConst.TD_PROM_COURCD
            + "}} set cstatus=cstatus|1 where cstatus&1=0 and coupno=? and compid=?";



    private static final String QUERY_ORDER_BASE =
            " SELECT " + QUERY_TRAN_ORDER_PARAMS
                    + " FROM " + FROM_BK_ORDER
                    + " WHERE ord.cstatus&1 = 0 ";

    @UserPermission(ignore = false)
    public Result placeOrderOne(AppContext appContext) {
        long coupon = 0;//优惠券码
        Result result = new Result();
        Gson gson = new Gson();
        List<TranOrderGoods> tranOrderGoods = new ArrayList<>();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int placeType = jsonObject.get("placeType").getAsInt();//1、直接下单 2、购物车下单
        JsonObject orderObj = jsonObject.get("orderObj").getAsJsonObject();
        JsonArray goodsArr = jsonObject.get("goodsArr").getAsJsonArray();
        TranOrder tranOrder = gson.fromJson(orderObj, TranOrder.class);
        if (tranOrder == null) return result.fail("订单信息有误");
        if (!jsonObject.get("coupon").isJsonNull()) {
            coupon = jsonObject.get("coupon").getAsLong();
        }
        int pdnum = 0;
        for (int i = 0; i < goodsArr.size(); i++) {
            TranOrderGoods goodsVO = gson.fromJson(goodsArr.get(i).toString(), TranOrderGoods.class);
            pdnum += goodsVO.getPnum();
            goodsVO.setCompid(tranOrder.getCusno());
            tranOrderGoods.add(goodsVO);
        }
        tranOrder.setPdnum(pdnum);

//        OrderEvent orderEvent = new OrderEvent();
//        orderEvent.setPlaceType(placeType);
//        orderEvent.setCoupon(coupon);
//        orderEvent.setTranOrder(tranOrder);
//        orderEvent.setAllTranOrderGoods(tranOrderGoods);
//        System.out.println("-------------- coming............");

//        ExecutorService es1 = Executors.newFixedThreadPool(1);
//        ExecutorService es2 = Executors.newFixedThreadPool(5);
//        int bufferSize = 1024*1024;//环形队列长度，必须是2的N次方
//        Disruptor<OrderEvent> disruptor = new Disruptor<>(new OrderEventFactory(orderEvent), bufferSize, es2,
//                ProducerType.SINGLE, new YieldingWaitStrategy());
//
//        disruptor.handleEventsWith(new DeStockEventConsumer())
//                .handleEventsWith(new DbOptConsumer());
//
//        disruptor.start();
//
//        OrderEventProducer ep = new OrderEventProducer().setDisruptor(disruptor, orderEvent);
//        CountDownLatch countDownLatch = ep.getLatch();
//        es1.submit(ep);
//        try {
//            countDownLatch.await();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        disruptor.shutdown();
//
//        LogUtil.getDefaultLogger().info("result = " + orderEvent.getResult());
        return new Result();
    }


    /**
     * @return com.onek.entitys.Result
     * @throws
     * @description 下单接口
     * @params [appContext]
     * @author 11842
     * @time 2019/4/16 16:47
     * @version 1.1.1
     **/
    @UserPermission(ignore = true)
    public Result placeOrder(AppContext appContext) {
//        List<TranOrderGoods> finalGoodsPrice = null;
        long unqid = 0, coupon = 0;//优惠券码
        Result result = new Result();
        Gson gson = new Gson();
        List<String> sqlList = new ArrayList<>();
        List<Object[]> params = new ArrayList<>();
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
        TranOrder tranOrder = JSON.parseObject(orderObj.toJSONString(), TranOrder.class);
        if (tranOrder == null) return result.fail("订单信息有误");
        String orderNo = GenIdUtil.getOrderId(tranOrder.getCusno());//订单号生成
        tranOrder.setOrderno(orderNo);
        if (!jsonObject.getString("unqid").isEmpty()) {
            unqid = jsonObject.getLong("unqid");
        }
        if (!jsonObject.getString("coupon").isEmpty()) {
            coupon = jsonObject.getLong("coupon");
        }
        int pdnum = 0;
        for (int i = 0; i < goodsArr.size(); i++) {
            TranOrderGoods goodsVO = gson.fromJson(goodsArr.get(i).toString(), TranOrderGoods.class);
            pdnum += goodsVO.getPnum();
            goodsVO.setCompid(tranOrder.getCusno());
            goodsVO.setActcode(String.valueOf(goodsVO.getActcode()));
            tranOrderGoods.add(goodsVO);
        }
        tranOrder.setPdnum(pdnum);
        List<GoodsStock> goodsStockList = new ArrayList<>();
        if (orderType == 0) {
            //库存判断
            boolean b = false;
            goodsStockList = stockIsEnough(tranOrderGoods, b);
            if (b) {
                //库存不足处理
                stockRecovery(goodsStockList);
                return result.fail("商品库存发生改变！");
            }
            //订单费用计算（费用分摊以及总费用计算）
            try {
                calculatePrice(tranOrderGoods, tranOrder, unqid);
            } catch (Exception e) {
                stockRecovery(goodsStockList);
                LogUtil.getDefaultLogger().info("计算活动价格异常！");
                return result.fail("下单失败");
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
                return result.fail("秒杀商品库存发生改变！");
            }
            //订单费用计算（费用分摊以及总费用计算）
            calculatePrice(tranOrderGoods, tranOrder, unqid);
        }
        //数据库相关操作
        sqlList.add(INSERT_TRAN_ORDER);
        params.add(new Object[]{orderNo, 0, tranOrder.getCusno(), tranOrder.getBusno(), 0, 0, tranOrder.getPdnum(),
                tranOrder.getPdamt(), tranOrder.getFreight(), tranOrder.getPayamt(), tranOrder.getCoupamt(), tranOrder.getDistamt(),
                tranOrder.getRvaddno(), 0, 0, tranOrder.getConsignee(), tranOrder.getContact(), tranOrder.getAddress()});

        if (unqid > 0) {
            //使用优惠券
            sqlList.add(UPD_COUENT_SQL);
            params.add(new Object[]{unqid});
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
            updateSku(tranOrderGoods);//若失败则需要处理（保证一致性）
            if (coupon > 0) {
                baseDao.updateNative(UPDATE_PROM_COURCD, coupon, tranOrder.getCusno());
            }

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

    private boolean updateSku(List<TranOrderGoods> tranOrderGoodsList) {
        List<Object[]> paramOne = new ArrayList<>();
        for (TranOrderGoods tranOrderGoods : tranOrderGoodsList) {
            paramOne.add(new Object[]{tranOrderGoods.getPnum(), tranOrderGoods.getPdno()});
        }
        return !ModelUtil.updateTransEmpty(baseDao.updateBatchNative(UPD_GOODS, paramOne, tranOrderGoodsList.size()));
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
                                long coupon) {
        List<TranOrderGoods> finalTranOrderGoods = new ArrayList<>();//最终的商品详情
        List<Product> tempProds = new ArrayList<>();
        tranOrderGoodsList.forEach(tranOrderGoods -> {
            Product product = new Product();
            product.setSku(tranOrderGoods.getPdno());
            product.autoSetCurrentPrice(tranOrderGoods.getPdprice(), tranOrderGoods.getPnum());
            tempProds.add(product);
        });
        DiscountResult discountResult = CalculateUtil.calculate(tranOrder.getCusno(), tempProds, coupon);
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
                TranOrderGoods tranOrderGoods = new TranOrderGoods();
                tranOrderGoods.setCompid(tranOrder.getCusno());
                tranOrderGoods.setPdno(iDiscount.getProductList().get(i).getSKU());
                tranOrderGoods.setPdprice(iDiscount.getProductList().get(i).getOriginalPrice());
                tranOrderGoods.setDistprice(iDiscount.getProductList().get(i).getDiscounted());
                System.out.println("");
                tranOrderGoods.setPayamt(iDiscount.getProductList().get(i).getCurrentPrice());
                tranOrderGoods.setPromtype((int) iDiscount.getBRule());
                finalTranOrderGoods.add(tranOrderGoods);
            }
        }
        for (TranOrderGoods goodsPrice : tranOrderGoodsList) {//传进来的
            for (TranOrderGoods finalGoods : finalTranOrderGoods) {
                if (goodsPrice.getPdno() == finalGoods.getPdno()) {
                    goodsPrice.setCompid(tranOrder.getCusno());
                    goodsPrice.setPdprice(finalGoods.getPdprice());
                    goodsPrice.setDistprice(finalGoods.getDistprice() * 100);
                    goodsPrice.setPayamt(finalGoods.getPayamt() * 100);
                    goodsPrice.setPromtype(finalGoods.getPromtype());
                }
            }
            goodsPrice.setPdprice(goodsPrice.getPdprice() * 100);
            if (goodsPrice.getPayamt() == 0) {
                goodsPrice.setPayamt(goodsPrice.getPdprice() * goodsPrice.getPnum());
            }
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
        for (TranOrderGoods tranOrderGoods : tranOrderGoodsList) {
            if (!RedisStockUtil.deductionActStock(tranOrderGoods.getPdno(), tranOrderGoods.getPnum(), actCode)) {
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
    private List<GoodsStock> stockIsEnough(List<TranOrderGoods> tranOrderGoodsList, boolean result) {
        List<GoodsStock> goodsStockList = new ArrayList<>();
        for (TranOrderGoods tranOrderGoods : tranOrderGoodsList) {
            String actCodeStr = tranOrderGoods.getActcode();
            List<Long> list = JSON.parseArray(actCodeStr).toJavaList(Long.class);
            for (Long aList : list) {
                if (aList > 0) {
                    if (!RedisStockUtil.deductionActStock(tranOrderGoods.getPdno(),
                            tranOrderGoods.getPnum(), aList)) {
                        result = true;
                    } else {
                        GoodsStock goodsStock = new GoodsStock();
                        goodsStock.setActCode(aList);
                        goodsStock.setSku(tranOrderGoods.getPdno());
                        goodsStock.setStock(tranOrderGoods.getPnum());
                        goodsStockList.add(goodsStock);
                    }
                } else {
                    if (RedisStockUtil.deductionStock(tranOrderGoods.getPdno(),
                            tranOrderGoods.getPnum()) != 2) {
                        result = true;
                    } else {
                        GoodsStock goodsStock = new GoodsStock();
                        goodsStock.setActCode(aList);
                        goodsStock.setSku(tranOrderGoods.getPdno());
                        goodsStock.setStock(tranOrderGoods.getPnum());
                        goodsStockList.add(goodsStock);
                    }
                }
            }
        }
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
            }else{
                RedisStockUtil.addStock(goodsStock.getSku(), goodsStock.getStock());
            }
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
                    tranOrderGoods.getPromtype(), tranOrderGoods.getPkgno(), tranOrderGoods.getPnum(), tranOrderGoods.getActcode()});
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
                    tranOrderGoods.getPdno()});
        }
    }


    /**
     * @return com.onek.entitys.Result
     * @throws
     * @description 取消订单
     * @params [appContext]
     * @author 11842
     * @time 2019/4/17 17:00
     * @version 1.1.1
     **/
    @UserPermission(ignore = true)
    public Result cancelOrder(AppContext appContext) {
        Result result = new Result();
//        List<String> sqlList = new ArrayList<>();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String orderNo = jsonObject.get("orderno").getAsString();//订单号
        int cusno = jsonObject.get("cusno").getAsInt(); //企业码
//        sqlList.add(UPD_ORDER_STATUS);
//        params.add(new Object[]{-4, orderNo});

//        String[] sqlNative = new String[sqlList.size()];
//        sqlNative = sqlList.toArray(sqlNative);
        boolean b = cancelOrder(orderNo, cusno);

        if (b) {
            CANCEL_DELAYED.removeByKey(orderNo);
        }

        return b ? result.success("取消成功") : result.fail("取消失败");
    }

    public boolean cancelOrder(String orderNo, int cusno) {
        List<Object[]> params = new ArrayList<>();
        int year = Integer.parseInt("20" + orderNo.substring(0, 2));
        int res = baseDao.updateNativeSharding(cusno, year, UPD_ORDER_STATUS, -4, orderNo, 0);
        if (res > 0) {
            TranOrderGoods[] tranOrderGoods = getGoodsArr(orderNo, cusno);
            for (TranOrderGoods tranOrderGood : tranOrderGoods) {
                String actCodeStr = tranOrderGood.getActcode();
                List<Long> list = JSON.parseArray(actCodeStr).toJavaList(Long.class);
                for (Long actcode: list) {
                    if (actcode > 0) {
                        RedisStockUtil.addActStock(tranOrderGood.getPdno(), actcode, tranOrderGood.getPnum());
                    } else {
                        RedisStockUtil.addStock(tranOrderGood.getPdno(), tranOrderGood.getPnum());//恢复redis库存
                    }
                }
                params.add(new Object[]{tranOrderGood.getPnum(), tranOrderGood.getPdno()});
            }
            baseDao.updateBatchNative(UPD_GOODS_FSTORE, params, tranOrderGoods.length);
        }
        return res > 0;
    }

    static TranOrderGoods[] getGoodsArr(String orderNo, int cusno) {
        String selectGoodsSql = "select pdno, pnum, payamt,actcode from {{?" + DSMConst.TD_TRAN_GOODS + "}} where cstatus&1=0 "
                + " and orderno=" + orderNo;
        int year = Integer.parseInt("20" + orderNo.substring(0, 2));
        List<Object[]> queryResult = baseDao.queryNativeSharding(cusno, year, selectGoodsSql);
        TranOrderGoods[] tranOrderGoods = new TranOrderGoods[queryResult.size()];
        baseDao.convToEntity(queryResult, tranOrderGoods, TranOrderGoods.class, new String[]{
                "pdno", "pnum", "payamt", "actcode"
        });
        return tranOrderGoods;
    }

    private int getCompid(String orderno) {
        if (!StringUtils.isBiggerZero(orderno)) {
            return 0;
        }

        List<Object[]> queryResult = baseDao.queryNativeSharding(
                        0, TimeUtils.getCurrentYear(),
                        " SELECT cusno "
                        + " FROM {{?" + DSMConst.TD_BK_TRAN_ORDER + "}} "
                        + " WHERE cstatus&1 = 0 AND orderno = ? ", orderno);

        if (queryResult.isEmpty()) {
            return 0;
        }

        return Integer.parseInt(queryResult.get(0)[0].toString());
    }

    public boolean delivery(String orderno, int compid) {
        boolean result = true;

        if (result) {
            PayModule.DELIVERY_DELAYED.removeByKey(orderno);
            TAKE_DELAYED.add(new DelayedBase(compid, orderno));
        }

        return result;
    }

    public boolean takeDelivery(String orderno, int compid) {
        boolean result = BaseDAO.getBaseDAO().updateNativeSharding(compid, TimeUtils.getCurrentYear(),
                UPDATE_TAKE_DELIVERY, orderno) > 0;

        if (result) {
            TAKE_DELAYED.removeByKey(orderno);
        }

        return result;
    }

    /**
     * 发货
     *
     * @param appContext
     * @return
     */
    @UserPermission(ignore = true)
    public Result delivery(AppContext appContext) {
        String[] params = appContext.param.arrays;

        if (ArrayUtil.isEmpty(params)) {
            return new Result().fail("参数为空");
        }

        String orderNo = params[1];
        int compid = getCompid(orderNo);

        if (compid <= 0 && !StringUtils.isBiggerZero(orderNo)) {
            return new Result().fail("非法参数");
        }

        int result  = BaseDAO.getBaseDAO().updateNativeSharding(compid, TimeUtils.getCurrentYear(),
                UPDATE_DELIVERY, orderNo);

        return result > 0 ? new Result().success("已发货") : new Result().fail("操作失败");
    }

    /**
     * 收货
     *
     * @param appContext
     * @return
     */

    @UserPermission(ignore = true)
    public Result takeDelivery(AppContext appContext) {
        String[] params = appContext.param.arrays;

        if (ArrayUtil.isEmpty(params)) {
            return new Result().fail("参数为空");
        }

        String orderNo = params[1];
        int compid = getCompid(orderNo);

        if (compid <= 0 && !StringUtils.isBiggerZero(orderNo)) {
            return new Result().fail("非法参数");
        }

        boolean result = takeDelivery(orderNo, compid);

        return result ? new Result().success("已签收") : new Result().fail("操作失败");
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