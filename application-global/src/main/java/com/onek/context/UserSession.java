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

    public static UserSession genUserSession(int userId, long roleCode, String phone, String lastId, String userName, int compId, int belong){
        UserSession userSession = new UserSession(userId,roleCode,phone);
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
        return userSession;
    }



}
