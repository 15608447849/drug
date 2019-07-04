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
