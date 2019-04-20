package com.onek.propagation.prod;

import com.alibaba.fastjson.JSONObject;
import com.onek.propagation.queue.DelayStockQueueManager;
import com.onek.propagation.queue.DelayStockWorker;
import com.onek.util.stock.RedisStockUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ProdDiscountObserver implements ProdObserver {

    DelayStockQueueManager manager = DelayStockQueueManager.getInstance();

    @Override
    public void update(List<String> list) {
        if (list != null && list.size() > 0) {
            if (list != null && list.size() > 0) {
                List<JSONObject> failList = new ArrayList<>();
                for (String obj : list) {
                    JSONObject jsonObject = JSONObject.parseObject(obj);
                    if (jsonObject == null || !jsonObject.containsKey("discount")) {
                        continue;
                    }
                    String gcode = jsonObject.get("gcode").toString();
                    if(gcode.length() >= 14){
                        long actcode = Long.parseLong(jsonObject.get("actcode").toString());
                        int stock = Integer.parseInt(jsonObject.get("stock").toString());
                        int result = 0;
                        if (Integer.parseInt(jsonObject.get("cstatus").toString()) == 0) {
                            result = RedisStockUtil.setActStock(Long.parseLong(gcode), actcode, stock);

                        }

                        if (Integer.parseInt(jsonObject.get("cstatus").toString()) == 1) {
                            result = RedisStockUtil.clearActStock(Long.parseLong(gcode), actcode);

                        }

                        if(result <= 0){
                            failList.add(jsonObject);
                        }
                    }

                }

                // redis设置失败,调用延时队列重试一次,1秒后重试
                if (failList != null && failList.size() > 0) {
                    DelayStockWorker worker = new DelayStockWorker(failList);
                    manager.put(worker, 1000, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

}
