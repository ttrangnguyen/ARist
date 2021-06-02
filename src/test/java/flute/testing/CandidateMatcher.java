package flute.testing;

import flute.data.testcase.Candidate;

import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CandidateMatcher {
    private static String identifieRegex = "([a-zA-Z_$][a-zA-Z\\d_$]*\\.)*[a-zA-Z_$][a-zA-Z\\d_$]*";
    private static String stringLiteralRegex = "\"([^\"]|(\\\\\"))*\"";

    public static String preprocess(String target) {
        target = emptyStringLiteral(target);
        target = removeArrayAccessIndex(target);
        return target;
    }

    private static String emptyStringLiteral(String target) {
        StringBuilder sb = new StringBuilder();
        boolean insideStringLiteral = false;
        for (String s: target.split("\\\\\"", -1)) {
            String[] t = s.split("\"", -1);
            for (int i = 0; i < t.length; ++i) {
                if (!insideStringLiteral) sb.append(t[i]);
                if (i < t.length - 1) {
                    sb.append('"');
                    insideStringLiteral = !insideStringLiteral;
                }
            }
        }
        return sb.toString();
    }

    private static String removeArrayAccessIndex(String target) {
        Matcher m = Pattern.compile("\\[|\\]|[^\\[\\]]+").matcher(target);
        Stack<String> stack = new Stack<>();
        while (m.find()) {
            String s = m.group();
            if (s.compareTo("]") == 0) {
                while (true) {
                    if (stack.empty()|| stack.pop().compareTo("[") == 0) break;
                }
            }
            stack.add(s);
        }
        StringBuilder sb = new StringBuilder();
        for (String s: stack) {
            if (s.compareTo("]") == 0) {
                sb.append('[');
            }
            sb.append(s);
        }
        return sb.toString();
    }

    public static boolean matches(Candidate candidate, String target) {
        target = preprocess(target);
        if (matchesTarget(candidate, target)) return true;
        if (target.startsWith("this.")) {
            if (matchesTarget(candidate, target.substring(5))) return true;
        } else {
            if (matchesTarget(candidate, "this." + target)) return true;
        }
        return false;
    }

    private static boolean matchesTarget(Candidate candidate, String target) {
        if (equalsLexical(candidate, target)) return true;
        if (matchesLiteral(candidate, target)) return true;
        if (matchesMethodCall(candidate, target)) return true;
        if (matchesObjectCreation(candidate, target)) return true;
        if (matchesClassExpr(candidate, target)) return true;
        return false;
    }

    public static boolean equalsLexical(Candidate candidate, String target) {
        return candidate.getName().compareTo(target) == 0;
    }

    public static boolean matchesMethodCall(Candidate candidate, String target) {
        if (!candidate.getExcode().matches(".*M_ACCESS\\("+"\\w+"+"(<.*>)?,"+"\\w+"+",\\d+\\) OPEN_PART")) return false;
        if (!target.matches(".*"+"\\w+"+"\\(.*\\)$")) return false;
        return target.startsWith(candidate.getName());
    }

    public static boolean matchesObjectCreation(Candidate candidate, String target) {
        if (!candidate.getExcode().matches("^C_CALL\\("+"\\w+"+"(<.*>)?,"+"\\w+"+"\\) OPEN_PART")) return false;
        if (!target.matches("^new "+"\\w+"+"\\(.*\\)$")) return false;
        return target.startsWith(candidate.getName());
    }

    public static boolean matchesLiteral(Candidate candidate, String target) {
        if (!candidate.getExcode().matches("^LIT\\([a-zA-Z]+\\)$")) return false;
        if (matchesStringLiteral(candidate, target)) return true;
        if (matchesNumLiteral(candidate, target)) return true;
        if (matchesCharLiteral(candidate, target)) return true;
        return false;
    }

    public static boolean matchesStringLiteral(Candidate candidate, String target) {
        if (!target.matches("^" + stringLiteralRegex + "(\\s*\\+\\s*(" + stringLiteralRegex + "|'[^']+'))*$")) return false;
        if (candidate.getExcode().compareTo("LIT(String)") != 0) return false;
        return candidate.getName().compareTo("\"\"") == 0;
    }

    public static boolean matchesNumLiteral(Candidate candidate, String target) {
        if (candidate.getExcode().compareTo("LIT(num)") != 0) return false;
        try {
            Double.parseDouble(target);
        } catch (NumberFormatException e) {
            if (!target.matches("^((\\d+\\.\\d+)|(\\d+)|([+\\-*\\/^])|([\\(\\)]))+$")) return false;
        }
        return candidate.getName().compareTo("0") == 0;
    }

    public static boolean matchesCharLiteral(Candidate candidate, String target) {
        if (!target.matches("^'.+'$")) return false;
        if (candidate.getExcode().compareTo("LIT(num)") != 0) return false;
        return candidate.getName().compareTo("0") == 0;
    }

    public static boolean matchesClassExpr(Candidate candidate, String target) {
        if (!target.endsWith(".class")) return false;
        if (candidate.getExcode().compareTo("VAR(Class)") != 0) return false;
        return candidate.getName().compareTo(".class") == 0;
    }

    public static void main(String[] args) {
        Candidate candidate = new Candidate("VAR(Animal,this) M_ACCESS(Animal,moveTo,2) OPEN_PART", "this.moveTo(");
        System.out.println(CandidateMatcher.matches(candidate, "a.get[b.go[\"\\\"<![CDATA[\\\"\"]]"));
    }
}
