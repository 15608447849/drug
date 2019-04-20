package com.onek.order;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entity.AppriseVO;
import com.onek.entitys.Result;
import constant.DSMConst;
import dao.BaseDAO;
import global.GenIdUtil;
import util.ModelUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author 11842
 * @version 1.1.1
 * @description 订单其他操作模块
 * @time 2019/4/20 14:27
 **/
public class OrderOptModule {
    private static BaseDAO baseDao = BaseDAO.getBaseDAO();

    //商品评价
    private static final String INSERT_APPRISE_SQL = "insert into {{?" + DSMConst.TD_TRAN_APPRAISE + "}} "
            + "(unqid,orderno,level,descmatch,logisticssrv,"
            + "content,createtdate,createtime,cstatus,compid,sku) "
            + " values(?,?,?,?,?,"
            + "?,CURRENT_DATE,CURRENT_TIME,0,?,"
            + "?)";

    /* *
     * @description 评价
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/20 14:48
     * @version 1.1.1
     **/
    @UserPermission(ignore = false)
    public Result insertApprise(AppContext appContext) {
        Result result = new Result();
        Gson gson = new Gson();
        LocalDateTime localDateTime = LocalDateTime.now();
        List<Object[]> params = new ArrayList<>();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String orderNo = jsonObject.get("orderno").getAsString();
        int compid = jsonObject.get("compid").getAsInt();
        JsonArray appriseArr = jsonObject.get("appriseArr").getAsJsonArray();
        for (int i = 0; i < appriseArr.size(); i++) {
            AppriseVO appriseVO = gson.fromJson(appriseArr.get(i).toString(), AppriseVO.class);
            params.add(new Object[]{GenIdUtil.getUnqId(), orderNo, appriseVO.getLevel(), appriseVO.getDescmatch(),
                    appriseVO.getLogisticssrv(), appriseVO.getContent(),compid, appriseVO.getSku()});
        }
        boolean b = !ModelUtil.updateTransEmpty(baseDao.updateBatchNativeSharding(0, localDateTime.getYear(),
                INSERT_APPRISE_SQL, params, params.size()));
        return b ? result.success("评价成功!"): result.fail("评价失败!");
    }

    /* *
     * @description 查询商品评价
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/20 15:07
     * @version 1.1.1
     **/
    @UserPermission(ignore = false)
    public Result getGoodsApprise(AppContext appContext) {
        Result result = new Result();
        LocalDateTime localDateTime = LocalDateTime.now();
        String json = appContext.param.json;
        Page page = new Page();
        page.pageIndex = appContext.param.pageIndex;
        page.pageSize = appContext.param.pageNumber;
        PageHolder pageHolder = new PageHolder(page);
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        long sku = jsonObject.get("sku").getAsLong();
        String selectSQL = "select unqid,orderno,level,descmatch,logisticssrv,"
                + "content,createtdate,createtime,cstatus,compid from {{?"
                + DSMConst.TD_TRAN_APPRAISE + "}} where cstatus&1=0 and sku=" + sku;
        List<Object[]> queryResult = baseDao.queryNativeSharding(0, localDateTime.getYear(),
                pageHolder, page, selectSQL);
        AppriseVO[] appriseVOS = new AppriseVO[queryResult.size()];
        baseDao.convToEntity(queryResult, appriseVOS, AppriseVO.class);
        for (AppriseVO appriseVO : appriseVOS) {
            appriseVO.setCompName("李世平-A门面");//暂无接口。。。。
        }
        return result.setQuery(appriseVOS, pageHolder);
    }
}
