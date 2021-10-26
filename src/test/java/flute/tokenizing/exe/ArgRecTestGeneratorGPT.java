package flute.tokenizing.exe;

import com.github.javaparser.ParseException;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import flute.analysis.ExpressionType;
import flute.analysis.config.Config;
import flute.data.MultiMap;
import flute.jdtparser.ProjectParser;
import flute.preprocessing.MethodExtractor;
import flute.tokenizing.excode_data.ArgRecTest;
import flute.tokenizing.excode_data.ContextInfo;
import flute.tokenizing.excode_data.NodeSequenceInfo;
import flute.tokenizing.excode_data.RecTest;
import flute.utils.StringUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.*;
import java.util.stream.Collectors;

public class ArgRecTestGeneratorGPT extends ArgRecTestGenerator {
    private MethodExtractor methodExtractor;

    public ArgRecTestGeneratorGPT(String projectPath, ProjectParser projectParser) {
        super(projectPath, projectParser);
        methodExtractor = new MethodExtractor();
    }

    @Override
    List<? extends RecTest> generateFromMethodCall(List<NodeSequenceInfo> excodes, int methodCallStartIdx, int methodCallEndIdx,
                                                   MethodCallExpr methodCall, String contextMethodCall, String methodScope, String methodName) {
        List<RecTest> tests = new ArrayList<>();

        // Lack of libraries
        if (!getFileParser().acceptedMethod()) {
            System.err.println("ERROR: Cannot resolve: " + methodCall + ". This may be due to the absence of required libraries.");
            if (Config.LOG_WARNING) System.err.println("WARNING: Corresponding tests will not be generated.");
            return tests;
        }

        ASTNode curMethodScope = getFileParser().getCurMethodScope();
        String contextArg = MethodExtractor.preprocessCodeBlock(contextMethodCall + methodScope + methodName + '(');

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
                            String lexContext = null;
                            Set thisfieldSet = new HashSet();
                            nextLexList.stream().flatMap(Collection::stream).collect(Collectors.toList()).forEach(candidate -> {
                                if (!candidate.endsWith("(") && !candidate.contains(".")) {
                                    thisfieldSet.add(candidate.substring(candidate.lastIndexOf('.') + 1));
                                }
                            });

                            Set thisMethodSet = new HashSet();
                            nextLexList.stream().flatMap(Collection::stream).collect(Collectors.toList()).forEach(candidate -> {
                                if (candidate.endsWith("(") && !candidate.contains(".")) {
                                    thisMethodSet.add(candidate.substring(candidate.lastIndexOf('.') + 1, candidate.length() - 1));
                                }
                            });
                            if (curMethodScope instanceof MethodDeclaration) {
                                lexContext = this.methodExtractor.getMethodDeclarationContextForTesting((MethodDeclaration) curMethodScope, thisfieldSet, thisMethodSet, methodName);
                            } else {
                                lexContext = this.methodExtractor.getInitializerContextForTesting((Initializer) curMethodScope, thisfieldSet, thisMethodSet, methodName);
                            }
                            lexContext += contextArg;

                            ArgRecTest test = new ArgRecTest();
                            test.setLine(methodCall.getBegin().get().line);
                            test.setCol(methodCall.getBegin().get().column);
                            test.setLex_context(Collections.singletonList(lexContext));
                            test.setExcode_context(NodeSequenceInfo.convertListToString(excodeContext));
                            test.setExpected_excode(NodeSequenceInfo.convertListToString(argExcodes));
                            test.setExpected_lex(arg.toString());
                            test.setStaticMemberAccessLex(getFileParser().getTargetPattern(j));
                            test.setArgType(ExpressionType.get(arg));
                            test.setNext_excode(nextExcodeList);
                            test.setNext_lex(nextLexList);
                            test.setCandidates_locality(getCandidatesLocality(nextLexList));
                            test.setCandidates_scope_distance(getCandidatesScopeDistance(nextLexList));
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
                String lexContext = null;
                Set thisfieldSet = new HashSet();
                nextLexList.stream().flatMap(Collection::stream).collect(Collectors.toList()).forEach(candidate -> {
                    if (!candidate.endsWith("(") && !candidate.contains(".")) {
                        thisfieldSet.add(candidate.substring(candidate.lastIndexOf('.') + 1));
                    }
                });

                Set thisMethodSet = new HashSet();
                nextLexList.stream().flatMap(Collection::stream).collect(Collectors.toList()).forEach(candidate -> {
                    if (candidate.endsWith("(") && !candidate.contains(".")) {
                        thisMethodSet.add(candidate.substring(candidate.lastIndexOf('.') + 1, candidate.length() - 1));
                    }
                });
                if (curMethodScope instanceof MethodDeclaration) {
                    lexContext = this.methodExtractor.getMethodDeclarationContextForTesting((MethodDeclaration) curMethodScope, thisfieldSet, thisMethodSet, methodName);
                } else {
                    lexContext = this.methodExtractor.getInitializerContextForTesting((Initializer) curMethodScope, thisfieldSet, thisMethodSet, methodName);
                }
                lexContext += contextArg;

                ArgRecTest test = new ArgRecTest();
                test.setLine(methodCall.getBegin().get().line);
                test.setCol(methodCall.getBegin().get().column);
                test.setLex_context(Collections.singletonList(lexContext));
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
                test.setStaticMemberAccessLex(getFileParser().getTargetPattern(methodCall.getArguments().size() - 1));
                test.setNext_excode(nextExcodeList);
                test.setNext_lex(nextLexList);
                test.setCandidates_locality(getCandidatesLocality(nextLexList));
                test.setCandidates_scope_distance(getCandidatesScopeDistance(nextLexList));
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
//                if (Config.API_CRAWLER && paramName == null) {
//                    try {
//                        if (paramName == null) {
//                            paramName = APICrawler.paramNames(
//                                    getFileParser().getCurMethodInvocation().getClassQualifiedName().orElse(""), getFileParser().getCurMethodInvocation().genMethodString()
//                            ).get(oneArgTest.getArgPos() - 1);
////                        System.out.println(paramName);
//                        }
//                    } catch (Exception e) {
////                    System.out.println(getFileParser().getCurMethodInvocation().getClassQualifiedName());
//                        e.printStackTrace();
//                    }
//                }
                oneArgTest.setParam_name(paramName);
            }
        }

        tests.addAll(oneArgTests);
        return tests;
    }
}
