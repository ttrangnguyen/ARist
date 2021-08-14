package flute.preprocessing;

import flute.config.Config;
import flute.utils.ProgressBar;
import flute.utils.file_processing.DirProcessor;
import flute.utils.file_processing.FileProcessor;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Preprocessor {
    public String preprocessFile(File file) {
        return FileProcessor.read(file);
    }

    public String revertFile(File file) {
        return FileProcessor.read(file);
    }

    protected void exportCode(String sourceCode, File outputFolder, File project, File file) {
        //System.out.println(sourceCode);
        String projectPath = project.getAbsolutePath();
        String relativeFilePath = file.getAbsolutePath();
        relativeFilePath = relativeFilePath.substring(projectPath.length() - project.getName().length());
        String outputFilePath = outputFolder.getAbsolutePath() + "/" + relativeFilePath;
        try {
            FileProcessor.write(sourceCode, outputFilePath);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void preprocessProject(File project, File outputFolder, boolean revert) {
        List<File> rawJavaFiles = DirProcessor.walkJavaFile(project.getAbsolutePath());
        List<File> javaFiles = FileFilter.filter(rawJavaFiles);

        boolean flag = true;
        //if (project.getName().compareToIgnoreCase("amaembo_huntbugs") <= 0) flag = false;
        for (File javaFile: javaFiles) {
            //if (javaFile.getName().compareTo("BackLinkAnnotator.java") == 0) flag = true;
            if (!flag) continue;
            String sourceCode = (!revert)? preprocessFile(javaFile): revertFile(javaFile);
            exportCode(sourceCode, outputFolder, project, javaFile);
        }
    }

    public void preprocessProject(File project, File outputFolder) {
        preprocessProjects(project, outputFolder, false);
    }

    public void preprocessProjects(File inputFolder, File outputFolder, boolean revert) {
        List<File> projects = DirProcessor.walkData(inputFolder.getAbsolutePath());

        ProgressBar progressBar = new ProgressBar();
        for (int i = 0; i < projects.size(); ++i) {
            System.out.println("Processing: " + projects.get(i).getAbsolutePath());
            preprocessProject(projects.get(i), outputFolder, revert);
            progressBar.setProgress(((float)i + 1) / projects.size(), true);
        }
    }

    public void preprocessProjects(File inputFolder, File outputFolder) {
        preprocessProjects(inputFolder, outputFolder, false);
    }

    public static void main(String[] args) {
        Preprocessor preprocessor = new Preprocessor();
        preprocessor = new RemoveCommentDecorator(preprocessor);
        preprocessor = new RemovePackageDecorator(preprocessor);
        preprocessor = new RemoveImportDecorator(preprocessor);
        preprocessor = new RemoveNewLineDecorator(preprocessor);
        preprocessor = new RemoveIndentDecorator(preprocessor);

        preprocessor.preprocessProjects(new File("D:\\Java\\Research\\java-data\\"),
                                        new File(Config.LOG_DIR + "dataset-gpt/"));
    }
}
