package com.onek.calculate.filter;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.onek.calculate.entity.Activity;
import com.onek.calculate.entity.IDiscount;
import com.onek.util.area.AreaUtil;
import com.onek.util.member.MemberStore;
import com.onek.util.order.RedisOrderUtil;
import redis.util.RedisUtil;

/**
 * 质资过滤器。用以过滤用户资质。
 */

public class QualFilter extends BaseFilter {
    private static final int ALL = 0;
    private static final int ORDER_NUMS = 1;
    private static final int LV = 2;
    private static final int AREA = 3;
    private final int compid;

    private Integer currentLV;
    private Integer currentOrdNum;
    private Integer currentArea;

    public QualFilter(int compid) {
        this.compid = compid;
    }

    protected boolean isFilter(IDiscount activity) {
        if (this.compid <= 0) {
            return true;
        }

        boolean result = true;

        try {
            Activity act = (Activity) activity;

            int qualCode = act.getQualcode();
            long qualValue = act.getQualvalue();

            switch (qualCode) {
                case ALL:
                    result = false;
                    break;
                case ORDER_NUMS:
                    result = qualValue == 0 ? getCurrentOrdNum() > 0 : getCurrentOrdNum() < qualValue;
                    break;
                case LV:
                    result = getCurrentLV() < qualValue;
                    break;
                case AREA:
                    result = qualValue != 0 && !AreaUtil.isChildren(qualValue, getCurrentArea());
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private Integer getCurrentLV() {
        if (this.currentLV == null) {
            this.currentLV = MemberStore.getLevelByCompid(this.compid);
        }

        return this.currentLV;
    }

    private Integer getCurrentOrdNum() {
        if (this.currentOrdNum == null) {
            this.currentOrdNum = RedisOrderUtil.getOrderNumByCompid(this.compid);
        }

        return this.currentOrdNum;
    }

    private Integer getCurrentArea() {
        if (this.currentArea == null) {
            String compStr = RedisUtil.getStringProvide()
                    .get(String.valueOf(this.compid));

            JSONObject compJson = JSON.parseObject(compStr);
            this.currentArea = compJson.getIntValue("addressCode");
        }

        return this.currentArea;
    }
}
