package com.onek.report.col;

import com.onek.report.core.IRowData;
import com.onek.report.vo.*;
import com.onek.util.area.AreaUtil;
import constant.DSMConst;
import dao.BaseDAO;
import util.ArrayUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColItem implements IRowData {
    private static final Map<Long, String[]> areaStore = new HashMap<>();

    private OrderNum orderNum;
    private GMV gmv;
    private CompPrice compPrice;

    private long areac;
    private String arean;
    private int compNum;

    public ColItem() {
        orderNum = new OrderNum();
        gmv = new GMV();
        compPrice = new CompPrice();
    }

    public ColItem(long areac) {
        this();
        setAreac(areac);
    }

    public void setArean(String arean) {
        this.arean = arean;
    }

    public long getAreac() {
        return areac;
    }

    public String getArean() {
        return arean;
    }

    public int getCompNum() {
        return compNum;
    }

    public void setAreac(long areac) {
        this.areac = areac;
        String[] an = areaStore.get(ColItem.this.areac);

        if (an == null) {
            an = new String[2];

            an[0] = getArean(areac);

            String sql =
                    " SELECT COUNT(0) "
                            + " FROM {{?" + DSMConst.TB_COMP + "}} "
                            + " WHERE cstatus&1 = 0 AND ctype = 0 AND caddrcode LIKE ? ";
            List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNative(sql, getWhereLike(ColItem.this.areac));

            an[1] = queryResult.get(0)[0].toString();

            areaStore.put(ColItem.this.areac, an);
        }

        ColItem.this.arean = an[0];
        ColItem.this.compNum = Integer.parseInt(an[1]);
    }

    private String getArean(long areac) {
        if (areac == 0) {
            return "";
        }

        String sql = " SELECT arean "
                + " FROM {{?" + DSMConst.TB_AREA_PCA + "}} "
                + " WHERE cstatus&1 = 0 AND areac = ? ";

        List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNative(sql, areac);

        if (queryResult.isEmpty()) {
            return "";
        }

        return queryResult.get(0)[0].toString();
    }

    private String getWhereLike(long areac) {
        int layer = AreaUtil.getLayer(areac);

        String like = String.valueOf(areac)
                .substring(0,
                        layer <= 2
                                ? layer * 2 + 2
                                : layer * 3);

        return like + "%";
    }

    public OrderNum getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(OrderNum orderNum) {
        this.orderNum = orderNum;
    }

    public GMV getGmv() {
        return gmv;
    }

    public void setGmv(GMV gmv) {
        this.gmv = gmv;
    }

    public CompPrice getCompPrice() {
        return compPrice;
    }

    public void setCompPrice(CompPrice compPrice) {
        this.compPrice = compPrice;
    }

    public void accItem(ColItem colItem) {
        accGMV(colItem.getGmv());
        accOrderNum(colItem.getOrderNum());
        accCompPrice(colItem.getCompPrice());
    }

    private void accCompPrice(CompPrice compPrice) {
        this.getCompPrice().addMax(compPrice.getMax());
        this.getCompPrice().addMin(compPrice.getMin());
        this.getCompPrice().addAvg(compPrice.getAvg());
    }

    private void accOrderNum(OrderNum orderNum) {
        accCanceledNum(orderNum.getCanceledNum());
        accSuccessNum(orderNum.getSuccessNum());
    }

    private void accSuccessNum(SuccessNum successNum) {
        this.orderNum.getSuccessNum().addBack(successNum.getBack());
        this.orderNum.getSuccessNum().addRet(successNum.getRet());
        this.orderNum.getSuccessNum().addSuccessTotal(successNum.getSuccessTotal());
    }

    private void accCanceledNum(CanceledNum canceledNum) {
        this.orderNum.getCanceledNum().addBackCanceled(canceledNum.getBackCanceled());
        this.orderNum.getCanceledNum().addUserCanceled(canceledNum.getUserCanceled());
    }


    private void accGMV(GMV gmv) {
        accCancelVolume(gmv.getCancelVolume());
        accSuccessVolume(gmv.getSuccessVolume());
    }

    private void accCancelVolume(CancelVolume cancelVolume) {
        this.gmv.getCancelVolume().addBackCanceled(cancelVolume.getBackCanceled());
        this.gmv.getCancelVolume().addUserCanceled(cancelVolume.getUserCanceled());
    }

    private void accSuccessVolume(SuccessVolume successVolume) {
        this.gmv.getSuccessVolume().addRet(successVolume.getRet());
        this.gmv.getSuccessVolume().addSuccessTotal(successVolume.getSuccessTotal());
    }

    @Override
    public double[] getEachCol() {
        double[] result = ArrayUtil.concat(orderNum.getEachCol(), gmv.getEachCol());

        result = ArrayUtil.concat(result, compPrice.getEachCol());

        return result;
    }
}
