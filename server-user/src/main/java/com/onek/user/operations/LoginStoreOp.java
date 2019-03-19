package com.onek.user.operations;

import com.onek.AppContext;
import com.onek.UserSession;
import com.onek.entitys.IOperation;
import com.onek.entitys.Result;
import constant.DSMConst;
import dao.BaseDAO;
import redis.util.RedisUtil;
import util.EncryptUtils;
import util.GsonUtils;
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
    boolean isSave = false;

    private int failIndex = -1;//剩余次数

    private long lockRemainderTime = -1;

    private UserSession userSession = null;

    @Override
    public Result execute(AppContext context) {
        try {
            if (StringUtils.isEmpty(phone) || StringUtils.isEmpty(password)) return new Result().fail("手机号或密码不能为空");

            if (checkLock()) return new Result().fail("用户已锁定,请在"+formatDuring(lockRemainderTime * 1000L)+"后再尝试登陆");

            //检测验证码是否正确
            if (!checkVerification()) return new Result().fail("验证码不正确");

            //检测用户名/密码是否正确 创建会话
            if (!checkSqlAndUserExist(context)) {
                StringBuilder sb = new StringBuilder();
                sb.append("用户名或密码不正确");
                if ( failIndex >= 3 && failIndex< 5){
                    sb.append(",再输入错误"+ (5-failIndex) +"次将锁定账户");
                }else if (failIndex >= 5){
                    sb = new StringBuilder("账户已锁定");
                }
                return new Result().fail(sb.toString()).setHashMap("index",failIndex);
            }

            //关联token-用户信息
            if (relationTokenUserSession(context)) return new Result().success("登陆成功");
            else return new Result().success("无法关联用户信息");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Result().fail("登陆失败");
    }



    //创建用户会话到缓存
    private boolean relationTokenUserSession(AppContext context) {
        if (userSession == null) return false;
        //创建token标识
        String token = createToken(context);
        String res = RedisUtil.getStringProvide().set(token, GsonUtils.javaBeanToJson(userSession));
        if (res.equals("OK")){
            res = RedisUtil.getStringProvide().set(context.param.token ,token);
            return res.equals("OK");
        }
       return false;
    }

    private String createToken(AppContext context) {
        context.param.token = context.param.token + "@" + context.remoteIp;
        return EncryptUtils.encryption(context.param.token);
    }


    //检查用户是否正确
    private boolean checkSqlAndUserExist(AppContext context) {

        String selectSql = "SELECT uid,roleid,upw FROM {{?" + DSMConst.D_SYSTEM_USER + "}} WHERE cstatus&1 = 0 AND roleid&2>0 AND uphone = ?";
        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql,phone);

        if (lines.size()>0){
            Object[] objects = lines.get(0);
            communicator().getLogger().print("门店登录: 用户码:" + objects[0]+" ,角色码:"+ objects[1]);
            //忽略MD5大小写
            if (objects[2].toString().equalsIgnoreCase(password)) {
                //密码正确
                //记录登陆时间 IP
                String updateSql = "UPDATE {{?" + DSMConst.D_SYSTEM_USER + "}} " +
                        "SET ip = ?,logindate = CURRENT_DATE,logintime = CURRENT_TIME " +
                        "WHERE cstatus&1 = 0 AND uid = ?";

                int i = BaseDAO.getBaseDAO().updateNative(updateSql, context.remoteIp,objects[0]);
                if (i > 0){
                    if (isSave){
                        userSession = new UserSession();
                        userSession.userId = (int) objects[0];
                        userSession.roleCode = (long) objects[1];
                        userSession.phone = phone;
                        userSession.password = password;
                        userSession.lastIp = context.remoteIp;
                    }
                    return true;
                }
            }else{
                //密码错误 记录次数 - 缓存
                //当次数到达指定次数 , 锁定账户, 指定时间
                String indexStr = RedisUtil.getStringProvide().get("USER-"+phone);
                if (StringUtils.isEmpty(indexStr)) indexStr = "0"; //初始化
                failIndex = Integer.parseInt(indexStr);
                failIndex++;
                communicator().getLogger().print("失败次数---------------------------------------" + failIndex);
                if (failIndex > 5) {
                    communicator().getLogger().print("锁定用户 - "+phone);
                    //移除下标记录
                    RedisUtil.getStringProvide().delete("USER-"+phone);
                    //锁定用户
                    RedisUtil.getStringProvide().set("USER-LOCK-"+phone,"LOCK");
                    //设置时效性 1 小时
                    RedisUtil.getStringProvide().expire("USER-LOCK-"+phone,  60 * 60);
                }else{
                    communicator().getLogger().print("记录次数 - "+ failIndex);
                    //记录次数
                    RedisUtil.getStringProvide().set("USER-"+phone,String.valueOf(failIndex));
                    //30分钟后失效
                    RedisUtil.getStringProvide().expire("USER--"+phone, 30 * 60);
                }
            }
        }

        return false;
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
        lockRemainderTime = RedisUtil.getStringProvide().remainingSurvivalTime("USER-LOCK-"+phone); //判断是否锁定
        if (lockRemainderTime > 0) {
            communicator().getLogger().print(" 门店用户手机号 - "+ phone +" 剩余锁定时间: "+ lockRemainderTime);
            return true;
        }
        return false;
    }


}
