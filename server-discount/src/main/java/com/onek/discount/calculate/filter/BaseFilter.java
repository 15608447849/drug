package com.onek.discount.calculate.filter;

import com.onek.discount.calculate.entity.IDiscount;

import java.util.Iterator;
import java.util.List;

public abstract class BaseFilter implements ActivitiesFilter {
    @Override
    public void doFilter(List<? extends IDiscount> activities) {
        Iterator<? extends IDiscount> it = activities.iterator();

        IDiscount activity;

        while (it.hasNext()) {
            activity = it.next();

            if (isFilter(activity)) {
                it.remove();
            }
        }
    }

    protected abstract boolean isFilter(IDiscount iDiscount);
}
