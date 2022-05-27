package com.mnzn.lex.imp;

import com.mnzn.lex.Token;
import com.mnzn.lex.TokenTag;
import lombok.Getter;

public class TokenBool extends Token {
    @Getter
    private final boolean value;

    public TokenBool(String value) {
        super(TokenTag.BoolLiteral);
        this.value = Boolean.parseBoolean(value);
    }

    @Override
    public String toString() {
        return String.format("<%s, %s>", name(), value);
    }
}
