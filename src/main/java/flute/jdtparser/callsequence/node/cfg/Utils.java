package flute.jdtparser.callsequence.node.cfg;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;

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
}
