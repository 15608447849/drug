package com.onek.entity;

public class TranTransVO {

    private long unqid;
    private int compid;
    private long orderno;
    private int payno;
    private int payprice;

    private int payway;
    private int paysource;
    private int paystatus;
    private long payorderno;
    private String tppno;

    private String paydate;
    private String paytime;
    private String completedate;
    private String completetime;
    private int cstatus;

    public long getUnqid() {
        return unqid;
    }

    public void setUnqid(long unqid) {
        this.unqid = unqid;
    }

    public int getCompid() {
        return compid;
    }

    public void setCompid(int compid) {
        this.compid = compid;
    }

    public long getOrderno() {
        return orderno;
    }

    public void setOrderno(long orderno) {
        this.orderno = orderno;
    }

    public int getPayno() {
        return payno;
    }

    public void setPayno(int payno) {
        this.payno = payno;
    }

    public int getPayprice() {
        return payprice;
    }

    public void setPayprice(int payprice) {
        this.payprice = payprice;
    }

    public int getPayway() {
        return payway;
    }

    public void setPayway(int payway) {
        this.payway = payway;
    }

    public int getPaysource() {
        return paysource;
    }

    public void setPaysource(int paysource) {
        this.paysource = paysource;
    }

    public int getPaystatus() {
        return paystatus;
    }

    public void setPaystatus(int paystatus) {
        this.paystatus = paystatus;
    }

    public long getPayorderno() {
        return payorderno;
    }

    public void setPayorderno(long payorderno) {
        this.payorderno = payorderno;
    }

    public String getTppno() {
        return tppno;
    }

    public void setTppno(String tppno) {
        this.tppno = tppno;
    }

    public String getPaydate() {
        return paydate;
    }

    public void setPaydate(String paydate) {
        this.paydate = paydate;
    }

    public String getPaytime() {
        return paytime;
    }

    public void setPaytime(String paytime) {
        this.paytime = paytime;
    }

    public String getCompletedate() {
        return completedate;
    }

    public void setCompletedate(String completedate) {
        this.completedate = completedate;
    }

    public String getCompletetime() {
        return completetime;
    }

    public void setCompletetime(String completetime) {
        this.completetime = completetime;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }
}
