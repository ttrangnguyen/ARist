package flute.jdtparser;

import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

enum CheckValue {
    FALSE(-1), UNKNOWN(0), CAST(1), TRUE(2);
    private int value;

    public int getValue() {
        return this.value;
    }

    CheckValue(int value) {
        this.value = value;
    }
}

public class TypeChecker {
    private CompilationUnit cu;
    private int startPos;
    private int stopPos;

    public TypeChecker(CompilationUnit cu, int startPos, int stopPos) {
        this.cu = cu;
        Object m = cu.getProblems();

        this.startPos = startPos;
        this.stopPos = stopPos;
    }

    public boolean check() {
        TypeCheckerVisitor visitor = new TypeCheckerVisitor(startPos, stopPos);
        cu.accept(visitor);
        return visitor.status == CheckValue.TRUE;
    }
}

class TypeCheckerVisitor extends ASTVisitor {
    public CheckValue status = CheckValue.TRUE;
    private int startPos, stopPos;

    public TypeCheckerVisitor(int startPos, int stopPos) {
        this.startPos = startPos;
        this.stopPos = stopPos;
    }

    @Override
    public boolean preVisit2(ASTNode astNode) {
        int start = astNode.getStartPosition();
        int end = start + astNode.getLength();
        if ((start < startPos && end < startPos) || start > stopPos) return false;
        return true;
    }

    //Assignment, VariableDeclarationFragment, PrefixExpression, PostfixExpression, InfixExpression, MethodInvocation, ConstructorInvocation
    @Override
    public boolean visit(VariableDeclarationFragment variableDeclarationFragment) {
        Expression init = variableDeclarationFragment.getInitializer();
        if (init == null) return false;

        IVariableBinding variableBinding = variableDeclarationFragment.resolveBinding();


        ITypeBinding leftType = variableBinding.getType();
        ITypeBinding rightType = init.resolveTypeBinding();

        addValue(compareType(leftType, rightType));
        return true;
    }

    @Override
    public boolean visit(PrefixExpression prefixExpression) {
        return typeExprCalc(prefixExpression);
    }

    @Override
    public boolean visit(PostfixExpression postfixExpression) {
        return typeExprCalc(postfixExpression);
    }

    @Override
    public boolean visit(InfixExpression infixExpression) {
        return typeExprCalc(infixExpression);
    }

    private boolean typeExprCalc(Expression expression) {
        ITypeBinding typeBinding = expression.resolveTypeBinding();
        if (typeBinding == null) {
            // infix expr can not be calculate
            addValue(CheckValue.FALSE);
            return false;
        } else {
            addValue(CheckValue.TRUE);
        }
        return true;
    }

    @Override
    public boolean visit(Assignment assignment) {
        ITypeBinding leftType = assignment.getLeftHandSide().resolveTypeBinding();
        ITypeBinding rightType = assignment.getRightHandSide().resolveTypeBinding();

        CheckValue compare = compareType(leftType, rightType);
        if (compare == CheckValue.CAST || !assignment.getOperator().toString().equals("=")) {
            return addValue(CheckValue.TRUE);
        }
        addValue(compare);
        return true;
    }

