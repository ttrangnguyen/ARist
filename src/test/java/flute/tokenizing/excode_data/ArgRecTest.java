package flute.tokenizing.excode_data;

import java.util.ArrayList;
import java.util.List;

public class ArgRecTest {
    private static int count = 0;
    private transient int id;
    private String filePath;

    private int argPos;

    private List<String> lex_context;
    private String excode_context;

    private List<String> next_excode;
    private List<List<String>> next_lex;

    private String expected_excode;
    private String expected_lex;

    private String methodAccessExcode;
    private String methodAccessLex;

    private String objectCreationExcode;
    private String objectCreationLex;

    private boolean ignored = false;

    private transient List<NodeSequenceInfo> expected_excode_ori;

    public ArgRecTest() {
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

    public int getArgPos() {
        return argPos;
    }

    public void setArgPos(int argPos) {
        this.argPos = argPos;
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

    public List<String> getNext_excode() {
        return next_excode;
    }

    public void setNext_excode(List<String> next_excode) {
        this.next_excode = next_excode;
    }

    public List<List<String>> getNext_lex() {
        return next_lex;
    }

    public List<String> getNext_lexList() {
        List<String> list = new ArrayList<>();
        for (List<String> nl: next_lex) {
            list.addAll(nl);
        }
        return list;
    }

    public void setNext_lex(List<List<String>> next_lex) {
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

    public String getMethodAccessExcode() {
        return methodAccessExcode;
    }

    public void setMethodAccessExcode(String methodAccessExcode) {
        this.methodAccessExcode = methodAccessExcode;
    }

    public String getMethodAccessLex() {
        return methodAccessLex;
    }

    public void setMethodAccessLex(String methodAccessLex) {
        this.methodAccessLex = methodAccessLex;
    }

    public String getObjectCreationExcode() {
        return objectCreationExcode;
    }

    public void setObjectCreationExcode(String objectCreationExcode) {
        this.objectCreationExcode = objectCreationExcode;
    }

    public String getObjectCreationLex() {
        return objectCreationLex;
    }

    public void setObjectCreationLex(String objectCreationLex) {
        this.objectCreationLex = objectCreationLex;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }

    public List<NodeSequenceInfo> getExpected_excode_ori() {
        return expected_excode_ori;
    }

    public void setExpected_excode_ori(List<NodeSequenceInfo> expected_excode_ori) {
        this.expected_excode_ori = expected_excode_ori;
    }
}
