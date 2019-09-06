package com.onek.op;


import com.onek.context.AppContext;
import com.onek.entity.BDOrderAchieveemntVO;
import com.onek.util.GLOBALConst;
import constant.DSMConst;
import dao.BaseDAO;
import util.ArrayUtil;
import util.StringUtils;
import util.TimeUtils;

import java.util.*;

public class BDOrderAchieveementOP {

    public static String _QUERY_ORDER = " SELECT IFNULL(re.inviter,0) inviter, sum( re.canclord ) canclord, sum( re.completeord ) completeord, sum( re.returnord ) returnord, sum( re.afsaleord ) afsaleord, "+
            " IFNULL(sum( re.returnordamt ),0) canclordamt, IFNULL(sum( re.originalprice ) ,0)originalprice, IFNULL(sum( re.payamt ),0) payamt, IFNULL(sum( re.distamt ),0) distamt, IFNULL(sum( re.balamt ),0) balamt, "+
            " IFNULL(max( re.payamt ),0) maxpayamt, IFNULL(min( re.payamt ),0) minpayamt, IFNULL(avg( re.payamt ),0) avgpayamt, IFNULL(sum( re.realrefamt ),0) realrefamt, re.odate "+
            "FROM ( " +
            " SELECT o.inviter inviter, "+
            " ( SELECT count( * ) FROM {{?"+ DSMConst.TD_BK_TRAN_ORDER +"}} ord WHERE ord.ostatus =- 4 AND ord.orderno = o.orderno ) canclord, "+
            " ( SELECT count( * ) FROM {{?"+ DSMConst.TD_BK_TRAN_ORDER +"}} ord WHERE ord.ostatus !=-4 and ord.ostatus!=0 AND ord.orderno = o.orderno ) completeord, "+
            " ( SELECT count( DISTINCT asord.orderno ) FROM {{?"+DSMConst.TD_TRAN_ASAPP+"}} asord WHERE asord.orderno = o.orderno and astype in(0,1,2) and ( asord.ckstatus = 1 or asord.ckstatus = 200 ) ) returnord, "+
            " ( SELECT count( DISTINCT asord.orderno ) FROM {{?"+DSMConst.TD_TRAN_ASAPP+"}} asord WHERE asord.orderno = o.orderno AND ( asord.ckstatus = 1 or asord.ckstatus = 200 ) ) afsaleord, "+
            " ( SELECT o.payamt FROM {{?"+ DSMConst.TD_BK_TRAN_ORDER +"}} ord WHERE ord.ostatus =- 4 AND ord.orderno = o.orderno ) returnordamt, "+
            " ( SELECT o.pdamt FROM {{?"+ DSMConst.TD_BK_TRAN_ORDER +"}} ord WHERE ord.ostatus !=-4 and ord.ostatus!=0 AND ord.orderno = o.orderno ) originalprice, "+
            " ( SELECT o.payamt FROM {{?"+ DSMConst.TD_BK_TRAN_ORDER +"}} ord WHERE (ord.ostatus !=-4 and ord.ostatus!=0 ) AND ord.orderno = o.orderno ) payamt, "+
            " ( SELECT o.distamt FROM {{?"+ DSMConst.TD_BK_TRAN_ORDER +"}} ord WHERE (ord.ostatus !=-4 and ord.ostatus!=0 ) AND ord.orderno = o.orderno ) distamt, "+
            " ( SELECT o.balamt FROM {{?"+ DSMConst.TD_BK_TRAN_ORDER +"}} ord WHERE (ord.ostatus !=-4 and ord.ostatus!=0 ) AND ord.orderno = o.orderno ) balamt, "+
            " o.realrefamt realrefamt, o.odate "+
            " FROM ( "+
            " SELECT comp.cid cid, comp.inviter inviter, ord.asstatus asstatus, ord.orderno orderno, ord.cusno cusno, ord.ostatus ostatus, "+
            " ord.pdamt pdamt, ord.payamt payamt, ord.distamt distamt, ord.balamt balamt, sum(IFNULL( asapp.realrefamt, 0 )) realrefamt, ord.odate "+
            " FROM {{?"+ DSMConst.TD_BK_TRAN_ORDER +"}} ord LEFT JOIN {{?"+DSMConst.TD_TRAN_ASAPP+"}} asapp ON ord.orderno = asapp.orderno LEFT JOIN tb_bk_comp comp ON ord.cusno = comp.cid GROUP BY ord.orderno ${var}"+
            " ) o  ) re ";

    private static final String _SELECT_GROUP = "  GROUP BY re.inviter  ";

    /**
     * 获取所有订单详情
     * @return
     */
    public static List<BDOrderAchieveemntVO> executeOrderInfos(AppContext appContext){
        StringBuilder builder = new StringBuilder();
        builder.append(_QUERY_ORDER);
        builder.append(_SELECT_GROUP);

        String[] strParam = appContext.param.arrays;
        String sql = builder.toString();
        appContext.logger.print("==========================时间维度："+strParam[0]);
        if(strParam.length>0){
            sql = sql.replace("${var}",strParam[0]);
        }

        List<Object[]> queryResult = BaseDAO.getBaseDAO()
                .queryNativeSharding(GLOBALConst.COMP_INIT_VAR, TimeUtils.getCurrentYear(), sql);
        BDOrderAchieveemntVO[] bdOrderAchieveemntVOS = new BDOrderAchieveemntVO[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult,bdOrderAchieveemntVOS,BDOrderAchieveemntVO.class);

        //System.out.println(boList.size());
        return Arrays.asList(bdOrderAchieveemntVOS);
    }




    private static String _QUERY_CUMULATIVE_SG = "SELECT re.inviter inviter, count( DISTINCT cid ) countnum  FROM" +
            "( SELECT DISTINCT ( co.cid ) cid, co.inviter inviter," +
            " co.cname cname, co.odate odate  FROM " +
            " ( SELECT comp.cid cid, comp.inviter inviter, comp.cname cname, ord.orderno orderno," +
            " ord.ostatus ostatus, ord.odate odate  FROM tb_bk_comp comp, {{?"+DSMConst.TD_BK_TRAN_ORDER+"}} ord " +
            " WHERE comp.cid = ord.cusno  AND ord.ostatus >= 1  ) co " +
            "WHERE co.odate <= ?  GROUP BY co.orderno  ) re  GROUP BY re.inviter";

    /**
     * 查询当前时间门店累计首购
     */
    public static Map getCumulative(String param){
        StringBuilder sb = new StringBuilder(_QUERY_CUMULATIVE_SG);
        List<Object[]> list = BaseDAO.getBaseDAO().queryNativeSharding(GLOBALConst.COMP_INIT_VAR,TimeUtils.getCurrentYear(),sb.toString(),param);
        Map map = new HashMap();
        for (int i = 0;i<list.size();i++){
            map.put(list.get(i)[0].toString(),list.get(i)[1].toString());
        }
        return map;
    }


}
