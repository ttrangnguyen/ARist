package flute.tokenizing.exe;

import com.github.javaparser.ParseException;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import flute.analysis.config.Config;
import flute.data.MultiMap;
import flute.jdtparser.ProjectParser;
import flute.tokenizing.excode_data.*;
import org.eclipse.jdt.core.dom.IMethodBinding;

import java.util.ArrayList;
import java.util.List;

public class MethodCallOriginEnumerator extends MethodCallRecTestGenerator {

    public MethodCallOriginEnumerator(String projectPath, ProjectParser projectParser) {
        super(projectPath, projectParser);
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

        String parsedMethodCall = getFileParser().getLastMethodCallGen().replaceAll("[ \r\n]", "");
        if (!parsedMethodCall.equals(methodCall.toString().replaceAll("[ \r\n]", ""))) {
            System.err.println("ERROR: " + getFileParser().getLastMethodCallGen() + " was parsed instead of " + methodCall.toString()
                    + " at " + methodCall.getBegin().get());
            if (Config.LOG_WARNING) System.err.println("WARNING: Corresponding tests will not be generated.");
            return tests;
        }

        IMethodBinding methodBinding = getFileParser().getCurMethodInvocation().resolveMethodBinding();

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
                        System.err.println(methodCall);
                        System.err.println(methodCall.getBegin().get());
                        e.printStackTrace();
                    } catch (IndexOutOfBoundsException e) {
                        System.err.println(methodCall);
                        System.err.println(methodCall.getBegin().get());
                        e.printStackTrace();
                    } catch (NullPointerException e) {
                        System.err.println(methodCall);
                        System.err.println(methodCall.getBegin().get());
                        e.printStackTrace();
                    }

                    if (/*params != null*/true) {
                        List<NodeSequenceInfo> argExcodes = new ArrayList<>();
                        for (int t = contextIdx + 1; t < k; ++t) argExcodes.add(excodes.get(t));

                        ArgRecTest test = new ArgRecTest();
                        test.setLine(methodCall.getBegin().get().line);
                        test.setCol(methodCall.getBegin().get().column);
                        test.setExpected_excode(NodeSequenceInfo.convertListToString(argExcodes));
                        test.setExpected_lex(arg.toString());
                        if (methodBinding == null) {
                            test.setMethodInvocOrigin("lib");
                        } else if (methodBinding.getDeclaringClass().getPackage().getName().startsWith("java.")) {
                            test.setMethodInvocOrigin("jre");
                        } else {
                            test.setMethodInvocOrigin("src");
                        }
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
                    break;
                }
                ++k;
            }
        }

        MultiMap params = null;
        try {
            params = getFileParser().genParamsAt(methodCall.getArguments().size() - 1);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println(methodCall);
            System.err.println(methodCall.getBegin().get());
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
            System.err.println(methodCall);
            System.err.println(methodCall.getBegin().get());
            e.printStackTrace();
        }

        if (/*params != null*/true) {
            ArgRecTest test = new ArgRecTest();
            test.setLine(methodCall.getBegin().get().line);
            test.setCol(methodCall.getBegin().get().column);
            boolean isClean = true;
            if (methodCall.getArguments().isEmpty()) {
                test.setExpected_excode(excodes.get(methodCallEndIdx).toStringSimple());
                test.setExpected_lex(")");
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
                if (!RecTestFilter.predictable(argExcodes)) isClean = false;
            }
            if (methodBinding == null) {
                test.setMethodInvocOrigin("lib");
            } else if (methodBinding.getDeclaringClass().getPackage().getName().startsWith("java.")) {
                test.setMethodInvocOrigin("jre");
            } else {
                test.setMethodInvocOrigin("src");
            }
            if (isClean) {
                RecTestNormalizer.normalize(test);
            } else {
                test.setIgnored(true);
            }
            oneArgTests.add(test);
        } else {
            //System.out.println("No candidate generated: " + methodCall);
        }

        tests.addAll(oneArgTests);
        return tests;
    }

    @Override
    void postProcess(List<RecTest> tests) {
    }
}
