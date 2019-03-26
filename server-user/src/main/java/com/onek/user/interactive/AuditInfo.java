package com.onek.user.interactive;

/**
 * @Author: leeping
 * @Date: 2019/3/26 14:59
 */
public class AuditInfo {
    //查询条件:
    public String phone; //手机
    public String company; //公司名
    public String submitDate; //提交日期
    public String submitTime; //提交时间
    public String auditDate; //审核日期
    public String auditTime; //审核时间
    public String status; //状态
    public String auditer;//审核人

    //需要返回:
    public String companyId; //公司码
    public String createDate; //创建日期
    public String createTime; //创建时间
    public String examine;//审核失败原因

}
