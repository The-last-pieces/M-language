package com.mnzn.utils.visual.console;

import com.mnzn.lex.Token;
import com.mnzn.lex.TokenTag;

import java.util.List;

// 打印有颜色的Token序列
public class PrintTokenUnits {
    public static void printTokens(List<Token> tokens) {
        for (Token token : tokens) {
            if (token.getTag() != TokenTag.Nop)
                System.out.printf("match : %s\n", token);
        }
    }

    public static void printColorTokens(List<Token> tokens) {
        for (Token token : tokens) {
            System.out.print(PrintConfig.build(token.getRaw(), token.getColor()));
        }
        System.out.println();
    }
}
