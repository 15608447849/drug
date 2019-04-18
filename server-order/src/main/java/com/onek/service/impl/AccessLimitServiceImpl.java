package com.onek.service.impl;

import com.onek.service.AccessLimitService;

public class AccessLimitServiceImpl implements AccessLimitService {
    @Override
    public boolean tryAcquireSeckill() {
        return false;
    }
}
