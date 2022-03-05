package flute.jdtparser.statistics;

import flute.config.Config;
import flute.data.MultiMap;
import flute.data.testcase.Candidate;
import flute.jdtparser.FileParser;
import flute.jdtparser.ProjectParser;
import flute.jdtparser.PublicStaticMember;
import flute.testing.CandidateMatcher;
import flute.utils.ProgressBar;
import flute.utils.file_processing.DirProcessor;
import flute.utils.logging.Logger;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

class FullStatStep1 {
    public static void main(String[] args) throws IOException {
        ///home/hieuvd/Kien/Flute-Kien-full/storage/repositories/git/four_hundred/
        File fullFolder = new File(args[2]);
        for (int i = Integer.valueOf(args[0]); i < Integer.valueOf(args[1]); i++) {
            Config.autoConfigure(fullFolder.listFiles()[i].getName(), fullFolder.listFiles()[i].getAbsolutePath());
            ProjectParser projectParser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH,
                    Config.ENCODE_SOURCE, Config.CLASS_PATH, Config.JDT_LEVEL, Config.JAVA_VERSION);

            projectParser.initPublicStaticMembers();
            projectParser.loadPublicStaticRTMembers();
            projectParser.loadObjectMapping();
            projectParser.loadTypeTree();

            List<File> allJavaFiles = DirProcessor.walkJavaFile(Config.PROJECT_DIR);

            List<File> javaFiles = allJavaFiles.stream().filter(file -> {
                if (!file.getAbsolutePath().contains("src")) return false;

                for (String blackName : Config.BLACKLIST_NAME_SRC) {
                    if (file.getAbsolutePath().contains(blackName)) return false;
                }

                return true;
            }).collect(Collectors.toList());

            ProgressBar progressBar = new ProgressBar();
            AtomicInteger count = new AtomicInteger();
            AtomicInteger numberTrue = new AtomicInteger();
            AtomicInteger numberTest = new AtomicInteger();

            javaFiles.forEach(file -> {
                count.getAndIncrement();
//                progressBar.setProgress(count.get() * 1f / javaFiles.size(), true);
                FileParser fileParser = new FileParser(projectParser, file, 0, 0);
                fileParser.getCu().accept(new ASTVisitor() {
                    @Override
                    public boolean visit(MethodInvocation methodInvocation) {
                        try {
                            fileParser.setPosition(methodInvocation.getName().getStartPosition());
                            if (methodInvocation.arguments().size() < 0) return true;

                            if (!methodInvocation.getName().toString()
                                    .equals(fileParser.getCurMethodInvocation().getName().toString())) {
                                return true;
                            }

                            for (int i = 0; i < methodInvocation.arguments().size(); i++) {
                                //start predict
                                MultiMap result = fileParser.genParamsAt(i);
                                if (result == null) throw new Exception();
                                long numberOfCandidates = 0;
                                AtomicBoolean isMatched = new AtomicBoolean(false);

                                String target = "";

                                target = fileParser.getCurMethodInvocation().arguments().get(i).toString();

                                for (Map.Entry<String, List<String>> entry : result.getValue().entrySet()) {
                                    numberOfCandidates = numberOfCandidates + entry.getValue().size();
                                    for (String item : entry.getValue()) {
                                        if (!isMatched.get() && (CandidateMatcher.matches(new Candidate(entry.getKey(), item), target) ||
                                                (fileParser.getTargetPattern(i) != null && CandidateMatcher.matches(new Candidate(entry.getKey(), item), fileParser.getTargetPattern(i))))) {
                                            isMatched.set(true);
                                            break;
                                        }
                                    }
                                }

                                List<PublicStaticMember> resultPublicStatic = projectParser.getFasterPublicStaticCandidates(result.getParamTypeKey(), file.getPath(), fileParser.getCu().getPackage().getName().toString());
                                for (PublicStaticMember item
                                        : resultPublicStatic) {
                                    if (!isMatched.get() && (CandidateMatcher.matches(new Candidate(item.excode, item.lexical), target) ||
                                            (fileParser.getTargetPattern(i) != null && CandidateMatcher.matches(new Candidate(item.excode, item.lexical), fileParser.getTargetPattern(i))))) {
                                        isMatched.set(true);
                                        break;
                                    }
                                }
                                numberTest.getAndIncrement();
                                if (isMatched.get()) numberTrue.getAndIncrement();
                                Logger.write(
                                        String.format("%s,%d,%d,%d,%s,%d,%d,%s,%b",
                                                file.getPath(),
                                                fileParser.getCu().getLineNumber(fileParser.getCurPosition()),
                                                fileParser.getCu().getColumnNumber(fileParser.getCurPosition()),
                                                i,
                                                result.getParamTypeKey(),
                                                numberOfCandidates,
                                                resultPublicStatic.size(),
                                                fileParser.getCurMethodInvocation().arguments().get(i).getClass().getName(),
                                                isMatched.get()
                                        ), "stat/" + Config.PROJECT_NAME + "_stat.csv");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            return true;
                        }
                        return true;
                    }
                });

            });
            System.out.println(String.format("%s,%.2f%% on %d tests", fullFolder.listFiles()[i].getName(), numberTrue.get() * 100f / numberTest.get(), numberTest.get()));
            Logger.write(String.format("%s,%.2f%% on %d tests", fullFolder.listFiles()[i].getName(), numberTrue.get() * 100f / numberTest.get(), numberTest.get()), "stat/full_stat_s1.txt");
        }
    }
}