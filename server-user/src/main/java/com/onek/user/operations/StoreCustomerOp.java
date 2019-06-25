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
import static constant.DSMConst.TB_PROXY_UAREA;
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
    String areac;//门店地区码
    int storetype;//药店类型  0 医疗单位, 1 批发企业, 2零售连锁门店, 3零售单体门店

    @Override
    public Result execute(AppContext context) {

        String msg = "";
        //更新门店类型
        String updStType = "UPDATE {{?"+TB_COMP+"}} SET storetype=? WHERE ctype=0 AND cid=?";
        int code = BaseDAO.getBaseDAO().updateNative(updStType,storetype,compid);
        if (code < 0) {
            return new Result().fail("门店类型保存失败！");
        }
        if (type == -1) return new Result().success(queryAllDB(name));
        int cid = 0;
        if (!StringUtils.isEmpty(uphone)) {
            if (type >= 0){
                //根据手机号码 , 查询是否存在
                String selectSql = "SELECT uid,cid FROM {{?" + TB_SYSTEM_USER +"}} WHERE cstatus&1=0 AND (roleid&?>0 OR roleid&?>0) AND uphone=?";
                List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql,
                        RoleCodeCons._DBM,
                        RoleCodeCons._DB,
                        uphone);
                if (lines.size() == 1 && lines.get(0)[0] != null){
                    uid = lines.get(0)[0];
                    cid = Integer.parseInt(lines.get(0)[1].toString());
                    String aSql =  "select arearng from {{?"+TB_PROXY_UAREA+"}} where uid = ? and areac = ? and cstatus & 1 = 0 ";
                    List<Object[]> ret = BaseDAO.getBaseDAO().queryNative(aSql,uid,areac);
                    if(ret == null || ret.isEmpty()){
                        msg = "您的门店不在该商务经理服务范围，确定操作？";
                        uid = null;
                    }
                }else{
                    msg = "商务经理不存在！";
                }
            }
            if (type >= 1 && uid!=null && compid>0){
                //公司-客服专员 关联
                String updateSql = "UPDATE {{?"+TB_COMP+"}} SET inviter=?,invitercid=? WHERE ctype=0 AND cid=?";
                int i = BaseDAO.getBaseDAO().updateNative(updateSql,uid,cid,compid);
                if (i<=0) uid = null;
            }
        }


        return new Result().success(uid!=null).message(msg);
    }

    private static List<StoreCustomerOp> queryAllDB(String name) {
        List<StoreCustomerOp> list = new ArrayList<>();
        try {
            if (name == null) name = "";
            String selectSql = "SELECT uid,urealname,uphone,belong FROM {{?" + TB_SYSTEM_USER +"}} WHERE cstatus&1=0 AND (roleid&?>0 OR roleid&?>0) AND urealname LIKE '%"+name+"%'";
            List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql,  RoleCodeCons._DBM, RoleCodeCons._DB);
            for (Object[] rows : lines){
                StoreCustomerOp data = new StoreCustomerOp();
                data.uid = StringUtils.checkObjectNull(rows[0],0);
                data.name = StringUtils.checkObjectNull(rows[1],"");
                data.uphone = StringUtils.checkObjectNull(rows[2],"");
                data.belong = StringUtils.checkObjectNull(rows[3],0);
                list.add(data);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
