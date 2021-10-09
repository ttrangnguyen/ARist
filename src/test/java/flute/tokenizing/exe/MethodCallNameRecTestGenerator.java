package flute.tokenizing.exe;

import com.github.javaparser.ast.expr.MethodCallExpr;
import flute.data.MethodInvocationModel;
import flute.data.MultiMap;
import flute.jdtparser.ProjectParser;
import flute.jdtparser.callsequence.FileNode;
import flute.jdtparser.callsequence.MethodCallNode;
import flute.jdtparser.callsequence.node.cfg.MinimalNode;
import flute.jdtparser.callsequence.node.cfg.Utils;
import flute.jdtparser.utils.ParserUtils;
import flute.tokenizing.excode_data.ContextInfo;
import flute.tokenizing.excode_data.MethodCallNameRecTest;
import flute.tokenizing.excode_data.NodeSequenceInfo;
import flute.tokenizing.excode_data.RecTest;
import flute.utils.file_processing.JavaTokenizer;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;

import java.io.IOException;
import java.util.*;

public class MethodCallNameRecTestGenerator extends MethodCallRecTestGenerator {
    public MethodCallNameRecTestGenerator(String projectPath, ProjectParser projectParser) {
        super(projectPath, projectParser);
    }

    @Override
    List<? extends RecTest> generateFromMethodCall(List<NodeSequenceInfo> excodes, int methodCallStartIdx, int methodCallEndIdx,
                                                   MethodCallExpr methodCall, String contextMethodCall, String methodScope, String methodName) {

        List<RecTest> tests = new ArrayList<>();

        // Lack of libraries
        if (getFileParser().getCurMethodInvocation().resolveMethodBinding() == null) {
            System.err.println("Cannot resolve: " + methodCall);
            return tests;
        }

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
            test.setMethodInvocationModel(getFileParser().getCurMethodInvocation());
            test.setIgnored(false);

            List<IMethodBinding> methodBindingList = getFileParser().genMethodCall().orElse(null);
            List<String> methodCandidates = new ArrayList<>();
            if (methodBindingList != null) {
                for (IMethodBinding methodBinding: methodBindingList) {
                    methodCandidates.add(Utils.nodeToString(methodBinding));
                }
            }
            test.setNext_lex(methodCandidates);

            tests.add(test);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return tests;
    }

    @Override
    void postProcess(List<RecTest> tests) {
        if (getFileParser() == null) return;
        Map<MethodInvocationModel, MethodCallNameRecTest> testMap = new HashMap<>();
        for (RecTest recTest: tests) {
            if (recTest instanceof MethodCallNameRecTest) {
                MethodCallNameRecTest test = (MethodCallNameRecTest)recTest;
                testMap.put(test.getMethodInvocationModel(), test);
            }
            else return;
        }

        FileNode fileNode = new FileNode(getFileParser());
        fileNode.parse();

        // Build CFGs
        List<MinimalNode> rootNodeList = fileNode.getRootNodeList();
        for (MinimalNode rootNode: rootNodeList) {
            // Build method invoc trees from CFGs
            MethodCallNode methodCallNode = Utils.visitMinimalNode(rootNode);

            // Group by tracking node
            Map<IBinding, MethodCallNode> trackingNodeMap = Utils.groupMethodCallNodeByTrackingNode(methodCallNode);

            for (IBinding id: trackingNodeMap.keySet()) {
                // Generate method invoc sequences
                visitMethodCallNode(trackingNodeMap.get(id), testMap);
            }
        }
    }

    private static void visitMethodCallNode(MethodCallNode node, Map<MethodInvocationModel, MethodCallNameRecTest> testMap) {
        if (node.getValue() != null) {
            visitMethodCallNode(node, testMap, new Stack<>());
        } else {
            for (MethodCallNode childNode : node.getChildNode()) {
                visitMethodCallNode(childNode, testMap, new Stack<>());
            }
        }
    }

    private static void visitMethodCallNode(MethodCallNode node, Map<MethodInvocationModel, MethodCallNameRecTest> testMap, Stack<String> stack) {
        MethodCallNameRecTest test = testMap.getOrDefault(node.getValue(), null);
        if (test != null) {
            test.addMethod_context(String.join(" ", stack));
        }

        stack.push(node.getValue().resolveMethodBinding() != null ?
                Utils.nodeToString(node.getValue().resolveMethodBinding()) :
                Utils.nodeToString(node.getValue()));

        for (MethodCallNode childNode : node.getChildNode()) {
            visitMethodCallNode(childNode, testMap, stack);
        }
        stack.pop();
    }
}
