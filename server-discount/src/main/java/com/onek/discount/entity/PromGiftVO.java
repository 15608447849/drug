package com.onek.discount.entity;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName PromGiftVO
 * @Description TODO
 * @date 2019-04-02 11:36
 */
public class PromGiftVO {

    //赠换商品码
    private String giftno;

    //赠换商品名称
    private String giftname;

    //商品描述
    private String desc;

    //综合状态码
    private int cstatus;

    private String createdate;
    private String createtime;
    private String updatedate;
    private String updatetime;

    public String getCreatedate() {
        return createdate;
    }

    public void setCreatedate(String createdate) {
        this.createdate = createdate;
    }

    public String getCreatetime() {
        return createtime;
    }

    public void setCreatetime(String createtime) {
        this.createtime = createtime;
    }

    public String getUpdatedate() {
        return updatedate;
    }

    public void setUpdatedate(String updatedate) {
        this.updatedate = updatedate;
    }

    public String getUpdatetime() {
        return updatetime;
    }

    public void setUpdatetime(String updatetime) {
        this.updatetime = updatetime;
    }

    public String getGiftno() {
        return giftno;
    }

    public void setGiftno(String giftno) {
        this.giftno = giftno;
    }

    public String getGiftname() {
        return giftname;
    }

    public void setGiftname(String giftname) {
        this.giftname = giftname;
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
