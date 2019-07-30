package com.onek.entity;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName PkgProduct
 * @Description TODO
 * @date 2019-07-24 19:02
 */
public class PkgProduct {

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

    public String getPtitle() {
        return ptitle;
    }

    public void setPtitle(String ptitle) {
        this.ptitle = ptitle;
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
}
