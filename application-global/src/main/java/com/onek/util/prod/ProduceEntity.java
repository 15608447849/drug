package com.onek.util.prod;

import util.ITreeNode;

import java.util.ArrayList;
import java.util.List;

public class ProduceEntity implements Cloneable, ITreeNode<ProduceEntity> {
    private String classId;
    private String className;
    private int cstatus;

    private List<ProduceEntity> children;

    public ProduceEntity() {
        children = new ArrayList<>();
    }

    public String getClassId() {
        return classId;
    }

    public String getClassName() {
        return className;
    }

    public int getCstatus() {
        return cstatus;
    }

    public List<ProduceEntity> getChildren() {
        return children;
    }

    @Override
    public void addChild(ProduceEntity child) {
        children.add(child);
    }

    @Override
    public boolean isRoot() {
        return ProduceClassUtil.isRoot(this.classId);
    }

    @Override
    public String getParentId() {
        return ProduceClassUtil.getParent(this.classId);
    }

    @Override
    public String getSelfId() {
        return this.classId;
    }
}
