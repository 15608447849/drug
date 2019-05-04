package com.onek.goods.calculate;

import com.onek.calculate.entity.Couent;
import com.onek.calculate.entity.IDiscount;
import com.onek.calculate.service.filter.BaseDiscountFilterService;
import constant.DSMConst;
import dao.BaseDAO;
import util.StringUtils;
import util.TimeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CouponFilterService extends BaseDiscountFilterService {
    private long couentUnqid;
    private int compid;
    private boolean excoupon;
    private Boolean noway = null;
    private Couent couent;

    private static final String GET_COUPON =
            " SELECT oid, unqid, coupno, compid, startdate, "
                    + " starttime, enddate, endtime, brulecode, rulename, "
                    + " goods, ladder, glbno, ctype, cstatus, reqflag "
                    + " FROM {{?" + DSMConst.TD_PROM_COUENT + "}} "
                    + " WHERE cstatus = 0 "
                    + " AND startdate <= CURRENT_DATE AND CURRENT_DATE <= enddate "
                    + " AND unqid = ? ";

    private static final String CHECK_SKU =
            " SELECT COUNT(0) "
                    + " FROM {{?" + DSMConst.TD_PROM_ASSDRUG + "}} "
                    + " WHERE cstatus&1 = 0 "
                    + " AND actcode = ? AND gcode IN (?, ? , 0) ";

    public CouponFilterService(
            long couponUnqid,
            boolean excoupon,
            int compid) {
        super();

        this.couentUnqid = couponUnqid;
        this.compid = compid;
        this.excoupon = excoupon;
    }

    @Override
    public List<IDiscount> getCurrentDiscounts(long sku) {
        // 判定优惠券阶段
        Couent checkCoupon = getCouent();

        if (checkCoupon == null) {
            return Collections.EMPTY_LIST;
        }

        // 校验SKU
        if (checkCoupon.getGoods() > 0 && !checkSKU(sku)) {
            return Collections.EMPTY_LIST;
        }

        return new ArrayList<IDiscount>() {{add(checkCoupon);}};
    }

    private boolean checkSKU(long sku) {
        String productCode = getProductCode(sku);
        List<Object[]> check = BaseDAO.getBaseDAO().queryNative(CHECK_SKU,
                this.couent.getCoupno(), sku, productCode);

        return StringUtils.isBiggerZero(check.get(0)[0].toString());
    }

    private Couent getCouent() {
        if (this.noway != null) {
            return this.couent;
        }

        if (this.compid <= 0) {
            this.noway = true;
            return null;
        }

        List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNativeSharding(
                this.compid, TimeUtils.getCurrentYear(),
                GET_COUPON, this.couentUnqid);

        if (queryResult.isEmpty()) {
            this.noway = true;
            return null;
        }

        Couent[] cArray = new Couent[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, cArray, Couent.class);

        Couent couent = cArray[0];

        // glbno为0 -> 不全局兼容
        // excoupon -> 是否排斥优惠券
        if (couent.getGlbno() == 0 && this.excoupon) {
            this.noway = true;
            return null;
        }

        this.noway = false;
        return this.couent = couent;
    }

}