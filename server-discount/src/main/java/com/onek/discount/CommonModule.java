package com.onek.discount;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.discount.entity.GiftableProdVO;
import com.onek.discount.entity.PromGiftVO;
import com.onek.discount.entity.RulesVO;
import com.onek.entitys.Result;
import constant.DSMConst;
import dao.BaseDAO;
import util.ArrayUtil;
import util.MathUtil;
import util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @服务名 discountServer
 * @author Administrator
 * @version V1.0
 * @ClassName CommonModule
 * @Description TODO
 * @date 2019-04-02 12:01
 */
public class CommonModule {


    private static BaseDAO baseDao = BaseDAO.getBaseDAO();

    private static final String SELECT_LADDER_NO = "select IFNULL(max(SUBSTR(offercode,6)),0) from {{?" + DSMConst.TD_PROM_LADOFF + "}} ";


    /**
     * @接口摘要 查询赠品信息
     * @业务场景 下拉查询赠品
     * @传参类型
     * @传参列表
     * @返回列表 PromGiftVO对象数组
     */
    @UserPermission(ignore = true)
    public Result queryPromGift(AppContext appContext) {
        Result result = new Result();
        String[] params = appContext.param.arrays;
        Page page = new Page();
        page.pageIndex = appContext.param.pageIndex;
        page.pageSize = appContext.param.pageNumber;
        PageHolder pageHolder = new PageHolder(page);
        String selectSQL = "select unqid giftno,giftname, giftdesc, cstatus, createdate, createtime, updatedate, updatetime from {{?" + DSMConst.TD_PROM_GIFT + "}} where cstatus&1=0 ";

        StringBuilder sql = new StringBuilder(selectSQL);

        List<Object> paramList = new ArrayList<>();
        String param;
        for (int i = 0; i < params.length; i++) {
            param = params[i];

            if (StringUtils.isEmpty(param)) {
                continue;
            }

            try {
                switch (i) {
                    case 0:
                        sql.append(" AND giftname LIKE ? ");
                        param = "%" + param + "%";
                        break;
                    case 1:
                        sql.append(" AND cstatus&? > 0 ");
                        break;
                }
            } catch (Exception e) {
                continue;
            }

            paramList.add(param);
        }

        List<Object[]> queryResult = baseDao.queryNative(pageHolder, page, sql.toString(), paramList.toArray());
        PromGiftVO[] promGiftVOS = new PromGiftVO[queryResult.size()];
        baseDao.convToEntity(queryResult, promGiftVOS, PromGiftVO.class, new String[]{"giftno", "giftname", "desc", "cstatus", "createdate", "createtime", "updatedate", "updatetime"});
        return result.setQuery(promGiftVOS, pageHolder);
    }

    private static final String QUERY_PROD_BASE =
            " SELECT spu.spu, spu.popname, spu.prodname, spu.standarno, "
                    + " spu.brandno, b.brandname, spu.manuno, m.manuname, "
                    + " sku.sku, sku.vatp, sku.mp, sku.rrp, sku.spec, sku.wp, "
                    + " sku.erpsku "
                    + " FROM ({{?" + DSMConst.TD_PROD_SPU + "}} spu "
                    + " INNER JOIN {{?" + DSMConst.TD_PROD_SKU + "}} sku ON spu.spu = sku.spu ) "
                    + " LEFT  JOIN {{?" + DSMConst.TD_PROD_MANU
                    + "}} m ON m.cstatus&1 = 0 AND m.manuno  = spu.manuno "
                    + " LEFT  JOIN {{?" + DSMConst.TD_PROD_BRAND
                    + "}} b ON b.cstatus&1 = 0 AND b.brandno = spu.brandno "
                    + " WHERE sku.cstatus&1 = 0 AND spu.rx <> 2 "
                    + " AND sku.erpsku IS NOT NULL AND LENGTH(sku.erpsku) > 0 ";

