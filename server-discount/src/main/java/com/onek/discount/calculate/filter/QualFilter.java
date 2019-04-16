package com.onek.discount.calculate.filter;


import com.onek.context.UserSession;
import com.onek.discount.calculate.entity.IDiscount;

import java.util.Iterator;
import java.util.List;

/**
 * 质资过滤器。用以过滤用户资质。
 */

public class QualFilter implements ActivitiesFilter {
    private static final int ALL = 0;
    private static final int ORDER_NUMS = 1;
    private static final int LV = 2;
    private static final int AREA = 3;
    private final int compid;

    public QualFilter(int compid) {
        this.compid = compid;
    }

    @Override
    public void doFilter(List<IDiscount> activities) {
        if (this.compid <= 0) {
            return ;
        }

        Iterator<IDiscount> it = activities.iterator();
        IDiscount activity;
        while (it.hasNext()) {
            activity = it.next();

            if (false && isFilter(activity)) {
                it.remove();
            }
        }
    }

    private boolean isFilter(IDiscount activity) {
        boolean result = true;
        int qualCode = activity.getQualcode();
        int qualValue = activity.getQualvalue();

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
