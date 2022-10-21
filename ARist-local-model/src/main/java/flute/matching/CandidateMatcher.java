package flute.matching;

import flute.candidate.Candidate;

import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CandidateMatcher {
    private static String identifieRegex = "([a-zA-Z_$][a-zA-Z\\d_$]*\\.)*[a-zA-Z_$][a-zA-Z\\d_$]*";
    private static String stringLiteralRegex = "\"([^\"]|(\\\\\"))*\"";

    public static String preprocess(String target) {
        target = emptyStringLiteralDecorator(target);
        target = removeArrayAccessIndexDecorator(target);
        if (target.contains("{")) {
            target = target.substring(0, target.indexOf("{")).trim();
        }
        return target;
    }

    public static String removeArrayAccessIndexDecorator(String sourceCode) {
        Matcher m = Pattern.compile("\\[|\\]|[^\\[\\]]+").matcher(sourceCode);
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

    public static String emptyStringLiteralDecorator(String sourceCode) {
        StringBuilder sb = new StringBuilder();
        boolean insideStringLiteral = false;
        for (String s: sourceCode.split("\\\\\"", -1)) {
            String[] t = s.replace("'\"'", "''").split("\"", -1);
            for (int i = 0; i < t.length; ++i) {
                if (!insideStringLiteral) sb.append(t[i].replace("''", "'\"'"));
                if (i < t.length - 1) {
                    sb.append('"');
                    insideStringLiteral = !insideStringLiteral;
                }
            }
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
        if (matchesArrayCreation(candidate, target)) return true;
        if (matchesClassExpr(candidate, target)) return true;
        if (matchesMethodReference(candidate, target)) return true;
        return false;
    }

    public static boolean equalsLexical(Candidate candidate, String target) {
        return candidate.lexical.compareTo(target) == 0;
    }

    public static boolean matchesMethodCall(Candidate candidate, String target) {
        if (!candidate.excode.matches(".*M_ACCESS\\("+"\\w+"+"(<.*>)?,"+"\\w+"+",\\d+\\) OPEN_PART")) return false;
        if (!target.matches(".*"+"\\w+"+"\\(.*\\)$")) return false;
        return target.startsWith(candidate.lexical);
    }

    public static boolean matchesObjectCreation(Candidate candidate, String target) {
        if (!candidate.excode.matches("^C_CALL\\("+"\\w+"+"(<.*>)?,"+"\\w+"+"\\) OPEN_PART")) return false;
        if (!target.matches("^new "+"\\w+"+"\\(.*\\)$")) return false;
        return target.startsWith(candidate.lexical);
    }

    public static boolean matchesArrayCreation(Candidate candidate, String target) {
        if (!target.matches("^new \\w+\\[.*\\]$")) return false;
        String className = target.substring(target.indexOf(' ') + 1, target.indexOf('['));
        if (candidate.excode.compareTo("C_CALL(Array_"+className+","+className+") OPEN_PART LIT(num) CLOSE_PART") != 0) return false;
        return candidate.lexical.compareTo("new "+className+"[0]") == 0;
    }

    public static boolean matchesLiteral(Candidate candidate, String target) {
        if (!candidate.excode.matches("^LIT\\([a-zA-Z]+\\)$")) return false;
        if (matchesStringLiteral(candidate, target)) return true;
        if (matchesNumLiteral(candidate, target)) return true;
        if (matchesCharLiteral(candidate, target)) return true;
        return false;
    }

    public static boolean matchesStringLiteral(Candidate candidate, String target) {
        if (!target.matches("^" + stringLiteralRegex + "(\\s*\\+\\s*(" + stringLiteralRegex + "|'[^']+'))*$")) return false;
        if (candidate.excode.compareTo("LIT(String)") != 0) return false;
        return candidate.lexical.compareTo("\"\"") == 0;
    }

    public static boolean matchesNumLiteral(Candidate candidate, String target) {
        if (candidate.excode.compareTo("LIT(num)") != 0) return false;
        try {
            Double.parseDouble(target);
        } catch (NumberFormatException e) {
            if (!target.matches("^((\\d+\\.\\d+)|(\\d+)|([+\\-*\\/^])|([\\(\\)]))+$")) return false;
        }
        return candidate.lexical.compareTo("0") == 0;
    }

    public static boolean matchesCharLiteral(Candidate candidate, String target) {
        if (!target.matches("^'.+'$")) return false;
        if (candidate.excode.compareTo("LIT(num)") != 0) return false;
        return candidate.lexical.compareTo("0") == 0;
    }

    public static boolean matchesClassExpr(Candidate candidate, String target) {
        if (!target.endsWith(".class")) return false;
        if (candidate.excode.compareTo("LIT(Class)") != 0) return false;
        return candidate.lexical.compareTo(".class") == 0;
    }

    public static boolean matchesMethodReference(Candidate candidate, String target) {
        if (!target.contains("::")) return false;
        if (target.endsWith("::new")) {
            if (!candidate.excode.matches("^M_REF\\("+"\\w+"+"(<.*>)?,new\\)$")) return false;
            return candidate.lexical.compareTo(target) == 0;
        } else {
            if (!candidate.excode.matches("^M_REF\\([^,]*,[^,]*\\)$")) return false;
            return candidate.lexical.compareTo("::") == 0;
        }
    }
}

