package flute.tokenizing.exe;

import com.google.gson.Gson;
import flute.analysis.structure.DataFrame;
import flute.communicate.SocketClient;
import flute.communicate.schema.PredictResponse;
import flute.communicate.schema.Response;
import flute.config.Config;
import flute.jdtparser.ProjectParser;
import flute.tokenizing.excode_data.AllArgRecTest;
import flute.tokenizing.excode_data.ArgRecTest;
import flute.tokenizing.excode_data.NodeSequenceInfo;
import flute.utils.StringUtils;
import flute.utils.logging.Logger;
import flute.utils.logging.Timer;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ArgRecTester {
    private static int CONTEXT_LENGTH_LIMIT = 20;

    public static ArgRecTestGenerator generator;
    public static Gson gson = new Gson();

    public static boolean canAcceptGeneratedExcodes(ArgRecTest test) {
        String expectedExpcode = test.getExpected_excode();
        if (test.getNext_excode().contains(expectedExpcode)) return true;

        //TODO: Handle unknown excode
        if (expectedExpcode.contains("<unk>")) return true;

        for (NodeSequenceInfo excode: test.getExpected_excode_ori()) {
            if (NodeSequenceInfo.isMethodAccess(excode)) {
                int tmp = StringUtils.indexOf(expectedExpcode, "M_ACCESS(");
                tmp = expectedExpcode.indexOf("OPEN_PART", tmp);
                if (test.getNext_excode().contains(expectedExpcode.substring(0, tmp + 9))) return true;
                break;
            }
            if (NodeSequenceInfo.isObjectCreation(excode)) {
                int tmp = StringUtils.indexOf(expectedExpcode, "C_CALL(");
                tmp = expectedExpcode.indexOf("OPEN_PART", tmp);
                if (test.getNext_excode().contains(expectedExpcode.substring(0, tmp + 9))) return true;
                break;
            }
        }
        return false;
    }

    public static boolean canAcceptGeneratedExcodes(AllArgRecTest test) {
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

        for (NodeSequenceInfo excode: test.getExpected_excode_ori()) {
            if (NodeSequenceInfo.isMethodAccess(excode)) {
                String methodName = excode.getAttachedAccess();
                int tmp = StringUtils.indexOf(expectedLex, methodName + "(");
                if (test.getNext_lexList().contains(expectedLex.substring(0, tmp + methodName.length() + 1))) return true;
                break;
            }
            if (NodeSequenceInfo.isObjectCreation(excode)) {
                String className = excode.getAttachedAccess();
                int tmp = StringUtils.indexOf(expectedLex, className + "(");
                if (test.getNext_lexList().contains(expectedLex.substring(0, tmp + className.length() + 1))) return true;
                break;
            }
        }
        return false;
    }

    public static boolean canAcceptGeneratedLexes(AllArgRecTest test) {
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

        for (NodeSequenceInfo excode: test.getExpected_excode_ori()) {
            if (NodeSequenceInfo.isMethodAccess(excode)) {
                String methodName = excode.getAttachedAccess();
                int tmp = StringUtils.indexOf(expectedLex, methodName + "(");
                if (result.equals(expectedLex.substring(0, tmp + methodName.length() + 1))) return true;
                break;
            }
            if (NodeSequenceInfo.isObjectCreation(excode)) {
                String className = excode.getAttachedAccess();
                int tmp = StringUtils.indexOf(expectedLex, className + "(");
                if (result.equals(expectedLex.substring(0, tmp + className.length() + 1))) return true;
                break;
            }
        }
        return false;
    }

    public static boolean canAcceptResult(AllArgRecTest test, String result) {
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

    public static void main(String[] args) throws IOException {
        String projectName = "ant";
        Timer timer = new Timer();
        timer.startCounter();
        List<AllArgRecTest> tests = getTests(projectName, false, false);
        //List<AllArgRecTest> tests = generateTestsFromFile("demo", Config.REPO_DIR + "sampleproj/src/Main.java");
        double averageGetTestsTime = timer.getTimeCounter() / 1000f / (tests.size() + (generator == null? 0: generator.discardedTests.size()));

        //logTests(tests);

        System.out.println("Generated " + tests.size() + " tests.");

        long generatedExcodeCount = 0;
        long generatedLexCount = 0;
        for (AllArgRecTest test: tests) {
            generatedExcodeCount += test.getNext_excodeList().size();
            generatedLexCount += test.getNext_lexList().size();
        }
        System.out.println("Number of generated excode candidates: " + generatedExcodeCount);
        System.out.println("Number of generated lexical candidates: " + generatedLexCount);

        int adequateGeneratedExcodeCount = 0;
        int adequateGeneratedLexCount = 0;
        int adequateGeneratedArgCount = 0;
        Map<Integer, Boolean> testMap = new HashMap<>();
        for (AllArgRecTest test: tests) {
            boolean adequateGeneratedExcode = false;
            boolean adequateGeneratedLex = false;
            if (canAcceptGeneratedExcodes(test)) adequateGeneratedExcode = true;
            if (canAcceptGeneratedLexes(test)) adequateGeneratedLex = true;
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
        boolean isNGramUsed = false;
        boolean isRNNUsed = false;
        int nGramOverallCorrectTop1PredictionCount = 0;
        int nGramOverallCorrectTopKPredictionCount = 0;
        int RNNOverallCorrectTop1PredictionCount = 0;
        int RNNOverallCorrectTopKPredictionCount = 0;
        adequateGeneratedArgCount = 0;
        int nGramCorrectTop1PredictionCount = 0;
        int nGramCorrectTopKPredictionCount = 0;
        int RNNCorrectTop1PredictionCount = 0;
        int RNNCorrectTopKPredictionCount = 0;
        DataFrame dataFrame = new DataFrame();
        try {
            SocketClient socketClient = new SocketClient(18007);
            for (AllArgRecTest test: tests) {
                Response response = socketClient.write(gson.toJson(test));
                if (response instanceof PredictResponse) {
                    PredictResponse predictResponse = (PredictResponse) response;
                    isNGramUsed = predictResponse.getData().ngram != null;
                    isRNNUsed = predictResponse.getData().rnn != null;
                    List<String> nGramResults = null;
                    if (isNGramUsed) nGramResults = predictResponse.getData().ngram.getResult();
                    List<String> RNNResults = null;
                    if (isRNNUsed) RNNResults = predictResponse.getData().rnn.getResult();

//                    System.out.println("==========================");
//                    System.out.println(gson.toJson(test));
//                    if (isNGramUsed) {
//                        System.out.println("==========================");
//                        System.out.println("NGram's results:");
//                        nGramResults.forEach(item -> {
//                            System.out.println(item);
//                        });
//                        System.out.println("==========================");
//                        System.out.println("NGram's runtime: " + predictResponse.getData().ngram.getRuntime() + "s");
//                    }
//
//                    if (isRNNUsed) {
//                        System.out.println("==========================");
//                        System.out.println("RNN's results:");
//                        RNNResults.forEach(item -> {
//                            System.out.println(item);
//                        });
//                        System.out.println("==========================");
//                        System.out.println("RNN's runtime: " + predictResponse.getData().rnn.getRuntime() + "s");
//                    }

                    System.out.println(String.format("Progress: %.2f%%", 100.0 * testCount / tests.size()));

                    ++testCount;
                    if (testMap.getOrDefault(test.getId(), false)) ++adequateGeneratedArgCount;

                    if (isNGramUsed) {
                        if (!nGramResults.isEmpty() && canAcceptResult(test, nGramResults.get(0))) {
                            ++nGramOverallCorrectTop1PredictionCount;
                            if (testMap.getOrDefault(test.getId(), false)) {
                                ++nGramCorrectTop1PredictionCount;
                            }
                        }
                        for (String item: nGramResults) {
                            if (canAcceptResult(test, item)) {
                                ++nGramOverallCorrectTopKPredictionCount;
                                if (testMap.getOrDefault(test.getId(), false)) {
                                    ++nGramCorrectTopKPredictionCount;
                                }
                                break;
                            }
                        }
                    }

                    if (isRNNUsed) {
                        if (!RNNResults.isEmpty() && canAcceptResult(test, RNNResults.get(0))) {
                            ++RNNOverallCorrectTop1PredictionCount;
                            if (testMap.getOrDefault(test.getId(), false)) {
                                ++RNNCorrectTop1PredictionCount;
                            }
                        }
                        for (String item: RNNResults) {
                            if (canAcceptResult(test, item)) {
                                ++RNNOverallCorrectTopKPredictionCount;
                                if (testMap.getOrDefault(test.getId(), false)) {
                                    ++RNNCorrectTopKPredictionCount;
                                }
                                break;
                            }
                        }
                    }

                    if (isNGramUsed) dataFrame.insert("NGram's runtime", predictResponse.getData().ngram.getRuntime());
                    if (isRNNUsed) dataFrame.insert("RNN's runtime", predictResponse.getData().rnn.getRuntime());
                }
            }
            socketClient.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("==========================");
        System.out.println("Number of tests: " + testCount);
        if (isNGramUsed) {
            System.out.println(String.format("NGram's top-1 accuracy: %.2f%%", 100.0 * nGramCorrectTop1PredictionCount / adequateGeneratedArgCount));
            System.out.println(String.format("NGram's top-K accuracy: %.2f%%", 100.0 * nGramCorrectTopKPredictionCount / adequateGeneratedArgCount));
        }
        if (isRNNUsed) {
            System.out.println(String.format("RNN's top-1 accuracy: %.2f%%", 100.0 * RNNCorrectTop1PredictionCount / adequateGeneratedArgCount));
            System.out.println(String.format("RNN's top-K accuracy: %.2f%%", 100.0 * RNNCorrectTopKPredictionCount / adequateGeneratedArgCount));
        }
        System.out.println(String.format("Overall top-1 accuracy: %.2f%%", 100.0 *
                Math.max(nGramOverallCorrectTop1PredictionCount, RNNOverallCorrectTop1PredictionCount) / testCount));
        System.out.println(String.format("Overall top-K accuracy: %.2f%%", 100.0 *
                Math.max(nGramOverallCorrectTopKPredictionCount, RNNOverallCorrectTopKPredictionCount) / testCount));
        System.out.println(String.format("Actual top-1 accuracy: %.2f%%", 100.0 *
                Math.max(nGramOverallCorrectTop1PredictionCount, RNNOverallCorrectTop1PredictionCount) / (testCount + (generator == null? 0: generator.discardedTests.size()))));
        System.out.println(String.format("Actual top-K accuracy: %.2f%%", 100.0 *
                Math.max(nGramOverallCorrectTopKPredictionCount, RNNOverallCorrectTopKPredictionCount) / (testCount + (generator == null? 0: generator.discardedTests.size()))));
        System.out.println("Average parsing runtime: " + averageGetTestsTime + "s");
        if (isNGramUsed) System.out.println("Average NGram's runtime: " + dataFrame.getVariable("NGram's runtime").getMean() + "s");
        if (isRNNUsed) System.out.println("Average RNN's runtime: " + dataFrame.getVariable("RNN's runtime").getMean() + "s");
        System.out.println("Average overall runtime: "
                + (dataFrame.getVariable("NGram's runtime").getMean()
                + dataFrame.getVariable("RNN's runtime").getMean()
                + averageGetTestsTime) + "s");
    }

    public static void setupGenerator(String projectName) throws IOException {
        Config.loadConfig(Config.STORAGE_DIR + "/json/" + projectName + ".json");
        ProjectParser projectParser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH,
                Config.ENCODE_SOURCE, Config.CLASS_PATH, Config.JDT_LEVEL, Config.JAVA_VERSION);
        generator = new ArgRecTestGenerator(Config.PROJECT_DIR, projectParser);
        generator.setLengthLimit(CONTEXT_LENGTH_LIMIT);
    }

    public static List<AllArgRecTest> getTests(String projectName, boolean fromSavefile, boolean doSaveTestsAfterGen) throws IOException {
        List<AllArgRecTest> tests;
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

    public static List<AllArgRecTest> getTests(String projectName, boolean fromSavefile) throws IOException {
        return getTests(projectName, fromSavefile, false);
    }

    public static List<AllArgRecTest> readTestsFromFile(String filePath) throws IOException {
        Scanner sc = new Scanner(new File(filePath));
        List<AllArgRecTest> tests = new ArrayList<>();
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            tests.add(gson.fromJson(line, AllArgRecTest.class));
        }
        sc.close();
        return tests;
    }

    public static List<AllArgRecTest> generateTestsFromDemoProject() {
        return generator.generateAll();
    }

    public static List<AllArgRecTest> generateTestsFromGitProject(String projectName) throws IOException {
        List<AllArgRecTest> tests = new ArrayList<>();
        Scanner sc = new Scanner(new File("docs/testFilePath/" + projectName + ".txt"));
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            List<AllArgRecTest> oneFileTests = generator.generate(Config.REPO_DIR + "git/" + line);
            for (AllArgRecTest test: oneFileTests) test.setFilePath(line);
            tests.addAll(oneFileTests);
        }
        sc.close();
        return tests;
    }

    public static List<AllArgRecTest> generateTestsFromFile(String projectName, String filePath) throws IOException {
        setupGenerator(projectName);
        return generator.generate(filePath);
    }

    public static void logTests(List<AllArgRecTest> tests) {
        for (AllArgRecTest test: tests) {
            System.out.println(gson.toJson(test));
        }
    }

    public static void saveTests(String projectName, List<AllArgRecTest> tests) {
        for (AllArgRecTest test: tests) {
            Logger.write(gson.toJson(test), projectName + "_tests.txt");
        }
    }
}
