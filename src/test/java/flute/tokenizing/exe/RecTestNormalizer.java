package flute.tokenizing.exe;

import flute.tokenizing.excode_data.RecTest;

public class RecTestNormalizer {
    public static void normalize(RecTest test) {
        switch (test.getExpected_excode()) {
            case "LIT(wildcard)":
                test.setExpected_lex("?");
                break;
            case "LIT(null)":
                test.setExpected_lex("null");
                break;
            case "LIT(num)":
                test.setExpected_lex("0");
                break;
            case "LIT(String)":
                test.setExpected_lex("\"\"");
                break;
            case "VAR(Class)":
                test.setExpected_lex(".class");
        }
    }
}
