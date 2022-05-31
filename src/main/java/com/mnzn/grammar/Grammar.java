package com.mnzn.grammar;

import com.mnzn.lex.LexParser;
import com.mnzn.lex.Token;
import com.mnzn.lex.TokenTag;
import com.mnzn.utils.sys.SystemUtils;
import com.mnzn.utils.visual.console.PrintUtils;
import com.mnzn.utils.visual.paint.PaintUnits;
import io.vavr.Tuple;
import io.vavr.Tuple2;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

// 储存一个增广文法, 提供匹配token流的接口
public class Grammar {
    private enum ActionTag {
        Shift, // 移入
        Reduce,// 规约
        Accept,// 接受
        Error  // 错误
    }

    // Action表中的行为
    // 移动或规约的下标
    private record Action(ActionTag tag, int index) {
        @Override
        public String toString() {
            return switch (tag) {
                case Shift -> String.format("s%d", index);
                case Reduce -> String.format("r%d", index);
                case Accept -> "acc";
                case Error -> "";
            };
        }
    }

    // 产生式的项
    // product的id
    // ·所在的下标
    // 前向查看的搜索符(终结符)
    private record Item(Grammar belong, int pid, int dot, TokenTag lookahead) {
        @Override
        public String toString() {
            return String.format("[%s , %s]", belong.products.get(pid).toString(dot), lookahead);
        }
    }

    // 项集的别名
    private static class ItemSet extends HashSet<Item> {
        public ItemSet() {
            super();
        }

        public ItemSet(Collection<? extends Item> c) {
            super(c);
        }

        public static ItemSet one(Grammar ptr, int pid, int dot, TokenTag lookahead) {
            ItemSet set = new ItemSet();
            set.add(ptr.buildItem(pid, dot, lookahead));
            return set;
        }
    }

    // 增广文法
    private final List<Product> products;                                   // 产生式集合
    private final Map<String, List<Product>> prodGroup = new HashMap<>();   // 按照left分组的产生式
    // 语法分析表
    private final Action[][] actionTable;   // action表
    private final int[][] gotoTable;        // goto表
    private final int stateCount;           // 状态数(项集数,action&goto表的行数)
    private final int terminalCount;        // 终结符数(token类型数,action表的列数)
    private final int productCount;         // 非终结符数(product左端数,goto表的列数)
    // id映射表
    private final Map<String, Integer> productNameToId = new HashMap<>(); // 产生式名到id的映射
    private final Map<Product, Integer> productToId = new HashMap<>(); // 产生式到id的映射
    private final Map<TokenTag, Integer> tokenToId = new HashMap<>(); // token类型到id的映射
    private final Set<Product.Symbol> allSymbols = new HashSet<>();   // 全部文法符号的集合

    // 从一组产生式创建一个增广文法,并生成语法分析表
    // S为开始符号
    public Grammar(String S, List<Product> products) {
        // 创建增广文法,令S'作为新的开始符号,S'->S唯一的接受式
        Product G = new Product.ProductBuilder()
                .addOr(String.format("%s' |> %s", S, S))
                .buildOne();
        // 生成产生式列表,不共用列表的引用
        this.products = new ArrayList<>(1 + products.size());
        this.products.add(G);
        this.products.addAll(products);

        // 生成快速访问的映射表
        initMap();

        /// 生成语法分析表
        // 初始化需要的变量
        stateCount = initStateCount();
        terminalCount = initTerminalCount();
        productCount = initProductCount();
        actionTable = new Action[stateCount][terminalCount];
        gotoTable = new int[stateCount][productCount];
        // 填充分析表
        initAnalysis();
    }

    /// 初始化相关
    // 获取状态数(项集族数)
    private int initStateCount() {
        return calculateItems().size();
    }

    // 获取终结符数
    private int initTerminalCount() {
        return (int) allSymbols.stream().filter(Product.Symbol::isTerminal).count() + 1;
    }

    // 获取非终结符数
    private int initProductCount() {
        return productNameToId.size();
    }

