package flute.analysis.analysers;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import flute.analysis.structure.DataFrame;
import flute.preprocessing.EmptyStringLiteralDecorator;
import flute.utils.file_processing.FileProcessor;

import java.io.File;

public class CountMethodCallDecorator extends AnalyzeDecorator {
    public CountMethodCallDecorator(JavaAnalyser analyser) {
        super(analyser);
    }

    private DataFrame.Variable analyseMethodDeclaration(MethodDeclaration methodDeclaration, int lineCount) {
        DataFrame.Variable seriesOfMethodDeclaration = new DataFrame.Variable();
        DataFrame.Variable counter = new DataFrame.Variable();
        try {
            methodDeclaration.findAll(MethodCallExpr.class).forEach(methodCallExpr -> {
                counter.insert(methodCallExpr.getBegin().get().line);
            });
        } catch (ParseProblemException ppe) {
            //ppe.printStackTrace();
        }
        for (int lineId = 1; lineId <= Math.max(lineCount, counter.getMax()); ++lineId) {
            int sum = counter.countValue(lineId);
            seriesOfMethodDeclaration.insert(sum);
            seriesLine.insert(sum);
        }
        return seriesOfMethodDeclaration;
    }

    @Override
    DataFrame analyseFile(File file) {
        DataFrame dataFrameOfFile = super.analyseFile(file);
        String data = FileProcessor.read(file);
        try {
            CompilationUnit cu = StaticJavaParser.parse(data);
            int lineCount = EmptyStringLiteralDecorator.preprocess(cu.toString()).split("\n").length;
            cu.findAll(MethodDeclaration.class).forEach(methodDeclaration -> {
                double sum = analyseMethodDeclaration(methodDeclaration, lineCount).getSum();
                dataFrameOfFile.insert(CountMethodCallDecorator.class.getName(), sum);
                seriesMethodDeclaration.insert(sum);
            });
        } catch (ParseProblemException ppe) {
            //ppe.printStackTrace();
        }
        return dataFrameOfFile;
    }
}
