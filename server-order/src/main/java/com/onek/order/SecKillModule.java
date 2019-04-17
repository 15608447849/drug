package com.onek.order;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.calculate.ActivityFilterService;
import com.onek.calculate.entity.IDiscount;
import com.onek.calculate.entity.Product;
import com.onek.calculate.filter.ActivitiesFilter;
import com.onek.calculate.filter.QualFilter;
import com.onek.context.AppContext;
import com.onek.context.UserSession;
import com.onek.entitys.Result;
import com.onek.util.RedisGlobalKeys;
import com.onek.util.stock.RedisStockUtil;
import global.GenIdUtil;
import redis.util.RedisUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 11842
 * @version 1.1.1
 * @description 秒杀模块
 * @time 2019/4/16 11:14
 **/
public class SecKillModule {

    @UserPermission(ignore = true)
    public Result beforeSecKill(AppContext appContext) {
        UserSession userSession = appContext.getUserSession();
        int compid = userSession.compId;
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        long sku = jsonObject.get("sku").getAsLong();
        long actno = jsonObject.get("actno").getAsLong();
        List<Product> products = new ArrayList<>();
        Product product = new Product();
        product.setSku(sku);

        boolean flag = false;
        List<IDiscount> discounts = new ActivityFilterService(new ActivitiesFilter[]{new QualFilter(compid)}).getCurrentActivities(products);
        if(discounts != null && discounts.size() > 0){
            for(IDiscount discount : discounts){
                long discountNo = discount.getDiscountNo();
                if(discountNo == actno){
                    flag = true;
                    break;
                }
            }
        }
        if(!flag){
            return new Result().fail("用户没有资格参加!", null);
        }
        long unqid = GenIdUtil.getUnqId();
        RedisUtil.getStringProvide().set(RedisGlobalKeys.SECKILL_TOKEN_PREFIX  +compid, unqid);
        return new Result().success(unqid);
    }


    @UserPermission(ignore = true)
    public Result attendSecKill(AppContext appContext) {
        UserSession userSession = appContext.getUserSession();
        int compid = userSession.compId;
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        long sku = jsonObject.get("sku").getAsLong();
        long actno = jsonObject.get("actno").getAsLong();
        long unqid = jsonObject.get("unqid").getAsLong();
        int stock = jsonObject.get("stock").getAsInt();

        String key = RedisUtil.getStringProvide().get(RedisGlobalKeys.SECKILL_TOKEN_PREFIX + compid);
        if(unqid != Long.parseLong(key)){
            return new Result().fail("请勿重复提交!", null);
        }
        boolean isEnough = RedisStockUtil.deductionSecKillStock(sku, compid, stock);
        if(!isEnough){
            return new Result().fail("库存不够参加!", null);
        }
        return new Result().success(1);
    }
}
