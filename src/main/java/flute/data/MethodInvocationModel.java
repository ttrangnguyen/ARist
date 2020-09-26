package flute.data;

import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;

public class MethodInvocationModel {
    private Expression expression = null;
    private ITypeBinding expressionType = null;
    private ASTNode orgASTNode = null;
    private IMethodBinding methodBinding = null;
    List arguments = new ArrayList();
    ITypeBinding curClass;
    SimpleName methodName;

    public MethodInvocationModel(ITypeBinding curClass, MethodInvocation methodInvocation) {
        this.curClass = curClass;
        orgASTNode = methodInvocation;
        methodBinding = methodInvocation.resolveMethodBinding();
        expression = methodInvocation.getExpression();
        expressionType = methodInvocation.getExpression() == null ? null : methodInvocation.getExpression().resolveTypeBinding();
        arguments = methodInvocation.arguments();
        methodName = methodInvocation.getName();
    }

    public MethodInvocationModel(ITypeBinding curClass, SuperMethodInvocation superMethodInvocation) {
        this.curClass = curClass;
        orgASTNode = superMethodInvocation;
        methodBinding = superMethodInvocation.resolveMethodBinding();
        expression = superMethodInvocation;
        expressionType = curClass.getSuperclass();
        arguments = superMethodInvocation.arguments();
        methodName = superMethodInvocation.getName();
    }

    public boolean isStaticExpression() {
        if (orgASTNode instanceof SuperMethodInvocation) return false;
        return curClass.getName().equals(expression.toString())
                || curClass.getQualifiedName().equals(expression.toString());
    }

    public List arguments() {
        return arguments;
    }

    public SimpleName getName() {
        return methodName;
    }

    public Expression getExpression() {
        return expression;
    }

    public ITypeBinding getExpressionType() {
        return expressionType;
    }

    public IMethodBinding resolveMethodBinding() {
        return methodBinding;
    }


    public ASTNode getOrgASTNode() {
        return orgASTNode;
    }

    @Override
    public String toString() {
        return orgASTNode.toString();
    }
}
