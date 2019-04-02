package com.onek.discount.entity;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName GoodsVO
 * @Description TODO
 * @date 2019-04-02 13:41
 */
public class GoodsVO {

    //商品类别ID
    private int classid;

    //商品类别名称
    private String classname;

    //商品SKU
    private long gcode;

    //商品规格
    private String desc;

    //限购数量
    private int limitnum;

    //生产厂商
    private String manuname;

    //批准文号
    private String standarno;

    public int getClassid() {
        return classid;
    }

    public void setClassid(int classid) {
        this.classid = classid;
    }

    public String getClassname() {
        return classname;
    }

    public void setClassname(String classname) {
        this.classname = classname;
    }

    public long getGcode() {
        return gcode;
    }

    public void setGcode(long gcode) {
        this.gcode = gcode;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public int getLimitnum() {
        return limitnum;
    }

    public void setLimitnum(int limitnum) {
        this.limitnum = limitnum;
    }

    public String getManuname() {
        return manuname;
    }

    public void setManuname(String manuname) {
        this.manuname = manuname;
    }

    public String getStandarno() {
        return standarno;
    }

    public void setStandarno(String standarno) {
        this.standarno = standarno;
    }
}
