package com.onek.discount;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.*;
import com.onek.annotation.UserPermission;
import com.onek.consts.CSTATUS;
import com.onek.consts.IntegralConstant;
import com.onek.context.AppContext;
import com.onek.context.StoreBasicInfo;
import com.onek.context.UserSession;
import com.onek.discount.entity.*;
import com.onek.entitys.Result;
import com.onek.util.*;
import com.onek.util.area.AreaFeeUtil;
import com.onek.util.area.AreaUtil;
import com.onek.util.member.MemberStore;
import constant.DSMConst;
import dao.BaseDAO;
import org.hyrdpf.util.LogUtil;
import redis.provide.RedisStringProvide;
import redis.util.RedisUtil;
import util.GsonUtils;
import util.MathUtil;
import util.ModelUtil;
import util.StringUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.onek.discount.CommonModule.getLaderNo;

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
            + " where cstatus&1=0 and actcode=?";

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

    private static final String QUERY_COUP_PUB = "select coupno,brulecode,rulename,validday,validflag,glbno,goods,qlfno,qlfval from ("+
            "select coupno,brulecode,rulename,validday,validflag,periodtype,periodday,actstock,glbno,goods,qlfno,qlfval from ("+
            "select tpcp.unqid coupno,tpcp.brulecode,rulename,validday,validflag,periodtype,periodday,tpcp.actstock,glbno,0 goods,qlfno,qlfval "+
            "from {{?" + DSMConst.TD_PROM_COUPON + "}} tpcp inner join {{?" + DSMConst.TD_PROM_RULE + "}}" +
            " tpcr on tpcp.brulecode = tpcr.brulecode  "+
            " inner join {{?" + DSMConst.TD_PROM_ASSDRUG + "}} assd on assd.actcode = tpcp.unqid "+
            " where assd.gcode = 0  and tpcp.cstatus & 64 > 0 and tpcp.cstatus & 33 = 0 and tpcr.cstatus & 33 = 0 and assd.cstatus & 33 = 0 "+
            " union "+
            "select distinct tpcp.unqid coupno,tpcp.brulecode,rulename,validday,validflag,periodtype,periodday,tpcp.actstock,glbno,1 goods,qlfno,qlfval  from {{?"+
            DSMConst.TD_PROM_COUPON +"}} tpcp inner join {{?" + DSMConst.TD_PROM_RULE + "}} tpcr on tpcp.brulecode = tpcr.brulecode " +
            " inner join {{?" +DSMConst.TD_PROM_ASSDRUG+"}} assd on assd.actcode = tpcp.unqid "+
            " where assd.gcode in (?,?) "+
            " and tpcp.cstatus & 64 > 0 and tpcp.cstatus & 33 = 0 and tpcr.cstatus & 33 = 0 and assd.cstatus & 33 = 0 ) a "+
            " where a.actstock > 0 and 1 = fun_prom_cycle(coupno,periodtype,periodday,DATE_FORMAT(NOW(),'%m%d'),0) and a.glbno = 0 "+
            " and not exists (select 1 from td_prom_courcd where coupno = a.coupno and compid = ? and cstatus & 1 = 0)) a ";


    private static final String QUERY_COUP_ALL_PUB = "select coupno,brulecode,rulename,validday,validflag,glbno,goods,qlfno,qlfval from ("+
            "select coupno,brulecode,rulename,validday,validflag,periodtype,periodday,actstock,glbno,goods,qlfno,qlfval from ("+
            "select tpcp.unqid coupno,tpcp.brulecode,rulename,validday,validflag,periodtype,periodday,tpcp.actstock,glbno,0 goods,qlfno,qlfval "+
            "from {{?" + DSMConst.TD_PROM_COUPON + "}} tpcp inner join {{?" + DSMConst.TD_PROM_RULE + "}}" +
            " tpcr on tpcp.brulecode = tpcr.brulecode  "+
            " inner join {{?" + DSMConst.TD_PROM_ASSDRUG + "}} assd on assd.actcode = tpcp.unqid "+
            " where assd.gcode = 0  and tpcp.cstatus & 64 > 0 and tpcp.cstatus & 33 = 0 and tpcr.cstatus & 33 = 0 and assd.cstatus & 33 = 0 "+
            " union "+
            "select distinct tpcp.unqid coupno,tpcp.brulecode,rulename,validday,validflag,periodtype,periodday,tpcp.actstock,glbno,1 goods,qlfno,qlfval  from {{?"+
            DSMConst.TD_PROM_COUPON +"}} tpcp inner join {{?" + DSMConst.TD_PROM_RULE + "}} tpcr on tpcp.brulecode = tpcr.brulecode " +
            " where exists (select 1 from {{?" +DSMConst.TD_PROM_ASSDRUG +"}}  a where tpcp.unqid = actcode and gcode != 0  and cstatus & 1 = 0) "+
            " and tpcp.cstatus & 64 > 0 and tpcp.cstatus & 33 = 0 and tpcr.cstatus & 33 = 0 and assd.cstatus & 33 = 0 ) a "+
            " where a.actstock > 0 and 1 = fun_prom_cycle(coupno,periodtype,periodday,DATE_FORMAT(NOW(),'%m%d'),0) and a.glbno = 0 "+
            " and not exists (select 1 from td_prom_courcd where coupno = a.coupno and compid = ? and cstatus & 1 = 0)) a ";



