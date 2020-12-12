package flute.tokenizing.exe;

import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import flute.config.Config;
import flute.data.MultiMap;
import flute.jdtparser.FileParser;
import flute.jdtparser.ProjectParser;
import flute.tokenizing.excode_data.*;
import flute.utils.StringUtils;
import flute.utils.file_processing.DirProcessor;
import flute.utils.file_processing.JavaTokenizer;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ArgRecTestGenerator {
    private JavaExcodeTokenizer tokenizer;
    private ProjectParser projectParser;
    private int lengthLimit = -1;

    public ArgRecTestGenerator(String projectPath, ProjectParser projectParser) {
        tokenizer = new JavaExcodeTokenizer(projectPath);
        this.projectParser = projectParser;
    }

    public void setLengthLimit(int lengthLimit) {
        this.lengthLimit = lengthLimit;
    }

    private <T> List<T> truncateList(List<T> list, boolean fromBegin) {
        if (lengthLimit < 0 || list.size() <= lengthLimit) return list;
        if (fromBegin) {
            return list.subList(list.size() - lengthLimit, list.size());
        } else {
            return list.subList(0, lengthLimit);
        }
    }

    private <T> List<T> truncateList(List<T> list) {
        return truncateList(list, true);
    }

    public static boolean isClean(List<NodeSequenceInfo> nodeSequenceList) {
        for (NodeSequenceInfo excode: nodeSequenceList) {
            // TODO: Ignore null literal for now
            if (NodeSequenceInfo.isLiteral(excode, "null") && !Config.FEATURE_PARAM_TYPE_NULL_LIT) return false;
            if (NodeSequenceInfo.isMethodAccess(excode) && !Config.FEATURE_PARAM_TYPE_METHOD_INVOC) return false;
            if (NodeSequenceInfo.isOpenBrak(excode) && !Config.FEATURE_PARAM_TYPE_ARRAY_ACCESS) return false;
            if (NodeSequenceInfo.isCast(excode)) {
                if (!Config.FEATURE_PARAM_TYPE_CAST) return false;
                // Only accept (Class) object
                if (!(nodeSequenceList.size() == 2 && NodeSequenceInfo.isVar(nodeSequenceList.get(1)))) return false;
            }
            if (NodeSequenceInfo.isObjectCreation(excode)) {
                if (!Config.FEATURE_PARAM_TYPE_OBJ_CREATION) return false;
                // Not accept Primitive wrapper classes
                List primitiveWrapperClasses = Arrays.asList("Byte", "Short", "Integer", "Long", "Float", "Double", "Character", "Boolean");
                if (primitiveWrapperClasses.contains(excode.getAttachedAccess())) return false;
            };
            if (NodeSequenceInfo.isArrayCreation(excode)) {
                if (!Config.FEATURE_PARAM_TYPE_ARR_CREATION) return false;
            }
            if (!Config.FEATURE_PARAM_TYPE_COMPOUND) {
                if (NodeSequenceInfo.isAssign(excode)) return false;
                if (NodeSequenceInfo.isOperator(excode)) return false;
                if (NodeSequenceInfo.isUnaryOperator(excode)) return false;
                if (NodeSequenceInfo.isConditionalExpr(excode)) return false;

                // For EnclosedExpr
                if (excode == nodeSequenceList.get(0) && NodeSequenceInfo.isOpenPart(excode)) return false;
            }
            if (NodeSequenceInfo.isClassExpr(excode) && !Config.FEATURE_PARAM_TYPE_TYPE_LIT) return false;

            // For static field access
            if (NodeSequenceInfo.isFieldAccess(excode) && !Config.FEATURE_PARAM_STATIC_FIELD_ACCESS_FROM_CLASS) {
                FieldAccessExpr fieldAccess = (FieldAccessExpr) excode.oriNode;
                boolean isScopeAClass = false;
                if (fieldAccess.getScope() instanceof NameExpr) {
                    try {
                        ((NameExpr) fieldAccess.getScope()).resolve();
                    }
                    // Field access from generic type?
                    catch (IllegalStateException ise) {
                        isScopeAClass = true;
                    }
                    // Field access from a class
                    catch (UnsolvedSymbolException use) {
                        isScopeAClass = true;
                    }
                    // ???
                    catch (UnsupportedOperationException uoe) {
                        isScopeAClass = true;
                    }
                    // ???
                    catch (RuntimeException re) {
                        isScopeAClass = true;
                    }
                } else if (fieldAccess.getScope() instanceof FieldAccessExpr) {
                    isScopeAClass = true;
                }
                if (isScopeAClass) {
                    String scope = fieldAccess.getScope().toString();
                    if (scope.indexOf('.') >= 0) {
                        scope = scope.substring(scope.lastIndexOf('.') + 1);
                    }
                    if (Character.isUpperCase(scope.charAt(0))) {
                        try {
                            ResolvedFieldDeclaration resolve = fieldAccess.resolve().asField();
                            if (resolve.isStatic() || resolve.declaringType().isInterface()) {
                                //System.out.println("Detected: " + excode.oriNode);
                                return false;
                            }
                        }
                        // Not an actual field
                        catch (IllegalStateException | UnsolvedSymbolException | UnsupportedOperationException e) {
                            if (fieldAccess.getNameAsString().matches("^[A-Z]+(?:_[A-Z]+)*$")) {
                                //System.out.println("Detected: " + excode.oriNode);
                                return false;
                            } else {
                                //System.out.println(fieldAccess);
                                //e.printStackTrace();
                            }
                        }
                    } else {
                        //use.printStackTrace();
                    }
                }
            }
        }
        return true;
    }

    public static void cleanTest(ArgRecTest test) {
        switch (test.getExpected_excode()) {
            case "LIT(wildcard)":
                test.setExpected_lex("?");
                break;
            case "LIT(null)":
                test.setExpected_lex("null");
                break;
            case "LIT(num)":
                test.setExpected_lex("0");
                break;
            case "LIT(String)":
                test.setExpected_lex("\"\"");
                break;
            case "VAR(Class)":
                test.setExpected_lex(".class");
        }
    }

    public List<ArgRecTest> generate(String javaFilePath) {
        System.out.println("File path: " + javaFilePath);
        List<ArgRecTest> tests = new ArrayList<>();
        List<NodeSequenceInfo> excodes = tokenizer.tokenize(javaFilePath);
        if (excodes.isEmpty()) return tests;
        List<Integer> stack = new ArrayList<>();
        MethodDeclaration methodDeclaration = null;
        FileParser fileParser = null;

        for (int i = 0; i < excodes.size(); ++i) {
            NodeSequenceInfo excode = excodes.get(i);

            if (excode.oriNode instanceof MethodDeclaration) {
                if (methodDeclaration == null) methodDeclaration = (MethodDeclaration) excode.oriNode;
                else if (excode.oriNode == methodDeclaration) {
                    methodDeclaration = null;
                }
            }
            if (methodDeclaration == null) continue;
            String methodDeclarationContent = methodDeclaration.toString();

            if (NodeSequenceInfo.isMethodAccess(excode)) stack.add(i);
            if (NodeSequenceInfo.isClosePart(excode)
                    && !stack.isEmpty() && excode.oriNode == excodes.get(stack.get(stack.size() - 1)).oriNode) {

                MethodCallExpr methodCall = (MethodCallExpr) excode.oriNode;
                String methodCallContent = methodCall.toString();

                //TODO: Handle multiple-line method invocation
                String contextMethodCall = methodDeclarationContent.substring(0, StringUtils.indexOf(methodDeclarationContent, StringUtils.getFirstLine(methodCallContent)));

                String methodName = methodCall.getNameAsString();
                String methodScope = "";
                if (methodCall.getScope().isPresent()) {
                    methodScope = methodCall.getScope().get() + ".";
                    methodName = methodScope + methodName;
                }
                contextMethodCall += methodName + '(';

                File javaFile = new File(javaFilePath);
                Node node = methodDeclaration;
                while (!(node instanceof CompilationUnit)) node = node.getParentNode().get();
                if (fileParser == null) {
                    fileParser = new FileParser(projectParser, javaFile.getName(), node.toString(),
                            methodCall.getBegin().get().line, methodCall.getBegin().get().column);
                } else {
                    try {
                        fileParser.setPosition(methodCall.getBegin().get().line, methodCall.getBegin().get().column);
                    } catch (Exception e) {
                        // TODO: Handle enums
                        e.printStackTrace();

                        stack.remove(stack.size() - 1);
                        continue;
                    }
                }
                int curPos = fileParser.getCurPosition();
                curPos += methodScope.length();
                try {
                    fileParser.setPosition(curPos);
                } catch (Exception e) {
                    // TODO: Handle enums
                    e.printStackTrace();

                    stack.remove(stack.size() - 1);
                    continue;
                }

                //System.out.println("Position: " + methodCall.getBegin().get());

                List<ArgRecTest> oneArgTests = new ArrayList<>();
                int methodCallIdx = stack.get(stack.size() - 1);
                int k = methodCallIdx + 1;
                int contextIdx = methodCallIdx + 1;
                for (int j = 0; j < methodCall.getArguments().size(); ++j) {
                    Expression arg = methodCall.getArgument(j);
                    while (k <= i) {
                        if (NodeSequenceInfo.isSEPA(excodes.get(k), ',') && excodes.get(k).oriNode == arg) {
                            MultiMap params = null;
                            try {
                                params = fileParser.genParamsAt(j);
                            } catch (ArrayIndexOutOfBoundsException e) {
                                System.out.println(methodCall);
                                System.out.println(methodCall.getBegin().get());
                                e.printStackTrace();
                            } catch (IndexOutOfBoundsException e) {
                                System.out.println(methodCall);
                                System.out.println(methodCall.getBegin().get());
                                e.printStackTrace();
                            } catch (NullPointerException e) {
                                System.out.println(methodCall);
                                System.out.println(methodCall.getBegin().get());
                                e.printStackTrace();
                            }

                            if (params != null && !params.getValue().keySet().isEmpty()) {
                                List<String> nextExcodeList = new ArrayList<>(params.getValue().keySet());
                                List<List<String>> nextLexList = new ArrayList<>();
                                for (String nextExcode : nextExcodeList) {
                                    nextLexList.add(params.getValue().get(nextExcode));
                                }
                                ContextInfo context = new ContextInfo(excodes, contextIdx);

                                List<NodeSequenceInfo> argExcodes = new ArrayList<>();
                                for (int t = contextIdx + 1; t < k; ++t) argExcodes.add(excodes.get(t));

                                try {
                                    List<String> tokenizedContextMethodCall = JavaTokenizer.tokenize(contextMethodCall);
                                    while (tokenizedContextMethodCall.get(tokenizedContextMethodCall.size() - 1).equals("")) {
                                        tokenizedContextMethodCall.remove(tokenizedContextMethodCall.size() - 1);
                                    }
                                    tokenizedContextMethodCall = truncateList(tokenizedContextMethodCall);

                                    List<NodeSequenceInfo> excodeContext = context.getContextFromMethodDeclaration();
                                    excodeContext = truncateList(excodeContext);

                                    ArgRecTest test = new ArgRecTest();
                                    test.setLex_context(tokenizedContextMethodCall);
                                    test.setExcode_context(NodeSequenceInfo.convertListToString(excodeContext));
                                    test.setExpected_excode(NodeSequenceInfo.convertListToString(argExcodes));
                                    test.setExpected_lex(arg.toString());
                                    test.setNext_excode(nextExcodeList);
                                    test.setNext_lex(nextLexList);
                                    test.setExpected_excode_ori(argExcodes);
                                    if (isClean(argExcodes)) {
                                        cleanTest(test);
                                    } else {
                                        test.setIgnored(true);
                                    }
                                    oneArgTests.add(test);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                //System.out.println("No candidate generated: " + methodCall);
                            }

                            contextIdx = k;
                            contextMethodCall += arg.toString() + ',';
                            break;
                        }
                        ++k;
                    }
                }

                MultiMap params = null;
                try {
                    params = fileParser.genParamsAt(methodCall.getArguments().size() - 1);
                    String parsedMethodCall = fileParser.getLastMethodCallGen().replaceAll("[ \r\n]", "");
                    if (!parsedMethodCall.equals(methodCallContent.replaceAll("[ \r\n]", ""))) {
                        throw new ParseException(fileParser.getLastMethodCallGen() + " was parsed instead of " + methodCallContent
                                + " at " + methodCall.getBegin().get());
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println(methodCall);
                    System.out.println(methodCall.getBegin().get());
                    e.printStackTrace();
                } catch (IndexOutOfBoundsException e) {
                    System.out.println(methodCall);
                    System.out.println(methodCall.getBegin().get());
                    e.printStackTrace();
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                if (params != null && !params.getValue().keySet().isEmpty()) {
                    List<String> nextExcodeList = new ArrayList<>(params.getValue().keySet());
                    List<List<String>> nextLexList = new ArrayList<>();
                    for (String nextExcode : nextExcodeList) {
                        nextLexList.add(params.getValue().get(nextExcode));
                    }
                    ContextInfo context = new ContextInfo(excodes, contextIdx);

                    try {
                        List<String> tokenizedContextMethodCall = JavaTokenizer.tokenize(contextMethodCall);
                        while (!tokenizedContextMethodCall.isEmpty() && tokenizedContextMethodCall.get(tokenizedContextMethodCall.size() - 1).equals("")) {
                            tokenizedContextMethodCall.remove(tokenizedContextMethodCall.size() - 1);
                        }
                        tokenizedContextMethodCall = truncateList(tokenizedContextMethodCall);

                        List<NodeSequenceInfo> excodeContext = context.getContextFromMethodDeclaration();
                        excodeContext = truncateList(excodeContext);

                        ArgRecTest test = new ArgRecTest();
                        test.setLex_context(tokenizedContextMethodCall);
                        test.setExcode_context(NodeSequenceInfo.convertListToString(excodeContext));
                        boolean isClean = true;
                        if (methodCall.getArguments().isEmpty()) {
                            test.setExpected_excode(excodes.get(i).toStringSimple());
                            test.setExpected_lex(")");
                            test.setExpected_excode_ori(Collections.singletonList(excodes.get(i)));
                        } else {
                            List<NodeSequenceInfo> argExcodes = new ArrayList<>();
                            for (int t = contextIdx + 1; t < i; ++t) argExcodes.add(excodes.get(t));

                            // Due to Lambda expression
                            if (argExcodes.isEmpty()) {
                                isClean = false;
                            } else {
                                test.setExpected_excode(NodeSequenceInfo.convertListToString(argExcodes));
                            }
                            test.setExpected_lex(methodCall.getArgument(methodCall.getArguments().size() - 1).toString());
                            test.setExpected_excode_ori(argExcodes);
                            if (!isClean(argExcodes)) isClean = false;
                        }
                        test.setNext_excode(nextExcodeList);
                        test.setNext_lex(nextLexList);
                        if (isClean) {
                            cleanTest(test);
                        } else {
                            test.setIgnored(true);
                        }
                        oneArgTests.add(test);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    //System.out.println("No candidate generated: " + methodCall);
                }

                for (int j = 0; j < oneArgTests.size(); ++j) {
                    ArgRecTest oneArgTest = oneArgTests.get(j);
                    if (j == oneArgTests.size() - 1) {
                        oneArgTest.setArgPos(methodCall.getArguments().size());
                    } else {
                        oneArgTest.setArgPos(j + 1);
                    }

                    String expectedExcode = oneArgTest.getExpected_excode();
                    String expectedLex = oneArgTest.getExpected_lex();
                    for (NodeSequenceInfo argExcode: oneArgTest.getExpected_excode_ori()) {
                        if (NodeSequenceInfo.isMethodAccess(argExcode)) {
                            int tmp = StringUtils.indexOf(expectedExcode, "M_ACCESS(");
                            tmp = expectedExcode.indexOf("OPEN_PART", tmp);
                            oneArgTest.setMethodAccessExcode(expectedExcode.substring(0, tmp + 9));

                            String methodNameArg = argExcode.getAttachedAccess();
                            tmp = StringUtils.indexOf(expectedLex, methodNameArg + "(");
                            oneArgTest.setMethodAccessLex(expectedLex.substring(0, tmp + methodNameArg.length() + 1));

                            break;
                        }
                        if (NodeSequenceInfo.isObjectCreation(argExcode)) {
                            int tmp = StringUtils.indexOf(expectedExcode, "C_CALL(");
                            tmp = expectedExcode.indexOf("OPEN_PART", tmp);
                            oneArgTest.setObjectCreationExcode(expectedExcode.substring(0, tmp + 9));

                            String classNameArg = argExcode.getAttachedAccess();
                            tmp = StringUtils.indexOf(expectedLex, classNameArg + "(");
                            oneArgTest.setObjectCreationLex(expectedLex.substring(0, tmp + classNameArg.length() + 1));

                            break;
                        }
                    }
                }

                tests.addAll(oneArgTests);

                stack.remove(stack.size() - 1);
            }
        }
        return tests;
    }

    public List<ArgRecTest> generateAll(int threshold) {
        List<File> javaFiles = DirProcessor.walkJavaFile(tokenizer.getProject().getAbsolutePath());
        List<ArgRecTest> tests = new ArrayList<>();
        for (File file: javaFiles) {
            tests.addAll(generate(file.getAbsolutePath()));
            if (threshold >= 0 && tests.size() >= threshold) break;
        }
        return tests;
    }

    public List<ArgRecTest> generateAll() {
        return generateAll(-1);
    }

    public static List<AllArgRecTest> getAllArgRecTests(List<ArgRecTest> oneArgRecTests) {
        List<AllArgRecTest> tests = new ArrayList<>();
        List<ArgRecTest> pile = new ArrayList<>();
        for (int i = 0; i < oneArgRecTests.size(); ++i) {
            ArgRecTest oneArgTest = oneArgRecTests.get(i);
            pile.add(oneArgTest);
            if (i == oneArgRecTests.size() - 1 || oneArgRecTests.get(i + 1).getArgPos() <= 1) {
                AllArgRecTest test = new AllArgRecTest();
                if (pile.size() > 0) {
                    test.setLex_context(pile.get(0).getLex_context());
                    test.setExcode_context(pile.get(0).getExcode_context());
                }

                StringBuilder expectedExcode = new StringBuilder();
                for (int j = 0; j < pile.size(); ++j) {
                    expectedExcode.append(pile.get(j).getExpected_excode());
                    if (j < pile.size() - 1) {
                        expectedExcode.append(' ');
                        expectedExcode.append(NodeSequenceInfo.getSEPA(NodeSequenceConstant.SEPA, ',').toStringSimplest());
                        expectedExcode.append(' ');
                    }
                }
                test.setExpected_excode(expectedExcode.toString());

                StringBuilder expectedLex = new StringBuilder();
                for (int j = 0; j < pile.size(); ++j) {
                    expectedLex.append(pile.get(j).getExpected_lex());
                    if (j < pile.size() - 1) expectedLex.append(", ");
                }
                test.setExpected_lex(expectedLex.toString());

                List<List<String>> allNextExcodeList = new ArrayList<>();
                List<List<List<String>>> allNextLexList = new ArrayList<>();
                test.setIgnored(false);
                for (int j = 0; j < pile.size(); ++j) {
                    allNextExcodeList.add(pile.get(j).getNext_excode());
                    allNextLexList.add(pile.get(j).getNext_lex());
                    if (pile.get(j).isIgnored()) {
                        test.setIgnored(true);
                    }
                }
                test.setNext_excode(allNextExcodeList);
                test.setNext_lex(allNextLexList);
                test.setArgRecTestList(pile);
                test.setNumArg(pile.get(pile.size() - 1).getArgPos());

                tests.add(test);

                pile = new ArrayList<>();
            }
        }
        return tests;
    }
}
