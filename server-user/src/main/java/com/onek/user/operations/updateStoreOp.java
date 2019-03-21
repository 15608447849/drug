package com.onek.user.operations;

import com.onek.AppContext;
import com.onek.RedisGlobalKeys;
import com.onek.UserSession;
import com.onek.entitys.IOperation;
import com.onek.entitys.Result;
import constant.DSMConst;
import dao.BaseDAO;

import java.util.List;

import static com.onek.RedisGlobalKeys.getCompanyCode;

/**
 * @Author: leeping
 * @Date: 2019/3/20 14:36
 */
public class updateStoreOp implements IOperation<AppContext> {

    String storeName;
    String address; //根据地址 自动采集经纬度信息
    int addressCode; //地区码
    public double longitude;//营业执照地址纬度
    public double latitude;//营业执照地址经度

    @Override
    public Result execute(AppContext context) {
        UserSession session = context.getUserSession();

        //根据传递的企业名称查询是否存在
        String selectSql = "SELECT cid FROM {{?" + DSMConst.D_COMP +"}} WHERE cstatus&1 = 0 AND ctype=0 AND cname = ?";
        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql,storeName);
        if (lines.size()>0){
            if (session.compId > 0) return new Result().fail("修改失败,存在相同门店名");
            int compid = (int) lines.get(0)[0];
            //用户关联企业码
            String updateSql = "UPDATE {{?" + DSMConst.D_SYSTEM_USER + "}} SET cid = ? WHERE uid = ?";
            int i = BaseDAO.getBaseDAO().updateNative(updateSql,compid,session.userId);
            if (i <= 0){
                return new Result().fail("无法关联门店信息");
            }
            session.compId = compid;
            context.relationTokenUserSession();
        }

        if (session.compId > 0 ){
            //查询出当前状态
            selectSql = "SELECT cstatus FROM {{?" + DSMConst.D_COMP +"}} WHERE cstatus&1 = 0 AND ctype=0 AND cid = ?";

            lines = BaseDAO.getBaseDAO().queryNative(selectSql,session.compId);
            if (lines.size() > 0){
                int status = (int) lines.get(0)[0]; //状态
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
                //修改 门店信息
                String updateSql = "UPDATE {{?" + DSMConst.D_SYSTEM_USER + "}} " +
                        "SET cname=?,cnamehash=crc32(?),caddr=?,caddrcode=?,lat=?,lng=?,cstatus=cstatus&~?|128 " +
                        "WHERE cstatus&1=0 AND ctype=0 AND compid=?";
                int i = BaseDAO.getBaseDAO().updateNative(updateSql,
                        storeName,
                        storeName,
                        address,
                        addressCode,
                        latitude,
                        longitude,
                        status,
                        session.compId
                );
                if (i > 0){
                    return new Result().success("门店修改信息成功");
                }
            }
        }else{
            //新增企业信息
            long compId = getCompanyCode();
            String insertSql = "INSERT INTO {{?"+ DSMConst.D_COMP +"}} " +
                    "(cid,cname,cnamehash,ctype,caddr,caddrcode,lat,lng,cstatus,createdate, createtime) " +
                    "VALUES(?,?,crc32(?),?,?,?,?,?,?,CURRENT_DATE,CURRENT_TIME)";
            int i = BaseDAO.getBaseDAO().updateNative(insertSql,
                    compId,
                    storeName,
                    storeName,
                    0,
                    address,
                    addressCode,
                    latitude,
                    longitude,
                    128 //新增企业-审核中
            );
            if (i>0){
                return new Result().success("新增门店信息,关联成功");
            }
        }

        return new Result().fail("关联异常");
    }
}
