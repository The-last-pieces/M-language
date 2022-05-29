package com.mnzn.utils.visual.console;

import com.mnzn.lex.Token;
import com.mnzn.lex.TokenTag;
import de.vandermeer.asciitable.AsciiTable;

import java.util.List;

public class PrintUtils {
    // 打印Token匹配结果
    public static void printTokens(List<Token> tokens) {
        for (Token token : tokens) {
            if (token.getTag() != TokenTag.Nop)
                System.out.printf("match : %s\n", token);
        }
    }

    // 打印有颜色的Token序列
    public static void printColorTokens(List<Token> tokens) {
        for (Token token : tokens) {
            System.out.print(PrintConfig.build(token.getRaw(), token.getColor()));
        }
        System.out.println();
    }

    // 打印表格
    public static void printTable(List<List<String>> table) {
        AsciiTable at = new AsciiTable();
        for (List<String> row : table) {
            at.addRule();
            at.addRow(row);
        }
        at.addRule();
        System.out.println(at.render());
    }

    public static void main(String[] args) {
        List<List<String>> table = List.of(
                List.of("a", "b", "c"),
                List.of("d", "e", "f"),
                List.of("g", "h", "i")
        );
        PrintUtils.printTable(table);
    }
}
