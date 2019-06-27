package com.onek.global;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.onek.annotation.UserPermission;
import com.onek.consts.CSTATUS;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.global.area.AreaStore;
import com.onek.global.produce.ProduceStore;
import com.onek.util.area.AreaEntity;
import com.onek.util.area.AreaUtil;
import com.onek.util.dict.DictEntity;
import com.onek.util.prod.ProdEntity;
import constant.DSMConst;
import dao.BaseDAO;
import util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: leeping
 * @Date: 2019/4/10 19:24
 */
public class CommonModule {

    private static BaseDAO baseDao = BaseDAO.getBaseDAO();

    /**
     * @接口摘要 获取子集
     * @业务场景 三级联动获取信息
     * @传参类型 Array
     * @传参列表 [父级地区码]
     * @返回列表 code=200 data=结果信息
     */

    @UserPermission(ignore = true)
    public Result getChildren(AppContext appContext) {
        String[] array = appContext.param.arrays;
        if (array == null || array.length == 0) {
            return new Result().fail("参数为空");
        }
        if (!StringUtils.isInteger(array[0])) {
            return new Result().fail("参数错误");
        }
        return new Result().success(AreaStore.getChildren(Long.parseLong(array[0])));
    }

    /**
     * @接口摘要 获取地区信息
     * @业务场景 获取地区信息
     * @传参类型 Array
     * @传参列表 [地区码]
     * @返回列表 code=200 data=结果信息
     */
    @UserPermission(ignore = true)
    public Result getArea(AppContext appContext) {
        String[] array = appContext.param.arrays;
        if (array == null || array.length == 0) {
            return new Result().fail("参数为空");
        }
        if (!StringUtils.isBiggerZero(array[0])) {
            return new Result().fail("参数错误");
        }
        return new Result().success(AreaStore.getAreaByAreac(Long.parseLong(array[0])));
    }

    /**
     * @接口摘要 获取地区名
     * @业务场景 获取地区名
     * @传参类型 Array
     * @传参列表 [地区码]
     * @返回列表 code=200 data=结果信息
     */
    @UserPermission(ignore = true)
    public Result getAreaName(AppContext appContext) {
        Result r = getArea(appContext);
        if (r.code != 200) {
            return r;
        }
        if (r.data == null) {
            return new Result().success("");
        }
        AreaEntity ae = (AreaEntity) r.data;
        return new Result().success(ae.getArean());
    }

    private AreaEntity getArea(long areac) {
        return AreaStore.getAreaByAreac(areac);
    }


    /**
     * @接口摘要 获取祖先地区
     * @业务场景 获取祖先地区
     * @传参类型 Array
     * @传参列表 [地区码]
     * @返回列表 code=200 data=结果信息
     */

    @UserPermission(ignore = true)
    public Result getAncestors(AppContext appContext) {
        String[] array = appContext.param.arrays;

        if (array == null || array.length == 0) {
            return new Result().fail("参数为空");
        }

        if (!StringUtils.isBiggerZero(array[0])) {
            return new Result().fail("参数错误");
        }

        AreaEntity areaBase = AreaStore.getAreaByAreac(Long.parseLong(array[0]));

        if (areaBase == null) {
            return new Result().success(null);
        }

        long[] ancestorCodes = AreaUtil.getAllAncestorCodes(areaBase.getAreac());

        AreaEntity[] areaEntities = new AreaEntity[ancestorCodes.length];

        for (int i = 0; i < ancestorCodes.length; i++) {
            areaEntities[i] = getArea(ancestorCodes[i]);
        }

        return new Result().success(areaEntities);
    }

    @UserPermission(ignore = true)
    public Result getDicts(AppContext appContext){
        JSONObject j = getAllDict();
        return new Result().success(j.toJSONString());
    }

    /**
     * @接口摘要 获取类别树
     * @业务场景 类别展示
     * @传参类型 -
     * @传参列表 -
     * @返回列表 code=200 data=结果信息
     */
    @UserPermission(ignore = true)
    public Result getProduceClasses(AppContext appContext) {
        return new Result().success(ProduceStore.getTreeJson());
    }

    /**
     * @接口摘要 获取类别名
     * @业务场景 获取类别名
     * @传参类型 Array
     * @传参列表 [类别码]
     * @返回列表 code=200 data=结果信息
     */
    @UserPermission(ignore = true)
    public String getProduceName(AppContext appContext){
        return ProduceStore.getProduceName(appContext.param.arrays[0]);
    }

    /**
     * @接口摘要 获取产品信息
     * @业务场景 获取产品信息
     * @传参类型 Array
     * @传参列表 [sku]
     * @返回列表 code=200 data=结果信息
     */
    @UserPermission(ignore = true)
    public ProdEntity getProdBySku(AppContext appContext){
        ProdEntity entity = ProduceStore.getProdBySku(Long.parseLong(appContext.param.arrays[0]));
        return entity;
    }

    /**
     * @接口摘要 获取完整名
     * @业务场景 获取完整名
     * @传参类型 Array
     * @传参列表 [地区码]
     * @返回列表 code=200 data=结果信息
     */
    @UserPermission(ignore = true)
    public String getCompleteName(AppContext appContext){
        long areaCode = Long.parseLong(appContext.param.arrays[0]);
        String[] areas = AreaStore.getCompleteName(areaCode);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i<areas.length ;i++){
            sb.append(areas[i]);
        }
        return sb.toString();
    }

    /**
     * @接口摘要 获取城市树
     * @业务场景 获取城市树
     * @传参类型 -
     * @传参列表 -
     * @返回列表 code=200 data=结果信息
     */
    @UserPermission(ignore = true)
    public Result getAllCities(AppContext appContext){
        return new Result().success(AreaStore.getAllCities());
    }

    public  static JSONObject getAllDict(){
        JSONObject j = new JSONObject();
        List<Object[]> result = baseDao.queryNative("select * from {{?"+ DSMConst.TB_GLOBAL_DICT +"}} where cstatus&1= 0");
        DictEntity[] dicts = new DictEntity[result.size()];
        baseDao.convToEntity(result, dicts, DictEntity.class);
        if(dicts != null && dicts.length > 0){
            Map<String, List<DictEntity>> dictMap = new HashMap<>();
            for(DictEntity dictVo : dicts){
                List<DictEntity> subList = dictMap.get(dictVo.getType());
                if(subList == null) subList = new ArrayList<>();
                if((dictVo.getCstatus() & CSTATUS.DELETE) > 0){
                    continue;
                }
                subList.add(dictVo);
                dictMap.put(dictVo.getType(), subList);
            }

            for (String type : dictMap.keySet()){
                JSONArray dictArr = new JSONArray();
                List<DictEntity> subList = dictMap.get(type);
                for(DictEntity dictVo : subList){
                    JSONObject obj = new JSONObject();
                    obj.put("value", dictVo.getDictc());
                    obj.put("text", dictVo.getText());
                    obj.put("label", dictVo.getText());
                    obj.put("remark", dictVo.getText());
                    obj.put("customc", dictVo.getCustomc());
                    dictArr.add(obj);
                }
                j.put(type, dictArr);
            }
        }
        return j;
    }

}
