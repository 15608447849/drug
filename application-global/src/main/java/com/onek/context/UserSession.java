package com.onek.context;


import java.io.Serializable;

/**
 * lzp
 * 用户会话信息 - 登陆自动关联创建用户信息 全局可用
 */
public class UserSession implements Serializable{
    public int userId; //用户ID
    public long roleCode; //角色复合码
    public String phone; //手机号码
    public String password;//密码MD5
    public String lastIp;//最后登录IP
    public String userName;//姓名
    public int compId;//企业ID
    public int belong;//所属ID
    public StoreBasicInfo comp; //公司信息

    private UserSession(){}

    //创建用户
   public UserSession(int userId,long roleCode,String phone){
       this.userId = userId;
       this.roleCode = roleCode;
       this.phone = phone;
   }

    public static UserSession genUserSession(int userId, long roleCode, String phone,String password, String lastId, String userName, int compId, int belong){
        UserSession userSession = new UserSession(userId,roleCode,phone);
        userSession.password = password;
        userSession.lastIp = lastId;
        userSession.userName = userName;
        userSession.compId = compId;
        userSession.belong = belong;
        return userSession;
    }


    //复制门店用户信息
    public UserSession cloneStoreUserInfo(StoreBasicInfo info) {
        UserSession userSession = new UserSession();
        userSession.userId = userId;
        userSession.roleCode = roleCode;
        userSession.phone = phone;
        userSession.compId = compId;
        userSession.comp = info;
        return userSession;
    }

    //复制后台用户信息
    public UserSession cloneBackUserInfo(){
        UserSession userSession = new UserSession();
        userSession.userId = userId;
        userSession.roleCode = roleCode;
        userSession.userName = userName;
        userSession.phone = phone;
        userSession.belong = belong;
        userSession.compId = compId;
        return userSession;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append( "用户码:"+userId+",角色码:"+roleCode+" ,手机号码:"+phone );
        if (compId>0) sb.append(" ,门店编码:"+compId);
        if (belong>0) sb.append(" ,归属编码:"+belong);
        if (comp != null) sb.append(" ,门店状态:"+ comp.authenticationStatus);

        return sb.toString();

    }


}
