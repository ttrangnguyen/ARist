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
import flute.tokenizing.excode_data.MultipleArgRecTest;
import flute.tokenizing.excode_data.ArgRecTest;
import flute.tokenizing.excode_data.RecTest;
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

public class ArgRecTester {
    private static final int CONTEXT_LENGTH_LIMIT = -1;

    private static final Gson gson = new Gson();
    private static ArgRecTestGenerator generator;
    private static DataFrame dataFrame = new DataFrame();;

    public static boolean canAcceptGeneratedExcodes(ArgRecTest test) {
        String expectedExcode = test.getExpected_excode();
        if (test.getNext_excode().contains(expectedExcode)) return true;

        //TODO: Handle unknown excode
        if (expectedExcode.contains("<unk>")) return true;

        if (test.getMethodAccessExcode() != null) {
            if (test.getNext_excode().contains(test.getMethodAccessExcode())) return true;
        }

        if (test.getObjectCreationExcode() != null) {
            return test.getNext_excode().contains(test.getObjectCreationExcode());
        }

        return false;
    }

    public static boolean canAcceptGeneratedExcodes(MultipleArgRecTest test) {
        for (ArgRecTest oneArgTest: test.getArgRecTestList())
            if (!canAcceptGeneratedExcodes(oneArgTest)) return false;
        return true;
    }

    public static boolean canAcceptGeneratedLexes(ArgRecTest test) {
        String expectedLex = test.getExpected_lex();
        if (expectedLex.contains(".this")) {
            expectedLex = expectedLex.substring(expectedLex.indexOf("this"));
        }

        if (test.getNext_lexList().contains(expectedLex)) return true;
        if (expectedLex.startsWith("this.")) {
            if (test.getNext_lexList().contains(expectedLex.substring(5))) return true;
        } else {
            if (test.getNext_lexList().contains("this." + expectedLex)) return true;
        }

        if (test.getMethodAccessLex() != null) {
            if (test.getNext_lexList().contains(test.getMethodAccessLex())) return true;
        }

        if (test.getObjectCreationLex() != null) {
            return test.getNext_lexList().contains(test.getObjectCreationLex());
        }

        return false;
    }

    public static boolean canAcceptGeneratedLexes(MultipleArgRecTest test) {
        for (ArgRecTest oneArgTest: test.getArgRecTestList())
            if (!canAcceptGeneratedLexes(oneArgTest)) return false;
        return true;
    }

    public static boolean canAcceptResult(ArgRecTest test, String result) {
        String expectedLex = test.getExpected_lex();
        if (expectedLex.contains(".this")) {
            expectedLex = expectedLex.substring(expectedLex.indexOf("this"));
        }

        if (result.equals(expectedLex)) return true;
        if (expectedLex.startsWith("this.")) {
            if (result.equals(expectedLex.substring(5))) return true;
        } else {
            if (result.equals("this." + expectedLex)) return true;
        }

        if (test.getMethodAccessLex() != null) {
            if (result.equals(test.getMethodAccessLex())) return true;
        }

        if (test.getObjectCreationLex() != null) {
            return result.equals(test.getObjectCreationLex());
        }

        return false;
    }

    public static boolean canAcceptResult(MultipleArgRecTest test, String result) {
        int i = -1;
        for (ArgRecTest oneArgTest: test.getArgRecTestList()) {
            StringBuilder sb = new StringBuilder();
            int bal = 0;
            while (++i < result.length()) {
                char c = result.charAt(i);
                if (c == '(') ++bal;
                if (c == ')') --bal;
                if (bal == 0 && c == ' ') continue;
                if (bal == 0 && c == ',') break;
                sb.append(c);
            }
            if (!canAcceptResult(oneArgTest, sb.toString())) return false;
        }
        return true;
    }

