package com.onek.util.area;

import com.onek.util.IceRemoteUtil;
import util.MathUtil;

public class AreaFeeUtil {

    public static double getFee(long areac){
        double fee = 0D;
        AreaEntity[] areaEntities = IceRemoteUtil.getAncestors(areac);
        if(areaEntities != null && areaEntities.length > 0){
            for(int i = areaEntities.length - 1; i>=0; i--){
                if(areaEntities[i].getFee() > 0){
                    fee = MathUtil.exactDiv(areaEntities[i].getFee(), 100).doubleValue();
                    break;
                }
            }
        }
        return fee;
    }
}
