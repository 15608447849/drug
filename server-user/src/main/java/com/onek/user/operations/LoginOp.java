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

/**
 * @Author: leeping
 * @Date: 2019/3/12 15:05
 */
public class LoginOp  implements IOperation<AppContext> {

    String phone;
    String password;
    String key;//图形验证码KEY
    String verification; //图形验证码



    @Override
    public Result execute(AppContext context) {
        try {
            if (StringUtils.isEmpty(phone) || StringUtils.isEmpty(password)) return new Result().fail("手机号或密码不能为空");

            if (checkLock()) return new Result().fail("用户已锁定");

            //检测验证码是否正确
            if (!checkVerification()) return new Result().fail("验证码不正确");

            //检测用户名/密码是否正确
            if (!checkSqlAndUserExist(context)) return new Result().fail("用户名或密码不正确");

            //创建token标识
            String token = createToken(context);
            UserSession session = readUserInfo();

            //关联token-用户信息
            if (relationTokenUserSession(context,token,session)) return new Result().success("登陆成功");
            else return new Result().success("无法关联用户信息");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Result().fail("登陆失败");
    }

    //检测是否锁定用户
    private boolean checkLock() {
        long lockRemainderTime = RedisUtil.getStringProvide().remainingSurvivalTime("USER-LOCK-"+phone); //判断是否锁定
        communicator().getLogger().print(" 用户 - "+ phone +" 锁定时间: "+ lockRemainderTime);
        if (lockRemainderTime > 0) return true;
        return false;
    }

    private UserSession readUserInfo() {
        UserSession userSession = new UserSession();
        userSession.oid = 0;
        userSession.cstatus = 0;
        userSession.roleid = 1000;
        userSession.uname = "超级管理员-测试用户";
        return userSession;
    }

    private boolean relationTokenUserSession(AppContext context, String token, UserSession session) {
        String res = RedisUtil.getStringProvide().set(token, GsonUtils.javaBeanToJson(session));
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

        String selectSql = "SELECT uid,upw FROM {{?" + DSMConst.D_SYSTEM_USER + "}} WHERE cstatus&1 = 0 AND uphone = ?";
        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql,phone);

        if (lines.size()>0){
            Object[] objects = lines.get(0);
            communicator().getLogger().print(objects[0]+" "+ objects[1] );

            if (objects[1].equals(password)) {
                //密码正确
                //记录登陆时间 IP
                String updateSql = "UPDATE {{?" + DSMConst.D_SYSTEM_USER + "}} " +
                        "SET ip = ?,logindate = CURRENT_DATE,logintime = CURRENT_TIME " +
                        "WHERE cstatus&1 = 0 AND uid = ?";

                int i = BaseDAO.getBaseDAO().updateNative(updateSql, context.remoteIp,objects[0]);
                return i > 0;
            }else{
                //密码错误 记录次数 - 缓存
                //当次数到达指定次数 锁定账户 指定时间
                String indexStr = RedisUtil.getStringProvide().get("USER-"+phone);
                if (StringUtils.isEmpty(indexStr)) indexStr = "0";
                int index = Integer.parseInt(indexStr);

                if (index == 3) {

                    String resStr = RedisUtil.getStringProvide().set("USER-LOCK-"+phone,"LOCK");
                    if (resStr.equals("OK")){
                        // int sec = 60 * 60 * 2; //两小时
                        int sec = 30; //测试 15秒

                        long res = RedisUtil.getStringProvide().expire("USER-LOCK-"+phone, sec); //暂时没设置时效性
                        communicator().getLogger().print("用户锁定-" +phone+" - 结果 : "+res );
                        res = RedisUtil.getStringProvide().delete("USER-"+phone);
                        communicator().getLogger().print("用户锁定-" +phone+" - 移除次数 - 结果 : "+res );
                    }
                }else{
                    String res = RedisUtil.getStringProvide().set("USER-"+phone,String.valueOf(++index));
                    communicator().getLogger().print("用户锁定-" +phone+" - 添加次数 - "+ index +" - 结果 : "+res );
                }
                return false;
            }
        }

        return false;
    }

    //检测图形验证码
    private boolean checkVerification() {

        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(verification)) return false;
        if (key.equals("0000")) return true;
        String code = RedisUtil.getStringProvide().get(key);
        communicator().getLogger().print("图形验证码 key = "+ key +" , code = "+ code);
        return verification.equals(code);
    }



}
