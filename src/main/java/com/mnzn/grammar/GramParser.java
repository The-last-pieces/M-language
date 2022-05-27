package com.mnzn.grammar;

import com.mnzn.grammar.ast.ASTNode;
import com.mnzn.lex.LexParser;
import com.mnzn.lex.Token;
import com.mnzn.lex.TokenTag;
import com.mnzn.utils.visual.paint.PaintUnits;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

// LR(1)语法分析器,根据文法把Token序列转为ASTNode
public class GramParser {
    private enum ActionTag {
        Shift, // 移入
        Reduce,// 规约
        Accept,// 接受
        Error  // 错误
    }

    // record
    private record Action(
            // Action表中的行为
            ActionTag tag,
            // 移动或规约的下标
            int index
    ) {
    }

    private static Action s(int i) {
        return new Action(ActionTag.Shift, i);
    }

    private static Action r(int i) {
        return new Action(ActionTag.Reduce, i);
    }

    private static Action a() {
        return new Action(ActionTag.Accept, -1);
    }

    private static Action e() {
        return new Action(ActionTag.Error, -1);
    }

    private final Product[] products;               // 产生式列表
    private final Map<String, List<Product>> prodGroup; // 按照left分组的产生式

    private final int stateCount;                   // 状态数(项集数,action&goto表的行数)
    private final int endCount;                     // 终结符数(token类型数,action表的列数)
    private final int leftCount;                    // 非终结符数(product左端数,goto表的列数)

    private final Action[][] actionTable;           // action表
    private final int[][] gotoTable;                // goto表

    private final Map<String, Integer> productToId; // 产生式名到id的映射
    private final Map<TokenTag, Integer> tokenToId; // token类型到id的映射

    // 产生式的项
    private record Item(
            // product的id
            int pid,
            // ·所在的下标
            int dot,
            // 前向查看的搜索符(终结符)
            TokenTag lookahead
    ) {
    }

    /// 辅助算法

    // 求文法符号串r的first集
    Set<TokenTag> calculateFirst(List<Product.Symbol> symbols) {
        // 结果集
        final Set<TokenTag> result = new HashSet<>();
        // 辅助nullable去重
        final Set<String> inNull = new HashSet<>();
        // 辅助first去重
        final Set<String> inFirst = new HashSet<>();

        // 检查一个文法符号串能否推导出epsilon
        Function<List<Product.Symbol>, Boolean> nullable = new Function<>() {
            @Override
            public Boolean apply(List<Product.Symbol> symbols) {
                // 每个节点都必须可推导为epsilon
                for (Product.Symbol s : symbols) {
                    if (s.isTerminal()) {
                        // 遇到不是epsilon的终结符,则不可能推导出epsilon
                        if (s.terminal() != TokenTag.Epsilon) {
                            return false;
                        }
                    } else if (!inNull.contains(s.left())) {
                        // 检查s对应的产生式,任意一个能推导出epsilon,则s可推导出epsilon
                        boolean hasEpsilon = false;
                        for (Product prod : prodGroup.get(s.left())) {
                            // 避免重复搜索
                            inNull.add(s.left());
                            boolean hasNull = this.apply(prod.getSymbols());
                            inNull.remove(s.left());
                            if (hasNull) {
                                // 任意一个产生式能生成epsilon即可
                                hasEpsilon = true;
                                break;
                            }
                        }
                        // s的所有产生式都不能生成epsilon,则s不可推导出epsilon
                        if (!hasEpsilon) return false;
                    } else {
                        // 在inNull中的非终结符不需要重复搜索
                        return false;
                    }
                }
                return true;
            }
        };

        // 求一个文法符号串的first集
        Consumer<List<Product.Symbol>> first = new Consumer<>() {
            @Override
            public void accept(List<Product.Symbol> symbols) {
                // 最后一次迭代Symbol能否推导出epsilon
                boolean hasEpsilon = false;
                for (Product.Symbol node : symbols) {
                    hasEpsilon = false; // 重置
                    // 前面的Symbol都能推导出epsilon
                    if (node.isTerminal()) {
                        // 遇到终结符,则直接添加
                        result.add(node.terminal());
                    } else if (!inFirst.contains(node.left())) {
                        // 合并node可能的产生式的first集
                        for (Product prod : prodGroup.get(node.left())) {
                            // 避免重复搜索
                            inFirst.add(node.left());
                            this.accept(prod.getSymbols());
                            inFirst.remove(node.left());
                        }
                    }
                    // 去除epsilon,因为只有所有的Symbol都能推导出epsilon,此序列才能推导出epsilon
                    result.remove(TokenTag.Epsilon);
                    // 检查当前节点能不能推导出epsilon
                    if (nullable.apply(List.of(node))) {
                        hasEpsilon = true; // 当前节点能推导出epsilon,继续迭代
                    } else {
                        break; // 当前节点不能推导出epsilon,终止迭代
                    }
                }
                if (hasEpsilon) result.add(TokenTag.Epsilon);
            }
        };

        // 调用辅助函数
        first.accept(symbols);

        return result;
    }

