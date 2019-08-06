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
        put(1114,8192);
    }};


    final static Map<Integer, String> ruleNameMap = new HashMap(){{
        put(1110,"满减现金");
        put(1120,"满减包邮");
        put(1130,"满减折扣");
        put(1210,"满赠返利");
        put(1220,"满赠包邮券");
        put(1230,"满赠折扣券");
        put(1240,"满赠赠品");
        put(2110,"现金券");
        put(2120,"包邮券");
        put(2130,"折扣券");
        put(1113,"秒杀");
        put(1133,"团购");
        put(1114,"套餐");

    }};

    private static String[] FOUR = {"", "每满", "满"};
    private static String[][] ONE_TWO = {
            {"", "$", "包邮", "$折", "#" },
            {"", "减&元", "&", "打&" },
            {"", "返利&", "赠&券", "赠&券", "赠&"}};

    public static int getRuleByBRule(int brule){
        return map.get(brule);
    }

    public static String getRuleByName(int brule){
        return ruleNameMap.get(brule);
    }

    public static String getLadoffDesc(Ladoff ladoff) {
        try {
            if (ladoff == null) {
                return "";
            }

            String codeStr = String.valueOf(ladoff.getOffercode());
            StringBuilder value = new StringBuilder();
            int four = Character.digit(codeStr.charAt(4), 10);
            int one = Character.digit(codeStr.charAt(1), 10);
            int two = Character.digit(codeStr.charAt(2), 10);
            double offerValue = ladoff.getOffer();
            String offer = offerValue + "";

            if (ladoff.isExActivity()) {
                return "不参与返利";
            }

            StringBuilder giftName = new StringBuilder();

            if (one == 2) {
                if (two == 4) {
                    if (ladoff.getGiftList() != null && !ladoff.getGiftList().isEmpty()) {
                        for (Gift gift : ladoff.getGiftList()) {
                            giftName.append(" " + gift.getGiftName());
                        }
                    }
                } else if (two == 1) {
                    if (ladoff.isPercentage()) {
                        offer = (offerValue * 100) + "%";
                    } else {
                        offer = offerValue + "元";
                    }
                }
            }

            value.append(FOUR[four]);

            if (ladoff.getLadnum() > 0) {
                value.append(ladoff.getLadnum() + "件，");
            }

            if (ladoff.getLadamt() > 0) {
                value.append(ladoff.getLadamt() + "元，");
            }

            value.append(
                    ONE_TWO[one][two]
                            .replace("&",
                                    ONE_TWO[0][two]
                                            .replace("$", offer).replace("#", giftName.toString())));

            return value.toString();

        } catch (Exception e) {
//            e.printStackTrace();
            return "";
        }
    }

    public static String getCurrActivityDesc(Activity activity) {
        return getLadoffDesc(activity.getCurrLadoff());
    }

    public static String getGapActivityDesc(Activity activity) {
        try {
            Ladoff nextLadoff = activity.getNextLadoff();

            if (nextLadoff == null) {
                return "";
            }

            String codeStr = String.valueOf(nextLadoff.getOffercode());
            StringBuilder value = new StringBuilder();
            int one = Character.digit(codeStr.charAt(1), 10);
            int two = Character.digit(codeStr.charAt(2), 10);

            double offerValue = nextLadoff.getOffer();
            String offer = offerValue + "";

            StringBuilder giftName = new StringBuilder();


            if (one == 2) {
                if (two == 4) {
                    if (nextLadoff.getGiftList() != null && !nextLadoff.getGiftList().isEmpty()) {
                        for (Gift gift : nextLadoff.getGiftList()) {
                            giftName.append(" " + gift.getGiftName());
                        }
                    }
                } else if (two == 1) {
                    if (nextLadoff.isPercentage()) {
                        offer = (offerValue * 100) + "%";
                    } else {
                        offer = offerValue + "元";
                    }
                }
            }

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
