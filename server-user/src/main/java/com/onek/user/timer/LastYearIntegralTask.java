package com.onek.user.timer;

import constant.DSMConst;
import dao.BaseDAO;
import util.TimeUtils;

import java.util.Calendar;
import java.util.TimerTask;

/**
 * 每年1月1号就过年的积分余额转换为过期积分
 */
public class LastYearIntegralTask extends TimerTask {

    private static final String SQL = "update {{?" + DSMConst.TD_MEMBER + "}} set expirepoint = balpoints";

    @Override
    public void run() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, Calendar.JANUARY);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        if(TimeUtils.getCurrentDate() == TimeUtils.date_yMd_2String(calendar.getTime())){

            int result = BaseDAO.getBaseDAO().updateNative(SQL, new Object[]{});
        }

    }
}
