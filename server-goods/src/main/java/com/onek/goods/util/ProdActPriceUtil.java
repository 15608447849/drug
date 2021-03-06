package com.onek.goods.util;

import com.onek.calculate.entity.IDiscount;
import com.onek.calculate.entity.IProduct;
import com.onek.calculate.entity.Product;
import com.onek.goods.calculate.ActivityCalculateService;
import com.onek.goods.calculate.ActivityFilterService;
import com.onek.goods.service.PromLoadOffService;
import com.onek.goods.service.PromRuleService;
import com.onek.goods.service.PromTimeService;
import com.onek.util.RedisGlobalKeys;
import com.onek.util.prod.ProdPriceEntity;
import org.hyrdpf.util.LogUtil;
import redis.provide.RedisStringProvide;
import redis.util.RedisUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProdActPriceUtil {

    public static String key = RedisGlobalKeys.ACTVERSION;

    private static transient Map<Long, Long> versionMap = new ConcurrentHashMap<>();
    private static Map<Long, Double> prizeMap = new ConcurrentHashMap<>();

    private static transient Map<Long, Long> versionIntervalMap = new ConcurrentHashMap<>();
    private static Map<Long,  ProdPriceEntity> prizeIntervalMap = new ConcurrentHashMap<>();

    private static transient Map<Long, Long> versionRuleMap = new ConcurrentHashMap<>();
    private static Map<Long,  Integer> prodRuleMap = new ConcurrentHashMap<>();

    private static transient Map<Long, Long> versionTimeMap = new ConcurrentHashMap<>();
    private static Map<Long,  List<String[]>> promTimeMap = new ConcurrentHashMap<>();


    private static Map<Long, Long> versionLadOffMap = new ConcurrentHashMap<>();
    private static Map<Long, List<String>> promLadOffMap = new ConcurrentHashMap<>();

    private static PromTimeService timeService = new PromTimeService();

    private static PromRuleService ruleService = new PromRuleService();

    private static PromLoadOffService ladService = new PromLoadOffService();

    private static long getVersion(){
        RedisStringProvide rs = RedisUtil.getStringProvide();
        long v = rs.get(key) != null ? Long.parseLong(rs.get(key)) : 0;
        if(v == 0){
            rs.set(key, 1);
            v = 1;
        }
        return v;

    }

    public static double getActPrizeBySku(long actcode,long sku,double vatp){
        long v = getVersion();
        Long subVersion = versionMap.containsKey(sku) ? versionMap.get(sku) : 0L;
        if(v > subVersion || subVersion == 0){
            ProdPriceEntity entity = calcSingleProdActPrize(actcode, vatp, sku );
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
        long v = getVersion();
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
                ProdPriceEntity [] array = calcMultiProdActPrize(actcode, skuPriceList);
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
        long v = getVersion();

        Long subVersion = versionIntervalMap.containsKey(sku) ? versionIntervalMap.get(sku) : 0L;
        if(v > subVersion || subVersion == 0){
            ProdPriceEntity entity = calcSingleProdActIntervalPrize(sku, vatp);
            if(entity != null){
                prizeIntervalMap.put(sku, entity);
            }else{
                if(entity == null){
                    entity = new ProdPriceEntity();
                    entity.setSku(sku);
                    entity.setVatp(vatp);
                    entity.setMinactprize(vatp);
                    entity.setMaxactprize(vatp);
                }
                prizeIntervalMap.put(sku, entity);
            }
            versionIntervalMap.put(sku, v);
        }

        return prizeIntervalMap.get(sku);
    }

    public static int getRuleBySku(long sku){
        long v = getVersion();
        Long subVersion = versionRuleMap.containsKey(sku) ? versionRuleMap.get(sku) : 0L;
        if(v > subVersion || subVersion == 0){
            Map<Long, Integer> map = ruleService.getProdRule();
            if(map != null && map.size() > 0){
                for(Long key : map.keySet()){
                    if(key != null){
                        prodRuleMap.put(key, map.get(key));
                        versionRuleMap.put(key, v);
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

    public static List<String[]> getTimesByActcode(long actcode){
        long v = getVersion();
        Long subVersion = versionTimeMap.containsKey(actcode) ? versionTimeMap.get(actcode) : 0L;
        if(v > subVersion || subVersion == 0){
            List<String[]> times = timeService.getTimesByActcode(actcode);
            promTimeMap.put(actcode, times);
            versionTimeMap.put(actcode, v);
        }

        return promTimeMap.get(actcode);
    }


    public static List<String> getLoadOffBySku(long sku){
        long v = getVersion();
        Long subVersion = versionLadOffMap.getOrDefault(sku, 0L);
        if(v > subVersion || subVersion == 0){
            //
            List<String> loadOffs = ladService.getLadOffBySku(sku);
            promLadOffMap.put(sku, loadOffs);
            versionLadOffMap.put(sku, v);
        }
        return promLadOffMap.get(sku);
    }


    public static ProdPriceEntity calcSingleProdActPrize(long actcode,double vatp,long sku) {

        List<IProduct> products = new ArrayList<>();
        Product product1 = new Product();
        product1.setCurrentPrice(vatp * 1);
        product1.setNums(1);
        product1.setOriginalPrice(vatp);
        product1.setSku(sku);
        products.add(product1);

        double actPrize = 0D;
        List<IDiscount> discounts = new ActivityFilterService(null).getCurrentActivities(products);
        new ActivityCalculateService().calculate(discounts);
        for(IDiscount discount : discounts){
            if(discount.getDiscountNo() == actcode){
                List<IProduct> discountProduct = discount.getProductList();
                for(IProduct product : discountProduct){
                    if(sku == product.getSKU()){
                        actPrize = product.getCurrentPrice();
                    }
                }

            }

        }
        ProdPriceEntity entity = new ProdPriceEntity();
        entity.setSku(sku);
        entity.setVatp(vatp);
        entity.setActprice(actPrize);
        return entity;
    }

    public static  ProdPriceEntity[] calcMultiProdActPrize(long actcode, List<ProdPriceEntity> list) {

        List<Product> products = new ArrayList<>();
        if(list != null && list.size() > 0){
            for(ProdPriceEntity entity : list){
                Product product = new Product();
                product.setCurrentPrice(entity.getVatp() * 1);
                product.setNums(1);
                product.setOriginalPrice(entity.getVatp());
                product.setSku(entity.getSku());
                products.add(product);

            }
        }

        ProdPriceEntity[] array = new ProdPriceEntity[list.size()];
        List<IDiscount> discounts = new ActivityFilterService(null).getCurrentActivities(products);
        new ActivityCalculateService().calculate(discounts);
        for(IDiscount discount : discounts){
            if(discount.getDiscountNo() == actcode){
                List<IProduct> discountProduct = discount.getProductList();
                int i = 0;
                for(IProduct product : discountProduct){
                    ProdPriceEntity entity = new ProdPriceEntity();
                    long sku = product.getSKU();
                    entity.setSku(sku);
                    entity.setActprice(product.getCurrentPrice());
                    array[i++] = entity;
                }
            }


        }
        return array;
    }


    public static ProdPriceEntity calcSingleProdActIntervalPrize(long sku,double vatp) {

        List<Product> products = new ArrayList<>();
        Product product1 = new Product();
        product1.setCurrentPrice(vatp *1);
        product1.setNums(1);
        product1.setOriginalPrice(vatp);
        product1.setSku(sku);
        products.add(product1);

        double actPrize = 0D;
        double minPrize = 0D;
        double maxPrize = 0D;
        List<IDiscount> discounts = new ActivityFilterService(null).getCurrentActivities(products);
        new ActivityCalculateService().calculate(discounts);
        List<Long> actList = new ArrayList<>();
        for(IDiscount discount : discounts){
            if(discount.getPriority() !=0){
                actList.add(discount.getDiscountNo());
            }
            List<IProduct> discountProduct = discount.getProductList();
            for(IProduct product : discountProduct){
                if(sku == product.getSKU()){
                    actPrize = product.getCurrentPrice();

                    LogUtil.getDefaultLogger().info("####### " + sku + " ########## " + actPrize);

                    if(minPrize <= actPrize || minPrize == 0){
                        minPrize = actPrize;
                    }
                    if(maxPrize >= actPrize || maxPrize == 0){
                        maxPrize = actPrize;
                    }
                }
            }

        }

        LogUtil.getDefaultLogger().info("####### " + minPrize + " ########## " + maxPrize);

        ProdPriceEntity entity = new ProdPriceEntity();
        entity.setSku(sku);
        entity.setVatp(vatp);
        entity.setActprice(actPrize);
        entity.setMinactprize(minPrize);
        entity.setMaxactprize(maxPrize);
        if(actList.size() == 1){
            entity.setActcode(actList.get(0));
        }
        return entity;
    }
}
