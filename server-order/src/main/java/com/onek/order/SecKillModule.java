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
import com.onek.util.RedisGlobalKeys;
import com.onek.util.prod.ProdEntity;
import com.onek.util.prod.ProdInfoStore;
import com.onek.util.stock.RedisStockUtil;
import constant.DSMConst;
import dao.BaseDAO;
import com.onek.util.GenIdUtil;
import redis.util.RedisUtil;
import util.NumUtil;
import util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 11842
 * @version 1.1.1
 * @description 秒杀模块
 * @time 2019/4/16 11:14
 **/
public class SecKillModule {

    private static String ACT_PROD_BY_ACTCODE_SQL = "select a.unqid,d.gcode,d.actstock,d.limitnum,d.price from " +
            "{{?" + DSMConst.TD_PROM_ACT + "}} a, {{?" + DSMConst.TD_PROM_ASSDRUG + "}} d " +
            "where a.unqid = d.actcode " +
            "and d.actcode = ? and d.gcode= ?" +
            "and a.sdate <= CURRENT_DATE and CURRENT_DATE<= a.edate ";

    private static BaseDAO baseDao = BaseDAO.getBaseDAO();

    private AccessLimitService accessLimitService = new AccessLimitServiceImpl();

    @UserPermission(ignore = false)
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


    @UserPermission(ignore = false)
    public Result attendSecKill(AppContext appContext) {
        UserSession userSession = appContext.getUserSession();
        int compid = userSession != null ? userSession.compId : 0;
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
        boolean exist = RedisUtil.getSetProvide().existElement(RedisGlobalKeys.SECKILLPREFIX + sku, compid);
        if(exist){
            return new Result().fail("不要重复秒杀!");
        }

        ShoppingCartVO shoppingCartVO = getCartSku(actno,sku+"");

        if(shoppingCartVO != null){
            shoppingCartVO.setNum(stock);
            shoppingCartVO.setChecked(1);
            shoppingCartVO.setUnqid(0);
            shoppingCartVO.setCounpon(0);
            shoppingCartVO.setAcamt(stock * shoppingCartVO.getDiscount());
            shoppingCartVO.setAmt(shoppingCartVO.getPdprice() * stock - shoppingCartVO.getAcamt());
        }

        int currentStock = RedisStockUtil.getActStockBySkuAndActno(sku, actno);
        if(currentStock <= 0 || (currentStock - stock) <= 0){
            return new Result().fail("库存不够!", null);
        }

        RedisUtil.getSetProvide().addElement(RedisGlobalKeys.SECKILLPREFIX + sku, compid);
        RedisUtil.getStringProvide().delete(RedisGlobalKeys.SECKILL_TOKEN_PREFIX + compid);

        List<ShoppingCartVO> shoppingCartVOS = new ArrayList<>();
        shoppingCartVO.setSeckill(true);
        shoppingCartVOS.add(shoppingCartVO);
        return new Result().success(shoppingCartVOS);
    }


    public ShoppingCartVO getCartSku(long actno,String sku){
        List<Object[]> queryResult = baseDao.queryNative(ACT_PROD_BY_ACTCODE_SQL, new Object[]{ actno, sku});

        if (queryResult.isEmpty()) {
            return null;
        }
        ShoppingCartVO shoppingCartVO = new ShoppingCartVO();
        if(queryResult != null && queryResult.size() > 0){
            Object[] objects = queryResult.get(0);
            Long gcode = Long.parseLong(objects[1].toString());
            int actstock = Integer.parseInt(objects[2].toString());
            int limitnum = Integer.parseInt(objects[3].toString());
            int prize = Integer.parseInt(objects[4].toString());


            shoppingCartVO.setPdno(gcode);
            shoppingCartVO.setDiscount(NumUtil.div(prize, 100));
            shoppingCartVO.setInventory(actstock);
            shoppingCartVO.setLimitnum(limitnum);

            ProdEntity entity =  ProdInfoStore.getProdBySku(Long.parseLong(sku));
            if(entity != null){
                shoppingCartVO.setPdprice(NumUtil.div(entity.getVatp(), 100));
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
}
