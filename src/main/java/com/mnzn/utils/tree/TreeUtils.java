package com.mnzn.utils.tree;

import com.mnzn.utils.sys.SystemUtils;
import com.mnzn.utils.visual.console.PrintUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.function.Function;

// 树的工具类
public class TreeUtils {
    // 左括号,右括号,子节点分隔符
    public record TreeStringDetermine(Character left, Character right, Character determine) {
    }

    private static final TreeStringDetermine defaultConfig = new TreeStringDetermine('[', ']', ',');

    // 从字符串中构建树,无视空格, "[]," 为保留字符串,不可转义
    public static <T extends TreeNode<T>> T buildTree(String str, Function<String, T> make, TreeStringDetermine deter) {
        // 去除空字符
        str = str.replaceAll("\\s", "");
        int n = str.length();
        // 维护一个待添加子节点 的 节点 的栈
        Stack<T> stack = new Stack<>();
        // 用来构造子节点
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < n; i++) {
            char c = str.charAt(i);
            if (c == deter.left) {
                // 进入子节点的状态
                if (sb.isEmpty()) throw new IllegalArgumentException("'['前面缺少父节点");
                // 构造父节点
                T node = make.apply(sb.toString());
                // 如果现在有别的节点,则添加到其子节点中
                if (!stack.isEmpty()) {
                    stack.peek().addChild(node);
                }
                // 设置当前节点为栈顶节点
                stack.push(node);
                // 设为无状态
                sb.setLength(0);
            } else if (c == deter.right || c == deter.determine) {
                if (stack.isEmpty()) {
                    throw new IllegalArgumentException(String.format("'%c'不存在匹配的父节点", c));
                } else {
                    // 如果有数字,则添加到栈顶节点的子节点中
                    if (!sb.isEmpty()) {
                        stack.peek().addChild(make.apply(sb.toString()));
                    }
                    if (c == deter.right) { // 栈顶结点的子节点收集完毕
                        if (stack.size() == 1) {
                            // 是最后一个结点,结束循环
                            if (i + 1 == n) break;
                            throw new IllegalArgumentException("只能存在一个根节点");
                        }
                        stack.pop();
                    }
                    sb.setLength(0); // 设为无状态
                }
            } else {
                // 作为构造子节点的输入
                sb.append(c);
            }
        }
        // 栈空,即只有一个结点的树
        if (stack.isEmpty()) {
            if (!sb.isEmpty()) {
                return make.apply(sb.toString());
            } else {
                throw new RuntimeException("rule不能为空");
            }
        }
        // 只能存在一个根节点
        if (stack.size() != 1) throw new RuntimeException(String.format("解析异常,存在多个根节点: %d", stack.size()));
        // 返回根节点
        return stack.pop();
    }

    public static <T extends TreeNode<T>> T buildTree(String str, Function<String, T> make) {
        return buildTree(str, make, defaultConfig);
    }

    private static class StrTree extends DrawableTreeNode<StrTree> {
        String name;
        List<StrTree> children;

        public StrTree(String name) {
            this.name = name;
            this.children = new ArrayList<>();
        }

        @Override
        public List<StrTree> getChildren() {
            return children;
        }

        @Override
        public void addChild(StrTree child) {
            children.add(child);
        }

        @Override
        public List<String> getLabels() {
            return List.of(name);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static void main(String[] args) {
        SystemUtils.consoleLoopLine(str -> {
            var root = buildTree(str, StrTree::new,
                    new TreeStringDetermine('[', ']', '~')
            );
            PrintUtils.printTree(root, "--");
            //PaintUnits.paintTree(root, "./.cache/test.png");
            System.out.println("生成成功");
        });
    }
}
