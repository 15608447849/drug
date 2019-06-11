package com.onek.report.data;

import constant.DSMConst;
import dao.BaseDAO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class MarketStoreData {

    public static final Map<String, Integer> ETM_CITY_MAP = new HashMap<>();
    public static final Map<String, Integer> ETM_AREA_MAP = new HashMap<>();

    public static final Map<String, Integer> CHAIN_CITY_MAP = new HashMap<>();
    public static final Map<String, Integer> CHAIN_AREA_MAP = new HashMap<>();

    public static final Map<String, Integer> OTHER_CITY_MAP = new HashMap<>();
    public static final Map<String, Integer> OTHER_AREA_MAP = new HashMap<>();

    private static final String PATTERN_CITY = "^[1-9][0-9][0-9]{2}[0]{8}$";
    private static final String PATTERN_AREA = "^[1-9][0-9][0-9]{4}[0]{6}$";

    private static String CONFIG_SQL = "select saddrcode,count(1) as storenum,cstatus from {{?"+ DSMConst.TB_MARKET_STORE +"}} where cstatus&1 = 0 group by saddrcode,cstatus";

    public static void init(){
        List<Object[]> list = BaseDAO.getBaseDAO().queryNative(CONFIG_SQL);
        if(list != null && list.size() > 0){
            for(Object[] arr : list){
                String code = arr[0].toString();
                int num = Integer.parseInt(arr[1].toString());
                String cstatus = arr[2].toString();

                boolean isArea = Pattern.matches(PATTERN_AREA, code);
                if(isArea){
                        if((Integer.parseInt(cstatus) & 64) > 0){
                            if(ETM_AREA_MAP.keySet().contains(code)){
                                int _n = ETM_AREA_MAP.get(code);
                                ETM_AREA_MAP.put(code, new Integer(_n + num));
                            }else{
                                ETM_AREA_MAP.put(code, new Integer(num));
                            }
                            String parentCode = code.substring(0, 4) + "00000000";
                            if(ETM_CITY_MAP.keySet().contains(parentCode)){
                                int _n = ETM_CITY_MAP.get(parentCode);
                                ETM_CITY_MAP.put(parentCode, new Integer(_n + num));
                            }else{
                                ETM_CITY_MAP.put(parentCode, new Integer(num));
                            }
                        }else if((Integer.parseInt(cstatus) & 512) > 0){
                            if(CHAIN_AREA_MAP.keySet().contains(code)){
                                int _n = CHAIN_AREA_MAP.get(code);
                                CHAIN_AREA_MAP.put(code, new Integer(_n + num));
                            }else{
                                CHAIN_AREA_MAP.put(code, new Integer(num));
                            }
                            String parentCode = code.substring(0, 4) + "00000000";
                            if(CHAIN_CITY_MAP.keySet().contains(parentCode)){
                                int _n = CHAIN_CITY_MAP.get(parentCode);
                                CHAIN_CITY_MAP.put(parentCode, new Integer(_n + num));
                            }else{
                                CHAIN_CITY_MAP.put(parentCode, new Integer(num));
                            }
                        }else{
                            if(OTHER_AREA_MAP.keySet().contains(code)){
                                int _n = OTHER_AREA_MAP.get(code);
                                OTHER_AREA_MAP.put(code, new Integer(_n + num));
                            }else{
                                OTHER_AREA_MAP.put(code, new Integer(num));
                            }
                            String parentCode = code.substring(0, 4) + "00000000";
                            if(OTHER_CITY_MAP.keySet().contains(parentCode)){
                                int _n = OTHER_CITY_MAP.get(parentCode);
                                OTHER_CITY_MAP.put(parentCode, new Integer(_n + num));
                            }else{
                                OTHER_CITY_MAP.put(parentCode, new Integer(num));
                            }
                        }


                }else{
                    boolean isCity = Pattern.matches(PATTERN_CITY, code);
                    if(isCity){
                        if((Integer.parseInt(cstatus) & 64) > 0){
                            if(ETM_CITY_MAP.keySet().contains(code)){
                                int _n = ETM_CITY_MAP.get(code);
                                ETM_CITY_MAP.put(code, new Integer(_n + num));
                            }else{
                                ETM_CITY_MAP.put(code, new Integer(num));
                            }
                        }else if((Integer.parseInt(cstatus) & 512) > 0){
                            if(CHAIN_CITY_MAP.keySet().contains(code)){
                                int _n = CHAIN_CITY_MAP.get(code);
                                CHAIN_CITY_MAP.put(code, new Integer(_n + num));
                            }else{
                                CHAIN_CITY_MAP.put(code, new Integer(num));
                            }
                        }else{
                            if(OTHER_CITY_MAP.keySet().contains(code)){
                                int _n = OTHER_CITY_MAP.get(code);
                                OTHER_CITY_MAP.put(code, new Integer(_n + num));
                            }else{
                                OTHER_CITY_MAP.put(code, new Integer(num));
                            }
                        }
                    }
                }
            }
        }


    }

    public static int getEtmByAreac(String areac){
        if(areac.contains("00000000")){
            if(ETM_CITY_MAP.containsKey(areac)){
                return ETM_CITY_MAP.get(areac);
            }
            return 0;
        }else{
            if(ETM_AREA_MAP.containsKey(areac)){
                return ETM_AREA_MAP.get(areac);
            }
            return 0;
        }

    }

    public static int getChainByAreac(String areac){
        if(areac.contains("00000000")) {
            if (CHAIN_CITY_MAP.containsKey(areac)) {
                return CHAIN_CITY_MAP.get(areac);
            }
            return 0;
        }else{
            if(CHAIN_AREA_MAP.containsKey(areac)){
                return CHAIN_AREA_MAP.get(areac);
            }
            return 0;
        }
    }

    public static int getOtherByAreac(String areac){
        if(areac.contains("00000000")) {
            if(OTHER_CITY_MAP.containsKey(areac)){
                return OTHER_CITY_MAP.get(areac);
            }
            return 0;
        }else{
            if(OTHER_AREA_MAP.containsKey(areac)){
                return OTHER_AREA_MAP.get(areac);
            }
            return 0;
        }

    }

}
