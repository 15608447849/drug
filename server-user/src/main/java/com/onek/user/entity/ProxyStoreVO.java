package com.onek.user.entity;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName ProxyStore
 * @Description TODO
 * @date 2019-06-01 12:02
 */
public class ProxyStoreVO {

    public String phone; //手机
    public int uid; //用户码
    public String company; //公司名
    public String addressCode;//地区码
    public String address;//地址
    private String createdate;
    private String createtime;
    public int status; //状态
    public int companyId; //公司码
    public int cursorId;//客服专员ID
    public String cursorName;//客户专员姓名
    public String cursorPhone;//客户手机号码
    private String province; //省
    private String city; //市
    private String region; //区域
    private int bdmid; //bdm id
    private String bdmn; //bdm 名称
    private int control;//控销协议

    public int getBdmid() {
        return bdmid;
    }

    public void setBdmid(int bdmid) {
        this.bdmid = bdmid;
    }

    public String getBdmn() {
        return bdmn;
    }

    public void setBdmn(String bdmn) {
        this.bdmn = bdmn;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getAddressCode() {
        return addressCode;
    }

    public void setAddressCode(String addressCode) {
        this.addressCode = addressCode;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

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

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }

    public int getCursorId() {
        return cursorId;
    }

    public void setCursorId(int cursorId) {
        this.cursorId = cursorId;
    }

    public String getCursorName() {
        return cursorName;
    }

    public void setCursorName(String cursorName) {
        this.cursorName = cursorName;
    }

    public String getCursorPhone() {
        return cursorPhone;
    }

    public void setCursorPhone(String cursorPhone) {
        this.cursorPhone = cursorPhone;
    }

    public int getControl() {
        return control;
    }

    public void setControl(int control) {
        this.control = control;
    }
}
