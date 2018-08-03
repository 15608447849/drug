package com.onek.discount.timer;

import util.TimeUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;

public class TeamBuyTimer {

    static {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 16); //凌晨2点
        calendar.set(Calendar.MINUTE, 10);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 1);
        Date date  =calendar.getTime(); //第一次执行定时任务的时间
        //如果第一次执行定时任务的时间 小于当前的时间
        //此时要在 第一次执行定时任务的时间加一天，以便此任务在下个时间点执行。如果不加一天，任务会立即执行。
        if (date.before(new Date())) {
            date = TimeUtils.addDay(date, 1);
        }
        Timer timer = new Timer();
        TeamBuyTask task = new TeamBuyTask();
        //安排指定的任务在指定的时间开始进行重复的固定延迟执行。
        timer.schedule(task, date, TimeUtils.PERIOD_DAY);    // 这里其实调用的是 public void schedule(TimerTask task,Date firstTime,long period) // firstTime--这是首次该任务将被执行的时间，即便设置时间为凌晨1点执行，如果不// 加一天，任务会立即执行的话，那么下次执行的时刻是在距此次执行任务时刻的24小 //时后执行，如果现在是14点执行了一次，那么明天14点才会执行第二次，而不是在//凌晨1点执行
    }

    public TeamBuyTimer() {

    }

}
