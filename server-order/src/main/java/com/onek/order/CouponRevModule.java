package com.onek.order;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.google.gson.*;
import com.onek.annotation.UserPermission;
import com.onek.consts.CSTATUS;
import com.onek.context.AppContext;
import com.onek.entity.CouponPubLadderVO;
import com.onek.entity.CouponPubVO;
import com.onek.entitys.Result;
import constant.DSMConst;
import dao.BaseDAO;
import global.GenIdUtil;
import global.IceRemoteUtil;
import util.GsonUtils;
import util.StringUtils;
import util.TimeUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

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
            " where cstatus&1=0 ";

    /**
     * 查询领取的优惠券列表
     */
    private final String QUERY_COUPONREV_ONE_SQL = "select unqid,coupno,compid,DATE_FORMAT(startdate,'%Y-%m-%d') startdate," +
            "DATE_FORMAT(enddate,'%Y-%m-%d') enddate,brulecode,rulename,goods,ladder," +
            "glbno,ctype,reqflag from {{?"+ DSMConst.TD_PROM_COUENT +"}} "+
            " where cstatus&1=0 and unqid = ?";


    private static final String INSERT_COURCD =  "insert into {{?" + DSMConst.TB_PROM_COURCD + "}}" +
            " (unqid,coupno,compid,offercode,gettime) values (?,?,?,?,now())";

    private static final String DEL_COURCD =  "update {{?" + DSMConst.TB_PROM_COURCD + "}}" +
            " SET cstatus = cstatus | " + CSTATUS.DELETE +" WHERE unqid = ? ";


    //扣减优惠券库存
    private static final String UPDATE_COUPON_STOCK = " update {{?" + DSMConst.TD_PROM_COUPON + "}}"
            + " set actstock = actstock - 1 " +
            "where unqid = ? and actstock > 0 and cstatus & 1 = 0";




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
        int type = jsonObject.get("type").getAsInt();


        StringBuilder sqlBuilder = new StringBuilder(QUERY_COUPONREV_SQL);

        switch (type){
            case 0:
                sqlBuilder.append(" and  CURRENT_DATE <= enddate ");
                break;
            case 1:
                sqlBuilder.append(" and  cstatus & 64 > 0 ");
                break;
            case 2:
                sqlBuilder.append(" and  CURRENT_DATE > enddate ");
        }


        List<Object[]> queryResult = baseDao.queryNativeSharding(compid,TimeUtils.getCurrentYear(),pageHolder, page, sqlBuilder.toString());
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
        long rcdid = GenIdUtil.getUnqId();
        int ret = baseDao.updateNative(INSERT_COURCD,
                new Object[]{rcdid,couponVO.getCoupno(),
                        couponVO.getCompid(),0});
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
        return result.success("领取失败");
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


}
