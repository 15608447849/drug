package com.onek.discount;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.context.AppContext;
import com.onek.discount.entity.*;
import com.onek.entitys.Result;
import constant.DSMConst;
import dao.BaseDAO;
import global.GenIdUtil;
import util.GsonUtils;
import util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName CouponManageModule
 * @Description TODO
 * @date 2019-04-01 17:01
 */
public class CouponManageModule {

    private static BaseDAO baseDao = BaseDAO.getBaseDAO();


    String selectActSQL = "select unqid,actname,incpriority,cpriority," +
            "qualcode,qualvalue,actdesc,excdiscount,acttype," +
            "actcycle,sdate,edate,rulecode,cstatus from {{?" + DSMConst.TD_PROM_ACT + "}} "
            + " where cstatus&1=0 ";

    //新增优惠券
    private final String INSERT_COUPON_SQL = "insert into {{?" + DSMConst.TD_PROM_COUPON + "}} "
            + "(unqid,coupname,glbno,qlfno,qlfval,coupdesc,periodtype,"
            + "periodday,startdate,enddate,ruleno,cstatus) "
            + "values(?,?,?,?,?,"
            + "?,?,?,?,?,?,?)";


    //新增场次
    private final String INSERT_TIME_SQL = "insert into {{?" + DSMConst.TD_PROM_TIME + "}} "
            + "(unqid,actcode,sdate,edate) "
            + " values(?,?,?,?)";

    //新增活动商品
    private final String INSERT_ASS_DRUG_SQL = "insert into {{?" + DSMConst.TD_PROM_ASSDRUG + "}} "
            + "(unqid,actcode,gcode,menucode,actstock,limitnum,cstatus) "
            + " values(?,?,?,?,?,?,?)";

    //优惠阶梯
    private final String INSERT_LAD_OFF_SQL = "insert into {{?" + DSMConst.TD_PROM_LADOFF + "}} "
            + "(unqid,actcode,ruleno,ladamt,ladnum,offer) "
            + " values(?,?,?,?,?,?)";

    //优惠赠换商品
    private final String INSERT_ASS_GIFT_SQL = "insert into {{?" + DSMConst.TD_PROM_ASSGIFT + "}} "
            + "(unqid,giftname,giftdesc)"
            + " values(?,?,?)";

    //查询优惠券详情
    private final String QUERY_COUPON_SQL = "select unqid,coupname,glbno,qlfno,qlfval,coupdesc,periodtype," +
            "periodday,DATE_FORMAT(startdate,'%Y-%m-%d') startdate,DATE_FORMAT(enddate,'%Y-%m-%d') enddate," +
            "ruleno from {{?"+ DSMConst.TD_PROM_COUPON +"}} where unqid = ? ";


    //查询优惠券列表
    private final String QUERY_COUPON_LIST_SQL = "select unqid,coupname,glbno,qlfno,qlfval,coupdesc,periodtype," +
            "periodday,DATE_FORMAT(startdate,'%Y-%m-%d') startdate,DATE_FORMAT(enddate,'%Y-%m-%d') enddate," +
            "ruleno,rulename,cop.cstatus from {{?"+ DSMConst.TD_PROM_COUPON +"}} cop left join" +
            " {{?"+ DSMConst.TD_PROM_RULE +"}}  ru on cop.ruleno = ru.rulecode  where 1 = 1 ";


    private final String QUERY_PROM_TIME_SQL = "select unqid,sdate,edate from {{?" + DSMConst.TD_PROM_TIME+"}} where actcode = ? ";

    private final String QUERY_PROM_RULE_SQL = "select rulecode,rulename from {{?" + DSMConst.TD_PROM_RULE+"}} where rulecode like '2%' ";

    private final String QUERY_PROM_LAD_SQL = "select unqid,ladamt,ladnum,offer from {{?" + DSMConst.TD_PROM_LADOFF+"}} where actcode = ? ";

