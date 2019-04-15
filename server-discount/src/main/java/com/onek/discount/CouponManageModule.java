package com.onek.discount;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.google.gson.*;
import com.onek.annotation.UserPermission;
import com.onek.consts.CSTATUS;
import com.onek.context.AppContext;
import com.onek.discount.entity.*;
import com.onek.entitys.Result;
import constant.DSMConst;
import dao.BaseDAO;
import global.GenIdUtil;
import global.IceRemoteUtil;
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


    //新增优惠券
    private final String INSERT_COUPON_SQL = "insert into {{?" + DSMConst.TD_PROM_COUPON + "}} "
            + "(unqid,coupname,glbno,qlfno,qlfval,coupdesc,periodtype,"
            + "periodday,startdate,enddate,brulecode,validday,validflag,cstatus,actstock) "
            + "values(?,?,?,?,?,"
            + "?,?,?,?,?,?,?,?,?,?)";

    //新增活动优惠券
    private final String INSERT_ASSCOUPON_SQL = "insert into {{?" + DSMConst.TD_PROM_COUPON + "}} "
            + "(unqid,glbno,coupdesc,brulecode,validday,validflag,cstatus,actstock,reqflag) "
            + " values(?,?,?,?,?,?,?,?,?)";

    //修改优惠券
    private static final String UPDATE_COUPON_SQL = "update {{?" + DSMConst.TD_PROM_COUPON + "}} set coupname=?,"
            + "glbno=?,qlfno=?,qlfval=?,coupdesc=?,periodtype=?,"
            + "periodday=?,startdate=?,enddate=?,brulecode=?,validday=?,validflag=?,actstock=? where cstatus&1=0 "
            + " and unqid=? ";


    //修改活动优惠券
    private static final String UPDATE_ASSCOUPON_SQL = "update {{?" + DSMConst.TD_PROM_COUPON + "}} set "
            + "glbno=?,coupdesc=?,brulecode=?,validday=?,validflag=?,actstock=?,reqflag=? ";


    //新增场次
    private final String INSERT_TIME_SQL = "insert into {{?" + DSMConst.TD_PROM_TIME + "}} "
            + "(unqid,actcode,sdate,edate) "
            + " values(?,?,?,?)";

    //删除场次
    private static final String DEL_TIME_SQL = "update {{?" + DSMConst.TD_PROM_TIME + "}} set cstatus=cstatus|1 "
            + " where cstatus&1=0 and actcode=?";


    //新增活动商品
    private final String INSERT_ASS_DRUG_SQL = "insert into {{?" + DSMConst.TD_PROM_ASSDRUG + "}} "
            + "(unqid,actcode,gcode,menucode,actstock,limitnum,price) "
            + " values(?,?,?,?,?,?,?)";

    //删除商品
    private static final String DEL_ASS_DRUG_SQL = "update {{?" + DSMConst.TD_PROM_ASSDRUG + "}} set cstatus=cstatus|1 "
            + " where cstatus&1=0 or  and actcode=?";

    //优惠阶梯
    private final String INSERT_LAD_OFF_SQL = "insert into {{?" + DSMConst.TD_PROM_LADOFF + "}} "
            + "(unqid,ladamt,ladnum,offer,offercode) "
            + " values(?,?,?,?,?)";

    //删除阶梯
    private static final String DEL_LAD_OFF_SQL = "update {{?" + DSMConst.TD_PROM_LADOFF + "}} set cstatus=cstatus|1 "
            + " where cstatus&1=0 ";

    //优惠赠换商品
    private final String INSERT_ASS_GIFT_SQL = "insert into {{?" + DSMConst.TD_PROM_ASSGIFT + "}} "
            + "(unqid,giftname,giftdesc)"
            + " values(?,?,?)";

    /**
     * 查询优惠券详情
     */
    private final String QUERY_COUPON_SQL = "select unqid,coupname,glbno,qlfno,qlfval,coupdesc,periodtype," +
            "periodday,DATE_FORMAT(startdate,'%Y-%m-%d') startdate,DATE_FORMAT(enddate,'%Y-%m-%d') enddate," +
            "cop.brulecode,validday,validflag,rulename,actstock from {{?"+ DSMConst.TD_PROM_COUPON +"}}  cop left join " +
            "  {{?"+ DSMConst.TD_PROM_RULE +"}} ru on cop.brulecode = ru.brulecode  where cop.cstatus&1=0 and unqid = ? ";


    /**
     * 查询活动优惠券详情
     */
    private final String QUERY_ASSCOUPON_SQL = "select unqid,glbno,coupdesc," +
            "cop.brulecode,validday,validflag,rulename,actstock,reqflag,cop.cstatus from {{?"+ DSMConst.TD_PROM_COUPON +"}}  cop left join " +
            "  {{?"+ DSMConst.TD_PROM_RULE +"}} ru on cop.brulecode = ru.brulecode  where cop.cstatus&1=0 and unqid = ? ";


    /**
     * 查询优惠券列表
     */
    private final String QUERY_COUPON_LIST_SQL = "select unqid,coupname,glbno,qlfno,qlfval,coupdesc,periodtype," +
            "periodday,DATE_FORMAT(startdate,'%Y-%m-%d') startdate,DATE_FORMAT(enddate,'%Y-%m-%d') enddate," +
            "cop.brulecode,rulename,cop.cstatus,actstock,validday,validflag from {{?"+ DSMConst.TD_PROM_COUPON +"}} cop left join" +
            " {{?"+ DSMConst.TD_PROM_RULE +"}}  ru on cop.brulecode = ru.brulecode " +
            " where cop.cstatus & ? > 0 and cop.cstatus&1=0 ";


    /**
     * 查询活动优惠券列表
     */
    private final String QUERY_ASSCOUPON_LIST_SQL = "select unqid,glbno,coupdesc," +
            "cop.brulecode,rulename,validday,validflag,cop.cstatus,actstock,reqflag from {{?"+ DSMConst.TD_PROM_COUPON +"}} cop left join" +
            " {{?"+ DSMConst.TD_PROM_RULE +"}}  ru on cop.brulecode = ru.brulecode " +
            " where cop.cstatus & 128 > 0 and cop.cstatus&1=0 ";



    private final String QUERY_PROM_TIME_SQL = "select unqid,sdate,edate from {{?" + DSMConst.TD_PROM_TIME+"}} where actcode = ? and cstatus&1=0";

    private final String QUERY_PROM_RULE_SQL = "select rulecode,rulename from {{?" + DSMConst.TD_PROM_RULE+"}} where cstatus&1=0 ";

    private final String QUERY_PROM_LAD_SQL = "select unqid,convert(ladamt/100,decimal(10,2)) ladamt,ladnum,convert(offer/100,decimal(10,2)),offercode from {{?" + DSMConst.TD_PROM_LADOFF+"}} where cstatus&1=0 ";

    private final String QUERY_PROM_GOODS_SQL = "select pdrug.unqid,pdrug.actcode,`spec`,gcode,limitnum,manuname,standarno,prodname,classname,convert(pdrug.price/100,decimal(10,2)) price,actstock " +
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

    private static final String DEL_ASS_GIFT_SQL = "update {{?" + DSMConst.TD_PROM_ASSGIFT + "}} set cstatus=cstatus|1 "
            + " where cstatus&1=0 and offercode=?";


    /**
     * 查询发布的优惠券
     */
