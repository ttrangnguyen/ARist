package flute.tokenizing.exe;

import com.google.gson.Gson;
import flute.config.Config;
import flute.jdtparser.ProjectParser;
import flute.tokenizing.excode_data.ArgRecTest;
import flute.tokenizing.excode_data.MultipleArgRecTest;
import flute.utils.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ArgRecDataset {
    public static final Gson gson = new Gson();

    String projectName;
    ProjectParser projectParser;
    RecTestGenerator generator;

    public ArgRecDataset(String projectName) {
        this.projectName = projectName;
    }

    private void setupGenerator() throws IOException {
        setupProjectParser();
        createNewGenerator();
    }

    private void setupProjectParser() throws IOException {
        Config.loadConfig(Config.STORAGE_DIR + "/json/" + projectName + ".json");
        projectParser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH,
                Config.ENCODE_SOURCE, Config.CLASS_PATH, Config.JDT_LEVEL, Config.JAVA_VERSION);
    }

    void createNewGenerator() {
        generator = new ArgRecTestGenerator(Config.PROJECT_DIR, projectParser);
    }

    private List<ArgRecTest> generateFromFile(String filePath) {
        List<ArgRecTest> oneFileTests = (List<ArgRecTest>) generator.generate(Config.REPO_DIR + "git/" + filePath);
        for (ArgRecTest test : oneFileTests) test.setFilePath(filePath);

        if (Config.TEST_APIS != null && Config.TEST_APIS.length > 0) {
            List<ArgRecTest> tmp = new ArrayList<>();
            for (ArgRecTest test: oneFileTests) {
                if (test.getMethodInvocClassQualifiedName() != null) {
                    for (String targetAPI: Config.TEST_APIS) {
                        if (test.getMethodInvocClassQualifiedName().startsWith(targetAPI + '.')) {
                            tmp.add(test);
                            break;
                        }
                    }
                }
            }
            oneFileTests = tmp;
        }

        return oneFileTests;
    }

    public List<MultipleArgRecTest> load(boolean doLogEachFold) throws IOException {
        setupGenerator();
        List<MultipleArgRecTest> tests = new ArrayList<>();
        AllArgRecTestGenerator allArgRecTestGenerator = new AllArgRecTestGenerator((ArgRecTestGenerator) generator);

        File trainListDir = new File("docs/trainFilePath/" + projectName);
        int foldCnt = 0;
        for (File foldList: trainListDir.listFiles()) {
            List<MultipleArgRecTest> oneFoldTests = new ArrayList<>();

            final ExecutorService executor = Executors.newFixedThreadPool(Config.NUM_THREAD); // it's just an arbitrary number
            final List<Future<?>> futures = new ArrayList<>();

            Scanner sc = new Scanner(foldList);
            while (sc.hasNextLine()) {
                String filePath = sc.nextLine();
                if (Config.MULTIPROCESS) {
                    Future<?> future = executor.submit(() -> {
                        createNewGenerator();
                        List<ArgRecTest> oneFileTests = generateFromFile(filePath);
                        oneFoldTests.addAll(allArgRecTestGenerator.generate(oneFileTests));
                    });
                    futures.add(future);
                } else {
                    List<ArgRecTest> oneFileTests = generateFromFile(filePath);
                    oneFoldTests.addAll(allArgRecTestGenerator.generate(oneFileTests));
                }
            }
            sc.close();

            if (Config.MULTIPROCESS) {
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
            }

            if (doLogEachFold) {
                logFold(oneFoldTests, foldCnt);
                foldCnt += 1;
            }
            tests.addAll(oneFoldTests);
        }

        return tests;
    }

    private void logFold(List<MultipleArgRecTest> tests, int fold) {
        for (MultipleArgRecTest test: tests) {
            File classFile = new File(test.getFilePath());
            String className = classFile.getName().replace(".java", ".txt");
            Logger.write(gson.toJson(test),  "dataset/" + projectName + "/fold" + fold + "/" + className);
        }
    }

    public static void main(String[] args) throws IOException {
        ArgRecDataset ds = new ArgRecDataset("eclipse");
        ds.load(true);
        System.exit(0);
    }
}
