package com.onek.discount.entity;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName GoodsVO
 * @Description TODO
 * @date 2019-04-02 13:41
 */
public class GoodsVO {

    private String unqid;//全局唯一

    private String actcode;//活动码

    //商品类别名称
    private String classname;

    //商品名称
    private String prodname;

    //商品SKU
    private long gcode;

    //商品规格
    private String spec;

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

    //乐观锁
    private int vcode;

    //商品单价
    private double price;

    private int stockType;

    private int priceType;

    private int pkgprodnum;

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
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


    public String getProdname() {
        return prodname;
    }

    public void setProdname(String prodname) {
        this.prodname = prodname;
    }

    public String getUnqid() {
        return unqid;
    }

    public void setUnqid(String unqid) {
        this.unqid = unqid;
    }

    public String getActcode() {
        return actcode;
    }

    public void setActcode(String actcode) {
        this.actcode = actcode;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public int getVcode() {
        return vcode;
    }

    public void setVcode(int vcode) {
        this.vcode = vcode;
    }

    public int getStockType() {
        return stockType;
    }

    public void setStockType(int stockType) {
        this.stockType = stockType;
    }

    public int getPriceType() {
        return priceType;
    }

    public void setPriceType(int priceType) {
        this.priceType = priceType;
    }

    public int getPkgprodnum() {
        return pkgprodnum;
    }

    public void setPkgprodnum(int pkgprodnum) {
        this.pkgprodnum = pkgprodnum;
    }
}
