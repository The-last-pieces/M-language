package com.mnzn.grammar;

import com.mnzn.lex.TokenTag;
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

    @Getter
    private final String left; // 左部,非终结符的名字
    @Getter
    private final List<Symbol> symbols; // 右部,每个元素都是一个文法符号

    private Product(String left, List<Symbol> symbols) {
        this.left = left;
        this.symbols = symbols.stream().anyMatch(c -> c.isTerminal() && c.terminal == TokenTag.Epsilon) ? new ArrayList<>() : symbols;
    }

    // 构造结构和rule相同的ASTNode
    public ASTNode buildASNode(ASTNode[] nodes) {
        if (nodes.length != getSymbolCount())
            throw new IllegalArgumentException(String.format("节点数量不匹配: (提供)%d != (需要)%d", nodes.length, getSymbolCount()));
        // 简化语法树
        if (nodes.length == 1) return nodes[0];
        // 构造ASTNode
        ASTNode root = new ASTNode(ProduceTag.of(left));
        Arrays.stream(nodes).forEach(root::addChild);
        return root;
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

        // 添加一个产生式,使用空格分割symbol , -> 表示and产生式 , |> 表示or产生式
        public ProductBuilder add(String product) {
            if (product.contains("->")) {
                return addAnd(product);
            } else if (product.contains("|>")) {
                return addOr(product);
            } else {
                throw new IllegalArgumentException("产生式格式错误: string -> string...");
            }
        }

        public ProductBuilder addAnd(String product) {
            String[] parts = product.split("->");
            if (parts.length != 2) throw new IllegalArgumentException(String.format("产生式格式错误: %s", product));
            String left = parts[0].trim();
            List<Symbol> rights = new ArrayList<>();
            Arrays.stream(parts[1].trim().split("\\s+")).
                    map(this::node).forEach(rights::add);
            products.add(new Product(left, rights));
            return this;
        }

        public ProductBuilder addOr(String product) {
            String[] parts = product.split("\\|>");
            if (parts.length != 2) throw new IllegalArgumentException(String.format("产生式格式错误: %s", product));
            String left = parts[0].trim();
            Arrays.stream(parts[1].trim().split("\\s+")).
                    forEach(c -> products.add(new Product(left, List.of(node(c)))));
            return this;
        }

        // 从文件读取文法
        public ProductBuilder load(String file) {
            // 读取文件
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String all = String.join("\n", reader.lines().toList());
                all = all.replaceAll("(?m)(//[\\s\\S]*?(\n|$))", "\n");
                // 按行分割并去除空行
                List<String> lines = Arrays.stream(all.split("\n")).map(String::trim).
                        filter(s -> !s.isEmpty()).toList();
                StringBuilder sb = new StringBuilder();
                for (String line : lines) {
                    sb.append(' ');
                    if (line.endsWith("$")) {
                        sb.append(line, 0, line.length() - 1);
                        add(sb.toString());
                        sb.setLength(0);
                    } else {
                        sb.append(line);
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
