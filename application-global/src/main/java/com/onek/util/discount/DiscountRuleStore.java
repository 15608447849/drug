package com.onek.util.discount;

import com.onek.calculate.entity.Activity;
import com.onek.calculate.entity.Gift;
import com.onek.calculate.entity.Ladoff;

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
        put(1210,"满赠返现");
        put(1220,"满赠包邮券");
        put(1230,"满赠折扣券");
        put(1240,"满赠赠品");
        put(2110,"现金券");
        put(2120,"包邮券");
        put(2130,"折扣券");
        put(1113,"秒杀");
        put(1133,"团购");

    }};

    private static String[] FOUR = {"", "每满", "满"};
    private static String[][] ONE_TWO = {
            {"", "$", "包邮", "$折", "#" },
            {"", "减&", "&", "打&" },
            {"", "返现&", "赠&券", "赠&券", "赠&"}};

    public static int getRuleByBRule(int brule){
        return map.get(brule);
    }

    public static String getRuleByName(int brule){
        return ruleNameMap.get(brule);
    }

    public static String getActivityDesc(Activity activity) {
        try {
            Ladoff currentLadoff = activity.getCurrLadoff();

            if (currentLadoff == null) {
                return "";
            }

            String codeStr = String.valueOf(currentLadoff.getOffercode());
            StringBuilder value = new StringBuilder();
            int four = Character.digit(codeStr.charAt(4), 10);
            int one = Character.digit(codeStr.charAt(1), 10);
            int two = Character.digit(codeStr.charAt(2), 10);
            double offerValue = currentLadoff.getOffer();
            String offer = offerValue + "元";

            if (currentLadoff.isPercentage()) {
                offer = (offerValue * 100) + "%";
            }

            StringBuilder giftName = new StringBuilder();

            if (one == 2 && two == 4) {
                if (currentLadoff.getGiftList() != null && !currentLadoff.getGiftList().isEmpty()) {
                    for (Gift gift : currentLadoff.getGiftList()) {
                        giftName.append(" " + gift.getGiftName());
                    }
                }
            }

            value.append(FOUR[four]);

            if (currentLadoff.getLadnum() > 0) {
                value.append(currentLadoff.getLadnum() + "件，");
            }

            if (currentLadoff.getLadamt() > 0) {
                value.append(currentLadoff.getLadamt() + "元，");
            }

            value.append(
                    ONE_TWO[one][two]
                            .replace("&",
                                ONE_TWO[0][two]
                                        .replace("$", offer).replace("#", giftName.toString())));

            return value.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static String getGapActivityDesc(Activity activity) {
        try {
            Ladoff nextLadoff = activity.getNextLadoff();

            if (nextLadoff == null) {
                return "";
            }

            String codeStr = String.valueOf(nextLadoff);
            StringBuilder value = new StringBuilder();
            int one = Character.digit(codeStr.charAt(1), 10);
            int two = Character.digit(codeStr.charAt(2), 10);

            double offerValue = nextLadoff.getOffer();
            String offer = offerValue + "元";

            if (nextLadoff.isPercentage()) {
                offer = (nextLadoff.getOffercode() * 100) + "%";
            }

            StringBuilder giftName = new StringBuilder();

            if (one == 2 && two == 4) {
                if (nextLadoff.getGiftList() != null && !nextLadoff.getGiftList().isEmpty()) {
                    for (Gift gift : nextLadoff.getGiftList()) {
                        giftName.append(" " + gift.getGiftName());
                    }
                }
            }

            value.append("还需");

            if (activity.getNextGapAmt() > 0) {
                value.append(activity.getNextGapAmt() + "元，");
            }

            if (activity.getNextGapNum() > 0) {
                value.append(activity.getNextGapNum() + "件，");
            }

            value.append(
                    ONE_TWO[one][two]
                            .replace("&",
                                ONE_TWO[0][two]
                                        .replace("$", offer).replace("#", giftName.toString())));

            return value.toString();
        } catch (Exception e) {
            return "";
        }
    }

}
