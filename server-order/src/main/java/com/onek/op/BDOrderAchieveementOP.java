package com.onek.op;


import com.onek.context.AppContext;
import com.onek.entity.BDOrderAchieveemntVO;
import com.onek.util.GLOBALConst;
import constant.DSMConst;
import dao.BaseDAO;
import util.ArrayUtil;
import util.TimeUtils;

import java.util.Arrays;
import java.util.List;

public class BDOrderAchieveementOP {

    public static String _QUERY_ORDER = " SELECT IFNULL(re.inviter,0) inviter, sum( re.canclord ) canclord, sum( re.completeord ) completeord, sum( re.returnord ) returnord, sum( re.afsaleord ) afsaleord, "+
            " IFNULL(sum( re.returnordamt ),0) canclordamt, IFNULL(sum( re.originalprice ) ,0)originalprice, IFNULL(sum( re.payamt ),0) payamt, IFNULL(sum( re.distamt ),0) distamt, IFNULL(sum( re.balamt ),0) balamt, "+
            " IFNULL(max( re.payamt ),0) maxpayamt, IFNULL(min( re.payamt ),0) minpayamt, IFNULL(avg( re.payamt ),0) avgpayamt, IFNULL(sum( re.realrefamt ),0) realrefamt, re.odate "+
            "FROM ( " +
            " SELECT o.inviter inviter, "+
            " ( SELECT count( * ) FROM {{?"+ DSMConst.TD_BK_TRAN_ORDER +"}} ord WHERE ord.ostatus =- 4 AND ord.orderno = o.orderno ) canclord, "+
            " ( SELECT count( * ) FROM {{?"+ DSMConst.TD_BK_TRAN_ORDER +"}} ord WHERE ord.ostatus = 4 AND ord.orderno = o.orderno ) completeord, "+
            " ( SELECT count( * ) FROM {{?"+ DSMConst.TD_BK_TRAN_ORDER +"}} ord WHERE (ord.ostatus =-1 or ord.ostatus =-2 or ord.ostatus =-3) AND ord.orderno = o.orderno ) returnord, "+
            " ( SELECT count( * ) FROM {{?"+ DSMConst.TD_BK_TRAN_ORDER +"}} ord WHERE ord.asstatus != 0 AND ord.orderno = o.orderno ) afsaleord, "+
            " ( SELECT o.payamt FROM {{?"+ DSMConst.TD_BK_TRAN_ORDER +"}} ord WHERE ord.ostatus =- 4 AND ord.orderno = o.orderno ) returnordamt, "+
            " ( SELECT o.pdamt FROM {{?"+ DSMConst.TD_BK_TRAN_ORDER +"}} ord WHERE ord.ostatus =4 AND ord.orderno = o.orderno ) originalprice, "+
            " ( SELECT o.payamt FROM {{?"+ DSMConst.TD_BK_TRAN_ORDER +"}} ord WHERE (ord.ostatus =4) AND ord.orderno = o.orderno ) payamt, "+
            " ( SELECT o.distamt FROM {{?"+ DSMConst.TD_BK_TRAN_ORDER +"}} ord WHERE (ord.ostatus =4) AND ord.orderno = o.orderno ) distamt, "+
            " ( SELECT o.balamt FROM {{?"+ DSMConst.TD_BK_TRAN_ORDER +"}} ord WHERE (ord.ostatus =4) AND ord.orderno = o.orderno ) balamt, "+
            " o.realrefamt realrefamt, o.odate "+
            " FROM ( "+
            " SELECT comp.cid cid, comp.inviter inviter, ord.asstatus asstatus, ord.orderno orderno, ord.cusno cusno, ord.ostatus ostatus, "+
            " ord.pdamt pdamt, ord.payamt payamt, ord.distamt distamt, ord.balamt balamt, IFNULL( asapp.realrefamt, 0 ) realrefamt, ord.odate "+
            " FROM {{?"+ DSMConst.TD_BK_TRAN_ORDER +"}} ord LEFT JOIN {{?"+DSMConst.TD_TRAN_ASAPP+"}} asapp ON ord.orderno = asapp.orderno LEFT JOIN tb_bk_comp comp ON ord.cusno = comp.cid "+
            " ) o  ) re ";

    private static String _SELECT_WHERE = "";
    private static final String _SELECT_GROUP = "  GROUP BY re.inviter  ";

    /**
     * 获取所有订单详情
     * @return
     */
    public static List<BDOrderAchieveemntVO> executeOrderInfos(AppContext appContext){
        StringBuilder builder = new StringBuilder();
        builder.append(_QUERY_ORDER);
//        builder.append(getOrdWhereParam(_SELECT_WHERE));
        builder.append(_SELECT_GROUP);

        List<Object[]> queryResult = BaseDAO.getBaseDAO()
                .queryNativeSharding(GLOBALConst.COMP_INIT_VAR, TimeUtils.getCurrentYear(), builder.toString());
        BDOrderAchieveemntVO[] bdOrderAchieveemntVOS = new BDOrderAchieveemntVO[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult,bdOrderAchieveemntVOS,BDOrderAchieveemntVO.class);

        //System.out.println(boList.size());
        return Arrays.asList(bdOrderAchieveemntVOS);
    }



}
