package com.onek.user.operations

import com.onek.consts.IntegralConstant
import com.onek.context.AppContext
import com.onek.entitys.IOperation
import com.onek.entitys.Result
import com.onek.user.interactive.AptitudeInfo
import com.onek.user.operations.StoreBasicInfoOp.updateCompInfoToCacheById
import com.onek.user.service.MemberImpl
import com.onek.util.IceRemoteUtil
import com.onek.util.SmsTempNo.AUTHENTICATION_FAILURE
import com.onek.util.SmsTempNo.AUTHENTICATION_SUCCESS
import com.onek.util.SmsUtil
import com.onek.util.member.MemberEntity
import constant.DSMConst
import constant.DSMConst.TB_COMP
import constant.DSMConst.TB_SYSTEM_USER
import dao.BaseDAO
import redis.IRedisPartCache
import redis.proxy.CacheProxyInstance
import java.util.regex.Pattern



/**
 * @Author: leeping
 * @Date: 2019/3/27 11:25
 * 企业资质审核
 */
class UpdateAuditOp :AptitudeInfo(), IOperation<AppContext> {

    var companyId:String? = null; //公司码
    var auditCause:String? = null //审核失败原因
    var auditStatus:Int = 0; //审核状态 256成功/512失败

    override fun execute(context: AppContext?): Result {
        //当前审核人id
        val auditer = context?.userSession?.userId
        //添加资质信息
        if(auditStatus == 256){ //审核通过
            var isOK = updateIds(companyId!!,businessId!!,businessIdStart!!,businessIdEnd!!,10)
            if (isOK) isOK = updateIds(companyId!!,permitId!!,permitIdStart!!,permitIdEnd!!,11)
            if (isOK) isOK = updateIds(companyId!!,gspId!!,gspIdStart!!,gspIdEnd!!,12)
            if (!isOK) return Result().fail("资质信息保存失败")
        }

        val storetype = settingCompStoretype(permitId!!);

        //修改门店审核状态
        val updateSql = "UPDATE {{?$TB_COMP}} SET cstatus=?,examine=?,auditer=?,auditdate=CURRENT_DATE,audittime=CURRENT_TIME,storetype=? WHERE cstatus&1=0 AND cid=?"
        val i = BaseDAO.getBaseDAO().updateNative(updateSql,auditStatus,auditCause,auditer,storetype,companyId)

        if(i > 0){
            val tempLines = BaseDAO.getBaseDAO().queryNative("SELECT uphone FROM {{?$TB_SYSTEM_USER}} WHERE cid = ?", companyId)
            var phone :String ? = null
            if (tempLines.size == 1) phone =  tempLines[0][0].toString()
            if(auditStatus == 256) {
                SmsUtil.sendSmsBySystemTemp(phone,AUTHENTICATION_SUCCESS)
                IceRemoteUtil.sendTempMessageToClient(companyId!!.toInt(),AUTHENTICATION_SUCCESS)
                try {
                    giftPoints(companyId!!.toInt())
                    IceRemoteUtil.revNewComerCoupon(companyId!!.toInt(), context!!.userSession.phone.toLong())
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }else if(auditStatus == 512) {
                SmsUtil.sendSmsBySystemTemp(phone,AUTHENTICATION_FAILURE,auditCause)
                IceRemoteUtil.sendTempMessageToClient(companyId!!.toInt(),AUTHENTICATION_FAILURE,auditCause)
            }
            updateCompInfoToCacheById(companyId!!.toInt()) //更新企业信息到缓存
            return Result().success("审核操作成功")
        }
        return Result().fail("审核操作失败")   }

    //赠送积分
    private fun giftPoints(companyId: Int) {
        try {
            val memProxy = CacheProxyInstance.createPartInstance(MemberImpl()) as IRedisPartCache
            val point = 1000;
            val memberVO = memProxy.getId(companyId) as MemberEntity
            val accupoints = memberVO.accupoints
            val balpoints = memberVO.balpoints

            if(balpoints <= 0){ // 积分余额为0代表第一次注册
                val updateMemberVO = MemberEntity()
                updateMemberVO.compid = companyId
                updateMemberVO.accupoints = accupoints + point
                updateMemberVO.balpoints = balpoints + point
                val r = memProxy.update(companyId, updateMemberVO)
                if(r > 0){
                    IceRemoteUtil.addIntegralDetail(companyId, IntegralConstant.SOURCE_AUTH_MATERIAL, point, 0);
                }
            }
        } catch (e: Exception) {
        }
    }
    //更新企业资质
    private fun updateIds(companyId: String, id: String, idStartTime: String, idEndTime: String, type:Int) :Boolean{
        try {
            val selectSql = "SELECT aptid FROM {{?${DSMConst.TB_COMP_APTITUDE}}} WHERE compid = $companyId AND atype = $type"
            val lines = BaseDAO.getBaseDAO().queryNative(selectSql);
            if (lines.size == 0){
                //新增
                val aptid = genCompanyAptCode(companyId.toInt(),type)
                val insertSql = "INSERT INTO {{?${DSMConst.TB_COMP_APTITUDE}}} (aptid,compid,atype,certificateno,validitys,validitye) VALUES (?,?,?,?,?,?)"
                val i = BaseDAO.getBaseDAO().updateNative(insertSql,
                        aptid,
                        companyId,
                        type,
                        id,
                        idStartTime,
                        idEndTime
                )
                if (i > 0) return true
            }else{
                //修改
                val updateSql = "UPDATE {{?${DSMConst.TB_COMP_APTITUDE}}} SET certificateno='$id',validitys='$idStartTime',validitye='$idEndTime' WHERE aptid ='${lines[0][0]}'"
                val i = BaseDAO.getBaseDAO().updateNative(updateSql)
                if (i > 0) return true
            }
        } catch (e: Exception) {

        }
        return false
    }
    //设置企业类型,单体/连锁等
    private fun settingCompStoretype(id: String) :Int{
        try {
            val pattern = Pattern.compile("^[\\u4e00-\\u9fa5]([A-D])[A-B][0-9].*$")
            val m = pattern.matcher(id)
            var index = -1;
            while (m.find()) {
             val res =   m.group(1);
                return when (res){
                    "A" -> -1;
                    "D" -> 0;
                    else -> 1;
                }
            }
        } catch (e: Exception) {
        }
        return -1;
    }

    /**
     * 生成企业资质ID
     */
    private fun genCompanyAptCode(compid: Int, type: Int): Any? {
        return (compid + type).toLong()
    }
}