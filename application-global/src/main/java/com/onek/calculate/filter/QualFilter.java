package com.onek.calculate.filter;


import com.onek.calculate.entity.Activity;
import com.onek.calculate.entity.IDiscount;

/**
 * 质资过滤器。用以过滤用户资质。
 */

public class QualFilter extends BaseFilter {
    private static final int ALL = 0;
    private static final int ORDER_NUMS = 1;
    private static final int LV = 2;
    private static final int AREA = 3;
    private final int compid;

    public QualFilter(int compid) {
        this.compid = compid;
        System.out.println("compid " + compid);
    }

    protected boolean isFilter(IDiscount activity) {
        if (this.compid <= 0) {
            return true;
        }

        Activity act = (Activity) activity;

        boolean result = true;
        int qualCode = act.getQualcode();
        int qualValue = act.getQualvalue();

        switch (qualCode) {
            case ALL:
                result = false;
                break;
            case ORDER_NUMS:
                // TODO 根据用户获取订单数
                break;
            case LV:
                // TODO 根据用户获取等级
                break;
            case AREA:
                // TODO 根据用户获取区域
                break;
            default:
                break;
        }

        return result;
    }
}
