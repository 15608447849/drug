package com.onek.goods.calculate;

import com.onek.calculate.entity.Package;
import com.onek.calculate.entity.*;
import com.onek.calculate.filter.ActivitiesFilter;
import com.onek.calculate.service.filter.BaseDiscountFilterService;
import com.onek.util.IceRemoteUtil;
import com.onek.util.prod.ProdEntity;
import com.onek.util.prod.ProdInfoStore;
import constant.DSMConst;
import dao.BaseDAO;
import util.MathUtil;
import util.StringUtils;

import java.math.BigDecimal;
import java.util.*;

public class ActivityFilterService extends BaseDiscountFilterService {
    //远程调用
    private static final String GET_ACTIVITIES_BY_SKU =
            " SELECT act.oid, act.unqid, act.actname, act.incpriority, act.cpriority, "
                    + " act.qualcode, act.qualvalue, act.actdesc, act.excdiscount, act.acttype, "
                    + " act.actcycle, act.sdate, act.edate, act.brulecode, act.cstatus, "
                    + " ass.limitnum, time.sdate, time.edate, ass.price, ass.cstatus, ass.gcode "
                    + " FROM ({{?" + DSMConst.TD_PROM_ASSDRUG + "}} ass "
                    + " INNER JOIN {{?" + DSMConst.TD_PROM_ACT + "}} act"
                    + " ON ass.cstatus&1 = 0 "
                    + " AND act.cstatus&1 = 0 "
                    + " AND act.cstatus&32 = 0 "
                    + " AND act.cstatus&2048 > 0 "
                    + " AND act.ckstatus&32 = 0 "
                    + " AND ass.actcode = act.unqid "
                    + " AND act.sdate <= CURRENT_DATE "
                    + " AND CURRENT_DATE <= act.edate"
                    // 全局通用， 品类，商品
                    + " AND ass.gcode IN (0, ?, ?, ?, ?) "
                    + " AND menucode = 0 ) "
                    + " INNER JOIN {{?" + DSMConst.TD_PROM_TIME + "}} time "
                    + " ON time.cstatus&1 = 0 "
                    + " AND time.actcode = act.unqid "
                    + " AND time.sdate <= CURRENT_TIME "
                    + " AND CURRENT_TIME <= time.edate "
                    + " WHERE 1=1 ";

    private static final String GET_ACTIVITIES_BY_PKG =
            " SELECT act.oid, act.unqid, act.actname, act.incpriority, act.cpriority, "
                    + " act.qualcode, act.qualvalue, act.actdesc, act.excdiscount, act.acttype, "
                    + " act.actcycle, act.sdate, act.edate, act.brulecode, act.cstatus, "
                    + " ass.limitnum, time.sdate, time.edate, ass.price, ass.cstatus, ass.gcode, ass.pkgprodnum "
                    + " FROM ({{?" + DSMConst.TD_PROM_ASSDRUG + "}} ass "
                    + " INNER JOIN {{?" + DSMConst.TD_PROM_ACT + "}} act"
                    + " ON ass.cstatus&1 = 0 "
                    + " AND act.cstatus&1 = 0 "
                    + " AND act.cstatus&32 = 0 "
                    + " AND act.cstatus&2048 > 0 "
                    + " AND ass.actcode = act.unqid "
                    + " AND act.sdate <= CURRENT_DATE "
                    + " AND CURRENT_DATE <= act.edate"
                    // 全局通用， 品类，商品
                    + " AND (ass.menucode = ? OR (ass.gcode = 0 AND act.incpriority = 0 ))) "
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
        if (product instanceof Product) {
            return getFromSKU((Product) product);
        } else {
            return getFromPackage((Package) product);
        }
    }

    private List<IDiscount> getFromPackage(Package pkg) {
        long sku = pkg.getSKU();

        //远程调用
        List<Object[]> queryResult = IceRemoteUtil.queryNative(
                GET_ACTIVITIES_BY_PKG, sku);

        if (queryResult.isEmpty()) {
            return Collections.emptyList();
        }

        Activity[] activities = new Activity[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, activities, Activity.class);

        boolean invalid = true;
        for (Activity activity : activities) {
            if (activity.getPkgprodnum() > 0) {
                invalid = false;
                break;
            }
        }

        if (invalid) {
            pkg.setExpireFlag(-2);
        }

        addPkgProd(pkg, activities);

        Set<Activity> activitySet = new HashSet<>();

        for (Activity activity : activities) {
            activitySet.add(activity);
        }

        Activity a;
        for (IDiscount discount : activitySet) {
            a = (Activity) discount;
            discount.setLimits(sku, a.getLimitnum());

            if (a.getActPrice() <= 0) {
                continue;
            }

            a.setActPrice(
                    (a.getAssCstatus() & 512) == 0
                            ? MathUtil.exactDiv(a.getActPrice(), 100).doubleValue()
                            : MathUtil.exactDiv(a.getActPrice(), 100)
                            .multiply(BigDecimal.valueOf(pkg.getOriginalPrice()))
                            .setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());

            discount.setActionPrice(sku, a.getActPrice());
        }

        return new ArrayList<>(activitySet);
    }

    private void addPkgProd(Package pkg, Activity[] activities) {
        long gcode;
        ProdEntity prod;
        Product p;
        Set<Long> gcodeSet = new HashSet<>();
        int total = 0;
        for (Activity activity : activities) {
            gcode = activity.getActivityGcode();

            if (activity.getPkgprodnum() > 0 && gcode > 0 && gcodeSet.add(gcode)) {
                prod = ProdInfoStore.getProdBySku(gcode);

                if (prod != null) {
                    p = new Product();
                    p.setSku(gcode);
                    total += (prod.getVatp() * activity.getPkgprodnum());
                    p.autoSetCurrentPrice(MathUtil.exactDiv(prod.getVatp(), 100).doubleValue(),
                            activity.getPkgprodnum() * pkg.getNums());
                    pkg.addPacageProd(p);
                }
            }
        }

        pkg.setOriginalPrice(total / 100.0);
    }

    private List<IDiscount> getFromSKU(Product product) {
        long sku = product.getSKU();

        String[] pclasses = getProductCode(sku);

        if (StringUtils.isEmpty(pclasses)) {
            return new ArrayList<>();
        }
        //远程调用
        List<Object[]> queryResult = IceRemoteUtil.queryNative(
                GET_ACTIVITIES_BY_SKU,
                sku, pclasses[0], pclasses[1], pclasses[2]);

        Activity[] activities = new Activity[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, activities, Activity.class);

        List<IDiscount> returnResult = new ArrayList<>(
                new HashSet<>(Arrays.asList(activities)));

        ProdEntity prod = ProdInfoStore.getProdBySku(sku);

        if (prod != null) {
            product.setOriginalPrice(MathUtil.exactDiv(prod.getVatp(), 100).doubleValue());
        }

        Activity a;
        for (IDiscount discount : returnResult) {
            a = (Activity) discount;
            discount.setLimits(sku, a.getLimitnum());

            if (a.getActPrice() <= 0) {
                continue;
            }

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
