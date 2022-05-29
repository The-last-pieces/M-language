package com.mnzn.utils.visual.paint;

import com.mnzn.utils.visual.DrawableTreeNode;
import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.Node;
import org.apache.commons.text.StringEscapeUtils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static guru.nidi.graphviz.model.Factory.*;

public class PaintUnits {
    private static Node makeNode(DrawableTreeNode n) {
        // 附带要显示的所有属性
        Attributes<ForNode> rec = Records.of(n.getLabels().stream().map(StringEscapeUtils::escapeHtml3).toArray(String[]::new));
        return node(n.getId()).with(rec);
    }

    // 打印树状图
    public static void paintTree(DrawableTreeNode root, String outputPath) {
        Graph g = graph().directed().
                graphAttr().with(Rank.dir(Rank.RankDir.TOP_TO_BOTTOM)).
                with(makeNode(root));
        // bfs
        List<DrawableTreeNode> queue = new LinkedList<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            DrawableTreeNode cur = queue.remove(0);
            Node n = makeNode(cur);
            for (DrawableTreeNode.ForwardEdge child : cur.getEdges()) {
                // 创建节点
                Node c = makeNode(child.node());
                // 创建边
                Link l = to(c).with(Label.of(child.desc()));
                // 添加边,添加子节点到bfs队列中
                g = g.with(n.link(l));
                queue.add(child.node());
            }
        }
        try {
            Graphviz.fromGraph(g).width(1000).render(Format.PNG).toFile(new File(outputPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
