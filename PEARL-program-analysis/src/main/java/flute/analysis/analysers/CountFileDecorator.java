package flute.analysis.analysers;

import flute.analysis.structure.DataFrame;
import flute.config.Config;

import java.io.File;

public class CountFileDecorator extends AnalyzeDecorator {
    public CountFileDecorator(JavaAnalyser analyser) {
        super(analyser);
    }

    @Override
    DataFrame analyseFile(File file) {
        DataFrame dataFrameOfFile = super.analyseFile(file);

        long startTime = System.nanoTime();

        dataFrameOfFile.insert(CountFileDecorator.class.getName(), 1);

        analysingTime += System.nanoTime() - startTime;

        return dataFrameOfFile;
    }

    public static void main(String[] args) {
        JavaAnalyser javaAnalyser = new JavaAnalyser();
        javaAnalyser = new CountFileDecorator(javaAnalyser);

        javaAnalyser.analyseProjects(new File(Config.REPO_DIR + "oneproj/"), true);

        javaAnalyser.printAnalysingTime();
        DataFrame.Variable variable = null;

        variable = javaAnalyser.getStatisticsByProject(CountFileDecorator.class);
        System.out.println("Statistics on files:");
        System.out.println(DataFrame.describe(variable));
    }
}