package com.mnzn.grammar;

import com.mnzn.lex.Token;
import com.mnzn.lex.TokenTag;
import com.mnzn.utils.visual.DrawableTreeNode;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

// 抽象语法树节点
//@Getter
public class ASTNode implements DrawableTreeNode {
    private final Token token; // 附带的词法单元
    private final List<ASTNode> children; // 子节点

    public ASTNode(Token token) {
        this.token = token;
        this.children = new ArrayList<>();
    }

//    public ASTNode(Token token, List<ASTNode> children) {
//        this.token = token;
//        this.children = children;
//    }

    public void addChild(ASTNode node) {
        children.add(node);
    }

    public TokenTag tokenTag() {
        return token.getTag();
    }

    // 获取子结点
    public ASTNode c(int i) {
        return children.get(i);
    }

    // 获取token
    public <T> T t(Class<T> ct) {
        return ct.cast(token);
    }

    // 获取子节点的token
    public <T> T t(int i, Class<T> ct) {
        return ct.cast(c(i).token);
    }

    // size
    public int size() {
        return children.size();
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
