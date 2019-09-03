package com.onek.goods.util;

import com.alibaba.fastjson.JSONObject;
import com.onek.goods.entities.BgProdVO;
import com.onek.util.RedisGlobalKeys;
import constant.DSMConst;
import dao.BaseDAO;
import redis.util.RedisUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class RecreateProdDataUtil {

    private static final String QUERY_PROD_BASE =
            " SELECT spu.spu, spu.popname, spu.prodname, spu.standarno, "
                    + " spu.brandno, b.brandname, spu.manuno, m.manuname, spu.rx, "
                    + " spu.insurance, spu.gspgms, spu.gspsc, spu.detail, spu.cstatus, "
                    + " spu.qsc, spu.busscope, s.codename, "
                    + " sku.sku, sku.vatp, sku.mp, sku.rrp, sku.vaildsdate, sku.vaildedate, "
                    + " sku.prodsdate, sku.prodedate, sku.store, "
                    + " sku.limits, sku.sales, sku.wholenum, sku.medpacknum, sku.unit, "
                    + " sku.ondate, sku.ontime, sku.offdate, sku.offtime, sku.spec, sku.prodstatus, "
                    + " sku.imagestatus, sku.cstatus, sku.expmonth, sku.wp, sku.consell "
                    + " FROM ({{?" + DSMConst.TD_PROD_SPU + "}} spu "
                    + " INNER JOIN {{?" + DSMConst.TD_PROD_SKU + "}} sku ON spu.spu = sku.spu ) "
                    + " LEFT  JOIN {{?" + DSMConst.TD_PROD_MANU
                    + "}} m ON m.cstatus&1 = 0 AND m.manuno  = spu.manuno "
                    + " LEFT  JOIN {{?" + DSMConst.TD_PROD_BRAND
                    + "}} b ON b.cstatus&1 = 0 AND b.brandno = spu.brandno "
                    + " LEFT  JOIN {{?" + DSMConst.TB_SYSTEM_BUS_SCOPE
                    + "}} s ON s.cstatus&1 = 0 AND spu.busscope = s.code "
                    + " WHERE 1=1 ";

    public static void execute(){

        if("on".equals(RedisUtil.getStringProvide().get(RedisGlobalKeys.RECREATE_ES_SWITCH))){

            List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNative(QUERY_PROD_BASE);

            BgProdVO[] returnResults = new BgProdVO[queryResult.size()];

            BaseDAO.getBaseDAO().convToEntity(queryResult, returnResults, BgProdVO.class);

            for(BgProdVO vo : returnResults){
                int status = ProdESUtil.addProdDocument(vo);
                if(status < 0){
                    Logger.getAnonymousLogger().info("+++++ "+ JSONObject.toJSON(vo));
                }
            }
//            List<BgProdVO> bgProdList = new ArrayList<>();
//            bgProdList = Arrays.asList(returnResults);
//            ProdESUtil.batchAddProdDocument(bgProdList);

            RedisUtil.getStringProvide().set(RedisGlobalKeys.RECREATE_ES_SWITCH, "off");
        }
    }
}
