package com.onek.order;

import com.google.gson.*;
import com.onek.annotation.UserPermission;
import com.onek.calculate.ActivityFilterService;
import com.onek.calculate.entity.*;
import com.onek.calculate.filter.*;
import com.onek.context.AppContext;
import com.onek.entity.DiscountRule;
import com.onek.entity.OfferTipsVO;
import com.onek.entity.ShoppingCartDTO;
import com.onek.entity.ShoppingCartVO;
import com.onek.entitys.Result;
import com.onek.util.CalculateUtil;
import com.onek.util.area.AreaFeeUtil;
import com.onek.util.discount.DiscountRuleStore;
import com.onek.util.order.RedisOrderUtil;
import com.onek.util.stock.RedisStockUtil;
import constant.DSMConst;
import dao.BaseDAO;
import com.onek.util.GenIdUtil;
import util.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName ShoppingCartModule
 * @Description TODO
 * @date 2019-04-15 20:54
 */
public class ShoppingCartModule {

    private static BaseDAO baseDao = BaseDAO.getBaseDAO();

    //新增购物车
    private final String INSERT_SHOPCART_SQL = "insert into {{?" + DSMConst.TD_TRAN_GOODS + "}} "
            + "(unqid,orderno,pdno,pnum,createdate,createtime,compid,cstatus) "
            + "values (?,?,?,?,CURRENT_DATE,CURRENT_TIME,?,?)";


    //新增购物车
    private final String SELECT_SHOPCART_SQL_EXT = "select unqid,pnum from {{?" + DSMConst.TD_TRAN_GOODS + "}} "
            + " where orderno = 0 and compid = ? and  pdno = ? and cstatus&1=0";

    //新增数量
    private final String UPDATE_SHOPCART_SQL_EXT_ADD = "update {{?" + DSMConst.TD_TRAN_GOODS + "}} set pnum = pnum + ? " +
            " where unqid = ? ";

    //新增数量
    private final String UPDATE_SHOPCART_SQL_EXT = "update {{?" + DSMConst.TD_TRAN_GOODS + "}} set pnum = ? " +
            " where unqid = ? ";


    //新增数量
    private final String UPDATE_SHOPCART_SQL_NUM = "update {{?" + DSMConst.TD_TRAN_GOODS + "}} set pnum =  ? " +
            " where unqid = ? ";




    //删除购物车信息
    private final String DEL_SHOPCART_SQL = "update {{?" + DSMConst.TD_TRAN_GOODS + "}} set cstatus=cstatus|1 "
            + " where cstatus&1=0 and unqid=?";


    //查询购物车列表
    private final String QUERY_SHOPCART_SQL = "select unqid,pdno,compid,cstatus,pnum from {{?" + DSMConst.TD_TRAN_GOODS + "}} "+
            " where cstatus&1=0 and  orderno = 0 and compid = ? order by createdate,createtime desc";


    private static final String QUERY_PROD_BASE =
            " SELECT  spu.prodname ptitle,m.manuname verdor," +
                    "sku.sku pdno, convert(sku.vatp/100,decimal(10,2)) pdprice, DATE_FORMAT(sku.vaildedate,'%Y-%m-%d') vperiod," +
                    "sku.store-sku.freezestore inventory, sku.spec, sku.prodstatus,spu.spu,sku.limits,brandname brand "
                    + " FROM ({{?" + DSMConst.TD_PROD_SPU + "}} spu "
                    + " INNER JOIN {{?" + DSMConst.TD_PROD_SKU   + "}} sku ON spu.spu = sku.spu ) "
                    + " LEFT  JOIN {{?" + DSMConst.TD_PROD_MANU  + "}} m   ON m.cstatus&1 = 0 AND m.manuno  = spu.manuno "
                    + " LEFT  JOIN {{?" + DSMConst.TD_PROD_BRAND + "}} b   ON b.cstatus&1 = 0 AND b.brandno = spu.brandno "
                    + " WHERE 1=1 ";


    private  final String QUERY_ONE_PROD_INV = "SELECT store-freezestore inventory,limits,convert(vatp/100,decimal(10,2)) pdprice from " +
            " {{?" + DSMConst.TD_PROD_SKU   + "}} where sku =? and cstatus & 1 = 0";









