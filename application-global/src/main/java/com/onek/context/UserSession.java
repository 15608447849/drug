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
    public int orderServerIndex = -1; //用户服务调用下标 规则: 企业码/8%65535
    private UserSession(){}


    private static UserSession createUser(int userId,long roleCode,String phone,String lastId,String password){
        UserSession userSession = new UserSession();
        userSession.userId = userId;
        userSession.roleCode = roleCode;
        userSession.phone = phone;
        userSession.password = password;
        userSession.lastIp = lastId;
        return userSession;
    }

    public static UserSession createStoreUser(int userId,long roleCode,String phone,String lastId,String password,int compId){
        UserSession userSession = createUser(userId,roleCode,phone,lastId,password);
        userSession.compId = compId;
        userSession.orderServerIndex = genOrderServerIndex(userSession.compId);
        return userSession;
    }
    //获取订单服务的下标规则
    private static int genOrderServerIndex(int compId){
        return compId/8%65535;
    }

    public static UserSession createBackManagerUser(int userId,long roleCode,String phone,String lastId,String password,String account,String userName){
        UserSession userSession = createUser(userId,roleCode,phone,lastId,password);
        userSession.account =  account;
        userSession.userName = userName;
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

}
