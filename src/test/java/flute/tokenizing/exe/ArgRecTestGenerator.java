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
import flute.utils.file_processing.JavaTokenizer;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ArgRecTestGenerator extends MethodCallRecTestGenerator {
    private int lengthLimit = -1;

    public ArgRecTestGenerator(String projectPath, ProjectParser projectParser) {
        super(projectPath, projectParser);
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

    @Override
    List<? extends RecTest> generateFromMethodCall(List<NodeSequenceInfo> excodes, int methodCallStartIdx, int methodCallEndIdx,
                                                   MethodCallExpr methodCall, String contextMethodCall, String methodName) {

        List<RecTest> tests = new ArrayList<>();

        String contextArg = contextMethodCall + methodName + '(';
        String classQualifiedName = getFileParser().getCurMethodInvocation().getClassQualifiedName().orElse(null);

        List<ArgRecTest> oneArgTests = new ArrayList<>();
        int k = methodCallStartIdx + 1;
        int contextIdx = methodCallStartIdx + 1;
        for (int j = 0; j < methodCall.getArguments().size(); ++j) {
            Expression arg = methodCall.getArgument(j);
            while (k <= methodCallEndIdx) {
                if (NodeSequenceInfo.isSEPA(excodes.get(k), ',') && excodes.get(k).oriNode == arg) {
                    MultiMap params = null;
                    try {
                        params = getFileParser().genParamsAt(j);
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
                            List<String> tokenizedContextMethodCall = JavaTokenizer.tokenize(contextArg);
                            while (tokenizedContextMethodCall.get(tokenizedContextMethodCall.size() - 1).equals("")) {
                                tokenizedContextMethodCall.remove(tokenizedContextMethodCall.size() - 1);
                            }
                            tokenizedContextMethodCall = truncateList(tokenizedContextMethodCall);

                            List<NodeSequenceInfo> excodeContext = context.getContextFromMethodDeclaration();
                            excodeContext = truncateList(excodeContext);

                            ArgRecTest test = new ArgRecTest();
                            test.setLex_context(tokenizedContextMethodCall);
                            test.setExcode_context(NodeSequenceInfo.convertListToString(excodeContext));
                            test.setMethodScope_name(getFileParser().getCurMethodScopeName().orElse(""));
                            test.setClass_name(getFileParser().getCurClassScopeName().orElse(""));
                            test.setExpected_excode(NodeSequenceInfo.convertListToString(argExcodes));
                            test.setExpected_lex(arg.toString());
                            test.setNext_excode(nextExcodeList);
                            test.setNext_lex(nextLexList);
                            test.setMethodInvocClassQualifiedName(classQualifiedName);
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
                    contextArg += arg.toString() + ',';
                    break;
                }
                ++k;
            }
        }

        MultiMap params = null;
        try {
            params = getFileParser().genParamsAt(methodCall.getArguments().size() - 1);
            String parsedMethodCall = getFileParser().getLastMethodCallGen().replaceAll("[ \r\n]", "");
            if (!parsedMethodCall.equals(methodCall.toString().replaceAll("[ \r\n]", ""))) {
                throw new ParseException(getFileParser().getLastMethodCallGen() + " was parsed instead of " + methodCall.toString()
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
                List<String> tokenizedContextMethodCall = JavaTokenizer.tokenize(contextArg);
                while (!tokenizedContextMethodCall.isEmpty() && tokenizedContextMethodCall.get(tokenizedContextMethodCall.size() - 1).equals("")) {
                    tokenizedContextMethodCall.remove(tokenizedContextMethodCall.size() - 1);
                }
                tokenizedContextMethodCall = truncateList(tokenizedContextMethodCall);

                List<NodeSequenceInfo> excodeContext = context.getContextFromMethodDeclaration();
                excodeContext = truncateList(excodeContext);

                ArgRecTest test = new ArgRecTest();
                test.setLex_context(tokenizedContextMethodCall);
                test.setExcode_context(NodeSequenceInfo.convertListToString(excodeContext));
                test.setMethodScope_name(getFileParser().getCurMethodScopeName().orElse(""));
                test.setClass_name(getFileParser().getCurClassScopeName().orElse(""));
                boolean isClean = true;
                if (methodCall.getArguments().isEmpty()) {
                    test.setExpected_excode(excodes.get(methodCallEndIdx).toStringSimple());
                    test.setExpected_lex(")");
                    test.setExpected_excode_ori(Collections.singletonList(excodes.get(methodCallEndIdx)));
                } else {
                    List<NodeSequenceInfo> argExcodes = new ArrayList<>();
                    for (int t = contextIdx + 1; t < methodCallEndIdx; ++t) argExcodes.add(excodes.get(t));

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
                test.setMethodInvocClassQualifiedName(classQualifiedName);
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

            if (oneArgTest.getArgPos() == 0) {
                oneArgTest.setParam_name("");
            } else {
                //oneArgTest.setParam_name(getFileParser().getParamName(oneArgTest.getArgPos() - 1).orElse(null));
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
        return tests;
    }
}
