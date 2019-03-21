package com.onek.user.operations;

import com.onek.AppContext;
import com.onek.entitys.IOperation;
import com.onek.entitys.Result;
import com.onek.user.interactive.StoreBasicInfo;
import constant.DSMConst;
import dao.BaseDAO;

import java.math.BigDecimal;
import java.util.List;

import static util.StringUtils.checkObjectNull;

/**
 * @Author: leeping
 * @Date: 2019/3/20 16:30
 */
public class StoreBasicInfoOp implements IOperation<AppContext> {
    @Override
    public Result execute(AppContext appContext) {
        StoreBasicInfo info = new StoreBasicInfo();
        info.phone = appContext.getUserSession().phone;
        info.storeId = appContext.getUserSession().compId;
        //通过企业码获取企业信息
        String selectSql = "SELECT cstatus,examine,cname,caddr,caddrcode,lat,lng" +
                " FROM {{?"+ DSMConst.D_COMP+"}}"+
                " WHERE ctype=0 AND cid = ?";
        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql, appContext.getUserSession().compId);
        if (lines.size() == 1){
            info.isRelated = true;
            //获取企业信息
            Object[] rows = lines.get(0);
            int status = (int) rows[0];

            if ((status&64) == 64){
                status = 64; //待认证
                info.authenticationMessage = "待认证";
            }else if ( (status&128) == 128 ){
                status = 128; //审核中
                info.authenticationMessage = "审核中";
            }else if ((status&256) == 256){
                status = 256; //已认证
                info.authenticationMessage = "已认证";
            }else if ((status&512) == 512){
                status = 512; //认证失败
                info.authenticationMessage = "认证失败 "+checkObjectNull(rows[1],"");
            }else if ((status&1024) == 1024){
                status = 1024; //停用
                info.authenticationMessage = "已停用";
            }
            info.authenticationStatus = status;
            info.storeName = checkObjectNull(rows[2],"");
            info.address = checkObjectNull(rows[3],"未设置");
            info.addressCode = checkObjectNull(rows[4],0);
            info.latitude = checkObjectNull(rows[5],new BigDecimal(0)); //纬度
            info.longitude = checkObjectNull(rows[6],new BigDecimal(0)); //精度
        }

        return new Result().success(info);//返回用户信息
    }

}
