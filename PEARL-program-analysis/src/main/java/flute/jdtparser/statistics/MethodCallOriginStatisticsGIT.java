package flute.jdtparser.statistics;

import com.google.common.collect.Lists;
import com.google.common.math.IntMath;
import flute.config.Config;
import flute.jdtparser.ProjectParser;
import flute.utils.ProgressBar;
import flute.utils.file_processing.DirProcessor;
import flute.utils.logging.Logger;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

class Data {
    public long jreCall;
    public long srcCall;
    public long libCall;

    public void Data() {
        jreCall = 0;
        srcCall = 0;
        libCall = 0;
    }

    public long getSum() {
        return jreCall + srcCall + libCall;
    }
}

public class MethodCallOriginStatisticsGIT {

    public static void checkMethod(IMethodBinding methodBinding, Data summaryData, Data projectData) {
        if (methodBinding == null) {
            summaryData.libCall++;
            projectData.libCall++;
        } else if (methodBinding.getDeclaringClass().getPackage().getName().startsWith("java.")) {
            summaryData.jreCall++;
            projectData.jreCall++;
        } else {
            summaryData.srcCall++;
            projectData.srcCall++;
        }
    }

    public static void main(String[] args) throws IOException {
        Data summaryData = new Data();

        File projectFolder = new File(Config.STORAGE_DIR + "repositories/git/JAVA_repos");

        File[] projects = projectFolder.listFiles();

        ProgressBar progressBar = new ProgressBar();
        int projectCount = 0;

        for (File project : projects) {
            Data projectData = new Data();
            Config.PROJECT_DIR = project.getAbsolutePath();
            String[] prefixSrc = new String[]{"/src", "/demosrc", "/testsrc", "/antsrc", "/src_ant", "/src/main/java"};
            for (String str : prefixSrc) {
                try {
                    Config.loadSrcPath(Config.PROJECT_DIR, str);
                } catch (Exception e) {
                    System.out.println("a");
                }
            }

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

            if (javaFiles.size() == 0) continue;

            int batchSize = IntMath.divide(javaFiles.size(), Config.NUM_THREAD, RoundingMode.UP);
            List<List<File>> fileBatches = Lists.partition(javaFiles, batchSize);
            final ExecutorService executor = Executors.newFixedThreadPool(Config.NUM_THREAD); // it's just an arbitrary number
            final List<Future<?>> futures = new ArrayList<>();

            for (List<File> fileBatch : fileBatches) {
                Future<?> future = executor.submit(() -> {
                    for (File file : fileBatch) {
                        CompilationUnit cu = projectParser.createCU(file);
                        cu.accept(new ASTVisitor() {
                            @Override
                            public boolean visit(MethodInvocation methodInvocation) {
                                IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
                                checkMethod(methodBinding, summaryData, projectData);
                                return true;
                            }

                            @Override
                            public boolean visit(SuperMethodInvocation superMethodInvocation) {
                                IMethodBinding methodBinding = superMethodInvocation.resolveMethodBinding();
                                checkMethod(methodBinding, summaryData, projectData);
                                return super.visit(superMethodInvocation);
                            }
                        });
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

            Logger.write(String.format("PROJECT JRE call: %d ~ %.2f%%", projectData.jreCall, projectData.jreCall * 100f / projectData.getSum()), "stat.txt");
            Logger.write(String.format("PROJECT Source call: %d ~ %.2f%%", projectData.srcCall, projectData.srcCall * 100f / projectData.getSum()), "stat.txt");
            Logger.write(String.format("PROJECT Lib call: %d ~ %.2f%%", projectData.libCall, projectData.libCall * 100f / projectData.getSum()), "stat.txt");
            Logger.write("====");

            progressBar.setProgress(projectCount++ * 1f / projects.length, true);
            System.out.println(String.format("JRE call: %d ~ %.2f%%", summaryData.jreCall, summaryData.jreCall * 100f / summaryData.getSum()));
            System.out.println(String.format("Source call: %d ~ %.2f%%", summaryData.srcCall, summaryData.srcCall * 100f / summaryData.getSum()));
            System.out.println(String.format("Lib call: %d ~ %.2f%%", summaryData.libCall, summaryData.libCall * 100f / summaryData.getSum()));
        }
    }
}