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

    public List<List<MultipleArgRecTest> > load() throws IOException {
        setupGenerator();
        List<List<MultipleArgRecTest> > testFolds = new ArrayList<>();
        AllArgRecTestGenerator allArgRecTestGenerator = new AllArgRecTestGenerator((ArgRecTestGenerator) generator);

        final ExecutorService executor = Executors.newFixedThreadPool(Config.NUM_THREAD); // it's just an arbitrary number
        final List<Future<?>> futures = new ArrayList<>();

        File trainListDir = new File("docs/trainFilePath/" + projectName);
        for (File foldList: trainListDir.listFiles()) {
            List<MultipleArgRecTest> tests = new ArrayList<>();
            Scanner sc = new Scanner(foldList);
            while (sc.hasNextLine()) {
                String filePath = sc.nextLine();
                if (Config.MULTIPROCESS) {
                    Future<?> future = executor.submit(() -> {
                        createNewGenerator();
                        List<ArgRecTest> oneFileTests = (List<ArgRecTest>) generator.generate(Config.REPO_DIR + "git/" + filePath);
                        for (ArgRecTest test : oneFileTests) test.setFilePath(filePath);
                        tests.addAll(allArgRecTestGenerator.generate(oneFileTests));
                    });
                    futures.add(future);
                } else {
                    List<ArgRecTest> oneFileTests = (List<ArgRecTest>) generator.generate(Config.REPO_DIR + "git/" + filePath);
                    for (ArgRecTest test : oneFileTests) test.setFilePath(filePath);
                    tests.addAll(allArgRecTestGenerator.generate(oneFileTests));
                }
            }
            sc.close();
            testFolds.add(tests);
        }

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

        return testFolds;
    }

    public void log(List<List<MultipleArgRecTest> > testFolds) {
        for (int i = 0; i < testFolds.size(); ++i) {
            for (MultipleArgRecTest test: testFolds.get(i)) {
                File classFile = new File(test.getFilePath());
                String className = classFile.getName().replace(".java", ".txt");
                Logger.write(gson.toJson(test),  "dataset/" + projectName + "/fold" + i + "/" + className);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        ArgRecDataset ds = new ArgRecDataset("eclipse");
        List<List<MultipleArgRecTest> > tests = ds.load();
        ds.log(tests);
        System.exit(0);
    }
}
