package com.onek.discount.entity;

/**
 * @author 11842
 * @version 1.1.1
 * @description 规则
 * @time 2019/4/1 13:34
 **/
public class RulesVO {

    private long unqid;//规则码
    private String rulename;//规则名称
    private String desc;//规则描述
    private int cstatus;//综合状态码


    public long getUnqid() {
        return unqid;
    }

    public void setUnqid(long unqid) {
        this.unqid = unqid;
    }

    public String getRulename() {
        return rulename;
    }

    public void setRulename(String rulename) {
        this.rulename = rulename;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }
}
