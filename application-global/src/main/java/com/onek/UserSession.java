package com.onek;

import java.io.Serializable;

public class UserSession implements Serializable {
    private String token;
    private long oid;
    private String uname;
    private long roleid;
    private int cstatus;
    private long storeId; // 门店id

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getOid() {
        return oid;
    }

    public void setOid(long oid) {
        this.oid = oid;
    }

    public String getUname() {
        return uname;
    }

    public void setUname(String uname) {
        this.uname = uname;
    }

    public long getRoleid() {
        return roleid;
    }

    public void setRoleid(long roleid) {
        this.roleid = roleid;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }

    public long getStoreId() {
        return storeId;
    }

    public void setStoreId(long storeId) {
        this.storeId = storeId;
    }
}
