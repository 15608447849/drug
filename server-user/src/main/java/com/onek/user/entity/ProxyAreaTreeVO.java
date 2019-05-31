package com.onek.user.entity;

import com.google.gson.annotations.SerializedName;
import com.onek.util.area.AreaUtil;
import util.ITreeNode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName ProxyAreaTreeVO
 * @Description TODO
 * @date 2019-05-29 14:33
 */
public class ProxyAreaTreeVO  implements ITreeNode<ProxyAreaTreeVO> {

    @SerializedName("id")
    private String areac;
    @SerializedName("label")
    private String arean;

    private int layer;


    private List<ProxyAreaTreeVO> children;

    public String getAreac() {
        return areac;
    }

    public void setAreac(String areac) {
        this.areac = areac;
    }

    public String getArean() {
        return arean;
    }

    public void setArean(String arean) {
        this.arean = arean;
    }

    public void setChildren(List<ProxyAreaTreeVO> children) {
        this.children = children;
    }

    public int getLayer() {
        return layer;
    }

    public void setLayer(int layer) {
        this.layer = layer;
    }

    @Override
    public void addChild(ProxyAreaTreeVO child) {
        if (children == null) {
            children = new ArrayList<>();
        }

        children.add(child);
    }

    @Override
    public boolean isRoot() {
        if(getLayer() == AreaUtil.getLayer(Long.parseLong(getAreac()))){
            return true;
        }
        return false;
    }

    @Override
    public String getParentId() {
        return AreaUtil.getParent(Long.parseLong(getAreac()))+"";
    }

    @Override
    public String getSelfId() {
        return this.areac;
    }

    @Override
    protected ProxyAreaTreeVO clone() {
        ProxyAreaTreeVO clone = new ProxyAreaTreeVO();
        clone.areac = this.areac;
        clone.arean = this.arean;
        if (this.children != null) {
            clone.children = new ArrayList<>(this.children);
        }

        return clone;
    }


}
