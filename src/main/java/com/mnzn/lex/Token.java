package com.mnzn.lex;

import com.mnzn.lex.imp.*;
import com.mnzn.utils.visual.console.PrintConfig;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import lombok.Getter;

// 不可分割的最小单元
public class Token {
    @Getter
    private final TokenTag tag;
    @Getter
    private String raw;
    @Getter
    private PrintConfig[] color; // Todo 着色策略

    public Token(TokenTag tag) {
        this.tag = tag;
    }

    public String name() {
        return tag.name();
    }

    @Override
    public String toString() {
        if (tag.isPure()) {
            return String.format("<%s, '%s'>", name(), tag.getPure());
        } else {
            return String.format("<%s>", name());
        }
    }

    public static Token of(TokenTag tag, String value) {
        Tuple2<Token, PrintConfig[]> ret = builder(tag, value);
        ret._1.raw = value;
        ret._1.color = ret._2;
        return ret._1;
    }

    private static Tuple2<Token, PrintConfig[]> builder(TokenTag tag, String value) {
        if (tag.isPure() || tag.isNone()) {
            return Tuple.of(new Token(tag), new PrintConfig[]{
                    PrintConfig.RED, PrintConfig.BLUE // 紫色
            });
        } else {
            return switch (tag) {
                case Identifier -> Tuple.of(
                        new TokenId(value),
                        new PrintConfig[]{

                        }
                );
                case BoolLiteral -> Tuple.of(
                        new TokenBool(value),
                        new PrintConfig[]{
                                PrintConfig.RED, PrintConfig.BLUE
                        }
                );
                case IntLiteral -> Tuple.of(
                        new TokenInt(value),
                        new PrintConfig[]{PrintConfig.YELLOW}
                );
                case FloatLiteral -> Tuple.of(
                        new TokenFloat(value),
                        new PrintConfig[]{PrintConfig.YELLOW}
                );
                case StringLiteral -> Tuple.of(
                        new TokenString(value),
                        new PrintConfig[]{
                                PrintConfig.GREEN
                        }
                );
                case Nop -> Tuple.of(
                        new Token(tag),
                        new PrintConfig[]{
                                PrintConfig.GREY
                        }
                );
                // Todo
                default -> throw new IllegalArgumentException("unexpected token terminal: " + tag);
            };
        }
    }
}
