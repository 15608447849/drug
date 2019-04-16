package com.onek.discount;

import com.alibaba.fastjson.JSONObject;
import com.onek.context.AppContext;
import com.onek.discount.calculate.entity.IDiscount;
import com.onek.discount.calculate.entity.Ladoff;
import com.onek.discount.calculate.entity.Product;
import com.onek.discount.calculate.filter.ActivitiesFilter;
import com.onek.discount.calculate.filter.CycleFilter;
import com.onek.discount.calculate.service.calculate.ActivityCalculateService;
import com.onek.discount.calculate.service.filter.ActivityFilterService;
import com.onek.entitys.Result;
import util.StringUtils;
import util.TimeUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DiscountModule {
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

        List<Product> products = new ArrayList<>();
        Product p = new Product();
        p.setSku(sku);

        List<IDiscount> discounts
                = new ActivityFilterService(
                        new ActivitiesFilter[] { new CycleFilter() })
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

        Ladoff[] ladoffs =
                new ActivityCalculateService().getLadoffs(currDiscount.getBRule());

        if (ladoffs.length == 0) {
            return new Result().success(jsonObject);
        }

        jsonObject.put("currentDate", TimeUtils.date_yMd_Hms_2String(new Date()));
        jsonObject.put("startTime", currDiscount.getStartTime());
        jsonObject.put("endTime", currDiscount.getEndTime());
        jsonObject.put("limits", currDiscount.getLimits(sku));

        if (currDiscount.getBRule() == 1133) {
            // 团购
            jsonObject.put("ladoffs", ladoffs);
        }

        return new Result().success(jsonObject);
    }

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

        List<IDiscount> discounts
                = new ActivityFilterService(
                        new ActivitiesFilter[] { new CycleFilter() })
                 .getCurrentActivities(products);

        return new Result().success(discounts);
    }

}
