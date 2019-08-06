package com.onek.goods;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.goods.entities.BGWishListVO;
import com.onek.goods.entities.BgProdVO;
import constant.DSMConst;
import dao.BaseDAO;
import util.GsonUtils;
import util.MathUtil;
import util.StringUtils;

import java.util.List;


/**
 * @Author: leeping
 * @Date: 2019/8/6 10:33
 */
public class WishListModule {
    private static final BaseDAO BASE_DAO = BaseDAO.getBaseDAO();
    /**运营后台查询心愿单详情*/
    private static String _QUERY_WISHLIST = "SELECT " +
            "wl.oid oid, "+
            "wl.cid cid, "+
            "comp.cname cname, " +
            "wl.prodname prodname, " +
            "wl.manuname manuname, " +
            "wl.spec spec, " +
            "wl.num num, " +
            "wl.dtaile dtaile, " +
            "wl.price/100 price, " +
            "CONCAT( wl.submitdate, ' ', wl.submittime ) submitdate, " +
            "CONCAT( wl.auditdate, ' ', wl.audittime ) auditdate, " +
            "su.urealname auditname , " +
            "wl.cstatus cstatus, " +
            "wl.auditid auditid " +
            "FROM " +
            "{{?" + DSMConst.TD_WISH_LIST + "}} wl " +
            "LEFT JOIN {{?" + DSMConst.TB_COMP + "}} comp ON wl.cid = comp.cid " +
            "LEFT JOIN {{?" + DSMConst.TB_SYSTEM_USER + "}} su ON wl.auditid = su.uid where 1=1 ";

    private static String _UPDATE_WISHLIST_STATUS = "update {{?" + DSMConst.TD_WISH_LIST + "}} set cstatus = ? where oid = ?";
    /**
     * @auther lz
     * 查询心愿单List
     * @入参参数 appContext{pageIndex：页码，pageSize：显示数，arrays:[cid(企业码)，prodname商品名，cstatus(状态)]}
     * @返回值
     */
    public Result queryWishList(AppContext appContext){
        Page page = new Page();
        page.pageIndex = appContext.param.pageIndex;
        page.pageSize = appContext.param.pageNumber;

        PageHolder pageHolder = new PageHolder(page);
        StringBuilder sql = new StringBuilder(_QUERY_WISHLIST);
        //查询条件
        String[] params = appContext.param.arrays;
        String param = null;
        if(params.length<=0){
            //app query param
            int compid = appContext.getUserSession().compId;
            if(compid<=0){
                return new Result().fail("用户未登录！");
            }
            sql.append(" and wl.cid = "+compid);
        }else{
            //pc query param
            StringBuilder paramSql = new StringBuilder();

            if(!StringUtils.isEmpty(params[0])) paramSql.append("and comp.cname LIKE '%").append(params[0]).append("%' ");
            if(!StringUtils.isEmpty(params[1])) paramSql.append("and wl.prodname LIKE '%").append(params[1]).append("%' ");
            if(!StringUtils.isEmpty(params[2])) paramSql.append("and wl.cstatus = ").append(params[2]);

            sql.append(paramSql.toString());
        }
        List<Object[]> queryResult = BASE_DAO.queryNative(
                pageHolder, page, " wl.submitdate DESC", sql.toString());
        BGWishListVO[] bgWishListVOS = new BGWishListVO[queryResult.size()];

        BASE_DAO.convToEntity(queryResult, bgWishListVOS, BGWishListVO.class);

        return new Result().setQuery(bgWishListVOS, pageHolder);
    }

    /**
     * 查询当前心愿单详情
     * @param appContext
     * @return
     */
    public Result getWishListDeitl(AppContext appContext){

        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        if(!jsonObject.has("wsId")) return new Result().fail("当前参数错误");
        int wsId = jsonObject.get("wsId").getAsInt();
        if(wsId<=0){
            return new Result().fail("当前心愿单不存在");
        }
        StringBuilder sql = new StringBuilder(_QUERY_WISHLIST);
        sql.append("and wl.oid = ?");

        List<Object[]> queryResult = BASE_DAO.queryNative(sql.toString(),wsId);

        BGWishListVO[] bgWishListVOS = new BGWishListVO[queryResult.size()];

        BASE_DAO.convToEntity(queryResult, bgWishListVOS, BGWishListVO.class);

        return new Result().success("心愿单查询成功",bgWishListVOS[0]);

    }

