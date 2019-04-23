package com.onek.user.operations

import com.onek.context.AppContext
import com.onek.entitys.IOperation
import com.onek.entitys.Result
import com.onek.user.interactive.AptitudeInfo
import com.onek.user.operations.StoreBasicInfoOp.updateCompInfoToCacheById
import com.onek.util.RedisGlobalKeys
import constant.DSMConst
import dao.BaseDAO

/**
 * @Author: leeping
 * @Date: 2019/3/27 11:25
 * 企业资质审核
 */
class UpdateAuditOp :AptitudeInfo(), IOperation<AppContext> {

    var companyId:String? = null; //公司码
    var auditCause:String? = null //审核失败原因
    var auditStatus:Int = 0; //审核状态


    override fun execute(context: AppContext?): Result {

        //修改企业审核信息
        //修改门店状态 1.移除当前状态 2.添加新状态
        val selectSql = "SELECT cstatus FROM {{?" + DSMConst.D_COMP + "}} WHERE cstatus&1 = 0 AND cid = ?"

        val lines = BaseDAO.getBaseDAO().queryNative(selectSql,companyId!!)

        if (lines.size > 0) {

            var status = lines.get(0)[0] as Int //状态
            if (status and 64 == 64) {
                status = 64 //待认证
            } else if (status and 128 == 128) {
                status = 128 //审核中
            } else if (status and 256 == 256) {
                status = 256 //已认证
            } else if (status and 512 == 512) {
                status = 512 //认证失败
            } else if (status and 1024 == 1024) {
                status = 1024 //停用
            }

            if(auditStatus == 256){ //审核通过
                var isOK = updateIds(companyId!!,businessId!!,businessIdStart!!,businessIdEnd!!,10)
                if (isOK) isOK = updateIds(companyId!!,permitId!!,permitIdStart!!,permitIdEnd!!,11)
                if (isOK) isOK = updateIds(companyId!!,gspId!!,gspIdStart!!,gspIdEnd!!,12)
                if (!isOK) return Result().fail("资质信息保存失败")
            }

            //修改 门店信息
            val updateSql = "UPDATE {{?" + DSMConst.D_COMP + "}} SET cstatus=cstatus&~$status|$auditStatus,examine=?,auditdate=CURRENT_DATE,audittime=CURRENT_TIME WHERE cstatus&1=0 AND cid=?"
            val i = BaseDAO.getBaseDAO().updateNative(updateSql,auditCause,companyId)
            if (i > 0) {
                updateCompInfoToCacheById(companyId!!.toInt())//更新企业信息到缓存
               return Result().success("审核保存提交成功")
            }
        }
        return Result().fail("审核修改失败")
    }

    private fun updateIds(companyId: String, id: String, idStartTime: String, idEndTime: String, type:Int) :Boolean{
        try {
            val selectSql = "SELECT aptid FROM {{?${DSMConst.D_COMP_APTITUDE}}} WHERE compid = $companyId AND atype = $type"
            val lines = BaseDAO.getBaseDAO().queryNative(selectSql);
            if (lines.size == 0){
                //新增
                val aptid = RedisGlobalKeys.getCompanyAptCode()
                val insertSql = "INSERT INTO {{?${DSMConst.D_COMP_APTITUDE}}} (aptid,compid,atype,certificateno,validitys,validitye) VALUES (?,?,?,?,?,?)"
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
                val updateSql = "UPDATE {{?${DSMConst.D_COMP_APTITUDE}}} SET certificateno=$id,validitys='$idStartTime',validitye='$idEndTime' WHERE aptid =${lines[0][0]}"
                val i = BaseDAO.getBaseDAO().updateNative(updateSql)
                if (i > 0) return true
            }
        } catch (e: Exception) { }
        return false
    }
}