    // 求项集的闭包,返回项集
    Set<Item> closure(Set<Item> items) {
        // 结果集
        Set<Item> result = new HashSet<>(items);
        // 使用bfs求闭包,等待迭代的项集
        Queue<Item> queue = new LinkedList<>(items);
        while (!queue.isEmpty()) {
            // 取出当前项
            Item item = queue.poll();
            // 获取项对应的产生式,需要[A->α · B β , a]格式
            // α β为可空 结点列表,B为非终结符,a为终结符(输入的token流)
            Product A = products[item.pid];
            // 点在最后,为规约项,跳过(即B为空)
            if (item.dot == A.getSymbolCount()) continue;
            // 所在位置的结点(即B)
            Product.Symbol B = A.getSymbols().get(item.dot);
            // 为终结符,跳过
            if (B.isTerminal()) continue;
            // 获取每个产生式[B -> γ] , γ为symbol列表
            for (Product By : prodGroup.get(B.left())) {
                // 构造symbol序列 βα
                List<Product.Symbol> beta = A.getSymbols().subList(item.dot + 1, A.getSymbolCount());
                List<Product.Symbol> alpha = A.getSymbols().subList(0, item.dot);
                beta.addAll(alpha);
                // 计算βα的first集
                Set<TokenTag> first = calculateFirst(beta);
                // 对First(βα)中的每个终结符,将[B -> · γ, b]加入到项集中
                for (TokenTag b : first) {
                    // 构造项[B -> · γ, b]
                    Item add = new Item(productId(By), 0, b);
                    // 如果不在结果集中,则加入到队列中参与层序遍历
                    if (result.add(add)) {
                        queue.add(add);
                    }
                }
            }
        }
        return result;
    }

    /// 初始化相关
    // 获取分组
    private Map<String, List<Product>> initProdGroups() {
        Map<String, List<Product>> groups = new HashMap<>();
        for (Product p : products) {
            groups.computeIfAbsent(p.getLeft(), k -> new ArrayList<>()).add(p);
        }
        return groups;
    }

    // 获取状态数 Todo
    private int initStateCount() {
        // 构造产生式的项集

//        int count = 0;
//        for (Product p : products) {
//            count += 1;
//        }
//        return count;
        return 12;
    }

    // 获取终结符数
    private int initEndCount() {
        Set<TokenTag> set = new HashSet<>();
        for (Product p : products) {
            for (Product.Symbol symbol : p.getSymbols()) {
                if (symbol.isTerminal()) {
                    set.add(symbol.terminal());
                }
            }
        }
        return set.size() + 1; // 加一个终结符';'
    }

    // 获取非终结符数
    private int initLeftCount() {
        Set<String> set = new HashSet<>();
        for (Product p : products) {
            set.add(p.getLeft());
        }
        return set.size();
    }

    // 获取action表 Todo
    private Action[][] initActionTable() {
        Action[][] table = new Action[stateCount][endCount];
        // init with error
        for (int i = 0; i < stateCount; i++) {
            for (int j = 0; j < endCount; j++) {
                table[i][j] = e();
            }
        }
        // 0 ~ 0:s5 , 3:s4
        table[0][0] = s(5);
        table[0][3] = s(4);
        // 1 ~ 1:s6 , 5:a
        table[1][1] = s(6);
        table[1][5] = a();
        // 2 ~ 1:r2 , 2:s7 , 4:r2 , 5:r2
        table[2][1] = r(2);
        table[2][2] = s(7);
        table[2][4] = r(2);
        table[2][5] = r(2);
        // 3 ~ 1:r4 , 2:r4 , 4:r4 , 5:r4
        table[3][1] = r(4);
        table[3][2] = r(4);
        table[3][4] = r(4);
        table[3][5] = r(4);
        // 4 ~ 0:s5 , 3:s4
        table[4][0] = s(5);
        table[4][3] = s(4);
        // 5 ~ 1:r6 , 2:r6 , 4:r6 , 5:r6
        table[5][1] = r(6);
        table[5][2] = r(6);
        table[5][4] = r(6);
        table[5][5] = r(6);
        // 6 ~ 0:s5 , 3:s4
        table[6][0] = s(5);
        table[6][3] = s(4);
        // 7 ~ 0:s5 , 3:s4
        table[7][0] = s(5);
        table[7][3] = s(4);
        // 8 ~ 1:s6 , 4:s11
        table[8][1] = s(6);
        table[8][4] = s(11);
        // 9 ~ 1:r1 , 2:s7 , 4:r1 , 5:r1
        table[9][1] = r(1);
        table[9][2] = s(7);
        table[9][4] = r(1);
        table[9][5] = r(1);
        // 10 ~ 1:r3 , 2:r3 , 4:r3 , 5:r3
        table[10][1] = r(3);
        table[10][2] = r(3);
        table[10][4] = r(3);
        table[10][5] = r(3);
        // 11 ~ 1:r5 , 2:r5 , 4:r5 , 5:r5
        table[11][1] = r(5);
        table[11][2] = r(5);
        table[11][4] = r(5);
        table[11][5] = r(5);
        return table;
    }

