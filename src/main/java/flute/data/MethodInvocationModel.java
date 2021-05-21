package flute.data;

import flute.data.typemodel.ArgumentModel;
import flute.jdtparser.FileParser;
import org.eclipse.jdt.core.dom.*;

import java.util.*;
import java.util.stream.Collectors;

public class MethodInvocationModel {
    private Expression expression = null;
    private ITypeBinding expressionType = null;
    private ASTNode orgASTNode = null;
    private IMethodBinding methodBinding = null;
    List arguments = new ArrayList();
    List<ArgumentModel> argumentTypes = new ArrayList<>();
    ITypeBinding curClass;
    SimpleName methodName;

    public MethodInvocationModel(MethodInvocation methodInvocation) {
        this.curClass = null;
        orgASTNode = methodInvocation;
        methodBinding = methodInvocation.resolveMethodBinding();
        expression = methodInvocation.getExpression();
        expressionType = methodInvocation.getExpression() == null ? null : methodInvocation.getExpression().resolveTypeBinding();
        arguments = methodInvocation.arguments();
        genArgumentTypes();
        methodName = methodInvocation.getName();
    }

    public MethodInvocationModel(SuperMethodInvocation superMethodInvocation) {
        this.curClass = null;
        orgASTNode = superMethodInvocation;
        methodBinding = superMethodInvocation.resolveMethodBinding();
        expression = superMethodInvocation;
        expressionType = methodBinding == null ? null : methodBinding.getDeclaringClass();
        arguments = superMethodInvocation.arguments();
        genArgumentTypes();
        methodName = superMethodInvocation.getName();
    }

    public MethodInvocationModel(ITypeBinding curClass, MethodInvocation methodInvocation) {
        this.curClass = curClass;
        orgASTNode = methodInvocation;
        methodBinding = methodInvocation.resolveMethodBinding();
        expression = methodInvocation.getExpression();
        expressionType = methodInvocation.getExpression() == null ? null : methodInvocation.getExpression().resolveTypeBinding();
        arguments = methodInvocation.arguments();
        genArgumentTypes();
        methodName = methodInvocation.getName();
    }

    public MethodInvocationModel(ITypeBinding curClass, SuperMethodInvocation superMethodInvocation) {
        this.curClass = curClass;
        orgASTNode = superMethodInvocation;
        methodBinding = superMethodInvocation.resolveMethodBinding();
        expression = superMethodInvocation;
        expressionType = curClass.getSuperclass();
        arguments = superMethodInvocation.arguments();
        genArgumentTypes();
        methodName = superMethodInvocation.getName();
    }

    private void genArgumentTypes() {
        for (Object argument : arguments) {
            if (argument instanceof Expression) {
                Expression argExpr = (Expression) argument;
                argumentTypes.add(new ArgumentModel(argExpr));
            }
        }
    }

    public boolean isStaticExpression() {
        if (orgASTNode instanceof SuperMethodInvocation) return false;
        if (expressionType != null) {
            return expressionType.getName().equals(expression.toString())
                    || expressionType.getQualifiedName().equals(expression.toString());
        } else {
            FileParser.isStaticScope(orgASTNode);
        }
        return false;
    }

    public List arguments() {
        return arguments;
    }

    public List<ArgumentModel> argumentTypes() {
        return argumentTypes;
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

    public Optional<String> getClassQualifiedName() {
        Optional<String> classQualifiedName;
        try {
            classQualifiedName = Optional.of(methodBinding.getDeclaringClass().getQualifiedName());
        } catch (Exception e) {
            return Optional.empty();
        }
        return classQualifiedName;
    }

    public ASTNode getOrgASTNode() {
        return orgASTNode;
    }

    public String genClassString() {
        return curClass.getQualifiedName();
    }

    public String genMethodString() {
        List<ITypeBinding> params = Arrays.asList(methodBinding.getParameterTypes());
        List<String> paramString = params.stream().map(param -> {
            return param.getName();
        }).collect(Collectors.toList());
        return methodName.toString() + "(" + String.join(",", paramString) + ")";
    }

    public int getParamStartPosition(){
        int start;
        if (orgASTNode instanceof MethodInvocation) {
            MethodInvocation methodInvocation = (MethodInvocation) orgASTNode;
            start = methodInvocation.getName().getStartPosition() + methodInvocation.getName().getLength();
        } else {
            SuperMethodInvocation superMethodInvocation = (SuperMethodInvocation) orgASTNode;
            start = superMethodInvocation.getName().getStartPosition() + superMethodInvocation.getName().getLength() ;
        }
        return start;
    }

    @Override
    public String toString() {
        return orgASTNode.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodInvocationModel that = (MethodInvocationModel) o;
        return Objects.equals(orgASTNode, that.orgASTNode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orgASTNode);
    }
}
