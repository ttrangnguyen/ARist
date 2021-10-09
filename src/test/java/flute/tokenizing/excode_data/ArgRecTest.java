package flute.tokenizing.excode_data;

import flute.analysis.ExpressionType;
import flute.jdtparser.PublicStaticMember;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArgRecTest extends MethodCallRecTest {
    private int argPos;

    /**
     * Additional features
     */
    private String param_name = "";
    private List<List<Integer>> candidates_locality;
    private List<List<Integer>> candidates_scope_distance;

    /**
     * Recommendations
     */
    private List<String> next_excode;
    private List<List<String>> next_lex;
    private String paramTypeKey;
    private transient List<PublicStaticMember> publicStaticCandidateList;

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

    /**
     * @return 6 if a candidate is a local variable in the same block,
     * 5 if it is a local variable in the same method declaration scope,
     * 4 if it is a param,
     * 3 if it is a (static) field of current class,
     * 2 if it is a field of different class,
     * 1 if it is a static field of different class,
     * -1 otherwise.
     */
    public List<List<Integer>> getCandidates_locality() {
        return candidates_locality;
    }

    public void setCandidates_locality(List<List<Integer>> candidates_locality) {
        this.candidates_locality = candidates_locality;
    }

    public List<List<Integer>> getCandidates_scope_distance() {
        return candidates_scope_distance;
    }

    public void setCandidates_scope_distance(List<List<Integer>> candidates_scope_distance) {
        this.candidates_scope_distance = candidates_scope_distance;
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

    public List<PublicStaticMember> getPublicStaticCandidateList() {
        return publicStaticCandidateList;
    }

    public void setPublicStaticCandidateList(List<PublicStaticMember> publicStaticCandidateList) {
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
        test.setCandidates_locality(Collections.singletonList(this.getCandidates_locality()));
        test.setCandidates_scope_distance(Collections.singletonList(this.getCandidates_scope_distance()));
        test.setExpected_excode(this.getExpected_excode());
        test.setExpected_lex(this.getExpected_lex());
        test.setIgnored(this.isIgnored());
        test.setMethodInvoc(this.getMethodInvoc());
        test.setMethodInvocCaller(this.getMethodInvocCaller());
        test.setMethodInvocClassQualifiedName(this.getMethodInvocClassQualifiedName());
        test.setArgRecTestList(Collections.singletonList(this));
        return test;
    }
}
