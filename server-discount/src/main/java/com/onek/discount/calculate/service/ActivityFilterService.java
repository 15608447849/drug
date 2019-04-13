package com.onek.discount.calculate.service;

import com.onek.discount.calculate.entity.Activity;
import com.onek.discount.calculate.entity.IDiscount;
import com.onek.discount.calculate.filter.ActivitiesFilter;
import constant.DSMConst;
import dao.BaseDAO;
import util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ActivityFilterService extends BaseDiscountFilterService {
    private static final String GET_ACTIVITIES_BY_SKU =
            " SELECT act.*, ass.limitnum, time.sdate, time.edate "
                    + " FROM ({{?" + DSMConst.TD_PROM_ASSDRUG + "}} ass "
                    + " INNER JOIN {{?" + DSMConst.TD_PROM_ACT + "}} act"
                    + " ON ass.cstatus&1 = 0 "
                    + " AND act.cstatus&1 = 0 "
                    + " AND ass.actcode = act.unqid "
                    + " AND act.sdate <= CURRENT_DATE "
                    + " AND CURRENT_DATE <= act.edate"
                    // 全局通用， 品类，商品
                    + " AND ass.gcode IN (0, ?, ?) ) "
                    + " INNER JOIN {{?" + DSMConst.TD_PROM_TIME + "}} time "
                    + " ON time.cstatus&1 = 0 "
                    + " AND time.actcode = act.unqid "
                    + " AND time.sdate <= CURRENT_TIME "
                    + " AND CURRENT_TIME <= time.edate "
                    + " WHERE 1=1 ";

    public ActivityFilterService(ActivitiesFilter[] discountFilters) {
        super(discountFilters);
    }

    public List<IDiscount> getCurrentDiscounts(long sku) {
        String pclass = getProductCode(sku);

        if (StringUtils.isEmpty(pclass)) {
            return new ArrayList<>();
        }

        List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNative(GET_ACTIVITIES_BY_SKU,
                sku, pclass);

        Activity[] activities = new Activity[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, activities, Activity.class);

        List<IDiscount> returnResult =  new ArrayList<>(Arrays.asList(activities));

        // 不参与活动的商品不加入。
        doFilter(returnResult);

        return returnResult;
    }

}
