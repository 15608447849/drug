package com.onek.queue.delay;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import redis.util.RedisUtil;

import java.util.ArrayList;
import java.util.List;

public class RedisDelayedHandler<D extends IDelayedObject> extends DelayedHandler<D> {
    private String redisHead;
    private Class<D> clazz;

    public RedisDelayedHandler(String redisHead, long delayTime,
                               IDelayedHandler<D> handlerCall,
                               Class<D> clazz) {
        super(delayTime, handlerCall);
        this.redisHead = redisHead;
        this.clazz = clazz;

        loadRedisData();
    }


    public RedisDelayedHandler(String redisHead, long delayTime,
                               IDelayedHandler<D> handlerCall,
                               TIME_TYPE time_type,
                               Class<D> clazz) {
        super(delayTime, handlerCall, time_type);
        this.redisHead = redisHead;
        this.clazz = clazz;

        loadRedisData();
    }

    protected void loadRedisData() {
        execute(() -> {
            try {
                List<D> delayedList = getDelayedList();

                if (!delayedList.isEmpty()) {
                    for (D delayed : delayedList) {
                        super.addToQueue(delayed);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private List<D> getDelayedList() {
        List<D> result = new ArrayList<>();

        List<String> allVals = RedisUtil.getHashProvide().getAllVals(this.redisHead);

        if (allVals != null && !allVals.isEmpty()) {
            for (int i = 0; i < allVals.size(); i++) {
                result.add(JSONObject.parseObject(allVals.get(i), this.clazz));
            }
        }

        return result;
    }

    @Override
    protected void addSuccess(D delayed) {
        try {
            RedisUtil.getHashProvide().putElement(
                    this.redisHead, delayed.getUnqKey(),
                    JSONObject.toJSONString(delayed));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void removeSuccess(D delayed) {
        try {
            RedisUtil.getHashProvide().delByKey(this.redisHead, delayed.getUnqKey());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