    public static void updateTopKResult(MultipleArgRecTest test, List<String> results, int k, boolean adequateGeneratedCandidate,
                                        String modelName) {

        if (test.isIgnored()) {
            dataFrame.insert(String.format("%sActualTop%d", modelName, k), 0);
            dataFrame.insert(String.format("%sActualTop%dArg%d", modelName, k, test.getNumArg()), 0);
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
            dataFrame.insert(String.format("%sOverallTop%dArg%d", modelName, k, test.getNumArg()), 1);

            dataFrame.insert(String.format("%sActualTop%d", modelName, k), 1);
            dataFrame.insert(String.format("%sActualTop%dArg%d", modelName, k, test.getNumArg()), 1);
            if (adequateGeneratedCandidate) {
                dataFrame.insert(String.format("%sTop%d", modelName, k), 1);
                dataFrame.insert(String.format("%sTop%dArg%d", modelName, k, test.getNumArg()), 1);
            }
        } else {
            dataFrame.insert(String.format("%sOverallTop%d", modelName, k), 0);
            dataFrame.insert(String.format("%sOverallTop%dArg%d", modelName, k, test.getNumArg()), 0);

            dataFrame.insert(String.format("%sActualTop%d", modelName, k), 0);
            dataFrame.insert(String.format("%sActualTop%dArg%d", modelName, k, test.getNumArg()), 0);
            if (adequateGeneratedCandidate) {
                dataFrame.insert(String.format("%sTop%d", modelName, k), 0);
                dataFrame.insert(String.format("%sTop%dArg%d", modelName, k, test.getNumArg()), 0);
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

        List<String[]> accuracyPerNumArg = new ArrayList<>();
        List<String> row = new ArrayList<>();
        row.add("Number of params");
        row.add("Percentage distribution");
        if (isNGramUsed) {
            for (int k: tops) row.add(String.format("NGram's top-%d accuracy", k));
        }
        if (isRNNUsed) {
            for (int k: tops) row.add(String.format("RNN's top-%d accuracy", k));
        }
        for (int k: tops) row.add(String.format("Top-%d precision", k));
        for (int k: tops) row.add(String.format("Top-%d recall", k));
        accuracyPerNumArg.add(row.toArray(new String[row.size()]));

        if (!Config.TEST_ARG_ONE_BY_ONE) {
            DataFrame.Variable numArgVar = dataFrame.getVariable("NumArg");
            for (int i = (int)numArgVar.getMin(); i <= numArgVar.getMax(); ++i) {
                row = new ArrayList<>();
                row.add(String.format("%d", i));
                row.add(String.format("%f", dataFrame.getVariable("NumArg").getProportionOfValue(i, true)));
                if (isNGramUsed) {
                    for (int k: tops) row.add(String.format("%f", dataFrame.getVariable(String.format("nGramTop%dArg%d", k, i)).getMean()));
                }
                if (isRNNUsed) {
                    for (int k: tops) row.add(String.format("%f", dataFrame.getVariable(String.format("RNNTop%dArg%d", k, i)).getMean()));
                }
                for (int k: tops) row.add(String.format("%f", dataFrame.getVariable(String.format("nGramOverallTop%dArg%d", k, i)).getMean()));
                for (int k: tops) row.add(String.format("%f", dataFrame.getVariable(String.format("nGramActualTop%dArg%d", k, i)).getMean()));
                accuracyPerNumArg.add(row.toArray(new String[row.size()]));
            }
        }

        row = new ArrayList<>();
        row.add("all");
        row.add("100");
        if (isNGramUsed) {
            for (int k: tops) row.add(String.format("%f", dataFrame.getVariable(String.format("nGramTop%d", k)).getMean()));
        }
        if (isRNNUsed) {
            for (int k: tops) row.add(String.format("%f", dataFrame.getVariable(String.format("RNNTop%d", k)).getMean()));
        }
        for (int k: tops) row.add(String.format("%f", dataFrame.getVariable(String.format("nGramOverallTop%d", k)).getMean()));
        for (int k: tops) row.add(String.format("%f", dataFrame.getVariable(String.format("nGramActualTop%d", k)).getMean()));
        accuracyPerNumArg.add(row.toArray(new String[row.size()]));

        CSVWritor.write(Config.LOG_DIR + projectName + "_acc_per_num_arg.csv", accuracyPerNumArg);
    }

    public static void test(Response response, Map<Integer, Boolean> testMap, MultipleArgRecTest test) {
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

        dataFrame.insert("NumArg", test.getNumArg());
    }

    public static void main(String[] args) throws IOException {
        String projectName = "lucene";
        Timer timer = new Timer();
        timer.startCounter();
        List<MultipleArgRecTest> tests = getTests(projectName, false, true);
        //List<AllArgRecTest> tests = generateTestsFromFile("demo", Config.REPO_DIR + "sampleproj/src/Main.java");
        double averageGetTestsTime = timer.getTimeCounter() / 1000f / tests.size();

        //logTests(tests);

        for (MultipleArgRecTest test: tests) dataFrame.insert("Ignored test", test.isIgnored());

        System.out.println("Generated " + dataFrame.getVariable("Ignored test").getCount() + " tests.");
        System.out.println("Ignored " + dataFrame.getVariable("Ignored test").getSum() + " tests.");

        for (MultipleArgRecTest test: tests)
            if (!test.isIgnored()) {
                dataFrame.insert("Generated excode count", test.getNext_excodeList().size());
                dataFrame.insert("Generated lexical count", test.getNext_lexList().size());
            }
        System.out.println("Number of generated excode candidates: " +
                dataFrame.getVariable("Generated excode count").getSum());

        System.out.println("Number of generated lexical candidates: " +
                dataFrame.getVariable("Generated lexical count").getSum());

        Map<Integer, Boolean> testMap = new HashMap<>();
        for (MultipleArgRecTest test: tests)
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
                    //Logger.write(gson.toJson(test), projectName + "_inadequate_generated_arg_tests.txt");
                }
            }
        System.out.printf("Adequate generated excodes: %.2f%%%n",
                dataFrame.getVariable("Adequate generated excodes").getMean() * 100);

        System.out.printf("Adequate generated lexicals: %.2f%%%n",
                dataFrame.getVariable("Adequate generated lexicals").getMean() * 100);

        System.out.printf("Adequate generated candidates: %.2f%%%n",
                dataFrame.getVariable("Adequate generated candidates").getMean() * 100);

        List<List<MultipleArgRecTest>> testBatches = null;

        if (Config.MULTIPROCESS) {
            int batchSize = IntMath.divide(tests.size(), Config.NUM_THREAD, RoundingMode.UP);
            testBatches = Lists.partition(tests, batchSize);
        }

        ProgressBar testProgressBar = new ProgressBar();

        if (Config.MULTIPROCESS) {
            final ExecutorService executor = Executors.newFixedThreadPool(Config.NUM_THREAD); // it's just an arbitrary number
            final List<Future<?>> futures = new ArrayList<>();

            List<List<MultipleArgRecTest>> finalTestBatches = testBatches;
            for (List<MultipleArgRecTest> testBatch : finalTestBatches) {
                Future<?> future = executor.submit(() -> {
                    try {
                        SocketClient socketClient = new SocketClient(Config.SOCKET_PORT);
                        for (MultipleArgRecTest test : testBatch) {
                            dataFrame.insert("Tested", 1);
                            testProgressBar.setProgress(dataFrame.getVariable("Tested").getCount() * 1f / tests.size(), true);
                            if (test.getNumArg() == 0 && !Config.TEST_ZERO_ARG) continue;

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
                SocketClient socketClient = new SocketClient(Config.SOCKET_PORT);
                for (MultipleArgRecTest test : tests) {
                    dataFrame.insert("Tested", 1);
                    testProgressBar.setProgress(dataFrame.getVariable("Tested").getCount() * 1f / tests.size(), true);
                    if (test.getNumArg() == 0 && !Config.TEST_ZERO_ARG) continue;

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
        generator = new ArgRecTestGenerator(Config.PROJECT_DIR, projectParser);
        generator.setLengthLimit(CONTEXT_LENGTH_LIMIT);
    }

    public static List<MultipleArgRecTest> getTests(String projectName, boolean fromSavefile, boolean doSaveTestsAfterGen) throws IOException {
        List<ArgRecTest> tests;
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
            List<ArgRecTest> tmp = new ArrayList<>();
            for (ArgRecTest test: tests) {
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

        return Config.TEST_ARG_ONE_BY_ONE? generator.getSingleArgRecTests(tests) : generator.getAllArgRecTests(tests);
    }

    public static List<MultipleArgRecTest> getTests(String projectName, boolean fromSavefile) throws IOException {
        return getTests(projectName, fromSavefile, false);
    }

    public static List<ArgRecTest> readTestsFromFile(String filePath) throws IOException {
        Scanner sc = new Scanner(new File(filePath));
        List<ArgRecTest> tests = new ArrayList<>();
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            tests.add(gson.fromJson(line, ArgRecTest.class));
        }
        sc.close();
        return tests;
    }

    public static List<ArgRecTest> generateTestsFromDemoProject() {
        return (List<ArgRecTest>) generator.generateAll();
    }

    public static List<ArgRecTest> generateTestsFromGitProject(String projectName) throws IOException {
        List<ArgRecTest> tests = new ArrayList<>();
        Scanner sc = new Scanner(new File("docs/testFilePath/" + projectName + ".txt"));

        final ExecutorService executor = Executors.newFixedThreadPool(Config.NUM_THREAD); // it's just an arbitrary number
        final List<Future<?>> futures = new ArrayList<>();

        while (sc.hasNextLine()) {
            String filePath = sc.nextLine();
            if (Config.MULTIPROCESS) {
                Future<?> future = executor.submit(() -> {
                    List<ArgRecTest> oneFileTests = (List<ArgRecTest>) generator.generate(Config.REPO_DIR + "git/" + filePath);
                    for (ArgRecTest test : oneFileTests) test.setFilePath(filePath);
                    tests.addAll(oneFileTests);
                });
                futures.add(future);
            } else {
                List<ArgRecTest> oneFileTests = (List<ArgRecTest>) generator.generate(Config.REPO_DIR + "git/" + filePath);
                for (ArgRecTest test : oneFileTests) test.setFilePath(filePath);
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

    public static List<ArgRecTest> generateTestsFromFile(String projectName, String filePath) throws IOException {
        setupGenerator(projectName);
        return (List<ArgRecTest>) generator.generate(filePath);
    }

    public static void saveTests(String projectName, List<ArgRecTest> tests) {
        for (ArgRecTest test: tests) {
            Logger.write(gson.toJson(test), projectName + "_tests.txt");
        }
    }

    public static void logTests(List<MultipleArgRecTest> tests) {
        for (MultipleArgRecTest test: tests) {
            System.out.println(gson.toJson(test));
        }
    }
}