package com.onek.util.prod;

import com.google.gson.*;
import constant.DSMConst;
import dao.BaseDAO;
import elasticsearch.ElasticSearchProvider;
import util.TreeUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ProduceStore {
    private static List<ProduceEntity> PRODUCE_TREE = null;
    private static String TREE_STR = null;
    private static final String PRODUCE_SELECT =
            " SELECT * "
            + " FROM {{?" + DSMConst.D_PRODUCE_CLASS + "}} "
            + " WHERE cstatus&1 = 0 ";

    static {
        init();
    }

    private static void init() {
        List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNative(PRODUCE_SELECT);
        ProduceEntity[] produceEntities = new ProduceEntity[queryResult.size()];
        BaseDAO.getBaseDAO().convToEntity(queryResult, produceEntities, ProduceEntity.class);
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

        ProduceEntity produce;
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
    public static ProduceEntity getProduceByClassNo(String pclass) {
        ProduceEntity produceEntity = findArea(PRODUCE_TREE, pclass, 0);

        return produceEntity;
    }

    private static ProduceEntity findArea(List<ProduceEntity> areas, String pclass, int layer) {
        if (areas == null || areas.isEmpty()) {
            return null;
        }

        String indexCode = ProduceClassUtil.getCodeByLayer(pclass, layer);
        ProduceEntity result = null;
        for (ProduceEntity area : areas) {
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
