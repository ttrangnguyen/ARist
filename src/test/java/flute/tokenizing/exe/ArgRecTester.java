package flute.tokenizing.exe;

import com.google.gson.Gson;
import flute.communicate.SocketClient;
import flute.communicate.schema.PredictResponse;
import flute.communicate.schema.Response;
import flute.config.Config;
import flute.jdtparser.ProjectParser;
import flute.tokenizing.excode_data.ArgRecTest;
import flute.utils.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ArgRecTester {
    private static int CONTEXT_LENGTH_LIMIT = 20;

    public static ArgRecTestGenerator generator;
    public static Gson gson = new Gson();

    public static void main(String[] args) throws IOException {
        String projectName = "ant";
        List<ArgRecTest> tests = getTests(projectName, false, false);
        //List<ArgRecTest> tests = generateTestsFromFile("demo", Config.REPO_DIR + "sampleproj/src/Main.java");

        //logTests(tests);

        System.out.println("Generated " + tests.size() + " tests.");

        int adequateGeneratedExcodeCount = 0;
        int adequateGeneratedLexCount = 0;
        int adequateGeneratedArgCount = 0;
        Map<Integer, Boolean> testMap = new HashMap<>();
        for (ArgRecTest test: tests) {
            boolean adequateGeneratedExcode = false;
            boolean adequateGeneratedLex = false;
            //TODO: Handle unknown excode
            if (test.getNext_excode().contains(test.getExpected_excode())
                    || test.getExpected_excode().contains("<unk>")) adequateGeneratedExcode = true;
            if (test.getNext_lexList().contains(test.getExpected_lex())) adequateGeneratedLex = true;
            if (adequateGeneratedExcode) ++adequateGeneratedExcodeCount;
            if (adequateGeneratedLex) ++adequateGeneratedLexCount;
            if (adequateGeneratedExcode && adequateGeneratedLex) {
                ++adequateGeneratedArgCount;
                testMap.put(test.getId(), true);
            } else {
                //Logger.write(gson.toJson(test), projectName + "_inadequate_generated_arg_tests.txt");
            }
        }
        System.out.println(String.format("Adequate generated excodes: %.2f%%", 100.0 * adequateGeneratedExcodeCount / tests.size()));
        System.out.println(String.format("Adequate generated lexicals: %.2f%%", 100.0 * adequateGeneratedLexCount / tests.size()));
        System.out.println(String.format("Adequate generated candidates: %.2f%%", 100.0 * adequateGeneratedArgCount / tests.size()));


        //Collections.shuffle(tests);
        int testCount = 0;
        int nGramOverallCorrectTop1PredictionCount = 0;
        int nGramOverallCorrectTopKPredictionCount = 0;
        int RNNOverallCorrectTop1PredictionCount = 0;
        int RNNOverallCorrectTopKPredictionCount = 0;
        adequateGeneratedArgCount = 0;
        int nGramCorrectTop1PredictionCount = 0;
        int nGramCorrectTopKPredictionCount = 0;
        int RNNCorrectTop1PredictionCount = 0;
        int RNNCorrectTopKPredictionCount = 0;
        try {
            SocketClient socketClient = new SocketClient(18007);
            for (ArgRecTest test: tests) {
                Response response = socketClient.write(gson.toJson(test));
                if (response instanceof PredictResponse) {
                    PredictResponse predictResponse = (PredictResponse) response;
                    List<String> nGramResults = predictResponse.getData().ngram.getResult();
                    List<String> RNNResults = predictResponse.getData().rnn.getResult();

//                    System.out.println("==========================");
//                    System.out.println(gson.toJson(test));
//                    System.out.println("==========================");
//                    System.out.println("Result:");
//                    results.forEach(item -> {
//                        System.out.println(item);
//                    });
//                    System.out.println("==========================");
//                    System.out.println("Runtime: " + predictResponse.getData().ngram.getRuntime() + "s");
                    System.out.println(String.format("Progress: %.2f%%", 100.0 * testCount / tests.size()));

                    ++testCount;
                    if (testMap.getOrDefault(test.getId(), false)) ++adequateGeneratedArgCount;

                    if (!nGramResults.isEmpty() && nGramResults.get(0).equals(test.getExpected_lex())) {
                        ++nGramOverallCorrectTop1PredictionCount;
                        if (testMap.getOrDefault(test.getId(), false)) {
                            ++nGramCorrectTop1PredictionCount;
                        }
                    }
                    for (String item: nGramResults) {
                        if (item.equals(test.getExpected_lex())) {
                            ++nGramOverallCorrectTopKPredictionCount;
                            if (testMap.getOrDefault(test.getId(), false)) {
                                ++nGramCorrectTopKPredictionCount;
                            }
                            break;
                        }
                    }

                    if (!RNNResults.isEmpty() && RNNResults.get(0).equals(test.getExpected_lex())) {
                        ++RNNOverallCorrectTop1PredictionCount;
                        if (testMap.getOrDefault(test.getId(), false)) {
                            ++RNNCorrectTop1PredictionCount;
                        }
                    }
                    for (String item: RNNResults) {
                        if (item.equals(test.getExpected_lex())) {
                            ++RNNOverallCorrectTopKPredictionCount;
                            if (testMap.getOrDefault(test.getId(), false)) {
                                ++RNNCorrectTopKPredictionCount;
                            }
                            break;
                        }
                    }
                }
            }
            socketClient.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("==========================");
        System.out.println("Number of tests: " + testCount);
        System.out.println(String.format("NGram's top-1 accuracy: %.2f%%", 100.0 * nGramCorrectTop1PredictionCount / adequateGeneratedArgCount));
        System.out.println(String.format("NGram's top-K accuracy: %.2f%%", 100.0 * nGramCorrectTopKPredictionCount / adequateGeneratedArgCount));
        System.out.println(String.format("RNN's top-1 accuracy: %.2f%%", 100.0 * RNNCorrectTop1PredictionCount / adequateGeneratedArgCount));
        System.out.println(String.format("RNN's top-K accuracy: %.2f%%", 100.0 * RNNCorrectTopKPredictionCount / adequateGeneratedArgCount));
        System.out.println(String.format("Overall top-1 accuracy: %.2f%%", 100.0 *
                Math.max(nGramOverallCorrectTop1PredictionCount, RNNOverallCorrectTop1PredictionCount) / testCount));
        System.out.println(String.format("Overall top-K accuracy: %.2f%%", 100.0 *
                Math.max(nGramOverallCorrectTopKPredictionCount, RNNOverallCorrectTopKPredictionCount) / testCount));
        System.out.println(String.format("Actual top-1 accuracy: %.2f%%", 100.0 *
                Math.max(nGramOverallCorrectTop1PredictionCount, RNNOverallCorrectTop1PredictionCount) / (testCount + generator.discardedTests.size())));
        System.out.println(String.format("Actual top-K accuracy: %.2f%%", 100.0 *
                Math.max(nGramOverallCorrectTopKPredictionCount, RNNOverallCorrectTopKPredictionCount) / (testCount + generator.discardedTests.size())));
    }

    public static void setupGenerator(String projectName) throws IOException {
        Config.loadConfig(Config.STORAGE_DIR + "/json/" + projectName + ".json");
        ProjectParser projectParser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH,
                Config.ENCODE_SOURCE, Config.CLASS_PATH, Config.JDT_LEVEL, Config.JAVA_VERSION);
        generator = new ArgRecTestGenerator(Config.PROJECT_DIR, projectParser);
        generator.setLengthLimit(CONTEXT_LENGTH_LIMIT);
    }

    public static List<ArgRecTest> getTests(String projectName, boolean fromSavefile, boolean doSaveTestsAfterGen) throws IOException {
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
        return tests;
    }

    public static List<ArgRecTest> getTests(String projectName, boolean fromSavefile) throws IOException {
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
        return generator.generateAll();
    }

    public static List<ArgRecTest> generateTestsFromGitProject(String projectName) throws IOException {
        List<ArgRecTest> tests = new ArrayList<>();
        Scanner sc = new Scanner(new File("docs/testFilePath/" + projectName + ".txt"));
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            List<ArgRecTest> oneFileTests = generator.generate(Config.REPO_DIR + "git/" + line);
            for (ArgRecTest test: oneFileTests) test.setFilePath(line);
            tests.addAll(oneFileTests);
        }
        sc.close();
        return tests;
    }

    public static List<ArgRecTest> generateTestsFromFile(String projectName, String filePath) throws IOException {
        setupGenerator(projectName);
        return generator.generate(filePath);
    }

    public static void logTests(List<ArgRecTest> tests) {
        for (ArgRecTest test: tests) {
            System.out.println(gson.toJson(test));
        }
    }

    public static void saveTests(String projectName, List<ArgRecTest> tests) {
        for (ArgRecTest test: tests) {
            Logger.write(gson.toJson(test), projectName + "_tests.txt");
        }
    }
}
