package flute.tokenizing.excode_data;

import java.util.ArrayList;
import java.util.List;

public class ArgRecTest {
    private String filePath;

    private List<String> lex_context;
    private String excode_context;

    private List<String> next_excode;
    private List<List<String>> next_lex;

    private String expected_excode;
    private String expected_lex;

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
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
}