    /**
     * @description 购物车保存
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @time  2019/4/2 14:34
     * @version 1.1.1
     **/
    @UserPermission(ignore = true)
    public Result saveShopCart(AppContext appContext) {
        String json = appContext.param.json;
        Result result = new Result();
        ShoppingCartDTO shopVO = GsonUtils.jsonToJavaBean(json, ShoppingCartDTO.class);

        if (shopVO == null){
            return result.fail("操作失败");
        }

        int compid = shopVO.getCompid();

        List<Object[]> queryInvRet = baseDao.queryNative(QUERY_ONE_PROD_INV, new Object[]{shopVO.getPdno()});

        if(queryInvRet == null || queryInvRet.isEmpty()){
            return result.fail("操作失败");
        }

        int inventory = Integer.parseInt(queryInvRet.get(0)[0].toString());
        List<Object[]> queryRet = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(),
                SELECT_SHOPCART_SQL_EXT, new Object[]{compid, shopVO.getPdno()});

        int ret = 0;
        List<Product> productList = new ArrayList<>();
        Product product = new Product();
        product.setSku(shopVO.getPdno());

        double pdprice = Double.parseDouble(queryInvRet.get(0)[2].toString());
        if(queryRet == null || queryRet.isEmpty()){
            product.setNums(shopVO.getPnum());
            product.autoSetCurrentPrice(pdprice,shopVO.getPnum());
            productList.add(product);
            ret = baseDao.updateNativeSharding(compid,TimeUtils.getCurrentYear(),
                    INSERT_SHOPCART_SQL,new Object[]{GenIdUtil.getUnqId(),0,
                            shopVO.getPdno(),getSkuInv(compid,productList,inventory),compid,0});
        }else{
            long unqid = Long.parseLong(queryRet.get(0)[0].toString());
            int pnum = Integer.parseInt(queryRet.get(0)[1].toString());

            product.setNums(shopVO.getPnum()+pnum);
            product.autoSetCurrentPrice(pdprice,shopVO.getPnum()+pnum);
            productList.add(product);

            ret = baseDao.updateNativeSharding(compid,TimeUtils.getCurrentYear(),
                    UPDATE_SHOPCART_SQL_EXT,new Object[]{getSkuInv(compid,productList,inventory),
                            unqid});

        }
        if(ret > 0){
            return result.success("新增成功");
        }
        return result.fail("新增失败");
    }


    public int getSkuInv(int compid,List<Product> products,int inventory){
        List<IDiscount> discountList = getActivityList(compid,products);
        int limitNum = 0;
        int pnum = products.get(0).getNums();
        long actcode = 0;
        if(discountList != null && !discountList.isEmpty()){
            for (IDiscount discount : discountList){
                Activity activity = (Activity)discount;
                if(activity.getLimits(products.get(0).getSku()) < limitNum){
                    limitNum = activity.getLimits(products.get(0).getSku());
                    actcode = activity.getUnqid();
                }
            }
        }
        int maxNum = 0;
        int stock = inventory;
        int limitsub = 0;
        try{
            limitsub = RedisOrderUtil.getActBuyNum(compid,products.get(0).getSku(),actcode);
            stock = RedisStockUtil.getStock(products.get(0).getSku());
        }catch (Exception e){
            e.printStackTrace();
        }


        if(limitNum > stock || limitNum == 0){
            maxNum = stock;
        }else{
            maxNum = limitNum - limitsub;
        }

        if(pnum >= maxNum){
           return maxNum;
        }
        return pnum;
    }



    /**
     * @description 再次购买（购物车批量保存）
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @time  2019/4/2 14:34
     * @version 1.1.1
     **/
    @UserPermission(ignore = true)
    public Result againShopCart(AppContext appContext) {
        String json = appContext.param.json;
        Result result = new Result();

        JsonParser jsonParser = new JsonParser();
        JsonArray jsonArray = jsonParser.parse(json).getAsJsonArray();
        List<ShoppingCartDTO> shoppingCartDTOS = new ArrayList<>();
        Gson gson = new Gson();
        List<Object[]> updateParm = new ArrayList<>();
        List<Object[]> insertParm = new ArrayList<>();
        StringBuilder parmSql = new StringBuilder();
        for (JsonElement shopVO : jsonArray) {
            ShoppingCartDTO shoppingCartDTO = gson.fromJson(shopVO, ShoppingCartDTO.class);
            if (shoppingCartDTO != null) {
                parmSql.append(shoppingCartDTO.getPdno()).append(",");
            }
            shoppingCartDTOS.add(shoppingCartDTO);
        }
        int compid = shoppingCartDTOS.get(0).getCompid();
        String skuStr = parmSql.toString().substring(0, parmSql.toString().length() - 1);

        String querySql = " select unqid,pdno,pnum from {{?" + DSMConst.TD_TRAN_GOODS + "}} "
                + " where orderno = 0 and cstatus&1 = 0 and compid = " + compid;
        querySql = querySql + " and pdno in (" + skuStr + ")";


        String querySkuSql = "SELECT store-freezestore inventory,limits,convert(vatp/100,decimal(10,2)) pdprice,sku pdno from " +
                " {{?" + DSMConst.TD_PROD_SKU   + "}} where cstatus & 1 = 0 ";
        querySkuSql = querySkuSql + " and sku in ("+skuStr+")";


        List<Object[]> querySkuRet = baseDao.queryNative(querySkuSql, new Object[]{});
        if(querySkuRet == null || querySkuRet.isEmpty()){
            return result.fail("添加失败");
        }

        List<Object[]> queryRet = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(),
                querySql, new Object[]{});
            convtShopCartDTO(compid,querySkuRet,shoppingCartDTOS,queryRet);
             boolean flag = true;
            for (ShoppingCartDTO shoppingCartDTO : shoppingCartDTOS) {
                if (queryRet != null && !queryRet.isEmpty()) {
                    for (Object[] objects : queryRet) {
                        if (shoppingCartDTO.getPdno() == Long.parseLong(objects[1].toString())) {
                            updateParm.add(new Object[]{shoppingCartDTO.getPnum(), objects[0]});
                            flag = false;
                            break;
                        }
                     }
                }
                if (flag) {
                    insertParm.add(new Object[]{GenIdUtil.getUnqId(), 0,
                            shoppingCartDTO.getPdno(), shoppingCartDTO.getPnum(), compid, 0});
                    flag = true;
                }
            }

            int[] uret = baseDao.updateBatchNativeSharding(compid, TimeUtils.getCurrentYear(),
                    UPDATE_SHOPCART_SQL_EXT, updateParm, updateParm.size());

            int[] iret = baseDao.updateBatchNativeSharding(compid, TimeUtils.getCurrentYear(),
                    INSERT_SHOPCART_SQL, insertParm, insertParm.size());

            if (!ModelUtil.updateTransEmpty(uret) || !ModelUtil.updateTransEmpty(iret)) {
                return result.success("添加成功");
            }
        return result.fail("添加失败");
    }

    public void convtShopCartDTO(int compid,
                                 List<Object[]> querySkuRet,
                                 List<ShoppingCartDTO> shoppingCartDTOS,
                                 List<Object[]> queryRet){

        List<Product> productList = new ArrayList<>();
        HashMap<Long,Integer> invMap = new HashMap();
        for(ShoppingCartDTO shoppingCartDTO : shoppingCartDTOS){

            Product product = new Product();
            product.setNums(shoppingCartDTO.getPnum());
            for (Object[] objects : querySkuRet){
                if(shoppingCartDTO.getPdno() == Long.parseLong(objects[3].toString())){
                    invMap.put(Long.parseLong(objects[3].toString()),
                            Integer.parseInt(objects[0].toString()));
                    product.setOriginalPrice(Double.parseDouble(objects[2].toString()));
                }
            }

            for (Object[] objects : queryRet){
                if(shoppingCartDTO.getPdno() == Long.parseLong(objects[1].toString())){
                    shoppingCartDTO.setPnum(shoppingCartDTO.getPnum()
                            + Integer.parseInt(objects[2].toString()));

                    product.setNums(shoppingCartDTO.getPnum()
                            + Integer.parseInt(objects[2].toString()));
                }
            }
            product.autoSetCurrentPrice(product.getOriginalPrice(),product.getNums());
            productList.add(product);

            List<IDiscount> discountList = getActivityList(compid, productList);

            int limitNum = 0;
            long actcode = 0;
            if(discountList != null && !discountList.isEmpty()){
                for (IDiscount discount : discountList){
                    Activity activity = (Activity)discount;
                    for (IProduct prdt: activity.getProductList()){
                        if(prdt.getSKU() == shoppingCartDTO.getPdno()){
                            if(activity.getLimits(shoppingCartDTO.getPdno()) < limitNum){
                                limitNum = activity.getLimits(shoppingCartDTO.getPdno());
                                actcode = activity.getUnqid();
                            }
                        }
                    }
                }
            }

            int limitsub = 0;
            try{
                limitsub = RedisOrderUtil.getActBuyNum(compid,shoppingCartDTO.getPdno(),actcode);
            }
            catch (Exception e){
                e.printStackTrace();
            }

            int maxNum = 0;
            if(invMap.containsKey(shoppingCartDTO.getPdno())){
                int stock = invMap.get(shoppingCartDTO.getPdno());
                try{
                    stock = RedisStockUtil.getStock(shoppingCartDTO.getPdno());
                }catch (Exception e){
                    e.printStackTrace();
                }
                if(limitNum > stock || limitNum == 0){
                    maxNum = stock;
                }else{
                    maxNum = limitNum - limitsub;
                }

                if(shoppingCartDTO.getPnum() >= maxNum){
                    shoppingCartDTO.setPnum(maxNum);
                }
            }
        }
    }



    /**
     * @description 清空购物车
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @time  2019/4/2 14:34
     * @version 1.1.1
     **/
    @UserPermission(ignore = true)
    public Result clearShopCart(AppContext appContext) {
        String json = appContext.param.json;
        Result result = new Result();

        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int compid = jsonObject.get("compid").getAsInt();
        String ids = jsonObject.get("ids").getAsString();

        if(StringUtils.isEmpty(ids)){
            return result.fail("操作失败");
        }
        String [] idArry = ids.split(",");
        boolean ret = delShoppingCart(idArry, compid);
        if(ret){
           return result.success("操作成功");
        }
        return result.fail("操作失败");
    }


    /**
     * @description 移入收藏
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @time  2019/4/2 14:34
     * @version 1.1.1
     **/
