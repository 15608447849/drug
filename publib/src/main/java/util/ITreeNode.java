package util;

/**
 * 树形结构，用于转换为树形。
 * @author Helena Rubinstein
 *
 */

public interface ITreeNode<T> {
    /** 增加孩子 **/
    void addChild(T child);
    /** 是否为树根 **/
    boolean isRoot();
    /** 获取父级 **/
    String getParentId();
    /** 获取本ID **/
    String getSelfId();
}
