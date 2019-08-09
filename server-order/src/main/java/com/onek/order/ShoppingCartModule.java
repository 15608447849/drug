package com.onek.order;

import Ice.Application;
import Ice.Logger;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.*;
import com.onek.annotation.UserPermission;
import com.onek.calculate.ActivityFilterService;
import com.onek.calculate.entity.*;
import com.onek.calculate.entity.Package;
import com.onek.calculate.filter.*;
import com.onek.context.AppContext;
import com.onek.entity.*;
import com.onek.entitys.Result;
import com.onek.server.infimp.IceDebug;
import com.onek.util.CalculateUtil;
import com.onek.util.GenIdUtil;
import com.onek.util.IceRemoteUtil;
import com.onek.util.area.AreaFeeUtil;
import com.onek.util.discount.DiscountRuleStore;
import com.onek.util.order.RedisOrderUtil;
import com.onek.util.stock.RedisStockUtil;
import constant.DSMConst;
import dao.BaseDAO;
import org.hyrdpf.util.LogUtil;
import util.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * @author liuhui
 * @version V1.0
 * @ClassName ShoppingCartModule
 * @服务名 orderServer
 * @Description 购物车
 * @date 2019-04-15 20:54
 */
public class ShoppingCartModule {

    private static BaseDAO baseDao = BaseDAO.getBaseDAO();

    //新增购物车
    private final String INSERT_SHOPCART_SQL = "insert into {{?" + DSMConst.TD_TRAN_GOODS + "}} "
            + "(unqid,orderno,pdno,pnum,createdate,createtime,compid,cstatus) "
            + "values (?,?,?,?,CURRENT_DATE,CURRENT_TIME,?,?)";


    //新增购物车
    private static final String SELECT_SHOPCART_SQL_EXT = "select unqid,pnum from {{?" + DSMConst.TD_TRAN_GOODS + "}} "
            + " where orderno = 0 and compid = ? and  pdno = ? and cstatus&1=0";

    //查询订单再次购买
    private static final String SELECT_SHOPCART_ORDER = " select unqid,pdno,compid,pnum,pkgno from {{?" + DSMConst.TD_TRAN_GOODS + "}} "
            + " where orderno = ? and cstatus & 1 = 0";

    //新增数量
    private final String UPDATE_SHOPCART_SQL_EXT_ADD = "update {{?" + DSMConst.TD_TRAN_GOODS + "}} set pnum = pnum + ? " +
            " where unqid = ? ";

    //新增数量
    private final String UPDATE_SHOPCART_SQL_EXT = "update {{?" + DSMConst.TD_TRAN_GOODS + "}} set pnum = ? " +
            " where unqid = ?  and orderno=0 ";


    //新增数量
    private final String UPDATE_SHOPCART_SQL_NUM = "update {{?" + DSMConst.TD_TRAN_GOODS + "}} set pnum =  ? " +
            " where unqid = ? and orderno=0 ";




    //删除购物车信息
    private final String DEL_SHOPCART_SQL = "update {{?" + DSMConst.TD_TRAN_GOODS + "}} set cstatus=cstatus|1 "
            + " where cstatus&1=0 and unqid=? and orderno=0 ";


    //删除购物车信息
    private final String REAL_DEL_SHOPCART_SQL = "delete  from {{?" + DSMConst.TD_TRAN_GOODS + "}}  "
            + " where cstatus&1=0 and unqid=? and orderno=0 ";


    //查询购物车列表
    private final String QUERY_SHOPCART_SQL = "select unqid,pdno,compid,cstatus,pnum from {{?" + DSMConst.TD_TRAN_GOODS + "}} "+
            " where cstatus&1=0 and  orderno = 0 and compid = ? order by createdate,createtime desc";

    //远程调用
    private static final String QUERY_PROD_BASE =
            " SELECT ifnull(spu.prodname,'') ptitle,ifnull(m.manuname,'') verdor," +
                    "sku.sku pdno, convert(sku.vatp/100,decimal(10,2)) pdprice, DATE_FORMAT(sku.vaildsdate,'%Y-%m-%d') vperiod," +
                    "sku.store-sku.freezestore inventory,ifnull(sku.spec,'') spec, sku.prodstatus,spu.spu,sku.limits,ifnull(brandname,'') brand,medpacknum,unit," +
                    "convert(mp/100,decimal(10,2)) mp, IFNULL(spu.busscope, 0), IFNULL(sku.consell, 0) "
                    + " FROM ({{?" + DSMConst.TD_PROD_SPU + "}} spu "
                    + " INNER JOIN {{?" + DSMConst.TD_PROD_SKU   + "}} sku ON spu.spu = sku.spu ) "
                    + " LEFT  JOIN {{?" + DSMConst.TD_PROD_MANU  + "}} m   ON m.cstatus&1 = 0 AND m.manuno  = spu.manuno "
                    + " LEFT  JOIN {{?" + DSMConst.TD_PROD_BRAND + "}} b   ON b.cstatus&1 = 0 AND b.brandno = spu.brandno "
                    + " WHERE 1=1 ";


    //远程调用
    private static final String QUERY_ONE_PROD_INV = "SELECT store-freezestore inventory,limits,convert(vatp/100,decimal(10,2)) pdprice from " +
            " {{?" + DSMConst.TD_PROD_SKU   + "}} where sku =? and cstatus & 1 = 0";

