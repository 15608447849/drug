package com.onek.discount.calculate.filter;


import com.onek.discount.calculate.entity.IDiscount;

import java.util.Iterator;
import java.util.List;

public class QualFilter implements ActivitiesFilter {
    private static final int ALL = 0;
    private static final int ORDER_NUMS = 1;
    private static final int LV = 2;
    private static final int AREA = 3;

//    private final UserSession userSession;

//    public QualFilter(UserSession userSession) {
//        this.userSession = userSession;
//    }

    @Override
    public void doFilter(List<IDiscount> activities) {
        Iterator<IDiscount> it = activities.iterator();
        IDiscount activity;
        while (it.hasNext()) {
            activity = it.next();

            if (isFilter(activity)) {
                it.remove();
            }
        }
    }

    private boolean isFilter(IDiscount activity) {
        boolean result = true;
        int qualCode = activity.getQualcode();
        int qualValue = activity.getQualvalue();
//        int uid = userSession.userId;
//        int cid = userSession.compId;
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
