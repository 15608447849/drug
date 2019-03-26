package com.onek.user.operations;

import com.onek.AppContext;
import com.onek.UserSession;
import com.onek.entitys.IOperation;
import com.onek.entitys.Result;
import constant.DSMConst;
import util.EncryptUtils;
import util.StringUtils;

/**
 * @Author: leeping
 * @Date: 2019/3/26 17:39
 */
public class ChangeInfoOp implements IOperation<AppContext> {
    String newPhone;
    String newPassword; //明文

    @Override
    public Result execute(AppContext context) {
        UserSession session = context.getUserSession();

        int uid = session.userId;

        if ( StringUtils.isEmpty(newPhone)){
            changUserByUid("uphone="+newPassword, "uid = "+ uid);
        }
        if (StringUtils.isEmpty(newPassword)){
            changUserByUid("upw="+ EncryptUtils.encryption(newPassword),"uid = "+ uid);
        }

        return new Result().fail("修改失败");
    }

    private void changUserByUid(String param ,String ifs) {
        String sql = "UPDATE {{?" + DSMConst.D_SYSTEM_USER +"}} SET " + param + " WHERE "+ifs;

    }
}
