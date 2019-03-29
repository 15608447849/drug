package com.onek.util.area;

import util.ITreeNode;

import java.util.ArrayList;
import java.util.List;

public class AreaEntity implements Cloneable, ITreeNode<AreaEntity> {
    private int areac;
    private String arean;
    private double lat;
    private double lng;
    private int cstatus;

    private List<AreaEntity> children;

    public AreaEntity() {
        children = new ArrayList<>();
    }

    public int getAreac() {
        return areac;
    }

    public String getArean() {
        return arean;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public int getCstatus() {
        return cstatus;
    }

    public List<AreaEntity> getChildren() {
        return children;
    }

    @Override
    public void addChild(AreaEntity child) {
        children.add(child);
    }

    @Override
    public boolean isRoot() {
        return AreaUtil.isRoot(this.areac);
    }

    @Override
    public String getParentId() {
        return AreaUtil.getParent(this.areac) + "";
    }

    @Override
    public String getSelfId() {
        return this.areac + "";
    }

    @Override
    public String toString() {
        return "AreaEntity{" +
                "areac=" + areac +
                ", arean='" + arean + '\'' +
                ", lat=" + lat +
                ", lng=" + lng +
                ", cstatus=" + cstatus +
                ", children=" + children +
                '}';
    }

    @Override
    public Object clone() {
        AreaEntity clone = new AreaEntity();

        clone.arean = this.arean;
        clone.areac = this.areac;
        clone.cstatus = this.cstatus;
        clone.lat = this.lat;
        clone.lng = this.lng;
        clone.children = new ArrayList<>(this.children);

        return clone;
    }
}
