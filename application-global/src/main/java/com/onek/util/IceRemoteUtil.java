package com.onek.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import com.onek.client.IceClient;
import com.onek.entity.SyncErrVO;
import com.onek.entitys.Result;
import com.onek.prop.AppProperties;
import com.onek.util.area.AreaEntity;
import com.onek.util.dict.DictEntity;
import com.onek.util.member.MemberEntity;
import com.onek.util.prod.ProdEntity;
import com.onek.util.sqltransfer.SqlRemoteReq;
import com.onek.util.sqltransfer.SqlRemoteResp;
import redis.util.RedisUtil;
import util.EncryptUtils;
import util.GsonUtils;
import util.StringUtils;
import util.http.HttpRequest;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * @Author: leeping
 * @Date: 2019/4/10 19:08
 * 远程调用工具
 */
public class IceRemoteUtil {
    private final static IceClient ic = new IceClient(
            AppProperties.INSTANCE.iceInstance,
            AppProperties.INSTANCE.iceServers)
            .setTimeout(AppProperties.INSTANCE.iceTimeOut)
            .startCommunication();

    /**
     * 根据企业码 获取 分库分表的订单服务的下标序列
     */
    public static int getOrderServerNo(int compid){
        return compid /  GLOBALConst._DMNUM % GLOBALConst._SMALLINTMAX;
    }

