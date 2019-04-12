package com.onek.discount.aaa.ccb.filter;


import com.onek.discount.aaa.ccb.entity.IDiscount;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

public class CycleFilter implements ActivitiesFilter {
    private static final int DAY = 0;
    private static final int WEEK = 1;
    private static final int MONTH = 2;
    private static final int YEAR = 3;

    private static final long[] WEEK_OF_DAY_STATUS =
            { 64, 1, 2, 4, 8, 16, 32 };

    private static final SimpleDateFormat YYMMDD = new SimpleDateFormat("yyMMdd");

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
        int actType = activity.getActtype();
        long actcycle = activity.getActcycle();
        Calendar now = Calendar.getInstance();

        switch (actType) {
            case DAY :
                result = false;
                break;
            case WEEK :
                result = (actcycle & WEEK_OF_DAY_STATUS[now.get(Calendar.DAY_OF_WEEK) - 1]) == 0;
                break;
            case MONTH :
                result = (actcycle & (1L << (now.get(Calendar.DAY_OF_MONTH) - 1))) == 0;
                break;
            case YEAR :
                try {
                    Calendar tarc = Calendar.getInstance();
                    // 年的存储规则：YYMMDD
                    tarc.setTime(YYMMDD.parse(String.valueOf(actcycle)));
                    result = tarc.get(Calendar.MONTH) != now.get(Calendar.MONTH)
                            || tarc.get(Calendar.DAY_OF_MONTH) != now.get(Calendar.DAY_OF_MONTH);
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                break;
            default:
                break;
        }

        return result;
    }

}