    public boolean visitArgInvocation(IMethodBinding iMethodBinding, List invocationArgList, boolean isVarargs) {
        List<ITypeBinding> invocationITypeBindings = new ArrayList<>();

        invocationArgList.forEach(node -> {
            if (node instanceof Expression) {
                Expression expr = (Expression) node;
                invocationITypeBindings.add(expr.resolveTypeBinding());
            }
        });


        List<ITypeBinding> methodITypeBindings = Arrays.asList(iMethodBinding.getParameterTypes());

//        if (invocationITypeBindings.size() < methodITypeBindings.size()) {
//            return addValue(CheckValue.FALSE);
//        }

        if (!iMethodBinding.isVarargs()) {
            CheckValue equalListType = checkListType(methodITypeBindings, invocationITypeBindings);
            if (equalListType == CheckValue.TRUE && invocationITypeBindings.size() == methodITypeBindings.size()) {
                addValue(CheckValue.TRUE);
                return true;
            } else {
                return addValue(CheckValue.FALSE);
            }
        } else {
            /** Varargs
             *  Each method can only have one varargs parameter
             *  The varargs argument must be the last parameter **/
            CheckValue equalFirst = checkListType(methodITypeBindings.subList(0, methodITypeBindings.size() - 1), invocationITypeBindings.subList(0, methodITypeBindings.size() - 1));
            ITypeBinding typeVarargs = methodITypeBindings.get(methodITypeBindings.size() - 1);

            ITypeBinding lastType = invocationITypeBindings.get(invocationITypeBindings.size() - 1);

            if (lastType.isAssignmentCompatible(typeVarargs)) return addValue(CheckValue.TRUE);

            if (equalFirst != CheckValue.TRUE || !typeVarargs.isArray()) {
                return addValue(CheckValue.FALSE);
            }
            ITypeBinding componentType = typeVarargs.getComponentType();

            for (int i = methodITypeBindings.size() - 1; i < invocationITypeBindings.size(); i++) {
                if (invocationITypeBindings.get(i).isAssignmentCompatible(componentType)) continue;
                if (invocationITypeBindings.get(i).isCastCompatible(componentType)) {
                    addValue(CheckValue.CAST);
                    continue;
                }
                if (invocationITypeBindings.get(i) == null) {
                    addValue(CheckValue.UNKNOWN);
                    continue;
                }
                addValue(CheckValue.FALSE);
                break;
            }

        }
        return addValue(CheckValue.TRUE);
    }

    public CheckValue checkListType(List<ITypeBinding> listDeclaration, List<ITypeBinding> listInvocation) {
        CheckValue value = CheckValue.TRUE;
        if (listDeclaration.size() != listInvocation.size()) return CheckValue.FALSE;

        for (int i = 0; i < listInvocation.size(); i++) {
            CheckValue compareType = compareType(listDeclaration.get(i), listInvocation.get(i));
            if (compareType.getValue() < value.getValue()) value = compareType;
            if (compareType == CheckValue.FALSE) return CheckValue.FALSE;
        }
        return value;
    }

    @Override
    public boolean visit(ConstructorInvocation constructorInvocation) {
        List invocationArgList = constructorInvocation.arguments();

        IMethodBinding iMethodBinding = constructorInvocation.resolveConstructorBinding();
        //Skip unresolved method
        if (iMethodBinding == null) return true;

        return visitArgInvocation(iMethodBinding, invocationArgList, iMethodBinding.isVarargs());
    }

    @Override
    public boolean visit(MethodInvocation methodInvocation) {
        List invocationArgList = methodInvocation.arguments();

        IMethodBinding iMethodBinding = methodInvocation.resolveMethodBinding();
        //Skip unresolved method
        if (iMethodBinding == null) return true;

        return visitArgInvocation(iMethodBinding, invocationArgList, iMethodBinding.isVarargs());
    }

    @Override
    public boolean visit(ReturnStatement returnStatement) {
        ITypeBinding leftType = getMethodType(returnStatement);

        Expression returnExpression = returnStatement.getExpression();

        if (returnExpression != null) {
            ITypeBinding rightType = returnStatement.getExpression().resolveTypeBinding();
            return addValue(compareType(leftType, rightType));
        } else {
            if (leftType == null || (leftType.isPrimitive() && leftType.getName().equals("void"))) {
                return addValue(CheckValue.TRUE);
            }
        }

        return addValue(CheckValue.FALSE);
    }

    public ITypeBinding getMethodType(ASTNode astNode) {
        if (astNode instanceof MethodDeclaration) {
            if (((MethodDeclaration) astNode).isConstructor()) return null;
            return ((MethodDeclaration) astNode).getReturnType2().resolveBinding();
        }
        return getMethodType(astNode.getParent());
    }

    public CheckValue compareType(ITypeBinding leftType, ITypeBinding rightType) {
        if (leftType == null || rightType == null) return CheckValue.UNKNOWN;
        if (rightType.isAssignmentCompatible(leftType)) return CheckValue.TRUE;
        if (rightType.isCastCompatible(leftType)) {
            return CheckValue.CAST;
        }

        if (leftType.isPrimitive() && rightType.isPrimitive()
                && !leftType.getName().equals("boolean") && !rightType.getName().equals("boolean")) {
            return CheckValue.CAST;
        }

        //Consider Integer, Float, Double,...

        return CheckValue.FALSE;
    }

    public boolean addValue(CheckValue value) {
        //return false to stop visit
        if (value.getValue() < status.getValue())
            status = value;
        if (value.getValue() == -1) return false;
        return true;
    }
}