//    private static final String QUERY_COUP_PUB = "select tpcp.unqid coupno,tpcp.brulecode,rulename,ladamt," +
//            "ladnum,offer,validday,validflag,offercode from {{?" + DSMConst.TD_PROM_COUPON + "}} tpcp " +
//            " inner join td_prom_rule tpcr on tpcp.brulecode = tpcr.brulecode "+
//            " inner join {{?" + DSMConst.TD_PROM_LADOFF + "}} prf on tpcp.brulecode = left(prf.offercode,4) "+
//            " where tpcp.cstatus & 33 = 0 and tpcr.cstatus & 33 = 0 and prf.cstatus & 33 = 0 "+
//            " and 1 = fun_prom_cycle(tpcp.unqid,periodtype,periodday,DATE_FORMAT(NOW(),'%m%d'),0) " +
//            " and actstock > 0 and not exists (select 1 from {{?" + DSMConst.TB_PROM_COURCD + "}} "+
//            " where coupno = tpcp.unqid and offercode = prf.offercode  and compid = ? and cstatus & 1 = 0)";



//    private static final String QUERY_COUP_PUB = "select tpcp.unqid coupno,tpcp.brulecode,rulename," +
//            "validday,validflag from {{?" + DSMConst.TD_PROM_COUPON + "}} tpcp " +
//            " inner join {?" + DSMConst.TD_PROM_RULE + "}} tpcr on tpcp.brulecode = tpcr.brulecode "+
//            " where tpcp.cstatus & 33 = 0 and tpcr.cstatus & 33 = 0  "+
//            " and 1 = fun_prom_cycle(tpcp.unqid,periodtype,periodday,DATE_FORMAT(NOW(),'%m%d'),0) " +
//            " and not exists (select 1 from {{?" + DSMConst.TB_PROM_COURCD + "}} "+
//            " where coupno = tpcp.unqid and offercode = prf.offercode  and compid = ? and cstatus & 1 = 0)";

    private static final String QUERY_COUP_PUB = "select coupno,brulecode,rulename,validday,validflag,glbno,goods from ("+
            "select coupno,brulecode,rulename,validday,validflag,periodtype,periodday,actstock,glbno,goods from ("+
            "select tpcp.unqid coupno,tpcp.brulecode,rulename,validday,validflag,periodtype,periodday,tpcp.actstock,glbno,0 goods "+
            "from {{?" + DSMConst.TD_PROM_COUPON + "}} tpcp inner join {{?" + DSMConst.TD_PROM_RULE + "}}" +
            " tpcr on tpcp.brulecode = tpcr.brulecode  "+
            " inner join {{?" + DSMConst.TD_PROM_ASSDRUG + "}} assd on assd.actcode = tpcp.unqid "+
            " where assd.gcode = 0  and tpcp.cstatus & 64 > 0 and tpcp.cstatus & 33 = 0 and tpcr.cstatus & 33 = 0 and assd.cstatus & 33 = 0 "+
            " union "+
            "select tpcp.unqid coupno,tpcp.brulecode,rulename,validday,validflag,periodtype,periodday,tpcp.actstock,glbno,1 goods  from {{?"+
            DSMConst.TD_PROM_COUPON +"}} tpcp inner join {{?" + DSMConst.TD_PROM_RULE + "}} tpcr on tpcp.brulecode = tpcr.brulecode " +
            " inner join {{?" +DSMConst.TD_PROM_ASSDRUG+"}} assd on assd.actcode = tpcp.unqid "+
            " where assd.gcode = ? "+
            " and tpcp.cstatus & 64 > 0 and tpcp.cstatus & 33 = 0 and tpcr.cstatus & 33 = 0 and assd.cstatus & 33 = 0 ) a "+
            " where a.actstock > 0 and 1 = fun_prom_cycle(coupno,periodtype,periodday,DATE_FORMAT(NOW(),'%m%d'),0) "+
            " and not exists (select 1 from td_prom_courcd where coupno = a.coupno and compid = ? and cstatus & 1 = 0)) a ";


    private static final String QUERY_COUP_CNT_PUB = "select count(1) from ("+
            "select coupno,brulecode,rulename,validday,validflag,periodtype,periodday,actstock,glbno,goods from ("+
            "select tpcp.unqid coupno,tpcp.brulecode,rulename,validday,validflag,periodtype,periodday,tpcp.actstock,glbno,0 goods "+
            "from {{?" + DSMConst.TD_PROM_COUPON + "}} tpcp inner join {{?" + DSMConst.TD_PROM_RULE + "}}" +
            " tpcr on tpcp.brulecode = tpcr.brulecode  "+
            " inner join {{?" + DSMConst.TD_PROM_ASSDRUG + "}} assd on assd.actcode = tpcp.unqid "+
            " where assd.gcode = 0  and tpcp.cstatus & 64 > 0 and tpcp.cstatus & 33 = 0 and tpcr.cstatus & 33 = 0 and assd.cstatus & 33 = 0 "+
            " union "+
            "select tpcp.unqid coupno,tpcp.brulecode,rulename,validday,validflag,periodtype,periodday,tpcp.actstock,glbno,1 goods  from {{?"+
            DSMConst.TD_PROM_COUPON +"}} tpcp inner join {{?" + DSMConst.TD_PROM_RULE + "}} tpcr on tpcp.brulecode = tpcr.brulecode " +
            " inner join {{?" +DSMConst.TD_PROM_ASSDRUG+"}} assd on assd.actcode = tpcp.unqid "+
            " where assd.gcode = ? "+
            " and tpcp.cstatus & 64 > 0 and tpcp.cstatus & 33 = 0 and tpcr.cstatus & 33 = 0 and assd.cstatus & 33 = 0 ) a "+
            " where a.actstock > 0 and 1 = fun_prom_cycle(coupno,periodtype,periodday,DATE_FORMAT(NOW(),'%m%d'),0) "+
            " and not exists (select 1 from td_prom_courcd where coupno = a.coupno and compid = ? and cstatus & 1 = 0)) a ";




    private static final String INSERT_COURCD =  "insert into {{?" + DSMConst.TB_PROM_COURCD + "}}" +
            " (unqid,coupno,compid,offercode,gettime) values (?,?,?,?,now())";

    private static final String DEL_COURCD =  "update {{?" + DSMConst.TB_PROM_COURCD + "}}" +
            " SET cstatus = cstatus | " + CSTATUS.DELETE +" WHERE unqid = ? ";


    //扣减优惠券库存
    private static final String UPDATE_COUPON_STOCK = " update {{?" + DSMConst.TD_PROM_COUPON + "}}"
                    + " set actstock = actstock - 1 " +
            "where unqid = ? and actstock > 0 and cstatus & 1 = 0";

    //查询库存版本号
