package com.onek.entity;

import java.util.List;

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
    private double pdamt;//商品总价
    private double freight;//运费金额
    private double payamt;//支付金额
    private double coupamt;//使用券额
    private double distamt;//优惠金额
    private long rvaddno;//收货地址码
    private String shipdate;//发货日期
    private String shiptime;//发货时间
    private int settstatus;//结算状态
    private String settdate;//结算日期
    private String setttime;//结算时间
    private int otype;//订单类型
    private String odate;//订单日期
    private String otime;//订单时间
    private int cstatus;//
    private String consignee;//收货人
    private String contact;//收货人联系方式
    private String address;//收货详细地址


    private List<TranOrderGoods> goods;


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

    public double getPdamt() {
        return pdamt;
    }

    public void setPdamt(double pdamt) {
        this.pdamt = pdamt;
    }

    public double getFreight() {
        return freight;
    }

    public void setFreight(double freight) {
        this.freight = freight;
    }

    public double getPayamt() {
        return payamt;
    }

    public void setPayamt(double payamt) {
        this.payamt = payamt;
    }

    public double getCoupamt() {
        return coupamt;
    }

    public void setCoupamt(double coupamt) {
        this.coupamt = coupamt;
    }

    public double getDistamt() {
        return distamt;
    }

    public void setDistamt(double distamt) {
        this.distamt = distamt;
    }

    public long getRvaddno() {
        return rvaddno;
    }

    public void setRvaddno(long rvaddno) {
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

    public List<TranOrderGoods> getGoods() {
        return goods;
    }

    public void setGoods(List<TranOrderGoods> goods) {
        this.goods = goods;
    }

    public String getConsignee() {
        return consignee;
    }

    public void setConsignee(String consignee) {
        this.consignee = consignee;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