    /**
     * @接口摘要 查询可作为赠品的药品信息
     * @业务场景 增加赠品
     * @传参类型 array
     * @传参列表 [prodname, manuno, spec, standarno, popname]
     * @返回列表 PromGiftVO对象数组
     */
    public Result queryGiftableProd(AppContext appContext) {
        Page page = new Page();
        page.pageIndex = appContext.param.pageIndex;
        page.pageSize = appContext.param.pageNumber;

        PageHolder pageHolder = new PageHolder(page);

        StringBuilder sql = new StringBuilder(QUERY_PROD_BASE);

        List<Object> paramList = new ArrayList<>();
        String[] params = appContext.param.arrays;
        String param = null;
        for (int i = 0; i < params.length; i++) {
            param = params[i];

            if (StringUtils.isEmpty(param)) {
                continue;
            }

            try {
                switch (i) {
                    case 0:
                        sql.append(" AND spu.prodname LIKE ? ");
                        param = "%" + param + "%";
                        break;
                    case 1:
                        sql.append(" AND spu.manuno = ? ");
                        break;
                    case 2:
                        sql.append(" AND sku.spec LIKE ? ");
                        param = "%" + param + "%";
                        break;
                    case 3:
                        sql.append(" AND spu.standarnoh = CRC32(?) AND spu.standarno = ? ");
                        paramList.add(param);
                        break;
                    case 4:
                        sql.append(" AND spu.popname LIKE ? ");
                        param = "%" + param + "%";
                        break;

                }
            } catch (Exception e) {
                continue;
            }

            paramList.add(param);
        }

        List<Object[]> queryResult = baseDao.queryNative(
                pageHolder, page, " sku.oid DESC ", sql.toString(), paramList.toArray());

        GiftableProdVO[] result = new GiftableProdVO[queryResult.size()];

        baseDao.convToEntity(queryResult, result, GiftableProdVO.class);

        for (GiftableProdVO giftableProdVO : result) {
            giftableProdVO.setVatp(MathUtil.exactDiv(giftableProdVO.getVatp(), 100).doubleValue());
            giftableProdVO.setRrp(MathUtil.exactDiv(giftableProdVO.getRrp(), 100).doubleValue());
            giftableProdVO.setMp(MathUtil.exactDiv(giftableProdVO.getMp(), 100).doubleValue());
            giftableProdVO.setWp(MathUtil.exactDiv(giftableProdVO.getWp(), 100).doubleValue());
        }

        return new Result().setQuery(result, pageHolder);
    }

    /**
     * @接口摘要 增加赠品
     * @业务场景 增加赠品
     * @传参类型 json
     * @传参列表 com.onek.discount.entity.PromGiftVO[]
     * @返回列表 200 成功 -1 失败
     */
    public Result addGift(AppContext appContext) {
        List<PromGiftVO> giftList =
                JSONArray.parseArray(appContext.param.json, PromGiftVO.class);

        if (giftList == null || giftList.isEmpty()) {
            return new Result().fail("请选择赠品");
        }

        List<Object[]> params = new ArrayList<>(giftList.size());

        giftList.forEach((giftVo) -> {
            if (StringUtils.isInteger(giftVo.getGiftno())) {
                params.add(new Object[] {
                        giftVo.getGiftno(), giftVo.getGiftname(),
                        giftVo.getDesc(), giftVo.getCstatus(), giftVo.getGiftno() } );
            }
        });

        String sql = " INSERT INTO {{?" + DSMConst.TD_PROM_GIFT + "}} "
                + " (unqid, giftname, giftdesc, cstatus, createdate, createtime) "
                + " SELECT ?, ?, ?, ?, CURRENT_DATE, CURRENT_TIME "
                + " FROM DUAL "
                + " WHERE NOT EXISTS ( "
                + "     SELECT * "
                + "     FROM {{?" + DSMConst.TD_PROM_GIFT + "}} "
                + "     WHERE unqid = ? AND cstatus&1 = 0 ) ";

        baseDao.updateBatchNative(sql, params, params.size());

        return new Result().success();
    }

    /**
     * @接口摘要 修改赠品
     * @业务场景 修改赠品
     * @传参类型 json
     * @传参列表 com.onek.discount.entity.PromGiftVO
     * @返回列表 200 成功 -1 失败
     */
    public Result updateGift(AppContext appContext) {
        PromGiftVO giftVo = JSON.parseObject(appContext.param.json, PromGiftVO.class);

        if (giftVo == null
                || StringUtils.isEmpty(giftVo.getGiftno())) {
            return new Result().fail("参数异常");
        }

        String sql = " UPDATE {{?" + DSMConst.TD_PROM_GIFT + "}} "
                + " SET giftname = ?, giftdesc = ?, updatedate = CURRENT_DATE, updatetime = CURRENT_TIME "
                + " WHERE cstatus&1 = 0 AND unqid = ? ";

        baseDao.updateNative(sql, giftVo.getGiftname(), giftVo.getDesc(), giftVo.getGiftno());

        return new Result().success();
    }

