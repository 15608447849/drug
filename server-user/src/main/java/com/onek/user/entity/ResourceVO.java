package com.onek.user.entity;

import com.google.gson.annotations.SerializedName;
import util.ITreeNode;

import java.util.ArrayList;
import java.util.List;

public final class ResourceVO implements ITreeNode<ResourceVO> {
    @SerializedName("id")
    private String resourceId;
    @SerializedName("label")
    private String resourceName;
    private long roleId;
    private int cstatus;
    private List<ResourceVO> children;

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public long getRoleId() {
        return roleId;
    }

    public void setRoleId(long roleId) {
        this.roleId = roleId;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }

    public List<ResourceVO> getChildren() {
        return children;
    }

    public void setChildren(List<ResourceVO> children) {
        this.children = children;
    }

    @Override
    public void addChild(ResourceVO child) {
        if (children == null) {
            children = new ArrayList<>();
        }

        children.add(child);
    }

    @Override
    public boolean isRoot() {
        return this.resourceId.length() == 2;
    }

    @Override
    public String getParentId() {
        long resourceNum = Long.parseLong(this.resourceId);
        return Long.toString(resourceNum / 100);
    }

    @Override
    public String getSelfId() {
        return this.resourceId;
    }

    @Override
    protected ResourceVO clone() {
        ResourceVO clone = new ResourceVO();
        clone.cstatus = this.cstatus;
        clone.resourceId = this.resourceId;
        clone.resourceName = this.resourceName;
        clone.roleId = this.roleId;
        if (this.children != null) {
            clone.children = new ArrayList<>(this.children);
        }

        return clone;
    }
}
