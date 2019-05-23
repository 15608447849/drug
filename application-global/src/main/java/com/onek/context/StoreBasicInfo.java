package com.onek.context;

import java.math.BigDecimal;

/**
 * @Author: leeping
 * @Date: 2019/3/19 14:30
 * 门店客户信息
 */
public class StoreBasicInfo {

    public int storeId; //门店码(企业码)
    public String storeName;//门店名
    public int authenticationStatus;// 状态 128:审核中; 256:认证成功; 512:认证失败;
    public String authenticationMessage = "";//认证消息
    public String address;//营业执照地址
    public Long addressCode;//地区码
    public BigDecimal longitude;//营业执照地址纬度
    public BigDecimal latitude;//营业执照地址经度
    public StoreBasicInfo(int storeId) {
        this.storeId = storeId;
    }
}
