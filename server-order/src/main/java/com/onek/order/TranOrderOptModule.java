package com.onek.order;

import com.alibaba.fastjson.JSONArray;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.calculate.ActivityCalculateService;
import com.onek.calculate.ActivityFilterService;
import com.onek.calculate.entity.DiscountResult;
import com.onek.calculate.entity.IDiscount;
import com.onek.calculate.entity.Product;
import com.onek.calculate.filter.*;
import com.onek.context.AppContext;
import com.onek.entity.TranOrder;
import com.onek.entity.TranOrderGoods;
import com.onek.entitys.Result;
import com.onek.util.stock.RedisStockUtil;
import constant.DSMConst;
import dao.BaseDAO;
import global.GenIdUtil;
import util.GsonUtils;
import util.ModelUtil;
import util.StringUtils;

import java.util.*;

/**
 * @author 11842
 * @version 1.1.1
 * @description 订单基本操作（下单等等）
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
            + "coupamt,promtype,pkgno,asstatus,createdate,createtime,cstatus) "
            + " values(?,?,?,?,?,"
            + "?,?,?,?,?,"
            + "?,CURRENT_DATE,CURRENT_TIME,0)";

    private static final String UPD_TRAN_GOODS = "update {{?" + DSMConst.TD_TRAN_GOODS + "}} set "
            + "orderno=?, pdprice=?, distprice=?,payamt=?,coupamt=?,promtype=?,"
            + "pkgno=?,createdate=CURRENT_DATE,createtime=CURRENT_TIME where cstatus&1=0 and "
            + " pdno=?";

    //是否要减商品总库存
    private static final String UPD_GOODS = "update {{?" + DSMConst.TD_PROD_SKU + "}} set "
            + "freezestore=? where cstatus&1=0 and sku=? ";



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
        JsonObject orderObj = jsonObject.get("orderObj").getAsJsonObject();
        JsonArray goodsArr = jsonObject.get("goodsArr").getAsJsonArray();
        TranOrder tranOrder = gson.fromJson(orderObj, TranOrder.class);
        if (tranOrder == null) return result.fail("订单信息有误");
        String orderNo = GenIdUtil.getOrderId(tranOrder.getCusno());//订单号生成
        for (int i = 0; i < goodsArr.size(); i++) {
            TranOrderGoods goodsVO = gson.fromJson(goodsArr.get(i).toString(), TranOrderGoods.class);
            tranOrderGoods.add(goodsVO);
        }
        //库存判断
        if (stockIsEnough(tranOrderGoods).size() != tranOrderGoods.size()) {
            //库存不足处理

            return result.fail("商品库存发生改变！");
        }
        //订单费用计算（费用分摊以及总费用计算）
        calculatePrice(tranOrderGoods, tranOrder.getCusno());
        //优惠券

        //数据库相关操作
        sqlList.add(INSERT_TRAN_ORDER);
        params.add(new Object[]{orderNo, 0, tranOrder.getCusno(), tranOrder.getBusno(), 0, 0,tranOrder.getPdnum(),
                tranOrder.getPdamt(),tranOrder.getFreight(),tranOrder.getPayamt(),tranOrder.getCoupamt(),
                tranOrder.getRvaddno(),0,0});
        if (placeType == 1) {
            getInsertSqlList(sqlList, params, tranOrderGoods, orderNo);
        } else {
            getUpdSqlList(sqlList, params, tranOrderGoods, orderNo);
        }
        String[] sqlNative = new String[sqlList.size()];
        sqlNative = sqlList.toArray(sqlNative);
        int year = Integer.parseInt("20" + orderNo.substring(0,2));
        boolean b = !ModelUtil.updateTransEmpty(baseDao.updateTransNativeSharding(tranOrder.getCusno(),year, sqlNative, params));
        if (b){
            return result.success("下单成功");
        } else {//下单失败
            //库存处理

            return result.fail("下单失败");
        }
    }


    private void calculatePrice(List<TranOrderGoods> tranOrderGoodsList, int compId) {
        List<Product> tempProds = new ArrayList<>();
        tranOrderGoodsList.forEach(tranOrderGoods -> {
            Product product = new Product();
            product.setSku(tranOrderGoods.getPdno());
            product.autoSetCurrentPrice(tranOrderGoods.getPdprice(), tranOrderGoods.getPnum());
            tempProds.add(product);
        });
        DiscountResult discountResult = getProdsPrice(tempProds, compId);
    }

    /**
     * @description
     * @params [tempProds, compId]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/17 13:50
     * @version 1.1.1
     **/
    public DiscountResult getProdsPrice( List<Product> tempProds, int compId) {

        List<IDiscount> activityList;
        activityList = new ActivityFilterService(
                new ActivitiesFilter[] {
                    new TypeFilter(),
                    new CycleFilter(),
                    new QualFilter(compId),
                    new PriorityFilter(),
                }).getCurrentActivities(tempProds);

        TreeMap<Integer, List<IDiscount>> pri_act =
                new TreeMap<>((o1, o2) -> o2 - o1);

        int priority;
        List<IDiscount> tempDiscount;
        for (IDiscount discount : activityList) {
            priority = discount.getPriority();

            tempDiscount = pri_act.get(priority);

            if (tempDiscount == null) {
                tempDiscount = new ArrayList<>();
                pri_act.put(priority, tempDiscount);
            }

            tempDiscount.add(discount);
        }


        for (List<IDiscount> discounts : pri_act.values()) {
            new ActivityCalculateService().calculate(discounts);
        }

        return new DiscountResult(activityList);
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
    private Map<Long, Integer> stockIsEnough(List<TranOrderGoods> tranOrderGoodsList) {
        Map<Long, Integer> skuMap = new HashMap<>();
        for (TranOrderGoods tranOrderGoods : tranOrderGoodsList) {
            if (!RedisStockUtil.deductionStock(tranOrderGoods.getPdno(), tranOrderGoods.getPnum())) {
                return skuMap;
            }
            skuMap.put(tranOrderGoods.getPdno(), tranOrderGoods.getPnum());
        }
        return skuMap;
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
    private void stockRecovery(Map<Long, Integer> skuMap) {
        Set<Map.Entry<Long, Integer>> set = skuMap.entrySet();
        for (int i = 0; i < set.size(); i++) {
            
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
        tranOrderGoodsList.forEach(tranOrderGoods -> {
            sqlList.add(INSERT_TRAN_GOODS);
//            unqid,orderno,compid,pdno,pdprice,distprice,payamt,
//                    coupamt,promtype,pkgno,asstatus,createdate,createtime,cstatus
            params.add(new Object[]{GenIdUtil.getUnqId(), orderNo, tranOrderGoods.getCompid(),tranOrderGoods.getPdno(),
                    tranOrderGoods.getPdprice(),tranOrderGoods.getDistprice(), tranOrderGoods.getPayamt(),tranOrderGoods.getCoupamt(),
                    tranOrderGoods.getPromtype(),tranOrderGoods.getPkgno(),tranOrderGoods.getAsstatus()});
            sqlList.add(UPD_GOODS);
            params.add(new Object[]{tranOrderGoods.getPnum(),tranOrderGoods.getPdno()});
        });
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
        tranOrderGoodsList.forEach(tranOrderGoods -> {
            sqlList.add(UPD_TRAN_GOODS);
//            "orderno=?, pdprice=?, distprice=?,payamt=?,coupamt=?,promtype=?,"
//                    + "pkgno=?,createdate=CURRENT_DATE,createtime=CURRENT_TIME where cstatus&1=0 and "
//                    + " pdno=?";
            params.add(new Object[]{orderNo, tranOrderGoods.getPdprice(),tranOrderGoods.getDistprice(),tranOrderGoods.getPayamt(),
                    tranOrderGoods.getCoupamt(),tranOrderGoods.getPromtype(),tranOrderGoods.getPkgno(),tranOrderGoods.getPdno()});
            sqlList.add(UPD_GOODS);
            params.add(new Object[]{tranOrderGoods.getPnum(),tranOrderGoods.getPdno()});
        });
    }
}
