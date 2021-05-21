package flute.testing;

import flute.data.testcase.Candidate;

public class CandidateMatcher {
    private static String identifieRegex = "([a-zA-Z_$][a-zA-Z\\d_$]*\\.)*[a-zA-Z_$][a-zA-Z\\d_$]*";

    public static String preprocess(String target) {
        if (target.contains("[")) {
            StringBuilder sb = new StringBuilder();
            while (target.contains("[")) {
                sb.append(target, 0, target.indexOf('[') + 1);
                target = target.substring(target.indexOf(']'));
            }
            sb.append(target);
            target = sb.toString();
        }
        return target;
    }

    private static boolean matchesTarget(Candidate candidate, String target) {
        if (equalsLexical(candidate, target)) return true;
        if (matchesLiteral(candidate, target)) return true;
        if (matchesMethodCall(candidate, target)) return true;
        if (matchesObjectCreation(candidate, target)) return true;
        if (matchesClassExpr(candidate, target)) return true;
        return false;
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

    public static boolean equalsLexical(Candidate candidate, String target) {
        return candidate.getName().compareTo(target) == 0;
    }

    public static boolean matchesMethodCall(Candidate candidate, String target) {
        if (!candidate.getExcode().matches(".*M_ACCESS\\("+"\\w+"+","+"\\w+"+",\\d+\\) OPEN_PART")) return false;
        if (!target.matches(".*"+"\\w+"+"\\(.*\\)$")) return false;
        return target.startsWith(candidate.getName());
    }

    public static boolean matchesObjectCreation(Candidate candidate, String target) {
        if (!candidate.getExcode().matches("^C_CALL\\("+"\\w+"+","+"\\w+\\) OPEN_PART")) return false;
        if (!target.matches("^new "+"\\w+"+"\\(.*\\)$")) return false;
        return target.startsWith(candidate.getName());
    }

    public static boolean matchesLiteral(Candidate candidate, String target) {
        if (!candidate.getExcode().matches("^LIT\\([a-zA-Z]+\\)$")) return false;
        if (matchesStringLiteral(candidate, target)) return true;
        if (matchesNumLiteral(candidate, target)) return true;
        return false;
    }

    public static boolean matchesStringLiteral(Candidate candidate, String target) {
        if (!target.matches("^\".*\"$")) return false;
        if (candidate.getExcode().compareTo("LIT(String)") != 0) return false;
        return candidate.getName().compareTo("\"\"") == 0;
    }

    public static boolean matchesNumLiteral(Candidate candidate, String target) {
        if (candidate.getExcode().compareTo("LIT(num)") != 0) return false;
        try {
            Double.parseDouble(target);
        } catch (NumberFormatException e) {
            return false;
        }
        return candidate.getName().compareTo("0") == 0;
    }

    public static boolean matchesClassExpr(Candidate candidate, String target) {
        if (!target.endsWith(".class")) return false;
        if (candidate.getExcode().compareTo("VAR(Class)") != 0) return false;
        return candidate.getName().compareTo(".class") == 0;
    }

    public static void main(String[] args) {
        Candidate candidate = new Candidate("VAR(Animal,this) M_ACCESS(Animal,moveTo,2) OPEN_PART", "this.moveTo(");
        System.out.println(CandidateMatcher.matches(candidate, "moveTo(x, y)"));
    }
}
