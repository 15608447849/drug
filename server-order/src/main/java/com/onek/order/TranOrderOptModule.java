package com.onek.order;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.calculate.entity.DiscountResult;
import com.onek.calculate.entity.IDiscount;
import com.onek.calculate.entity.Product;
import com.onek.context.AppContext;
import com.onek.entity.TranOrder;
import com.onek.entity.TranOrderGoods;
import com.onek.entitys.Result;
import com.onek.util.CalculateUtil;
import com.onek.util.stock.RedisStockUtil;
import constant.DSMConst;
import dao.BaseDAO;
import global.GenIdUtil;
import org.hyrdpf.util.LogUtil;
import util.ModelUtil;

import java.util.*;

/**
 * @author 11842
 * @version 1.1.1
 * @description 订单下单
 * @time 2019/4/16 16:01
 **/
public class TranOrderOptModule {

    private static BaseDAO baseDao = BaseDAO.getBaseDAO();
    //订单表新增
    private static final String INSERT_TRAN_ORDER = "insert into {{?" + DSMConst.TD_TRAN_ORDER + "}} "
            + "(orderno,tradeno,cusno,busno,ostatus,asstatus,pdnum," +
            "pdamt,freight,payamt,coupamt,distamt,rvaddno," +
            "settstatus,otype,odate,otime,cstatus) "
            + " values(?,?,?,?,?,"
            + "?,?,?,?,?,"
            + "?,?,?,?,?,"
            + "CURRENT_DATE,CURRENT_TIME,0)";

    //订单商品表新增
    private static final String INSERT_TRAN_GOODS = "insert into {{?" + DSMConst.TD_TRAN_GOODS + "}} "
            + "(unqid,orderno,compid,pdno,pdprice,distprice,payamt,"
            + "coupamt,promtype,pkgno,pnum,asstatus,createdate,createtime,cstatus) "
            + " values(?,?,?,?,?,"
            + "?,?,?,?,?,"
            + "?,0,CURRENT_DATE,CURRENT_TIME,0)";

    private static final String UPD_TRAN_GOODS = "update {{?" + DSMConst.TD_TRAN_GOODS + "}} set "
            + "orderno=?, pdprice=?, distprice=?,payamt=?,coupamt=?,promtype=?,"
            + "pkgno=?,createdate=CURRENT_DATE,createtime=CURRENT_TIME where cstatus&1=0 and "
            + " pdno=? and orderno=0";

    //是否要减商品总库存
    private static final String UPD_GOODS = "update {{?" + DSMConst.TD_PROD_SKU + "}} set "
            + "freezestore=freezestore+? where cstatus&1=0 and sku=? ";

    //更新订单状态
    private static final String UPD_ORDER_STATUS = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set ostatus=? "
            + " where cstatus&1=0 and orderno=?";

    //释放商品冻结库存
    private static final String UPD_GOODS_FSTORE = "update {{?" + DSMConst.TD_PROD_SKU + "}} set "
            + "freezestore=freezestore-? where cstatus&1=0 and sku=? ";




    /**
     * @description 下单接口
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/16 16:47
     * @version 1.1.1
     **/
    @UserPermission(ignore = true)
    public Result placeOrder(AppContext appContext) {
        Result result = new Result();
        Gson gson = new Gson();
        List<String> sqlList = new ArrayList<>();
        List<Object[]> params = new ArrayList<>();
        List<TranOrderGoods> tranOrderGoods = new ArrayList<>();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int placeType = jsonObject.get("placeType").getAsInt();//1、直接下单 2、购物车下单
        long coupon = 0;//优惠券码
        if (!jsonObject.get("coupon").isJsonNull()) {
            coupon = jsonObject.get("coupon").getAsLong();
        }
        JsonObject orderObj = jsonObject.get("orderObj").getAsJsonObject();
        JsonArray goodsArr = jsonObject.get("goodsArr").getAsJsonArray();
        TranOrder tranOrder = gson.fromJson(orderObj, TranOrder.class);
        if (tranOrder == null) return result.fail("订单信息有误");
        String orderNo = GenIdUtil.getOrderId(tranOrder.getCusno());//订单号生成
        int pdnum = 0;
        for (int i = 0; i < goodsArr.size(); i++) {
            TranOrderGoods goodsVO = gson.fromJson(goodsArr.get(i).toString(), TranOrderGoods.class);
            pdnum += goodsVO.getPnum();
            goodsVO.setCompid(tranOrder.getCusno());
            tranOrderGoods.add(goodsVO);
        }
        tranOrder.setPdnum(pdnum);
        //库存判断
        List<TranOrderGoods> goodsList = stockIsEnough(tranOrderGoods);
        if (goodsList.size() != tranOrderGoods.size()) {
            //库存不足处理
            stockRecovery(goodsList);
            return result.fail("商品库存发生改变！");
        }
        //订单费用计算（费用分摊以及总费用计算）
        List<TranOrderGoods> finalGoodsPrice = null;
        try {
            finalGoodsPrice = calculatePrice(tranOrderGoods, tranOrder, coupon);
        } catch (Exception e) {
            stockRecovery(goodsList);
            LogUtil.getDefaultLogger().info("计算活动价格异常！");
            return result.fail("下单失败");
        }
        //数据库相关操作
        sqlList.add(INSERT_TRAN_ORDER);
        params.add(new Object[]{orderNo, 0, tranOrder.getCusno(), tranOrder.getBusno(), 0, 0,tranOrder.getPdnum(),
                tranOrder.getPdamt(),tranOrder.getFreight(),tranOrder.getPayamt(),tranOrder.getCoupamt(),tranOrder.getDistamt(),
                tranOrder.getRvaddno(),0,0});

        if (placeType == 1) {
            getInsertSqlList(sqlList, params, finalGoodsPrice, orderNo);
        } else {
            getUpdSqlList(sqlList, params, finalGoodsPrice, orderNo);
        }

        String[] sqlNative = new String[sqlList.size()];
        sqlNative = sqlList.toArray(sqlNative);
        int year = Integer.parseInt("20" + orderNo.substring(0,2));
        boolean b = !ModelUtil.updateTransEmpty(baseDao.updateTransNativeSharding(tranOrder.getCusno(),year, sqlNative, params));
        if (b){
            updateSku(finalGoodsPrice);//若失败则需要处理（保证一致性）
            return result.success("下单成功");
        } else {//下单失败
            //库存处理
            stockRecovery(tranOrderGoods);
            return result.fail("下单失败");
        }
    }

