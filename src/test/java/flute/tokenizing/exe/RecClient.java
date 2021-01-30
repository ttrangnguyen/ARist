package flute.tokenizing.exe;

import com.google.gson.Gson;
import flute.analysis.structure.DataFrame;
import flute.config.Config;
import flute.jdtparser.ProjectParser;
import flute.tokenizing.excode_data.RecTest;
import flute.utils.logging.Logger;
import flute.utils.logging.Timer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public abstract class RecClient {
    private static final Gson gson = new Gson();
    private Class testClass;
    private String projectName;
    ProjectParser projectParser;
    RecTestGenerator generator;
    DataFrame dataFrame = new DataFrame();

    public RecClient(String projectName) {
        this.projectName = projectName;
    }

    void setTestClass(Class testClass) {
        this.testClass = testClass;
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

    abstract void createNewGenerator();

    public List<? extends RecTest> getTests(boolean fromSavefile, boolean doSaveTestsAfterGen) throws IOException {
        List<RecTest> tests;
        if (fromSavefile) {
            tests = readTestsFromFile(Config.LOG_DIR + projectName + "_" + this.testClass.getSimpleName() + "s.txt");
        } else {
            setupGenerator();

            if (projectName.equals("demo")) {
                tests = generateTestsFromDemoProject();
            } else {
                tests = generateTestsFromGitProject();
            }

            if (doSaveTestsAfterGen) saveTests(tests);
        }

        return tests;
    }

    public List<? extends RecTest> getTests(boolean fromSavefile) throws IOException {
        return getTests(fromSavefile, false);
    }

    public List<? extends RecTest> getTestsAndReport(boolean fromSavefile, boolean doSaveTestsAfterGen) throws IOException {
        Timer timer = new Timer();
        timer.startCounter();
        List<? extends RecTest> tests = getTests(fromSavefile, doSaveTestsAfterGen);
        double averageGetTestsTime = timer.getTimeCounter() / 1000f / tests.size();

        //logTests(tests);

        for (RecTest test: tests) dataFrame.insert("Ignored test", test.isIgnored());

        System.out.println("Generated " + dataFrame.getVariable("Ignored test").getCount() + " tests.");
        System.out.println("Ignored " + dataFrame.getVariable("Ignored test").getSum() + " tests.");

        return tests;
    }

    private List<RecTest> readTestsFromFile(String filePath) throws IOException {
        if (this.testClass == null) {
            throw new NullPointerException("Field testClass has not been set!");
        }

        Scanner sc = new Scanner(new File(filePath));
        List<RecTest> tests = new ArrayList<>();
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            tests.add(gson.fromJson(line, (Type) this.testClass));
        }
        sc.close();
        return tests;
    }

    private List<RecTest> generateTestsFromDemoProject() {
        return (List<RecTest>) generator.generateAll();
    }

    private List<RecTest> generateTestsFromGitProject() throws IOException {
        List<RecTest> tests = new ArrayList<>();
        Scanner sc = new Scanner(new File("docs/testFilePath/" + projectName + ".txt"));

        final ExecutorService executor = Executors.newFixedThreadPool(Config.NUM_THREAD); // it's just an arbitrary number
        final List<Future<?>> futures = new ArrayList<>();

        while (sc.hasNextLine()) {
            String filePath = sc.nextLine();
            if (Config.MULTIPROCESS) {
                Future<?> future = executor.submit(() -> {
                    List<RecTest> oneFileTests = (List<RecTest>) generator.generate(Config.REPO_DIR + "git/" + filePath);
                    for (RecTest test : oneFileTests) test.setFilePath(filePath);
                    tests.addAll(oneFileTests);
                });
                futures.add(future);
            } else {
                List<RecTest> oneFileTests = (List<RecTest>) generator.generate(Config.REPO_DIR + "git/" + filePath);
                for (RecTest test : oneFileTests) test.setFilePath(filePath);
                tests.addAll(oneFileTests);
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

        return tests;
    }

    public List<? extends RecTest> generateTestsFromFile(String filePath) throws IOException {
        setupGenerator();
        return generator.generate(filePath);
    }

    private void saveTests(List<RecTest> tests) {
        for (RecTest test: tests) {
            Logger.write(gson.toJson(this.testClass.cast(test)), projectName + "_" + this.testClass.getSimpleName() + "s.txt");
        }
    }

    public static void logTests(List<? extends RecTest> tests) {
        for (RecTest test: tests) {
            System.out.println(gson.toJson(test));
        }
    }
}
