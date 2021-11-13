package flute.analysis.analysers;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import flute.analysis.structure.DataFrame;
import flute.config.Config;
import flute.preprocessing.FileFilter;
import flute.utils.ProgressBar;
import flute.utils.file_processing.DirProcessor;

import java.io.File;
import java.util.List;

public class JavaAnalyser {
    private final DataFrame dataFrameProject = new DataFrame();
    private final DataFrame dataFrameFile = new DataFrame();
    final DataFrame.Variable seriesMethodDeclaration = new DataFrame.Variable();
    final DataFrame.Variable seriesLOC = new DataFrame.Variable();
    final DataFrame.Variable seriesMethodCall = new DataFrame.Variable();

    long analysingTime = 0;

    DataFrame analyseFile(File file) {
        return new DataFrame();
    }

    public DataFrame analyseProject(File project) {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
        combinedTypeSolver.add(new JavaParserTypeSolver(project));

        List<File> rawJavaFiles = DirProcessor.walkJavaFile(project.getAbsolutePath());
        List<File> javaFiles = FileFilter.filter(rawJavaFiles);
        DataFrame dataFrameOfProject = new DataFrame();
        for (File javaFile: javaFiles) {
            DataFrame dataFrameOfFile = analyseFile(javaFile);
            for (String label: dataFrameOfFile.getLabels()) {
                double sum = dataFrameOfFile.getVariable(label).getSum();
                dataFrameOfProject.insert(label, sum);
                dataFrameFile.insert(label, sum);
            }
        }
        return dataFrameOfProject;
    }

    public void analyseProjects(File directory) {
        List<File> projects = DirProcessor.walkData(directory.getAbsolutePath());

        ProgressBar progressBar = new ProgressBar();
        for (int i = 0; i < projects.size(); ++i) {
            System.out.println("Analyzing: " + projects.get(i).getAbsolutePath());
            DataFrame dataFrameOfProject = analyseProject(projects.get(i));
            for (String label: dataFrameOfProject.getLabels()) {
                dataFrameProject.insert(label, dataFrameOfProject.getVariable(label).getSum());
            }
            progressBar.setProgress(((float)i + 1) / projects.size(), true);
        }
    }

    public void printAnalysingTime() {
        System.out.println("Analyzing time:");
        JavaAnalyser currentAnalyser = this;
        while (currentAnalyser instanceof AnalyzeDecorator) {
            System.out.println(String.format("\t%s: %d s", currentAnalyser.getClass().getSimpleName(), currentAnalyser.analysingTime / 1000000000));
            currentAnalyser = ((AnalyzeDecorator) currentAnalyser).analyser;
        }
        System.out.println();
    }

    private JavaAnalyser getAnalyserOfClass(Class clazz) {
        JavaAnalyser currentAnalyser = this;
        while (currentAnalyser.getClass() != clazz) {
            if (!(currentAnalyser instanceof AnalyzeDecorator)) return null;
            currentAnalyser = ((AnalyzeDecorator) currentAnalyser).analyser;
        }
        return currentAnalyser;
    }

    public DataFrame.Variable getStatisticsByProject(Class clazz) {
        return dataFrameProject.getVariable(clazz.getName());
    }

    public DataFrame.Variable getStatisticsByFile(Class clazz) {
        return dataFrameFile.getVariable(clazz.getName());
    }

    public DataFrame.Variable getStatisticsByMethodDeclaration(Class clazz) {
        return getAnalyserOfClass(clazz).seriesMethodDeclaration;
    }

    public DataFrame.Variable getStatisticsByLOC(Class clazz) {
        return getAnalyserOfClass(clazz).seriesLOC;
    }

    public DataFrame.Variable getStatisticsByMethodCall(Class clazz) {
        return getAnalyserOfClass(clazz).seriesMethodCall;
    }

    public static void main(String[] args) {
        JavaAnalyser javaAnalyser = new JavaAnalyser();
        javaAnalyser = new CountMethodCallDecorator(javaAnalyser);
        javaAnalyser = new CountArgumentDecorator(javaAnalyser);

        javaAnalyser.analyseProjects(new File(Config.REPO_DIR + "oneproj/"));
        //javaAnalyser.analyseProjects(new File("../../Tannm/Flute/storage/repositories/git/"));

        javaAnalyser.printAnalysingTime();
        DataFrame.Variable variable = null;
//        variable = javaAnalyser.getStatisticsByMethodDeclaration(CountMethodCallDecorator.class);
//        System.out.println("Statistics on method calls by method declaration:");
//        System.out.println(DataFrame.describe(variable));

//        variable = javaAnalyser.getStatisticsByLOC(CountMethodCallDecorator.class);
//        System.out.println("Statistics on method calls by loc:");
//        System.out.println(DataFrame.describe(variable));

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
