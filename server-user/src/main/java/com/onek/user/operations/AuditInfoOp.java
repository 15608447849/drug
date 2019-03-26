package com.onek.user.operations;

import com.onek.AppContext;
import com.onek.entitys.IOperation;
import com.onek.entitys.Result;
import com.onek.user.interactive.AuditInfo;
import constant.DSMConst;
import dao.BaseDAO;
import util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: leeping
 * @Date: 2019/3/26 14:36
 */
public class AuditInfoOp extends AuditInfo implements IOperation<AppContext> {

    @Override
    public Result execute(AppContext context) {

        Result result = null;
        //根据手机号码查询企业审核信息
        if(!StringUtils.isEmpty(phone))
            result = queryByPhone();

        if (result == null){
            //根据所选条件查询
            StringBuilder sb = new StringBuilder();
            if (!StringUtils.isEmpty(company)){
                sb.append("cname="+company);
            }
            if (!StringUtils.isEmpty(submitDate)){
                sb.append(" AND submitdate="+submitDate);
            }
            if (!StringUtils.isEmpty(submitTime)){
                sb.append(" AND submittime="+submitTime);
            }
            if (!StringUtils.isEmpty(auditDate)){
                sb.append(" AND auditdate="+auditDate);
            }
            if (!StringUtils.isEmpty(auditTime)){
                sb.append(" AND audittime="+auditTime);
            }
            if (!StringUtils.isEmpty(status)){
                try {
                    int istatus = Integer.parseInt(status);
                    sb.append(" AND cstatus="+istatus);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }

            String selectSql = generationAuditSelectSql(sb.toString());

            List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql);
            if (lines.size() > 0) {
                List<AuditInfo> list = filterObject2Result(lines);
                if (list!=null){
                    return new Result().success(list);
                }
            }

        }
        return new Result().fail("无数据");
    }

    //根据手机账号查询
    private Result queryByPhone() {
        //手机号 -> 企业码
        String selectSql = "SELECT cid FROM {{?" + DSMConst.D_SYSTEM_USER +"}} WHERE cstatus&1 = 0 AND roleid&2>0 AND uphone = ?";
        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql,phone);
        if (lines.size() == 0) return null;
        //用户码-> 企业审核信息

        selectSql = generationAuditSelectSql("cid=?");
        lines = BaseDAO.getBaseDAO().queryNative(selectSql,lines.get(0));
        if (lines.size() > 0) {
            List<AuditInfo> list = filterObject2Result(lines);
            if (list!=null){
                return new Result().success(list);
            }
        }
        return  null;
    }



    /**
     *  public String company; //公司名
     *     public String submitDate; //提交日期
     *     public String submitTime; //提交时间
     *     public String auditDate; //审核日期
     *     public String auditTime; //审核时间
     *     public String status; //状态
     *     public String auditer;//审核人
     */
    private String generationAuditSelectSql(String paramSql) {

        String sqlPrev = "SELECT " +
                "cid,cname,createdate,createtime,submitdate,submittime,auditdate,audittime,examine,cstatus" +
                " FROM {{?" +DSMConst.D_COMP+ "}}";
                if(!StringUtils.isEmpty(paramSql)){
                    sqlPrev = sqlPrev + " WHERE  "+paramSql;
                }
        return sqlPrev;
    }

    private List<AuditInfo> filterObject2Result(List<Object[]> lines) {
        if (lines==null || lines.size() == 0) return  null;
        List<AuditInfo> list = new ArrayList<>();
        AuditInfo info;
        for (Object[] arr : lines){
            try {
                info = new AuditInfoOp();

                info.companyId = StringUtils.obj2Str(arr[0],"");
                info.company = StringUtils.obj2Str(arr[1],"");

                info.createDate = StringUtils.obj2Str(arr[2],"");
                info.createTime = StringUtils.obj2Str(arr[3],"");

                info.submitDate = StringUtils.obj2Str(arr[4],"");
                info.submitTime = StringUtils.obj2Str(arr[5],"");

                info.auditDate = StringUtils.obj2Str(arr[6],"");
                info.auditTime = StringUtils.obj2Str(arr[7],"");

                info.examine = StringUtils.obj2Str(arr[8],"");
                info.status = StringUtils.obj2Str(arr[9],"");

                list.add(info);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }
}
