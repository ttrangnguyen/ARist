package flute.jdtparser.callsequence.node.cfg;

import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;

public class Utils {
    public static List<MethodInvocation> extractNode(MinimalNode minimalNode) {
        List<MethodInvocation> methodInvocationList = new ArrayList<>();
        if (minimalNode instanceof StmtNode) {
            StmtNode stmtNode = (StmtNode) minimalNode;
            return visitMethodCall(stmtNode.getStatement());
        } else if (minimalNode instanceof IfNode) {
            IfNode ifNode = (IfNode) minimalNode;
            return visitMethodCall(ifNode.getExpression());
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
}
