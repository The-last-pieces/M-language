package com.mnzn.inter;

import com.mnzn.grammar.ASTNode;
import com.mnzn.grammar.Grammar;
import com.mnzn.grammar.Product;
import com.mnzn.lex.LexParser;
import com.mnzn.lex.Token;
import com.mnzn.lex.TokenTag;
import com.mnzn.lex.imp.TokenFloat;
import com.mnzn.lex.imp.TokenId;
import com.mnzn.lex.imp.TokenInt;
import com.mnzn.lex.imp.TokenString;
import com.mnzn.model.ModuleFunction;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;

// 一个解释器上下文,对String流进行解释
public class InterpreterContext {
    private final Map<String, Object> variables = new HashMap<>();
    // Todo
    private final Map<String, ModuleFunction> functions = new HashMap<>() {{
        put("print", new ModuleFunction() {
            @Override
            public Object apply(Object[] args) {
                Arrays.stream(args).forEach(System.out::println);
                return null;
            }
        });
    }};

    private Object getVariable(String name) {
        Object value = variables.get(name);
        if (value == null) {
            throw new RuntimeException("Variable " + name + " not found");
        } else {
            return value;
        }
    }

    private Object setVariable(String name, Object value) {
        variables.put(name, value);
        return value;
    }

    private BigDecimal num(Object value) {
        return new BigDecimal(value.toString());
    }

    // 计算表达式树 Todo 优化执行流程
    public Object eval(ASTNode root) {
        // + - * / = id int float
        return switch (root.tokenTag()) {
            case Add -> {
                if (root.size() == 1) {
                    yield num(eval(root.c(0)));
                } else {
                    yield num(eval(root.c(0))).add(num(eval(root.c(1))));
                }
            }
            case Sub -> {
                if (root.size() == 1) {
                    yield num(eval(root.c(0))).negate();
                } else {
                    yield num(eval(root.c(0))).subtract(num(eval(root.c(1))));
                }
            }
            case Mut -> num(eval(root.c(0))).multiply(num(eval(root.c(1))));
            case Div -> num(eval(root.c(0))).divide(num(eval(root.c(1))), new MathContext(10));
            case Pow -> Math.pow(num(eval(root.c(0))).doubleValue(), num(eval(root.c(1))).doubleValue());
            case Mod -> num(eval(root.c(0))).remainder(num(eval(root.c(1))));
            case Assign -> setVariable(root.t(0, TokenId.class).getId(), eval(root.c(1)));
            case Identifier -> getVariable(root.t(TokenId.class).getId());
            case IntLiteral -> root.t(TokenInt.class).getValue();
            case FloatLiteral -> root.t(TokenFloat.class).getValue();
            case StringLiteral -> root.t(TokenString.class).getValue();
            case Comma -> {
                Object list = eval(root.c(0));
                if (list instanceof List l) {
                    List<Object> arr = new ArrayList<>(Arrays.stream(l.toArray()).toList());
                    arr.add(eval(root.c(1)));
                    yield arr;
                } else {
                    yield List.of(list, eval(root.c(1)));
                }
            }
            default -> throw new IllegalStateException("Unexpected token: " + root.tokenTag());
        };
    }

    public static void main(String[] args) {
        /*
        // 计算完+的表达式进行赋值运算(右结合)
        expr    -> id = expr+
        1[0,2]
        expr    |> expr+
        // 计算完*的表达式进行+-运算
        expr+   -> expr+ +- expr*
        1[0,2]
        expr+   |> expr*
        +-      |> + -
        // 计算完顶级元素的表达式进行* / **%运算
        expr*   -> expr* * / **% value
        1[0,2]
        expr*   |> value
        * /**%   |> * / ** %
        // 字面量/标识符/()包裹的表达式
        value   -> ( expr )
        1[0,2]
        value   |> bi ii fi si
        // 函数调用也是一个value
        value   -> id ( exprs )
        0[1]
        exprs   -> expr , exprs
        0[1]
        exprs   |> expr e
        */
        // Todo 自动导入文法文件
        Grammar grammar = new Grammar("expr", Arrays.stream(new Product.ProductBuilder()
                // 设置tag别名
                .autoImport()
                .add("1[0,2]", "expr -> id = expr+")
                .addOr("expr -> expr+")
                .add("1[0,2]", "expr+ -> expr+ +- expr*")
                .addOr("expr+ -> expr*")
                .addOr("+- -> + -")
                .add("1[0,2]", "expr* -> expr* */**% value")
                .addOr("expr* -> value")
                .addOr("*/**% -> * / ** %")
                .add("1[0,2]", "value -> ( expr )")
                .add("0[1]", "value -> +- value")
                .addOr("value -> id bi ii fi si")
                // Todo
//                .add("0[1]", "value -> id ( exprs )")
//                .add("0[1]", "exprs -> expr , exprs")
//                .addOr("exprs -> expr e")
                .build()).toList());
        // 解释器上下文
        InterpreterContext context = new InterpreterContext();
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            try {
                List<Token> tokens = new LexParser().parse(scanner.next());
                tokens.add(Token.of(TokenTag.Eof, ""));
                ASTNode node = grammar.parse(tokens);
                System.out.println(context.eval(node));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
//        while (scanner.hasNext()) {
//            try {
//                List<Token> tokens = new LexParser().parse(scanner.nextLine()).stream().
//                        filter(t -> t.getTag() != TokenTag.Nop).toList();
//
//                Grammar.MatchResult matchResult = grammar.matchTokens(tokens);
//                if (matchResult.isSuccess()) {
//                    ASTNode node = matchResult.node();
//                    int cnt = matchResult.cnt() - 1;
//                    System.out.printf("match cnt = %d %s\n", cnt,
//                            String.join(" ", tokens.stream().
//                                    map(Token::toString).toList().subList(0, cnt)));
//                    System.out.println(context.eval(node));
//                } else {
//                    System.out.println("error: " + matchResult.error());
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
    }
}
