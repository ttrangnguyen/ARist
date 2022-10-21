package flute.lexing;

import slp.core.lexing.code.JavaLexer;

import java.util.ArrayList;
import java.util.List;

public class ExcodeLexer extends JavaLexer {
    @Override
    public List<List<String>> tokenizeLines(String text) {
        String[] data = text.split(" ");
        ArrayList<String> allTokens = new ArrayList<>();
        List<List<String>> lineTokens = new ArrayList<>();
        int methodLevel=0;
        for (String s : data) {
            if (s.startsWith("METHOD{")) {
                methodLevel += 1;
            } else if (s.equals("ENDMETHOD")) {
                methodLevel -= 1;
                if (methodLevel == 0) {
                    lineTokens.add(allTokens);
                    allTokens = new ArrayList<>();
                }
            } else if (methodLevel > 0) {
                allTokens.add(modify(s));
            }
        }
        return lineTokens;
    }

    private String modify(String s) {
        if (s.startsWith("VAR(")) {
            return s.substring(0, s.indexOf(",")) + ")";
        } else {
            return s;
        }
    }
}
