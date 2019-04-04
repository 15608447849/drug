package com.onek.user.operations;

import com.onek.context.AppContext;
import com.onek.context.UserSession;
import com.onek.entitys.IOperation;
import com.onek.entitys.Result;
import constant.DSMConst;
import dao.BaseDAO;
import util.StringUtils;

import java.util.List;

import static Ice.Application.communicator;

/**
 * @Author: leeping
 * @Date: 2019/3/12 15:05
 */
public class LoginBackOp implements IOperation<AppContext> {

    String account;
    String password;
    private UserSession userSession = null;

    @Override
    public Result execute(AppContext context) {
        try {
            if (StringUtils.isEmpty(account) || StringUtils.isEmpty(password)) return new Result().fail("用户名或密码不能为空");
            //检测用户名/密码是否正确
            if (!checkSqlAndUserExist(context)) return new Result().fail("用户名或密码不正确");
            //关联token-用户信息
            if (relationTokenUserSession(context)) return new Result().success("登陆成功");
            else return new Result().success("无法关联用户信息");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Result().fail("登陆失败");
    }

    private boolean relationTokenUserSession(AppContext context) {
        context.setUserSession(userSession);
        return context.relationTokenUserSession();
    }

    //检查用户是否正确
    private boolean checkSqlAndUserExist(AppContext context) {

        String selectSql = "SELECT uid,roleid,upw,uaccount,uphone,urealname " +
                "FROM {{?" + DSMConst.D_SYSTEM_USER + "}} " +
                "WHERE cstatus&1 = 0 AND roleid&1 > 0 AND uaccount = ? OR uphone = ?";
        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql,account,account);

        if (lines.size()>0){
            Object[] objects = lines.get(0);
            communicator().getLogger().print("管理员登录: 用户码:" + objects[0]+" ,角色码:"+ objects[1]+" ,姓名:"+objects[5]);
            if (objects[2].toString().equalsIgnoreCase(password)) { //忽略MD5大小写
                //密码正确 - 记录登陆时间 IP
                String updateSql = "UPDATE {{?" + DSMConst.D_SYSTEM_USER + "}} " +
                        "SET ip = ?,logindate = CURRENT_DATE,logintime = CURRENT_TIME " +
                        "WHERE cstatus&1 = 0 AND uid = ?";
                int i = BaseDAO.getBaseDAO().updateNative(updateSql, context.remoteIp,objects[0]);
                if (i > 0){
                    userSession = new UserSession();
                    userSession.userId = (int) objects[0];
                    userSession.roleCode = (long) objects[1];
                    userSession.account =  objects[3].toString();
                    userSession.phone =  objects[4].toString();
                    userSession.userName = objects[5].toString();
                    userSession.password = password;
                    userSession.lastIp = context.remoteIp;
                    return true;
                }
            }
        }
        return false;
    }




}
