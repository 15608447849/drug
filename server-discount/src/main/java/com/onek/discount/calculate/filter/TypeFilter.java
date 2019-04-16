package com.onek.discount.calculate.filter;

import com.onek.discount.calculate.entity.IDiscount;

import java.util.Iterator;
import java.util.List;

/**
 * 类型过滤器。用以过滤团购类型。
 */

public class TypeFilter implements ActivitiesFilter {
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
        long bRule = activity.getBRule();

        return bRule == 1133;
    }
}
