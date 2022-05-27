package com.mnzn.utils.visual;

import java.util.List;

// 可绘制的树形结点接口
public interface DrawableTreeNode {
    record ForwardEdge(DrawableTreeNode node, String desc) {
    }

    // 获取结点唯一标识符
    String getId();

    // 获取要显示的文本组
    List<String> getLabels();

    // 获取到子节点的边
    ForwardEdge[] getEdges();
}
