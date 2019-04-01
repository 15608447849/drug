package com.onek.discount.entity;

/**
 * @author 11842
 * @version 1.1.1
 * @description 活动
 * @time 2019/4/1 13:46
 **/
public class ActivityVO {
    private long unqid;//全局唯一
    private long actcode;//活动码
    private String actname;//活动名称
    private int incpriority;//互斥优先级
    private int cpriority;//兼容优先级
    private int qualcode;//资格码
    private int qualvalue;//资格值
    private String actdesc;//活动描述
    private int excdiscount;//排斥优惠券
    private int acttype;//活动周期类型
    private long actcycle;//活动周期
    private String sdate;//活动开始日期
    private String edate;//活动结束日期
    private int rulecode;//活动规则码
    private int cstatus;//综合状态码

}
