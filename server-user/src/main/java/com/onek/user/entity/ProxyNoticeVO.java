package com.onek.user.entity;

import java.util.List;

/**
 * @author 11842
 * @version 1.1.1
 * @description 区域广播
 * @time 2019/5/28 16:32
 **/
public class ProxyNoticeVO {
    private String msgid;//公告id
    private int sender;//发送人
    private String msgtxt;//消息内容
    private String createdate;//创建日期
    private String createtime;//创建时间
    private String invdtime;//失效时间
    private int readtimes;//强制阅读次数 0代表不强制阅读
    private String effcttime;//生效时间
    private int revobj;//发送对象（角色叠加码）
    private int cstatus;//综合状态码 1 删除
    private int roleid;//发送人角色码
    private String title;//公告标=标题
    private String content;//公告内容
    private List<Long> areaList;//公告发送地区码集合

    public String getMsgid() {
        return msgid;
    }

    public void setMsgid(String msgid) {
        this.msgid = msgid;
    }

    public int getSender() {
        return sender;
    }

    public void setSender(int sender) {
        this.sender = sender;
    }

    public String getMsgtxt() {
        return msgtxt;
    }

    public void setMsgtxt(String msgtxt) {
        this.msgtxt = msgtxt;
    }

    public String getCreatedate() {
        return createdate;
    }

    public void setCreatedate(String createdate) {
        this.createdate = createdate;
    }

    public String getInvdtime() {
        return invdtime;
    }

    public void setInvdtime(String invdtime) {
        this.invdtime = invdtime;
    }

    public int getReadtimes() {
        return readtimes;
    }

    public void setReadtimes(int readtimes) {
        this.readtimes = readtimes;
    }

    public int getRevobj() {
        return revobj;
    }

    public void setRevobj(int revobj) {
        this.revobj = revobj;
    }

    public String getCreatetime() {
        return createtime;
    }

    public void setCreatetime(String createtime) {
        this.createtime = createtime;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Long> getAreaList() {
        return areaList;
    }

    public void setAreaList(List<Long> areaList) {
        this.areaList = areaList;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getEffcttime() {
        return effcttime;
    }

    public void setEffcttime(String effcttime) {
        this.effcttime = effcttime;
    }

    public int getRoleid() {
        return roleid;
    }

    public void setRoleid(int roleid) {
        this.roleid = roleid;
    }
}
