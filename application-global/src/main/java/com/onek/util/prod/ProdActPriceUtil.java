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
    private static HashMap<Long,  ProdPriceEntity> prizeIntervalMap = new HashMap<>();

    private static transient HashMap<Long, Long> versionRuleMap = new HashMap<>();
    private static HashMap<Long,  Integer> prodRuleMap = new HashMap<>();

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
            boolean needLoad = false;
            for(ProdPriceEntity skuPrice : skuPriceList){
                Long subVersion = versionMap.containsKey(skuPrice.getSku()) ? versionMap.get(skuPrice.getSku()) : 0L;
                if(v > subVersion || subVersion == 0){
                    needLoad = true;
                    break;
                }
            }

            if(needLoad){
                ProdPriceEntity [] array = IceRemoteUtil.calcMultiProdActPrize(actcode, skuPriceList);
                for(ProdPriceEntity calcEntity : array){
                    if(calcEntity != null){
                        prizeMap.put(calcEntity.getSku(),calcEntity.getActprice());
                    }

                    versionMap.put(calcEntity.getSku(), v);
                    resultList.add(calcEntity);
                }
                return resultList;
            }
            for(ProdPriceEntity prodPriceEntity : skuPriceList){
                ProdPriceEntity priceEntity = new ProdPriceEntity();
                priceEntity.setSku(prodPriceEntity.getSku());
                priceEntity.setActprice(prizeMap.get(prodPriceEntity.getSku()));
                resultList.add(priceEntity);

            }
        }


        return resultList;
    }


    public static ProdPriceEntity getActIntervalPrizeBySku(long sku,double vatp){
        RedisStringProvide rs = RedisUtil.getStringProvide();
        long v = rs.get(key) != null ? Long.parseLong(rs.get(key)) : 0;

        Long subVersion = versionIntervalMap.containsKey(sku) ? versionIntervalMap.get(sku) : 0L;
        if(v > subVersion || subVersion == 0){
            ProdPriceEntity entity = IceRemoteUtil.calcSingleProdActIntervalPrize(sku, vatp);
            if(entity != null){
                prizeIntervalMap.put(sku, entity);
            }else{
                entity.setMinactprize(vatp);
                entity.setMaxactprize(vatp);
                prizeIntervalMap.put(sku, entity);
            }
            versionIntervalMap.put(sku, v);
        }

        return prizeIntervalMap.get(sku);
    }

    public static int getRuleBySku(long sku){
        RedisStringProvide rs = RedisUtil.getStringProvide();
        long v = rs.get(key) != null ? Long.parseLong(rs.get(key)) : 0;
        Long subVersion = versionRuleMap.containsKey(sku) ? versionRuleMap.get(sku) : 0L;
        if(v > subVersion || subVersion == 0){
            HashMap<Object, Object> map = IceRemoteUtil.getEffectiveRule();
            if(map != null && map.size() > 0){
                for(Object key : map.keySet()){
                    if(key != null){
                        prodRuleMap.put(Long.parseLong(key.toString()), ((int)Double.parseDouble(map.get(key).toString())));
                        versionRuleMap.put(Long.parseLong(key.toString()), v);
                    }
                }
            }

        }

        if(!prodRuleMap.containsKey(sku)){
            prodRuleMap.put(sku, 0);
            versionRuleMap.put(sku, v);
        }

        return prodRuleMap.get(sku);
    }

}
