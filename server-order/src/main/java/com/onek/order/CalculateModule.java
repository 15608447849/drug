package com.onek.order;

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
import com.onek.util.stock.RedisStockUtil;
import util.StringUtils;
import util.TimeUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CalculateModule {
    public Result getActivitiesBySKU(AppContext appContext) {
        String[] arrays = appContext.param.arrays;

        if (arrays == null || arrays.length == 0) {
            return new Result().fail("参数为空");
        }

        if (!StringUtils.isBiggerZero(arrays[0])) {
            return new Result().fail("参数错误");
        }

        long sku = Long.parseLong(arrays[0]);

        List<Product> products = new ArrayList<>();
        Product p = new Product();
        p.setSku(sku);
        products.add(p);

        List<IDiscount> discounts
                = CalculateUtil.getDiscount(appContext.getUserSession().compId, products);

        return new Result().success(discounts);
    }

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
        products.add(p);

        List<IDiscount> discounts
                = new ActivityFilterService(
                        new ActivitiesFilter[] {
                                new CycleFilter(),
                                new QualFilter(compid),
                                new PriorityFilter(), })
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
                new ActivityCalculateService().getLadoffs(currDiscount.getBRule());

            if (ladoffs.length == 0) {
                return new Result().success(jsonObject);
            }

            // 团购
            jsonObject.put("ladoffs", ladoffs);
        } else if (currDiscount.getBRule() == 1113) {
            jsonObject.put("killPrice", currDiscount.getActionPrice(sku));
            jsonObject.put("actRealStore", RedisStockUtil.getActStockBySkuAndActno(sku, actcode));
        }

        return new Result().success(jsonObject);
    }
}
