package flute.analysis.analysers;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import flute.analysis.structure.DataFrame;
import flute.config.Config;
import flute.utils.file_processing.FileProcessor;

import java.io.File;

public class CountArgumentDecorator extends AnalyzeDecorator {
    public CountArgumentDecorator(JavaAnalyser analyser) {
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
                seriesMethodCall.insert(methodCallExpr.getArguments().size());
            });
        } catch (ParseProblemException ppe) {
            //ppe.printStackTrace();
        }

        analysingTime += System.nanoTime() - startTime;

        return dataFrameOfFile;
    }

    public static void main(String[] args) {
        JavaAnalyser javaAnalyser = new JavaAnalyser();
        javaAnalyser = new CountArgumentDecorator(javaAnalyser);

        javaAnalyser.analyseProjects(new File(Config.REPO_DIR + "oneproj/"));

        javaAnalyser.printAnalysingTime();
        DataFrame.Variable variable = null;

        variable = javaAnalyser.getStatisticsByMethodCall(CountArgumentDecorator.class);
        System.out.println("Statistics on arguments:");
        System.out.println(DataFrame.describe(variable));
        System.out.println("Frequency distribution:");
        System.out.println(String.format("\t%3d arguments: %5.2f%%", 0, variable.getProportionOfValue(0, true)));
        System.out.println(String.format("\t%3d arguments: %5.2f%%", 1, variable.getProportionOfValue(1, true)));
        System.out.println(String.format("\t%3d arguments: %5.2f%%", 2, variable.getProportionOfValue(2, true)));
        System.out.println(String.format("\t>=%1d arguments: %5.2f%%", 3, variable.getProportionOfRange(3, variable.getMax(), true)));
    }
}