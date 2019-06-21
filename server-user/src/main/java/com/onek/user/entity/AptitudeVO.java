package com.onek.user.entity;

/**
 * @author 11842
 * @version 1.1.1
 * @description 资质
 * @time 2019/6/20 15:30
 **/
public class AptitudeVO {
    private int aptid;//资质id唯一码
    private int compid;//企业id
    private int atype;//资质类型 10-营业执照 11-药店经营许可证 12-gsp认证 13-采购/提货委托书 14-采购/提货人员复印件 15-医疗机构执业许可证(医疗单位)
    private String certificateno;//证件编号
    private String validitys;//有效期始
    private String validitye;//有效期止
    private int cstatus;
    private String pname;//姓名

    public int getAptid() {
        return aptid;
    }

    public void setAptid(int aptid) {
        this.aptid = aptid;
    }

    public int getCompid() {
        return compid;
    }

    public void setCompid(int compid) {
        this.compid = compid;
    }

    public int getAtype() {
        return atype;
    }

    public void setAtype(int atype) {
        this.atype = atype;
    }

    public String getCertificateno() {
        return certificateno;
    }

    public void setCertificateno(String certificateno) {
        this.certificateno = certificateno;
    }

    public String getValiditys() {
        return validitys;
    }

    public void setValiditys(String validitys) {
        this.validitys = validitys;
    }

    public String getValiditye() {
        return validitye;
    }

    public void setValiditye(String validitye) {
        this.validitye = validitye;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }

    public String getPname() {
        return pname;
    }

    public void setPname(String pname) {
        this.pname = pname;
    }
}
