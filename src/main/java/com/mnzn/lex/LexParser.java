package com.mnzn.lex;

import io.vavr.Tuple2;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

// 词法解析器,使用有限自动机解析所有Token
public class LexParser {
    private int line = 1, col = 1;

    private void updateCursor(String value) {
        for (char c : value.toCharArray()) {
            if (c == '\n') {
                ++line;
                col = 1;
            } else {
                ++col;
            }
        }
    }

    public List<Token> parse(String str) {
        List<Token> tokens = new ArrayList<>();
        int start = 0;
        try {
            while (start < str.length()) {
                // 获取最优匹配
                Tuple2<TokenTag, Integer> best = TokenTag.bestMatch(str, start);
                // 构造Token
                String value = str.substring(start, best._2);
                tokens.add(Token.of(best._1, value));
                // 匹配下一个位置, 更新游标
                start = best._2;
                updateCursor(value);
            }
        } catch (Exception e) {
            throw buildException(e.getMessage());
        }
        return tokens;
    }

    public List<Token> parseFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            // Todo
            throw new RuntimeException(String.format("file %s not exists", filePath));
        }
        String str;

        try (FileInputStream inputStream = new FileInputStream(file)) {
            str = new String(inputStream.readAllBytes());
        } catch (Exception e) {
            e.printStackTrace();
            // Todo
            throw new RuntimeException(String.format("read file %s error", filePath));
        }
        return parse(str);
    }

    private RuntimeException buildException(String msg) {
        // Todo
        return new RuntimeException(String.format("\nfail at line %d , col %d \n error : \n%s\n", line, col, msg));
    }
}
