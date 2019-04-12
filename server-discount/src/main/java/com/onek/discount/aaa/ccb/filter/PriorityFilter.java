package com.onek.discount.aaa.ccb.filter;


import com.onek.discount.aaa.ccb.entity.IDiscount;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class PriorityFilter implements ActivitiesFilter {

    public void doFilter(List<IDiscount> activities) {
        int len = activities.size();

        if (len == 0) {
            return;
        }

        final int[] priorityArr = new int[len];

        for (int i = 0; i < len; i++) {
            priorityArr[i] = activities.get(i).getIncpriority();
        }

        int max = Arrays.stream(priorityArr).max().getAsInt();

        Iterator<IDiscount> it = activities.iterator();
        IDiscount activity;
        while (it.hasNext()) {
            activity = it.next();

            if (activity.getIncpriority() != 0
                    && activity.getIncpriority() != max) {
                it.remove();
            }
        }

    }
}
