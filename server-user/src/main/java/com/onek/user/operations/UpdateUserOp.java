package com.onek.user.operations;

import com.onek.context.AppContext;
import com.onek.context.UserSession;
import com.onek.entitys.IOperation;
import com.onek.entitys.Result;
import constant.DSMConst;
import dao.BaseDAO;
import redis.util.RedisUtil;
import util.EncryptUtils;
import util.StringUtils;

/**
 * @Author: leeping
 * @Date: 2019/3/26 17:39
 */
public class UpdateUserOp implements IOperation<AppContext> {
    String oldPhone;//旧手机号码
    String newPhone;
    String smsCode;//输入的短信验证码

    String oldPassword;//旧密码 - 明文
    String newPassword; //明文  - 明文



    @Override
    public Result execute(AppContext context) {
        UserSession session = context.getUserSession();

        int uid = session.userId;

        //修改手机号码
        if ( !StringUtils.isEmpty(oldPhone,newPhone, smsCode)){
            if (newPhone.length()==11) {
                String code = RedisUtil.getStringProvide().get("SMS"+oldPhone);
                if (code.equals(smsCode) && !newPhone.equals(oldPhone)){
                    context.getUserSession().phone = newPhone;
                    return changUserByUid(context,"uphone="+newPhone, "uid = "+ uid);
                }
            }
            }

        //修改密码
        if (!StringUtils.isEmpty(oldPassword,newPassword)){
            String curPassword = session.password;
            if (EncryptUtils.encryption(oldPassword).equalsIgnoreCase(curPassword)){
                return changUserByUid(context,"upw='"+ EncryptUtils.encryption(newPassword)+"'","uid = "+ uid);
            }
        }

        //忘记密码
        if (!StringUtils.isEmpty(oldPhone,smsCode,newPassword)){
            String code = RedisUtil.getStringProvide().get("SMS"+oldPhone);
            if (code.equals(smsCode)){
                return changUserByUid(context,"upw='"+ EncryptUtils.encryption(newPassword)+"'","uid = "+ uid);
            }
        }
        return new Result().fail("修改失败");
    }

    private Result changUserByUid(AppContext context,String param ,String ifs) {
        String sql = "UPDATE {{?" + DSMConst.D_SYSTEM_USER +"}} SET " + param + " WHERE "+ifs;
        int i = BaseDAO.getBaseDAO().updateNative(sql);
        if (i>0) {
            context.relationTokenUserSession();
            return new Result().success("修改成功");
        }
        return new Result().fail("修改失败," +sql);
    }
}
