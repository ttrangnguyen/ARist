package flute.analysis.analysers;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import flute.analysis.enumeration.ExpressionType;
import flute.analysis.structure.DataFrame;
import flute.analysis.structure.StringCounter;
import flute.config.Config;
import flute.utils.LexsimCalculator;
import flute.utils.file_processing.FileProcessor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AnalyseTokenizedMethodCallDecorator extends AnalyzeDecorator {
    public AnalyseTokenizedMethodCallDecorator(JavaAnalyser analyser) {
        super(analyser);
    }

    @Override
    DataFrame analyseFile(File file) {
        DataFrame dataFrameOfFile = super.analyseFile(file);

        long startTime = System.nanoTime();

        String data = FileProcessor.read(file);
        try {
            CompilationUnit cu = StaticJavaParser.parse(data);
            cu.findAll(MethodCallExpr.class).forEach(methodCallExpr -> {
                List<String> tokenizedMethodCall = new ArrayList<>();
                List<String> tokenizedMethodName = LexsimCalculator.tokenize(methodCallExpr.getNameAsString());
                if (!tokenizedMethodName.isEmpty()) {
                    tokenizedMethodCall.addAll(tokenizedMethodName);
                    int cntMethod = 0;
                    for (String token: tokenizedMethodName) {
                        if (stringCounter.getCount(token) > 0) ++cntMethod;
                    }
                    seriesArgument.insert((double) cntMethod / tokenizedMethodName.size());
                }
                NodeList<Expression> arguments = methodCallExpr.getArguments();
                for (int i = 0; i < arguments.size(); ++i) {
                    int argPos = i + 1;
                    List<String> argOperandList = new ArrayList<>();
                    visit(arguments.get(i), argOperandList);
                    List<String> tokenizedArgument = new ArrayList<>();
                    argOperandList.forEach(operand -> {
                        tokenizedArgument.addAll(LexsimCalculator.tokenize(operand));
                    });
                    if (!tokenizedArgument.isEmpty()) {
                        tokenizedMethodCall.addAll(tokenizedArgument);
                        int cntArg = 0;
                        for (String token: tokenizedArgument) {
                            if (stringCounter.getCount(token) > 0) ++cntArg;
                        }
                        seriesArgument.insert((double) cntArg / tokenizedArgument.size());
                    }
                }
                if (!tokenizedMethodCall.isEmpty()) {
                    int cntMethodCall = 0;
                    for (String token: tokenizedMethodCall) {
                        if (stringCounter.getCount(token) > 0) ++cntMethodCall;
                    }
                    seriesMethodCall.insert((double) cntMethodCall / tokenizedMethodCall.size());
                    for (String token: tokenizedMethodCall) stringCounter.add(token);
                }
            });
        } catch (ParseProblemException ppe) {
            //ppe.printStackTrace();
        }

        analysingTime += System.nanoTime() - startTime;

        return dataFrameOfFile;
    }

    private void visit(Node node, List<String> argOperandList) {
        if (node instanceof Expression) {
            String nodeString = node.toString();
            ExpressionType expressionType = ExpressionType.get((Expression) node);
            switch (expressionType) {
                case NAME: case BOOL_LIT: case NULL_LIT:
                    argOperandList.add(nodeString);
                    return;
                case METHOD_INVOC:
                    if (((MethodCallExpr) node).getScope().isPresent()) {
                        visit(((MethodCallExpr) node).getScope().get(), argOperandList);
                    }
                    argOperandList.add(((MethodCallExpr) node).getNameAsString() + "(");
                    return;
                case FIELD_ACCESS:
                    visit(((FieldAccessExpr) node).getScope(), argOperandList);
                    argOperandList.add(((FieldAccessExpr) node).getNameAsString());
                    return;
                case ARRAY_ACCESS:
                    visit(((ArrayAccessExpr) node).getName(), argOperandList);
                    return;
                case CAST:
                    argOperandList.add(((CastExpr) node).getTypeAsString());
                    visit(((CastExpr) node).getExpression(), argOperandList);
                    return;
                case STRING_LIT:
                    argOperandList.add("\"\"");
                    return;
                case NUM_LIT: case CHAR_LIT:
                    argOperandList.add("0");
                    return;
                case TYPE_LIT:
                    argOperandList.add(".class");
                    return;
                case OBJ_CREATION:
                    argOperandList.add("new " + ((ObjectCreationExpr) node).getTypeAsString() + "(");
                    return;
                case ARR_CREATION:
                    argOperandList.add("new " + ((ArrayCreationExpr) node).getElementType().asString() + "[");
                    return;
                case THIS:
                    argOperandList.add("this");
                    return;
                case SUPER:
                    argOperandList.add("super");
                    return;
                case LAMBDA: case METHOD_REF:
                    return;
            }
        }

        node.getChildNodes().forEach(childNode -> {
            visit(childNode, argOperandList);
        });
    }

    public static void main(String[] args) {
        JavaAnalyser javaAnalyser = new JavaAnalyser();
        javaAnalyser = new AnalyseTokenizedMethodCallDecorator(javaAnalyser);

        javaAnalyser.analyseProjects(new File(Config.REPO_DIR + "oneproj/"), true);

        javaAnalyser.printAnalysingTime();
        DataFrame.Variable variable = null;

        variable = javaAnalyser.getStatisticsByMethodCall(AnalyseTokenizedMethodCallDecorator.class);
        System.out.println("Statistics on proportion of method call tokens which are found in seen argument usages:");
        System.out.println(DataFrame.describe(variable));
        variable = javaAnalyser.getStatisticsByArgument(AnalyseTokenizedMethodCallDecorator.class);
        System.out.println("Statistics on proportion of argument tokens which are found in seen argument usages:");
        System.out.println(DataFrame.describe(variable));
    }
}