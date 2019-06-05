package com.onek.util.area;

public class AreaEntity {
    private long areac;
    private String arean;
    private int cstatus;
    private double fee;
    private String lcareac;

    private Object[] liuQi = {};

    public long getAreac() {
        return areac;
    }
    public String getArean() {
        return arean;
    }
    public int getCstatus() {
        return cstatus;
    }

//    public long[] getAncestors() {
//        return AreaUtil.getAllAncestorCodes(this.areac);
//    }

    public Object[] getLiuQi() {
        return liuQi;
    }

    public void setAreac(long areac) {
        this.areac = areac;
    }

    public void setArean(String arean) {
        this.arean = arean;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }

    public double getFee() {
        return fee;
    }

    public void setFee(double fee) {
        this.fee = fee;
    }

    public String getLcareac() {
        return lcareac;
    }

    public void setLcareac(String lcareac) {
        this.lcareac = lcareac;
    }

    public void setLiuQi(Object[] liuQi) {
        this.liuQi = liuQi;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AreaEntity that = (AreaEntity) o;

        return areac == that.areac;

    }

    @Override
    public int hashCode() {
        return (int) (areac ^ (areac >>> 32));
    }
}
