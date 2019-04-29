package com.onek.context;

import java.math.BigDecimal;

/**
 * @Author: leeping
 * @Date: 2019/3/19 14:30
 * 门店客户信息
 */
public class StoreBasicInfo {

    public int storeId; //门店码(企业码)
    //门店信息
    public String storeName;//门店名
    public int authenticationStatus;// 状态 64:未认证; 128:审核中; 256:已认证; 512:认证失败; 1024:停用
    public String authenticationMessage = "未关联";//认证消息
    public String address;//营业执照地址
    public Long addressCode;//地区码
    public String addressCodeStr;//地区码对应的详细信息
    public BigDecimal longitude;//营业执照地址纬度
    public BigDecimal latitude;//营业执照地址经度

    public StoreBasicInfo(int storeId) {
        this.storeId = storeId;
    }
}
