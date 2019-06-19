package com.onek.report.init;

public interface ColInfo {
    int COL_NUM_ORDERNUM_MAX = 18;
    int COL_NUM_ORDERNUM_MIN = 19;
    int COL_NUM_ORDERNUM_AVG = 20;
    // MAX:1, MIN:2, AVG:3, PERSENT:4
    int[] COL_STATUS =
    {
            0, 0, 0, 0, 0, // 0-4
            0, 4, 4, 0, 4, // 5-9
            0, 0, 0, 0, 0,
            4, 0, 4, 1, 2,
            4,
    };
}
