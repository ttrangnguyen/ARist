package flute.tokenizing.excode_data;

import java.util.List;

public abstract class RecTest {
    /**
     * Identifier
     */
    private static int count = 0;
    private transient int id;
    private String filePath;
    private int line;
    private int col;

    /**
     * Features
     */
    private List<String> classHierarchy;
    private List<String> lex_context;
    private String excode_context;
    private String method_name = "";
    private String class_name = "";
    private int test_id;

    /**
     * Expected result
     */
    private String expected_excode = "";
    private String expected_lex = "";

    /**
     * etc
     */
    private boolean ignored = false;

    public RecTest() {
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

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public List<String> getClassHierarchy() {
        return classHierarchy;
    }

    public void setClassHierarchy(List<String> classHierarchy) {
        this.classHierarchy = classHierarchy;
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

    public void setTest_id(int test_id) {
        this.test_id = test_id;
    }
}
