package flute.analysis.analysers;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import flute.analysis.structure.DataFrame;
import flute.config.Config;
import flute.utils.file_processing.FileProcessor;

import java.io.File;

public class CountArgumentAccessingMemberDecorator extends AnalyzeDecorator {
    public CountArgumentAccessingMemberDecorator(JavaAnalyser analyser) {
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
                    seriesArgument.insert(argument.toString().chars().filter(ch -> ch == '.').count());
                });
            });
        } catch (ParseProblemException ppe) {
            //ppe.printStackTrace();
        }

        analysingTime += System.nanoTime() - startTime;

        return dataFrameOfFile;
    }

    public static void main(String[] args) {
        JavaAnalyser javaAnalyser = new JavaAnalyser();
        javaAnalyser = new CountArgumentAccessingMemberDecorator(javaAnalyser);

        javaAnalyser.analyseProjects(new File(Config.REPO_DIR + "oneproj/"), true);

        javaAnalyser.printAnalysingTime();
        DataFrame.Variable variable = null;

        variable = javaAnalyser.getStatisticsByArgument(CountArgumentAccessingMemberDecorator.class);
        System.out.println("Statistics on the number of times an argument accesses its members:");
        System.out.println(DataFrame.describe(variable));
        System.out.println("Frequency distribution:");
        System.out.println(String.format("\t%3d times: %5.2f%%", 0, variable.getProportionOfValue(0, true)));
        System.out.println(String.format("\t%3d times: %5.2f%%", 1, variable.getProportionOfValue(1, true)));
        System.out.println(String.format("\t>=%1d times: %5.2f%%", 2, variable.getProportionOfRange(2, variable.getMax(), true)));
    }
}
