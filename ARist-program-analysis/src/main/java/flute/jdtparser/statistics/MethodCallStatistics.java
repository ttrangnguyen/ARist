package flute.jdtparser.statistics;

import flute.config.Config;
import flute.data.MethodInvocationModel;
import flute.jdtparser.ProjectParser;
import flute.jdtparser.callsequence.FileNode;
import flute.jdtparser.callsequence.node.cfg.Utils;
import flute.utils.ProgressBar;
import flute.utils.file_processing.DirProcessor;
import flute.utils.logging.Logger;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MethodCallStatistics {
    public static void increaseMap(HashMap<String, Integer> map, String key) {
        if (map.get(key) == null)
            map.put(key, 1);
        else map.put(key, map.get(key) + 1);
    }

    public static void main(String[] args) throws IOException {
        String projectName = "ant";
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

        HashMap<String, Integer> unsortedMap = new HashMap<>();

        ProgressBar progressBar = new ProgressBar();
        int fileCount = 0;
        //visit astNode
        for (File file : javaFiles) {
            progressBar.setProgress(fileCount++ * 1f / javaFiles.size(), true);
            CompilationUnit cu = projectParser.createCU(file);
            cu.accept(new ASTVisitor() {
                @Override
                public boolean visit(MethodInvocation methodInvocation) {
                    if (Config.TEST_APIS.length == 0
                            || Utils.checkTargetAPI(Utils.getOrgPackage(FileNode.genBindingKey(methodInvocation)))) {
                        if (methodInvocation.resolveMethodBinding() != null)
                            increaseMap(unsortedMap, Utils.nodeToString(methodInvocation.resolveMethodBinding()));
                        else
                            increaseMap(unsortedMap, Utils.nodeToString(new MethodInvocationModel(methodInvocation)));
                    }
                    return super.visit(methodInvocation);
                }

                @Override
                public boolean visit(SuperMethodInvocation superMethodInvocation) {
                    if (Config.TEST_APIS.length == 0
                            || Utils.checkTargetAPI(Utils.getOrgPackage(FileNode.genBindingKey(superMethodInvocation)))) {
                        if (superMethodInvocation.resolveMethodBinding() != null)
                            increaseMap(unsortedMap, Utils.nodeToString(superMethodInvocation.resolveMethodBinding()));
                        else
                            increaseMap(unsortedMap, Utils.nodeToString(new MethodInvocationModel(superMethodInvocation)));
                    }
                    return super.visit(superMethodInvocation);
                }
            });
        }

        HashMap<String, Integer> sortedMap = unsortedMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));

        for (Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
            Logger.write(entry.getKey() + " " + entry.getValue()
                    , "methodCall_" + projectName + ".txt");
        }
    }
}
