package com.onek.order;

import com.google.gson.*;
import com.onek.annotation.UserPermission;
import com.onek.calculate.ActivityFilterService;
import com.onek.calculate.entity.*;
import com.onek.calculate.filter.*;
import com.onek.context.AppContext;
import com.onek.entity.DiscountRule;
import com.onek.entity.ShoppingCartDTO;
import com.onek.entity.ShoppingCartVO;
import com.onek.entitys.Result;
import com.onek.util.CalculateUtil;
import com.onek.util.discount.DiscountRuleStore;
import constant.DSMConst;
import dao.BaseDAO;
import global.GenIdUtil;
import util.GsonUtils;
import util.ModelUtil;
import util.StringUtils;
import util.TimeUtils;

import java.lang.ref.PhantomReference;
import java.util.ArrayList;
import java.util.Arrays;
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
    private final String SELECT_SHOPCART_SQL_EXT = "select unqid from {{?" + DSMConst.TD_TRAN_GOODS + "}} "
            + " where orderno = 0 and compid = ? and  pdno = ? ";

    //新增数量
    private final String UPDATE_SHOPCART_SQL_EXT = "update {{?" + DSMConst.TD_TRAN_GOODS + "}} set pnum = pnum + ? " +
            " where unqid = ? ";


    //新增数量
    private final String UPDATE_SHOPCART_SQL_NUM = "update {{?" + DSMConst.TD_TRAN_GOODS + "}} set pnum =  ? " +
            " where unqid = ? ";




    //删除购物车信息
    private final String DEL_SHOPCART_SQL = "update {{?" + DSMConst.TD_TRAN_GOODS + "}} set cstatus=cstatus|1 "
            + " where cstatus&1=0 and unqid=?";


    //查询购物车列表
    private final String QUERY_SHOPCART_SQL = "select unqid,pdno,compid,cstatus,pnum from {{?" + DSMConst.TD_TRAN_GOODS + "}} "+
            " where cstatus&1=0 and compid = ? order by createdate,createtime desc";


    private static final String QUERY_PROD_BASE =
            " SELECT  spu.prodname ptitle,m.manuname verdor," +
                    "sku.sku pdno, convert(sku.vatp/100,decimal(10,2)) pdprice, DATE_FORMAT(sku.vaildedate,'%Y-%m-%d') vperiod," +
                    "sku.store inventory, sku.spec, sku.prodstatus,spu.spu "
                    + " FROM ({{?" + DSMConst.TD_PROD_SPU + "}} spu "
                    + " INNER JOIN {{?" + DSMConst.TD_PROD_SKU   + "}} sku ON spu.spu = sku.spu ) "
                    + " LEFT  JOIN {{?" + DSMConst.TD_PROD_MANU  + "}} m   ON m.cstatus&1 = 0 AND m.manuno  = spu.manuno "
                    + " LEFT  JOIN {{?" + DSMConst.TD_PROD_BRAND + "}} b   ON b.cstatus&1 = 0 AND b.brandno = spu.brandno "
                    + " WHERE 1=1 ";









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
        List<Object[]> queryRet = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(),
                SELECT_SHOPCART_SQL_EXT, new Object[]{compid, shopVO.getPdno()});

        int ret = 0;
        if(queryRet == null || queryRet.isEmpty()){
            ret = baseDao.updateNativeSharding(compid,TimeUtils.getCurrentYear(),
                    INSERT_SHOPCART_SQL,new Object[]{GenIdUtil.getUnqId(),0,
                            shopVO.getPdno(),shopVO.getPnum(),compid,0});
        }else{
            long unqid = Long.parseLong(queryRet.get(0)[0].toString());

            ret = baseDao.updateNativeSharding(compid,TimeUtils.getCurrentYear(),
                    UPDATE_SHOPCART_SQL_EXT,new Object[]{shopVO.getPnum(),
                            unqid});

        }
        if(ret > 0){
            return result.success("新增成功");
        }
        return result.fail("新增失败");
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
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int compid = jsonObject.get("compid").getAsInt();
        Result result = new Result();
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
        baseDao.updateBatchNativeSharding(shoppingCartDTOS.get(0).getCompid(),TimeUtils.getCurrentYear(),UPDATE_SHOPCART_SQL_NUM, updateParm, updateParm.size());
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
                new String[]{"ptitle","verdor","pdno","pdprice","vperiod","inventory","spec","status","spu"});


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
        for(ShoppingCartVO shoppingCartVO : shoppingCartList){
            for(int i = 0; i < shoppingCartDTOS.size(); i++){
                if(shoppingCartVO.getPdno() == shoppingCartDTOS.get(i).getPdno()){
                    shoppingCartVO.setNum(shoppingCartDTOS.get(i).getPnum());
                    shoppingCartVO.setChecked(shoppingCartDTOS.get(i).getChecked());
                    shoppingCartVO.setUnqid(shoppingCartDTOS.get(i).getUnqid());
                    shoppingCartVO.setCounpon(shoppingCartDTOS.get(i).getConpno());
                    break;
                }
            }
        }
        return shoppingCartList;
    }


    public void convResult(List<ShoppingCartVO> shoppingCartList,int compid){
        List<Product> productList = new ArrayList<>();

        List<Product> ckProduct = new ArrayList<>();


        for (ShoppingCartVO shoppingCartVO : shoppingCartList){
            Product product = new Product();
            product.setSku(shoppingCartVO.getPdno());
            product.autoSetCurrentPrice(shoppingCartVO.getPdprice(),shoppingCartVO.getNum());
            productList.add(product);

            if(shoppingCartVO.getChecked() == 1){
                ckProduct.add(product);
            }
        }

        List<IDiscount> discountList = getActivityList(compid,productList);

        //获取活动
        for (ShoppingCartVO shoppingCartVO : shoppingCartList){
           List<DiscountRule> ruleList = new ArrayList<>();
            for(IDiscount discount : discountList){
                int brule = (int)discount.getBRule();
                List<IProduct> pList =  discount.getProductList();
                for (IProduct product: pList){
                    if(product.getSKU() == shoppingCartVO.getPdno()){
                        DiscountRule discountRule = new DiscountRule();
                        discountRule.setRulecode(brule);
                        discountRule.setRulename(DiscountRuleStore.getRuleByName(brule));
                        if(brule == 1113 ||  brule == 1133){
                            shoppingCartVO.setStatus(1);
                        }
                        ruleList.add(discountRule);
                    }
                }
            }
            shoppingCartVO.setRule(ruleList);
        }


        DiscountResult discountResult
                = CalculateUtil.calculate(compid,ckProduct,shoppingCartList.get(0).getConpno());
        for (ShoppingCartVO shoppingCartVO : shoppingCartList){
            for(Product product: ckProduct){
                if(shoppingCartVO.getPdno() == product.getSKU()
                        && shoppingCartVO.getChecked() == 1){
                    shoppingCartVO.setDiscount(product.getDiscounted());
                    shoppingCartVO.setAmt(discountResult.getTotalDiscount());
                    shoppingCartVO.setAcamt(discountResult.getTotalCurrentPrice());
                    shoppingCartVO.setCounpon(discountResult.getCouponValue());
                    shoppingCartVO.setFreepost(discountResult.isFreeShipping());
                    shoppingCartVO.setFreight(20);
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
                                new TypeFilter(),
                                new CycleFilter(),
                                new QualFilter(compid),
                                new PriorityFilter(),
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

}
