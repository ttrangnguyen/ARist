package flute.tokenizing.excode_data;

import java.util.ArrayList;
import java.util.List;

/**
 * This class should only contain fields which are used for predicting
 */
public class MultipleArgRecTest extends MethodCallRecTest {
    private transient int numArg;

    /**
     * Additional features
     */
    private List<String> param_list;
    private List<List<List<Integer>>> candidates_locality;
    private List<List<List<Integer>>> candidates_scope_distance;

    /**
     * Recommendations
     */
    private List<List<String>> next_excode;
    private List<List<List<String>>> next_lex;
    private List<String> paramTypeKeyList;
    private String packageName;

    /**
     * References
     */
    private transient List<ArgRecTest> argRecTestList;

    public int getNumArg() {
        return numArg;
    }

    public void setNumArg(int numArg) {
        this.numArg = numArg;
    }

    public List<String> getParam_list() {
        return param_list;
    }

    public void setParam_list(List<String> param_list) {
        this.param_list = param_list;
    }

    public List<List<List<Integer>>> getCandidates_locality() {
        return candidates_locality;
    }

    public void setCandidates_locality(List<List<List<Integer>>> candidates_locality) {
        this.candidates_locality = candidates_locality;
    }

    public List<List<List<Integer>>> getCandidates_scope_distance() {
        return candidates_scope_distance;
    }

    public void setCandidates_scope_distance(List<List<List<Integer>>> candidates_scope_distance) {
        this.candidates_scope_distance = candidates_scope_distance;
    }

    public List<List<String>> getNext_excode() {
        return next_excode;
    }

    public List<String> getNext_excodeList() {
        List<String> list = new ArrayList<>();
        for (List<String> ne: next_excode) {
            list.addAll(ne);
        }
        return list;
    }

    public void setNext_excode(List<List<String>> next_excode) {
        this.next_excode = next_excode;
    }

    public List<List<List<String>>> getNext_lex() {
        return next_lex;
    }

    public List<String> getNext_lexList() {
        List<String> list = new ArrayList<>();
        for (List<List<String>> arg: next_lex) {
            for (List<String> nl: arg) {
                list.addAll(nl);
            }
        }
        return list;
    }

    public void setNext_lex(List<List<List<String>>> next_lex) {
        this.next_lex = next_lex;
    }

    public List<String> getParamTypeKeyList() {
        return paramTypeKeyList;
    }

    public void setParamTypeKeyList(List<String> paramTypeKeyList) {
        this.paramTypeKeyList = paramTypeKeyList;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public List<ArgRecTest> getArgRecTestList() {
        return argRecTestList;
    }

    public void setArgRecTestList(List<ArgRecTest> argRecTestList) {
        this.argRecTestList = argRecTestList;
    }
}