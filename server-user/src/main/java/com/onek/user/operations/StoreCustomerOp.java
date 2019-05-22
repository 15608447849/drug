package com.onek.user.operations;

import com.onek.context.AppContext;
import com.onek.entitys.IOperation;
import com.onek.entitys.Result;
import dao.BaseDAO;
import util.StringUtils;

import java.util.List;

import static constant.DSMConst.TB_COMP;
import static constant.DSMConst.TB_SYSTEM_USER;

/**
 * @Author: leeping
 * @Date: 2019/5/22 15:27
 */
public class StoreCustomerOp implements IOperation<AppContext> {
    public int type = 0;//0 - 查询 , 1 - 关联客服专员
    String uphone;//客服专员手机号码
    int compid;
    Object uid;//客服专员ID
    @Override
    public Result execute(AppContext context) {
        if (!StringUtils.isEmpty(uphone)) {
            if (type >= 0){
                //查询是否存在
                String selectSql = "SELECT uid FROM {{?" + TB_SYSTEM_USER +"}} WHERE cstatus&1=0 AND roleid&512>0 AND uphone=?";
                List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql,uphone);
                if (lines.size() == 1){
                    uid = lines.get(0)[0];
                }
            }
            if (type >= 1 && uid!=null && compid>0){
                //公司-客服专员 关联
                String updateSql = "UPDATE {{?"+TB_COMP+"}} SET inviter=? WHERE cid=?";
                int i = BaseDAO.getBaseDAO().updateNative(updateSql,uid,compid);
                if (i<=0) uid = null;
            }
        }
        return new Result().success(uid!=null );
    }
}
