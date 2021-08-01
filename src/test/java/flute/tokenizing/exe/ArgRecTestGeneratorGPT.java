package flute.tokenizing.exe;

import com.github.javaparser.ParseException;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import flute.analysis.ExpressionType;
import flute.data.MultiMap;
import flute.jdtparser.ProjectParser;
import flute.preprocessing.StaticMethodExtractor;
import flute.tokenizing.excode_data.ArgRecTest;
import flute.tokenizing.excode_data.ContextInfo;
import flute.tokenizing.excode_data.NodeSequenceInfo;
import flute.tokenizing.excode_data.RecTest;
import flute.utils.StringUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArgRecTestGeneratorGPT extends ArgRecTestGenerator {
    private StaticMethodExtractor methodExtractor;

    public ArgRecTestGeneratorGPT(String projectPath, ProjectParser projectParser) {
        super(projectPath, projectParser);
        methodExtractor = new StaticMethodExtractor();
    }

    @Override
    List<? extends RecTest> generateFromMethodCall(List<NodeSequenceInfo> excodes, int methodCallStartIdx, int methodCallEndIdx,
                                                   MethodCallExpr methodCall, String contextMethodCall, String methodScope, String methodName) {
        List<RecTest> tests = new ArrayList<>();

        String contextArg;
        ASTNode curMethodScope = getFileParser().getCurMethodScope();
        if (curMethodScope instanceof MethodDeclaration) {
            contextArg = this.methodExtractor.getMethodDeclarationContext((MethodDeclaration) curMethodScope, Collections.singleton(methodName));
        } else {
            contextArg = this.methodExtractor.getInitializerContext((Initializer) curMethodScope, Collections.singleton(methodName));
        }
        contextArg += StaticMethodExtractor.preprocessCodeBlock(contextMethodCall + methodScope + methodName + '(');

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

                    if (params != null) {
                        List<String> nextExcodeList = new ArrayList<>(params.getValue().keySet());
                        List<List<String>> nextLexList = new ArrayList<>();
                        for (String nextExcode : nextExcodeList) {
                            nextLexList.add(params.getValue().get(nextExcode));
                        }
                        ContextInfo context = new ContextInfo(excodes, contextIdx);

                        List<NodeSequenceInfo> argExcodes = new ArrayList<>();
                        for (int t = contextIdx + 1; t < k; ++t) argExcodes.add(excodes.get(t));


                            List<NodeSequenceInfo> excodeContext = context.getContextFromMethodDeclaration();

                            ArgRecTest test = new ArgRecTest();
                            test.setLex_context(Collections.singletonList(contextArg));
                            test.setExcode_context(NodeSequenceInfo.convertListToString(excodeContext));
                            test.setExpected_excode(NodeSequenceInfo.convertListToString(argExcodes));
                            test.setExpected_lex(arg.toString());
                            test.setArgType(ExpressionType.get(arg));
                            test.setNext_excode(nextExcodeList);
                            test.setNext_lex(nextLexList);
                            test.setExpected_excode_ori(argExcodes);
                            if (RecTestFilter.predictable(argExcodes)) {
                                RecTestNormalizer.normalize(test);
                            } else {
                                test.setIgnored(true);
                            }
                            oneArgTests.add(test);
                    } else {
                        //System.out.println("No candidate generated: " + methodCall);
                    }

                    contextIdx = k;
                    contextArg += arg.toString() + ",";
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

        if (params != null) {
            List<String> nextExcodeList = new ArrayList<>(params.getValue().keySet());
            List<List<String>> nextLexList = new ArrayList<>();
            for (String nextExcode : nextExcodeList) {
                nextLexList.add(params.getValue().get(nextExcode));
            }
            ContextInfo context = new ContextInfo(excodes, contextIdx);


                List<NodeSequenceInfo> excodeContext = context.getContextFromMethodDeclaration();

                ArgRecTest test = new ArgRecTest();
                test.setLex_context(Collections.singletonList(contextArg));
                test.setExcode_context(NodeSequenceInfo.convertListToString(excodeContext));
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
                    test.setArgType(ExpressionType.get(methodCall.getArgument(methodCall.getArguments().size() - 1)));
                    test.setExpected_excode_ori(argExcodes);
                    if (!RecTestFilter.predictable(argExcodes)) isClean = false;
                }
                test.setNext_excode(nextExcodeList);
                test.setNext_lex(nextLexList);
                if (isClean) {
                    RecTestNormalizer.normalize(test);
                } else {
                    test.setIgnored(true);
                }
                oneArgTests.add(test);
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
                String paramName = getFileParser().getParamName(oneArgTest.getArgPos() - 1).orElse(null);
//                try {
//                    if(paramName == null){
//                        paramName = APICrawler.paramNames(
//                                getFileParser().getCurMethodInvocation().getClassQualifiedName().orElse(""), getFileParser().getCurMethodInvocation().genMethodString()
//                        ).get(oneArgTest.getArgPos() - 1);
////                        System.out.println(paramName);
//                    }
//                } catch (Exception e) {
////                    System.out.println(getFileParser().getCurMethodInvocation().getClassQualifiedName());
//                    e.printStackTrace();
//                }
                oneArgTest.setParam_name(paramName);
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
