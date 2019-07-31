package com.onek.order;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.onek.calculate.ActivityCalculateService;
import com.onek.calculate.ActivityFilterService;
import com.onek.calculate.entity.IDiscount;
import com.onek.calculate.entity.Ladoff;
import com.onek.calculate.entity.Product;
import com.onek.calculate.filter.*;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.util.CalculateUtil;
import com.onek.util.IceRemoteUtil;
import com.onek.util.order.RedisOrderUtil;
import com.onek.util.stock.RedisStockUtil;
import util.StringUtils;
import util.TimeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class CalculateModule {
    /**
     * @接口摘要 获取商品参与活动
     * @业务场景 获取商品参与活动
     * @传参类型 arrays
     * @传参列表 [sku]
     * @返回列表 code=200 data=结果信息
     */
    public Result getActivitiesBySKU(AppContext appContext) {
        String[] arrays = appContext.param.arrays;

        if (arrays == null || arrays.length == 0) {
            return new Result().fail("参数为空");
        }

        if (!StringUtils.isBiggerZero(arrays[0])) {
            return new Result().fail("参数错误");
        }

        long sku = Long.parseLong(arrays[0]);
        int compid = appContext.getUserSession().compId;

        List<Product> products = new ArrayList<>();
        Product p = new Product();
        p.setSku(sku);
        products.add(p);

        List<IDiscount> discounts
                = CalculateUtil.getDiscount(appContext.getUserSession().compId, products);

        Collections.sort(discounts, (o1, o2) -> o2.getPriority() - o1.getPriority());

        int minStock = Integer.MAX_VALUE;
        int minLimit = Integer.MAX_VALUE;
        int maxBuyed = 0;
        boolean isExCoupon = false;

        for (IDiscount discount : discounts) {
            minStock = Math.min(
                    RedisStockUtil
                        .getActStockBySkuAndActno(sku, discount.getDiscountNo()),
                    minStock);

            if (discount.getLimits(sku) > 0) {
                minLimit = Math.min(discount.getLimits(sku), minLimit);
            }

            maxBuyed = Math.max(RedisOrderUtil.getActBuyNum(compid, sku, discount.getDiscountNo()), maxBuyed);

            isExCoupon = isExCoupon || discount.getExCoupon();
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("discounts", discounts);
        jsonObject.put("minStock", minStock);
        jsonObject.put("minLimit", minLimit == Integer.MAX_VALUE ? 0 : minLimit);
        jsonObject.put("maxBuyed", maxBuyed);
        jsonObject.put("isExCoupon", isExCoupon);
        jsonObject.put("minimum", Math.max(0, Math.min(minLimit - maxBuyed, minStock)));

        return new Result().success(jsonObject);
    }


    /**
     * @接口摘要 获取活动商品具体详情
     * @业务场景 获取活动商品具体详情
     * @传参类型 arrays
     * @传参列表 [sku, 活动码]
     * @返回列表 code=200 data=结果信息
     */
    public Result getGoodsActInfo(AppContext appContext) {
        String[] arrays = appContext.param.arrays;

        if (arrays == null || arrays.length == 0) {
            return new Result().fail("参数为空");
        }

        if (!StringUtils.isBiggerZero(arrays[0])
                || !StringUtils.isBiggerZero(arrays[1])) {
            return new Result().fail("参数错误");
        }

        long sku = Long.parseLong(arrays[0]);
        long actcode = Long.parseLong(arrays[1]);
        int compid = appContext.getUserSession().compId;

        List<Product> products = new ArrayList<>();
        Product p = new Product();
        p.setSku(sku);
        p.autoSetCurrentPrice(0, 1);
        products.add(p);

        List<IDiscount> discounts
                = new ActivityFilterService(
                        new ActivitiesFilter[] {
                                new CycleFilter(),
                                new QualFilter(compid),
                                new PriorityFilter(),
                                new StoreFilter(), })
                .getCurrentActivities(products);

        IDiscount currDiscount = null;

        JSONObject jsonObject = new JSONObject();

        for (IDiscount discount : discounts) {
            if (discount.getDiscountNo() == actcode) {
                currDiscount = discount;
                break;
            }
        }

        if (currDiscount == null) {
            return new Result().success(jsonObject);
        }

        jsonObject.put("currentDate", TimeUtils.date_yMd_Hms_2String(new Date()));
        jsonObject.put("startTime", currDiscount.getStartTime());
        jsonObject.put("endTime", currDiscount.getEndTime());
        jsonObject.put("limits", currDiscount.getLimits(sku));

        if (currDiscount.getBRule() == 1133) {
            Ladoff[] ladoffs =
                new ActivityCalculateService().getLadoffs(actcode);

            if (ladoffs.length == 0) {
                return new Result().success(jsonObject);
            }

            jsonObject.put("currBuy",
                    RedisOrderUtil.getActBuyNum(compid, sku, actcode));

            jsonObject.put("currNums", IceRemoteUtil.getGroupCount(actcode));
            // 团购
            jsonObject.put("ladoffs", ladoffs);
            jsonObject.put("actRealStore", RedisStockUtil.getActStockBySkuAndActno(sku, actcode));
        } else if (currDiscount.getBRule() == 1113) {
            jsonObject.put("currBuy",
                    RedisOrderUtil.getActBuyNum(compid, sku, actcode));
            jsonObject.put("killPrice", currDiscount.getActionPrice(sku));
            jsonObject.put("actRealStore", RedisStockUtil.getActStockBySkuAndActno(sku, actcode));
        }

        return new Result().success(jsonObject);
    }

    /**
     * @接口摘要 获取商品参与活动阶梯
     * @业务场景 获取商品参与活动阶梯
     * @传参类型 arrays
     * @传参列表 [sku]
     * @返回列表 code=200 data=结果信息
     */
    public Result getLadoff(AppContext appContext) {
        int compid = appContext.getUserSession().compId;

        if (compid <= 0) {
            return new Result().success(null);
        }

        long sku = Long.parseLong(appContext.param.arrays[0]);

        List<Product> products = new ArrayList<>();
        Product p = new Product();
        products.add(p);
        p.setSku(sku);

        return new Result().success(JSON.toJSON(CalculateUtil.getLadoff(compid, products)));
    }
}
