package com.onek.consts;

public interface CSTATUS {
    /* 全局标记 */
    long DELETE = 1;  // 删除 1
    long CLOSE  = 32; // 停用 32
    /* 全局标记 END */

    long ROLE_NOT_HANDLEABLE = 2; // 不可停用 2

    long ORDER_BACK_CANCEL = 1024; // 后台取消订单标记
}
