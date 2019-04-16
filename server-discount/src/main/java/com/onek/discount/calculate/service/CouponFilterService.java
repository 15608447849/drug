package com.onek.discount.calculate.service;

import com.alibaba.fastjson.JSONObject;
import com.onek.context.UserSession;
import com.onek.discount.calculate.entity.Couent;
import com.onek.discount.calculate.entity.IDiscount;
import constant.DSMConst;
import dao.BaseDAO;
import global.IceRemoteUtil;
import util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CouponFilterService extends BaseDiscountFilterService {
    private long couponNo;
    private UserSession userSession;
    private boolean excoupon;
    private Boolean noway = null;
    private Couent couent;

    private static final String GET_COUPON =
            " SELECT * "
            + " FROM {{?" + DSMConst.TD_PROM_COUENT + "}} "
            + " WHERE cstatus=0 "
            + " AND startdate <= CURRENT_DATE AND CURRENT_DATE <= enddate "
            + " AND unqid = ? ";

    private static final String CHECK_SKU =
            " SELECT COUNT(0) "
            + " FROM {{?" + DSMConst.TD_PROM_ASSDRUG + "}} "
            + " WHERE cstatus&1 = 0 "
            + " AND actcode = ? AND gcode IN (?, ? , 0) ";



    public CouponFilterService(
            long couponNo,
            boolean excoupon,
            UserSession userSession) {
        super(null);

        this.couponNo = couponNo;
        this.userSession = userSession;
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

        List<IDiscount> returnList = new ArrayList<>();
        returnList.add(checkCoupon);

        return returnList;
    }

    private boolean checkSKU(long sku) {
        String productCode = getProductCode(sku);
        List<Object[]> check = BaseDAO.getBaseDAO().queryNative(CHECK_SKU, sku, productCode);

        return StringUtils.isBiggerZero(check.get(0)[0].toString());
    }

    private Couent getCouent() {
        if (noway != null) {
            return this.couent;
        }

        String coupStr = IceRemoteUtil.getCoup(this.userSession.compId, this.couponNo);

        if (StringUtils.isEmpty(coupStr)) {
            this.noway = true;
            return null;
        }

        Couent couent;

        try {
           couent = JSONObject.parseObject(coupStr, Couent.class);
        } catch (Exception e) {
            this.noway = true;
            return null;
        }

        if (couent.getGlbno() == 0 && this.excoupon) {
            this.noway = true;
            return null;
        }

        this.noway = false;
        return this.couent = couent;
    }

}