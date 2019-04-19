package com.onek.calculate.filter;


import com.onek.calculate.entity.Activity;
import com.onek.calculate.entity.IDiscount;
import com.onek.util.area.AreaUtil;
import com.onek.util.member.MemberStore;

/**
 * 质资过滤器。用以过滤用户资质。
 */

public class QualFilter extends BaseFilter {
    private static final int ALL = 0;
    private static final int ORDER_NUMS = 1;
    private static final int LV = 2;
    private static final int AREA = 3;
    private final int compid;

    private Integer currentLV;
    private Integer currentOrdNum;
    private Integer currentArea;

    public QualFilter(int compid) {
        this.compid = compid;
    }

    protected boolean isFilter(IDiscount activity) {
        if (this.compid <= 0) {
            return true;
        }

        Activity act = (Activity) activity;

        boolean result = true;
        int qualCode = act.getQualcode();
        int qualValue = act.getQualvalue();
        long value;
        switch (qualCode) {
            case ALL:
                result = false;
                break;
            case ORDER_NUMS:
                // TODO 根据用户获取订单数
                result = getCurrentOrdNum() < qualValue;
                break;
            case LV:
                // TODO 根据用户获取等级
                result = getCurrentLV() < qualValue;
                break;
            case AREA:
                // TODO 根据用户获取区域
                result = qualValue != 0 && !AreaUtil.isChildren(qualValue, getCurrentArea());
                break;
            default:
                break;
        }

        return result;
    }

    public Integer getCurrentLV() {
        if (this.currentLV == null) {
            this.currentLV = MemberStore.getLevelByCompid(this.compid);
        }

        return this.currentLV;
    }

    public Integer getCurrentOrdNum() {
//        if (this.currentOrdNum == null) {
//            this.currentOrdNum = MemberStore.getLevelByCompid(this.compid);
//        }

//        return this.currentOrdNum;
        return 0;
    }

    public Integer getCurrentArea() {
//        if (this.currentArea == null) {
//            this.currentArea = MemberStore.getLevelByCompid(this.compid);
//        }

//        return this.currentArea;
        return 0;
    }
}
