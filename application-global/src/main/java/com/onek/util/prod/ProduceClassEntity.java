package com.onek.util.prod;

import util.ITreeNode;

import java.util.ArrayList;
import java.util.List;

public class ProduceClassEntity implements Cloneable, ITreeNode<ProduceClassEntity> {
    private String classId;
    private String className;
    private int cstatus;
    private int sorted;

    public int getSorted() {
        return sorted;
    }

    private List<ProduceClassEntity> children;

    public ProduceClassEntity() {
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

    public List<ProduceClassEntity> getChildren() {
        return children;
    }

    @Override
    public void addChild(ProduceClassEntity child) {
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
