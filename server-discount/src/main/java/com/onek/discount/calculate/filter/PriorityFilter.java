package com.onek.discount.calculate.filter;


import com.onek.discount.calculate.entity.Activity;
import com.onek.discount.calculate.entity.IDiscount;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * 优先级过滤器。用以过滤除全局外的低优先级。
 */

public class PriorityFilter extends BaseFilter {

    public void doFilter(List<? extends IDiscount> activities) {
        int len = activities.size();

        if (len == 0) {
            return;
        }

        final int[] priorityArr = new int[len];

        for (int i = 0; i < len; i++) {
            priorityArr[i] = ((Activity) activities.get(i)).getIncpriority();
        }

        int max = Arrays.stream(priorityArr).max().getAsInt();

        Iterator<? extends IDiscount> it = activities.iterator();
        Activity activity;
        while (it.hasNext()) {
            activity = (Activity) it.next();

            if (activity.getIncpriority() != 0
                    && activity.getIncpriority() != max) {
                it.remove();
            }
        }

    }

    @Override
    protected boolean isFilter(IDiscount iDiscount) {
        return false;
    }
}
