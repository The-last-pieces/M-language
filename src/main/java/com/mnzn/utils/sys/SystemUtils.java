package com.mnzn.utils.sys;

import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Function;

// 系统相关的工具类
public class SystemUtils {
    public static <T> void consoleLoop(Consumer<T> consumer, Function<Scanner, T> maker) {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            try {
                T value = maker.apply(scanner);
                consumer.accept(value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void consoleLoopLine(Consumer<String> consumer) {
        consoleLoop(consumer, Scanner::nextLine);
    }

    // 输入长字符串直到指定的终结符
    public static void consoleLoopUntil(Consumer<String> consumer, String terminal) {
        Scanner scanner = new Scanner(System.in);
        StringBuilder sb = new StringBuilder();

        while (true) {
            try {
                String line = scanner.nextLine();
                sb.append(line).append("\n");
                // 尝试从尾部开始搜索
                int idx = sb.lastIndexOf(terminal);
                if (idx != -1) {
                    consumer.accept(sb.substring(0, idx));
                    sb.delete(0, idx + terminal.length());
                }
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }
}