    // 获取goto表 Todo
    private int[][] initGotoTable() {
        int[][] table = new int[stateCount][leftCount];
        // init with -1
        for (int i = 0; i < stateCount; i++) {
            for (int j = 0; j < leftCount; j++) {
                table[i][j] = -1;
            }
        }
        // 0: 0:1 , 1:2 , 2:3
        table[0][0] = 1;
        table[0][1] = 2;
        table[0][2] = 3;
        // 4: 0:8 , 1:2 , 2:3
        table[4][0] = 8;
        table[4][1] = 2;
        table[4][2] = 3;
        // 6: 1:9 , 2:3
        table[6][1] = 9;
        table[6][2] = 3;
        // 7: 2:10
        table[7][2] = 10;
        return table;
    }

    // 获取product => id的映射
    private Map<String, Integer> initProductToId() {
        Map<String, Integer> map = new HashMap<>();
        int i = 0;
        for (Product prod : products) {
            if (!map.containsKey(prod.getLeft())) {
                map.put(prod.getLeft(), i++);
            }
        }
        return map;
    }

    // 获取token => id的映射
    private Map<TokenTag, Integer> initTokenToId() {
        Map<TokenTag, Integer> map = new HashMap<>();
        int i = 0;
        for (Product p : products) {
            for (Product.Symbol symbol : p.getSymbols()) {
                if (symbol.isTerminal()) {
                    if (!map.containsKey(symbol.terminal())) {
                        map.put(symbol.terminal(), i++);
                    }
                }
            }
        }
        return map;
    }

    /*
    辅助函数
     */
    // 获取token在action表中的下标 Todo
    private int tokenId(TokenTag tag) {
        return switch (tag) {
            case Identifier -> 0;
            case Add -> 1;
            case Mut -> 2;
            case L1 -> 3;
            case R1 -> 4;
            case Semi -> 5;
            default -> -1;
        };
        /*
        Integer id = tokenToId.get(terminal);
        if (id == null) {
            throw new RuntimeException("token not found");
        } else {
            return id;
        }
        */
    }

    // 获取product在goto表中的下标 Todo
    private int productId(Product product) {
        return switch (product.getLeft()) {
            case "E" -> 0;
            case "T" -> 1;
            case "F" -> 2;
            default -> -1;
        };
        /*
        String left = product.getLeft();
        Integer id = productToId.get(left);
        if (id == null) {
            throw new RuntimeException("product not found");
        } else {
            return id;
        }
        */
    }

