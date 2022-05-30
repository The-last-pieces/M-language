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
}
