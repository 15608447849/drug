package com.onek.user.operations

import com.onek.AppContext
import com.onek.RedisGlobalKeys
import com.onek.entitys.IOperation
import com.onek.entitys.Result
import com.sun.org.apache.xpath.internal.operations.Bool
import constant.DSMConst
import dao.BaseDAO

/**
 * @Author: leeping
 * @Date: 2019/3/27 11:25
 * glacier2router --Glacier2.Client.Endpoints="tcp -h 38.108.85.159 -p 50000" --Glacier2.PermissionsVerifier=Glacier2/NullPermissionsVerifier
 */
class UpdateAuditOp : IOperation<AppContext> {

    var companyId:String? = null; //公司码
    var businessId:String? = null; //营业执照
    var businessIdStart:String? = null; //营业执照 有效开始
    var businessIdEnd:String? = null; //营业执照 有效结束

    var permitId:String? = null; //经营许可证
    var permitIdStart:String? = null; //经营许可证
    var permitIdEnd:String? = null; //经营许可证

    var gspId:String? = null; //gsp
    var gspIdStart:String? = null; //gsp
    var gspIdEnd:String? = null; //gsp

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
        if (auditStatus == status) return Result().fail("请改变审核状态再保存提交")

        var isOK = updateIds(companyId!!,businessId!!,businessIdStart!!,businessIdEnd!!,10)
        if (isOK) isOK = updateIds(companyId!!,permitId!!,permitIdStart!!,permitIdEnd!!,11)
        if (isOK) isOK = updateIds(companyId!!,gspId!!,gspIdStart!!,gspIdEnd!!,12)
            if (!isOK) return Result().fail("资质信息保存失败")

            //修改 门店信息
            val updateSql = "UPDATE {{?" + DSMConst.D_COMP + "}} SET cstatus=cstatus&~$status|$auditStatus,examine=?,auditdate=CURRENT_DATE,audittime=CURRENT_TIME WHERE cstatus&1=0 AND cid=?"
            val i = BaseDAO.getBaseDAO().updateNative(updateSql,auditCause,companyId)
            if (i > 0) {
               return Result().success("审核保存提交成功")
            }
        }
        return Result().fail("审核修改失败")
    }

    private fun updateIds(companyId: String, id: String, idStartTime: String, idEndTime: String, type:Int) :Boolean{
        val selectSql = "SELECT aptid FROM {{?${DSMConst.D_COMP_APTITUDE}}} WHERE compid = $companyId"
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

        }

        return false
    }
}