    /*
    Todo 输入文法自动生成语法分析表
    输入 : 一个增广文法G'
    自动生成 :
    action表, goto表
    token映射表, product映射表
    步骤 :
    0.1 将所有涉及的token映射为有序int,将所有不重复的product左端映射为有序int
    0.2 由G'求出项集族,GOTO函数,闭包函数
    0.3 初始状态为[S'->·S, $]
    1. 构建G'的规范LR(0)项集族C={I0,I1,...,In}
    2. 根据Ii构造得到状态i, 以如下规则求action的第i行
    2.1. 如果[A->α·aβ, b]在Ii中, 且GOTO(Ii, a) = Ij 则action[i,a] = s(j)
    2.2. 如果[A->α·, a]在Ii中, A≠S' 则action[i,a] = r(id(A->α))
    2.3. 如果[S'->S·, $]在Ii中, 则action[i,$] = acc
    3. 对每个状态i,对每个非终结符, 如果GOTO(Ii, A) = Ij ,则goto[i,A] = j
    4. 2,3中未填充的空位都设置为error/-1
     */
    public GramParser() {
        /*
        E -> TE'
        E' -> +TE' | e
        T -> FT'
        T' -> *FT' | e
        F -> (E) | id
        */
//        products = new Product.ProductBuilder()
//                .alias("e", TokenTag.Epsilon)
//                .alias("id", TokenTag.Identifier)
//                .alias("+", TokenTag.Add)
//                .alias("*", TokenTag.Mut)
//                .alias("(", TokenTag.L1)
//                .alias(")", TokenTag.R1)
//                .add("0[1,2]", "E -> T E'")
//                .add("0[1,2]", "E' -> + T E'")
//                .addOr("E' -> e")
//                .add("0[1,2]", "T -> F T'")
//                .add("0[1,2]", "T' -> * F T'")
//                .addOr("T' -> e")
//                .add("0[1,2]", "F -> ( E )")
//                .addOr("F -> id")
//                .build();

        products = new Product.ProductBuilder()
                // 设置tag别名
                .alias("id", TokenTag.Identifier)
                .alias("+", TokenTag.Add)
                .alias("*", TokenTag.Mut)
                .alias("(", TokenTag.L1)
                .alias(")", TokenTag.R1)
                // 任意个顶级表达式相乘的表达式相加
                .add("1[0,2]", "E -> E + T")
                .add("0", "E -> T")
                // 任意个顶级表达式相乘
                .add("1[0,2]", "T -> T * F")
                .add("0", "T -> F")
                // 用括号提升成顶级表达式
                .add("1", "F -> ( E )")
                // 单个变量就是一个顶级运算单元
                .add("0", "F -> id")
                .build();
        prodGroup = initProdGroups();   // 产生式分组
//        Set<TokenTag> f;
//        f = calculateFirst(List.of(new Product.Symbol("E", null)));
//        f = calculateFirst(List.of(new Product.Symbol("E'", null)));
//        f = calculateFirst(List.of(new Product.Symbol("T", null)));
//        f = calculateFirst(List.of(new Product.Symbol("T'", null)));
//        f = calculateFirst(List.of(new Product.Symbol("F", null)));

        stateCount = initStateCount();  // 状态数
        endCount = initEndCount();      // 终结符数
        leftCount = initLeftCount();    // 非终结符数

        actionTable = initActionTable();// action表
        gotoTable = initGotoTable();    // goto表

        productToId = initProductToId();// product映射表
        tokenToId = initTokenToId();    // token映射表
    }

    // 解析token流
    public ASTNode parse(List<Token> tokens) {
        // 符号栈
        Stack<Integer> statueStack = new Stack<>();
        statueStack.push(0); // 栈底标记
        // 符号栈
        Stack<ASTNode> tokensStack = new Stack<>();
        // 当前符号下标
        int cur = 0;
        while (true) {
            int s = statueStack.peek(); // 栈顶状态
            Token a = tokens.get(cur); // 当前输入符号
            Action action = actionTable[s][tokenId(a.getTag())];
            if (action.tag() == ActionTag.Shift) {
                // 状态入栈
                statueStack.push(action.index());
                // 符号入栈
                tokensStack.push(new ASTNode(a));
                ++cur;
            } else if (action.tag() == ActionTag.Reduce) {
                // 获取产生式
                Product production = products[action.index() - 1];
                // 弹出|β|个ASNode和状态
                ASTNode[] nodes = new ASTNode[production.getSymbolCount()];
                for (int i = production.getSymbolCount() - 1; i >= 0; i--) {
                    nodes[i] = tokensStack.pop();
                    statueStack.pop();
                }
                // 规约A->β
                tokensStack.push(production.buildASNode(nodes));
                // 要跳到的状态下标
                int A = gotoTable[statueStack.peek()][productId(production)];
                statueStack.push(A); // 入栈
                // 将规约结果入栈
            } else if (action.tag() == ActionTag.Accept) {
                break;
            } else {
                throw new RuntimeException("语法解析出错!");
            }
        }

        return tokensStack.pop();
    }

    public static void main(String[] args) {
        GramParser parser = new GramParser();
        Set<TokenTag> first = parser.calculateFirst(parser.products[0].getSymbols());
        // "id*id+id+id*id+id*id*id*(id+id);"
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            try {
                List<Token> tokens = new LexParser().parse(scanner.next());
                ASTNode node = parser.parse(tokens);
                String savePath = "./.cache/out.png";
                PaintUnits.paintTree(node, savePath);
                System.out.printf("语法树图片已经保存在%s中\n", savePath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
