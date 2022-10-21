package flute.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LexsimCalculator {
    // work with already converted string tokens to indices in vocabulary
    public static Double calculate(String target1, String target2) {
        List<String> s1 = tokenize(target1);
        List<String> s2 = tokenize(target2);
        //System.out.println(s1);
        //System.out.println(s2);
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;
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
        return dp[s1.size()-1][s2.size()-1]*2.0 / (s1.size() + s2.size());
    }

    public static ArrayList<String> tokenize(String s) {
        // split by punctuation
        String[] s1 = s.split("[\\p{Punct}\\s]+");
        ArrayList<String> tokens = new ArrayList<>();
        for (String ss : s1) {
            // split by camel case
            ArrayList<String> subTokens = new ArrayList<>(Arrays.asList(ss.split("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|(?<=[0-9])(?=[A-Z][a-z])|(?<=[a-zA-Z])(?=[0-9])")));
            for (String subToken : subTokens)
                tokens.add(subToken.toLowerCase());
        }
        return tokens;
    }

    public static void main(String[] args) {
        String s1 = "hello_World_big_boy";
        String s2 = "ByeWorld";
        System.out.println(LexsimCalculator.calculate(s1, s2));
    }
}
