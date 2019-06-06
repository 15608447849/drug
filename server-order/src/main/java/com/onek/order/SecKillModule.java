package com.onek.order;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.calculate.ActivityFilterService;
import com.onek.calculate.entity.IDiscount;
import com.onek.calculate.entity.Product;
import com.onek.calculate.filter.ActivitiesFilter;
import com.onek.calculate.filter.QualFilter;
import com.onek.context.AppContext;
import com.onek.context.UserSession;
import com.onek.entity.ShoppingCartVO;
import com.onek.entitys.Result;
import com.onek.service.AccessLimitService;
import com.onek.service.impl.AccessLimitServiceImpl;
import com.onek.util.IceRemoteUtil;
import com.onek.util.RedisGlobalKeys;
import com.onek.util.area.AreaFeeUtil;
import com.onek.util.order.RedisOrderUtil;
import com.onek.util.prod.ProdEntity;
import com.onek.util.prod.ProdInfoStore;
import com.onek.util.stock.RedisStockUtil;
import constant.DSMConst;
import dao.BaseDAO;
import com.onek.util.GenIdUtil;
import redis.util.RedisUtil;
import util.MathUtil;
import util.NumUtil;
import util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 11842
 * @version 1.1.1
 * @description 秒杀模块
 * @time 2019/4/16 11:14
 **/
public class SecKillModule {

    //远程调用
    private static String ACT_PROD_BY_ACTCODE_SQL = "select a.unqid,d.gcode,d.actstock,d.limitnum,d.price,d.cstatus,a.excdiscount from " +
            "{{?" + DSMConst.TD_PROM_ACT + "}} a, {{?" + DSMConst.TD_PROM_ASSDRUG + "}} d " +
            "where a.unqid = d.actcode " +
            "and d.actcode = ? and d.gcode IN (0, ?, ?, ?, ?) " +
            "and a.sdate <= CURRENT_DATE and CURRENT_DATE<= a.edate ";

    private static BaseDAO baseDao = BaseDAO.getBaseDAO();

    private AccessLimitService accessLimitService = new AccessLimitServiceImpl();

    /**
     *
     * 功能: 秒杀前校验用户是否具有资格
     * 参数类型: json
     * 参数集: sku=商品SKU码 actno=活动码
     * 返回值: code=200 data=唯一生成码,用来进行身份校验
     * 详情说明:
     * 作者: 蒋文广
     */
    @UserPermission(ignore = false, needAuthenticated = true)
    public Result beforeSecKill(AppContext appContext) {
        UserSession userSession = appContext.getUserSession();
        int compid = userSession != null ? userSession.compId : 0;
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        long sku = jsonObject.get("sku").getAsLong();
        long actno = jsonObject.get("actno").getAsLong();
        if (actno <= 0 || compid <= 0 || sku<=0) {
            return new Result().fail("秒杀活动非法参数!");
        }
        List<Product> products = new ArrayList<>();
        Product product = new Product();
        product.setSku(sku);
        products.add(product);

        boolean flag = false;
        List<IDiscount> discounts = new ActivityFilterService(new ActivitiesFilter[]{new QualFilter(compid)}).getCurrentActivities(products);
        if(discounts != null && discounts.size() > 0){
            for(IDiscount discount : discounts){
                long discountNo = discount.getDiscountNo();
                if(discountNo == actno){
                    flag = true;
                    break;
                }
            }
        }
        if(!flag){
            return new Result().fail("用户没有资格参加!", null);
        }
        long unqid = GenIdUtil.getUnqId();
        RedisUtil.getStringProvide().set(RedisGlobalKeys.SECKILL_TOKEN_PREFIX  +compid, unqid);
        return new Result().success(unqid);
    }

