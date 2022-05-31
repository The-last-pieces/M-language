package com.mnzn.grammar;

import com.mnzn.lex.Token;
import com.mnzn.lex.TokenTag;
import com.mnzn.utils.tree.DrawableTreeNode;

import java.util.ArrayList;
import java.util.List;

// 抽象语法树节点
//@Getter
public class ASTNode extends DrawableTreeNode<ASTNode> {
    private final ProduceTag produce;   // 非终结符结点
    private final Token terminal;       // 终结符结点
    private final List<ASTNode> children; // 子节点

    public ASTNode(Token terminal) {
        this.terminal = terminal;
        this.produce = null;
        this.children = new ArrayList<>();
    }

    public ASTNode(ProduceTag produce) {
        this.produce = produce;
        this.terminal = null;
        this.children = new ArrayList<>();
    }

    // 是否为叶节点(即终结符节点)
    public boolean isLeaf() {
        return terminal != null;
    }

    // 获取子结点
    public ASTNode c(int i) {
        return children.get(i);
    }

    // 获取tag
    public TokenTag tag() {
        assert terminal != null;
        return terminal.getTag();
    }

    public TokenTag tag(int i) {
        return c(i).tag();
    }

    // 获取token
    public Token t() {
        return t(Token.class);
    }

    public <T> T t(Class<T> ct) {
        return ct.cast(terminal);
    }

    // 获取子节点的token
    public <T> T t(int i, Class<T> ct) {
        return ct.cast(c(i).terminal);
    }

    // 获取产生式
    public ProduceTag p() {
        return produce;
    }

    // 获取子节点的产生式
    public ProduceTag p(int i) {
        return c(i).produce;
    }

    // size
    public int size() {
        return children.size();
    }

    @Override
    public List<ASTNode> getChildren() {
        return children.stream().toList();
    }

    @Override
    public void addChild(ASTNode child) {
        children.add(child);
    }

    @Override
    public List<String> getLabels() {
        return List.of(this.toString());
    }

    @Override
    public String toString() {
        if (terminal != null) {
            return terminal.toString();
        } else if (produce != null) {
            return produce.toString();
        } else {
            throw new RuntimeException("ASTNode is null");
        }
    }
}
