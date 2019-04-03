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
    private long coupno;

    //优惠券名称
    private String coupname;

    //全局兼容码
    private int glbno;

    //资格码
    private int qlfno;

    //资格值
    private int qlfval;

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


    //综合状态
    private int cstatus;

    //活动关联商品集合
    private List<GoodsVO> assDrugVOS;

    //活动场次
    private List<TimeVO> timeVOS;

    //优惠商品赠换
    private List<AssGiftVO> assGiftVOS;

    //阶梯
    private List<LadderVO> ladderVOS;

    //规则
    private List<RulesVO>  rulesVOS;

    public List<RulesVO> getRulesVOS() {
        return rulesVOS;
    }

    public void setRulesVOS(List<RulesVO> rulesVOS) {
        this.rulesVOS = rulesVOS;
    }

    public List<GoodsVO> getAssDrugVOS() {
        return assDrugVOS;
    }

    public void setAssDrugVOS(List<GoodsVO> assDrugVOS) {
        this.assDrugVOS = assDrugVOS;
    }

    public List<TimeVO> getTimeVOS() {
        return timeVOS;
    }

    public void setTimeVOS(List<TimeVO> timeVOS) {
        this.timeVOS = timeVOS;
    }

    public List<AssGiftVO> getAssGiftVOS() {
        return assGiftVOS;
    }

    public void setAssGiftVOS(List<AssGiftVO> assGiftVOS) {
        this.assGiftVOS = assGiftVOS;
    }

    public List<LadderVO> getLadderVOS() {
        return ladderVOS;
    }

    public void setLadderVOS(List<LadderVO> ladderVOS) {
        this.ladderVOS = ladderVOS;
    }

    public long getCoupno() {
        return coupno;
    }

    public void setCoupno(long coupno) {
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

    public int getQlfval() {
        return qlfval;
    }

    public void setQlfval(int qlfval) {
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
}