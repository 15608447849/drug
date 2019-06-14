package com.onek.order;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.alibaba.fastjson.JSON;
import com.google.gson.*;
import com.onek.annotation.UserPermission;
import com.onek.calculate.CouponListFilterService;
import com.onek.calculate.entity.*;
import com.onek.calculate.util.DiscountUtil;
import com.onek.consts.CSTATUS;
import com.onek.context.AppContext;
import com.onek.entity.CouponPubLadderVO;
import com.onek.entity.CouponPubVO;
import com.onek.entity.CouponUseDTO;
import com.onek.entitys.Result;
import com.onek.util.CalculateUtil;
import com.onek.util.GenIdUtil;
import com.onek.util.IceRemoteUtil;
import constant.DSMConst;
import dao.BaseDAO;
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
            + " AND  CURRENT_DATE <= enddate "
            + " AND compid = ?  ";

    /**
     * 查询领取的优惠券列表
     */
    private final String QUERY_COUPONREV_ONE_SQL = "select unqid,coupno,compid,DATE_FORMAT(startdate,'%Y-%m-%d') startdate," +
            "DATE_FORMAT(enddate,'%Y-%m-%d') enddate,brulecode,rulename,goods,ladder," +
            "glbno,ctype,reqflag from {{?"+ DSMConst.TD_PROM_COUENT +"}} "+
            " where cstatus&1=0 and unqid = ?";

    //远程调用
    private static final String INSERT_COURCD =  "insert into {{?" + DSMConst.TD_PROM_COURCD + "}}" +
            " (unqid,coupno,compid,offercode,gettime,cstatus) values (?,?,?,?,now(),?)";
    //远程调用
    private static final String DEL_COURCD =  "update {{?" + DSMConst.TD_PROM_COURCD + "}}" +
            " SET cstatus = cstatus | " + CSTATUS.DELETE +" WHERE unqid = ? ";
    //远程调用
    private static final String QUERY_COURCD_EXT =  "select unqid from  {{?" + DSMConst.TD_PROM_COURCD + "}}" +
            " where compid = ? and coupno = ?  ";
    //远程调用
    private static final String UPDATE_COURCD =  "update  {{?" + DSMConst.TD_PROM_COURCD + "}}" +
            " set cstatus = 0,gettime = now() where unqid = ? ";






    //扣减优惠券库存 远程调用
    private static final String UPDATE_COUPON_STOCK = " update {{?" + DSMConst.TD_PROM_COUPON + "}}"
            + " set actstock = actstock - 1 " +
            "where unqid = ? and actstock > 0 and cstatus & 1 = 0";
    //远程调用
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
     * 远程调用
     */
    private final static String QUERY_ACCOUP_SQL = "select unqid coupno,glbno,brulecode,validday,validflag,reqflag from {{?"+ DSMConst.TD_PROM_COUPON +"}} "+
            " where cstatus & 128 > 0 and brulecode = ? and cstatus & 1= 0 and actstock > 0 ";


    //领取活动优惠券
    private final static String INSERT_ACCOUPONREV_SQL = "insert into {{?" + DSMConst.TD_PROM_COUENT + "}} "
            + "(unqid,coupno,compid,startdate,starttime,enddate,endtime,brulecode,"
            + "rulename,goods,ladder,glbno,actcode,ctype,reqflag,cstatus) "
            + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";


    private final String QUERY_ORDER_CNT = "select count(1) cnt from {{?"+DSMConst.TD_TRAN_ORDER+"}} where cusno = ?";


    private final static String QUERY_ORDER_PRODUCT = "select pdno,pnum,convert(pdprice/100,decimal(10,2)) pdprice from {{?"+DSMConst.TD_TRAN_ORDER+"}} orders, "+
            "{{?"+DSMConst.TD_TRAN_GOODS+"}} goods where orders.orderno = goods.orderno and orders.orderno = ? "+
            " and orders.ostatus > 0 and orders.cstatus &1 = 0 and goods.cstatus & 1 = 0 ";

    private final static String QUERY_ORDER_GIFT =
            " SELECT rebate "
            + " FROM {{?" + DSMConst.TD_TRAN_REBATE + "}} "
            + " WHERE cstatus&1 = 0 AND compid = ? AND orderno = ? ";


    private final static String INSERT_GLBCOUPONREV_SQL = "insert into {{?" + DSMConst.TD_PROM_COUENT + "}} "
            + "(unqid,coupno,compid,brulecode,rulename,glbno,ctype,cstatus,amt,gettime) "
            + "values (?,?,?,?,?,?,?,?,?,now())";


    //新增领取优惠券
    private final String INSERT_COUPON_EXCG_REV_SQL = "insert into {{?" + DSMConst.TD_PROM_COUENT + "}} "
            + "(unqid,coupno,compid,startdate,starttime,enddate,endtime,brulecode,"
            + "rulename,goods,ladder,glbno,cstatus,ctype,excoupno) "
            + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";



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
                couponVO.getRulename(),couponVO.getGoods(),ladderJson,couponVO.getGlbno(),0,3,"0"});
        if(ret > 0){
            return result.success("新增成功");
        }
        return result.fail("新增失败");
    }



    @UserPermission(ignore = true)
    public String insertRevOfflineCoupon(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        CouponPubVO couponVO = GsonUtils.jsonToJavaBean(json, CouponPubVO.class);
        long unqid = GenIdUtil.getUnqId();

//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
//        Date curDate = new Date();
//        String startDate = dateFormat.format(curDate);
        //couponVO.setEnddate("2099-01-01");
//        if(couponVO.getValidday() > 0){
//            setCoupValidDay(couponVO);
//        }

        String ladderJson =  GsonUtils.javaBeanToJson(couponVO.getLadderVOS());
        int ret = baseDao.updateNativeSharding(couponVO.getCompid(), TimeUtils.getCurrentYear(),INSERT_COUPON_EXCG_REV_SQL,new Object[]{unqid,couponVO.getCoupno(),
                couponVO.getCompid(),couponVO.getStartdate(),"00:00:00",couponVO.getEnddate(),"00:00:00",couponVO.getBrulecode(),
                couponVO.getRulename(),couponVO.getGoods(),ladderJson,0,0,6,couponVO.getExno()});

        if(ret > 0){
            return unqid+"";
        }
        return 0+"";
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
                break;
            case 3:
                sqlBuilder.append(" and (cstatus & 64 > 0 or CURRENT_DATE > enddate) ");
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

        assert couponVO != null;
        if(couponVO.getCompid() <= 0){
            return result.fail("用户未登陆或未认证！");
        }
        List<Object[]> extCoup = baseDao.queryNativeSharding(couponVO.getCompid(),
                TimeUtils.getCurrentYear(), QUERY_COUP_EXT_SQL,
                couponVO.getCompid(),
                couponVO.getCoupno());
        if(extCoup != null && !extCoup.isEmpty()){
            if(Integer.parseInt(extCoup.get(0)[0].toString()) > 0){
                return result.success("已领取过该优惠券！");
            }
        }
        //远程调用
        List<Object[]> crdResult = IceRemoteUtil.queryNative(QUERY_COURCD_EXT, couponVO.getCompid(), couponVO.getCoupno());
        int ret = 0;
        long rcdid = 0L;
        if(crdResult == null || crdResult.isEmpty()){
            rcdid = GenIdUtil.getUnqId();
            int cstatus = 0;
            if(couponVO.getQlfno() == 1){
                cstatus = 64;
            }
            //远程调用
            ret = IceRemoteUtil.updateNative(INSERT_COURCD,
                    rcdid,couponVO.getCoupno(),
                            couponVO.getCompid(),0,cstatus);
        }else{
            rcdid = Long.parseLong(crdResult.get(0)[0].toString());
            //远程调用
            ret = IceRemoteUtil.updateNative(UPDATE_COURCD, rcdid);
        }

        if(ret > 0){
            try{
                if(insertCoupon(couponVO) > 0){
                    //远程调用
                    IceRemoteUtil.updateNative(UPDATE_COUPON_STOCK, couponVO.getCoupno());
                    return result.success("领取成功");
                }else{
                    //远程调用,删除优惠记录
                    IceRemoteUtil.updateNative(DEL_COURCD,rcdid);
                }
            }catch (Exception e){
                IceRemoteUtil.updateNative(DEL_COURCD,rcdid);
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
                productList, Long.parseLong(couponUseDTOS.get(0).getCoupon()));

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
        bal = MathUtil.exactDiv(bal,100L).
                setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();
        resultMap.put("bal",bal);
        resultMap.put("debal",0);
        resultMap.put("acpay",payamt);
        resultMap.put("payamt",payamt);
        resultMap.put("payflag",0);
        if(couponUseDTOS.get(0).getBalway() > 0 && bal > 0){
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


    /**
     * 计算线下优惠券价格
     * @param appContext
     * @return
     */
    @UserPermission(ignore = true)
    public Result offlineCouponCalculate(AppContext appContext){
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

        boolean excoupon = false;
        if(couponUseDTOS.get(0).getFlag() == 1){
            excoupon = true;
        }

        Map<String, String> verifyResult
                = verifyCoupon(productList, excoupon, Long.parseLong(couponUseDTOS.get(0).getCoupon()), compid);

        resultMap.put("code",verifyResult.get("code"));
        resultMap.put("msg",verifyResult.get("msg"));

        DiscountResult calculate = CalculateUtil.calculate(compid,
                productList, Long.parseLong(couponUseDTOS.get(0).getCoupon()));

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
        bal = MathUtil.exactDiv(bal,100L).
                setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();

        resultMap.put("bal",bal);
        resultMap.put("debal",0);
        resultMap.put("acpay",payamt);
        resultMap.put("payamt",payamt);
        resultMap.put("payflag",0);
        if(couponUseDTOS.get(0).getBalway() > 0){
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


    /**
     * 满赠赠优惠券
     * @param appContext
     * @return
     *//*
    @UserPermission(ignore = true)
    public Result revGiftCoupon(AppContext appContext){
        long orderno = Long.parseLong(appContext.param.arrays[0]);
        int compid = Integer.parseInt(appContext.param.arrays[1]);
        Result result = new Result();
        if(compid <= 0){
            return result.fail("操作失败！");
        }

        List<Object[]> queryResult = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(),
                QUERY_ORDER_PRODUCT,orderno);

        if(queryResult == null || queryResult.isEmpty()){
            return result.fail("操作失败！");
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

        boolean b = insertGiftCoupon(compid,activityList);

        return  b ? result.success("操作成功"):result.fail("操作失败！");
    }*/


    /**
     * 新增满赠券
     * @param compid
     * @param giftList
     * @return
     */
    public static boolean insertGiftCoupon(int compid , List<Gift> giftList){
        System.out.println("~~~~~~~~~~~~~~~~~~~ ");
        System.out.println(JSON.toJSONString(giftList));

        List<Object[]> coupParams = new ArrayList<>();
        List<Object[]> stockParams = new ArrayList<>();
        List<String> sqlList = new ArrayList<>();
        List<String> coupSqlList = new ArrayList<>();
        if(giftList == null || giftList.isEmpty()){
            return false;
        }

        int bal = 0;
        int brule = 0;

        for (Gift gift: giftList){
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
                    brule = 0;
                    break;
            }

            //远程调用
            List<Object[]> queryResult = IceRemoteUtil.queryNative(QUERY_ACCOUP_SQL, brule);

            if (queryResult == null || queryResult.isEmpty()) {
                continue;
            }

            CouponPubVO[] couponPubVOS = new CouponPubVO[queryResult.size()];

            baseDao.convToEntity(queryResult, couponPubVOS, CouponPubVO.class,
                    new String[]{"coupno","glbno","brulecode","validday","validflag",
                            "reqflag"});

            CouponPubVO couponResult = setCoupValidDay(couponPubVOS[0]);
            CouponPubLadderVO couponPubLadderVO = new CouponPubLadderVO();
            couponPubLadderVO.setUnqid("0");

            couponPubLadderVO.setOffercode(Integer.parseInt(brule+"201"));
            couponPubLadderVO.setOffer(gift.getGiftValue());
            couponResult.setLadder(GsonUtils.
                    javaBeanToJson(new CouponPubLadderVO[]{couponPubLadderVO}));

            sqlList.add(UPDATE_COUPON_STOCK_NUM);//远程调用
            stockParams.add(new Object[]{gift.getNums(),couponResult.getCoupno()});

            for (int i = 0; i < gift.getNums(); i++){
                if(brule == 2110){
                    bal = bal + new Double(gift.getGiftValue() * 100).intValue();
                    coupSqlList.add(INSERT_GLBCOUPONREV_SQL);
                    coupParams.add(new Object[]{GenIdUtil.getUnqId(),couponResult.getCoupno(),compid,2110,"全局现金券",1,5,0,new Double(gift.getGiftValue() * 100).intValue()});
                }else{
                    coupSqlList.add(INSERT_ACCOUPONREV_SQL);
                    coupParams.add(new Object[]{GenIdUtil.getUnqId(),couponResult.getCoupno(),compid,
                            couponResult.getStartdate(), "00:00:00",couponResult.getEnddate(),"00:00:00",
                            couponResult.getBrulecode(), gift.getGiftName(),0,couponResult.getLadder(),
                            couponResult.getGlbno(), gift.getActivityCode(),1,couponResult.getReqflag(),0});
                }
            }
        }

        if (coupSqlList.isEmpty()) {
            return false;
        }

        String[] sqlArry = new String[coupSqlList.size()];
        sqlArry = coupSqlList.toArray(sqlArry);
        //添加余额
        IceRemoteUtil.updateCompBal(compid,bal);
        int[] result = baseDao.updateTransNativeSharding(compid,
                TimeUtils.getCurrentYear(), sqlArry, coupParams);

        String[] nativeSQL = new String[sqlList.size()];
        nativeSQL = sqlList.toArray(nativeSQL);
        if(nativeSQL.length != 0){
            IceRemoteUtil.updateTransNative(nativeSQL,stockParams);//远程调用
        }
        return !ModelUtil.updateTransEmpty(result);
    }


    public static boolean revGiftCoupon(long orderno,int compid){
        if(compid <= 0 || orderno <= 0){
            return false;
        }

        List<Object[]> queryResult = baseDao.queryNativeSharding(
                compid, TimeUtils.getYearByOrderno(String.valueOf(orderno)),
                QUERY_ORDER_GIFT, compid, orderno);

        if(queryResult == null || queryResult.isEmpty()){
            return false;
        }

        List<Gift> jsonObj = JSON.parseArray(queryResult.get(0)[0].toString(), Gift.class);

        return insertGiftCoupon(compid,jsonObj);
    }

    /**
     * 获取赠品
     * @param orderno
     * @param compid
     * @return
     */
    public List<Gift> revGiftGoods(long orderno,int compid){

        if(compid <= 0){
            return null;
        }

        List<Object[]> queryResult = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(),
                QUERY_ORDER_PRODUCT,orderno);

        if(queryResult == null || queryResult.isEmpty()){
            return null;
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

        List<Gift> giftList = new ArrayList<>();
        for (IDiscount discount : activityList){
            Activity activity = (Activity)discount;
            List<Gift> gifts = activity.getGiftList();
            for (Gift gift : gifts){
                if(gift.getType() == 3){
                    giftList.add(gift);
                }
            }

        }
        return giftList;
    }


    public static CouponPubVO setCoupValidDay(CouponPubVO couponVO){
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


    public Map<String,String> verifyCoupon(List<Product> productList,boolean excoupon,
                                           long coupid,int compid){

        Map<String,String> map = new HashMap<>();
        map.put("code","0");
        map.put("msg","");

        if(excoupon){
            map.put("msg","当前活动排斥优惠券！");
            return map;
        }


        String query_coup = "select unqid,coupno,compid," +
                "DATE_FORMAT(startdate,'%Y-%m-%d') startdate," +
                "DATE_FORMAT(enddate,'%Y-%m-%d') enddate,brulecode,rulename,goods,ladder," +
                "glbno,ctype,reqflag from {{?"+ DSMConst.TD_PROM_COUENT +"}} "+
                " where unqid = ? " +
                " and cstatus = 0 AND startdate <= CURRENT_DATE AND CURRENT_DATE <= enddate ";


        List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNativeSharding(
                compid, TimeUtils.getCurrentYear(),
                query_coup,coupid);

        if (queryResult == null || queryResult.isEmpty()) {
            map.put("msg","当前优惠券没在有效期内！");
            return map;
        }

        CouponPubVO[] cArray = new CouponPubVO[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, cArray, CouponPubVO.class,
                new String[]{"unqid","coupno","compid","startdate","enddate","brulecode",
                        "rulename","goods","ladder","glbno","ctype","reqflag"});


        String ldjson = cArray[0].getLadder();

        JsonParser jsonParser = new JsonParser();
        List<CouponPubLadderVO> ladderVOS = new ArrayList<>();
        if (!StringUtils.isEmpty(ldjson)) {
            JsonArray jsonArrayLadder = jsonParser.parse(ldjson).getAsJsonArray();
            Gson gson = new Gson();
            for (JsonElement goodvo : jsonArrayLadder) {
                CouponPubLadderVO ldvo = gson.fromJson(goodvo, CouponPubLadderVO.class);
                ladderVOS.add(ldvo);
            }
        }

        double priceTotal = DiscountUtil.getCurrentPriceTotal(productList);

        CouponPubLadderVO couponPubLadderVO
                = CouponListFilterService.getLadoffable(ladderVOS,priceTotal);

        if(couponPubLadderVO == null){
            map.put("msg","当前购买商品金额没达到 "+ladderVOS.get(0).getLadamt()+"元！");
            return map;
        }
        map.put("code","200");
        map.put("msg","操作成功！");
        return map;
    }






//    public static void main(String[] args) {
//                double[] dprice = new double[2];
//                dprice[0] = 20;13.33
//                dprice[1] = 10;6.67
//
//                double[] pprice = DiscountUtil.shareDiscount(dprice, 20);
//                System.out.println(pprice[0]);
//                System.out.println(pprice[1]);
//    }


}
