package com.onek.global;

import com.alibaba.fastjson.JSONObject;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.global.area.AreaStore;
import com.onek.util.area.AreaEntity;
import com.onek.util.area.AreaUtil;
import com.onek.util.dict.DictStore;
import com.onek.global.produce.ProduceStore;
import com.onek.util.prod.ProdEntity;
import util.GsonUtils;
import util.StringUtils;

/**
 * @Author: leeping
 * @Date: 2019/4/10 19:24
 */
public class CommonModule {

    @UserPermission(ignore = true)
    // 获取子类
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

    @UserPermission(ignore = true)
    public Result getArea(AppContext appContext) {
        String[] array = appContext.param.arrays;

        if (array == null || array.length == 0) {
            return new Result().fail("参数为空");
        }

        if (!StringUtils.isBiggerZero(array[0])) {
            return new Result().fail("参数错误");
        }

        return new Result().success(Long.parseLong(array[0]));
    }

    private AreaEntity getArea(long areac) {
        return AreaStore.getAreaByAreac(areac);
    }

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
        JSONObject j = DictStore.getAllDict();
        return new Result().success(j.toJSONString());
    }

    @UserPermission(ignore = true)
    public Result getProduceClasses(AppContext appContext) {
        return new Result().success(ProduceStore.getTreeJson());
    }

    @UserPermission(ignore = true)
    public Result getProduceName(AppContext appContext){
        return new Result().success(ProduceStore.getProduceName(appContext.param.arrays[0]));
    }

    @UserPermission(ignore = true)
    public Result getProdBySku(AppContext appContext){
        ProdEntity entity = ProduceStore.getProdBySku(Long.parseLong(appContext.param.arrays[0]));
        return new Result().success(GsonUtils.javaBeanToJson(entity));
    }
    @UserPermission(ignore = true)
    public Result getCompleteName(AppContext appContext){
        long areaCode = Integer.parseInt(appContext.param.arrays[0]);
        String[] areas = AreaStore.getCompleteName(areaCode);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i<areas.length ;i++){
            sb.append(areas[i]);
        }
        return new Result().success(sb.toString());
    }

}
