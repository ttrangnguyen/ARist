package flute.tokenizing.exe;

import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
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
import java.util.*;

public class ArgRecTestGenerator {
    private JavaExcodeTokenizer tokenizer;
    private ProjectParser projectParser;
    private int lengthLimit = -1;

    public List<ArgRecTest> discardedTests = new ArrayList<>();

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
            // TODO: Ignore null literal for now
            if (NodeSequenceInfo.isLiteral(excode, "null")) return false;
            if (NodeSequenceInfo.isMethodAccess(excode)) return false;
            if (NodeSequenceInfo.isCast(excode)) return false;
            if (NodeSequenceInfo.isConstructorCall(excode)) return false;
            if (NodeSequenceInfo.isAssign(excode)) return false;
            if (NodeSequenceInfo.isOperator(excode)) return false;
            if (NodeSequenceInfo.isUnaryOperator(excode)) return false;
            if (NodeSequenceInfo.isConditionalExpr(excode)) return false;

            // For EnclosedExpr
            if (NodeSequenceInfo.isOpenPart(excode)) return false;

            // For static field access
            if (NodeSequenceInfo.isFieldAccess(excode)) {
                FieldAccessExpr fieldAccess = (FieldAccessExpr) excode.oriNode;
                if (fieldAccess.getScope() instanceof NameExpr) {
                    try {
                        ((NameExpr) fieldAccess.getScope()).resolve();
                    }
                    // Field access from generic type
                    catch (IllegalStateException ise) {
                        //System.out.println(excode.oriNode);
                    }
                    // Field access from a class
                    catch (UnsolvedSymbolException use) {
                        String scope = fieldAccess.getScope().toString();
                        if (scope.indexOf('.') >= 0) {
                            scope = scope.substring(scope.lastIndexOf('.') + 1);
                        }
                        if (Character.isUpperCase(scope.charAt(0))) {
                            try {
                                ResolvedFieldDeclaration resolve = fieldAccess.resolve().asField();
                                if (resolve.isStatic()) return false;
                            }
                            // Not an actual field
                            catch (UnsolvedSymbolException use2) {
                                if (!Character.isUpperCase(fieldAccess.getNameAsString().charAt(0))) {
                                    use2.printStackTrace();
                                }
                            }
                        } else {
                            //use.printStackTrace();
                        }
                    }
                }
            }
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
                    e.printStackTrace();

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
                                    if (isClean(argExcodes)) {
                                        cleanTest(test);
                                        tests.add(test);
                                    } else {
                                        discardedTests.add(test);
                                    }
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
                        } else {
                            discardedTests.add(test);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    //System.out.println("No candidate generated: " + methodCall);
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
}
