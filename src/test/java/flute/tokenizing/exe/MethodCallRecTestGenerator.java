package flute.tokenizing.exe;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import flute.jdtparser.ProjectParser;
import flute.tokenizing.excode_data.NodeSequenceInfo;
import flute.tokenizing.excode_data.RecTest;
import flute.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class MethodCallRecTestGenerator extends RecTestGenerator {

    public MethodCallRecTestGenerator(String projectPath, ProjectParser projectParser) {
        super(projectPath, projectParser);
    }

    @Override
    List<RecTest> generateInMethodScope(List<NodeSequenceInfo> excodes, int methodDeclarationStartIdx, int methodDeclarationEndIdx) {
        List<RecTest> tests = new ArrayList<>();
        List<Integer> stack = new ArrayList<>();

        MethodDeclaration methodDeclaration = (MethodDeclaration) excodes.get(methodDeclarationStartIdx).oriNode;
        String methodDeclarationContent = methodDeclaration.toString();
        for (int i = methodDeclarationStartIdx; i < methodDeclarationEndIdx; ++i) {
            NodeSequenceInfo excode = excodes.get(i);

            if (NodeSequenceInfo.isMethodAccess(excode)) stack.add(i);
            if (NodeSequenceInfo.isClosePart(excode)
                    && !stack.isEmpty() && excode.oriNode == excodes.get(stack.get(stack.size() - 1)).oriNode) {

                MethodCallExpr methodCall = (MethodCallExpr) excode.oriNode;
                String methodCallContent = methodCall.toString();

                try {
                    getFileParser().setPosition(methodCall.getBegin().get().line, methodCall.getBegin().get().column);
                } catch (Exception e) {
                    // TODO: Handle enums
                    e.printStackTrace();

                    stack.remove(stack.size() - 1);
                    continue;
                }

                //TODO: Handle multiple-line method invocation
                String contextMethodCall = methodDeclarationContent.substring(0, StringUtils.indexOf(methodDeclarationContent, StringUtils.getFirstLine(methodCallContent)));
                String methodName = methodCall.getNameAsString();
                String methodScope = "";
                if (methodCall.getScope().isPresent()) {
                    methodScope = methodCall.getScope().get() + ".";
                    methodName = methodScope + methodName;
                }

                int curPos = getFileParser().getCurPosition();
                curPos += methodScope.length();
                try {
                    getFileParser().setPosition(curPos);
                } catch (Exception e) {
                    // TODO: Handle enums
                    e.printStackTrace();

                    stack.remove(stack.size() - 1);
                    continue;
                }
                //System.out.println("Position: " + methodCall.getBegin().get());

                tests.addAll(generateFromMethodCall(excodes, stack.get(stack.size() - 1), i, methodCall, contextMethodCall, methodName));

                stack.remove(stack.size() - 1);
            }
        }

        return tests;
    }

    abstract List<RecTest> generateFromMethodCall(List<NodeSequenceInfo> excodes, int methodCallStartIdx, int methodCallEndIdx,
                                                  MethodCallExpr methodCall, String contextMethodCall, String methodName);
}
