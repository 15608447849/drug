package com.onek.user.operations;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.onek.context.AppContext;
import com.onek.entitys.IOperation;
import com.onek.entitys.Result;
import com.onek.user.interactive.AptitudeInfo;
import com.onek.user.interactive.AuditInfo;
import com.onek.util.IceRemoteUtil;
import com.onek.util.RoleCodeCons;
import com.onek.util.area.AreaEntity;
import constant.DSMConst;
import dao.BaseDAO;
import util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static constant.DSMConst.TB_COMP;
import static constant.DSMConst.TB_SYSTEM_USER;

/**
 * @Author: leeping
 * @Date: 2019/3/26 14:36
 * 企业资质审核查询
 * 条件 :  门店手机号码 门店名 状态 客服专员id
 *
 */
public class AuditInfoOp extends AuditInfo implements IOperation<AppContext> {
    private Page page = new Page();
    private PageHolder pageHolder = new PageHolder(page);
    @Override
    public Result execute(AppContext appContext) {
        try {
            page.pageIndex = appContext.param.pageIndex;
            page.pageSize  = appContext.param.pageNumber;

            StringBuilder sb = new StringBuilder();
            sb.append("SELECT " +
                    //公司码-0,手机号-1,公司名-2,审核状态-3,审核失败原因-4,客服专员id-5,审核人id-6，地区码-7,营业执照地址-8,submitdate-9提交审核日期,submittime-10提交审核时间,审核日期-11,审核时间-12
                    "a.cid,a.uphone,b.cname,b.cstatus,b.examine,b.inviter,b.auditer,b.caddrcode,b.caddr,b.submitdate,b.submittime,b.auditdate,b.audittime " +
                    " FROM {{?"+TB_SYSTEM_USER +"}} AS a INNER JOIN {{?" +TB_COMP + "}} AS b ON a.cid=b.cid WHERE a.cstatus&1=0 AND b.cstatus&1=0 AND b.ctype=0");

            if (!StringUtils.isEmpty(phone)){
                sb.append(" AND ").append("a.uphone LIKE '%"+phone+"%'"); //模糊查询手机
            }
            if (!StringUtils.isEmpty(company)){
                sb.append(" AND ").append(" b.cname LIKE '%"+company+"%'");//模糊查询公司名
            }
            if (!StringUtils.isEmpty(status)){
                sb.append(" AND ").append("b.cstatus&"+status+">0");//状态查询
            }
            if (!StringUtils.isEmpty(cursorId)){
                sb.append(" AND ").append("b.inviter&"+cursorId+">0");//根据客服专员DB - id查询
            }
            // 根据所选地区查询
            if (!StringUtils.isEmpty(addressCode)){
                try {
                    sb.append(" AND ").append("b.caddrcode IN ("+getAdderRandge(Long.parseLong(addressCode))+")");
                } catch (NumberFormatException ignored) {
                }
            }
            String selectSql = sb.toString();
            List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(pageHolder, page, selectSql);
            if (lines.size() > 0) {
                List<AuditInfo> list = filterObject2Result(lines);
                if (list!=null){
                    return new Result().setQuery(list,pageHolder);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Result().setQuery(new ArrayList<>(),pageHolder);
    }

    //根据地区码获取所有的地区
    private String getAdderRandge(long addressCode) {
        AreaEntity[]  arr = IceRemoteUtil.getChildren(addressCode);
        String[] areaArr = new String[arr.length+1];
        for (int i = 0 ; i < arr.length ; i++ ){
            areaArr[i] = arr[i].getAreac()+"";
        }
        areaArr[areaArr.length-1] =  addressCode+"";
        return String.join(",",areaArr);
    }
    //赋值
    private List<AuditInfo> filterObject2Result(List<Object[]> lines) {
        if (lines==null || lines.size() == 0) return  null;
        List<AuditInfo> list = new ArrayList<>();
        AuditInfo info;
        for (Object[] arr : lines){
            try {
                //公司码-0,手机号-1,公司名-2,审核状态-3,审核失败原因-4,客服专员id-5,审核人id-6，地区码-7,营业执照地址-8,submitdate-9提交审核日期,submittime-10提交审核时间,审核日期-11,审核时间-12
                info = new AuditInfo();
                info.companyId = StringUtils.obj2Str(arr[0]);
                info.phone = StringUtils.obj2Str(arr[1]);
                info.company = StringUtils.obj2Str(arr[2]);
                info.status = StringUtils.obj2Str(arr[3]);
                info.examine = StringUtils.obj2Str(arr[4]);
                info.cursorId = StringUtils.obj2Str(arr[5]);
                info.auditerId = StringUtils.obj2Str(arr[6]);
                info.addressCode = StringUtils.obj2Str(arr[7]);
                info.address = StringUtils.obj2Str(arr[8]);
                info.submitDate = StringUtils.obj2Str(arr[9])+" "+StringUtils.obj2Str(arr[10]);
                info.auditDate = StringUtils.obj2Str(arr[11])+" "+StringUtils.obj2Str(arr[12]);
                queryAptitude(info);
                queryCursor(info);
                list.add(info);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }
    //查询客服专员信息
    private void queryCursor(AuditInfo info) {
        if (StringUtils.isEmpty(info.cursorId)) return;
        String selectSql = "SELECT urealname,uphone FROM {{?" + TB_SYSTEM_USER +"}} WHERE cstatus&1=0 AND uid=?";
        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql,info.cursorId);
        if (lines.size()==1){
            Object[] o = lines.get(0);
            info.cursorName = StringUtils.obj2Str(o[0]);
            info.cursorPhone = StringUtils.obj2Str(o[1]);
        }
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
