package com.onek.user.operations;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.onek.context.AppContext;
import com.onek.entitys.IOperation;
import com.onek.entitys.Result;
import com.onek.user.interactive.AptitudeInfo;
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
    private Page page = new Page();
    private PageHolder pageHolder = new PageHolder(page);
    @Override
    public Result execute(AppContext appContext) {

        page.pageIndex = appContext.param.pageIndex;
        page.pageSize  = appContext.param.pageNumber;

        Result result = null;
        //根据手机号码查询企业审核信息
        if(!StringUtils.isEmpty(phone))
            result = queryByPhone();

        if (StringUtils.isEmpty(phone) && result == null){
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
                    sb.append(" AND cstatus&"+istatus+"="+istatus);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }

            String selectSql = generationAuditSelectSql(sb.toString());

            List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(pageHolder, page, selectSql);
            if (lines.size() > 0) {
                List<AuditInfo> list = filterObject2Result(lines);
                if (list!=null){
                    return new Result().setQuery(list,pageHolder);
                }
            }
        }
        return new Result().setQuery(new ArrayList<>(),pageHolder);
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
                return new Result().setQuery(list,pageHolder);
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
                //获取企业账号
                String selectSql = "SELECT uphone FROM {{?" + DSMConst.D_SYSTEM_USER +"}} WHERE cid = "+ arr[0];
                List<Object[]> lines2 = BaseDAO.getBaseDAO().queryNative(selectSql);

                if (lines2.size() != 1) continue;

                info = new AuditInfoOp();

                info.phone = StringUtils.obj2Str(lines2.get(0)[0],"");
                info.companyId = StringUtils.obj2Str(arr[0],"");
                info.company = StringUtils.obj2Str(arr[1],"");

                info.createDate = StringUtils.obj2Str(arr[2],"");
                info.createTime = StringUtils.obj2Str(arr[3],"");

                info.submitDate = StringUtils.obj2Str(arr[4],"");
                info.submitTime = StringUtils.obj2Str(arr[5],"");

                info.auditDate = StringUtils.obj2Str(arr[6],"");
                info.auditTime = StringUtils.obj2Str(arr[7],"");

                info.examine = StringUtils.obj2Str(arr[8],"");

                int status = (int)arr[9];
                if ((status&64) == 64){
                    status = 64; //待认证
                }else if ( (status&128) == 128 ){
                    status = 128; //审核中
                }else if ((status&256) == 256){
                    status = 256; //已认证
                }else if ((status&512) == 512){
                    status = 512; //认证失败
                }else if ((status&1024) == 1024){
                    status = 1024; //停用
                }
                queryAptitude(info.cardInfo,arr[0]);
                info.status = StringUtils.obj2Str(status,"");
                list.add(info);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    private void queryAptitude(AptitudeInfo cardInfo, Object compid) {
        String selectSql = "SELECT atype,certificateno,validitys,validitye FROM {{?" + DSMConst.D_COMP_APTITUDE + "}} WHERE compid=?";
        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql,compid);
        for (Object[] row : lines){
            int type = StringUtils.checkObjectNull(row[0],0);
            if (type == 10){
                cardInfo.businessId = StringUtils.obj2Str(row[1],"");
                cardInfo.businessIdStart = StringUtils.obj2Str(row[2],"");
                cardInfo.businessIdEnd = StringUtils.obj2Str(row[3],"");
            }else if (type == 11){
                cardInfo.permitId = StringUtils.obj2Str(row[1],"");
                cardInfo.permitIdStart = StringUtils.obj2Str(row[2],"");
                cardInfo.permitIdEnd = StringUtils.obj2Str(row[3],"");
            }else if (type == 12){
                cardInfo.gspId = StringUtils.obj2Str(row[1],"");
                cardInfo.gspIdStart = StringUtils.obj2Str(row[2],"");
                cardInfo.gspIdEnd = StringUtils.obj2Str(row[3],"");
            }
        }
    }
}