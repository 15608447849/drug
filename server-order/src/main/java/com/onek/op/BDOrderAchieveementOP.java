package com.onek.op;


import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.onek.context.AppContext;
import com.onek.entity.BDOrderAchieveemntVO;
import com.onek.entity.TranOrder;
import com.onek.entitys.Result;
import com.onek.order.BackOrderInfoModule;
import com.onek.util.GLOBALConst;
import com.onek.util.IceRemoteUtil;
import constant.DSMConst;
import dao.BaseDAO;
import util.*;

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
            " FROM {{?"+ DSMConst.TD_BK_TRAN_ORDER +"}} ord LEFT JOIN {{?"+DSMConst.TD_TRAN_ASAPP+"}} asapp ON ord.orderno = asapp.orderno LEFT JOIN tb_bk_comp comp ON ord.cusno = comp.cid GROUP BY ord.orderno ${var} "+
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
        }else{
            sql = sql.replace("${var}","");
        }

        List<Object[]> queryResult = BaseDAO.getBaseDAO()
                .queryNativeSharding(GLOBALConst.COMP_INIT_VAR, TimeUtils.getCurrentYear(), sql);
        BDOrderAchieveemntVO[] bdOrderAchieveemntVOS = new BDOrderAchieveemntVO[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult,bdOrderAchieveemntVOS,BDOrderAchieveemntVO.class);

        //System.out.println(boList.size());
        return Arrays.asList(bdOrderAchieveemntVOS);
    }




    private static String _QUERY_CUMULATIVE_SG = "SELECT IFNULL(re.inviter,0) inviter, IFNULL(count( DISTINCT cid ),0) countnum  FROM" +
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


    private static String _QUERY_NEWADD_CUMULATIVE_SQ ="SELECT DISTINCT IFNULL(co.inviter,0) inviter, IFNULL(count(DISTINCT co.cid),0) compnum" +
            " FROM ( SELECT comp.cid cid, comp.inviter inviter, comp.cname cname, ord.orderno orderno, ord.ostatus ostatus, ord.odate odate " +
            " FROM tb_bk_comp comp, {{?"+DSMConst.TD_BK_TRAN_ORDER+"}} ord " +
            " WHERE comp.cid = ord.cusno  AND ord.ostatus > 0  AND ord.settstatus = 1  ) co " +
            " WHERE co.odate BETWEEN ?  AND ?  AND co.cid NOT IN (" +
            " SELECT DISTINCT o.cid  FROM ( SELECT DISTINCT o.cusno cid " +
            " FROM {{?"+DSMConst.TD_BK_TRAN_ORDER+"}} o " +
            " WHERE o.odate < ? AND o.settstatus = 1 AND o.ostatus > 0  ) r " +
            " INNER JOIN ( SELECT DISTINCT ( co.cid ) cid " +
            " FROM ( SELECT comp.cid cid, comp.inviter inviter, comp.cname cname, ord.orderno orderno, ord.ostatus ostatus, ord.odate odate " +
            " FROM tb_bk_comp comp, {{?"+DSMConst.TD_BK_TRAN_ORDER+"}} ord " +
            " WHERE comp.cid = ord.cusno  AND ord.ostatus > 0  AND ord.settstatus = 1  ) co " +
            " WHERE co.odate BETWEEN ? AND ?  ) o ON r.cid = o.cid  ) GROUP BY co.inviter";


    private static final String TB_BK_SYSTEM_USER= "tb_bk_system_user";

    public static Map getNewAddCumulative(String[] time){
        StringBuilder sb = new StringBuilder(_QUERY_NEWADD_CUMULATIVE_SQ);
        List<Object[]> list = BaseDAO.getBaseDAO().queryNativeSharding(GLOBALConst.COMP_INIT_VAR,TimeUtils.getCurrentYear(),sb.toString(),time[0],time[1],time[1],time[0],time[1]);
        Map map = new HashMap();
        for (int i = 0;i<list.size();i++){
            map.put(list.get(i)[0].toString(),list.get(i)[1].toString());
        }
        return map;
    }

    private class Param{
        private int selectFlag; //查询订单类型
        private long uid; //当前用户id
        private long roleid; //当前用户权限id
        private String sdate; //开始时间
        private String edate; //结束时间
    }


    private static String _QUERY_BD_INFO = "select DISTINCT ord.orderno, ord.tradeno, ord.cusno, comp.cname, comp.caddrcode, u.urealname, ord.busno, ord.ostatus, "
            + " ord.asstatus, ord.pdnum, ord.pdamt, ord.freight, ord.payamt, "
            + " ord.coupamt, ord.distamt, ord.rvaddno, ord.shipdate, ord.shiptime, "
            + " ord.settstatus, ord.settdate, ord.setttime, ord.otype, ord.odate, "
            + " ord.otime, ord.cstatus, ord.consignee, ord.contact, ord.address, ord.balamt, ord.payway, ord.remarks, ord.invoicetype ";

    private static String _ASORD_TABLE = " FROM tb_bk_comp comp, tb_bk_system_user u , {{?"+DSMConst.TD_BK_TRAN_ORDER+"}} ord,{{?"+DSMConst.TD_TRAN_ASAPP+"}} asord WHERE comp.cid = ord.cusno AND comp.inviter = u.uid  AND ord.orderno = asord.orderno " ;

    private static String _ORDER_TABLE = " FROM tb_bk_comp comp, {{?"+DSMConst.TD_BK_TRAN_ORDER+"}} ord, tb_bk_system_user u WHERE comp.cid = ord.cusno AND comp.inviter = u.uid  " ;

    public static Result getBDUserOrderInfo(AppContext appContext){

        Page page = new Page();
        page.pageIndex = appContext.param.pageIndex;
        page.pageSize = appContext.param.pageNumber;

        PageHolder pageHolder = new PageHolder(page);

        StringBuilder sb = new StringBuilder(_QUERY_BD_INFO);
        String json = appContext.param.json;
        Param param = GsonUtils.jsonToJavaBean(json,Param.class);
        if(param == null){
            return new Result().fail("查询失败");
        }
        if(param.selectFlag == 1 || param.selectFlag == 2){ //完成订单-1  取消订单-2
            sb.append(_ORDER_TABLE);
            if(param.selectFlag == 1){ //完成订单
                sb.append(" AND ord.ostatus !=- 4 AND ord.ostatus != 0 ");
            }else{//取消订单
                sb.append(" AND ord.ostatus =-4 ");
            }
        }else if(param.selectFlag == 3 || param.selectFlag == 4 || param.selectFlag == 5){ //售后-3   退货-4   退款-5
            sb.append(_ASORD_TABLE);
            if(param.selectFlag == 3){ //售后
                sb.append(" AND ( asord.ckstatus = 1 OR asord.ckstatus = 200 ) ");
            }else if(param.selectFlag == 4){//退货订单
                sb.append(" AND asord.astype IN ( 0, 1, 2 ) AND ( asord.ckstatus = 1 OR asord.ckstatus = 200 ) ");
            }else{
                sb.append(" AND asord.astype IN (1, 2 ) AND ( asord.ckstatus = 1 OR asord.ckstatus = 200 ) ");
            }
        }else if(param.selectFlag == 6){ //小计-6
            sb.append(_ORDER_TABLE);
            sb.append(" AND ord.ostatus != 0 ");
        }
        sb.append(" AND ord.odate BETWEEN ? and ? ");
        String pdata = getGLUser(param.uid,param.roleid);
        if(pdata.length()<=0 || StringUtils.isEmpty(pdata)){
            return  new Result().fail("当前人员暂无订单信息！");
        }
        sb.append(" AND comp.inviter IN ("+pdata+") ");

        List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNativeSharding(
                GLOBALConst.COMP_INIT_VAR, TimeUtils.getCurrentYear(), pageHolder, page,
                " ord.oid DESC ", sb.toString(), param.sdate,param.edate);

        TranOrder[] result = new TranOrder[queryResult.size()];
        String[] reParam = new String[]{"orderno","tradeno","cusno","cname","cusaddrcode","urealname",
                                        "busno","ostatus","asstatus","pdnum","pdamt","freight",
                                        "payamt","coupamt","distamt","rvaddno","shipdate","shiptime",
                                        "settstatus","settdate","setttime","otype","odate","otime",
                                        "cstatus","consignee","contact","address","balamt","payway","remarks","invoicetype"};
        BaseDAO.getBaseDAO().convToEntity(queryResult, result, TranOrder.class,reParam);

        Map<String, String> compMap;
        for (TranOrder tranOrder : result) {

            String compAddr = IceRemoteUtil.getCompleteName(tranOrder.getCusaddrcode());
            /*
            compMap = GsonUtils.string2Map(compStr);

            if (compMap != null) {
                tranOrder.setCusname(compMap.get("storeName"));
            }
            */
//            tranOrder.setGoods(getOrderGoods(tranOrder.getOrderno(), compid));
            tranOrder.setCusaddr(compAddr);
            tranOrder.setPayamt(MathUtil.exactDiv(tranOrder.getPayamt(), 100).doubleValue());
            tranOrder.setFreight(MathUtil.exactDiv(tranOrder.getFreight(), 100).doubleValue());
            tranOrder.setPdamt(MathUtil.exactDiv(tranOrder.getPdamt(), 100).doubleValue());
            tranOrder.setDistamt(MathUtil.exactDiv(tranOrder.getDistamt(), 100).doubleValue());
            tranOrder.setCoupamt(MathUtil.exactDiv(tranOrder.getCoupamt(), 100).doubleValue());
            tranOrder.setBalamt(MathUtil.exactDiv(tranOrder.getBalamt(), 100).doubleValue());
        }


        return  new Result().success(result);
    }


    private static String getGLUser(long uid,long roleid){
        StringBuilder sb = new StringBuilder();
        String sql = "";

        if((roleid & 8192) >0){ //BD
            sb.delete( 0, sb.length() );
            sb.append("select uid ,roleid,urealname FROM {{?"+DSMConst.TB_BK_SYSTEM_USER+"}} where (roleid&8192>0) and belong = ? UNION select uid ,roleid,urealname FROM {{?"+DSMConst.TB_BK_SYSTEM_USER+"}} where uid = ? and roleid&8192>0");
            sql = sb.toString();
        }

        if((roleid & 4096)>0){ //BDM
            sb.delete( 0, sb.length() );
            sb.append("select uid ,roleid,urealname FROM {{?"+DSMConst.TB_BK_SYSTEM_USER+"}} where (roleid&8192>0) and belong = ? UNION select uid ,roleid,urealname FROM {{?"+DSMConst.TB_BK_SYSTEM_USER+"}} where uid = ? and roleid&8192>0");
            sql = sb.toString();
        }

        if((roleid & 2048)>0){ //城市经理
            sb.delete( 0, sb.length() );
            sb.append("select uid ,roleid,urealname FROM {{?"+DSMConst.TB_BK_SYSTEM_USER+"}} where (roleid&4096>0 and roleid&8192>0) and belong = ? UNION ");
            sb.append(" select uid ,roleid,urealname FROM {{?"+DSMConst.TB_BK_SYSTEM_USER+"}} where (roleid&8192>0) and belong in (select uid FROM {{?"+DSMConst.TB_BK_SYSTEM_USER+"}} where (roleid&4096>0) and belong = ?) ");
            sql = sb.toString();
        }

        if((roleid & 1024)>0){ //渠道经理
            sb.delete( 0, sb.length() );
            sb.append("select uid ,roleid,urealname FROM {{?"+DSMConst.TB_BK_SYSTEM_USER+"}} where (roleid&4096>0 and roleid&8192>0) and belong in (select uid FROM {{?"+DSMConst.TB_BK_SYSTEM_USER+"}} where (roleid&2048>0) and belong = ?) UNION ");
            sb.append(" select uid ,roleid,urealname FROM {{?"+DSMConst.TB_BK_SYSTEM_USER+"}} where (roleid&8192>0) and belong in (select uid FROM {{?"+DSMConst.TB_BK_SYSTEM_USER+"}} where (roleid&4096>0) and belong in (select uid FROM tb_bk_system_user where ");
            sb.append(" (roleid&2048>0) and belong = ?))");
            sb.append(" UNION SELECT uid, roleid, urealname  FROM {{?"+DSMConst.TB_BK_SYSTEM_USER+"}}  WHERE ( roleid & 2048 > 0 AND roleid & 4096 > 0 AND roleid & 8192 > 0 ) AND belong = ? ");
            sql = sb.toString();
        }

        if((roleid & 512)>0){ //渠道总监
            sb.delete( 0, sb.length() );
            sb.append("select uid ,roleid,urealname FROM {{?"+DSMConst.TB_BK_SYSTEM_USER+"}} where (roleid&4096>0 and roleid&8192>0) and belong in (select uid FROM tb_bk_system_user where (roleid&2048>0) and belong in (select uid FROM ");
            sb.append(" {{?"+DSMConst.TB_BK_SYSTEM_USER+"}} where (roleid&1024>0) and belong = ?)) UNION ");
            sb.append(" select uid ,roleid,urealname FROM {{?"+DSMConst.TB_BK_SYSTEM_USER+"}} where (roleid&8192>0) and belong in (select uid FROM {{?"+DSMConst.TB_BK_SYSTEM_USER+"}} where (roleid&4096>0) and belong in (select uid FROM tb_bk_system_user where ");
            sb.append(" (roleid&2048>0) and belong in (select uid FROM {{?"+DSMConst.TB_BK_SYSTEM_USER+"}} where (roleid&1024>0) and belong = ?))) ");
            sb.append(" UNION SELECT uid, roleid, urealname  FROM {{?"+DSMConst.TB_BK_SYSTEM_USER+"}}  WHERE ( roleid & 2048 > 0 and roleid&4096>0 and roleid&8192>0)  AND belong IN ( SELECT uid FROM {{?"+DSMConst.TB_BK_SYSTEM_USER+"}} WHERE ( roleid & 1024 > 0 ) AND belong = ? ) ");
            sql = sb.toString();
        }
        List<Object[]> list;
        if((roleid & 1024)>0 || (roleid & 512)>0){
            list = BaseDAO.getBaseDAO().queryNativeSharding(GLOBALConst.COMP_INIT_VAR,0,sql,uid,uid,uid);
        }else{
            list = BaseDAO.getBaseDAO().queryNativeSharding(GLOBALConst.COMP_INIT_VAR,0,sql,uid,uid);
        }

        StringBuilder param = new StringBuilder();
        for (Object[] objs: list){
            param.append(objs[0].toString()+",");
        }
        String params = param.toString();
        if(params.length()>0) {
            params = params.substring(0, params.length() - 1);
        }
        if((roleid & 8192) >0){
            if(params.indexOf(String.valueOf(uid)) < 0 ){
                if(params.length()>0){
                    params += ","+uid;
                }else{
                    params = ""+uid;
                }
            }
        }
        System.out.println("==============查询条件："+params);
        return params;
    }
}
