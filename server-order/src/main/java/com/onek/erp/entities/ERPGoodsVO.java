package com.onek.erp.entities;

/**
 * @author 11842
 * @version 1.1.1
 * @description ERP商品数据结构VO（包含赠品）
 * @time 2019/6/26 11:40
 **/
public class ERPGoodsVO {
    private String orderno;
    private String erpsku;//sku
    private String unqid;//详情唯一码
    private String num;//开票数量
    private String pdprice;//单价
    private String payamt;//支付金额


    public String getErpsku() {
        return erpsku;
    }

    public void setErpsku(String erpsku) {
        this.erpsku = erpsku;
    }

    public String getUnqid() {
        return unqid;
    }

    public void setUnqid(String unqid) {
        this.unqid = unqid;
    }

    public String getNum() {
        return num;
    }

    public void setNum(String num) {
        this.num = num;
    }

    public String getPdprice() {
        return pdprice;
    }

    public void setPdprice(String pdprice) {
        this.pdprice = pdprice;
    }

    public String getPayamt() {
        return payamt;
    }

    public void setPayamt(String payamt) {
        this.payamt = payamt;
    }

    public String getOrderno() {
        return orderno;
    }

    public void setOrderno(String orderno) {
        this.orderno = orderno;
    }
}
