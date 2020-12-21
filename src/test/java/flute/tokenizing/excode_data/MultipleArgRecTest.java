package flute.tokenizing.excode_data;

import java.util.ArrayList;
import java.util.List;

public class MultipleArgRecTest {
    private static int count = 0;
    private transient int id;
    private String filePath;
    private transient int numArg;

    private List<String> lex_context;
    private String excode_context;

    private String method_name ="";
    private String class_name ="";

    private List<List<String>> next_excode;
    private List<List<List<String>>> next_lex;

    private String expected_excode;
    private String expected_lex;

    private boolean ignored = false;

    private transient List<ArgRecTest> argRecTestList;

    public MultipleArgRecTest() {
        id = count++;
    }

    public int getId() {
        return id;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getNumArg() {
        return numArg;
    }

    public void setNumArg(int numArg) {
        this.numArg = numArg;
    }

    public List<String> getLex_context() {
        return lex_context;
    }

    public void setLex_context(List<String> lex_context) {
        this.lex_context = lex_context;
    }

    public String getExcode_context() {
        return excode_context;
    }

    public void setExcode_context(String excode_context) {
        this.excode_context = excode_context;
    }

    public String getMethodScope_name() {
        return method_name;
    }

    public void setMethodScope_name(String method_name) {
        this.method_name = method_name;
    }

    public String getClass_name() {
        return class_name;
    }

    public void setClass_name(String class_name) {
        this.class_name = class_name;
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

    public String getExpected_excode() {
        return expected_excode;
    }

    public void setExpected_excode(String expected_excode) {
        this.expected_excode = expected_excode;
    }

    public String getExpected_lex() {
        return expected_lex;
    }

    public void setExpected_lex(String expected_lex) {
        this.expected_lex = expected_lex;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }

    public List<ArgRecTest> getArgRecTestList() {
        return argRecTestList;
    }

    public void setArgRecTestList(List<ArgRecTest> argRecTestList) {
        this.argRecTestList = argRecTestList;
    }
}