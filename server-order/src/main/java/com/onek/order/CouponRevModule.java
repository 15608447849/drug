package com.onek.order;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.alibaba.fastjson.JSON;
import com.google.gson.*;
import com.onek.annotation.UserPermission;
import com.onek.calculate.CouponListFilterService;
import com.onek.calculate.entity.Package;
import com.onek.calculate.entity.*;
import com.onek.calculate.util.DiscountUtil;
import com.onek.consts.CSTATUS;
import com.onek.consts.IntegralConstant;
import com.onek.context.AppContext;
import com.onek.entity.CouponPubLadderVO;
import com.onek.entity.CouponPubVO;
import com.onek.entity.CouponUseDTO;
import com.onek.entitys.Result;
import com.onek.util.CalculateUtil;
import com.onek.util.GenIdUtil;
import com.onek.util.IceRemoteUtil;
import com.onek.util.member.MemberStore;
import com.onek.util.order.RedisOrderUtil;
import constant.DSMConst;
import dao.BaseDAO;
import org.hyrdpf.util.LogUtil;
import util.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
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

    //新增积分详情
    private final static String INSERT_INTEGRAL_SQL = "insert into {{?"+ DSMConst.TD_INTEGRAL_DETAIL + "}} " +
            "(unqid,compid,istatus,integral,busid,createdate,createtime,cstatus)" +
            " values(?,?,?,?,?,CURRENT_DATE,CURRENT_TIME,?)";

    //删除优惠券
    private final static String DEL_COUPON_SQL = "update {{?"+ DSMConst.TD_PROM_COUENT + "}} set cstatus = cstatus | 1 " +
            " where unqid = ? ";


    //新增领取优惠券
    private final String INSERT_COUPONREV_CSQL = "insert into {{?" + DSMConst.TD_PROM_COUENT + "}} "
            + "(unqid,coupno,compid,startdate,starttime,enddate,endtime,brulecode,"
            + "rulename,goods,ladder,glbno,cstatus,ctype) "
            + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";



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
        long unqid = GenIdUtil.getUnqId();
        couponVO.setUnqid(unqid+"");
        int insertRet = insertCoupon(couponVO);

        if(insertRet > 0){
            try{
                int ret = IceRemoteUtil.couponRevRecord(couponVO.getCompid(),
                        Long.parseLong(couponVO.getCoupno()),couponVO.getQlfno());
                if(ret > 0){
                    return result.success("优惠券领取成功","领取成功");
                }
                baseDao.updateNativeSharding(couponVO.getCompid(),
                        TimeUtils.getCurrentYear(),DEL_COUPON_SQL,unqid);

            }catch (Exception e){
                e.printStackTrace();
                baseDao.updateNativeSharding(couponVO.getCompid(),TimeUtils.
                        getCurrentYear(),DEL_COUPON_SQL,unqid);
            }
        }
        return result.success("优惠券领取失败");


//        ret = IceRemoteUtil.updateNative(INSERT_COURCD,
//                rcdid,couponVO.getCoupno(),
//                couponVO.getCompid(),0,cstatus);


        //远程调用
