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
import flute.utils.logging.Logger;

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

    public boolean isClean(List<NodeSequenceInfo> nodeSequenceList) {
        for (NodeSequenceInfo excode: nodeSequenceList) {
            //if (NodeSequenceInfo.isLiteral(excode)) return false;
            if (NodeSequenceInfo.isMethodAccess(excode)) return false;
            if (NodeSequenceInfo.isCast(excode)) return false;
            if (NodeSequenceInfo.isConstructorCall(excode)) return false;
            if (NodeSequenceInfo.isAssign(excode)) return false;
            if (NodeSequenceInfo.isOperator(excode)) return false;
            if (NodeSequenceInfo.isUnaryOperator(excode)) return false;
            if (NodeSequenceInfo.isConditionalExpr(excode)) return false;
            if (NodeSequenceInfo.isOpenPart(excode)) return false;
        }
        return true;
    }

    public void cleanTest(ArgRecTest test) {
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
        }
    }

    public List<ArgRecTest> generate(String javaFilePath) {
        System.out.println("File path: " + javaFilePath);
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
                String methodScope = "";
                if (methodCall.getScope().isPresent()) {
                    methodScope = methodCall.getScope().get() + ".";
                    methodName = methodScope + methodName;
                }
                contextMethodCall += methodName + '(';

                File javaFile = new File(javaFilePath);
                Node node = methodDeclaration;
                while (!(node instanceof CompilationUnit)) node = node.getParentNode().get();
                FileParser fileParser = new FileParser(projectParser, javaFile.getName(), node.toString(),
                        methodCall.getBegin().get().line, methodCall.getBegin().get().column);
                int curPos = fileParser.getCurPosition();
                curPos += methodScope.length();
                try {
                    fileParser.setPosition(curPos);
                } catch (Exception e) {
                    // TODO: Handle enums
//                    e.printStackTrace();

                    stack.remove(stack.size() - 1);
                    continue;
                }

                //System.out.println("Position: " + methodCall.getBegin().get());

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
                            }
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
                                    if (isClean(argExcodes)) {
                                        cleanTest(test);
                                        tests.add(test);
                                    }
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

                MultiMap params = null;
                try {
                    params = fileParser.genParamsAt(methodCall.getArguments().size() - 1);
                    String parsedMethodCall = fileParser.getLastMethodCallGen().replaceAll("[ \r\n]", "");
                    if (!parsedMethodCall.equals(methodCallContent.replaceAll("[ \r\n]", ""))) {
                        throw new Exception(fileParser.getLastMethodCallGen() + " was parsed instead of " + methodCallContent
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
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (params != null && !params.getValue().keySet().isEmpty()) {
                    List<String> nextExcodeList = new ArrayList<>(params.getValue().keySet());
                    List<List<String>> nextLexList = new ArrayList<>();
                    for (String nextExcode: nextExcodeList) {
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
                        } else {
                            List<NodeSequenceInfo> argExcodes = new ArrayList<>();
                            for (int t = contextIdx + 1; t < i; ++t) argExcodes.add(excodes.get(t));
                            test.setExpected_excode(NodeSequenceInfo.convertListToString(argExcodes));
                            test.setExpected_lex(methodCall.getArgument(methodCall.getArguments().size() - 1).toString());
                            if (!isClean(argExcodes)) isClean = false;
                        }
                        test.setNext_excode(nextExcodeList);
                        test.setNext_lex(nextLexList);
                        if (isClean) {
                            cleanTest(test);
                            tests.add(test);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

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

    public static void main(String[] args) throws IOException {
        Config.loadConfig(Config.STORAGE_DIR + "/json/ant.json");
        ProjectParser projectParser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH,
                Config.ENCODE_SOURCE, Config.CLASS_PATH, Config.JDT_LEVEL, Config.JAVA_VERSION);
        ArgRecTestGenerator generator = new ArgRecTestGenerator(Config.PROJECT_DIR, projectParser);
        generator.setLengthLimit(20);
        //List<ArgRecTest> tests = generator.generate(Config.REPO_DIR + "sampleproj/src/Main.java");
        List<ArgRecTest> tests = generator.generateAll(1000);
        Gson gson = new Gson();

        for (ArgRecTest test: tests) {
            //System.out.println(gson.toJson(test));
            //Logger.write(gson.toJson(test), "tests.txt");
        }

//        Scanner sc = new Scanner(new File(Config.LOG_DIR + "tests.txt"));
//        List<ArgRecTest> tests = new ArrayList<>();
//        while (sc.hasNextLine()) {
//            String line = sc.nextLine();
//            tests.add(gson.fromJson(line, ArgRecTest.class));
//        }
//        sc.close();

        System.out.println(tests.size());
//        //Collections.shuffle(tests);
//        int testCount = 0;
//        int correctTop1PredictionCount = 0;
//        int correctTopKPredictionCount = 0;
//        try {
//            SocketClient socketClient = new SocketClient(18007);
//            for (ArgRecTest test: tests) {
//                System.out.println("==========================");
//                System.out.println(gson.toJson(test));
//                Response response = socketClient.write(gson.toJson(test));
//                if (response instanceof PredictResponse) {
//                    PredictResponse predictResponse = (PredictResponse) response;
//                    System.out.println("==========================");
//                    System.out.println("Result:");
//                    List<String> results = predictResponse.getData();
//                    results.forEach(item -> {
//                        System.out.println(item);
//                    });
//                    System.out.println("==========================");
//                    System.out.println("Runtime: " + predictResponse.getRuntime() + "s");
//
//                    ++testCount;
//                    if (results.get(0).equals(test.getExpected_lex())) ++correctTop1PredictionCount;
//                    for (String item: results) {
//                        if (item.equals(test.getExpected_lex())) ++correctTopKPredictionCount;
//                    }
//                }
//            }
//            socketClient.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        System.out.println("==========================");
//        System.out.println("Number of tests: " + testCount);
//        System.out.println(String.format("Top-1 accuracy: %.2f%%", 100.0 * correctTop1PredictionCount / testCount));
//        System.out.println(String.format("Top-K accuracy: %.2f%%", 100.0 * correctTopKPredictionCount / testCount));
    }
}
