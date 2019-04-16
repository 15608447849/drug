package com.onek.util.discount;

import java.util.HashMap;
import java.util.Map;

public class DiscountRuleStore {
    final static Map<Integer, Integer> map = new HashMap(){{
        put(1110,1);
        put(1120,2);
        put(1130,4);
        put(1210,8);
        put(1220,16);
        put(1230,32);
        put(1240,64);
        put(2110,128);
        put(2120,256);
        put(2130,512);
        put(1113,2048);
        put(1133,4096);
    }};

    public static int getRuleByBRule(int brule){
        return map.get(brule);
    }
}
