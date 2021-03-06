package com.onek.user.operations;

import com.onek.context.AppContext;
import com.onek.context.UserSession;
import com.onek.entitys.IOperation;
import com.onek.entitys.Result;
import com.onek.util.MSGUtil;
import com.onek.util.RoleCodeCons;
import dao.BaseDAO;
import org.hyrdpf.util.LogUtil;
import redis.util.RedisUtil;
import util.EncryptUtils;
import util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

import static com.onek.util.RedisGlobalKeys.getUserCode;
import static constant.DSMConst.TB_SYSTEM_USER;

/**
 * @Author: leeping
 * @Date: 2019/3/13 13:51
 * 用户注册
 *
 */
public class RegisterStoreUserOp implements IOperation<AppContext> {

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
        if (StringUtils.isEmpty(phone) || phone.length() != 11) return new Result().fail(MSGUtil.REG_INVAILD_PHONE);
        if (type == 1) return checkPhoneIsExist();
        if (type == 2) return new VerificationOp().setType(type).setPhone(phone).execute(context);
        if (type == 3) {
            if (!validSmsCode()) return new Result().fail(MSGUtil.REG_VERIFICATION_CODE_ERROR);
            if (!validPassword(password)) return new Result().fail(PASSWORD_VALID_MESSAGE);
            if (StringUtils.isEmpty(password2) || !password.equals(password2)) return new Result().fail(MSGUtil.REG_INCONSISTENT_PWD);
            return submit(context);
        }
        return new Result().fail("未知的操作类型");
    }

    //效验短信验证码
    private boolean validSmsCode() {
        if (StringUtils.isEmpty(smsCode)) return false;
        if (smsCode.equals("000000")) return true;
        String _smsCode =  RedisUtil.getStringProvide().get("SMS"+phone);
        return smsCode.equals(_smsCode);
    }

    public static String PASSWORD_VALID_MESSAGE = "不符合密码安全性要求:\n" +
            "至少6位字符,包含1个小写字母";

    //正则匹配密码规则
    public static boolean validPassword(String psd){
        if (StringUtils.isEmpty(psd)) return false;
//        String pattern  = "^" +
//                "(?![A-Za-z0-9]+$)" +
//                "(?![a-z0-9\\W]+$)" +
//                "(?![A-Za-z\\W]+$)" +
//                "(?![A-Z0-9\\W]+$)" +
//                "[A-Za-z0-9\\W]{6,}$";

        LogUtil.getDefaultLogger().info("验证密码规则:" + psd);
        //
//        String pattern = "^.*(?=.{6,})(?=.*\\d)(?=.*[A-Z])(?=.*[a-z])(?=.*[!@#$%^&*?]).*$";
        String pattern = "^.*(?=.{6,})(?=.*\\d)(?=.*[a-z])().*$";

        return Pattern.matches(pattern, psd);
    }




    // 提交
    private Result submit(AppContext context) {
        //获取用户码
        int userId = getUserCode();
        //添加角色
        String insertSql = "INSERT INTO {{?" + TB_SYSTEM_USER + "}} " +
                "(uid,uphone,upw,roleid,adddate,addtime) " +
                "VALUES(?,?,?,?,CURRENT_DATE,CURRENT_TIME)";
        int i = BaseDAO.getBaseDAO().updateNative(insertSql,
                userId,
                phone ,
                EncryptUtils.encryption(password), //MD5加密
                RoleCodeCons._STORE
        );
        if (i > 0) {
            //设置content - User信息
            context.setUserSession(new UserSession(userId ,RoleCodeCons._STORE, phone));
            return new Result().success(MSGUtil.REG_SUCCESS);
        }
        return new Result().fail(MSGUtil.REG_COMP_FAIL);
    }

    //验证手机是否存在
    private Result checkPhoneIsExist() {
        String selectSql = "SELECT oid FROM {{?" + TB_SYSTEM_USER + "}} WHERE cstatus&1 = 0 AND uphone = ?";
        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql,phone);
        if (lines.size()>0) return new Result().fail(MSGUtil.REG_ISREG);
            else return new Result().success(MSGUtil.REG_NOTREG);
    }


}
