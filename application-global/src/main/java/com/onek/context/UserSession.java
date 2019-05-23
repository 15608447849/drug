package com.onek.context;


import java.io.Serializable;

/**
 * lzp
 * 用户会话信息 - 登陆自动关联创建用户信息 全局可用
 */
public class UserSession implements Serializable{
    public int userId; //用户ID
    public int compId;//企业ID
    public long roleCode; //用户角色码
    public String account; //用户账号
    public String phone; //用户手机号码
    public String password;//用户密码
    public String lastIp;//最后登录IP
    public String userName;//用户姓名
    public StoreBasicInfo comp; //公司信息

    private UserSession(){}

    public static UserSession createStoreUser(int userId,String phone){
        UserSession userSession = new UserSession();
        userSession.userId = userId;
        userSession.phone = phone;
        return userSession;
    }

    public static UserSession createUser(int userId,long roleCode,String phone,String lastId,String password){
        UserSession userSession = createStoreUser(userId,phone);
        userSession.roleCode = roleCode;
        userSession.password = password;
        userSession.lastIp = lastId;
        return userSession;
    }

    public static UserSession createStoreUser(int userId,long roleCode,String phone,String lastId,String password,int compId){
        UserSession userSession = createUser(userId,roleCode,phone,lastId,password);
        userSession.compId = compId;
        return userSession;
    }

    public static UserSession createBackManagerUser(int userId,long roleCode,String phone,String lastId,String password,String account,String userName){
        UserSession userSession = createUser(userId,roleCode,phone,lastId,password);
        userSession.account =  account;
        userSession.userName = userName;
        return userSession;
    }



    public UserSession cloneStoreUserInfo(StoreBasicInfo info) {
        UserSession userSession = new UserSession();
        userSession.userId = userId;
        userSession.roleCode = roleCode;
        userSession.phone = phone;
        userSession.comp = info;
        return userSession;
    }

    public UserSession cloneBackUserInfo(){
        UserSession userSession = new UserSession();
        userSession.userId = userId;
        userSession.roleCode = roleCode;
        userSession.userName = userName;
        userSession.account = account;
        userSession.phone = phone;
        return userSession;
    }

    @Override
    public String toString() {
        return "UserSession{" +
                "userId=" + userId +
                ", compId=" + compId +
                ", roleCode=" + roleCode +
                ", account='" + account + '\'' +
                ", phone='" + phone + '\'' +
                ", password='" + password + '\'' +
                ", lastIp='" + lastIp + '\'' +
                ", userName='" + userName + '\'' +
                '}';
    }

}
