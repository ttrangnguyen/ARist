package flute.analysis.analysers;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.*;
import flute.analysis.structure.DataFrame;
import flute.analysis.structure.StringCounter;
import flute.config.Config;
import flute.utils.ProgressBar;
import flute.utils.file_processing.FileProcessor;
import flute.utils.logging.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CollectIdentifierDecorator extends AnalyzeDecorator {
    public CollectIdentifierDecorator(JavaAnalyser analyser) {
        super(analyser);
    }

    @Override
    DataFrame analyseFile(File file) {
        DataFrame dataFrameOfFile = super.analyseFile(file);

        long startTime = System.nanoTime();

        String data = FileProcessor.read(file);
        try {
            CompilationUnit cu = StaticJavaParser.parse(data);
            cu.findAll(SimpleName.class).forEach(simpleName -> stringCounter.add(simpleName.toString()));
        } catch (ParseProblemException ppe) {
            //ppe.printStackTrace();
        }

        analysingTime += System.nanoTime() - startTime;

        return dataFrameOfFile;
    }

    public static void main(String[] args) {
        JavaAnalyser javaAnalyser = new JavaAnalyser();
        javaAnalyser = new CollectIdentifierDecorator(javaAnalyser);

        List<String> projectList = new ArrayList<>();
        for (int i = 0; i < 9; ++i) {
            projectList.addAll(FileProcessor.readLineByLineToList(String.format("../../Tannm/storage/four_hundred_projects_excluded_%d.txt", i + 1)));
        }
        ProgressBar progressBar = new ProgressBar();
        for (int i = 0; i < projectList.size(); ++i) {
            String project = projectList.get(i);
            javaAnalyser.analyseProject(new File("../../CodeCompletion/dataset/CugLM/java_repos/" + project), true);
            progressBar.setProgress(((float) i + 1) / projectList.size(), true);
        }

        javaAnalyser.printAnalysingTime();
        StringCounter stringCounter = null;

        stringCounter = javaAnalyser.getCollection(CollectIdentifierDecorator.class);
        stringCounter.getDistinctStrings().forEach(identifier -> {
            Logger.write(identifier, "identifier_list.txt");
        });
    }
}