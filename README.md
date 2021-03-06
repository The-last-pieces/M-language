# M语言解释器

借(chao)鉴(xi)各家语言的特点,构建了一个简单易用的解释器.

支持脚本运行和交互式运行.

# 运行截图

## 交互执行

![](assets/demo.png)

## 绘制语法树

![](assets/gram_tree.png)

# 语言特性

画饼ing(

## 标识符

由字母,数字,下划线组成,且不能以数字开头.

## 模块

一组函数和变量的集合,模块名由多个标识符组成,用点号(.)分隔.

每个文件都是必须是一个模块,在开头使用绝对路径声明.

### 内置模块

- 系统(sys)
    - 输入输出(io)
        - 文件(file)
        - 控制台(console)
        - 网络(net)
    - 时间(time)
- 数学(math)
- 算法(algorithm)

## 作用域

用来定位标识符的位置.

global/class/function/block

## 函数

支持重载,闭包,lambda表达式.

运算符是一种内置的函数,可重载.

类型转换是一种内置的运算符,可重载,可指定隐式/显式转换.

## 变量

所有变量/形参/返回值都需要显式指定类型.

除了object均以值传递.

### 布尔(bool)

表示真假的类型.

### 数字(number)

兼容整数和浮点数的类型.

### 字符串(string)

支持内插(format),原始(raw)字符串.

### 数组(array)

一组连续的变量.

### 函数(function)

函数是一种特殊的变量.

### 对象(object)

key-value结构的变量,用户自定义的类型均继承于此.

#### 继承

只支持单继承,无访问权限控制,无虚函数.

## 注释

通过`//`标记单行注释.

通过`/* */`标记多行注释.

## 控制语句

if/else/while/for/condition

# 关键字

内置的关键字无法作为关键字使用.

# 项目架构

~~使用两遍扫描, 第一遍扫描所有模块并维护所有符号表, 第二遍构建具体语法树.~~

## 词法分析

识别所有token

## 语法分析

将token转为语法树

## 语义分析

对语义的合法性进行检测(主要为类型检查) , 并进行适当修改

## 语法树优化

主要为进行编译期求值 , 减小语法树大小

## 语法树执行

维护符号表&函数表,依次执行所有节点

# 文法格式

见/resource/all_grammars .

目前只编写了一个类C语法用于测试 , 见下 .

```c
// 顶级单元
root_unit |> stmt_seq expr $

/// 语句
stmt |> not_if_stmt
        if_matched_stmt
        if_open_stmt $

// 除了两个特殊if语句之外的其他语句
not_if_stmt |> expr_stmt
               compound_stmt
               while_stmt $

// 语句列表
stmt_seq -> stmt_seq stmt $
stmt_seq |> stmt e $

// 表达式语句
expr_stmt -> expr ; $
expr_stmt -> ; $

// 复合语句
compound_stmt -> { stmt_seq } $

// 循环语句
while_stmt -> while parent_expr stmt $

// 条件语句(匹配了else)
if_matched_stmt -> if parent_expr if_matched_stmt else if_matched_stmt $
if_matched_stmt |> not_if_stmt $

// 条件语句(未匹配else)
if_open_stmt -> if parent_expr stmt $
if_open_stmt -> if parent_expr if_matched_stmt else if_open_stmt $

/// 表达式(优先级从上往下依次降低)
expr        |> ass_expr $
expr_seq    -> expr_seq , expr $
expr_seq    |> expr e $

/// 优先级最高的表达式 [] . ()

// 可赋值的表达式
ass_able |> id field_expr sub_expr $
// -字段访问表达式
field_expr -> obj_expr . id $
// -数组下标表达式
sub_expr -> obj_expr [ expr ] $
// --对象表达式
obj_expr |> ass_able call_expr literal_expr parent_expr $
// ---函数调用表达式
call_expr   -> obj_expr ( expr_seq ) $
// ---括号表达式
parent_expr -> ( expr ) $
// ---字面量表达式
literal_expr |> ii fi si bi $

/// 一元运算符(算术,逻辑,位) + - ! ~ ++ --
unary_expr -> unary_op unary_expr $
unary_expr |> obj_expr $
unary_op   |> + - ! ~ ++ -- $
/// 二元运算符(算术) * / % **
binary_expr_* -> binary_expr_* binary_op* unary_expr $
binary_expr_* |> unary_expr $
binary_op*    |> * / % ** $

/// 二元运算符(算术) + -
binary_expr_+ -> binary_expr_+ binary_op+ binary_expr_* $
binary_expr_+ |> binary_expr_* $
binary_op+    |> + - $

/// 二元运算符(位) << >>
binary_expr_<< -> binary_expr_<< binary_op<< binary_expr_+ $
binary_expr_<< |> binary_expr_+ $
binary_op<<    |> << >> $

/// 二元运算符(比较) <= >= < >
binary_expr_<= -> binary_expr_<= binary_op<= binary_expr_<< $
binary_expr_<= |> binary_expr_<< $
binary_op<=    |> <= >= < > $

/// 二元运算符(比较) == !=
binary_expr_== -> binary_expr_== binary_op== binary_expr_<= $
binary_expr_== |> binary_expr_<= $
binary_op==    |> == != $

/// 二元运算符(位) &
binary_expr_& -> binary_expr_& binary_op& binary_expr_== $
binary_expr_& |> binary_expr_== $
binary_op&    |> & $

/// 二元运算符(位) ^
binary_expr_^ -> binary_expr_^ binary_op^ binary_expr_& $
binary_expr_^ |> binary_expr_& $
binary_op^    |> ^ $

/// 二元运算符(位) |
binary_expr_| -> binary_expr_| binary_op| binary_expr_^ $
binary_expr_| |> binary_expr_^ $
binary_op|    |> | $

/// 二元运算符(逻辑) &&
binary_expr_&& -> binary_expr_&& binary_op&& binary_expr_| $
binary_expr_&& |> binary_expr_| $
binary_op&&    |> && $

/// 二元运算符(逻辑) ||
binary_expr_|| -> binary_expr_|| binary_op|| binary_expr_&& $
binary_expr_|| |> binary_expr_&& $
binary_op||    |> || $

/// 条件表达式
cond_expr -> binary_expr_|| ? binary_expr_|| : binary_expr_|| $
cond_expr |> binary_expr_|| $

/// 赋值表达式
ass_expr -> ass_able ass_op ass_expr $
ass_expr |> cond_expr $
ass_op   |> = += -= *= /= %= **= <<= >>= &= ^= |= $
```
