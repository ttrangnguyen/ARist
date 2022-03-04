package flute.analysis.analysers;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import flute.analysis.enumeration.ExpressionType;
import flute.analysis.structure.DataFrame;
import flute.analysis.structure.StringCounter;
import flute.config.Config;
import flute.utils.file_processing.FileProcessor;

import java.io.File;

public class ClassifyArgumentTokenTypeDecorator extends AnalyzeDecorator {
    public ClassifyArgumentTokenTypeDecorator(JavaAnalyser analyser) {
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
                    stringCounter.add(ExpressionType.get(argument).toString());
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
        javaAnalyser = new ClassifyArgumentTokenTypeDecorator(javaAnalyser);

        javaAnalyser.analyseProjects(new File(Config.REPO_DIR + "oneproj/"), true);

        javaAnalyser.printAnalysingTime();
        StringCounter stringCounter = null;

        stringCounter = javaAnalyser.getCollection(ClassifyArgumentTokenTypeDecorator.class);
        System.out.println(stringCounter.describe());
    }
}