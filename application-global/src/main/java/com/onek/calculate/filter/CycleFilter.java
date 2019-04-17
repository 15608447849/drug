package com.onek.calculate.filter;


import com.onek.calculate.entity.Activity;
import com.onek.calculate.entity.IDiscount;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * 周期过滤器。用以过滤在周期内的活动
 */
public class CycleFilter extends BaseFilter {
    private static final int DAY = 0;
    private static final int WEEK = 1;
    private static final int MONTH = 2;
    private static final int YEAR = 3;

    private static final long[] WEEK_OF_DAY_STATUS =
            { 64, 1, 2, 4, 8, 16, 32 };

    private static final SimpleDateFormat YYMMDD = new SimpleDateFormat("yyMMdd");

    protected boolean isFilter(IDiscount activity) {
        Activity act = (Activity) activity;

        boolean result = true;
        int actType = act.getActtype();
        long actcycle = act.getActcycle();
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
