package flute.tokenizing.exe;

import flute.analysis.ExpressionType;
import flute.jdtparser.PublicStaticMember;
import flute.testing.CandidateMatcher;
import flute.tokenizing.excode_data.ArgRecTest;
import flute.tokenizing.excode_data.MethodCallNameRecTest;
import flute.tokenizing.excode_data.MultipleArgRecTest;
import flute.tokenizing.excode_data.RecTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        List<String> candidates = new ArrayList<>();
        for (PublicStaticMember publicStaticCandidate: test.getPublicStaticCandidateList()) {
            candidates.add(publicStaticCandidate.excode);
        }
        if (candidates.contains(expectedExcode)) return true;

        //TODO: Handle unknown excode
        if (expectedExcode.contains("<unk>")) return true;

        if (test.getMethodAccessExcode() != null) {
            if (test.getNext_excode().contains(test.getMethodAccessExcode())) return true;
        }

        if (test.getObjectCreationExcode() != null) {
            if (test.getNext_excode().contains(test.getObjectCreationExcode())) return true;
        }

        return false;
    }

    public static boolean canAcceptGeneratedExcodes(MultipleArgRecTest test) {
        for (ArgRecTest oneArgTest: test.getArgRecTestList())
            if (!canAcceptGeneratedExcodes(oneArgTest)) return false;
        return true;
    }

    public static boolean matchesArg(String expectedLex, String result) {
        if (result.compareTo(expectedLex) == 0) return true;

        if (expectedLex.contains(".this")) {
            if (matchesArg(expectedLex.substring(expectedLex.indexOf(".this") + 1), result)) return true;
        }

        if (result.contains(".this")) {
            if (matchesArg(expectedLex, result.substring(result.indexOf(".this") + 1))) return true;
        }

        if (expectedLex.startsWith("this.")) {
            if (matchesArg(expectedLex.substring(5), result)) return true;
        }

        if (result.startsWith("this.")) {
            if (matchesArg(expectedLex, result.substring(5))) return true;
        }

        return false;
    }

    public static boolean canAcceptGeneratedLexes(ArgRecTest test) {
        String expectedLex = test.getExpected_lex();

        expectedLex = CandidateMatcher.preprocess(expectedLex);

        List<String> candidates = test.getNext_lexList();
        if (test.getPublicStaticCandidateList() != null) {
            for (PublicStaticMember publicStaticCandidate: test.getPublicStaticCandidateList()) {
                candidates.add(publicStaticCandidate.lexical);
            }
        }

        for (String candidate : candidates) {
            candidate = CandidateMatcher.preprocess(candidate);
            if (matchesArg(expectedLex, candidate)) return true;

            if (test.getArgType() == ExpressionType.METHOD_REF) {
                if (!expectedLex.endsWith("::new")) {
                    if (candidate.equals("::")) return true;
                }
                continue;
            }

            String alternateLex = null;
            if (test.getMethodAccessLex() != null) {
                alternateLex = test.getMethodAccessLex();
            }
            if (test.getObjectCreationLex() != null) {
                alternateLex = test.getObjectCreationLex();
            }
            if (alternateLex != null && matchesArg(alternateLex, candidate)) return true;

            if (test.getStaticMemberAccessLex() != null) {
                if (matchesArg(test.getStaticMemberAccessLex(), candidate)) return true;
            }
        }

        return false;
    }

    public static boolean canAcceptGeneratedLexes(MultipleArgRecTest test) {
        for (ArgRecTest oneArgTest: test.getArgRecTestList())
            if (!canAcceptGeneratedLexes(oneArgTest)) return false;
        return true;
    }

    private static String normalizeMethodInvocation(String s) {
        StringBuilder sb = new StringBuilder(s);
        int bal = 0;
        for (int i = sb.length() - 1; i >= 0; --i) {
            char c = sb.charAt(i);
            if (c == '(') ++bal;
            if (c == ')') --bal;
            if (bal >= 0) break;
            sb.deleteCharAt(i);
        }
        return sb.toString();
    }

    public static boolean canAcceptResult(ArgRecTest test, String result) {
        String expectedLex = test.getExpected_lex();

        expectedLex = CandidateMatcher.preprocess(expectedLex);

        result = CandidateMatcher.preprocess(result);
        if (result.indexOf("(") > 0) {
            result = normalizeMethodInvocation(result);
        }

        if (matchesArg(expectedLex, result)) return true;

        if (test.getArgType() == ExpressionType.METHOD_REF) {
            if (!expectedLex.endsWith("::new")) {
                return result.equals("::");
            }
            return false;
        }

        expectedLex = null;
        if (test.getMethodAccessLex() != null) {
            expectedLex = test.getMethodAccessLex();
        }
        if (test.getObjectCreationLex() != null) {
            expectedLex = test.getObjectCreationLex();
        }
        if (expectedLex != null && matchesArg(expectedLex, result)) return true;

        if (test.getStaticMemberAccessLex() != null) {
            if (matchesArg(test.getStaticMemberAccessLex(), result)) return true;
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

                // Leading spaces
                if (bal == 0 && c == ' ' && sb.length() == 0) continue;

                if (c == ',') {
                    if (bal == 0) break;

                    // Method call, object creation
                    if (bal == 1 && sb.charAt(sb.length() - 1) == '(') break;
                }
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
        if (/*expectedExcode.contains("<unk>")*/true) {
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
//        //Results in excode
//        String expectedExcode = test.getExpected_excode();
//        if (result.equals(expectedExcode)) return true;
//
//        //TODO: Handle unknown excode
//        if (expectedExcode.contains("<unk>")) {
//            String expectedNumParam = test.getExpected_excode().split(",")[2];
//            if (!result.split(",")[1].equals(test.getExpected_lex())) return false;
//            if (!result.split(",")[2].equals(expectedNumParam)) return false;
//            return true;
//        }

        //Results in lex
        String expectedLex = test.getExpected_lex();
        if (result.contains(expectedLex)) return true;

        return false;
    }

    public static void main(String[] args) {
        ArgRecTest test = new ArgRecTest();
        test.setExpected_lex("new Object[]{i,order[i]}");
        test.setNext_lex(Collections.singletonList(Collections.singletonList("new Object[0]")));
        System.out.println(RecTester.canAcceptGeneratedLexes(test));
    }
}