    /**
     * 修改心愿单状态
     * @param appContext
     * @return
     */
    public Result updateWishList(AppContext appContext){

//        int compid = appContext.getUserSession().compId;
//        if (compid == 0) return new Result().fail("请登录后在操作!");
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int wsId = jsonObject.get("wsId").getAsInt();
        String useType = jsonObject.get("useType").getAsString();
        if(wsId<=0){
            return new Result().fail("当前心愿单不存在");
        }
        StringBuilder sql = new StringBuilder("select cstatus from {{?"+ DSMConst.TD_WISH_LIST+"}} where oid = ?");
        List<Object[]> result = BaseDAO.getBaseDAO().queryNative(sql.toString(),wsId);
        BGWishListVO[] bgWishListVOS = new BGWishListVO[result.size()];
        BASE_DAO.convToEntity(result, bgWishListVOS, BGWishListVO.class,new String[]{"cstatus"});
        System.out.println("bgWishListVOS[0].cstatus&4 = " + bgWishListVOS[0].cstatus);
        int statu ;
        if("shipped".equals(useType)){
            if((bgWishListVOS[0].cstatus&4) == 4){
                return new Result().fail("心愿单状态已在到货状态！无法再次修改");
            }
            statu = 4;
        }else{
            if((bgWishListVOS[0].cstatus&2) == 2){
                return new Result().fail("心愿单状态无法改变！");
            }
            statu = 2;
        }
        int row = BASE_DAO.updateNative(_UPDATE_WISHLIST_STATUS,statu,wsId);

        if(row>0){
            return new Result().success("状态更新成功","操作成功");
        }else{
            return new Result().fail("操作失败");
        }
    }


    private static class Param{
        String prodname;//药品名
        String spec;//规格
        String manuname;// 厂家名

        String num; //数量
        String price; //价格
        String detail; // 50字以内

        int _num = 0;
        double _price = 0.0f;
    }


    public Result add(AppContext context){
        int cid = context.getUserSession().compId;
        if (cid == 0) return new Result().fail("异常操作");
        String json = context.param.json;
        Param p = GsonUtils.jsonToJavaBean(json,Param.class);
        if (p == null) return new Result().fail("提交心愿单失败,信息不完整");

        if (StringUtils.isEmpty(p.prodname,p.spec,p.manuname)) return new Result().fail("请输入心愿单必选参数");
        if (!StringUtils.isEmpty(p.num)){
            try {
                p._num = Integer.parseInt(p.num);
            } catch (NumberFormatException e) {
                return new Result().fail("心愿单数量输入异常");
            }
        }

        if (!StringUtils.isEmpty(p.price)){
            try {
                p._price = Double.parseDouble(p.price);
            } catch (NumberFormatException e) {
                return new Result().fail("心愿单价格输入异常");
            }
        }

        if (!StringUtils.isEmpty(p.detail)){
            if (p.detail.length() > 50) return new Result().fail("心愿单详情超过50字限制");
        }

        //查询是否存在相同心愿单
        String sql = "SELECT * FROM {{?"+ DSMConst.TD_WISH_LIST+"}} WHERE prodname=? AND spec=? AND manuname=?";
        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(sql,p.prodname,p.spec,p.manuname);
        if (lines.size() > 0) return new Result().success("已存在此心愿单");
        //添加-存储数据
        String insertSql = "INSERT INTO {{?"+ DSMConst.TD_WISH_LIST +"}} " +
                "(cid,prodname,prodnameh,spec,manuname,manunameh,num,price,dtaile, submitdate,submittime) " +
                "VALUES(?,?,crc32(?),?,?,crc32(?),?,?,?,CURRENT_DATE,CURRENT_TIME)";

        int price = MathUtil.exactMul(p._price,100.0f).intValue();
        int i = BaseDAO.getBaseDAO().updateNative(insertSql,cid,p.prodname,p.prodname,p.spec,p.manuname,p.manuname,p._num,price,p.detail);
        if (i <= 0)return new Result().fail("提交心愿单失败,无法保存数据");
        return new Result().success("已提交心愿单");
    }

}
