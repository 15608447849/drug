package com.onek.discount;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.util.IceRemoteUtil;
import com.onek.util.RoleCodeCons;
import com.onek.util.SmsTempNo;
import com.onek.util.SmsUtil;
import com.onek.util.prod.ProdEntity;
import com.onek.util.prod.ProdInfoStore;
import constant.DSMConst;
import dao.BaseDAO;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DiscountModule {

    private static BaseDAO baseDao = BaseDAO.getBaseDAO();

    /*public Result getGoodsActInfo(AppContext appContext) {
        String[] arrays = appContext.param.arrays;

        if (arrays == null || arrays.length == 0) {
            return new Result().fail("参数为空");
        }

        if (!StringUtils.isBiggerZero(arrays[0])
                || !StringUtils.isBiggerZero(arrays[1])) {
            return new Result().fail("参数错误");
        }

        long sku = Long.parseLong(arrays[0]);
        long actcode = Long.parseLong(arrays[1]);

        List<Product> products = new ArrayList<>();
        Product p = new Product();
        p.setSku(sku);

        List<IDiscount> discounts
                = new ActivityFilterService(
                        new ActivitiesFilter[] { new CycleFilter() })
                .getCurrentActivities(products);

        IDiscount currDiscount = null;

        JSONObject jsonObject = new JSONObject();

        for (IDiscount discount : discounts) {
            if (discount.getDiscountNo() == actcode) {
                currDiscount = discount;
                break;
            }
        }

        if (currDiscount == null) {
            return new Result().success(jsonObject);
        }

        Ladoff[] ladoffs =
                new ActivityCalculateService().getLadoffs(currDiscount.getBRule());

        if (ladoffs.length == 0) {
            return new Result().success(jsonObject);
        }

        jsonObject.put("currentDate", TimeUtils.date_yMd_Hms_2String(new Date()));
        jsonObject.put("startTime", currDiscount.getStartTime());
        jsonObject.put("endTime", currDiscount.getEndTime());
        jsonObject.put("limits", currDiscount.getLimits(sku));

        if (currDiscount.getBRule() == 1133) {
            // 团购
            jsonObject.put("ladoffs", ladoffs);
        }

        return new Result().success(jsonObject);
    }*/

    /*public Result getActivitiesBySKU(AppContext appContext) {
        String[] arrays = appContext.param.arrays;

        if (arrays == null || arrays.length == 0) {
            return new Result().fail("参数为空");
        }

        if (!StringUtils.isBiggerZero(arrays[0])) {
            return new Result().fail("参数错误");
        }

        long sku = Long.parseLong(arrays[0]);

        List<Product> products = new ArrayList<>();
        Product p = new Product();
        p.setSku(sku);

        List<IDiscount> discounts
                = new ActivityFilterService(
                        new ActivitiesFilter[] { new CycleFilter() })
                 .getCurrentActivities(products);

        return new Result().success(discounts);
    }*/


    /**
     *
     * 功能: 调整活动库存
     * 参数类型: json
     * 参数集: actstock=活动库存 actno=活动码 sku=商品SKU码
     * 返回值: code=200 data=结果集 构造的购物车数据
     * 详情说明:当活动库存大于库存时调整时触发,并发送短信给运营人员
     * 作者: 蒋文广
     */
    /**
     * @接口摘要 调整活动库存
     * @业务场景 当活动库存大于库存时调整时触发,并发送短信给运营人员
     * @传参类型 JSON
     * @传参列表 actstock=活动库存 actno=活动码 sku=商品SKU码
     * @返回列表 code=200 data=结果信息
     * @作者: 蒋文广
     */
    @UserPermission(ignore = true)
    public int adjustActivityStock(AppContext appContext) {

        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        long actstock = json.get("actstock").getAsInt();
        long actcode = json.get("actcode").getAsLong();
        long sku = json.get("sku").getAsLong();
        String updateSQL = "update {{?" + DSMConst.TD_PROM_ASSDRUG + "}} set actstock=? where cstatus&1=0 and actcode=? and gcode=?";
        int result = baseDao.updateNative(updateSQL, actstock, actcode, sku);

        ExecutorService executors = Executors.newSingleThreadExecutor();
        executors.execute(() -> {
                    HashMap<String,String> userMap = IceRemoteUtil.getUserByRoles(RoleCodeCons._OPER);
                    if(userMap != null && userMap.size() > 0){
                        ProdEntity prodEntity = ProdInfoStore.getProdBySku(sku);
                        if(prodEntity != null){
                            for(String key : userMap.keySet()){
                                SmsUtil.sendSmsBySystemTemp(key, SmsTempNo.ACTIVE_INVENTORY,userMap.get(key),prodEntity.getProdname());
                            }
                        }
                    }
                }

        );


        return result;
    }
}
