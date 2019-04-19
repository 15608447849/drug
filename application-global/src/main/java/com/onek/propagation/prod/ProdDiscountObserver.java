package com.onek.propagation.prod;

import com.alibaba.fastjson.JSONObject;
import com.onek.util.stock.RedisStockUtil;

import java.util.ArrayList;
import java.util.List;

public class ProdDiscountObserver implements ProdObserver {

    @Override
    public void update(List<String> list) {
        if (list != null && list.size() > 0) {
            if (list != null && list.size() > 0) {
                for (String obj : list) {
                    JSONObject jsonObject = JSONObject.parseObject(obj);
                    if (jsonObject == null || !jsonObject.containsKey("discount")) {
                        continue;
                    }
                    String gcode = jsonObject.get("gcode").toString();
                    if(gcode.length() >= 14){
                        long actcode = Long.parseLong(jsonObject.get("actcode").toString());
                        int stock = Integer.parseInt(jsonObject.get("stock").toString());
                        if (Integer.parseInt(jsonObject.get("cstatus").toString()) == 0) {
                            RedisStockUtil.setActStock(Long.parseLong(gcode), actcode, stock);
                        }

                        if (Integer.parseInt(jsonObject.get("cstatus").toString()) == 1) {
                            RedisStockUtil.clearActStock(Long.parseLong(gcode), actcode);
                        }
                    }

                }
            }
        }
    }

}
