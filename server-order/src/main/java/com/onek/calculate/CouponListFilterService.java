package com.onek.calculate;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.onek.calculate.entity.*;
import com.onek.calculate.entity.Package;
import com.onek.calculate.service.filter.BaseDiscountFilterService;
import com.onek.calculate.util.DiscountUtil;
import com.onek.entity.CouponPubLadderVO;
import com.onek.entity.CouponPubVO;
import com.onek.util.CalculateUtil;
import com.onek.util.IceRemoteUtil;
import constant.DSMConst;
import dao.BaseDAO;
import util.StringUtils;
import util.TimeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class CouponListFilterService extends BaseDiscountFilterService {
    private long couentUnqid;
    private int compid;
    private boolean excoupon;
   // private Boolean noway = null;

//    private static final String GET_COUPON =
//            " SELECT * "
//            + " FROM {{?" + DSMConst.TD_PROM_COUENT + "}} "
//            + " WHERE cstatus = 0 "
//            + " AND startdate <= CURRENT_DATE AND CURRENT_DATE <= enddate ";


    /**
     * 查询领取的优惠券列表
     */
    private final String QUERY_COUPONREV = "select unqid,coupno,compid," +
            "DATE_FORMAT(startdate,'%Y-%m-%d') startdate," +
            "DATE_FORMAT(enddate,'%Y-%m-%d') enddate,brulecode,rulename,goods,ladder," +
            "glbno,ctype,reqflag from {{?"+ DSMConst.TD_PROM_COUENT +"}} "+
            " where compid = ? and cstatus = 0 AND startdate <= CURRENT_DATE AND CURRENT_DATE <= enddate ";

    //远程调用
    private static final String CHECK_SKU =
            " SELECT COUNT(0) "
            + " FROM {{?" + DSMConst.TD_PROM_ASSDRUG + "}} "
            + " WHERE cstatus&1 = 0 "
            + " AND actcode = ? ";

    public CouponListFilterService(
            boolean excoupon,
            int compid) {
        super();
        this.compid = compid;
        this.excoupon = excoupon;
    }


    public List<CouponPubVO> getCurrentDiscounts(List<IProduct> skus) {
        // 判定优惠券阶段
        List<CouponPubVO> checkCoupon = getCouents();

        if (checkCoupon == null || checkCoupon.isEmpty()) {
            return null;
        }
        // 过滤SKU
        Iterator<CouponPubVO> couentIterator = checkCoupon.iterator();

        List<Product> prdList = new ArrayList<>();
        for (IProduct prd: skus){
            if(prd instanceof Product){
                prdList.add((Product) prd);
            }
        }
        while(couentIterator.hasNext()){
            CouponPubVO couponPubVO = couentIterator.next();

            if (couponPubVO.getGoods() > 0 && !checkSKU(prdList,couponPubVO)) {
                couentIterator.remove();
            }
        }
        if(checkCoupon.isEmpty()){
            return null;
        }

        DiscountResult prdDiscountResult
                = CalculateUtil.calculate(compid, skus, 0);


        //double priceTotal = DiscountUtil.getCurrentPriceTotal(skus);
        double priceTotal = prdDiscountResult.getTotalCurrentPrice();
        //过滤阶梯优惠券
        Iterator<CouponPubVO> couentLadderIterator = checkCoupon.iterator();
        while(couentLadderIterator.hasNext()){
            CouponPubVO couponPubVO = couentLadderIterator.next();
            CouponPubLadderVO couponPubLadderVO = getLadoffable(couponPubVO.getLadderVOS(),priceTotal);
            if(couponPubLadderVO == null){
                couentLadderIterator.remove();
            }
        }
        return checkCoupon;
    }

    private boolean checkSKU(List<Product> skus,CouponPubVO couent) {
        //远程调用
        String skuSQL = getSkusSql(skus);
        if(StringUtils.isEmpty(skuSQL)){
            return false;
        }
        List<Object[]> check = IceRemoteUtil.queryNative(getSkusSql(skus), couent.getCoupno());
        return StringUtils.isBiggerZero(check.get(0)[0].toString());
    }


    private String getSkusSql(List<Product> skus){
        StringBuilder sbSql = new StringBuilder(CHECK_SKU);
        if(skus.size() == 0){
            return "";
        }
        sbSql.append(" and gcode in (");
        for(int i = 0; i < skus.size(); i++){
            if(skus.get(i).getSKU() > 0){
                if(i == (skus.size() -1)){
                    Arrays.stream(getProductCode(skus.get(i).getSKU()))
                            .forEach(str -> sbSql.append(str).append(","));
                    sbSql.append(skus.get(i).getSKU());
//                    sbSql.append(getProductCode(skus.get(i).getSku())).append(",").append(skus.get(i).getSku());
                }else{
                    Arrays.stream(getProductCode(skus.get(i).getSKU()))
                            .forEach(str -> sbSql.append(str).append(","));
                    sbSql.append(skus.get(i).getSKU()).append(",");
                }
            }
        }
        sbSql.append(")");
        return sbSql.toString();
    }


    private List<CouponPubVO> getCouents() {
        if (this.compid <= 0) {
            return null;
        }

        List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNativeSharding(
                        this.compid, TimeUtils.getCurrentYear(),
                QUERY_COUPONREV,this.compid);

        if (queryResult.isEmpty()) {
            return null;
        }

        CouponPubVO[] cArray = new CouponPubVO[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, cArray, CouponPubVO.class,
                new String[]{"unqid","coupno","compid","startdate","enddate","brulecode",
                        "rulename","goods","ladder","glbno","ctype","reqflag"});

        List<CouponPubVO> couponPubVOList = new ArrayList<>();
        for(CouponPubVO couponPubVO: cArray){
            if (couponPubVO.getGlbno() == 1
                    || (couponPubVO.getGlbno() == 0 && !this.excoupon)) {
                couponPubVOList.add(couponPubVO);
            }
        }
        JsonParser jsonParser = new JsonParser();
        for(CouponPubVO cvs :couponPubVOList) {
            String ldjson = cvs.getLadder();
            List<CouponPubLadderVO> ladderVOS = new ArrayList<>();
            if (!StringUtils.isEmpty(ldjson)) {
                JsonArray jsonArrayLadder = jsonParser.parse(ldjson).getAsJsonArray();
                Gson gson = new Gson();
                for (JsonElement goodvo : jsonArrayLadder) {
                    CouponPubLadderVO ldvo = gson.fromJson(goodvo, CouponPubLadderVO.class);
                    ladderVOS.add(ldvo);
                }
            }
            cvs.setLadderVOS(ladderVOS);
        }
        return couponPubVOList;
    }


    @Override
    protected List<IDiscount> getCurrentDiscounts(IProduct product) {
        return null;
    }

    public static CouponPubLadderVO getLadoffable(List<CouponPubLadderVO> ladoffs, double price) {
        double ladAmt;
        boolean able;

        for (CouponPubLadderVO ladoff : ladoffs) {
            ladAmt = ladoff.getLadamt();
            able = true;
            // 全为0则直接拿value
            if (ladAmt > 0) {
                able = price >= ladAmt;
            }
            if(ladAmt == 0 && ladoff.getOffer() > 0){
                able = true;
            }
            if (able) {
                return ladoff;
            }
        }
        return null;
    }
}