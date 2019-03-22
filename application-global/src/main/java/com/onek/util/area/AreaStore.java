package com.onek.util.area;

import com.google.gson.*;
import constant.DSMConst;
import dao.BaseDAO;
import util.TreeUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class AreaStore {
    private static List<AreaEntity> AREA_TREE = null;
    private static String TREE_STR = null;
    private static final String AREA_SELECT =
            " SELECT areac, arean, lat, lng, cstatus "
                    + " FROM {{?" + DSMConst.D_GLOBAL_AREA + "}} "
                    + " WHERE cstatus&1 = 0 ";

    static {
        init();
    }

    private static void init() {
        List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNative(AREA_SELECT);
        AreaEntity[] areaEntities = new AreaEntity[queryResult.size()];
        BaseDAO.getBaseDAO().convToEntity(queryResult, areaEntities, AreaEntity.class);
        List treeList = TreeUtil.list2Tree(Arrays.asList(areaEntities));
        AREA_TREE = Collections.unmodifiableList(treeList);

        JsonArray temp = new JsonParser()
                        .parse(new Gson().toJson(AREA_TREE))
                        .getAsJsonArray();
        handlerJson(temp);
        TREE_STR = temp.toString();
    }

    public static String getTreeJson() {
        return TREE_STR;
    }

    public static String[] getCompleteName(int areac) {
        try {
            AreaUtil.areacCheck(areac);
        } catch (Exception ex) {
            return new String[4];
        }

        String[] result = new String[AreaUtil.getLayer(areac) + 1];

        AreaEntity area;
        for (int i = 0; i < result.length; i++) {
            area = getAreaByAreac(AreaUtil.getCodeByLayer(areac, i));

            if (area != null) {
                result[i] = area.getArean();
            }
        }

        return result;
    }

    /**
     * 通过areac获取area对象
     * @param areac
     * @return
     */
    public static AreaEntity getAreaByAreac(int areac) {
        try {
            AreaUtil.areacCheck(areac);
        } catch (Exception ex) {
            return null;
        }

        AreaEntity areaEntity = findArea(AREA_TREE, areac, 0);

        return areaEntity;
    }

    private static AreaEntity findArea(List<AreaEntity> areas, int areac, int layer) {
        if (areas == null || areas.isEmpty()) {
            return null;
        }

        int indexCode = AreaUtil.getCodeByLayer(areac, layer);
        AreaEntity result = null;
        for (AreaEntity area : areas) {
            if (area.getAreac() == indexCode) {
                if (areac == area.getAreac()) {
                    result = area;
                    break;
                }

                result = findArea(area.getChildren(), areac, layer + 1);

                if (result != null) {
                    break;
                }
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

            jsonObject.remove("lat");
            jsonObject.remove("lng");
            jsonObject.remove("cstatus");
            jsonObject.add("label", jsonObject.remove("arean"));
            JsonElement c = jsonObject.remove("areac");
            jsonObject.add("value", c);
            jsonObject.add("id", c);

            JsonArray children = jsonObject.getAsJsonArray("children");

            if (children.size() == 0) {
                jsonObject.remove("children");
            } else {
                handlerJson(children);
            }
        }

    }
}
