package com.onek.goods.mainpagebean;

/**
 * @Author: leeping
 * @Date: 2019/6/27 16:42
 */
public class UiElement {
    public String name;//元素页面显示名 - 如: 为你精选
    public int index; //位置下标

    public String module; // 模块类型, 1.bar 2.楼层 3.专区

    public int option;//操作类型ID  100-跳转页面,读取route , 101 跳转活动页面,102跳转专区页面, 读取template ,
    // (* template含意: 一系列模板的后台标识 ,例,A , 在首页时, 可能是指 专区或者活动的列表摆放模板, 在进入详情页时, 可能是整个页面的样子 的模板)

    public int template;//模板ID

    public String route;//页面路由 ,

    public String img;//图片链接

    public long code;//活动复合码,数据表示码

    public  Object attr;//附加属性

}
