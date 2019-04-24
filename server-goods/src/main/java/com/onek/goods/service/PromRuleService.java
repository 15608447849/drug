package com.onek.goods.service;

import com.onek.util.discount.DiscountRuleStore;
import constant.DSMConst;
import dao.BaseDAO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PromRuleService {

    private static final String QUERY_ALL_PROD = "select sku from {{?" + DSMConst.TD_PROD_SKU +"}} where cstatus&1=0";

    private static final String QUERY_ALL_PROD_BRULE = "select brulecode from {{?" + DSMConst.TD_PROM_ACT +"}} a, {{?"+ DSMConst.TD_PROM_ASSDRUG +"}} d where a.unqid = d.actcode and d.cstatus&1 = 0 " +
            "and gcode = 0 and a.sdate <= CURRENT_DATE and CURRENT_DATE<= a.edate";

    private static final String QUERY_PRECISE_PROD_BRULE  = "select gcode,brulecode from ( " +
            "select (select sku from td_prod_sku where cstatus&1=0 and spu like CONCAT('_', d.gcode,'%')) gcode,a.brulecode from {{?" + DSMConst.TD_PROM_ACT +"}} a, {{?"+ DSMConst.TD_PROM_ASSDRUG +"}} d where a.unqid = d.actcode  " +
            "and d.cstatus &1 =0  " +
            "and a.sdate <= CURRENT_DATE and CURRENT_DATE<= a.edate and length(d.gcode) < 14 and d.gcode !=0 " +
            "union all " +
            "select gcode,a.brulecode from {{?" + DSMConst.TD_PROM_ACT +"}} a, {{?"+ DSMConst.TD_PROM_ASSDRUG +"}} d where a.unqid = d.actcode " +
            "and d.cstatus &1 =0  and length(d.gcode) >= 14 and a.sdate <= CURRENT_DATE and CURRENT_DATE<= a.edate " +
            ") tab where tab.gcode is not null group by gcode,brulecode";


    private static final BaseDAO BASE_DAO = BaseDAO.getBaseDAO();

    public Map<Long, Integer> getProdRule(){
        List<Object[]> allBruleList = BASE_DAO.queryNative(QUERY_ALL_PROD_BRULE, new Object[]{});
        Map<Long, Integer> prodMap = new HashMap<>();
        if(allBruleList != null && allBruleList.size() > 0){
            List<Object[]> allProdList = BASE_DAO.queryNative(QUERY_ALL_PROD, new Object[]{});
            for(Object[] objects : allProdList){
                Long sku = Long.parseLong(objects[0].toString());
                prodMap.put(sku, 0);
            }
            for(Object[] objects : allBruleList){
                int brule = Integer.parseInt(objects[0].toString());
                int rule = DiscountRuleStore.getRuleByBRule(brule);
                for(Long sku : prodMap.keySet()){
                    int val = rule | prodMap.get(sku);
                    prodMap.put(sku, val);
                }
            }
        }

        List<Object[]> preciseBruleList = BASE_DAO.queryNative(QUERY_PRECISE_PROD_BRULE, new Object[]{});
        if(preciseBruleList != null && preciseBruleList.size() > 0){
            for(Object[] objects : preciseBruleList){
                Long sku = Long.parseLong(objects[0].toString());
                int brule = Integer.parseInt(objects[1].toString());
                int rule = DiscountRuleStore.getRuleByBRule(brule);
                if(!prodMap.containsKey(sku)){
                    prodMap.put(sku, rule);
                }else{
                    prodMap.put(sku, (rule | prodMap.get(sku)));
                }
            }
        }
        return prodMap;
    }
}