//    private static final String SELECT_COUPON_VER_ = " select ver from {{?" + DSMConst.TD_PROM_COUPON + "}}"
//            + "  where unqid = ? cstatus & 33 = 0";


    /**
     * @description 优惠券新增
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @time  2019/4/2 14:34
     * @version 1.1.1
     **/
    @UserPermission(ignore = true)
    public Result insertCoupon(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        CouponVO couponVO = GsonUtils.jsonToJavaBean(json, CouponVO.class);
        long unqid = GenIdUtil.getUnqId();
        List<Object[]> parmList = new ArrayList<>();
        parmList.add(new Object[]{
                unqid,couponVO.getCoupname(),couponVO.getGlbno(),couponVO.getQlfno(),
                couponVO.getQlfval(),couponVO.getDesc(),couponVO.getPeriodtype(),
                couponVO.getPeriodday(),couponVO.getStartdate(),couponVO.getEnddate(),
                couponVO.getRuleno(),couponVO.getValidday(),couponVO.getValidflag(),
                couponVO.getCstatus(),couponVO.getActstock()});
        parmList.add(new Object[]{GenIdUtil.getUnqId(),unqid,0,0,0,0,0});

        int[] coupRet = baseDao.updateTransNative(new String[]{INSERT_COUPON_SQL, INSERT_ASS_DRUG_SQL},
                parmList);
        if (!ModelUtil.updateTransEmpty(coupRet)) {
            //新增活动场次
            if (couponVO.getTimeVOS() != null && !couponVO.getTimeVOS().isEmpty()) {
                insertTimes(couponVO.getTimeVOS(), unqid);
            }
            //新增阶梯
            if (couponVO.getLadderVOS() != null && !couponVO.getLadderVOS().isEmpty()) {
                insertLadOff(couponVO.getLadderVOS(),couponVO.getRuleno()+""+couponVO.getRulecomp());
            }

        } else {
            return result.fail("新增失败");
        }
        return result.success("新增成功");
    }




    /**
     * @description 活动优惠券新增
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @time  2019/4/2 14:34
     * @version 1.1.1
     **/
    @UserPermission(ignore = true)
    public Result insertAssCoupon(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        CouponAssVO couponAssVO = GsonUtils.jsonToJavaBean(json, CouponAssVO.class);
        long unqid = GenIdUtil.getUnqId();
        int cstatus = couponAssVO.getCstatus();
        if(cstatus == 0){
            cstatus = 128;
        }else{
            cstatus = 128 | 32;
        }


        int ret = baseDao.updateNative(INSERT_ASSCOUPON_SQL,
                 new Object[]{unqid,couponAssVO.getGlbno(),couponAssVO.getCoupdesc(),
                        couponAssVO.getRuleno(),couponAssVO.getValidday(),couponAssVO.getValidflag(),
                         cstatus,couponAssVO.getActstock(),couponAssVO.getReqflag()});
        if (ret > 0) {
            return result.success("新增成功");
        }
        return result.fail("新增失败");
    }


    /**
     * @description 查询活动优惠券详情
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @time  2019/4/2 14:34
     * @version 1.1.1
     **/
    @UserPermission(ignore = true)
    public Result queryAssCoupon(AppContext appContext) {

        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        long actcode = jsonObject.get("actcode").getAsLong();

        List<Object[]> coupResult = baseDao.queryNative(QUERY_ASSCOUPON_SQL,
                new Object[]{actcode});

        CouponAssVO[] couponAssVOS = new CouponAssVO[coupResult.size()];
        if(coupResult == null || coupResult.isEmpty()){
            return  result.success(couponAssVOS);
        }

        baseDao.convToEntity(coupResult, couponAssVOS, CouponAssVO.class,
                new String[]{"coupno", "glbno", "coupdesc",
                        "ruleno", "validday", "validflag", "rulename", "actstock",
                        "reqflag","cstatus"});


        return  result.success(couponAssVOS[0]);
    }





    /**
     * @description 查询优惠券详情
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @time  2019/4/2 14:34
     * @version 1.1.1
     **/
    @UserPermission(ignore = true)
    public Result queryCoupon(AppContext appContext) {

        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        long actcode = jsonObject.get("actcode").getAsLong();

        List<Object[]> coupResult = baseDao.queryNative(QUERY_COUPON_SQL,
                new Object[]{actcode});

        CouponVO[] couponVOS = new CouponVO[coupResult.size()];
        if(coupResult == null || coupResult.isEmpty()){
            return  result.success(couponVOS);
        }
        baseDao.convToEntity(coupResult, couponVOS, CouponVO.class,
                    new String[]{"coupno", "coupname", "glbno",
                            "qlfno", "qlfval", "desc", "periodtype", "periodday",
                            "startdate", "enddate", "ruleno","validday","validflag",
                            "rulename","actstock"});

        couponVOS[0].setTimeVOS(getTimeVOS(actcode));
        couponVOS[0].setLadderVOS(getCoupLadder(couponVOS[0],couponVOS[0].getRuleno()));
        couponVOS[0].setActiveRule(getRules(couponVOS[0].getRuleno()));

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
     * @param rulecode
     * @return
     */
    private List<LadderVO> getCoupLadder(CouponVO couponVO,int rulecode){
        StringBuilder sb = new StringBuilder(QUERY_PROM_LAD_SQL);
        sb.append(" and offercode like '").append(rulecode).append("%'");
        List<Object[]> result = baseDao.queryNative(sb.toString());

        if(result == null || result.isEmpty()){
            return null;
        }

        LadderVO[] ladderVOS = new LadderVO[result.size()];
        baseDao.convToEntity(result, ladderVOS, LadderVO.class,
                new String[]{"unqid","ladamt","ladnum","offer","offercode"});

        String offerCode = ladderVOS[0].getOffercode() + "";
        couponVO.setRulecomp(Integer.parseInt(offerCode.substring(4,5)));
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
                    assDrugVO.getMenucode(),assDrugVO.getActstock(),assDrugVO.getLimitnum(),assDrugVO.getPrice()*100});
        }
        int[] result = baseDao.updateBatchNative(INSERT_ASS_DRUG_SQL, assDrugParams, assDrugVOS.size());
        return !ModelUtil.updateTransEmpty(result);
    }


    /**
     * 新增阶梯
     * @param ladderVOS
     * @param laddrno
     */
    private void insertLadOff(List<LadderVO> ladderVOS,String laddrno) {

        List<Object[]> ladOffParams = new ArrayList<>();
        int [] ladernos = CommonModule.getLaderNo(laddrno, ladderVOS.size());

        for (int i = 0; i < ladderVOS.size(); i++) {
            ladOffParams.add(new Object[]{GenIdUtil.getUnqId(),
                    ladderVOS.get(i).getLadamt()*100,ladderVOS.get(i).getLadnum(),ladderVOS.get(i).getOffer()*100,ladernos[i]});
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
    @UserPermission(ignore = true)
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

        int coupType = jsonObject.get("couptype").getAsInt();
        String coupname = jsonObject.get("coupname").getAsString();
        int rulecode = jsonObject.get("rulecode").getAsInt();

        StringBuilder sqlBuilder = new StringBuilder(QUERY_COUPON_LIST_SQL);
        if(!StringUtils.isEmpty(coupname)){
            sqlBuilder.append(" and coupname like '%");
            sqlBuilder.append(coupname);
            sqlBuilder.append("%' ");
        }

        if(rulecode != 0){
            sqlBuilder.append(" and rulecode = ");
            sqlBuilder.append(rulecode);
        }

        List<Object[]> queryResult = baseDao.queryNative(pageHolder, page, sqlBuilder.toString(),coupType);
        CouponListVO[] couponListVOS = new CouponListVO[queryResult.size()];
        if (queryResult == null || queryResult.isEmpty()) {
            return result.setQuery(couponListVOS, pageHolder);
        }
        baseDao.convToEntity(queryResult, couponListVOS, CouponListVO.class,
                new String[]{"coupno","coupname","glbno","qlfno","qlfval","desc",
                        "periodtype","periodday",
                        "startdate","enddate","ruleno","rulename","cstatus","actstock","validday","validflag"});

        return result.setQuery(couponListVOS, pageHolder);
    }


    /**
     * 查询活动优惠券列表
     * @param appContext
     * @return
     */
    @UserPermission(ignore = true)
    public Result queryAssCouponList(AppContext appContext){
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

        int rulecode = jsonObject.get("rulecode").getAsInt();



        StringBuilder sqlBuilder = new StringBuilder(QUERY_ASSCOUPON_LIST_SQL);

        if(rulecode != 0){
            sqlBuilder.append(" and rulecode = ");
            sqlBuilder.append(rulecode);
        }

        List<Object[]> queryResult = baseDao.queryNative(pageHolder, page, sqlBuilder.toString());
        CouponAssVO[] couponListVOS = new CouponAssVO[queryResult.size()];
        if (queryResult == null || queryResult.isEmpty()) {
            return result.setQuery(couponListVOS, pageHolder);
        }

        baseDao.convToEntity(queryResult, couponListVOS, CouponAssVO.class,
                new String[]{"coupno","glbno","coupdesc",
                        "ruleno","rulename","validday","validflag","cstatus","actstock","reqflag"});

        return result.setQuery(couponListVOS, pageHolder);
    }


    /**
     * 优惠券修改
     * @param appContext
     * @return
     */
    @UserPermission(ignore = true)
    public Result updateCoupon(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        CouponVO couponVO = GsonUtils.jsonToJavaBean(json, CouponVO.class);
        long actCode = couponVO.getCoupno();
        //新增活动

        int ret = baseDao.updateNative(UPDATE_COUPON_SQL,new Object[]{couponVO.getCoupname(),
        couponVO.getGlbno(),couponVO.getQlfno(),couponVO.getQlfval(),
        couponVO.getDesc(),couponVO.getPeriodtype(),couponVO.getPeriodday(),
        couponVO.getStartdate(),couponVO.getEnddate(),couponVO.getRuleno(),couponVO.getValidday(),
                couponVO.getValidflag(),couponVO.getActstock(),actCode});

        if (ret > 0) {
            //新增活动场次
            if (couponVO.getTimeVOS() != null && !couponVO.getTimeVOS().isEmpty()) {
                if (baseDao.updateNative(DEL_TIME_SQL,actCode) > 0) {
                    insertTimes(couponVO.getTimeVOS(), actCode);
                }
            }
            //新增阶梯
            if (couponVO.getLadderVOS() != null && !couponVO.getLadderVOS().isEmpty()) {
                StringBuilder sb = new StringBuilder(DEL_LAD_OFF_SQL);
                sb.append(" and offercode like '").append(couponVO.getRuleno()).append("%'");
                if (baseDao.updateNative(sb.toString()) > 0) {
                    insertLadOff(couponVO.getLadderVOS(),
                            couponVO.getRuleno()+""+couponVO.getRulecomp());
                }
            }

        } else {
            return result.fail("修改失败");
        }
        return result.success("修改成功");
    }


    /**
     * 活动优惠券修改
     * @param appContext
     * @return
     */
    @UserPermission(ignore = true)
    public Result updateAssCoupon(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        CouponAssVO couponVO = GsonUtils.jsonToJavaBean(json, CouponAssVO.class);
        long actCode = couponVO.getCoupno();

        StringBuilder sb = new StringBuilder(UPDATE_ASSCOUPON_SQL);
        int cstatus = couponVO.getCstatus();

        if(cstatus == 0){
            sb.append(",cstatus = cstatus & ").append(~CSTATUS.CLOSE);
        }else{
            sb.append(",cstatus = cstatus | ").append(CSTATUS.CLOSE);
        }
        sb.append(" where cstatus&1=0 and unqid=? ");


        //新增活动
        int ret = baseDao.updateNative(sb.toString(),new Object[]{
                couponVO.getGlbno(),couponVO.getCoupdesc(),couponVO.getRuleno(),couponVO.getValidday(),
                couponVO.getValidflag(),couponVO.getActstock(),couponVO.getReqflag(),actCode});

        if (ret > 0) {
            return result.success("修改成功");
        }
        return result.fail("修改失败");

    }


    /**
     * 更新优惠券状态
     * @param appContext 0 启用  32 停用  1 删除
     * @return
     */
    @UserPermission(ignore = true)
    public Result updateCouponStatus(AppContext appContext){
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        Result result = new Result();

        long actcode = jsonObject.get("actcode").getAsLong();
        int brulecode = jsonObject.get("brulecode").getAsInt();
        int cstatus = jsonObject.get("cstatus").getAsInt();
        int ret = 0;
        switch (cstatus){
            case 0:
                ret = baseDao.updateNative(OPEN_COUPON,actcode);
                break;
            case 1:
                List<Object[]> params = new ArrayList<>();
                params.add(new Object[]{actcode});
                params.add(new Object[]{});
                StringBuilder sb = new StringBuilder(DEL_LAD_OFF_SQL);
                sb.append(" and offercode like '").append(brulecode).append("%'");
                int [] rets = baseDao.updateTransNative(new String[]{DELETE_COUPON,sb.toString()},params);
                if(!ModelUtil.updateTransEmpty(rets)){
                    ret = 1;
                }
                break;
            case 32:
                ret = baseDao.updateNative(CLOSE_COUPON,actcode);
        }

        return ret > 0 ? result.success("操作成功") : result.fail("操作失败");

    }



    /**
     * 更新活动优惠券状态
     * @param appContext 0 启用  32 停用  1 删除
     * @return
     */
    @UserPermission(ignore = true)
    public Result updateAssCouponStatus(AppContext appContext){
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
    @UserPermission(ignore = true)
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

        GoodsVO[] goodsVOS = new GoodsVO[queryResult.size()];
        if(queryResult == null || queryResult.isEmpty()){
            return result.setQuery(goodsVOS, pageHolder);
        }
     //   GoodsVO[] goodsVOS = new GoodsVO[queryResult.size()];

        baseDao.convToEntity(queryResult, goodsVOS, GoodsVO.class,
                new String[]{"unqid","actcode","spec","gcode","limitnum",
                        "manuname","standarno","prodname","classname","price","actstock"});

        return result.setQuery(goodsVOS, pageHolder);
    }


    /**
     * 新增修改商品
     * @param appContext
     */
    @UserPermission(ignore = true)
    public Result optGoods(AppContext appContext) {

        String json = appContext.param.json;
        Result result = new Result();
        JsonParser jsonParser = new JsonParser();
        JsonArray jsonArray = jsonParser.parse(json).getAsJsonArray();
        List<GoodsVO> assDrugVOS = new ArrayList<>();
        Gson gson = new Gson();
        for (JsonElement goodvo : jsonArray){
            GoodsVO goodsVO = gson.fromJson(goodvo, GoodsVO.class);
            assDrugVOS.add(goodsVO);
        }

        if (assDrugVOS == null || assDrugVOS.isEmpty()){
            return result.fail("操作失败");
        }

        baseDao.updateNative(DEL_ASS_DRUG_SQL,assDrugVOS.get(0).getActcode());

        if(insertAssDrug(assDrugVOS)){
            return result.success("操作成功");
        }
        return result.fail("操作失败");
    }


    private List<RulesVO> getRules(int bRuleCode) {
        int code = Integer.parseInt((bRuleCode + "").substring(0,3));
        String selectSQL = "select brulecode,rulename from {{?" + DSMConst.TD_PROM_RULE + "}} a where cstatus&1=0 "
                + " and brulecode REGEXP '^2' and  NOT EXISTS(select brulecode from {{?"
                + DSMConst.TD_PROM_COUPON +"}} b where cstatus&1=0 and cstatus&64>0 and a.brulecode = b.brulecode and brulecode "
                + " REGEXP '^2' and enddate>CURRENT_DATE and a.brulecode<>"+bRuleCode+")";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        RulesVO[] rulesVOS = new RulesVO[queryResult.size()];
        baseDao.convToEntity(queryResult, rulesVOS, RulesVO.class, new String[]{"brulecode", "rulename"});
        return Arrays.asList(rulesVOS);
    }


    /**
     * 查询优惠券发布列表
     * @param appContext
     * @return
     */
    @UserPermission(ignore = true)
    public Result queryCouponPub(AppContext appContext){
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int pageSize = jsonObject.get("pageSize").getAsInt();
        int pageIndex = jsonObject.get("pageNo").getAsInt();
        Page page = new Page();
        page.pageSize = pageSize;
        page.pageIndex = pageIndex;
        Result result = new Result();

        int compid = jsonObject.get("compid").getAsInt();
        long gcode = jsonObject.get("gcode").getAsLong();

        int curpageCount = 0;
        StringBuilder sbSql = new StringBuilder(QUERY_COUP_PUB);
        if(page.pageIndex > 0){
            curpageCount = (page.pageIndex - 1) * page.pageSize;
        }
        sbSql.append("LIMIT ").append(curpageCount).append(",").append(page.pageSize);
        int count = 0;
        List<Object[]> listCount = baseDao.queryNative(QUERY_COUP_CNT_PUB,new Object[]{gcode,compid});
        if(!listCount.isEmpty()){
            count = Integer.parseInt(listCount.get(0)[0].toString());
        }
        page.totalItems = count;
        List<Object[]> queryResult = baseDao.queryNative(sbSql.toString(),gcode,compid);
        CouponPubVO[] couponPubVOS = new CouponPubVO[queryResult.size()];
        PageHolder pageHolder = new PageHolder(page);
        if(queryResult == null || queryResult.isEmpty()){
            return result.setQuery(couponPubVOS, pageHolder);
        }

        baseDao.convToEntity(queryResult, couponPubVOS, CouponPubVO.class,
                new String[]{"coupno","brulecode","rulename",
                        "validday","validflag","glbno","goods"});

        for (CouponPubVO couponPubVO:couponPubVOS){
            StringBuilder sb = new StringBuilder(QUERY_PROM_LAD_SQL);
            sb.append(" and offercode like '").append(couponPubVO.getBrulecode()).append("%'");
            List<Object[]> ret = baseDao.queryNative(sb.toString());

            CouponPubLadderVO[] ladderVOS = new CouponPubLadderVO[ret.size()];
            baseDao.convToEntity(ret, ladderVOS, CouponPubLadderVO.class,
                    new String[]{"unqid","ladamt","ladnum","offer","offercode"});

            couponPubVO.setLadderVOS(Arrays.asList(ladderVOS));
            couponPubVO.setCompid(compid);
        }

        return result.setQuery(couponPubVOS, pageHolder);
    }


    public Result revCoupon(AppContext appContext){
        Result result = new Result();
        String json = appContext.param.json;
        CouponPubVO couponVO = GsonUtils.jsonToJavaBean(json, CouponPubVO.class);
        long rcdid = GenIdUtil.getUnqId();
        int ret = baseDao.updateNative(INSERT_COURCD,
                new Object[]{rcdid,couponVO.getCoupno(),
                couponVO.getCompid(),0});
        if(ret > 0){
          //  int oret = 0;
            int oret = IceRemoteUtil.collectCoupons(couponVO.getCompid(),json);
            if(oret > 0){
                baseDao.updateNative(UPDATE_COUPON_STOCK,
                        new Object[]{couponVO.getCoupno()});
                return result.success("领取成功");
            }else{
                //删除优惠记录
                baseDao.updateNative(DEL_COURCD,rcdid);
            }
        }
        return result.success("领取失败");
    }






//    private void insertAssGift(List<AssGiftVO> assGiftVOS, long actCode) {
//        List<Object[]> assGiftParams = new ArrayList<>();
//        for (AssGiftVO assGiftVO : assGiftVOS) {
//            assGiftParams.add(new Object[]{GenIdUtil.getUnqId(), actCode, assGiftVO.getAssgiftno()});
//        }
//        int[] result = baseDao.updateBatchNative(INSERT_ASS_GIFT_SQL, assGiftParams, assGiftVOS.size());
//        boolean b = !ModelUtil.updateTransEmpty(result);
//    }

//    private List<AssGiftVO> getAssGift(long actCode) {
//        String sql = "select unqid,offerno,assgiftno,cstatus,giftname from {{?" + DSMConst.TD_PROM_ASSGIFT
//                + "}} a left join {{?" + DSMConst.TD_PROM_GIFT + "}} g on a.assgiftno=g.unqid "
//                + " where cstatus&1=0 and actcode=" + actCode;
//        List<Object[]> queryResult = baseDao.queryNative(sql);
//        AssGiftVO[] assGiftVOS = new AssGiftVO[queryResult.size()];
//        baseDao.convToEntity(queryResult, assGiftVOS, AssGiftVO.class);
//        return Arrays.asList(assGiftVOS);
//    }

}