    /**
     *
     * 功能: 参与秒杀
     * 参数类型: json
     * 参数集: sku=商品SKU码 actno=活动码
     * 返回值: code=200 data=结果集 构造的购物车数据
     * 详情说明: 异常说明 1.秒杀参数非法 2:通过非法途径进入此方法 3:重复提交 4:秒杀过多
     *           5:库存不够
     * 作者: 蒋文广
     */
    @UserPermission(ignore = false, needAuthenticated = true)
    public Result attendSecKill(AppContext appContext) {
        UserSession userSession = appContext.getUserSession();
        int compid = userSession != null ? userSession.compId : 0;
        long addressCode = userSession != null && userSession.comp != null ? userSession.comp.addressCode : 0;
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        long sku = jsonObject.get("sku").getAsLong();
        long actno = jsonObject.get("actno").getAsLong();
        long unqid = jsonObject.get("unqid").getAsLong();
        int stock = jsonObject.get("stock").getAsInt();

        if (actno <= 0 || compid <= 0 || stock <= 0 || sku<=0) {
            return new Result().fail("参加秒杀活动非法参数!");
        }
        if(addressCode <= 0){
            return new Result().fail("获取用户的地区失败!");
        }
        if (!accessLimitService.tryAcquireSecKill()) {
            return new Result().fail("当前秒杀人数过多!", null);
        }

        String key = RedisUtil.getStringProvide().get(RedisGlobalKeys.SECKILL_TOKEN_PREFIX + compid);

        if (StringUtils.isEmpty(key)) {
            return new Result().fail("非法进入页面!");
        }

        if(unqid != Long.parseLong(key)){
            return new Result().fail("请勿重复提交!", null);
        }
        int num = RedisOrderUtil.getActBuyNum(compid, sku ,actno);
        int limitNum = RedisOrderUtil.getActLimit(sku, actno);
//        System.out.println("#### num:"+num + "; limitNum:"+limitNum+"; stock:"+stock);
        if(num > 0 && limitNum > 0 && (limitNum - (num + stock)) < 0){
            return new Result().fail("您秒杀的数量过多或次数过于频繁!");
        }

        ShoppingCartVO shoppingCartVO = getCartSku(actno,sku+"");

        if(shoppingCartVO != null){
            shoppingCartVO.setNum(stock);
            shoppingCartVO.setChecked(1);
            shoppingCartVO.setUnqid("0");
            shoppingCartVO.setCounpon(0);
            shoppingCartVO.setAcamt(stock * shoppingCartVO.getDiscount());
            shoppingCartVO.setAmt(0);

            if(addressCode > 0){
                shoppingCartVO.setFreight(AreaFeeUtil.getFee(addressCode));
            }
        }

        int currentStock = RedisStockUtil.getActStockBySkuAndActno(sku, actno);
        if(currentStock <= 0 || (currentStock - stock) < 0){
            return new Result().fail("库存不够!", null);
        }

        RedisUtil.getStringProvide().delete(RedisGlobalKeys.SECKILL_TOKEN_PREFIX + compid);

        List<ShoppingCartVO> shoppingCartVOS = new ArrayList<>();
        shoppingCartVO.setSeckill(true);
        shoppingCartVOS.add(shoppingCartVO);
        return new Result().success(shoppingCartVOS);
    }


    public ShoppingCartVO getCartSku(long actno,String sku){
        String[] pclasses = getProductCode(Long.parseLong(sku));

        //远程调用
        List<Object[]> queryResult = IceRemoteUtil.queryNative(ACT_PROD_BY_ACTCODE_SQL, actno, sku, pclasses[0], pclasses[1], pclasses[2]);

        if (queryResult.isEmpty()) {
            return null;
        }
        ShoppingCartVO shoppingCartVO = new ShoppingCartVO();
        if(queryResult != null && queryResult.size() > 0){
            Object[] objects = queryResult.get(0);
//            Long gcode = Long.parseLong(objects[1].toString());
            int actstock = Integer.parseInt(objects[2].toString());
            int limitnum = Integer.parseInt(objects[3].toString());
            int prize = Integer.parseInt(objects[4].toString());
            int cstatus = Integer.parseInt(objects[5].toString());
            int excdiscount = Integer.parseInt(objects[6].toString());

            ProdEntity entity =  ProdInfoStore.getProdBySku(Long.parseLong(sku));
            if((cstatus & 512) > 0 && entity.getVatp() > 0){ // 价格百分比
                double rate = MathUtil.exactDiv(prize, 100F).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                prize = MathUtil.exactMul(entity.getVatp(), rate).setScale(0, BigDecimal.ROUND_HALF_DOWN).intValue();
            }

            if(excdiscount == 1){ // 排除优惠券
                shoppingCartVO.setOflag(true);
            }
            shoppingCartVO.setPdno(Long.parseLong(sku));
            shoppingCartVO.setDiscount(NumUtil.div(prize, 100));
            shoppingCartVO.setInventory(actstock);
            shoppingCartVO.setLimitnum(limitnum);

            if(entity != null){
                shoppingCartVO.setPdprice(NumUtil.div(prize, 100));
                shoppingCartVO.setPtitle(entity.getProdname());
                shoppingCartVO.setVerdor(entity.getManuName());
                shoppingCartVO.setVperiod(entity.getVaildedate());

                shoppingCartVO.setSpec(entity.getSpec());
                shoppingCartVO.setStatus(entity.getSkuCstatus());
                shoppingCartVO.setSpu(entity.getSpu());

            }
        }

        return shoppingCartVO;
    }

    private final int checkSKU(long sku) {
        int length = String.valueOf(sku).length();

        switch (length) {
            case 14 :
                return 0;
            default :
                return -1;
        }
    }

    protected final String[] getProductCode(long sku) {
        if (checkSKU(sku) < 0) {
            throw new IllegalArgumentException("SKU is illegal, " + sku);
        }

        String classNo = String.valueOf(sku).substring(1, 7);

        return new String[] {
                classNo.substring(0, 2),
                classNo.substring(0, 4),
                classNo.substring(0, 6) };
    }
}
