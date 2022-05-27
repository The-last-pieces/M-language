package com.mnzn.lex.imp;

import com.mnzn.lex.Token;
import com.mnzn.lex.TokenTag;
import lombok.Getter;

import java.util.Map;

public class TokenString extends Token {
    @Getter
    private final String value;

    public TokenString(String value) {
        super(TokenTag.StringLiteral);
        this.value = buildFrom(value);
    }

    @Override
    public String toString() {
        return String.format("<%s, %s>", name(), value);
    }

    private static String buildFrom(String value) {
        StringBuilder sb = new StringBuilder();
        // 移除首尾引号, 并以\作为转义标识符进行转义
        int end = value.length() - 1;
        for (int i = 1; i < end; i++) {
            char c = value.charAt(i);
            if (c == '\\') {
                c = value.charAt(++i); // Token匹配的时候会确保\不是最后一个字符
                Character rep = castMap.get(c);
                if (rep == null) {
                    throw new IllegalArgumentException("Invalid escape sequence: " + value);
                }
                sb.append(rep.charValue());
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static final Map<Character, Character> castMap = Map.of(
            '"', '\"',  //双引号
            '\\', '\\',     //反斜杠字符
            'n', '\n',      //换行
            'r', '\r',      //回车
            't', '\t',      //水平制表符
            'b', '\b',      //退格
            'f', '\f'           //换页
    );
}
