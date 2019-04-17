package com.onek.entity;

/**
 * @author 11842
 * @version 1.1.1
 * @description 订单实体类
 * @time 2019/4/16 15:27
 **/
public class TranOrder {
    private String orderno;//订单号
    private long tradeno;//交易号
    private int cusno;//客户企业码
    private int busno;//商家企业码
    private int ostatus;//订单状态
    private int asstatus;//售后状态
    private int pdnum;//商品数量
    private int pdamt;//商品总价
    private int freight;//运费金额
    private int payamt;//支付金额
    private int coupamt;//使用券额
    private int distamt;//优惠金额
    private int rvaddno;//收货地址码
    private String shipdate;//发货日期
    private String shiptime;//发货时间
    private int settstatus;//结算状态
    private String settdate;//结算日期
    private String setttime;//结算时间
    private int otype;//订单类型
    private String odate;//订单日期
    private String otime;//订单时间
    private int cstatus;//

    public String getOrderno() {
        return orderno;
    }

    public void setOrderno(String orderno) {
        this.orderno = orderno;
    }

    public long getTradeno() {
        return tradeno;
    }

    public void setTradeno(long tradeno) {
        this.tradeno = tradeno;
    }

    public int getCusno() {
        return cusno;
    }

    public void setCusno(int cusno) {
        this.cusno = cusno;
    }

    public int getBusno() {
        return busno;
    }

    public void setBusno(int busno) {
        this.busno = busno;
    }

    public int getOstatus() {
        return ostatus;
    }

    public void setOstatus(int ostatus) {
        this.ostatus = ostatus;
    }

    public int getAsstatus() {
        return asstatus;
    }

    public void setAsstatus(int asstatus) {
        this.asstatus = asstatus;
    }

    public int getPdnum() {
        return pdnum;
    }

    public void setPdnum(int pdnum) {
        this.pdnum = pdnum;
    }

    public int getPdamt() {
        return pdamt;
    }

    public void setPdamt(int pdamt) {
        this.pdamt = pdamt;
    }

    public int getFreight() {
        return freight;
    }

    public void setFreight(int freight) {
        this.freight = freight;
    }

    public int getPayamt() {
        return payamt;
    }

    public void setPayamt(int payamt) {
        this.payamt = payamt;
    }

    public int getCoupamt() {
        return coupamt;
    }

    public void setCoupamt(int coupamt) {
        this.coupamt = coupamt;
    }

    public int getDistamt() {
        return distamt;
    }

    public void setDistamt(int distamt) {
        this.distamt = distamt;
    }

    public int getRvaddno() {
        return rvaddno;
    }

    public void setRvaddno(int rvaddno) {
        this.rvaddno = rvaddno;
    }

    public String getShipdate() {
        return shipdate;
    }

    public void setShipdate(String shipdate) {
        this.shipdate = shipdate;
    }

    public String getShiptime() {
        return shiptime;
    }

    public void setShiptime(String shiptime) {
        this.shiptime = shiptime;
    }

    public int getSettstatus() {
        return settstatus;
    }

    public void setSettstatus(int settstatus) {
        this.settstatus = settstatus;
    }

    public String getSettdate() {
        return settdate;
    }

    public void setSettdate(String settdate) {
        this.settdate = settdate;
    }

    public String getSetttime() {
        return setttime;
    }

    public void setSetttime(String setttime) {
        this.setttime = setttime;
    }

    public int getOtype() {
        return otype;
    }

    public void setOtype(int otype) {
        this.otype = otype;
    }

    public String getOdate() {
        return odate;
    }

    public void setOdate(String odate) {
        this.odate = odate;
    }

    public String getOtime() {
        return otime;
    }

    public void setOtime(String otime) {
        this.otime = otime;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }
}
