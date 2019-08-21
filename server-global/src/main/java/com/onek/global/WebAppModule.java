package com.onek.global;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.global.area.AreaStore;
import com.onek.util.area.AreaEntity;
import com.onek.util.FileServerUtils;
import util.http.HttpRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * @接口摘要 app获取地区层级信息
     * @业务场景 app地图选择时
     * @传参类型 array
     * @传参列表 [地区码]
     * @返回列表 {value:地区码,label:地区名,type:地区类型,letter=地区首字母大写}
     */
    @UserPermission(ignore = true)
    public Result appAllArea(AppContext context){
        // 获取全部区
        AreaEntity[] array = AreaStore.getAllArea();

        List<AreaEntityByApp> list = new ArrayList<>();

        //处理
        for (AreaEntity areaEntity : array){
            list.add(new AreaEntityByApp(areaEntity.getAreac(), areaEntity.getArean(),1) );
        }
        return new Result().success(getAllArea(list));
    }

    private static String getAllArea( List<AreaEntityByApp> list){
        String reStr = "";

        //省份匹配
        String sf = "[0-9]{2}[0]{10}";
        Pattern sfp = Pattern.compile(sf);
        //
        String city = "[0-9]{4}[0]{8}";
        Pattern cityp = Pattern.compile(city);


        String qx = "[0-9]{6}[0]{6}";
        Pattern qxp = Pattern.compile(qx);

        List zss = new ArrayList<>(Arrays.asList(new String[]{"110000000000", "120000000000", "310000000000", "500000000000"}));
        System.out.println(zss.size());
        JSONArray array = new JSONArray();



//        JsonParser jsonParser = new JsonParser();
//        JsonArray json = jsonParser.parse(str).getAsJsonArray();
        for (AreaEntityByApp areaEntityByApp : list) {
            String value = String.valueOf(areaEntityByApp.value);
            String label = areaEntityByApp.label;
            //判断省
            Matcher sfm = sfp.matcher(value);
            if (sfm.find()) {
                JSONObject sFobj = new JSONObject();
                JSONArray sArr = new JSONArray();
                sFobj.put("value", value);
                sFobj.put("text", label);
                array.add(sFobj);

                if(zss.contains(value)){
                    for (AreaEntityByApp areaEntityByApp4 : list) {
                        String value4 = String.valueOf(areaEntityByApp4.value);
                        String label4 = areaEntityByApp4.label;
                        if("500200000000".equals(value4)){
                            break;
                        }
                        if (value4.startsWith(value.substring(0,2))) {
                            Matcher citym = cityp.matcher(value4);
                            if (citym.find()) {
                                JSONObject Sobj = new JSONObject();
                                JSONArray qxArr = new JSONArray();
                                Sobj.put("value", value4);
                                Sobj.put("text", label4);
                                sArr.add(Sobj);
                                //判断区县
                                for (AreaEntityByApp areaEntityByApp5 : list) {
                                    String value5 = String.valueOf(areaEntityByApp5.value);
                                    String label5 = areaEntityByApp5.label;
                                    if("500200000000".equals(value5)){
                                        break;
                                    }
                                    if (value4.equals(value5)) {
                                        continue;
                                    }
                                    if (value5.startsWith(value4.substring(0,2))) {

                                        //System.out.println(value1.substring(0,4) +"  "+value2.substring(0,4));
                                        //System.out.println(value2.substring(0,4));
                                        //判断区
                                        Matcher qxm = qxp.matcher(value5);
                                        if (qxm.find()) {
                                            JSONObject qxobj = new JSONObject();
                                            qxobj.put("value", value5);
                                            qxobj.put("text", label5);
                                            qxArr.add(qxobj);
                                        }
                                    }
                                }
                                Sobj.put("children", qxArr);
                            }
                        }
                        sFobj.put("children", sArr);
                    }
                }


                for (AreaEntityByApp areaEntityByApp1 : list) {
                    String value1 = String.valueOf(areaEntityByApp1.value);
                    String label1 = areaEntityByApp1.label;

                    if (value1.equals(value)) {
                        continue;
                    }

                    if (value1.startsWith(value.substring(0,2)) && !zss.contains(value1)) {
                        //判断市
                        Matcher citym = cityp.matcher(value1);
                        if (citym.find()) {
                            JSONObject Sobj = new JSONObject();
                            JSONArray qxArr = new JSONArray();
                            Sobj.put("value", value1);
                            Sobj.put("text", label1);
                            sArr.add(Sobj);


                            //判断区县
                            for (AreaEntityByApp areaEntityByApp2 : list) {
                                String value2 = String.valueOf(areaEntityByApp2.value);
                                String label2 = areaEntityByApp2.label;
                                if (value2.equals(value1)) {
                                    continue;
                                }
                                if (value2.startsWith(value1.substring(0,4))) {

                                    //System.out.println(value1.substring(0,4) +"  "+value2.substring(0,4));
                                    //System.out.println(value2.substring(0,4));
                                    //判断区
                                    Matcher qxm = qxp.matcher(value2);
                                    if (qxm.find()) {
                                        JSONObject qxobj = new JSONObject();
                                        qxobj.put("value", value2);
                                        qxobj.put("text", label2);
                                        qxArr.add(qxobj);
                                    }
                                }
                            }
                            Sobj.put("children", qxArr);
                        }
                    }
                    sFobj.put("children", sArr);
                }
            }
        }
        if(!array.toString().isEmpty()){
            reStr = array.toString();
        }
        return reStr;
    }

    /***
     * @接口摘要 客服人员qq号码
     * @业务场景
     * @传参类型 JSON/ARRAY
     * @传参列表
     * @返回列表
     */
    @UserPermission(ignore = true)
    public Result customerInfo(AppContext context){
        return new Result().success( new HttpRequest().accessUrl(FileServerUtils.fileDownloadPrev()+"/customer.json").getRespondContent());
    }





}
