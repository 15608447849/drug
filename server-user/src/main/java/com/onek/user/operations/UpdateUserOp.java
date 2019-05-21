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

        boolean flag = false;
        if (session != null){

            int uid = session.userId;
            //修改手机号码
            if ( !StringUtils.isEmpty(oldPhone,newPhone, smsCode)){
                if (newPhone.length()==11) { // 判断手机号码
                    if (!newPhone.equals(oldPhone)){ //判断新号码不等于旧号码
                        String code = RedisUtil.getStringProvide().get("SMS"+oldPhone); //获取短信验证码
                        if (code.equals(smsCode)){ //判断短信验证
                            session.phone = newPhone;
                            flag = changUserByUid("uphone="+newPhone, "uid = "+ uid);
                        }
                    }
                }
            }

            //修改密码
            if (!StringUtils.isEmpty(oldPassword,newPassword)){ //判断不为空

                String curPassword = session.password; //当前密码
                String inputOldPassword = EncryptUtils.encryption(oldPassword);
                String inputNewPassword = EncryptUtils.encryption(newPassword);
                if (inputOldPassword.equalsIgnoreCase(curPassword) && !inputNewPassword.equalsIgnoreCase(curPassword)){
                    //如果当前密码与输入的旧密码相同 并且 新密码与旧密码不相同
                    session.password = inputNewPassword;
                    flag = changUserByUid("upw='"+ inputNewPassword +"'","uid = "+ uid);
                }
            }
            if (flag)  context.relationTokenUserSession(); //修改成功 关联用户信息
        }

        //忘记密码
        if (!StringUtils.isEmpty(oldPhone,smsCode,newPassword)){
            String code = RedisUtil.getStringProvide().get("SMS"+oldPhone);
            if (code.equals(smsCode)){
                flag = changUserByUid("upw='"+ EncryptUtils.encryption(newPassword)+"'","uphone = '"+ oldPhone+"' AND roleid");
            }
        }

        if (flag) return new Result().success("修改成功");
        return new Result().fail("修改失败");
    }

    private Boolean changUserByUid(String param ,String ifs) {
        String sql = "UPDATE {{?" + DSMConst.TB_SYSTEM_USER +"}} SET " + param + " WHERE "+ifs;
        return BaseDAO.getBaseDAO().updateNative(sql) > 0;
    }
}