//    @UserPermission(ignore = true)
//    public Result collectionShopCart(AppContext appContext) {
//        String json = appContext.param.json;
//        Result result = new Result();
//
//        JsonParser jsonParser = new JsonParser();
//        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
//        int compid = jsonObject.get("compid").getAsInt();
//        String ids = jsonObject.get("ids").getAsString();
//
//        if(StringUtils.isEmpty(ids)){
//            return result.fail("操作失败");
//        }
//        String [] idArry = ids.split(",");
//        boolean ret = delShoppingCart(idArry, compid);
//        if(ret){
//            return result.success("操作成功");
//        }
//        return result.fail("操作失败");
//    }


    /**
     * 查询未选中购物车列表
     * @param appContext
     * @return
     */
    @UserPermission(ignore = true)
    public Result queryUnCheckShopCartList(AppContext appContext){
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        Result result = new Result();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        if(!jsonObject.has("compid")){
            return  result.success(null);
        }

        int compid = jsonObject.get("compid").getAsInt();

        if(compid <= 0){
            return  result.success(null);
        }

        List<Object[]> queryResult = baseDao.queryNativeSharding(compid,TimeUtils.getCurrentYear(),QUERY_SHOPCART_SQL,compid);
        ShoppingCartDTO[] shoppingCartVOS = new ShoppingCartDTO[queryResult.size()];
        if (queryResult == null || queryResult.isEmpty()) {
            return  result.success(shoppingCartVOS);
        }
        baseDao.convToEntity(queryResult, shoppingCartVOS, ShoppingCartDTO.class,
                new String[]{"unqid","pdno","compid","cstatus","pnum"});


        List<ShoppingCartVO> shopCart = getShopCart(Arrays.asList(shoppingCartVOS));


        //TODO 获取活动匹配
        convResult(shopCart,compid);
        return result.success(shopCart);
    }

    /**
     * 查询选中的购物车列表
     * @param appContext
     * @return
     */
    @UserPermission(ignore = true)
    public Result queryCheckShopCartList(AppContext appContext){
        String json = appContext.param.json;
        Result result = new Result();
        JsonParser jsonParser = new JsonParser();
        JsonArray jsonArray = jsonParser.parse(json).getAsJsonArray();
        List<ShoppingCartDTO> shoppingCartDTOS = new ArrayList<>();
        Gson gson = new Gson();
        List<Object[]> updateParm = new ArrayList<>();
        for (JsonElement shopVO : jsonArray){
            ShoppingCartDTO shoppingCartDTO = gson.fromJson(shopVO, ShoppingCartDTO.class);
            shoppingCartDTOS.add(shoppingCartDTO);
            if(shoppingCartDTO.getChecked() == 1){
                updateParm.add(new Object[]{shoppingCartDTO.getPnum(),shoppingCartDTO.getUnqid()});
            }
        }
        //更新购物车数量
        baseDao.updateBatchNativeSharding(shoppingCartDTOS.get(0).getCompid(),TimeUtils.getCurrentYear(),
                UPDATE_SHOPCART_SQL_NUM, updateParm, updateParm.size());
        List<ShoppingCartVO> shopCart = getShopCart(shoppingCartDTOS);
        //TODO 获取活动匹配
        convResult(shopCart,shoppingCartDTOS.get(0).getCompid());
        return result.success(shopCart);
    }


    public List<ShoppingCartVO> getCartSkus(String ids){
        StringBuilder sql = new StringBuilder(QUERY_PROD_BASE);
        if(StringUtils.isEmpty(ids)){
            return null;
        }
        sql.append(" AND sku.sku IN (");
        sql.append(ids);
        sql.append(") ");
        List<Object[]> queryResult = baseDao.queryNative(sql.toString());

        if (queryResult.isEmpty()) {
           return null;
        }
        ShoppingCartVO[] returnResults = new ShoppingCartVO[queryResult.size()];
        baseDao.convToEntity(queryResult, returnResults, ShoppingCartVO.class,
                new String[]{"ptitle","verdor","pdno","pdprice","vperiod","inventory",
                        "spec","pstatus","spu","limitnum","brand"});
        return Arrays.asList(returnResults);
    }

    public  List<ShoppingCartVO> getShopCart(List<ShoppingCartDTO> shoppingCartDTOS){
        StringBuilder ids = new StringBuilder();
        for (int i = 0; i < shoppingCartDTOS.size(); i++){
            ids.append(shoppingCartDTOS.get(i).getPdno());
            if (i < shoppingCartDTOS.size() - 1) {
                ids.append(", ");
            }
        }

        List<ShoppingCartVO> shoppingCartList = getCartSkus(ids.toString());
        if(shoppingCartList == null){
            return null;
        }
        for(ShoppingCartVO shoppingCartVO : shoppingCartList){
            if(shoppingCartVO.getPstatus() == 0){
                shoppingCartVO.setStatus(2);
            }

            for(int i = 0; i < shoppingCartDTOS.size(); i++){
                if(shoppingCartVO.getPdno() == shoppingCartDTOS.get(i).getPdno()){
                    shoppingCartVO.setNum(shoppingCartDTOS.get(i).getPnum());
                    shoppingCartVO.setChecked(shoppingCartDTOS.get(i).getChecked());
                    shoppingCartVO.setUnqid(shoppingCartDTOS.get(i).getUnqid());
                    shoppingCartVO.setCounpon(shoppingCartDTOS.get(i).getConpno());
                    shoppingCartVO.setAreano(shoppingCartDTOS.get(i).getAreano());
                    break;
                }
            }
        }
        return shoppingCartList;
    }



    public void convResult(List<ShoppingCartVO> shoppingCartList,int compid){

        if(shoppingCartList == null){
            return;
        }
        List<Product> productList = new ArrayList<>();
        List<Product> ckProduct = new ArrayList<>();
        BigDecimal result = BigDecimal.ZERO;
        for (ShoppingCartVO shoppingCartVO : shoppingCartList){
            Product product = new Product();
            product.setSku(shoppingCartVO.getPdno());
            product.autoSetCurrentPrice(shoppingCartVO.getPdprice(),shoppingCartVO.getNum());
            productList.add(product);
            if(shoppingCartVO.getChecked() == 1){
                ckProduct.add(product);
                result = result.add(BigDecimal.valueOf(product.getCurrentPrice()));
            }
        }

        List<IDiscount> discountList = getActivityList(compid,productList);

        //获取活动

        for (ShoppingCartVO shoppingCartVO : shoppingCartList){
           List<DiscountRule> ruleList = new ArrayList<>();
           List<Long> actCodeList = new ArrayList<>();
            int minLimit = 0;
            long actcode = 0;
            for(IDiscount discount : discountList){
                Activity activity = (Activity)discount;
                int brule = (int)discount.getBRule();
                List<IProduct> pList =  discount.getProductList();
                for (IProduct product: pList){
                    if(product.getSKU() == shoppingCartVO.getPdno()){
                        DiscountRule discountRule = new DiscountRule();
                        discountRule.setRulecode(brule);
                        discountRule.setRulename(DiscountRuleStore.getRuleByName(brule));
                        actCodeList.add(activity.getUnqid());
                        if(activity.getLimits(product.getSKU()) < minLimit){
                            minLimit = activity.getLimits(product.getSKU());
                            actcode = activity.getUnqid();
                        }
                        //判断秒杀
                        if(brule == 1113){
                            shoppingCartVO.setStatus(1);
                        }
                        ruleList.add(discountRule);
                    }
                }
            }
            shoppingCartVO.setLimitnum(minLimit);
            int stock = shoppingCartVO.getInventory();
            int limitsub = 0;
            try{
                stock = RedisStockUtil.getStock(shoppingCartVO.getPdno());
                limitsub = RedisOrderUtil.getActBuyNum(compid,shoppingCartVO.getPdno(),actcode);
            }catch (Exception e){
                e.printStackTrace();
            }
            if(stock == 0){
                shoppingCartVO.setStatus(3);
            }
            shoppingCartVO.setLimitsub(limitsub);
            shoppingCartVO.setInventory(stock);
            shoppingCartVO.setActcode(actCodeList);
            shoppingCartVO.setRule(ruleList);
        }
        DiscountResult discountResult
                = CalculateUtil.calculate(compid,ckProduct,shoppingCartList.get(0).getConpno());
        for (ShoppingCartVO shoppingCartVO : shoppingCartList){
            for(Product product: ckProduct){
                if(shoppingCartVO.getPdno() == product.getSKU()
                        && shoppingCartVO.getChecked() == 1){
                    shoppingCartVO.setSubtotal(product.getCurrentPrice());
                    shoppingCartVO.setDiscount(product.getDiscounted());
                    shoppingCartVO.setAmt(discountResult.getTotalDiscount());
                    shoppingCartVO.setAcamt(discountResult.getTotalCurrentPrice());
                    shoppingCartVO.setCounpon(discountResult.getCouponValue());
                    shoppingCartVO.setFreepost(discountResult.isFreeShipping());
                    shoppingCartVO.setOflag(discountResult.isExCoupon());
                    shoppingCartVO.setTotalamt(result.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
                    shoppingCartVO.setSubtotal(MathUtil.exactMul(product.getOriginalPrice(),product.getNums()).
                            setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue());
                    if(shoppingCartVO.getAreano() > 0 && !discountResult.isFreeShipping()){
                        shoppingCartVO.setFreight(AreaFeeUtil.getFee(shoppingCartVO.getAreano()));
                    }

                }
            }
        }
    }


    /**
     * 清空购物车
     * @param idList
     * @param compid
     */
    private boolean delShoppingCart(String[] idList,int compid) {

        List<Object[]> shopParm = new ArrayList<>();
        for (String unqid : idList) {
            if(StringUtils.isInteger(unqid)){
                shopParm.add(new Object[]{Long.parseLong(unqid)});
            }

        }
        int[] result = baseDao.updateBatchNativeSharding(compid,
                TimeUtils.getCurrentYear(), DEL_SHOPCART_SQL,shopParm,shopParm.size());
        return !ModelUtil.updateTransEmpty(result);
    }


    public  List<IDiscount> getActivityList(int compid,List<Product> products){
        List<IDiscount> activityList =
                new ActivityFilterService(
                        new ActivitiesFilter[] {
                                new CycleFilter(),
                                new QualFilter(compid),
                                new PriorityFilter(),
                                new StoreFilter(),
                        }).getCurrentActivities(products);
        return activityList;
    }


    /**
     * 查询结算购物车列表
     * @param appContext
     * @return
     */
    @UserPermission(ignore = true)
    public Result querySettShopCartList(AppContext appContext){
        String json = appContext.param.json;
        Result result = new Result();
        JsonParser jsonParser = new JsonParser();
        JsonArray jsonArray = jsonParser.parse(json).getAsJsonArray();
        List<ShoppingCartDTO> shoppingCartDTOS = new ArrayList<>();
        Gson gson = new Gson();
        for (JsonElement shopVO : jsonArray){
            ShoppingCartDTO shoppingCartDTO = gson.fromJson(shopVO, ShoppingCartDTO.class);
            shoppingCartDTOS.add(shoppingCartDTO);
        }
        //更新购物车数量
       // baseDao.updateBatchNativeSharding(shoppingCartDTOS.get(0).getCompid(),TimeUtils.getCurrentYear(),UPDATE_SHOPCART_SQL_NUM, updateParm, updateParm.size());
        List<ShoppingCartVO> shopCart = getShopCart(shoppingCartDTOS);
        //TODO 获取活动匹配

        convResult(shopCart,shoppingCartDTOS.get(0).getCompid());
        return result.success(shopCart);
    }

    /**
     * 获取购物车优惠提示
     * @param appContext
     * @return
     */
    @UserPermission(ignore = true)
    public Result getOfferTip(AppContext appContext){
        String json = appContext.param.json;
        Result result = new Result();
        JsonParser jsonParser = new JsonParser();
        JsonArray jsonArray = jsonParser.parse(json).getAsJsonArray();
        List<ShoppingCartDTO> shoppingCartDTOS = new ArrayList<>();
        Gson gson = new Gson();
        List<OfferTipsVO> offerTipsVOS = new ArrayList<>();
        for (JsonElement shopVO : jsonArray){
            ShoppingCartDTO shoppingCartDTO = gson.fromJson(shopVO, ShoppingCartDTO.class);
            shoppingCartDTOS.add(shoppingCartDTO);
        }
        
        List<ShoppingCartVO> shopCartList = getShopCart(shoppingCartDTOS);
        List<Product> productList = new ArrayList<>();
        for (ShoppingCartVO shoppingCartVO : shopCartList){
            Product product = new Product();
            product.setSku(shoppingCartVO.getPdno());
            product.autoSetCurrentPrice(shoppingCartVO.getPdprice(),shoppingCartVO.getNum());
            productList.add(product);
        }

        DiscountResult discountResult
                = CalculateUtil.calculate(shoppingCartDTOS.get(0).getCompid(),
                productList,0);
        List<IDiscount> activityList = discountResult.getActivityList();

        for (IDiscount discount : activityList){
            Activity activity = (Activity)discount;
            Ladoff currLadoff = activity.getCurrLadoff();
            Ladoff nextLadoff = activity.getNextLadoff();
            int brule = (int)discount.getBRule();
            if(brule == 1113 || brule == 1133){
                continue;
            }

            if(currLadoff == null && nextLadoff == null){
                continue;
            }

            OfferTipsVO offerTipsVO = new OfferTipsVO();
            offerTipsVO.setOffername(DiscountRuleStore.getRuleByName(brule));
            offerTipsVO.setGapamt(activity.getNextGapAmt());
            offerTipsVO.setGapnum(activity.getNextGapNum());
            if(currLadoff != null){
                offerTipsVO.setLadamt(currLadoff.getLadamt());
                offerTipsVO.setLadnum(currLadoff.getLadnum());
                offerTipsVO.setOffer(currLadoff.getOffer());
                offerTipsVO.setOffercode(currLadoff.getOffercode());
                offerTipsVO.setUnqid(currLadoff.getUnqid());
            }

            if(nextLadoff != null){
                offerTipsVO.setNladamt(nextLadoff.getLadamt());
                offerTipsVO.setNladnum(nextLadoff.getLadnum());
                offerTipsVO.setNoffer(nextLadoff.getOffer());
                offerTipsVO.setOffercode(nextLadoff.getOffercode());
                offerTipsVO.setUnqid(nextLadoff.getUnqid());
            }

            offerTipsVOS.add(offerTipsVO);
        }
        return result.success(offerTipsVOS);
    }

    public static void main(String[] args) {

    }

}
