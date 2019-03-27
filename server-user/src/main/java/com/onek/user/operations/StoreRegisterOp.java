package com.onek.user.operations;

import com.onek.AppContext;
import com.onek.entitys.IOperation;
import com.onek.entitys.Result;
import constant.DSMConst;
import dao.BaseDAO;
import redis.util.RedisUtil;
import util.EncryptUtils;
import util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

import static com.onek.RedisGlobalKeys.*;

/**
 * @Author: leeping
 * @Date: 2019/3/13 13:51
 * 用户注册
 *
 */
public class StoreRegisterOp implements IOperation<AppContext> {

    // 1 效验手机号是否存在
    // 2 获取短信验证码
    // 3 申请注册
    public int type = 0;
    String phone;
    String password; //密码明文传输 - 后台加密 ,登录时-前端需MD5加密传输匹配
    String password2;
    String smsCode;

    @Override
    public Result execute(AppContext context) {

        if (StringUtils.isEmpty(phone) || phone.length() != 11) return new Result().fail("无效的手机号码");
        if (type == 1) return checkPhoneIsExist();
        if (type == 2) {
            return new VerificationOp().setType(type).setPhone(phone).execute(context);
        }
        if (type == 3) {
            if (!validSmsCode()) return new Result().fail("短信验证码不正确");
            if (!validPassword()) return new Result().fail("不符合密码安全性要求:\n" +
                    "至少6位字符,包含1个大写字母,1个小写字母,1个特殊字符");
            if (StringUtils.isEmpty(password2) || !password.equals(password2)) return new Result().fail("两次密码输入不一致");
            return submit();
        }
        return new Result().fail("未知的操作类型");
    }

    //效验短信验证码
    private boolean validSmsCode() {
        if (StringUtils.isEmpty(smsCode)) return false;
        if (!smsCode.equals("000000")) return false;
        return true;
    }

    //正则匹配密码规则
    private boolean validPassword(){
        if (StringUtils.isEmpty(password)) return false;
        String pattern  = "^" +
                "(?![A-Za-z0-9]+$)" +
                "(?![a-z0-9\\W]+$)" +
                "(?![A-Za-z\\W]+$)" +
                "(?![A-Z0-9\\W]+$)" +
                "[A-Za-z0-9\\W]" +
                "{6,}" +
                "$";
        return Pattern.matches(pattern, password);
    }

    // 提交
    private Result submit() {
        //获取角色码
        long userId = getUserCode();
            //添加角色
           String insertSql = "INSERT INTO {{?" + DSMConst.D_SYSTEM_USER + "}} " +
                    "(uid,uphone,upw,roleid,adddate,addtime) " +
                    "VALUES(? , ? , ? , ? , CURRENT_DATE,CURRENT_TIME)";

            int i = BaseDAO.getBaseDAO().updateNative(insertSql,
                    userId,
                    phone ,
                    EncryptUtils.encryption(password), //MD5加密
                    2
            );
            if (i > 0) {
                return new Result().success("注册成功");
            }
        return new Result().fail("注册失败");
    }

    //验证手机是否存在
    private Result checkPhoneIsExist() {
        String selectSql = "SELECT oid FROM {{?" + DSMConst.D_SYSTEM_USER + "}} WHERE cstatus&1 = 0 AND uphone = ?";
        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql,phone);
        if (lines.size()>0) return new Result().fail("已注册");
            else return new Result().success("未注册");
    }


}
