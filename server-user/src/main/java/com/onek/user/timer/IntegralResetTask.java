package com.onek.user.timer;

import constant.DSMConst;
import dao.BaseDAO;
import util.TimeUtils;

import java.util.Calendar;
import java.util.TimerTask;

/**
 * 每年1月1号就过年的积分余额转换为过期积分
 */
public class IntegralResetTask extends TimerTask {

    private static final String SQL = "update {{?" + DSMConst.TD_MEMBER + "}} set balpoints = balpoints - expirepoint, expirepoint = 0";

    @Override
    public void run() {


        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, Calendar.MARCH);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        if(TimeUtils.getCurrentDate() == TimeUtils.date_yMd_2String(calendar.getTime())){
            int result = BaseDAO.getBaseDAO().updateNative(SQL, new Object[]{});

        }
    }
}
