package com.onek.util.prod;

import com.google.gson.*;
import com.onek.entitys.Result;
import constant.DSMConst;
import dao.BaseDAO;
import elasticsearch.ElasticSearchProvider;
import util.TreeUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("unchecked")
public class ProduceStore {
    private static List<ProduceClassEntity> PRODUCE_TREE = null;
    private static String TREE_STR = null;
    private static final String PRODUCE_SELECT =
            " SELECT * "
            + " FROM {{?" + DSMConst.D_PRODUCE_CLASS + "}} "
            + " WHERE cstatus&1 = 0 ";

    private static final String QUERY_PROD_BASE =
            " SELECT spu.spu, spu.popname, spu.prodname, spu.standarno, "
                    + " spu.brandno, b.brandname, spu.manuno, m.manuname, spu.rx, "
                    + " spu.insurance, spu.gspGMS, spu.gspSC, spu.detail, spu.cstatus,"
                    + " sku.sku, sku.vatp, sku.mp, sku.rrp, sku.vaildsdate, sku.vaildedate,"
                    + " sku.prodsdate, sku.prodedate, sku.store, sku.activitystore, "
                    + " sku.limits, sku.sales, sku.wholenum, sku.medpacknum, sku.unit, "
                    + " sku.ondate, sku.ontime, sku.offdate, sku.offtime, sku.spec, sku.prodstatus, "
                    + " sku.imagestatus, sku.cstatus "
                    + " FROM ({{?" + DSMConst.TD_PROD_SPU + "}} spu "
                    + " INNER JOIN {{?" + DSMConst.TD_PROD_SKU   + "}} sku ON spu.spu = sku.spu ) "
                    + " LEFT  JOIN {{?" + DSMConst.TD_PROD_MANU  + "}} m   ON m.cstatus&1 = 0 AND m.manuno  = spu.manuno "
                    + " LEFT  JOIN {{?" + DSMConst.TD_PROD_BRAND + "}} b   ON b.cstatus&1 = 0 AND b.brandno = spu.brandno "
                    + " WHERE 1=1 ";

    static {
        init();
    }

    private static void init() {
        List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNative(PRODUCE_SELECT);
        ProduceClassEntity[] produceEntities = new ProduceClassEntity[queryResult.size()];
        BaseDAO.getBaseDAO().convToEntity(queryResult, produceEntities, ProduceClassEntity.class);
        List treeList = TreeUtil.list2Tree(Arrays.asList(produceEntities));
        PRODUCE_TREE = Collections.unmodifiableList(treeList);

        JsonArray temp = new JsonParser()
                .parse(new Gson().toJson(PRODUCE_TREE))
                .getAsJsonArray();
        handlerJson(temp);
        TREE_STR = temp.toString();
    }

    public static String getTreeJson() {
        return TREE_STR;
    }

    public static String[] getCompleteName(String pclass) {
        String[] result = new String[ProduceClassUtil.getLayer(pclass) + 1];

        ProduceClassEntity produce;
        for (int i = 0; i < result.length; i++) {
            produce = getProduceByClassNo(ProduceClassUtil.getCodeByLayer(pclass, i));

            if (produce != null) {
                result[i] = produce.getClassName();
            }
        }

        return result;
    }

    /**
     * 通过pclass获取area对象
     * @param pclass
     * @return
     */
    public static ProduceClassEntity getProduceByClassNo(String pclass) {
        ProduceClassEntity ProduceClassEntity = findArea(PRODUCE_TREE, pclass, 0);

        return ProduceClassEntity;
    }

    public static ProdEntity getProdBySku(long sku){
        String sql = QUERY_PROD_BASE + " AND sku.sku = ? ";

        List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNative(sql, sku);

        if (queryResult.isEmpty()) {
           return null;
        }

        ProdEntity[] returnResults = new ProdEntity[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, returnResults, ProdEntity.class);

        return returnResults[0];
    }

    public static String getProduceName(String pclass) {
        ProduceClassEntity ProduceClassEntity = findArea(PRODUCE_TREE, pclass, 0);

        return ProduceClassEntity == null ? "" : ProduceClassEntity.getClassName();
    }

    private static ProduceClassEntity findArea(List<ProduceClassEntity> areas, String pclass, int layer) {
        if (areas == null || areas.isEmpty()) {
            return null;
        }

        String indexCode = ProduceClassUtil.getCodeByLayer(pclass, layer);
        ProduceClassEntity result = null;
        for (ProduceClassEntity area : areas) {
            if (!indexCode.equals(area.getClassId())) {
                continue;
            }

            if (pclass.equals(area.getClassId())) {
                result = area;
                break;
            }

            result = findArea(area.getChildren(), pclass, layer + 1);

            if (result != null) {
                break;
            }
        }

        return result;
    }

    private static void handlerJson(JsonElement element) {
        if (element == null) {
            return;
        }

        if (element.isJsonArray()) {
            JsonArray jsonArray = element.getAsJsonArray();
            Iterator<JsonElement> it = jsonArray.iterator();
            while (it.hasNext()) {
                handlerJson(it.next());
            }
        } else if (element.isJsonObject()) {
            JsonObject jsonObject = element.getAsJsonObject();

            jsonObject.remove("cstatus");
            jsonObject.add("value", jsonObject.remove("classId"));
            jsonObject.add("label", jsonObject.remove("className"));

            JsonArray children = jsonObject.getAsJsonArray("children");

            if (children.size() == 0) {
                jsonObject.remove("children");
            } else {
                handlerJson(children);
            }
        }

    }
    
    

    public static int addProduce(){
//        ElasticSearchProvider.addDocument();
        return 0;
    }

}
