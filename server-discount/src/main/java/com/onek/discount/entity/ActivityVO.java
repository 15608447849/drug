package com.onek.discount.entity;

import java.util.List;

/**
 * @author 11842
 * @version 1.1.1
 * @description 活动
 * @time 2019/4/1 13:46
 **/
public class ActivityVO {
    private long unqid;//活动码
    private String actname;//活动名称
    private int incpriority;//互斥优先级
    private int cpriority;//兼容优先级
    private int qualcode;//资格码
    private int qualvalue;//资格值
    private String actdesc;//活动描述
    private int excdiscount;//排斥优惠券
    private int acttype;//活动周期类型
    private long actcycle;//活动周期
    private String sdate;//活动开始日期
    private String edate;//活动结束日期
    private int rulecode;//活动规则码
    private int cstatus;//综合状态码
    private String ruleName;//规则名称

//    private List<AssDrugVO> assDrugVOS;//活动关联商品集合

    private List<TimeVO> timeVOS;//活动场次

    private List<LadderVO> ladderVOS;//阶梯优惠

//    private List<AssGiftVO> assGiftVOS;//优惠商品赠换

    //活动关联商品集合
//    private List<GoodsVO> assDrugVOS;

    public long getUnqid() {
        return unqid;
    }

    public void setUnqid(long unqid) {
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

    public int getQualvalue() {
        return qualvalue;
    }

    public void setQualvalue(int qualvalue) {
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

    public int getRulecode() {
        return rulecode;
    }

    public void setRulecode(int rulecode) {
        this.rulecode = rulecode;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }

//    public List<GoodsVO> getAssDrugVOS() {
//        return assDrugVOS;
//    }
//
//    public void setAssDrugVOS(List<GoodsVO> assDrugVOS) {
//        this.assDrugVOS = assDrugVOS;
//    }

    public List<TimeVO> getTimeVOS() {
        return timeVOS;
    }

    public void setTimeVOS(List<TimeVO> timeVOS) {
        this.timeVOS = timeVOS;
    }

//    public List<AssGiftVO> getAssGiftVOS() {
//        return assGiftVOS;
//    }
//
//    public void setAssGiftVOS(List<AssGiftVO> assGiftVOS) {
//        this.assGiftVOS = assGiftVOS;
//    }

    public List<LadderVO> getLadderVOS() {
        return ladderVOS;
    }

    public void setLadderVOS(List<LadderVO> ladderVOS) {
        this.ladderVOS = ladderVOS;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }
}
