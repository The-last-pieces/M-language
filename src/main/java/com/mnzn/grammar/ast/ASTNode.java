package com.mnzn.grammar.ast;

import com.mnzn.lex.Token;
import com.mnzn.utils.visual.DrawableTreeNode;

import java.util.ArrayList;
import java.util.List;

// 抽象语法树节点
public class ASTNode implements DrawableTreeNode {
    Token token; // 附带的词法单元
    List<ASTNode> children; // 子节点

    public ASTNode(Token token) {
        this.token = token;
        this.children = new ArrayList<>();
    }

    public ASTNode(Token token, List<ASTNode> children) {
        this.token = token;
        this.children = children;
    }

    public void addChild(ASTNode node) {
        children.add(node);
    }

    @Override
    public String getId() {
        return String.valueOf(System.identityHashCode(this));
    }

    @Override
    public List<String> getLabels() {
        return List.of(token.toString());
    }

    @Override
    public ForwardEdge[] getEdges() {
        return children.stream().
                map(child -> new ForwardEdge(child, "")).
                toArray(ForwardEdge[]::new);
    }
}
