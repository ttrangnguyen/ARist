package flute.jdtparser.statistics;

import flute.config.Config;
import flute.jdtparser.ProjectParser;
import flute.jdtparser.statistics.data.DataType;
import flute.jdtparser.statistics.data.StatData;
import flute.utils.file_processing.DirProcessor;

import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;


public class JDTStatistics {
    static StatData statData = new StatData();

    public static void originMethod(CompilationUnit cu, MethodInvocation methodInvocation) {
        ITypeBinding declareType = methodInvocation.resolveMethodBinding().getDeclaringClass();
        if (cu.findDeclaringNode(declareType) != null) {
            statData.increase(DataType.INNER_CLASS_METHOD);
        } else {
            statData.increase(DataType.OTHER_CLASS_METHOD);
        }
        statData.increase(DataType.NUM_METHOD_INVOCATION);
    }

    public static void main(String[] args) throws IOException {
        Config.loadConfig(Config.STORAGE_DIR + "/json/log4j.json");
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

        //visit astnode
        for (File file : javaFiles) {
            CompilationUnit cu = projectParser.createCU(file);
            cu.accept(new ASTVisitor() {
                @Override
                public boolean visit(MethodInvocation methodInvocation) {
                    originMethod(cu, methodInvocation);
                    return true;
                }
            });
        }

        //print result
        System.out.printf("Inner class method declaration: %05.2f%%\n",
                statData.get(DataType.INNER_CLASS_METHOD) * 100f / statData.get(DataType.NUM_METHOD_INVOCATION));
        System.out.printf("Other class method declaration: %05.2f%%\n",
                statData.get(DataType.OTHER_CLASS_METHOD) * 100f / statData.get(DataType.NUM_METHOD_INVOCATION));

    }
}
