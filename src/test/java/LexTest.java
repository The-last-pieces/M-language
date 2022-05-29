import com.mnzn.lex.LexParser;
import com.mnzn.lex.Token;
import com.mnzn.utils.visual.console.PrintUtils;

import java.util.List;
import java.util.Scanner;

public class LexTest {
    public static void main(String[] args) {
        testFile();
    }

    private static void testFile() {
        LexParser lexParser = new LexParser();
        String str = "./test_script/test.m";
        List<Token> tokens = lexParser.parseFile(str);
        PrintUtils.printTokens(tokens);
        PrintUtils.printColorTokens(tokens);
    }

    private static void testConsole() {
        LexParser lexParser = new LexParser();
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String str = scanner.nextLine();
            List<Token> tokens = lexParser.parse(str);
            PrintUtils.printTokens(tokens);
            PrintUtils.printColorTokens(tokens);
        }
    }
}
