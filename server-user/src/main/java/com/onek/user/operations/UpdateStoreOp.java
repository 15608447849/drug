package com.onek.user.operations;

import Ice.Application;
import com.onek.context.AppContext;
import com.onek.context.UserSession;
import com.onek.entitys.IOperation;
import com.onek.entitys.Result;
import com.onek.util.GenIdUtil;
import com.onek.util.SmsTempNo;
import com.onek.util.SmsUtil;
import dao.BaseDAO;
import util.GaoDeMapUtil;
import util.StringUtils;

import java.util.List;

import static com.onek.user.operations.StoreBasicInfoOp.updateCompInfoToCacheById;
import static com.onek.util.RedisGlobalKeys.getCompanyCode;
import static constant.DSMConst.*;

/**
 * @Author: leeping
 * @Date: 2019/3/20 14:36
 */
public class UpdateStoreOp implements IOperation<AppContext> {

    String storeName;//门店名
    String address; //营业执照地址->根据地址 自动采集经纬度信息
    String addressCode; //地区码
    double longitude;//营业执照地址纬度
    double latitude;//营业执照地址经度

    @Override
    public Result execute(AppContext context) {
        UserSession session = context.getUserSession();

        if (session == null || session.userId < 0) return new Result().fail("用户信息异常");

       if (StringUtils.isEmpty(storeName,address))  return new Result().fail("门店或地址未填写");

        //根据企业营业执照地址查询是否存在相同已认证的企业
        String selectSql = "SELECT cid,caddr,cname FROM {{?" + TB_COMP +"}} WHERE ctype=0 AND cstatus&256>0 AND caddr=?  OR cname=?";
        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql,address,storeName);
        if (lines.size()>0){
            boolean isAccess = true;
            if (session.compId > 0){
                for (Object[] rows : lines){
                    int cid = StringUtils.checkObjectNull(rows[0],0);
                    if (cid>0 && cid != session.compId) isAccess = false;
                }
            }else isAccess = false;
            if (!isAccess) return new Result().fail("存在已认证的相同营业执照地址或药店名称");
        }

        //转换经纬度
        convertLatLon();
        /*创建企业并关联信息*/
        if (session.compId == 0){
            //查询企业ID是否存在
            long compid = _getCompanyCode(BaseDAO.getBaseDAO().queryNative("SELECT cid FROM {{?"+ TB_COMP +"}}")); //生成企业码

            //ctype 数据库默认 0
            String insertSql = "INSERT INTO {{?"+ TB_COMP +"}} " +
                    "(ctype,cid,cname,cnamehash,caddr,caddrcode,lat,lng,cstatus,createdate, createtime,submitdate,submittime) " +
                    "VALUES(0,?,?,crc32(?),?,?,?,?,?,CURRENT_DATE,CURRENT_TIME,CURRENT_DATE,CURRENT_TIME)";
            int i = BaseDAO.getBaseDAO().updateNative(insertSql,
                    compid,
                    storeName,
                    storeName,
                    address,
                    addressCode,
                    latitude,
                    longitude,
                    128 //新增企业-未认证
            );
            if (i<=0) return new Result().fail("注册失败,无法添加门店信息");

            //用户关联门店
            String updateSql = "UPDATE {{?" + TB_SYSTEM_USER +"}} SET cid=? WHERE cstatus&1=0 AND uid=?";
            i = BaseDAO.getBaseDAO().updateNative(updateSql,compid,session.userId);
            if (i>0) {
                //企业关联会员
                compLinkMember(compid);
                if (session.phone!=null){
                    //发送短信提醒
                    SmsUtil.sendSmsBySystemTemp(session.phone, SmsTempNo.REGISTERED_SUCCESSFULLY);
                }
                return new Result().success("添加门店信息,关联成功").setHashMap("compid",compid); //带入公司码到前台- 前端跳转到下一步 资质上传
            }else{
                return new Result().fail("此用户无法关联门店信息");
            }
        }
        //修改门店信息
        String updateSql = "UPDATE {{?" + TB_COMP + "}} " +
                "SET cname=?,cnamehash=crc32(?),caddr=?,caddrcode=?,lat=?,lng=?,cstatus=?,submitdate=CURRENT_DATE,submittime=CURRENT_TIME" +
                " WHERE ctype=0 AND cstatus&1=0 AND cid=?";
        int i = BaseDAO.getBaseDAO().updateNative(updateSql,
                storeName,
                storeName,
                address,
                addressCode,
                latitude,
                longitude,
                128, //审核中
                session.compId
        );
        if (i>0){
            updateCompInfoToCacheById(session.compId);//更新企业信息到缓存
            return new Result().success("修改信息成功");
        }else{
            return new Result().success("无法修改信息");
        }
    }

    private long _getCompanyCode(List<Object[]> lines) {
        long id = getCompanyCode();
        for (Object[] rows: lines){
            int cid =StringUtils.checkObjectNull(rows[0],0);
            if (cid == id){
                return _getCompanyCode(lines);
            }
        }
        return id;
    }

    //转换经纬度
    private void convertLatLon() {
        try {
            GaoDeMapUtil.Point p = GaoDeMapUtil.addressConvertLatLon(address);
            Application.communicator().getLogger().print(address +" ===============地址转换经纬度==============> " + p);
            longitude = p.lng;
            latitude = p.lat;
        } catch (Exception ignored) {
        }
    }

    //企业关联会员
    private void compLinkMember(long compId) {
        try {
            String insertSql = "INSERT INTO {{?"+ TD_MEMBER +"}} " +
                    "(unqid,compid,accupoints,balpoints,createdate,createtime,cstatus) " +
                    "VALUES(?,?,0,0,CURRENT_DATE,CURRENT_TIME,0)";
            BaseDAO.getBaseDAO().updateNative(insertSql, GenIdUtil.getUnqId(), compId);
        } catch (Exception ignored) { }
    }

}
