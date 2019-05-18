package com.onek.entity;

import com.onek.calculate.entity.Gift;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName ActivityGiftVO
 * @Description TODO
 * @date 2019-04-27 17:49
 */
public class ActivityGiftVO {

    private String actcode;

    private List<Gift> giftList = new ArrayList<Gift>();

    public String getActcode() {
        return actcode;
    }

    public void setActcode(String actcode) {
        this.actcode = actcode;
    }

    public List<Gift> getGiftList() {
        return giftList;
    }

    public void setGiftList(List<Gift> giftList) {
        this.giftList = giftList;
    }
}
