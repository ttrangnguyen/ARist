package flute.jdtparser;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.*;

import flute.data.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FileParser {
    private ProjectParser projectParser;
    private File curFile;
    private int curPosition;

    private ITypeBinding curClass;

    private CompilationUnit cu;

    private boolean isStatic = false;

    public List<Variable> visibleVariable = new ArrayList<>();
    public HashMap<String, ClassModel> visibleClass;

    public FileParser(ProjectParser projectParser, File curFile, int curPosition) {
        this.projectParser = projectParser;
        this.curFile = curFile;
        this.curPosition = curPosition;
        cu = projectParser.createCU(curFile);
    }

    public FileParser(ProjectParser projectParser, File curFile, int curLine, int curPosition) {
        this.projectParser = projectParser;
        this.curFile = curFile;
        cu = projectParser.createCU(curFile);
        this.curPosition = this.getPosition(curLine, curPosition);
    }

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

    public void setPosition(int position) throws Exception {
        this.curPosition = position;
        parse();
    }

    public void parse() throws Exception {
        try {
            ITypeBinding clazz = getClassScope(curPosition);
            if (clazz != curClass) {
                curClass = clazz;
                visibleClass = projectParser.getListAccess(clazz);
            }
        } catch (NullPointerException err) {
            visibleClass.clear();
            throw new Exception("Can not get class");
        }

        ASTNode scope = getScope(curPosition);

        if (scope != null) {
            getVariableScope(scope);
            getNextParams();
        } else {
            visibleVariable.clear();
        }
    }

//    public List<Variable> getVariableScope(ASTNode astNode) {
//        List<Variable> listVariable = new ArrayList<>();
//        getVariableScope(astNode, listVariable);
//        return listVariable;
//    }

    public MultiMap getNextParams() {
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

        ClassModel classModel;
        if (methodInvocation.getExpression() == null) {
            classModel = visibleClass.get(curClass.getKey());
        } else {
            classModel = visibleClass.get(methodInvocation.getExpression().resolveTypeBinding().getKey());
        }

        List<Member> listMember = new ArrayList<>();

        classModel.getMembers().forEach(member -> {
            if (member instanceof MethodMember && methodName.equals(member.getMember().getName())) {
                //Add filter for parent expression
                if (parentValue(methodInvocation) == null
                        || compareWithArrayType(((MethodMember) member).getMember().getReturnType(), parentValue(methodInvocation))) {
                    if (checkInvoMember(methodInvocation.arguments(), (MethodMember) member)) {
                        listMember.add(member);
                    }
                }
            }
        });

        List<String> nextVariable = new ArrayList<>();

        MultiMap nextVariableMap = new MultiMap();

        listMember.forEach(member ->
        {
            ITypeBinding[] params = ((IMethodBinding) member.getMember()).getParameterTypes();
            if (methodInvocation.arguments().size() == params.length) {
                nextVariable.add(")");
                nextVariableMap.put("CLOSE_PART", ")");

            } else {

                visibleVariable.forEach(variable -> {
                    if (!nextVariable.contains(variable.getName()) && variable.getTypeBinding().isAssignmentCompatible(params[methodInvocation.arguments().size()])) {
                        nextVariable.add(variable.getName());
                        String exCode = "VAR(" + variable.getTypeBinding().getName() + "," + variable.getName() + ")";
                        nextVariableMap.put(exCode, variable.getName());
                    }

                    ClassModel variableClass = visibleClass.get(variable.getTypeBinding().getKey());

                    if (variableClass != null)
                        variableClass.getMembers().forEach(varMember -> {
                            if (varMember instanceof FieldMember) {
                                FieldMember varFieldMember = (FieldMember) varMember;
                                ITypeBinding varMemberType = ((IVariableBinding) varFieldMember.getMember()).getType();
                                if (varMemberType.isAssignmentCompatible(params[methodInvocation.arguments().size()])) {
                                    String nextVar = variable.getName() + "." + varFieldMember.getMember().getName();
                                    if (!nextVariable.contains(nextVar)) {
                                        String exCode = "VAR(" + variable.getTypeBinding().getName() + "," + variable.getName() + ")\n"
                                                + "F_ACCESS(" + varMemberType.getName() + "," + varFieldMember.getMember().getName() + ")";
                                        nextVariable.add(nextVar);
                                        nextVariableMap.put(exCode, nextVar);
                                    }
                                }
                            }
                        });
                });
            }
        });

        return nextVariableMap;
    }

    public int getPosition(int line, int column) {
        return cu.getPosition(line, column);
    }


    public ITypeBinding[] parentValue(MethodInvocation methodInvocation) {
        ASTNode astNode = methodInvocation.getParent();
        if (astNode instanceof Assignment) {
            Assignment assignment = (Assignment) astNode;
            return new ITypeBinding[]{assignment.getLeftHandSide().resolveTypeBinding()};
        } else if (astNode instanceof VariableDeclarationFragment) {
            VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) astNode;
            return new ITypeBinding[]{variableDeclarationFragment.resolveBinding().getType()};
        } else if (astNode instanceof ReturnStatement) {
            ASTNode methodNode = getMethodScope(astNode);
            if (methodNode instanceof MethodDeclaration) {
                ITypeBinding returnType = ((MethodDeclaration) methodNode).getReturnType2().resolveBinding();
                return new ITypeBinding[]{returnType};
            } else {
                return null;
            }
        } else if (astNode instanceof MethodInvocation) {
            MethodInvocation methodInvocationParent = (MethodInvocation) astNode;
            ITypeBinding[] parentTypes = parentValue(methodInvocationParent);
            List<ITypeBinding> typeResults = new ArrayList<>();

            for (ITypeBinding parentType : parentTypes) {
                ClassModel classModel;
                if (methodInvocation.getExpression() == null) {
                    classModel = visibleClass.get(methodInvocation.getExpression().resolveTypeBinding().getKey());
                } else {
                    classModel = visibleClass.get(curClass.getKey());
                }
                classModel.getMembers().forEach(member -> {
                    if (member instanceof MethodMember
                            && ((MethodMember) member).getMember().getName().equals(methodInvocationParent.getName().getIdentifier())) {
                        MethodMember methodMember = (MethodMember) member;
                        if (methodMember.getMember().getReturnType().isAssignmentCompatible(parentType)) {
                            int positionParam = -1;

                            for (int i = 0; i < methodInvocationParent.arguments().size(); i++) {
                                if (methodInvocation == methodInvocationParent.arguments().get(i)) {
                                    positionParam = i;
                                    break;
                                }
                            }

                            if (checkInvoMember(methodInvocationParent.arguments(), methodMember, positionParam)) {
                                typeResults.add(methodMember.getMember().getParameterTypes()[positionParam]);
                            }
                        }
                    }
                });
            }
            return typeResults.toArray(new ITypeBinding[0]);
        }
        return null;
    }

    private boolean compareWithArrayType(ITypeBinding iTypeBinding, ITypeBinding[] iTypeBindings) {
        for (int i = 0; i < iTypeBindings.length; i++) {
            if (iTypeBinding.isAssignmentCompatible(iTypeBindings[i])) return true;
        }
        return false;
    }

    public static boolean checkInvoMember(List args, MethodMember member, int ignorPos) {
        int index = 0;
        for (Object argument :
                args) {
            if (index == ignorPos) {
                index++;
                continue;
            }
            if (argument instanceof Expression) {
                Expression argExpr = (Expression) argument;
                ITypeBinding[] params = ((IMethodBinding) member.getMember()).getParameterTypes();
                if (!argExpr.resolveTypeBinding().isAssignmentCompatible(params[index++])) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    public static boolean checkInvoMember(List args, MethodMember member) {
        int index = 0;
        for (Object argument :
                args) {
            if (argument instanceof Expression) {
                Expression argExpr = (Expression) argument;
                if (argument.toString().equals("$missing$")) {
                    args.remove(argument);
                    break;
                }
                ITypeBinding[] params = ((IMethodBinding) member.getMember()).getParameterTypes();
                if (!argExpr.resolveTypeBinding().isAssignmentCompatible(params[index++])) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    public static ASTNode getMethodScope(ASTNode astNode) {
        if (astNode instanceof MethodDeclaration || astNode instanceof Initializer) {
            return astNode;
        } else if (astNode.getParent() != null) {
            return getMethodScope(astNode);
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
                    addVariableToList(position, variableBinding, true);
                }
            });
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
                        addVariableToList(position, variableBinding, isStatic);
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

                    addVariableToList(position, variableBinding, false);
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

                            addVariableToList(position, variableBinding, false);
                        }
                    });
                }
            });
        } else if (astNode instanceof EnhancedForStatement) {
            EnhancedForStatement enhancedForStatement = (EnhancedForStatement) astNode;
            SingleVariableDeclaration singleVariableDeclaration = enhancedForStatement.getParameter();

            int position = singleVariableDeclaration.getStartPosition();

            IVariableBinding variableBinding = singleVariableDeclaration.resolveBinding();

            addVariableToList(position, variableBinding, false);
        }

        if (block != null) {
            List listStatement = block.statements();
            listStatement.forEach(stmt -> {
                if (stmt instanceof VariableDeclarationStatement) {
                    VariableDeclarationStatement declareStmt = (VariableDeclarationStatement) stmt;
                    int position = declareStmt.getStartPosition();
                    declareStmt.fragments().forEach(fragment -> {
                        if (fragment instanceof VariableDeclarationFragment) {
                            IVariableBinding variableBinding = ((VariableDeclarationFragment) fragment).resolveBinding();
                            addVariableToList(position, variableBinding, false);
                        }
                    });
                }
            });
        }

        getVariableScope(getParentBlock(astNode));
    }

    private void addVariableToList(int startPosition, IVariableBinding variableBinding, boolean isStatic) {
        ITypeBinding typeBinding = variableBinding.getType();
        String varName = variableBinding.getName();

        if (this.isStatic == true && isStatic == false) return;

        if (!checkVariableInList(varName, visibleVariable) && startPosition <= curPosition) {
            Variable variable = new Variable(typeBinding, varName);
            variable.setStatic(isStatic);
            visibleVariable.add(variable);
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

    public static ASTNode getParentBlock(ASTNode astNode) {
        if (astNode == null) return null;
        ASTNode parentNode = astNode.getParent();
        if (parentNode instanceof Block) {
//            if (parentNode.getParent() instanceof MethodDeclaration) return parentNode.getParent();
            return (Block) parentNode; //block object
        } else if (parentNode instanceof MethodDeclaration || parentNode instanceof Initializer) {
            return parentNode;
        } else if (parentNode instanceof TypeDeclaration) {
            return parentNode;
        } else if (parentNode instanceof LambdaExpression) {
            return parentNode;
        } else if (parentNode instanceof ForStatement || parentNode instanceof EnhancedForStatement) {
            return parentNode;
        } else return getParentBlock(parentNode);
    }

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

    public List<Variable> getVisibleVariable() {
        return visibleVariable;
    }

    public HashMap<String, ClassModel> getVisibleClass() {
        return visibleClass;
    }
}
