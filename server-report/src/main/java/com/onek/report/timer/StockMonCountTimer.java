package com.onek.report.timer;

import util.TimeUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;

/**
 * @author liuhui
 * @version V1.0
 * @ClassName StockMonCountTimer
 * @Description 按月统计库存
 * @date 2019-06-13 10:43
 */
public class StockMonCountTimer {

    static {
        Calendar calendar=Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DATE);
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH,(month));
        int end=calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        int begin=calendar.getActualMinimum(Calendar.DAY_OF_MONTH);
        Date date  =  null;//第一次执行定时任务的时间
        if(begin == day){
            calendar.set(Calendar.HOUR_OF_DAY, 0); //0点
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 1);
            date = calendar.getTime();
            Timer timer = new Timer();
            StockMonCountTask task = new StockMonCountTask();
            timer.schedule(task, date, TimeUtils.PERIOD_MONTH);
        }
    }

    public static void main(String[] args) {

    }
}
