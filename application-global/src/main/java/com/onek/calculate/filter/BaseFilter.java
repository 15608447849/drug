package com.onek.calculate.filter;

import com.onek.calculate.entity.IDiscount;
import com.onek.calculate.entity.IProduct;

import java.util.Iterator;
import java.util.List;

public abstract class BaseFilter implements ActivitiesFilter {
    @Override
    public void doFilter(List<? extends IDiscount> activities, IProduct product) {
        Iterator<? extends IDiscount> it = activities.iterator();

        IDiscount activity;

        while (it.hasNext()) {
            activity = it.next();

            if (isFilter(activity, product)) {
                it.remove();
            }
        }
    }

    protected abstract boolean isFilter(IDiscount iDiscount, IProduct product);
}
