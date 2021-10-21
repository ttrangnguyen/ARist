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
import flute.tokenizing.excode_data.ArgRecTest;
import flute.tokenizing.excode_data.MultipleArgRecTest;
import flute.tokenizing.excode_data.RecTest;
import flute.utils.ProgressBar;
import flute.utils.logging.Logger;
import flute.utils.logging.Timer;

import java.io.*;
import java.lang.reflect.Type;
import java.math.RoundingMode;
import java.net.SocketException;
import java.nio.file.NoSuchFileException;
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
    boolean isGPTUsed = false;
    String projectName;
    String projectDir = null;
    ProjectParser projectParser = null;
    RecTestGenerator generator = null;
    DataFrame dataFrame = new DataFrame();

    int[] tops = {1, 3, 5, 10};

    public RecClient(String projectName) {
        this.projectName = projectName;
    }

    public RecClient(String projectName, String projectDir) {
        this(projectName);
        this.projectDir = projectDir;
    }

    Class getTestClass() {
        return testClass;
    }

    void setTestClass(Class testClass) {
        this.testClass = testClass;
    }

    private void setupGenerator() throws IOException {
        setupProjectParser();
        if (generator == null) createNewGenerator();
    }

    void setupProjectParser() throws IOException {
        if (projectParser != null) return;
        try {
            Config.loadConfig(Config.STORAGE_DIR + "/json/" + projectName + ".json");
        } catch (NoSuchFileException nsfe) {
            System.err.println("WARNING: Config file does not exist: " + nsfe.getFile());
            System.err.println("Project Parser is now configured automatically.");

            if (projectDir == null) {
                if (projectName.equals("demo")) {
                    Config.autoConfigure(projectName, Config.REPO_DIR + "sampleproj");
                } else {
                    Config.autoConfigure(projectName, Config.REPO_DIR + "git/" + projectName);
                }
            } else {
                Config.autoConfigure(projectName, projectDir);
            }
        }

        projectParser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH,
                Config.ENCODE_SOURCE, Config.CLASS_PATH, Config.JDT_LEVEL, Config.JAVA_VERSION);
        projectParser.initPublicStaticMembers();
        projectParser.loadPublicStaticMembers();
        projectParser.loadPublicStaticRTMembers();
        projectParser.loadObjectMapping();
        projectParser.loadTypeTree();
    }

    abstract void createNewGenerator();

    public List<? extends RecTest> getTests(boolean fromSavefile, boolean doSaveTestsAfterGen) throws IOException {
        List<RecTest> tests;
        setupGenerator();
        if (fromSavefile) {
            tests = readTestsFromFile(Config.LOG_DIR + projectName + "_" + this.testClass.getSimpleName() + "s.txt");
        } else {
            if (projectDir == null && !projectName.equals("demo")) {
                tests = generateTestsFromGitProject();
            } else {
                tests = generateTestsRecursively();
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

        for (RecTest test : tests) dataFrame.insert("Ignored test", test.isIgnored());

        System.out.println("Generated " + dataFrame.getVariable("Ignored test").getCount() + " tests.");
        System.out.println("Supported " + (dataFrame.getVariable("Ignored test").getCount() - dataFrame.getVariable("Ignored test").getSum()) + " tests.");
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

    private RecTest readLastTestFromFile(String filePath) throws IOException {
        if (this.testClass == null) {
            throw new NullPointerException("Field testClass has not been set!");
        }

        Scanner sc = new Scanner(new File(filePath));
        String lastLine = null;
        while (sc.hasNextLine()) {
            lastLine = sc.nextLine();
        }
        sc.close();
        if (lastLine == null) return null;
        return gson.fromJson(lastLine, (Type) this.testClass);
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

    private List<RecTest> generateTestsRecursively() {
        return (List<RecTest>) generator.generateAll();
    }

    public void generateTestsAndQuerySimultaneously(boolean verbose, boolean doPrintIncorrectPrediction) throws IOException {
        setupGenerator();
        List<String> fileList = new ArrayList<>();
        Scanner sc = new Scanner(new File("docs/testFilePath/" + projectName + ".txt"));
        while (sc.hasNextLine()) {
            String filePath = sc.nextLine();
            fileList.add(filePath);
        }
        sc.close();
//        Collections.reverse(fileList);

        RecTest lastTest = null;
        try {
            lastTest = readLastTestFromFile(Config.LOG_DIR + projectName + "_" + this.testClass.getSimpleName() + "s.txt");
        } catch (IOException ioe) {

        }
        boolean isGenerated = false;
        if (lastTest != null) isGenerated = true;
        loadTestResult();
        try {
            SocketClient socketClient = getSocketClient();
            for (int i = 0; i < fileList.size(); ++i) {
                String filePath = fileList.get(i);
                if (!isGenerated) {
                    List<RecTest> tests = (List<RecTest>) generateTestsFromFile(filePath, true);

                    for (RecTest test : tests) validateTest(test);

                    for (RecTest test : tests) {
                        dataFrame.insert("Tested", 1);
                        queryAndTest(socketClient, test, verbose, doPrintIncorrectPrediction);
                        saveTestResult();
                    }
                }
                if (lastTest != null && filePath.compareTo(lastTest.getFilePath()) == 0) isGenerated = false;
            }
            socketClient.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<? extends RecTest> generateTestsFromFile(String fileRelativePath, boolean doSaveTestsAfterGen) throws IOException {
        setupGenerator();
        List<RecTest> tests = (List<RecTest>) generator.generate(Config.REPO_DIR + "git/" + fileRelativePath);
        for (RecTest test : tests) test.setFilePath(fileRelativePath);
        if (doSaveTestsAfterGen) saveTests(tests);
        return tests;
    }

    public List<? extends RecTest> generateTestsFromFile(String fileRelativePath) throws IOException {
        return generateTestsFromFile(fileRelativePath, false);
    }

    void saveTests(List<RecTest> tests) {
        for (RecTest test : tests) {
            Logger.write(gson.toJson(this.testClass.cast(test)), projectName + "_" + this.testClass.getSimpleName() + "s.txt");
        }
    }

    public static void logTests(List<? extends RecTest> tests) {
        for (RecTest test : tests) {
            System.out.println(gson.toJson(test));
        }
    }

    private void loadTestResult() {
        try {
            FileInputStream fileInputStream = new FileInputStream(Config.LOG_DIR + "checkpoint/" + projectName + "_" + this.getClass().getSimpleName() + ".ser");
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            dataFrame = (DataFrame) objectInputStream.readObject();
            objectInputStream.close();
            fileInputStream.close();
        } catch (IOException ioe) {
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
    }

    private void saveTestResult() throws IOException {
        File outputFile = new File(Config.LOG_DIR + "checkpoint/" + projectName + "_" + this.getClass().getSimpleName() + ".ser");
        outputFile.getParentFile().mkdirs();
        FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
        objectOutputStream.writeObject(this.dataFrame);
        objectOutputStream.close();
        fileOutputStream.close();
    }

    public void validateTests(List<? extends RecTest> tests, boolean doPrintInadequateTests) {
        for (RecTest test : tests) validateTest(test, doPrintInadequateTests);

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

    public void validateTest(RecTest test, boolean doPrintInadequateTests) {
        if (test instanceof ArgRecTest && ((ArgRecTest) test).getArgPos() == 0 && !Config.TEST_ZERO_ARG) return;
        if (test instanceof MultipleArgRecTest && ((MultipleArgRecTest)test).getNumArg() == 0 && !Config.TEST_ZERO_ARG) return;
        if (!test.isIgnored()) {
            boolean adequateGeneratedExcode = false;
            boolean adequateGeneratedLex = false;
            if (RecTester.canAcceptGeneratedExcodes(test)) adequateGeneratedExcode = true;
            if (RecTester.canAcceptGeneratedLexes(test)) adequateGeneratedLex = true;
            dataFrame.insert("Adequate generated excodes", adequateGeneratedExcode);
            dataFrame.insert("Adequate generated lexicals", adequateGeneratedLex);
            dataFrame.insert("Adequate generated candidates", adequateGeneratedExcode && adequateGeneratedLex);
            if (adequateGeneratedLex) {
                testMap.put(test.getId(), true);
            } else if (doPrintInadequateTests) {
                Logger.write(gson.toJson(test), projectName + "_inadequate_" + this.testClass.getSimpleName() + "s.txt");
            }
        }
    }

    public void validateTest(RecTest test) {
        validateTest(test, false);
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void queryAndTest(RecTest test) {
        queryAndTest(test, false, false);
    }

    private void queryAndTest(SocketClient socketClient, RecTest test, boolean verbose, boolean doPrintIncorrectPrediction) throws IOException {
        if (doSkipTest(test)) return;
        dataFrame.insert("Predicted", 1);
        if (!test.isIgnored()) dataFrame.insert("Predicted supported", 1);

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
        isGPTUsed = predictResponse.getData().gpt != null;

        List<String> nGramResults = null;
        if (isNGramUsed) nGramResults = predictResponse.getData().ngram.getResult();
        List<String> RNNResults = null;
        if (isRNNUsed) RNNResults = predictResponse.getData().rnn.getResult();
        List<String> gptResults = null;
        if (isGPTUsed) gptResults = predictResponse.getData().gpt.getResult();

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

            if (isGPTUsed) {
                System.out.println("==========================");
                System.out.println("GPT's results:");
                gptResults.forEach(item -> {
                    System.out.println(item);
                });
                System.out.println("==========================");
                System.out.println("GPT's runtime: " + predictResponse.getData().gpt.getRuntime() + "s");
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

        if (isGPTUsed) {
            for (int k : this.tops)
                updateTopKResult(test, gptResults, k, testMap.getOrDefault(test.getId(), false),
                        "GPT", doPrintIncorrectPrediction);
        }

        if (isNGramUsed) dataFrame.insert("NGram's runtime", predictResponse.getData().ngram.getRuntime());
        if (isRNNUsed) dataFrame.insert("RNN's runtime", predictResponse.getData().rnn.getRuntime());
        if (isGPTUsed) dataFrame.insert("GPT's runtime", predictResponse.getData().gpt.getRuntime());
    }

    abstract void updateTopKResult(RecTest test, List<String> results, int k, boolean adequateGeneratedCandidate,
                                   String modelName, boolean doPrintIncorrectPrediction);

    public void printTestResult() {
        System.out.println("==========================");
        System.out.println("Ran " + dataFrame.getVariable("Tested").getCount() + " tests successfully.");
        System.out.println("Predicted " + dataFrame.getVariable("Predicted").getCount() + " tests.");
        System.out.println("Predicted " + dataFrame.getVariable("Predicted supported").getCount() + " tests that were supported.");
        System.out.println("Skipped " + (dataFrame.getVariable("Tested").getCount() - dataFrame.getVariable("Predicted").getCount())
                + " tests. They were not taken into account during evaluation.");
        System.out.println("Average parsing runtime: " + dataFrame.getVariable("averageGetTestsTime").getSum() + "s");
        if (isNGramUsed)
            System.out.println("Average NGram's runtime: " + dataFrame.getVariable("NGram's runtime").getMean() + "s");
        if (isRNNUsed)
            System.out.println("Average RNN's runtime: " + dataFrame.getVariable("RNN's runtime").getMean() + "s");
        if (isGPTUsed)
            System.out.println("Average GPT's runtime: " + dataFrame.getVariable("GPT's runtime").getMean() + "s");
        System.out.println("Average overall runtime: "
                + (dataFrame.getVariable("NGram's runtime").getMean()
                + dataFrame.getVariable("RNN's runtime").getMean()
                + dataFrame.getVariable("GPT's runtime").getMean()
                + dataFrame.getVariable("averageGetTestsTime").getSum()) + "s");
    }

    String getBestModel(List<String> modelNames, String category) {
        String bestModel = modelNames.get(0);
        for (int k : this.tops) {
            double bestAcc = dataFrame.getVariable(String.format(category, bestModel, k)).getMean();
            double temp = bestAcc;
            for (String modelName : modelNames) {
                double curAcc = dataFrame.getVariable(String.format(category, modelName, k)).getMean();
                if (Math.abs(bestAcc - curAcc) > 1e-7) {
                    if (bestAcc < curAcc) {
                        bestModel = modelName;
                        bestAcc = curAcc;
                    }
                }
            }
            if (bestAcc > temp) {
                System.out.println(String.format("%s has the best accuracy at top-%d.", bestModel, k));
                break;
            }
        }
        System.out.println(String.format("%s is chosen.", bestModel));
        return bestModel;
    }


    public void generateTests(String fold, String setting) throws IOException {
        Timer timer = new Timer();
        timer.startCounter();
        setupGenerator();
        Scanner sc = new Scanner(new File("docs/testFilePath/" + "datapath/fold" + fold + "/" + projectName + ".txt"));
        String testOutputPath = projectName + "_" + this.testClass.getSimpleName() + "s_fold" + fold + "_" + setting + ".txt";
        String badTestOutputPath = projectName + "_" + this.testClass.getSimpleName() + "s_fold" + fold + "_" + setting + "_bad.txt";
        File file = new File(Config.LOG_DIR + testOutputPath);
        file.delete();
        file = new File(Config.LOG_DIR + badTestOutputPath);
        file.delete();
//        final ExecutorService executor = Executors.newFixedThreadPool(Config.NUM_THREAD); // it's just an arbitrary number
//        final List<Future<?>> futures = new ArrayList<>();
        MultipleArgRecTestGenerator multipleGenerator = Config.TEST_ARG_ONE_BY_ONE?
                new SingleArgRecTestGenerator((ArgRecTestGenerator) generator): new AllArgRecTestGenerator((ArgRecTestGenerator) generator);
        List<ArgRecTest> tests = new ArrayList<>();
        while (sc.hasNextLine()) {
            String filePath = sc.nextLine();
//            Future<?> future = executor.submit(() -> {
            createNewGenerator();
            List<? extends RecTest> oneFileTests = generator.generate(Config.REPO_DIR + "git/" + filePath);
            for (RecTest test : oneFileTests) test.setFilePath(filePath);
            for (RecTest test : oneFileTests) {
                Logger.write(gson.toJson(this.testClass.cast(test)), testOutputPath);
            }
            tests.addAll((List<ArgRecTest>) oneFileTests);
//            for (RecTest test: oneFileTests)
//                if (!test.isIgnored()) {
//                    boolean adequateGeneratedExcode = false;
//                    boolean adequateGeneratedLex = false;
//                    if (RecTester.canAcceptGeneratedExcodes(test)) adequateGeneratedExcode = true;
//                    if (RecTester.canAcceptGeneratedLexes(test)) adequateGeneratedLex = true;
//                    if (adequateGeneratedExcode && adequateGeneratedLex) {
//                    } else {
//                        Logger.write(gson.toJson(this.testClass.cast(test)), badTestOutputPath);
//                    }
//                }
//            });
//            futures.add(future);
//            List<RecTest> oneFileTests = (List<RecTest>) generator.generate(Config.REPO_DIR + "git/" + filePath);
//            for (RecTest test : oneFileTests) test.setFilePath(filePath);
//            for (RecTest test: oneFileTests) {
//                Logger.write(gson.toJson(this.testClass.cast(test)), projectName + "_" + this.testClass.getSimpleName() + "s_fold" + fold + "_off.txt");
//            }
        }
        double averageGetTestsTime = timer.getTimeCounter() / 1000f / tests.size();
        dataFrame.insert("averageGetTestsTime", averageGetTestsTime);

        for (RecTest test : tests) dataFrame.insert("Ignored test", test.isIgnored());

        System.out.println("Generated " + dataFrame.getVariable("Ignored test").getCount() + " tests.");
        System.out.println("Supported " + (dataFrame.getVariable("Ignored test").getCount() - dataFrame.getVariable("Ignored test").getSum()) + " tests.");
        System.out.println("Ignored " + dataFrame.getVariable("Ignored test").getSum() + " tests.");
        List<MultipleArgRecTest> multiArgRecTests = multipleGenerator.generate(tests);
        this.validateTests(multiArgRecTests, true);
        //RecClient.logTests(tests);
        this.queryAndTest(multiArgRecTests, false, false);
        this.printTestResult();
        System.exit(0);
        sc.close();

//        if (Config.MULTIPROCESS) {
//            boolean isDone = false;
//            while (!isDone) {
//                boolean isProcessing = false;
//                for (Future<?> future : futures) {
//                    if (!future.isDone()) {
//                        isProcessing = true;
//                        break;
//                    }
//                }
//                if (!isProcessing) isDone = true;
//            }
//        }

        sc.close();
    }
}
