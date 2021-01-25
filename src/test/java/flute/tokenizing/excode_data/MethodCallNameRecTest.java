package flute.tokenizing.excode_data;

import java.util.ArrayList;
import java.util.List;

public class MethodCallNameRecTest extends MethodCallRecTest {
    /**
     * Recommendations
     */
    private List<String> method_candidate_excode;
    private List<List<String>> method_candidate_lex;

    public List<String> getMethod_candidate_excode() {
        return method_candidate_excode;
    }

    public void setMethod_candidate_excode(List<String> method_candidate_excode) {
        this.method_candidate_excode = method_candidate_excode;
    }

    public List<List<String>> getMethod_candidate_lex() {
        return method_candidate_lex;
    }

    public List<String> getMethod_candidate_lexList() {
        List<String> list = new ArrayList<>();
        for (List<String> mcl : method_candidate_lex) {
            list.addAll(mcl);
        }
        return list;
    }

    public void setMethod_candidate_lex(List<List<String>> method_candidate_lex) {
        this.method_candidate_lex = method_candidate_lex;
    }
}
