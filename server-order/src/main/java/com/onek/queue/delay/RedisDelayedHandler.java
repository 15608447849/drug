package com.onek.queue.delay;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import redis.util.RedisUtil;

import java.util.ArrayList;
import java.util.List;

public class RedisDelayedHandler<D extends IDelayedObject> extends DelayedHandler<D> {
    static {
        ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
    }

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
            try {
                List<DelayedObject> delayedList = getDelayedList();

                if (!delayedList.isEmpty()) {
                    for (DelayedObject delayed : delayedList) {
                        super.addToQueue(delayed);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private List<DelayedObject> getDelayedList() {
        List<DelayedObject> result = new ArrayList<>();

        List<String> allVals = RedisUtil.getHashProvide().getAllVals(this.redisHead);


        if (allVals != null && !allVals.isEmpty()) {
            for (int i = 0; i < allVals.size(); i++) {
                result.add(JSON.parseObject(
                        allVals.get(i),
                        new TypeReference<DelayedObject>() {}));
            }
        }

        return result;
    }

    @Override
    protected void addSuccess(DelayedObject delayed) {
        try {
            RedisUtil.getHashProvide().putElement(
                    this.redisHead, delayed.getObj().getUnqKey(),
                    JSON.toJSONString(delayed, SerializerFeature.WriteClassName));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void removeSuccess(DelayedObject delayed) {
        try {
            RedisUtil.getHashProvide().delByKey(this.redisHead, delayed.getObj().getUnqKey());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
