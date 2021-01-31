package flute.tokenizing.exe;

import flute.tokenizing.excode_data.ArgRecTest;
import flute.tokenizing.excode_data.MethodCallNameRecTest;
import flute.tokenizing.excode_data.MultipleArgRecTest;
import flute.tokenizing.excode_data.RecTest;

public class RecTester {
    public static boolean canAcceptGeneratedExcodes(RecTest test) {
        if (test instanceof ArgRecTest) {
            return canAcceptGeneratedExcodes((ArgRecTest) test);
        }
        if (test instanceof MultipleArgRecTest) {
            return canAcceptGeneratedExcodes((MultipleArgRecTest) test);
        }
        if (test instanceof MethodCallNameRecTest) {
            return canAcceptGeneratedExcodes((MethodCallNameRecTest) test);
        }
        return false;
    }

    public static boolean canAcceptGeneratedLexes(RecTest test) {
        if (test instanceof ArgRecTest) {
            return canAcceptGeneratedLexes((ArgRecTest) test);
        }
        if (test instanceof MultipleArgRecTest) {
            return canAcceptGeneratedLexes((MultipleArgRecTest) test);
        }
        if (test instanceof MethodCallNameRecTest) {
            return canAcceptGeneratedLexes((MethodCallNameRecTest) test);
        }
        return false;
    }

    public static boolean canAcceptGeneratedExcodes(ArgRecTest test) {
        String expectedExcode = test.getExpected_excode();
        if (test.getNext_excode().contains(expectedExcode)) return true;

        //TODO: Handle unknown excode
        if (expectedExcode.contains("<unk>")) return true;

        if (test.getMethodAccessExcode() != null) {
            if (test.getNext_excode().contains(test.getMethodAccessExcode())) return true;
        }

        if (test.getObjectCreationExcode() != null) {
            return test.getNext_excode().contains(test.getObjectCreationExcode());
        }

        return false;
    }

    public static boolean canAcceptGeneratedExcodes(MultipleArgRecTest test) {
        for (ArgRecTest oneArgTest: test.getArgRecTestList())
            if (!canAcceptGeneratedExcodes(oneArgTest)) return false;
        return true;
    }

    public static boolean canAcceptGeneratedLexes(ArgRecTest test) {
        String expectedLex = test.getExpected_lex();
        if (expectedLex.contains(".this")) {
            expectedLex = expectedLex.substring(expectedLex.indexOf("this"));
        }

        if (test.getNext_lexList().contains(expectedLex)) return true;
        if (expectedLex.startsWith("this.")) {
            if (test.getNext_lexList().contains(expectedLex.substring(5))) return true;
        } else {
            if (test.getNext_lexList().contains("this." + expectedLex)) return true;
        }

        if (test.getMethodAccessLex() != null) {
            if (test.getNext_lexList().contains(test.getMethodAccessLex())) return true;
        }

        if (test.getObjectCreationLex() != null) {
            return test.getNext_lexList().contains(test.getObjectCreationLex());
        }

        return false;
    }

    public static boolean canAcceptGeneratedLexes(MultipleArgRecTest test) {
        for (ArgRecTest oneArgTest: test.getArgRecTestList())
            if (!canAcceptGeneratedLexes(oneArgTest)) return false;
        return true;
    }

    public static boolean canAcceptResult(ArgRecTest test, String result) {
        String expectedLex = test.getExpected_lex();
        if (expectedLex.contains(".this")) {
            expectedLex = expectedLex.substring(expectedLex.indexOf("this"));
        }

        if (result.equals(expectedLex)) return true;
        if (expectedLex.startsWith("this.")) {
            if (result.equals(expectedLex.substring(5))) return true;
        } else {
            if (result.equals("this." + expectedLex)) return true;
        }

        if (test.getMethodAccessLex() != null) {
            if (result.equals(test.getMethodAccessLex())) return true;
        }

        if (test.getObjectCreationLex() != null) {
            return result.equals(test.getObjectCreationLex());
        }

        return false;
    }

    public static boolean canAcceptResult(MultipleArgRecTest test, String result) {
        int i = -1;
        for (ArgRecTest oneArgTest: test.getArgRecTestList()) {
            StringBuilder sb = new StringBuilder();
            int bal = 0;
            while (++i < result.length()) {
                char c = result.charAt(i);
                if (c == '(') ++bal;
                if (c == ')') --bal;
                if (bal == 0 && c == ' ') continue;
                if (bal == 0 && c == ',') break;
                sb.append(c);
            }
            if (!canAcceptResult(oneArgTest, sb.toString())) return false;
        }
        return true;
    }

    public static boolean canAcceptGeneratedExcodes(MethodCallNameRecTest test) {
        String expectedExcode = test.getExpected_excode();
        if (test.getMethod_candidate_excode().contains(expectedExcode)) return true;

        //TODO: Handle unknown excode
        if (expectedExcode.contains("<unk>")) {
            String expectedNumParam = test.getExpected_excode().split(",")[2];
            for (String methodExcode: test.getMethod_candidate_excode()) {
                if (!methodExcode.split(",")[1].equals(test.getExpected_lex())) continue;
                if (!methodExcode.split(",")[2].equals(expectedNumParam)) continue;
                return true;
            }
        }

        return false;
    }

    public static boolean canAcceptGeneratedLexes(MethodCallNameRecTest test) {
        String expectedLex = test.getExpected_lex();
        if (test.getMethod_candidate_lexList().contains(expectedLex)) return true;

        return false;
    }

    public static boolean canAcceptResult(MethodCallNameRecTest test, String result) {
        String expectedExcode = test.getExpected_excode();
        if (result.equals(expectedExcode)) return true;

        //TODO: Handle unknown excode
        if (expectedExcode.contains("<unk>")) {
            String expectedNumParam = test.getExpected_excode().split(",")[2];
            if (!result.split(",")[1].equals(test.getExpected_lex())) return false;
            if (!result.split(",")[2].equals(expectedNumParam)) return false;
            return true;
        }

        return false;
    }
}
