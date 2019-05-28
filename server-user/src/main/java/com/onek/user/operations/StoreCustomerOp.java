package com.onek.user.operations;

import com.onek.context.AppContext;
import com.onek.entitys.IOperation;
import com.onek.entitys.Result;
import com.onek.util.RoleCodeCons;
import dao.BaseDAO;
import util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static constant.DSMConst.TB_COMP;
import static constant.DSMConst.TB_SYSTEM_USER;

/**
 * @Author: leeping
 * @Date: 2019/5/22 15:27
 */
public class StoreCustomerOp implements IOperation<AppContext> {
    public int type = 0;//0 - 查询 , 1 - 关联客服专员 ,-1 查询所有客服专员信息,并模糊匹配姓名
    String uphone;//客服专员手机号码
    int compid;
    Object uid;//客服专员ID
    String name = "";
    int belong; //DB用户归属
    @Override
    public Result execute(AppContext context) {
        if (!StringUtils.isEmpty(uphone)) {
            if (type == -1) return new Result().success(queryAllDB(name));
            if (type >= 0){
                //根据手机号码 , 查询是否存在
                String selectSql = "SELECT uid FROM {{?" + TB_SYSTEM_USER +"}} WHERE cstatus&1=0 AND (roleid&?>0 OR roleid&?>0) AND uphone=?";
                List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql,
                        RoleCodeCons._DBM,
                        RoleCodeCons._DB,
                        uphone);
                if (lines.size() == 1){
                    uid = lines.get(0)[0];
                }
            }
            if (type >= 1 && uid!=null && compid>0){
                //公司-客服专员 关联
                String updateSql = "UPDATE {{?"+TB_COMP+"}} SET inviter=? WHERE ctype=0 AND cid=?";
                int i = BaseDAO.getBaseDAO().updateNative(updateSql,uid,compid);
                if (i<=0) uid = null;
            }
        }
        return new Result().success(uid!=null );
    }

    private static List<StoreCustomerOp> queryAllDB(String name) {
        List<StoreCustomerOp> list = new ArrayList<>();
        try {
            String selectSql = "SELECT uid,urealname,uphone,belong FROM {{?" + TB_SYSTEM_USER +"}} WHERE cstatus&1=0 AND (roleid&?>0 OR roleid&?>0) AND LIKE '%?%'";

            List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql,  RoleCodeCons._DBM, RoleCodeCons._DB,name);

            for (Object[] rows : lines){
                StoreCustomerOp data = new StoreCustomerOp();
                data.uid = StringUtils.checkObjectNull(rows[0],0);
                data.name = StringUtils.checkObjectNull(rows[1],"");
                data.uphone = StringUtils.checkObjectNull(rows[2],"");
                data.belong = StringUtils.checkObjectNull(rows[4],0);
                list.add(data);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