    /**
     * @接口摘要 删除赠品
     * @业务场景 删除赠品
     * @传参类型 json
     * @传参列表 com.onek.discount.entity.PromGiftVO
     * @返回列表 200 成功 -1 失败
     */
    public Result delGift(AppContext appContext) {
        String[] delIds = appContext.param.arrays;

        if (ArrayUtil.isEmpty(delIds)) {
            return new Result().fail("参数异常");
        }

        List<Object[]> params = new ArrayList<>();
        for (String delId : delIds) {
            if (!StringUtils.isEmpty(delId)) {
                params.add(new Object[] { delId });
            }
        }

        String sql = " UPDATE {{?" + DSMConst.TD_PROM_GIFT + "}} "
                + " SET cstatus = cstatus | 1 "
                + " WHERE unqid = ? ";

        baseDao.updateBatchNative(sql, params, params.size());

        return new Result().success();
    }



    /**
     * @接口摘要 保存赠品
     * @业务场景 ERP专用
     * @传参类型 json
     * @传参列表 com.onek.discount.entity.PromGiftVO
     * @返回列表 200 成功 -1 失败
     */
    @UserPermission(ignore = true)
    public Result saveGiftFromERP(AppContext appContext) {
        PromGiftVO gift = JSON.parseObject(appContext.param.json, PromGiftVO.class);

        if (gift == null) {
            return new Result().fail("参数为空");
        }

        String sql =
                " SELECT unqid, giftname, giftdesc "
                + " FROM {{?" + DSMConst.TD_PROM_GIFT + "}} "
                + " WHERE cstatus&1=0 AND unqid = ? ";

        List<Object[]> queryResult = baseDao.queryNative(sql, gift.getGiftno());

        if (queryResult.isEmpty()) {
            sql = " INSERT INTO {{?" + DSMConst.TD_PROM_GIFT + "}} "
                    + " (unqid, giftname, giftdesc, cstatus) "
                    + " VALUES(?, ?, ?, ?) ";

            baseDao.updateNative(sql,
                    gift.getGiftno(), gift.getGiftname(),
                    gift.getDesc(), 0);
        } else {
            sql = " UPDATE {{?" + DSMConst.TD_PROM_GIFT + "}} "
                    + " SET giftname = ?, giftdesc = ? "
                    + " WHERE cstatus&1 = 0 AND unqid = ? ";

            baseDao.updateNative(sql,
                    gift.getGiftname(), gift.getDesc(), gift.getGiftno());
        }




        return new Result().success();
    }


    /**
     * @接口摘要 查询活动规则
     * @业务场景 下拉查询
     * @传参类型 json
     * @传参列表 {type 活动规则码}
     * @返回列表 RulesVO对象数组
     */
    @UserPermission(ignore = true)
    public Result queryRules(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int brulecode = jsonObject.get("type").getAsInt();
        String selectSQL = "select brulecode,rulename from {{?" + DSMConst.TD_PROM_RULE + "}} a where cstatus&1=0 "
                + " and brulecode like '" + brulecode + "%'";
//        String selectSQLEx = "select brulecode,rulename from {{?" + DSMConst.TD_PROM_RULE + "}} a where cstatus&1=0 "
//                + " and brulecode like '" + brulecode + "%' and  NOT EXISTS(select brulecode from {{?"
//                + DSMConst.TD_PROM_ACT +"}} b where cstatus&1=0 and a.brulecode = b.brulecode and brulecode like '"
//                + brulecode +"%' and edate>CURRENT_DATE)";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        RulesVO[] rulesVOS = new RulesVO[queryResult.size()];
        baseDao.convToEntity(queryResult, rulesVOS, RulesVO.class, new String[]{"brulecode", "rulename"});
        return result.success(rulesVOS);
    }


