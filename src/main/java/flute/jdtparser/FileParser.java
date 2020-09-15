package flute.jdtparser;

import com.google.common.collect.Lists;
import flute.data.constraint.ParserConstant;
import flute.data.type.*;
import flute.data.typemodel.ClassModel;
import flute.data.typemodel.Variable;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.*;

import flute.data.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class FileParser {
    private ProjectParser projectParser;
    private File curFile;
    private int curPosition;

    private ITypeBinding curClass;

    private CompilationUnit cu;

    private boolean isStatic = false;

    public List<Variable> visibleVariables = new ArrayList<>();
    private HashMap<String, Integer> initVariables = new HashMap<>();

    public HashMap<String, ClassModel> visibleClass = new HashMap<>();

    /**
     * Create parser with position by length
     *
     * @param projectParser
     * @param curFile
     * @param curPosition
     */
    public FileParser(ProjectParser projectParser, File curFile, int curPosition) {
        this.projectParser = projectParser;
        this.curFile = curFile;
        this.curPosition = curPosition;
        cu = projectParser.createCU(curFile);
    }


    /**
     * Create parser with position by file content and length
     *
     * @param projectParser
     * @param fileName
     * @param fileContent
     * @param curPosition
     */
    public FileParser(ProjectParser projectParser, String fileName, String fileContent, int curPosition) {
        this.projectParser = projectParser;
        this.curFile = null;
        this.curPosition = curPosition;
        cu = projectParser.createCU(fileName, fileContent);
    }

    /**
     * Create parser with position by line, height
     *
     * @param projectParser
     * @param curFile
     * @param curLine
     * @param curColumn
     */
    public FileParser(ProjectParser projectParser, File curFile, int curLine, int curColumn) {
        this.projectParser = projectParser;
        this.curFile = curFile;
        cu = projectParser.createCU(curFile);
        this.curPosition = this.getPosition(curLine, curColumn);
    }


    /**
     * Create parser with position by file content and line, height
     *
     * @param projectParser
     * @param fileName
     * @param fileContent
     * @param curLine
     * @param curColumn
     */
    public FileParser(ProjectParser projectParser, String fileName, String fileContent, int curLine, int curColumn) {
        this.projectParser = projectParser;
        this.curFile = null;
        cu = projectParser.createCU(fileName, fileContent);
        this.curPosition = this.getPosition(curLine, curColumn);
    }

    /**
     * If the result is empty, type checking is passed
     *
     * @param startPos
     * @param stopPos
     * @return List of errors in between two position.
     */
    public List<IProblem> getErrors(int startPos, int stopPos) {
        List<IProblem> problemList = new ArrayList<>();
        IProblem[] iProblems = cu.getProblems();
        for (IProblem iProblem : iProblems) {
            if (iProblem.isError()) {
                if (iProblem.getSourceEnd() < startPos || iProblem.getSourceStart() > stopPos) continue;
                problemList.add(iProblem);
            }
        }
        return problemList;
    }

    public boolean typeCheck(int startPos, int stopPos) {
        cu = projectParser.createCU(curFile);
        TypeChecker typeChecker = new TypeChecker(cu, startPos, stopPos);
        return typeChecker.check();
    }

    public void testAssigment() {
        cu.accept(new ASTVisitor() {
            public boolean visit(Assignment assignment) {
                ITypeBinding d = assignment.getLeftHandSide().resolveTypeBinding();
                ITypeBinding m = assignment.getRightHandSide().resolveTypeBinding();
                if (d != null && m != null) {
                    Object r = m.isAssignmentCompatible(d);
                    boolean re1 = d.isSubTypeCompatible(m);
                    boolean re2 = m.isSubTypeCompatible(d);
                }
                return true;
            }
        });
    }

    /**
     * When change position, the parse process will run again.
     *
     * @param position
     * @throws Exception
     */
    public void setPosition(int position) throws Exception {
        this.curPosition = position;
        parse();
    }

    /**
     * Run it after generate file parser. It will parse visible variables with the current position.
     *
     * @throws Exception
     */
    public void parse() throws Exception {
        try {
            ITypeBinding clazz = getClassScope(curPosition);

            if (clazz != curClass) {
                curClass = clazz;
                visibleClass = projectParser.getListAccess(clazz);
            }

        } catch (NullPointerException err) {
            visibleClass.clear();
            throw new Exception("Can not get class scope");
        }

        ASTNode scope = getScope(curPosition);

        if (scope != null) {
            getVariableScope(scope);
        } else {
            visibleVariables.clear();
            initVariables.clear();
        }
    }

//    public List<Variable> getVariableScope(ASTNode astNode) {
//        List<Variable> listVariable = new ArrayList<>();
//        getVariableScope(astNode, listVariable);
//        return listVariable;
//    }

    /**
     * @return First params can append the position of a method invocation.
     */
    public MultiMap genFirstParams() {
        return genNextParams(0);
    }

    /**
     * @return Next params can append the position of a method invocation with some pre-written parameters.
     */
    public MultiMap genNextParams() {
        return genNextParams(-1);
    }

    /**
     * @return Next params can append the input position of a method invocation with sublist of pre-written parameters.
     */
    public MultiMap genParamsAt(int position) {
        return genNextParams(position);
    }

    private MultiMap genNextParams(int position) {
        final ASTNode[] astNode = {null};

        cu.accept(new ASTVisitor() {
            public void preVisit(ASTNode node) {
                if (node instanceof MethodInvocation && node.getStartPosition() <= curPosition
                        && curPosition <= (node.getStartPosition() + node.getLength())) {
                    astNode[0] = node;
                }
            }
        });

        if (astNode[0] == null) return null;

        MethodInvocation methodInvocation = (MethodInvocation) astNode[0];
        String methodName = methodInvocation.getName().getIdentifier();

        boolean isStaticExpr = false;
        ITypeBinding classBinding;

        if (methodInvocation.getExpression() == null) {
            classBinding = curClass;
            isStaticExpr = isStaticScope(methodInvocation);
        } else {
            Expression expr = methodInvocation.getExpression();
            classBinding = expr.resolveTypeBinding();
            isStaticExpr = curClass.getName().equals(expr.toString());
        }

        List preArgs = position >= 0 ? methodInvocation.arguments().subList(0, position) : methodInvocation.arguments();

        ClassParser classParser = new ClassParser(classBinding);

        List<IMethodBinding> listMember = new ArrayList<>();

        List<IMethodBinding> methodBindings = classParser.getMethodsFrom(curClass, isStaticExpr);

        for (IMethodBinding methodBinding : methodBindings) {
            if (methodName.equals(methodBinding.getName())) {
                //Add filter for parent expression
                if (parentValue(methodInvocation) == null
                        || compareWithMultiType(methodBinding.getReturnType(), parentValue(methodInvocation))) {
                    if (checkInvoMember(preArgs, methodBinding)) {
                        listMember.add(methodBinding);
                    }
                }
            }
        }

        List<String> nextVariable = new ArrayList<>();

        MultiMap nextVariableMap = new MultiMap();

        int methodArgLength = preArgs.size();
        methodArgLength = methodArgLength > 0 && preArgs.get(methodArgLength - 1).toString().equals("$missing$")
                ? methodArgLength - 1 : methodArgLength;

        int finalMethodArgLength = methodArgLength;
        listMember.forEach(methodBinding ->
        {
            ITypeBinding[] params = methodBinding.getParameterTypes();
            if (finalMethodArgLength == params.length && !methodBinding.isVarargs()) {
                nextVariable.add(")");
                nextVariableMap.put("CLOSE_PART", ")");
            } else {
                visibleVariables.stream().filter(variable -> variable.isInitialized()).forEach(variable -> {
                    int compareValue = compareParam(variable.getTypeBinding(), methodBinding, finalMethodArgLength);
                    if (!nextVariable.contains(variable.getName())
                            && compareValue != ParserConstant.FALSE_VALUE) {
                        nextVariable.add(variable.getName());
                        String exCode = "VAR(" + variable.getTypeBinding().getName() + ")";
                        nextVariableMap.put(exCode, variable.getName());
                        if (compareValue == ParserConstant.VARARGS_TRUE_VALUE && !nextVariable.contains(")")) {
                            nextVariable.add(")");
                            nextVariableMap.put("CLOSE_PART", ")");
                        }
                    }

                    ITypeBinding variableClass = variable.getTypeBinding();

                    if (variableClass != null) {
                        List<IVariableBinding> varFields = new ClassParser(variableClass).getFieldsFrom(curClass);
                        for (IVariableBinding varField : varFields) {
                            ITypeBinding varMemberType = varField.getType();
                            int compareFieldValue = compareParam(varMemberType, methodBinding, finalMethodArgLength);
                            if (compareFieldValue != ParserConstant.FALSE_VALUE) {
                                String nextVar = variable.getName() + "." + varField.getName();
                                if (!nextVariable.contains(nextVar)) {
                                    String exCode = "VAR(" + variable.getTypeBinding().getName() + ") "
                                            + "F_ACCESS(" + varMemberType.getName() + "," + varField.getName() + ")";
                                    nextVariable.add(nextVar);
                                    nextVariableMap.put(exCode, nextVar);
                                }
                                if (compareFieldValue == ParserConstant.VARARGS_TRUE_VALUE && !nextVariable.contains(")")) {
                                    nextVariable.add(")");
                                    nextVariableMap.put("CLOSE_PART", ")");
                                }
                            }
                        }
                    }
                });
            }
        });

        return nextVariableMap;
    }

    public static int compareParam(ITypeBinding varType, IMethodBinding methodBinding, int position) {
        if (methodBinding.getParameterTypes().length > position
                && ((varType.isAssignmentCompatible(methodBinding.getParameterTypes()[position])))
        ) {
            return ParserConstant.TRUE_VALUE;
        }

        if (methodBinding.isVarargs()
                && methodBinding.getParameterTypes().length - 1 <= position
                && methodBinding.getParameterTypes()[methodBinding.getParameterTypes().length - 1].isArray()) {
            if (varType.isAssignmentCompatible(methodBinding.getParameterTypes()[methodBinding.getParameterTypes().length - 1].getComponentType())) {
                return ParserConstant.VARARGS_TRUE_VALUE;
            }
        }

        return compareWithGenericType(varType, methodBinding, position);
    }

    public static int compareWithGenericType(ITypeBinding varType, IMethodBinding methodBinding, int position) {
        ITypeBinding paramType = methodBinding.isVarargs() ? methodBinding.getParameterTypes()[methodBinding.getParameterTypes().length - 1]
                : methodBinding.getParameterTypes()[position];
        ITypeBinding elementType = paramType.isArray() ? paramType.getElementType() : paramType;
        //break when is not generic type in method
        if (!elementType.isTypeVariable()) return ParserConstant.FALSE_VALUE;
        //can append primitive when type param is declared in method
        //boolean isMethodDeclare = Arrays.asList(methodBinding.getParameterTypes()).contains(elementType);
        if (!methodBinding.isVarargs() && position < methodBinding.getParameterTypes().length - 1) {
            if (varType.getDimensions() != paramType.getDimensions())
                return ParserConstant.FALSE_VALUE;
            if (paramType.isCastCompatible(varType)) {
                return ParserConstant.TRUE_VALUE;
            } else if (varType.isPrimitive() || (varType.isArray() && varType.getElementType().isPrimitive())) {
                //check type is primitive
                if (TypeConstraintKey.WRAP_TYPES.contains(elementType.getSuperclass().getKey())) {
                    return ParserConstant.TRUE_VALUE;
                }
            }
        } else {
            //can apply array to varargs type
            if (paramType.getDimensions() - varType.getDimensions() > 1) return ParserConstant.FALSE_VALUE;
            if (varType.isPrimitive() || (varType.isArray() && varType.getElementType().isPrimitive())) {
                //check type is primitive
                if (paramType.getDimensions() > 1) return ParserConstant.FALSE_VALUE;
//                || (!isMethodDeclare && )
                if (TypeConstraintKey.WRAP_TYPES.contains(elementType.getSuperclass().getKey())) {
                    return ParserConstant.VARARGS_TRUE_VALUE;
                }
            } else if (elementType.isCastCompatible(varType) || (varType.isArray() && elementType.isCastCompatible(varType.getElementType()))) {
                return ParserConstant.VARARGS_TRUE_VALUE;
            }
        }

        return ParserConstant.FALSE_VALUE;
    }

    public int getPosition(int line, int column) {
        return cu.getPosition(line, column);
    }

    public ITypeBinding[] parentValue(MethodInvocation methodInvocation) {
        ASTNode parentNode = methodInvocation.getParent();
        if (parentNode instanceof Assignment) {
            Assignment assignment = (Assignment) parentNode;
            return new ITypeBinding[]{assignment.getLeftHandSide().resolveTypeBinding()};
        } else if (parentNode instanceof VariableDeclarationFragment) {
            VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) parentNode;
            return new ITypeBinding[]{variableDeclarationFragment.resolveBinding().getType()};
        } else if (parentNode instanceof ReturnStatement) {
            ASTNode methodNode = getMethodScope(parentNode);
            if (methodNode instanceof MethodDeclaration) {
                ITypeBinding returnType = ((MethodDeclaration) methodNode).getReturnType2().resolveBinding();
                return new ITypeBinding[]{returnType};
            } else {
                return null;
            }
        } else if (parentNode instanceof IfStatement) {
            return new ITypeBinding[]{new IBooleanType()};
        } else if (parentNode instanceof MethodInvocation) {
            MethodInvocation methodInvocationParent = (MethodInvocation) parentNode;
            //if method call is a param of method call
            if (methodInvocationParent.arguments().contains(methodInvocation)) {
                ITypeBinding[] parentTypes = parentValue(methodInvocationParent);
                List<ITypeBinding> typeResults = new ArrayList<>();

                ClassParser classParser;
                boolean isStaticExpr = false;
                if (methodInvocationParent.getExpression() == null) {
                    classParser = new ClassParser(curClass);
                    isStaticExpr = isStaticScope(methodInvocation);
                } else {
                    Expression expr = methodInvocationParent.getExpression();
                    classParser = new ClassParser(expr.resolveTypeBinding());
                    isStaticExpr = expr.toString().equals(expr.resolveTypeBinding().getName());
                }

                classParser.getMethodsFrom(curClass, isStaticExpr).forEach(methodMember -> {
                    if (methodMember.getName().equals(methodInvocationParent.getName().getIdentifier())) {

                        if (parentTypes == null ||
                                compareWithMultiType(methodMember.getReturnType(), parentTypes)) {
                            int positionParam = -1;

                            for (int i = 0; i < methodInvocationParent.arguments().size(); i++) {
                                if (methodInvocation == methodInvocationParent.arguments().get(i)) {
                                    positionParam = i;
                                    break;
                                }
                            }

                            if (checkInvoMember(methodInvocationParent.arguments(), methodMember, positionParam)) {
                                if (methodMember.isVarargs() && methodMember.getParameterTypes().length - 1 == positionParam) {
                                    typeResults.add(methodMember.getParameterTypes()[positionParam].getComponentType());
                                }
                                typeResults.add(methodMember.getParameterTypes()[positionParam]);
                            }
                        }
                    }
                });
                return typeResults.toArray(new ITypeBinding[0]);
            }
        }
        return null;
    }

    private boolean compareWithMultiType(ITypeBinding iTypeBinding, ITypeBinding[] iTypeBindings) {
        for (int i = 0; i < iTypeBindings.length; i++) {
            if (iTypeBinding.isAssignmentCompatible(iTypeBindings[i])
                    || (iTypeBindings[i] instanceof IGenericType && ((IGenericType) iTypeBindings[i]).canBeAssignmentBy(iTypeBinding))) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkInvoMember(List args, IMethodBinding iMethodBinding, int ignorPos) {
        int index = 0;
        for (Object argument :
                args) {
            if (index == ignorPos) {
                index++;
                continue;
            }
            if (argument instanceof Expression) {
                Expression argExpr = (Expression) argument;
//                ITypeBinding[] params = iMethodBinding.getParameterTypes();
                if (compareParam(argExpr.resolveTypeBinding(), iMethodBinding, index++) == 0) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    public static boolean checkInvoMember(List args, IMethodBinding iMethodBinding) {
        int index = 0;
        for (Object argument :
                args) {
            if (argument instanceof Expression) {
                Expression argExpr = (Expression) argument;
                if (argument.toString().equals("$missing$")) {
                    args.remove(argument);
                    break;
                }
//                ITypeBinding[] params = iMethodBinding.getParameterTypes();
                if (argExpr.resolveTypeBinding() == null
                        || compareParam(argExpr.resolveTypeBinding(), iMethodBinding, index++) == 0) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    public static boolean isStaticScope(ASTNode astNode) {
        ASTNode methodScope = getMethodScope(astNode);
        if (methodScope instanceof MethodDeclaration) {
            return Modifier.isStatic(((MethodDeclaration) methodScope).getModifiers());
        } else if (methodScope instanceof Initializer) {
            return Modifier.isStatic(((Initializer) methodScope).getModifiers());
        }
        return false;
    }

    public static ASTNode getMethodScope(ASTNode astNode) {
        if (astNode instanceof MethodDeclaration || astNode instanceof Initializer) {
            return astNode;
        } else if (astNode.getParent() != null) {
            return getMethodScope(astNode.getParent());
        }
        return null;
    }

    private void getVariableScope(ASTNode astNode) {
        if (astNode == null) return;
        Block block = null;
        if (astNode instanceof Block) {
            block = (Block) astNode;
        } else if (astNode instanceof MethodDeclaration) {
            MethodDeclaration methodDeclaration = (MethodDeclaration) astNode;
            isStatic = Modifier.isStatic(methodDeclaration.getModifiers());
//            block = methodDeclaration.getBody();

            List params = methodDeclaration.parameters();
            params.forEach(param -> {
                if (param instanceof SingleVariableDeclaration) {
                    SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) param;
                    int position = singleVariableDeclaration.getStartPosition();

                    IVariableBinding variableBinding = singleVariableDeclaration.resolveBinding();
                    addVariableToList(position, variableBinding, true, true);
                }
            });

            if (!isStatic) {
                Variable variable = new Variable(curClass, "this");
                variable.setStatic(false);
                variable.setInitialized(true);
                visibleVariables.add(variable);
            }

        } else if (astNode instanceof Initializer) {
            Initializer initializer = (Initializer) astNode;
            isStatic = true;
        } else if (astNode instanceof TypeDeclaration) {
            FieldDeclaration[] fields = ((TypeDeclaration) astNode).getFields();

            for (FieldDeclaration field : fields) {
                int position = field.getStartPosition();
                List fragments = field.fragments();

                fragments.forEach(fragment -> {
                    if (fragment instanceof VariableDeclarationFragment) {
                        VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) fragment;
                        IVariableBinding variableBinding = variableDeclarationFragment.resolveBinding();

                        boolean isStatic = Modifier.isStatic(variableBinding.getModifiers());
                        addVariableToList(position, variableBinding, isStatic, true);
                    }
                });
            }
        } else if (astNode instanceof LambdaExpression) {
            LambdaExpression lambdaExpression = (LambdaExpression) astNode;
            List params = lambdaExpression.parameters();
            params.forEach(param -> {
                if (param instanceof VariableDeclarationFragment) {
                    VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) param;
                    int position = variableDeclarationFragment.getStartPosition();
                    IVariableBinding variableBinding = variableDeclarationFragment.resolveBinding();

                    addVariableToList(position, variableBinding, false, true);
                }
            });
        } else if (astNode instanceof ForStatement) {
            ForStatement forStatement = (ForStatement) astNode;
            List inits = forStatement.initializers();
            inits.forEach(init -> {
                if (init instanceof VariableDeclarationExpression) {
                    VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression) init;
                    int position = variableDeclarationExpression.getStartPosition();
                    List variableDeclarations = variableDeclarationExpression.fragments();
                    variableDeclarations.forEach(variableDeclarationItem -> {
                        if (variableDeclarationItem instanceof VariableDeclarationFragment) {
                            IVariableBinding variableBinding = ((VariableDeclarationFragment) variableDeclarationItem).resolveBinding();

                            addVariableToList(position, variableBinding, false, true);
                        }
                    });
                }
            });
        } else if (astNode instanceof EnhancedForStatement) {
            EnhancedForStatement enhancedForStatement = (EnhancedForStatement) astNode;
            SingleVariableDeclaration singleVariableDeclaration = enhancedForStatement.getParameter();

            int position = singleVariableDeclaration.getStartPosition();

            IVariableBinding variableBinding = singleVariableDeclaration.resolveBinding();

            addVariableToList(position, variableBinding, false, true);
        }


        if (block != null) {
            List listStatement = block.statements();
            Lists.reverse(listStatement).forEach(stmt -> {
                if (stmt instanceof VariableDeclarationStatement) {
                    VariableDeclarationStatement declareStmt = (VariableDeclarationStatement) stmt;
                    int position = declareStmt.getStartPosition();
                    declareStmt.fragments().forEach(fragment -> {
                        if (fragment instanceof VariableDeclarationFragment) {
                            VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) fragment;
                            IVariableBinding variableBinding = variableDeclarationFragment.resolveBinding();
                            addVariableToList(position, variableBinding, false,
                                    initVariables.get(variableBinding.getName()) != null
                                            || (variableDeclarationFragment.getInitializer() != null && (variableDeclarationFragment.getStartPosition() + variableDeclarationFragment.getLength()) < curPosition));
                        }
                    });
                }
                if (stmt instanceof ExpressionStatement) {
                    if (((ExpressionStatement) stmt).getExpression() instanceof Assignment) {
                        Assignment assignment = (Assignment) ((ExpressionStatement) stmt).getExpression();
                        if (assignment.getLeftHandSide() instanceof SimpleName) {
                            addInitVariable(assignment.getLeftHandSide().toString(), assignment.getStartPosition() + assignment.getLength());
                        }
                    }

                }
            });
        }

        getVariableScope(getParentBlock(astNode));
    }

    private void addInitVariable(String variableName, int endPosition) {
        if (endPosition < curPosition) {
            initVariables.put(variableName, endPosition);
        }
    }

    private void addVariableToList(int startPosition, IVariableBinding variableBinding, boolean isStatic, boolean isInitialized) {
        ITypeBinding typeBinding = variableBinding.getType();
        String varName = variableBinding.getName();

        if (this.isStatic == true && isStatic == false) return;

        if (!checkVariableInList(varName, visibleVariables) && startPosition <= curPosition) {
            Variable variable = new Variable(typeBinding, varName);
            variable.setStatic(isStatic);
            variable.setInitialized(isInitialized);
            visibleVariables.add(variable);
        }
    }

    public static boolean checkVariableInList(String varName, List<Variable> variableList) {
        for (Variable variableTmp : variableList) {
            if (varName.equals(variableTmp.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param astNode
     * @return Get parent block nearest ASTNode, that have type MethodDeclaration, Initializer,
     * TypeDeclaration, Block, LambdaExpression, ForStatement, ForStatement
     */
    public static ASTNode getParentBlock(ASTNode astNode) {
        if (astNode == null) return null;
        ASTNode parentNode = astNode.getParent();
        if (parentNode instanceof Block) {
//            if (parentNode.getParent() instanceof MethodDeclaration) return parentNode.getParent();
            return parentNode; //block object
        } else if (parentNode instanceof MethodDeclaration || parentNode instanceof Initializer) {
            return parentNode;
        } else if (parentNode instanceof TypeDeclaration) {
            return parentNode;
        } else if (parentNode instanceof LambdaExpression) {
            return parentNode;
        } else if (parentNode instanceof ForStatement || parentNode instanceof ForStatement) {
            return parentNode;
        } else return getParentBlock(parentNode);
    }

    /**
     * @param position
     * @return Parent class nearest of position.
     * @throws NullPointerException
     */
    public ITypeBinding getClassScope(int position) throws NullPointerException {
        final TypeDeclaration[] result = {null};
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                if (isNode(position, node)) {
                    result[0] = node;
                }
                return true;
            }
        });
        if (result[0] == null) throw new NullPointerException();
        return result[0].resolveBinding();
    }

    public boolean isBlockScope() {
        //can code in this scope
        ASTNode astNode = getScope(curPosition);
        ASTNode parent = getParentBlock(astNode);
        if ((parent instanceof TypeDeclaration) || parent == null) {
            return false;
        }

        return true;
    }

    public ASTNode getScope(int position) {
        final ASTNode[] astNode = {null};

        cu.accept(new ASTVisitor() {
            public void preVisit(ASTNode node) {
                if (isNode(position, node)) {
                    astNode[0] = node;
                }
            }
        });
        return astNode[0];
    }


    public static boolean isNode(int pos, ASTNode astNode) {
        int cPosStart = astNode.getStartPosition();
        int cPosEnd = astNode.getStartPosition() + astNode.getLength();
        if (cPosStart <= pos && cPosEnd >= pos) {
            return true;
        }
        return false;
    }

    public File getCurFile() {
        return curFile;
    }

    public int getCurPosition() {
        return curPosition;
    }

    public CompilationUnit getCu() {
        return cu;
    }

    public List<Variable> getVisibleVariables() {
        return visibleVariables;
    }

    public HashMap<String, ClassModel> getVisibleClass() {
        return visibleClass;
    }
}
