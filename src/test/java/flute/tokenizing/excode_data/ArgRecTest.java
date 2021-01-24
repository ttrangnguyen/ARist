package flute.tokenizing.excode_data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArgRecTest extends RecTest {
    private int argPos;

    /**
     * Additional features
     */
    private String param_name = "";

    /**
     * Recommendations
     */
    private List<String> next_excode;
    private List<List<String>> next_lex;

    /**
     * Expected result (only the name is required)
     */
    private String methodAccessExcode;
    private String methodAccessLex;
    private String objectCreationExcode;
    private String objectCreationLex;

    /**
     * For filtering
     */
    private String methodInvocClassQualifiedName;

    /**
     * References
     */
    private transient List<NodeSequenceInfo> expected_excode_ori;

    public int getArgPos() {
        return argPos;
    }

    public void setArgPos(int argPos) {
        this.argPos = argPos;
    }

    public String getParam_name() {
        return param_name;
    }

    public void setParam_name(String param_name) {
        this.param_name = param_name;
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
        for (List<String> nl : next_lex) {
            list.addAll(nl);
        }
        return list;
    }

    public void setNext_lex(List<List<String>> next_lex) {
        this.next_lex = next_lex;
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

    public String getMethodInvocClassQualifiedName() {
        return methodInvocClassQualifiedName;
    }

    public void setMethodInvocClassQualifiedName(String methodInvocClassQualifiedName) {
        this.methodInvocClassQualifiedName = methodInvocClassQualifiedName;
    }

    public List<NodeSequenceInfo> getExpected_excode_ori() {
        return expected_excode_ori;
    }

    public void setExpected_excode_ori(List<NodeSequenceInfo> expected_excode_ori) {
        this.expected_excode_ori = expected_excode_ori;
    }

    public MultipleArgRecTest toSingleArgRecTest() {
        MultipleArgRecTest test = new MultipleArgRecTest();
        test.setFilePath(this.getFilePath());
        test.setNumArg(1);
        test.setLex_context(this.getLex_context());
        test.setExcode_context(this.getExcode_context());
        test.setMethodScope_name(this.getMethodScope_name());
        test.setClass_name(this.getClass_name());
        if ("".equals(this.getParam_name())) {
            test.setParam_list(new ArrayList<>());
        } else {
            test.setParam_list(Collections.singletonList(this.getParam_name()));
        }
        test.setNext_excode(Collections.singletonList(this.getNext_excode()));
        test.setNext_lex(Collections.singletonList(this.getNext_lex()));
        test.setExpected_excode(this.getExpected_excode());
        test.setExpected_lex(this.getExpected_lex());
        test.setIgnored(this.isIgnored());
        test.setArgRecTestList(Collections.singletonList(this));
        return test;
    }
}