    // 初始化语法分析表,G为增广的产生式
    /*
    输入 : 一个增广文法G'
    自动生成 :
    action表, goto表
    步骤 :
    1. 构建G'的规范LR(1)项集族C={I0,I1,...,In}
    注: 其中I0包含[S'->· S, $],即初始状态为0
    2. 根据Ii构造得到状态i, 以如下规则求action的第i行
    2.1. 如果[A->α·aβ, b]在Ii中, 且GOTO(Ii, a) = Ij 则action[i,a] = s(j)
    2.2. 如果[A->α·, a]在Ii中, A≠S' 则action[i,a] = r(id(A->α))
    2.3. 如果[S'->S·, $]在Ii中, 则action[i,$] = acc
    3. 对每个状态i,对每个非终结符, 如果GOTO(Ii, A) = Ij ,则goto[i,A] = j
    4. 2,3中未填充的空位都设置为error/-1
     */
    private void initAnalysis() {
        /// 4. 填充默认值
        // action填充null
        for (int i = 0; i < stateCount; i++) {
            for (int j = 0; j < terminalCount; j++) {
                actionTable[i][j] = null;
            }
        }
        // goto填充-1
        for (int i = 0; i < stateCount; i++) {
            for (int j = 0; j < productCount; j++) {
                gotoTable[i][j] = -1;
            }
        }

        /// 填充具体值
        // 1. 构造项集族
        List<ItemSet> C = calculateItems();
        // 在C中查询一个项集
        Function<ItemSet, Integer> findIj = s -> {
            for (int j = 0; j < stateCount; ++j) {
                if (s.equals(C.get(j))) {
                    return j;
                }
            }
            return -1;
        };
        // 遍历项集
        for (int i = 0; i < stateCount; ++i) {
            // 2. 对C(i)即Ii的每个项目item
            ItemSet Ii = C.get(i);
            for (Item item : Ii) {
                // 获取产生式A
                Product A = products.get(item.pid);
                // 格式为[A->α · a β , b]
                if (A.getSymbolCount() != item.dot) {
                    Product.Symbol a = A.get(item.dot);
                    // 2.1. a是终结符且 GOTO(Ii, a) == Ij , 则移入到状态j
                    if (a.isTerminal()) {
                        ItemSet go = calculateGoto(Ii, a);
                        int j = findIj.apply(go);
                        if (j != -1) setAction(i, tokenId(a.terminal()), s(j));
                    }
                }
                // 格式为[A->α · , a]
                else {
                    TokenTag a = item.lookahead;
                    // 2.2. A != S' , 规约到A -> α
                    if (item.pid != 0) {
                        setAction(i, tokenId(a), r(item.pid));
                    }
                    // 2.3. A == S' , 且a == $ , 成功匹配
                    else if (a == TokenTag.Eof) {
                        setAction(i, tokenId(TokenTag.Eof), a());
                    }
                }
            }
            // 3. 对每个非终结符A, 如果GOTO(Ii, A) = Ij ,则goto[i,A] = j
            for (Map.Entry<String, Integer> entry : productNameToId.entrySet()) {
                ItemSet go = calculateGoto(Ii, new Product.Symbol(entry.getKey(), null));
                int j = findIj.apply(go);
                if (j != -1) setGoto(i, entry.getValue(), j);
            }
        }
        // action null to error
        for (int i = 0; i < stateCount; i++) {
            for (int j = 0; j < terminalCount; j++) {
                if (actionTable[i][j] == null) {
                    actionTable[i][j] = e();
                }
            }
        }
    }

    private void setAction(int i, int j, Action action) {
        actionTable[i][j] = action;
    }

    private void setGoto(int i, int j, int k) {
        gotoTable[i][j] = k;
    }


    // 初始化映射表:productNameToId,productToId,tokenToId,productGroup,allSymbols
    private void initMap() {
        // 获取productName => id的映射
        for (Product p : products) {
            productNameToId.computeIfAbsent(p.getLeft(), k -> productNameToId.size());
        }
        // 获取product => id的映射
        for (Product p : products) {
            productToId.computeIfAbsent(p, k -> productToId.size());
        }
        // 获取token => id的映射
        for (Product p : products) {
            for (Product.Symbol symbol : p.getSymbols()) {
                if (symbol.isTerminal()) {
                    tokenToId.computeIfAbsent(symbol.terminal(), k -> tokenToId.size());
                }
            }
        }
        tokenToId.computeIfAbsent(TokenTag.Eof, k -> tokenToId.size());
        // 对产生式进行分组
        for (Product p : products) {
            prodGroup.computeIfAbsent(p.getLeft(), k -> new ArrayList<>()).add(p);
        }
        // 获取所有符号
        for (Product p : products) {
            allSymbols.addAll(p.getSymbols());
        }
    }

