package com.mnzn.utils.tree;

import java.util.List;

// 树形结点接口
public interface TreeNode<T extends TreeNode<T>> {
    List<T> getChildren();

    void addChild(T child);
}
