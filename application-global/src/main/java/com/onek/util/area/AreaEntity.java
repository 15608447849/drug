package com.onek.util.area;

public class AreaEntity {
    private long areac;
    private String arean;
    private int cstatus;
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
}
