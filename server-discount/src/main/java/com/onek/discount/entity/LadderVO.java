package com.onek.discount.entity;

import java.util.List;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName LadderVO
 * @Description TODO
 * @date 2019-04-02 11:20
 */
public class LadderVO {

    //阶梯码
    private String unqid;

    //金额阶梯值
    private double ladamt;

    //数量阶梯值
    private int ladnum;

    private int offercode;//优惠码

    //优惠值
    private double offer;

    //综合状态
    private int cstatus;

    private List<AssGiftVO> assGiftVOS;

    private String assgiftno;//赠换商品码

    private String giftname;//商品名称

    public double getLadamt() {
        return ladamt;
    }

    public void setLadamt(double ladamt) {
        this.ladamt = ladamt;
    }

    public int getLadnum() {
        return ladnum;
    }

    public void setLadnum(int ladnum) {
        this.ladnum = ladnum;
    }

    public double getOffer() {
        return offer;
    }

    public void setOffer(double offer) {
        this.offer = offer;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }

    public String getUnqid() {
        return unqid;
    }

    public void setUnqid(String unqid) {
        this.unqid = unqid;
    }

    public int getOffercode() {
        return offercode;
    }

    public void setOffercode(int offercode) {
        this.offercode = offercode;
    }

    public String getAssgiftno() {
        return assgiftno;
    }

    public void setAssgiftno(String assgiftno) {
        this.assgiftno = assgiftno;
    }

    public String getGiftname() {
        return giftname;
    }

    public void setGiftname(String giftname) {
        this.giftname = giftname;
    }

    public List<AssGiftVO> getAssGiftVOS() {
        return assGiftVOS;
    }

    public void setAssGiftVOS(List<AssGiftVO> assGiftVOS) {
        this.assGiftVOS = assGiftVOS;
    }
}
