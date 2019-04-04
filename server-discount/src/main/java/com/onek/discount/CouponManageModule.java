package com.onek.discount;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.consts.CSTATUS;
import com.onek.context.AppContext;
import com.onek.discount.entity.*;
import com.onek.entitys.Result;
import constant.DSMConst;
import dao.BaseDAO;
import global.GenIdUtil;
import org.hyrdpf.ds.AppConfig;
import util.GsonUtils;
import util.ModelUtil;
import util.StringUtils;

import java.math.BigDecimal;
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

    static {
        /** 初始化LOG4J2日志环境 */
        AppConfig.initLogger();
        /** 初始化应用程序环境，如数据源等 */
        AppConfig.initialize();
    }

    //新增优惠券
    private final String INSERT_COUPON_SQL = "insert into {{?" + DSMConst.TD_PROM_COUPON + "}} "
            + "(unqid,coupname,glbno,qlfno,qlfval,coupdesc,periodtype,"
            + "periodday,startdate,enddate,ruleno,validday,validflag,cstatus) "
            + "values(?,?,?,?,?,"
            + "?,?,?,?,?,?,?,?,?)";

    //修改优惠券
    private static final String UPDATE_COUPON_SQL = "update {{?" + DSMConst.TD_PROM_COUPON + "}} set coupname=?,"
            + "glbno=?,qlfno=?,qlfval=?,coupdesc=?,periodtype=?,"
            + "periodday=?,startdate=?,enddate=?,ruleno=?,validday=?,validflag=? where cstatus&1=0 "
            + " and unqid=? ";


    //新增场次
    private final String INSERT_TIME_SQL = "insert into {{?" + DSMConst.TD_PROM_TIME + "}} "
            + "(unqid,actcode,sdate,edate) "
            + " values(?,?,?,?)";

    //删除场次
    private static final String DEL_TIME_SQL = "update {{?" + DSMConst.TD_PROM_TIME + "}} set cstatus=cstatus|1 "
            + " where cstatus&1=0 and actcode=?";

    //新增活动商品
    private final String INSERT_ASS_DRUG_SQL = "insert into {{?" + DSMConst.TD_PROM_ASSDRUG + "}} "
            + "(unqid,actcode,gcode,menucode,actstock,limitnum) "
            + " values(?,?,?,?,?,?)";

    //删除商品
    private static final String DEL_ASS_DRUG_SQL = "update {{?" + DSMConst.TD_PROM_ASSDRUG + "}} set cstatus=cstatus|1 "
            + " where cstatus&1=0 and actcode=?";

    //优惠阶梯
    private final String INSERT_LAD_OFF_SQL = "insert into {{?" + DSMConst.TD_PROM_LADOFF + "}} "
            + "(unqid,actcode,ruleno,ladamt,ladnum,offer) "
            + " values(?,?,?,?,?,?)";

    //删除阶梯
    private static final String DEL_LAD_OFF__SQL = "update {{?" + DSMConst.TD_PROM_LADOFF + "}} set cstatus=cstatus|1 "
            + " where cstatus&1=0 and actcode=?";

    //优惠赠换商品
    private final String INSERT_ASS_GIFT_SQL = "insert into {{?" + DSMConst.TD_PROM_ASSGIFT + "}} "
            + "(unqid,giftname,giftdesc)"
            + " values(?,?,?)";

    //查询优惠券详情
    private final String QUERY_COUPON_SQL = "select unqid,coupname,glbno,qlfno,qlfval,coupdesc,periodtype," +
            "periodday,DATE_FORMAT(startdate,'%Y-%m-%d') startdate,DATE_FORMAT(enddate,'%Y-%m-%d') enddate," +
            "ruleno,validday,validflag from {{?"+ DSMConst.TD_PROM_COUPON +"}} where cstatus&1=0 and unqid = ? ";


    //查询优惠券列表
    private final String QUERY_COUPON_LIST_SQL = "select unqid,coupname,glbno,qlfno,qlfval,coupdesc,periodtype," +
            "periodday,DATE_FORMAT(startdate,'%Y-%m-%d') startdate,DATE_FORMAT(enddate,'%Y-%m-%d') enddate," +
            "ruleno,rulename,cop.cstatus from {{?"+ DSMConst.TD_PROM_COUPON +"}} cop left join" +
            " {{?"+ DSMConst.TD_PROM_RULE +"}}  ru on cop.ruleno = ru.rulecode  where cop.cstatus&1=0 ";



    private final String QUERY_PROM_TIME_SQL = "select unqid,sdate,edate from {{?" + DSMConst.TD_PROM_TIME+"}} where actcode = ? and cstatus&1=0";

    private final String QUERY_PROM_RULE_SQL = "select rulecode,rulename from {{?" + DSMConst.TD_PROM_RULE+"}} where cstatus&1=0 ";

    private final String QUERY_PROM_LAD_SQL = "select unqid,ladamt,ladnum,offer from {{?" + DSMConst.TD_PROM_LADOFF+"}} where actcode = ? and cstatus&1=0 ";

    private final String QUERY_PROM_GOODS_SQL = "select `spec`,gcode,limitnum,,manuname,standarno,prodname,classname,convert(vatp/100,decimal(10,2)) price,actstock " +
            " from {{?" + DSMConst.TD_PROM_ASSDRUG+"}} pdrug" +
            " left join {{?" + DSMConst.TD_PROD_SKU+"}} psku on pdrug.gcode = psku.sku " +
            " left join {{?" + DSMConst.TD_PROD_SPU+"}} pspu on psku.spu = pspu.spu "+
            " left join {{?" + DSMConst.TD_PROD_MANU+"}} pmun on pmun.manuno = pspu.manuno "+
            " left join {{?" + DSMConst.D_PRODUCE_CLASS+"}} dpr on pdrug.gcode = dpr.classid "+
            " where actcode = ? and pdrug.cstatus&1=0 ";

    //启用
    private static final String OPEN_COUPON =
            " UPDATE {{?" + DSMConst.TD_PROM_COUPON + "}}"
                    + " SET cstatus = cstatus & " + ~CSTATUS.CLOSE
                    + " WHERE unqid = ? ";

    //停用
    private static final String CLOSE_COUPON =
            " UPDATE {{?" + DSMConst.TD_PROM_COUPON + "}}"
                    + " SET cstatus = cstatus | " + CSTATUS.CLOSE
                    + " WHERE unqid = ? ";

    //删除
    private static final String DELETE_COUPON =
            " UPDATE {{?" + DSMConst.TD_PROM_COUPON + "}}"
                    + " SET cstatus = cstatus | " + CSTATUS.DELETE
                    + " WHERE unqid = ? ";



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

        int ret = baseDao.updateNative(INSERT_COUPON_SQL,new Object[]{
                unqid,couponVO.getCoupname(),couponVO.getGlbno(),couponVO.getQlfno(),
                couponVO.getQlfval(),couponVO.getDesc(),couponVO.getPeriodtype(),
                couponVO.getPeriodday(),couponVO.getStartdate(),couponVO.getEnddate(),
                couponVO.getRuleno(),couponVO.getValidday(),couponVO.getValidflag(),0
        });
        if (ret > 0) {
            //新增活动场次
            if (couponVO.getTimeVOS() != null && !couponVO.getTimeVOS().isEmpty()) {
                insertTimes(couponVO.getTimeVOS(), unqid);
            }
            //新增阶梯
            if (couponVO.getLadderVOS() != null && !couponVO.getLadderVOS().isEmpty()) {
                insertLadOff(couponVO.getLadderVOS(), unqid);
            }
            //新增活动商品
//            if (couponVO.getAssDrugVOS() != null && !couponVO.getAssDrugVOS().isEmpty()) {
//                insertAssDrug(couponVO.getAssDrugVOS());
//            }
        } else {
            return result.fail("新增失败");
        }
        return result.success("新增成功");
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
                            "startdate", "enddate", "ruleno","validday","validflag"});

        couponVOS[0].setTimeVOS(getTimeVOS(actcode));
        couponVOS[0].setRulesVOS(getCoupRule());
    //    couponVOS[0].setAssDrugVOS(getCoupGoods(actcode));
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

        List<Object[]> result = baseDao.queryNative(QUERY_PROM_GOODS_SQL, actcode);

        if(result == null || result.isEmpty()){
            return null;
        }

        GoodsVO[] goodsVOS = new GoodsVO[result.size()];


        baseDao.convToEntity(result, goodsVOS, GoodsVO.class,
                new String[]{"spec","gcode","limitnum",
                        "manuname","standarno","prodname","classname","price"});
        return Arrays.asList(goodsVOS);
    }


    /**
     * 新增优惠券商品
     * @param assDrugVOS
     */
    private boolean insertAssDrug(List<GoodsVO> assDrugVOS) {

        List<Object[]> assDrugParams = new ArrayList<>();
        for (GoodsVO assDrugVO : assDrugVOS) {
            assDrugParams.add(new Object[]{GenIdUtil.getUnqId(), assDrugVO.getActcode(),assDrugVO.getGcode(),
                    assDrugVO.getMenucode(),assDrugVO.getActstock(),assDrugVO.getLimitnum()});
        }
        int[] result = baseDao.updateBatchNative(INSERT_ASS_DRUG_SQL, assDrugParams, assDrugVOS.size());
        return !ModelUtil.updateTransEmpty(result);
    }


    /**
     * 新增阶梯
     * @param ladderVOS
     * @param actCode
     */
    private void insertLadOff(List<LadderVO> ladderVOS, long actCode) {

        List<Object[]> ladOffParams = new ArrayList<>();
        for (LadderVO ladderVO : ladderVOS) {
            ladOffParams.add(new Object[]{GenIdUtil.getUnqId(), actCode,ladderVO.getRuleno(),
                    ladderVO.getLadamt(),ladderVO.getLadnum(),ladderVO.getOffer()});
        }
        int[] result = baseDao.updateBatchNative(INSERT_LAD_OFF_SQL, ladOffParams, ladderVOS.size());
        boolean b = !ModelUtil.updateTransEmpty(result);
    }


    /**
     * 新增活动场次
     * @param timeVOS
     * @param actCode
     */
    private void insertTimes(List<TimeVO> timeVOS, long actCode) {
        List<Object[]> timeParams = new ArrayList<>();
        for (TimeVO timeVO : timeVOS) {
            timeParams.add(new Object[]{GenIdUtil.getUnqId(), actCode,
                    timeVO.getSdate(), timeVO.getEdate()});
        }
        int[] result = baseDao.updateBatchNative(INSERT_TIME_SQL, timeParams, timeVOS.size());
        boolean b = !ModelUtil.updateTransEmpty(result);
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


    /**
     * 优惠券修改
     * @param appContext
     * @return
     */
    private Result updateCoupon(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        CouponVO couponVO = GsonUtils.jsonToJavaBean(json, CouponVO.class);
        long actCode = couponVO.getCoupno();
        //新增活动

        int ret = baseDao.updateNative(UPDATE_COUPON_SQL,new Object[]{couponVO.getCoupname(),
        couponVO.getGlbno(),couponVO.getQlfno(),couponVO.getQlfval(),
        couponVO.getDesc(),couponVO.getPeriodtype(),couponVO.getPeriodday(),
        couponVO.getStartdate(),couponVO.getEnddate(),couponVO.getRuleno(),couponVO.getValidday(),
                couponVO.getValidflag(),actCode});

        if (ret > 0) {
            //新增活动场次
            if (couponVO.getTimeVOS() != null && !couponVO.getTimeVOS().isEmpty()) {
                if (baseDao.updateNative(DEL_TIME_SQL,actCode) > 0) {
                    insertTimes(couponVO.getTimeVOS(), actCode);
                }
            }
            //新增阶梯
            if (couponVO.getLadderVOS() != null && !couponVO.getLadderVOS().isEmpty()) {
                if (baseDao.updateNative(DEL_LAD_OFF__SQL, actCode) > 0) {
                    insertLadOff(couponVO.getLadderVOS(), actCode);
                }
            }
            //新增活动商品
//            if (couponVO.getAssDrugVOS() != null && !couponVO.getAssDrugVOS().isEmpty()) {
//                if (baseDao.updateNative(DEL_ASS_DRUG_SQL,actCode) > 0) {
//                    insertAssDrug(couponVO.getAssDrugVOS());
//                }
//            }
        } else {
            result.fail("修改失败");
        }
        return result.success("修改成功");
    }


    /**
     * 更新优惠券状态
     * @param appContext 0 启用  32 停用  1 删除
     * @return
     */
    public Result updateCouponStatus(AppContext appContext){
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        Result result = new Result();

        long actcode = jsonObject.get("actcode").getAsLong();
        int cstatus = jsonObject.get("cstatus").getAsInt();
        int ret = 0;
        switch (cstatus){
            case 0:
                ret = baseDao.updateNative(OPEN_COUPON,actcode);
                break;
            case 1:
                ret = baseDao.updateNative(DELETE_COUPON,actcode);
                break;
            case 32:
                ret = baseDao.updateNative(CLOSE_COUPON,actcode);
        }

        return ret > 0 ? result.success("操作成功") : result.fail("操作失败");

    }


    /**
     * 查询商品列表
     * @param appContext
     * @return
     */
    public Result queryGoodList(AppContext appContext){

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

        long actcode = jsonObject.get("actcode").getAsLong();

        List<Object[]> queryResult = baseDao.queryNative(pageHolder, page, QUERY_PROM_GOODS_SQL,actcode);

        if(queryResult == null || queryResult.isEmpty()){
            return result.success(null);
        }
        GoodsVO[] goodsVOS = new GoodsVO[queryResult.size()];

        baseDao.convToEntity(queryResult, goodsVOS, GoodsVO.class,
                new String[]{"spec","gcode","limitnum",
                        "manuname","standarno","prodname","classname","price","actstock"});

        return result.setQuery(goodsVOS, pageHolder);
    }


    /**
     * 新增修改商品
     * @param appContext
     */
    private Result optGoods(AppContext appContext) {

        String json = appContext.param.json;
        Result result = new Result();

        List<GoodsVO> assDrugVOS = GsonUtils.string2List(json);

        if (assDrugVOS == null || assDrugVOS.isEmpty()){
            return result.fail("操作失败");
        }

        baseDao.updateNative(DEL_ASS_DRUG_SQL,assDrugVOS.get(0).getActcode());

        if(insertAssDrug(assDrugVOS)){
            return result.success("操作成功");
        }
        return result.fail("操作失败");
    }

}
