package com.onek.entity;

import java.util.List;

public class TranOrderDetail {
    private String orderno;//订单号
    private String tradeno;//交易号
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

    private String consignee;
    private String contact;
    private String address;
    private double balamt;
    private int opayway;
    private String remarks;

    /**      trans         **/
    private int payno;
    private double payprice;

    private int payway;
    private int paysource;
    private int paystatus;
    private String payorderno;
    private String tppno;

    private String paydate;
    private String paytime;
    private String completedate;
    private String completetime;
    private int transcstatus;

    /**      appraise         **/
    private int level;
    private int descmatch;
    private int logisticssrv;

    private String content;
    private String appcreatetdate;
    private String appcreatetime;
    private int appcstatus;


    /**         ex           **/
    private String pname;
    private String cusname;
    private String areaAllName;
    private int totalNum;
    private int classNum;

    public int getTotalNum() {
        return totalNum;
    }

    public void setTotalNum(int totalNum) {
        this.totalNum = totalNum;
    }

    public int getClassNum() {
        return classNum;
    }

    public void setClassNum(int classNum) {
        this.classNum = classNum;
    }

    public String getAreaAllName() {
        return areaAllName;
    }

    public void setAreaAllName(String areaAllName) {
        this.areaAllName = areaAllName;
    }

    private List<TranOrderGoods> goods;

    public String getOrderno() {
        return orderno;
    }

    public void setOrderno(String orderno) {
        this.orderno = orderno;
    }

    public String getTradeno() {
        return tradeno;
    }

    public void setTradeno(String tradeno) {
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

    public int getPayno() {
        return payno;
    }

    public void setPayno(int payno) {
        this.payno = payno;
    }

    public double getPayprice() {
        return payprice;
    }

    public void setPayprice(double payprice) {
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

    public String getPayorderno() {
        return payorderno;
    }

    public void setPayorderno(String payorderno) {
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

    public int getTranscstatus() {
        return transcstatus;
    }

    public void setTranscstatus(int transcstatus) {
        this.transcstatus = transcstatus;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getDescmatch() {
        return descmatch;
    }

    public void setDescmatch(int descmatch) {
        this.descmatch = descmatch;
    }

    public int getLogisticssrv() {
        return logisticssrv;
    }

    public void setLogisticssrv(int logisticssrv) {
        this.logisticssrv = logisticssrv;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAppcreatetdate() {
        return appcreatetdate;
    }

    public void setAppcreatetdate(String appcreatetdate) {
        this.appcreatetdate = appcreatetdate;
    }

    public String getAppcreatetime() {
        return appcreatetime;
    }

    public void setAppcreatetime(String appcreatetime) {
        this.appcreatetime = appcreatetime;
    }

    public int getAppcstatus() {
        return appcstatus;
    }

    public void setAppcstatus(int appcstatus) {
        this.appcstatus = appcstatus;
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

    public String getPname() {
        return pname;
    }

    public void setPname(String pname) {
        this.pname = pname;
    }

    public String getCusname() {
        return cusname;
    }

    public void setCusname(String cusname) {
        this.cusname = cusname;
    }

    public double getBalamt() {
        return balamt;
    }

    public void setBalamt(double balamt) {
        this.balamt = balamt;
    }

    public int getOpayway() {
        return opayway;
    }

    public void setOpayway(int opayway) {
        this.opayway = opayway;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
