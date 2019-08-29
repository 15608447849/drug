package com.onek.order;

import Ice.Application;
import Ice.Logger;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
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
            + " where orderno = 0 and compid = ? and  pdno = ? and pkgno = 0 and cstatus&1=0";

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

    //删除购物车套餐信息
    private final String DEL_SHOPCART_PKG_SQL = "delete from {{?" + DSMConst.TD_TRAN_GOODS + "}} "
            + " where cstatus&1=0 and pkgno=? and compid = ? and orderno=0 ";


    //查询购物车列表
    private final String QUERY_SHOPCART_SQL = "select unqid,pdno,compid,cstatus,pnum,ifnull(pkgno,0) pkgno from {{?" + DSMConst.TD_TRAN_GOODS + "}} " +
            " where cstatus&1=0 and  orderno = 0 and compid = ? order by createdate,createtime desc";

    //删除购物车信息
    private final String REAL_DEL_SHOPCART_SQL = "delete  from {{?" + DSMConst.TD_TRAN_GOODS + "}}  "
            + " where cstatus&1=0 and unqid=? or pdno=? and orderno=0 and pkgno=0";

    //删除购物车信息
    private final String REAL_DEL_PC_SHOPCART_SQL = "delete  from {{?" + DSMConst.TD_TRAN_GOODS + "}}  "
            + " where cstatus&1=0 and unqid=? and orderno=0 ";

    //远程调用
    private static final String QUERY_PROD_BASE =
            " SELECT ifnull(spu.prodname,'') ptitle,ifnull(m.manuname,'') verdor," +
                    "sku.sku pdno, convert(sku.vatp/100,decimal(10,2)) pdprice, DATE_FORMAT(sku.vaildsdate,'%Y-%m-%d') vperiod," +
                    "sku.store-sku.freezestore inventory,ifnull(sku.spec,'') spec, sku.prodstatus,spu.spu,sku.limits,ifnull(brandname,'') brand,medpacknum,unit," +
                    "convert(mp/100,decimal(10,2)) mp, IFNULL(spu.busscope, 0), IFNULL(sku.consell, 0) "
                    + " FROM ({{?" + DSMConst.TD_PROD_SPU + "}} spu "
                    + " INNER JOIN {{?" + DSMConst.TD_PROD_SKU + "}} sku ON spu.spu = sku.spu ) "
                    + " LEFT  JOIN {{?" + DSMConst.TD_PROD_MANU + "}} m   ON m.cstatus&1 = 0 AND m.manuno  = spu.manuno "
                    + " LEFT  JOIN {{?" + DSMConst.TD_PROD_BRAND + "}} b   ON b.cstatus&1 = 0 AND b.brandno = spu.brandno "
                    + " WHERE 1=1 ";


    //远程调用
    private static final String QUERY_ONE_PROD_INV = "SELECT store-freezestore inventory,limits,convert(vatp/100,decimal(10,2)) pdprice from " +
            " {{?" + DSMConst.TD_PROD_SKU + "}} where sku =? and cstatus & 1 = 0";

    private static final String QUERY_ONE_PROD_INV_BUSSCOPE =
            "SELECT store-freezestore inventory, limits, "
                    + " convert(vatp/100,decimal(10,2)) pdprice, IFNULL(spu.busscope, 0), IFNULL(sku.consell, 0) "
                    + " from {{?" + DSMConst.TD_PROD_SKU + "}} sku, {{?" + DSMConst.TD_PROD_SPU + "}} spu "
                    + " where spu.cstatus & 1 = 0 AND sku.spu = spu.spu "
                    + " AND sku.sku =? and sku.cstatus & 1 = 0 ";

    private static final String QUERY_ONE_PKG_INV_BUSSCOPE =
            "SELECT  IFNULL(spu.busscope, 0), IFNULL(sku.consell, 0) "
                    + " from {{?" + DSMConst.TD_PROD_SKU + "}} sku, {{?" + DSMConst.TD_PROD_SPU + "}} spu "
                    + " where spu.cstatus & 1 = 0 AND sku.spu = spu.spu "
                    + " and sku.cstatus & 1 = 0 ";


    //查询购物车商品数量
    private static final String SELECT_SKUNUM_SQL = "select pnum from {{?" + DSMConst.TD_TRAN_GOODS + "}} "
            + " where orderno = 0 and compid = ? and  pdno = ? and pkgno = 0  and cstatus&1=0";

    private static final String SELECT_PKGNUM_SQL = "select pnum,pdno from {{?" + DSMConst.TD_TRAN_GOODS + "}} "
            + " where orderno = 0 and compid = ? and  pkgno = ? and cstatus&1=0";


    private static final String SELECT_SHOPCART_SQL_EXT_PKG = "select unqid,pnum,pdno from {{?" + DSMConst.TD_TRAN_GOODS + "}} "
            + " where orderno = 0 and compid = ? and cstatus&1=0 ";


    private static final String SELECT_SHOPCART_SUM_GROUP_PKG = "select pkgno,sum(pnum) from {{?" + DSMConst.TD_TRAN_GOODS + "}} "
            + " where orderno = 0 and compid = ? and cstatus&1=0 ";    //更新购物车套餐数量

    private final String UPDATE_SHOPCART_PKG_SQL_EXT = "update {{?" + DSMConst.TD_TRAN_GOODS + "}} set pnum = ? " +
            " where orderno = 0 and pdno = ? and compid = ? and pkgno = ? ";

