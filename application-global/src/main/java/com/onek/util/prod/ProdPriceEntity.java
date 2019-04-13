package com.onek.util.prod;

public class ProdPriceEntity {
    private long sku;
    private double vatp;
    private double actprice;
    private double minactprize;
    private double maxactprize;

    public long getSku() {
        return sku;
    }

    public void setSku(long sku) {
        this.sku = sku;
    }

    public double getVatp() {
        return vatp;
    }

    public void setVatp(double vatp) {
        this.vatp = vatp;
    }

    public double getActprice() {
        return actprice;
    }

    public void setActprice(double actprice) {
        this.actprice = actprice;
    }

    public double getMinactprize() {
        return minactprize;
    }

    public void setMinactprize(double minactprize) {
        this.minactprize = minactprize;
    }

    public double getMaxactprize() {
        return maxactprize;
    }

    public void setMaxactprize(double maxactprize) {
        this.maxactprize = maxactprize;
    }
}
