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
import flute.utils.file_processing.FileProcessor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CollectArgumentUsageDecorator extends AnalyzeDecorator {
    public CollectArgumentUsageDecorator(JavaAnalyser analyser) {
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
                NodeList<Expression> arguments = methodCallExpr.getArguments();
                for (int i = 0; i < arguments.size(); ++i) {
                    int argPos = i + 1;
                    List<String> argOperandList = new ArrayList<>();
                    visit(arguments.get(i), argOperandList);
                    argOperandList.forEach(operand -> {
                        stringCounter.add(String.format("%s-%d-%s", methodCallExpr.getNameAsString(), argPos, operand));
                    });
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
                case NAME: //case BOOL_LIT: case NULL_LIT:
                    argOperandList.add(nodeString);
                    return;
                case METHOD_INVOC:
                    argOperandList.add(((MethodCallExpr) node).getNameAsString() + "(");
                    return;
                case FIELD_ACCESS:
                    argOperandList.add(((FieldAccessExpr) node).getNameAsString());
                    return;
                case ARRAY_ACCESS:
                    visit(((ArrayAccessExpr) node).getName(), argOperandList);
                    return;
                case CAST:
                    visit(((CastExpr) node).getExpression(), argOperandList);
                    return;
//                case STRING_LIT:
//                    argOperandList.add("\"\"");
//                    return;
//                case NUM_LIT: case CHAR_LIT:
//                    argOperandList.add("0");
//                    return;
                case TYPE_LIT:
                    argOperandList.add(".class");
                    return;
                case OBJ_CREATION:
                    argOperandList.add(((ObjectCreationExpr) node).getTypeAsString() + "(");
                    return;
                case ARR_CREATION:
                    argOperandList.add(((ArrayCreationExpr) node).getElementType().asString() + "[");
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
        javaAnalyser = new CollectArgumentUsageDecorator(javaAnalyser);

        javaAnalyser.analyseProjects(new File(Config.REPO_DIR + "oneproj/"));

        javaAnalyser.printAnalysingTime();
        DataFrame.Variable variable = null;
        StringCounter stringCounter = null;

        stringCounter = javaAnalyser.getCollection(CollectArgumentUsageDecorator.class);
        System.out.println(stringCounter.describe(100));
        variable = new DataFrame.Variable();
        for (String argUsage: stringCounter.getDistinctStrings()) {
            variable.insert(stringCounter.getCount(argUsage));
        }
        System.out.println("Statistics on usage frequency of argument:");
        System.out.println(DataFrame.describe(variable));
        System.out.println("Frequency distribution of frequency usage of argument:");
        for (int i = 1; i <= 9; ++i) {
            System.out.println(String.format("\t%5d times: %5.2f%%", i, variable.getProportionOfValue(i, true)));
        }
        for (int i = 1; i <= 9; ++i) {
            System.out.println(String.format("\t%4dx times: %5.2f%%", i, variable.getProportionOfRange(i*10, (i+1)*10-1, true)));
        }
        System.out.println(String.format("\t>=%3d times: %5.2f%%", 100, variable.getProportionOfRange(100, variable.getMax(), true)));
    }
}