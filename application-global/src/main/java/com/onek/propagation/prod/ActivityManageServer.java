package com.onek.propagation.prod;

import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ActivityManageServer implements ProdObserverable {
    private List<ProdObserver> list;
    private List<String> datas;

    public ActivityManageServer(){
        list = new ArrayList<>();
    }

    @Override
    public void registerObserver(ProdObserver o) {
        list.add(o);
    }

    @Override
    public void removeObserver(ProdObserver o) {
        if(!list.isEmpty())
            list.remove(o);
    }

    @Override
    public void notifyObserver() {
        for(int i = 0; i < list.size(); i++) {
            ProdObserver oserver = list.get(i);
            oserver.update(datas);
        }
    }

    /*
   * 设置产品列表
    **/
    public void setProd(List<String> list){
        datas = list;
        notifyObserver();
    }

    /*
     * 活动修改
     **/
    public void actUpdate(long actcode){
        List<String> list = new ArrayList<>();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("actcode" ,actcode);
        list.add(jsonObject.toJSONString());
        datas = list;
        notifyObserver();
    }
}
