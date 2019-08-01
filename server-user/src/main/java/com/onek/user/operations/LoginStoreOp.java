package com.onek.user.operations;

import com.onek.context.AppContext;
import com.onek.entitys.IOperation;
import com.onek.entitys.Result;
import com.onek.user.service.USProperties;
import com.onek.util.MSGUtil;
import redis.util.RedisUtil;
import util.StringUtils;

import static Ice.Application.communicator;
import static util.TimeUtils.formatDuring;

/**
 * @Author: leeping
 * @Date: 2019/3/12 15:05
 */
public class LoginStoreOp extends LoginAbsOp implements IOperation<AppContext> {

    String phone;
    String password;
    String key;//图形验证码KEY
    String verification; //图形验证码

    private int failIndex = -1;//剩余次数
    private long lockRemainderTime = -1;

    protected LoginStoreOp() {
        super(128);
    }

    @Override
    public Result execute(AppContext context) {
        try {
            if (!checkAccountPassword(phone,password)) return new Result().fail(error);
            //检测验证码是否正确
            if (!checkVerification()) return new Result().fail(MSGUtil.LOGIN_VERIFICATION_CODE_ERROR);
            //检查用户是否锁定
            if (checkLock()) return new Result().fail(error);
            //检测用户名/密码是否正确 创建会话
            if (!checkSqlAndUserExist(context.remoteIp,phone,password,2)) return new Result().fail(error).setHashMap("index",failIndex);;
            //关联token-用户信息
            if (relationTokenUserSession(context,true)) return new Result().success(MSGUtil.LOGIN_SUCCESS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Result().fail(MSGUtil.LOGIN_FAIL);
    }

    @Override
    void loginSuccess() {
        //锁定次数初始化,移除锁定
        removeLockCache();
    }

    @Override
    void passwordError() {
        loginFailHandle();
    }

    //用户-登陆失败处理
    private void loginFailHandle() {
        error = MSGUtil.LOGIN_USER_OR_PWD_ERROR;
        //密码错误 记录次数 - 缓存
        //当次数到达指定次数 , 锁定账户, 指定时间
        String indexStr = RedisUtil.getStringProvide().get("USER-"+phone);
        if (StringUtils.isEmpty(indexStr)) indexStr = "0"; //初始化
        failIndex = Integer.parseInt(indexStr);
        failIndex++;
        if (failIndex > USProperties.INSTANCE.sLoginNumMax) {
            //锁定用户
            RedisUtil.getStringProvide().set("USER-LOCK-"+phone,"LOCK");
            //设置时效性
            RedisUtil.getStringProvide().expire("USER-LOCK-"+phone,  USProperties.INSTANCE.sLoginLockTime);
            error+=","+MSGUtil.LOGIN_USER_LOCK;
        }else{
            //记录次数
            RedisUtil.getStringProvide().set("USER-"+phone,String.valueOf(failIndex));
            //设置时效性
            RedisUtil.getStringProvide().expire("USER-"+phone,  USProperties.INSTANCE.sLoginLockTime);
            error+=",当前登陆失败"+failIndex+"次,超过"+USProperties.INSTANCE.sLoginNumMax+"次将锁定账号";
        }
    }


    //锁定次数初始化-移除锁定
    public void removeLockCache() {
        if (StringUtils.isEmpty(phone)) return;
        RedisUtil.getStringProvide().delete("USER-"+phone);
        RedisUtil.getStringProvide().delete("USER-LOCK-"+phone);
    }

    //检测图形验证码
    private boolean checkVerification() {
        if (StringUtils.isEmpty(key)) return false;
        if (key.equals("uncheck")) return true;
        if (StringUtils.isEmpty(verification)) return false;
        String code = RedisUtil.getStringProvide().get(key);
        communicator().getLogger().print("图形验证码 key = "+ key +" , code = "+ code +" , verification = "+ verification);
        if (!code.equalsIgnoreCase(verification)){
            error = MSGUtil.LOGIN_VERIFICATION_CODE_ERROR;
            return false;
        };
        return true;
    }

    //检测是否锁定用户
    private boolean checkLock() {
        String val = RedisUtil.getStringProvide().get("USER-LOCK-"+phone);
        if (val!=null){
            lockRemainderTime = RedisUtil.getStringProvide().remainingSurvivalTime("USER-LOCK-"+phone); //判断是否锁定
            if (lockRemainderTime <= 0) {
                lockRemainderTime = USProperties.INSTANCE.sLoginLockTime;
            }
            error = "用户已锁定,请在"+formatDuring(lockRemainderTime * 1000L)+"后再尝试登陆";
            return true;
        }

        return false;
    }



}
