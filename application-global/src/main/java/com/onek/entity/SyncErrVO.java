package com.onek.entity;

public class SyncErrVO {
    private String unqid;
    private int synctype;
    private long syncid;
    private String syncmsg;
    private int cstatus;
    private String syncdate;
    private String synctime;
    private int syncfrom;
    private int syncreason;
    private int synctimes;
    private int syncway;

    private static final String[] TYPE = { "其他", "门店", "订单", "商品" };

    private static final String[] FROM = {
            "其他", "电商平台->中间库", "中间库->电商平台", "中间库->自有ERP",
            "自有ERP->中间库", "自有ERP->上游ERP", "上游ERP->自有ERP",
            "电商平台->自有ERP", "自有ERP->电商平台" };

    private static final String[] WAY = { "其他", "实时", "非实时" };

    private static final String[] REASON = {
            "其他", "网络异常", "数据异常", "数据库操作异常",
            "服务异常", "参数异常", "业务规则异常" };

    public String getSynctypen() {
        return TYPE[this.synctype];
    }

    public String getSyncfromn() {
        return FROM[this.syncfrom];
    }

    public String getSyncwayn() {
        return WAY[this.syncway];
    }

    public String getSyncreasonn() {
        return REASON[this.syncreason];
    }

    public String getUnqid() {
        return unqid;
    }

    public void setUnqid(String unqid) {
        this.unqid = unqid;
    }

    public int getSynctype() {
        return synctype;
    }

    public void setSynctype(int synctype) {
        this.synctype = synctype;
    }

    public long getSyncid() {
        return syncid;
    }

    public void setSyncid(long syncid) {
        this.syncid = syncid;
    }

    public String getSyncmsg() {
        return syncmsg;
    }

    public void setSyncmsg(String syncmsg) {
        this.syncmsg = syncmsg;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }

    public String getSyncdate() {
        return syncdate;
    }

    public void setSyncdate(String syncdate) {
        this.syncdate = syncdate;
    }

    public String getSynctime() {
        return synctime;
    }

    public void setSynctime(String synctime) {
        this.synctime = synctime;
    }

    public int getSyncfrom() {
        return syncfrom;
    }

    public void setSyncfrom(int syncfrom) {
        this.syncfrom = syncfrom;
    }

    public int getSyncreason() {
        return syncreason;
    }

    public void setSyncreason(int syncreason) {
        this.syncreason = syncreason;
    }

    public int getSynctimes() {
        return synctimes;
    }

    public void setSynctimes(int synctimes) {
        this.synctimes = synctimes;
    }

    public int getSyncway() {
        return syncway;
    }

    public void setSyncway(int syncway) {
        this.syncway = syncway;
    }
}
