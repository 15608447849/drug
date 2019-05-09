package com.onek.order;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.google.gson.*;
import com.onek.annotation.UserPermission;
import com.onek.calculate.CouponListFilterService;
import com.onek.calculate.entity.*;
import com.onek.consts.CSTATUS;
import com.onek.context.AppContext;
import com.onek.entity.ActivityGiftVO;
import com.onek.entity.CouponPubLadderVO;
import com.onek.entity.CouponPubVO;
import com.onek.entity.CouponUseDTO;
import com.onek.entitys.Result;
import com.onek.util.CalculateUtil;
import com.onek.util.GenIdUtil;
import com.onek.util.IceRemoteUtil;
import constant.DSMConst;
import dao.BaseDAO;
import org.hyrdpf.ds.AppConfig;
import util.*;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName CouponRevModule
 * @Description TODO
 * @date 2019-04-12 2:58
 */
public class CouponRevModule {

    private static BaseDAO baseDao = BaseDAO.getBaseDAO();

    //新增领取优惠券
    private final String INSERT_COUPONREV_SQL = "insert into {{?" + DSMConst.TD_PROM_COUENT + "}} "
            + "(unqid,coupno,compid,startdate,starttime,enddate,endtime,brulecode,"
            + "rulename,goods,ladder,glbno,cstatus) "
            + "values (?,?,?,?,?,?,?,?,?,?,?,?,?)";


    /**
     * 查询领取的优惠券列表
     */
    private final String QUERY_COUPONREV_SQL = "select unqid,coupno,compid,DATE_FORMAT(startdate,'%Y-%m-%d') startdate," +
            "DATE_FORMAT(enddate,'%Y-%m-%d') enddate,brulecode,rulename,goods,ladder," +
            "glbno,ctype,reqflag from {{?"+ DSMConst.TD_PROM_COUENT +"}} "+
            " where ctype != 2 and compid = ? ";

    private final String QUERY_COUPONREV_COUNT =
            " SELECT COUNT(0) "
            + " FROM {{?" + DSMConst.TD_PROM_COUENT + "}} "
            + " WHERE cstatus = 0 AND ctype != 2 "
            + " startdate <= CURRENT_DATE AND CURRENT_DATE <= enddate "
            + " AND compid = ?  ";

    /**
     * 查询领取的优惠券列表
     */
    private final String QUERY_COUPONREV_ONE_SQL = "select unqid,coupno,compid,DATE_FORMAT(startdate,'%Y-%m-%d') startdate," +
            "DATE_FORMAT(enddate,'%Y-%m-%d') enddate,brulecode,rulename,goods,ladder," +
            "glbno,ctype,reqflag from {{?"+ DSMConst.TD_PROM_COUENT +"}} "+
            " where cstatus&1=0 and unqid = ?";


    private static final String INSERT_COURCD =  "insert into {{?" + DSMConst.TD_PROM_COURCD + "}}" +
            " (unqid,coupno,compid,offercode,gettime,cstatus) values (?,?,?,?,now(),?)";

    private static final String DEL_COURCD =  "update {{?" + DSMConst.TD_PROM_COURCD + "}}" +
            " SET cstatus = cstatus | " + CSTATUS.DELETE +" WHERE unqid = ? ";

    private static final String QUERY_COURCD_EXT =  "select unqid from  {{?" + DSMConst.TD_PROM_COURCD + "}}" +
            " where compid = ? and coupno = ?  ";

    private static final String UPDATE_COURCD =  "update  {{?" + DSMConst.TD_PROM_COURCD + "}}" +
            " set cstatus = 0,gettime = now() where unqid = ? ";






    //扣减优惠券库存
    private static final String UPDATE_COUPON_STOCK = " update {{?" + DSMConst.TD_PROM_COUPON + "}}"
            + " set actstock = actstock - 1 " +
            "where unqid = ? and actstock > 0 and cstatus & 1 = 0";

    private static final String UPDATE_COUPON_STOCK_NUM = " update {{?" + DSMConst.TD_PROM_COUPON + "}}"
            + " set actstock = actstock - ? " +
            "where unqid = ? and actstock > 0 and cstatus & 1 = 0";


