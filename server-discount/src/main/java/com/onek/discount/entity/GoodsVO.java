package com.onek.discount.entity;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName GoodsVO
 * @Description TODO
 * @date 2019-04-02 13:41
 */
public class GoodsVO {

    //商品类别名称
    private String classname;

    //商品SKU
    private long gcode;

    //商品规格
    private String desc;

    //限购数量
    private int limitnum;

    //库存数量
    private int actstock;

    //生产厂商
    private String manuname;

    //批准文号
    private String standarno;

    //套餐码
    private int menucode;

    //综合状态码
    private int cstatus;

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

    public int getActstock() {
        return actstock;
    }

    public void setActstock(int actstock) {
        this.actstock = actstock;
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

    public int getMenucode() {
        return menucode;
    }

    public void setMenucode(int menucode) {
        this.menucode = menucode;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }
}