//    select distinct tpcp.unqid coupno,tpcp.brulecode,rulename,validday,validflag,periodtype,periodday,tpcp.actstock,glbno,1 goods,qlfno,qlfval td_prom_cou from
//    td_prom_coupon tpcp inner join td_prom_rule tpcr on tpcp.brulecode = tpcr.brulecode
//
//
//    where exists (select 1 from td_prom_assdrug a where tpcp.unqid = actcode and gcode != 0
//            and cstatus & 1 = 0)



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
            " where assd.gcode in (?,?) "+
            " and tpcp.cstatus & 64 > 0 and tpcp.cstatus & 33 = 0 and tpcr.cstatus & 33 = 0 and assd.cstatus & 33 = 0 ) a "+
            " where a.actstock > 0 and 1 = fun_prom_cycle(coupno,periodtype,periodday,DATE_FORMAT(NOW(),'%m%d'),0) "+
            " and not exists (select 1 from td_prom_courcd where coupno = a.coupno and compid = ? and cstatus & 1 = 0)) a ";




    private static final String INSERT_COURCD =  "insert into {{?" + DSMConst.TD_PROM_COURCD + "}}" +
            " (unqid,coupno,compid,offercode,gettime) values (?,?,?,?,now())";

    private static final String DEL_COURCD =  "update {{?" + DSMConst.TD_PROM_COURCD + "}}" +
            " SET cstatus = cstatus | " + CSTATUS.DELETE +" WHERE unqid = ? ";


    //扣减优惠券库存
    private static final String UPDATE_COUPON_STOCK = " update {{?" + DSMConst.TD_PROM_COUPON + "}}"
                    + " set actstock = actstock - 1 " +
            "where unqid = ? and actstock > 0 and cstatus & 1 = 0";

    private static final String INSERT_RELA_SQL = "insert into {{?" + DSMConst.TD_PROM_RELA + "}} "
            + "(unqid,actcode,ladid) values(?,?,?)";


    private static final String UPDATE_COMP_BAL = "update {{?" + DSMConst.D_COMP + "}} "
            + "set balance = balance + ? where cid = ? ";


    private static final String QUERY_COMP_BAL = " select balance from {{?" + DSMConst.D_COMP + "}} "
            + " where cid = ? ";


    private static final String QUERY_COUP_EXCG = " select tpcp.unqid coupno,tpcp.brulecode,rulename,validday," +
            "validflag,periodtype,periodday,tpcp.actstock,0 glbno," +
            "0 goods,0 qlfno,0 qlfval " +
            " from {{?" + DSMConst.TD_PROM_COUPON + "}} tpcp " +
            " inner join {{?" + DSMConst.TD_PROM_RULE + "}} tpcr "
            + " on tpcp.brulecode = tpcr.brulecode  where tpcp.brulecode in (2110,2120)" +
            " and tpcp.cstatus & 256 > 0 and tpcp.cstatus & 1 = 0 " +
            " and  1 = fun_prom_cycle(tpcp.unqid,periodtype,periodday,DATE_FORMAT(NOW(),'%m%d'),0) ";

    private static final String QUERY_COUP_NEWPERSON = "select unqid,coupname,offer from " +
            " {{?" + DSMConst.TD_PROM_COUPON + "}} cp  inner join (select min(offer) offer,actcode from " +
            " {{?" + DSMConst.TD_PROM_RELA + "}} rela join {{?" + DSMConst.TD_PROM_LADOFF + "}} lad on rela.ladid = lad.unqid and offer > 0 " +
            "  and rela.cstatus & 1 = 0 and lad.cstatus & 1 = 0 group by actcode) a on cp.unqid = a.actcode " +
            " where cp.qlfno = 1 and cp.glbno = 1 and cp.cstatus & 1 = 0 " +
            " and cp.cstatus & 64 > 0 and 1 = fun_prom_cycle(unqid,periodtype,periodday,DATE_FORMAT(NOW(),'%m%d'),0) ";

    private static final  String QUERY_COURCD_EXT = "select 1 from {{?"+DSMConst.TD_PROM_COURCD+"}} "+
            " where compid = ? and cstatus & 128 > 0 ";

    //查询库存版本号
