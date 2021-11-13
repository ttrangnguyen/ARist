package flute.analysis.analysers;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import flute.analysis.structure.DataFrame;
import flute.utils.file_processing.FileProcessor;
import flute.utils.file_processing.LOCCounter;

import java.io.File;

public class CountMethodCallDecorator extends AnalyzeDecorator {
    public CountMethodCallDecorator(JavaAnalyser analyser) {
        super(analyser);
    }

    private DataFrame.Variable analyseMethodDeclaration(MethodDeclaration methodDeclaration) {
        DataFrame.Variable seriesOfMethodDeclaration = new DataFrame.Variable();
        DataFrame.Variable counter = new DataFrame.Variable();
        try {
            methodDeclaration.findAll(MethodCallExpr.class).forEach(methodCallExpr -> {
                counter.insert(methodCallExpr.getBegin().get().line);
            });
        } catch (ParseProblemException ppe) {
            //ppe.printStackTrace();
        }
        for (int lineId = methodDeclaration.getBegin().get().line; lineId <= methodDeclaration.getEnd().get().line; ++lineId) {
            int sum = counter.countValue(lineId);
            seriesOfMethodDeclaration.insert(sum);
            if (sum > 0) seriesLOC.insert(sum);
        }
        return seriesOfMethodDeclaration;
    }

    @Override
    DataFrame analyseFile(File file) {
        DataFrame dataFrameOfFile = super.analyseFile(file);

        long startTime = System.nanoTime();

        String data = FileProcessor.read(file);
        try {
            CompilationUnit cu = StaticJavaParser.parse(data);
            cu.findAll(MethodDeclaration.class).forEach(methodDeclaration -> {
                double sum = analyseMethodDeclaration(methodDeclaration).getSum();
                dataFrameOfFile.insert(CountMethodCallDecorator.class.getName(), sum);
                seriesMethodDeclaration.insert(sum);
            });
        } catch (ParseProblemException ppe) {
            //ppe.printStackTrace();
        }
        int LOCcount = LOCCounter.countJava(file);
        while (seriesLOC.getCount() < LOCcount) seriesLOC.insert(0);

        analysingTime += System.nanoTime() - startTime;

        return dataFrameOfFile;
    }
}
