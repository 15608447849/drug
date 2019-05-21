package com.onek.calculate.auth;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.onek.util.area.AreaUtil;
import com.onek.util.member.MemberStore;
import com.onek.util.order.RedisOrderUtil;
import redis.util.RedisUtil;

/**
 * 权限判断 (活动)
 */
public class QualJudge {

    private static final int ALL = 0; // 所有会员
    private static final int ORDER_NUMS = 1; // 订单数
    private static final int LV = 2; // 会员等级
    private static final int AREA = 3; // 会员地区

    /**
     * 判断是否有权限访问
     *
     * @param compid 企业码
     * @param qualCode 资格码
     * @param qualValue 资格值
     * @return true:代表有权限; false:代表没有权限
     */
    public static boolean hasPermission(int compid, int qualCode, long qualValue) {
        if (compid <= 0) {
            return true;
        }

        boolean result = true;

        try {

            switch (qualCode) {
                case ALL:
                    result = false;
                    break;
                case ORDER_NUMS:
                    result = qualValue == 0 ? getCurrentOrdNum(compid) > 0 : getCurrentOrdNum(compid) < qualValue;
                    break;
                case LV:
                    result = getCurrentLV(compid) < qualValue;
                    break;
                case AREA:
                    result = qualValue != 0
                            && qualValue != getCurrentArea(compid)
                            && !AreaUtil.isChildren(qualValue, getCurrentArea(compid));
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return !result;
    }

    private static Integer getCurrentLV(int compid) {

        return MemberStore.getLevelByCompid(compid);

    }

    private static Integer getCurrentOrdNum(int compid) {
        return RedisOrderUtil.getOrderNumByCompid(compid);

    }

    private static Long getCurrentArea(int compid) {
        String compStr = RedisUtil.getStringProvide().get(String.valueOf(compid));

        JSONObject compJson = JSON.parseObject(compStr);
        return compJson.getLongValue("addressCode");

    }
}
