package com.onek.user.operations;

import com.onek.context.AppContext;
import com.onek.context.UserSession;
import dao.BaseDAO;
import util.StringUtils;

import java.util.Arrays;
import java.util.List;

import static Ice.Application.communicator;
import static constant.DSMConst.TB_SYSTEM_ROLE;
import static constant.DSMConst.TB_SYSTEM_USER;

/**
 * @Author: leeping
 * @Date: 2019/5/31 11:55
 */
public abstract class LoginAbsOp {

    String error = "登录失败";
    int type; //客户端类型
    UserSession userSession = null;

    protected LoginAbsOp(int type) {
        this.type = type;
    }

    boolean checkAccountPassword(String account, String passwrod){
        if (StringUtils.isEmpty(account,passwrod)) {
            error = "手机号或密码不能为空";
            return false;
        }
        return true;
    }

    boolean checkUserRoleStatus(long urole,int ucstatus){
        if ((ucstatus&32)>0){
            error = "用户账号停止使用";
            return false;
        }
        //获取系统有效角色列表
        String selectSql = "SELECT cstatus,roleid FROM {{?" + TB_SYSTEM_ROLE +"}} WHERE cstatus&32=0";
        List<Object[]> lines2 = BaseDAO.getBaseDAO().queryNative(selectSql);
        boolean isAllow = false;
        for (Object[] o : lines2){
            int cstatus = StringUtils.checkObjectNull(o[0],0);
            if ((cstatus&type)>0){
                long roleid = StringUtils.checkObjectNull(o[1],0L);
                if ((urole & roleid)>0) {
                    isAllow = true;
                    break;
                }
            }
        }
        if (!isAllow){
            error = "用户角色权限不足";
            return false;
        }

        return true;
    }

    boolean recodeLoginInfo(String ipAddress,int userid){
        //密码正确 - 记录登陆时间 IP
        String updateSql = "UPDATE {{?" + TB_SYSTEM_USER + "}} SET ip=?, logindate=CURRENT_DATE, logintime=CURRENT_TIME WHERE uid=?";
        int i = BaseDAO.getBaseDAO().updateNative(updateSql, ipAddress,userid);
        if (i<=0){
            error = "系统异常,拒绝登陆";
            return false;
        }
        return true;
    }

    //检查用户是否正确
    boolean checkSqlAndUserExist(String ipAddress,String phone,String password){
        //0-用户码 1-角色码 2-状态码 3-用户手机号 4-用户MD5密码 5-用户名 6-用户企业码 7-用户所属
        String selectSql = "SELECT uid,roleid,cstatus,uphone,upw,urealname,cid,belong " +
                "FROM {{?" + TB_SYSTEM_USER + "}} " +
                "WHERE cstatus&1=0 AND uphone=?";

        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql,phone);

        if (lines.size()==1){
            Object[] objects = lines.get(0);
            communicator().getLogger().print("用户登录: "+ Arrays.toString(objects));
            int uid = StringUtils.checkObjectNull(objects[0],0);
            long urole = StringUtils.checkObjectNull(objects[1],0L);
            int ucstatus = StringUtils.checkObjectNull(objects[2],0);
            String uphone = StringUtils.checkObjectNull(objects[3],"");
            String upassword = StringUtils.checkObjectNull(objects[4],"");
            String uname = StringUtils.checkObjectNull(objects[5],"");
            int compid = StringUtils.checkObjectNull(objects[6],0);
            int belong = StringUtils.checkObjectNull(objects[7],0);

            if (StringUtils.isEmpty(upassword)){
                error = "用户未设置初始密码";
                return false;
            }

            //密码忽略MD5大小写
            if (upassword.equalsIgnoreCase(password)) {

                //检查用户是否停用,角色是否有效
                if (checkUserRoleStatus(urole, ucstatus) && recodeLoginInfo(ipAddress,uid)){
                    loginSuccess();
                    //赋值用户信息
                    userSession = UserSession.genUserSession(uid,urole,uphone,password,ipAddress,uname,compid,belong);
                    return true;
                }
            }else passwordError();
        }else error = "用户不存在";
        return false;
    }

    //关联用户
    boolean relationTokenUserSession(AppContext context,boolean flag) {
        context.setUserSession(userSession);
        if (flag) context.storeUserMappingToken();//防止多点登录设置
        return context.relationTokenUserSession();//关联用户信息
    }

    void loginSuccess(){

    }

    void passwordError(){

    }

}
