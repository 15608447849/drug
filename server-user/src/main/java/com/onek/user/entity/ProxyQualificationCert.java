package com.onek.user.entity;

/**
 * @author liuhui
 * @version V1.0
 * @ClassName ProxyQualificationCert
 * @Description TODO
 * @date 2019-06-11 10:34
 */
public class ProxyQualificationCert {

    private int atype;

    private int compid;

    private int aptid;

    private String certificateno;

    private String validitys;

    private String validitye;

    private String cstatus;

    public String getValiditye() {
        return validitye;
    }

    public void setValiditye(String validitye) {
        this.validitye = validitye;
    }

    public int getAtype() {
        return atype;
    }

    public void setAtype(int atype) {
        this.atype = atype;
    }

    public int getCompid() {
        return compid;
    }

    public void setCompid(int compid) {
        this.compid = compid;
    }

    public int getAptid() {
        return aptid;
    }

    public void setAptid(int aptid) {
        this.aptid = aptid;
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

    public String getCstatus() {
        return cstatus;
    }

    public void setCstatus(String cstatus) {
        this.cstatus = cstatus;
    }
}
