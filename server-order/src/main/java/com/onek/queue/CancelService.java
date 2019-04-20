package com.onek.queue;

import com.alibaba.fastjson.JSONObject;
import com.onek.order.TranOrderOptModule;
import redis.util.RedisUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class CancelService {
    static {
        EXECUTOR = Executors.newFixedThreadPool(10);
    }

    private static final Executor EXECUTOR;
    private static final String REDIS_HEAD = "_CANCEL_ORDERS";
    private final static CancelService CANCEL_SERVICE = new CancelService();
    private volatile DelayQueue<CancelDelayed> delayQueue;
    private static volatile CancelHandler cancelHandler;


    private void addRedisDataInQueue() {
        EXECUTOR.execute(() -> {
            try {
                List<CancelDelayed> cancelDelayeds = getCancelDelayedList();

                if (!cancelDelayeds.isEmpty()) {
                    CancelService.this.delayQueue.addAll(cancelDelayeds);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private List<CancelDelayed> getCancelDelayedList() {
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

    private CancelService() {
        this.delayQueue = new DelayQueue<>();

        addRedisDataInQueue();

        this.cancelHandler = new CancelHandler();
        this.cancelHandler.setDaemon(true);
        this.cancelHandler.start();
    }

    public void add(CancelDelayed cancelDelayed) {
        EXECUTOR.execute(() -> {
            boolean addResult = delayQueue.add(cancelDelayed);

            if (addResult) {
                RedisUtil.getHashProvide().putElement(
                        REDIS_HEAD, cancelDelayed.getOrderNo(),
                        JSONObject.toJSONString(cancelDelayed));
            }
        });
    }

    public void remove(String orderno) {
        EXECUTOR.execute(() -> {
                boolean removeResult = delayQueue.remove(orderno);

                if (removeResult) {
                    RedisUtil.getHashProvide().delByKey(REDIS_HEAD, orderno);
                }
        });
    }

    static class CancelHandler extends Thread {
        private TranOrderOptModule tranOrderOptModule = new TranOrderOptModule();

        @Override
        public void run() {
            while (true) {
                try {
                    CancelDelayed cancelDelayed = CancelService.getInstance().delayQueue.take();

                    if (cancelDelayed != null) {
                        // TODO 取消订单接口
                        boolean cancelResult =
                                this.tranOrderOptModule.cancelOrder(cancelDelayed.getOrderNo(), cancelDelayed.getCompid());

                        if (cancelResult) {
                            RedisUtil.getHashProvide().delByKey(REDIS_HEAD, cancelDelayed.getOrderNo());
                        }
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
