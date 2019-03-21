package com.onek.consts;

public interface CSTATUS {
    /* 全局标记 */
    int DELETE = 1 << 0;  // 删除 1
    int CLOSE  = 1 << 5; // 停用 32
    /* 全局标记 END */

    int ROLE_NOT_HANDLEABLE = 1 << 1; // 不可停用 2
}
