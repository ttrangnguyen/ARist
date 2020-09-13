package flute.tokenizing.exe;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.google.gson.Gson;
import flute.communicate.SocketClient;
import flute.communicate.schema.PredictResponse;
import flute.communicate.schema.Response;
import flute.config.Config;
import flute.data.MultiMap;
import flute.jdtparser.FileParser;
import flute.jdtparser.ProjectParser;
import flute.tokenizing.excode_data.ArgRecTest;
import flute.tokenizing.excode_data.ContextInfo;
import flute.tokenizing.excode_data.NodeSequenceInfo;
import flute.utils.StringUtils;
import flute.utils.file_processing.DirProcessor;
import flute.utils.file_processing.JavaTokenizer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ArgRecTestGenerator {
    private JavaExcodeTokenizer tokenizer;
    private ProjectParser projectParser;

    public ArgRecTestGenerator(String projectPath, ProjectParser projectParser) {
        tokenizer = new JavaExcodeTokenizer(projectPath);
        this.projectParser = projectParser;
    }

    public List<ArgRecTest> generate(String javaFilePath) {
        List<ArgRecTest> tests = new ArrayList<>();
        List<NodeSequenceInfo> excodes = tokenizer.tokenize(javaFilePath);
        if (excodes.isEmpty()) return tests;
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

                File javaFile = new File(javaFilePath);
                Node node = excodes.get(0).oriNode;
                while (!(node instanceof CompilationUnit)) node = node.getParentNode().get();
                FileParser fileParser = new FileParser(projectParser, javaFile,
                        methodCall.getBegin().get().line, methodCall.getBegin().get().column);
                int curPos = fileParser.getCurPosition();
                curPos += methodName.length() + 1;
                try {
                    fileParser.setPosition(curPos);
                } catch (Exception e) {
                    System.out.println("File path: " + javaFilePath);
                    System.out.println("Position: " + methodCall.getBegin().get());
                    e.printStackTrace();

                    stack.remove(stack.size() - 1);
                    continue;
                }

                int methodCallIdx = stack.get(stack.size() - 1);
                int k = methodCallIdx + 1;
                int contextIdx = methodCallIdx + 1;
                for (int j = 0; j < methodCall.getArguments().size(); ++j) {
                    Expression arg = methodCall.getArgument(j);
                    while (k <= i) {
                        if (NodeSequenceInfo.isSEPA(excodes.get(k), ',') && excodes.get(k).oriNode == arg) {
                            MultiMap params = fileParser.genParamsAt(j);
                            if (params != null && !params.getValue().keySet().isEmpty()) {
                                List<String> nextExcodeList = new ArrayList<>(params.getValue().keySet());
                                List<List<String>> nextLexList = new ArrayList<>();
                                for (String nextExcode: nextExcodeList) {
                                    nextLexList.add(params.getValue().get(nextExcode));
                                }
                                ContextInfo context = new ContextInfo(excodes, contextIdx);

                                List<NodeSequenceInfo> argExcodes = new ArrayList<>();
                                for (int t = contextIdx + 1; t < k; ++t) argExcodes.add(excodes.get(t));

                                try {
                                    ArgRecTest test = new ArgRecTest();
                                    List<String> tokenizedContextMethodCall = JavaTokenizer.tokenize(contextMethodCall);
                                    while (tokenizedContextMethodCall.get(tokenizedContextMethodCall.size() - 1).equals("")) {
                                        tokenizedContextMethodCall.remove(tokenizedContextMethodCall.size() - 1);
                                    }
                                    test.setLex_context(tokenizedContextMethodCall);
                                    test.setExcode_context(NodeSequenceInfo.convertListToString(context.getContextFromMethodDeclaration()));
                                    test.setExpected_excode(NodeSequenceInfo.convertListToString(argExcodes));
                                    test.setExpected_lex(arg.toString());
                                    test.setNext_excode(nextExcodeList);
                                    test.setNext_lex(nextLexList);
                                    tests.add(test);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            contextIdx = k;
                            contextMethodCall += arg.toString() + ',';
                            break;
                        }
                        ++k;
                    }
                }

                MultiMap params = fileParser.genParamsAt(methodCall.getArguments().size() - 1);
                if (params != null && !params.getValue().keySet().isEmpty()) {
                    List<String> nextExcodeList = new ArrayList<>(params.getValue().keySet());
                    List<List<String>> nextLexList = new ArrayList<>();
                    for (String nextExcode: nextExcodeList) {
                        nextLexList.add(params.getValue().get(nextExcode));
                    }
                    ContextInfo context = new ContextInfo(excodes, contextIdx);

                    try {
                        ArgRecTest test = new ArgRecTest();
                        List<String> tokenizedContextMethodCall = JavaTokenizer.tokenize(contextMethodCall);
                        while (tokenizedContextMethodCall.get(tokenizedContextMethodCall.size() - 1).equals("")) {
                            tokenizedContextMethodCall.remove(tokenizedContextMethodCall.size() - 1);
                        }
                        test.setLex_context(tokenizedContextMethodCall);
                        test.setExcode_context(NodeSequenceInfo.convertListToString(context.getContextFromMethodDeclaration()));
                        if (methodCall.getArguments().isEmpty()) {
                            test.setExpected_excode(excodes.get(i).toStringSimple());
                            test.setExpected_lex(")");
                        } else {
                            List<NodeSequenceInfo> argExcodes = new ArrayList<>();
                            for (int t = contextIdx + 1; t < i; ++t) argExcodes.add(excodes.get(t));
                            test.setExpected_excode(NodeSequenceInfo.convertListToString(argExcodes));
                            test.setExpected_lex(methodCall.getArgument(methodCall.getArguments().size() - 1).toString());
                        }
                        test.setNext_excode(nextExcodeList);
                        test.setNext_lex(nextLexList);
                        tests.add(test);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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

    public static void main(String[] args) throws IOException {
        Config.loadConfig(Config.STORAGE_DIR + "/json/demo.json");
        ProjectParser projectParser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH,
                Config.ENCODE_SOURCE, Config.CLASS_PATH, Config.JDT_LEVEL, Config.JAVA_VERSION);
        ArgRecTestGenerator generator = new ArgRecTestGenerator(Config.PROJECT_DIR, projectParser);
        //List<ArgRecTest> tests = generator.generate(Config.REPO_DIR + "sampleproj/src/Main.java");
        List<ArgRecTest> tests = generator.generateAll();
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