    private final String QUERY_PROM_GOODS_SQL = "select `spec`,gcode,limitnum,manuname,standarno,prodname,classname " +
            " from {{?" + DSMConst.TD_PROM_ASSDRUG+"}} pdrug" +
            " left join {{?" + DSMConst.TD_PROD_SKU+"}} psku on pdrug.gcode = psku.sku " +
            " left join {{?" + DSMConst.TD_PROD_SPU+"}} pspu on psku.spu = pspu.spu "+
            " left join {{?" + DSMConst.TD_PROD_MANU+"}} pmun on pmun.manuno = pspu.manuno "+
            " left join {{?" + DSMConst.D_PRODUCE_CLASS+"}} dpr on pdrug.gcode = dpr.classid "+
            " where actcode = ? ";



    /**
     * @description 优惠券新增
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @time  2019/4/2 14:34
     * @version 1.1.1
     **/
    public Result insertCoupon(AppContext appContext) {

        Result result = new Result();
        String json = appContext.param.json;
        CouponVO couponVO = GsonUtils.jsonToJavaBean(json, CouponVO.class);
        long unqid = GenIdUtil.getUnqId();

        List<TimeVO> timeVOS = couponVO.getTimeVOS();

        List<Object[]> timeParm = new ArrayList<>();

        for (TimeVO timeVO : timeVOS){
            timeParm.add(new Object[]{GenIdUtil.getUnqId(),unqid,timeVO.getSdate(),timeVO.getEdate()});
        }
        int [] timeResult = baseDao.updateBatchNative(INSERT_TIME_SQL,timeParm,timeParm.size()) ;


        List<LadderVO> ladderVOS = couponVO.getLadderVOS();
        List<Object[]> ladderParm = new ArrayList<>();
        //优惠阶梯
        for (LadderVO ladderVO : ladderVOS){
            ladderParm.add(new Object[]{GenIdUtil.getUnqId(),unqid,couponVO.getRuleno(),
                    ladderVO.getLadamt(),ladderVO.getLadnum(),ladderVO.getOffer() });
        }

        int [] laddrResult = baseDao.updateBatchNative(INSERT_LAD_OFF_SQL,ladderParm,ladderParm.size()) ;

        List<GoodsVO> goodsVOS = couponVO.getAssDrugVOS();
        List<Object[]> goodsParm = new ArrayList<>();


        for (GoodsVO goodsVO : goodsVOS){
            goodsParm.add(new Object[]{GenIdUtil.getUnqId(),unqid,goodsVO.getGcode(),
                    goodsVO.getMenucode(),goodsVO.getActstock(),goodsVO.getLimitnum(),0 });
        }


        int [] goodsResult = baseDao.updateBatchNative(INSERT_ASS_DRUG_SQL,goodsParm,goodsParm.size()) ;


        int ret = baseDao.updateNative(INSERT_COUPON_SQL,new Object[]{
                unqid,couponVO.getCoupname(),couponVO.getGlbno(),couponVO.getQlfno(),
                couponVO.getQlfval(),couponVO.getDesc(),couponVO.getPeriodtype(),
                couponVO.getPeriodday(),couponVO.getStartdate(),couponVO.getEnddate(),
                couponVO.getRuleno(),0
        });

        return ret > 0 ? result.success("操作成功"): result.fail("操作失败！");
    }


    /**
     * @description 查询优惠券详情
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @time  2019/4/2 14:34
     * @version 1.1.1
     **/
    public Result queryCoupon(AppContext appContext) {

        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        long actcode = jsonObject.get("actcode").getAsLong();

        List<Object[]> coupResult = baseDao.queryNative(QUERY_COUPON_SQL,
                new Object[]{actcode});

        if(coupResult == null || coupResult.isEmpty()){
            return  result.success(null);
        }

        CouponVO[] couponVOS = new CouponVO[coupResult.size()];
        baseDao.convToEntity(coupResult, couponVOS, CouponVO.class,
                    new String[]{"coupno", "coupname", "glbno",
                            "qlfno", "qlfval", "desc", "periodtype", "periodday",
                            "startdate", "enddate", "ruleno"});

        couponVOS[0].setTimeVOS(getTimeVOS(actcode));
        couponVOS[0].setRulesVOS(getCoupRule());
        couponVOS[0].setAssDrugVOS(getCoupGoods(actcode));
        couponVOS[0].setLadderVOS(getCoupLadder(actcode));

        return  result.success(couponVOS[0]);
    }

