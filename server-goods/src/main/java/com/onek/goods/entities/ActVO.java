package com.onek.goods.entities;


/**
 * @author 11842
 * @version 1.1.1
 * @description 活动信息
 * @time 2019/6/28 11:43
 **/
public class ActVO {
    private String unqid;//活动码
    private String actname;//活动名称
    private int incpriority;//互斥优先级
    private int cpriority;//兼容优先级

    private int qualcode;//资格码
    private long qualvalue;//资格值
    private String actdesc;//活动描述
    private int excdiscount;//排斥优惠券

    private int acttype;//活动周期类型
    private long actcycle;//活动周期
    private String sdate;//活动开始日期
    private String edate;//活动结束日期
    private int brulecode;//活动规则码

    private String ruleName;//规则名称


    public String getUnqid() {
        return unqid;
    }

    public void setUnqid(String unqid) {
        this.unqid = unqid;
    }

    public String getActname() {
        return actname;
    }

    public void setActname(String actname) {
        this.actname = actname;
    }

    public int getIncpriority() {
        return incpriority;
    }

    public void setIncpriority(int incpriority) {
        this.incpriority = incpriority;
    }

    public int getCpriority() {
        return cpriority;
    }

    public void setCpriority(int cpriority) {
        this.cpriority = cpriority;
    }

    public int getQualcode() {
        return qualcode;
    }

    public void setQualcode(int qualcode) {
        this.qualcode = qualcode;
    }

    public long getQualvalue() {
        return qualvalue;
    }

    public void setQualvalue(long qualvalue) {
        this.qualvalue = qualvalue;
    }

    public String getActdesc() {
        return actdesc;
    }

    public void setActdesc(String actdesc) {
        this.actdesc = actdesc;
    }

    public int getExcdiscount() {
        return excdiscount;
    }

    public void setExcdiscount(int excdiscount) {
        this.excdiscount = excdiscount;
    }

    public int getActtype() {
        return acttype;
    }

    public void setActtype(int acttype) {
        this.acttype = acttype;
    }

    public long getActcycle() {
        return actcycle;
    }

    public void setActcycle(long actcycle) {
        this.actcycle = actcycle;
    }

    public String getSdate() {
        return sdate;
    }

    public void setSdate(String sdate) {
        this.sdate = sdate;
    }

    public String getEdate() {
        return edate;
    }

    public void setEdate(String edate) {
        this.edate = edate;
    }

    public int getBrulecode() {
        return brulecode;
    }

    public void setBrulecode(int brulecode) {
        this.brulecode = brulecode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

}
