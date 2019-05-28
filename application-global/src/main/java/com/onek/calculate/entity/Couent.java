package com.onek.calculate.entity;

import com.onek.calculate.service.AccurateMath;
import com.onek.calculate.util.DiscountUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Couent extends AccurateMath implements IDiscount {
    private int oid;
    private long unqid;
    private long coupno;
    private int compid;
    private String startdate;
    private String starttime;
    private String enddate;
    private String endtime;
    private int brulecode;
    private String rulename;
    private int goods;
    private String ladder;
    private int glbno;
    private int ctype;
    private int cstatus;
    private int reqflag;

    private boolean freeShipping;
    private double discounted;
    private List<IProduct> productList = new ArrayList<>();

    private Ladoff currLadoff;
    private Ladoff nextLadoff;
    private double nextGapAmt;
    private int nextGapNum;


    public long getUnqid() {
        return unqid;
    }

    public void setUnqid(long unqid) {
        this.unqid = unqid;
    }

    public long getCoupno() {
        return coupno;
    }

    public void setCoupno(long coupno) {
        this.coupno = coupno;
    }

    public int getCompid() {
        return compid;
    }

    public void setCompid(int compid) {
        this.compid = compid;
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

    public String getLadder() {
        return ladder;
    }

    public void setLadder(String ladder) {
        this.ladder = ladder;
    }

    public void setEnddate(String enddate) {
        this.enddate = enddate;
    }

    public int getBrulecode() {
        return brulecode;
    }

    public void setBrulecode(int brulecode) {
        this.brulecode = brulecode;
    }

    public String getRulename() {
        return rulename;
    }

    public void setRulename(String rulename) {
        this.rulename = rulename;
    }

    public int getGoods() {
        return goods;
    }

    public void setGoods(int goods) {
        this.goods = goods;
    }

    public int getGlbno() {
        return glbno;
    }

    public void setGlbno(int glbno) {
        this.glbno = glbno;
    }

    public int getCtype() {
        return ctype;
    }

    public void setCtype(int ctype) {
        this.ctype = ctype;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }

    public int getReqflag() {
        return reqflag;
    }

    public void setReqflag(int reqflag) {
        this.reqflag = reqflag;
    }

    @Override
    public long getDiscountNo() {
        return this.unqid;
    }

    @Override
    public long getBRule() {
        return this.brulecode;
    }

    @Override
    public int getPriority() {
        return -1;
    }

    @Override
    public void setDiscounted(double discount) {
        if (discount <= 0) {
            return;
        }
        List<IProduct> prodList = getProductList();

        double[] shared = DiscountUtil.shareDiscount(
                DiscountUtil.getEachCurrent(prodList), discount);

        IProduct product;
        double befDiscounted;
        double totalDiscounted = .0;
        for (int i = 0; i < shared.length; i++) {
            product = prodList.get(i);
            befDiscounted = product.getDiscounted();
            product.addDiscounted(sub(product.getCurrentPrice(), shared[i]));
            totalDiscounted = add(totalDiscounted,
                    sub(product.getDiscounted(), befDiscounted));
        }

        this.discounted = totalDiscounted;
    }

    @Override
    public double getDiscounted() {
        return this.discounted;
    }

    @Override
    public void addProduct(IProduct product) {
        productList.add(product);
    }

    @Override
    public List<IProduct> getProductList() {
        return this.productList;
    }

    @Override
    public void setFreeShipping(boolean free) {
        this.freeShipping = free;
    }

    @Override
    public boolean getFreeShipping() {
        return this.freeShipping;
    }

    @Override
    public boolean getExCoupon() {
        return false;
    }

    @Override
    public void setLimits(long sku, int limits) {
    }

    @Override
    public int getLimits(long sku) {
        return 0;
    }

    @Override
    public void setActionPrice(long sku, double price) {

    }

    @Override
    public double getActionPrice(long sku) {
        return 0;
    }

    @Override
    public String getStartTime() {
        return "00:00:00";
    }

    @Override
    public String getEndTime() {
        return "23:59:59";
    }

    @Override
    public void addGift(Gift gift) {

    }

    @Override
    public void addGifts(List<Gift> gifts) {
    }

    @Override
    public List<Gift> getGiftList() {
        return Collections.EMPTY_LIST;
    }

    public Ladoff getCurrLadoff() {
        return currLadoff;
    }

    @Override
    public void setCurrLadoff(Ladoff currLadoff) {
        this.currLadoff = currLadoff;
    }

    public Ladoff getNextLadoff() {
        return nextLadoff;
    }

    @Override
    public void setNextLadoff(Ladoff nextLadoff) {
        this.nextLadoff = nextLadoff;
    }

    public double getNextGapAmt() {
        return nextGapAmt;
    }

    @Override
    public void setNextGapAmt(double nextGapAmt) {
        this.nextGapAmt = nextGapAmt;
    }

    public int getNextGapNum() {
        return nextGapNum;
    }

    public void setNextGapNum(int nextGapNum) {
        this.nextGapNum = nextGapNum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Couent couent = (Couent) o;

        return unqid == couent.unqid;
    }

    @Override
    public int hashCode() {
        return (int) (unqid ^ (unqid >>> 32));
    }
}
