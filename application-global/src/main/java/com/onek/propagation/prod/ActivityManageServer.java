package com.onek.propagation.prod;

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

    public void setProd(List<String> list){
        datas = list;
        notifyObserver();
    }
}
