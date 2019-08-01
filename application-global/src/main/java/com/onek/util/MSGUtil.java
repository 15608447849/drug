package com.onek.util;

/**
 * 全局提示信息工具类
 */
public class MSGUtil {
    //==================用户模块START================//
    /*登陆*/
    public final static String LOGIN_SUCCESS = "恭喜登陆成功";
    public final static String LOGIN_FAIL = "登陆失败";
    public final static String LOGIN_PHONE_AND_PWD_NOTNULL = "手机号或密码不能为空";
    public final static String LOGIN_USER_OR_PWD_ERROR = "用户名或者密码错误";
    public final static String LOGIN_VERIFICATION_CODE_ERROR = "验证码不正确";
    public final static String LOGIN_USER_LOCK = "账户已锁定";
    public final static String LOGIN_ACCOUNT_OR_ROLE_ERROR = "无效账户或角色";
    public final static String LOGIN_USER_ISSTOP = "用户账号停止使用";
    public final static String LOGIN_USER_POWER = "用户角色权限不足";
    public final static String LOGIN_SYSTEM_ERROR = "系统异常,拒绝登陆";
    public final static String LOGIN_NOT_INITPWD = "用户未设置初始密码";
    public final static String LOGIN_NOT_USER = "用户不存在";

    /*注册*/
    public final static String REG_SUCCESS = "注册成功,已添加用户信息";
    public final static String REG_FAIL = "注册失败,无法添加用户信息";
    public final static String REG_ISREG = "手机号已注册";
    public final static String REG_NOTREG = "手机号未注册";
    public final static String REG_INVAILD_PHONE = "无效的手机号码";
    public final static String REG_VERIFICATION_CODE_ERROR = "短信验证码不正确";
    public final static String REG_INCONSISTENT_PWD = "两次密码输入不一致";
    public final static String REG_FIAL_AND_NOTDELETE = "注册失败且无法删除此用户信息";

    public final static String REG_COMP_SUCCESS = "添加门店信息,关联成功";
    public final static String REG_COMP_FAIL = "注册失败,无法添加门店信息";
    public final static String REG_COMP_RE_ERROR = "此用户无法关联门店信息";
    public final static String REG_COMP_UPDATE_SUCCESS = "修改信息成功";
    public final static String REG_COMP_UPDATE_FAIL  = "无法修改信息";
    public final static String REG_COMP_MSGERROR = "用户信息异常";
    public final static String REG_COMP_CHOOSEAREA = "请选择区域";
    public final static String REG_COMP_NAME_AND_ADDRESS_ISNULL = "门店或地址未填写";
    public final static String REG_COMP_LICENSE_ISHAVE="存在已认证的相同营业执照地址或药店名称";
    //==================用户模块END==================//
}
