package flute.jdtparser;

import com.google.common.collect.Lists;
import com.google.common.collect.HashBasedTable;

import com.google.common.collect.Table;
import flute.config.Config;
import flute.data.*;
import flute.data.type.*;
import flute.data.constraint.ParserConstant;
import flute.data.exception.*;
import flute.data.typemodel.*;

import flute.jdtparser.utils.ParserCompare;
import flute.jdtparser.utils.ParserUtils;
import flute.utils.file_processing.FileProcessor;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FileParser {
    private ProjectParser projectParser;
    private File curFile;
    private String curFileContent;
    private int curPosition;

    private ITypeBinding curClass;
    private ClassParser curClassParser;

    private ASTNode curBlockScope = null;

    private MethodInvocationModel curMethodInvocation = null;

    private CompilationUnit cu;

    private boolean isStatic = false;

    public List<Variable> visibleVariables = new ArrayList<>();
    private HashMap<String, Integer> initVariables = new HashMap<>();

    public HashMap<String, ClassModel> visibleClass = new HashMap<>();

    public Table<String, String, MethodCallTypeArgument> methodCallArgumentMap = HashBasedTable.create(); //(excode, lex, binding)

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
        this.curFileContent = FileProcessor.read(curFile);
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
        this.curFileContent = fileContent;
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
        this.curFileContent = FileProcessor.read(curFile);
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
        this.curFileContent = fileContent;
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

    public MethodInvocationModel getCurMethodInvocation() {
        return curMethodInvocation;
    }

    public String getLastMethodCallGen() {
        return curMethodInvocation == null ? null : curMethodInvocation.toString();
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
        if (!Config.IGNORE_PARSE_AFTER_SET_POSITION) parse();
    }

    /**
     * When change position, the parse process will run again.
     *
     * @param line
     * @param column
     * @throws Exception
     */
    public void setPosition(int line, int column) throws Exception {
        this.curPosition = getPosition(line, column - 1);
        if (!Config.IGNORE_PARSE_AFTER_SET_POSITION) parse();
    }

    /**
     * Run it after generate file parser. It will parse visible variables with the current position.
     *
     * @throws Exception
     */
    public void parse() throws MethodInvocationNotFoundException, ClassScopeNotFoundException {
        paramPosition = -1;
        try {
            ITypeBinding clazz = getClassScope(curPosition);
            if (clazz != curClass) {
                curClass = clazz;
                curClassParser = new ClassParser(curClass);
                //visibleClass = projectParser.getListAccess(clazz);
            }
            parseCurMethodInvocation();
        } catch (ClassScopeNotFoundException | MethodInvocationNotFoundException err) {
            visibleClass.clear();
            throw err;
        }

        ASTNode scope = getScope(curPosition);

        visibleVariables.clear();
        initVariables.clear();

        if (scope != null) {
            isStatic = isStaticScope(scope);
            curBlockScope = getParentBlock(scope);
            getVariableScope(scope);
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
        return genNextParams(0, null);
    }

    /**
     * @return Next params can append the position of a method invocation with some pre-written parameters.
     */
    public MultiMap genNextParams() {
        return genNextParams(-1, null);
    }

    public MultiMap genCurParams() {
        if (paramPosition != -1) {
            return genNextParams(paramPosition, null);
        } else {
            return null;
        }
    }

    public String getCurContext() {
        return getCurContext(paramPosition);
    }

    public String getCurContext(int paramPos) {
        if (paramPos == 0) {
            return curFileContent.substring(
                    getCurMethodScope().getStartPosition(), getCurMethodInvocation().getParamStartPosition()
            ).concat("(");
        }
        if (paramPos < 0) return "";
        ASTNode curArg = (ASTNode) getCurMethodInvocation().arguments().get(paramPos);
        return curFileContent.substring(getCurMethodScope().getStartPosition(), curArg.getStartPosition());
    }

    /**
     * @return Next params can append the input position of a method invocation with sublist of pre-written parameters.
     */
    public MultiMap genParamsAt(int position, String... keys) {
        return genNextParams(position, null, keys);
    }

    public static int paramPos;

    private MultiMap genNextParams(int position, IMethodBinding method, String... keys) {
        paramPos = position;
        if (curMethodInvocation == null) return null;

        String methodName = curMethodInvocation.getName().getIdentifier();

        boolean isStaticExpr = false;
        String expressionTypeKey = null;
        ITypeBinding classBinding;

        List<ArgumentModel> preArgs = position >= 0 && method == null
                ? curMethodInvocation.argumentTypes().subList(0, position) : curMethodInvocation.argumentTypes();

        List<IMethodBinding> listMember = new ArrayList<>();

        if (keys.length == 2) {
            MethodCallTypeArgument methodCallTypeArgument = methodCallArgumentMap.get(keys[0], keys[1]);
            expressionTypeKey = methodCallTypeArgument.getExpressionType().getKey();
            listMember.add(methodCallTypeArgument.getMethodBinding());
        } else {
            expressionTypeKey = curMethodInvocation.getExpressionType() == null ? null : curMethodInvocation.getExpressionType().getKey();

            if (method != null) {
                listMember.add(method);
            } else if (!Config.FEATURE_USER_CHOOSE_METHOD) {
                if (curMethodInvocation.getExpression() == null) {
                    classBinding = curClass;
                    isStaticExpr = isStaticScope(curMethodInvocation.getOrgASTNode());
                } else {
                    classBinding = curMethodInvocation.getExpressionType();
                    isStaticExpr = curMethodInvocation.isStaticExpression();
                }
                ClassParser classParser = new ClassParser(classBinding);

                List<IMethodBinding> methodBindings = classParser.getMethodsFrom(curClass, isStaticExpr);

                for (IMethodBinding methodBinding : methodBindings) {
                    if (methodName.equals(methodBinding.getName())) {
                        //Add filter for parent expression
                        if (parentValue(curMethodInvocation.getOrgASTNode()) == null
                                || compareWithMultiType(methodBinding.getReturnType(), parentValue(curMethodInvocation.getOrgASTNode()))) {
                            if (checkInvoMember(preArgs, methodBinding)) {
                                listMember.add(methodBinding);
                            }
                        }
                    }
                }
            } else {
                IMethodBinding methodBinding = curMethodInvocation.resolveMethodBinding();
                if (methodBinding != null) listMember.add(methodBinding);
            }
        }

        MultiMap nextVariableMap = new MultiMap();

        int methodArgLength = preArgs.size();
        if (method == null) {
            methodArgLength = methodArgLength > 0 && preArgs.get(methodArgLength - 1).toString().equals("$missing$")
                    ? methodArgLength - 1 : methodArgLength;
        } else {
            methodArgLength = position;
        }

        int finalMethodArgLength = methodArgLength;

        String finalExpressionTypeKey = expressionTypeKey;
        listMember.forEach(methodBinding ->
        {
            ITypeBinding[] params = methodBinding.getParameterTypes();
            if (finalMethodArgLength == params.length && !methodBinding.isVarargs()) {
                nextVariableMap.put("CLOSE_PART", ")");
            } else {
                if (finalMethodArgLength >= params.length - 1 && methodBinding.isVarargs()) {
                    nextVariableMap.put("CLOSE_PART", ")");
                }
                ITypeBinding typeNeedCheck = null;
                if (methodBinding.isVarargs() && finalMethodArgLength >= methodBinding.getParameterTypes().length - 1) {
                    typeNeedCheck = methodBinding.getParameterTypes()[methodBinding.getParameterTypes().length - 1].getElementType();
                } else if (finalMethodArgLength <= methodBinding.getParameterTypes().length - 1) {
                    typeNeedCheck = methodBinding.getParameterTypes()[finalMethodArgLength];
                }

                if (curMethodInvocation.getName().toString().equals("equals")
                        && curMethodInvocation.arguments().size() == 1) {
                    typeNeedCheck = curMethodInvocation.getExpressionType() == null ?
                            curClassParser.getOrgType() : curMethodInvocation.getExpressionType();
                } else {
                    ITypeBinding paramSpecialType =
                            TypeConstraintKey.getSpecialParam(curMethodInvocation, position);
                    if (paramSpecialType != null) {
                        typeNeedCheck = paramSpecialType;
                    }
                }

                if (typeNeedCheck != null) {
                    if (Config.FEATURE_STATIC_CONSTANT) {
                        ITypeBinding finalTypeNeedCheckForConstant = typeNeedCheck;
                        projectParser.getPublicStaticFieldList().forEach(fieldConstant -> {
                            if (TypeConstraintKey.assignWith(fieldConstant.key, finalTypeNeedCheckForConstant.getKey())) {
                                nextVariableMap.put(fieldConstant.excode, fieldConstant.lexical);
                            }
                        });
                        projectParser.getPublicStaticMethodList().forEach(methodConstant -> {
                            if (TypeConstraintKey.assignWith(methodConstant.key, finalTypeNeedCheckForConstant.getKey())) {
                                nextVariableMap.put(methodConstant.excode, methodConstant.lexical);
                            }
                        });
                    } else {
                        nextVariableMap.setParamTypeKey(typeNeedCheck.getKey());
                    }
                    if (TypeConstraintKey.NUM_TYPES.contains(typeNeedCheck.getKey())) {
                        nextVariableMap.put("LIT(num)", "0");

                        visibleVariables.forEach(variable -> {
                            if (variable.getTypeBinding().getDimensions() > 0) {
                                nextVariableMap.put("F_ACCESS(" + variable.getName() + ".length", variable.getName() + ".length");
                            }
                        });
                    }

                    if (TypeConstraintKey.STRING_TYPE.equals(typeNeedCheck.getKey())
                            || TypeConstraintKey.CHAR_SEQUE_TYPE.equals(typeNeedCheck.getKey())
                            || (methodBinding.getName().equals("equals")
                            && finalExpressionTypeKey != null && finalExpressionTypeKey.equals(TypeConstraintKey.STRING_TYPE))) {
                        nextVariableMap.put("LIT(String)", "\"\"");
                    }

                    if (TypeConstraintKey.OBJECT_TYPE.equals(typeNeedCheck.getKey())) {
                        nextVariableMap.put("LIT(num)", "0");
                        nextVariableMap.put("LIT(String)", "\"\"");
                    }

                    if (TypeConstraintKey.BOOL_TYPES.contains(typeNeedCheck.getKey())) {
                        nextVariableMap.put("LIT(boolean)", "true");
                        nextVariableMap.put("LIT(boolean)", "false");
                    }

                    //feature 13
                    if (Config.FEATURE_PARAM_TYPE_OBJ_CREATION
                            && !typeNeedCheck.isArray() && !typeNeedCheck.isPrimitive()
                            && !TypeConstraintKey.NUM_WRAP_TYPES.contains(typeNeedCheck.getKey())
                            && !TypeConstraintKey.BOOL_TYPES.contains(typeNeedCheck.getKey())
                            && !TypeConstraintKey.STRING_TYPE.equals(typeNeedCheck.getKey())
                            && !TypeConstraintKey.WRAP_TYPES.contains(typeNeedCheck.getKey())
                    ) {
                        String lex = "new " + typeNeedCheck.getName().replace("? extends ", "") + "(";
                        String excode = "C_CALL(" + typeNeedCheck.getName() + "," + typeNeedCheck.getName() + ") "
                                + "OPEN_PART";
                        nextVariableMap.put(excode, lex);
                    }

                    //feature 14
                    if (Config.FEATURE_PARAM_TYPE_ARR_CREATION
                            && typeNeedCheck.isArray() && typeNeedCheck.getDimensions() == 1) {
                        String lex = "new " + typeNeedCheck.getElementType().getName() + "[0]";
                        String excode = "C_CALL(Array_" + typeNeedCheck.getElementType().getName() + "," + typeNeedCheck.getElementType().getName() + ") "
                                + "OPEN_PART LIT(num) CLOSE_PART";
                        nextVariableMap.put(excode, lex);
                    }

                    //feature 10
                    if (Config.FEATURE_PARAM_TYPE_TYPE_LIT
                            && (typeNeedCheck.getKey().replaceAll("\\<.*\\>", "<>").equals(TypeConstraintKey.CLASS_TYPE)
                            || typeNeedCheck.getKey().equals(TypeConstraintKey.OBJECT_TYPE))) {
                        nextVariableMap.put("LIT(Class)", ".class");
                    }

                    //feature 11
                    if (Config.FEATURE_PARAM_TYPE_NULL_LIT && !typeNeedCheck.isPrimitive()) {
                        nextVariableMap.put("LIT(null)", "null");
                    }
                }

                if (Config.FEATURE_PARAM_TYPE_METHOD_INVOC) {
                    for (IMethodBinding innerAndOuterMethod : ParserUtils.withOuterClassParserMethods(curClassParser)) {
                        if (!isStatic || Modifier.isStatic(innerAndOuterMethod.getModifiers())) {
                            ITypeBinding varMethodReturnType = innerAndOuterMethod.getReturnType();
                            ParserCompareValue compareFieldValue = compareParam(varMethodReturnType, methodBinding, finalMethodArgLength);
                            if (ParserCompare.isTrue(compareFieldValue)) {
                                String nextLex = innerAndOuterMethod.getName() + "(";
                                String exCode = "M_ACCESS(" + curClass.getName() + "," + innerAndOuterMethod.getName() + "," + innerAndOuterMethod.getParameterTypes().length + ") "
                                        + "OPEN_PART";
                                nextVariableMap.put(exCode, nextLex);
                                methodCallArgumentMap.put(exCode, nextLex, new MethodCallTypeArgument(curClass, innerAndOuterMethod));
                            }
                        }
                    }
                }

                //static class feature
                if (Config.FEATURE_PARAM_STATIC_FIELD_ACCESS_FROM_CLASS) {
                    cu.imports().forEach(importItem -> {
                        if (importItem instanceof ImportDeclaration) {
                            ImportDeclaration importDeclaration = (ImportDeclaration) importItem;
                            IBinding importBinding = importDeclaration.resolveBinding();
                            if (importBinding instanceof ITypeBinding) {
                                ITypeBinding iTypeBinding = (ITypeBinding) importBinding;
                                ClassParser classParser = new ClassParser(iTypeBinding);
                                classParser.getFieldsFrom(curClass, true).forEach(staticField -> {
                                    ITypeBinding staticMemberType = staticField.getType();
                                    ParserCompareValue compareFieldValue = compareParam(staticMemberType, methodBinding, finalMethodArgLength);
                                    if (ParserCompare.isTrue(compareFieldValue)) {
                                        String nextVar = importBinding.getName() + "." + staticField.getName();
                                        String exCode = "VAR(" + importBinding.getName() + ") "
                                                + "F_ACCESS(" + importBinding.getName() + "," + staticField.getName() + ")";
                                        nextVariableMap.put(exCode, nextVar);
                                    }
                                });
                            }
                        }
                    });
                }

                Stream<Variable> variables = Config.FEATURE_DFG_VARIABLE ?
                        visibleVariables.stream().filter(variable -> variable.isInitialized()) : visibleVariables.stream();

                ITypeBinding finalTypeNeedCheck = typeNeedCheck;
                variables.forEach(variable -> {
                    ParserCompareValue compareValue = compareParam(variable.getTypeBinding(), methodBinding, finalMethodArgLength);

                    //Just add cast and array access for variable
                    if (ParserCompare.isTrue(compareValue)) {
                        String exCode = "VAR(" + variable.getTypeBinding().getName() + ")";

                        if (!Config.FEATURE_LIMIT_CANDIDATES || ParserUtils.checkImportantVariable(variable.getName(), getParamName(position).orElse(null), getLocalVariableList())) {
                            nextVariableMap.put(exCode, variable.getName());
                        }
                    }
                    if (ParserCompare.canBeCast(compareValue) && finalTypeNeedCheck != null) {
                        compareValue.getCanBeCastType().forEach(type -> {
                            String exCode = "CAST(" + type.getName() + ") VAR(" + variable.getTypeBinding().getName() + ")";
                            nextVariableMap.put(exCode, "(" + type.getName() + ") " + variable.getName());
                        });
                    }
                    if (ParserCompare.isArrayType(compareValue)) {
                        String exCode = "VAR(" + variable.getTypeBinding().getName() + ") OPEN_PART CLOSE_PART";
                        nextVariableMap.put(exCode, variable.getName() + "[]");
                    }

                    ITypeBinding variableClass = variable.getTypeBinding();

                    if (variableClass != null) {
                        List<IVariableBinding> varFields = new ClassParser(variableClass).getFieldsFrom(curClass);

                        //gen candidate with field
                        for (IVariableBinding varField : varFields) {
                            ITypeBinding varMemberType = varField.getType();
                            ParserCompareValue compareFieldValue = compareParam(varMemberType, methodBinding, finalMethodArgLength);
                            if (ParserCompare.isTrue(compareFieldValue) && !Modifier.isStatic(varField.getModifiers())) {
                                String nextVar = variable.getName() + "." + varField.getName();
                                String exCode = "VAR(" + variableClass.getName() + ") "
                                        + "F_ACCESS(" + variableClass.getName() + "," + varField.getName() + ")";
                                nextVariableMap.put(exCode, nextVar);
                            }
                        }
                        //gen candidate with method
                        if (Config.FEATURE_PARAM_TYPE_METHOD_INVOC) {
                            List<IMethodBinding> varMethods = new ClassParser(variableClass).getMethodsFrom(curClass);
                            for (IMethodBinding varMethod : varMethods) {
                                ITypeBinding varMethodReturnType = varMethod.getReturnType();
                                ParserCompareValue compareFieldValue = compareParam(varMethodReturnType, methodBinding, finalMethodArgLength);
                                if (ParserCompare.isTrue(compareFieldValue)) {
                                    String nextLex = variable.getName() + "." + varMethod.getName() + "(";
                                    String exCode = "VAR(" + variableClass.getName() + ") "
                                            + "M_ACCESS(" + variableClass.getName() + "," + varMethod.getName() + "," + varMethod.getParameterTypes().length + ") "
                                            + "OPEN_PART";
                                    nextVariableMap.put(exCode, nextLex);
                                    methodCallArgumentMap.put(exCode, nextLex, new MethodCallTypeArgument(variableClass, varMethod));
                                }
                            }
                        }
                    }
                });
                if (Config.FEATURE_PARAM_TYPE_LAMBDA
//                        && nextVariableMap.getValue().isEmpty()
                        && typeNeedCheck.isInterface()) {
                    nextVariableMap.put("LAMBDA", "->{}");
                }
                if (Config.FEATURE_PARAM_TYPE_METHOD_REF && typeNeedCheck.isInterface()) {
                    nextVariableMap.put("M_REF(<unk>,<unk>)", "::");
                    ITypeBinding constructorRef = ParserUtils.checkConstructorReference(typeNeedCheck);
                    if (constructorRef != null) {
                        String constructorRefName = constructorRef.getName().replace("? extends ", "");
                        nextVariableMap.put("VAR(" + constructorRefName + ") M_REF(" + constructorRefName + ",new)"
                                , constructorRefName + "::" + "new");
                    }
                }
            }
        });

        return nextVariableMap;
    }

    public String getTargetPattern(int pos) {
        try {
            if (curMethodInvocation.arguments().size() <= pos) return null;
            Expression arg = (Expression) curMethodInvocation.arguments().get(pos);
            if (arg instanceof QualifiedName) {
                QualifiedName fieldAccess = (QualifiedName) arg;
                IBinding fieldBinding = fieldAccess.resolveBinding();
                if (fieldBinding != null && fieldBinding instanceof IVariableBinding) {
                    IVariableBinding fieldVariableBinding = (IVariableBinding) fieldBinding;
                    if (Modifier.isStatic(fieldVariableBinding.getModifiers())) {
                        return String.join(".",
                                fieldVariableBinding.getDeclaringClass().getName(), fieldVariableBinding.getName());
                    }
                }

            } else if (arg instanceof MethodInvocation) {
                MethodInvocation methodInvocation = (MethodInvocation) arg;
                if (Modifier.isStatic(methodInvocation.resolveMethodBinding().getModifiers())) {
                    return String.join(".",
                            methodInvocation.resolveMethodBinding().getDeclaringClass().getName(), methodInvocation.resolveMethodBinding().getName()) + "(";
                }
            } else if (arg instanceof ClassInstanceCreation) {
                ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation) arg;
                return "new " + classInstanceCreation.resolveTypeBinding().getName() + "(";
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public Optional<List<IMethodBinding>> genMethodCall() {
        if (curMethodInvocation == null) return Optional.empty();

        ITypeBinding classBinding;
        boolean isStaticExpr = false;
        List<IMethodBinding> listMember = new ArrayList<>();

        if (curMethodInvocation.getExpression() == null) {
            classBinding = curClass;
            isStaticExpr = isStaticScope(curMethodInvocation.getOrgASTNode());
        } else {
            classBinding = curMethodInvocation.getExpressionType();
            isStaticExpr = curMethodInvocation.isStaticExpression();
        }

        if (classBinding == null) return Optional.empty(); //can not be resolved

        ClassParser classParser = new ClassParser(classBinding);

        List<IMethodBinding> methodBindings = classParser.getMethodsFrom(curClass, isStaticExpr);


        if (Config.FEATURE_ONLY_VOID_FOR_STMT) {
            //filter for void
            if (curMethodInvocation.getOrgASTNode().getParent() instanceof ExpressionStatement
                    && curMethodInvocation.getOrgASTNode().getParent().getParent() instanceof Block) {
                ExpressionStatement expressionStatement = (ExpressionStatement) curMethodInvocation.getOrgASTNode().getParent();
                if (expressionStatement.getExpression() == curMethodInvocation.getOrgASTNode()) {
                    methodBindings = methodBindings.stream().filter(method -> {
                        return method.getReturnType().getKey().equals("V"); //void
                    }).collect(Collectors.toList());
                }
            }
        }

        if (Config.FEATURE_IGNORE_NATIVE_METHOD) {
            methodBindings = methodBindings.stream().filter(method -> {
                return !Modifier.isNative(method.getModifiers())
                        && !method.getDeclaringClass().getKey().equals(TypeConstraintKey.OBJECT_TYPE);
            }).collect(Collectors.toList());
        }

        for (IMethodBinding methodBinding : methodBindings) {
            //Add filter for parent expression
            if (!methodBinding.isConstructor() && (parentValue(curMethodInvocation.getOrgASTNode()) == null
                    || compareWithMultiType(methodBinding.getReturnType(), parentValue(curMethodInvocation.getOrgASTNode())))) {
                int lengthCheck = methodBinding.isVarargs() ? methodBinding.getParameterTypes().length - 1 : methodBinding.getParameterTypes().length;
                boolean availableCheck = true;
                for (int i = 0; i < lengthCheck; i++) {
                    MultiMap listResult = genNextParams(i, methodBinding);
                    if (listResult.getValue().isEmpty()) {
                        availableCheck = false;
                        break;
                    }
                }
                if (availableCheck)
                    listMember.add(methodBinding);
            }
        }
        return Optional.of(listMember);
    }

    private int paramPosition = -1;

    public int getParamPosition() {
        return paramPosition;
    }

    public void parseCurMethodInvocation() throws MethodInvocationNotFoundException {
        final ASTNode[] astNode = {null};

        if (Config.TARGET_PARAM_POSITION) {
            cu.accept(new ASTVisitor() {
                public void preVisit(ASTNode node) {
                    if ((node instanceof MethodInvocation || node instanceof SuperMethodInvocation)
                            && node.getStartPosition() <= curPosition
                            && curPosition <= (node.getStartPosition() + node.getLength())) {
                        int start = 0;
                        int stop = 0;
                        List arguments = null;
                        if (node instanceof MethodInvocation) {
                            MethodInvocation methodInvocation = (MethodInvocation) node;
                            start = methodInvocation.getName().getStartPosition() + methodInvocation.getName().getLength();
                            stop = methodInvocation.getStartPosition() + methodInvocation.getLength();
                            arguments = methodInvocation.arguments();
                        } else {
                            SuperMethodInvocation superMethodInvocation = (SuperMethodInvocation) node;
                            start = superMethodInvocation.getName().getStartPosition() + superMethodInvocation.getName().getLength();
                            stop = superMethodInvocation.getStartPosition() + superMethodInvocation.getLength();
                            arguments = superMethodInvocation.arguments();
                        }
                        if (curPosition > start + 1 && curPosition < stop + 1) {
                            astNode[0] = node;
                            int pos = 0;

                            paramPosition = 0;
                            for (Object arg : arguments) {
                                ASTNode astNodeArg = (ASTNode) arg;
                                if (astNodeArg.getStartPosition() <= curPosition
                                        && curPosition - 1 <= (astNodeArg.getStartPosition() + astNodeArg.getLength())) {
                                    paramPosition = pos;
                                }

                                if (astNodeArg.getStartPosition() + astNodeArg.getLength() < curPosition - 1
                                        && arguments.size() > pos + 1) {
                                    paramPosition = pos + 1;
                                }

                                pos++;
                            }
                        }
                    }
                }
            });
        } else {
            cu.accept(new ASTVisitor() {
                public void preVisit(ASTNode node) {
                    if ((node instanceof MethodInvocation || node instanceof SuperMethodInvocation)
                            && node.getStartPosition() <= curPosition
                            && curPosition <= (node.getStartPosition() + node.getLength())) {
                        astNode[0] = node;
                    }
                }
            });
        }
        if (astNode[0] == null) {
            curMethodInvocation = null;
            throw new MethodInvocationNotFoundException("Method invocation not found!");
        }

        if (astNode[0] instanceof MethodInvocation) {
            curMethodInvocation = new MethodInvocationModel(curClass, (MethodInvocation) astNode[0]);
        }

        if (astNode[0] instanceof SuperMethodInvocation) {
            curMethodInvocation = new MethodInvocationModel(curClass, (SuperMethodInvocation) astNode[0]);
        }
    }

    public static ParserCompareValue compareParam(ITypeBinding varType, IMethodBinding methodBinding, int position) {
        ParserCompareValue result = new ParserCompareValue();

        if (methodBinding.getParameterTypes().length > position) {
            if (varType.isAssignmentCompatible(methodBinding.getParameterTypes()[position]))
                result.addValue(ParserConstant.TRUE_VALUE);
            if (ParserUtils.compareSpecialCase(varType, methodBinding.getParameterTypes()[position], methodBinding)
                    && varType.getTypeArguments().length > 0 && methodBinding.getTypeArguments().length > 0)
                result.addValue(ParserConstant.TRUE_VALUE);
            if (Config.FEATURE_PARAM_TYPE_CAST && varType.isCastCompatible(methodBinding.getParameterTypes()[position])) {
                result.addValue(ParserConstant.CAN_BE_CAST_VALUE);
                result.addCastType(methodBinding.getParameterTypes()[position]);
            }
            if (Config.FEATURE_PARAM_TYPE_ARRAY_ACCESS && varType.isArray()
                    && varType.getComponentType().isAssignmentCompatible(methodBinding.getParameterTypes()[position]))
                result.addValue(ParserConstant.IS_ARRAY_VALUE);

        }

        if (methodBinding.isVarargs()
                && methodBinding.getParameterTypes().length - 1 <= position
                && methodBinding.getParameterTypes()[methodBinding.getParameterTypes().length - 1].isArray()) {
            if (varType.isAssignmentCompatible(methodBinding.getParameterTypes()[methodBinding.getParameterTypes().length - 1].getComponentType())) {
                result.addValue(ParserConstant.VARARGS_TRUE_VALUE);
            }
            if (Config.FEATURE_PARAM_TYPE_CAST && varType.isCastCompatible(methodBinding.getParameterTypes()[methodBinding.getParameterTypes().length - 1].getComponentType())) {
                result.addValue(ParserConstant.CAN_BE_CAST_VALUE);
                result.addCastType(methodBinding.getParameterTypes()[methodBinding.getParameterTypes().length - 1].getComponentType());
            }

        }

        result.addValue(compareWithGenericType(varType, methodBinding, position));
        return result;
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

    public ITypeBinding[] parentValue(ASTNode methodInvocation) {
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
        } else if (parentNode instanceof InfixExpression) {
            InfixExpression infixExpression = (InfixExpression) parentNode;
            String operator = infixExpression.getOperator().toString();
            if (ParserUtils.numberInfixOperation.contains(operator)) {
                return new ITypeBinding[]{new CommonNumType()};
            } else if (ParserUtils.boolInfixOperation.contains(operator)) {
                return new ITypeBinding[]{new BooleanPrimitiveType()};
            } else
                return null;
        } else if (parentNode instanceof IfStatement || parentNode instanceof DoStatement || parentNode instanceof WhileStatement) {
            return new ITypeBinding[]{new BooleanPrimitiveType()};
        } else if (parentNode instanceof MethodInvocation || parentNode instanceof SuperMethodInvocation) {
            MethodInvocationModel methodInvocationParentModel = null;
            if (parentNode instanceof MethodInvocation) {
                methodInvocationParentModel = new MethodInvocationModel(curClass, (MethodInvocation) parentNode);
            } else if (parentNode instanceof SuperMethodInvocation) {
                methodInvocationParentModel = new MethodInvocationModel(curClass, (SuperMethodInvocation) parentNode);
            }

            //if method call is a param of method call
            if (methodInvocationParentModel.arguments().contains(methodInvocation)) {
                ITypeBinding[] parentTypes = parentValue(methodInvocationParentModel.getOrgASTNode());
                List<ITypeBinding> typeResults = new ArrayList<>();

                ClassParser classParser;
                boolean isStaticExpr = false;
//                if (methodInvocationParent.getExpression() == null) {
//                    classParser = new ClassParser(curClass);
//                    isStaticExpr = isStaticScope(methodInvocation);
//                } else {
//                    Expression expr = methodInvocationParent.getExpression();
//                    classParser = new ClassParser(expr.resolveTypeBinding());
//                    isStaticExpr = expr.toString().equals(expr.resolveTypeBinding().getName());
//                }
                classParser = methodInvocationParentModel.getExpressionType() == null ? curClassParser
                        : new ClassParser(methodInvocationParentModel.getExpressionType());

                MethodInvocationModel finalMethodInvocationParentModel = methodInvocationParentModel;
                classParser.getMethodsFrom(curClass, isStaticExpr).forEach(methodMember -> {
                    if (methodMember.getName().equals(finalMethodInvocationParentModel.getName().getIdentifier())) {

                        if (parentTypes == null ||
                                compareWithMultiType(methodMember.getReturnType(), parentTypes)) {
                            int positionParam = -1;

                            for (int i = 0; i < finalMethodInvocationParentModel.arguments().size(); i++) {
                                if (methodInvocation == finalMethodInvocationParentModel.arguments().get(i)) {
                                    positionParam = i;
                                    break;
                                }
                            }

                            if (checkInvoMember(finalMethodInvocationParentModel.argumentTypes(), methodMember, positionParam)) {
                                if (methodMember.isVarargs() && positionParam >= methodMember.getParameterTypes().length - 1) {
                                    typeResults.add(methodMember.getParameterTypes()[methodMember.getParameterTypes().length - 1].getComponentType());
                                } else {
                                    typeResults.add(methodMember.getParameterTypes()[positionParam]);
                                }
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
                    || (iTypeBindings[i] instanceof GenericType && ((GenericType) iTypeBindings[i]).canBeAssignmentBy(iTypeBinding))) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkInvoMember(List<ArgumentModel> args, IMethodBinding iMethodBinding, int ignorPos) {
        if (!iMethodBinding.isVarargs() && args.size() > iMethodBinding.getParameterTypes().length) return false;
        int index = 0;
        for (ArgumentModel argument :
                args) {
            if (index == ignorPos) {
                index++;
                continue;
            }

            if (ParserCompare.isFalse(compareParam(argument.resolveType(), iMethodBinding, index++))) {
                return false;
            }
        }
        return true;
    }

    public static boolean checkInvoMember(List<ArgumentModel> args, IMethodBinding iMethodBinding) {
        if (!iMethodBinding.isVarargs() && args.size() > iMethodBinding.getParameterTypes().length) return false;
        int index = 0;
        for (ArgumentModel argument :
                args) {

            if (argument.toString().equals("$missing$")) {
                args.remove(argument);
                break;
            }
//                ITypeBinding[] params = iMethodBinding.getParameterTypes();
            if (argument.resolveType() == null
                    || ParserCompare.isFalse(compareParam(argument.resolveType(), iMethodBinding, index++))) {
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

    public ASTNode getCurMethodScope() {
        ASTNode scopeNode = getScope(curPosition);
        if (scopeNode == null) return null;
        return getMethodScope(scopeNode);
    }

    public Optional<String> getCurMethodScopeName() {
        Optional<String> methodName;
        ASTNode curMethodScope = getCurMethodScope();
        if (curMethodScope == null || !(curMethodScope instanceof MethodDeclaration)) {
            methodName = Optional.empty();
        } else {
            methodName = Optional.of(((MethodDeclaration) curMethodScope).getName().toString());
        }
        return methodName;
    }

    public ITypeBinding getCurClassScope() throws ClassScopeNotFoundException {
        return getClassScope(curPosition);
    }

    public Optional<String> getCurClassScopeName() {
        try {
            return Optional.of(getCurClassScope().getName());
        } catch (ClassScopeNotFoundException e) {
            return Optional.empty();
        }
    }

    public PackageDeclaration getCurPackage() {
        return cu.getPackage();
    }

    public Optional<String> getCurPackageName() {
        try {
            return Optional.of(cu.getPackage().getName().toString());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public int getScopeDistance(ASTNode variableNode) {
        if (variableNode == null) return -1;
        ASTNode parentBlock = getParentBlockDistanceNode(variableNode);
        ASTNode curBlockPointer = getParentBlockDistanceNode(curMethodInvocation.getOrgASTNode());
        if (parentBlock == curBlockPointer) return 0;
        int distance = 1;
        while (true) {
            curBlockPointer = getParentBlockDistanceNode(curBlockPointer);
            if (curBlockPointer == null || curBlockPointer instanceof CompilationUnit) break;
            if (parentBlock == curBlockPointer) {
                return distance;
            }
            distance++;
        }
        return -1;
    }

    private void getVariableScope(ASTNode astNode) {
        if (astNode == null) return;
        Block block = null;
        if (astNode instanceof Block) {
            block = (Block) astNode;
        } else if (astNode instanceof MethodDeclaration) {
            MethodDeclaration methodDeclaration = (MethodDeclaration) astNode;
//            isStatic = Modifier.isStatic(methodDeclaration.getModifiers());
//            block = methodDeclaration.getBody();

            List params = methodDeclaration.parameters();
            params.forEach(param -> {
                if (param instanceof SingleVariableDeclaration) {
                    SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) param;
                    int position = singleVariableDeclaration.getStartPosition();

                    IVariableBinding variableBinding = singleVariableDeclaration.resolveBinding();
                    Variable variable = addVariableToList(position, variableBinding, isStatic, true);
                    if (variable != null) {
                        variable.setLocalVariable(true);
                        variable.setLocalVariableLevel(4);
                        variable.setScopeDistance(getScopeDistance(singleVariableDeclaration) - 2);
                    }
                }
            });

            if (!isStatic) {
                Variable variable = new Variable(curClass, "this");
                variable.setStatic(false);
                variable.setInitialized(true);
                variable.setLocalVariableLevel(3);
                visibleVariables.add(variable);
            }

        } else if (astNode instanceof Initializer) {
            Initializer initializer = (Initializer) astNode;
//            block = initializer.getBody();
            isStatic = true;
        } else if (astNode instanceof TypeDeclaration) {
            TypeDeclaration typeDeclaration = (TypeDeclaration) astNode;
            FieldDeclaration[] fields = typeDeclaration.getFields();

            for (FieldDeclaration field : fields) {
                int position = field.getStartPosition();
                List fragments = field.fragments();

                fragments.forEach(fragment -> {
                    if (fragment instanceof VariableDeclarationFragment) {
                        VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) fragment;
                        IVariableBinding variableBinding = variableDeclarationFragment.resolveBinding();

                        boolean isStatic = Modifier.isStatic(variableBinding.getModifiers());
                        Variable variable = addVariableToList(position, variableBinding, isStatic, true);
                        if (variable != null) {
                            variable.setField(true);
                            variable.setLocalVariableLevel(3);
                            variable.setLocalVariable(true);
                            variable.setScopeDistance(getScopeDistance(variableDeclarationFragment) - 1);
                        }
                    }
                });
            }
            //super field as variable
            ParserUtils.getAllSuperFields(typeDeclaration.resolveBinding()).forEach(variable -> {
                boolean isStatic = Modifier.isStatic(variable.getModifiers());
                Variable variable1 = addVariableToList(-1, variable, isStatic, true);
                if (variable1 != null) {
                    variable1.setLocalVariableLevel(2);
                    variable1.setScopeDistance(getScopeDistance(getCurMethodScope()));
                }
            });
        } else if (astNode instanceof LambdaExpression) {
            LambdaExpression lambdaExpression = (LambdaExpression) astNode;
            List params = lambdaExpression.parameters();
            params.forEach(param -> {
                if (param instanceof VariableDeclarationFragment) {
                    VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) param;
                    int position = variableDeclarationFragment.getStartPosition();
                    IVariableBinding variableBinding = variableDeclarationFragment.resolveBinding();

                    Variable variable = addVariableToList(position, variableBinding, isStatic, true);
                    if (variable != null) {
                        variable.setScopeDistance(getScopeDistance(variableDeclarationFragment));
                        if (astNode == curBlockScope.getParent()) {
                            variable.setLocalVariable(true);
                            variable.setLocalVariableLevel(4);
                        }
                    }
                }
            });
        } else if (astNode instanceof CatchClause) {
            CatchClause catchClause = (CatchClause) astNode;
            IVariableBinding variableBinding = catchClause.getException().resolveBinding();
            Variable variable = addVariableToList(catchClause.getException().getStartPosition(), variableBinding, isStatic, true);
            if (variable != null) {
                variable.setScopeDistance(getScopeDistance(catchClause) - 1);
                if (astNode == curBlockScope.getParent()) {
                    variable.setLocalVariableLevel(4);
                    variable.setLocalVariable(true);
                }
            }
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

                            Variable variable = addVariableToList(position, variableBinding, isStatic, true);
                            if (variable != null) {
                                variable.setScopeDistance(getScopeDistance(variableDeclarationExpression) - 1);
                            }
                            if (astNode == curBlockScope.getParent()) {
                                variable.setLocalVariable(true);
                                variable.setLocalVariableLevel(4);
                            }
                        }
                    });
                }
            });
        } else if (astNode instanceof EnhancedForStatement) {
            EnhancedForStatement enhancedForStatement = (EnhancedForStatement) astNode;
            SingleVariableDeclaration singleVariableDeclaration = enhancedForStatement.getParameter();

            int position = singleVariableDeclaration.getStartPosition();

            IVariableBinding variableBinding = singleVariableDeclaration.resolveBinding();

            Variable variable = addVariableToList(position, variableBinding, isStatic, true);
            if (variable != null) {
                variable.setScopeDistance(getScopeDistance(enhancedForStatement) - 1);
                if (astNode == curBlockScope.getParent()) {
                    variable.setLocalVariable(true);
                    variable.setLocalVariableLevel(4);
                }
            }
        } else if (astNode instanceof SwitchStatement) {
            SwitchStatement switchStatement = (SwitchStatement) astNode;
            List listStatement = switchStatement.statements();
            for (Object stmt : Lists.reverse(listStatement)) {
                if (stmt instanceof ASTNode) {
                    ASTNode ASTNodeStatement = (ASTNode) stmt;
                    if (ASTNodeStatement.getStartPosition() > curPosition) continue;
                } else continue;

                //stop when top switch case
                //if (stmt instanceof SwitchCase) break;

                if (stmt instanceof VariableDeclarationStatement) {
                    VariableDeclarationStatement declareStmt = (VariableDeclarationStatement) stmt;
                    declareStmt.fragments().forEach(fragment -> {
                        if (fragment instanceof VariableDeclarationFragment) {
                            VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) fragment;
                            int position = variableDeclarationFragment.getStartPosition() + variableDeclarationFragment.getLength();
                            IVariableBinding variableBinding = variableDeclarationFragment.resolveBinding();
                            Variable variable = addVariableToList(position, variableBinding, isStatic,
                                    DFGParser.checkVariable(variableDeclarationFragment, getScope(curPosition), curPosition)
                            );
                            if (variable != null) {
                                variable.setScopeDistance(getScopeDistance(variableDeclarationFragment));
                                if (astNode == curBlockScope.getParent()) {
                                    variable.setLocalVariable(true);
                                    variable.setLocalVariableLevel(6);
                                }
                            }
                        }
                    });
                }
            }
        }

        if (block != null) {
            List listStatement = block.statements();
            Lists.reverse(listStatement).forEach(stmt -> {
                if (stmt instanceof VariableDeclarationStatement) {
                    VariableDeclarationStatement declareStmt = (VariableDeclarationStatement) stmt;
                    declareStmt.fragments().forEach(fragment -> {
                        if (fragment instanceof VariableDeclarationFragment) {
                            VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) fragment;
                            int position = variableDeclarationFragment.getStartPosition() + variableDeclarationFragment.getLength();
                            IVariableBinding variableBinding = variableDeclarationFragment.resolveBinding();
                            Variable variable = addVariableToList(position, variableBinding, isStatic,
                                    DFGParser.checkVariable(variableDeclarationFragment, getScope(curPosition), curPosition)
                                    //initVariables.get(variableBinding.getName()) != null
                                    //|| (variableDeclarationFragment.getInitializer() != null && (variableDeclarationFragment.getStartPosition() + variableDeclarationFragment.getLength()) < curPosition)
                            );
                            if (variable != null) {
                                variable.setScopeDistance(getScopeDistance(declareStmt));
                                if (astNode == curBlockScope) {
                                    variable.setLocalVariable(true);
                                    variable.setLocalVariableLevel(6);
                                }
                            }
                        }
                    });
                }
//                if (stmt instanceof ExpressionStatement) {
//                    if (((ExpressionStatement) stmt).getExpression() instanceof Assignment) {
//                        Assignment assignment = (Assignment) ((ExpressionStatement) stmt).getExpression();
//                        if (assignment.getLeftHandSide() instanceof SimpleName) {
//                            addInitVariable(assignment.getLeftHandSide().toString(), assignment.getStartPosition() + assignment.getLength());
//                        }
//                    }
//
//                }
            });
        }

        getVariableScope(getParentBlock(astNode));
    }

    public List<String> getLocalVariableList() {
        List<String> localVariableList = new ArrayList<>();
        localVariableList = visibleVariables.stream().filter(variable -> {
            return variable.isLocalVariable();
        }).map(variable -> {
            return variable.getName();
        }).collect(Collectors.toList());

        List<SimpleName> importantSimpleName = new ArrayList<>();
        if (curBlockScope.getParent() instanceof IfStatement) {
            importantSimpleName = ParserUtils.parseSimpleName(((IfStatement) curBlockScope.getParent()).getExpression());
        } else if (curBlockScope.getParent() instanceof WhileStatement) {
            importantSimpleName = ParserUtils.parseSimpleName(((WhileStatement) curBlockScope.getParent()).getExpression());
        } else if (curBlockScope.getParent() instanceof SwitchCase) {
            importantSimpleName = ParserUtils.parseSimpleName(((SwitchCase) curBlockScope.getParent()).getExpression());
        }

        localVariableList.addAll(importantSimpleName.stream().map(simpleName -> {
                    return simpleName.toString();
                }
        ).collect(Collectors.toList()));

        return localVariableList;
    }

    public Optional<String> getParamName(int pos) {
        Optional<String> result = Optional.empty();
        if (!Config.TEST_LEX_SIM) return result;
        MethodDeclaration methodDeclaration = ParserUtils.findMethodDeclaration(getCurMethodInvocation().resolveMethodBinding(), cu, projectParser);
        Object param = null;

        if (methodDeclaration == null) {
            return result;
        }

        if (methodDeclaration.isVarargs() && pos > methodDeclaration.parameters().size()) {
            param = methodDeclaration.parameters().get(methodDeclaration.parameters().size() - 1);
        } else {
            param = methodDeclaration.parameters().get(pos);
        }

        if (param instanceof SingleVariableDeclaration) {
            SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) param;
            result = Optional.of(singleVariableDeclaration.getName().toString());
        }
        return result;
    }

    private void addInitVariable(String variableName, int endPosition) {
        if (endPosition < curPosition) {
            initVariables.put(variableName, endPosition);
        }
    }

    private Variable addVariableToList(int startPosition, IVariableBinding variableBinding, boolean isStatic, boolean isInitialized) {
        ITypeBinding typeBinding = variableBinding.getType();
        String varName = variableBinding.getName();

        if (this.isStatic == true && isStatic == false) return null;

        if (!checkVariableInList(varName, visibleVariables) && startPosition <= curPosition) {
            Variable variable = new Variable(typeBinding, varName);
            variable.setStatic(isStatic);
            variable.setInitialized(isInitialized);
            visibleVariables.add(variable);
            return variable;
        }
        return null;
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
     * TypeDeclaration, Block, LambdaExpression, ForStatement, EnhancedForStatement, SwitchStatement, CatchClause
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
        } else if (parentNode instanceof CatchClause) {
            return parentNode;
        } else if (parentNode instanceof ForStatement || parentNode instanceof EnhancedForStatement) {
            return parentNode;
        } else if (parentNode instanceof SwitchStatement) {
            return parentNode;
        } else return getParentBlock(parentNode);
    }

    public static ASTNode getParentBlockDistanceNode(ASTNode astNode) {
        if (astNode == null) return null;
        ASTNode parentNode = astNode.getParent();
        if (parentNode instanceof Block) {
//            if (parentNode.getParent() instanceof MethodDeclaration) return parentNode.getParent();
            return parentNode; //block object
        } else if (parentNode instanceof MethodDeclaration || parentNode instanceof Initializer) {
            return parentNode;
        } else if (parentNode instanceof TypeDeclaration) {
            return parentNode;
        } else return getParentBlockDistanceNode(parentNode);
    }

    /**
     * @param position
     * @return Parent class nearest of position.
     * @throws NullPointerException
     */
    public ITypeBinding getClassScope(int position) throws ClassScopeNotFoundException {
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

        if (result[0] == null
                || (Config.IGNORE_JAVADOC && (getScope(position) instanceof Javadoc)))
            throw new ClassScopeNotFoundException();
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
        if (Config.IGNORE_JAVADOC && (astNode[0] instanceof Javadoc))
            return null;

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

    public boolean checkInsideMethod(MethodDeclaration methodDeclaration) {
        if (methodDeclaration.getBody() == null) return false;

        int startPos = methodDeclaration.getBody().getStartPosition();
        int endPos = methodDeclaration.getBody().getStartPosition() + methodDeclaration.getBody().getLength();
        if (curPosition > startPos && curPosition < endPos) return true;
        return false;
    }

    public boolean checkInsideMethod() {
        if (this.getCurMethodScope() != null && this.getCurMethodScope() instanceof MethodDeclaration) {
            return this.checkInsideMethod((MethodDeclaration) this.getCurMethodScope());
        } else {
            return false;
        }
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

    public HashMap<String, Variable> getVisibleVariablesHM() {
        HashMap<String, Variable> result = new HashMap<>();
        visibleVariables.forEach(variable -> {
            result.put(variable.getName(), variable);
        });
        return result;
    }

    public HashMap<String, Variable> getVisibleLocalVariables() {
        HashMap<String, Variable> result = new HashMap<>();
        visibleVariables.forEach(variable -> {
            if (variable.isLocalVariable()) {
                result.put(variable.getName(), variable);
            }
        });
        return result;
    }

    public List<String> getParentType() {
        ITypeBinding classPointer = null;
        List<String> result = new ArrayList<>();
        try {
            classPointer = getCurClassScope();
            while (true) {
                classPointer = classPointer.getSuperclass();
                if (classPointer == null) break;
                result.add(classPointer.getQualifiedName());
            }
        } catch (ClassScopeNotFoundException e) {
        }
        return result;
    }

    public HashMap<String, ClassModel> getVisibleClass() {
        return visibleClass;
    }
}
