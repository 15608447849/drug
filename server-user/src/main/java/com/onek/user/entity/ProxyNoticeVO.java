package com.onek.user.entity;

import java.util.List;

/**
 * @author 11842
 * @version 1.1.1
 * @description 区域广播
 * @time 2019/5/28 16:32
 **/
public class ProxyNoticeVO {
    private String msgid;
    private int sender;
    private String msgtxt;
    private String createdate;
    private String createtime;
    private String invdtime;
    private int readtimes;
    private String effcttime;
    private int revobj;
    private int cstatus;
    private String title;
    private String content;
    private List<Long> areaList;

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
}
