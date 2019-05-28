package com.onek.user.interactive;

/**
 * @Author: leeping
 * @Date: 2019/3/26 14:59
 */
public class AuditInfo {
    //可能存在的查询条件:
    public String phone; //手机
    public String company; //公司名
    public String status; //状态

    //需要返回:
    public String companyId; //公司码
    public String examine;//审核失败原因
    public String auditerId;//审核人
    public String address;//营业执照地址
    public String submitDate;//提交时间
    public String auditDate;//审核时间
    public String addressCode;//地区码
    //共有
    public String cursorId;//客服专员ID
    public String cursorName;//客户专员姓名
    public String cursorPhone;//客户手机号码

    //资质信息
    public AptitudeInfo cardInfo = new AptitudeInfo();

}
