package flute.tokenizing.exe;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.google.gson.Gson;
import flute.analysis.config.Config;
import flute.tokenizing.excode_data.ArgRecTest;
import flute.tokenizing.excode_data.ContextInfo;
import flute.tokenizing.excode_data.NodeSequenceInfo;
import flute.utils.StringUtils;
import flute.utils.file_processing.JavaTokenizer;

import java.util.ArrayList;
import java.util.List;

public class ArgRecTestGenerator {
    private JavaExcodeTokenizer tokenizer;
    private Gson gson = new Gson();

    public ArgRecTestGenerator(String projectPath) {
        tokenizer = new JavaExcodeTokenizer(projectPath);
    }

    public List<ArgRecTest> generate(String javaFilePath) {
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
                                test.setExpected_excode(NodeSequenceInfo.convertListToString(argExcodes));
                                test.setExpected_lex(arg.toString());

                                System.out.println(gson.toJson(test));
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
                        test.setExpected_excode(excodes.get(i).toStringSimple());
                        test.setExpected_lex(")");
                    } else {
                        List<NodeSequenceInfo> argExcodes = new ArrayList<>();
                        for (int t = contextPos + 1; t < i; ++t) argExcodes.add(excodes.get(t));
                        test.setExpected_excode(NodeSequenceInfo.convertListToString(argExcodes));
                        test.setExpected_lex(methodCall.getArgument(methodCall.getArguments().size() - 1).toString());
                    }

                    System.out.println(gson.toJson(test));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                stack.remove(stack.size() - 1);
            }
        }
        return null;
    }

    public static void main(String[] args) {
        ArgRecTestGenerator generator = new ArgRecTestGenerator(Config.REPO_DIR + "sampleproj/");
        List<ArgRecTest> tests = generator.generate(Config.REPO_DIR + "sampleproj/src/Main.java");
    }
}
