package flute.jdtparser.statistics;

import flute.config.Config;
import flute.data.MultiMap;
import flute.data.testcase.Candidate;
import flute.jdtparser.FileParser;
import flute.jdtparser.ProjectParser;
import flute.jdtparser.statistics.ps.TestCase;
import flute.testing.CandidateMatcher;
import flute.utils.ProgressBar;
import flute.utils.file_processing.FileProcessor;
import flute.utils.logging.Logger;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PublicStaticReduce {

    public static void main(String[] args) throws Exception {
        List<String> lines = FileProcessor.readLineByLineToList("/Users/maytinhdibo/Project/Flute/storage/logs/sampletest.csv");
        List<TestCase> testCases = new ArrayList<>();
        lines.forEach(line -> {
            String[] testData = line.split(",");
            TestCase testCase = new TestCase(testData[0], Integer.valueOf(testData[1]), Integer.valueOf(testData[2]), Integer.valueOf(testData[3]));
            testCases.add(testCase);
        });

        Config.loadConfig(Config.STORAGE_DIR + "/json/netbeans.json");
        ProjectParser projectParser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH,
                Config.ENCODE_SOURCE, Config.CLASS_PATH, Config.JDT_LEVEL, Config.JAVA_VERSION);
        projectParser.loadPublicStaticMembers();
        projectParser.loadPublicStaticRTMembers();
        projectParser.loadObjectMapping();
        projectParser.loadTypeTree();

        AtomicInteger id = new AtomicInteger();
        testCases.forEach(testCase -> {
            id.getAndIncrement();
            File file = new File("storage/repositories/git/netbeans/" + testCase.getFilePath());
            FileParser fileParser = new FileParser(projectParser, file, 0, 0);
            try {
                fileParser.setPosition(fileParser.getPosition(testCase.getLine(), testCase.getCol()) + 1);
                fileParser.parse();
            } catch (Exception e) {
                e.printStackTrace();
            }
            MultiMap result = fileParser.genParamsAt(testCase.getArgPos());
            AtomicBoolean isMatched = new AtomicBoolean(false);

            String target = "";

            try {
                target = fileParser.getCurMethodInvocation().arguments().get(testCase.getArgPos()).toString();
            } catch (Exception e) {
                target = ")";
            }

            long numberOfCandidates = 0;
            String finalTarget = target;
            for (Map.Entry<String, List<String>> entry : result.getValue().entrySet()) {
                numberOfCandidates = numberOfCandidates + entry.getValue().size();
                entry.getValue().forEach(item -> {
                    Logger.write(item, "testcase/testcase_" + id + ".txt");
                    if (!isMatched.get() && CandidateMatcher.matches(new Candidate(entry.getKey(), item), finalTarget)) {
                        isMatched.set(true);
                    }
                });
            }
            int origin = projectParser.getFasterPublicStaticCandidates(result.getParamTypeKey()).size();
            int reduce = projectParser.getFasterPublicStaticCandidates(result.getParamTypeKey(), file.getPath(), fileParser.getCu().getPackage().getName().toString()).size();
            projectParser.getFasterPublicStaticCandidates(result.getParamTypeKey(), file.getPath(), fileParser.getCu().getPackage().getName().toString()).forEach(item -> {
                Logger.write(item.lexical, "testcase/testcase_" + id + ".txt");
                if (!isMatched.get() && CandidateMatcher.matches(new Candidate(item.excode, item.lexical), finalTarget)) {
                    isMatched.set(true);
                }
            });
            Logger.write(String.format("%s,%d,%d,%d,%d,%d,%d,%b",
                    testCase.getFilePath(),
                    testCase.getLine(),
                    testCase.getCol(),
                    testCase.getArgPos(),
                    numberOfCandidates,
                    origin,
                    reduce,
                    isMatched.get()
            ), "sampletest_run.csv");
        });
    }

    public static void stat() throws IOException {
        Config.loadConfig(Config.STORAGE_DIR + "/json/netbeans.json");
        ProjectParser projectParser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH,
                Config.ENCODE_SOURCE, Config.CLASS_PATH, Config.JDT_LEVEL, Config.JAVA_VERSION);

        projectParser.loadPublicStaticMembers();
        projectParser.loadPublicStaticRTMembers();

        //get list java files
//        List<File> allJavaFiles = DirProcessor.walkJavaFile(Config.PROJECT_DIR);

//        List<File> javaFiles = allJavaFiles.stream().filter(file -> {
//            if (!file.getAbsolutePath().contains("src")) return false;
//
//            for (String blackName : Config.BLACKLIST_NAME_SRC) {
//                if (file.getAbsolutePath().contains(blackName)) return false;
//            }
//
//            return true;
//        }).collect(Collectors.toList());
        Set<File> javaFileSet = new HashSet<>();

        List<String> lines = FileProcessor.readLineByLineToList("/Users/maytinhdibo/Project/Flute/storage/logs/sampletest.csv");
        List<File> javaFiles = new ArrayList<>();


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
