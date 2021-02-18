package flute.tokenizing.exe;

import com.google.common.collect.Lists;
import com.google.common.math.IntMath;
import com.google.gson.Gson;
import flute.analysis.structure.DataFrame;
import flute.communicate.SocketClient;
import flute.communicate.schema.PredictResponse;
import flute.communicate.schema.Response;
import flute.config.Config;
import flute.jdtparser.ProjectParser;
import flute.tokenizing.excode_data.RecTest;
import flute.utils.ProgressBar;
import flute.utils.logging.Logger;
import flute.utils.logging.Timer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public abstract class RecClient {
    public static final Gson gson = new Gson();
    private Class testClass;
    private Map<Integer, Boolean> testMap = new HashMap<>();

    boolean isNGramUsed = false;
    boolean isRNNUsed = false;
    String projectName;
    ProjectParser projectParser;
    RecTestGenerator generator;
    DataFrame dataFrame = new DataFrame();

    int[] tops = {1, 3, 5, 10};

    public RecClient(String projectName) {
        this.projectName = projectName;
    }

    Class getTestClass() {
        return testClass;
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
        dataFrame.insert("averageGetTestsTime", averageGetTestsTime);

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
                    createNewGenerator();
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

    public void validateTests(List<? extends RecTest> tests, boolean doPrintInadequateTests) {
        for (RecTest test: tests)
            if (!test.isIgnored()) {
                boolean adequateGeneratedExcode = false;
                boolean adequateGeneratedLex = false;
                if (RecTester.canAcceptGeneratedExcodes(test)) adequateGeneratedExcode = true;
                if (RecTester.canAcceptGeneratedLexes(test)) adequateGeneratedLex = true;
                dataFrame.insert("Adequate generated excodes", adequateGeneratedExcode);
                dataFrame.insert("Adequate generated lexicals", adequateGeneratedLex);
                dataFrame.insert("Adequate generated candidates", adequateGeneratedExcode && adequateGeneratedLex);
                if (adequateGeneratedExcode && adequateGeneratedLex) {
                    testMap.put(test.getId(), true);
                } else if (doPrintInadequateTests) {
                    Logger.write(gson.toJson(test), projectName + "_inadequate_" + this.testClass.getSimpleName() + "s.txt");
                }
            }
        System.out.printf("Adequate generated excodes: %.2f%%%n",
                dataFrame.getVariable("Adequate generated excodes").getMean() * 100);

        System.out.printf("Adequate generated lexicals: %.2f%%%n",
                dataFrame.getVariable("Adequate generated lexicals").getMean() * 100);

        System.out.printf("Adequate generated candidates: %.2f%%%n",
                dataFrame.getVariable("Adequate generated candidates").getMean() * 100);
    }

    public void validateTests(List<? extends RecTest> tests) {
        validateTests(tests, false);
    }

    abstract SocketClient getSocketClient() throws Exception;
    abstract int getSocketPort();

    public void queryAndTest(List<? extends RecTest> tests, boolean verbose, boolean doPrintIncorrectPrediction) {
        List<? extends List<? extends RecTest>> testBatches = null;

        if (Config.MULTIPROCESS) {
            int batchSize = IntMath.divide(tests.size(), Config.NUM_THREAD, RoundingMode.UP);
            testBatches = Lists.partition(tests, batchSize);
        }

        ProgressBar testProgressBar = new ProgressBar();

        if (Config.MULTIPROCESS) {
            final ExecutorService executor = Executors.newFixedThreadPool(Config.NUM_THREAD); // it's just an arbitrary number
            final List<Future<?>> futures = new ArrayList<>();

            List<? extends List<? extends RecTest>> finalTestBatches = testBatches;
            for (List<? extends RecTest> testBatch : finalTestBatches) {
                Future<?> future = executor.submit(() -> {
                    try {
                        SocketClient socketClient = new SocketClient(getSocketPort());
                        for (RecTest test : testBatch) {
                            dataFrame.insert("Tested", 1);
                            testProgressBar.setProgress(dataFrame.getVariable("Tested").getCount() * 1f / tests.size(), true);

                            queryAndTest(socketClient, test, verbose, doPrintIncorrectPrediction);
                        }
                        socketClient.close();
                    } catch (Exception e) {
                        e.printStackTrace();
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
        } else {
            try {
                SocketClient socketClient = getSocketClient();
                for (RecTest test : tests) {
                    dataFrame.insert("Tested", 1);
                    testProgressBar.setProgress(dataFrame.getVariable("Tested").getCount() * 1f / tests.size(), true);

                    queryAndTest(socketClient, test, verbose, doPrintIncorrectPrediction);
                }
                socketClient.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void queryAndTest(List<? extends RecTest> tests) {
        queryAndTest(tests, false, false);
    }

    public void queryAndTest(RecTest test, boolean verbose, boolean doPrintIncorrectPrediction) {
        try {
            SocketClient socketClient = getSocketClient();
            queryAndTest(socketClient, test, verbose, doPrintIncorrectPrediction);
            socketClient.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void queryAndTest(RecTest test) {
        queryAndTest(test, false, false);
    }

    private void queryAndTest(SocketClient socketClient, RecTest test, boolean verbose, boolean doPrintIncorrectPrediction) throws IOException {
        if (doSkipTest(test)) return;

        Response response = socketClient.write(gson.toJson(test));
        if (response instanceof PredictResponse) {
            test(response, test, verbose, doPrintIncorrectPrediction);
        }
    }

    boolean doSkipTest(RecTest test) {
        return false;
    }

    void test(Response response, RecTest test, boolean verbose, boolean doPrintIncorrectPrediction) {
        PredictResponse predictResponse = (PredictResponse) response;
        isNGramUsed = predictResponse.getData().ngram != null;
        isRNNUsed = predictResponse.getData().rnn != null;
        List<String> nGramResults = null;
        if (isNGramUsed) nGramResults = predictResponse.getData().ngram.getResult();
        List<String> RNNResults = null;
        if (isRNNUsed) RNNResults = predictResponse.getData().rnn.getResult();

        if (verbose) {
            System.out.println("==========================");
            System.out.println(gson.toJson(test));
            if (isNGramUsed) {
                System.out.println("==========================");
                System.out.println("NGram's results:");
                nGramResults.forEach(item -> {
                    System.out.println(item);
                });
                System.out.println("==========================");
                System.out.println("NGram's runtime: " + predictResponse.getData().ngram.getRuntime() + "s");
            }

            if (isRNNUsed) {
                System.out.println("==========================");
                System.out.println("RNN's results:");
                RNNResults.forEach(item -> {
                    System.out.println(item);
                });
                System.out.println("==========================");
                System.out.println("RNN's runtime: " + predictResponse.getData().rnn.getRuntime() + "s");
            }
        }

        if (isNGramUsed) {
            for (int k : this.tops)
                updateTopKResult(test, nGramResults, k, testMap.getOrDefault(test.getId(), false),
                        "nGram", doPrintIncorrectPrediction);
        }

        if (isRNNUsed) {
            for (int k : this.tops)
                updateTopKResult(test, RNNResults, k, testMap.getOrDefault(test.getId(), false),
                        "RNN", doPrintIncorrectPrediction);
        }

        if (isNGramUsed) dataFrame.insert("NGram's runtime", predictResponse.getData().ngram.getRuntime());
        if (isRNNUsed) dataFrame.insert("RNN's runtime", predictResponse.getData().rnn.getRuntime());
    }

    abstract void updateTopKResult(RecTest test, List<String> results, int k, boolean adequateGeneratedCandidate,
                                   String modelName, boolean doPrintIncorrectPrediction);

    public void printTestResult() {
        System.out.println("==========================");
        System.out.println("Number of tests: " + dataFrame.getVariable("Tested").getCount());
        System.out.println("Average parsing runtime: " + dataFrame.getVariable("averageGetTestsTime").getSum() + "s");
        if (isNGramUsed) System.out.println("Average NGram's runtime: " + dataFrame.getVariable("NGram's runtime").getMean() + "s");
        if (isRNNUsed) System.out.println("Average RNN's runtime: " + dataFrame.getVariable("RNN's runtime").getMean() + "s");
        System.out.println("Average overall runtime: "
                + (dataFrame.getVariable("NGram's runtime").getMean()
                + dataFrame.getVariable("RNN's runtime").getMean()
                + dataFrame.getVariable("averageGetTestsTime").getSum()) + "s");
    }
}