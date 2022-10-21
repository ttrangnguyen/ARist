package flute.jdtparser.train;

import com.google.common.collect.Lists;
import com.google.common.math.IntMath;
import flute.config.Config;
import flute.jdtparser.ProjectParser;
import flute.jdtparser.callsequence.FileNode;
import flute.jdtparser.callsequence.node.cfg.Utils;
import flute.utils.ProgressBar;
import flute.utils.file_processing.DirProcessor;
import flute.utils.logging.Logger;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class FoldDiv {
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

        List<File> filteredList = new ArrayList<>();
        ProgressBar progressBar = new ProgressBar();
        AtomicInteger fileCount = new AtomicInteger();

        int batchSize = IntMath.divide(javaFiles.size(), Config.NUM_THREAD, RoundingMode.UP);
        List<List<File>> fileBatches = Lists.partition(javaFiles, batchSize);
        final ExecutorService executor = Executors.newFixedThreadPool(Config.NUM_THREAD); // it's just an arbitrary number
        final List<Future<?>> futures = new ArrayList<>();

        for (List<File> fileBatch : fileBatches) {
            Future<?> future = executor.submit(() -> {
                for (File file : fileBatch) {
                    progressBar.setProgress(fileCount.getAndIncrement() * 1f / javaFiles.size(), true);
                    CompilationUnit cu = projectParser.createCU(file);
                    final boolean[] isAPIFile = {false};
                    cu.accept(new ASTVisitor() {
                        @Override
                        public boolean visit(MethodInvocation methodInvocation) {
                            if (isAPIFile[0]) return false;
                            if (Config.TEST_APIS.length == 0
                                    || Utils.checkTargetAPI(Utils.getOrgPackage(FileNode.genBindingKey(methodInvocation)))) {
                                isAPIFile[0] = true;
                                return false;
                            }
                            return super.visit(methodInvocation);
                        }

                        @Override
                        public boolean visit(SuperMethodInvocation superMethodInvocation) {
                            if (isAPIFile[0]) return false;
                            if (Config.TEST_APIS.length == 0
                                    || Utils.checkTargetAPI(Utils.getOrgPackage(FileNode.genBindingKey(superMethodInvocation)))) {
                                isAPIFile[0] = true;
                                return false;
                            }
                            return super.visit(superMethodInvocation);
                        }
                    });
                    if (isAPIFile[0]) filteredList.add(file);
                }
            });
            futures.add(future);
        }
        boolean isDone = false;
        while (!isDone) {
            boolean isProcessing = false;
            for (Future<?> future : futures) {
                if (!future.isDone()) {
                    isProcessing = true;
                    break;
                }
            }
            if (!isProcessing) isDone = true;
        }
        Collections.shuffle(filteredList);

        int foldSize = IntMath.divide(filteredList.size(), 10, RoundingMode.UP);
        List<List<File>> folds = Lists.partition(filteredList, foldSize);
        for (int i = 0; i < folds.size(); i++) {
            List<File> fold = folds.get(i);
            for (File file : fold) {
                Logger.write(file.getAbsolutePath(), projectName + "_" + i + ".txt");
            }
        }
        System.out.println("Done");
        System.exit(0);
    }
}
