package flute.jdtparser.statistics;

import flute.config.Config;
import flute.data.MultiMap;
import flute.jdtparser.FileParser;
import flute.jdtparser.ProjectParser;
import flute.utils.ProgressBar;
import flute.utils.file_processing.DirProcessor;
import flute.utils.logging.Logger;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class PublicStaticReduce {
    public static void main(String[] args) throws IOException {
        Config.loadConfig(Config.STORAGE_DIR + "/json/netbeans.json");
        ProjectParser projectParser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH,
                Config.ENCODE_SOURCE, Config.CLASS_PATH, Config.JDT_LEVEL, Config.JAVA_VERSION);

        projectParser.loadPublicStaticMembers();
        projectParser.loadPublicStaticRTMembers();

        //get list java files
        List<File> allJavaFiles = DirProcessor.walkJavaFile(Config.PROJECT_DIR);

        List<File> javaFiles = allJavaFiles.stream().filter(file -> {
            if (!file.getAbsolutePath().contains("src")) return false;

            for (String blackName : Config.BLACKLIST_NAME_SRC) {
                if (file.getAbsolutePath().contains(blackName)) return false;
            }

            return true;
        }).collect(Collectors.toList());
        final int[] testCount = {0};
        AtomicInteger fileCount = new AtomicInteger();
        ProgressBar progressBar = new ProgressBar();

        javaFiles.forEach(file -> {
            FileParser fileParser = new FileParser(projectParser, file, 0, 0);
            fileParser.getCu().accept(new ASTVisitor() {
                @Override
                public boolean visit(MethodInvocation methodInvocation) {
                    try {
                        fileParser.setPosition(methodInvocation.getStartPosition() + 1);
                        fileParser.parse();
                        for (int i = 0; i < methodInvocation.arguments().size(); i++) {
                            MultiMap result = fileParser.genParamsAt(i);
                            long numberOfCandidates = 0;
                            for (Map.Entry<String, List<String>> entry : result.getValue().entrySet()) {
                                numberOfCandidates = numberOfCandidates + entry.getValue().size();
                            }

                            int origin = projectParser.getFasterPublicStaticCandidates(result.getParamTypeKey()).size();
                            int reduce = projectParser.getFasterPublicStaticCandidates(result.getParamTypeKey(), file.getPath()).size();
                            Logger.write(String.format("%s,%d,%d,%d,%d,%d,%d",
                                    file.getPath(),
                                    fileParser.getCu().getLineNumber(methodInvocation.getStartPosition()),
                                    fileParser.getCu().getColumnNumber(methodInvocation.getStartPosition()),
                                    i,
                                    numberOfCandidates,
                                    origin,
                                    reduce), "fullStaticlog_" + String.valueOf(fileCount.get() / 5000) + ".csv");
                        }
                    } catch (Exception e) {
//                        e.printStackTrace();
                    }
                    return true;
                }
            });
            progressBar.setProgress(fileCount.getAndIncrement() * 1f / javaFiles.size(), true);
        });
    }
}
