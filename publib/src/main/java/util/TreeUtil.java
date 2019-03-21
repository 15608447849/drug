package util;

import java.util.ArrayList;
import java.util.List;

/**
 * 树结构工具
 */

public class TreeUtil {

    public static List<? extends ITreeNode> list2Tree(List<? extends ITreeNode> list) {
        List<ITreeNode> tree = new ArrayList<>();

        for (ITreeNode node : list) {
            // 找到根节点
            if (node.isRoot()) {
                tree.add(node);
            }

            // 赋予孩子节点
            for (ITreeNode node2 : list) {
                if (node.getSelfId().equals(node2.getParentId())) {
                    node.addChild(node2);
                }
            }
        }

        return tree;
    }
}
