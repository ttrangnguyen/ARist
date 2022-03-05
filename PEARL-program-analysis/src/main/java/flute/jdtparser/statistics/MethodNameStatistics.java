package flute.jdtparser.statistics;

import flute.config.Config;
import flute.jdtparser.ProjectParser;
import flute.utils.ProgressBar;
import flute.utils.file_processing.DirProcessor;
import flute.utils.logging.Logger;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class MethodNameStatistics {
    public static void main(String[] args) throws IOException {
        String projectName = "netbeans";
        Config.loadConfig(Config.STORAGE_DIR + "/json/" + projectName + ".json");

        ProjectParser projectParser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH,
                Config.ENCODE_SOURCE, Config.CLASS_PATH, Config.JDT_LEVEL, Config.JAVA_VERSION);

        //get list java files
        List<File> allJavaFiles = DirProcessor.walkJavaFile(Config.PROJECT_DIR);

        List<File> javaFiles = allJavaFiles.stream().filter(file -> {
            if (!file.getAbsolutePath().contains("src")) return false;

            for (String blackName : Config.BLACKLIST_NAME_SRC) {
                if (file.getAbsolutePath().contains(blackName)) return false;
            }

            return true;
        }).collect(Collectors.toList());

        ProgressBar progressBar = new ProgressBar();
        int fileCount = 0;
        //visit astNode
        for (File file : javaFiles) {
            progressBar.setProgress(fileCount++ * 1f / javaFiles.size(), true);
            CompilationUnit cu = projectParser.createCU(file);
            cu.accept(new ASTVisitor() {
                @Override
                public boolean visit(MethodDeclaration methodDeclaration) {
                    if (methodDeclaration.getBody() != null) {
                        methodDeclaration.getBody().accept(new ASTVisitor() {
                            @Override
                            public boolean visit(MethodInvocation methodInvocation) {
                                Logger.write(methodInvocation.getName() + " " + methodDeclaration.getName()
                                        , "methodName_" + projectName + ".txt");
                                return true;
                            }
                        });
                    }
                    return false;
                }
            });
        }
    }
}
