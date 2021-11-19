package flute.analysis.analysers;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import flute.analysis.structure.DataFrame;
import flute.config.Config;
import flute.utils.file_processing.FileProcessor;

import java.io.File;

public class CountMethodDeclarationDecorator extends AnalyzeDecorator {
    public CountMethodDeclarationDecorator(JavaAnalyser analyser) {
        super(analyser);
    }

    @Override
    DataFrame analyseFile(File file) {
        DataFrame dataFrameOfFile = super.analyseFile(file);

        long startTime = System.nanoTime();

        String data = FileProcessor.read(file);
        try {
            CompilationUnit cu = StaticJavaParser.parse(data);
            dataFrameOfFile.insert(CountMethodDeclarationDecorator.class.getName(), cu.findAll(MethodDeclaration.class).size());
        } catch (ParseProblemException ppe) {
            //ppe.printStackTrace();
        }

        analysingTime += System.nanoTime() - startTime;

        return dataFrameOfFile;
    }

    public static void main(String[] args) {
        JavaAnalyser javaAnalyser = new JavaAnalyser();
        javaAnalyser = new CountMethodDeclarationDecorator(javaAnalyser);

        javaAnalyser.analyseProjects(new File(Config.REPO_DIR + "oneproj/"));

        javaAnalyser.printAnalysingTime();
        DataFrame.Variable variable = null;

        variable = javaAnalyser.getStatisticsByProject(CountMethodDeclarationDecorator.class);
        System.out.println("Statistics on method declarations:");
        System.out.println(DataFrame.describe(variable));
    }
}