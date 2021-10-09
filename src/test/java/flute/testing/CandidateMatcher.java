package flute.testing;

import flute.data.testcase.Candidate;
import flute.preprocessing.EmptyStringLiteralDecorator;
import flute.preprocessing.RemoveArrayAccessIndexDecorator;
import flute.preprocessing.RemoveArrayInitializerDecorator;

public class CandidateMatcher {
    private static String identifieRegex = "([a-zA-Z_$][a-zA-Z\\d_$]*\\.)*[a-zA-Z_$][a-zA-Z\\d_$]*";
    private static String stringLiteralRegex = "\"([^\"]|(\\\\\"))*\"";

    public static String preprocess(String target) {
        target = EmptyStringLiteralDecorator.preprocess(target);
        target = RemoveArrayAccessIndexDecorator.preprocess(target);
        if (target.contains("{")) {
            target = target.substring(0, target.indexOf("{")).trim();
        }
        return target;
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

    public static boolean matchesArrayCreation(Candidate candidate, String target) {
        if (!target.matches("^new \\w+\\[.*\\]$")) return false;
        String className = target.substring(target.indexOf(' ') + 1, target.indexOf('['));
        if (candidate.getExcode().compareTo("C_CALL(Array_"+className+","+className+") OPEN_PART LIT(num) CLOSE_PART") != 0) return false;
        return candidate.getName().compareTo("new "+className+"[0]") == 0;
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
        if (candidate.getExcode().compareTo("LIT(Class)") != 0) return false;
        return candidate.getName().compareTo(".class") == 0;
    }

    public static boolean matchesMethodReference(Candidate candidate, String target) {
        if (!target.contains("::")) return false;
        if (target.endsWith("::new")) {
            if (!candidate.getExcode().matches("^M_REF\\("+"\\w+"+"(<.*>)?,new\\)$")) return false;
            return candidate.getName().compareTo(target) == 0;
        } else {
            if (!candidate.getExcode().matches("^M_REF\\([^,]*,[^,]*\\)$")) return false;
            return candidate.getName().compareTo("::") == 0;
        }
    }

    public static void main(String[] args) {
        Candidate candidate = new Candidate("M_REF(<unk>,<unk>)", "::");
        System.out.println(CandidateMatcher.matches(candidate, "Student::get"));
    }
}
