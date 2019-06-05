package com.onek.report.vo;

import cn.hy.otms.rpcproxy.businessCenter.Order;
import com.onek.util.IceRemoteUtil;

public class ReturnResult {
    private String date;

    private OrderNum orderNum;
    private GMV gmv;
    private CompPrice compPrice;

    public ReturnResult() {
        orderNum = new OrderNum();
        gmv = new GMV();
    }

    public OrderNum getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(OrderNum orderNum) {
        this.orderNum = orderNum;
    }

    public GMV getGmv() {
        return gmv;
    }

    public void setGmv(GMV gmv) {
        this.gmv = gmv;
    }

    public CompPrice getCompPrice() {
        return compPrice;
    }

    public void setCompPrice(CompPrice compPrice) {
        this.compPrice = compPrice;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
