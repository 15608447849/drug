package com.onek.service.impl;

import com.google.common.util.concurrent.RateLimiter;
import com.onek.service.AccessLimitService;

public class AccessLimitServiceImpl implements AccessLimitService {

    /**
     * 每秒钟只发出100个令牌，拿到令牌的请求才可以进入秒杀过程
     */
    private RateLimiter secKillRateLimiter = RateLimiter.create(100);

    @Override
    public boolean tryAcquireSecKill() {
        return secKillRateLimiter.tryAcquire();
    }
}
