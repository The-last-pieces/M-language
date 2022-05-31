// 模块
/*
文法:
module ->   import module_name
        |   import module_name as id
        |   import module_name.*
module_name -> id | id.module_name
*/
import sys.io.console.*

// 变量(支持自动推导类型)
/*
文法:
declare ->  let id = expr
        |   let id : type = expr
type -> int | float | bool | string | type[]
*/
let a : string = "hello";
let b : int  = 10;
let c : float = 10.0;
let d : bool = true;
let e : int[] = [1,2,3]
let f : object = {"name":"Tom" , "age":20};

// 自定义类
class Person : object {
    name : string;
    age : int;
    constructor(name : string, age : int) {
        this.name = name;
        this.age = age;
    }
}

// 函数(参数类型必须指定)
/*
文法:
fun -> func id(args) : type { stmts }
args -> e
      | id:type args
// 单个语句
stmt -> declare
     |  expr;
     |  if(expr) stmt
     |  if(expr) stmt else stmt
     |  {stmts}
     |
// 一组语句
stmts -> stmts stmt
       | e
*/
func test(a : string, b : int) : int {
    return a.length + b;
}

// 控制语句
for(let i = 0; i < 10; i++) {
    if(i == 1) continue;
    if(i == 9) break;
    else {
        switch(i) {
            case 1:
                print("1");
                break;
            case 2:
                print("2");
                break;
            default:
                print("default");
        }
    }
}
