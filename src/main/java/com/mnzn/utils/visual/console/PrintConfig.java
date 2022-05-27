package com.mnzn.utils.visual.console;

import java.util.Arrays;

// 控制台打印配置
public enum PrintConfig {
    BLACK(30), // 黑色
    BLACK_BACKGROUND(40), // 黑色背景
    RED(31), // 红色
    RED_BACKGROUND(41), // 红色背景
    GREEN(32), // 绿色
    GREEN_BACKGROUND(42), // 绿色背景
    YELLOW(33), // 黄色
    YELLOW_BACKGROUND(43), // 黄色背景
    BLUE(34), // 蓝色
    BLUE_BACKGROUND(44), // 蓝色背景
    MAGENTA(35), // 品红（洋红）
    MAGENTA_BACKGROUND(45), // 品红背景
    CYAN(36), // 蓝绿
    CYAN_BACKGROUND(46), // 蓝绿背景
    GREY(37), // 灰色
    GREY_BACKGROUND(47), // 灰色背景
    BOLD(1), // 粗体
    ITALIC(3), // 斜体
    UNDERLINE(4); // 下划线

    private static final String SEMICOLON = ";";
    private final int code;

    PrintConfig(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    static String build(String str, PrintConfig... codes) {
        String codeStr = String.join(SEMICOLON,
                Arrays.stream(codes).map((printCode) -> String.valueOf(printCode.getCode()))
                        .toArray(String[]::new));
        return (char) 27 + "[" + codeStr + "m" + str + (char) 27 + "[0m";
    }
}
