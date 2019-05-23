package com.onek.user;

import com.onek.context.AppContext;
import com.onek.annotation.UserPermission;
import com.onek.entitys.Result;
import com.onek.user.operations.StoreCustomerOp;
import com.onek.user.operations.UpdateStoreOp;
import com.onek.util.RoleCodeCons;
import dao.BaseDAO;
import util.GsonUtils;
import util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static constant.DSMConst.TB_COMP;
import static constant.DSMConst.TB_SYSTEM_USER;

/**
 * @Author: leeping
 * @Date: 2019/3/20 14:26
 * 门店管理
 */
public class StoreManageModule {

    //查询是否存在客服专员
    @UserPermission(ignore = true)
    public Result existStoreCustomer(AppContext appContext){
        String json = appContext.param.json;
        StoreCustomerOp op = GsonUtils.jsonToJavaBean(json, StoreCustomerOp.class);
        assert op!=null;
        op.type = 0;
        return op.execute(appContext);
    }

    /**
     * 门店关联客服专员
     */
    @UserPermission(ignore = true)
    public Result updateStoreCustomer(AppContext appContext){
        String json = appContext.param.json;
        StoreCustomerOp op = GsonUtils.jsonToJavaBean(json, StoreCustomerOp.class);
        assert op!=null;
        op.type = 1;
        return op.execute(appContext);
    }

    /**
     * 修改门店信息
     */
    @UserPermission(allowRoleList = {RoleCodeCons._STORE})
    public Result updateStoreInfo(AppContext appContext){
        String json = appContext.param.json;
        UpdateStoreOp op = GsonUtils.jsonToJavaBean(json, UpdateStoreOp.class);
        assert op!=null;
        return op.execute(appContext);
    }

    /**
     * 获取全部用户手机号码
     */
    @UserPermission(ignore = true)
    public List<String> getAllUserPhone(AppContext appContext){
        List<String> list = new ArrayList<>();
        try {
            String selectSql = "SELECT uphone FROM {{?"+ TB_SYSTEM_USER +"}} as a INNER JOIN {{?"+ TB_COMP +"}} AS b ON a.cid=b.cid WHERE b.ctype = 0";
            List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql);
            for (Object[] row : lines){
                String val = StringUtils.checkObjectNull(row[0],"");
                if (!StringUtils.isEmpty(val)) list.add(val+"");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 获取全部企业ID
     */
    @UserPermission(ignore = true)
    public List<String> getAllCompId(AppContext appContext){
        List<String> list = new ArrayList<>();
        try {
            String selectSql = "SELECT cid FROM {{?"+ TB_COMP +"}} WHERE ctype = 0";
            List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql);
            for (Object[] row : lines){
                String val = StringUtils.checkObjectNull(row[0],"");
                if (!StringUtils.isEmpty(val)) list.add(val+"");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /*
    * 根据角色码查询所有
     */
    @UserPermission(ignore = true)
    public HashMap<String,String> queryUserByRoleCode(AppContext appContext){
        HashMap<String,String> map = new HashMap<>();
        try {
            List<Integer> list = GsonUtils.json2List(appContext.param.json,Integer.class);
            if (list!=null && list.size()>0){
                final  String selectSql = "SELECT urealname,uphone FROM {{?"+ TB_SYSTEM_USER +"}} WHERE roleid&?>0";
                for (Integer code : list){
                    List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql,code);
                    for (Object[] row : lines){
                        String name = StringUtils.checkObjectNull(row[0],"");
                        String phone = StringUtils.checkObjectNull(row[1],"");
                        map.put(phone,name);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }



    /**
     * 根据企业码->手机号码
     */
    @UserPermission(ignore = true)
    public String getSpecifyUserPhone(AppContext context){
        String compId = context.param.arrays[0];
        String selectSql = "SELECT uphone FROM {{?"+ TB_SYSTEM_USER +"}} WHERE cid = ?";
        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql,compId);
        if (lines.size() == 1){
            return lines.get(0)[0].toString();
        }
        return "";
    }
}
