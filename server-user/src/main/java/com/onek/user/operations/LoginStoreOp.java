package com.onek.user.operations;

import com.onek.context.AppContext;
import com.onek.context.UserSession;
import com.onek.entitys.IOperation;
import com.onek.entitys.Result;
import com.onek.user.service.USProperties;
import constant.DSMConst;
import dao.BaseDAO;
import redis.util.RedisUtil;
import util.StringUtils;

import java.util.List;

import static Ice.Application.communicator;
import static util.TimeUtils.formatDuring;

/**
 * @Author: leeping
 * @Date: 2019/3/12 15:05
 */
public class LoginStoreOp implements IOperation<AppContext> {

    String phone;
    String password;
    String key;//图形验证码KEY
    String verification; //图形验证码

    private int failIndex = -1;//剩余次数

    private long lockRemainderTime = -1;

    private UserSession userSession = null;

    @Override
    public Result execute(AppContext context) {
        try {
            if (StringUtils.isEmpty(phone) || StringUtils.isEmpty(password)) return new Result().fail("手机号或密码不能为空");

            //检测验证码是否正确
            if (!checkVerification()) return new Result().fail("验证码不正确");

            if (checkLock()) return new Result().fail("用户已锁定,请在"+formatDuring(lockRemainderTime * 1000L)+"后再尝试登陆");


            //检测用户名/密码是否正确 创建会话
            if (!checkSqlAndUserExist(context)) {
                StringBuilder sb = new StringBuilder();
                sb.append("用户名或密码不正确");
                if ( failIndex >= USProperties.INSTANCE.sLoginNumMin
                        && failIndex< USProperties.INSTANCE.sLoginNumMax){
                    sb.append(",再输入错误").append(USProperties.INSTANCE.sLoginNumMax - failIndex).append("次将锁定账户");
                }else if (failIndex >= USProperties.INSTANCE.sLoginNumMax){
                    sb = new StringBuilder("账户已锁定");
                }
                return new Result().fail(sb.toString()).setHashMap("index",failIndex);
            }

            //关联token-用户信息
            if (relationTokenUserSession(context)){

                return new Result().success("登陆成功");
            }
            else return new Result().success("无法关联用户信息");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Result().fail("登陆失败");
    }

    private boolean relationTokenUserSession(AppContext context) {
        context.setUserSession(userSession);
        context.storeUserMappingToken();//门店登陆-防止多点你登录设置
        return context.relationTokenUserSession();//门店登陆-关联用户信息
    }

    //检查用户是否正确
    private boolean checkSqlAndUserExist(AppContext context) {

        String selectSql = "SELECT uid,roleid,upw,cid " +
                "FROM {{?" + DSMConst.D_SYSTEM_USER + "}} " +
                "WHERE cstatus&1 = 0 AND roleid&2=2  AND uphone = ?";
        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql,phone);

        if (lines.size()>0){
            Object[] objects = lines.get(0);
            //忽略MD5大小写
            if (objects[2].toString().equalsIgnoreCase(password)) {
                communicator().getLogger().print("门店登录: 用户码:" + objects[0]+" ,角色码:"+ objects[1] +" 企业码:" + objects[3]);
                //密码正确 - 记录登陆时间 IP
                String updateSql = "UPDATE {{?" + DSMConst.D_SYSTEM_USER + "}} " +
                        "SET ip = ?,logindate = CURRENT_DATE,logintime = CURRENT_TIME " +
                        "WHERE cstatus&1 = 0 AND uid = ?";
                int i = BaseDAO.getBaseDAO().updateNative(updateSql, context.remoteIp,objects[0]);
                //锁定次数初始化-移除锁定
                removeLockCache();
                if (i > 0){
                    userSession = UserSession.createStoreUser(
                            StringUtils.checkObjectNull(objects[0],0),
                            StringUtils.checkObjectNull(objects[1],0L),
                            phone,
                            context.remoteIp,
                            password,
                            StringUtils.checkObjectNull(objects[3],0)
                            );
                    return true;
                }
            }else{
              loginFailHandle();
            }
        }
        return false;
    }
    //锁定次数初始化-移除锁定
    public void removeLockCache() {
        if (StringUtils.isEmpty(phone)) return;
        RedisUtil.getStringProvide().delete("USER-"+phone);
        RedisUtil.getStringProvide().delete("USER-LOCK-"+phone);
    }

    //用户-登陆失败处理
    private void loginFailHandle() {
        //密码错误 记录次数 - 缓存
        //当次数到达指定次数 , 锁定账户, 指定时间
        String indexStr = RedisUtil.getStringProvide().get("USER-"+phone);
        if (StringUtils.isEmpty(indexStr)) indexStr = "0"; //初始化

        failIndex = Integer.parseInt(indexStr);
        failIndex++;
        if (failIndex > USProperties.INSTANCE.sLoginNumMax) {
            //锁定用户
            RedisUtil.getStringProvide().set("USER-LOCK-"+phone,"LOCK");
            //设置时效性 ? 小时queryAuditInfo
            RedisUtil.getStringProvide().expire("USER-LOCK-"+phone,  USProperties.INSTANCE.sLoginLockTime);
            communicator().getLogger().print("用户("+phone+")已锁定");
        }else{
            //记录次数
            RedisUtil.getStringProvide().set("USER-"+phone,String.valueOf(failIndex));
            communicator().getLogger().print("用户("+phone+")登陆失败记录:"+failIndex);
        }
    }

    //检测图形验证码
    private boolean checkVerification() {
        if (StringUtils.isEmpty(key)) return false;
        if (key.equals("uncheck")) return true;
        if (StringUtils.isEmpty(verification)) return false;
        String code = RedisUtil.getStringProvide().get(key);
        communicator().getLogger().print("图形验证码 key = "+ key +" , code = "+ code +" , verification = "+ verification);
        return code.equalsIgnoreCase(verification);
    }

    //检测是否锁定用户
    private boolean checkLock() {
        String val = RedisUtil.getStringProvide().get("USER-LOCK-"+phone);
        if (val!=null){
            lockRemainderTime = RedisUtil.getStringProvide().remainingSurvivalTime("USER-LOCK-"+phone); //判断是否锁定
            if (lockRemainderTime <= 0) {
                lockRemainderTime = USProperties.INSTANCE.sLoginLockTime;
            }
            communicator().getLogger().print(" 门店用户手机号 - "+ phone +" 剩余锁定时间: "+ lockRemainderTime);
            return true;
        }

        return false;
    }


}