//    private final String UPDATE_SHOPCART_PKG_SQL = "update {{?" + DSMConst.TD_TRAN_GOODS + "}} "
//            + "(unqid,orderno,pdno,pnum,createdate,createtime,compid,cstatus,pkgno) "
//            + "values (?,?,?,?,CURRENT_DATE,CURRENT_TIME,?,?,?)";


    //新增购物车
    private final String INSERT_SHOPCART_PKG_SQL = "insert into {{?" + DSMConst.TD_TRAN_GOODS + "}} "
            + "(unqid,orderno,pdno,pnum,createdate,createtime,compid,cstatus,pkgno) "
            + "values (?,?,?,?,CURRENT_DATE,CURRENT_TIME,?,?,?)";


    //查询订单再次购买
    private static final String SELECT_SHOPCART_ORDER = " select unqid,pdno,compid,pnum,pkgno from {{?" + DSMConst.TD_TRAN_GOODS + "}} "
            + " where orderno = ? and cstatus & 1 = 0";


    /**
     * @接口摘要 购物车商品新增
     * @业务场景 商品加入购物车
     * @传参类型 JSON
     * @传参列表 pdno - 商品码
     * pnum - 商品数量
     * compid - 买家企业码
     * @返回列表 code - 200 成功/ -1 失败
     * message - 成功/失败
     **/
    public Result saveShopCart(AppContext appContext) {
        String json = appContext.param.json;
        Result result = new Result();
        ShoppingCartDTO shopVO = GsonUtils.jsonToJavaBean(json, ShoppingCartDTO.class);

        if (shopVO == null) {
            LogUtil.getDefaultLogger().debug("参数有误");
            return result.fail("参数有误");
        }

        int compid = appContext.getUserSession().compId;
        //判断当前加入购物车是否为套餐
        if (Long.parseLong(shopVO.getPkgno()) > 0) {
            List<IProduct> productList = new ArrayList<>();
            Package pkg = new Package();
            pkg.setNums(shopVO.getPkgnum());
            int opflag = 0;
            if (shopVO.getPkgnum() < 0) {
                pkg.setNums(-shopVO.getPkgnum());
                opflag = 1;
            }

            pkg.setPackageId(Long.parseLong(shopVO.getPkgno()));
            productList.add(pkg);

            int ret = -3;
            try {
                ret = optGoodsPkg(productList, compid, opflag,appContext);
            } catch (Exception e) {
                LogUtil.getDefaultLogger().debug("套餐加入购物车失败");
                e.printStackTrace();
            }
            if (ret == 0) {
                return new Result().success("加入采购单成功", "新增成功");
            } else if (ret == -1) {
                return new Result().fail("该套餐已失效");
            } else if (ret == -2) {
                return new Result().fail("超过限购量");
            } else if(ret == -4){
                return new Result().fail("活动库存不足");
            } else if(ret == -5){
                return new Result().fail("此为控销商品，您无权加入购物车！");
            }
            return new Result().fail("加入购物车失败");
        }

        //远程调用
        List<Object[]> queryInvRet = IceRemoteUtil.queryNative(QUERY_ONE_PROD_INV_BUSSCOPE, shopVO.getPdno());
        if (queryInvRet == null || queryInvRet.isEmpty()) {
            return result.fail("查询商品失败");
        }

        if (!appContext.isSignControlAgree()) {
            int consell = Integer.parseInt(queryInvRet.get(0)[4].toString());

            if ((consell & 1) > 0) {
                return result.fail("此为控销商品，您无权加入购物车！");
            }
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
            //临时注释
            if (outOfScope) {
                return new Result().fail("非经营范围内的商品不可加入购物车！");
            }
        }


        int inventory = Integer.parseInt(queryInvRet.get(0)[0].toString());
        List<Object[]> queryRet = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(),
                SELECT_SHOPCART_SQL_EXT, compid, shopVO.getPdno());

        int ret = 0;
        List<IProduct> productList = new ArrayList<>();
        Product product = new Product();
        product.setSku(shopVO.getPdno());

        double pdprice = Double.parseDouble(queryInvRet.get(0)[2].toString());
        if (queryRet == null || queryRet.isEmpty()) {
            product.setNums(shopVO.getPnum());
            product.autoSetCurrentPrice(pdprice, shopVO.getPnum());
            productList.add(product);
            ret = baseDao.updateNativeSharding(compid, TimeUtils.getCurrentYear(),
                    INSERT_SHOPCART_PKG_SQL, GenIdUtil.getUnqId(), 0,
                    shopVO.getPdno(), getSkuInv(compid, productList, inventory), compid, 0, 0);
        } else {
            long unqid = Long.parseLong(queryRet.get(0)[0].toString());
            int pnum = Integer.parseInt(queryRet.get(0)[1].toString());
            int current = shopVO.getPnum() + pnum;
            appContext.logger.print("存入数量: " + current);
            product.setNums(current < 0 ? 0 : current);
            product.autoSetCurrentPrice(pdprice, shopVO.getPnum() + pnum);
            productList.add(product);
            ret = baseDao.updateNativeSharding(compid, TimeUtils.getCurrentYear(),
                    UPDATE_SHOPCART_SQL_EXT, getSkuInv(compid, productList, inventory),
                    unqid);

        }
        if (ret > 0) {
            return result.success("加入采购单成功", "新增成功");
        }
        return result.fail("无法加入采购单");
    }


    public int getSkuInv(int compid, List<IProduct> products, int inventory) {
        List<IDiscount> discountList = getActivityList(compid, products);
        int pnum = products.get(0).getNums();
        int subStock = Integer.MAX_VALUE;
        int actStock = Integer.MAX_VALUE;
        if (discountList != null && !discountList.isEmpty()) {
            for (IDiscount discount : discountList) {
                Activity activity = (Activity) discount;

                actStock = Math.min(
                        RedisStockUtil
                                .getActStockBySkuAndActno(products.get(0).getSKU(),
                                        discount.getDiscountNo()),
                        actStock);

                if (activity.getLimits(products.get(0).getSKU()) == 0) {
                    continue;
                }

                subStock = Math.min
                        (activity.getLimits(products.get(0).getSKU())
                                - RedisOrderUtil.getActBuyNum(compid, products.get(0).getSKU(),
                                activity.getUnqid()), subStock);
            }
        }
        int stock = inventory;
        try {
            stock = RedisStockUtil.getStock(products.get(0).getSKU());
        } catch (Exception e) {
            e.printStackTrace();
        }

        int minStock = stock;

        if (actStock < minStock) {
            minStock = actStock;
        }

        if (subStock < minStock) {
            minStock = subStock;
        }

        if (minStock < pnum) {
            return minStock;
        }

        return pnum;
    }


    /**
     * 获取SKU可以购买的数量
     *
     * @param compid 企业码
     * @param sku    SKU码
     * @param skuNum SKU购买数量
     * @param type   0 购物车购买 1非购物车购买
     * @return 可以购买的数量
     */
    public static int getCanbuySkuNum(int compid, long sku, int skuNum, int type) {
        Logger logger = Application.communicator().getLogger();

        List<Object[]> queryInvRet = IceRemoteUtil.queryNative(QUERY_ONE_PROD_INV, sku);
        logger.print("IceRemoteUtil.queryNative = " + queryInvRet.size());
        for (Object[] arr : queryInvRet) logger.print("\t" + Arrays.toString(arr));

        if (queryInvRet.isEmpty()) {
            return 0;
        }
        List<IProduct> productList = new ArrayList<>();
        Product product = new Product();
        product.setSku(sku);
        double pdprice = Double.parseDouble(queryInvRet.get(0)[2].toString());
        int inventory = Integer.parseInt(queryInvRet.get(0)[0].toString());
        logger.print("商品sku = " + sku + " 当前价格: " + pdprice + " , 库存 - " + inventory);
        List<Object[]> queryRet = null;
        if (type == 0) {
            queryRet = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(),
                    SELECT_SHOPCART_SQL_EXT, compid, sku);
        }
        if (queryRet == null || queryRet.isEmpty()) { //不存在购物车数量记录,新购买
            product.setNums(skuNum);
            product.autoSetCurrentPrice(pdprice, skuNum);
            productList.add(product);
        } else { //存在购物车数量记录
            int pnum = Integer.parseInt(queryRet.get(0)[1].toString());
            product.setNums(skuNum + pnum);
            product.autoSetCurrentPrice(pdprice, skuNum + pnum);
            productList.add(product);
        }

        //活动列表
        List<IDiscount> discountList = getActivityList(compid, productList);
        int pnum = productList.get(0).getNums();
        int subStock = Integer.MAX_VALUE;
        int actStock = Integer.MAX_VALUE;
        if (discountList != null && !discountList.isEmpty()) {
            logger.print("活动列表数量; " + discountList.size());
            for (IDiscount discount : discountList) {
                Activity activity = (Activity) discount;

                actStock = Math.min(
                        RedisStockUtil.getActStockBySkuAndActno(productList.get(0).getSKU(), discount.getDiscountNo()),
                        actStock);

                if (activity.getLimits(productList.get(0).getSKU()) == 0) {
                    continue;
                }

                subStock = Math.min
                        (activity.getLimits(productList.get(0).getSKU())
                                - RedisOrderUtil.getActBuyNum(compid, productList.get(0).getSKU(),
                                activity.getUnqid()), subStock);
            }
            logger.print(" actStock = " + actStock + " , subStock = " + subStock);
        }
        int stock = inventory;
        try {
            stock = RedisStockUtil.getStock(productList.get(0).getSKU());
        } catch (Exception e) {
            e.printStackTrace();
        }

        int minStock = stock;

        logger.print("minStock = " + minStock + " , actStock = " + actStock + " , subStock = " + subStock);

        if (actStock < minStock) {
            minStock = actStock;
        }

        if (subStock < minStock) {
            minStock = subStock;
        }
        if (minStock < pnum) {
            return minStock;
        }
        return pnum;
    }

    public static int getCanbuyPkgNum(int compid, long pkgno, int pkgNum, int type) {
        int minLimit = Integer.MAX_VALUE;
        int minStock = Integer.MAX_VALUE;
        List<Object[]> queryRet = null;
        if (type == 0) {
            StringBuilder sb = new StringBuilder(SELECT_SHOPCART_SUM_GROUP_PKG);
            sb.append(" and pkgno = ? ");
            queryRet = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(),
                    sb.toString(), compid, pkgno);
        }
        List<IProduct> productList = new ArrayList<>();
        Package pkg = new Package();
        //不存在购物车数量记录,新购买
        if (queryRet == null || queryRet.isEmpty()) {
            pkg.setNums(pkgNum);
            pkg.setPackageId(pkgno);
            productList.add(pkg);
        } else { //存在购物车数量记录
            pkg.setNums(pkgNum);
            pkg.setPackageId(pkgno);
            productList.add(pkg);

            DiscountResult discountResult
                    = CalculateUtil.calculate(compid, productList,
                    0);

            int tmpPkgNum = 0;
            for (IDiscount discount : discountResult.getActivityList()) {
                Activity activity = (Activity) discount;
                List<IProduct> pkgList = activity.getProductList();
                for (int i = 0; i < pkgList.size(); i++) {
                    if (pkgList.get(i) instanceof Package) {
                        Package apkg = (Package) pkgList.get(i);
                        if (queryRet != null && !queryRet.isEmpty()) {
                            for (Object[] objects : queryRet) {
                                if (Integer.parseInt(objects[0].toString()) == apkg.getPackageId()) {
                                    //计算当前套餐总件数
                                    tmpPkgNum = Integer.parseInt(objects[1].toString())
                                            / apkg.singleProdsCount();
                                }
                            }
                        }
                    }
                }
            }
            pkg.setNums(tmpPkgNum + pkgNum);
        }

        DiscountResult discountResult
                = CalculateUtil.calculate(compid, productList,
                0);

        if (discountResult.isPkgExpired()) {
            return 0;
        }

        for (IDiscount iDiscount : discountResult.getActivityList()) {
            for (IProduct iProduct : iDiscount.getProductList()) {
                if (iDiscount.getLimits(iProduct.getSKU()) > 0
                        && iDiscount.getLimits(iProduct.getSKU()) < iProduct.getNums()) {
                    return 0;
                }
            }
            minLimit = Math.min
                    (iDiscount.getLimits(((Package) iDiscount.getProductList().get(0)).getPackageId())
                            , minLimit);

            minStock = Math.min(getPkgInv(pkg, (Activity) iDiscount, compid), minStock);
        }
        return Math.min(Math.min(minLimit, minStock), pkgNum);
    }


    /**
     * @接口摘要 再次购买
     * @业务场景 订单交易成功后再次购买
     * @传参类型 JSONARRAY
     * @传参列表 pdno - 商品码
     * pnum - 商品数量
     * compid - 买家企业码
     * @返回列表 code - 200 成功/ -1 失败
     * message - 成功/失败
     **/
    @UserPermission(ignore = true)
    public Result againShopCart(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        Result result = new Result();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        if (!jsonObject.has("compid")
                || !jsonObject.has("orderno")) {
            return result.fail("添加失败");
        }

        int compid = jsonObject.get("compid").getAsInt();
        String orderno = jsonObject.get("orderno").getAsString();

        List<Object[]> orderRet = baseDao.queryNativeSharding(compid,
                TimeUtils.getCurrentYear(), SELECT_SHOPCART_ORDER,
                new Object[]{orderno});

        if (orderRet == null || orderRet.isEmpty()) {
            return new Result().fail("商品为空！");
        }

        ShoppingCartDTO[] shoppingCartDTOArray = new ShoppingCartDTO[orderRet.size()];
        baseDao.convToEntity(orderRet, shoppingCartDTOArray, ShoppingCartDTO.class,
                new String[]{"unqid", "pdno", "compid", "pnum", "pkgno"});


        //List<ShoppingCartDTO> shoppingCartDTOS = new ArrayList<>();
        HashSet<Long> pkgnoSet = new HashSet<>();
        List<Object[]> updateParm = new ArrayList<>();
        List<Object[]> insertParm = new ArrayList<>();
        StringBuilder parmSql = new StringBuilder();
        for (ShoppingCartDTO shoppingCartDTO : shoppingCartDTOArray) {
            if (shoppingCartDTO != null) {
                parmSql.append(shoppingCartDTO.getPdno()).append(",");
                if(Long.parseLong(shoppingCartDTO.getPkgno()) > 0){
                    pkgnoSet.add(Long.parseLong(shoppingCartDTO.getPkgno()));
                }
            }
        }
        String skuStr = parmSql.toString().substring(0, parmSql.toString().length() - 1);

        String querySql = " select unqid,pdno,pnum,pkgno from {{?" + DSMConst.TD_TRAN_GOODS + "}} "
                + " where orderno = 0 and cstatus&1 = 0 and compid = " + compid;
        querySql = querySql + " and pdno in (" + skuStr + ")";

        //远程调用
        String querySkuSql = "SELECT store-freezestore inventory,limits,convert(vatp/100,decimal(10,2)) pdprice,sku pdno from " +
                " {{?" + DSMConst.TD_PROD_SKU + "}} where cstatus & 1 = 0 ";
        querySkuSql = querySkuSql + " and sku in (" + skuStr + ")";

        //远程调用
        List<Object[]> querySkuRet = IceRemoteUtil.queryNative(querySkuSql);
        if (querySkuRet == null || querySkuRet.isEmpty()) {
            return result.fail("添加失败");
        }

        List<Object[]> queryRet = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(),
                querySql);
        convtShopCartDTO(compid, querySkuRet, shoppingCartDTOArray, queryRet,pkgnoSet);
        boolean flag = true;
        for (ShoppingCartDTO shoppingCartDTO : shoppingCartDTOArray) {
            if(shoppingCartDTO.getCstatus() == 3){
                continue;
            }
            if (queryRet != null && !queryRet.isEmpty()) {
                for (Object[] objects : queryRet) {
                    if (shoppingCartDTO.getPdno() == Long.parseLong(objects[1].toString())
                             && Long.parseLong(shoppingCartDTO.getPkgno()) == Long.parseLong(objects[3].toString()) ) {
                        updateParm.add(new Object[]{shoppingCartDTO.getPnum(), objects[0]});
                        flag = false;
                        break;
                    }
                }
            }
            if (flag) {
                insertParm.add(new Object[]{GenIdUtil.getUnqId(), 0,
                        shoppingCartDTO.getPdno(), shoppingCartDTO.getPnum(), compid, 0, shoppingCartDTO.getPkgno()});
                flag = true;
            }


        }

        int[] uret = baseDao.updateBatchNativeSharding(compid, TimeUtils.getCurrentYear(),
                UPDATE_SHOPCART_SQL_EXT, updateParm, updateParm.size());

        int[] iret = baseDao.updateBatchNativeSharding(compid, TimeUtils.getCurrentYear(),
                INSERT_SHOPCART_PKG_SQL, insertParm, insertParm.size());

        if (!ModelUtil.updateTransEmpty(uret) || !ModelUtil.updateTransEmpty(iret)) {
            return result.success("添加成功");
        }
        return result.fail("添加失败，该订单存在已下架商品！");
    }

    public void convtShopCartDTO(int compid,
                                 List<Object[]> querySkuRet,
                                 ShoppingCartDTO[] shoppingCartDTOS,
                                 List<Object[]> queryRet,
                                 HashSet<Long> pkgnoSet) {

        List<IProduct> productList = new ArrayList<>();
        HashMap<Long, List<Integer>> invMap = new HashMap();
        List<IProduct> pkgList = new ArrayList<>();
        for (Long pkgno : pkgnoSet) {
            Package pkg = new Package();
            pkg.setNums(1);
            pkg.setPackageId(pkgno);
            pkgList.add(pkg);
        }

        getActivityList(compid, pkgList);


        for (ShoppingCartDTO shoppingCartDTO : shoppingCartDTOS) {
            if (Long.parseLong(shoppingCartDTO.getPkgno()) == 0) {
                Product product = new Product();
                List<Integer> stockList = new ArrayList<>();
                invMap.put(shoppingCartDTO.getPdno(), stockList);


                for (Object[] objects : querySkuRet) {
                    if (shoppingCartDTO.getPdno() == Long.parseLong(objects[3].toString())
                            && Long.parseLong(shoppingCartDTO.getPkgno()) == 0) {
                        int stock = Integer.parseInt(objects[0].toString());
                        try {
                            stock = RedisStockUtil.getStock(shoppingCartDTO.getPdno());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        invMap.get(shoppingCartDTO.getPdno()).
                                add(stock);
                        product.setOriginalPrice(Double.parseDouble(objects[2].toString()));
                    }
                }


                for (Object[] objects : queryRet) {
                    //判断
                    if (Long.parseLong(shoppingCartDTO.getPkgno()) == 0 &&
                            shoppingCartDTO.getPdno() == Long.parseLong(objects[1].toString())
                            && Long.parseLong(shoppingCartDTO.getPkgno()) == Long.parseLong(objects[3].toString())) {

                        shoppingCartDTO.setPnum(shoppingCartDTO.getPnum()
                                + Integer.parseInt(objects[2].toString()));

                        product.setNums(shoppingCartDTO.getPnum()
                                + Integer.parseInt(objects[2].toString()));
                    }
                    product.setNums(shoppingCartDTO.getPnum());
                    product.autoSetCurrentPrice(product.getOriginalPrice(), product.getNums());
                    product.setSku(shoppingCartDTO.getPdno());
                    productList.add(product);
                }
            } else {
                for (Object[] objects : queryRet) {
                    if (Long.parseLong(shoppingCartDTO.getPkgno()) > 0
                            && shoppingCartDTO.getPdno() == Long.parseLong(objects[1].toString())
                            && Long.parseLong(shoppingCartDTO.getPkgno()) == Long.parseLong(objects[3].toString())) {

                        shoppingCartDTO.setPnum(shoppingCartDTO.getPnum()
                                + Integer.parseInt(objects[2].toString()));
                    }
                }
            }
        }

        //计算套餐数量
        for (Long pkgno : pkgnoSet) {
            int pkgDtoNum = 0;
            int pkgnum = 0;
            for (ShoppingCartDTO shoppingCartDTO : shoppingCartDTOS) {
                if (pkgno == Long.parseLong(shoppingCartDTO.getPkgno())) {
                    pkgDtoNum = pkgDtoNum + shoppingCartDTO.getPnum();
                }
            }

            for (IProduct product : pkgList) {
                //计算当前套餐总件数
                Package pkg = (Package) product;
                if (pkgno == pkg.getPackageId() && pkg.getExpireFlag() >= 0
                        &&  ((Package) product).singleProdsCount() > 0) {
                    pkgnum = pkgDtoNum
                            / ((Package) product).singleProdsCount();
                }
            }
            Package pkg = new Package();
            pkg.setPackageId(pkgno);
            pkg.setNums(pkgnum);
            productList.add(pkg);
        }

        List<IDiscount> discountList = getActivityList(compid, productList);


        for (ShoppingCartDTO shoppingCartDTO : shoppingCartDTOS) {
            int subStock = Integer.MAX_VALUE;
            int actStock = Integer.MAX_VALUE;
            if (discountList != null && !discountList.isEmpty()) {
                for (IDiscount discount : discountList) {
                    Activity activity = (Activity) discount;
                    for (IProduct prdt : activity.getProductList()) {
                        if (prdt instanceof Package
                                && Long.parseLong(shoppingCartDTO.getPkgno()) == ((Package) prdt).getPackageId()){
                            if (((Package) prdt).getExpireFlag() < 0) {
                                shoppingCartDTO.setCstatus(3);
                            }
                        }


                        if (prdt instanceof Product
                                && prdt.getSKU() == shoppingCartDTO.getPdno()) {
                                actStock = Math.min(
                                        RedisStockUtil
                                                .getActStockBySkuAndActno(shoppingCartDTO.getPdno(),
                                                        discount.getDiscountNo()),
                                        actStock);

                                if (activity.getLimits(shoppingCartDTO.getPdno()) == 0) {
                                    break;
                                }

                                subStock = Math.min
                                        (activity.getLimits(shoppingCartDTO.getPdno())
                                                - RedisOrderUtil.getActBuyNum(compid, shoppingCartDTO.getPdno(),
                                                activity.getUnqid()), subStock);

                        }
                    }
                }
            }

            if(Long.parseLong(shoppingCartDTO.getPkgno()) == 0){
                invMap.get(shoppingCartDTO.getPdno()).
                        add(subStock);

                invMap.get(shoppingCartDTO.getPdno()).
                        add(actStock);

            }

        }

        for (ShoppingCartDTO shoppingCartDTO : shoppingCartDTOS) {
            if (invMap.containsKey(shoppingCartDTO.getPdno())
                    && Long.parseLong(shoppingCartDTO.getPkgno()) == 0) {
                List<Integer> stockList = invMap.get(shoppingCartDTO.getPdno());
                if (!stockList.isEmpty()) {
                    int minStock = Collections.min(stockList);
                    if (shoppingCartDTO.getPnum() > minStock) {
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
     * @传参列表 pdno - 商品码
     * pnum - 商品数量
     * compid - 买家企业码
     * @返回列表 code - 200 成功/ -1 失败
     * message - 成功/失败
     **/
    @UserPermission(ignore = true)
    public Result clearShopCart(AppContext appContext) {
        String json = appContext.param.json;
        Result result = new Result();

        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int compid = jsonObject.get("compid").getAsInt();
        String ids = jsonObject.get("ids").getAsString();

        String pkgids = "";
        if(jsonObject.has("pkgids")){
            pkgids = jsonObject.get("pkgids").getAsString();
        }

        if (StringUtils.isEmpty(ids) && StringUtils.isEmpty(pkgids)) {
            return result.fail("操作失败");
        }
        String[] idArry = null;
        String[] pkgArry = null;
        if (!StringUtils.isEmpty(ids)) {
            idArry = ids.split(",");
        }

        if (!StringUtils.isEmpty(pkgids)) {
            pkgArry = pkgids.split(",");
        }


        boolean ret = delShoppingCart(idArry, pkgArry, compid);
        if (ret) {
            return result.success("操作成功");
        }
        return result.fail("操作失败");
    }


    /**
     * @接口摘要 查询购物车列表（未选中）
     * @业务场景 进入购物车页面
     * @传参类型 JSON
     * @传参列表 compid - 买家企业码
     * @返回列表 pdno - 商品码
     * pnum - 商品数量
     * compid - 买家企业码
     * ptitle - 商品标题
     * spec - 商品规格
     * verdor - 厂商
     * vperiod - 有效期
     * brand - 品牌
     * limitnum - 限购量
     * inventory - 库存量
     * medpacknum - 中包装
     **/
    @UserPermission(ignore = true)
    public Result queryUnCheckShopCartList(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        Result result = new Result();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        if (!jsonObject.has("compid")) {
            return result.success(null);
        }

        int compid = jsonObject.get("compid").getAsInt();

        if (compid <= 0) {
            return result.success(null);
        }

        List<Object[]> queryResult = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(), QUERY_SHOPCART_SQL, compid);
        ShoppingCartDTO[] shoppingCartVOS = new ShoppingCartDTO[queryResult.size()];
        if (queryResult == null || queryResult.isEmpty()) {
            return result.success(shoppingCartVOS);
        }
        baseDao.convToEntity(queryResult, shoppingCartVOS, ShoppingCartDTO.class,
                new String[]{"unqid", "pdno", "compid", "cstatus", "pnum", "pkgno"});

        List<ShoppingCartVO> shopCart = getShopCart(new ArrayList<>(Arrays.asList(shoppingCartVOS)));


        //TODO 获取活动匹配
        return result.success(convUnckResult(shopCart, compid));
    }

    private List<ShoppingCartVO> convUnckResult(List<ShoppingCartVO> shopCart, int compid) {

        List<ShoppingCartVO> shopCartGoodsList = new ArrayList<>();
        Set<String> pkgnoSet = new HashSet<>();
        for (ShoppingCartVO shoppingCartVO : shopCart) {
            if (Long.parseLong(shoppingCartVO.getPkgno()) == 0) {
                shopCartGoodsList.add(shoppingCartVO);
            } else {
                pkgnoSet.add(shoppingCartVO.getPkgno());
            }
        }

        if (!pkgnoSet.isEmpty()) {
            convertUnckPkgList(shopCartGoodsList, shopCart, pkgnoSet, compid);
            shopCart = shopCartGoodsList;
        }

        List<IProduct> productList = new ArrayList<>();
        for (ShoppingCartVO shoppingCartVO : shopCart) {
            Package pkg = null;
            Product product = null;
            if (Long.parseLong(shoppingCartVO.getPkgno()) > 0) {
                pkg = new Package();
                pkg.setPackageId(Long.parseLong(shoppingCartVO.getPkgno()));
                pkg.setNums(shoppingCartVO.getPkgnum());
                productList.add(pkg);
            } else {
                product = new Product();
                product.setSku(shoppingCartVO.getPdno());
                product.autoSetCurrentPrice(shoppingCartVO.getPdprice(), shoppingCartVO.getNum());
                productList.add(product);
            }
        }

        DiscountResult discountResult
                = CalculateUtil.calculate(compid, productList, Long.parseLong(shopCart.get(0).getConpno()));

        //获取活动
        for (ShoppingCartVO shoppingCartVO : shopCart) {
            List<DiscountRule> ruleList = new ArrayList<>();
            List<String> actCodeList = new ArrayList<>();
            int minLimit = Integer.MAX_VALUE;
            int subStock = Integer.MAX_VALUE;
            int actStock = Integer.MAX_VALUE;
            int pkgStock = Integer.MAX_VALUE;
            int skuStock = Integer.MAX_VALUE;
            for (IDiscount discount : discountResult.getActivityList()) {
                Activity activity = (Activity) discount;
                int brule = (int) discount.getBRule();
              //  actCodeList.add(activity.getUnqid() + "");
                List<IProduct> pList = discount.getProductList();
                // long codno;
                for (IProduct product : pList) {
                    if (product instanceof Package
                            && Long.parseLong(shoppingCartVO.getPkgno()) == ((Package) product).getPackageId()) {
                        //判断库存
                        if (((Package) product).getExpireFlag() < 0) {
                            shoppingCartVO.setStatus(3);
                        }


                       // shoppingCartVO.setActcode(listCntRemove(product.getActivityCodes()));
                        if(activity.getBRule() == 1114){
                            pkgStock = Math.min(getPkgInv((Package) product, activity, compid), pkgStock);
                        }

                        shoppingCartVO.setInventory(pkgStock);
                        LogUtil.getDefaultLogger().debug(((Package) product).getPackageId()+"最新套餐库存："+pkgStock);
                        DiscountRule discountRule = new DiscountRule();
                        discountRule.setRulecode(brule);
                        discountRule.setRulename(DiscountRuleStore.getRuleByName(brule));

                        //shoppingCartVO.setLimitsub(subStock);
                       // shoppingCartVO.setInventory(skuStock);
                       // shoppingCartVO.setActstock(actStock);

                        if (activity.getLimits(Long.parseLong(shoppingCartVO.getPkgno())) == 0) {
                            break;
                        }
                        minLimit = Math.min
                                (activity.getLimits(((Package) product).getPackageId())
                                        , minLimit);


                    }

                    if (product instanceof Product
                            && product.getSKU() == shoppingCartVO.getPdno()) {
                        //codno = product.getSKU();
                        if (pList.size() == 1) {
                            shoppingCartVO.addCurrLadDesc(brule, activity.getCurrentLadoffDesc());
                            shoppingCartVO.addNextLadDesc(brule, activity.getNextLadoffDesc());
                        }
                       // shoppingCartVO.setActcode(listCntRemove(product.getActivityCodes()));
                        DiscountRule discountRule = new DiscountRule();
                        discountRule.setRulecode(brule);
                        discountRule.setRulename(DiscountRuleStore.getRuleByName(brule));

//                        if(activity.getLimits(product.getSKU()) < minLimit){
//                            minLimit = activity.getLimits(product.getSKU());
//                        }
                        //判断秒杀
                        if (brule == 1113) {
                            shoppingCartVO.setSkprice(product.getOriginalPrice());
                            shoppingCartVO.setStatus(1);
                        }
                        ruleList.add(discountRule);


                        actStock = Math.min(
                                RedisStockUtil.getActStockBySkuAndActno(shoppingCartVO.getPdno(),
                                        discount.getDiscountNo()), actStock);


                        skuStock = shoppingCartVO.getInventory();
                        try {
                            skuStock = RedisStockUtil.getStock(shoppingCartVO.getPdno());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (skuStock == 0) {
                            shoppingCartVO.setStatus(3);
                        }
                        //actCodeList.add(activity.getUnqid() + "");
                        shoppingCartVO.setLimitsub(subStock);
                        shoppingCartVO.setInventory(skuStock);
                        shoppingCartVO.setActstock(actStock);

                        if (activity.getLimits(shoppingCartVO.getPdno()) == 0) {
                            break;
                        }

                        subStock = Math.min
                                (activity.getLimits(shoppingCartVO.getPdno())
                                        - RedisOrderUtil.getActBuyNum(compid, shoppingCartVO.getPdno(),
                                        activity.getUnqid()), subStock);

                        minLimit = Math.min
                                (activity.getLimits(shoppingCartVO.getPdno())
                                        , minLimit);

                    }

                }

            }
            shoppingCartVO.setLimitnum(minLimit);
            if (minLimit == Integer.MAX_VALUE) {
                shoppingCartVO.setLimitnum(0);
            }

            shoppingCartVO.setRule(ruleList);
        }
        return shopCart;
    }

    private void convertUnckPkgList(List<ShoppingCartVO> shopCartGoodsList,
                                    List<ShoppingCartVO> shopCart,
                                    Set<String> pkgnoSet,
                                    int compid) {
        //套餐商品明细总数量
        HashMap<String, Integer> pkgMap = new HashMap<>();
        for (String pkgno : pkgnoSet) {
            int pdnum = 0;
            for (ShoppingCartVO shoppingCartVO : shopCart) {
                if (pkgno.equals(shoppingCartVO.getPkgno())) {
                    pdnum = pdnum + shoppingCartVO.getNum();
                }
            }
            pkgMap.put(pkgno, pdnum);
        }


        //套餐件数
        HashMap<String, Integer> pkgNumMap = new HashMap<>();
        if (!pkgMap.isEmpty()) {
            List<IProduct> pkgNoList = new ArrayList<>();
            for (String key : pkgMap.keySet()) {
                Package pkg = new Package();
                pkg.setNums(1);
                pkg.setPackageId(Long.parseLong(key));
                pkgNoList.add(pkg);
            }

            DiscountResult discountResult
                    = CalculateUtil.calculate(compid, pkgNoList,
                    0);

            //套餐商品明细总数量换算套餐件数
            for (IDiscount discount : discountResult.getActivityList()) {
                Activity activity = (Activity) discount;
                if(activity.isGlobalActivity()){
                   continue;
                }
                List<IProduct> productList = activity.getProductList();
                for (int i = 0; i < productList.size(); i++) {
                    if (productList.get(i) instanceof Package) {
                        Package pkg = (Package) productList.get(i);
                        int pkgNum = pkgMap.get(pkg.getPackageId() + "") / pkg.singleProdsCount();
                        pkgNumMap.put(pkg.getPackageId() + "", pkgNum);
                    }
                }
            }
        }


        //套餐件数
        if (!pkgNumMap.isEmpty()) {
            List<IProduct> pkgNoList = new ArrayList<>();
            for (String key : pkgNumMap.keySet()) {
                Package pkg = new Package();
                pkg.setNums(pkgNumMap.get(key));
                pkg.setPackageId(Long.parseLong(key));
                pkgNoList.add(pkg);
            }

            DiscountResult discountResult
                    = CalculateUtil.calculate(compid, pkgNoList,
                    0);

            //套餐计算总数量换算成件数
            for (IDiscount discount : discountResult.getActivityList()) {
                Activity activity = (Activity) discount;
                if(activity.isGlobalActivity()){
                    continue;
                }
                List<IProduct> productList = activity.getProductList();
                for (int i = 0; i < productList.size(); i++) {
                    if (productList.get(i) instanceof Package) {
                        Package pkg = (Package) productList.get(i);
                        ShoppingCartVO shoppingCartVO = new ShoppingCartVO();
                        shoppingCartVO.setPkgnum(pkg.getNums());
                        shoppingCartVO.setPkgno(pkg.getPackageId() + "");
                        shoppingCartVO.setPdprice(pkg.getOriginalPrice());
                        shoppingCartVO.setSubtotal(pkg.getCurrentPrice());
                        double dsprice = MathUtil.exactDiv(pkg.getCurrentPrice(), pkg.getNums()).
                                setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                        shoppingCartVO.setPkgprice(dsprice);
                        shoppingCartVO.setDiscount(pkg.getDiscounted());
                        List<PkgProduct> pkgProducts = new ArrayList<>();
                        for (Product product : pkg.getPacageProdList()) {
                            for (ShoppingCartVO spcvo : shopCart) {
                                if (Long.parseLong(spcvo.getPkgno()) == pkg.getPackageId()
                                        && spcvo.getPdno() == product.getSku()) {
                                    PkgProduct pkgProduct = new PkgProduct();
                                    pkgProduct.setPdno(spcvo.getPdno());
                                    if (pkg.getNums() == 0) {
                                        pkgProduct.setNum(0);
                                    } else {
                                        pkgProduct.setNum(product.getNums() / pkg.getNums());
                                    }

                                    double pdprice = MathUtil.exactDiv(product.getCurrentPrice(), product.getNums()).
                                            setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                                    pkgProduct.setPdprice(pdprice);
                                    pkgProduct.setOpdprice(product.getOriginalPrice());
                                    pkgProduct.setApdprice(product.getCurrentPrice());
                                    pkgProduct.setPtitle(spcvo.getPtitle());
                                    pkgProduct.setSpec(spcvo.getSpec());
                                    pkgProduct.setVerdor(spcvo.getVerdor());
                                    pkgProduct.setVperiod(spcvo.getVperiod());
                                    pkgProducts.add(pkgProduct);
                                    shoppingCartVO.setChecked(spcvo.getChecked());
                                    break;
                                }
                            }
                        }

                        shoppingCartVO.setPkgList(pkgProducts);
                        shopCartGoodsList.add(shoppingCartVO);
                    }
                }
            }
        }

    }

    /**
     * @接口摘要 查询购物车列表（选中）
     * @业务场景 购物车页面，购物车商品数量增加操作
     * @传参类型 JSONARRAY
     * @传参列表 pdno - 商品码
     * pnum - 商品数量
     * compid - 买家企业码
     * checked - 是否选中：0 未选中，1 选中
     * pdprice - 商品单价
     * @返回列表 pdno - 商品码
     * pnum - 商品数量
     * compid - 买家企业码
     * ptitle - 商品标题
     * spec - 商品规格
     * verdor - 厂商
     * vperiod - 有效期
     * brand - 品牌
     * limitnum - 限购量
     * inventory - 库存量
     * medpacknum - 中包装
     **/
    @UserPermission(ignore = true)
    public Result queryCheckShopCartList(AppContext appContext) {
        String json = appContext.param.json;
        Result result = new Result();
        JsonParser jsonParser = new JsonParser();
        JsonArray jsonArray = jsonParser.parse(json).getAsJsonArray();
        List<ShoppingCartDTO> shoppingCartDTOS = new ArrayList<>();
        Gson gson = new Gson();
        List<Object[]> updateParm = new ArrayList<>();
        List<String> sqlList = new ArrayList<>();
        for (JsonElement shopVO : jsonArray) {
            ShoppingCartDTO shoppingCartDTO = gson.fromJson(shopVO, ShoppingCartDTO.class);
            shoppingCartDTOS.add(shoppingCartDTO);
            if (shoppingCartDTO.getChecked() == 1) {
                if (Long.parseLong(shoppingCartDTO.getPkgno()) == 0) {
                    sqlList.add(UPDATE_SHOPCART_SQL_NUM);
                    updateParm.add(new Object[]{shoppingCartDTO.getPnum(), shoppingCartDTO.getUnqid()});
                }
            }
        }
        //判断套餐
        List<ShoppingCartDTO> pkgDTOList = getPkgDTO(shoppingCartDTOS);
        List<ShoppingCartDTO> pkgEfDTOList = getEffectivePkgDto(pkgDTOList);

        if (pkgEfDTOList != null && !pkgEfDTOList.isEmpty()) {
            for (ShoppingCartDTO pkgDTO : pkgEfDTOList) {
                sqlList.add(UPDATE_SHOPCART_PKG_SQL_EXT);
                updateParm.add(new Object[]{pkgDTO.getPnum(), pkgDTO.getPdno(),
                        pkgDTO.getCompid(), pkgDTO.getPkgno()});
            }
        }
        //更新购物车数量
        String[] sqlArray = new String[sqlList.size()];
        sqlArray = sqlList.toArray(sqlArray);
        if (sqlArray.length > 0) {
            baseDao.updateTransNativeSharding(shoppingCartDTOS.get(0).getCompid(),
                    TimeUtils.getCurrentYear(),
                    sqlArray, updateParm);
        }

        //List<ShoppingCartVO> shopCart = getShopCart(shoppingCartDTOS);

        List<ShoppingCartVO> shopCart = getShopCart(cnvShoppingCartDtos(pkgDTOList, shoppingCartDTOS));
        return result.success(convResult(shopCart,
                shoppingCartDTOS.get(0).getCompid(),shoppingCartDTOS.get(0).getAreano()));
    }

    private List<ShoppingCartDTO> cnvShoppingCartDtos
            (List<ShoppingCartDTO> pkgDTOList,
             List<ShoppingCartDTO> shoppingCartDTOS) {
        List<ShoppingCartDTO> newDtoList = new ArrayList<>();

        for (ShoppingCartDTO shoppingCartDTO : shoppingCartDTOS) {
            if (shoppingCartDTO.getPdno() > 0) {
                newDtoList.add(shoppingCartDTO);
            }
        }
        newDtoList.addAll(pkgDTOList);
        return newDtoList;
    }

    private List<ShoppingCartDTO> getEffectivePkgDto(List<ShoppingCartDTO> shopDtos) {
        if (shopDtos == null || shopDtos.isEmpty()) {
            return null;
        }
        List<ShoppingCartDTO> cartDTOList = new ArrayList<>();
        HashSet<String> pkgSet = new HashSet<>();

        for (ShoppingCartDTO pkgDTO : shopDtos) {
            if (pkgDTO.getChecked() == 0 || pkgDTO.getCstatus() != 0) {
                pkgSet.add(pkgDTO.getPkgno());
            }
        }


        for (ShoppingCartDTO shoppingCartDTO : shopDtos) {
            if (pkgSet.contains(shoppingCartDTO.getPkgno())) {
                continue;
            } else {
                cartDTOList.add(shoppingCartDTO);
            }
        }

        return cartDTOList;
    }


    private List<ShoppingCartVO> getCartSkus(String ids) {
        if (StringUtils.isEmpty(ids)) {
            return null;
        }
        //远程调用
        StringBuilder sql = new StringBuilder(QUERY_PROD_BASE);
        sql.append(" AND sku.sku IN (");
        sql.append(ids);
        sql.append(") ");
        //远程调用
        List<Object[]> queryResult = IceRemoteUtil.queryNative(sql.toString());

        if (queryResult == null || queryResult.isEmpty()) {
            return null;
        }
        for (Object[] objs : queryResult) {
            Application.communicator().getLogger().print("Object[]:" + Arrays.toString(objs));
        }
        ShoppingCartVO[] returnResults = new ShoppingCartVO[queryResult.size()];
        baseDao.convToEntity(queryResult, returnResults, ShoppingCartVO.class,
                new String[]{"ptitle", "verdor", "pdno", "pdprice", "vperiod", "inventory",
                        "spec", "pstatus", "spu", "limitnum", "brand", "medpacknum", "unit", "mp", "busscope", "consell"});
        return new ArrayList<>(Arrays.asList(returnResults));
    }

    private List<ShoppingCartVO> getShopCart(List<ShoppingCartDTO> shoppingCartDTOS) {
        StringBuilder ids = new StringBuilder();
        List<ShoppingCartVO> shopVOList = new ArrayList<>();
        for (int i = 0; i < shoppingCartDTOS.size(); i++) {
            if (shoppingCartDTOS.get(i).getPdno() == 0) {
                continue;
            }
            ids.append(shoppingCartDTOS.get(i).getPdno());
            ids.append(",");
        }
        String idstmp = ids.toString();
        if (idstmp.endsWith(",")) {
            idstmp = idstmp.substring(0, idstmp.length() - 1);
        }

        List<ShoppingCartVO> shoppingCartList = getCartSkus(idstmp);
        if (shoppingCartList == null) {
            return null;
        }

        for (ShoppingCartDTO shoppingCartDTO : shoppingCartDTOS) {
            for (ShoppingCartVO shoppingCartVO : shoppingCartList) {
                if (shoppingCartVO.getPdno() == shoppingCartDTO.getPdno()) {
                    ShoppingCartVO nShoppingCartVO = new ShoppingCartVO();
                    nShoppingCartVO.setNum(shoppingCartDTO.getPnum());
                    nShoppingCartVO.setChecked(shoppingCartDTO.getChecked());
                    nShoppingCartVO.setUnqid(shoppingCartDTO.getUnqid());
                    nShoppingCartVO.setConpno(shoppingCartDTO.getConpno() + "");
                    nShoppingCartVO.setAreano(shoppingCartDTO.getAreano());
                    nShoppingCartVO.setPkgno(shoppingCartDTO.getPkgno());
                    nShoppingCartVO.setPkgnum(shoppingCartDTO.getPkgnum());
                    nShoppingCartVO.setPtitle(shoppingCartVO.getPtitle());
                    nShoppingCartVO.setVerdor(shoppingCartVO.getVerdor());
                    nShoppingCartVO.setPdno(shoppingCartVO.getPdno());
                    nShoppingCartVO.setPdprice(shoppingCartVO.getPdprice());
                    nShoppingCartVO.setVperiod(shoppingCartVO.getVperiod());
                    nShoppingCartVO.setInventory(shoppingCartVO.getInventory());
                    nShoppingCartVO.setSpec(shoppingCartVO.getSpec());
                    nShoppingCartVO.setPstatus(shoppingCartVO.getPstatus());
                    nShoppingCartVO.setSpu(shoppingCartVO.getSpu());
                    nShoppingCartVO.setLimitnum(shoppingCartVO.getLimitnum());
                    nShoppingCartVO.setBrand(shoppingCartVO.getBrand());
                    nShoppingCartVO.setMedpacknum(shoppingCartVO.getMedpacknum());
                    nShoppingCartVO.setUnit(shoppingCartVO.getUnit());
                    nShoppingCartVO.setMp(shoppingCartVO.getMp());
                    nShoppingCartVO.setConsell(shoppingCartVO.getConsell());
                    nShoppingCartVO.setBusscope(shoppingCartVO.getBusscope());
                    if (shoppingCartVO.getPstatus() == 0) {
                        nShoppingCartVO.setStatus(2);
                    }
                    shopVOList.add(nShoppingCartVO);
                    break;
                }
            }
        }
        return shopVOList;
    }

    private List<Gift> getGifts(List<ShoppingCartVO> shoppingCartList, int compid) {
        if (shoppingCartList == null) {
            return Collections.emptyList();
        }

        List<IProduct> productList = new ArrayList<>();
        List<IProduct> ckProduct = new ArrayList<>();
        BigDecimal result = BigDecimal.ZERO;
        for (ShoppingCartVO shoppingCartVO : shoppingCartList) {
            if (Long.parseLong(shoppingCartVO.getPkgno()) > 0) {
                Package pkg = new Package();
                pkg.setPackageId(Long.parseLong(shoppingCartVO.getPkgno()));
                pkg.setNums(shoppingCartVO.getPkgnum());
                if (shoppingCartVO.getChecked() == 1) {
                    ckProduct.add(pkg);
                    result.add(BigDecimal.valueOf(pkg.getCurrentPrice()));
                }
                productList.add(pkg);
            } else {
                Product product = new Product();
                product.setSku(shoppingCartVO.getPdno());
                product.autoSetCurrentPrice(shoppingCartVO.getPdprice(), shoppingCartVO.getNum());
                if (shoppingCartVO.getChecked() == 1) {
                    ckProduct.add(product);
                    result.add(BigDecimal.valueOf(product.getCurrentPrice()));
                }
                productList.add(product);
            }
        }

        List<IDiscount> discountList = getActivityList(compid, productList);

        //获取活动

        for (ShoppingCartVO shoppingCartVO : shoppingCartList) {
            List<DiscountRule> ruleList = new ArrayList<>();
            List<String> actCodeList = new ArrayList<>();
            int minLimit = Integer.MAX_VALUE;
            int subStock = Integer.MAX_VALUE;
            int actStock = Integer.MAX_VALUE;
            int stock = Integer.MAX_VALUE;
            for (IDiscount discount : discountList) {
                Activity activity = (Activity) discount;
                int brule = (int) discount.getBRule();
               // actCodeList.add(activity.getUnqid() + "");
                List<IProduct> pList = discount.getProductList();
                for (IProduct product : pList) {
                    if (product instanceof Package
                            && Long.parseLong(shoppingCartVO.getPkgno()) == ((Package) product).getPackageId()) {
                        shoppingCartVO.setExCoupon(shoppingCartVO.isExCoupon() || activity.getExCoupon());
                        //判断库存
                        if (((Package) product).getExpireFlag() < 0) {
                            shoppingCartVO.setStatus(3);
                        }
                        shoppingCartVO.setActcode(new ArrayList<>(product.getActivityCodes()));
                        if(activity.getBRule() == 1114){
                            stock = Math.min(getPkgInv((Package) product, activity, compid), stock);
                        }
                        shoppingCartVO.setInventory(stock);
                        minLimit = Math.min
                                (activity.getLimits(((Package) product).getPackageId())
                                        , minLimit);

                    }

                    if (product instanceof Product
                            && product.getSKU() == shoppingCartVO.getPdno()) {
                        LogUtil.getDefaultLogger().info(
                                "PNO -> " + product.getSKU() + "\n" +
                                        JSON.toJSONString(activity));

                        shoppingCartVO.setActcode(new ArrayList<>(product.getActivityCodes()));
                        shoppingCartVO.setExCoupon(shoppingCartVO.isExCoupon() || activity.getExCoupon());
                        if (pList.size() == 1) {
                            shoppingCartVO.addCurrLadDesc(brule, activity.getCurrentLadoffDesc());
                            shoppingCartVO.addNextLadDesc(brule, activity.getNextLadoffDesc());
                        }

                        DiscountRule discountRule = new DiscountRule();
                        discountRule.setRulecode(brule);
                        discountRule.setRulename(DiscountRuleStore.getRuleByName(brule));
                        if (activity.getLimits(product.getSKU()) < minLimit) {
                            minLimit = activity.getLimits(product.getSKU());
                        }
                        //判断秒杀
                        if (brule == 1113) {
                            shoppingCartVO.setSkprice(product.getOriginalPrice());
                            shoppingCartVO.setStatus(1);
                        }
                        ruleList.add(discountRule);


                        actStock = Math.min(
                                RedisStockUtil
                                        .getActStockBySkuAndActno(shoppingCartVO.getPdno(),
                                                discount.getDiscountNo()),
                                actStock);

                        if (activity.getLimits(shoppingCartVO.getPdno()) == 0) {
                            break;
                        }

                        subStock = Math.min
                                (activity.getLimits(shoppingCartVO.getPdno())
                                        - RedisOrderUtil.getActBuyNum(compid, shoppingCartVO.getPdno(),
                                        activity.getUnqid()), subStock);

                        minLimit = Math.min
                                (activity.getLimits(shoppingCartVO.getPdno())
                                        , minLimit);


                        stock = shoppingCartVO.getInventory();
                        try {
                            stock = RedisStockUtil.getStock(shoppingCartVO.getPdno());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (stock == 0) {
                            shoppingCartVO.setStatus(3);
                        }

                        shoppingCartVO.setLimitsub(subStock);
                        shoppingCartVO.setInventory(stock);
                        shoppingCartVO.setActstock(actStock);
                    }
                }
            }
            shoppingCartVO.setLimitnum(minLimit);
            if (minLimit == Integer.MAX_VALUE) {
                shoppingCartVO.setLimitnum(0);
            }
            shoppingCartVO.setRule(ruleList);
        }

        for(IProduct product : ckProduct){
            if (product instanceof Package) {
                ((Package) product).getPacageProdList().clear();
            }
        }


        DiscountResult discountResult
                = CalculateUtil.calculate(compid, ckProduct,
                Long.parseLong(shoppingCartList.get(0).getConpno()));

//        for (ShoppingCartVO shoppingCartVO : shoppingCartList) {
//            for (IProduct product : ckProduct) {
//                if (shoppingCartVO.getChecked() == 1) {
//                    Package pkg = null;
//                    if (product instanceof Package) {
//                        pkg = (Package) product;
//                    }
//                    if ((product instanceof Product && shoppingCartVO.getPdno() == product.getSKU())
//                            || (pkg != null && pkg.getPackageId() == Long.parseLong(shoppingCartVO.getPkgno()))) {
//                        shoppingCartVO.setSubtotal(product.getCurrentPrice());
//                        shoppingCartVO.setDiscount(product.getDiscounted());
//                        shoppingCartVO.setAmt(discountResult.getTotalDiscount());
//                        shoppingCartVO.setAcamt(discountResult.getTotalCurrentPrice());
//                        shoppingCartVO.setCounpon(discountResult.getCouponValue());
//                        shoppingCartVO.setFreepost(discountResult.isFreeShipping());
//                        shoppingCartVO.setOflag(discountResult.isExCoupon());
//                        shoppingCartVO.setTotalamt(result.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//                        shoppingCartVO.setoSubtotal(MathUtil.exactMul(product.getOriginalPrice(), product.getNums()).
//                                setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//                        if (shoppingCartVO.getAreano() > 0 && !discountResult.isFreeShipping()) {
//                            shoppingCartVO.setFreight(AreaFeeUtil.getFee(shoppingCartVO.getAreano()));
//                        }
//                    }
//                }
//            }
//        }

        return discountResult.getGiftList();
    }

    private List<ShoppingCartVO> convResult(List<ShoppingCartVO> shopCart, int compid,long areano) {
        List<ShoppingCartVO> shopCartGoodsList = new ArrayList<>();
        Set<String> pkgnoSet = new HashSet<>();
        for (ShoppingCartVO shoppingCartVO : shopCart) {
            if (Long.parseLong(shoppingCartVO.getPkgno()) == 0) {
                shopCartGoodsList.add(shoppingCartVO);
            } else {
                pkgnoSet.add(shoppingCartVO.getPkgno());
            }
        }

//        if(!pkgnoSet.isEmpty()){
//            convertUnckPkgList(shopCartGoodsList,shopCart,pkgnoSet,compid);
//            convertPkgList(shopCart,shopCartGoodsList,pkgnoSet);
//            shopCart = shopCartGoodsList;
//        }

        if (!pkgnoSet.isEmpty()) {
            convertPkgList(shopCart, shopCartGoodsList, pkgnoSet);
            shopCart = shopCartGoodsList;
        }

        List<IProduct> productList = new ArrayList<>();
        List<IProduct> ckProduct = new ArrayList<>();
        List<IProduct> pkgProductList = new ArrayList<>();
        BigDecimal result = BigDecimal.ZERO;
        for (ShoppingCartVO shoppingCartVO : shopCart) {
            Package pkg = null;
            Product product = null;
            if (Long.parseLong(shoppingCartVO.getPkgno()) > 0) {
                pkg = new Package();
                pkg.setPackageId(Long.parseLong(shoppingCartVO.getPkgno()));
                pkg.setNums(shoppingCartVO.getPkgnum());
                productList.add(pkg);
                pkgProductList.add(pkg);
            } else {
                product = new Product();
                product.setSku(shoppingCartVO.getPdno());
                product.autoSetCurrentPrice(shoppingCartVO.getPdprice(), shoppingCartVO.getNum());
                productList.add(product);
            }

            if (shoppingCartVO.getChecked() == 1) {
                if (product != null) {
                    result = result.add(BigDecimal.valueOf(product.getCurrentPrice()));
                }
            }
        }

        DiscountResult prdDiscountResult
                = CalculateUtil.calculate(compid, productList, Long.parseLong(shopCart.get(0).getConpno()));


        for (ShoppingCartVO shoppingCartVO : shopCart) {
            List<DiscountRule> ruleList = new ArrayList<>();
            List<String> actCodeList = new ArrayList<>();
            int minLimit = Integer.MAX_VALUE;
            int subStock = Integer.MAX_VALUE;
            int actStock = Integer.MAX_VALUE;
            int pkgStock = Integer.MAX_VALUE;
            int skuStock = Integer.MAX_VALUE;
            for (IDiscount discount : prdDiscountResult.getActivityList()) {
                Activity activity = (Activity) discount;
                int brule = (int) discount.getBRule();
                List<IProduct> pList = discount.getProductList();
                //actCodeList.add(activity.getUnqid() + "");
                for (IProduct product : pList) {
                    if (product instanceof Package
                            && Long.parseLong(shoppingCartVO.getPkgno()) == ((Package) product).getPackageId()) {
                        shoppingCartVO.setExCoupon(shoppingCartVO.isExCoupon() || activity.getExCoupon());
                        //添加活动码
                     //   actCodeList.add(activity.getUnqid() + "");
                       // shoppingCartVO.setActcode(listCntRemove(product.getActivityCodes()));
                        //判断库存
                        if (((Package) product).getExpireFlag() < 0) {
                            shoppingCartVO.setStatus(3);
                        }
                        DiscountRule discountRule = new DiscountRule();
                        discountRule.setRulecode(brule);
                        discountRule.setRulename(DiscountRuleStore.getRuleByName(brule));

                        if(activity.getBRule() == 1114){
                            pkgStock = Math.min(getPkgInv((Package) product, activity, compid), pkgStock);
                        }
                        //pkgStock = Math.min(getPkgInv((Package) product, activity, compid), pkgStock);
                        shoppingCartVO.setInventory(pkgStock);
                        if (activity.getLimits(Long.parseLong(shoppingCartVO.getPkgno())) == 0) {
                            break;
                        }

                        minLimit = Math.min
                                (activity.getLimits(((Package) product).getPackageId())
                                        , minLimit);

                    }

                    if (product instanceof Product
                            && product.getSKU() == shoppingCartVO.getPdno()) {
                        LogUtil.getDefaultLogger().info(
                                "PNO -> " + product.getSKU() + "\n" +
                                        JSON.toJSONString(activity));
                       // shoppingCartVO.setActcode(product.getActivityCodes());
                        shoppingCartVO.setExCoupon(shoppingCartVO.isExCoupon() || activity.getExCoupon());
                       // shoppingCartVO.setActcode(listCntRemove(product.getActivityCodes()));
                        if (pList.size() == 1) {
                            shoppingCartVO.addCurrLadDesc(brule, activity.getCurrentLadoffDesc());
                            shoppingCartVO.addNextLadDesc(brule, activity.getNextLadoffDesc());
                        }

                        DiscountRule discountRule = new DiscountRule();
                        discountRule.setRulecode(brule);
                        discountRule.setRulename(DiscountRuleStore.getRuleByName(brule));
                        //加入活动码
                     //   actCodeList.add(activity.getUnqid() + "");
                        if (activity.getLimits(product.getSKU()) < minLimit) {
                            minLimit = activity.getLimits(product.getSKU());
                        }
                        //判断秒杀
                        if (brule == 1113) {
                            shoppingCartVO.setSkprice(product.getOriginalPrice());
                            shoppingCartVO.setStatus(1);
                        }
                        ruleList.add(discountRule);


                        actStock = Math.min(
                                RedisStockUtil
                                        .getActStockBySkuAndActno(shoppingCartVO.getPdno(),
                                                discount.getDiscountNo()),
                                actStock);
                        skuStock = shoppingCartVO.getInventory();
                        try {
                            skuStock = RedisStockUtil.getStock(shoppingCartVO.getPdno());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (skuStock == 0) {
                            shoppingCartVO.setStatus(3);
                        }

                        shoppingCartVO.setInventory(skuStock);
                        shoppingCartVO.setActstock(actStock);

                        if (activity.getLimits(shoppingCartVO.getPdno()) == 0) {
                            break;
                        }

                        subStock = Math.min
                                (activity.getLimits(shoppingCartVO.getPdno())
                                        - RedisOrderUtil.getActBuyNum(compid, shoppingCartVO.getPdno(),
                                        activity.getUnqid()), subStock);
                        shoppingCartVO.setLimitsub(subStock);

                        minLimit = Math.min
                                (activity.getLimits(shoppingCartVO.getPdno())
                                        , minLimit);
                    }

                }
            }
            shoppingCartVO.setLimitnum(minLimit);
            if (minLimit == Integer.MAX_VALUE) {
                shoppingCartVO.setLimitnum(0);
            }
            shoppingCartVO.setRule(ruleList);
        }

        for (ShoppingCartVO shoppingCartVO : shopCart) {
            if (shoppingCartVO.getChecked() == 1) {
                if (Long.parseLong(shoppingCartVO.getPkgno()) > 0) {
                    Package pkg = new Package();
                    pkg.setPackageId(Long.parseLong(shoppingCartVO.getPkgno()));
                    pkg.setNums(shoppingCartVO.getPkgnum());
                    ckProduct.add(pkg);
                } else {
                    Product product = new Product();
                    product.setSku(shoppingCartVO.getPdno());
                    product.autoSetCurrentPrice(shoppingCartVO.getPdprice(), shoppingCartVO.getNum());
                    ckProduct.add(product);
                }
            }
        }
        DiscountResult discountResult
                = CalculateUtil.calculate(compid, ckProduct, Long.parseLong(shopCart.get(0).getConpno()));


        for (ShoppingCartVO shoppingCartVO : shopCart) {
            for (IProduct product : ckProduct) {
                if (shoppingCartVO.getChecked() == 1) {
                    Package pkg = null;
                    if (product instanceof Package) {
                        pkg = (Package) product;
                        result = result.add(MathUtil.exactMul(product.getOriginalPrice(),product.getNums()));
                    }
                    if ((product instanceof Product && shoppingCartVO.getPdno() == product.getSKU())
                            || (pkg != null && pkg.getPackageId() == Long.parseLong(shoppingCartVO.getPkgno()))) {
                        shoppingCartVO.setSubtotal(product.getCurrentPrice());
                        shoppingCartVO.setDiscount(product.getDiscounted());
                        shoppingCartVO.setAmt(discountResult.getTotalDiscount());
                        shoppingCartVO.setAcamt(discountResult.getTotalCurrentPrice());
                        shoppingCartVO.setCounpon(discountResult.getCouponValue());
                        shoppingCartVO.setFreepost(discountResult.isFreeShipping());
                        shoppingCartVO.setOflag(discountResult.isExCoupon());
                        shoppingCartVO.setoSubtotal(MathUtil.exactMul(product.getOriginalPrice(), product.getNums()).
                                setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
                        if (areano > 0 && !discountResult.isFreeShipping()) {
                            shoppingCartVO.setFreight(AreaFeeUtil.getFee(areano));
                        }
                    }
                }
            }
        }

        shopCart.get(0).setTotalamt(result.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
        //套餐赋值
        for (ShoppingCartVO shoppingCartVO : shopCart) {
            for (IDiscount discount : prdDiscountResult.getActivityList()) {
                if(((Activity)discount).isGlobalActivity()){
                    continue;
                }
                List<IProduct> pList = discount.getProductList();
                for (IProduct product : pList) {
                    if (product instanceof Package
                            && Long.parseLong(shoppingCartVO.getPkgno()) == ((Package) product).getPackageId()) {
                        shoppingCartVO.setSubtotal(product.getCurrentPrice());
                        shoppingCartVO.setDiscount(product.getDiscounted());
                        shoppingCartVO.setAmt(discountResult.getTotalDiscount());
                        shoppingCartVO.setAcamt(discountResult.getTotalCurrentPrice());
                        shoppingCartVO.setFreepost(discountResult.isFreeShipping());
                        shoppingCartVO.setOflag(discountResult.isExCoupon());
                        shoppingCartVO.setTotalamt(result.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
                        shoppingCartVO.setoSubtotal(MathUtil.exactMul(product.getOriginalPrice(), product.getNums()).
                                setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
                        if (shoppingCartVO.getAreano() > 0 && !discountResult.isFreeShipping()) {
                            shoppingCartVO.setFreight(AreaFeeUtil.getFee(shoppingCartVO.getAreano()));
                        }
                        shoppingCartVO.setPdprice(product.getOriginalPrice());
                        double dsprice = MathUtil.exactDiv(product.getCurrentPrice(), product.getNums()).
                                setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                        shoppingCartVO.setPkgprice(dsprice);

                        for (PkgProduct pkgProduct : shoppingCartVO.getPkgList()) {
                            for (Product pkgProductdt : ((Package) product).getPacageProdList()) {
                                if (pkgProduct.getPdno() == pkgProductdt.getSku()) {
                                    pkgProduct.setOpdprice(pkgProductdt.getOriginalPrice());
                                    double pdprice = MathUtil.exactDiv(pkgProductdt.getCurrentPrice(), pkgProductdt.getNums()).
                                            setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                                    pkgProduct.setPdprice(pdprice);
                                    pkgProduct.setApdprice(pkgProductdt.getCurrentPrice());
                                }
                            }
                        }
                    }
                }
            }
            shopCart.get(0).setAmt(discountResult.getTotalDiscount());
        }
        return shopCart;
    }


    private boolean delShoppingCart(String[] idList, String[] pkgidList, int compid) {

        List<Object[]> shopParm = new ArrayList<>();
        List<String> sqlList = new ArrayList<>();
        if (idList != null && idList.length > 0) {
            for (String unqid : idList) {
                if (StringUtils.isInteger(unqid)) {
                    shopParm.add(new Object[]{Long.parseLong(unqid)});
                    sqlList.add(REAL_DEL_PC_SHOPCART_SQL);
                }
            }
        }


        if (pkgidList != null && pkgidList.length > 0) {
            for (String unqid : pkgidList) {
                if (StringUtils.isInteger(unqid)) {
                    if(Long.parseLong(unqid) == 0){
                        continue;
                    }
                    shopParm.add(new Object[]{Long.parseLong(unqid), compid});
                    sqlList.add(DEL_SHOPCART_PKG_SQL);
                }
            }
        }

        if (sqlList.isEmpty()) {
            return false;
        }

        String[] sqlArray = new String[sqlList.size()];
        sqlArray = sqlList.toArray(sqlArray);

        int[] result = baseDao.updateTransNativeSharding(compid,
                TimeUtils.getCurrentYear(), sqlArray, shopParm);
        return !ModelUtil.updateTransEmpty(result);
    }


    private static List<IDiscount> getActivityList(int compid, List<IProduct> products) {
        List<IDiscount> activityList =
                new ActivityFilterService(
                        new ActivitiesFilter[]{
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
     * pdno - 商品码
     * pnum - 商品数量
     * compid - 买家企业码
     * checked - 是否选中：0 未选中，1 选中
     * pdprice - 商品单价
     * @返回列表 JSONObject:
     * goods 商品详情 com.onek.entity.ShoppingCartVO
     * gifts 赠品列表 com.onek.entity.ShoppingCartVO
     */
    @UserPermission(ignore = true)
    public Result querySettShopCartList(AppContext appContext) {
        int compid = appContext.getUserSession() == null
                ? 0
                : appContext.getUserSession().compId;
        String json = appContext.param.json;
        Result result = new Result();
        JsonParser jsonParser = new JsonParser();
        JsonArray jsonArray = jsonParser.parse(json).getAsJsonArray();
        List<ShoppingCartDTO> shoppingCartDTOS = new ArrayList<>();
        Gson gson = new Gson();
        for (JsonElement shopVO : jsonArray) {
            ShoppingCartDTO shoppingCartDTO = gson.fromJson(shopVO, ShoppingCartDTO.class);
            shoppingCartDTOS.add(shoppingCartDTO);
        }

        int controlCode = appContext.getUserSession().comp.controlCode;

        List<ShoppingCartDTO> pkgDTOList = getPkgDTO(shoppingCartDTOS);

        List<ShoppingCartVO> shopCart = getShopCart(cnvShoppingCartDtos(pkgDTOList, shoppingCartDTOS));

        for (ShoppingCartVO shoppingCartVO : shopCart) {
            int resultConsell = shoppingCartVO.getConsell();

            if ((resultConsell & controlCode) != resultConsell) {
                return result.fail("存在控销商品，您无权购买！");
            }
            //if(ShoppingCartVO shoppingCartVO : shopCart)
        }


//        return result.success(convResult(shopCart,
//                shoppingCartDTOS.get(0).getCompid()));

        shopCart = convResult(shopCart,
                shoppingCartDTOS.get(0).getCompid(),shoppingCartDTOS.get(0).getAreano());

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
                getGifts(shopCart, shoppingCartDTOS.get(0).getCompid());

        List<ShoppingCartVO> giftVOS = new ArrayList<>();

        //判断库存invnum:1846,  actnum:0,limitsub:0,limit:0
        for (ShoppingCartVO shoppingCartVO : shopCart){
            if(shoppingCartVO.getPdno() > 0){
                int invnum = shoppingCartVO.getInventory();
                int actnum = shoppingCartVO.getActstock();
                int limitsub = shoppingCartVO.getLimitsub();
                LogUtil.getDefaultLogger().debug("actCodeSize()---->>: "+ (shoppingCartVO.getActcode() != null
                        ? shoppingCartVO.getActcode().size() : shoppingCartVO.getActcode()) +
                        ",invnum:"+invnum+",  actnum:"+actnum+"," +
                        "limitsub:"+limitsub +",limit:"+shoppingCartVO.getLimitnum());
                int cbuy;
                if (shoppingCartVO.getActcode() != null && shoppingCartVO.getActcode().size() > 0 ) {//有活动
                    if(shoppingCartVO.getLimitnum() == 0){
                        cbuy = Math.min(invnum, actnum);
                    } else {
                        cbuy = Math.min(Math.min(invnum, actnum),limitsub);
                    }
                } else {
                    cbuy = invnum;
                }


                if(shoppingCartVO.getNum() > cbuy){
                    StringBuilder sb = new StringBuilder();
                    sb.append("商品：【");
                    sb.append(shoppingCartVO.getPtitle());
                    sb.append("】");
                    sb.append("库存不足，当前可用库存数量为：");
                    sb.append(cbuy);
                    sb.append("  ");
                    if(!StringUtils.isEmpty(shoppingCartVO.getVerdor())){
                        sb.append(" 厂商：【");
                        sb.append(shoppingCartVO.getVerdor());
                        sb.append("】");
                    }

                    if(!StringUtils.isEmpty(shoppingCartVO.getSpec())){
                        sb.append(" 规格：【");
                        sb.append(shoppingCartVO.getSpec());
                        sb.append("】");
                    }
                    return new Result().fail(sb.toString());
                }
            }
        }

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
            if (shoppingCartVO.getPkgnum() > 0) {
                continue;
            }
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
     * pdno - 商品码
     * pnum - 商品数量
     * compid - 买家企业码
     * checked - 是否选中：0 未选中，1 选中
     * pdprice - 商品单价
     * @返回列表 JsonArray:
     * ladamt - 阶梯金额
     * ladnum - 数量阶梯值
     * offercode - 优惠码
     * offername - 活动名称
     * offer - 优惠值
     * nladnum - 下个阶梯数量
     * nladamt - 下一个阶梯金额
     * noffer - 下个阶梯优惠值
     * currladDesc - 当前阶梯描述
     * nextladDesc - 下个阶梯描述
     */
    @UserPermission(ignore = true)
    public Result getOfferTip(AppContext appContext) {
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
        for (JsonElement shopVO : jsonArray) {
            ShoppingCartDTO shoppingCartDTO = gson.fromJson(shopVO, ShoppingCartDTO.class);
            shoppingCartDTOS.add(shoppingCartDTO);
        }

        List<ShoppingCartVO> shopCartList = getShopCart(shoppingCartDTOS);
        List<Product> productList = new ArrayList<>();

        if (shopCartList != null) {
            for (ShoppingCartVO shoppingCartVO : shopCartList) {
                Product product = new Product();
                product.setSku(shoppingCartVO.getPdno());
                product.autoSetCurrentPrice(shoppingCartVO.getPdprice(), shoppingCartVO.getNum());
                productList.add(product);
            }
        }

        DiscountResult discountResult
                = CalculateUtil.calculate(shoppingCartDTOS.get(0).getCompid(),
                productList, 0);
        List<IDiscount> activityList = discountResult.getActivityList();

        for (IDiscount discount : activityList) {
            Activity activity = (Activity) discount;
            Ladoff currLadoff = activity.getCurrLadoff();
            Ladoff nextLadoff = activity.getNextLadoff();
            int brule = (int) discount.getBRule();
            if (brule == 1113 || brule == 1133) {
                continue;
            }

            if (currLadoff == null && nextLadoff == null) {
                continue;
            }

            if (activity.isExActivity()) {
                continue;
            }

            OfferTipsVO offerTipsVO = new OfferTipsVO();
            offerTipsVO.setOffername(DiscountRuleStore.getRuleByName(brule));
            offerTipsVO.setGapamt(activity.getNextGapAmt());
            offerTipsVO.setGapnum(activity.getNextGapNum());
            if (currLadoff != null) {
                offerTipsVO.setLadamt(currLadoff.getLadamt());
                offerTipsVO.setLadnum(currLadoff.getLadnum());
                offerTipsVO.setOffer(currLadoff.getOffer());
                offerTipsVO.setOffercode(currLadoff.getOffercode());
                offerTipsVO.setUnqid(currLadoff.getUnqid() + "");
                offerTipsVO.setCurrladDesc(activity.getCurrentLadoffDesc());
            }

            if (nextLadoff != null) {
                offerTipsVO.setNladamt(nextLadoff.getLadamt());
                offerTipsVO.setNladnum(nextLadoff.getLadnum());
                offerTipsVO.setNoffer(nextLadoff.getOffer());
                offerTipsVO.setOffercode(nextLadoff.getOffercode());
                offerTipsVO.setUnqid(nextLadoff.getUnqid() + "");
                offerTipsVO.setNextladDesc(activity.getNextLadoffDesc());
            }

            offerTipsVOS.add(offerTipsVO);
        }
        return result.success(offerTipsVOS);
    }

    /**
     * 根据SKU查询购物车现有数量
     */
    public static int queryShopCartNumBySku(int compid, long sku) {
        List<Object[]> queryRet = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(),
                SELECT_SKUNUM_SQL, compid, sku);
        if (queryRet == null || queryRet.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(queryRet.get(0)[0].toString());
    }

    /**
     * 根据SKU查询购物车现有数量
     */
    public static int queryShopCartNumByPkg(int compid, long pkgno) {
        List<Object[]> queryRet = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(),
                SELECT_PKGNUM_SQL, compid, pkgno);
        if (queryRet == null || queryRet.isEmpty()) {
            return 0;
        }


        return Integer.parseInt(queryRet.get(0)[0].toString());
    }

    @UserPermission(ignore = true)
    public String queryShopCartNumBySkus(AppContext context) {
        try {
            return queryShopCartNumBySkus(Integer.parseInt(context.param.arrays[0]), context.param.arrays[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据SKU查询购物车现有数量
     */
    public static String queryShopCartNumBySkus(int compid, String skus) {
        String sql = "select pdno,pnum from {{?" + DSMConst.TD_TRAN_GOODS + "}} "
                + " where orderno = 0 and pkgno=0 and compid = ? and cstatus&1=0 and  pdno in(" + skus + ") ";
        List<Object[]> queryRet = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(), sql, compid);
        if (queryRet == null || queryRet.isEmpty()) return "";
        JsonArray jsonArray = new JsonArray();
        queryRet.forEach(qr -> {
            JsonObject object = new JsonObject();
            object.addProperty("sku", Long.parseLong(qr[0].toString()));
            object.addProperty("pnum", Integer.parseInt(qr[1].toString()));
            jsonArray.add(object);
        });
        return jsonArray.toString();
    }


    @UserPermission(ignore = true)
    public int remoteQueryShopCartNumBySku(AppContext context) {
        try {
            return queryShopCartNumBySku(Integer.parseInt(context.param.arrays[0]), Long.parseLong(context.param.arrays[1]));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }


    public int optGoodsPkg(List<IProduct> pkgNoList, int cid, int opflag,AppContext appContext) {
        List<IProduct> tempPkgNoList = new ArrayList<>(pkgNoList);

        DiscountResult discountResult
                = CalculateUtil.calculate(cid, tempPkgNoList,
                0);

        if (discountResult.isPkgExpired()) {
            return -1;
        }

        if (discountResult.getActivityList() == null
                || discountResult.getActivityList().isEmpty()) {
            return -4;
        }

        StringBuilder ids = new StringBuilder();
        for (int i = 0; i < tempPkgNoList.size(); i++) {
            Package pkg = (Package) tempPkgNoList.get(i);
            ids.append(pkg.getPackageId());
            if (i < tempPkgNoList.size() - 1) {
                ids.append(",");
            }
        }

        List<Object[]> queryRet = null;
        if (!StringUtils.isEmpty(ids.toString())) {
            StringBuilder sbSql = new StringBuilder(SELECT_SHOPCART_SUM_GROUP_PKG);
            sbSql.append(" and pkgno in (");
            sbSql.append(ids);
            sbSql.append(") group by pkgno ");
            queryRet = baseDao.queryNativeSharding(cid, TimeUtils.getCurrentYear(),
                    sbSql.toString(), cid);
        }

        Map<Long, Integer> pkgMap = new HashMap<>();
        for (IDiscount discount : discountResult.getActivityList()) {
            Activity activity = (Activity) discount;
            List<IProduct> productList = activity.getProductList();
            for (int i = 0; i < productList.size(); i++) {
                if (productList.get(i) instanceof Package) {
                    Package pkg = (Package) productList.get(i);
                    if (queryRet != null && !queryRet.isEmpty()) {
                        for (Object[] objects : queryRet) {
                            if (Integer.parseInt(objects[0].toString()) == pkg.getPackageId()) {
                                //计算当前套餐总件数
                                int pkgNum = Integer.parseInt(objects[1].toString())
                                        / pkg.singleProdsCount();
                                pkgMap.put(pkg.getPackageId(), pkgNum);
                            }
                        }
                    }
                }
            }
        }

        for (int i = 0; i < tempPkgNoList.size(); i++) {
            Package pkg = (Package) tempPkgNoList.get(i);
            pkg.getPacageProdList().clear();
            if (pkgMap.containsKey(pkg.getPackageId())) {
                int npkgnum = 0;
                int opkgnum = pkgMap.get(pkg.getPackageId());
                if (opflag == 1) {
                    npkgnum = opkgnum - pkg.getNums();
                } else {
                    npkgnum = opkgnum + pkg.getNums();
                }
                pkg.setNums(npkgnum < 0 ? 0 : npkgnum);
            }
        }

        clearPkg(pkgNoList);

        discountResult
                = CalculateUtil.calculate(cid, pkgNoList,
                0);

        StringBuilder skuIds = new StringBuilder();
        for (IDiscount iDiscount : discountResult.getActivityList()) {
            for (IProduct iProduct : iDiscount.getProductList()) {
                if (iDiscount.getLimits(iProduct.getSKU()) > 0
                        && iDiscount.getLimits(iProduct.getSKU()) < iProduct.getNums()) {
                    return -2;
                }
            }

            if(((Activity)iDiscount).isGlobalActivity()){
                continue;
            }

            for (IProduct iProduct : iDiscount.getProductList()) {
                if (iProduct instanceof Package) {
                    Package pkg = (Package) iProduct;
                    List<Product> pacageProdList = pkg.getPacageProdList();
                    for(Product product: pacageProdList){
                        skuIds.append(product.getSku()).append(",");
                    }
                 }
            }
        }

        String skuIdsStr = skuIds.toString();
        if(skuIdsStr.endsWith(",")){
            skuIdsStr = skuIdsStr.substring(0,skuIdsStr.length() -1);
        }

        //远程调用
        if(!StringUtils.isEmpty(skuIdsStr)){
            StringBuilder sqlSb = new StringBuilder(QUERY_ONE_PKG_INV_BUSSCOPE);
            sqlSb.append(" and sku in (").append(skuIdsStr).append(")");
            List<Object[]> queryInvRet = IceRemoteUtil.queryNative(sqlSb.toString());
            if (queryInvRet == null || queryInvRet.isEmpty()) {
                return -2;
            }

            if (!appContext.isSignControlAgree()) {
                for(int i = 0; i <queryInvRet.size(); i++){
                    int consell = Integer.parseInt(queryInvRet.get(i)[1].toString());
                    if ((consell & 1) > 0) {
                        return -5;
                    }
                }
            }
        }


        if (discountResult.getActivityList() == null
                || discountResult.getActivityList().isEmpty()) {
            return -4;
        }


        List<String> sqlList = new ArrayList<>();
        List<Object[]> parmObj = new ArrayList<>();
        for (IDiscount discount : discountResult.getActivityList()) {
            Activity activity = (Activity) discount;
            if(activity.isGlobalActivity()){
                continue;
            }
            LogUtil.getDefaultLogger().debug("====ActivityList_size===="+discountResult.getActivityList().size());
            LogUtil.getDefaultLogger().debug("====ActivityList_uid===="+activity.getUnqid());

            List<IProduct> productList = activity.getProductList();
            LogUtil.getDefaultLogger().debug("====productList_size===="+productList.size());
            for (int i = 0; i < productList.size(); i++) {
                if (productList.get(i) instanceof Package) {
                    Package pkg = (Package) productList.get(i);
                    List<Product> pacageProdList = pkg.getPacageProdList();
                    LogUtil.getDefaultLogger().debug(pkg.getPackageId()+"===pacageProdList_size==="+pacageProdList.size());
                    for (int j = 0; j < pacageProdList.size(); j++) {
                        if (pkgMap.containsKey(pkg.getPackageId())) {
                            sqlList.add(UPDATE_SHOPCART_PKG_SQL_EXT);
                            parmObj.add(new Object[]{pacageProdList.get(j).getNums(),
                                    pacageProdList.get(j).getSku(), cid, pkg.getPackageId()});
                        } else {
                            sqlList.add(INSERT_SHOPCART_PKG_SQL);
                            parmObj.add(new Object[]{GenIdUtil.getUnqId(), 0,
                                    pacageProdList.get(j).getSku(),
                                    pacageProdList.get(j).getNums(), cid, 0, pkg.getPackageId()});
                        }
                    }
                }
            }
        }



        String[] sqlArray = new String[sqlList.size()];
        if (sqlArray.length > 0) {
            sqlArray = sqlList.toArray(sqlArray);
            LogUtil.getDefaultLogger().debug("执行SQL :"+Arrays.toString(sqlArray));
            int[] rets = baseDao.updateTransNativeSharding(cid, TimeUtils.getCurrentYear(), sqlArray, parmObj);
            if (!ModelUtil.updateTransEmpty(rets)) {
                return 0;
            }
        }
        return -3;
    }


    /**
     * 转换成套餐数据结构
     *
     * @param shopCart
     * @param shopCartGoodsList
     * @param pkgnoSet
     */
    private void convertPkgList(List<ShoppingCartVO> shopCart,
                                List<ShoppingCartVO> shopCartGoodsList,
                                Set<String> pkgnoSet) {

        for (String pkgNo : pkgnoSet) {
            ShoppingCartVO pkvo = new ShoppingCartVO();
            List<PkgProduct> pkgProducts = new ArrayList<>();
            for (ShoppingCartVO shoppingCartVO : shopCart) {
                if (pkgNo.equals(shoppingCartVO.getPkgno())) {
                    // pkvo.setPdprice(shoppingCartVO.getPdprice());
                    pkvo.setPkgno(shoppingCartVO.getPkgno());
                    pkvo.setChecked(shoppingCartVO.getChecked());
                    pkvo.setPkgnum(shoppingCartVO.getPkgnum());
                    pkvo.setStatus(shoppingCartVO.getStatus());
                    PkgProduct pkgProduct = new PkgProduct();
                    pkgProduct.setVperiod(shoppingCartVO.getVperiod());
                    pkgProduct.setVerdor(shoppingCartVO.getVerdor());
                    pkgProduct.setSpec(shoppingCartVO.getSpec());
                    pkgProduct.setPtitle(shoppingCartVO.getPtitle());
                    pkgProduct.setNum(pkvo.getNum() / shoppingCartVO.getPkgnum());
                    // pkgProduct.setPdprice(shoppingCartVO.getPdprice());
                    pkgProduct.setOpdprice(shoppingCartVO.getPdprice());
                    pkgProduct.setPdno(shoppingCartVO.getPdno());
                    pkgProduct.setNum(shoppingCartVO.getNum());
                    pkgProducts.add(pkgProduct);
                    pkvo.setPkgList(pkgProducts);
                }
            }
            shopCartGoodsList.add(pkvo);
        }

    }


    public List<ShoppingCartDTO> getPkgDTO(List<ShoppingCartDTO> shoppingCartDTOS) {

        List<IProduct> pkgNoList = new ArrayList<>();
        List<ShoppingCartDTO> dtoList = new ArrayList<>();
        for (ShoppingCartDTO shoppingCartDTO : shoppingCartDTOS) {
            if (Long.parseLong(shoppingCartDTO.getPkgno()) > 0) {
                Package pkg = new Package();
                pkg.setNums(shoppingCartDTO.getPkgnum());
                pkg.setPackageId(Long.parseLong(shoppingCartDTO.getPkgno()));
                pkgNoList.add(pkg);
            }
        }

        DiscountResult discountResult
                = CalculateUtil.calculate(shoppingCartDTOS.get(0).getCompid(), pkgNoList,
                0);

        for (IDiscount discount : discountResult.getActivityList()) {
            Activity activity = (Activity) discount;
            if(activity.isGlobalActivity()){
                continue;
            }
            List<IProduct> productList = activity.getProductList();
            for (int i = 0; i < productList.size(); i++) {
                if (productList.get(i) instanceof Package) {
                    Package pkg = (Package) productList.get(i);
                    List<Product> pacageProdList = pkg.getPacageProdList();


                    for (int j = 0; j < pacageProdList.size(); j++) {

                        ShoppingCartDTO sdto = new ShoppingCartDTO();

                        for (ShoppingCartDTO shoppingCartDTO : shoppingCartDTOS) {
                            if (pkg.getPackageId() == Long.parseLong(shoppingCartDTO.getPkgno())) {
                                sdto.setChecked(shoppingCartDTO.getChecked());
                            }
                        }
                        sdto.setPdno(pacageProdList.get(j).getSku());
                        sdto.setPkgno(pkg.getPackageId() + "");
                        sdto.setCompid(shoppingCartDTOS.get(0).getCompid());
                        sdto.setPkgnum(pkg.getNums());
                        sdto.setPnum(pacageProdList.get(j).getNums());
                        sdto.setAreano(shoppingCartDTOS.get(0).getAreano());

                        double pdprice = MathUtil.exactDiv(pacageProdList.get(j).getCurrentPrice(), pacageProdList.get(j).getNums()).
                                setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                        sdto.setPdprice(pdprice);
                        sdto.setOpdprice(pacageProdList.get(j).getOriginalPrice());
                        if (discount.getLimits(pkg.getSKU()) > 0
                                && discount.getLimits(pkg.getSKU()) < pkg.getNums()) {
                            sdto.setCstatus(3);
                        }
                        if (pkg.getExpireFlag() < 0) {
                            sdto.setCstatus(4);
                        }


                        dtoList.add(sdto);
                    }
                }
            }
        }
        return dtoList;
    }

    private static int getPkgInv(Package pkg, Activity activity, int compid) {
        List<Product> pacageProdList = pkg.getPacageProdList();
        long minSku = 0;
        HashMap<Long, Integer> stockMap = new HashMap<>();
        for (Product product : pacageProdList) {
//            int tsubStock =  activity.getLimits(product.getSku())
//                        - RedisOrderUtil.getActBuyNum(compid, product.getSku(),
//                        activity.getUnqid());

            int actStock =  RedisStockUtil
                    .getActStockBySkuAndActno(product.getSku(),
                            activity.getUnqid());
            LogUtil.getDefaultLogger().debug("===获取套餐库存===："+pkg.getPackageId());
            LogUtil.getDefaultLogger().debug("活动码："+activity.getUnqid());
            int tminStock = RedisStockUtil.getStock(product.getSku());
            LogUtil.getDefaultLogger().debug("套餐："+pkg.getPackageId()+" SKU："+product.getSku()
                    + " 活动库存："+actStock+" 真实库存："+tminStock);
            int skuUnitNum = product.getNums() / pkg.getNums();
            tminStock = Math.min(actStock, tminStock);
            LogUtil.getDefaultLogger().debug("套餐换算库存："+pkg.getPackageId()+"  "+tminStock / skuUnitNum);
            stockMap.put(product.getSku(), tminStock / skuUnitNum);

        }

        int minSkuNum = Integer.MAX_VALUE;
        for (Long sku : stockMap.keySet()) {
            int invnum = stockMap.get(sku);
            if (minSkuNum > invnum) {
                minSku = sku;
                minSkuNum = invnum;
            }
        }

        LogUtil.getDefaultLogger().debug("最终返回库存："+pkg.getPackageId()+"  "+stockMap.get(minSku));

        return stockMap.get(minSku);
    }

    @UserPermission(ignore = true)
    public String queryPkgShopCartNum(AppContext context) {
        try {
            return queryPkgShopCartNum(Integer.parseInt(context.param.arrays[0]), context.param.arrays[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据SKU查询购物车套餐现有数量
     */
    public static String queryPkgShopCartNum(int compid, String pkgnos) {
        String sql = "select pdno,pnum,pkgno from {{?" + DSMConst.TD_TRAN_GOODS + "}} "
                + " where orderno = 0 and pkgno>0 and cstatus&1=0 and compid=? and pkgno in(" + pkgnos + ")";
        List<Object[]> queryRet = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(), sql, compid);
        if (queryRet == null || queryRet.isEmpty()) return "";
        JSONArray array = new JSONArray();
        queryRet.forEach(qr -> {
            JSONObject object = new JSONObject();
            object.put("sku", Long.parseLong(qr[0].toString()));
            object.put("pnum", Integer.parseInt(qr[1].toString()));
            object.put("pkgno", Integer.parseInt(qr[2].toString()));
            array.add(object);
        });
        LogUtil.getDefaultLogger().info("222222222----------4444444444----     " + array.toJSONString());
        return array.toJSONString();
    }


    public void clearPkg(List<IProduct> pkgList){
        for (IProduct product : pkgList){
            Package pkg = (Package)product;
            pkg.getPacageProdList().clear();
        }
    }


    private static List<String> listCntRemove(List<String> actlist)
    {
        LinkedHashSet<String> tmpSet = new LinkedHashSet<String>(actlist.size());
        tmpSet.addAll(actlist);
        actlist.clear();
        actlist.addAll(tmpSet);
        return actlist;
    }

    public static void main(String[] args) {

    }
}
