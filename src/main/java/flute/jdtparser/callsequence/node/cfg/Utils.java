package flute.jdtparser.callsequence.node.cfg;

import flute.jdtparser.callsequence.node.ast.CaseBlock;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class Utils {
    public static List<MethodInvocation> extractNode(MinimalNode minimalNode) {
        List<MethodInvocation> methodInvocationList = new ArrayList<>();
        if (minimalNode instanceof StmtNode) {
            StmtNode stmtNode = (StmtNode) minimalNode;
            return visitMethodCall(stmtNode.getStatement());
        } else if (minimalNode instanceof IfNode) {
            IfNode ifNode = (IfNode) minimalNode;
            return visitMethodCall(ifNode.getExpression());
        } else if (minimalNode instanceof SwitchNode) {
            SwitchNode switchNode = (SwitchNode) minimalNode;
            return visitMethodCall(switchNode.getExpression());
        } else if (minimalNode instanceof CaseNode) {
            CaseNode caseNode = (CaseNode) minimalNode;
            return visitMethodCall(caseNode.getExpression());
        }
        return methodInvocationList;
    }

    private static List<MethodInvocation> visitMethodCall(ASTNode astNode) {
        List<MethodInvocation> methodInvocationList = new ArrayList<>();
        if (astNode != null)
            astNode.accept(new ASTVisitor() {
                @Override
                public boolean visit(MethodInvocation methodInvocation) {
                    methodInvocationList.add(methodInvocation);
                    return super.visit(methodInvocation);
                }
            });
        return methodInvocationList;
    }

    public static String nodeToString(IMethodBinding methodInvocation) {
        String result;
        String identifierName = String.join(".",
                methodInvocation.getDeclaringClass().getQualifiedName(),
                methodInvocation.getName());
        String paramTypes = "";
        for (ITypeBinding param : methodInvocation.getParameterTypes()) {
            if (paramTypes.length() == 0) {
                paramTypes = param.getQualifiedName();
            } else {
                paramTypes = String.join(",", paramTypes, param.getQualifiedName());
            }
        }
        result = identifierName + "(" + paramTypes + ")";

        result += methodInvocation.getReturnType().getQualifiedName();

        return result;
    }

    public static List<CaseBlock> parseCaseBlock(SwitchStatement switchStatement) {
        List<CaseBlock> caseBlocks = new ArrayList<>();
        AtomicReference<CaseBlock> caseBlock = new AtomicReference<>();
        switchStatement.statements().forEach(stmt -> {
            Statement statement = (Statement) stmt;
            if (statement instanceof SwitchCase) {
                if (caseBlock.get() != null) caseBlocks.add(caseBlock.get());
                SwitchCase switchCase = (SwitchCase) statement;
                caseBlock.set(new CaseBlock(switchCase.getExpression()));
            } else {
                caseBlock.get().statements().add(statement);
            }
        });
        return caseBlocks;
    }
}
