package com.onek.user;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.alibaba.fastjson.JSON;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.context.AppContext;
import com.onek.consts.CSTATUS;
import com.onek.entitys.Result;
import com.onek.user.entity.RoleVO;
import constant.DSMConst;
import dao.BaseDAO;
import util.ArrayUtil;
import util.BUSUtil;
import util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class BackgroundRoleModule {
    private static final BaseDAO BASE_DAO = BaseDAO.getBaseDAO();

    private static final String QUERY_ROLE_BASE =
            " SELECT * "
            + " FROM {{?" + DSMConst.TB_SYSTEM_ROLE + "}} "
            + " WHERE cstatus&1 = 0 ";

    private static final String INSERT_ROLE =
            " INSERT INTO {{?" + DSMConst.TB_SYSTEM_ROLE + "}} "
            + " VALUES (?, ?, CURRENT_DATE, CURRENT_TIME, NULL, NULL, ?) ";

    private static final String UPDATE_ROLE =
            " UPDATE {{?" + DSMConst.TB_SYSTEM_ROLE + "}} "
            + " SET rname = ?, cstatus = ? "
            + " WHERE cstatus&(1|2) = 0 AND roleid = ? ";

    private static final String DELETE_ROLE =
            " UPDATE {{?" + DSMConst.TB_SYSTEM_ROLE + "}} "
            + " SET cstatus = cstatus | 1 "
            + " WHERE cstatus&2 = 0 AND roleid = ? ";

    private static final String OPEN_ROLE_SELF =
            " UPDATE {{?" + DSMConst.TB_SYSTEM_ROLE + "}}"
            + " SET cstatus = cstatus & " + ~CSTATUS.CLOSE
            + " WHERE roleid = ? ";

    private static final String CLOSE_ROLE_SELF =
            " UPDATE {{?" + DSMConst.TB_SYSTEM_ROLE + "}}"
            + " SET cstatus = cstatus | " + CSTATUS.CLOSE + ", "
            + " offdate = CURRENT_DATE, offtime = CURRENT_TIME "
            + " WHERE roleid = ? ";

    private static final String CLOSE_ROLE_RESOURCE =
            " UPDATE {{?" + DSMConst.TB_SYSTEM_RESOURCE + "}}"
            + " SET roleid = roleid & ~? ";

    private static final String CLOSE_ROLE_USER =
            " UPDATE {{?" + DSMConst.TB_SYSTEM_USER + "}}"
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
                        param = "%" + param + "%";
                        break;
                    case 1:
                        if (Integer.parseInt(param) == 0) {
                            // 0为查询非停用。
                            sql.append(" AND cstatus & ? = 0 ");
                        } else {
                            // 其他为查询停用。
                            sql.append(" AND cstatus & ? > 0 ");
                        }

                        param = String.valueOf(CSTATUS.CLOSE);
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
//        paramList.add(new Object[] { roleId });

        BASE_DAO.updateTransNative(
                new String[] {
                        CLOSE_ROLE_SELF,
//                        CLOSE_ROLE_RESOURCE,
//                        CLOSE_ROLE_USER
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
                " UPDATE {{?" + DSMConst.TB_SYSTEM_RESOURCE + "}} "
                + " SET roleid = roleid & ~? "
                + " WHERE cstatus&2 = 0 ";

        if (StringUtils.isEmpty(resource)) {
            BASE_DAO.updateNative(clearSQL, roleId);
        } else {
            String[] resources = resource.split(",");

            String authorizeSQL =
                    " UPDATE {{?" + DSMConst.TB_SYSTEM_RESOURCE + "}} "
                    + " SET roleid = roleid | ? "
                    + " WHERE cstatus&2 = 0 "
                    + " AND resourceid IN (" + String.join(",", resources) + ") ";

            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { roleId });
            params.add(new Object[] { roleId });

            BASE_DAO.updateTransNative(
                    new String[] { clearSQL, authorizeSQL },
                    params);
        }

        return new Result().success("操作成功");
    }

    public Result addRole(AppContext appContext) {
        if (StringUtils.isEmpty(appContext.param.json)) {
            return new Result().fail("参数为空");
        }

        RoleVO paramRole =
                JSON.parseObject(appContext.param.json, RoleVO.class);

        List<Object[]> queryResult = BASE_DAO.queryNative(
                QUERY_ROLE_BASE + " ORDER BY roleid ");

        if (queryResult.size() >= 63) {
            return new Result().fail("角色已达上限！");
        }

        RoleVO[] roleVOS = new RoleVO[queryResult.size()];

        BASE_DAO.convToEntity(queryResult, roleVOS, RoleVO.class);

        int pow;
        long newRole = 1L;

        for (pow = 0; pow < roleVOS.length; pow++, newRole <<= 1) {
            if (newRole != roleVOS[pow].getRoleId()) {
                break;
            }
        }

        BASE_DAO.updateNative(INSERT_ROLE,
                newRole, paramRole.getRoleName(), paramRole.getCstatus());

        return new Result().success("新增成功");
    }


    public Result deleteRole(AppContext appContext) {
        if (ArrayUtil.isEmpty(appContext.param.arrays)
            || !StringUtils.isBiggerZero(appContext.param.arrays[0])) {
            return new Result().fail("参数错误");
        }

        long roleId = Long.parseLong(appContext.param.arrays[0]);

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
                        DELETE_ROLE,
                        CLOSE_ROLE_RESOURCE,
                        CLOSE_ROLE_USER
                }, paramList);

        return new Result().success(null);
    }


    public Result updateRole(AppContext appContext) {
        if (StringUtils.isEmpty(appContext.param.json)) {
            return new Result().fail("参数为空");
        }

        RoleVO paramRole =
                JSON.parseObject(appContext.param.json, RoleVO.class);

        int updateResult =
                BASE_DAO.updateNative(UPDATE_ROLE,
                    paramRole.getRoleName(), paramRole.getCstatus());

        return updateResult == 0
                ? new Result().fail("更新失败")
                : new Result().success("更新成功");
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