//        List<Object[]> crdResult = IceRemoteUtil.queryNative(QUERY_COURCD_EXT, couponVO.getCompid(), couponVO.getCoupno());
//        int ret = 0;
//        long rcdid = 0L;
//        if(crdResult == null || crdResult.isEmpty()){
//            rcdid = GenIdUtil.getUnqId();
//            int cstatus = 0;
//            if(couponVO.getQlfno() == 1){
//                cstatus = 64;
//            }
//            //远程调用
//            ret = IceRemoteUtil.updateNative(INSERT_COURCD,
//                    rcdid,couponVO.getCoupno(),
//                            couponVO.getCompid(),0,cstatus);
//        }else{
//            rcdid = Long.parseLong(crdResult.get(0)[0].toString());
//            //远程调用
//            ret = IceRemoteUtil.updateNative(UPDATE_COURCD, rcdid);
//        }
//
//        if(ret > 0){
//            try{
//                if(insertCoupon(couponVO) > 0){
//                    //远程调用
//                    IceRemoteUtil.updateNative(UPDATE_COUPON_STOCK, couponVO.getCoupno());
//                    return result.success("领取成功");
//                }else{
//                    //远程调用,删除优惠记录
//                    IceRemoteUtil.updateNative(DEL_COURCD,rcdid);
//                }
//            }catch (Exception e){
//                IceRemoteUtil.updateNative(DEL_COURCD,rcdid);
//                e.printStackTrace();
//            }
//        }
       // return result.fail("领取失败");
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
                new Object[]{couponVO.getUnqid(),couponVO.getCoupno(),
                couponVO.getCompid(),startDate,"00:00:00",
                        endDate,"00:00:00",couponVO.getBrulecode(),
                couponVO.getRulename(),couponVO.getGoods(),
                        ladderJson,couponVO.getGlbno(),0});
    }


    public boolean insertCoupons(List<CouponPubVO> couponPubVOS){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date curDate = new Date();
        List<Object[]> parmList = new ArrayList<>();
        for(CouponPubVO couponPubVO :couponPubVOS){
            String startDate = dateFormat.format(curDate);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(curDate);

            calendar.add(Calendar.DATE, couponPubVO.getValidday());
            String endDate = dateFormat.format(calendar.getTime());
            if(couponPubVO.getValidflag() == 1){
                calendar.setTime(curDate);
                calendar.add(Calendar.DATE, 1);
                startDate = dateFormat.format(calendar.getTime());
                calendar.add(Calendar.DATE, couponPubVO.getValidday());
                endDate = dateFormat.format(calendar.getTime());
            }
            String ladderJson =  GsonUtils.javaBeanToJson(couponPubVO.getLadderVOS());
            parmList.add( new Object[]{GenIdUtil.getUnqId(),couponPubVO.getCoupno(),
                    couponPubVO.getCompid(),startDate,"00:00:00",
                    endDate,"00:00:00",couponPubVO.getBrulecode(),
                    couponPubVO.getRulename(),couponPubVO.getGoods(),
                    ladderJson,couponPubVO.getGlbno(),0,couponPubVO.getCtype()});
        }
        int result[] = baseDao.updateBatchNativeSharding(couponPubVOS.get(0).getCompid(),
                TimeUtils.getCurrentYear(),INSERT_COUPONREV_CSQL,parmList,parmList.size());

       // IceRemoteUtil.updateTransNative()

        return !ModelUtil.updateTransEmpty(result);

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
        List<IProduct> productList = new ArrayList<>();
        for (JsonElement coupn : jsonArray) {
            CouponUseDTO couponUseDTO = gson.fromJson(coupn, CouponUseDTO.class);
            if (couponUseDTO != null) {
                couponUseDTOS.add(couponUseDTO);
                if(Long.parseLong(couponUseDTO.getPkgno()) > 0){
                    Package pkg = new Package();
                    pkg.setPackageId(Long.parseLong(couponUseDTO.getPkgno()));
                    pkg.setNums(couponUseDTO.getPkgnum());
                    productList.add(pkg);
                }else{
                    Product product = new Product();
                    product.setSku(couponUseDTO.getPdno());
                    product.autoSetCurrentPrice(couponUseDTO.getPrice(),couponUseDTO.getPnum());
                    productList.add(product);
                }
            }
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
        List<IProduct> productList = new ArrayList<>();
        BigDecimal subCalRet = BigDecimal.ZERO;
        for (JsonElement coupn : jsonArray) {
            CouponUseDTO couponUseDTO = gson.fromJson(coupn, CouponUseDTO.class);
            if (couponUseDTO != null) {
                couponUseDTOS.add(couponUseDTO);
                if(Long.parseLong(couponUseDTO.getPkgno()) > 0){
                    Package pkg = new Package();
                    pkg.setPackageId(Long.parseLong(couponUseDTO.getPkgno()));
                    pkg.setNums(couponUseDTO.getPkgnum());
                    productList.add(pkg);

                    //subCalRet = subCalRet.add(BigDecimal.valueOf(product.getCurrentPrice()));
                }else{
                    Product product = new Product();
                    product.setSku(couponUseDTO.getPdno());
                    if(couponUseDTO.getSkprice() > 0){
                        product.autoSetCurrentPrice(couponUseDTO.getSkprice(),couponUseDTO.getPnum());
                    }else{
                        product.autoSetCurrentPrice(couponUseDTO.getPrice(),couponUseDTO.getPnum());
                    }
                    subCalRet = subCalRet.add(BigDecimal.valueOf(product.getCurrentPrice()));
                    productList.add(product);
                }
            }
        }
        int compid = couponUseDTOS.get(0).getCompid();
        DiscountResult calculate = CalculateUtil.calculate(compid,
                productList, Long.parseLong(couponUseDTOS.get(0).getCoupon()));
        BigDecimal pgkDiscount = BigDecimal.ZERO;
        for(IProduct product:productList){
            if(product instanceof Package){
                pgkDiscount.add(BigDecimal.valueOf(product.getDiscounted()));
                subCalRet = subCalRet.add(MathUtil.exactMul(product.getOriginalPrice(),product.getNums()));
            }
        }

        List<IDiscount> activityList = calculate.getActivityList();
        long sku;

        for(IDiscount discount :activityList){
//            if(discount instanceof Activity && ((Activity)discount).isGlobalActivity()
//                    || discount instanceof Couent){
//                continue;
//            }

//            for(IProduct product : discount.getProductList()){
//                if(product instanceof Package){
//                    pgkDiscount.add(BigDecimal.valueOf(product.getDiscounted()));
//                    subCalRet = subCalRet.add(MathUtil.exactMul(product.getOriginalPrice(),product.getNums()));
//                }
//            }

            for(CouponUseDTO couponUseDTO: couponUseDTOS){
                //判断
                if(Long.parseLong(couponUseDTO.getPkgno()) > 0 ) {
                    continue;
                }

                sku = couponUseDTO.getPdno();
                int limits = discount.getLimits(sku);
                int buyed = RedisOrderUtil.getActBuyNum(compid,sku,discount.getDiscountNo());
                if(limits > 0 && (limits - buyed < couponUseDTO.getPnum())){
                    return new Result().fail("当前购买量大于可购量，请重新下单！");
                }
            }
        }

        resultMap.put("tprice",subCalRet.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
        resultMap.put("tdiscount",calculate.getTotalDiscount());
        resultMap.put("cpvalue",calculate.getCouponValue());
        double acvalue = MathUtil.exactSub(calculate.getTotalDiscount(), calculate.getCouponValue()).
                setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();
        resultMap.put("acvalue",acvalue);
        resultMap.put("freeship",calculate.isFreeShipping());
        resultMap.put("pkgvalue",pgkDiscount.doubleValue());
        double payamt = calculate.getTotalCurrentPrice();
        double sfee = couponUseDTOS.get(0).getShipfee();
        if(!calculate.isFreeShipping()){
            payamt = MathUtil.exactAdd(calculate.getTotalCurrentPrice(), sfee).
                    setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();
        }

        double bal = IceRemoteUtil.queryCompBal(compid);


        bal = MathUtil.exactDiv(bal,100L).
                setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();
        //可抵扣余额
        double useBal = getUseBal(payamt,resultMap);
        double flBal = bal;
        if(flBal>=useBal){
            flBal = useBal;
        }

        double rebeatTotal = 0;
        Map<Long, Integer> balMap = new HashMap<>();

        double[] r = apportionBal(productList,
                couponUseDTOS.get(0).getBalway() > 0 ? flBal : 0);

        for(int i = 0; i < r.length; i++) {
            balMap.put(productList.get(i).getSKU(), (int) (r[i] * 100));
        }

        for (IDiscount iDiscount : activityList) {
            if (iDiscount.getBRule() == 1210) {
                if (iDiscount instanceof Activity) {
                    Activity a = (Activity) iDiscount;
                    Ladoff currLadoff = a.getCurrLadoff();

                    if (currLadoff == null) {
                        continue;
                    }

                    if (currLadoff.isPercentage()) {
                        int total = 0;
                        for (IProduct iProduct : iDiscount.getProductList()) {
                            Integer bm = balMap.get(iProduct.getSKU());
                            if (bm != null) {
                                total += bm;
                            }
                        }
                        // TODO
                        rebeatTotal = MathUtil.exactAdd(
                                total * currLadoff.getOffer(), rebeatTotal).doubleValue();
                    } else {
                        rebeatTotal = MathUtil.exactAdd(
                                currLadoff.getOffer() * 100, rebeatTotal).doubleValue();
                    }

                }
            }
        }

        resultMap.put("bal",bal);
        resultMap.put("debal",0);
        resultMap.put("acpay",payamt);
        resultMap.put("payamt",payamt);
        resultMap.put("payflag",0);
        resultMap.put("rebeatp", BigDecimal.valueOf(rebeatTotal / 100.0).setScale(2, RoundingMode.DOWN).doubleValue());
        resultMap.put("usebal",useBal);
        if(couponUseDTOS.get(0).getBalway() > 0 && bal > 0){
            resultMap.put("bal",bal);
            if(useBal>0){
                if(bal >= useBal){
//                    appContext.logger.print("可抵扣余额："+useBal);
                    resultMap.put("debal",useBal);
                    resultMap.put("acpay",MathUtil.exactSub(payamt,useBal));
                }else{
                    resultMap.put("debal",bal);
                    resultMap.put("acpay",MathUtil.exactSub(payamt,bal));
                }
            }

        }

        return result.success(resultMap);
    }

    /**
     * 获取可抵扣的余额总额
     * @return
     */
    public static double getUseBal(double payamt,Map resultMap){
        double bal = 0.0;
        try{

            String baldeduction = IceRemoteUtil.getUseBal("BALANCE_DEDUCTION");
            if(baldeduction.length()>0){
                resultMap.put("baldeduction",baldeduction);
                double balDeductionPe =  MathUtil.exactDiv(Double.parseDouble(baldeduction),100).
                        setScale(2,RoundingMode.DOWN).doubleValue();
//                System.out.println("余额抵扣百分比："+balDeductionPe);
                bal = new BigDecimal(payamt).multiply(new BigDecimal(String.valueOf(balDeductionPe))).
                        setScale(2,RoundingMode.DOWN).doubleValue();
                System.out.println("余额抵扣百分比："+balDeductionPe);
//                if(bal<0.1){
//                    bal = 0.01;
//                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return bal;
    }

    public static double[] apportionBal(List<IProduct> tranOrderGoodsList, double bal) {
        double[] dprice = new double[tranOrderGoodsList.size()];
        double[] result = new double[tranOrderGoodsList.size()];
        double afterDiscountPrice = .0;


        for (int i = 0; i < tranOrderGoodsList.size(); i++) {
            dprice[i] = tranOrderGoodsList.get(i).getCurrentPrice();

            afterDiscountPrice =
                    MathUtil.exactAdd(afterDiscountPrice, tranOrderGoodsList.get(i).getCurrentPrice())
                            .setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        }


        bal = Math.min(bal, afterDiscountPrice);

        double[] cdprice = DiscountUtil.shareDiscount(dprice, bal);

        for (int i = 0; i < tranOrderGoodsList.size(); i++) {
            result[i] = MathUtil.exactSub(
                    tranOrderGoodsList.get(i).getCurrentPrice(),
                    MathUtil.exactSub(dprice[i], cdprice[i]).
                            setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()).
                    setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        }

        return result;
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
        List<IProduct> productList = new ArrayList<>();
        BigDecimal subCalRet = BigDecimal.ZERO;
        List<Product> skuList = new ArrayList<>();
        for (JsonElement coupn : jsonArray) {
            CouponUseDTO couponUseDTO = gson.fromJson(coupn, CouponUseDTO.class);
            if (couponUseDTO != null) {
                couponUseDTOS.add(couponUseDTO);
                if(Long.parseLong(couponUseDTO.getPkgno()) > 0){
                    Package pkg = new Package();
                    pkg.setPackageId(Long.parseLong(couponUseDTO.getPkgno()));
                    pkg.setNums(couponUseDTO.getPkgnum());
                    productList.add(pkg);

                    //subCalRet = subCalRet.add(BigDecimal.valueOf(product.getCurrentPrice()));
                }else{
                    Product product = new Product();
                    product.setSku(couponUseDTO.getPdno());
                    if(couponUseDTO.getSkprice() > 0){
                        product.autoSetCurrentPrice(couponUseDTO.getSkprice(),couponUseDTO.getPnum());
                    }else{
                        product.autoSetCurrentPrice(couponUseDTO.getPrice(),couponUseDTO.getPnum());
                    }
                    subCalRet = subCalRet.add(BigDecimal.valueOf(product.getCurrentPrice()));
                    productList.add(product);
                    skuList.add(product);
                }
            }
        }


        int compid = couponUseDTOS.get(0).getCompid();

        boolean excoupon = false;
        if(couponUseDTOS.get(0).getFlag() == 1){
            excoupon = true;
        }

        Map<String, String> verifyResult
                = verifyCoupon(skuList, excoupon, Long.parseLong(couponUseDTOS.get(0).getCoupon()), compid);

        resultMap.put("code",verifyResult.get("code"));
        resultMap.put("msg",verifyResult.get("msg"));

        DiscountResult calculate = CalculateUtil.calculate(compid,
                productList, Long.parseLong(couponUseDTOS.get(0).getCoupon()));
        BigDecimal pgkDiscount = BigDecimal.ZERO;
        for(IProduct product:productList){
            if(product instanceof Package){
                pgkDiscount.add(BigDecimal.valueOf(product.getDiscounted()));
                subCalRet = subCalRet.add(MathUtil.exactMul(product.getOriginalPrice(),product.getNums()));
            }
        }

        List<IDiscount> activityList = calculate.getActivityList();
        long sku;
      //  BigDecimal pgkDiscount = BigDecimal.ZERO;
        for(IDiscount discount :activityList){
//            if(discount instanceof Activity && ((Activity)discount).isGlobalActivity()){
//                continue;
//            }

//            for(IProduct product : discount.getProductList()){
//                if(product instanceof Package){
//                    pgkDiscount.add(BigDecimal.valueOf(product.getDiscounted()));
//                    subCalRet = subCalRet.add(MathUtil.exactMul(product.getOriginalPrice(),product.getNums()));
//                }
//            }

            for(CouponUseDTO couponUseDTO: couponUseDTOS){
                if(Long.parseLong(couponUseDTO.getPkgno()) > 0 ) {
                    continue;
                }
                sku = couponUseDTO.getPdno();
                int limits = discount.getLimits(sku);
                int buyed = RedisOrderUtil.getActBuyNum(compid,sku,discount.getDiscountNo());
                if(limits > 0 && (limits - buyed < couponUseDTO.getPnum())){
                    return new Result().fail("当前购买量大于可购量，请重新下单！");
                }
            }
        }


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
        //可抵扣余额
        double useBal = getUseBal(payamt,resultMap);
        resultMap.put("bal",bal);
        resultMap.put("debal",0);
        resultMap.put("acpay",payamt);
        resultMap.put("payamt",payamt);
        resultMap.put("payflag",0);
        if(couponUseDTOS.get(0).getBalway() > 0){
            if(useBal>0){
                if(bal >= useBal){
                    appContext.logger.print("可抵扣余额："+useBal);
                    resultMap.put("debal",useBal);
                    resultMap.put("acpay",MathUtil.exactSub(payamt,useBal));
                }else{
                    resultMap.put("debal",bal);
                    resultMap.put("acpay",MathUtil.exactSub(payamt,bal));
                }
            }
            /*
            if(bal >= payamt){
                resultMap.put("debal",payamt);
                resultMap.put("acpay",0);
                resultMap.put("payflag",1);
            }else{
                resultMap.put("debal",bal);
                resultMap.put("acpay",MathUtil.exactSub(payamt,bal).
                        setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue());
            }
            */
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



    public static int getPayamtByAct(long orderno, long actcode) {
        List<Object[]> result = baseDao.queryNativeSharding(0,
                TimeUtils.getYearByOrderno(orderno + ""),
                " SELECT IFNULL(SUM(g.payamt), 0) - SUM(IFNULL(a.realrefamt, 0)) "
                + " FROM {{?" + DSMConst.TD_BK_TRAN_GOODS + "}} g "
                + " LEFT JOIN {{?" + DSMConst.TD_TRAN_ASAPP + "}} a "
                + " ON g.orderno = a.orderno AND g.pdno = a.pdno "
                + " AND a.cstatus&1 = 0 AND a.ckstatus = 200 "
                + " WHERE g.cstatus&1 = 0 AND g.orderno = ? "
                + " AND JSON_SEARCH(g.actcode, 'one', ?) IS NOT NULL ", orderno, actcode);

        if (result.isEmpty()) {
            return 0;
        }

        return Integer.parseInt(result.get(0)[0].toString());
    }

    /**
     * 新增满赠券
     * @param compid
     * @param giftList
     * @return
     */
    public static boolean insertGiftCoupon(int compid, long orderno, List<Gift> giftList){
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
                    Ladoff currLadoff = gift.getCurrLadoff();
                    if (currLadoff != null) {
                        int value = (int) (currLadoff.getOffer() * 100);

                        if (currLadoff.isPercentage()) {
                            int currPayamt = getPayamtByAct(orderno, gift.getActivityCode());
                            value = (int) (currPayamt * currLadoff.getOffer());
                        }

                        if (value > 0) {
                            bal = bal + value;
                            coupSqlList.add(INSERT_GLBCOUPONREV_SQL);
                            coupParams.add(new Object[]{GenIdUtil.getUnqId(),couponResult.getCoupno(),compid,2110,"全局现金券",1,5,0,value});
                        }
                    }

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

    public static List<Gift> getGifts(long orderno, int compid) {
        if(compid <= 0 || orderno <= 0){
            return Collections.emptyList();
        }

        List<Object[]> queryResult = baseDao.queryNativeSharding(
                compid, TimeUtils.getYearByOrderno(String.valueOf(orderno)),
                QUERY_ORDER_GIFT, compid, orderno);

        if(queryResult == null || queryResult.isEmpty()){
            return Collections.emptyList();
        }

        return JSON.parseArray(queryResult.get(0)[0].toString(), Gift.class);
    }

    private static int getPayamt(long orderno, int compid) {
        List<Object[]> queryResult = baseDao.queryNativeSharding(compid,
                TimeUtils.getYearByOrderno(orderno + ""),
                " SELECT payamt "
                + " FROM {{?" + DSMConst.TD_TRAN_ORDER + "}} "
                + " WHERE cstatus&1 = 0 AND orderno = ? ", orderno);

        if (queryResult.isEmpty()) {
            return 0;
        }

        return Integer.parseInt(queryResult.get(0)[0].toString());
    }

    public static boolean revGiftCoupon(long orderno,int compid){
        if(compid <= 0 || orderno <= 0){
            return false;
        }

        List<Gift> jsonObj = getGifts(orderno, compid);

        if (!jsonObj.isEmpty()) {
//            int payamt = getPayamt(orderno, compid);
            return insertGiftCoupon(compid, orderno, jsonObj);
        }

        return false;
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
    public Result insertNewComerCoupon(AppContext appContext){
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


    /**
     * 验证优惠券
     * @param productList
     * @param excoupon
     * @param coupid
     * @param compid
     * @return
     */
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

    /**
     * 领取积分兑换券
     * @param appContext
     * @return
     */
    @UserPermission(ignore = false)
    public Result revExcgCoupon(AppContext appContext){
        Result result = new Result();
        String json = appContext.param.json;
        CouponPubVO couponVO = GsonUtils.jsonToJavaBean(json, CouponPubVO.class);
        if(couponVO == null){
            return result.fail("兑换失败");
        }
        if(couponVO.getLadderVOS() == null || couponVO.getLadderVOS().isEmpty()){
            return result.fail("兑换失败");
        }
        long rcdid = GenIdUtil.getUnqId();
        CouponPubLadderVO  CouponPubLadderVO = couponVO.getLadderVOS().get(0);
        try{
            double integralByCompid
                    = MathUtil.exactDiv(MemberStore.
                    getIntegralByCompid(couponVO.getCompid()), 1000).doubleValue();

            LogUtil.getDefaultLogger().debug("获取积分/1000："+integralByCompid);

            if(integralByCompid < CouponPubLadderVO.getOffer()){
                return result.fail("积分不够,兑换失败！");
            }
            List<Object[]> params = new ArrayList<>();
            Double offer = CouponPubLadderVO.getOffer();
            int reducePoint = offer.intValue() * 1000;
            String ladderJson =  GsonUtils.javaBeanToJson(couponVO.getLadderVOS());
            int pret = MemberStore.reducePoint(couponVO.getCompid(), reducePoint);
            boolean b = false;
            if (pret > 0) {//积分扣减成功
                params.add(new Object[]{rcdid,couponVO.getCoupno(),
                        couponVO.getCompid(),LocalDate.now().toString(),"00:00:00","2099-01-01","00:00:00",couponVO.getBrulecode(),
                        couponVO.getRulename(),couponVO.getGoods(),ladderJson,couponVO.getGlbno(),0,3,"0"});
                params.add(new Object[]{ GenIdUtil.getUnqId(), couponVO.getCompid(),
                        IntegralConstant.SOURCE_EXCHANGE_COUPON, reducePoint, rcdid, 0});
                b = !ModelUtil.updateTransEmpty(baseDao.updateTransNativeSharding(couponVO.getCompid(),TimeUtils.getCurrentYear(),
                        new String[]{INSERT_COUPON_EXCG_REV_SQL,INSERT_INTEGRAL_SQL},params));
            }
            if (!b){
                MemberStore.addPoint(couponVO.getCompid(), reducePoint);
                return result.fail("领取失败");
            }
        }catch (Exception e){
            e.printStackTrace();
            return result.fail("领取失败");
        }
        return result.success("领取成功");
    }




    /**
     * 注册有礼
     * @param appContext
     * @return
     */
    @UserPermission(ignore = true)
    public Result revNewCoupons(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        LogUtil.getDefaultLogger().debug(json);
        JsonParser jsonParser = new JsonParser();
        JsonArray jsonArray = jsonParser.parse(json).getAsJsonArray();

        List<CouponPubVO> couponPubVOList = new ArrayList<>();
        Gson gson = new Gson();
        for (JsonElement coupn : jsonArray) {
            CouponPubVO couponPubVO = gson.fromJson(coupn, CouponPubVO.class);
            if (couponPubVO != null) {
                if(couponPubVO.getQlfno() == 1){
                    couponPubVO.setCtype(4);
                }
                couponPubVOList.add(couponPubVO);
            }
        }

        if (insertCoupons(couponPubVOList)) {
            return result.success("领取成功");
        }
        return result.fail("领取失败");
    }


    /**
     * 注册有礼
     * @param appContext
     * @return
     */
    @UserPermission(ignore = true)
    public Result revHBCoupons(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        LogUtil.getDefaultLogger().debug(json);
        JsonParser jsonParser = new JsonParser();
        JsonArray jsonArray = jsonParser.parse(json).getAsJsonArray();

        List<CouponPubVO> couponPubVOList = new ArrayList<>();
        Gson gson = new Gson();
        for (JsonElement coupn : jsonArray) {
            CouponPubVO couponPubVO = gson.fromJson(coupn, CouponPubVO.class);
            if (couponPubVO != null) {
                if(couponPubVO.getQlfno() == 1){
                    couponPubVO.setCtype(4);
                }
                couponPubVOList.add(couponPubVO);
            }
        }

        if (insertHBCoupons(couponPubVOList)) {
            return result.success("领取成功");
        }
        return result.fail("领取失败");
    }

    public boolean insertHBCoupons(List<CouponPubVO> couponPubVOS){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date curDate = new Date();
        List<Object[]> parmList = new ArrayList<>();
        for(CouponPubVO couponPubVO :couponPubVOS){
            String startDate = dateFormat.format(curDate);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(curDate);

            calendar.add(Calendar.DATE, couponPubVO.getValidday());
            String endDate = dateFormat.format(calendar.getTime());
            if(couponPubVO.getValidflag() == 1){
                calendar.setTime(curDate);
                calendar.add(Calendar.DATE, 1);
                startDate = dateFormat.format(calendar.getTime());
                calendar.add(Calendar.DATE, couponPubVO.getValidday());
                endDate = dateFormat.format(calendar.getTime());
            }
            String ladderJson =  GsonUtils.javaBeanToJson(couponPubVO.getLadderVOS());
            parmList.add( new Object[]{GenIdUtil.getUnqId(),couponPubVO.getCoupno(),
                    couponPubVO.getCompid(),startDate,"00:00:00",
                    "2019-08-25","23:59:59",couponPubVO.getBrulecode(),
                    couponPubVO.getRulename(),couponPubVO.getGoods(),
                    ladderJson,couponPubVO.getGlbno(),0,couponPubVO.getCtype()});
        }
        int result[] = baseDao.updateBatchNativeSharding(couponPubVOS.get(0).getCompid(),
                TimeUtils.getCurrentYear(),INSERT_COUPONREV_CSQL,parmList,parmList.size());

        // IceRemoteUtil.updateTransNative()

        return !ModelUtil.updateTransEmpty(result);

    }

    public static void main(String[] args) {

    }

}
