package com.onek.discount.entity;

import java.util.List;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName CouponVO
 * @Description TODO
 * @date 2019-04-02 8:58
 */
public class CouponVO {

    //优惠券码
    private String coupno;

    //优惠券名称
    private String coupname;

    //全局兼容码
    private int glbno;

    //资格码
    private int qlfno;

    //资格值
    private long qlfval;

    //优惠券描述
    private String desc;

    //发放周期类型
    private int periodtype;

    //发放周期日
    private int periodday;

    //发放开始日期
    private String startdate;

    //活动结束日期
    private String enddate;

    //优惠券规则码
    private int ruleno;

    //有效天数
    private int validday;

    //生效标记 0 即日生效 、1 次日生效
    private int validflag;

    //综合状态
    private int cstatus;

    //规则名称
    private String rulename;

    private int actstock;


    public int getRulecomp() {
        return rulecomp;
    }

    public void setRulecomp(int rulecomp) {
        this.rulecomp = rulecomp;
    }


    private int rulecomp;//优惠算法



    //活动场次
    private List<TimeVO> timeVOS;


    //阶梯
    private List<LadderVO> ladderVOS;


    //规则
    private List<RulesVO> activeRule;

    public List<RulesVO> getActiveRule() {
        return activeRule;
    }

    public void setActiveRule(List<RulesVO> activeRule) {
        this.activeRule = activeRule;
    }

    public int getActstock() {
        return actstock;
    }

    public void setActstock(int actstock) {
        this.actstock = actstock;
    }






    public List<TimeVO> getTimeVOS() {
        return timeVOS;
    }

    public void setTimeVOS(List<TimeVO> timeVOS) {
        this.timeVOS = timeVOS;
    }

    public List<LadderVO> getLadderVOS() {
        return ladderVOS;
    }

    public void setLadderVOS(List<LadderVO> ladderVOS) {
        this.ladderVOS = ladderVOS;
    }

    public String getCoupno() {
        return coupno;
    }

    public void setCoupno(String coupno) {
        this.coupno = coupno;
    }

    public String getCoupname() {
        return coupname;
    }

    public void setCoupname(String coupname) {
        this.coupname = coupname;
    }

    public int getGlbno() {
        return glbno;
    }

    public void setGlbno(int glbno) {
        this.glbno = glbno;
    }

    public int getQlfno() {
        return qlfno;
    }

    public void setQlfno(int qlfno) {
        this.qlfno = qlfno;
    }

    public long getQlfval() {
        return qlfval;
    }

    public void setQlfval(long qlfval) {
        this.qlfval = qlfval;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public int getPeriodtype() {
        return periodtype;
    }

    public void setPeriodtype(int periodtype) {
        this.periodtype = periodtype;
    }

    public int getPeriodday() {
        return periodday;
    }

    public void setPeriodday(int periodday) {
        this.periodday = periodday;
    }

    public String getStartdate() {
        return startdate;
    }

    public void setStartdate(String startdate) {
        this.startdate = startdate;
    }

    public String getEnddate() {
        return enddate;
    }

    public void setEnddate(String enddate) {
        this.enddate = enddate;
    }

    public int getRuleno() {
        return ruleno;
    }

    public void setRuleno(int ruleno) {
        this.ruleno = ruleno;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }

    public int getValidday() {
        return validday;
    }

    public void setValidday(int validday) {
        this.validday = validday;
    }

    public int getValidflag() {
        return validflag;
    }

    public void setValidflag(int validflag) {
        this.validflag = validflag;
    }

    public String getRulename() {
        return rulename;
    }

    public void setRulename(String rulename) {
        this.rulename = rulename;
    }

}
