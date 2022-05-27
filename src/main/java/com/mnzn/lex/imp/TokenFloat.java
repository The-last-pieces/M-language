package com.mnzn.lex.imp;

import com.mnzn.lex.Token;
import com.mnzn.lex.TokenTag;
import lombok.Getter;

public class TokenFloat extends Token {
    @Getter
    private final float value;

    public TokenFloat(String value) {
        super(TokenTag.FloatLiteral);
        this.value = Float.parseFloat(value);
    }

    @Override
    public String toString() {
        return String.format("<%s, %f>", name(), value);
    }
}
