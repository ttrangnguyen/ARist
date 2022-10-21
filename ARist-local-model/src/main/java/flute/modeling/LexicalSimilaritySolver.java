package flute.modeling;

import flute.config.ModelConfig;
import slp.core.lexing.code.JavaLexer;

import java.util.ArrayList;
import java.util.List;

public class LexicalSimilaritySolver {
    private static final double LOWER_BOUND = 0.1;

    // work with already converted string tokens to indices in vocabulary
    public static Double lexicalSimilarity(List<String> target1, List<String> target2) {
        List<String> s1 = modified(target1);
        List<String> s2 = modified(target2);
        if (s1.isEmpty() || s2.isEmpty()) return LOWER_BOUND;
        int[][] dp = new int[s1.size()][s2.size()];
        for (int i = 0; i < s1.size(); ++i) {
            for (int j = 0; j < s2.size(); ++j) {
                dp[i][j] = Math.max((i>0?dp[i-1][j]:0), (j>0?dp[i][j-1]:0));
                if (s1.get(i).equals(s2.get(j))) {
                    dp[i][j] = Math.max(dp[i][j], 1+((i>0 && j>0)?dp[i-1][j-1]:0));
                }
            }
        }
        // result = (comterms(s1,s2)+comterms(s2,s1)) / (terms(s1)+terms(s2))
        return Math.max(dp[s1.size()-1][s2.size()-1]*2.0 / (s1.size() + s2.size()), LOWER_BOUND);
    }

    public static List<String> modified(List<String> target) {
        List<String> filtered = new ArrayList<>();
        JavaLexer lexer = new JavaLexer();
        for (String token : target) {
            if (!token.isEmpty() && Character.isLetterOrDigit(token.charAt(0))) {
                if (ModelConfig.tokenizedType == ModelConfig.TokenizedType.FULL_TOKEN) {
                    ArrayList<String> subTokens = lexer.tokenizeWordSubToken(token);
                    for (String subToken : subTokens) {
                        filtered.add(subToken.toLowerCase());
                    }
                }
                else {
                    filtered.add(token.toLowerCase());
                }
            }
        }
        return filtered;
    }

    public static void main(String[] args) {
        // the following example should output 0.5714285714285714
        List<String> s1 = new ArrayList<>();
        s1.add("2");
        s1.add("1");
        s1.add("3");
        s1.add("5");
        List<String> s2 = new ArrayList<>();
        s2.add("2");
        s2.add("5");
        s2.add("4");
        System.out.println(lexicalSimilarity(s1, s2));
    }
}
