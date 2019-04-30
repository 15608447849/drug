package com.onek.user.timer;

import util.TimeUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;

public class IntegralResetTimer {

    static {
        Calendar calendar = Calendar.getInstance();
        Date date  =calendar.getTime(); //第一次执行定时任务的时间
        Timer timer = new Timer();
        IntegralResetTask task = new IntegralResetTask();
        //安排指定的任务在指定的时间开始进行重复的固定延迟执行。
        timer.schedule(task, date, TimeUtils.PERIOD_WEEK);    // 这里其实调用的是 public void schedule(TimerTask task,Date firstTime,long period) // firstTime--这是首次该任务将被执行的时间，即便设置时间为凌晨1点执行，如果不// 加一天，任务会立即执行的话，那么下次执行的时刻是在距此次执行任务时刻的24小 //时后执行，如果现在是14点执行了一次，那么明天14点才会执行第二次，而不是在//凌晨1点执行
    }

    public IntegralResetTimer() {

    }

}
