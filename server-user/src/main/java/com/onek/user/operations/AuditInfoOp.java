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

            //根据所选条件查询
            StringBuilder sb = new StringBuilder();
            int i = 0;
            if (!StringUtils.isEmpty(phone)){
                sb.append("a.uphone LIKE '%"+phone+"%'");
                i++;
            }
            if (!StringUtils.isEmpty(company)){
                if (i>0) sb.append(" AND ");
                sb.append("b.cname LIKE '%"+company+"%'");
                i++;
            }
            if (!StringUtils.isEmpty(submitDate)){
                if (i>0) sb.append(" AND ");
                sb.append("b.submitdate='"+submitDate+"'");
                i++;
            }
            if (!StringUtils.isEmpty(submitTime)){
                if (i>0) sb.append(" AND ");
                sb.append("b.submittime='"+submitTime+"'");
                i++;
            }
            if (!StringUtils.isEmpty(auditDate)){
                if (i>0) sb.append(" AND ");
                sb.append("b.auditdate='"+auditDate+"'");
                i++;
            }
            if (!StringUtils.isEmpty(auditTime)){
                sb.append("b.audittime='"+auditTime+"'");
                i++;
            }
            if (!StringUtils.isEmpty(status)){
                try {
                    if (i>0) sb.append(" AND ");
                    int istatus = Integer.parseInt(status);
                    sb.append("b.cstatus&"+istatus+"="+istatus);
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

        return new Result().setQuery(new ArrayList<>(),pageHolder);
    }

    //连表查询
    private String generationAuditSelectSql(String paramSql) {

        String sqlPrev = "SELECT " +
                "a.cid,b.cname,b.createdate,b.createtime,b.submitdate,b.submittime,b.auditdate,b.audittime,b.examine,b.cstatus,a.uphone" +
                " FROM {{?"+DSMConst.TB_SYSTEM_USER +"}} AS a INNER JOIN {{?" +DSMConst.TB_COMP + "}} AS b ON a.cid=b.cid";
                if(!StringUtils.isEmpty(paramSql)){
                    sqlPrev  += " WHERE  "+paramSql;
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

                info.companyId = StringUtils.obj2Str(arr[0]);
                info.company = StringUtils.obj2Str(arr[1]);

                info.createDate = StringUtils.obj2Str(arr[2]);
                info.createTime = StringUtils.obj2Str(arr[3]);

                info.submitDate = StringUtils.obj2Str(arr[4]);
                info.submitTime = StringUtils.obj2Str(arr[5]);

                info.auditDate = StringUtils.obj2Str(arr[6]);
                info.auditTime = StringUtils.obj2Str(arr[7]);

                info.examine = StringUtils.obj2Str(arr[8]);

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
                info.status = StringUtils.obj2Str(status);
                info.phone = StringUtils.obj2Str(arr[10]);

                queryAptitude(info);

                list.add(info);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }
    //查询企业的审核资质信息
    private void queryAptitude(AuditInfo info) {
        AptitudeInfo cardInfo = info.cardInfo;
        String selectSql = "SELECT atype,certificateno,validitys,validitye FROM {{?" + DSMConst.TB_COMP_APTITUDE + "}} WHERE compid=?";
        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql,info.companyId);
        for (Object[] row : lines){
            int type = StringUtils.checkObjectNull(row[0],0);
            if (type == 10){
                cardInfo.businessId = StringUtils.obj2Str(row[1]);
                cardInfo.businessIdStart = StringUtils.obj2Str(row[2]);
                cardInfo.businessIdEnd = StringUtils.obj2Str(row[3]);
            }else if (type == 11){
                cardInfo.permitId = StringUtils.obj2Str(row[1]);
                cardInfo.permitIdStart = StringUtils.obj2Str(row[2]);
                cardInfo.permitIdEnd = StringUtils.obj2Str(row[3]);
            }else if (type == 12){
                cardInfo.gspId = StringUtils.obj2Str(row[1]);
                cardInfo.gspIdStart = StringUtils.obj2Str(row[2]);
                cardInfo.gspIdEnd = StringUtils.obj2Str(row[3]);
            }
        }
    }
}
