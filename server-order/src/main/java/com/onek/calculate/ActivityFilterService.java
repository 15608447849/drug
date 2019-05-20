package com.onek.calculate;

import com.onek.calculate.entity.IProduct;
import com.onek.calculate.filter.ActivitiesFilter;
import com.onek.calculate.entity.Activity;
import com.onek.calculate.entity.IDiscount;
import com.onek.calculate.service.filter.BaseDiscountFilterService;
import com.onek.util.IceRemoteUtil;
import constant.DSMConst;
import dao.BaseDAO;
import util.MathUtil;
import util.StringUtils;

import java.math.BigDecimal;
import java.util.*;

public class ActivityFilterService extends BaseDiscountFilterService {
    private static final String GET_ACTIVITIES_BY_SKU =
            " SELECT act.*, ass.limitnum, time.sdate, time.edate, ass.price "
                    + " FROM ({{?" + DSMConst.TD_PROM_ASSDRUG + "}} ass "
                    + " INNER JOIN {{?" + DSMConst.TD_PROM_ACT + "}} act"
                    + " ON ass.cstatus&1 = 0 "
                    + " AND act.cstatus&1 = 0 "
                    + " AND ass.actcode = act.unqid "
                    + " AND act.sdate <= CURRENT_DATE "
                    + " AND CURRENT_DATE <= act.edate"
                    // 全局通用， 品类，商品
                    + " AND ass.gcode IN (0, ?, ?, ?, ?) ) "
                    + " INNER JOIN {{?" + DSMConst.TD_PROM_TIME + "}} time "
                    + " ON time.cstatus&1 = 0 "
                    + " AND time.actcode = act.unqid "
                    + " AND time.sdate <= CURRENT_TIME "
                    + " AND CURRENT_TIME <= time.edate "
                    + " WHERE 1=1 ";

    public ActivityFilterService(ActivitiesFilter[] discountFilters) {
        super(discountFilters);
    }

    public ActivityFilterService() { super(); }

    protected List<IDiscount> getCurrentDiscounts(IProduct product) {
        long sku = product.getSKU();

        String[] pclasses = getProductCode(sku);

        if (StringUtils.isEmpty(pclasses)) {
            return new ArrayList<>();
        }

        List<Object[]> queryResult = IceRemoteUtil.queryNative(
                GET_ACTIVITIES_BY_SKU,
                sku, pclasses[0], pclasses[1], pclasses[2]);

        Activity[] activities = new Activity[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, activities, Activity.class);

        List<IDiscount> returnResult = new ArrayList<>(
                new HashSet<>(Arrays.asList(activities)));

        Activity a;
        for (IDiscount discount : returnResult) {
            a = (Activity) discount;
            discount.setLimits(sku, a.getLimitnum());
            a.setActPrice(
                    (a.getAssCstatus() & 512) == 0
                        ? MathUtil.exactDiv(a.getActPrice(), 100).doubleValue()
                        : MathUtil.exactDiv(a.getActPrice(), 100)
                            .multiply(BigDecimal.valueOf(product.getOriginalPrice()))
                            .setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
            discount.setActionPrice(sku, a.getActPrice());
        }

        return returnResult;
    }

}
