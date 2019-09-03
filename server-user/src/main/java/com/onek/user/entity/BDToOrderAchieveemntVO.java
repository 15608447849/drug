package com.onek.user.entity;

import java.math.BigDecimal;

public class BDToOrderAchieveemntVO {
    private long inviter; //用户id
    private BigDecimal canclord; //订单取消数
    private BigDecimal completeord;//订单交易完成数
    private BigDecimal returnord; //退货订单数
    private BigDecimal afsaleord;//售后订单数
    private BigDecimal canclordamt;//取消订单金额
    private BigDecimal originalprice;//原价交易金额
    private BigDecimal payamt; //实付交易额
    private BigDecimal distamt; //优惠金额
    private BigDecimal balamt; //使用余额金额
    private long maxpayamt;//最高支付金额
    private long minpayamt;//最低支付金额
    private BigDecimal avgpayamt;//完成交易订单平均订单金额

    public long getInviter() {
        return inviter;
    }
    public void setInviter(long inviter) {
        this.inviter = inviter;
    }
    public BigDecimal getCanclord() {
        return canclord;
    }
    public void setCanclord(BigDecimal canclord) {
        this.canclord = canclord;
    }
    public BigDecimal getCompleteord() {
        return completeord;
    }
    public void setCompleteord(BigDecimal completeord) {
        this.completeord = completeord;
    }
    public BigDecimal getReturnord() {
        return returnord;
    }
    public void setReturnord(BigDecimal returnord) {
        this.returnord = returnord;
    }
    public BigDecimal getAfsaleord() {
        return afsaleord;
    }
    public void setAfsaleord(BigDecimal afsaleord) {
        this.afsaleord = afsaleord;
    }
    public BigDecimal getCanclordamt() {
        return canclordamt;
    }
    public void setCanclordamt(BigDecimal canclordamt) {
        this.canclordamt = canclordamt;
    }
    public BigDecimal getOriginalprice() {
        return originalprice;
    }
    public void setOriginalprice(BigDecimal originalprice) {
        this.originalprice = originalprice;
    }
    public BigDecimal getPayamt() {
        return payamt;
    }
    public void setPayamt(BigDecimal payamt) {
        this.payamt = payamt;
    }
    public BigDecimal getDistamt() {
        return distamt;
    }
    public void setDistamt(BigDecimal distamt) {
        this.distamt = distamt;
    }
    public BigDecimal getBalamt() {
        return balamt;
    }
    public void setBalamt(BigDecimal balamt) {
        this.balamt = balamt;
    }
    public long getMaxpayamt() {
        return maxpayamt;
    }
    public void setMaxpayamt(long maxpayamt) {
        this.maxpayamt = maxpayamt;
    }
    public long getMinpayamt() {
        return minpayamt;
    }
    public void setMinpayamt(long minpayamt) {
        this.minpayamt = minpayamt;
    }
    public BigDecimal getAvgpayamt() {
        return avgpayamt;
    }
    public void setAvgpayamt(BigDecimal avgpayamt) {
        this.avgpayamt = avgpayamt;
    }

}
