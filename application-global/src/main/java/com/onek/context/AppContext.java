package com.onek.context;

import Ice.Current;
import com.onek.entitys.Result;
import com.onek.server.inf.IRequest;
import com.onek.server.inf.PushMessageClientPrx;
import com.onek.server.infimp.IceContext;
import com.onek.util.IceRemoteUtil;
import redis.util.RedisUtil;
import util.EncryptUtils;
import util.GsonUtils;
import util.StringUtils;

import java.util.Map;

/**
 * 平台上下文对象
 */
public class AppContext extends IceContext {


    private UserSession userSession;

    public AppContext(Current current, IRequest request)  {
        super(current, request);
    }

    //初始化用户信息 lzp
    @Override
    public void initialization(){

            //加载用户信息
            if (StringUtils.isEmpty(param.token)) return;
            String key = genUKey();
            String json = RedisUtil.getStringProvide().get(key);
            userSession = GsonUtils.jsonToJavaBean(json, UserSession.class);

            if (userSession == null) return;
            if (userSession.compId > 0) {
                //加载企业信息
                json = RedisUtil.getStringProvide().get(userSession.compId+"");
                if(StringUtils.isEmpty(json)) {
                    //远程调用查询
                    json = IceRemoteUtil.getCompanyJson(userSession.compId);
                };
                userSession.comp = GsonUtils.jsonToJavaBean(json, StoreBasicInfo.class);
            }
            if (userSession!=null) logger.print(" ##------- 当 前 用 户 ( "+ key +" ) ----->>  用户码:" + userSession.userId + " ,企业码:"+ userSession.compId );
    }

    public UserSession getUserSession() {
        return userSession;
    }
        //登录时设置
    public void setUserSession(UserSession userSession) {
        this.userSession = userSession;
    }


    //创建用户会话KRY lzp
    public String genUKey() {
        return EncryptUtils.encryption(param.token + "@" + remoteIp);
    }

    //创建多端检测key
    public String genUKeyByMore(){
        return EncryptUtils.encryption(userSession.roleCode+ "@" +userSession.userId);
    }

    //创建用户会话到缓存 lzp
    public boolean relationTokenUserSession() {
        if (userSession == null) return false;
        //创建token 标识
        String key = genUKey();
        String json = GsonUtils.javaBeanToJson(userSession);

        String res = RedisUtil.getStringProvide().set(key,json);
//        logger.print( key + " ||===写入用户信息到缓存======== "+ res +" =======>> "+ json);
        return  res.equals("OK");
    }

    public boolean clearTokenByUserSession(){
        if (userSession!=null){
            String key = genUKey();
            RedisUtil.getStringProvide().delete(key);
            String key2 = genUKeyByMore();
            RedisUtil.getStringProvide().delete(key2);
            userSession = null;
            return true;
        }
        return false;
    }

    @Override
    protected void longConnectionSetting(Map<String, PushMessageClientPrx> map, Result result) {
        try {
            if (userSession==null || userSession.compId == 0) return;
            String key = userSession.compId+"";
            PushMessageClientPrx clientPrx = map.get(key);
            if (clientPrx == null) {
                result.setRequestOnline();
            } else {
                try {
                    clientPrx.ice_ping();
                } catch (Exception e) {
                    result.setRequestOnline();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkMorePointLogin() {
        //检测是存在多端登陆的情况
        String key = genUKeyByMore();
        String val = RedisUtil.getStringProvide().get(key);
        String val2 = genUKey();
        if (!StringUtils.isEmpty(val)) {
            if (!val.equals(val2)){
                //删除上一个
                RedisUtil.getStringProvide().delete(val);
            }
        }
        RedisUtil.getStringProvide().set(key,val2);//防止多点登陆使用
    }
}
