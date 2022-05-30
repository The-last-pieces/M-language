package com.mnzn.utils.tree;

import com.mnzn.grammar.ASTNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.List;

public abstract class DrawableTreeNode<T extends TreeNode<T>> implements TreeNode<T> {
    @AllArgsConstructor
    @Getter
    public class ForwardEdge {
        T node;
        String desc;
    }

    // 获取结点唯一标识符
    public String getId() {
        return String.valueOf(System.identityHashCode(this));
    }

    // 获取要显示的文本组
    public abstract List<String> getLabels();

    // 获取到子节点的边的信息,desc默认为空
    public List<ForwardEdge> getEdges() {
        return getChildren().stream().map(c -> new ForwardEdge(c, "")).toList();
    }
}
