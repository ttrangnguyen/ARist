package flute.tokenizing.excode_data;

import flute.analysis.ExpressionType;
import flute.jdtparser.PublicStaticMember;
import flute.utils.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArgRecTest extends MethodCallRecTest {
    private int argPos;

    /**
     * Additional features
     */
    private String param_name = "";
    private List<List<Boolean>> is_local_var;

    /**
     * Recommendations
     */
    private List<String> next_excode;
    private List<List<String>> next_lex;
    private String paramTypeKey;
    private transient List<Pair<String, String>> publicStaticCandidateList;

    /**
     * Expected result (alternate)
     */
    private ExpressionType argType;
    private String methodAccessExcode;
    private String methodAccessLex;
    private String objectCreationExcode;
    private String objectCreationLex;
    private String staticMemberAccessLex;

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

    public List<List<Boolean>> getIs_local_var() {
        return is_local_var;
    }

    public void setIs_local_var(List<List<Boolean>> is_local_var) {
        this.is_local_var = is_local_var;
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

    public String getParamTypeKey() {
        return paramTypeKey;
    }

    public void setParamTypeKey(String paramTypeKey) {
        this.paramTypeKey = paramTypeKey;
    }

    public List<Pair<String, String>> getPublicStaticCandidateList() {
        return publicStaticCandidateList;
    }

    public void setPublicStaticCandidateList(List<Pair<String, String>> publicStaticCandidateList) {
        this.publicStaticCandidateList = publicStaticCandidateList;
    }

    public ExpressionType getArgType() {
        return argType;
    }

    public void setArgType(ExpressionType argType) {
        this.argType = argType;
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

    public String getStaticMemberAccessLex() {
        return staticMemberAccessLex;
    }

    public void setStaticMemberAccessLex(String staticMemberAccessLex) {
        this.staticMemberAccessLex = staticMemberAccessLex;
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
        test.setNumArg(this.getArgPos() != 0? 1: 0);
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
        test.setParamTypeKeyList(Collections.singletonList(this.getParamTypeKey()));
        test.setIs_local_var(Collections.singletonList(this.getIs_local_var()));
        test.setExpected_excode(this.getExpected_excode());
        test.setExpected_lex(this.getExpected_lex());
        test.setIgnored(this.isIgnored());
        test.setMethodInvocClassQualifiedName(this.getMethodInvocClassQualifiedName());
        test.setArgRecTestList(Collections.singletonList(this));
        return test;
    }
}
