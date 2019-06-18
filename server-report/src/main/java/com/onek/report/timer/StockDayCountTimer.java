package com.onek.report.timer;

import util.TimeUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;

/**
 * @author liuhui
 * @version V1.0
 * @ClassName StockDayCountTimer
 * @Description 定时统计每天的库存，每天晚上12点定时统计（数据保存近30天）
 * @date 2019-06-13 10:08
 */
public class StockDayCountTimer {

    static {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0); //0点
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 1);
        Date date  =calendar.getTime(); //第一次执行定时任务的时间
        //如果第一次执行定时任务的时间 小于当前的时间
        //此时要在 第一次执行定时任务的时间加一天，以便此任务在下个时间点执行。如果不加一天，任务会立即执行。
        if (date.before(new Date())) {
            date = TimeUtils.addDay(date, 1);
        }
        Timer timer = new Timer();
        StockDayCountTask task = new StockDayCountTask();
        //安排指定的任务在指定的时间开始进行重复的固定延迟执行。
        timer.schedule(task, date, TimeUtils.PERIOD_DAY);    // 这里其实调用的是 public void schedule(TimerTask task,Date firstTime,long period) // firstTime--这是首次该任务将被执行的时间，即便设置时间为凌晨1点执行，如果不// 加一天，任务会立即执行的话，那么下次执行的时刻是在距此次执行任务时刻的24小 //时后执行，如果现在是14点执行了一次，那么明天14点才会执行第二次，而不是在//凌晨1点执行
    }
}
