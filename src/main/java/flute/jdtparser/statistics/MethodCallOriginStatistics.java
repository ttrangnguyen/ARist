package flute.jdtparser.statistics;

import flute.config.Config;
import flute.jdtparser.ProjectParser;
import flute.utils.ProgressBar;
import org.eclipse.jdt.core.dom.*;
import flute.utils.file_processing.DirProcessor;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MethodCallOriginStatistics {
    static int jreCall = 0;
    static int srcCall = 0;
    static int libCall = 0;

    public static void checkMethod(IMethodBinding methodBinding) {
        if (methodBinding == null) {
            libCall++;
        } else if (methodBinding.getDeclaringClass().getPackage().getName().startsWith("java.")) {
            jreCall++;
        } else {
            srcCall++;
        }
    }

    public static void main(String[] args) throws IOException {
        String projectName = "ant";
        Config.loadConfig(Config.STORAGE_DIR + "/json/" + projectName + ".json");

        ProjectParser projectParser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH,
                Config.ENCODE_SOURCE, new String[]{}, Config.JDT_LEVEL, Config.JAVA_VERSION);

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
                public boolean visit(MethodInvocation methodInvocation) {
                    IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
                    checkMethod(methodBinding);
                    return true;
                }

                @Override
                public boolean visit(SuperMethodInvocation superMethodInvocation) {
                    IMethodBinding methodBinding = superMethodInvocation.resolveMethodBinding();
                    checkMethod(methodBinding);
                    return super.visit(superMethodInvocation);
                }
            });
        }

        float sumCall = (jreCall + srcCall + libCall) / 100f;
        System.out.println(String.format("JRE call: %d ~ %.2f%%", jreCall, jreCall / sumCall));
        System.out.println(String.format("Source call: %d ~ %.2f%%", srcCall, srcCall / sumCall));
        System.out.println(String.format("Lib call: %d ~ %.2f%%", libCall, libCall / sumCall));
    }
}