    /**
     * 查询场次
     * @param actcode
     * @return
     */
    private List<TimeVO> getTimeVOS(long actcode){
        List<Object[]> result = baseDao.queryNative(QUERY_PROM_TIME_SQL,
                new Object[]{actcode});

        if(result == null || result.isEmpty()){
            return null;
        }

        TimeVO[] timeVOS = new TimeVO[result.size()];
        baseDao.convToEntity(result, timeVOS, TimeVO.class,
                new String[]{"unqid","sdate","edate"});

        return Arrays.asList(timeVOS);

    }

    /**
     * 查询规则
     * @return
     */
    private List<RulesVO> getCoupRule(){
        List<Object[]> result = baseDao.queryNative(QUERY_PROM_RULE_SQL,
                new Object[]{});

        if(result == null || result.isEmpty()){
            return null;
        }

        RulesVO[] rulesVOS = new RulesVO[result.size()];
        baseDao.convToEntity(result, rulesVOS, RulesVO.class,
                new String[]{"rulecode","rulename"});
        return Arrays.asList(rulesVOS);
    }


    /**
     * 查询阶梯
     * @param actcode
     * @return
     */
    private List<LadderVO> getCoupLadder(long actcode){
        List<Object[]> result = baseDao.queryNative(QUERY_PROM_LAD_SQL,
                new Object[]{actcode});

        if(result == null || result.isEmpty()){
            return null;
        }

        LadderVO[] ladderVOS = new LadderVO[result.size()];
        baseDao.convToEntity(result, ladderVOS, LadderVO.class,
                new String[]{"unqid","ladamt","ladnum","offer"});
        return Arrays.asList(ladderVOS);
    }


    /**
     * 查询商品
     * @param actcode
     * @return
     */
    public List<GoodsVO> getCoupGoods(long actcode){

        List<Object[]> result = baseDao.queryNative(QUERY_PROM_GOODS_SQL,
                new Object[]{actcode});

        if(result == null || result.isEmpty()){
            return null;
        }

        GoodsVO[] goodsVOS = new GoodsVO[result.size()];

        baseDao.convToEntity(result, goodsVOS, GoodsVO.class,
                new String[]{"spec","gcode","limitnum",
                        "manuname","standarno","prodname","classname"});
        return Arrays.asList(goodsVOS);
    }

    /**
     * 查询优惠券列表
     * @param appContext
     * @return
     */
    public Result queryCouponList(AppContext appContext){
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

        String coupname = jsonObject.get("coupname").getAsString();
        String rulename = jsonObject.get("rulename").getAsString();

        StringBuilder sqlBuilder = new StringBuilder(QUERY_COUPON_LIST_SQL);
        if(!StringUtils.isEmpty(coupname)){
            sqlBuilder.append(" and coupname like '%");
            sqlBuilder.append(coupname);
            sqlBuilder.append("%' ");
        }

        if(!StringUtils.isEmpty(rulename)){
            sqlBuilder.append(" and rulename like '%");
            sqlBuilder.append(rulename);
            sqlBuilder.append("%' ");
        }

        List<Object[]> queryResult = baseDao.queryNative(pageHolder, page, sqlBuilder.toString());
        if (queryResult == null || queryResult.isEmpty()) return result.success(null);


        CouponListVO[] couponListVOS = new CouponListVO[queryResult.size()];

        baseDao.convToEntity(queryResult, couponListVOS, CouponListVO.class,
                new String[]{"coupno","coupname","glbno","qlfno","qlfval","coupdesc",
                        "periodtype","periodday",
                        "startdate","enddate","ruleno","rulename","cstatus"});

        return result.setQuery(couponListVOS, pageHolder);
    }

}
