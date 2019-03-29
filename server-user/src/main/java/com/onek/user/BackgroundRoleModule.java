package com.onek.user;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.context.AppContext;
import com.onek.consts.CSTATUS;
import com.onek.entitys.Result;
import com.onek.user.entity.RoleVO;
import constant.DSMConst;
import dao.BaseDAO;
import util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class BackgroundRoleModule {
    private static final BaseDAO BASE_DAO = BaseDAO.getBaseDAO();

    private static final String QUERY_ROLE_BASE =
            " SELECT * "
            + " FROM {{?" + DSMConst.D_SYSTEM_ROLE + "}} "
            + " WHERE cstatus&1 = 0 ";

    private static final String OPEN_ROLE_SELF =
            " UPDATE {{?" + DSMConst.D_SYSTEM_ROLE + "}}"
            + " SET cstatus = cstatus & " + ~CSTATUS.CLOSE
            + " WHERE roleid = ? ";

    private static final String CLOSE_ROLE_SELF =
            " UPDATE {{?" + DSMConst.D_SYSTEM_ROLE + "}}"
            + " SET cstatus = cstatus | " + CSTATUS.CLOSE + ", "
            + " offdate = CURRENT_DATE, offtime = CURRENT_TIME "
            + " WHERE roleid = ? ";

    private static final String CLOSE_ROLE_RESOURCE =
            " UPDATE {{?" + DSMConst.D_SYSTEM_RESOURCE + "}}"
            + " SET roleid = roleid & ~? ";

    private static final String CLOSE_ROLE_USER =
            " UPDATE {{?" + DSMConst.D_SYSTEM_USER + "}}"
            + " SET roleid = roleid & ~? ";

    /**
     * 查询角色
     * @param appContext
     * @return
     */

    public Result queryRole(AppContext appContext) {
        Page page = new Page();
        page.pageIndex = appContext.param.pageIndex;
        page.pageSize  = appContext.param.pageNumber;

        PageHolder pageHolder = new PageHolder(page);

        StringBuilder sql = new StringBuilder(QUERY_ROLE_BASE);
        List<Object> paramList = new ArrayList<>();
        String[] params =  appContext.param.arrays;
        String param = null;
        for (int i = 0; i < params.length; i++) {
            param = params[i];

            if (StringUtils.isEmpty(param)) {
                continue;
            }

            try {
                switch (i) {
                    case 0:
                        sql.append(" AND rname LIKE ? ");
                        break;
                    case 1:
                        if (Integer.parseInt(param) == 0) {
                            // 0为查询非停用。
                            sql.append(" AND cstatus & ? = 0 ");
                        } else {
                            // 其他为查询停用。
                            sql.append(" AND cstatus & ? > 0 ");
                        }

                        param = Integer.toString(CSTATUS.CLOSE);
                }
            } catch (Exception e) {
                continue;
            }

            paramList.add(param);
        }
        
        List<Object[]> queryResult = BASE_DAO.queryNative(
                pageHolder, page, sql.toString(), paramList.toArray());
        RoleVO[] result = new RoleVO[queryResult.size()];

        if (result.length > 0) {
            BASE_DAO.convToEntity(queryResult, result, RoleVO.class);
        }

        return new Result().setQuery(result, pageHolder);
    }

    /**
     * 停用角色
     * @param appContext
     * @return
     */

    public Result closeRole(AppContext appContext) {
        String[] params = appContext.param.arrays;

        if (params.length == 0) {
            return new Result().fail("参数为空");
        }

        long roleId;
        try {
            roleId = Long.parseLong(params[0]);
        } catch (NumberFormatException e) {
            return new Result().fail("参数异常");
        }

        int checkResult = roleHandlerCheck(roleId);
        if (checkResult < 0) {
            if (checkResult == -2) {
                return new Result().fail("该角色不可操作");
            }

            return new Result().fail("操作失败");
        }

        List<Object[]> paramList = new ArrayList<>();
        // SELF
        paramList.add(new Object[] { roleId });
        // RESOURCE
        paramList.add(new Object[] { roleId });
        // USER
        paramList.add(new Object[] { roleId });

        BASE_DAO.updateTransNative(
                new String[] {
                        CLOSE_ROLE_SELF,
                        CLOSE_ROLE_RESOURCE,
                        CLOSE_ROLE_USER
                }, paramList);

        return new Result().success(null);
    }


    /**
     * 启用角色
     * @param appContext
     * @return
     */

    public Result openRole(AppContext appContext) {
        String[] params = appContext.param.arrays;

        if (params.length == 0) {
            return new Result().fail("参数为空");
        }

        long roleId;
        try {
            roleId = Long.parseLong(params[0]);
        } catch (NumberFormatException e) {
            return new Result().fail("参数异常");
        }

        BASE_DAO.updateNative(OPEN_ROLE_SELF, roleId);

        return new Result().success(null);
    }

    /**
     * 角色授资源权限
     * @param appContext
     * @return
     */

    public Result authorizeRole(AppContext appContext) {
        long roleId;
        String resource;
        try {
            JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
            roleId = json.get("roleId").getAsLong();
            resource = json.get("resources").getAsString();
        } catch (Exception e) {
            return new Result().fail("参数错误");
        }

        int checkResult = roleHandlerCheck(roleId);
        if (checkResult < 0) {
            if (checkResult == -2) {
                return new Result().fail("该角色不可操作");
            }
            return new Result().fail("操作失败");
        }

        String clearSQL =
                " UPDATE {{?" + DSMConst.D_SYSTEM_RESOURCE + "}} "
                + " SET roleid = roleid & ~? ";

        if (StringUtils.isEmpty(resource)) {
            BASE_DAO.updateNative(clearSQL, roleId);
        } else {
            String[] resources = resource.split(",");

            String authorizeSQL =
                    " UPDATE {{?" + DSMConst.D_SYSTEM_RESOURCE + "}} "
                    + " SET roleid = roleid | ? "
                    + " WHERE resourceid IN (" + String.join(",", resources) + ") ";

            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { roleId });
            params.add(new Object[] { roleId });

            BASE_DAO.updateTransNative(
                    new String[] { clearSQL, authorizeSQL },
                    params);
        }

        return new Result().success("操作成功");
    }

    /**
     *
     * @param roleId
     * @return
     *   0：正常
     *  -1:角色不存在
     *  -2：角色不可操作
     */

    private int roleHandlerCheck(long roleId) {
        RoleVO roleVO = getRoleByRoleId(roleId);

        if (roleVO == null) {
            return -1;
        }

        if ((roleVO.getCstatus() & CSTATUS.ROLE_NOT_HANDLEABLE) > 0) {
            return -2;
        }

        return 0;
    }

    private RoleVO getRoleByRoleId(long roleId) {
        if (roleId <= 0) {
            return null;
        }

        String sql = QUERY_ROLE_BASE
                + " AND roleid = ? ";

        List<Object[]> queryResult = BASE_DAO.queryNative(sql, roleId);

        if (queryResult.isEmpty()) {
            return null;
        }

        RoleVO[] results = new RoleVO[queryResult.size()];
        BASE_DAO.convToEntity(queryResult, results, RoleVO.class);

        return results[0];
    }


}
