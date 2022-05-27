package com.mnzn.lex.imp;

import com.mnzn.lex.Token;
import com.mnzn.lex.TokenTag;
import lombok.Getter;

// 标识符Token
public class TokenId extends Token {
    @Getter
    private final String id;

    public TokenId(String name) {
        super(TokenTag.Identifier);
        this.id = name;
    }

    @Override
    public String toString() {
        return String.format("<%s, %s>", name(), id);
    }
}
