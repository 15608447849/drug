package com.onek.entity;

import com.onek.calculate.entity.Gift;

import java.util.List;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName ShoppingCartVO
 * @Description TODO
 * @date 2019-04-16 11:36
 */
public class ShoppingCartVO {

    private long unqid;

    //商品SKU
    private long pdno;

    //商品单价
    private double pdprice;

    //商品标题
    private String ptitle;

    //规格
    private String spec;

    //厂商
    private String verdor;

    //有效期
    private String vperiod;

    //数量
    private int num;

    //优惠金额
    private double amt;

    //规则
    private List<DiscountRule> rule;

    //限购量
    private int limitnum;

    //库存量
    private int inventory;

    //状态
    private int status;

    //是否选中
    private int checked;

    //折扣价
    private double discount;

    //应付金额
    private double acamt;

    private long spu;

    private double counpon;

    private long conpno;

    private long areano;

    private List<Long> actcode;

    //运费
    private double freight;

    //是否免邮
    private boolean freepost;

    private boolean seckill = false;

    private boolean oflag;

    private double subtotal;

    private double totalamt;

    private int pstatus;

    private String brand;

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public long getAreano() {
        return areano;
    }

    public void setAreano(long areano) {
        this.areano = areano;
    }

    public int getPstatus() {
        return pstatus;
    }

    public void setPstatus(int pstatus) {
        this.pstatus = pstatus;
    }

    public double getTotalamt() {
        return totalamt;
    }

    public void setTotalamt(double totalamt) {
        this.totalamt = totalamt;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public boolean isOflag() {
        return oflag;
    }

    public void setOflag(boolean oflag) {
        this.oflag = oflag;
    }

    public boolean isFreepost() {
        return freepost;
    }

    public void setFreepost(boolean freepost) {
        this.freepost = freepost;
    }

    public double getFreight() {
        return freight;
    }

    public void setFreight(double freight) {
        this.freight = freight;
    }

    public long getConpno() {
        return conpno;
    }

    public void setConpno(long conpno) {
        this.conpno = conpno;
    }

    public double getCounpon() {
        return counpon;
    }

    public void setCounpon(double counpon) {
        this.counpon = counpon;
    }

    public long getSpu() {
        return spu;
    }

    public void setSpu(long spu) {
        this.spu = spu;
    }

    public double getAcamt() {
        return acamt;
    }

    public void setAcamt(double acamt) {
        this.acamt = acamt;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public int getChecked() {
        return checked;
    }

    public void setChecked(int checked) {
        this.checked = checked;
    }

    public String getPtitle() {
        return ptitle;
    }

    public void setPtitle(String ptitle) {
        this.ptitle = ptitle;
    }

    public long getUnqid() {
        return unqid;
    }

    public void setUnqid(long unqid) {
        this.unqid = unqid;
    }

    public long getPdno() {
        return pdno;
    }

    public void setPdno(long pdno) {
        this.pdno = pdno;
    }

    public double getPdprice() {
        return pdprice;
    }

    public void setPdprice(double pdprice) {
        this.pdprice = pdprice;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public String getVerdor() {
        return verdor;
    }

    public void setVerdor(String verdor) {
        this.verdor = verdor;
    }

    public String getVperiod() {
        return vperiod;
    }

    public void setVperiod(String vperiod) {
        this.vperiod = vperiod;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public double getAmt() {
        return amt;
    }

    public void setAmt(double amt) {
        this.amt = amt;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getLimitnum() {
        return limitnum;
    }

    public void setLimitnum(int limitnum) {
        this.limitnum = limitnum;
    }

    public int getInventory() {
        return inventory;
    }

    public void setInventory(int inventory) {
        this.inventory = inventory;
    }

    public void setRule(List<DiscountRule> rule) {
        this.rule = rule;
    }

    public List<DiscountRule> getRule() {
        return rule;
    }

    public boolean isSeckill() {
        return seckill;
    }

    public void setSeckill(boolean seckill) {
        this.seckill = seckill;
    }

    public List<Long> getActcode() {
        return actcode;
    }

    public void setActcode(List<Long> actcode) {
        this.actcode = actcode;
    }
}