    private static final String QUERY_ONE_PROD_INV_BUSSCOPE =
            "SELECT store-freezestore inventory, limits, "
            + " convert(vatp/100,decimal(10,2)) pdprice, IFNULL(spu.busscope, 0), IFNULL(sku.consell, 0) "
            + " from {{?" + DSMConst.TD_PROD_SKU + "}} sku, {{?" + DSMConst.TD_PROD_SPU + "}} spu "
            + " where spu.cstatus & 1 = 0 AND sku.spu = spu.spu "
            + " AND sku.sku =? and sku.cstatus & 1 = 0 ";


    //查询购物车商品数量
    private static final String SELECT_SKUNUM_SQL = "select pnum from {{?" + DSMConst.TD_TRAN_GOODS + "}} "
            + " where orderno = 0 and compid = ? and  pdno = ? and cstatus&1=0";




    /**
     * @接口摘要 购物车商品新增
     * @业务场景 商品加入购物车
     * @传参类型 JSON
     * @传参列表
     *  pdno - 商品码
     *  pnum - 商品数量
     *  compid - 买家企业码
     * @返回列表
     *  code - 200 成功/ -1 失败
     *  message - 成功/失败
     **/
    public Result saveShopCart(AppContext appContext) {
        String json = appContext.param.json;
        Result result = new Result();
        ShoppingCartDTO shopVO = GsonUtils.jsonToJavaBean(json, ShoppingCartDTO.class);

        if (shopVO == null || shopVO.getPdno() <= 0){
            LogUtil.getDefaultLogger().debug("参数有误");
            return result.fail("参数有误");
        }

        int compid = appContext.getUserSession().compId;

        //远程调用
        List<Object[]> queryInvRet = IceRemoteUtil.queryNative(QUERY_ONE_PROD_INV_BUSSCOPE, shopVO.getPdno());
        if(queryInvRet == null || queryInvRet.isEmpty()){
            return result.fail("查询商品失败");
        }

        int controlCode = appContext.getUserSession().comp.controlCode;
        int resultConsell = Integer.parseInt(queryInvRet.get(0)[4].toString());

        if ((resultConsell & controlCode) != resultConsell) {
            return result.fail("此为控销商品，您无权加入购物车！");
        }

        int prodScope = Integer.parseInt(queryInvRet.get(0)[3].toString());

        if (prodScope > 0) {
            String sql = " SELECT IFNULL(busscope, 0) "
                    + " FROM {{?" + DSMConst.TB_COMP_BUS_SCOPE + "}} "
                    + " WHERE cstatus&1 = 0 AND compid = ? ";

            List<Object[]> queryResult = IceRemoteUtil.queryNative(sql, compid);

            boolean outOfScope = true;
            for (int i = 0; i < queryResult.size(); i++) {
                if (prodScope == Integer.parseInt(queryResult.get(i)[0].toString())) {
                    outOfScope = false;
                    break;
                }
            }

            if (outOfScope) {
                return new Result().fail("非经营范围内的商品不可加入购物车！");
            }
        }


        int inventory = Integer.parseInt(queryInvRet.get(0)[0].toString());
        List<Object[]> queryRet = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(),
                SELECT_SHOPCART_SQL_EXT, compid, shopVO.getPdno());

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
                    INSERT_SHOPCART_SQL, GenIdUtil.getUnqId(),0,
                    shopVO.getPdno(),getSkuInv(compid,productList,inventory),compid,0);
        }else{
            long unqid = Long.parseLong(queryRet.get(0)[0].toString());
            int pnum = Integer.parseInt(queryRet.get(0)[1].toString());
            int current = shopVO.getPnum()+pnum;
            appContext.logger.print("存入数量: " + current);
            product.setNums( current < 0 ? 0 : current);
            product.autoSetCurrentPrice(pdprice,shopVO.getPnum()+pnum);
            productList.add(product);
            ret = baseDao.updateNativeSharding(compid,TimeUtils.getCurrentYear(),
                    UPDATE_SHOPCART_SQL_EXT, getSkuInv(compid,productList,inventory),
                    unqid);

        }
        if(ret > 0){
            return result.success("加入采购单成功","新增成功");
        }
        return result.fail("无法加入采购单");
    }


    public int getSkuInv(int compid,List<Product> products,int inventory){
        List<IDiscount> discountList = getActivityList(compid,products);
        int pnum = products.get(0).getNums();
        int subStock = Integer.MAX_VALUE;
        int actStock = Integer.MAX_VALUE;
        if(discountList != null && !discountList.isEmpty()){
            for (IDiscount discount : discountList){
                Activity activity = (Activity)discount;

                actStock = Math.min(
                    RedisStockUtil
                            .getActStockBySkuAndActno(products.get(0).getSku(),
                                    discount.getDiscountNo()),
                        actStock);

                if(activity.getLimits(products.get(0).getSku()) == 0){
                    continue;
                }

                subStock = Math.min
                        (activity.getLimits(products.get(0).getSku())
                                - RedisOrderUtil.getActBuyNum(compid, products.get(0).getSku(),
                                activity.getUnqid()),subStock);
            }
        }
        int stock = inventory;
        try{
            stock = RedisStockUtil.getStock(products.get(0).getSku());
        }catch (Exception e){
            e.printStackTrace();
        }

        int minStock = stock;

        if(actStock < minStock){
            minStock = actStock;
        }

        if(subStock < minStock){
            minStock = subStock;
        }

        if(minStock < pnum){
           return minStock;
        }

        return pnum;
    }


    /**
     * 获取SKU可以购买的数量
     * @param compid 企业码
     * @param sku SKU码
     * @param skuNum SKU购买数量
     * @param type 0 购物车购买 1非购物车购买
     * @return 可以购买的数量
     */
    public static int getCanbuySkuNum(int compid,long sku,int skuNum,int type){
        Logger logger = Application.communicator().getLogger();

        List<Object[]> queryInvRet = IceRemoteUtil.queryNative(QUERY_ONE_PROD_INV, sku);
        logger.print("IceRemoteUtil.queryNative = " + queryInvRet.size()  );
        for (Object[] arr : queryInvRet) logger.print("\t" + Arrays.toString(arr));

        if(queryInvRet.isEmpty()){
            return 0;
        }
        List<Product> productList = new ArrayList<>();
        Product product = new Product();
        product.setSku(sku);
        double pdprice = Double.parseDouble(queryInvRet.get(0)[2].toString());
        int inventory = Integer.parseInt(queryInvRet.get(0)[0].toString());
        logger.print("商品sku = " + sku + " 当前价格: " + pdprice +" , 库存 - "+ inventory);
        List<Object[]> queryRet = null;
        if(type == 0){
            queryRet = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(),
                     SELECT_SHOPCART_SQL_EXT, compid, sku);
        }
        if(queryRet == null || queryRet.isEmpty()){ //不存在购物车数量记录,新购买
            product.setNums(skuNum);
            product.autoSetCurrentPrice(pdprice,skuNum);
            productList.add(product);
        }else{ //存在购物车数量记录
            int pnum = Integer.parseInt(queryRet.get(0)[1].toString());
            product.setNums(skuNum+pnum);
            product.autoSetCurrentPrice(pdprice,skuNum+pnum);
            productList.add(product);
        }

        //活动列表
        List<IDiscount> discountList = getActivityList(compid,productList);
        int pnum = productList.get(0).getNums();
        int subStock = Integer.MAX_VALUE;
        int actStock = Integer.MAX_VALUE;
        if(discountList != null && !discountList.isEmpty()){
            logger.print("活动列表数量; " + discountList.size());
            for (IDiscount discount : discountList){
                Activity activity = (Activity)discount;

                actStock = Math.min(
                        RedisStockUtil.getActStockBySkuAndActno(productList.get(0).getSku(), discount.getDiscountNo()),
                        actStock);

                if(activity.getLimits(productList.get(0).getSku()) == 0){
                    continue;
                }

                subStock = Math.min
                        (activity.getLimits(productList.get(0).getSku())
                                - RedisOrderUtil.getActBuyNum(compid, productList.get(0).getSku(),
                                activity.getUnqid()),subStock);
            }
            logger.print(" actStock = "+ actStock + " , subStock = "+ subStock);
        }
        int stock = inventory;
        try{
            stock = RedisStockUtil.getStock(productList.get(0).getSku());
        }catch (Exception e){
            e.printStackTrace();
        }

        int minStock = stock;

        logger.print("minStock = "+ minStock + " , actStock = "+ actStock + " , subStock = "+ subStock);

        if(actStock < minStock){
            minStock = actStock;
        }

        if(subStock < minStock){
            minStock = subStock;
        }
        if(minStock < pnum){
            return minStock;
        }
        return pnum;
    }




    /**
     * @接口摘要 再次购买
     * @业务场景 订单交易成功后再次购买
     * @传参类型 JSONARRAY
     * @传参列表
     *  pdno - 商品码
     *  pnum - 商品数量
     *  compid - 买家企业码
     * @返回列表
     *  code - 200 成功/ -1 失败
     *  message - 成功/失败
     **/
    @UserPermission(ignore = true)
    public Result againShopCart(AppContext appContext) {
        String json = appContext.param.json;
      //  Result result = new Result();

       // String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        Result result = new Result();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        if(!jsonObject.has("compid")
                || !jsonObject.has("orderno")){
            return result.fail("添加失败");
        }

        int compid = jsonObject.get("compid").getAsInt();
        String orderno = jsonObject.get("orderno").getAsString();

        List<Object[]> orderRet = baseDao.queryNativeSharding(compid,
                TimeUtils.getCurrentYear(), SELECT_SHOPCART_ORDER,
                new Object[]{orderno});

        if(orderRet == null || orderRet.isEmpty()){
            return new Result().fail("商品为空！");
        }

        ShoppingCartDTO [] shoppingCartDTOArray = new ShoppingCartDTO[orderRet.size()];
        baseDao.convToEntity(orderRet,shoppingCartDTOArray,ShoppingCartDTO.class,
                new String[]{"unqid","pdno","compid","pnum","pkgno"});


        //List<ShoppingCartDTO> shoppingCartDTOS = new ArrayList<>();
        List<Object[]> updateParm = new ArrayList<>();
        List<Object[]> insertParm = new ArrayList<>();
        StringBuilder parmSql = new StringBuilder();
        for (ShoppingCartDTO shoppingCartDTO : shoppingCartDTOArray) {
            if (shoppingCartDTO != null) {
                parmSql.append(shoppingCartDTO.getPdno()).append(",");
            }
        }
        String skuStr = parmSql.toString().substring(0, parmSql.toString().length() - 1);

        String querySql = " select unqid,pdno,pnum from {{?" + DSMConst.TD_TRAN_GOODS + "}} "
                + " where orderno = 0 and cstatus&1 = 0 and compid = " + compid;
        querySql = querySql + " and pdno in (" + skuStr + ")";

        //远程调用
        String querySkuSql = "SELECT store-freezestore inventory,limits,convert(vatp/100,decimal(10,2)) pdprice,sku pdno from " +
                " {{?" + DSMConst.TD_PROD_SKU   + "}} where cstatus & 1 = 0 ";
        querySkuSql = querySkuSql + " and sku in ("+skuStr+")";

        //远程调用
        List<Object[]> querySkuRet = IceRemoteUtil.queryNative(querySkuSql);
        if(querySkuRet == null || querySkuRet.isEmpty()){
            return result.fail("添加失败");
        }

        List<Object[]> queryRet = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(),
                querySql);
            convtShopCartDTO(compid,querySkuRet,shoppingCartDTOArray,queryRet);
            boolean flag = true;
            for (ShoppingCartDTO shoppingCartDTO : shoppingCartDTOArray) {
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
                                 ShoppingCartDTO[] shoppingCartDTOS,
                                 List<Object[]> queryRet){

        List<Product> productList = new ArrayList<>();
        HashMap<Long,List<Integer>> invMap = new HashMap();
        for(ShoppingCartDTO shoppingCartDTO : shoppingCartDTOS){
            Product product = new Product();
            product.setNums(shoppingCartDTO.getPnum());
            List<Integer> stockList = new ArrayList<>();
            invMap.put(shoppingCartDTO.getPdno(),stockList);
            for (Object[] objects : querySkuRet){
                if(shoppingCartDTO.getPdno() == Long.parseLong(objects[3].toString())){

                    int stock = Integer.parseInt(objects[0].toString());
                    try{
                        stock = RedisStockUtil.getStock(shoppingCartDTO.getPdno());
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    invMap.get(shoppingCartDTO.getPdno()).
                            add(stock);
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
            product.setSku(shoppingCartDTO.getPdno());
            productList.add(product);

            List<IDiscount> discountList = getActivityList(compid, productList);

            int subStock = Integer.MAX_VALUE;
            int actStock = Integer.MAX_VALUE;

            if(discountList != null && !discountList.isEmpty()){
                for (IDiscount discount : discountList){
                    Activity activity = (Activity)discount;
                    for (IProduct prdt: activity.getProductList()){
                        if(prdt.getSKU() == shoppingCartDTO.getPdno()){


                            actStock = Math.min(
                                    RedisStockUtil
                                            .getActStockBySkuAndActno(shoppingCartDTO.getPdno(),
                                                    discount.getDiscountNo()),
                                    actStock);

                            if(activity.getLimits(shoppingCartDTO.getPdno()) == 0){
                                break;
                            }

                            subStock = Math.min
                                    (activity.getLimits(shoppingCartDTO.getPdno())
                                            - RedisOrderUtil.getActBuyNum(compid, shoppingCartDTO.getPdno(),
                                            activity.getUnqid()),subStock);

                        }
                    }
                }
            }

            invMap.get(shoppingCartDTO.getPdno()).
                    add(subStock);

            invMap.get(shoppingCartDTO.getPdno()).
                    add(actStock);
        }

        for(ShoppingCartDTO shoppingCartDTO : shoppingCartDTOS){
            if(invMap.containsKey(shoppingCartDTO.getPdno())){
                List<Integer> stockList = invMap.get(shoppingCartDTO.getPdno());
                if(!stockList.isEmpty()){
                    int minStock = Collections.min(stockList);
                    if(shoppingCartDTO.getPnum() > minStock){
                        shoppingCartDTO.setPnum(minStock);
                    }
                }
            }
        }
    }





    /**
     * @接口摘要 清空购物车
     * @业务场景 订单交易成功后再次购买
     * @传参类型 JSONARRAY
     * @传参列表
     *  pdno - 商品码
     *  pnum - 商品数量
     *  compid - 买家企业码
     * @返回列表
     *  code - 200 成功/ -1 失败
     *  message - 成功/失败
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
     * @接口摘要 查询购物车列表（未选中）
     * @业务场景 进入购物车页面
     * @传参类型 JSON
     * @传参列表
     *  compid - 买家企业码
     * @返回列表
     *  pdno - 商品码
     *  pnum - 商品数量
     *  compid - 买家企业码
     *  ptitle - 商品标题
     *  spec - 商品规格
     *  verdor - 厂商
     *  vperiod - 有效期
     *  brand - 品牌
     *  limitnum - 限购量
     *  inventory - 库存量
     *  medpacknum - 中包装
     *
     **/
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

        List<ShoppingCartVO> shopCart = getShopCart(new ArrayList<>(Arrays.asList(shoppingCartVOS)));


        //TODO 获取活动匹配
        convResult(shopCart,compid);
        return result.success(shopCart);
    }

    /**
     * @接口摘要 查询购物车列表（选中）
     * @业务场景 购物车页面，购物车商品数量增加操作
     * @传参类型 JSONARRAY
     * @传参列表
     *  pdno - 商品码
     *  pnum - 商品数量
     *  compid - 买家企业码
     *  checked - 是否选中：0 未选中，1 选中
     *  pdprice - 商品单价
     * @返回列表
     *  pdno - 商品码
     *  pnum - 商品数量
     *  compid - 买家企业码
     *  ptitle - 商品标题
     *  spec - 商品规格
     *  verdor - 厂商
     *  vperiod - 有效期
     *  brand - 品牌
     *  limitnum - 限购量
     *  inventory - 库存量
     *  medpacknum - 中包装
     **/
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


    private List<ShoppingCartVO> getCartSkus(String ids){
        if(StringUtils.isEmpty(ids)){
            return null;
        }
        //远程调用
        StringBuilder sql = new StringBuilder(QUERY_PROD_BASE);
        sql.append(" AND sku.sku IN (");
        sql.append(ids);
        sql.append(") ");
        //远程调用
        List<Object[]> queryResult = IceRemoteUtil.queryNative(sql.toString());

        if (queryResult==null || queryResult.isEmpty()) {
           return null;
        }
        for (Object[] objs : queryResult){
            Application.communicator().getLogger().print("Object[]:"+ Arrays.toString(objs));
        }
        ShoppingCartVO[] returnResults = new ShoppingCartVO[queryResult.size()];
        baseDao.convToEntity(queryResult, returnResults, ShoppingCartVO.class,
                new String[]{"ptitle","verdor","pdno","pdprice","vperiod","inventory",
                        "spec","pstatus","spu","limitnum","brand","medpacknum","unit","mp", "busscope", "consell"});
        return new ArrayList<>(Arrays.asList(returnResults));
    }

    private List<ShoppingCartVO> getShopCart(List<ShoppingCartDTO> shoppingCartDTOS){
        StringBuilder ids = new StringBuilder();
        for (int i = 0; i < shoppingCartDTOS.size(); i++){
            ids.append(shoppingCartDTOS.get(i).getPdno());
            if (i < shoppingCartDTOS.size() - 1){
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

            for (ShoppingCartDTO shoppingCartDTO : shoppingCartDTOS) {
                if (shoppingCartVO.getPdno() == shoppingCartDTO.getPdno()) {
                    shoppingCartVO.setNum(shoppingCartDTO.getPnum());
                    shoppingCartVO.setChecked(shoppingCartDTO.getChecked());
                    shoppingCartVO.setUnqid(shoppingCartDTO.getUnqid());
                    shoppingCartVO.setConpno(shoppingCartDTO.getConpno() + "");
                    shoppingCartVO.setAreano(shoppingCartDTO.getAreano());
                    break;
                }
            }
        }


        return shoppingCartList;
    }

    private List<Gift> getGifts(List<ShoppingCartVO> shoppingCartList, int compid){
        if(shoppingCartList == null){
            return Collections.emptyList();
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
           List<String> actCodeList = new ArrayList<>();
            int minLimit = Integer.MAX_VALUE;
            int subStock = Integer.MAX_VALUE;
            int actStock = Integer.MAX_VALUE;
            for(IDiscount discount : discountList){
                Activity activity = (Activity)discount;
                int brule = (int)discount.getBRule();
                List<IProduct> pList =  discount.getProductList();
                for (IProduct product: pList){
                    if(product.getSKU() == shoppingCartVO.getPdno()){
                        DiscountRule discountRule = new DiscountRule();
                        discountRule.setRulecode(brule);
                        discountRule.setRulename(DiscountRuleStore.getRuleByName(brule));
                        actCodeList.add(activity.getUnqid()+"");
                        if(activity.getLimits(product.getSKU()) < minLimit){
                            minLimit = activity.getLimits(product.getSKU());
                        }
                        //判断秒杀
                        if(brule == 1113){
                            shoppingCartVO.setStatus(1);
                        }
                        ruleList.add(discountRule);


                        actStock = Math.min(
                                RedisStockUtil
                                        .getActStockBySkuAndActno(shoppingCartVO.getPdno(),
                                                discount.getDiscountNo()),
                                actStock);

                        if(activity.getLimits(shoppingCartVO.getPdno()) == 0){
                            break;
                        }

                        subStock = Math.min
                                (activity.getLimits(shoppingCartVO.getPdno())
                                        - RedisOrderUtil.getActBuyNum(compid, shoppingCartVO.getPdno(),
                                        activity.getUnqid()),subStock);

                        minLimit = Math.min
                                (activity.getLimits(shoppingCartVO.getPdno())
                                        ,minLimit);
                    }
                }
            }
            shoppingCartVO.setLimitnum(minLimit);
            if(minLimit == Integer.MAX_VALUE){
                shoppingCartVO.setLimitnum(0);
            }

            int stock = shoppingCartVO.getInventory();
            try{
                stock = RedisStockUtil.getStock(shoppingCartVO.getPdno());
            }catch (Exception e){
                e.printStackTrace();
            }
            if(stock == 0){
                shoppingCartVO.setStatus(3);
            }
            shoppingCartVO.setLimitsub(subStock);
            shoppingCartVO.setInventory(stock);
            shoppingCartVO.setActstock(actStock);
            shoppingCartVO.setActcode(actCodeList);
            shoppingCartVO.setRule(ruleList);
        }

        DiscountResult discountResult
                = CalculateUtil.calculate(compid,ckProduct,Long.parseLong(shoppingCartList.get(0).getConpno()));

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

        return discountResult.getGiftList();
    }

    private void convResult(List<ShoppingCartVO> shoppingCartList, int compid){
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

            if (shoppingCartVO.getChecked() == 1) {
                result = result.add(BigDecimal.valueOf(product.getCurrentPrice()));
            }

        }

        DiscountResult discountResult
                = CalculateUtil.calculate(compid,productList,Long.parseLong(shoppingCartList.get(0).getConpno()));
        //获取活动

        for (ShoppingCartVO shoppingCartVO : shoppingCartList){
           List<DiscountRule> ruleList = new ArrayList<>();
           List<String> actCodeList = new ArrayList<>();
            int minLimit = Integer.MAX_VALUE;
            int subStock = Integer.MAX_VALUE;
            int actStock = Integer.MAX_VALUE;
            for(IDiscount discount : discountResult.getActivityList()){
                Activity activity = (Activity)discount;
                int brule = (int)discount.getBRule();
                List<IProduct> pList =  discount.getProductList();
               // long codno;
                for (IProduct product: pList){
                    if(product instanceof Package
                            && Long.parseLong(shoppingCartVO.getPkgno()) == ((Package) product).getPackageId()){
                        shoppingCartVO.setExCoupon(shoppingCartVO.isExCoupon() || activity.getExCoupon());

                        //判断库存
                        if(((Package) product).getExpireFlag() < 0){
                            shoppingCartVO.setStatus(3);
                        }

                        minLimit = Math.min
                                (activity.getLimits(((Package) product).getPackageId())
                                        ,minLimit);
                    }

                    if(product instanceof Product
                            && product.getSKU() == shoppingCartVO.getPdno()){
                        LogUtil.getDefaultLogger().info(
                                "PNO -> " + product.getSKU() + "\n" +
                                JSON.toJSONString(activity));

                        shoppingCartVO.setExCoupon(shoppingCartVO.isExCoupon() || activity.getExCoupon());
                        shoppingCartVO.setExActivity(activity.isExActivity());

                        if (pList.size() == 1 || activity.isExActivity()) {
                            shoppingCartVO.addCurrLadDesc(brule, activity.getCurrentLadoffDesc());
                            shoppingCartVO.addNextLadDesc(brule, activity.getNextLadoffDesc());
                        }

                        DiscountRule discountRule = new DiscountRule();
                        discountRule.setRulecode(brule);
                        discountRule.setRulename(DiscountRuleStore.getRuleByName(brule));
                        actCodeList.add(activity.getUnqid()+"");
                        if(activity.getLimits(product.getSKU()) < minLimit){
                            minLimit = activity.getLimits(product.getSKU());
                        }
                        //判断秒杀
                        if(brule == 1113){
                            shoppingCartVO.setSkprice(product.getOriginalPrice());
                            shoppingCartVO.setStatus(1);
                        }
                        ruleList.add(discountRule);


                        actStock = Math.min(
                                RedisStockUtil
                                        .getActStockBySkuAndActno(shoppingCartVO.getPdno(),
                                                discount.getDiscountNo()),
                                actStock);

                        if(activity.getLimits(shoppingCartVO.getPdno()) == 0){
                            break;
                        }

                        subStock = Math.min
                                (activity.getLimits(shoppingCartVO.getPdno())
                                        - RedisOrderUtil.getActBuyNum(compid, shoppingCartVO.getPdno(),
                                        activity.getUnqid()),subStock);

                        minLimit = Math.min
                                (activity.getLimits(shoppingCartVO.getPdno())
                                        ,minLimit);
                    }
                }
            }
            shoppingCartVO.setLimitnum(minLimit);
            if(minLimit == Integer.MAX_VALUE){
                shoppingCartVO.setLimitnum(0);
            }

            int stock = shoppingCartVO.getInventory();
            try{
                stock = RedisStockUtil.getStock(shoppingCartVO.getPdno());
            }catch (Exception e){
                e.printStackTrace();
            }
            if(stock == 0){
                shoppingCartVO.setStatus(3);
            }
            shoppingCartVO.setLimitsub(subStock);
            shoppingCartVO.setInventory(stock);
            shoppingCartVO.setActstock(actStock);
            shoppingCartVO.setActcode(actCodeList);
            shoppingCartVO.setRule(ruleList);
        }

        for (ShoppingCartVO shoppingCartVO : shoppingCartList) {
            if (shoppingCartVO.getChecked() == 1) {
                Product product = new Product();
                product.setSku(shoppingCartVO.getPdno());
                product.autoSetCurrentPrice(shoppingCartVO.getPdprice(), shoppingCartVO.getNum());
                ckProduct.add(product);
            }
        }

        discountResult
                = CalculateUtil.calculate(compid,ckProduct,Long.parseLong(shoppingCartList.get(0).getConpno()));

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



    private boolean delShoppingCart(String[] idList,int compid) {
        List<Object[]> shopParm = new ArrayList<>();
        for (String unqid : idList) {
            if(StringUtils.isInteger(unqid)){
                shopParm.add(new Object[]{Long.parseLong(unqid)});
            }
        }
        int[] result = baseDao.updateBatchNativeSharding(compid,
                TimeUtils.getCurrentYear(), REAL_DEL_SHOPCART_SQL,shopParm,shopParm.size());
        return !ModelUtil.updateTransEmpty(result);
    }


    private static List<IDiscount> getActivityList(int compid, List<Product> products) {
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
     * @接口摘要 查询结算购物车列表
     * @业务场景 下单商品列表展示
     * @传参类型 json
     * @传参列表 JsonArray
     *  pdno - 商品码
     *  pnum - 商品数量
     *  compid - 买家企业码
     *  checked - 是否选中：0 未选中，1 选中
     *  pdprice - 商品单价
     * @返回列表 JSONObject:
     *  goods 商品详情 com.onek.entity.ShoppingCartVO
     *  gifts 赠品列表 com.onek.entity.ShoppingCartVO
     */
    @UserPermission(ignore = true)
    public Result querySettShopCartList(AppContext appContext){
        int compid = appContext.getUserSession() == null
                ? 0
                : appContext.getUserSession().compId;
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

        int controlCode = appContext.getUserSession().comp.controlCode;

        for (ShoppingCartVO shoppingCartVO : shopCart) {
            int resultConsell = shoppingCartVO.getConsell();

            if ((resultConsell & controlCode) != resultConsell) {
                return result.fail("存在控销商品，您无权购买！");
            }
        }

        convResult(shopCart,compid);

        // 获取活动范围外
        List<ShoppingCartVO> outOfScope = getOutOfScope(shopCart, compid);

        if (!outOfScope.isEmpty()) {
            return new Result().fail("不可购买在经营范围外的商品！");
        }

        // 获取价格变动
        List<ShoppingCartVO> diffCheck = getDiffPrice(shopCart, shoppingCartDTOS);

        if (!diffCheck.isEmpty()) {
            return new Result().fail("商品价格存在变动，请重新刷新页面！");
        }

        List<Gift> giftList =
                getGifts(shopCart,shoppingCartDTOS.get(0).getCompid());

        List<ShoppingCartVO> giftVOS = new ArrayList<>();

        ShoppingCartVO giftVO;
        for (Gift gift : giftList) {
            if (gift.getType() != 3) {
                continue;
            }

            giftVO = new ShoppingCartVO();

            giftVO.setPtitle(gift.getGiftName());
            giftVO.setNum(gift.getTotalNums());
            giftVO.setPdno(gift.getTurlyId());

            giftVOS.add(giftVO);
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("goods", shopCart);
        jsonObject.put("gifts", giftVOS);

        return result.success(jsonObject);
    }

    private List<ShoppingCartVO> getDiffPrice(List<ShoppingCartVO> shopCarts, List<ShoppingCartDTO> shoppingCartDTOs) {
        List<ShoppingCartVO> results = new ArrayList<>();

        if (shopCarts == null || shoppingCartDTOs == null) {
            return results;
        }

        for (ShoppingCartVO shoppingCartVO : shopCarts) {
            for (ShoppingCartDTO shoppingCartDTO : shoppingCartDTOs) {
                if (shoppingCartDTO.getPdno() == shoppingCartVO.getPdno()) {
                    if (shoppingCartDTO.getPdprice() != shoppingCartVO.getPdprice()) {
                        results.add(shoppingCartVO);
                    }
                }
            }
        }

        return results;
    }

    private List<ShoppingCartVO> getOutOfScope(List<ShoppingCartVO> shopCarts, int compid) {
        List<ShoppingCartVO> results = new ArrayList<>();

        if (shopCarts == null || compid <= 0) {
            return results;
        }

        String sql = " SELECT IFNULL(busscope, 0) "
                + " FROM {{?" + DSMConst.TB_COMP_BUS_SCOPE + "}} "
                + " WHERE cstatus&1 = 0 AND compid = ? ";

        List<Object[]> queryResult = IceRemoteUtil.queryNative(sql, compid);

        int[] scopes = new int[queryResult.size()];

        for (int i = 0; i < queryResult.size(); i++) {
            scopes[i] = Integer.parseInt(queryResult.get(i)[0].toString());
        }

        OUTTER:
        for (ShoppingCartVO shoppingCartVO : shopCarts) {
            if (shoppingCartVO.getBusscope() == 0) {
                continue;
            }

            for (int scope : scopes) {
                if (scope == shoppingCartVO.getBusscope()) {
                    continue OUTTER;
                }
            }

            results.add(shoppingCartVO);
        }

        return results;
    }

    /**
     * @接口摘要 获取购物车优惠提示
     * @业务场景 购物车优惠显示
     * @传参类型 json
     * @传参列表 JsonArray
     *  pdno - 商品码
     *  pnum - 商品数量
     *  compid - 买家企业码
     *  checked - 是否选中：0 未选中，1 选中
     *  pdprice - 商品单价
     * @返回列表 JsonArray:
     *  ladamt - 阶梯金额
     *  ladnum - 数量阶梯值
     *  offercode - 优惠码
     *  offername - 活动名称
     *  offer - 优惠值
     *  nladnum - 下个阶梯数量
     *  nladamt - 下一个阶梯金额
     *  noffer - 下个阶梯优惠值
     *  currladDesc - 当前阶梯描述
     *  nextladDesc - 下个阶梯描述
     */
    @UserPermission(ignore = true)
    public Result getOfferTip(AppContext appContext){
        String json = appContext.param.json;
        Result result = new Result();
        JsonParser jsonParser = new JsonParser();
        JsonArray jsonArray = jsonParser.parse(json).getAsJsonArray();

        if (jsonArray.size() == 0) {
            return new Result().success(null);
        }

        List<ShoppingCartDTO> shoppingCartDTOS = new ArrayList<>();
        Gson gson = new Gson();
        List<OfferTipsVO> offerTipsVOS = new ArrayList<>();
        for (JsonElement shopVO : jsonArray){
            ShoppingCartDTO shoppingCartDTO = gson.fromJson(shopVO, ShoppingCartDTO.class);
            shoppingCartDTOS.add(shoppingCartDTO);
        }
        
        List<ShoppingCartVO> shopCartList = getShopCart(shoppingCartDTOS);
        List<Product> productList = new ArrayList<>();

        if (shopCartList != null) {
            for (ShoppingCartVO shoppingCartVO : shopCartList){
                Product product = new Product();
                product.setSku(shoppingCartVO.getPdno());
                product.autoSetCurrentPrice(shoppingCartVO.getPdprice(),shoppingCartVO.getNum());
                productList.add(product);
            }
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

            if (activity.isExActivity()) {
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
                offerTipsVO.setUnqid(currLadoff.getUnqid()+"");
                offerTipsVO.setCurrladDesc(activity.getCurrentLadoffDesc());
            }

            if(nextLadoff != null){
                offerTipsVO.setNladamt(nextLadoff.getLadamt());
                offerTipsVO.setNladnum(nextLadoff.getLadnum());
                offerTipsVO.setNoffer(nextLadoff.getOffer());
                offerTipsVO.setOffercode(nextLadoff.getOffercode());
                offerTipsVO.setUnqid(nextLadoff.getUnqid()+"");
                offerTipsVO.setNextladDesc(activity.getNextLadoffDesc());
            }

            offerTipsVOS.add(offerTipsVO);
        }
        return result.success(offerTipsVOS);
    }

    /**
     * 根据SKU查询购物车现有数量
     */
    public static int queryShopCartNumBySku(int compid ,long sku){
        List<Object[]> queryRet = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(),
                SELECT_SKUNUM_SQL, compid, sku);
        if(queryRet == null || queryRet.isEmpty()){
            return 0;
        }
        return Integer.parseInt(queryRet.get(0)[0].toString());
    }

    @UserPermission(ignore = true)
    public String queryShopCartNumBySkus(AppContext context){
        try{
            return queryShopCartNumBySkus(Integer.parseInt(context.param.arrays[0]),context.param.arrays[1]);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据SKU查询购物车现有数量
     */
    public static String queryShopCartNumBySkus(int compid ,String skus){
        String sql = "select pdno,pnum from {{?" + DSMConst.TD_TRAN_GOODS + "}} "
                + " where orderno = 0 and pkgno=0 and compid = ? and cstatus&1=0 and  pdno in(" +skus+ ") ";
        List<Object[]> queryRet = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(), sql, compid);
        if(queryRet == null || queryRet.isEmpty()) return "";
        JsonArray jsonArray = new JsonArray();
        queryRet.forEach(qr -> {
            JsonObject object = new JsonObject();
            object.addProperty("sku",Long.parseLong(qr[0].toString()));
            object.addProperty("pnum",Integer.parseInt(qr[1].toString()));
            jsonArray.add(object);
        });
        return jsonArray.toString();
    }


    @UserPermission(ignore = true)
    public int remoteQueryShopCartNumBySku(AppContext context){
        try{
            return queryShopCartNumBySku(Integer.parseInt(context.param.arrays[0]),Long.parseLong(context.param.arrays[1]));
        }catch (Exception e){
            e.printStackTrace();
        }
            return 0;
    }

    /**
     * 根据SKU查询购物车套餐现有数量
     */
    public static String queryPkgShopCartNum(int compid ,String pkgnos){
        String sql = "select pdno,pnum,pkgno from {{?" + DSMConst.TD_TRAN_GOODS + "}} "
                + " where orderno = 0 and cstatus&1=0 and compid=? and pkgno in(" + pkgnos +")";
        List<Object[]> queryRet = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(), sql, compid);
        if(queryRet == null || queryRet.isEmpty()) return "";
        JSONObject object = new JSONObject();
        object.put("sku",Long.parseLong(queryRet.get(0)[0].toString()));
        object.put("pnum",Integer.parseInt(queryRet.get(0)[1].toString()));
        object.put("pkgno",Integer.parseInt(queryRet.get(0)[2].toString()));
        return object.toJSONString();
    }
}
