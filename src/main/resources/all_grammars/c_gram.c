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

