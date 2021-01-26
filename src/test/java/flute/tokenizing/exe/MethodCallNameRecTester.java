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
import flute.tokenizing.excode_data.MethodCallNameRecTest;
import flute.tokenizing.excode_data.MultipleArgRecTest;
import flute.utils.ProgressBar;
import flute.utils.file_writing.CSVWritor;
import flute.utils.logging.Logger;
import flute.utils.logging.Timer;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MethodCallNameRecTester {
    private static final Gson gson = new Gson();
    private static MethodCallNameRecTestGenerator generator;
    private static DataFrame dataFrame = new DataFrame();

    public static boolean canAcceptGeneratedExcodes(MethodCallNameRecTest test) {
        String expectedExcode = test.getExpected_excode();
        if (test.getMethod_candidate_excode().contains(expectedExcode)) return true;

        //TODO: Handle unknown excode
        if (expectedExcode.contains("<unk>")) {
            String expectedNumParam = test.getExpected_excode().split(",")[2];
            for (String methodExcode: test.getMethod_candidate_excode()) {
                if (!methodExcode.split(",")[1].equals(test.getExpected_lex())) continue;
                if (!methodExcode.split(",")[2].equals(expectedNumParam)) continue;
                return true;
            }
        }

        return false;
    }

    public static boolean canAcceptGeneratedLexes(MethodCallNameRecTest test) {
        String expectedLex = test.getExpected_lex();
        if (test.getMethod_candidate_lexList().contains(expectedLex)) return true;

        return false;
    }

    public static boolean canAcceptResult(MethodCallNameRecTest test, String result) {
        String expectedExcode = test.getExpected_excode();
        if (result.equals(expectedExcode)) return true;

        //TODO: Handle unknown excode
        if (expectedExcode.contains("<unk>")) {
            String expectedNumParam = test.getExpected_excode().split(",")[2];
            if (!result.split(",")[1].equals(test.getExpected_lex())) return false;
            if (!result.split(",")[2].equals(expectedNumParam)) return false;
            return true;
        }

        return false;
    }

    public static void updateTopKResult(MethodCallNameRecTest test, List<String> results, int k, boolean adequateGeneratedCandidate,
                                        String modelName) {

        if (test.isIgnored()) {
            dataFrame.insert(String.format("%sActualTop%d", modelName, k), 0);
            return;
        }

        boolean isOverallCorrectTopK = false;
        for (int i = 0; i < Math.min(k, results.size()); ++i) {
            if (canAcceptResult(test, results.get(i))) {
                isOverallCorrectTopK = true;
                break;
            }
        }

        if (isOverallCorrectTopK) {
            dataFrame.insert(String.format("%sOverallTop%d", modelName, k), 1);

            dataFrame.insert(String.format("%sActualTop%d", modelName, k), 1);
            if (adequateGeneratedCandidate) {
                dataFrame.insert(String.format("%sTop%d", modelName, k), 1);
            }
        } else {
            dataFrame.insert(String.format("%sOverallTop%d", modelName, k), 0);

            dataFrame.insert(String.format("%sActualTop%d", modelName, k), 0);
            if (adequateGeneratedCandidate) {
                dataFrame.insert(String.format("%sTop%d", modelName, k), 0);
            }
        }
    }

    private static final int[] tops = {1, 3, 5, 10};
    private static boolean isNGramUsed = false;
    private static boolean isRNNUsed = false;

    public static void printResult(String projectName, double averageGetTestsTime) {
        System.out.println("==========================");
        System.out.println("Number of tests: " + dataFrame.getVariable("Tested").getCount());
        System.out.println("Average parsing runtime: " + averageGetTestsTime + "s");
        if (isNGramUsed) System.out.println("Average NGram's runtime: " + dataFrame.getVariable("NGram's runtime").getMean() + "s");
        if (isRNNUsed) System.out.println("Average RNN's runtime: " + dataFrame.getVariable("RNN's runtime").getMean() + "s");
        System.out.println("Average overall runtime: "
                + (dataFrame.getVariable("NGram's runtime").getMean()
                + dataFrame.getVariable("RNN's runtime").getMean()
                + averageGetTestsTime) + "s");

        List<String[]> accuracy = new ArrayList<>();
        List<String> row = new ArrayList<>();
        if (isNGramUsed) {
            for (int k: tops) row.add(String.format("NGram's top-%d accuracy", k));
        }
        if (isRNNUsed) {
            for (int k: tops) row.add(String.format("RNN's top-%d accuracy", k));
        }
        for (int k: tops) row.add(String.format("Top-%d precision", k));
        for (int k: tops) row.add(String.format("Top-%d recall", k));
        accuracy.add(row.toArray(new String[row.size()]));

        row = new ArrayList<>();
        if (isNGramUsed) {
            for (int k: tops) row.add(String.format("%f", dataFrame.getVariable(String.format("nGramTop%d", k)).getMean()));
        }
        if (isRNNUsed) {
            for (int k: tops) row.add(String.format("%f", dataFrame.getVariable(String.format("RNNTop%d", k)).getMean()));
        }
        for (int k: tops) row.add(String.format("%f", dataFrame.getVariable(String.format("nGramOverallTop%d", k)).getMean()));
        for (int k: tops) row.add(String.format("%f", dataFrame.getVariable(String.format("nGramActualTop%d", k)).getMean()));
        accuracy.add(row.toArray(new String[row.size()]));

        CSVWritor.write(Config.LOG_DIR + projectName + "_acc.csv", accuracy);
    }

    public static void test(Response response, Map<Integer, Boolean> testMap, MethodCallNameRecTest test) {
        PredictResponse predictResponse = (PredictResponse) response;
        isNGramUsed = predictResponse.getData().ngram != null;
        isRNNUsed = predictResponse.getData().rnn != null;
        List<String> nGramResults = null;
        if (isNGramUsed) nGramResults = predictResponse.getData().ngram.getResult();
        List<String> RNNResults = null;
        if (isRNNUsed) RNNResults = predictResponse.getData().rnn.getResult();
//        System.out.println("==========================");
//        System.out.println(gson.toJson(test));
//        if (isNGramUsed) {
//            System.out.println("==========================");
//            System.out.println("NGram's results:");
//            nGramResults.forEach(item -> {
//                System.out.println(item);
//            });
//            System.out.println("==========================");
//            System.out.println("NGram's runtime: " + predictResponse.getData().ngram.getRuntime() + "s");
//        }
//
//        if (isRNNUsed) {
//            System.out.println("==========================");
//            System.out.println("RNN's results:");
//            RNNResults.forEach(item -> {
//                System.out.println(item);
//            });
//            System.out.println("==========================");
//            System.out.println("RNN's runtime: " + predictResponse.getData().rnn.getRuntime() + "s");
//        }

        if (isNGramUsed) {
            for (int k : tops)
                updateTopKResult(test, nGramResults, k, testMap.getOrDefault(test.getId(), false),
                        "nGram");
        }

        if (isRNNUsed) {
            for (int k : tops)
                updateTopKResult(test, RNNResults, k, testMap.getOrDefault(test.getId(), false),
                        "RNN");
        }

        if (isNGramUsed) dataFrame.insert("NGram's runtime", predictResponse.getData().ngram.getRuntime());
        if (isRNNUsed) dataFrame.insert("RNN's runtime", predictResponse.getData().rnn.getRuntime());
    }

    public static void main(String[] args) throws IOException {
        String projectName = "lucene";
        Timer timer = new Timer();
        timer.startCounter();
        List<MethodCallNameRecTest> tests = getTests(projectName, false, true);
        //List<MethodCallNameRecTest> tests = generateTestsFromFile("demo", Config.REPO_DIR + "sampleproj/src/Main.java");
        double averageGetTestsTime = timer.getTimeCounter() / 1000f / tests.size();

        //logTests(tests);

        for (MethodCallNameRecTest test: tests) dataFrame.insert("Ignored test", test.isIgnored());

        System.out.println("Generated " + dataFrame.getVariable("Ignored test").getCount() + " tests.");

        for (MethodCallNameRecTest test: tests)
            if (!test.isIgnored()) {
                dataFrame.insert("Generated excode count", test.getMethod_candidate_excode().size());
                dataFrame.insert("Generated lexical count", test.getMethod_candidate_lex().size());
            }
        System.out.println("Number of generated excode candidates: " +
                dataFrame.getVariable("Generated excode count").getSum());

        System.out.println("Number of generated lexical candidates: " +
                dataFrame.getVariable("Generated lexical count").getSum());

        Map<Integer, Boolean> testMap = new HashMap<>();
        for (MethodCallNameRecTest test: tests)
            if (!test.isIgnored()) {
                boolean adequateGeneratedExcode = false;
                boolean adequateGeneratedLex = false;
                if (canAcceptGeneratedExcodes(test)) adequateGeneratedExcode = true;
                if (canAcceptGeneratedLexes(test)) adequateGeneratedLex = true;
                dataFrame.insert("Adequate generated excodes", adequateGeneratedExcode);
                dataFrame.insert("Adequate generated lexicals", adequateGeneratedLex);
                dataFrame.insert("Adequate generated candidates", adequateGeneratedExcode && adequateGeneratedLex);
                if (adequateGeneratedExcode && adequateGeneratedLex) {
                    testMap.put(test.getId(), true);
                } else {
                    //Logger.write(gson.toJson(test), projectName + "_inadequate_generated_method_call_name_tests.txt");
                }
            }
        System.out.printf("Adequate generated excodes: %.2f%%%n",
                dataFrame.getVariable("Adequate generated excodes").getMean() * 100);

        System.out.printf("Adequate generated lexicals: %.2f%%%n",
                dataFrame.getVariable("Adequate generated lexicals").getMean() * 100);

        System.out.printf("Adequate generated candidates: %.2f%%%n",
                dataFrame.getVariable("Adequate generated candidates").getMean() * 100);

        List<List<MethodCallNameRecTest>> testBatches = null;

        if (Config.MULTIPROCESS) {
            int batchSize = IntMath.divide(tests.size(), Config.NUM_THREAD, RoundingMode.UP);
            testBatches = Lists.partition(tests, batchSize);
        }

        ProgressBar testProgressBar = new ProgressBar();

        if (Config.MULTIPROCESS) {
            final ExecutorService executor = Executors.newFixedThreadPool(Config.NUM_THREAD); // it's just an arbitrary number
            final List<Future<?>> futures = new ArrayList<>();

            List<List<MethodCallNameRecTest>> finalTestBatches = testBatches;
            for (List<MethodCallNameRecTest> testBatch : finalTestBatches) {
                Future<?> future = executor.submit(() -> {
                    try {
                        SocketClient socketClient = new SocketClient(Config.METHOD_NAME_SERVICE_PORT);
                        for (MethodCallNameRecTest test : testBatch) {
                            dataFrame.insert("Tested", 1);
                            testProgressBar.setProgress(dataFrame.getVariable("Tested").getCount() * 1f / tests.size(), true);

                            Response response = socketClient.write(gson.toJson(test));
                            if (response instanceof PredictResponse) {
                                test(response, testMap, test);
                            }
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
                SocketClient socketClient = new SocketClient(Config.METHOD_NAME_SERVICE_PORT);
                for (MethodCallNameRecTest test : tests) {
                    dataFrame.insert("Tested", 1);
                    testProgressBar.setProgress(dataFrame.getVariable("Tested").getCount() * 1f / tests.size(), true);

                    Response response = socketClient.write(gson.toJson(test));
                    if (response instanceof PredictResponse) {
                        test(response, testMap, test);
                    }
                }
                socketClient.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        printResult(projectName, averageGetTestsTime);
        System.exit(0);
    }

    public static void setupGenerator(String projectName) throws IOException {
        Config.loadConfig(Config.STORAGE_DIR + "/json/" + projectName + ".json");
        ProjectParser projectParser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH,
                Config.ENCODE_SOURCE, Config.CLASS_PATH, Config.JDT_LEVEL, Config.JAVA_VERSION);
        generator = new MethodCallNameRecTestGenerator(Config.PROJECT_DIR, projectParser);
    }

    public static List<MethodCallNameRecTest> getTests(String projectName, boolean fromSavefile, boolean doSaveTestsAfterGen) throws IOException {
        List<MethodCallNameRecTest> tests;
        if (fromSavefile) {
            tests = readTestsFromFile(Config.LOG_DIR + projectName + "_tests.txt");
        } else {
            setupGenerator(projectName);

            if (projectName.equals("demo")) {
                tests = generateTestsFromDemoProject();
            } else {
                tests = generateTestsFromGitProject(projectName);
            }

            if (doSaveTestsAfterGen) saveTests(projectName, tests);
        }

        if (Config.TEST_APIS != null && Config.TEST_APIS.length > 0) {
            List<MethodCallNameRecTest> tmp = new ArrayList<>();
            for (MethodCallNameRecTest test: tests) {
                if (test.getMethodInvocClassQualifiedName() != null) {
                    for (String targetAPI: Config.TEST_APIS) {{
                        if (test.getMethodInvocClassQualifiedName().startsWith(targetAPI + '.')) {
                            tmp.add(test);
                            break;
                        }
                    }}
                }
            }
            tests = tmp;
        }

        return tests;
    }

    public static List<MethodCallNameRecTest> getTests(String projectName, boolean fromSavefile) throws IOException {
        return getTests(projectName, fromSavefile, false);
    }

    public static List<MethodCallNameRecTest> readTestsFromFile(String filePath) throws IOException {
        Scanner sc = new Scanner(new File(filePath));
        List<MethodCallNameRecTest> tests = new ArrayList<>();
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            tests.add(gson.fromJson(line, MethodCallNameRecTest.class));
        }
        sc.close();
        return tests;
    }

    public static List<MethodCallNameRecTest> generateTestsFromDemoProject() {
        return (List<MethodCallNameRecTest>) generator.generateAll();
    }

    public static List<MethodCallNameRecTest> generateTestsFromGitProject(String projectName) throws IOException {
        List<MethodCallNameRecTest> tests = new ArrayList<>();
        Scanner sc = new Scanner(new File("docs/testFilePath/" + projectName + ".txt"));

        final ExecutorService executor = Executors.newFixedThreadPool(Config.NUM_THREAD); // it's just an arbitrary number
        final List<Future<?>> futures = new ArrayList<>();

        while (sc.hasNextLine()) {
            String filePath = sc.nextLine();
            if (Config.MULTIPROCESS) {
                Future<?> future = executor.submit(() -> {
                    List<MethodCallNameRecTest> oneFileTests = (List<MethodCallNameRecTest>) generator.generate(Config.REPO_DIR + "git/" + filePath);
                    for (MethodCallNameRecTest test : oneFileTests) test.setFilePath(filePath);
                    tests.addAll(oneFileTests);
                });
                futures.add(future);
            } else {
                List<MethodCallNameRecTest> oneFileTests = (List<MethodCallNameRecTest>) generator.generate(Config.REPO_DIR + "git/" + filePath);
                for (MethodCallNameRecTest test : oneFileTests) test.setFilePath(filePath);
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

    public static List<MethodCallNameRecTest> generateTestsFromFile(String projectName, String filePath) throws IOException {
        setupGenerator(projectName);
        return (List<MethodCallNameRecTest>) generator.generate(filePath);
    }

    public static void saveTests(String projectName, List<MethodCallNameRecTest> tests) {
        for (MethodCallNameRecTest test: tests) {
            Logger.write(gson.toJson(test), projectName + "_tests.txt");
        }
    }

    public static void logTests(List<MultipleArgRecTest> tests) {
        for (MultipleArgRecTest test: tests) {
            System.out.println(gson.toJson(test));
        }
    }
}