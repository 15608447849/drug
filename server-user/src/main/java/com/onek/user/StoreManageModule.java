package com.onek.user;

import com.onek.context.AppContext;
import com.onek.annotation.UserPermission;
import com.onek.entitys.Result;
import com.onek.user.operations.UpdateStoreOp;
import dao.BaseDAO;
import util.GsonUtils;
import util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static constant.DSMConst.D_COMP;
import static constant.DSMConst.D_SYSTEM_USER;

/**
 * @Author: leeping
 * @Date: 2019/3/20 14:26
 * 门店管理
 */
public class StoreManageModule {
    /**
     * 新增门店企业信息
     */
    @UserPermission(allowRoleList = {2},allowedUnrelated = true)
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
        String selectSql = "SELECT uphone FROM {{?"+ D_SYSTEM_USER +"}} as a INNER JOIN {{?"+D_COMP+"}} AS b ON a.cid=b.cid WHERE b.ctype = 0";
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
            String selectSql = "SELECT cid FROM {{?"+D_COMP+"}} WHERE ctype = 0";
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
}
