package com.onek.propagation.queue;

import com.alibaba.fastjson.JSONObject;
import com.onek.util.stock.RedisStockUtil;

import java.util.List;

public class DelayStockWorker implements Runnable {

    public DelayStockWorker(List<JSONObject> stockList){
        this.stockList = stockList;
    }

    private List<JSONObject> stockList;

    @Override
    public void run() {

        if(stockList != null && stockList.size() > 0){
            for (JSONObject jsonObject : stockList) {
                String gcode = jsonObject.get("gcode").toString();
                System.out.println("#### delay stock work : [" +gcode+"] ######");
                if(gcode.length() >= 14){
                    long actcode = Long.parseLong(jsonObject.get("actcode").toString());
                    int stock = Integer.parseInt(jsonObject.get("stock").toString());
                    int cstatus = Integer.parseInt(jsonObject.get("cstatus").toString());
                    System.out.println("#### delay stock work : [" +gcode+"] ######");
                    if (cstatus == 0) {
                        RedisStockUtil.setActStock(Long.parseLong(gcode), actcode, stock);
                    }

                    if (cstatus == 1) {
                        RedisStockUtil.clearActStock(Long.parseLong(gcode), actcode);
                    }
                }
            }
        }
    }
}
