package com.onek.queue;

import com.alibaba.fastjson.JSONObject;
import com.onek.order.TranOrderOptModule;
import redis.util.RedisUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.DelayQueue;

public class CancelService {
    private static final String REDIS_HEAD = "_CANCEL_ORDERS";
    private final static CancelService CANCEL_SERVICE;
    private volatile DelayQueue<CancelDelayed> delayQueue;
    private static volatile CancelHandler cancelHandler;

    static {
        CANCEL_SERVICE = new CancelService(getDelayQueue());
    }

    private static DelayQueue<CancelDelayed> getDelayQueue() {
        DelayQueue<CancelDelayed> delayQueue = new DelayQueue<>();

        List<CancelDelayed> cancelDelayeds = getCancelDelayedList();

        if (!cancelDelayeds.isEmpty()) {
            delayQueue.addAll(getCancelDelayedList());
        }

        return delayQueue;
    }

    private static List<CancelDelayed> getCancelDelayedList() {
        List<CancelDelayed> result = new ArrayList<>();

        List<String> allVals = RedisUtil.getHashProvide().getAllVals(REDIS_HEAD);

        if (allVals != null && !allVals.isEmpty()) {
            for (int i = 0; i < allVals.size(); i++) {
                result.add(JSONObject.parseObject(allVals.get(i), CancelDelayed.class));
            }
        }

        return result;
    }

    public static CancelService getInstance() {
        return CANCEL_SERVICE;
    }

    private CancelService(DelayQueue<CancelDelayed> delayQueue) {
        this.delayQueue = delayQueue;

        this.cancelHandler = new CancelHandler();
        this.cancelHandler.setDaemon(true);
        this.cancelHandler.start();
    }

    public void add(CancelDelayed cancelDelayed) {
        boolean addResult = delayQueue.add(cancelDelayed);

        if (addResult) {
            RedisUtil.getHashProvide().putElement(
                REDIS_HEAD, cancelDelayed.getOrderNo(),
                JSONObject.toJSONString(cancelDelayed));
        }
    }

    public void remove(CancelDelayed cancelDelayed) {
        boolean removeResult = delayQueue.remove(cancelDelayed);

        if (removeResult) {
            RedisUtil.getHashProvide().delByKey(REDIS_HEAD, cancelDelayed.getOrderNo());
        }
    }

    static class CancelHandler extends Thread {
        private TranOrderOptModule tranOrderOptModule = new TranOrderOptModule();
        @Override
        public void run() {
            while (true) {
                try {
                    CancelDelayed c = CancelService.getInstance().delayQueue.take();

                    if (c != null) {
//                        tranOrderOptModule.cancelOrder();
                        // TODO 取消订单接口
                        boolean cancelResult = true;

                        if (cancelResult) {
                            RedisUtil.getHashProvide().delByKey(REDIS_HEAD, c.getOrderNo());
                        }
//                        System.out.println("Remove --- " + c.getOrderNo());
                    }

                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
