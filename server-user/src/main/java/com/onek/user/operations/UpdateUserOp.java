package com.onek.user.operations;

import com.onek.AppContext;
import com.onek.UserSession;
import com.onek.entitys.IOperation;
import com.onek.entitys.Result;
import constant.DSMConst;
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

        if ( !StringUtils.isEmpty(oldPhone,newPhone,smsCode)){
            String code = RedisUtil.getStringProvide().get(oldPhone);
            if (code.equals(smsCode) && !newPhone.equals(oldPhone)){
                changUserByUid("uphone="+newPhone, "uid = "+ uid);
            }
        }

        if (!StringUtils.isEmpty(oldPassword,newPassword)){
            String curPassword = session.password;
            if (EncryptUtils.encryption(oldPassword).equalsIgnoreCase(curPassword)){
                changUserByUid("upw="+ EncryptUtils.encryption(newPassword),"uid = "+ uid);
            }
        }
        return new Result().fail("修改失败");
    }

    private void changUserByUid(String param ,String ifs) {
        String sql = "UPDATE {{?" + DSMConst.D_SYSTEM_USER +"}} SET " + param + " WHERE "+ifs;

    }
}