    private boolean updateSku(List<TranOrderGoods> tranOrderGoodsList) {
        List<Object[]> paramOne = new ArrayList<>();
        for (TranOrderGoods tranOrderGoods : tranOrderGoodsList){
            paramOne.add(new Object[]{tranOrderGoods.getPnum(),tranOrderGoods.getPdno()});
        }
        return !ModelUtil.updateTransEmpty(baseDao.updateBatchNative(UPD_GOODS,paramOne, tranOrderGoodsList.size()));
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
    private List<TranOrderGoods> calculatePrice(List<TranOrderGoods> tranOrderGoodsList, TranOrder tranOrder,
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
        tranOrder.setCoupamt((discountResult.getCouponValue()*100));//订单使用优惠券(李康亮记得填)
        if (discountResult.isFreeShipping()) {//免邮
            tranOrder.setFreight(0);
        } else {
            tranOrder.setFreight(0);//运费(暂无)
        }
        tranOrder.setPayamt((discountResult.getTotalCurrentPrice()*100));
        tranOrder.setDistamt((discountResult.getTotalDiscount()*100));

        List<IDiscount> iDiscountList = discountResult.getActivityList();
        for (IDiscount iDiscount : iDiscountList) {
            for (int i = 0; i < iDiscount.getProductList().size(); i++) {
                TranOrderGoods tranOrderGoods = new TranOrderGoods();
                tranOrderGoods.setCompid(tranOrder.getCusno());
                tranOrderGoods.setPdno(iDiscount.getProductList().get(i).getSKU());
                tranOrderGoods.setPdprice(iDiscount.getProductList().get(i).getOriginalPrice());
                tranOrderGoods.setPayamt(iDiscount.getProductList().get(i).getCurrentPrice());
                tranOrderGoods.setPromtype((int)iDiscount.getBRule());
                finalTranOrderGoods.add(tranOrderGoods);
            }
        }
        for (TranOrderGoods goodsPrice :tranOrderGoodsList) {//传进来的
            for (TranOrderGoods finalGoods : finalTranOrderGoods) {
                if (goodsPrice.getPdno() == finalGoods.getPdno()){
                    goodsPrice.setCompid(tranOrder.getCusno());
                    goodsPrice.setPdprice(finalGoods.getPdprice());
                    goodsPrice.setPayamt(finalGoods.getPayamt());
                    goodsPrice.setPromtype(finalGoods.getPromtype());
                }
            }
            goodsPrice.setPdprice(goodsPrice.getPdprice() * 100);
            goodsPrice.setPayamt(goodsPrice.getPayamt() * 100);
        }
        return tranOrderGoodsList;
    }

    /**
     * @description 判断库存是否足够
     * @params []
     * @return boolean
     * @exception
     * @author 11842
     * @time  2019/4/17 11:49
     * @version 1.1.1
     **/
    private List<TranOrderGoods> stockIsEnough(List<TranOrderGoods> tranOrderGoodsList) {
        List<TranOrderGoods> goodsList = new ArrayList<>();
        for (TranOrderGoods tranOrderGoods : tranOrderGoodsList) {
            if (!RedisStockUtil.deductionStock(tranOrderGoods.getPdno(), tranOrderGoods.getPnum())) {
                return goodsList;
            }
            goodsList.add(tranOrderGoods);
        }
        return goodsList;
    }

    /**
     * @description 库存恢复
     * @params [tranOrderGoodsList]
     * @return boolean
     * @exception
     * @author 11842
     * @time  2019/4/17 14:38
     * @version 1.1.1
     **/
    private void stockRecovery(List<TranOrderGoods> goodsList) {
        for (TranOrderGoods aGoodsList : goodsList) {
            RedisStockUtil.addStock(aGoodsList.getPdno(), aGoodsList.getPnum());
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
            String orderNo){
        for (TranOrderGoods tranOrderGoods : tranOrderGoodsList){
            sqlList.add(INSERT_TRAN_GOODS);
//            unqid,orderno,compid,pdno,pdprice,distprice,payamt,
//                    coupamt,promtype,pkgno,asstatus,createdate,createtime,cstatus
            params.add(new Object[]{GenIdUtil.getUnqId(), orderNo, tranOrderGoods.getCompid(),tranOrderGoods.getPdno(),
                    tranOrderGoods.getPdprice(),tranOrderGoods.getDistprice(), tranOrderGoods.getPayamt(),tranOrderGoods.getCoupamt(),
                    tranOrderGoods.getPromtype(),tranOrderGoods.getPkgno(), tranOrderGoods.getPnum()});
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
                                  String orderNo){
        for (TranOrderGoods tranOrderGoods : tranOrderGoodsList){
            sqlList.add(UPD_TRAN_GOODS);
//            + "orderno=?, pdprice=?, distprice=?,payamt=?,coupamt=?,promtype=?,"
//                    + "pkgno=?,createdate=CURRENT_DATE,createtime=CURRENT_TIME where cstatus&1=0 and "
//                    + " pdno=? and orderno=0";
            params.add(new Object[]{orderNo, tranOrderGoods.getPdprice(),tranOrderGoods.getDistprice(),tranOrderGoods.getPayamt(),
                    tranOrderGoods.getCoupamt(),tranOrderGoods.getPromtype(),tranOrderGoods.getPkgno(),tranOrderGoods.getPdno()});
        }
    }


    /**
     * @description 取消订单
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/17 17:00
     * @version 1.1.1
     **/
    @UserPermission(ignore = true)
    public Result cancelOrder(AppContext appContext) {
        Result result = new Result();
        Gson gson = new Gson();
        List<String> sqlList = new ArrayList<>();
        List<Object[]> params = new ArrayList<>();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String orderNo = jsonObject.get("orderno").getAsString();//订单号
        int cusno = jsonObject.get("cusno").getAsInt(); //企业码
        sqlList.add(UPD_ORDER_STATUS);
        params.add(new Object[]{-4, orderNo});
        TranOrderGoods[] tranOrderGoods = getGoodsArr(orderNo, cusno);

        for (int i = 0; i < tranOrderGoods.length; i++) {
            RedisStockUtil.addStock(tranOrderGoods[i].getPdno(),tranOrderGoods[i].getPnum());//恢复redis库存
            sqlList.add(UPD_GOODS_FSTORE);
            params.add(new Object[]{tranOrderGoods[i].getPnum(), tranOrderGoods[i].getPdno()});
        }
        String[] sqlNative = new String[sqlList.size()];
        sqlNative = sqlList.toArray(sqlNative);
        int year = Integer.parseInt("20" + orderNo.substring(0,2));
        boolean b = !ModelUtil.updateTransEmpty(baseDao.updateTransNativeSharding(cusno,year, sqlNative, params));

        return b ? result.success("取消成功") : result.fail("取消失败");
    }

    public static TranOrderGoods[] getGoodsArr(String orderNo, int cusno){
        String selectGoodsSql = "select pdno, pnum from {{?" + DSMConst.TD_TRAN_GOODS + "}} where cstatus&1=0 "
                + " and orderno=" + orderNo;
        int year = Integer.parseInt("20" + orderNo.substring(0,2));
        List<Object[]> queryResult = baseDao.queryNativeSharding(cusno, year, selectGoodsSql);
        TranOrderGoods[] tranOrderGoods = new TranOrderGoods[queryResult.size()];
        baseDao.convToEntity(queryResult, tranOrderGoods, TranOrderGoods.class, new String[]{
                "pdno", "pnum"
        });
        return tranOrderGoods;
    }

//    public static void main(String[] args) {
//        Gson gson = new Gson();
//        TranOrderGoods goodsVO = gson.fromJson("{\"pdno\":\"11000000140102\",\"pnum\":\"1\"}", TranOrderGoods.class);
//        System.out.println("------------" + goodsVO.getPdno());
//    }
}
