package com.onek.global;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.util.dict.DictEntity;
import constant.DSMConst;
import dao.BaseDAO;
import util.GsonUtils;
import util.StringUtils;

import java.util.List;

/**
 * @Author: leeping
 * @Date: 2019/4/11 15:29
 */
public class DictUtilRemoteModule {
    private static BaseDAO baseDao = BaseDAO.getBaseDAO();

    @UserPermission(ignore = true)
    public DictEntity getId(AppContext appContext) {
        List<Object[]> result = baseDao.queryNative("select * from {{?"+ DSMConst.TB_GLOBAL_DICT +"}} where cstatus&1= 0 and dictc = ?", appContext.param.arrays[0]);
        DictEntity[] dicts = new DictEntity[result.size()];
        baseDao.convToEntity(result, dicts, DictEntity.class);
        return dicts[0];
    }

    @UserPermission(ignore = true)
    public Result queryAll(AppContext appContext) {
        List<Object[]> result = baseDao.queryNative("select * from {{?"+ DSMConst.TB_GLOBAL_DICT +"}} where cstatus&1= 0");
        DictEntity[] dicts = new DictEntity[result.size()];
        baseDao.convToEntity(result, dicts, DictEntity.class);
        return new Result().success(GsonUtils.javaBeanToJson(dicts));
    }

    @UserPermission(ignore = true)
    public Result queryByParams(AppContext appContext) {
        String [] params = appContext.param.arrays;
        List<Object[]> result = baseDao.queryNative("select * from {{?"+ DSMConst.TB_GLOBAL_DICT +"}} where cstatus&1= 0 and customc = ? and type =?", params[0], params[1]);
        DictEntity[] dicts = new DictEntity[result.size()];
        baseDao.convToEntity(result, dicts, DictEntity.class);
        return new Result().success(GsonUtils.javaBeanToJson(dicts));
    }

    @UserPermission(ignore = true)
    public Result saveDictFromERP(AppContext appContext) {
        JSONObject json = JSON.parseObject(appContext.param.json);

        if (json == null) {
            return new Result().fail("参数错误！");
        }

        String type = json.getString("type");
        String text = json.getString("text");

        if (StringUtils.isEmpty(type, text)) {
            return new Result().fail("参数为空！");
        }

        int customc = json.getIntValue("customc");

        String sql = " SELECT IFNULL(COUNT(0), 0) "
                + " FROM {{?" + DSMConst.TB_GLOBAL_DICT + "}} "
                + " WHERE cstatus&1 = 0 AND customc = ? AND type = ? ";

        List<Object[]> queryResult = baseDao.queryNative(sql, customc, type);

        if (Integer.parseInt(queryResult.get(0)[0].toString()) == 0) {
            sql = " INSERT INTO {{?" + DSMConst.TB_GLOBAL_DICT + "}} "
                    + " (`customc`, `type`, `text`, `cstatus`, `remark`) "
                    + " SELECT IFNULL(?, ?), IFNULL(?, ?), IFNULL(?, ?), IFNULL(0, 0), IFNULL(remark, '') "
                    + " FROM {{?" + DSMConst.TB_GLOBAL_DICT + "}} "
                    + " WHERE cstatus&1 = 0 AND type = ? ";

            baseDao.updateNative(sql, customc, customc, type, type, text, text, type);
        } else {
            sql = " UPDATE {{?" + DSMConst.TB_GLOBAL_DICT + "}} "
                    + " SET text = ? "
                    + " WHERE customc = ? AND cstatus&1 = 0 AND type = ? ";

            baseDao.updateNative(sql, text, customc, type);
        }

        return new Result().success();
    }

}
