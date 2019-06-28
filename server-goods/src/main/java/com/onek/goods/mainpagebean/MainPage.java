package com.onek.goods.mainpagebean;

import util.GsonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: leeping
 * @Date: 2019/6/27 16:56
 */
public class MainPage {
    List<UiElement> adList ; //广告轮播图
    List<UiElement> splist ; //专区
    List<UiElement> flList ; //楼层


    public static void main(String[] args) {
        MainPage p = new MainPage();

        p.adList = new ArrayList<>();

        UiElement e = new UiElement();
            e.tabName = "轮播图1";
            e.identType =  100;
            e.tempType =  "bar";
            e.imgLink ="http://192.168.1.1/轮播图1.png";

        p.adList.add(e);

        UiElement e2 = new UiElement();
            e2.tabName = "轮播图2";
            e2.identType =  101;
            e2.tempType =  "bar";
            e2.imgLink ="http://192.168.1.1/轮播图2.png";
        p.adList.add(e2);

        p.splist = new ArrayList<>();
        UiElement r = new UiElement();
            r.tabName = "满减满赠";
            r.identType =  200;
            r.tempType =  "专区";
            r.index = 2;
        r.imgLink ="http://192.168.1.1/专区图片2.png";
        p.splist.add(r);

        UiElement r2 = new UiElement();
            r2.tabName = "品牌日";
            r2.identType =  201;
            r2.tempType =  "专区";
            r2.index = 1;
        r2.imgLink ="http://192.168.1.1/专区图片1.png";
        p.splist.add(r2);

        p.flList = new ArrayList<>();
        UiElement f = new UiElement();
            f.tabName = "新人专享";
            f.identType =  300;
            f.tempType =  "楼层";
            f.index = 1;
            f.imgLink ="http://192.168.1.1/楼层1.png";
            List<String> list = new ArrayList<>();
                list.add("sku1");
                list.add("sku2");
                list.add("sku3");
                f.attr = list;
        p.flList.add(f);


        String json = GsonUtils.javaBeanToJson(p);
        System.out.println(json);
    }

}
