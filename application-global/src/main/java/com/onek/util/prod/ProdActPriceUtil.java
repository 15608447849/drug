package com.onek.util.prod;

import global.IceRemoteUtil;
import redis.provide.RedisStringProvide;
import redis.util.RedisUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static global.IceRemoteUtil.calcSingleProdActPrize;

public class ProdActPriceUtil {

    private static final String key = "_currprize_version";

    private static transient HashMap<Long, Long> versionMap = new HashMap<>();
    private static HashMap<Long, Double> prizeMap = new HashMap<>();

    private static transient HashMap<Long, Long> versionIntervalMap = new HashMap<>();
    private static HashMap<Long, Double[]> prizeIntervalMap = new HashMap<>();

    public static double getActPrizeBySku(long actcode,long sku,double vatp){
        RedisStringProvide rs = RedisUtil.getStringProvide();
        long v = rs.get(key) != null ? Long.parseLong(rs.get(key)) : 0;

        Long subVersion = versionMap.containsKey(sku) ? versionMap.get(sku) : 0L;
        if(v > subVersion || subVersion == 0){
            ProdPriceEntity entity = IceRemoteUtil.calcSingleProdActPrize(actcode, sku, vatp);
            if(entity != null){
                prizeMap.put(sku,entity.getActprice());
            }else{
                prizeMap.put(sku, vatp);
            }
            versionMap.put(sku, v);
        }

        return prizeMap.get(sku);
    }

    public static List<ProdPriceEntity> getActPrizeByMutiSku(long actcode, List<ProdPriceEntity> skuPriceList){
        RedisStringProvide rs = RedisUtil.getStringProvide();
        long v = rs.get(key) != null ? Long.parseLong(rs.get(key)) : 0;

        List<ProdPriceEntity> resultList = new ArrayList<>();
        if(skuPriceList != null){
            for(ProdPriceEntity prodPriceEntity : skuPriceList){
                long sku = prodPriceEntity.getSku();
                Long subVersion = versionMap.containsKey(sku) ? versionMap.get(sku) : 0L;
                if(v > subVersion || subVersion == 0){
                    ProdPriceEntity [] array = IceRemoteUtil.calcMultiProdActPrize(actcode, skuPriceList);
                    for(ProdPriceEntity calcEntity : array){
                        if(calcEntity != null){
                            prizeMap.put(sku,calcEntity.getActprice());
                        }else{
                            prizeMap.put(sku, prodPriceEntity.getVatp());
                        }

                        versionMap.put(sku, v);
                        resultList.add(calcEntity);
                    }

                }

            }
        }


        return resultList;
    }


    public static Double [] getActIntervalPrizeBySku(long sku,double vatp){
        RedisStringProvide rs = RedisUtil.getStringProvide();
        long v = rs.get(key) != null ? Long.parseLong(rs.get(key)) : 0;

        Long subVersion = versionIntervalMap.containsKey(sku) ? versionIntervalMap.get(sku) : 0L;
        if(v > subVersion || subVersion == 0){
            ProdPriceEntity entity = IceRemoteUtil.calcSingleProdActIntervalPrize(sku, vatp);
            if(entity != null){
                prizeIntervalMap.put(sku, new Double[]{entity.getMinactprize(), entity.getMaxactprize()});
            }else{
                prizeIntervalMap.put(sku, new Double[]{vatp, vatp});
            }
            versionIntervalMap.put(sku, v);
        }

        return prizeIntervalMap.get(sku);
    }

}
