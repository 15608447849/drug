package com.onek.entity;

/**
 * @author 11842
 * @version 1.1.1
 * @description 评价
 * @time 2019/4/20 14:36
 **/
public class AppriseVO {
    private String unqid;//
    private String orderno;//订单号
    private int level;//评价等级
    private int descmatch;//描述相符
    private int logisticssrv;//物流服务
    private String content;//评价内容
    private String createtdate;
    private String createtime;
    private int cstatus;
    private int compid;//企业码
    private long sku;//sku
    private String compName;//企业名称

    public String getUnqid() {
        return unqid;
    }

    public void setUnqid(String unqid) {
        this.unqid = unqid;
    }

    public String getOrderno() {
        return orderno;
    }

    public void setOrderno(String orderno) {
        this.orderno = orderno;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getDescmatch() {
        return descmatch;
    }

    public void setDescmatch(int descmatch) {
        this.descmatch = descmatch;
    }

    public int getLogisticssrv() {
        return logisticssrv;
    }

    public void setLogisticssrv(int logisticssrv) {
        this.logisticssrv = logisticssrv;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreatetdate() {
        return createtdate;
    }

    public void setCreatetdate(String createtdate) {
        this.createtdate = createtdate;
    }

    public String getCreatetime() {
        return createtime;
    }

    public void setCreatetime(String createtime) {
        this.createtime = createtime;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }

    public int getCompid() {
        return compid;
    }

    public void setCompid(int compid) {
        this.compid = compid;
    }

    public long getSku() {
        return sku;
    }

    public void setSku(long sku) {
        this.sku = sku;
    }

    public String getCompName() {
        return compName;
    }

    public void setCompName(String compName) {
        this.compName = compName;
    }
}
