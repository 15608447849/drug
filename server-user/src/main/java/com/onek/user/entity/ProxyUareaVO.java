package com.onek.user.entity;

import com.google.gson.annotations.SerializedName;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName ProxyUareaVO
 * @Description TODO
 * @date 2019-05-29 1:37
 */
public class ProxyUareaVO {

    private String areac;

    private String arean;

    private int uid;

    private String uphone;

    private String urealname;

    public String getAreac() {
        return areac;
    }

    public void setAreac(String areac) {
        this.areac = areac;
    }

    public String getArean() {
        return arean;
    }

    public void setArean(String arean) {
        this.arean = arean;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getUphone() {
        return uphone;
    }

    public void setUphone(String uphone) {
        this.uphone = uphone;
    }

    public String getUrealname() {
        return urealname;
    }

    public void setUrealname(String urealname) {
        this.urealname = urealname;
    }
}
