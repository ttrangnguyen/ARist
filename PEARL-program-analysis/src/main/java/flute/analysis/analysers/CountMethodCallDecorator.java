package flute.analysis.analysers;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import flute.analysis.structure.DataFrame;
import flute.config.Config;
import flute.utils.file_processing.FileProcessor;
import flute.utils.file_processing.LOCCounter;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

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
            if (sum > 0) {
                seriesOfMethodDeclaration.insert(sum);
                seriesLOC.insert(sum);
            }
        }
        return seriesOfMethodDeclaration;
    }

    @Override
    DataFrame analyseFile(File file) {
        DataFrame dataFrameOfFile = super.analyseFile(file);

        long startTime = System.nanoTime();

        AtomicInteger lineHavingMethodCallCount = new AtomicInteger();
        String data = FileProcessor.read(file);
        try {
            CompilationUnit cu = StaticJavaParser.parse(data);
            cu.findAll(MethodDeclaration.class).forEach(methodDeclaration -> {
                DataFrame.Variable seriesOfMethodDeclaration = analyseMethodDeclaration(methodDeclaration);

                lineHavingMethodCallCount.addAndGet(seriesOfMethodDeclaration.getCount());
                dataFrameOfFile.insert(CountMethodCallDecorator.class.getName(), seriesOfMethodDeclaration.getSum());
                seriesMethodDeclaration.insert(seriesOfMethodDeclaration.getSum());
            });
        } catch (ParseProblemException ppe) {
            //ppe.printStackTrace();
        }
        int LOCcount = LOCCounter.countJava(file);
        for (int i = 0; i < LOCcount - lineHavingMethodCallCount.get(); ++i) seriesLOC.insert(0);

        analysingTime += System.nanoTime() - startTime;

        return dataFrameOfFile;
    }

    public static void main(String[] args) {
        JavaAnalyser javaAnalyser = new JavaAnalyser();
        javaAnalyser = new CountMethodCallDecorator(javaAnalyser);

        javaAnalyser.analyseProjects(new File(Config.REPO_DIR + "oneproj/"), true);

        javaAnalyser.printAnalysingTime();
        DataFrame.Variable variable = null;

        variable = javaAnalyser.getStatisticsByMethodDeclaration(CountMethodCallDecorator.class);
        System.out.println("Statistics on method calls by method declaration:");
        System.out.println(DataFrame.describe(variable));
        variable = javaAnalyser.getStatisticsByLOC(CountMethodCallDecorator.class);
        System.out.println("Statistics on method calls by loc:");
        System.out.println(DataFrame.describe(variable));
    }
}
