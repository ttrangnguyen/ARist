package flute.tokenizing.exe;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.google.gson.Gson;
import flute.analysis.config.Config;
import flute.communicate.SocketClient;
import flute.communicate.schema.PredictResponse;
import flute.communicate.schema.Response;
import flute.tokenizing.excode_data.ArgRecTest;
import flute.tokenizing.excode_data.ContextInfo;
import flute.tokenizing.excode_data.NodeSequenceInfo;
import flute.utils.StringUtils;
import flute.utils.file_processing.DirProcessor;
import flute.utils.file_processing.JavaTokenizer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ArgRecTestGenerator {
    private JavaExcodeTokenizer tokenizer;

    public ArgRecTestGenerator(String projectPath) {
        tokenizer = new JavaExcodeTokenizer(projectPath);
    }

    public List<ArgRecTest> generate(String javaFilePath) {
        List<ArgRecTest> tests = new ArrayList<>();
        List<NodeSequenceInfo> excodes = tokenizer.tokenize(javaFilePath);
        List<Integer> stack = new ArrayList<>();
        MethodDeclaration methodDeclaration = null;

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
                if (methodCall.getScope().isPresent()) {
                    methodName = methodCall.getScope().get() + "." + methodName;
                }
                contextMethodCall += methodName + '(';

                int methodCallPos = stack.get(stack.size() - 1);
                int k = methodCallPos + 1;
                int contextPos = methodCallPos + 1;
                for (int j = 0; j < methodCall.getArguments().size(); ++j) {
                    Expression arg = methodCall.getArgument(j);
                    while (k <= i) {
                        if (NodeSequenceInfo.isSEPA(excodes.get(k), ',') && excodes.get(k).oriNode == arg) {
                            ContextInfo context = new ContextInfo(excodes, contextPos);

                            List<NodeSequenceInfo> argExcodes = new ArrayList<>();
                            for (int t = contextPos + 1; t < k; ++t) argExcodes.add(excodes.get(t));

                            try {
                                ArgRecTest test = new ArgRecTest();
                                List<String> tokenizedContextMethodCall = JavaTokenizer.tokenize(contextMethodCall);
                                while (tokenizedContextMethodCall.get(tokenizedContextMethodCall.size() - 1).equals("")) {
                                    tokenizedContextMethodCall.remove(tokenizedContextMethodCall.size() - 1);
                                }
                                test.setLex_context(tokenizedContextMethodCall);
                                test.setExcode_context(NodeSequenceInfo.convertListToString(context.getContextFromMethodDeclaration()));
                                test.setExpected_lex(arg.toString());
                                test.setExpected_excode(NodeSequenceInfo.convertListToString(argExcodes));
                                List<String> tmp = new ArrayList<>();
                                tmp.add(test.getExpected_lex());
                                test.setNext_lex(tmp);
                                tmp = new ArrayList<>();
                                tmp.add(test.getExpected_excode());
                                test.setNext_excode(tmp);
                                tests.add(test);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            contextPos = k;
                            contextMethodCall += arg.toString() + ',';
                            break;
                        }
                        ++k;
                    }
                }
                ContextInfo context = new ContextInfo(excodes, contextPos);

                try {
                    ArgRecTest test = new ArgRecTest();
                    List<String> tokenizedContextMethodCall = JavaTokenizer.tokenize(contextMethodCall);
                    while (tokenizedContextMethodCall.get(tokenizedContextMethodCall.size() - 1).equals("")) {
                        tokenizedContextMethodCall.remove(tokenizedContextMethodCall.size() - 1);
                    }
                    test.setLex_context(tokenizedContextMethodCall);
                    test.setExcode_context(NodeSequenceInfo.convertListToString(context.getContextFromMethodDeclaration()));
                    if (methodCall.getArguments().isEmpty()) {
                        test.setExpected_lex(")");
                        test.setExpected_excode(excodes.get(i).toStringSimple());
                    } else {
                        test.setExpected_lex(methodCall.getArgument(methodCall.getArguments().size() - 1).toString());
                        List<NodeSequenceInfo> argExcodes = new ArrayList<>();
                        for (int t = contextPos + 1; t < i; ++t) argExcodes.add(excodes.get(t));
                        test.setExpected_excode(NodeSequenceInfo.convertListToString(argExcodes));
                    }
                    List<String> tmp = new ArrayList<>();
                    tmp.add(test.getExpected_lex());
                    test.setNext_lex(tmp);
                    tmp = new ArrayList<>();
                    tmp.add(test.getExpected_excode());
                    test.setNext_excode(tmp);
                    tests.add(test);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                stack.remove(stack.size() - 1);
            }
        }
        return tests;
    }

    public List<ArgRecTest> generateAll() {
        List<File> javaFiles = DirProcessor.walkJavaFile(tokenizer.getProject().getAbsolutePath());
        List<ArgRecTest> tests = new ArrayList<>();
        for (File file: javaFiles) {
            tests.addAll(generate(file.getAbsolutePath()));
        }
        return tests;
    }

    public static void main(String[] args) {
        ArgRecTestGenerator generator = new ArgRecTestGenerator(Config.REPO_DIR + "sampleproj/");
        List<ArgRecTest> tests = generator.generate(Config.REPO_DIR + "sampleproj/src/Main.java");
        //List<ArgRecTest> tests = generator.generateAll();
        Gson gson = new Gson();
//        for (ArgRecTest test: tests) {
//            System.out.println(gson.toJson(test));
//        }
//        System.out.println(tests.size());

        int correctTop1PredictionCount = 0;
        int correctTopKPredictionCount = 0;
        try {
            SocketClient socketClient = new SocketClient(18007);
            for (ArgRecTest test: tests) {
                System.out.println("==========================");
                System.out.println(gson.toJson(test));
                Response response = socketClient.write(gson.toJson(test));
                if (response instanceof PredictResponse) {
                    PredictResponse predictResponse = (PredictResponse) response;
                    System.out.println("==========================");
                    System.out.println("Result:");
                    List<String> results = predictResponse.getData();
                    results.forEach(item -> {
                        System.out.println(item);
                    });
                    System.out.println("==========================");
                    System.out.println("Runtime: " + predictResponse.getRuntime() + "s");

                    if (results.get(0).equals(test.getExpected_lex())) ++correctTop1PredictionCount;
                    for (String item: results) {
                        if (item.equals(test.getExpected_lex())) ++correctTopKPredictionCount;
                    }
                }
            }
            socketClient.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("==========================");
        System.out.println("Top-1 accuracy: " + 100.0 * correctTop1PredictionCount / tests.size() + "%");
        System.out.println("Top-K accuracy: " + 100.0 * correctTopKPredictionCount / tests.size() + "%");
    }
}
