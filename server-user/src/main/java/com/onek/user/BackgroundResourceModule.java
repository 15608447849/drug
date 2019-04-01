package com.onek.user;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.user.entity.ResourceVO;
import constant.DSMConst;
import dao.BaseDAO;
import util.StringUtils;
import util.TreeUtil;

import java.util.*;

public class BackgroundResourceModule {

    private static final BaseDAO BASE_DAO = BaseDAO.getBaseDAO();

    private static final String QUERY_RESOURCE_BASE =
            " SELECT *"
            + " FROM {{?" + DSMConst.D_SYSTEM_RESOURCE + "}} "
            + " WHERE cstatus&1 = 0 ";

    public Result queryResources(AppContext appContext) {
        Page page = new Page();
        page.pageIndex = appContext.param.pageIndex;
        page.pageSize  = appContext.param.pageNumber;

        PageHolder pageHolder = new PageHolder(page);

        StringBuilder sql = new StringBuilder(QUERY_RESOURCE_BASE);

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
                        sql.append(" AND sname LIKE ? ");
                        param = "%" + param + "%";
                        break;
                    case 1:
                        sql.append(" AND roleid & ? > 0 ");
                        break;
//                    case 1:
//                        if (Integer.parseInt(param) == 0) {
//                            // 0为查询非停用。
//                            sql.append(" AND cstatus & ? = 0 ");
//                        } else {
//                            // 其他为查询停用。
//                            sql.append(" AND cstatus & ? > 0 ");
//                        }
//
//                        param = Integer.toString(CSTATUS.CLOSE);
                }
            } catch (Exception e) {
                continue;
            }

            paramList.add(param);
        }

        List<Object[]> queryResult = BASE_DAO.queryNative(
                pageHolder, page, sql.toString(), paramList.toArray());
        ResourceVO[] result = new ResourceVO[queryResult.size()];

        if (result.length > 0) {
            BASE_DAO.convToEntity(queryResult, result, ResourceVO.class);
        }

        return new Result().setQuery(result, pageHolder);
    }

    public Result authorizeResource(AppContext appContext) {
        long resourceId, roles = 0;
        try {
            JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
            resourceId = json.get("resourceId").getAsLong();
            String[] roleArr = json.get("roles").getAsString().split(",");

            for (String role : roleArr) {
                roles |= Long.parseLong(role);
            }
        } catch (Exception e) {
            return new Result().fail("参数错误");
        }

        String authorizeSQL =
                " UPDATE {{?" + DSMConst.D_SYSTEM_RESOURCE + "}} "
                // 此处不得给超级管理员取消角色。
                + " SET roleid = 1, roleid = roleid | ?"
                + " WHERE resourceid = ? ";

        BASE_DAO.updateNative(authorizeSQL, roles, resourceId);

        return new Result().success("操作成功");
    }

    public Result getResourceTree(AppContext appContext) {
        ResourceVO[] resourceVOS = queryAllResources();
        List<ResourceVO> list = Arrays.asList(resourceVOS);
        return new Result().success(new Gson().toJson(TreeUtil.list2Tree(list)));
    }

    public Result getRoleResourceTree(AppContext appContext) {
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

        ResourceVO[] resourceVOS = queryAllResources();
        List<String> selectedList = new ArrayList<>();

        for (ResourceVO resourceVO : resourceVOS) {
            if ((resourceVO.getRoleId() & roleId) > 0) {
                selectedList.add(resourceVO.getResourceId());
            }
        }

        Map<String, Object> returnMap = new HashMap<>();
        returnMap.put("resourceVOS",
                new Gson().toJson(
                    TreeUtil.list2Tree(
                            Arrays.asList(resourceVOS))));
        returnMap.put("selectedList", selectedList);
        return new Result().success(returnMap);
    }

    private ResourceVO[] queryAllResources() {
        List<Object[]> queryResult = BASE_DAO.queryNative(QUERY_RESOURCE_BASE);
        ResourceVO[] results = new ResourceVO[queryResult.size()];
        BASE_DAO.convToEntity(queryResult, results, ResourceVO.class);
        return results;
    }

}
