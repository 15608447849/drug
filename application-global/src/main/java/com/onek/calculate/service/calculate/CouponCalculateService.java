package com.onek.calculate.service.calculate;

import com.onek.calculate.entity.Ladoff;

public class CouponCalculateService extends BaseDiscountCalculateService {
    private Ladoff[] ladoff;

    public CouponCalculateService(Ladoff[] ladoff) {
        this.ladoff = ladoff;
    }

    @Override
    protected Ladoff[] getLadoffs(long actCode) {
        return this.ladoff;
    }
}
