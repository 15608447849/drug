package com.onek.global.sms.entity;

/**
 * @author 11842
 * @version 1.1.1
 * @description 短信模板
 * @time 2019/3/20 10:44
 **/
public class SmsTemplate {
    private Short tno;

    private String tname;

    private String tcontext;

    private Boolean rverify;

    private Short cstatus;

    public Short getTno() {
        return tno;
    }

    public void setTno(Short tno) {
        this.tno = tno;
    }

    public String getTname() {
        return tname;
    }

    public void setTname(String tname) {
        this.tname = tname;
    }

    public String getTcontext() {
        return tcontext;
    }

    public void setTcontext(String tcontext) {
        this.tcontext = tcontext;
    }

    public Boolean getRverify() {
        return rverify;
    }

    public void setRverify(Boolean rverify) {
        this.rverify = rverify;
    }

    public Short getCstatus() {
        return cstatus;
    }

    public void setCstatus(Short cstatus) {
        this.cstatus = cstatus;
    }
}
