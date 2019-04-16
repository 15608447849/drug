package com.onek.discount;

import com.alibaba.fastjson.JSONArray;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.discount.calculate.entity.DiscountResult;
import com.onek.discount.calculate.entity.IDiscount;
import com.onek.discount.calculate.entity.IProduct;
import com.onek.discount.calculate.entity.Product;
import com.onek.discount.calculate.filter.*;
import com.onek.discount.calculate.service.ActivityCalculateService;
import com.onek.discount.calculate.service.ActivityFilterService;
import com.onek.entitys.Result;
import com.onek.util.prod.ProdEntity;
import com.onek.util.prod.ProdInfoStore;
import com.onek.util.prod.ProdPriceEntity;
import util.GsonUtils;
import util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

public class DiscountCalcModule {

    @UserPermission(ignore = true)
    public Result calcSingleProdActPrize(AppContext appContext) {

        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        long actcode = json.get("actcode").getAsLong();
        double vatp = json.get("vatp").getAsLong();
        long sku = json.get("sku").getAsLong();

        List<IProduct> products = new ArrayList<>();
        Product product1 = new Product();
        product1.setCurrentPrice(vatp * 1);
        product1.setNums(1);
        product1.setOriginalPrice(vatp);
        product1.setSku(sku);
        products.add(product1);

        double actprize = 0D;
        List<IDiscount> discounts = new ActivityFilterService(null).getCurrentActivities(products);
        new ActivityCalculateService().calculate(discounts);
        for(IDiscount discount : discounts){
            if(discount.getActNo() == actcode){
                List<IProduct> discountProduct = discount.getProductList();
                for(IProduct product : discountProduct){
                    if(sku == product.getSKU()){
                        actprize = product.getCurrentPrice();
                    }
                }

            }

        }
        ProdPriceEntity entity = new ProdPriceEntity();
        entity.setSku(sku);
        entity.setVatp(vatp);
        entity.setActprice(actprize);
        return new Result().success(GsonUtils.javaBeanToJson(entity));
    }

    @UserPermission(ignore = true)
    public Result calcMultiProdActPrize(AppContext appContext) {

        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        long actcode = json.get("actcode").getAsLong();
        JsonArray jsonArray = json.getAsJsonArray("skulist");
        List<IProduct> products = new ArrayList<>();
        if(jsonArray != null && jsonArray.size() > 0){
            Iterator<JsonElement> it = jsonArray.iterator();
            while (it.hasNext()) {
                JsonElement elem = it.next();
                Product product = new Product();
                product.setCurrentPrice(elem.getAsJsonObject().get("vatp").getAsDouble() * 1);
                product.setNums(1);
                product.setOriginalPrice(elem.getAsJsonObject().get("vatp").getAsDouble());
                product.setSku(elem.getAsJsonObject().get("sku").getAsLong());
                products.add(product);

            }
        }

        ProdPriceEntity[] array = new ProdPriceEntity[jsonArray.size()];
        List<IDiscount> discounts = new ActivityFilterService(null).getCurrentActivities(products);
        new ActivityCalculateService().calculate(discounts);
        for(IDiscount discount : discounts){
            if(discount.getActNo() == actcode){
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
        return new Result().success(GsonUtils.javaBeanToJson(array));
    }

    @UserPermission(ignore = true)
    public Result calcSingleProdActIntervalPrize(AppContext appContext) {

        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        double vatp = json.get("vatp").getAsLong();
        long sku = json.get("sku").getAsLong();

        List<IProduct> products = new ArrayList<>();
        Product product1 = new Product();
        product1.setCurrentPrice(vatp *1);
        product1.setNums(1);
        product1.setOriginalPrice(vatp);
        product1.setSku(sku);
        products.add(product1);

        double actprize = 0D;
        double minprize = 0D;
        double maxprize = 0D;
        List<IDiscount> discounts = new ActivityFilterService(null).getCurrentActivities(products);
        new ActivityCalculateService().calculate(discounts);
        List<Long> actList = new ArrayList<>();
        for(IDiscount discount : discounts){
            if(discount.getPriority() !=0){
                actList.add(discount.getActNo());
            }
            List<IProduct> discountProduct = discount.getProductList();
            for(IProduct product : discountProduct){
                if(sku == product.getSKU()){
                    actprize = product.getCurrentPrice();
                    if(minprize <= actprize || minprize == 0){
                        minprize = actprize;
                    }
                    if(maxprize >= actprize || maxprize == 0){
                        maxprize = actprize;
                    }
                }
            }

        }

        ProdPriceEntity entity = new ProdPriceEntity();
        entity.setSku(sku);
        entity.setVatp(vatp);
        entity.setActprice(actprize);
        entity.setMinactprize(minprize);
        entity.setMaxactprize(maxprize);
        if(actList.size() == 1){
            entity.setActcode(actList.get(0));
        }

        return new Result().success(GsonUtils.javaBeanToJson(entity));
    }


    public Result getProdsPrice(AppContext appContext) {
        String json = appContext.param.json;

        if (StringUtils.isEmpty(json)) {
            return new Result().fail("参数为空");
        }

        List<Product> tempProds = JSONArray.parseArray(json, Product.class);
//        ProdEntity pes;
//        for (Product prod : tempProds) {
//            pes = ProdInfoStore.getProdBySku(prod.getSKU());
//            prod.autoSetCurrentPrice(pes.getVatp(), prod.getNums());
//        }

        List<IDiscount> activityList =
                new ActivityFilterService(
                        new ActivitiesFilter[] {
                            new TypeFilter(),
                            new CycleFilter(),
                            new QualFilter(appContext.getUserSession()),
                            new PriorityFilter(),
                        }).getCurrentActivities(tempProds);

        TreeMap<Integer, List<IDiscount>> pri_act =
                new TreeMap<>((o1, o2) -> o2 - o1);

        int priority;
        List<IDiscount> tempDiscount;
        for (IDiscount discount : activityList) {
            priority = discount.getPriority();

            tempDiscount = pri_act.get(priority);

            if (tempDiscount == null) {
                tempDiscount = new ArrayList<>();
                pri_act.put(priority, tempDiscount);
            }

            tempDiscount.add(discount);
        }


        for (List<IDiscount> discounts : pri_act.values()) {
            new ActivityCalculateService().calculate(discounts);
        }

        DiscountResult result = new DiscountResult(activityList);

        return new Result().success(result);
    }

}
