package flute.tokenizing.exe;

import com.github.javaparser.ast.expr.MethodCallExpr;
import flute.data.MultiMap;
import flute.jdtparser.ProjectParser;
import flute.jdtparser.utils.ParserUtils;
import flute.tokenizing.excode_data.ContextInfo;
import flute.tokenizing.excode_data.MethodCallNameRecTest;
import flute.tokenizing.excode_data.NodeSequenceInfo;
import flute.tokenizing.excode_data.RecTest;
import flute.utils.file_processing.JavaTokenizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MethodCallNameRecTestGenerator extends MethodCallRecTestGenerator {
    public MethodCallNameRecTestGenerator(String projectPath, ProjectParser projectParser) {
        super(projectPath, projectParser);
    }

    @Override
    List<? extends RecTest> generateFromMethodCall(List<NodeSequenceInfo> excodes, int methodCallStartIdx, int methodCallEndIdx,
                                                   MethodCallExpr methodCall, String contextMethodCall, String methodScope, String methodName) {

        List<RecTest> tests = new ArrayList<>();

        String classQualifiedName = getFileParser().getCurMethodInvocation().getClassQualifiedName().orElse(null);
        int contextIdx = methodCallStartIdx - 1;

        MultiMap methodMap = null;
        try {
            methodMap = ParserUtils.methodMap(getFileParser().genMethodCall());
        } catch (ArrayIndexOutOfBoundsException e) {
            return tests;
        }

        List<String> methodExcodeList = new ArrayList<>(methodMap.getValue().keySet());
        List<List<String>> methodLexList = new ArrayList<>();
        for (String methodExcode : methodExcodeList) {
            methodLexList.add(methodMap.getValue().get(methodExcode));
        }
        ContextInfo context = new ContextInfo(excodes, contextIdx);

        List<NodeSequenceInfo> methodNameExcode = Collections.singletonList(excodes.get(methodCallStartIdx));

        try {
            List<String> tokenizedContextMethodCall = JavaTokenizer.tokenize(contextMethodCall + methodScope);
            while (!tokenizedContextMethodCall.isEmpty() && tokenizedContextMethodCall.get(tokenizedContextMethodCall.size() - 1).equals("")) {
                tokenizedContextMethodCall.remove(tokenizedContextMethodCall.size() - 1);
            }

            List<NodeSequenceInfo> excodeContext = context.getContextFromMethodDeclaration();

            MethodCallNameRecTest test = new MethodCallNameRecTest();
            test.setLex_context(tokenizedContextMethodCall);
            test.setExcode_context(NodeSequenceInfo.convertListToString(excodeContext));
            test.setMethodScope_name(getFileParser().getCurMethodScopeName().orElse(""));
            test.setClass_name(getFileParser().getCurClassScopeName().orElse(""));
            test.setExpected_excode(NodeSequenceInfo.convertListToString(methodNameExcode));
            test.setExpected_lex(methodName);
            test.setMethod_candidate_excode(methodExcodeList);
            test.setMethod_candidate_lex(methodLexList);
            test.setMethodInvocClassQualifiedName(classQualifiedName);
            test.setIgnored(false);

            tests.add(test);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return tests;
    }
}
