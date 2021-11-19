package flute.analysis.analysers;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.Position;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import flute.analysis.enumeration.ExpressionType;
import flute.analysis.structure.DataFrame;
import flute.utils.file_processing.FileProcessor;

import java.io.File;
import java.util.*;

public class CountLineFromLastUsageOfArgumentDecorator extends AnalyzeDecorator {
    public CountLineFromLastUsageOfArgumentDecorator(JavaAnalyser analyser) {
        super(analyser);
    }

    @Override
    DataFrame analyseFile(File file) {
        DataFrame dataFrameOfFile = super.analyseFile(file);

        long startTime = System.nanoTime();

        String data = FileProcessor.read(file);
        try {
            CompilationUnit cu = StaticJavaParser.parse(data);
            cu.findAll(CallableDeclaration.class).forEach(methodDeclaration -> {
                List<String> operandList = new ArrayList<>();
                List<Position> operandPosList = new ArrayList<>();
                visit(methodDeclaration, operandList, operandPosList, true);
                Map<String, List<Position>> operandMap = new HashMap<>();
                for (int i = 0; i < operandList.size(); ++i) {
                    if (!operandMap.containsKey(operandList.get(i))) {
                        operandMap.put(operandList.get(i), new ArrayList<>());
                    }
                    operandMap.get(operandList.get(i)).add(operandPosList.get(i));
                }

                List<String> argOperandList = new ArrayList<>();
                List<Position> argOperandPosList = new ArrayList<>();
                methodDeclaration.findAll(MethodCallExpr.class).forEach(methodCallExpr -> {
                    methodCallExpr.getArguments().forEach(argument -> {
                        visit(argument, argOperandList, argOperandPosList, false);
                    });
                });
                for (int i = 0; i < argOperandList.size(); ++i) {
                    Position lastUsagePos = searchLastUsage(operandMap.get(argOperandList.get(i)), argOperandPosList.get(i));
                    if (lastUsagePos != null) {
                        seriesArgument.insert(argOperandPosList.get(i).line - lastUsagePos.line);
                    } else {
                        seriesArgument.insert(-1);
                    }
                }
            });
        } catch (ParseProblemException ppe) {
            //ppe.printStackTrace();
        }

        analysingTime += System.nanoTime() - startTime;

        return dataFrameOfFile;
    }

    public static void visit(Node node, List<String> operandList, List<Position> operandPosList, boolean deepRecur) {
        if (node instanceof VariableDeclarator) {
            operandList.add(((VariableDeclarator) node).getNameAsString());
            operandPosList.add(node.getBegin().get());
        } else if (node instanceof Parameter) {
            operandList.add(((Parameter) node).getNameAsString());
            operandPosList.add(node.getBegin().get());
        } else if (node instanceof MethodDeclaration) {
            operandList.add(((MethodDeclaration) node).getNameAsString() + "(");
            operandPosList.add(node.getBegin().get());
        } else if (node instanceof ConstructorDeclaration) {
            operandList.add(((ConstructorDeclaration) node).getNameAsString() + "(");
            operandPosList.add(node.getBegin().get());
        } else if (node instanceof ClassOrInterfaceDeclaration) {
            operandList.add(((ClassOrInterfaceDeclaration) node).getNameAsString() + "(");
            operandPosList.add(node.getBegin().get());
        } else if (node instanceof Expression) {
            String nodeString = node.toString();
            ExpressionType expressionType = ExpressionType.get((Expression) node);
            switch (expressionType) {
                case NAME:
                    operandList.add(nodeString);
                    operandPosList.add(node.getBegin().get());
                    if (deepRecur) break; else return;
                case METHOD_INVOC:
                    operandList.add(((MethodCallExpr) node).getNameAsString() + "(");
                    operandPosList.add(node.getBegin().get());
                    if (deepRecur) break; else return;
                case FIELD_ACCESS:
                    operandList.add(((FieldAccessExpr) node).getNameAsString());
                    operandPosList.add(node.getBegin().get());
                    if (deepRecur) break; else return;
                case ARRAY_ACCESS:
                    if (!deepRecur) {
                        visit(((ArrayAccessExpr) node).getName(), operandList, operandPosList, deepRecur);
                        return;
                    } else break;
                case CAST:
                    if (!deepRecur) {
                        visit(((CastExpr) node).getExpression(), operandList, operandPosList, deepRecur);
                        return;
                    } else break;
                case OBJ_CREATION:
                    operandList.add(((ObjectCreationExpr) node).getTypeAsString() + "(");
                    operandPosList.add(node.getBegin().get());
                    if (deepRecur) break; else return;
                case ARR_CREATION:
                    operandList.add(((ArrayCreationExpr) node).getElementType().asString() + "[");
                    operandPosList.add(node.getBegin().get());
                    if (deepRecur) break; else return;
                case THIS:
                    operandList.add("this");
                    operandPosList.add(node.getBegin().get());
                    if (deepRecur) break; else return;
                case SUPER:
                    operandList.add("super");
                    operandPosList.add(node.getBegin().get());
                    if (deepRecur) break; else return;
                case LAMBDA: case METHOD_REF:
                    if (deepRecur) break; else return;
            }
        }

        node.getChildNodes().forEach(childNode -> {
            visit(childNode, operandList, operandPosList, deepRecur);
        });
    }

    public static Position searchLastUsage(List<Position> list, Position key) {
        int l = 0, r = list.size() - 1;
        while (l <= r) {
            int mid = (l + r) / 2;
            Position midPos = list.get(mid);
            if (midPos.line < key.line || (midPos.line == key.line && midPos.column < key.column)) l = mid + 1; else r = mid - 1;
        }
        return r < 0? null: list.get(r);
    }
}