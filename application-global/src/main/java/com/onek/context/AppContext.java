package com.onek.context;

import Ice.Current;
import com.onek.server.inf.IRequest;
import com.onek.server.infimp.IceContext;
import com.onek.util.IceRemoteUtil;
import redis.util.RedisUtil;
import util.EncryptUtils;
import util.GsonUtils;
import util.StringUtils;

/**
 * 平台上下文对象
 */
public class AppContext extends IceContext {


    private UserSession userSession;

    public AppContext(Current current, IRequest request) throws Exception {
        super(current, request);
    }

    //初始化用户信息 lzp
    @Override
    public void initialization(){

            //加载用户信息
            if (StringUtils.isEmpty(param.token)) {
//                logger.print("没有token信息,不加载用户信息,匿名访问");
                return;
            }
            String key = genUKey();
            String json = RedisUtil.getStringProvide().get(key); //获取用户信息
            userSession = GsonUtils.jsonToJavaBean(json, UserSession.class);
            if (userSession == null) {
//                logger.print("KEY : "+ key +" 找不到缓存的用户信息 ");
                return;
            };
            if (userSession.compId > 0) {
                //加载企业信息
                json = RedisUtil.getStringProvide().get(userSession.compId+"");
                if(StringUtils.isEmpty(json)) {
                    //远程调用查询
                    json = IceRemoteUtil.getCompanyJson(userSession.compId);
                };
                userSession.comp = GsonUtils.jsonToJavaBean(json, StoreBasicInfo.class);
            }
            if (userSession!=null) logger.print(" ##-------当 前 用 户 ( "+ key +" )----->>>"+userSession);
    }

    public UserSession getUserSession() {
        return userSession;
    }
        //登录时设置
    public void setUserSession(UserSession userSession) {
        this.userSession = userSession;
    }


    //创建用户会话KRY lzp
    private String genUKey() {
        return EncryptUtils.encryption(param.token);
    }

    //创建多端检测key
    private String genUKeyByMore(){
        return  userSession.userId+ "@" +userSession.compId + "@" +genTempType() ;//EncryptUtils.encryption();
    }

    //终端类型
    private String genTempType() {
        if (!StringUtils.isEmpty(param.token) && param.token.contains("@")){
            String[] arr = param.token.split("@");
            return arr[arr.length-1];
        }
        return "Unknown";
    }

    //创建用户会话到缓存 lzp
    //用户登陆时 - 存入 用户信息,同时存入 用户信息对应的在线用户信息
    public boolean relationTokenUserSession() {
        if (userSession == null) return false;
        //创建token 标识
        return  RedisUtil.getStringProvide().set(genUKey(), GsonUtils.javaBeanToJson(userSession)).equals("OK"); //写入用户信息到缓存
    }

    //存储多点登陆判断条件-仅对门用户登陆有效
    public void storeUserMappingToken() {
        checkMorePointLogin();
        //防止多点登陆 - 存入  当前登陆用户k = 已存在的token
        RedisUtil.getStringProvide().set(genUKeyByMore(),genUKey());
        logger.print("登陆成功, 设置当前用户("+genUKeyByMore()+") 唯一token = " +" = "+ genUKey());

    }


    //检测是存在多端登陆的情况
    private void checkMorePointLogin() {
        String token = RedisUtil.getStringProvide().get(genUKeyByMore());
        logger.print(genUKeyByMore()+" - 检测是否存在多点登陆,上一个用户信息token = "+token+", 当前用户信息token = "+ genUKey());
        if (StringUtils.isEmpty(token)) return;
        if (!token.equals(genUKey())){
            //删除上一个用户token
            RedisUtil.getStringProvide().delete(token);
            logger.print(genUKeyByMore()+" - 检测多点登陆,删除上一个用户token缓存信息 = "+token);
        }
    }

    //清理用户会话
    public boolean clearTokenByUserSession(){
        if (userSession!=null){
            RedisUtil.getStringProvide().delete(genUKey());
            RedisUtil.getStringProvide().delete(genUKeyByMore());
            userSession = null;
            return true;
        }
        return false;
    }

    //是否匿名访问
    public boolean isAnonymous(){
        if (userSession == null || userSession.compId == 0 || userSession.comp == null) return true;
        return userSession.comp.authenticationStatus != 256;//认证成功
    }



}
