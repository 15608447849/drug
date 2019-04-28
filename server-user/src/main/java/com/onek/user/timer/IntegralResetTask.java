package com.onek.user.timer;

import constant.DSMConst;
import dao.BaseDAO;
import util.TimeUtils;

import java.util.TimerTask;

/**
 * 每年1月1号就过年的积分余额转换为过期积分
 */
public class IntegralResetTask extends TimerTask {

    private static final String SQL = "update {{?" + DSMConst.TD_MEMBER + "}} set balpoints = balpoints - expirepoint, expirepoint = 0";

    @Override
    public void run() {
        System.out.println("#### [" + TimeUtils.getCurrentDate() + "] reset intergal start ########");
        int result = BaseDAO.getBaseDAO().updateNative(SQL, new Object[]{});

        System.out.println("#### [" + TimeUtils.getCurrentDate() + "] reset intergal result:[" + result + "] ########");
        System.out.println("#### [" + TimeUtils.getCurrentDate() + "] reset intergal end ########");
    }
}
