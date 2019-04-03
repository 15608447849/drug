package com.onek.discount.entity;

/**
 * @author 11842
 * @version 1.1.1
 * @description 活动商品关联
 * @time 2019/4/2 14:44
 **/
public class AssDrugVO {
    private long unqid;//编号
    private long actcode;//活动码
    private long gcode;//商品码/商品类别码
    private int menucode;//套餐码
    private int actstock;//活动库存
    private int cstatus;
    private int limitnum;//限购数量

    private String prodname;//商品名称
    private String spec;//规格
    private String standarno;//批准文号
    private long manuno;//生产厂家码
    private String manuname;//生产厂家

    public int getLimitnum() {
        return limitnum;
    }

    public void setLimitnum(int limitnum) {
        this.limitnum = limitnum;
    }

    public long getUnqid() {
        return unqid;
    }

    public void setUnqid(long unqid) {
        this.unqid = unqid;
    }

    public long getActcode() {
        return actcode;
    }

    public void setActcode(long actcode) {
        this.actcode = actcode;
    }

    public long getGcode() {
        return gcode;
    }

    public void setGcode(long gcode) {
        this.gcode = gcode;
    }

    public int getMenucode() {
        return menucode;
    }

    public void setMenucode(int menucode) {
        this.menucode = menucode;
    }

    public int getActstock() {
        return actstock;
    }

    public void setActstock(int actstock) {
        this.actstock = actstock;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }

    public String getProdname() {
        return prodname;
    }

    public void setProdname(String prodname) {
        this.prodname = prodname;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public String getStandarno() {
        return standarno;
    }

    public void setStandarno(String standarno) {
        this.standarno = standarno;
    }

    public long getManuno() {
        return manuno;
    }

    public void setManuno(long manuno) {
        this.manuno = manuno;
    }

    public String getManuname() {
        return manuname;
    }

    public void setManuname(String manuname) {
        this.manuname = manuname;
    }
}
