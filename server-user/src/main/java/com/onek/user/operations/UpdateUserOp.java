package com.onek.user.operations;

import Ice.Application;
import com.onek.context.AppContext;
import com.onek.context.UserSession;
import com.onek.entitys.IOperation;
import com.onek.entitys.Result;
import constant.DSMConst;
import dao.BaseDAO;
import redis.util.RedisUtil;
import util.EncryptUtils;
import util.StringUtils;

import static com.onek.user.operations.RegisterStoreUserOp.PASSWORD_VALID_MESSAGE;
import static com.onek.user.operations.RegisterStoreUserOp.validPassword;

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

    String rmsg = "";

    public int type = 0;

    @Override
    public Result execute(AppContext context) {
        boolean flag = false;
        if (type == 0){
            //修改手机/密码
            UserSession session = context.getUserSession();
            if (session != null){
                int uid = session.userId;
                if ( !StringUtils.isEmpty(oldPhone,newPhone, smsCode)){//修改手机号码
                    if (newPhone.length()==11) { // 判断手机号码
                        if (!newPhone.equals(oldPhone)){ //判断新号码不等于旧号码
                            String code = RedisUtil.getStringProvide().get("SMS"+oldPhone); //获取短信验证码
                            if (code.equals(smsCode)){ //判断短信验证
                                session.phone = newPhone;
                                flag = changUserByUid("uphone='"+newPhone+"'", "uid='"+ uid+"' AND uphone='"+oldPhone+"'");
                                if(flag) rmsg = "已修改您的手机号码,请重新登陆";
                                else rmsg = "无法修改手机号码";
                            }else{
                                Application.communicator().getLogger().print("缓存验证码:" + code+" , 短信验证码:"+smsCode);
                                rmsg = "验证码不正确,请重新输入";
                            }
                        }else{
                            rmsg = "新手机号码与旧手机号码相同";
                        }
                    }else{
                        rmsg = "手机号码位数不正确";
                    }
                }else if (!StringUtils.isEmpty(oldPassword,newPassword)){ //修改密码 , 判断不为空
                    String curPassword = session.password; //当前密码
                    String inputOldPassword = EncryptUtils.encryption(oldPassword);
                    String inputNewPassword = EncryptUtils.encryption(newPassword);
                    if (inputOldPassword.equalsIgnoreCase(curPassword)) {
                        if( !inputNewPassword.equalsIgnoreCase(curPassword)){
                            //如果当前密码与输入的旧密码相同 并且 新密码与旧密码不相同
                            //检查密码正确性
                            if (validPassword(newPassword)){
                                session.password = inputNewPassword;
                                flag = changUserByUid("upw='"+ inputNewPassword +"'","uid='"+ uid+"'");
                                if(flag) rmsg = "修改成功,请使用新密码登陆";
                                else rmsg = "无法修改密码";
                            }else{
                                rmsg = PASSWORD_VALID_MESSAGE;
                            }
                        }else{
                            rmsg = "原密码与新密码相同";
                        }
                    }else{
                        rmsg = "原密码不正确";
                    }
                }
                if (flag)  context.relationTokenUserSession(); //修改成功 关联用户信息
            }else{ rmsg = "用户信息异常"; }
        }

        if (type == 1){
            //忘记密码
            if (!StringUtils.isEmpty(oldPhone,smsCode,newPassword)){
                String code = RedisUtil.getStringProvide().get("SMS"+oldPhone);
                if (smsCode.equals(code)){
                    //检查密码正确性
                    if (validPassword(newPassword)){
                        flag = changUserByUid("upw='"+ EncryptUtils.encryption(newPassword)+"'","uphone='"+ oldPhone+"'");
                        if (flag) rmsg = "修改成功,请使用新密码登陆";
                        else rmsg = "无法修改密码";
                    }else{
                        rmsg = PASSWORD_VALID_MESSAGE;
                    }
                }else{
                    rmsg = "验证码不正确";
                }
            }else{
                rmsg = "手机号码不正确或密码不符合要求";
            }
        }


        return flag? new Result().success(rmsg).message(rmsg) : new Result().fail(rmsg) ;
    }

    private Boolean changUserByUid(String param ,String ifs) {
        String sql = "UPDATE {{?" + DSMConst.TB_SYSTEM_USER +"}} SET " + param + " WHERE "+ifs;
        return BaseDAO.getBaseDAO().updateNative(sql) > 0;
    }


}
