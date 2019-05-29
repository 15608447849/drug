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
    private String error = "用户名或密码不正确";

    @Override
    public Result execute(AppContext context) {
        try {
            if (StringUtils.isEmpty(account) || StringUtils.isEmpty(password)) return new Result().fail("用户名或密码不能为空");
            //检测用户名/密码是否正确
            if (!checkSqlAndUserExist(context)) return new Result().fail(error);
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
        return context.relationTokenUserSession();//后台管理登陆-关联企业信息
    }

    //检查用户是否正确
    private boolean checkSqlAndUserExist(AppContext context) {

        String selectSql = "SELECT uid,roleid,upw,uphone,urealname,cstatus " +
                "FROM {{?" + DSMConst.TB_SYSTEM_USER + "}} " +
                "WHERE cstatus&1=0 " +
                "AND uphone=?";
        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql,account);

        if (lines.size()>0){
            Object[] objects = lines.get(0);

            if (objects[2].toString().equalsIgnoreCase(password)) { //忽略MD5大小写
                communicator().getLogger().print("管理/运营人员登录: 用户码:" + objects[0]+" ,角色码:"+ objects[1]+" ,姓名:"+objects[4]+" ,手机号:"+objects[3]);

                 //判断角色
                int role = StringUtils.checkObjectNull(objects[1],0);
                //获取有表效角色列
                selectSql = "SELECT cstatus,roleid FROM {{?" + DSMConst.TB_SYSTEM_ROLE +"}} ";
                List<Object[]> lines2 = BaseDAO.getBaseDAO().queryNative(selectSql);
                boolean isAllow = false;
                for (Object[] o : lines2){
                    int cstatus = StringUtils.checkObjectNull(o[0],0);
                    if ((cstatus&256)==256){
                        int roleid = StringUtils.checkObjectNull(o[1],0);
                        if ((role & roleid) == roleid) {
                            isAllow = true;
                            break;
                        }
                    }
                }
                if (!isAllow){
                    error = "用户("+objects[4]+")角色权限拒绝登陆";
                    return false;
                }

                int cstatus = StringUtils.checkObjectNull(objects[5],0);
                if ((cstatus&32)==32){
                    error = "用户("+objects[4]+")已被停止使用";
                    return false;
                }

                //密码正确 - 记录登陆时间 IP
                String updateSql = "UPDATE {{?" + DSMConst.TB_SYSTEM_USER + "}} " +
                        "SET ip = ?,logindate = CURRENT_DATE,logintime = CURRENT_TIME " +
                        "WHERE cstatus&1 = 0 AND uid = ?";
                int i = BaseDAO.getBaseDAO().updateNative(updateSql, context.remoteIp,objects[0]);
                if (i > 0){
                    userSession = UserSession.createBackManagerUser(
                            StringUtils.checkObjectNull(objects[0],0),
                            StringUtils.checkObjectNull(objects[1],0L),
                            StringUtils.checkObjectNull(objects[3],""),
                            context.remoteIp,
                            password,
                            StringUtils.checkObjectNull(objects[4],"")
                    );
                    return true;
                }
            }
        }
        return false;
    }




}