    //获取商品名
    public static String getProduceName(String pclass) {
        try {
           return ic.setServerAndRequest("globalServer","CommonModule","getProduceName").setArrayParams(pclass).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //地区码->地区详细名
    public static String getCompleteName(String areaCode) {
        try {
            return ic.setServerAndRequest("globalServer","CommonModule","getCompleteName").setArrayParams(areaCode).execute().replace("\"","").trim();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据sku查询商品信息
     * @param sku
     * @return
     * @author jiangwenguang
     */
    public static ProdEntity getProdBySku(long sku) {
        try {
            String result = ic.setServerAndRequest("globalServer",
                    "CommonModule",
                    "getProdBySku").setArrayParams(sku).execute();
            return GsonUtils.jsonToJavaBean(result,ProdEntity.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * lzp
     * 0短信模板序列id ,1及以后:模板需要的占位符信息参数
     */
    public static String getMessageByNo(String... args){
        try {
            return ic.setServerAndRequest("globalServer","MessageModule","convertMessage")
                    .settingParam(args)
                    .execute().trim();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //lzp 获取模板权限值
    public static int getMessagePower(int tempNo){
        try {
          String status =  ic.setServerAndRequest("globalServer","MessageModule","getMessageTempPower")
                    .setArrayParams(tempNo)
                    .execute().trim();
          return Integer.parseInt(status);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    //lzp , 模板id转换成系统消息
    public static String getMessageByNo(int tempNo,String... params){
        try {
            String[] args = new String[params.length+1];
            args[0] = tempNo+"";
            System.arraycopy(params, 0, args, 1, params.length);
            return getMessageByNo(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据id获取字典信息
     *
     * @param id
     * @return
     * @author jiangwenguang
     */
    public static DictEntity getId(Object id) {
        try {
            String result = ic.setServerAndRequest("globalServer","DictUtilRemoteModule","getId")
                    .setArrayParams(id)
                    .execute();
            return GsonUtils.jsonToJavaBean(result,DictEntity.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //地区码->地址名
    public static String getArean(long areac) {
        String result = ic.setServerAndRequest("globalServer","CommonModule","getAreaName")
                .setArrayParams(areac)
                .execute();
        HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
        assert hashMap != null;
        Object data = hashMap.get("data");
        return data == null ? "" : data.toString();
    }

    //地区码->地区对象
    public static AreaEntity getAreaByAreac(long areac){
        String result = ic.setServerAndRequest("globalServer","CommonModule","getArea")
                .setArrayParams(areac)
                .execute();
        HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
        assert hashMap != null;
        Object data = hashMap.get("data");
        if (data == null) return null;
        String json = data.toString();
        return GsonUtils.jsonToJavaBean(json,AreaEntity.class);
    }

    //地区码->地区对象数组(祖先)
    public static AreaEntity[] getAncestors(long areac){
        String result = ic.setServerAndRequest("globalServer","CommonModule","getAncestors")
                .setArrayParams(areac)
                .execute();

        HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
        assert hashMap != null;
        Object data = hashMap.get("data");
        if (data == null) return null;
        String json = data.toString();
        return GsonUtils.jsonToJavaBean(json,AreaEntity[].class);
    }
    //地区码->地区对象数组(子集)
    public static AreaEntity[] getChildren(long areac){
        String result = ic.setServerAndRequest("globalServer","CommonModule","getChildren")
                .setArrayParams(areac)
                .execute();
        HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
        assert hashMap != null;
        Object data = hashMap.get("data");
        if (data == null) return null;
        String json = data.toString();
        return GsonUtils.jsonToJavaBean(json,AreaEntity[].class);
    }

    //查询所有字典对象
    public static DictEntity[] queryAll() {
        String result = ic.setServerAndRequest("globalServer","DictUtilRemoteModule","queryAll").execute();
        HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
        assert hashMap != null;
        Object data = hashMap.get("data");
        if (data == null) return null;
        String json = data.toString();
        return GsonUtils.jsonToJavaBean(json,DictEntity[].class);
    }

    //查询字典对象
    public static DictEntity[] queryByParams(String [] params) {
        String result = ic.setServerAndRequest("globalServer","DictUtilRemoteModule","queryByParams").settingParam(params).execute();
        HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
        assert hashMap != null;
        Object data = hashMap.get("data");
        if (data == null) return null;
        String json = data.toString();
        return GsonUtils.jsonToJavaBean(json,DictEntity[].class);
    }


    /**
     * 优惠券领取
     * @param compid
     * @param content
     * @return
     */
    public static int collectCoupons(int compid,String content){
        String result = ic.setServerAndRequest("orderServer"+getOrderServerNo(compid),"CouponRevModule","insertRevCoupon")
                .settingParam(content)
                .execute();
        Result ret = GsonUtils.jsonToJavaBean(result,Result.class);
        assert ret != null;
        return ret.code;
    }

    /**
     * 优惠券兑换
     * @param compid
     * @param content
     * @return
     */
    public static int collectExcgCoupons(int compid,String content){
        String result = ic.setServerAndRequest("orderServer"+getOrderServerNo(compid),"CouponRevModule","insertRevExcgCoupon")
                .settingParam(content)
                .execute();
        Result ret = GsonUtils.jsonToJavaBean(result,Result.class);
        assert ret != null;
        return ret.code;
    }

    /**
     * 线下优惠券兑换
     * @param compid
     * @param content
     * @return
     */
    public static long collectOfflineExcgCoupons(int compid,String content){
        String result = ic.setServerAndRequest("orderServer"+getOrderServerNo(compid),"CouponRevModule","insertRevOfflineCoupon")
                .settingParam(content)
                .execute();
        return Long.parseLong(result);

    }


    /**
     * 发送消息到指定客户端
     * 消息规则 :  push:消息模板ID#消息模板参数1#消息模板参数2#...
     * push:1#60030
     */
    public static void sendMessageToClient(int compid,String message){
        try {
            int index = getOrderServerNo(compid);
            ic.settingProxy("orderServer"+index).sendMessageToClient(compid+"",message);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //发送消息到全部客户端
    public static void sendMessageToAllClient(String message){
        List<String> list = getAllCompId();
        for (String compid : list){
            try {
                sendMessageToClient(Integer.parseInt(compid), message);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    //发送系统消息到指定客户端
    public static void sendTempMessageToClient(int compid,int tempNo,String... params){
        if (!SmsTempNo.isPmAllow(tempNo)) return;
        sendMessageToClient(compid,SmsTempNo.genPushMessageBySystemTemp(tempNo,params));
    }

    //发送系统消息到全部客户端
    public static void sendMessageToAllClient(int tempNo,String... params){
        List<String> list = getAllCompId();
        for (String compid : list){
            try {
                sendMessageToClient(Integer.parseInt(compid), SmsTempNo.genPushMessageBySystemTemp(tempNo,params));
            } catch (NumberFormatException ignored) {
            }
        }
    }



    //获取全部的公司码ID
    public static List<String> getAllCompId() {
        String json = ic.setServerAndRequest("userServer","StoreManageModule","getAllCompId").execute();
        return GsonUtils.json2List(json,String.class);
    }

    //获取全部门店用户手机号码
    public static List<String> getAllStorePhone() {
        String json = ic.setServerAndRequest("userServer","StoreManageModule","getAllUserPhone").execute();
        return GsonUtils.json2List(json,String.class);
    }

    //获取指定门店的手机号码
    public static String getSpecifyStorePhone(int compid) {
        String phone =  ic.setServerAndRequest("userServer","StoreManageModule","getSpecifyUserPhone").setArrayParams(compid).execute();
        return StringUtils.isEmpty(phone)? null : phone;
    }

    //获取此团购的团购数
    public static int getGroupCount(long actCode) {
        String json = ic.setServerAndRequest(
                "userServer","GroupBuyModule","getGroupCount")
                .setArrayParams(actCode).execute();
        Result result = JSON.parseObject(json, Result.class);
        if (result == null
                || result.data == null) {
            return 0;
        }
        return (int) result.data;
    }

    //查询所有足迹
    public static List<String> queryFootprint(int compid){
        int index = getOrderServerNo(compid);

        String json = ic.setServerAndRequest("orderServer"+index,"MyFootprintModule","backQuery")
                .setArrayParams(compid).execute();
        return GsonUtils.json2List(json,String.class);
    }

    /**
     * 添加积分明细
     *
     * @param compid 企业id
     * @param istatus 来源
     * @param integral 积分数
     * @param busid 相关业务id
     * @return
     */
    public static int addIntegralDetail(int compid,int istatus,int integral,long busid){

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("compid", compid);
        jsonObject.put("istatus", istatus);
        jsonObject.put("integral", integral);
        jsonObject.put("busid", busid);
        int index = getOrderServerNo(compid);

        String json = ic.setServerAndRequest("orderServer"+index,"IntegralModule","addIntegral")
                .settingParam(jsonObject.toJSONString()).execute();
        Result ret = GsonUtils.jsonToJavaBean(json,Result.class);
        assert ret != null;
        return ret.code;
    }

    /**
     * 查询团购订单信息
     *
     * @param sdate 团购活动起始时间
     * @param edate 团购活动结束时间
     * @param actno 活动码
     * @return
     * @author jiangwenguang
     * @since 1.0
     */
    public static JSONArray queryTeamBuyOrder(String sdate,String edate,String actno){

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sdate", sdate);
        jsonObject.put("edate", edate);
        jsonObject.put("actno", actno);
        int index = getOrderServerNo(RedisGlobalKeys.COMP_INIT_VAR);

        String json = ic.setServerAndRequest("orderServer"+index,"OrderOptModule","queryTeamBuyOrder")
                .settingParam(jsonObject.toJSONString()).execute();
        Result ret = GsonUtils.jsonToJavaBean(json,Result.class);
        return  JSONArray.parseArray(ret.data.toString());
    }

    /**
     * 增加积分
     *
     * @param compid 企业id
     * @return
     * @author jiangwenguang
     * @since 1.0
     */
    public static int addPoint(int compid, int point){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("compid", compid);
        jsonObject.put("point", point);
        String result = ic.settingProxy("userServer")
                .settingReq("","MemberModule","addPoint")
                .settingParam(jsonObject.toJSONString())
                .execute();
        Result ret = GsonUtils.jsonToJavaBean(result,Result.class);
        assert ret != null;
        return ret.code;
    }

    /**
     * 减少积分
     *
     * @param compid 企业id
     * @return
     * @author jiangwenguang
     * @since 1.0
     */
    public static int reducePoint(int compid, int point){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("compid", compid);
        jsonObject.put("point", point);
        String result = ic.settingProxy("userServer")
                .settingReq("","MemberModule","reducePoint")
                .settingParam(jsonObject.toJSONString())
                .execute();
        Result ret = GsonUtils.jsonToJavaBean(result,Result.class);
        assert ret != null;
        return ret.code;
    }

    /**
     * 根据企业id查询会员信息
     *
     * @param compid 企业id
     * @return
     * @author jiangwenguang
     * @since 1.0
     * */
    public static MemberEntity getMemberByCompid(int compid) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("compid", compid);
        String result = ic.settingProxy("userServer")
                .settingReq("","MemberModule","getMember")
                .settingParam(jsonObject.toJSONString())
                .execute();
        HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
        Object data = hashMap.get("data");
        if (data == null) return null;
        String json = data.toString();
        return GsonUtils.jsonToJavaBean(json,MemberEntity.class);
    }

    /**
     * lzp
     * 获取企业信息 json
     */
    public static String getCompanyJson(int compid) {
        try {
            return ic.setServerAndRequest("userServer","LoginRegistrationModule","getStoreInfo").setArrayParams(compid).execute();
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
        return null;
    }
    /*
    优先缓存获取企业信息,
    如果缓存没有,进去数据库查询,自动记录到缓存
     */
    public static String getCompInfoByCacheOrSql(int compid){
        //加载企业信息
        String json = RedisUtil.getStringProvide().get(compid+"");
        if(StringUtils.isEmpty(json)) {
            //远程调用查询
            json = IceRemoteUtil.getCompanyJson(compid);
        };
        return json;
    }


    /**
     * 根据企业码获取订单数
     * @param compid
     * @return
     */
    public static long getOrderCntByCompid(int compid){
        String result = ic.setServerAndRequest("orderServer"+getOrderServerNo(compid),"CouponRevModule","getOrderCntByCompid")
                .setArrayParams(compid)
                .execute();
        return Long.parseLong(result);

    }

    /**
     * 更新余额
     * @param compid
     * @param amt
     * @return
     */
    public static int updateCompBal(int compid,int amt){
        String result = ic.setServerAndRequest("discountServer",
                "CouponManageModule","updateCompBal")
                .setArrayParams(compid,amt)
                .execute();
        return Integer.parseInt(result);
    }

    /**
     * 调整活动库存
     *
     * @param actstock
     * @param actcode
     * @param sku
     * @return
     */
    public static int adjustActivityStock(int actstock,long actcode, long sku){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("actstock", actstock);
        jsonObject.put("actcode", actcode);
        jsonObject.put("sku", sku);
        String result = ic.setServerAndRequest("discountServer",
                "DiscountModule","adjustActivityStock")
                .settingParam(jsonObject.toJSONString())
                .execute();
        return Integer.parseInt(result);
    }

    /**
     * 新增余额
     * @param compid
     * @param amt
     * @return
     */
    public static int insertBalCoup(int compid,int amt){
        String result = ic.setServerAndRequest("orderServer"+getOrderServerNo(compid),"CouponRevModule","insertBalCoup")
                .setArrayParams(compid,amt)
                .execute();
        return Integer.parseInt(result);
    }


    /**
     * 查询企业余额
     * @param compid
     * @return
     */
    public static int queryCompBal(int compid){
        String result = ic.setServerAndRequest("discountServer",
                "CouponManageModule","queryCompBal")
                .setArrayParams(compid)
                .execute();
        return Integer.parseInt(result);
    }

    /**
     * 获取财务的手机号码/姓名
     * lzp
     */
    public static HashMap<String,String> getUserByFinance(){
        return getUserByRoles(RoleCodeCons._FINA);
    }
    /**
     * 获取执行角色的手机号码/姓名
     * lzp
     */
    public static HashMap<String,String> getUserByRoles(int... codes){
        List<Integer> list = new ArrayList<>();
        for (int i = 0 ; i<codes.length ;i++){
            list.add(codes[i]);
        }
        String result = ic.setServerAndRequest("userServer","StoreManageModule","queryUserByRoleCode").setJsonParams(list).execute();
        return GsonUtils.string2Map(result);
    }

    /**
     * 添加公告
     * lzp
     */
    public static String addNotice(String title , String type, String editor,int priority, File image){

        try {
            //上传图片
            String json = new HttpRequest().addFile(
                    image,
                    FileServerUtils.defaultNotice(),  //远程路径
                    EncryptUtils.encryption(type+title+editor)+".png")//文件名
                    .fileUploadUrl(FileServerUtils.fileUploadAddress())//文件上传URL
                    .getRespondContent();
            HashMap<String,Object> maps = GsonUtils.jsonToJavaBean(json,new TypeToken<HashMap<String,Object>>(){}.getType());
            ArrayList<LinkedTreeMap<String,Object>> list = (ArrayList<LinkedTreeMap<String, Object>>) maps.get("data");
            assert list != null;
            String img = list.get(0).get("relativePath").toString();

            HashMap<String,Object> map = new HashMap<>();
            map.put("title",title);
            map.put("type",type);
            map.put("editor",editor);
            map.put("img",img);
            map.put("priority",priority);
            return ic.setServerAndRequest("globalServer","NoticeModule","add").setJsonParams(map).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }






    /**
     * 记录新人有礼活动领取优惠券记录
     * @param compid
     * @param content
     * @return
     */
    public static int insertNewComerBalCoup(int compid,String content){
        try {
            String result = ic.setServerAndRequest("orderServer"+getOrderServerNo(compid),"CouponRevModule","insertNewComerBalCoupon")
                    .settingParam(content)
                    .execute();
            Result ret = GsonUtils.jsonToJavaBean(result,Result.class);
            assert ret != null;
            return ret.code;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 领取新人有礼活动领取优惠券记录
     * @param compid
     * @return
     */
    public static int revNewComerCoupon(int compid,long pho){
        try {
            String result = ic.setServerAndRequest("discountServer","CouponManageModule","revNewComerCoupons")
                    .setArrayParams(compid,pho)
                    .execute();
            Result ret = GsonUtils.jsonToJavaBean(result,Result.class);
            assert ret != null;
            return ret.code;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 满赠优惠券
     * @param compid
     * @return
     */
    public static int revGiftCoupon(long orderno,int compid){
        try {
            String result = ic.setServerAndRequest("orderServer"+getOrderServerNo(compid),"CouponRevModule","revGiftCoupon")
                    .setArrayParams(orderno,compid)
                    .execute();
            Result ret = GsonUtils.jsonToJavaBean(result,Result.class);
            assert ret != null;
            return ret.code;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }


    /**
     * 根据spu查询相对于的商品的sku集合
     * @param spu 0代表全部商品
     * @return
     */
    public static List<Long> querySkuListByCondition(long spu){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("spu", spu);
        String json = ic.setServerAndRequest("goodsServer",
                "ProdExtModule","getSkuListByCondition")
                .settingParam(jsonObject.toJSONString())
                .execute();
        return GsonUtils.json2List(json,Long.class);
    }

    /**
     * 调用全局服务,传递sql执行
     */
    public static int[] updateBatchNative(String sql,List<Object[]> params,int len){
        String json = ic.setServerAndRequest("globalServer",
                "InternalCallModule","updateBatchNative")
                .setJsonParams(new SqlRemoteReq(sql, params, len))
                .execute();
        return Objects.requireNonNull(GsonUtils.jsonToJavaBean(json, SqlRemoteResp.class)).resArr;
    }

    public static  List<Object[]> queryNative(String sql,Object... params){
        String json = ic.setServerAndRequest("globalServer",
                "InternalCallModule","queryNative")
                .setJsonParams(new SqlRemoteReq(sql, params))
                .execute();
        return Objects.requireNonNull(GsonUtils.jsonToJavaBean(json, SqlRemoteResp.class)).getLines();
    }

    public static int updateNative(String sql,final Object... params){
        String json = ic.setServerAndRequest("globalServer",
                "InternalCallModule","updateNative")
                .setJsonParams(new SqlRemoteReq(sql, params))
                .execute();
        return Objects.requireNonNull(GsonUtils.jsonToJavaBean(json, SqlRemoteResp.class)).res;
    }

    public static int[] updateTransNative(String[] sql,List<Object[]> params){
        String json = ic.setServerAndRequest("globalServer",
                "InternalCallModule","updateTransNative")
                .setJsonParams(new SqlRemoteReq(sql, params))
                .execute();
        return Objects.requireNonNull(GsonUtils.jsonToJavaBean(json, SqlRemoteResp.class)).resArr;
    }

    /**
     * 满赠优惠券
     * @param
     * @return
     */
    public static int insertApprise(String json){
        try {
            String result = ic.setServerAndRequest("goodsServer",
                    "ProdModule","insertApprise")
                    .setArrayParams(json)
                    .execute();
            return Integer.parseInt(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 获取购物车数量
     */
    public static int remoteQueryShopCartNumBySku(int compid,long sku){
        return Integer.parseInt(ic.setServerAndRequest("orderServer" + getOrderServerNo(compid),
                "ShoppingCartModule",
                "remoteQueryShopCartNumBySku").setArrayParams(compid,sku).execute());
    }

    /**
     * 获取购物车数量
     */
    public static String queryShopCartNumBySkus(int compid, String sku){
        return ic.setServerAndRequest("orderServer" + getOrderServerNo(compid),
                "ShoppingCartModule",
                "queryShopCartNumBySkus").setArrayParams(compid,sku).execute();
    }

    /**
     * 获取所有套餐购物车数量
     */
    public static String queryPkgShopCartNum(int compid, String pkgnos){
        return ic.setServerAndRequest("orderServer" + getOrderServerNo(compid),
                "ShoppingCartModule",
                "queryPkgShopCartNum").setArrayParams(compid,pkgnos).execute();
    }



    public static boolean systemConfigOpen(String name) {
        return Boolean.parseBoolean(ic.setServerAndRequest("userServer",
                "SyncCustomerInfoModule","systemConfigIsOpen").setArrayParams(name).execute());
    }

    public static int insertOrUpdSyncErr(SyncErrVO errVO) {
        return Integer.parseInt(ic.setServerAndRequest("userServer",
                "SyncCustomerInfoModule","insertOrUpdSyncErr").setJsonParams(errVO).execute());
    }

    public static int updSyncCState(long syncid) {
        return Integer.parseInt(ic.setServerAndRequest("userServer",
                "SyncCustomerInfoModule","updSyncCState").setArrayParams(syncid).execute());
    }


    public static String getErpSkuBySku(String sks){
        try {
            return ic.setServerAndRequest("globalServer",
                    "CommonModule","getAllErpSkuBySku")
                    .setArrayParams(sks)
                    .execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getErpSKU(String sku){
        try {
            return ic.setServerAndRequest("globalServer",
                    "CommonModule", "getErpSKU")
                    .setArrayParams(sku)
                    .execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取购物车数量
     */
    public static String batchProduceSalesOrder(String orderNoStr, int compId){
        return ic.setServerAndRequest("orderServer" + getOrderServerNo(compId),
                "OrderDockedWithERPModule",
                "batchProduceSalesOrder").setArrayParams(orderNoStr).execute();
    }

    /**
     * 领取优惠券记录
     * @param compid
     * @return
     */
    public static int couponRevRecord(int compid,long coupno,int qlfno){
        String result = ic.setServerAndRequest("discountServer",
                "CouponManageModule","couponRevRecord")
                .setArrayParams(compid,coupno,qlfno)
                .execute();
        return Integer.parseInt(result);
    }

    /**
     * 记录新人有礼活动领取优惠券记录
     * @param compid
     * @param content
     * @return
     */
    public static int insertNewComerCoups(int compid,String content){
        try {
            String result = ic.setServerAndRequest("orderServer"+getOrderServerNo(compid),"CouponRevModule","revNewCoupons")
                    .settingParam(content)
                    .execute();
            Result ret = GsonUtils.jsonToJavaBean(result,Result.class);
            assert ret != null;
            return ret.code;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 获取订单详情
     * @param compid
     * @param orderno
     * @return
     */
    public static String getOrderDetail(int compid,long orderno){
        try {
            String result = ic.setServerAndRequest("orderServer"+getOrderServerNo(compid),"OrderInfoModule","getOrderDetail")
                    .setArrayParams(compid,orderno)
                    .execute();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 获取余额抵扣率
     * @return
     */
    public static String getUseBal(String key){
        try {
            String result = ic.setServerAndRequest("userServer","BackgroundUserModule","getUseBal")
                    .setArrayParams(key)
                    .execute();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void main(String[] args) {
        HashMap map = new HashMap();
            map.put("identity",-4);

            HashMap map2 = new HashMap();
//                map2.put("manuname","华润三九医药股份有限公司委托惠州市九惠制药股份有限公司");

            map.put("jsonStr",GsonUtils.javaBeanToJson(map2));
        String json =
                ic.setServerAndRequest("goodsServer","MainPageModule","pageInfo")
                .settingParam(GsonUtils.javaBeanToJson(map),1,10)
                .execute();
        ic.stopCommunication();
    }

}



