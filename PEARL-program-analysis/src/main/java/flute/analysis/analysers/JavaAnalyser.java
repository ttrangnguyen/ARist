package flute.analysis.analysers;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import flute.analysis.structure.DataFrame;
import flute.analysis.structure.StringCounter;
import flute.config.Config;
import flute.jdtparser.ProjectParser;
import flute.preprocessing.FileFilter;
import flute.utils.ProgressBar;
import flute.utils.file_processing.DirProcessor;

import java.io.File;
import java.util.List;

public class JavaAnalyser {
    ProjectParser projectParser;
    String currentProject;

    final StringCounter stringCounter = new StringCounter();

    private final DataFrame dataFrameProject = new DataFrame();
    private final DataFrame dataFrameFile = new DataFrame();
    final DataFrame.Variable seriesMethodDeclaration = new DataFrame.Variable();
    final DataFrame.Variable seriesLOC = new DataFrame.Variable();
    final DataFrame.Variable seriesMethodCall = new DataFrame.Variable();
    final DataFrame.Variable seriesArgument = new DataFrame.Variable();

    long analysingTime = 0;

    void setupParsers(File project, boolean parseStatically) {
        currentProject = project.getName();

        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
        combinedTypeSolver.add(new JavaParserTypeSolver(project));

        if (!parseStatically) {
            Config.autoConfigure(project.getName(), project.getAbsolutePath());
            projectParser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH, Config.ENCODE_SOURCE,
                    Config.CLASS_PATH, Config.JDT_LEVEL, Config.JAVA_VERSION);
        }
    }

    DataFrame analyseFile(File file) {
        return new DataFrame();
    }

    public DataFrame analyseProject(File project, boolean parseStatically) {
        setupParsers(project, parseStatically);

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

    public DataFrame analyseProject(File project) {
        return analyseProject(project, false);
    }

    public void analyseProjects(File directory, boolean parseStatically) {
        List<File> projects = DirProcessor.walkData(directory.getAbsolutePath());

        ProgressBar progressBar = new ProgressBar();
        for (int i = 0; i < projects.size(); ++i) {
            System.out.println("Analyzing: " + projects.get(i).getAbsolutePath());
            DataFrame dataFrameOfProject = analyseProject(projects.get(i), parseStatically);
            for (String label: dataFrameOfProject.getLabels()) {
                dataFrameProject.insert(label, dataFrameOfProject.getVariable(label).getSum());
            }
            progressBar.setProgress(((float)i + 1) / projects.size(), true);
        }
    }

    public void analyseProjects(File directory) {
        analyseProjects(directory, false);
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

    public StringCounter getCollection(Class clazz) {
        return getAnalyserOfClass(clazz).stringCounter;
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

    public DataFrame.Variable getStatisticsByArgument(Class clazz) {
        return getAnalyserOfClass(clazz).seriesArgument;
    }

    public static void main(String[] args) {
        JavaAnalyser javaAnalyser = new JavaAnalyser();
//        javaAnalyser = new CollectArgumentDataTypeDecorator(javaAnalyser);
        javaAnalyser = new CollectMethodCallSignatureDecorator(javaAnalyser);
//        javaAnalyser = new ClassifyMethodCallDeclaringLibraryDecorator(javaAnalyser);
//        javaAnalyser = new ClassifyArgumentIdentifierDeclaringLibraryDecorator(javaAnalyser);

        //javaAnalyser.analyseProjects(new File(Config.REPO_DIR + "oneproj/"), false);
        javaAnalyser.analyseProjects(new File("../../Tannm/Flute/storage/repositories/git/"), false);

        javaAnalyser.printAnalysingTime();
        DataFrame.Variable variable = null;
        StringCounter stringCounter = null;

//        stringCounter = javaAnalyser.getCollection(CollectArgumentDataTypeDecorator.class);
//        System.out.println(stringCounter.describe(100));

        stringCounter = javaAnalyser.getCollection(CollectMethodCallSignatureDecorator.class);
        System.out.println(stringCounter.describe(100));
        variable = new DataFrame.Variable();
        for (String argUsage: stringCounter.getDistinctStrings()) {
            variable.insert(stringCounter.getCount(argUsage));
        }
        System.out.println("Statistics on usage frequency of method call:");
        System.out.println(DataFrame.describe(variable));
        System.out.println("Frequency distribution of occurrence of method call:");
        for (int i = 1; i <= 9; ++i) {
            System.out.println(String.format("\t%5d times: %5.2f%%", i, variable.getProportionOfValue(i, true)));
        }
        for (int i = 1; i <= 9; ++i) {
            System.out.println(String.format("\t%4dx times: %5.2f%%", i, variable.getProportionOfRange(i*10, (i+1)*10-1, true)));
        }
        System.out.println(String.format("\t>=%3d times: %5.2f%%", 100, variable.getProportionOfRange(100, variable.getMax(), true)));

//        stringCounter = javaAnalyser.getCollection(ClassifyMethodCallDeclaringLibraryDecorator.class);
//        System.out.println(stringCounter.describe());

//        stringCounter = javaAnalyser.getCollection(ClassifyArgumentIdentifierDeclaringLibraryDecorator.class);
//        System.out.println(stringCounter.describe());
    }
}
