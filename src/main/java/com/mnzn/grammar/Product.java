package com.mnzn.grammar;

import com.mnzn.lex.TokenTag;
import com.mnzn.utils.tree.TreeNode;
import com.mnzn.utils.tree.TreeUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

// 产生式, 构成方式为 String -> 用空格隔开的Symbol
public class Product {
    // 一个文法符号, left表示非终结符, terminal表示终结符
    public record Symbol(String left, TokenTag terminal) {
        // 是否为终结符
        public boolean isTerminal() {
            return terminal != null;
        }

        @Override
        public String toString() {
            return isTerminal() ? terminal.toString() : left;
        }
    }

    @AllArgsConstructor
    private static class RuleTreeNode implements TreeNode<RuleTreeNode> {
        int index; // 结点的值,代表第index个symbol
        List<RuleTreeNode> children; // 子symbol列表

        @Override
        public List<RuleTreeNode> getChildren() {
            return children.stream().toList();
        }

        @Override
        public void addChild(RuleTreeNode child) {
            children.add(child);
        }
    }

    @Getter
    private final String left; // 左部,非终结符的名字
    @Getter
    private final List<Symbol> symbols; // 右部,每个元素都是一个文法符号
    private final RuleTreeNode rule; // AST的构造规则

    private Product(String left, List<Symbol> symbols, String rule) {
        this.left = left;
        this.symbols = symbols;
        this.rule = initRule(rule);
    }

    // ASNode的构造规则root[child...]
    // 例: 0[1,2,3]
    private RuleTreeNode initRule(String rule) {
        // 辅助索引去重
        final Set<Integer> set = new HashSet<>();
        return TreeUtils.buildTree(rule, str -> {
            int v = Integer.parseInt(str);
            if (set.contains(v))
                throw new IllegalArgumentException(String.format("索引重复: %d", v));
            if (v > getSymbolCount())
                throw new IllegalArgumentException(String.format("索引 %d 超出范围: [0,%d)", v, getSymbolCount()));
            set.add(v);
            return new RuleTreeNode(v, new ArrayList<>());
        });
    }

    // 构造结构和rule相同的ASTNode
    public ASTNode buildASNode(ASTNode[] nodes) {
        if (nodes.length != getSymbolCount())
            throw new IllegalArgumentException(String.format("节点数量不匹配: (提供)%d != (需要)%d", nodes.length, getSymbolCount()));
        // 通过层序遍历rule的结构进行构造
        Queue<RuleTreeNode> queue = new LinkedList<>();
        queue.add(rule); // 根节点
        while (!queue.isEmpty()) {
            // 待添加子节点的节点
            RuleTreeNode parent = queue.poll();
            for (RuleTreeNode child : parent.children) {
                // 将子节点也加入队列中
                queue.add(child);
                // 链接子节点和父节点
                nodes[parent.index].addChild(nodes[child.index]);
            }
        }
        // 返回根节点
        return nodes[rule.index];
    }

    // 获取第i个符号
    public Symbol get(int index) {
        return symbols.get(index);
    }

    // 获取符号长度
    public int getSymbolCount() {
        return symbols.size();
    }

    // 转字符串
    public String toString() {
        return String.format("%s -> %s", left, String.join(" ", symbols.stream().map(Symbol::toString).toList()));
    }

    // 指定点的位置
    public String toString(int dot) {
        StringBuilder sb = new StringBuilder();
        sb.append(left);
        sb.append(" -> ");
        List<String> list = new ArrayList<>(getSymbolCount() + 1);
        for (int i = 0, n = getSymbolCount(); i < n; i++) {
            if (i == dot) {
                list.add(".");
            }
            list.add(get(i).toString());
        }
        sb.append(String.join(" ", list));
        return sb.toString();
    }


    // 辅助构造产生式
    public static class ProductBuilder {
        // 字符串到tag的映射
        private final Map<String, TokenTag> tagMap = new HashMap<>();
        // 储存产生式
        private final List<Product> products = new ArrayList<>();

        // 字符串转Symbol
        private Symbol node(String s) {
            TokenTag tag = tagMap.get(s);
            if (tag != null) {
                return new Symbol(null, tag);
            } else {
                return new Symbol(s, null);
            }
        }

        // 为tag设置别名,方便从字符串转换为tag
        public ProductBuilder alias(String name, TokenTag tag) {
            if (tagMap.containsKey(name)) {
                throw new IllegalArgumentException(String.format("tag别名重复: %s", name));
            } else {
                tagMap.put(name, tag);
            }
            return this;
        }

        // 自动导入别名
        public ProductBuilder autoImport() {
            for (TokenTag tag : TokenTag.values()) {
                alias(tag.getSymbol(), tag);
            }
            return this;
        }

        public ProductBuilder autoImport(String... names) {
            for (String name : names) {
                TokenTag tag = TokenTag.valueOf(name);
                alias(name, tag);
            }
            return this;
        }

        // 添加一个普通产生式,使用空格分割symbol
        public ProductBuilder add(String rule, String product) {
            String[] parts = product.split("->");
            if (parts.length != 2) throw new IllegalArgumentException("产生式格式错误: string -> string...");
            String left = parts[0].trim();
            List<Symbol> rights = new ArrayList<>();
            Arrays.stream(parts[1].trim().split("\\s+")).map(this::node).forEach(rights::add);
            products.add(new Product(left, rights, rule));
            return this;
        }

        // 添加一个别名产生式,使用空格分割symbol
        // 例: addOr("a->b c") 等价于 add("0","a->b").add("0","a->c")
        public ProductBuilder addOr(String product) {
            String[] parts = product.split("->");
            if (parts.length != 2) throw new IllegalArgumentException("产生式格式错误: string -> string...");
            String left = parts[0].trim();
            Arrays.stream(parts[1].trim().split("\\s+")).forEach(c -> {
                // 单点映射,rule取"0"即可
                products.add(new Product(left, List.of(node(c)), "0"));
            });
            return this;
        }

        // 从文件读取文法
        public ProductBuilder load(String file) {
            // 读取文件
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String all = String.join("\n", reader.lines().toList());
                all = all.replaceAll("(?m)(//[\\s\\S]*?(\n|$))", "\n");
                // 按行分割并去除空行
                List<String> lines = Arrays.stream(all.split("\n")).
                        filter(s -> !s.trim().isEmpty()).toList();
                for (int i = 0, n = lines.size(); i < n; i++) {
                    String line = lines.get(i);
                    // 使用add
                    if (line.matches("\\s*\\S+\\s*->.*")) {
                        add(lines.get(++i), line);
                    } else { // 使用addOr
                        addOr(line.replaceFirst("\\|>", "->"));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return this;
        }

        // 返回构造完毕的产生式列表
        public Product[] build() {
            return products.toArray(Product[]::new);
        }

        // 返回构造完毕的唯一一个产生式
        public Product buildOne() {
            if (products.size() != 1) throw new IllegalArgumentException("产生式数量不为1");
            return products.get(0);
        }
    }
}
