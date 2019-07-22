package com.onek.user.operations

import com.onek.consts.IntegralConstant
import com.onek.user.service.MemberImpl
import com.onek.util.IceRemoteUtil
import com.onek.util.member.MemberEntity
import redis.IRedisPartCache
import redis.proxy.CacheProxyInstance



/**
 * @Author: leeping
 * @Date: 2019/3/27 11:25
 * 企业资质审核
 */
class UpdateAuditOp {
    companion object {
        //赠送积分
         fun giftPoints(companyId: Int) {
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
    }
}