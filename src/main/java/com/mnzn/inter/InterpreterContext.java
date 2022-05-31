package com.mnzn.inter;

import com.mnzn.grammar.ASTNode;
import com.mnzn.grammar.Grammar;
import com.mnzn.grammar.Product;
import com.mnzn.lex.LexParser;
import com.mnzn.lex.Token;
import com.mnzn.lex.imp.*;
import com.mnzn.model.ModuleFunction;
import com.mnzn.utils.sys.SystemUtils;
import com.mnzn.utils.visual.paint.PaintUnits;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;

// 一个解释器上下文,对String流进行解释
public class InterpreterContext {
    private final Map<String, Object> variables = new HashMap<>();
    // Todo
    private final Map<String, ModuleFunction> functions = new HashMap<>() {{
        put("print", new ModuleFunction("print") {
            @Override
            public Object apply(Object[] args) {
                Arrays.stream(args).forEach(System.out::print);
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
        if (value instanceof Boolean b) {
            return BigDecimal.valueOf(b ? 1 : 0);
        }
        return new BigDecimal(value.toString());
    }

    // Todo 简化并检查语法树结构,如展开长树为List,检查类型系统,检查符号表
    private ASTNode simplify(ASTNode root) {
        return root;
    }

    // Todo 执行简化后的语法树 Todo 完善解释系统
    private Object evalSimple(ASTNode root) {
        if (!root.isLeaf()) {
            return switch (root.p().toString()) {
                case "if_open_stmt" -> {
                    BigDecimal cond = num(evalSimple(root.c(1)));
                    if (cond.compareTo(BigDecimal.ZERO) != 0) {
                        evalSimple(root.c(2));
                    }
                    yield null;
                }
                case "if_match_stmt" -> {
                    // cond , case1 case2
                    BigDecimal cond = num(evalSimple(root.c(1)));
                    if (cond.compareTo(BigDecimal.ZERO) == 0) {
                        evalSimple(root.c(2));
                    } else {
                        evalSimple(root.c(4));
                    }
                    yield null;
                }
                case "stmt_seq" -> {
                    if (root.size() == 1) evalSimple(root.c(0));
                    else if (root.size() == 2) {
                        evalSimple(root.c(0));
                        evalSimple(root.c(1));
                    }
                    yield null;
                }
                case "expr_stmt" -> {
                    if (root.size() == 1) yield null;
                    evalSimple(root.c(0));
                    yield null;
                }
                case "compound_stmt" -> {
                    evalSimple(root.c(1));
                    yield null;
                }
                case "while_stmt" -> {
                    while (true) {
                        BigDecimal cond = num(evalSimple(root.c(1)));
                        if (cond.compareTo(BigDecimal.ZERO) == 0) break;
                        evalSimple(root.c(2));
                    }
                    yield null;
                }
                case "field_expr" -> {
                    TokenId id = root.t(0, TokenId.class);
                    TokenId field = root.t(1, TokenId.class);
                    Object value = getVariable(id.getId());
                    try {
                        yield value.getClass().getField(field.getId()).get(value);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                case "sub_expr" -> {
                    TokenId id = root.t(0, TokenId.class);
                    Object value = getVariable(id.getId());
                    int index = num(evalSimple(root.c(1))).intValue();
                    if (value instanceof List) {
                        yield ((List<?>) value).get(index);
                    } else if (value instanceof String) {
                        yield String.valueOf(((String) value).charAt(index));
                    } else {
                        throw new RuntimeException("Unsupported sub expression " + value.getClass());
                    }
                }
                case "call_expr" -> {
                    TokenId id = root.t(0, TokenId.class);
                    Object val = evalSimple(root.c(2));
                    List<?> args;
                    if (val instanceof List) {
                        args = (List<?>) val;
                    } else {
                        args = List.of(val);
                    }
                    ModuleFunction method = functions.get(id.getId());
                    if (method == null) {
                        throw new RuntimeException("Unknown function " + id.getId());
                    }
                    yield method.apply(args.toArray());
                }
                case "expr_seq" -> {
                    List<Object> result;
                    if (root.size() == 3) {
                        Object[] arr = root.c(0).isLeaf() ?
                                new Object[]{evalSimple(root.c(0))} :
                                ((List<?>) evalSimple(root.c(0))).toArray();
                        result = new ArrayList<>(Arrays.asList(arr));
                        result.add(evalSimple(root.c(2)));
                    } else {
                        result = new ArrayList<>();
                    }
                    yield result;
                }
                case "parent_expr" -> evalSimple(root.c(1));
                case "unary_expr" -> {
                    BigDecimal value = num(evalSimple(root.c(1)));
                    yield switch (root.tag(0)) {
                        case Add -> value;
                        case Sub -> value.negate();
                        case Not -> value.compareTo(BigDecimal.ZERO) == 0;
                        case SelfAdd -> {
                            TokenId id = root.t(1, TokenId.class);
                            BigDecimal oldValue = num(getVariable(id.getId()));
                            yield setVariable(id.getId(), oldValue.add(BigDecimal.ONE));
                        }
                        case SelfSub -> {
                            TokenId id = root.t(1, TokenId.class);
                            BigDecimal oldValue = num(getVariable(id.getId()));
                            yield setVariable(id.getId(), oldValue.subtract(BigDecimal.ONE));
                        }
                        default -> throw new RuntimeException("Unsupported unary operator " + root.c(0).tag());
                    };
                }
                case "binary_expr_*",
                        "binary_expr_+",
                        "binary_expr_<<",
                        "binary_expr_<=",
                        "binary_expr_==",
                        "binary_expr_&",
                        "binary_expr_^",
                        "binary_expr_|",
                        "binary_expr_&&",
                        "binary_expr_||" -> {
                    BigDecimal left = num(evalSimple(root.c(0)));
                    BigDecimal right = num(evalSimple(root.c(2)));
                    yield switch (root.tag(1)) {
                        case Add -> left.add(right);
                        case Sub -> left.subtract(right);
                        case Mut -> left.multiply(right);
                        case Div -> left.divide(right, new MathContext(6));
                        case Mod -> left.remainder(right);
                        case Eq -> left.compareTo(right) == 0;
                        case Neq -> left.compareTo(right) != 0;
                        case Le -> left.compareTo(right) < 0;
                        case Ge -> left.compareTo(right) > 0;
                        case Leq -> left.compareTo(right) <= 0;
                        case Geq -> left.compareTo(right) >= 0;
                        case And -> left.compareTo(BigDecimal.ZERO) != 0 && right.compareTo(BigDecimal.ZERO) != 0;
                        case Or -> left.compareTo(BigDecimal.ZERO) != 0 || right.compareTo(BigDecimal.ZERO) != 0;
                        default -> throw new RuntimeException("Unknown operator " + root.tag());
                    };
                }
                case "cond_expr" -> {
                    // 0 ? 2 : 4
                    BigDecimal cond = num(evalSimple(root.c(0)));
                    if (cond.compareTo(BigDecimal.ZERO) == 0) {
                        yield evalSimple(root.c(4));
                    } else {
                        yield evalSimple(root.c(2));
                    }
                }
                case "ass_expr" -> {
                    TokenId id = root.t(0, TokenId.class);
                    Object value = evalSimple(root.c(2));
                    yield setVariable(id.getId(), value);
                }
                default -> throw new RuntimeException(String.format("Unsupported token %s", root.p()));
            };
        } else {
            return switch (root.t().getTag()) {
                case Identifier -> getVariable(root.t(TokenId.class).getId());
                case BoolLiteral -> root.t(TokenBool.class).isValue();
                case IntLiteral -> root.t(TokenInt.class).getValue();
                case FloatLiteral -> root.t(TokenFloat.class).getValue();
                case StringLiteral -> root.t(TokenString.class).getValue();
                case Semi -> null;
                default -> throw new RuntimeException(String.format("Unsupported token %s", root.t()));
            };
        }
    }

    // 执行表达式树
    public Object eval(ASTNode root) {
        return evalSimple(simplify(root));
    }

    public static void main(String[] args) {
        String path = Objects.requireNonNull(InterpreterContext.class.getResource("/all_grammars/c_gram.c")).getFile();
        // 自动导入文法文件
        Grammar grammar = new Grammar("root_unit", Arrays.stream(new Product.ProductBuilder()
                .autoImport().load(path)
                .build()).toList());
        System.out.println("build success");
        // 解释器上下文
        InterpreterContext context = new InterpreterContext();
        SystemUtils.consoleLoopLine(str -> {
            List<Token> tokens = new LexParser().parseFile(str);
            ASTNode node = grammar.parse(tokens);
            //PrintUtils.printTree(node, "  ");
            //PaintUnits.paintTree(node, "./.cache/ast_v.png", 1000);
            System.out.println(context.eval(node));
            //System.out.println("finish");
        });
    }
}
