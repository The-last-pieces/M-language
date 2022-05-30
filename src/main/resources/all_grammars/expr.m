// 计算完+的表达式进行赋值运算(右结合)
expr    -> id = expr+
1[0,2]
expr    |> expr+
// 计算完*的表达式进行+-运算
expr+   -> expr+ op+ expr*
1[0,2]
expr+   |> expr*
op+     |> + -
// 计算完顶级元素的表达式进行*/**%运算
expr*   -> expr* op* value
1[0,2]
expr*   |> value
op*     |> * / ** %
// 字面量/标识符/()包裹的表达式
value   -> ( expr )
1[0,2]
value   |> id bi ii fi si
value   -> op+ value
0[1]
// 函数调用也是一个value
value   -> id ( exprs )
0[2]
exprs   -> exprs , expr
1[0,2]
exprs   |> expr e
