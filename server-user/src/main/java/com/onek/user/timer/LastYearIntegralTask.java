package com.onek.user.timer;

import com.alibaba.fastjson.JSONObject;
import com.onek.propagation.prod.ActivityManageServer;
import com.onek.propagation.prod.ProdDiscountObserver;
import constant.DSMConst;
import dao.BaseDAO;
import util.TimeUtils;

import java.util.*;

/**
 * 每年1月1号就过年的积分余额转换为过期积分
 */
public class LastYearIntegralTask extends TimerTask {

    private static final String SQL = "update {{?" + DSMConst.TD_MEMBER + "}} set expirepoint = balpoints";

    @Override
    public void run() {
        System.out.println("#### [" + TimeUtils.getCurrentDate() + "] expire intergal convert start ########");
        int result = BaseDAO.getBaseDAO().updateNative(SQL, new Object[]{});
        System.out.println("#### [" + TimeUtils.getCurrentDate() + "] expire intergal result:["+ result+"] ########");
        System.out.println("#### [" + TimeUtils.getCurrentDate() + "] expire intergal convert end ########");
    }
}
