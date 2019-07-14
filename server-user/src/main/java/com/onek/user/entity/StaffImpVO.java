package com.onek.user.entity;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName StaffImpVO
 * @Description TODO
 * @date 2019-07-13 14:49
 */
public class StaffImpVO {

    /**
     * 人员姓名
     */
    private String uname = "";

    /**
     * 人员手机
     */
    private String uphone = "";

    /**
     * 人员角色
     */
    private String roleName = "";

    /**
     * 合伙人名称
     */
    private String partnerName = "";

    /**
     * 所属渠道经理
     */
    private String bmanager = "";

    /**
     * 所属合伙人
     */
    private String bpartner = "";

    /**
     * 所属BDM
     */
    private String bbdm = "";

    /**
     * 管辖区域
     */
    private String arean = "";


    public String getUname() {
        return uname;
    }

    public void setUname(String uname) {
        this.uname = uname;
    }

    public String getUphone() {
        return uphone;
    }

    public void setUphone(String uphone) {
        this.uphone = uphone;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public void setPartnerName(String partnerName) {
        this.partnerName = partnerName;
    }

    public String getBmanager() {
        return bmanager;
    }

    public void setBmanager(String bmanager) {
        this.bmanager = bmanager;
    }

    public String getBpartner() {
        return bpartner;
    }

    public void setBpartner(String bpartner) {
        this.bpartner = bpartner;
    }

    public String getBbdm() {
        return bbdm;
    }

    public void setBbdm(String bbdm) {
        this.bbdm = bbdm;
    }

    public String getArean() {
        return arean;
    }

    public void setArean(String arean) {
        this.arean = arean;
    }
}
