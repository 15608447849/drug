package com.onek.queue.delay;

import com.alibaba.fastjson.JSONObject;
import redis.util.RedisUtil;

import java.util.ArrayList;
import java.util.List;

public class RedisDelayedHandler<D extends IDelayedObject> extends DelayedHandler<D> {
    private String redisHead;

    public RedisDelayedHandler(String redisHead, long delayTime,
                               IDelayedHandler<D> handlerCall) {
        super(delayTime, handlerCall);
        this.redisHead = redisHead;

        loadRedisData();
    }


    public RedisDelayedHandler(String redisHead, long delayTime,
                               IDelayedHandler<D> handlerCall,
                               TIME_TYPE time_type) {
        super(delayTime, handlerCall, time_type);
        this.redisHead = redisHead;

        loadRedisData();
    }

    protected void loadRedisData() {
        execute(() -> {
            List<D> delayedList = getDelayedList();

            if (!delayedList.isEmpty()) {
                for (D delayed : delayedList) {
                    super.addToQueue(delayed);
                }
            }
        });
    }

    private List<D> getDelayedList() {
        List<D> result = new ArrayList<>();

        List<String> allVals = RedisUtil.getHashProvide().getAllVals(this.redisHead);

        if (allVals != null && !allVals.isEmpty()) {
            for (int i = 0; i < allVals.size(); i++) {
                result.add((D) JSONObject.parse(allVals.get(i)));
            }
        }

        return result;
    }

    @Override
    protected void addSuccess(D delayed) {
        RedisUtil.getHashProvide().putElement(
                this.redisHead, delayed.getUnqKey(),
                JSONObject.toJSONString(delayed));
    }

    @Override
    protected void removeSuccess(D delayed) {
        RedisUtil.getHashProvide().delByKey(this.redisHead, delayed.getUnqKey());
    }

}
