package com.onek.global;

import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.global.area.AreaStore;
import com.onek.util.area.AreaEntity;
import com.onek.util.FileServerUtils;
import util.http.HttpRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static util.StringUtils.converterToFirstSpell;

/**
 * @服务名 globalServer
 * @Author: leeping
 * @Date: 2019/5/17 16:46
 */
public class WebAppModule {

    private static class Bean{
        public Bean(String container, String fragment, String name) {
            this.container = container;
            this.fragment = fragment;
            this.name = name;
        }
        String container;
        String fragment;
        String name;
        HashMap<String,String> map;
    }

    @UserPermission(ignore = true)
    public String config(AppContext context){
        return new HttpRequest().accessUrl(FileServerUtils.fileDownloadPrev()+"/config.json").getRespondContent();
    }


    /** APP 地区数据对象*/
    private static class AreaEntityByApp {
        //地区码  : 110111000000
        long value;
        //地区名  : 房山区
        String label;
        int type = 1;
        String letter;

        public AreaEntityByApp(long value, String label,int tyep) {
            this.value = value;
            this.label = label;
            this.type = tyep;
            this.letter = converterToFirstSpell(label);
        }
    }


    /**
     * @接口摘要 app获取地区层级信息
     * @业务场景 app地图选择时
     * @传参类型 array
     * @传参列表 [地区码]
     * @返回列表 {value:地区码,label:地区名,type:地区类型,letter=地区首字母大写}
     */
    @UserPermission(ignore = true)
    public List<AreaEntityByApp> appAreaAll(AppContext context){
        long areaCode = 0;
        if (context.param.arrays!=null && context.param.arrays.length>0){
            areaCode = Long.parseLong(context.param.arrays[0]);
        }

        AreaEntity[] array;
        if (areaCode == 0){
            // 获取全部区
            array = AreaStore.getAllCities();
        }else{
            // 获取区下面的子集
            array =  AreaStore.getChildren(areaCode);
        }
        List<AreaEntityByApp> list = new ArrayList<>();

        //处理
        for (AreaEntity areaEntity : array){
            list.add(new AreaEntityByApp(areaEntity.getAreac(), areaEntity.getArean(), areaCode == 0 ? 1 : 2 ) );
        }

        return list;
    }








}
