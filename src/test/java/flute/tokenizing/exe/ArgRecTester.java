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
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ArgRecTester {
    public static ArgRecTestGenerator generator;
    public static Gson gson = new Gson();

    public static void main(String[] args) throws IOException {
        String projectName = "ant";
        Config.loadConfig(Config.STORAGE_DIR + "/json/" + projectName + ".json");
        ProjectParser projectParser = new ProjectParser(Config.PROJECT_DIR, Config.SOURCE_PATH,
                Config.ENCODE_SOURCE, Config.CLASS_PATH, Config.JDT_LEVEL, Config.JAVA_VERSION);
        generator = new ArgRecTestGenerator(Config.PROJECT_DIR, projectParser);
        generator.setLengthLimit(20);

        List<ArgRecTest> tests;
        if (projectName.equals("")) {
            tests = readTestsFromFile(Config.LOG_DIR + "tests.txt");
        } else if (projectName.equals("demo")) {
            tests = generateTestsFromDemoProject();
        } else {
            tests = generateTestsFromGitProject(projectName);
        }
        //tests = generateTestsFromFile(Config.REPO_DIR + "sampleproj/src/Main.java");

        //logTests(tests);
        //saveTests(tests);

        System.out.println("Generated " + tests.size() + " tests.");

        int adequateGeneratedExcodeCount = 0;
        int adequateGeneratedLexCount = 0;
        int adequateGeneratedArgCount = 0;
        for (ArgRecTest test: tests) {
            boolean adequateGeneratedExcode = false;
            boolean adequateGeneratedLex = false;
            if (test.getNext_excode().contains(test.getExpected_excode())) adequateGeneratedExcode = true;
            if (test.getNext_lexList().contains(test.getExpected_lex())) adequateGeneratedLex = true;
            if (adequateGeneratedExcode) ++adequateGeneratedExcodeCount;
            if (adequateGeneratedLex) ++adequateGeneratedLexCount;
            if (adequateGeneratedExcode && adequateGeneratedLex) {
                ++adequateGeneratedArgCount;
            } else {
                //Logger.write(gson.toJson(test), "inadequate_generated_arg_tests.txt");
            }
        }
        System.out.println(String.format("Adequate generated excodes: %.2f%%", 100.0 * adequateGeneratedExcodeCount / tests.size()));
        System.out.println(String.format("Adequate generated lexicals: %.2f%%", 100.0 * adequateGeneratedLexCount / tests.size()));
        System.out.println(String.format("Adequate generated arguments: %.2f%%", 100.0 * adequateGeneratedArgCount / tests.size()));


//        //Collections.shuffle(tests);
//        int testCount = 0;
//        int correctTop1PredictionCount = 0;
//        int correctTopKPredictionCount = 0;
//        try {
//            SocketClient socketClient = new SocketClient(18007);
//            for (ArgRecTest test: tests) {
//                System.out.println("==========================");
//                System.out.println(gson.toJson(test));
//                Response response = socketClient.write(gson.toJson(test));
//                if (response instanceof PredictResponse) {
//                    PredictResponse predictResponse = (PredictResponse) response;
//                    System.out.println("==========================");
//                    System.out.println("Result:");
//                    List<String> results = predictResponse.getData();
//                    results.forEach(item -> {
//                        System.out.println(item);
//                    });
//                    System.out.println("==========================");
//                    System.out.println("Runtime: " + predictResponse.getRuntime() + "s");
//
//                    ++testCount;
//                    if (results.get(0).equals(test.getExpected_lex())) ++correctTop1PredictionCount;
//                    for (String item: results) {
//                        if (item.equals(test.getExpected_lex())) ++correctTopKPredictionCount;
//                    }
//                }
//            }
//            socketClient.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        System.out.println("==========================");
//        System.out.println("Number of tests: " + testCount);
//        System.out.println(String.format("Top-1 accuracy: %.2f%%", 100.0 * correctTop1PredictionCount / testCount));
//        System.out.println(String.format("Top-K accuracy: %.2f%%", 100.0 * correctTopKPredictionCount / testCount));
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

    public static List<ArgRecTest> generateTestsFromFile(String filePath) {
        return generator.generate(filePath);
    }

    public static void logTests(List<ArgRecTest> tests) {
        for (ArgRecTest test: tests) {
            System.out.println(gson.toJson(test));
        }
    }

    public static void saveTests(List<ArgRecTest> tests) {
        for (ArgRecTest test: tests) {
            Logger.write(gson.toJson(test), "tests.txt");
        }
    }
}
