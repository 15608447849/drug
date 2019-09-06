package com.onek.entity;


public class BDOrderAchieveemntVO {
    private long inviter; //用户id
    private String canclord; //订单取消数
    private String completeord;//订单交易完成数
    private String returnord; //退货订单数
    private String afsaleord;//售后订单数
    private String canclordamt;//取消订单金额
    private String originalprice;//原价交易金额
    private String payamt; //实付交易额
    private String distamt; //优惠金额
    private String balamt; //使用余额金额
    private long maxpayamt;//最高支付金额
    private long minpayamt;//最低支付金额
    private String avgpayamt;//完成交易订单平均订单金额
    private String realrefamt;//余额抵扣
    private String odate;
    public long getInviter() {
        return inviter;
    }
    public void setInviter(long inviter) {
        this.inviter = inviter;
    }
    public String getCanclord() {
        return canclord;
    }
    public void setCanclord(String canclord) {
        this.canclord = canclord;
    }
    public String getCompleteord() {
        return completeord;
    }
    public void setCompleteord(String completeord) {
        this.completeord = completeord;
    }
    public String getReturnord() {
        return returnord;
    }
    public void setReturnord(String returnord) {
        this.returnord = returnord;
    }
    public String getAfsaleord() {
        return afsaleord;
    }
    public void setAfsaleord(String afsaleord) {
        this.afsaleord = afsaleord;
    }
    public String getCanclordamt() {
        return canclordamt;
    }
    public void setCanclordamt(String canclordamt) {
        this.canclordamt = canclordamt;
    }
    public String getOriginalprice() {
        return originalprice;
    }
    public void setOriginalprice(String originalprice) {
        this.originalprice = originalprice;
    }
    public String getPayamt() {
        return payamt;
    }
    public void setPayamt(String payamt) {
        this.payamt = payamt;
    }
    public String getDistamt() {
        return distamt;
    }
    public void setDistamt(String distamt) {
        this.distamt = distamt;
    }
    public String getBalamt() {
        return balamt;
    }
    public void setBalamt(String balamt) {
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
    public String getAvgpayamt() {
        return avgpayamt;
    }
    public void setAvgpayamt(String avgpayamt) {
        this.avgpayamt = avgpayamt;
    }

    public String getRealrefamt() {
        return realrefamt;
    }

    public String getOdate() {
        return odate;
    }

    public void setRealrefamt(String realrefamt) {
        this.realrefamt = realrefamt;
    }

    public void setOdate(String odate) {
        this.odate = odate;
    }
}
