package com.onek.discount.entity;

import com.onek.queue.delay.IDelayedObject;

import java.util.List;

/**
 * @author 11842
 * @version 1.1.1
 * @description 活动
 * @time 2019/4/1 13:46
 **/
public class ActivityVO implements IDelayedObject {
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
    private int cstatus;//综合状态码
    private String ruleName;//规则名称
    private int ruletype; // 活动类型
    private int preWay; // 活动优惠方式
    private int rulecomp;//优惠算法

//    private List<AssDrugVO> assDrugVOS;//活动关联商品集合

    private List<TimeVO> timeVOS;//活动场次

    private List<LadderVO> ladderVOS;//阶梯优惠

    private List<RulesVO> activeRule;//规则

//    private List<AssGiftVO> assGiftVOS;//优惠商品赠换

    //活动关联商品集合
//    private List<GoodsVO> assDrugVOS;

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

    public int getRuletype() {
        return ruletype;
    }

    public void setRuletype(int ruletype) {
        this.ruletype = ruletype;
    }

    public int getPreWay() {
        return preWay;
    }

    public void setPreWay(int preWay) {
        this.preWay = preWay;
    }

    public int getRulecomp() {
        return rulecomp;
    }

    public void setRulecomp(int rulecomp) {
        this.rulecomp = rulecomp;
    }

    public List<RulesVO> getActiveRule() {
        return activeRule;
    }

    public void setActiveRule(List<RulesVO> activeRule) {
        this.activeRule = activeRule;
    }

    @Override
    public String getUnqKey() {
        return unqid;
    }
}
