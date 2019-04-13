package com.onek.discount;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.discount.aaa.ccb.entity.IDiscount;
import com.onek.discount.aaa.ccb.entity.IProduct;
import com.onek.discount.aaa.ccb.entity.Product;
import com.onek.discount.aaa.ccb.service.ActivityCalculateService;
import com.onek.discount.aaa.ccb.service.ActivityFilterService;
import com.onek.entitys.Result;
import com.onek.util.prod.ProdPriceEntity;
import util.GsonUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
        for(IDiscount discount : discounts){
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
        return new Result().success(GsonUtils.javaBeanToJson(entity));
    }
}
