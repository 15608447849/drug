package com.onek.global;

import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.util.dict.DictEntity;
import constant.DSMConst;
import dao.BaseDAO;
import util.GsonUtils;

import java.util.List;

/**
 * @Author: leeping
 * @Date: 2019/4/11 15:29
 */
public class DictUtilRemoteModule {
    private static BaseDAO baseDao = BaseDAO.getBaseDAO();

    @UserPermission(ignore = true)
    public Result getId(AppContext appContext) {
        List<Object[]> result = baseDao.queryNative("select * from {{?"+ DSMConst.D_GLOBAL_DICT +"}} where cstatus&1= 0 and dictc = ?", appContext.param.arrays[0]);
        DictEntity[] dicts = new DictEntity[result.size()];
        baseDao.convToEntity(result, dicts, DictEntity.class);
        return new Result().success(GsonUtils.javaBeanToJson(dicts[0]));
    }

    @UserPermission(ignore = true)
    public Result queryAll(AppContext appContext) {
        List<Object[]> result = baseDao.queryNative("select * from {{?"+ DSMConst.D_GLOBAL_DICT +"}} where cstatus&1= 0");
        DictEntity[] dicts = new DictEntity[result.size()];
        baseDao.convToEntity(result, dicts, DictEntity.class);
        return new Result().success(GsonUtils.javaBeanToJson(dicts));
    }

    @UserPermission(ignore = true)
    public Result queryByParams(AppContext appContext) {
        String [] params = appContext.param.arrays;
        List<Object[]> result = baseDao.queryNative("select * from {{?"+ DSMConst.D_GLOBAL_DICT +"}} where cstatus&1= 0", params[0], params[1]);
        DictEntity[] dicts = new DictEntity[result.size()];
        baseDao.convToEntity(result, dicts, DictEntity.class);
        return new Result().success(GsonUtils.javaBeanToJson(dicts));
    }

}
