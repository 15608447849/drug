package com.onek.user.operations;

import com.onek.context.AppContext;
import com.onek.entitys.IOperation;
import com.onek.entitys.Result;
import com.onek.util.RoleCodeCons;
import dao.BaseDAO;

import java.util.List;

import static constant.DSMConst.TB_COMP;
import static constant.DSMConst.TB_SYSTEM_USER;

/**
 * @Author: leeping
 * @Date: 2019/3/12 15:05
 */
public class LoginBackOp extends LoginAbsOp implements IOperation<AppContext> {

    String account;
    String password;

    protected LoginBackOp() {
        super(256);
    }

    @Override
    public Result execute(AppContext context) {
        try {
            if (!checkAccountPassword(account,password)) return new Result().fail(error);
            //检测用户名/密码是否正确
            if (!checkSqlAndUserExist(context.remoteIp,account,password)) return new Result().fail(error);
            if (!partnerCheck(account))  return new Result().fail("该账号已被停用或该企业审核未通过！");
            //关联token-用户信息
            if (relationTokenUserSession(context,false)) return new Result().success("登陆成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Result().fail("登陆失败");
    }

    //判断当前手机是否是合伙人并且关联企业是否有效
    private boolean partnerCheck(String phone){
        String selectSql = "SELECT uid,roleid,IFNULL(cid,0) cid,belong FROM {{?" + TB_SYSTEM_USER + "}} " +
                "WHERE cstatus&33=0 AND uphone=?";

        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql,phone);

        if(lines == null || lines.isEmpty()){
            return false;
        }

        int uid = Integer.parseInt(lines.get(0)[0].toString());
        int roleid = Integer.parseInt(lines.get(0)[1].toString());
        if((roleid & (RoleCodeCons._PROXY_PARTNER+RoleCodeCons._DB+RoleCodeCons._DBM)) > 0){
            int cid = Integer.parseInt(lines.get(0)[2].toString());
            if(cid == 0){
                return false;
            }
            String queryCmp = "select 1 from {{?"+TB_COMP+"}} where cid = ? and ctype = 2 and (cstatus & 33 > 0 or cstatus & 256 = 0)";
            List<Object[]> extComp = BaseDAO.getBaseDAO().queryNative(queryCmp, cid);
            return extComp == null || extComp.isEmpty();
        }
        return true;
    }

}
