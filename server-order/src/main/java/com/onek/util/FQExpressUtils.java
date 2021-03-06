package com.onek.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.onek.express.BaseFQExpress;
import com.onek.express.RouteServiceFQExpress;
import com.onek.prop.AppProperties;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.hyrdpf.util.LogUtil;
import util.StringUtils;
import util.XMLUtils;
import util.http.HttpRequestUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FQExpressUtils {
    public enum RESULT_TYPE {
        XML,JSON
    }

    private static final List<BaseFQExpress> STORE;

    static {
        STORE = new ArrayList<>();
        STORE.add(new RouteServiceFQExpress());
    }

    public static String getTravingCode(String orderno) {
        if (!StringUtils.isBiggerZero(orderno)) {
            return "";
        }

        try {
            JSONObject params = new JSONObject();
            params.put("orderno", orderno);

            String result = HttpRequestUtil.postJson(
                    AppProperties.INSTANCE.erpUrlPrev + "/getLogisticsNo",
                    params.toJSONString());

            LogUtil.getDefaultLogger().info("The getLogisticsNo interface return value : " + result);

            if (!StringUtils.isEmpty(result)) {
                JSONObject jo = JSONObject.parseObject(result);

                if (jo.containsKey("code") && jo.getInteger("code") == 200) {
                    return jo.getString("loginsno");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    public static String getRouteInfo(String code) {
        return getRouteInfo(code, RESULT_TYPE.XML);
    }

    public static void main(String[] args) {
        System.out.println(getRouteInfo("SF1011099703744,SF2000932309557,SF2000932309566,SF2000932309575"));
//        System.out.println(getRouteInfo(getTravingCode("1908230000061301")));
    }

    public static String getRouteInfo(String code, RESULT_TYPE type) {
        if (StringUtils.isEmpty(code)) {
            return "";
        }

        String result;
        try {
            result = STORE.get(0).getResult(code);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

        switch (type) {
            case XML:
                break;
            case JSON:
                result = elementToJSONObject(XMLUtils.loadXML(result).getRootElement()).toJSONString();
                break;
        }

        return result;
    }

    private static JSONObject elementToJSONObject(Element node) {
        JSONObject result = new JSONObject();
        // 当前节点的名称、文本内容和属性
        List<Attribute> listAttr = node.attributes();// 当前节点的所有属性的list
        for (Attribute attr : listAttr) {// 遍历当前节点的所有属性
            result.put(attr.getName(), attr.getValue());
        }
        // 递归遍历当前节点所有的子节点
        List<Element> listElement = node.elements();// 所有一级子节点的list
        if (!listElement.isEmpty()) {
            for (Element e : listElement) {// 遍历所有一级子节点
                if (e.attributes().isEmpty() && e.elements().isEmpty()) // 判断一级节点是否有属性和子节点
                    result.put(e.getName(), e.getTextTrim());// 沒有则将当前节点作为上级节点的属性对待
                else {
                    if (!result.containsKey(e.getName())) // 判断父节点是否存在该一级节点名称的属性
                        result.put(e.getName(), new JSONArray());// 没有则创建
                    ((JSONArray) result.get(e.getName())).add(elementToJSONObject(e));// 将该一级节点放入该节点名称的属性对应的值中
                }
            }
        }

        return result;
    }


}
