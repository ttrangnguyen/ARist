package flute.tokenizing.excode_data;

import java.util.ArrayList;
import java.util.List;

public class MultipleArgRecTest extends MethodCallRecTest {
    private transient int numArg;

    /**
     * Additional features
     */
    private List<String> param_list;
    private List<List<List<Boolean>>> is_local_var;

    /**
     * Recommendations
     */
    private List<List<String>> next_excode;
    private List<List<List<String>>> next_lex;

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

    public List<List<List<Boolean>>> getIs_local_var() {
        return is_local_var;
    }

    public void setIs_local_var(List<List<List<Boolean>>> is_local_var) {
        this.is_local_var = is_local_var;
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

    public List<ArgRecTest> getArgRecTestList() {
        return argRecTestList;
    }

    public void setArgRecTestList(List<ArgRecTest> argRecTestList) {
        this.argRecTestList = argRecTestList;
    }
}