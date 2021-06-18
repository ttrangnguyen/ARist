package flute.modeling.gpt;

import flute.config.Config;
import flute.utils.ProgressBar;
import flute.utils.file_processing.CommentRemover;
import flute.utils.file_processing.DirProcessor;
import flute.utils.logging.Logger;

import java.io.File;
import java.util.List;

public class Preprocessor {
    public static void preprocessFile(File project, File file) {
        // Remove comments
        String sourceCode = CommentRemover.removeCommentFromFileAfterParsing(file);
        if (sourceCode == null) return;

        // Remove indents
        sourceCode = sourceCode.trim().replaceAll("( |\t)+", " ").replaceAll("\n ", "\n");

        String projectPath = project.getAbsolutePath();
        String relativeFilePath = file.getAbsolutePath();
        relativeFilePath = relativeFilePath.substring(projectPath.length() - project.getName().length());
        Logger.write(sourceCode, "dataset-gpt/" + relativeFilePath);
    }

    public static void preprocessProject(File project) {
        List<File> rawJavaFiles = DirProcessor.walkJavaFile(project.getAbsolutePath());
        List<File> javaFiles = FileFilter.filter(rawJavaFiles);

        for (File javaFile: javaFiles) {
            preprocessFile(project, javaFile);
        }
    }

    public static void preprocessProjects(File folder) {
        List<File> projects = DirProcessor.walkData(folder.getAbsolutePath());

        ProgressBar progressBar = new ProgressBar();
        for (int i = 0; i < projects.size(); ++i) {
            System.out.println("Processing: " + projects.get(i).getAbsolutePath());
            preprocessProject(projects.get(i));
            progressBar.setProgress(((float)i + 1) / projects.size(), true);
        }
    }

    public static void main(String[] args) {
        //Preprocessor.preprocessFile(new File(Config.REPO_DIR + "sampleproj"), new File(Config.REPO_DIR + "sampleproj/src/Test.java"));
        //Preprocessor.preprocessProjects(new File(Config.REPO_DIR + "bulk/"));
        Preprocessor.preprocessProjects(new File("D:\\Java\\Research\\java-data"));
    }
}
