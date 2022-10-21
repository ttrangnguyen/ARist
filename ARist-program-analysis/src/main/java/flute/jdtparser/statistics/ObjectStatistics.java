package flute.jdtparser.statistics;

import flute.config.Config;
import flute.jdtparser.ProjectParser;
import flute.utils.ProgressBar;
import flute.utils.file_processing.DirProcessor;
import flute.utils.logging.Logger;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ObjectStatistics {
    static long equalSameType = 0;
    static long equalTypePair = 0;
    static long equalDifTypeFieldAccess = 0;

    public static void main(String[] args) throws IOException {
        Config.loadConfig(Config.STORAGE_DIR + "/json/netbeans.json");
        ProjectParser projectParser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH,
                Config.ENCODE_SOURCE, Config.CLASS_PATH, Config.JDT_LEVEL, Config.JAVA_VERSION);
        System.out.println("Loaded project parser");

        //get list java files
        List<File> allJavaFiles = DirProcessor.walkJavaFile(Config.PROJECT_DIR);

        List<File> javaFiles = allJavaFiles.stream().filter(file -> {
            if (!file.getAbsolutePath().contains("src")) return false;

            for (String blackName : Config.BLACKLIST_NAME_SRC) {
                if (file.getAbsolutePath().contains(blackName)) return false;
            }

            return true;
        }).collect(Collectors.toList());

//        List<File> javaFiles = new ArrayList<>();
//        javaFiles.add(new File("/Users/maytinhdibo/Research/java-data/netbeans/nb/welcome/src/org/netbeans/modules/welcome/content/RSSFeedReaderPanel.java"));

        float countFile = 0;
        ProgressBar progressBar = new ProgressBar();
        System.out.println("Prepare file parser");
        //visit astnode
        for (File file : javaFiles) {
            CompilationUnit cu = projectParser.createCU(file);
            cu.accept(new ASTVisitor() {
                @Override
                public boolean visit(MethodInvocation methodInvocation) {
//                    countEqual(methodInvocation);
                    logCallAndArg(methodInvocation);
                    return true;
                }
            });
            progressBar.setProgress(countFile++ / javaFiles.size(), true);
        }
        System.out.println(String.format("Stat on equals with diff type and field access: %d tests", equalDifTypeFieldAccess));
        System.out.println(String.format("Stat on equals with same type: %d tests", equalTypePair));
        System.out.println(String.format("Stat on equals with same type: %.2f%%", equalSameType * 100f / equalTypePair));
    }

    private static void countEqual(MethodInvocation methodInvocation) {
        if (methodInvocation.getName().toString().equals("equals")
                && methodInvocation.arguments().size() == 1) {
            if (methodInvocation.resolveMethodBinding() != null && methodInvocation.getExpression() != null) {
                equalTypePair++;
                ITypeBinding exprType = methodInvocation.getExpression().resolveTypeBinding();
                ITypeBinding argType = ((Expression) methodInvocation.arguments().get(0)).resolveTypeBinding();

                if (exprType.equals(argType)) {
                    equalSameType++;
                } else {
                    if (methodInvocation.arguments().get(0) instanceof QualifiedName) {
                        equalDifTypeFieldAccess++;
                    }
                    Logger.write(exprType.getName() + "," + argType.getName(), "equals_type.csv");
                }
            }
        }
    }

    private static void logCallAndArg(MethodInvocation methodInvocation) {
        for (Object arg : methodInvocation.arguments()) {
            Expression argExpr = (Expression) arg;
            if (argExpr.toString().contains("\n")) break;
            Logger.write(methodInvocation.getName().toString() + "," + argExpr.toString(), "log_call_arg.csv");
        }
    }

}
