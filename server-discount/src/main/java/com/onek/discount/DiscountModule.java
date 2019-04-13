package com.onek.discount;

import com.alibaba.fastjson.JSONObject;
import com.onek.context.AppContext;
import com.onek.discount.calculate.entity.IDiscount;
import com.onek.discount.calculate.entity.Ladoff;
import com.onek.discount.calculate.filter.ActivitiesFilter;
import com.onek.discount.calculate.filter.CycleFilter;
import com.onek.discount.calculate.service.ActivityCalculateService;
import com.onek.discount.calculate.service.ActivityFilterService;
import com.onek.entitys.Result;
import org.apache.http.client.utils.DateUtils;
import util.StringUtils;

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

        List<IDiscount> discounts
                = new ActivityFilterService(new ActivitiesFilter[] { new CycleFilter() }).getCurrentDiscounts(sku);

        IDiscount currDiscount = null;

        for (IDiscount discount : discounts) {
            if (discount.getActNo() == actcode) {
                currDiscount = discount;
                break;
            }
        }

        if (currDiscount == null) {
            return new Result().success(null);
        }

        Ladoff[] ladoffs =
                new ActivityCalculateService().getLadoffs(currDiscount.getBRule());

        if (ladoffs.length == 0) {
            return new Result().success(null);
        }

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("currentDate", DateUtils.formatDate(
                new Date(), "yyyy-dd-MM HH:mm:ss"));
        jsonObject.put("startTime", currDiscount.getStartTime());
        jsonObject.put("endTime", currDiscount.getEndTime());
        if (currDiscount.getBRule() == 1113) {
            // 秒杀
            jsonObject.put("killPrice", ladoffs[0].getOfferValue());
        } else if (currDiscount.getBRule() == 1133) {
            // 团购
            jsonObject.put("ladoffs", ladoffs);
        }

        return new Result().success(jsonObject);
    }


}
