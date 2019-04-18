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


    final static Map<Integer, String> ruleNameMap = new HashMap(){{
        put(1110,"满减现金");
        put(1120,"满减包邮");
        put(1130,"满减折扣");
        put(1210,"满赠现金券");
        put(1220,"满赠包邮券");
        put(1230,"满赠折扣券");
        put(1240,"满赠赠品");
        put(2110,"现金券");
        put(2120,"包邮券");
        put(2130,"折扣券");
        put(1113,"秒杀");
        put(1133,"团购");

    }};

    public static int getRuleByBRule(int brule){
        return map.get(brule);
    }

    public static String getRuleByName(int brule){
        return ruleNameMap.get(brule);
    }
}