    /**
     * 查询领取的优惠券列表
     */
    private final String QUERY_COUP_EXT_SQL = "select count(1) from {{?"+ DSMConst.TD_PROM_COUENT +"}} "+
            " where compid = ? and coupno = ? and  cstatus = 0  and  CURRENT_DATE <= enddate ";



    /**
     * 查询活动优惠券
     */
    private final String QUERY_ACCOUP_SQL = "select unqid coupno,glbno,brulecode,validday,validflag,reqflag from {{?"+ DSMConst.TD_PROM_COUPON +"}} "+
            " where cstatus & 128 > 0 and brulecode = ? and cstatus & 1= 0 and actstock > 0 ";


    //领取活动优惠券
    private final String INSERT_ACCOUPONREV_SQL = "insert into {{?" + DSMConst.TD_PROM_COUENT + "}} "
            + "(unqid,coupno,compid,startdate,starttime,enddate,endtime,brulecode,"
            + "rulename,goods,ladder,glbno,actcode,ctype,reqflag,cstatus) "
            + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";


    private final String QUERY_ORDER_CNT = "select count(1) cnt from {{?"+DSMConst.TD_TRAN_ORDER+"}} where cusno = ?";


    private final String QUERY_ORDER_PRODUCT = "select pdno,pnum,convert(pdprice/100,decimal(10,2)) pdprice from {{?"+DSMConst.TD_TRAN_ORDER+"}} orders, "+
            "{{?"+DSMConst.TD_TRAN_GOODS+"}} goods where orders.orderno = goods.orderno and orders.orderno = ? "+
            " and orders.ostatus > 0 and orders.cstatus &1 = 0 and goods.cstatus & 1 = 0 ";

    private final String QUERY_COUPON_ACT = "select unqid,brulecode,validday,validflag,actstock,reqflag from " +
            "{{?"+DSMConst.TD_PROM_COUPON+"}}  where cstatus & 128 > 0 and brulecode = ? and  cstatus & 1 = 0 ";


    private final String QUERY_ACTCOUPON_LADD = "select lad.unqid,lad.ladamt,lad.ladnum,lad.offercode,lad.offer from {{?"+
            DSMConst.TD_PROM_RELA +"}} rela,{{?"+DSMConst.TD_PROM_LADOFF+"}} lad where rela.ladid = lad.unqid and rela.actcode = ? "+
            "  and rela.cstatus & 1 = 0 and lad.cstatus & 1 = 0 ";


    //新增领取活动优惠券
    private final String INSERT_ACTCOUPONREV_SQL = "insert into {{?" + DSMConst.TD_PROM_COUENT + "}} "
            + "(unqid,coupno,compid,startdate,starttime,enddate,endtime,brulecode,"
            + "rulename,goods,ladder,glbno,ctype,reqflag,actcode,cstatus) "
            + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";


    private final String INSERT_GLBCOUPONREV_SQL = "insert into {{?" + DSMConst.TD_PROM_COUENT + "}} "
            + "(unqid,coupno,compid,brulecode,rulename,glbno,ctype,cstatus,amt,gettime) "
            + "values (?,?,?,?,?,?,?,?,?,now())";