    /**
     * @接口摘要 查询优惠券规则
     * @业务场景 下拉查询
     * @传参类型 json
     * @传参列表 {couptype 优惠券规则码}
     * @返回列表 RulesVO对象数组
     */
    @UserPermission(ignore = true)
    public Result queryCoupRules(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
//        int brulecode = jsonObject.get("type").getAsInt();
        int coupType = jsonObject.get("couptype").getAsInt();
//        String selectActSQL = " SELECT brulecode FROM {{?" + DSMConst.TD_PROM_ACT +"}} where cstatus&1=0 and "
//                + " brulecode like '" + brulecode + "%' and edate>CURRENT_DATE" ;
//        List<Object[]> queryActResult = baseDao.queryNative(selectActSQL);

        String selectSQL = "select brulecode,rulename from {{?" + DSMConst.TD_PROM_RULE + "}} a where cstatus&1=0 "
                + " and brulecode REGEXP '^2' ";

        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        RulesVO[] rulesVOS = new RulesVO[queryResult.size()];
        baseDao.convToEntity(queryResult, rulesVOS, RulesVO.class, new String[]{"brulecode", "rulename"});
        return result.success(rulesVOS);
    }


    /**
     * @接口摘要 查询活动优惠券规则
     * @业务场景 下拉查询
     * @传参类型
     * @传参列表
     * @返回列表 RulesVO对象数组
     */
    @UserPermission(ignore = true)
    public Result queryCoupAssRules(AppContext appContext) {
        Result result = new Result();
//        String json = appContext.param.json;
//        JsonParser jsonParser = new JsonParser();
        String selectSQL = "select brulecode,rulename from {{?" + DSMConst.TD_PROM_RULE + "}} a where cstatus&1=0 "
                + " and brulecode REGEXP '^2' and  NOT EXISTS(select brulecode from {{?"
                + DSMConst.TD_PROM_COUPON +"}} b where cstatus & 128 >0 and cstatus&1=0 and a.brulecode = b.brulecode and "
                + " brulecode REGEXP '^2')";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        RulesVO[] rulesVOS = new RulesVO[queryResult.size()];
        baseDao.convToEntity(queryResult, rulesVOS, RulesVO.class, new String[]{"brulecode", "rulename"});
        return result.success(rulesVOS);
    }


    /**
     * @接口摘要 查询规则
     * @业务场景 下拉查询
     * @传参类型 json
     * @传参列表 {type 满减还是满赠}
     * @返回列表 RulesVO对象数组
     */
    @UserPermission(ignore = true)
    public Result queryRulesByType(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int type = jsonObject.get("type").getAsInt();
        String selectSQL = "select brulecode,rulename from {{?" + DSMConst.TD_PROM_RULE + "}}  where cstatus&1=0 "
                + " and brulecode REGEXP '^" + type + "'";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        RulesVO[] rulesVOS = new RulesVO[queryResult.size()];
        baseDao.convToEntity(queryResult, rulesVOS, RulesVO.class, new String[]{"brulecode", "rulename"});
        return result.success(rulesVOS);
    }

    
    /**
     * @接口摘要 参加资格 已弃用
     * @业务场景 下拉查询
     * @传参类型
     * @传参列表
     * @返回列表
     */
    public Result queryQual(AppContext appContext) {
        Result result = new Result();
        Map<Integer, String> map = new HashMap<>();
        map.put(0, "所有会员");
        map.put(1, "采购订单数");
        map.put(2, "会员等级");
        map.put(3, "特定区域会员会员");
        return result.success(map);
    }


    public static int[] getLaderNo(String preLader,int size){
        if(size <= 0){
            return null;
        }
        StringBuilder sb = new StringBuilder(SELECT_LADDER_NO);
        sb.append(" where offercode like '");
        sb.append(preLader);
        sb.append("%' and cstatus & 1 = 0 ");
        List<Object[]> queryResult = baseDao.queryNative(sb.toString());
        int ladernum = Integer.parseInt(queryResult.get(0)[0].toString());
        int [] laddnumArray = new int[size];
        int length;
        int maxLength = 5;
        int fid = Integer.parseInt(preLader.substring(0,1));
        if(fid > 2){
            maxLength = 4;
        }
        for(int i = 0; i < laddnumArray.length; i++){
            ladernum = ladernum +1;
            length = (ladernum+"").length();
            sb.setLength(0);
            sb.append(preLader);
            if(length < maxLength){
                for(int j = 0;j < maxLength - length; j++){
                    sb.append("0");
                }
                sb.append(ladernum);
            }else{
                sb.append(ladernum);
            }

            laddnumArray[i] = Integer.parseInt(sb.toString());
        }
        return laddnumArray;
    }


}
