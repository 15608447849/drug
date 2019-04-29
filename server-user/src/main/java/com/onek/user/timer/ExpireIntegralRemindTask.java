package com.onek.user.timer;

import constant.DSMConst;
import dao.BaseDAO;
import util.TimeUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.TimerTask;

/**
 * 每年2月1号提醒用户积分失效
 */
public class ExpireIntegralRemindTask extends TimerTask {

    @Override
    public void run() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, Calendar.FEBRUARY);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        if(TimeUtils.getCurrentDate() == TimeUtils.date_yMd_2String(calendar.getTime())){

        }
    }
}
