package com.onek.propagation.prod;

import com.alibaba.fastjson.JSONObject;
import com.onek.propagation.queue.DelayStockQueueManager;
import com.onek.propagation.queue.DelayStockWorker;
import com.onek.util.IceRemoteUtil;
import com.onek.util.RedisGlobalKeys;
import com.onek.util.discount.DiscountRuleStore;
import com.onek.util.order.RedisOrderUtil;
import com.onek.util.stock.RedisStockUtil;
import org.hyrdpf.util.LogUtil;
import redis.util.RedisUtil;
import util.MathUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ProdDiscountObserver implements ProdObserver {

    DelayStockQueueManager manager = DelayStockQueueManager.getInstance();

    @Override
    public void update(List<String> list) {

        if (list != null && list.size() > 0) {
            List<JSONObject> failList = new ArrayList<>();

            Map<String,String> actStockMap = new HashMap<>();
            for (String obj : list) {
                JSONObject jsonObject = JSONObject.parseObject(obj);
                if (jsonObject == null || !jsonObject.containsKey("discount")) {
                    continue;
                }
                String gcode = jsonObject.get("gcode").toString();
                if (gcode.length() >= 14) { // 指定商品
                    long actcode = Long.parseLong(jsonObject.get("actcode").toString());
                    int stock = Integer.parseInt(jsonObject.get("stock").toString());
                    int rulecode = Integer.parseInt(jsonObject.get("rulecode").toString());
                    int limitnum = 0;
                    if (jsonObject.containsKey("limitnum")) {
                        limitnum = Integer.parseInt(jsonObject.get("limitnum").toString());
                    }
                    int result = 0;
                    if ((Integer.parseInt(jsonObject.get("cstatus").toString()) & 256) > 0) {
                        if (stock > 0 && stock < 100) {
                            int s = RedisStockUtil.getStock(Long.parseLong(gcode));
                            double rate = MathUtil.exactDiv(stock, 100F).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                            int ns = MathUtil.exactMul(s, rate).setScale(0, BigDecimal.ROUND_HALF_DOWN).intValue();
                            result = RedisStockUtil.setActStock(Long.parseLong(gcode), actcode, ns, limitnum);
                            actStockMap.put(gcode+"", ns+"");
                        }else{
                            RedisStockUtil.setSpecActStock(Long.parseLong(gcode), actcode, limitnum);
                            actStockMap.put(gcode+"", "ALL");
                        }
                    } else {
                        if ((Integer.parseInt(jsonObject.get("cstatus").toString()) & 1) > 0) {
                            result = RedisStockUtil.clearActStock(Long.parseLong(gcode), actcode).intValue();
                            actStockMap.put(gcode+"", "0");

                        } else {
                            result = RedisStockUtil.setActStock(Long.parseLong(gcode), actcode, stock, limitnum);
                            actStockMap.put(gcode+"", stock+"");

                        }
                    }

                    LogUtil.getDefaultLogger().info("++++++@@@@@@@@@@ actcode:[" + actcode + "]; gcode:[" + gcode + "]; stock:[" + stock + "]; cstatus:[" + jsonObject.get("cstatus") + "]  @@@@@@@@@@+++++++");

                    if (result <= 0) {
                        failList.add(jsonObject);
                    }
                }


                if ("0".equals(gcode) || gcode.length() <= 12) { // 全部商品、指定类别
                    int stock = Integer.parseInt(jsonObject.get("stock").toString());
                    long actcode = Long.parseLong(jsonObject.get("actcode").toString());
                    int rulecode = Integer.parseInt(jsonObject.get("rulecode").toString());
                    int limitnum = 0;
                    if (jsonObject.containsKey("limitnum")) {
                        limitnum = Integer.parseInt(jsonObject.get("limitnum").toString());
                    }
                    LogUtil.getDefaultLogger().info("++++++@@@@@@@@@@ actcode:[" + actcode + "]; spu:[" + gcode + "]; stock:[" + stock + "]; cstatus:[" + jsonObject.get("cstatus") + "]  @@@@@@@@@@+++++++");
                    if ((Integer.parseInt(jsonObject.get("cstatus").toString()) & 256) > 0) { // 库存比例
                        if ((Integer.parseInt(jsonObject.get("cstatus").toString()) & 1) > 0) { // 删除
                            List<Long> skuList = IceRemoteUtil.querySkuListByCondition(Long.parseLong(gcode));
                            for (Long sku : skuList) {
                                int result = RedisStockUtil.clearActStock(sku, actcode).intValue();
                                actStockMap.put(sku+"", "0");
                                if (result <= 0) {
                                    JSONObject j = new JSONObject();
                                    j.put("discount", 1);
                                    j.put("gcode", sku);
                                    j.put("cstatus", "1");
                                    j.put("rulecode", rulecode);
                                    j.put("actcode", actcode);
                                    j.put("stock", 0);
                                    j.put("limitnum", limitnum);
                                    failList.add(j);
                                }
                            }
                        }else{
                            if (stock > 0 && stock < 100) {
                                double rate = MathUtil.exactDiv(stock, 100F).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                                List<Long> skuList = IceRemoteUtil.querySkuListByCondition(Long.parseLong(gcode));
                                for (Long sku : skuList) {
                                    int s = RedisStockUtil.getStock(sku);
                                    int result = 0;
                                    if (s > 0) {
                                        int ns = MathUtil.exactMul(s, rate).setScale(0, BigDecimal.ROUND_HALF_DOWN).intValue();
                                        LogUtil.getDefaultLogger().info("++++++@@@@@@@@@@ stockrate actcode:[" + actcode + "]; sku:[" + sku + "]; stock:[" + ns + "]; cstatus:[" + jsonObject.get("cstatus") + "]  @@@@@@@@@@+++++++");
                                        result = RedisStockUtil.setActStock(sku, actcode, ns, limitnum);
                                        actStockMap.put(sku+"", ns+"");

                                        if (result <= 0) {
                                            JSONObject j = new JSONObject();
                                            j.put("discount", 1);
                                            j.put("gcode", sku);
                                            j.put("cstatus", "0");
                                            j.put("rulecode", rulecode);
                                            j.put("actcode", actcode);
                                            j.put("stock", ns);
                                            j.put("limitnum", limitnum);
                                            failList.add(j);
                                        }
                                    }
                                }
                            }else{
                                RedisStockUtil.setSpecActStock(Long.parseLong(gcode), actcode, limitnum);
                                actStockMap.put(gcode+"", "ALL");
                            }
                        }

                    } else {
                        List<Long> skuList = IceRemoteUtil.querySkuListByCondition(Long.parseLong(gcode));
                        for (Long sku : skuList) {
                            int s = RedisStockUtil.getStock(sku);
                            int result = 0;
                            if (s > 0) {
                                LogUtil.getDefaultLogger().info("++++++@@@@@@@@@@ stocknum actcode:[" + actcode + "]; sku:[" + sku + "]; stock:[" + s + "]; cstatus:[" + jsonObject.get("cstatus") + "]  @@@@@@@@@@+++++++");
                                if ((Integer.parseInt(jsonObject.get("cstatus").toString()) & 1) > 0) {
                                    result = RedisStockUtil.clearActStock(sku, actcode).intValue();
                                    actStockMap.put(sku+"", "0");

                                }else{
                                    result = RedisStockUtil.setActStock(sku, actcode, stock, limitnum);
                                    actStockMap.put(sku+"", stock+"");
                                }

                                if (result <= 0) {
                                    JSONObject j = new JSONObject();
                                    j.put("discount", 1);
                                    j.put("gcode", sku);
                                    if ((Integer.parseInt(jsonObject.get("cstatus").toString()) & 1) > 0) {
                                        j.put("cstatus", "1");
                                    }else{
                                        j.put("cstatus", "0");
                                    }
                                    j.put("rulecode", rulecode);
                                    j.put("actcode", actcode);
                                    j.put("stock", s);
                                    j.put("limitnum", limitnum);
                                    failList.add(j);
                                }
                            }
                        }
                    }
                }
            }

            if(actStockMap != null && actStockMap.size() > 0){
                JSONObject jsonObject = JSONObject.parseObject(list.get(0));
                String act_code = jsonObject.get("actcode").toString();
                RedisUtil.getHashProvide().putHashMap(RedisGlobalKeys.ACT_PREFIX + act_code, actStockMap);
            }


            // redis设置失败,调用延时队列重试一次,1秒后重试
            if (failList != null && failList.size() > 0) {
                LogUtil.getDefaultLogger().info("++++++@@@@@@@@@@ fail size:[" + failList.size() + "]  @@@@@@@@@@+++++++");
                DelayStockWorker worker = new DelayStockWorker(failList);
                manager.put(worker, 1000, TimeUnit.MILLISECONDS);
            }
        }
    }

}