    //新增领取优惠券
    private final String INSERT_COUPON_EXCG_REV_SQL = "insert into {{?" + DSMConst.TD_PROM_COUENT + "}} "
            + "(unqid,coupno,compid,startdate,starttime,enddate,endtime,brulecode,"
            + "rulename,goods,ladder,glbno,cstatus,ctype) "
            + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,3)";




    /**
     * @description 优惠券新增
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @time  2019/4/2 14:34
     * @version 1.1.1
     **/
    @UserPermission(ignore = true)
    public Result insertRevCoupon(AppContext appContext) {

        Result result = new Result();
        String json = appContext.param.json;

        CouponPubVO couponVO = GsonUtils.jsonToJavaBean(json, CouponPubVO.class);
        long unqid = GenIdUtil.getUnqId();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date curDate = new Date();
        String startDate = dateFormat.format(curDate);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(curDate);
        calendar.add(Calendar.DATE, couponVO.getValidday());
        String endDate = dateFormat.format(calendar.getTime());
        if(couponVO.getValidflag() == 1){
            calendar.setTime(curDate);
            calendar.add(Calendar.DATE, 1);
            startDate = dateFormat.format(calendar.getTime());
            calendar.add(Calendar.DATE, couponVO.getValidday());
            endDate = dateFormat.format(calendar.getTime());
        }
        String ladderJson =  GsonUtils.javaBeanToJson(couponVO.getLadderVOS());
        int ret = baseDao.updateNativeSharding(couponVO.getCompid(), TimeUtils.getCurrentYear(),INSERT_COUPONREV_SQL,new Object[]{unqid,couponVO.getCoupno(),
        couponVO.getCompid(),startDate,"00:00:00",endDate,"00:00:00",couponVO.getBrulecode(),
        couponVO.getRulename(),couponVO.getGoods(),ladderJson,couponVO.getGlbno(),0});
        if(ret > 0){
            return result.success("新增成功");
        }
        return result.fail("新增失败");
    }



    @UserPermission(ignore = true)
    public Result insertRevExcgCoupon(AppContext appContext) {

        Result result = new Result();
        String json = appContext.param.json;

        CouponPubVO couponVO = GsonUtils.jsonToJavaBean(json, CouponPubVO.class);
        long unqid = GenIdUtil.getUnqId();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date curDate = new Date();
        String startDate = dateFormat.format(curDate);
        String ladderJson =  GsonUtils.javaBeanToJson(couponVO.getLadderVOS());
        int ret = baseDao.updateNativeSharding(couponVO.getCompid(), TimeUtils.getCurrentYear(),INSERT_COUPON_EXCG_REV_SQL,new Object[]{unqid,couponVO.getCoupno(),
                couponVO.getCompid(),startDate,"00:00:00","2099-01-01","00:00:00",couponVO.getBrulecode(),
                couponVO.getRulename(),couponVO.getGoods(),ladderJson,couponVO.getGlbno(),0});
        if(ret > 0){
            return result.success("新增成功");
        }
        return result.fail("新增失败");
    }

    /**
     * 查询领取的优惠券列表
     * @param appContext
     * @return
     */
    @UserPermission(ignore = true)
    public Result queryRevCouponList(AppContext appContext){
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int pageSize = jsonObject.get("pageSize").getAsInt();
        int pageIndex = jsonObject.get("pageNo").getAsInt();
        Page page = new Page();
        page.pageSize = pageSize;
        page.pageIndex = pageIndex;
        PageHolder pageHolder = new PageHolder(page);
        Result result = new Result();

        int compid = jsonObject.get("compid").getAsInt();

        if(compid == 0){
            result.success("用户未登陆或未认证！");
        }

        int type = jsonObject.get("type").getAsInt();


        StringBuilder sqlBuilder = new StringBuilder(QUERY_COUPONREV_SQL);

        switch (type){
            case 0:
                sqlBuilder.append(" and  cstatus = 0 and  CURRENT_DATE <= enddate ");
                break;
            case 1:
                sqlBuilder.append(" and  cstatus & 64 > 0 ");
                break;
            case 2:
                sqlBuilder.append(" and  CURRENT_DATE > enddate ");
        }


        List<Object[]> queryResult = baseDao.queryNativeSharding(compid,TimeUtils.getCurrentYear(),pageHolder, page, sqlBuilder.toString(),compid);
        CouponPubVO[] couponListVOS = new CouponPubVO[queryResult.size()];
        if (queryResult == null || queryResult.isEmpty()) {
            return result.setQuery(couponListVOS, pageHolder);
        }

        baseDao.convToEntity(queryResult, couponListVOS, CouponPubVO.class,
                new String[]{"unqid","coupno","compid","startdate","enddate","brulecode",
                        "rulename","goods","ladder","glbno","ctype","reqflag"});

        for(CouponPubVO cvs :couponListVOS){
            String ldjson = cvs.getLadder();
            if(!StringUtils.isEmpty(ldjson)){
                JsonArray jsonArray = jsonParser.parse(ldjson).getAsJsonArray();
                List<CouponPubLadderVO> ladderVOS = new ArrayList<>();
                Gson gson = new Gson();
                for (JsonElement goodvo : jsonArray){
                    CouponPubLadderVO ldvo = gson.fromJson(goodvo, CouponPubLadderVO.class);
                    ladderVOS.add(ldvo);
                }
                cvs.setLadderVOS(ladderVOS);
            }
        }
        return result.setQuery(couponListVOS, pageHolder);
    }


    /**
     * 领取优惠券
     * @param appContext
     * @return
     */
    @UserPermission(ignore = true)
    public Result revCoupon(AppContext appContext){
        Result result = new Result();
        String json = appContext.param.json;
        CouponPubVO couponVO = GsonUtils.jsonToJavaBean(json, CouponPubVO.class);

        if(couponVO.getCompid() <= 0){
            return result.fail("用户未登陆或未认证！");
        }
        List<Object[]> extCoup = baseDao.queryNativeSharding(couponVO.getCompid(),
                TimeUtils.getCurrentYear(), QUERY_COUP_EXT_SQL,
                new Object[]{couponVO.getCompid(),
                        couponVO.getCoupno()});
        if(extCoup != null && !extCoup.isEmpty()){
            if(Integer.parseInt(extCoup.get(0)[0].toString()) > 0){
                return result.success("已领取过该优惠券！");
            }
        }
        List<Object[]> crdResult = baseDao.queryNative(QUERY_COURCD_EXT, new Object[]{couponVO.getCompid(), couponVO.getCoupno()});
        int ret = 0;
        long rcdid = 0L;
        if(crdResult == null || crdResult.isEmpty()){
            rcdid = GenIdUtil.getUnqId();
            int cstatus = 0;
            if(couponVO.getQlfno() == 1){
                cstatus = 64;
            }
            ret = baseDao.updateNative(INSERT_COURCD,
                    new Object[]{rcdid,couponVO.getCoupno(),
                            couponVO.getCompid(),0,cstatus});
        }else{
            rcdid = Long.parseLong(crdResult.get(0)[0].toString());
            ret = baseDao.updateNative(UPDATE_COURCD,
                    new Object[]{rcdid});
        }

        if(ret > 0){
            try{
                if(insertCoupon(couponVO) > 0){
                    baseDao.updateNative(UPDATE_COUPON_STOCK,
                            new Object[]{couponVO.getCoupno()});
                    return result.success("领取成功");
                }else{
                    //删除优惠记录
                    baseDao.updateNative(DEL_COURCD,rcdid);
                }
            }catch (Exception e){
                baseDao.updateNative(DEL_COURCD,rcdid);
                e.printStackTrace();
            }
        }
        return result.fail("领取失败");
    }



    @UserPermission(ignore = true)
    public Result queryCouponByUid(AppContext appContext){
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        long unqid = jsonObject.get("unqid").getAsLong();
        int compid = jsonObject.get("compid").getAsInt();

        Result result = new Result();
        List<Object[]> queryResult = baseDao.queryNativeSharding(compid,TimeUtils.getCurrentYear(),QUERY_COUPONREV_ONE_SQL,unqid);
        CouponPubVO[] couponListVOS = new CouponPubVO[queryResult.size()];
        if (queryResult == null || queryResult.isEmpty()) {
            return result.success(couponListVOS);
        }

        baseDao.convToEntity(queryResult, couponListVOS, CouponPubVO.class,
                new String[]{"unqid","coupno","compid","startdate","enddate","brulecode",
                        "rulename","goods","ladder","glbno","ctype","reqflag"});

        return  result.success(couponListVOS);
    }

    public int insertCoupon(CouponPubVO couponVO){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date curDate = new Date();
        String startDate = dateFormat.format(curDate);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(curDate);
        calendar.add(Calendar.DATE, couponVO.getValidday());
        String endDate = dateFormat.format(calendar.getTime());
        if(couponVO.getValidflag() == 1){
            calendar.setTime(curDate);
            calendar.add(Calendar.DATE, 1);
            startDate = dateFormat.format(calendar.getTime());
            calendar.add(Calendar.DATE, couponVO.getValidday());
            endDate = dateFormat.format(calendar.getTime());
        }
        String ladderJson =  GsonUtils.javaBeanToJson(couponVO.getLadderVOS());
        return  baseDao.updateNativeSharding(couponVO.getCompid(),
                TimeUtils.getCurrentYear(),INSERT_COUPONREV_SQL,
                new Object[]{GenIdUtil.getUnqId(),couponVO.getCoupno(),
                couponVO.getCompid(),startDate,"00:00:00",
                        endDate,"00:00:00",couponVO.getBrulecode(),
                couponVO.getRulename(),couponVO.getGoods(),
                        ladderJson,couponVO.getGlbno(),0});
    }





    /**
     * 查询可以使用的优惠券
     * @param appContext
     * @return
     */
    @UserPermission(ignore = true)
    public Result queryActCouponList(AppContext appContext){
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonArray jsonArray = jsonParser.parse(json).getAsJsonArray();
        Result result = new Result();

        List<CouponUseDTO> couponUseDTOS = new ArrayList<>();
        Gson gson = new Gson();
        List<Product> productList = new ArrayList<>();
        for (JsonElement coupn : jsonArray) {
            CouponUseDTO couponUseDTO = gson.fromJson(coupn, CouponUseDTO.class);
            if (couponUseDTO != null) {
                couponUseDTOS.add(couponUseDTO);
                Product product = new Product();
                product.setSku(couponUseDTO.getPdno());
                product.autoSetCurrentPrice(couponUseDTO.getPrice(),couponUseDTO.getPnum());
                productList.add(product);
            }
            couponUseDTOS.add(couponUseDTO);
        }
        boolean excoupon = false;
        if(couponUseDTOS.get(0).getFlag() == 1){
            excoupon = true;
        }
        int compid = couponUseDTOS.get(0).getCompid();

        CouponListFilterService couponListFilterService = new CouponListFilterService(excoupon,
                compid);
        List<CouponPubVO> couponPubVOList = couponListFilterService.getCurrentDiscounts(productList);

        return result.success(couponPubVOList);
    }


    /**
     * 计算优惠券价格
     * @param appContext
     * @return
     */
    @UserPermission(ignore = true)
    public Result CouponCalculate(AppContext appContext){
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonArray jsonArray = jsonParser.parse(json).getAsJsonArray();
        Result result = new Result();

        Map resultMap = new HashMap();
        List<CouponUseDTO> couponUseDTOS = new ArrayList<>();
        Gson gson = new Gson();
        List<Product> productList = new ArrayList<>();
        BigDecimal subCalRet = BigDecimal.ZERO;
        for (JsonElement coupn : jsonArray) {
            CouponUseDTO couponUseDTO = gson.fromJson(coupn, CouponUseDTO.class);
            if (couponUseDTO != null) {
                couponUseDTOS.add(couponUseDTO);
                Product product = new Product();
                product.setSku(couponUseDTO.getPdno());
                product.autoSetCurrentPrice(couponUseDTO.getPrice(),couponUseDTO.getPnum());
                subCalRet = subCalRet.add(BigDecimal.valueOf(product.getCurrentPrice()));
                productList.add(product);
            }
        }
        int compid = couponUseDTOS.get(0).getCompid();
        DiscountResult calculate = CalculateUtil.calculate(compid,
                productList, couponUseDTOS.get(0).getCoupon());

        resultMap.put("tprice",subCalRet.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
        resultMap.put("tdiscount",calculate.getTotalDiscount());
        resultMap.put("cpvalue",calculate.getCouponValue());
        double acvalue = MathUtil.exactSub(calculate.getTotalDiscount(), calculate.getCouponValue()).
                setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();
        resultMap.put("acvalue",acvalue);
        resultMap.put("freeship",calculate.isFreeShipping());

        double payamt = calculate.getTotalCurrentPrice();
        double sfee = couponUseDTOS.get(0).getShipfee();
        if(!calculate.isFreeShipping()){
            payamt = MathUtil.exactAdd(calculate.getTotalCurrentPrice(), sfee).
                    setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();
        }
        double bal = IceRemoteUtil.queryCompBal(compid);
        resultMap.put("bal",0);
        resultMap.put("debal",0);
        resultMap.put("acpay",payamt);
        resultMap.put("payamt",payamt);
        resultMap.put("payflag",0);
        if(bal > 0){
            bal = MathUtil.exactDiv(bal,100L).
                        setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();
            resultMap.put("bal",bal);
            if(bal >= payamt){
                resultMap.put("debal",payamt);
                resultMap.put("acpay",0);
                resultMap.put("payflag",1);
            }else{
                resultMap.put("debal",bal);
                resultMap.put("acpay",MathUtil.exactSub(payamt,bal).
                        setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue());
            }
        }
        return result.success(resultMap);
    }

    public int couponRevCount(int compid) {
        if (compid <= 0) {
            return 0;
        }

        List<Object[]> queryResult = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(),
                QUERY_COUPONREV_COUNT, compid);


        return Integer.parseInt(queryResult.get(0)[0].toString());
    }

    /**
     * 查询企业订单统计数量
     * @param appContext
     * @return
     */
    @UserPermission(ignore = true)
    public String getOrderCntByCompid(AppContext appContext){
        String json = appContext.param.json;
        int compid = Integer.parseInt(appContext.param.arrays[0]);
        List<Object[]> queryResult = baseDao.queryNativeSharding(compid,TimeUtils.getCurrentYear(),QUERY_ORDER_CNT,compid);
        if (queryResult == null || queryResult.isEmpty()) {
           return "-1";
        }
        return queryResult.get(0)[0].toString();
    }



    public  boolean revGiftCoupon(long orderno,int compid){

        if(compid <= 0){
            return false;
        }

        List<Object[]> queryResult = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(),
                QUERY_ORDER_PRODUCT,orderno);

        if(queryResult == null || queryResult.isEmpty()){
            return false;
        }

        Product[] productArray = new Product[queryResult.size()];
        baseDao.convToEntity(queryResult, productArray, Product.class,
                new String[]{"sku","nums","originalPrice"});

        for(Product product: productArray){
            product.autoSetCurrentPrice(product.getOriginalPrice(),product.getNums());
        }
        List<Product> productList = Arrays.asList(productArray);
        DiscountResult discountResult = CalculateUtil.calculate(compid, productList, 0);
        List<IDiscount> activityList = discountResult.getActivityList();
        return insertGiftCoupon(compid,activityList);
    }


    
    public boolean insertGiftCoupon(int compid ,List<IDiscount> discounts){

        List<Object[]> coupParams = new ArrayList<>();
        List<Object[]> stockParams = new ArrayList<>();
        List<String> sqlList = new ArrayList<>();
        if(discounts == null || discounts.isEmpty()){
            return false;
        }
        for (IDiscount discount: discounts){
            Activity activity = (Activity) discount;
            List<Gift> giftList = activity.getGiftList();
            int brule = 0;
            if(giftList == null || giftList.isEmpty()){
                continue;
            }

            for (Gift gift: giftList){

                if(brule > 0){
                    break;
                }

                switch (gift.getType()){
                    case 0:
                        brule = 2120;
                        break;
                    case 1:
                        brule = 2110;
                        break;
                    case 2:
                        brule = 2130;
                        break;
                    default:
                        break;
                }
                List<Object[]> queryResult = baseDao.queryNative(QUERY_ACCOUP_SQL, brule);

                if (queryResult == null || queryResult.isEmpty()) {
                    break;
                }

                CouponPubVO[] couponPubVOS = new CouponPubVO[queryResult.size()];

                baseDao.convToEntity(queryResult, couponPubVOS, CouponPubVO.class,
                        new String[]{"coupno","glbno","brulecode","validday","validflag",
                                "reqflag"});
                CouponPubVO couponResult = setCoupValidDay(couponPubVOS[0]);

                CouponPubLadderVO couponPubLadderVO = new CouponPubLadderVO();
                couponPubLadderVO.setUnqid(0);
                couponPubLadderVO.setOffercode(0);
                couponPubLadderVO.setOffer(gift.getGiftValue());
                couponResult.setLadder(GsonUtils.
                        javaBeanToJson(new CouponPubLadderVO[]{couponPubLadderVO}));

                sqlList.add(UPDATE_COUPON_STOCK_NUM);
                stockParams.add(new Object[]{gift.getNums(),couponResult.getCoupno()});
                for (int i = 0; i < gift.getNums(); i++){
                    coupParams.add(new Object[]{GenIdUtil.getUnqId(),couponResult.getCoupno(),compid,
                            couponResult.getStartdate(), "00:00:00",couponResult.getEnddate(),"00:00:00",
                            couponResult.getBrulecode(), gift.getGiftName(),0,couponResult.getLadder(),
                            couponResult.getGlbno(),activity.getUnqid(),1,couponResult.getReqflag(),0});
                }
            }
        }

        int[] result = baseDao.updateBatchNativeSharding(compid,TimeUtils.getCurrentYear(),INSERT_ACCOUPONREV_SQL, coupParams, coupParams.size());

        String[] nativeSQL = new String[sqlList.size()];
        nativeSQL = sqlList.toArray(nativeSQL);
        if(nativeSQL.length != 0){
            baseDao.updateTransNative(nativeSQL,stockParams);
        }
        return !ModelUtil.updateTransEmpty(result);
    }


    public CouponPubVO setCoupValidDay(CouponPubVO couponVO){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date curDate = new Date();
        String startDate = dateFormat.format(curDate);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(curDate);
        calendar.add(Calendar.DATE, couponVO.getValidday());
        String endDate = dateFormat.format(calendar.getTime());
        if(couponVO.getValidflag() == 1){
            calendar.setTime(curDate);
            calendar.add(Calendar.DATE, 1);
            startDate = dateFormat.format(calendar.getTime());
            calendar.add(Calendar.DATE, couponVO.getValidday());
            endDate = dateFormat.format(calendar.getTime());
        }
        couponVO.setStartdate(startDate);
        couponVO.setEnddate(endDate);
        return couponVO;
    }



    @UserPermission(ignore = true)
    public int insertBalCoup(AppContext appContext){
        int compid = Integer.parseInt(appContext.param.arrays[0]);
        int amt = Integer.parseInt(appContext.param.arrays[1]);
        return baseDao.updateNativeSharding(compid,TimeUtils.getCurrentYear(),
                INSERT_GLBCOUPONREV_SQL,
                new Object[]{GenIdUtil.getUnqId(),666666L,compid,2110,"全局现金券",1,2,0,amt});
    }


    /**
     * 记录新人专享领取记录
     * @param appContext
     * @return
     */
    @UserPermission(ignore = true)
    public Result insertNewComerBalCoupon(AppContext appContext){
        Result result = new Result();
        String json = appContext.param.json;
        List<HashMap<String,String>> hashMaps = new ArrayList<>();
        Gson gson = new Gson();
        JsonArray array = new JsonParser().parse(json).getAsJsonArray();
        for (JsonElement element : array) {
            HashMap<String,String> map = gson.fromJson(element, HashMap.class);
            hashMaps.add(map);
        }
        List<Object[]> parms = new ArrayList<>();

        for (HashMap map : hashMaps){
            parms.add(new Object[]{GenIdUtil.getUnqId(),map.get("coupno"),
                    map.get("compid"),2110,"全局现金券",1,4,0,map.get("offer")});
        }

        int[] ret = baseDao.updateBatchNativeSharding(Integer.parseInt(hashMaps.get(0).get("compid")),
                TimeUtils.getCurrentYear(), INSERT_GLBCOUPONREV_SQL, parms, parms.size());

        if(!ModelUtil.updateTransEmpty(ret)){
            return result.success("领取成功");
        }
        return result.fail("领取失败");
    }






    public static void main(String[] args) {
        CouponRevModule couponRevModule = new CouponRevModule();
       // couponRevModule.revGiftCoupon(1904180003111203L,536862723);
    }


}