//    private static final String SELECT_COUPON_VER_ = " select ver from {{?" + DSMConst.TD_PROM_COUPON + "}}"
//            + "  where unqid = ? cstatus & 33 = 0";

    private final String QUERY_COUPON_OFFLINE = "select unqid,validday,expwd,amt,cstatus from " +
            "{{?"+DSMConst.TD_PROM_OFFLCOUP+"}}  where unqid = ? and cstatus & 1 = 0 ";

    private final String UPDATE_COUPON_OFFLINE = "update " +
            "{{?"+DSMConst.TD_PROM_OFFLCOUP+"}}  set cstatus = ? where unqid = ? ";

    private final String INSERT_COUPON_OFFLINE = " insert into  " +
            "{{?"+DSMConst.TD_PROM_OFFLCOUP+"}} (unqid,validday,expwd,amt,cstatus) values (?,?,?,?,?) ";

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
        parmList.add(new Object[]{GenIdUtil.getUnqId(),unqid,0,0,couponVO.getActstock(),0,0});

        int[] coupRet = baseDao.updateTransNative(new String[]{INSERT_COUPON_SQL, INSERT_ASS_DRUG_SQL},
                parmList);
        if (!ModelUtil.updateTransEmpty(coupRet)) {
            //新增活动场次
            if (couponVO.getTimeVOS() != null && !couponVO.getTimeVOS().isEmpty()) {
                insertTimes(couponVO.getTimeVOS(), unqid);
            }
            //新增阶梯
            if (couponVO.getLadderVOS() != null && !couponVO.getLadderVOS().isEmpty()) {
                insertLadOff(couponVO.getLadderVOS(),couponVO.getRuleno()+""+couponVO.getRulecomp(),unqid);
            }

        } else {
            return result.fail("新增失败");
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            long startTime = dateFormat.parse(couponVO.getStartdate() +
                    " " + couponVO.getTimeVOS().get(0).getSdate()).getTime();
            if (startTime > new Date().getTime()) {
                ExecutorService executors = Executors.newSingleThreadExecutor();
                executors.execute(() -> {
                        IceRemoteUtil.sendMessageToAllClient(SmsTempNo.NEW_COUPONS,
                            "", "【" +couponVO.getCoupname() + "】将于" + couponVO.getStartdate() +
                                    " " + couponVO.getTimeVOS().get(0).getSdate() + "开始进行");
                        SmsUtil.sendMsgToAllBySystemTemp(SmsTempNo.NEW_COUPONS,"", "【" +couponVO.getCoupname() + "】将于" + couponVO.getStartdate() +
                                " " + couponVO.getTimeVOS().get(0).getSdate() + "开始进行");
                    }
                );
            }
        } catch (ParseException e) {
            e.printStackTrace();
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
        couponVOS[0].setLadderVOS(getCoupLadder(couponVOS[0],couponVOS[0].getCoupno()));
        couponVOS[0].setActiveRule(getRules());

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



    private List<LadderVO> getCoupLadder(CouponVO couponVO, long actCode) {
        String selectSQL = "select a.unqid,ladamt,ladnum,offercode,offer,a.cstatus from {{?" + DSMConst.TD_PROM_RELA
                + "}} a left join {{?" + DSMConst.TD_PROM_LADOFF + "}} b on a.ladid=b.unqid where a.cstatus&1=0 "
                + " and a.actcode=" + actCode;
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        LadderVO[] ladderVOS = new LadderVO[queryResult.size()];
        baseDao.convToEntity(queryResult, ladderVOS, LadderVO.class);
        for (LadderVO ladderVO:ladderVOS) {
            ladderVO.setLadamt(ladderVO.getLadamt()/100);
            ladderVO.setOffer(ladderVO.getOffer()/100);
        }
        String offerCode = ladderVOS[0].getOffercode() + "";
        couponVO.setRulecomp(Integer.parseInt(offerCode.substring(4,5)));
        return Arrays.asList(ladderVOS);
    }

//    /**
//     * 查询阶梯
//     * @param rulecode
//     * @return
//     */
//    private List<LadderVO> getCoupLadder(CouponVO couponVO,int rulecode){
//        StringBuilder sb = new StringBuilder(QUERY_PROM_LAD_SQL);
//        sb.append(" and offercode like '").append(rulecode).append("%'");
//        List<Object[]> result = baseDao.queryNative(sb.toString());
//
//        if(result == null || result.isEmpty()){
//            return null;
//        }
//
//        LadderVO[] ladderVOS = new LadderVO[result.size()];
//        baseDao.convToEntity(result, ladderVOS, LadderVO.class,
//                new String[]{"unqid","ladamt","ladnum","offer","offercode"});
//
//        String offerCode = ladderVOS[0].getOffercode() + "";
//        couponVO.setRulecomp(Integer.parseInt(offerCode.substring(4,5)));
//        return Arrays.asList(ladderVOS);
//    }




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
     * @description 关联商品
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/6 10:09
     * @version 1.1.1
     **/
    @UserPermission(ignore = true)
    public Result relationGoods(AppContext appContext) {
        boolean re = false;
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int type = jsonObject.get("type").getAsInt();//1:全部商品  3部分商品  2 部分类别
        long actCode = jsonObject.get("actCode").getAsLong();
        switch (type) {
            case 1://全部商品
                re = relationAllGoods(jsonObject, actCode);
                break;
            case 2://类别关联
            default://商品关联
                relationGoods(jsonObject, actCode);
                return result.success("关联商品成功");
        }
        return re ? result.success("关联商品成功") : result.fail("操作失败");
    }

    private List<Long> getAllGoods(String skuStr, long actcode) {
        List<Long> gcList = new ArrayList<>();
        String selectSQL = "select gcode from {{?" + DSMConst.TD_PROM_ASSDRUG + "}} where cstatus&1=0 "
                + " and gcode in(" + skuStr + ") and actcode=?";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL, actcode);
        if (queryResult == null || queryResult.isEmpty()) return gcList;
        queryResult.forEach(obj -> {
            gcList.add((long)obj[0]);
        });
        return gcList;
    }



    private void relationGoods(JsonObject jsonObject, long actCode) {
        //关联活动商品
        JsonArray goodsArr = jsonObject.get("goodsArr").getAsJsonArray();
        List<GoodsVO> goodsVOS = new ArrayList<>();
        List<GoodsVO> insertGoodsVOS = new ArrayList<>();
        List<GoodsVO> updateGoodsVOS = new ArrayList<>();
        StringBuilder skuBuilder = new StringBuilder();
        if (goodsArr != null && !goodsArr.toString().isEmpty()) {
            for (int i = 0; i < goodsArr.size(); i++) {
                GoodsVO goodsVO = GsonUtils.jsonToJavaBean(goodsArr.get(i).toString(), GoodsVO.class);
                if (goodsVO != null) {
                    skuBuilder.append(goodsVO.getGcode()).append(",");
                }
                goodsVOS.add(goodsVO);
            }

            String skuStr = skuBuilder.toString().substring(0, skuBuilder.toString().length() - 1);
            List<Long> gcList = getAllGoods(skuStr, actCode);
            for (int i = 0; i < goodsVOS.size(); i++) {
                for(int j = 0; j < gcList.size(); j++){
                    if(goodsVOS.get(i).getGcode() == gcList.get(j)){
                        updateGoodsVOS.add(goodsVOS.get(i));
                    }else{
                        insertGoodsVOS.add(goodsVOS.get(i));
                    }
                }

                if(gcList.isEmpty()){
                    insertGoodsVOS.add(goodsVOS.get(i));
                }
            }
            relationAssDrug(insertGoodsVOS, updateGoodsVOS,actCode);
        }
    }

    private boolean relationAllGoods(JsonObject jsonObject,long actCode) {


        String limitnum = jsonObject.get("limitnum").getAsString();
        String actstock = jsonObject.get("actstock").getAsString();
        int limit = 0;
        int stock = 0;
        if(!StringUtils.isEmpty(limitnum)){
            limit = Integer.parseInt(limitnum);
        }

        if(!StringUtils.isEmpty(actstock)){
            stock = Integer.parseInt(actstock);
        }

        double price = jsonObject.get("price").getAsDouble() * 100;
        String delSql = "update {{?" + DSMConst.TD_PROM_ASSDRUG + "}} set cstatus=cstatus|1 "
                + " where cstatus&1=0 and actcode=?";
        baseDao.updateNative(delSql,actCode);
        int result = baseDao.updateNative(INSERT_ASS_DRUG_SQL,GenIdUtil.getUnqId(),actCode, 0, 0,
                stock,limit,price);
        return result > 0;

    }

    /* *
     * @description 关联活动商品
     * @params [goodsVOS]
     * @return void
     * @exception
     * @author 11842
     * @time  2019/4/2 16:23
     * @version 1.1.1
     **/
    private void relationAssDrug(List<GoodsVO> insertDrugVOS, List<GoodsVO> updDrugVOS, long actCode) {
        List<Object[]> insertDrugParams = new ArrayList<>();
        List<Object[]> updateDrugParams = new ArrayList<>();
        String updateSql = "update {{?" + DSMConst.TD_PROM_ASSDRUG + "}} set actstock=?,limitnum=?, vcode=?, price=? "
                + " where cstatus&1=0 and gcode=? and vcode=? and actcode=?";

        String delSql = "update {{?" + DSMConst.TD_PROM_ASSDRUG + "}} set cstatus=cstatus|1 "
                + " where cstatus&1=0 and gcode= 0 and actcode=?";

        for (GoodsVO insGoodsVO : insertDrugVOS) {
            insertDrugParams.add(new Object[]{GenIdUtil.getUnqId(), actCode, insGoodsVO.getGcode(),
                    insGoodsVO.getMenucode(),insGoodsVO.getActstock(),insGoodsVO.getLimitnum(),
                    insGoodsVO.getPrice()*100});
        }
        for (GoodsVO updateGoodsVO : updDrugVOS) {
            updateDrugParams.add(new Object[]{updateGoodsVO.getActstock(),updateGoodsVO.getLimitnum(),
                    updateGoodsVO.getVcode()+1, updateGoodsVO.getPrice()*100,updateGoodsVO.getGcode(),
                    updateGoodsVO.getVcode(),actCode});
        }
        baseDao.updateNative(delSql,actCode);
        baseDao.updateBatchNative(INSERT_ASS_DRUG_SQL, insertDrugParams, insertDrugVOS.size());
        baseDao.updateBatchNative(updateSql, updateDrugParams, updDrugVOS.size());
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


//    /**
//     * 新增阶梯
//     * @param ladderVOS
//     * @param laddrno
//     */
//    private void insertLadOff(List<LadderVO> ladderVOS,String laddrno) {
//
//        List<Object[]> ladOffParams = new ArrayList<>();
//        int [] ladernos = CommonModule.getLaderNo(laddrno, ladderVOS.size());
//
//        for (int i = 0; i < ladderVOS.size(); i++) {
//            ladOffParams.add(new Object[]{GenIdUtil.getUnqId(),
//                    ladderVOS.get(i).getLadamt()*100,ladderVOS.get(i).getLadnum(),ladderVOS.get(i).getOffer()*100,ladernos[i]});
//        }
//        int[] result = baseDao.updateBatchNative(INSERT_LAD_OFF_SQL, ladOffParams, ladderVOS.size());
//        boolean b = !ModelUtil.updateTransEmpty(result);
//    }


    /**
     * @description 新增优惠券阶梯
     * @params []
     * @return int
     **/
    private void insertLadOff(List<LadderVO> ladderVOS, String laddrno, long actCode) {
        List<Object[]> ladOffParams = new ArrayList<>();
        List<Object[]> relaParams = new ArrayList<>();
        int offerCode[] = getLaderNo(laddrno, ladderVOS.size());
        System.out.println("===============00活动码"+actCode);

        for (int i = 0; i < ladderVOS.size(); i++) {
            if (offerCode != null) {
                long ladderId = GenIdUtil.getUnqId();
                double offer = ladderVOS.get(i).getOffer()*100;
                if(laddrno.indexOf("213") == 0){
                    offer = ladderVOS.get(i).getOffer()*1000;
                }
                ladOffParams.add(new Object[]{ladderId, ladderVOS.get(i).getLadamt()*100,
                        ladderVOS.get(i).getLadnum(),offer,offerCode[i]});

                System.out.println("===============11活动码"+actCode);
                relaParams.add(new Object[]{GenIdUtil.getUnqId(),actCode,ladderId});
            }
        }
        int[] result = baseDao.updateBatchNative(INSERT_LAD_OFF_SQL, ladOffParams, ladderVOS.size());
        baseDao.updateBatchNative(INSERT_RELA_SQL, relaParams, ladderVOS.size());
        boolean b = !ModelUtil.updateTransEmpty(result);
//        return b ? 1 : 0;
    }


    private boolean delRelaAndLadder(long actCode) {
        List<Object[]> params = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder offerBuilder = new StringBuilder();
        String selectSQL = "select ladid,offercode from {{?" + DSMConst.TD_PROM_RELA + "}} a left join {{?"
                + DSMConst.TD_PROM_LADOFF +"}} b on a.ladid=b.unqid  where a.cstatus&1=0" +
                " and actcode=" + actCode;
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        if (queryResult == null || queryResult.isEmpty()) return false;
        for (Object[] aQueryResult : queryResult) {
            stringBuilder.append((long) aQueryResult[0]).append(",");
            offerBuilder.append((int) aQueryResult[1]).append(",");
        }
        String ladIdStr = stringBuilder.toString().substring(0, stringBuilder.toString().length() - 1);
        String offerStr = offerBuilder.toString().substring(0, offerBuilder.toString().length() - 1);
        String sqlOne = DEL_LAD_OFF_SQL + " and unqid in(" + ladIdStr + ")";
        params.add(new Object[]{});
        String sqlTwo = "update {{?" + DSMConst.TD_PROM_RELA + "}} set cstatus=cstatus|1 where cstatus&1=0 "
                + " and actcode=" + actCode;
        params.add(new Object[]{});
        return !ModelUtil.updateTransEmpty(baseDao.updateTransNative(new String[]{sqlOne,sqlTwo}, params));
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
            sqlBuilder.append(" and cop.brulecode = ");
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
            sqlBuilder.append(" and cop.brulecode = ");
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
                if (delRelaAndLadder(actCode)) {
                    insertLadOff(couponVO.getLadderVOS(),couponVO.getRuleno()+""+couponVO.getRulecomp(),actCode);
                }
            }
        } else {
            return result.fail("修改失败");
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            long startTime = dateFormat.parse(couponVO.getStartdate() +
                    " " + couponVO.getTimeVOS().get(0).getSdate()).getTime();
            if (startTime > new Date().getTime()) {
                ExecutorService executors = Executors.newSingleThreadExecutor();
                executors.execute(() -> {
                        IceRemoteUtil.sendMessageToAllClient(SmsTempNo.NEW_COUPONS,
                            "", "【" +couponVO.getCoupname() + "】将于" + couponVO.getStartdate() +
                                    " " + couponVO.getTimeVOS().get(0).getSdate() + "开始进行");

                        SmsUtil.sendMsgToAllBySystemTemp(SmsTempNo.NEW_COUPONS,
                                "", "【" +couponVO.getCoupname() + "】将于" + couponVO.getStartdate() +
                                        " " + couponVO.getTimeVOS().get(0).getSdate() + "开始进行");
                    }
                );
            }
        } catch (ParseException e) {
            e.printStackTrace();
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
        int cstatus = jsonObject.get("cstatus").getAsInt();
        int ret = 0;
        switch (cstatus){
            case 0:
                ret = baseDao.updateNative(OPEN_COUPON,actcode);
                break;
            case 1:
                ret = baseDao.updateNative(DELETE_COUPON,actcode);
                delRelaAndLadder(actcode);
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


    private List<RulesVO> getRules() {
        String selectSQL = "select brulecode,rulename from {{?" + DSMConst.TD_PROM_RULE + "}} a where cstatus&1=0 "
                + " and brulecode REGEXP '^2' ";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        RulesVO[] rulesVOS = new RulesVO[queryResult.size()];
        baseDao.convToEntity(queryResult, rulesVOS, RulesVO.class, new String[]{"brulecode", "rulename"});
        return Arrays.asList(rulesVOS);
    }




    /**
     * 查询兑换券发布列表
     * @param appContext
     * @return
     */
    @UserPermission(ignore = true)
    public Result queryCouponExcgPub(AppContext appContext){
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();

        if(!jsonObject.has("compid")){
            return  result.success(null);
        }
        int compid = jsonObject.get("compid").getAsInt();

        StringBuilder sbSql = new StringBuilder(QUERY_COUP_EXCG);

        List<Object[]> queryResult = baseDao.queryNative(sbSql.toString());
        CouponPubVO[] couponPubVOS = new CouponPubVO[queryResult.size()];
        if(queryResult == null || queryResult.isEmpty()){
            return result.success(couponPubVOS);
        }

        baseDao.convToEntity(queryResult, couponPubVOS, CouponPubVO.class,
                new String[]{"coupno","brulecode","rulename",
                        "validday","validflag","glbno","goods","qlfno","qlfval"});

        List<CouponPubVO> couponPubVOList = new ArrayList(Arrays.asList(couponPubVOS));
        Iterator<CouponPubVO> couentIterator = couponPubVOList.iterator();
        while(couentIterator.hasNext()){
            CouponPubVO couponPubVO = couentIterator.next();
            double fee = 0;
            if(couponPubVO.getBrulecode() == 2120){
                long area =  getCurrentArea(compid);
                if(area <= 0){
                    couentIterator.remove();
                    continue;
                }
                fee = AreaFeeUtil.getFee(area);
                if(fee <= 0){
                    couentIterator.remove();
                    continue;
                }
            }

            String selectSQL = "select a.unqid,ladamt,ladnum,offercode,offer from {{?" + DSMConst.TD_PROM_RELA
                    + "}} a left join {{?" + DSMConst.TD_PROM_LADOFF + "}} b on a.ladid=b.unqid where a.cstatus&1=0 "
                    + " and a.actcode=" + couponPubVO.getCoupno() + " order by ladamt desc ";
            List<Object[]> queryRet = baseDao.queryNative(selectSQL);
            CouponPubLadderVO[] ladderVOS = new CouponPubLadderVO[queryRet.size()];
            baseDao.convToEntity(queryRet, ladderVOS, CouponPubLadderVO.class,
                    new String[]{"unqid","ladamt","ladnum","offercode","offer"});
            boolean rvflag = false;
            for (CouponPubLadderVO ladderVO:ladderVOS) {
                ladderVO.setLadamt(MathUtil.exactDiv(ladderVO.getLadamt(),100L).
                        setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
                ladderVO.setOffer(MathUtil.exactDiv(ladderVO.getOffer(),100L).
                        setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
                if(couponPubVO.getBrulecode() == 2120){
                    ladderVO.setOffer(fee);
                }else if(ladderVO.getOffer() <=0){
                    rvflag = true;
                    break;
                }
            }
            if(rvflag){
                couentIterator.remove();
                continue;
            }
            couponPubVO.setLadderVOS(Arrays.asList(ladderVOS));
            couponPubVO.setCompid(compid);
        }
        return result.success(couponPubVOList);
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
//        int pageSize = jsonObject.get("pageSize").getAsInt();
//        int pageIndex = jsonObject.get("pageNo").getAsInt();
//        Page page = new Page();
//        page.pageSize = pageSize;
//        page.pageIndex = pageIndex;
        Result result = new Result();


        if(!jsonObject.has("gcode")
                ||!jsonObject.has("compid")){
            return  result.success(null);
        }

        int compid = jsonObject.get("compid").getAsInt();
        long gcode = jsonObject.get("gcode").getAsLong();
       // int curpageCount = 0;
        StringBuilder sbSql = new StringBuilder(QUERY_COUP_PUB);
//        if(page.pageIndex > 0){
//            curpageCount = (page.pageIndex - 1) * page.pageSize;
//        }
//        sbSql.append("LIMIT ").append(curpageCount).append(",").append(page.pageSize);
     //   int count = 0;

        String gtype = "-1";
        if(gcode > 0){
            gtype = String.valueOf(gcode).substring(1, 7);
        }

//        List<Object[]> listCount = baseDao.queryNative(QUERY_COUP_CNT_PUB,new Object[]{gtype,gcode,compid});
//        if(!listCount.isEmpty()){
//            count = Integer.parseInt(listCount.get(0)[0].toString());
//        }
        //page.totalItems = count;
        List<Object[]> queryResult = baseDao.queryNative(sbSql.toString(),gtype,gcode,compid);
        CouponPubVO[] couponPubVOS = new CouponPubVO[queryResult.size()];
        if(queryResult == null || queryResult.isEmpty()){
            return result.success(couponPubVOS);
        }

        baseDao.convToEntity(queryResult, couponPubVOS, CouponPubVO.class,
                new String[]{"coupno","brulecode","rulename",
                        "validday","validflag","glbno","goods","qlfno","qlfval"});

        List<CouponPubVO> couponPubVOList = new ArrayList(Arrays.asList(couponPubVOS));
        filterCoupon(couponPubVOList,compid);
        for (CouponPubVO couponPubVO:couponPubVOList){
            String selectSQL = "select a.unqid,ladamt,ladnum,offercode,offer from {{?" + DSMConst.TD_PROM_RELA
                    + "}} a left join {{?" + DSMConst.TD_PROM_LADOFF + "}} b on a.ladid=b.unqid where a.cstatus&1=0 "
                    + " and a.actcode=" + couponPubVO.getCoupno() + " order by ladamt desc ";
            List<Object[]> queryRet = baseDao.queryNative(selectSQL);
            CouponPubLadderVO[] ladderVOS = new CouponPubLadderVO[queryRet.size()];
            baseDao.convToEntity(queryRet, ladderVOS, CouponPubLadderVO.class,
                    new String[]{"unqid","ladamt","ladnum","offercode","offer"});
            for (CouponPubLadderVO ladderVO:ladderVOS) {
                ladderVO.setLadamt(MathUtil.exactDiv(ladderVO.getLadamt(),100L).
                        setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
                ladderVO.setOffer(MathUtil.exactDiv(ladderVO.getOffer(),100L).
                        setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
            }
            couponPubVO.setLadderVOS(Arrays.asList(ladderVOS));
            couponPubVO.setCompid(compid);
        }

        return result.success(couponPubVOList);
       // return result.setQuery(couponPubVOS, pageHolder);
    }



    /**
     * 查询领券专场发布列表
     * @param appContext
     * @return
     */
    @UserPermission(ignore = true)
    public Result queryCouponAllPub(AppContext appContext){
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        Result result = new Result();
        if(!jsonObject.has("compid")){
            return  result.success(null);
        }
        int compid = jsonObject.get("compid").getAsInt();
        StringBuilder sbSql = new StringBuilder(QUERY_COUP_ALL_PUB);
        List<Object[]> queryResult = baseDao.queryNative(sbSql.toString(),compid);
        CouponPubVO[] couponPubVOS = new CouponPubVO[queryResult.size()];
        if(queryResult == null || queryResult.isEmpty()){
            return result.success(couponPubVOS);
        }
        baseDao.convToEntity(queryResult, couponPubVOS, CouponPubVO.class,
                new String[]{"coupno","brulecode","rulename",
                        "validday","validflag","glbno","goods","qlfno","qlfval"});
        List<CouponPubVO> couponPubVOList = new ArrayList(Arrays.asList(couponPubVOS));
        filterCoupon(couponPubVOList,compid);
        for (CouponPubVO couponPubVO:couponPubVOList){
            String selectSQL = "select a.unqid,ladamt,ladnum,offercode,offer from {{?" + DSMConst.TD_PROM_RELA
                    + "}} a left join {{?" + DSMConst.TD_PROM_LADOFF + "}} b on a.ladid=b.unqid where a.cstatus&1=0 "
                    + " and a.actcode=" + couponPubVO.getCoupno() + " order by ladamt desc ";
            List<Object[]> queryRet = baseDao.queryNative(selectSQL);
            CouponPubLadderVO[] ladderVOS = new CouponPubLadderVO[queryRet.size()];
            baseDao.convToEntity(queryRet, ladderVOS, CouponPubLadderVO.class,
                    new String[]{"unqid","ladamt","ladnum","offercode","offer"});
            for (CouponPubLadderVO ladderVO:ladderVOS) {
                ladderVO.setLadamt(MathUtil.exactDiv(ladderVO.getLadamt(),100L).
                        setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
                ladderVO.setOffer(MathUtil.exactDiv(ladderVO.getOffer(),100L).
                        setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
            }
            couponPubVO.setLadderVOS(Arrays.asList(ladderVOS));
            couponPubVO.setCompid(compid);
        }
        return result.success(couponPubVOList);
    }






    public List<CouponPubVO> filterCoupon(List<CouponPubVO> couponPubVOS,int compid){
        Iterator<CouponPubVO> couentIterator = couponPubVOS.iterator();
        while(couentIterator.hasNext()){
            CouponPubVO couponPubVO = couentIterator.next();
            int qlfno = couponPubVO.getQlfno();
            long qlfval = couponPubVO.getQlfval();

            boolean rvflag = false;
            switch (qlfno){
                case 0:
                    break;
                case 1:
                    if(!compIsVerify(compid)
                            || !getOrderCnt(compid)){
                        rvflag = true;
                    }
                    break;
                case 2:
                    int mlevel = MemberStore.
                            getLevelByCompid(compid);
                    if(mlevel < qlfval){
                        rvflag = true;
                    }
                    break;
                case 3:
                    if(qlfval == 0 || (qlfval != getCurrentArea(compid)
                            && !AreaUtil.isChildren(qlfval, getCurrentArea(compid)))){
                        rvflag = true;
                    }
                    break;
                default:
                    break;
            }
            if(rvflag){
                couentIterator.remove();
            }

        }
        return couponPubVOS;
    }

    private long getCurrentArea(int compid) {
        String compStr = RedisUtil.getStringProvide()
                .get(String.valueOf(compid));
        if(!StringUtils.isEmpty(compStr)){
            JSONObject compJson = JSON.parseObject(compStr);
            System.out.println("企业信息缓存："+compJson);
            return compJson.getLongValue("addressCode");
        }
        return 0;
    }


    private boolean compIsVerify(int compid) {
        String compStr = RedisUtil.getStringProvide()
                .get(String.valueOf(compid));
        System.out.println("企业信息："+compStr);
        if(!StringUtils.isEmpty()){
            StoreBasicInfo storeBasicInfo
                    = GsonUtils.jsonToJavaBean(compStr, StoreBasicInfo.class);
            if(storeBasicInfo != null
                    && (storeBasicInfo.authenticationStatus & 256 )> 0){
                return true;
            }
        }
        return false;
    }


    private boolean getOrderCnt(int compid) {
        try{
            if(compid > 0){
                return IceRemoteUtil.getOrderCntByCompid(compid) == 0;
            }
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return  false;
    }


    @UserPermission(ignore = true)
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
        int oret = 0;
        try{
            double integralByCompid
                    = MathUtil.exactDiv(MemberStore.
                    getIntegralByCompid(couponVO.getCompid()), 1000).doubleValue();

            LogUtil.getDefaultLogger().debug("获取积分/1000："+integralByCompid);

//            if(couponVO.getBrulecode() == 2120){
//                long area =  getCurrentArea(couponVO.getCompid());
//                if(area <= 0){
//                    return result.fail("积分不够,兑换失败！");
//                }
//                double fee = AreaFeeUtil.getFee(area);
//                if(integralByCompid < fee){
//                    return result.fail("积分不够,兑换失败！");
//                }
//            }
           if(integralByCompid < CouponPubLadderVO.getOffer()){
               return result.fail("积分不够,兑换失败！");
           }

           oret = IceRemoteUtil.collectExcgCoupons(couponVO.getCompid(),json);
        }catch (Exception e){
            e.printStackTrace();
           return result.fail("领取失败");
        }

        if(oret > 0){
            Double offer = CouponPubLadderVO.getOffer();
            int reducePoint = offer.intValue() * 1000;

            try{
                int pret = MemberStore.reducePoint(couponVO.getCompid(), reducePoint);
                if(pret > 0){
                    IceRemoteUtil.addIntegralDetail(couponVO.getCompid(),
                            IntegralConstant.SOURCE_EXCHANGE_COUPON, reducePoint, rcdid);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            return result.success("领取成功");

        }
        return result.success("领取失败");
    }


    @UserPermission(ignore = true)
    public Result revOfflineExcgCoupon(AppContext appContext){
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        if(!jsonObject.has("compid")
                || !jsonObject.has("coupno")){
            return  result.fail("兑换失败");
        }
        int compid = jsonObject.get("compid").getAsInt();
        long coupno = jsonObject.get("coupno").getAsLong();
        String pwd = jsonObject.get("pwd").getAsString();

        int oret = 0;
        if(compid <= 0 || coupno <=0){
            return result.fail("兑换失败");
        }
        List<Object[]> ret = baseDao.queryNative(QUERY_COUPON_OFFLINE, coupno);

        if(ret == null || ret.isEmpty()){
            return result.fail("兑换失败");
        }

        if(!pwd.trim().equals(ret.get(0)[2].toString())){
            return result.fail("兑换失败");
        }

        CouponPubVO couponPubVO = new CouponPubVO();
        couponPubVO.setCompid(compid);
        couponPubVO.setBrulecode(2110);
        couponPubVO.setCoupno(coupno);
        couponPubVO.setRulename("线下兑换券");
        couponPubVO.setValidday(Integer.parseInt(ret.get(0)[1].toString()));
        List<CouponPubLadderVO> ladList = new ArrayList<>();
        CouponPubLadderVO couponPubLadderVO = new CouponPubLadderVO();
        couponPubLadderVO.setUnqid(0);
        couponPubLadderVO.setOffer(MathUtil.
                exactDiv(Double.parseDouble(ret.get(0)[3].toString()),
                100).doubleValue());
        couponPubLadderVO.setLadnum(0);
        couponPubLadderVO.setOffercode(2110201);
        ladList.add(couponPubLadderVO);
        couponPubVO.setLadderVOS(ladList);
        int cstatus = Integer.parseInt(ret.get(0)[4].toString());
        cstatus = cstatus | 1;
        int reflag = baseDao.updateNative(UPDATE_COUPON_OFFLINE, cstatus, coupno);
        if(reflag <= 0){
            return result.fail("兑换失败");
        }
        try{
            oret = IceRemoteUtil.collectOfflineExcgCoupons(compid,
                        GsonUtils.javaBeanToJson(couponPubVO));

        }catch (Exception e){
            cstatus = cstatus & ~1;
            baseDao.updateNative(UPDATE_COUPON_OFFLINE, cstatus, coupno);
            e.printStackTrace();
            return result.fail("兑换失败");
        }

        if(oret <= 0){
            cstatus = cstatus & ~1;
            baseDao.updateNative(UPDATE_COUPON_OFFLINE, cstatus, coupno);
            return result.fail("兑换失败");
        }
        return result.success("兑换成功");
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
            int oret = 0;
            try{
                oret = IceRemoteUtil.collectCoupons(couponVO.getCompid(),json);
            }catch (Exception e){
                baseDao.updateNative(DEL_COURCD,rcdid);
                e.printStackTrace();
            }

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


    @UserPermission(ignore = true)
    public Result revNewComerCoupon(AppContext appContext){
        Result result = new Result();
        int compid = Integer.parseInt(appContext.param.arrays[0]);
        long uph = Long.parseLong(appContext.param.arrays[1]);
        List<Object[]> crdRet = baseDao.queryNative(QUERY_COURCD_EXT, compid);
        if(crdRet != null && !crdRet.isEmpty()){
            return result.fail("已领取过新人专享！");
        }
        List<Object[]> coupRet = baseDao.queryNative(QUERY_COUP_NEWPERSON,new Object[]{});

        if(coupRet == null || coupRet.isEmpty()){
            return result.fail("没有开启新人专享活动！");
        }

        int bal = 0;
        List<Object[]> parm = new ArrayList<>();
        List<Map> parmList = new ArrayList<>();
        String courdSql = "insert into {{?" + DSMConst.TD_PROM_COURCD + "}}" +
                " (unqid,coupno,compid,offercode,gettime,cstatus) values (?,?,?,?,now(),?)";
        for (Object[] objects : coupRet){
            bal = bal + Integer.parseInt(objects[2].toString());
            Map<String,String> map = new HashMap();
            map.put("coupno",objects[0].toString());
            map.put("coupname",objects[1].toString());
            map.put("offer",objects[2].toString());
            map.put("compid",compid+"");
            parmList.add(map);
        }
        long rcdid = GenIdUtil.getUnqId();
        parm.add(new Object[]{bal,compid});

        parm.add(new Object[]{rcdid,coupRet.get(0)[0],
                compid,0,128});
        int[] ret = baseDao.updateTransNative(new String[]{UPDATE_COMP_BAL,courdSql}, parm);

        if(!ModelUtil.updateTransEmpty(ret)){
            try{
                String jsonDto = GsonUtils.javaBeanToJson(parmList);
                LogUtil.getDefaultLogger().debug("jsonDTO:"+jsonDto);
                IceRemoteUtil.insertNewComerBalCoup(compid,jsonDto);
            }catch (Exception e){
                e.printStackTrace();
            }
            //TODO  短信发送 ！bal
          //  SmsUtil.sendSmsBySystemTemp(uph+"",0,new String[]{});
            return result.success("领取成功");
        }
        return result.success("领取失败");
    }

    @UserPermission(ignore = true)
    public int updateCompBal(AppContext appContext){
        int compid = Integer.parseInt(appContext.param.arrays[0]);
        int amt = Integer.parseInt(appContext.param.arrays[1]);
        int ret = baseDao.updateNative(UPDATE_COMP_BAL,
                new Object[]{amt,compid});

        return ret;
    }

    @UserPermission(ignore = true)
    public int queryCompBal(AppContext appContext){
        int compid = Integer.parseInt(appContext.param.arrays[0]);
        List<Object[]> bal = baseDao.queryNative(QUERY_COMP_BAL,
                compid);
        if(bal == null || bal.isEmpty()) return 0;

        return Integer.parseInt(bal.get(0)[0].toString());
    }


    @UserPermission(ignore = true)
    public Result queryCompAllBal(AppContext appContext){
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        Result result = new Result();

        int compid = jsonObject.get("compid").getAsInt();
        Map map = new HashMap<>();
        map.put("balamt",0);
        if(compid <= 0){
            return result.success(map);
        }
        List<Object[]> bal = baseDao.queryNative(QUERY_COMP_BAL,
                compid);
        if(bal == null || bal.isEmpty()) return result.success(map);

        double balamt = MathUtil.exactDiv(Double.parseDouble(bal.get(0)[0].toString()), 100L)
                .setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();

        map.put("balamt",balamt);
        return  result.success(map);
    }

    @UserPermission(ignore = true)
    public Result prdOfflineExcgCoupon(AppContext appContext){
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int num = jsonObject.get("num").getAsInt();
        int validday = jsonObject.get("validday").getAsInt();
        double amt = jsonObject.get("amt").getAsDouble();

        List<Object[]> params = new ArrayList<>();
        for (int i = 0; i < num; i++){
            params.add(new Object[]{GenIdUtil.getUnqId(),
                    validday, RandomUtil.getRandomNumber(6),amt*100,0});
        }
        int[] ret = baseDao.updateBatchNative(UPDATE_COUPON_OFFLINE, params, params.size());
        boolean b = !ModelUtil.updateTransEmpty(ret);

        return b ? result.success("操作成功！"): result.fail("操作失败！");
    }
}