    // 解析token流 Todo 丰富报错信息
    public ASTNode parse(List<Token> tokens) {
        tokens = tokens.stream().filter(c -> c.getTag().needParse()).toList();
        // 符号栈
        Stack<Integer> statueStack = new Stack<>();
        statueStack.push(0); // 栈底标记
        // 符号栈
        Stack<ASTNode> tokensStack = new Stack<>();
        // 当前符号下标
        int cur = 0;
        while (true) {
            int s = statueStack.peek(); // 栈顶状态
            // 当前输入符号
            Token a = cur == tokens.size() ? Token.of(TokenTag.Eof, "") : tokens.get(cur);
            Action action = actionTable[s][tokenId(a.getTag())];
            if (action.tag() == ActionTag.Shift) {
                // 状态入栈
                statueStack.push(action.index());
                // 符号入栈
                tokensStack.push(new ASTNode(a));
                ++cur;
            } else if (action.tag() == ActionTag.Reduce) {
                // 获取产生式
                Product production = products.get(action.index());
                // 弹出|β|个ASNode和状态
                ASTNode[] nodes = new ASTNode[production.getSymbolCount()];
                for (int i = production.getSymbolCount() - 1; i >= 0; i--) {
                    nodes[i] = tokensStack.pop();
                    statueStack.pop();
                }
                // 规约A->β
                tokensStack.push(production.buildASNode(nodes));
                // 要跳到的状态下标
                int A = gotoTable[statueStack.peek()][productNameId(production.getLeft())];
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

    // 打印语法分析表
    public void printTable() {
        List<List<String>> table = new ArrayList<>();
        // action列
        List<TokenTag> terminals = new ArrayList<>(allSymbols.stream().
                filter(Product.Symbol::isTerminal).
                map(Product.Symbol::terminal).sorted(Comparator.comparingInt(Enum::ordinal)).toList());
        terminals.add(TokenTag.Eof);
        // goto列
        List<String> prods = allSymbols.stream().
                filter(c -> !c.isTerminal()).
                map(Product.Symbol::left).toList();
        // 表头: state action goto
        List<String> header = new ArrayList<>();
        header.add("");
        for (int i = 1; i < terminalCount; i++) header.add(null);
        header.add("action");
        for (int i = 2; i < productCount; i++) header.add(null);
        header.add("goto");
        table.add(header);
        // 表头细节
        header = new ArrayList<>();
        header.add("state");
        header.addAll(terminals.stream().map(TokenTag::getSymbol).toList());
        header.addAll(prods);
        table.add(header);
        // 表内容
        for (int i = 0; i < stateCount; i++) {
            List<String> row = new ArrayList<>();
            row.add(String.valueOf(i));
            for (TokenTag t : terminals) {
                row.add(String.valueOf(actionTable[i][tokenId(t)]));
            }
            for (String name : prods) {
                int val = gotoTable[i][productNameId(name)];
                row.add(String.valueOf(val == -1 ? "" : val));
            }
            table.add(row);
        }
        // 打印表
        PrintUtils.printTable(table);
    }

    // 打印项集族
    public void printItemSets() {
        List<List<String>> table = new ArrayList<>();
        int i = 0;
        for (ItemSet itemSet : calculateItems()) {
            int j = 0;
            for (Item item : itemSet) {
                List<String> row = new ArrayList<>();
                if (j++ == 0) row.add(String.valueOf(i++));
                else row.add("");
                row.add(item.toString());
                table.add(row);
            }
        }
        PrintUtils.printTable(table);
    }

    /// 辅助函数
    // 获取token在action表中的下标
    private int tokenId(TokenTag terminal) {
        Integer id = tokenToId.get(terminal);
        if (id == null) {
            throw new RuntimeException(String.format("未知的终结符: %s", terminal));
        } else {
            return id;
        }
    }

    // 获取product在item中下标
    private int productId(Product product) {
        Integer id = productToId.get(product);
        if (id == null) {
            throw new RuntimeException(String.format("未知的产生式: %s", product));
        } else {
            return id;
        }
    }

    private int productNameId(String left) {
        Integer id = productNameToId.get(left);
        if (id == null) {
            throw new RuntimeException("product not found");
        } else {
            return id;
        }
    }

    /// 分析表构造过程中相关的函数
    // first函数,求文法符号串r的first集
    Set<TokenTag> calculateFirst(List<Product.Symbol> symbols) {
        class ResultCache {
            public static final Map<List<Product.Symbol>, Set<TokenTag>> cache = new HashMap<>();
        }
        Set<TokenTag> cache = ResultCache.cache.get(symbols);
        if (cache != null) return cache;

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
                        for (Product prod : getProducts(s.left())) {
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
                boolean hasEpsilon = true;
                for (Product.Symbol node : symbols) {
                    hasEpsilon = false; // 重置
                    // 前面的Symbol都能推导出epsilon
                    if (node.isTerminal()) {
                        // 遇到终结符,则直接添加
                        result.add(node.terminal());
                    } else if (!inFirst.contains(node.left())) {
                        // 合并node可能的产生式的first集
                        for (Product prod : getProducts(node.left())) {
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

        ResultCache.cache.put(symbols, result);
        return result;
    }

    // closure函数,求项集的闭包,返回项集
    ItemSet calculateClosure(ItemSet items) {
        class ResultCache {
            public static final Map<ItemSet, ItemSet> cache = new HashMap<>();
        }
        ItemSet cache = ResultCache.cache.get(items);
        if (cache != null) return cache;

        // 结果集
        ItemSet result = new ItemSet(items);
        // 使用bfs求闭包,等待迭代的项集
        Queue<Item> queue = new LinkedList<>(items);
        while (!queue.isEmpty()) {
            // 取出当前项
            Item item = queue.poll();
            //System.out.println("当前项: " + item.toString(this));
            // 获取项对应的产生式,需要[A->α · B β , a]格式
            // α β为可空 结点列表,B为非终结符,a为终结符(输入的token流)
            Product A = products.get(item.pid);
            // 点在最后,为规约项,跳过(即B为空)
            if (item.dot == A.getSymbolCount()) continue;
            // 所在位置的结点(即B)
            Product.Symbol B = A.get(item.dot);
            // 为终结符,跳过
            if (B.isTerminal()) continue;

            // 构造symbol序列 βα
            List<Product.Symbol> beta = A.getSymbols().subList(item.dot + 1, A.getSymbolCount());
            List<Product.Symbol> ba = new ArrayList<>(beta);
            ba.add(new Product.Symbol(null, item.lookahead));
            // 计算βα的first集
            Set<TokenTag> first = calculateFirst(ba);

            // 获取每个产生式[B -> γ] , γ为symbol列表
            for (Product By : getProducts(B.left())) {
                // 对First(βα)中的每个终结符b,将[B -> · γ, b]加入到项集中
                for (TokenTag b : first) {
                    if (TokenTag.Epsilon == b) b = TokenTag.Eof;
                    // 构造项[B -> · γ, b]
                    Item add = buildItem(productId(By), 0, b);
                    // 如果不在结果集中,则加入到队列中参与层序遍历
                    if (result.add(add)) {
                        queue.add(add);
                    }
                }
            }
        }

        ResultCache.cache.put(items, result);
        return result;
    }

    // goto函数,求项集=>symbol的项集闭包
    ItemSet calculateGoto(ItemSet items, Product.Symbol symbol) {
        class ResultCache {
            public static final Map<Tuple2<ItemSet, Product.Symbol>, ItemSet> cache = new HashMap<>();
        }
        ItemSet cache = ResultCache.cache.get(Tuple.of(items, symbol));
        if (cache != null) return cache;

        // 结果集
        ItemSet result = new ItemSet();
        // 遍历每个项目 : [A -> α · X β, a]
        // 将[A -> α X · β, a] 加入到结果集中
        // 实际上是求[A -> α · X β, a]在输入为X的情况下,可以推导出的项集
        for (Item item : items) {
            Product A = products.get(item.pid);
            // X为空,跳过
            if (item.dot == A.getSymbolCount()) continue;
            // 获取X并和symbol比较,跳过不匹配的X
            Product.Symbol X = A.get(item.dot);
            if (!X.equals(symbol)) continue;
            // 将dot的位置加一,即[A -> α X · β, a]
            result.add(buildItem(item.pid, item.dot + 1, item.lookahead));
        }

        result = calculateClosure(result);
        ResultCache.cache.put(Tuple.of(items, symbol), result);
        return result;
    }

    // items函数,求LR(1)项集族,I0必须包含[S'->· S, $]
    List<ItemSet> calculateItems() {
        // 结果集
        List<ItemSet> result = new ArrayList<>();
        Set<ItemSet> used = new HashSet<>(); // 去重
        // 层序遍历
        Queue<ItemSet> queue = new LinkedList<>();
        // 初始化第一个项集作为I0 : closure({[S' -> · S, $]}
        ItemSet I0 = calculateClosure(ItemSet.one(this, 0, 0, TokenTag.Eof));
        result.add(I0);
        used.add(I0);
        queue.add(I0);
        while (!queue.isEmpty()) {
            // 取出当前项集I
            ItemSet I = queue.poll();
            // 对每个文法符号X
            for (Product.Symbol X : allSymbols) {
                // 求GOTO(I, X) , 如果非空则加入到queue中
                ItemSet go = calculateGoto(I, X);
                if (!go.isEmpty() && !used.contains(go)) {
                    // 参与层序遍历
                    queue.add(go);
                    // 加入到结果集中
                    result.add(go);
                    // 防止重复加入
                    used.add(go);
                }
            }
        }

        return result;
    }

    /// action的辅助构造器
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

    /// item的辅助构造器
    private Item buildItem(int pid, int dot, TokenTag lookahead) {
        return new Item(this, pid, dot, lookahead);
    }

    /// 获取Product[]
    private List<Product> getProducts(String left) {
        List<Product> ret = prodGroup.get(left);
        if (ret == null) {
            throw new RuntimeException("没有找到" + left + "的产生式");
        } else {
            return ret;
        }
    }

    public static void main(String[] args) {
        // 测试语法
        Grammar grammar1 = new Grammar("E", Arrays.stream(new Product.ProductBuilder()
                // 设置tag别名
                .alias("id", TokenTag.Identifier)
                .alias("int", TokenTag.IntLiteral)
                .alias("float", TokenTag.FloatLiteral)
                .alias("=", TokenTag.Assign)
                .alias("+", TokenTag.Add)
                .alias("*", TokenTag.Mut)
                .alias("-", TokenTag.Sub)
                .alias("/", TokenTag.Div)
                .alias("(", TokenTag.L1)
                .alias(")", TokenTag.R1)
                // 任意个顶级表达式相乘的表达式相加
                .addAnd("E -> E + T")
                .addAnd("E -> T")
                // 任意个顶级表达式相乘
                .addAnd("T -> T * F")
                .addAnd("T -> F")
                // 用括号提升成顶级表达式
                .addAnd("F -> ( E )")
                // 单个变量/数字就是一个顶级运算单元
                .addOr("F |> id int float")
                // 赋值操作
                .addAnd("F -> id = E")
                .build()).toList());

        // https://haihong.blog.csdn.net/article/details/105597613
        Grammar grammar2 = new Grammar("S", Arrays.stream(new Product.ProductBuilder()
                .alias("id", TokenTag.Identifier)
                .alias("*", TokenTag.Mut)
                .alias("=", TokenTag.Assign)
                .addAnd("S -> L = R")
                .addAnd("S -> R")
                .addAnd("L -> * R")
                .addAnd("L -> id")
                .addAnd("R -> L")
                .build()).toList());

        // grammar1.printTable();
        grammar2.printTable();
        grammar2.printItemSets();

        SystemUtils.consoleLoopLine(str -> {
            List<Token> tokens = new LexParser().parse(str);
            ASTNode node = grammar1.parse(tokens);
            String savePath = "./.cache/out.png";
            PaintUnits.paintTree(node, savePath);
            System.out.printf("语法树图片已经保存在%s中\n", savePath);
        });
    }
}
