package com.mnzn.lex;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Token的类型
public enum TokenTag {
    // 界定符
    L1("("),
    R1(")"),
    L2("["),
    R2("]"),
    L3("{"),
    R3("}"),
    Semi(";"),
    Inner(":"),
    Comma(","),
    Dot("."),
    // 关系运算符
    Eq("=="),
    Le("<"),
    Ge(">"),
    Leq("<="),
    Geq(">="),
    Neq("!="),
    // 数值运算符
    Add("+"),
    Sub("-"),
    Mut("*"),
    Div("/"),
    Pow("**"),
    Mod("%"),
    Assign("="),
    // 逻辑运算符
    And("&&"),
    Or("||"),
    Not("!"),
    // 按位运算符
    AndBit("&"),
    OrBit("|"),
    NotBit("~"),
    XorBit("^"),
    // 类型关键字
    VoidT("void"),
    BoolT("bool"),
    IntT("int"),
    FloatT("float"),
    StringT("string"),
    ObjectT("object"),
    ClassT("class"),
    // 控制流关键字
    For("for"), // for
    Break("break"),
    Continue("continue"),
    If("if"),
    Else("else"),
    Return("return"), // return
    Switch("switch"),
    Case("case"),
    Default("default"),
    // 其他特性的关键字
    Import("import"), // 引入模块
    Let("let"), // 定义变量
    // 标识符
    Identifier(Pattern.compile("[a-zA-Z_]\\w*"), "id"),
    /// 字面量
    // 数字
    BoolLiteral(Pattern.compile("true|false"), "bi"),
    IntLiteral(Pattern.compile("\\d+"), "ii"),
    FloatLiteral(Pattern.compile("\\d+\\.\\d*"), "fi"),
    // 字符串,使用\"转义
    StringLiteral(Pattern.compile("\".*?(?!\\\\)\""), "si"),
    // 空白/注释
    Nop(Pattern.compile("(?m)(\\s+)|(//[\\s\\S]*?\n)|(/\\*[\\s\\S]*?\\*/)")),
    /// 不参与词法分析的特殊token
    // 文法空串
    Epsilon("e", true),
    // 文法结束符
    Eof("$", true);

    private final Pattern pattern;
    @Getter
    private final String pure;
    @Getter
    // 文法中的别名
    private final String symbol;

    // 不参与词法分析的特殊token
    TokenTag(String str, boolean isNone) {
        if (isNone) {
            this.pattern = null;
            this.pure = null;
            this.symbol = str;
        } else {
            this.pattern = null;
            this.pure = str;
            this.symbol = str;
        }
    }

    TokenTag(String str) {
        this(str, false);
    }

    TokenTag(Pattern regex) {
        this(regex, null);
    }

    TokenTag(Pattern regex, String symbol) {
        this.pattern = Pattern.compile(regex.pattern());
        this.pure = null;
        this.symbol = symbol;
    }

    // 是否为纯字符串的TokenTag
    boolean isPure() {
        return this.pure != null;
    }

    // 用以支持语法分析的特殊token
    boolean isNone() {
        return this.pure == null && this.pattern == null;
    }

    boolean needParse() {
        return !isNone() && this != Nop;
    }

    // 在开头匹配,匹配失败返回-1,否则返回下一个字符的位置
    private int match(String s, int idx) {
        if (null != pure) {
            // 匹配纯字符
            if (s.startsWith(pure, idx)) return idx + pure.length();
        } else if (null != pattern) {
            Matcher matcher = pattern.matcher(s);
            if (matcher.find(idx) && matcher.start() == idx) {
                return matcher.end();
            }
        }
        return -1;
    }

    // 获取最优匹配 Todo 自动转为NFA
    static Tuple2<TokenTag, Integer> bestMatch(String s, int idx) {
        List<Tuple2<TokenTag, Integer>> matches = new ArrayList<>();
        for (TokenTag tag : values()) {
            int nxt = tag.match(s, idx);
            if (nxt != -1) {
                matches.add(Tuple.of(tag, nxt));
            }
        }
        // 多个匹配时不优先考虑标识符
        if (matches.size() > 1) {
            matches.removeIf(t -> t._1 == Identifier);
        }
        if (matches.isEmpty()) {
            // Todo
            throw new RuntimeException(String.format("No match : \"%s\"", s.substring(idx, s.indexOf('\n', idx))));
        }
        if (matches.size() == 1) {
            return matches.get(0);
        }
        // 获取最长匹配
        Tuple2<TokenTag, Integer> best = matches.stream().max(Comparator.comparingInt(a -> a._2)).get();
        // 检查最长匹配的个数
        long count = matches.stream().filter(c -> c._2.equals(best._2)).count();
        if (count > 1) {
            // Todo
            throw new RuntimeException(String.format("Ambiguous match: \"%s\"", s.substring(idx)));
        } else {
            return best;
        }
    }
}
