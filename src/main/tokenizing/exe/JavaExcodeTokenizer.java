package tokenizing.exe;

import analysis.config.Config;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import tokenizing.excode_data.NodeSequenceInfo;
import tokenizing.excode_data.SystemTableCrossProject;
import tokenizing.parsing.JavaFileParser;
import tokenizing.visitors.MetricsVisitor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class JavaExcodeTokenizer {
    private File project;

    public JavaExcodeTokenizer(String projectPath) {
        project = new File(projectPath);
        if (!project.exists()) throw new IllegalArgumentException("Project does not exist!");
        if (!project.isDirectory()) throw new IllegalArgumentException("Not a directory!");
        if (project.isHidden()) throw new IllegalArgumentException("Project is hidden!");
        configure();
    }

    private void configure() {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
        combinedTypeSolver.add(new JavaParserTypeSolver(project));
    }

    public ArrayList<NodeSequenceInfo> tokenize(File javaFile) {
        MetricsVisitor visitor = new MetricsVisitor();
        SystemTableCrossProject systemTableCrossProject = new SystemTableCrossProject();
        JavaFileParser.visitFile(visitor, javaFile, systemTableCrossProject, project.getAbsolutePath());
        systemTableCrossProject.getTypeVarNodeSequence();
        return systemTableCrossProject.fileList.get(0).nodeSequenceList;
    }

    public ArrayList<NodeSequenceInfo> tokenize(String javaFilePath) {
        File javaFile = new File(javaFilePath);
        if (!javaFile.exists()) throw new IllegalArgumentException("File does not exist!");
        if (!(javaFile.isFile() && javaFile.getName().toLowerCase().endsWith(".java"))) throw new IllegalArgumentException("Not a java file!");
        if (javaFile.isHidden()) throw new IllegalArgumentException("File is hidden!");
        return tokenize(javaFile);
    }

    public void tokenizeToFile(String javaFilePath, String outputFilePath) {
        ArrayList<NodeSequenceInfo> nodeSequenceList = tokenize(javaFilePath);
        File output = new File(outputFilePath);
        try {
            FileWriter fileWriter = new FileWriter(output, false);
            for (NodeSequenceInfo nodeSequence: nodeSequenceList) {
                fileWriter.append(nodeSequence.toStringSimple() + "\r\n");
                fileWriter.flush();
            }
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        JavaExcodeTokenizer tokenizer = new JavaExcodeTokenizer(Config.REPO_DIR + "sampleproj/");
        tokenizer.tokenizeToFile(Config.REPO_DIR + "sampleproj/src/Main.java", Config.LOG_DIR + "debugTokenizer.txt");
    }
}
