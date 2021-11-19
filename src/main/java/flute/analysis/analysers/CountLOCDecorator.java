package flute.analysis.analysers;

import flute.analysis.structure.DataFrame;
import flute.config.Config;
import flute.utils.file_processing.LOCCounter;

import java.io.File;

public class CountLOCDecorator extends AnalyzeDecorator {
    public CountLOCDecorator(JavaAnalyser analyser) {
        super(analyser);
    }

    @Override
    DataFrame analyseFile(File file) {
        DataFrame dataFrameOfFile = super.analyseFile(file);

        long startTime = System.nanoTime();

        dataFrameOfFile.insert(CountLOCDecorator.class.getName(), LOCCounter.countJava(file));

        analysingTime += System.nanoTime() - startTime;

        return dataFrameOfFile;
    }

    public static void main(String[] args) {
        JavaAnalyser javaAnalyser = new JavaAnalyser();
        javaAnalyser = new CountLOCDecorator(javaAnalyser);

        javaAnalyser.analyseProjects(new File(Config.REPO_DIR + "oneproj/"));

        javaAnalyser.printAnalysingTime();
        DataFrame.Variable variable = null;

        variable = javaAnalyser.getStatisticsByProject(CountLOCDecorator.class);
        System.out.println("Statistics on lines of code:");
        System.out.println(DataFrame.describe(variable));
    }
}