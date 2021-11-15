package flute.analysis.analysers;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import flute.analysis.enumeration.ExpressionType;
import flute.analysis.structure.DataFrame;
import flute.utils.file_processing.FileProcessor;

import java.io.File;

public class CollectArgumentTokenTypeDecorator extends AnalyzeDecorator {
    public CollectArgumentTokenTypeDecorator(JavaAnalyser analyser) {
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
                methodCallExpr.getArguments().forEach(argument -> {
                    stringCounter.add(ExpressionType.get(argument).toString());
                });
            });
        } catch (ParseProblemException ppe) {
            //ppe.printStackTrace();
        }

        analysingTime += System.nanoTime() - startTime;

        return dataFrameOfFile;
    }
}