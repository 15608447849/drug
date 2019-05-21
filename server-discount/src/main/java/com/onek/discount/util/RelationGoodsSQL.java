package com.onek.discount.util;

import constant.DSMConst;
import dao.BaseDAO;

/**
 * @author 11842
 * @version 1.1.1
 * @description 商品关联
 * @time 2019/5/17 22:48
 **/
public class RelationGoodsSQL {
    //分类
    public static final String SELECT_ClASS_GOODS = "select classno6,classno4,classno2,sku, systore, " +
            " min(notused) as minnotused from ( select SUBSTR(sku,2,6) as classno6, SUBSTR(sku,2,4) as classno4, " +
            " SUBSTR(sku,2,2) as classno2,sku,systore, sum(used) as uused, (systore-sum(used)) as notused from ( " +
            " select sku,systore, used  from ( " +
            " SELECT sku,gcode,(store-freezestore) as systore, " +
            " ceil(IF( ua.cstatus & 256 > 0, sum( actstock ), 0 ) * 0.01 * ( store - freezestore ) + IF " +
            " ( ua.cstatus & 256 = 0, sum( actstock ), 0 ) ) AS used FROM  " +
            " (SELECT " +
            " spu,sku,gcode,store,freezestore,actstock,a.cstatus  " +
            " FROM {{?" + DSMConst.TD_PROM_ASSDRUG + "}} a " +
            " LEFT JOIN td_prod_sku s ON s.sku LIKE CONCAT( '_', a.gcode, '%' )  " +
            " WHERE a.cstatus & 1 = 0 AND length( gcode ) < 14 AND gcode > 0  " +
            " AND actcode IN ( SELECT unqid FROM td_prom_act WHERE cstatus & 1 = 0 )  and actcode<>? UNION ALL " +
            " SELECT spu, sku,gcode, store, freezestore,actstock, a.cstatus  " +
            " FROM td_prom_assdrug a LEFT JOIN td_prod_sku s ON s.sku = gcode  " +
            " WHERE a.cstatus & 1 = 0  AND length( gcode ) = 14  " +
            " AND actcode IN ( SELECT unqid FROM td_prom_act WHERE cstatus & 1 = 0 )  and actcode<>? UNION ALL " +
            " SELECT spu, sku, gcode, store, freezestore, actstock, a.cstatus  " +
            " FROM td_prom_assdrug a, td_prod_sku s  " +
            " WHERE a.cstatus & 1 = 0  AND gcode = 0  " +
            " AND actcode IN ( SELECT unqid FROM td_prom_act WHERE cstatus & 1 = 0 )  and actcode<>? " +
            " ) ua " +
            " WHERE ua.cstatus & 1 = 0  " +
            " GROUP BY sku , ua.cstatus ) ub UNION ALL  " +
            " select sku,store, 0 from td_prod_sku where cstatus&1=0) uc GROUP BY sku " +
            " ) ud ";
}
