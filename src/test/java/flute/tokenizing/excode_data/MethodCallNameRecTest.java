package flute.tokenizing.excode_data;

import flute.data.MethodInvocationModel;

import java.util.ArrayList;
import java.util.List;

public class MethodCallNameRecTest extends MethodCallRecTest {
    /**
     * Features
     */
    private List<String> method_context;

    /**
     * Recommendations
     */
    private List<String> method_candidate_excode;
    private List<List<String>> method_candidate_lex;

    private List<String> next_lex;

    /**
     * References
     */
    private transient MethodInvocationModel methodInvocationModel;

    public List<String> getMethod_context() {
        return method_context;
    }

    public void setMethod_context(List<String> method_context) {
        this.method_context = method_context;
    }

    public void addMethod_context(String method_context) {
        if (this.method_context == null) setMethod_context(new ArrayList<>());
        this.method_context.add(method_context);
    }

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

    public List<String> getNext_lex() {
        return next_lex;
    }

    public void setNext_lex(List<String> next_lex) {
        this.next_lex = next_lex;
    }

    public MethodInvocationModel getMethodInvocationModel() {
        return methodInvocationModel;
    }

    public void setMethodInvocationModel(MethodInvocationModel methodInvocationModel) {
        this.methodInvocationModel = methodInvocationModel;
    }
}
