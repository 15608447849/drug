package com.onek;

import java.io.Serializable;

public class UserSession implements Serializable {
    public int userId; //用户ID
    public int compId;//企业ID
    public long roleCode; //用户角色码
    public String account; //用户账号
    public String phone; //用户手机号码
    public String password;//用户密码
    public String lastIp;//最后登录IP
    public String userName;//用户姓名
}
