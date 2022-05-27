package com.mnzn.lex.imp;

import com.mnzn.lex.Token;
import com.mnzn.lex.TokenTag;
import lombok.Getter;

public class TokenInt extends Token {
    @Getter
    private final int value;

    public TokenInt(String value) {
        super(TokenTag.IntLiteral);
        this.value = Integer.parseInt(value);
    }

    @Override
    public String toString() {
        return String.format("<%s, %d>", name(), value);
    }
}
